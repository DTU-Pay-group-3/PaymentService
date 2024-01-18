package models;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.math.BigDecimal;

@AllArgsConstructor
@Value
public class Payment {

    String paymentId;
    String merchantID;
    String customerToken;
    String description;
    BigDecimal amount;

}