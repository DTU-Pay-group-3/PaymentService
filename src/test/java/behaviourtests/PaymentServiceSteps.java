package behaviourtests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import dtu.ws.fastmoney.BankService;
import dtu.ws.fastmoney.BankServiceException_Exception;
import dtu.ws.fastmoney.BankServiceService;
import dtu.ws.fastmoney.User;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import main.CorrelationId;
import main.PaymentService;
import messaging.Event;
import messaging.MessageQueue;
import models.Payment;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
//not working at the moment
public class PaymentServiceSteps {
    //    private MessageQueue q = mock(MessageQueue.class);
    private Map<String, CompletableFuture<Event>> publishedEvents = new ConcurrentHashMap<>();

    private MessageQueue q = new MessageQueue() {

        @Override
        public void publish(Event event) {
            var token = event.getArgument(0, String.class);
            publishedEvents.get(token).complete(event);
        }

        @Override
        public void addHandler(String eventType, Consumer<Event> handler) {
        }

    };
    private PaymentService service = new PaymentService(q);

    private Payment payment;
    private Payment payment2;
    private User customer = new User();
    private User customer2 = new User();
    private User merchant = new User();

    private String customerId;
    private String customerId2;
    private String merchantID;

    private String merchantBankID;

    private String customerBankID;
    private String customerBankID2;

    private String customerToken;
    private String customerToken2;
    private BankService bank = new BankServiceService().getBankServicePort();

    private CompletableFuture<Event> finalResult = new CompletableFuture<>();
    private CompletableFuture<Event> finalResult2 = new CompletableFuture<>();

    private CorrelationId correlationPaymentReq;
    private CorrelationId correlationPaymentReq2;
    private Map<String, CorrelationId> correlationIdsToken = new HashMap<>();
    private Map<String, CorrelationId> correlationIdsBankAcc = new HashMap<>();


    @Before
    public void SetupAccounts() throws BankServiceException_Exception {

        customer.setFirstName("DONTREMOVE554855");
        customer.setLastName("DONTREMOVELAST558455");
        customer.setCprNumber("232423333586545");

        customer2.setFirstName("DONTREMOVE2565845");
        customer2.setLastName("DONTREMOVELAST2455685");
        customer2.setCprNumber("433234121856545");


        merchant.setFirstName("DONTREMOVE3568545");
        merchant.setLastName("DONTREMOVELAST3546585");
        merchant.setCprNumber("5674567453654585465");
        try {

            customerBankID = bank.createAccountWithBalance(customer, BigDecimal.valueOf(500));
            customerBankID2 = bank.createAccountWithBalance(customer2, BigDecimal.valueOf(850));
        } catch (Exception e) {
            System.out.println(customer.getFirstName() + " EXIST");
        }
        try {

            merchantBankID = bank.createAccountWithBalance(merchant, BigDecimal.valueOf(500));
        } catch (Exception e) {
            System.out.println(merchant.getFirstName() + " EXIST");
        }
        System.out.println(customerBankID + " account created");
        System.out.println(customerBankID2 + " account created");
        System.out.println(merchantBankID + " account created");

    }

    // I believe that this is supposed to test if all events are published correctly with the correct info
    // and in the end if bank.MakePayment doesn't give an error or bad status code the test should pass
    @Given("A {string} event is published with token {string} and amount {int}")
    public void aEventIsPublishedWithTokenAndAmount(String arg0, String arg1, int arg2) {
        merchantID="mid1";
        customerId="cid1";
        customerToken = arg1;
        payment = new Payment(merchantID, customerToken, "PaymentDesc1", BigDecimal.valueOf(arg2));
        //q.publish(new Event(arg0, new Object[]{payment}));
        // publish needs to wait for the service to finish before running the next step
        publishedEvents.put(arg1, new CompletableFuture<Event>());
        publishedEvents.put(payment.getDescription(), new CompletableFuture<Event>());

        correlationPaymentReq = CorrelationId.randomId();
        new Thread(() -> {
            service.makePaymentCorid(new Event(arg0, new Object[]{payment, correlationPaymentReq}));
        }).start();
    }

    @And("A second {string} event is published with token {string} and amount {int}")
    public void aSecondEventIsPublishedWithTokenAndAmount(String arg0, String arg1, int arg2) {
        customerId2="mid2";
        customerToken2 = arg1;
        payment2 = new Payment(merchantID, customerToken2, "PaymentDesc2", BigDecimal.valueOf(arg2));
        publishedEvents.put(arg1, new CompletableFuture<Event>());
        publishedEvents.put(payment2.getDescription(), new CompletableFuture<Event>());

        correlationPaymentReq2 = CorrelationId.randomId();
        new Thread(() -> {
            service.makePaymentCorid(new Event(arg0, new Object[]{payment2, correlationPaymentReq2}));
        }).start();

    }

    @Then("The {string} event is published to find the first customerID")
    public void theEventIsPublishedToFindTheFirstCustomerID(String arg0) {
        new Thread(() -> {
            var result = publishedEvents.get("PaymentDesc1").join();
            finalResult.complete(result);
        }).start();

        new Thread(() -> {
            var result = publishedEvents.get("PaymentDesc2").join();
            finalResult2.complete(result);
        }).start();
        //check if service publishes "ValidateToken" for token1
        Event event = publishedEvents.get(customerToken).join();
//        assertEquals(string,event.getType());
        var st = event.getArgument(0, String.class);
        var correlationId = event.getArgument(1, CorrelationId.class);
        correlationIdsToken.put(st, correlationId);
    }

    @And("The {string} event is published to find the second customerID")
    public void theEventIsPublishedToFindTheSecondCustomerID(String arg0) {
        //check if service publishes "ValidateToken" for token2
        Event event = publishedEvents.get(customerToken2).join();
//        assertEquals(string,event.getType());
        var st = event.getArgument(0, String.class);
        var correlationId = event.getArgument(1, CorrelationId.class);
        correlationIdsToken.put(st, correlationId);
    }

    @When("The {string} events are published and returns the userids")
    public void theEventsArePublishedAndReturnsTheUserids(String arg0) {

        publishedEvents.put(payment.getMerchantID(), new CompletableFuture<Event>());
        publishedEvents.put(customerId, new CompletableFuture<Event>());
        publishedEvents.put(payment2.getMerchantID(), new CompletableFuture<Event>());
        publishedEvents.put(customerId2, new CompletableFuture<Event>());
        //token validated
        service.handleTokenValidated(new Event(arg0, new Object[]{customerId, correlationIdsToken.get(customerToken)}));
        service.handleTokenValidated(new Event(arg0, new Object[]{customerId2, correlationIdsToken.get(customerToken2)}));

    }

    @Then("{int} {string} events are published")
    public void eventsArePublished(int arg0, String arg1) {


        Event event = publishedEvents.get(payment.getMerchantID()).join();
        Event event2 = publishedEvents.get(customerId).join();
        Event event3 = publishedEvents.get(payment2.getMerchantID()).join();
        Event event4 = publishedEvents.get(customerId2).join();

        var st1 = event.getArgument(0, String.class);
        var correlationId1 = event.getArgument(1, CorrelationId.class);
        correlationIdsBankAcc.put(st1, correlationId1);

        var st2 = event2.getArgument(0, String.class);
        var correlationId2 = event2.getArgument(1, CorrelationId.class);
        correlationIdsBankAcc.put(st2, correlationId2);

        var st3 = event3.getArgument(0, String.class);
        var correlationId3 = event3.getArgument(1, CorrelationId.class);
        correlationIdsBankAcc.put(st3, correlationId3);

        var st4 = event4.getArgument(0, String.class);
        var correlationId4 = event4.getArgument(1, CorrelationId.class);
        correlationIdsBankAcc.put(st4, correlationId4);


    }

    @When("The {string} event is found and returns the bank accounts and payments are created")
    public void theEventIsFoundAndReturnsTheBankAccountsAndPaymentsAreCreated(String arg0) {
        //order may differ
        service.handleBankAccReturned(new Event(arg0, new Object[]{merchantBankID, correlationIdsBankAcc.get(payment.getMerchantID())}));
        service.handleBankAccReturned(new Event(arg0, new Object[]{customerBankID, correlationIdsBankAcc.get(customerId)}));
        service.handleBankAccReturned(new Event(arg0, new Object[]{merchantBankID, correlationIdsBankAcc.get(payment2.getMerchantID())}));
        service.handleBankAccReturned(new Event(arg0, new Object[]{customerBankID2, correlationIdsBankAcc.get(customerId2)}));
    }

    @Then("The first payment success event is pushed with the same correlation id as request")
    public void theFirstPaymentSuccessEventIsPushedWithTheSameCorrelationIdAsRequest() {
        //check for event "PaymentCompleted"
        var result = publishedEvents.get("PaymentDesc1").join();
       assertEquals(result.getArgument(1, CorrelationId.class), correlationPaymentReq);
        assertEquals(result.getArgument(0, String.class), "PaymentDesc1");

    }

    @And("The Second payment success event is pushed with the same correlation id as request")
    public void theSecondPaymentSuccessEventIsPushedWithTheSameCorrelationIdAsRequest() {
        //check for "PaymentCompleted"
        var result = publishedEvents.get("PaymentDesc1").join();
        //assertEquals(result.getArgument(1, CorrelationId.class), correlationPaymentReq2);
        assertEquals(result.getArgument(0, String.class), "PaymentDesc1");
    }

    //  Scenario: Payment Request Successfully
    //    Given A "RequestPayment" event is published with token "token" and amount 250
    //    Then The "ValidateToken" event is published to find the customerID
    //    When The "TokenValidated" event is published and returns the userid "userId"
    //    Then The "RequestBankAccId" event is published
    //    When The verification is successfull and returns "customerBankId1" and payment is created
    //    Then The payment success event is pushed with the same correlation id as request

    @Then("The {string} event is published")
    public void theEventIsPublished(String arg0) {
        //event expected
        var event = new Event(arg0, new Object[]{customerId});
        verify(q).publish(event);
    }

    @When("The verification is successfull and returns {string} and payment is created")
    public void theVerificationIsSuccessfullAndReturnsAndPaymentIsCreated(String arg0) {
        service.handleBankAccReturned(new Event("BankAccId", new Object[]{customerBankID}));
    }

    @Then("The payment success event is pushed")
    public void thePaymentSuccessEventIsPushed() {
        //expected event from the service
        String expected = "PaymentGood";
        var event = new Event("PaymentCompleted", new Object[]{expected});
        verify(q).publish(event);
    }



    @Then("The payment success event is pushed with the same correlation id as request")
    public void thePaymentSuccessEventIsPushedWithTheSameCorrelationIdAsRequest() {
        //expected event from the service
        String expected = "PaymentGood";
        var event = new Event("PaymentCompleted", new Object[]{expected, correlationPaymentReq});
        verify(q).publish(event);
    }



    public void theStudentIsBeingRegistered() {
        //the payment is being whatever...... result can be complete once we find this event
        // We have to run the registration in a thread, because
        // the register method will only finish after the next @When
        // step is executed.

    }




    @After
    public void Clean() throws BankServiceException_Exception {
        //It is probably not necessary to check the balance since the PaymentComplete event was published
        System.out.println(bank.getAccount(customerBankID).getBalance());
        System.out.println(bank.getAccount(merchantBankID).getBalance());
        System.out.println(bank.getAccount(customerBankID2).getBalance());

        bank.retireAccount(customerBankID);
        bank.retireAccount(customerBankID2);
        bank.retireAccount(merchantBankID);
    }

}

