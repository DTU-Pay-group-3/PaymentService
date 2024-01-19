package models;

import lombok.*;

import java.math.BigDecimal;
/*Author Marian s233481 */
@AllArgsConstructor
@Value
@Data
@Setter
@Getter
public class Payment {

    String paymentId;
    String merchantID;
    String customerToken;
    String description;
    BigDecimal amount;

}