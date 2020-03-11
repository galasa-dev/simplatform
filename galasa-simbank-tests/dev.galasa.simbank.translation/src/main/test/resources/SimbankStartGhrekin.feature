Feature: SimBnk installation verification
  Check if the SimBnk application is installed
  
  Scenario: Simbank is installed
    Given The Simbank is available
    When I navigate to SimBank
    Then I should see the main screen
