Feature: SimBank IVT
  Scenario: Validate that the SimBank application is running
    GIVEN a terminal
    
    THEN wait for "SIMPLATFORM LOGON SCREEN" in any terminal field
    
    AND wait for terminal keyboard
    AND move terminal cursor to field "Userid"
    AND press terminal key TAB
    AND type credentials username on terminal
    AND press terminal key TAB
    AND type credentials password on terminal
    AND press terminal key ENTER
    AND wait for terminal keyboard
    
    THEN check "SIMPLATFORM MAIN MENU" appears only once on terminal

	AND press terminal key PF1
    AND wait for terminal keyboard
    AND press terminal key CLEAR
    AND wait for terminal keyboard
    AND type "bank" on terminal
    AND press terminal key ENTER
    AND wait for terminal keyboard
    
    THEN check "Options     Description        PFKey" appears only once on terminal
    THEN check "BROWSE      Browse Accounts    PF1" appears only once on terminal
    THEN check "TRANSF      Transfer Money     PF4" appears only once on terminal
    