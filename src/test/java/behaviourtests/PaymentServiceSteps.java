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

public class PaymentServiceSteps {

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

    private CorrelationId correlationPaymentReq;
    private CorrelationId correlationPaymentReq2;
    private Map<String, CorrelationId> correlationIdsToken = new HashMap<>();
    private Map<String, CorrelationId> correlationIdsBankAcc = new HashMap<>();


    @Before
    public void SetupAccounts() {

        customer.setFirstName("HelloWorld12");
        customer.setLastName("HelloWorldLast12");
        customer.setCprNumber("15553423212");

        customer2.setFirstName("WorldHello12");
        customer2.setLastName("WorldHelloLast12");
        customer2.setCprNumber("5634523523412");


        merchant.setFirstName("MerchantHello12");
        merchant.setLastName("MerchantHelloLast12");
        merchant.setCprNumber("46345243212");

        try {
            customerBankID = bank.createAccountWithBalance(customer, BigDecimal.valueOf(500));
            customerBankID2 = bank.createAccountWithBalance(customer2, BigDecimal.valueOf(850));
            merchantBankID = bank.createAccountWithBalance(merchant, BigDecimal.valueOf(500));
        } catch (Exception e) {
            System.out.println("USER EXIST");
        }

        System.out.println(customerBankID + " account created");
        System.out.println(customerBankID2 + " account created");
        System.out.println(merchantBankID + " account created");

    }

    // I believe that this is supposed to test if all events are published correctly with the correct info
    // and in the end if bank.MakePayment doesn't give an error or bad status code the test should pass
    @Given("A {string} event is published with token {string} and amount {int}")
    public void aEventIsPublishedWithTokenAndAmount(String arg0, String arg1, int arg2) {
        merchantID = "merchid1";
        customerId = "custid1";
        customerToken = arg1;
        payment = new Payment(merchantID, customerToken, "PaymentDesc1", BigDecimal.valueOf(arg2));

        publishedEvents.put(customerToken, new CompletableFuture<>());
        publishedEvents.put(payment.getDescription(), new CompletableFuture<>());


        correlationPaymentReq = CorrelationId.randomId();

        System.out.println("CORDID " + correlationPaymentReq);
        new Thread(() -> {
            service.makePaymentCorid(new Event(arg0, new Object[]{payment, correlationPaymentReq}));
        }).start();
    }

    @And("A second {string} event is published with token {string} and amount {int}")
    public void aSecondEventIsPublishedWithTokenAndAmount(String arg0, String arg1, int arg2) {
        customerId2 = "custid2";
        customerToken2 = arg1;
        payment2 = new Payment("merchid2", customerToken2, "PaymentDesc2", BigDecimal.valueOf(arg2));
        publishedEvents.put(customerToken2, new CompletableFuture<>());
        publishedEvents.put(payment2.getDescription(), new CompletableFuture<>());


        correlationPaymentReq2 = CorrelationId.randomId();
        System.out.println("CORDID2 " + correlationPaymentReq2);
        new Thread(() -> {
            service.makePaymentCorid(new Event(arg0, new Object[]{payment2, correlationPaymentReq2}));
        }).start();

    }

    @Then("The {string} event is published to find the first customerID")
    public void theEventIsPublishedToFindTheFirstCustomerID(String arg0) {
        //check if service publishes "ValidateToken" for token1
        Event event = publishedEvents.get(customerToken).join();
//        assertEquals(string,event.getType());
        var st = event.getArgument(0, String.class);
        var correlationId = event.getArgument(1, CorrelationId.class);
//        System.out.println(st);
//        System.out.println(correlationId);
        correlationIdsToken.put(st, correlationId);
    }

    @And("The {string} event is published to find the second customerID")
    public void theEventIsPublishedToFindTheSecondCustomerID(String arg0) {
        //check if service publishes "ValidateToken" for token2
        Event event = publishedEvents.get(customerToken2).join();
//        assertEquals(string,event.getType());
        var st = event.getArgument(0, String.class);
        var correlationId = event.getArgument(1, CorrelationId.class);
//        System.out.println(st);
//        System.out.println(correlationId);
        correlationIdsToken.put(st, correlationId);
    }

    @When("The {string} events are published and returns the userids")
    public void theEventsArePublishedAndReturnsTheUserids(String arg0) {

        publishedEvents.put(payment.getMerchantID(), new CompletableFuture<>());
        publishedEvents.put(customerId, new CompletableFuture<>());
        publishedEvents.put(payment2.getMerchantID(), new CompletableFuture<>());
        publishedEvents.put(customerId2, new CompletableFuture<>());
        //token validated
        service.handleTokenValidated(new Event(arg0, new Object[]{customerId, correlationIdsToken.get(customerToken)}));
        service.handleTokenValidated(new Event(arg0, new Object[]{customerId2, correlationIdsToken.get(customerToken2)}));

    }

    @Then("{int} {string} events are published")
    public void eventsArePublished(int arg0, String arg1) {
        new Thread(() -> {
            Event event = publishedEvents.get(payment.getMerchantID()).join();
            var st1 = event.getArgument(0, String.class);
            var correlationId1 = event.getArgument(1, CorrelationId.class);
            correlationIdsBankAcc.put(st1, correlationId1);
            service.handleBankAccReturned(new Event("BankAccReturned", new Object[]{merchantBankID, correlationIdsBankAcc.get(payment.getMerchantID())}));
        }).start();

        new Thread(() -> {
            Event event2 = publishedEvents.get(customerId).join();
            var st2 = event2.getArgument(0, String.class);
            var correlationId2 = event2.getArgument(1, CorrelationId.class);
            correlationIdsBankAcc.put(st2, correlationId2);
            service.handleBankAccReturned(new Event("BankAccReturned", new Object[]{customerBankID, correlationIdsBankAcc.get(customerId)}));
        }).start();

        new Thread(() -> {
            Event event3 = publishedEvents.get(payment2.getMerchantID()).join();
            var st3 = event3.getArgument(0, String.class);
            var correlationId3 = event3.getArgument(1, CorrelationId.class);
            correlationIdsBankAcc.put(st3, correlationId3);
            service.handleBankAccReturned(new Event("BankAccReturned", new Object[]{merchantBankID, correlationIdsBankAcc.get(payment2.getMerchantID())}));

        }).start();

        new Thread(() -> {
            Event event4 = publishedEvents.get(customerId2).join();
            var st4 = event4.getArgument(0, String.class);
            var correlationId4 = event4.getArgument(1, CorrelationId.class);
            correlationIdsBankAcc.put(st4, correlationId4);
            service.handleBankAccReturned(new Event("BankAccReturned", new Object[]{customerBankID2, correlationIdsBankAcc.get(customerId2)}));
        }).start();

    }

    @When("The {string} event is found and returns the bank accounts and payments are created")
    public void theEventIsFoundAndReturnsTheBankAccountsAndPaymentsAreCreated(String arg0) {

    }

    @Then("The first payment success event is pushed with the same correlation id as request")
    public void theFirstPaymentSuccessEventIsPushedWithTheSameCorrelationIdAsRequest() {
        //check for event "PaymentCompleted"
        var result1 = publishedEvents.get("PaymentDesc1").join();

        System.out.println("CORDID111 " + correlationPaymentReq);
        assertEquals(result1.getArgument(1, CorrelationId.class), correlationPaymentReq);
        assertEquals(result1.getArgument(0, String.class), "PaymentDesc1");

    }

    @And("The Second payment success event is pushed with the same correlation id as request")
    public void theSecondPaymentSuccessEventIsPushedWithTheSameCorrelationIdAsRequest() {
        //check for "PaymentCompleted"
        var result = publishedEvents.get("PaymentDesc2").join();

        System.out.println("CORDID2222 " + correlationPaymentReq2);
        assertEquals(result.getArgument(1, CorrelationId.class), correlationPaymentReq2);
        assertEquals(result.getArgument(0, String.class), "PaymentDesc2");
    }

//    Scenario: Payment Request Single -- needs refactoring of steps
//    Given A "RequestPayment" event is published with token "token" and amount 200
//    And The payment is being processed
//    And The "ValidateToken" event is published to find the first customerID
//    When The "ValidateTokenCompleted" event are published and returns the userid
//    Then The "RequestBankAccId" events are published
//    When The "BankAccReturned" event is found and returns the bank accounts and payments are created
//    Then The first payment success event is pushed with the same correlation id as request


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

