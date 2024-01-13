package main;

import dtu.ws.fastmoney.BankService;
import dtu.ws.fastmoney.BankServiceService;
import messaging.Event;
import messaging.MessageQueue;
import models.Merchant;
import models.Payment;

import java.util.HashMap;
import java.util.Map;

public class PaymentService {

    MessageQueue queue;
    public Map<String, Payment> payments = new HashMap<>();

    BankService bank = new BankServiceService().getBankServicePort();
    Payment payment;

    public PaymentService(MessageQueue q) {
        this.queue = q;
        this.queue.addHandler("RequestPayment", this::makePayment);
        this.queue.addHandler("TokenValidated", this::handleTokenValidated);
        this.queue.addHandler("BankAccId", this::handleBankAccId);

    }

    public void makePayment(Event ev) {
        System.out.println("Event RequestPayment found");
        payment = ev.getArgument(0, Payment.class);
        Event event = new Event("ValidateToken", new Object[]{payment.getCustomerToken()});
        System.out.println("Publishing ValidateToken event");
        queue.publish(event);
    }

    public void handleTokenValidated(Event ev) {
        System.out.println("Event TokenValidated found");
        String customerId = ev.getArgument(0, String.class);
        Event event = new Event("RequestBankAccId", new Object[]{customerId});
        System.out.println("Publishing RequestBankAccId event");
        queue.publish(event);
    }

    public void handleBankAccId(Event ev) {
        System.out.println("Event BankAccId found");
        String customerBankAccId = ev.getArgument(0, String.class);
        try {
            bank.transferMoneyFromTo(customerBankAccId, payment.getMerchantID(), payment.getAmount(), payment.getDescription());
            Event success = new Event("PaymentCompleted", new Object[]{"PaymentGood"});
            System.out.println("Publishing PaymentCompleted event");
            // probably should not be stored here but in reports service
            payments.put(customerBankAccId,payment);
            queue.publish(success);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            Event fail = new Event("PaymentFailed", new Object[]{});
            System.out.println("Publishing PaymentFailed event");
            queue.publish(fail);
        }
    }
}
