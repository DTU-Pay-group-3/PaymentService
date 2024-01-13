package behaviourtests;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import dtu.ws.fastmoney.BankService;
import dtu.ws.fastmoney.BankServiceException_Exception;
import dtu.ws.fastmoney.BankServiceService;
import dtu.ws.fastmoney.User;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import main.PaymentService;
import messaging.Event;
import messaging.MessageQueue;
import models.Payment;

import java.math.BigDecimal;

public class PaymentServiceSteps {
    private MessageQueue q = mock(MessageQueue.class);

    private PaymentService service = new PaymentService(q);

    private Payment payment;
    private User customer=new User();
    private User merchant= new User();

    private String customerId;

    private String merchantBankID;

    private String customerBankID;

    private String customerToken;
    private BankService bank = new BankServiceService().getBankServicePort();


    @Before
    public void SetupAccounts() throws BankServiceException_Exception {
        customer.setFirstName("ASDIUHW");
        customer.setLastName("DIAUSHD");
        customer.setCprNumber("5646335");

        merchant.setFirstName("LASKGDAJ");
        merchant.setLastName("IASUGAUYIS");
        merchant.setCprNumber("743673654");
try {

    customerBankID = bank.createAccountWithBalance(customer, BigDecimal.valueOf(500));
}catch (Exception e){
    System.out.println(customer.getFirstName()+" EXIST");
}
        try {

            merchantBankID = bank.createAccountWithBalance(merchant, BigDecimal.valueOf(500));
        }catch (Exception e){
            System.out.println(merchant.getFirstName()+" EXIST");
        }
        System.out.println(customerBankID +" account created");
        System.out.println(merchantBankID +" account created");

    }

    // I believe that this is supposed to test if all events are published correctly with the correct info
    // and in the end if bank.MakePayment doesn't give an error or bad status code the test should pass
    @Given("A {string} event is published with token {string} and amount {int}")
    public void aEventIsPublishedWithTokenAndAmount(String arg0, String arg1, int arg2) {
        customerToken=arg1;
        payment = new Payment( merchantBankID,customerToken, "PaymentDesc1", BigDecimal.valueOf(arg2));
        //q.publish(new Event(arg0, new Object[]{payment}));
        // publish needs to wait for the service to finish before running the next step
        service.makePayment(new Event(arg0, new Object[]{payment}));
    }

    @Then("The {string} event is published to find the customerID")
    public void theEventIsPublishedToFindTheCustomerID(String arg0) {
        var event = new Event(arg0, new Object[]{customerToken});
        verify(q).publish(event);
    }

    @When("The {string} event is published and returns the userid {string}")
    public void theEventIsPublishedAndReturnsTheUserid(String arg0, String arg1) {
        customerId=arg1;
        service.handleTokenValidated(new Event(arg0, new Object[]{arg1}));

    }

    @Then("The {string} event is published")
    public void theEventIsPublished(String arg0) {
        //event expected
        var event = new Event(arg0, new Object[]{customerId});
        verify(q).publish(event);
    }

    @When("The verification is successfull and returns {string} and payment is created")
    public void theVerificationIsSuccessfullAndReturnsAndPaymentIsCreated(String arg0) {
        service.handleBankAccId(new Event("BankAccId", new Object[]{customerBankID}));
    }

    @Then("The payment success event is pushed")
    public void thePaymentSuccessEventIsPushed() {
        //expected event from the service
        String expected = "PaymentGood";
        var event = new Event("PaymentCompleted", new Object[]{expected});
        verify(q).publish(event);
    }

    @After
    public void Clean() throws BankServiceException_Exception{
        //It is probably not necessary to check the balance since the PaymentComplete event was published
        System.out.println(bank.getAccount(customerBankID).getBalance());
        System.out.println(bank.getAccount(merchantBankID).getBalance());

        bank.retireAccount(customerBankID);
        bank.retireAccount(merchantBankID);
    }



}

