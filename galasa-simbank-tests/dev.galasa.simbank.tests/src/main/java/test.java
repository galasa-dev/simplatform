import dev.galasa.simbank.manager.ghrekin.CucumberSimbank;
import dev.galasa.Test;


@Test
public class test {
@dev.galasa.simbank.manager.Account(balance = "1000")
public dev.galasa.simbank.manager.IAccount iaccount1;

@dev.galasa.simbank.manager.Account(balance = "-100")
public dev.galasa.simbank.manager.IAccount iaccount2;

@dev.galasa.simbank.manager.Account
private dev.galasa.simbank.manager.IAccount iaccount3;

@dev.galasa.artifact.ArtifactManager
public dev.galasa.artifact.IArtifactManager iartifactmanager;

@dev.galasa.http.HttpClient
public dev.galasa.http.IHttpClient ihttpclient;

@dev.galasa.simbank.manager.SimBank
public dev.galasa.simbank.manager.ISimBank isimbank;


//   People like to pay money into their accounts
@Test
public void creditAnAccountAlreadyInCredit() {
java.lang.Exception exception1 = CucumberSimbank.whenTheWebApiIsCalledToCreditTheAccountWith("500",iaccount1,iartifactmanager,this.getClass(),ihttpclient,isimbank);
CucumberSimbank.thenTheBalanceOfTheAccountShouldBe("1500",iaccount1);
}
@Test
public void creditAnAccountInDebt() {
java.lang.Exception exception2 = CucumberSimbank.whenTheWebApiIsCalledToCreditTheAccountWith("500",iaccount2,iartifactmanager,this.getClass(),ihttpclient,isimbank);
CucumberSimbank.thenTheBalanceOfTheAccountShouldBe("400",iaccount2);
}
@Test
public void creditAnAccountThatDoesntExist() {
java.lang.Exception exception3 = CucumberSimbank.whenTheWebApiIsCalledToCreditTheAccountWith("500",iaccount3,iartifactmanager,this.getClass(),ihttpclient,isimbank);
CucumberSimbank.thenASpecificExceptionIsThrown(exception3);
}
}