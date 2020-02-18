import dev.galasa.Test;
@Test
public class test {
//   People like to pay money into their accounts
@Test
public void creditAnAccountAlreadyInCredit() {
//     Given I have an account with a balance of 1000
//     When the web API is called to credit the account with 500
//     Then the balance of the account should be 1500
}
@Test
public void creditAnAccountInDebt() {
//     Given I have an account with a balance of -100
//     When the web API is called to credit the account with 500
//     Then the balance of the account should be 400
}
@Test
public void creditAnAccountThatDoesntExist() {
//     Given I have an account that doesn't exist
//     When the web API is called to credit the account with 500
//     Then a accountNotFound Exception is thrown
}
}