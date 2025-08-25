package com.medco.HealthConnectProvider.controller.payment;

import com.medco.HealthConnectProvider.services.payment.PaymentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@RestController
@RequestMapping("api/premium/payment")
public class PaymentController {
    private final PaymentService paymentService;

    @Value("${chapa.SECRET_KEY}")
    private String SECRET_KEY;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

//    @PutMapping("/pay/{monthlyPaymentUuid}")
//    public String  pay(@PathVariable String monthlyPaymentUuid,
//                       @RequestParam PaymentType paymentType) throws JsonProcessingException, InterruptedException {
//
//        return paymentService.pay(monthlyPaymentUuid,paymentType);
//
//    }

//    @PutMapping("/transfer/{quotationUuid}")
//    public String  transfer(@PathVariable String quotationUuid) throws JsonProcessingException, InterruptedException {
//
//        return paymentService.transfer(quotationUuid);
//
//    }


//    @PutMapping("/disperse/{quotationUuid}")
//    public String  disperse(@PathVariable String quotationUuid) throws JsonProcessingException, InterruptedException {
//        System.out.println("quotationUuid"+quotationUuid);
//        return paymentService.disperse(quotationUuid);
//
//    }

    @PostMapping("/transactionWebhookResponse")
//    public ResponseEntity<?> transactionWebhookResponse( @RequestBody Map<String, Object> payload,
//                                                        @RequestHeader(value = "x-chapa-signature", required = false) String chapaSignature) {

    public ResponseEntity <String> transactionWebhookResponse( @RequestBody String payload,
                                                               @RequestHeader("Chapa-Signature") String chapaSignature) throws NoSuchAlgorithmException, InvalidKeyException {


        try{
            String hash = calculateHMAC(payload, SECRET_KEY);
            if (hash.equals(chapaSignature)) {

                paymentService.processWebhookResponse(payload);
                return ResponseEntity.ok().build();
            } else {

                return ResponseEntity.status(401).body("Invalid signature");

            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error processing webhook");
        }

    }


//    @GetMapping("/verifyPayment/{monthlyPaymentUuid}")
//    public String verifyPayment(@PathVariable String monthlyPaymentUuid
//    ) throws BadRequestException, IOException {
//        return paymentService.verifyPayment(monthlyPaymentUuid);
//    }

//    @GetMapping("/verifyTransfer/{quotationUuid}")
//    public ResponseEntity<?> verifyTransfer(@PathVariable String quotationUuid
//    ) throws BadRequestException, IOException {
//        return paymentService.verifyTransfer(quotationUuid);
//    }

    private String calculateHMAC(String data, String key) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256_HMAC.init(secretKeySpec);
        byte[] result = sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(result);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    @GetMapping("/getBanks")
    public ResponseEntity<?> getBanks() throws IOException {
        System.out.println( "in the controller");
        return paymentService.getBanks();
    }

}

