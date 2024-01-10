package dtu.payment;

import dtu.ws.fastmoney.BankService;
import dtu.ws.fastmoney.BankServiceService;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class PaymentSteps {

    BankService bank = new BankServiceService().getBankServicePort();

    @Given("a customer in the bank")
    public void aCustomerInTheBank() {

    }

    @And("a merchant in the bank")
    public void aMerchantInTheBank() {
    }

    @When("the merchant initiates a transfer of {int} kr from the customer")
    public void theMerchantInitiatesATransferOfKrFromTheCustomer(int arg0) {
    }

    @Then("the message {string} is shown")
    public void theMessageIsShown(String arg0) {
    }
}
