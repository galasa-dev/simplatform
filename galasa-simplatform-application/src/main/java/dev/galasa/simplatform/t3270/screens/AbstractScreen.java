package dev.galasa.simplatform.t3270.screens;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.logging.Logger;

import dev.galasa.zos3270.AttentionIdentification;
import dev.galasa.zos3270.internal.comms.Network;
import dev.galasa.zos3270.internal.comms.NetworkServer;
import dev.galasa.zos3270.internal.comms.NetworkThread;
import dev.galasa.zos3270.internal.datastream.BufferAddress;
import dev.galasa.zos3270.internal.datastream.CommandEraseWrite;
import dev.galasa.zos3270.internal.datastream.Order;
import dev.galasa.zos3270.internal.datastream.OrderSetBufferAddress;
import dev.galasa.zos3270.internal.datastream.OrderStartField;
import dev.galasa.zos3270.internal.datastream.WriteControlCharacter;
import dev.galasa.zos3270.spi.BufferChar;
import dev.galasa.zos3270.spi.BufferHolder;
import dev.galasa.zos3270.spi.BufferStartOfField;
import dev.galasa.zos3270.spi.Field;
import dev.galasa.zos3270.spi.Screen;

public abstract class AbstractScreen implements IScreen {

	protected final NetworkServer network;
	protected Logger log;

	public AbstractScreen(NetworkServer network) {
		this.network = network;
		this.log = Logger.getLogger("Simplatform");
	}

	protected void writeScreen(CommandEraseWrite commandEraseWrite, 
			WriteControlCharacter writeControlCharacter,
			Screen screen) throws ScreenException {
		try {
			ByteArrayOutputStream outboundBuffer = new ByteArrayOutputStream();
			outboundBuffer.write(commandEraseWrite.getBytes());
			outboundBuffer.write(writeControlCharacter.getBytes());

			for(Field field : screen.calculateFields()) {
				OrderSetBufferAddress sba = new OrderSetBufferAddress(new BufferAddress(field.getStart()));
				outboundBuffer.write(sba.getCharRepresentation());
				if (!field.isDummyField()) {
					OrderStartField sf = new OrderStartField(field.isProtected(), field.isNumeric(), field.isDisplay(), field.isIntenseDisplay(), field.isSelectorPen(), field.isFieldModifed());
					outboundBuffer.write(sf.getBytes());
				}

				outboundBuffer.write(field.getFieldWithNulls());
			}

			network.sendDatastream(outboundBuffer.toByteArray());
		} catch(Exception e) {
			throw new ScreenException("Problem writing screen",e);
		}
	}

	protected Screen buildScreen(String screenName) throws ScreenException {
		log.info("Building Screen: " + screenName);
		try (BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/screens/" + screenName)))) {	
			BufferHolder[] buffer = new BufferHolder[1920];
			String line = null;
			int cursorPosition = 0;
			while((line = br.readLine()) != null) {
				String controlChar = line.substring(0, 1);
				if (!"S".equals(controlChar)) {
					continue;
				}
				int lineLength = line.length();
				if (lineLength > 81) {
					lineLength = 81;
				}
				line = line.substring(1,lineLength);
				while(line.length() < 80) {
					line += " ";
				}

				for(int i = 0; i < 80; i++) {
					char c = line.charAt(i);

					if (c == ']') {
						buffer[cursorPosition] = new BufferStartOfField(cursorPosition, true, false, true, false, false, false);
					} else if (c == '@') {
						buffer[cursorPosition] = new BufferStartOfField(cursorPosition, true, false, true, true, false, false);
					} else if (c == '[') {
						buffer[cursorPosition] = new BufferStartOfField(cursorPosition, false, false, true, false, false, true);
					} else if (c == '{') {
						buffer[cursorPosition] = new BufferStartOfField(cursorPosition, false, false, false, false, false, true);
					} else {
						buffer[cursorPosition] = new BufferChar(c);
					}

					cursorPosition++;
				}
			}

			Screen screen = new Screen(80, 24, null);
			screen.setBuffer(buffer);

			return screen;
		} catch(Exception e) {
			throw new ScreenException("Unable to format the screen", e);
		}
	}


	protected AttentionIdentification receiveScreen(Screen screen) throws ScreenException {
		try {
			Network.expect(network.getInputStream(), (byte)0); // Should be D3270

			ByteBuffer buffer = NetworkThread.readTerminatedMessage(network.getInputStream());
			buffer.get();  // Request
			buffer.get();  // Response 
			buffer.get();  // SEQ
			buffer.get();  // SEQ

			byte aid = buffer.get();

			if (buffer.hasRemaining()) {
				byte[] cursor = new byte[2];
				buffer.get(cursor);

				if (buffer.hasRemaining()) {
					List<Order> orders = NetworkThread.processOrders(buffer);


					screen.processOrders(orders);
				}
			}

			return AttentionIdentification.valueOfAid(aid);
		} catch(Exception e) {
			//Empty exception thrown to prevent error trace on a client disconnecting 
			if (e.getMessage() == "Expected 1 but received only -1 bytes")
				throw new ScreenException();
			else
				throw new ScreenException("Problem reading screen",e);
		}
	}

	protected int calcPos(int col, int row) {
		return col + (row * 80);
	}

}
