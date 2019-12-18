package simplatform.unittest;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import dev.galasa.simplatform.application.Bank;
import dev.galasa.simplatform.listener.AccountTransferListener;

public class TestingP {
	
	@Mock
	Bank bank;
	
	@Mock
	AccountTransferListener listener;
//	
//	@Test
//	public void TestTest() {
//		assertEquals(2, AccountTransferListener.testMethod(1, 1));
//	}
//	
//	@Test
//	public void processInputTest() {
//		listener.processInput();
//	}
//	
//	@Test
//	public void testAccountTransfer() throws Exception {
//		MockitoAnnotations.initMocks(this);
//		doNothing().when(bank).transferMoney(anyString(), anyString(), anyDouble());
//		listener.accountTransfer();
//	}
}