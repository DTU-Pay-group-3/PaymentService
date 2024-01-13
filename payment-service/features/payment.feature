Feature: Payment
  Scenario: Payment Request Successfully
    Given A "RequestPayment" event is published with token "token" and amount 250
    Then The "ValidateToken" event is published to find the customerID
    When The "TokenValidated" event is published and returns the userid "userId"
    Then The "RequestBankAccId" event is published
    When The verification is successfull and returns "customerBankId1" and payment is created
    Then The payment success event is pushed