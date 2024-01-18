Feature: Payment
  Scenario: Payment Request Single -- needs refactoring of steps
    Given A "RequestPayment" event is published with token "token" and amount 200
    Then The "ValidateToken" event is awaited to find the first customerID
    Then The "ValidateTokenComplete" event is published with the first customer id
    Then The 2 "BankAccReturned" events are published for the first payment
    Then The first payment success event is pushed with the same correlation id as request

  Scenario: Payment Request Race v2
    Given A "RequestPayment" event is published with token "token" and amount 200
    And A second "RequestPayment" event is published with token "token2" and amount 300
    Then The "ValidateToken" event is awaited to find the first customerID
    And The "ValidateToken" event is awaited to find the second customerID
    Then The "ValidateTokenComplete" event is published with the first customer id
    And The "ValidateTokenComplete" event is published with the second customer id
    Then The 2 "BankAccReturned" events are published for the first payment
    Then The 2 "BankAccReturned" events are published for the second payment
    Then The first payment success event is pushed with the same correlation id as request
    And The Second payment success event is pushed with the same correlation id as request