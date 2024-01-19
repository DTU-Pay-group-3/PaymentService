package behaviourtests;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import models.DTUPayAccount;
import models.Payment;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/*Authors : Marian s233481 and Sandra s233484 */
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

        customer.setFirstName("VeryCoolFName1");
        customer.setLastName("VeryCoolLName1");
        customer.setCprNumber("10101101101011");

        customer2.setFirstName("VeryCoolFName21");
        customer2.setLastName("VeryCoolLName21");
        customer2.setCprNumber("010001011100111");


        merchant.setFirstName("VeryCoolFName31");
        merchant.setLastName("VeryCoolLName31");
        merchant.setCprNumber("0010111010110111");

        try {
            customerBankID = bank.createAccountWithBalance(customer, BigDecimal.valueOf(500));
            customerBankID2 = bank.createAccountWithBalance(customer2, BigDecimal.valueOf(1000));
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
        payment = new Payment(UUID.randomUUID().toString(),merchantID, customerToken, "PaymentDesc1", BigDecimal.valueOf(arg2));
        correlationPaymentReq = CorrelationId.randomId();

        publishedEvents.put(payment.getPaymentId(), new CompletableFuture<>());
        publishedEvents.put(customerToken, new CompletableFuture<>());
        publishedEvents.put(payment.getMerchantID(), new CompletableFuture<>());
        publishedEvents.put(customerId, new CompletableFuture<>());

        new Thread(() -> {
            service.MakePayment(new Event(arg0, new Object[]{payment, correlationPaymentReq}));
        }).start();
    }

    @And("A second {string} event is published with token {string} and amount {int}")
    public void aSecondEventIsPublishedWithTokenAndAmount(String arg0, String arg1, int arg2) {
        customerId2 = "custid2";
        customerToken2 = arg1;
        payment2 = new Payment(UUID.randomUUID().toString(),"merchid2", customerToken2, "PaymentDesc2", BigDecimal.valueOf(arg2));

        correlationPaymentReq2 = CorrelationId.randomId();
        publishedEvents.put(customerToken2, new CompletableFuture<>());
        publishedEvents.put(payment2.getPaymentId(), new CompletableFuture<>());
        publishedEvents.put(payment2.getMerchantID(), new CompletableFuture<>());
        publishedEvents.put(customerId2, new CompletableFuture<>());

        new Thread(() -> {
            service.MakePayment(new Event(arg0, new Object[]{payment2, correlationPaymentReq2}));
        }).start();
    }

    @Then("The {string} event is published to find the first customerID")
    public void theEventIsPublishedToFindTheFirstCustomerID(String arg0) {
        //check if service publishes "ValidateToken" for token1
        Event event = publishedEvents.get(customerToken).join();
        var st = event.getArgument(0, String.class);
        var correlationId = event.getArgument(1, CorrelationId.class);
        correlationIdsToken.put(st, correlationId);
    }

    @And("The {string} event is published to find the second customerID")
    public void theEventIsPublishedToFindTheSecondCustomerID(String arg0) {
        //check if service publishes "ValidateToken" for token2
        Event event = publishedEvents.get(customerToken2).join();
        var st = event.getArgument(0, String.class);
        var correlationId = event.getArgument(1, CorrelationId.class);
        correlationIdsToken.put(st, correlationId);
    }

    @When("The {string} events are published and returns the userids")
    public void theEventsArePublishedAndReturnsTheUserids(String arg0) {

        //token validated
        service.handleTokenValidated(new Event(arg0, new Object[]{customerId, correlationIdsToken.get(customerToken)}));
        service.handleTokenValidated(new Event(arg0, new Object[]{customerId2, correlationIdsToken.get(customerToken2)}));

    }

    @Then("The first payment success event is pushed with the same correlation id as request")
    public void theFirstPaymentSuccessEventIsPushedWithTheSameCorrelationIdAsRequest() {
        //check for event "PaymentCompleted"
        var result1 = publishedEvents.get(payment.getPaymentId()).join();

        assertEquals(result1.getType(),"PaymentCompleted");
        assertEquals(result1.getArgument(1, CorrelationId.class), correlationPaymentReq);
        assertEquals(result1.getArgument(0, String.class), payment.getPaymentId());

    }

    @And("The Second payment success event is pushed with the same correlation id as request")
    public void theSecondPaymentSuccessEventIsPushedWithTheSameCorrelationIdAsRequest() {
        //check for "PaymentCompleted"
        var result = publishedEvents.get(payment2.getPaymentId()).join();

        assertEquals(result.getType(),"PaymentCompleted");
        assertEquals(result.getArgument(1, CorrelationId.class), correlationPaymentReq2);
        assertEquals(result.getArgument(0, String.class), payment2.getPaymentId());
    }


    @After
    public void Clean() throws BankServiceException_Exception {
        //It is probably not necessary to check the balance since the PaymentComplete event was published
        System.out.println(bank.getAccount(customerBankID).getBalance()+" Cust1 balance");
        System.out.println(bank.getAccount(customerBankID2).getBalance()+" Cust2 balance");
        System.out.println(bank.getAccount(merchantBankID).getBalance() +" Merchant balance");

        bank.retireAccount(customerBankID);
        bank.retireAccount(customerBankID2);
        bank.retireAccount(merchantBankID);
    }

    @Then("The {string} event is awaited to find the first customerID")
    public void theEventIsAwaitedToFindTheFirstCustomerID(String arg0) {
        Event event = publishedEvents.get(customerToken).join();
        var st = event.getArgument(0, String.class);
        var correlationId = event.getArgument(1, CorrelationId.class);
        correlationIdsToken.put(st, correlationId);
    }

    @And("The {string} event is awaited to find the second customerID")
    public void theEventIsAwaitedToFindTheSecondCustomerID(String arg0) {

        Event event = publishedEvents.get(customerToken2).join();
        var st = event.getArgument(0, String.class);
        var correlationId = event.getArgument(1, CorrelationId.class);
        correlationIdsToken.put(st, correlationId);
    }

    @Then("The {string} event is published with the first customer id")
    public void theEventIsPublishedWithTheFirstCustomerId(String arg0) {
        service.handleTokenValidated(new Event(arg0, new Object[]{customerId, correlationIdsToken.get(customerToken)}));
    }

    @And("The {string} event is published with the second customer id")
    public void theEventIsPublishedWithTheSecondCustomerId(String arg0) {
        service.handleTokenValidated(new Event(arg0, new Object[]{customerId2, correlationIdsToken.get(customerToken2)}));
    }

    @Then("The {int} {string} events are published for the first payment")
    public void theEventsArePublishedForTheFirstPayment(int arg0, String arg1) {
        new Thread(() -> {
            Event event = publishedEvents.get(payment.getMerchantID()).join();
            var st1 = event.getArgument(0, String.class);
            var correlationId1 = event.getArgument(1, CorrelationId.class);
            correlationIdsBankAcc.put(st1, correlationId1);
            //await correlationIdsBankAcc.get(payment.getMerchantID())
            DTUPayAccount user= new DTUPayAccount("asd","asd","asd",merchantBankID);

            service.handleBankAccReturned(new Event("BankAccReturned", new Object[]{user, correlationIdsBankAcc.get(payment.getMerchantID())}));
        }).start();

        new Thread(() -> {
            Event event2 = publishedEvents.get(customerId).join();
            var st2 = event2.getArgument(0, String.class);
            var correlationId2 = event2.getArgument(1, CorrelationId.class);
            correlationIdsBankAcc.put(st2, correlationId2);
            //await response correlationIdsBankAcc.get(customerId)
            DTUPayAccount user= new DTUPayAccount("asd","asd","asd",customerBankID);

            service.handleBankAccReturned(new Event("BankAccReturned", new Object[]{user, correlationIdsBankAcc.get(customerId)}));
        }).start();

    }

    @Then("The {int} {string} events are published for the second payment")
    public void theEventsArePublishedForTheSecondPayment(int arg0, String arg1) {

        new Thread(() -> {
            Event event3 = publishedEvents.get(payment2.getMerchantID()).join();
            var st3 = event3.getArgument(0, String.class);
            var correlationId3 = event3.getArgument(1, CorrelationId.class);
            correlationIdsBankAcc.put(st3, correlationId3);
            DTUPayAccount user= new DTUPayAccount("asd","asd","asd",merchantBankID);

            //await response
            service.handleBankAccReturned(new Event("BankAccReturned", new Object[]{user, correlationIdsBankAcc.get(payment2.getMerchantID())}));

        }).start();

        new Thread(() -> {
            Event event4 = publishedEvents.get(customerId2).join();
            var st4 = event4.getArgument(0, String.class);
            var correlationId4 = event4.getArgument(1, CorrelationId.class);
            correlationIdsBankAcc.put(st4, correlationId4);
            DTUPayAccount user= new DTUPayAccount("asd","asd","asd",customerBankID2);

            //await response
            service.handleBankAccReturned(new Event("BankAccReturned", new Object[]{user, correlationIdsBankAcc.get(customerId2)}));
        }).start();
    }


}

