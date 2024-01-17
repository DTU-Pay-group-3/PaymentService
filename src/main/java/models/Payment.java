package models;

import java.math.BigDecimal;

public class Payment {
    private String merchantID;
    private String customerToken;
    private String description;
    private BigDecimal amount;

    public Payment(String merchantID, String customerToken, String description, BigDecimal amount) {
        this.merchantID = merchantID;
        this.customerToken = customerToken;
        this.description = description;
        this.amount = amount;
    }

    public Payment() {

    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCustomerToken() {
        return customerToken;
    }

    public void setCustomerToken(String customerToken) {
        this.customerToken = customerToken;
    }

    public String getMerchantID() {
        return this.merchantID;
    }

    public void setMerchantID(String merchantBankID) {
        this.merchantID = merchantBankID;
    }

    public BigDecimal getAmount() {
        return this.amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}