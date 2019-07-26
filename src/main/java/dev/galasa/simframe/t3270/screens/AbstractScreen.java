package dev.galasa.simframe.t3270.screens;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import dev.voras.common.zos3270.AttentionIdentification;
import dev.voras.common.zos3270.internal.comms.Network;
import dev.voras.common.zos3270.internal.comms.NetworkServer;
import dev.voras.common.zos3270.internal.comms.NetworkThread;
import dev.voras.common.zos3270.internal.datastream.BufferAddress;
import dev.voras.common.zos3270.internal.datastream.CommandEraseWrite;
import dev.voras.common.zos3270.internal.datastream.Order;
import dev.voras.common.zos3270.internal.datastream.OrderRepeatToAddress;
import dev.voras.common.zos3270.internal.datastream.OrderSetBufferAddress;
import dev.voras.common.zos3270.internal.datastream.OrderStartField;
import dev.voras.common.zos3270.internal.datastream.WriteControlCharacter;
import dev.voras.common.zos3270.internal.terminal.fields.Field;
import dev.voras.common.zos3270.internal.terminal.fields.FieldChars;
import dev.voras.common.zos3270.internal.terminal.fields.FieldStartOfField;
import dev.voras.common.zos3270.internal.terminal.fields.FieldText;
import dev.voras.common.zos3270.spi.DatastreamException;
import dev.voras.common.zos3270.spi.Screen;

public abstract class AbstractScreen implements IScreen {

	protected final NetworkServer network;

	public AbstractScreen(NetworkServer network) {
		this.network = network;
	}

	protected void writeScreen(CommandEraseWrite commandEraseWrite, 
			WriteControlCharacter writeControlCharacter,
			Screen screen) throws ScreenException {
		try {
			ByteArrayOutputStream outboundBuffer = new ByteArrayOutputStream();
			outboundBuffer.write(commandEraseWrite.getBytes());
			outboundBuffer.write(writeControlCharacter.getBytes());

			for(Field field : screen.getFields()) {
				if (field instanceof FieldStartOfField) {
					FieldStartOfField fsf = (FieldStartOfField) field;
					OrderSetBufferAddress sba = new OrderSetBufferAddress(new BufferAddress(fsf.getStart()));
					outboundBuffer.write(sba.getCharRepresentation());
					OrderStartField sf = new OrderStartField(fsf.isProtected(), fsf.isNumeric(), fsf.isDisplay(), fsf.isIntenseDisplay(), fsf.isSelectorPen(), fsf.isFieldModifed());
					outboundBuffer.write(sf.getBytes());
				} else if (field instanceof FieldText) {
					byte[] fieldText = field.getFieldEbcdicWithoutNulls();
					outboundBuffer.write(fieldText);
				} else if (field instanceof FieldChars) {
					FieldChars fc = (FieldChars) field;
					int end = fc.getEnd() + 1;
					if (end >= 1920) {
						end = end - 1920;
					}
					OrderRepeatToAddress ra = new OrderRepeatToAddress(fc.getCharacter(), new BufferAddress(end));
					outboundBuffer.write(ra.getBytes());
				} else {
					throw new UnsupportedOperationException("Unsupported field " + field.getClass().getName());
				}

			}

			network.sendDatastream(outboundBuffer.toByteArray());
		} catch(Exception e) {
			throw new ScreenException("Problem writing screen",e);
		}
	}

	protected Screen buildScreen(String screenName) throws ScreenException {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/screens/" + screenName)))) {	
			LinkedList<Field> fields = new LinkedList<>();
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
						fields.add(new FieldStartOfField(cursorPosition, true, false, true, false, false, false));
					} else if (c == '@') {
						fields.add(new FieldStartOfField(cursorPosition, true, false, true, true, false, false));
					} else if (c == '[') {
						fields.add(new FieldStartOfField(cursorPosition, false, false, true, false, false, true));
					} else if (c == '{') {
						fields.add(new FieldStartOfField(cursorPosition, false, false, false, false, false, true));
					} else {
						fields.add(new FieldChars(c, cursorPosition, cursorPosition));
					}

					cursorPosition++;
				}
			}

			int pos = 0;
			//*** Merge suitable fields,  text and chars
			while(pos < fields.size()) { //NOSONAR
				int nextPos = pos + 1;
				if (nextPos >= fields.size()) {
					break;
				}

				Field thisField = fields.get(pos);
				Field nextField = fields.get(nextPos);

				if ((thisField instanceof FieldText) 
						|| (thisField instanceof FieldChars)) {
					if ((nextField instanceof FieldText)  //NOSONAR
							|| (nextField instanceof FieldChars)) {
						thisField.merge(fields, nextField);
						continue; // Go round again without incrementing position
					}
				}
				pos++;
			}

			Screen screen = new Screen(80, 24);
			screen.setFields(fields);

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
			throw new ScreenException("Problem reading screen",e);
		}
	}

	protected int calcPos(int col, int row) {
		return col + (row * 80);
	}

	protected void makeTextField(Screen screen, int col, int row) throws DatastreamException {
		Field field = screen.locateFieldsAt(calcPos(col, row));
		if (field instanceof FieldChars) {
			screen.convertCharsToText((FieldChars) field);
		}
	}



}
