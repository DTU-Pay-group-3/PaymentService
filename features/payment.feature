Feature: Payment
  Scenario: Payment Request Race
    Given A "RequestPayment" event is published with token "token" and amount 200
    And A second "RequestPayment" event is published with token "token2" and amount 300
    Then The "ValidateToken" event is published to find the first customerID
    And The "ValidateToken" event is published to find the second customerID
    When The "ValidateTokenCompleted" events are published and returns the userids
    Then 4 "RequestBankAccId" events are published
    When The "BankAccReturned" event is found and returns the bank accounts and payments are created
    Then The first payment success event is pushed with the same correlation id as request
    And The Second payment success event is pushed with the same correlation id as request

