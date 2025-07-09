package com.medco.HealthConnectProvider.repository.payment;

import com.medco.HealthConnectProvider.entity.payment.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByChapaTransactionId(String chapaTransactionId);

}
