package main;

import dtu.ws.fastmoney.BankService;
import dtu.ws.fastmoney.BankServiceService;
import messaging.Event;
import messaging.MessageQueue;
import models.DTUPayAccount;
import models.Payment;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
/*Authors : Marian s233481 and Sandra s233484 */
public class PaymentService {
    MessageQueue queue;
    public Map<String, Payment> payments = new HashMap<>();
    private Map<CorrelationId, CompletableFuture<String>> correlations = new ConcurrentHashMap<>();
    BankService bank = new BankServiceService().getBankServicePort();

    /*Author Marian s233481*/
    public PaymentService(MessageQueue q) {
        this.queue = q;
        this.queue.addHandler("RequestPayment", this::MakePayment);
        this.queue.addHandler("ValidateTokenCompleted", this::handleTokenValidated);
        this.queue.addHandler("ValidateTokenFailed", this::handleTokenValidatedFailed);
        this.queue.addHandler("DTUPayAccountReturned", this::handleBankAccReturned);
        this.queue.addHandler("BankAccFailed", this::handleBankAccFailed);
    }

    /*Author Marian s233481*/
    public void MakePayment(Event ev) {

        // maybe make a new thread for each payment
        Payment payment = ev.getArgument(0, Payment.class);
        var paymentCorrelationId = ev.getArgument(1, CorrelationId.class);

        //call method to validate token
        String userID = ValidateToken(payment.getCustomerToken());

        //once you have token continue and send 2 events for bank accounts
        String userBankAccount = RequestBankAccount(userID);
        String merchantBankAccount = RequestBankAccount(payment.getMerchantID());


        try {
            bank.transferMoneyFromTo(userBankAccount, merchantBankAccount, payment.getAmount(), payment.getDescription());
            Event success = new Event("PaymentCompleted", new Object[]{payment.getPaymentId(), paymentCorrelationId});
            System.out.println("Publishing PaymentCompleted event " + success);
            // probably should not be stored here but in reports service
            payments.put(payment.getDescription(),payment);
            queue.publish(success);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            Event fail = new Event("PaymentFailed", new Object[]{payment.getPaymentId(), paymentCorrelationId});
            System.out.println("Publishing PaymentFailed event");
            queue.publish(fail);
        }

        // send log event
    }

    /*Author Sandra s233484 */
    public void handleTokenValidated(Event ev) {
        System.out.println("Event ValidateTokenCompleted found");
        var userID = ev.getArgument(0, String.class);
        var correlationid = ev.getArgument(1, CorrelationId.class);
        correlations.get(correlationid).complete(userID);
    }

    /*Author Marian s233481 */
    public void handleBankAccReturned(Event ev) {
        var dtuUser = ev.getArgument(0, DTUPayAccount.class);
        var correlationid = ev.getArgument(1, CorrelationId.class);
        correlations.get(correlationid).complete(dtuUser.getAccountNumber());
    }

    /*Author Sandra s233484 */
    public void handleBankAccFailed(Event ev) {
        var correlationid = ev.getArgument(1, CorrelationId.class);
        correlations.get(correlationid).cancel(true);
    }

    /*Author Sandra s233484 */
    public void handleTokenValidatedFailed(Event ev) {
        var correlationid = ev.getArgument(1, CorrelationId.class);
        correlations.get(correlationid).cancel(true);
    }

    /*Author Marian s233481 */
    public String ValidateToken(String userToken) {
        var id = CorrelationId.randomId();
        correlations.put(id, new CompletableFuture<>());
        Event event = new Event("ValidateToken", new Object[]{userToken, id});
        queue.publish(event);
        return correlations.get(id).join();
    }

    /*Author Marian s233481 */
    public String RequestBankAccount(String userID) {
        var correlationUserId = CorrelationId.randomId();
        correlations.put(correlationUserId, new CompletableFuture<>());
        Event event = new Event("GetDTUPayAccount", new Object[]{userID, correlationUserId});
        queue.publish(event);
        return correlations.get(correlationUserId).join();
    }

}
