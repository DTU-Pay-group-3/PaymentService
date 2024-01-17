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
    private Map<CorrelationId, CompletableFuture<String>> correlationsToken = new ConcurrentHashMap<>();

    //different idea
    //private Map<CorrelationId, Map<CorrelationId,CompletableFuture<String>>> correlationsBankAccID = new ConcurrentHashMap<>();
    //private Map<CorrelationId, CompletableFuture<Map<CorrelationId,CompletableFuture<String>>>> correlationsBankAccID2 = new ConcurrentHashMap<>();

    //maybe no need to correlate with payment corrid
    private Map<CorrelationId, CompletableFuture<String>> correlationsBankAccounts = new ConcurrentHashMap<>();

    BankService bank = new BankServiceService().getBankServicePort();
    Payment payment;

    public PaymentService(MessageQueue q) {
        this.queue = q;
        this.queue.addHandler("RequestPayment", this::makePaymentCorid);
        this.queue.addHandler("ValidateTokenCompleted", this::handleTokenValidated);
        //this.queue.addHandler("ValidateTokenFailed", this::handleTokenValidatedFailed);
        this.queue.addHandler("BankAccReturned", this::handleBankAccReturned);
        this.queue.addHandler("BankAccFailed", this::handleBankAccFailed);
//        this.queue.addHandler("BankAccId", this::handleBankAccId);

    }

    public void makePaymentCorid(Event ev) {

            // maybe make a new thread for each payment
            Payment payment = ev.getArgument(0, Payment.class);
            var paymentCorrelationId = ev.getArgument(1, CorrelationId.class);
            correlationsToken.put(paymentCorrelationId, new CompletableFuture<>());
            Event event = new Event("ValidateToken", new Object[]{payment.getCustomerToken(), paymentCorrelationId});
            queue.publish(event);

            var correlationIdMerchant = CorrelationId.randomId();

            var correlationIdCustomer = CorrelationId.randomId();
//        Map<CorrelationId,CompletableFuture<String>> asf = new ConcurrentHashMap<>();
//        asf.put(correlationIdMerchant,new CompletableFuture<>());
//        asf.put(correlationIdCustomer,new CompletableFuture<>());
            correlationsBankAccounts.put(correlationIdMerchant, new CompletableFuture<>());
            correlationsBankAccounts.put(correlationIdCustomer, new CompletableFuture<>());
            String what = correlationsToken.get(paymentCorrelationId).join();
            Event event3 = new Event("RequestBankAcc", new Object[]{what, correlationIdCustomer});
            Event event2 = new Event("RequestBankAcc", new Object[]{payment.getMerchantID(), correlationIdMerchant});

            System.out.println("Publishing RequestBankAcc event with " + what);
            System.out.println("Publishing RequestBankAcc event with " + payment.getMerchantID());
            queue.publish(event2);
            queue.publish(event3);

        new Thread(() -> {

            try {
                String from = correlationsBankAccounts.get(correlationIdCustomer).join();
                String to = correlationsBankAccounts.get(correlationIdMerchant).join();
                bank.transferMoneyFromTo(from, to, payment.getAmount(), payment.getDescription());
                Event success = new Event("PaymentCompleted", new Object[]{payment.getDescription(), paymentCorrelationId});
                System.out.println("Publishing PaymentCompleted event "+ success);
                // probably should not be stored here but in reports service
                //payments.put(customerBankAccId,payment);
                queue.publish(success);
            } catch (Exception e) {
                System.out.println(e.getMessage());
                Event fail = new Event("PaymentFailed", new Object[]{payment.getDescription(), paymentCorrelationId});
                System.out.println("Publishing PaymentFailed event");
                queue.publish(fail);
            }

        }).start();
        // bank.transferMoneyFromTo(correlationsBankAccID.get(paymentCorrelationId).get(correlationIdCustomer).join(),correlationsBankAccID.get(paymentCorrelationId).get(correlationIdMerchant).join(),payment.getAmount(),payment.getDescription());

        // payment 1 - user token 1 - bank acc - bank acc
        //call method to validate token
        // Event "ValidateToken" token,corrid wait for "ValidateTokenCompleted" userID,corrid
        //while token.iscomplete is false wait
        //once you have token continue and send 2 events for bank accounts
        // 2x Event "RequestBankAcc" userId,corrId2 and wait for "BankAccReturned" userBankAcc,corrid2
        // 2 events with same corrid2 - could be payment corrid?
        // while both bank acc is complete fales wait
        // call bank.pay
        // send log event
    }

    public void makePayment(Event ev) {
        System.out.println("Event RequestPayment found");
        payment = ev.getArgument(0, Payment.class);
        CorrelationId correlationId = ev.getArgument(1, CorrelationId.class);
        Event event = new Event("ValidateToken", new Object[]{payment.getCustomerToken(), correlationId});
        System.out.println("Publishing ValidateToken event");
        queue.publish(event);
    }

    public void handleTokenValidated(Event ev) {
        System.out.println("Event ValidateTokenCompleted found");
        var userID = ev.getArgument(0, String.class);
        var correlationid = ev.getArgument(1, CorrelationId.class);
        System.out.println("completing " + userID);
        correlationsToken.get(correlationid).complete(userID);
//        System.out.println("Event TokenValidated found");
//        String customerId = ev.getArgument(0, String.class);
//        Event event = new Event("RequestBankAccId", new Object[]{customerId});
//        System.out.println("Publishing RequestBankAccId event");
//        queue.publish(event);
    }

//    public void handleTokenValidatedFailed(Event ev) {
//        System.out.println("Event TokenValidated found");
//        String customerId = ev.getArgument(0, String.class);
//        Event event = new Event("RequestBankAccId", new Object[]{customerId});
//        System.out.println("Publishing RequestBankAccId event");
//        queue.publish(event);
//    }

//    public void handleBankAccId(Event ev) {
//        System.out.println("Event BankAccId found");
//        String customerBankAccId = ev.getArgument(0, String.class);
//        try {
//            bank.transferMoneyFromTo(customerBankAccId, payment.getMerchantID(), payment.getAmount(), payment.getDescription());
//            Event success = new Event("PaymentCompleted", new Object[]{"PaymentGood"});
//            System.out.println("Publishing PaymentCompleted event");
//            // probably should not be stored here but in reports service
//            payments.put(customerBankAccId,payment);
//            queue.publish(success);
//        } catch (Exception e) {
//            System.out.println(e.getMessage());
//            Event fail = new Event("PaymentFailed", new Object[]{});
//            System.out.println("Publishing PaymentFailed event");
//            queue.publish(fail);
//        }
//    }

    public void handleBankAccReturned(Event ev) {
        var bankAcc = ev.getArgument(0, String.class);
        System.out.println("Event BankAccReturned found with id " + bankAcc);
        var correlationid = ev.getArgument(1, CorrelationId.class);
        correlationsBankAccounts.get(correlationid).complete(bankAcc);
    }

    public void handleBankAccFailed(Event ev) {
        System.out.println("Event BankAccFailed found");
        var correlationid = ev.getArgument(1, CorrelationId.class);
        correlationsBankAccounts.get(correlationid).cancel(true);
    }
}
