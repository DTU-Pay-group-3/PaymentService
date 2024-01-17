package main;

import dtu.ws.fastmoney.BankService;
import dtu.ws.fastmoney.BankServiceService;
import io.cucumber.java.it.Ma;
import messaging.Event;
import messaging.MessageQueue;
import models.Merchant;
import models.Payment;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class PaymentService {
    MessageQueue queue;
    public Map<String, Payment> payments = new HashMap<>();
    private Map<CorrelationId, CompletableFuture<String>> correlations = new ConcurrentHashMap<>();
    BankService bank = new BankServiceService().getBankServicePort();

    public PaymentService(MessageQueue q) {
        this.queue = q;
        this.queue.addHandler("RequestPayment", this::makePaymentCorid);
        this.queue.addHandler("ValidateTokenCompleted", this::handleTokenValidated);
        this.queue.addHandler("ValidateTokenFailed", this::handleTokenValidatedFailed);
        this.queue.addHandler("BankAccReturned", this::handleBankAccReturned);
        this.queue.addHandler("BankAccFailed", this::handleBankAccFailed);
    }

    public void makePaymentCorid(Event ev) {

        // maybe make a new thread for each payment
        Payment payment = ev.getArgument(0, Payment.class);
        var paymentCorrelationId = ev.getArgument(1, CorrelationId.class);

        //call method to validate token
        String userID = ValidateToken(payment.getCustomerToken());

        //once you have token continue and send 2 events for bank accounts
        String merchantBankAccount = RequestBankAccount(payment.getMerchantID());
        String userBankAccount = RequestBankAccount(userID);

        try {
            bank.transferMoneyFromTo(userBankAccount, merchantBankAccount, payment.getAmount(), payment.getDescription());
            Event success = new Event("PaymentCompleted", new Object[]{payment.getDescription(), paymentCorrelationId});
            System.out.println("Publishing PaymentCompleted event " + success);
            // probably should not be stored here but in reports service
            payments.put(payment.getDescription(),payment);
            queue.publish(success);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            Event fail = new Event("PaymentFailed", new Object[]{payment.getDescription(), paymentCorrelationId});
            System.out.println("Publishing PaymentFailed event");
            queue.publish(fail);
        }

        // send log event
    }

    public void handleTokenValidated(Event ev) {
        System.out.println("Event ValidateTokenCompleted found");
        var userID = ev.getArgument(0, String.class);
        var correlationid = ev.getArgument(1, CorrelationId.class);
        correlations.get(correlationid).complete(userID);
    }

    public void handleBankAccReturned(Event ev) {
        var bankAcc = ev.getArgument(0, String.class);
        System.out.println("Event BankAccReturned found with id " + bankAcc);
        var correlationid = ev.getArgument(1, CorrelationId.class);
        correlations.get(correlationid).complete(bankAcc);
    }

    public void handleBankAccFailed(Event ev) {
        System.out.println("Event BankAccFailed found");
        var correlationid = ev.getArgument(1, CorrelationId.class);
        correlations.get(correlationid).cancel(true);
    }

    public void handleTokenValidatedFailed(Event ev) {
        var correlationid = ev.getArgument(1, CorrelationId.class);
        correlations.get(correlationid).cancel(true);
    }

    public String ValidateToken(String userToken) {
        var id = CorrelationId.randomId();
        correlations.put(id, new CompletableFuture<>());
        Event event = new Event("ValidateToken", new Object[]{userToken, id});
        queue.publish(event);
        return correlations.get(id).join();
    }

    public String RequestBankAccount(String userID) {
        var correlationUserId = CorrelationId.randomId();
        correlations.put(correlationUserId, new CompletableFuture<>());
        Event event = new Event("RequestBankAcc", new Object[]{userID, correlationUserId});
        queue.publish(event);
        return correlations.get(correlationUserId).join();
    }

}
