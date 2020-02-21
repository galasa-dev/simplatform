package dev.galasa.translatedghrekin;

import dev.galasa.simbank.manager.SimBank;
import dev.galasa.simbank.manager.ghrekin.CucumberSimbank;
import dev.galasa.http.IHttpClient;
import dev.galasa.simbank.manager.ISimBank;
import dev.galasa.simbank.manager.AccountType;
import dev.galasa.Test;
import dev.galasa.artifact.IArtifactManager;
import dev.galasa.artifact.ArtifactManager;
import java.lang.Exception;
import dev.galasa.simbank.manager.IAccount;
import dev.galasa.simbank.manager.Account;
import dev.galasa.http.HttpClient;

@Test
public class SimbankGhrekin {
@Account(balance = "1000")
public IAccount iaccount1;

@Account(balance = "-100")
public IAccount iaccount2;

@Account(accountType = AccountType.UnOpened)
public IAccount iaccount3;

@ArtifactManager
public IArtifactManager iartifactmanager;

@HttpClient
public IHttpClient ihttpclient;

@SimBank
public ISimBank isimbank;

@Test
public void creditAnAccountAlreadyInCredit() {
Exception exception1 = CucumberSimbank.whenTheWebApiIsCalledToCreditTheAccountWith("500",iaccount1,iartifactmanager,this.getClass(),ihttpclient,isimbank);
CucumberSimbank.thenTheBalanceOfTheAccountShouldBe("1500",iaccount1);
}
@Test
public void creditAnAccountInDebt() {
Exception exception2 = CucumberSimbank.whenTheWebApiIsCalledToCreditTheAccountWith("500",iaccount2,iartifactmanager,this.getClass(),ihttpclient,isimbank);
CucumberSimbank.thenTheBalanceOfTheAccountShouldBe("400",iaccount2);
}
@Test
public void creditAnAccountThatDoesntExist() {
Exception exception3 = CucumberSimbank.whenTheWebApiIsCalledToCreditTheAccountWith("500",iaccount3,iartifactmanager,this.getClass(),ihttpclient,isimbank);
CucumberSimbank.thenASpecificExceptionIsThrown(exception3);
}
}