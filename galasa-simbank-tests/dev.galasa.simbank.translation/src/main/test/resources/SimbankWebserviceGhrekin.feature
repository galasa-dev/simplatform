Feature: Crediting an existing account
  People like to pay money into their accounts
  
  Scenario: Credit an account already in credit
    Given I have an account with a balance of 1000
    When the web API is called to credit the account with 500
    Then the balance of the account should be 1500

  Scenario: Credit an account in debt
    Given I have an account with a balance of -100
    When the web API is called to credit the account with 500
    Then the balance of the account should be 400
    
  Scenario: Credit an account that doesn't exist
    Given I have an account that doesn't exist
    When the web API is called to credit the account with 500
    Then a accountNotFound Exception is thrown