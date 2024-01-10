Feature: Payment

  Scenario: Payment is successful
    Given a customer in the bank
    And a merchant in the bank
    When the merchant initiates a transfer of 200 kr from the customer
    Then the message "OK" is shown
