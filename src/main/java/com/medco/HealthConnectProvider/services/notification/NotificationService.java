package com.medco.HealthConnectProvider.services.notification;

import com.medco.HealthConnectProvider.entity.claims.Claim;
import com.medco.HealthConnectProvider.entity.payment.PaymentTransaction;
import com.medco.HealthConnectProvider.entity.providers.Provider;

public interface NotificationService {

    void notifyClaimSubmitted(Claim claim);

    void notifyPharmacy(Provider provider, String s);

    void notifyClaimApproved(Claim claim);

    void notifyClaimRejected(Claim claim);

    void notifyPaymentInitiated(PaymentTransaction transaction);

    void notifyPaymentCompleted(PaymentTransaction transaction);

    void notifyPaymentFailed(PaymentTransaction transaction);

    void notifyClaimReconciled(Claim claim);
}
