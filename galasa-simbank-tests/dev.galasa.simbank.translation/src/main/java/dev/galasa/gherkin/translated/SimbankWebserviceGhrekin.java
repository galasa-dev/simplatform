package dev.galasa.gherkin.translated;

import dev.galasa.simbank.manager.SimBank;
import dev.galasa.http.IHttpClient;
import dev.galasa.simbank.manager.ISimBank;
import dev.galasa.simbank.manager.AccountType;
import dev.galasa.Test;
import dev.galasa.artifact.IArtifactManager;
import dev.galasa.simbank.manager.gherkin.CucumberSimbank;
import dev.galasa.artifact.ArtifactManager;
import java.lang.Exception;
import dev.galasa.simbank.manager.IAccount;
import dev.galasa.simbank.manager.Account;
import dev.galasa.http.HttpClient;

@Test
public class SimbankWebserviceGhrekin {
	//I have an account with a balance of 1000
	@Account(balance = "1000")
	public IAccount iaccount1;

	//I have an account with a balance of -100
	@Account(balance = "-100")
	public IAccount iaccount2;

	//I have an account that doesn't exist
	@Account(accountType = AccountType.UnOpened)
	public IAccount iaccount3;

	@ArtifactManager
	public IArtifactManager iartifactmanager;

	@HttpClient
	public IHttpClient ihttpclient;

	@SimBank
	public ISimBank isimbank;

	//People like to pay money into their accounts

	@Test
	public void creditAnAccountAlreadyInCredit() {
		//the web API is called to credit the account with 500
		Exception exception1 = CucumberSimbank.whenTheWebApiIsCalledToCreditTheAccountWith("500", iaccount1, iartifactmanager, this.getClass(), ihttpclient, isimbank);
		//the balance of the account should be 1500
		CucumberSimbank.thenTheBalanceOfTheAccountShouldBe("1500", exception1, iaccount1);
	}

	@Test
	public void creditAnAccountInDebt() {
		//the web API is called to credit the account with 500
		Exception exception2 = CucumberSimbank.whenTheWebApiIsCalledToCreditTheAccountWith("500", iaccount2, iartifactmanager, this.getClass(), ihttpclient, isimbank);
		//the balance of the account should be 400
		CucumberSimbank.thenTheBalanceOfTheAccountShouldBe("400", exception2, iaccount2);
	}

	@Test
	public void creditAnAccountThatDoesntExist() {
		//the web API is called to credit the account with 500
		Exception exception3 = CucumberSimbank.whenTheWebApiIsCalledToCreditTheAccountWith("500", iaccount3, iartifactmanager, this.getClass(), ihttpclient, isimbank);
		//a accountNotFound Exception is thrown
		CucumberSimbank.thenASpecificExceptionIsThrown(exception3);
	}
}