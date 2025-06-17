
package com.medco.HealthConnectProvider.services.impl.notification;

import com.medco.HealthConnectProvider.entity.claims.Claim;
import com.medco.HealthConnectProvider.entity.payment.PaymentTransaction;
import com.medco.HealthConnectProvider.entity.providers.Provider;
import com.medco.HealthConnectProvider.services.notification.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationServiceImpl implements NotificationService {

    @Autowired
    private JavaMailSender emailSender;

    @Override
    public void notifyClaimSubmitted(Claim claim) {
        sendEmail(claim.getProvider().getEmail(), "Claim Submitted",
                "Your claim " + claim.getClaimUuid() + " has been submitted successfully.");
    }

    @Override
    public void notifyPharmacy(Provider provider, String message) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(provider.getEmail());
        mailMessage.setSubject("Claim Update Notification");
        mailMessage.setText(message);
        emailSender.send(mailMessage);
    }

    @Override
    public void notifyClaimApproved(Claim claim) {
        sendEmail(claim.getProvider().getEmail(), "Claim Approved",
                "Your claim " + claim.getClaimUuid() + " has been approved.");
    }

    @Override
    public void notifyClaimRejected(Claim claim) {
        sendEmail(claim.getProvider().getEmail(), "Claim Rejected",
                "Your claim " + claim.getClaimUuid() + " has been rejected.");
    }

    @Override
    public void notifyPaymentInitiated(PaymentTransaction transaction) {
        Claim claim = transaction.getClaim();
        String providerEmail = claim.getProvider().getEmail();
        String subject = "Payment Initiated";
        String message = String.format(
                "Payment for claim %s has been initiated.\n" +
                        "Transaction Details:\n" +
                        "- Amount: %s %s\n" +
                        "- Payment Method: %s\n" +
                        "- Chapa Transaction ID: %s\n" +
                        "- Checkout URL: %s\n" +
                        "- Initiated At: %s",
                claim.getClaimUuid(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getPaymentMethod(),
                transaction.getChapaTransactionId(),
                transaction.getCheckoutUrl(),
                transaction.getInitiatedAt()
        );
        sendEmail(providerEmail, subject, message);
    }

    @Override
    public void notifyPaymentCompleted(PaymentTransaction transaction) {
        Claim claim = transaction.getClaim();
        String providerEmail = claim.getProvider().getEmail();
        String subject = "Payment Completed";
        String message = String.format(
                "Payment for claim %s has been completed.\n" +
                        "Transaction Details:\n" +
                        "- Amount: %s %s\n" +
                        "- Payment Method: %s\n" +
                        "- Chapa Transaction ID: %s\n" +
                        "- Completed At: %s\n" +
                        "- Payer Name: %s\n" +
                        "- Payer Email: %s\n" +
                        "- Payer Phone: %s",
                claim.getClaimUuid(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getPaymentMethod(),
                transaction.getChapaTransactionId(),
                transaction.getCompletedAt(),
                transaction.getPayerName(),
                transaction.getPayerEmail(),
                transaction.getPayerPhone()
        );
        sendEmail(providerEmail, subject, message);
    }

    @Override
    public void notifyPaymentFailed(PaymentTransaction transaction) {
        Claim claim = transaction.getClaim();
        String providerEmail = claim.getProvider().getEmail();
        String subject = "Payment Failed";
        String message = String.format(
                "Payment for claim %s has failed.\n" +
                        "Transaction Details:\n" +
                        "- Amount: %s %s\n" +
                        "- Payment Method: %s\n" +
                        "- Chapa Transaction ID: %s\n" +
                        "- Initiated At: %s\n" +
                        "- Status: %s\n" +
                        "- Additional Details: %s",
                claim.getClaimUuid(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getPaymentMethod(),
                transaction.getChapaTransactionId(),
                transaction.getInitiatedAt(),
                transaction.getStatus(),
                transaction.getTransactionDetails()
        );
        sendEmail(providerEmail, subject, message);
    }

    @Override
    public void notifyClaimReconciled(Claim claim) {
        sendEmail(claim.getProvider().getEmail(), "Claim Reconciled",
                "Your claim " + claim.getClaimUuid() + " has been reconciled.");
    }

    private void sendEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@healthconnect.com");
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        emailSender.send(message);
    }
}
