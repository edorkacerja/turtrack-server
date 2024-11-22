package com.turtrack.server.repository.turtrack;

import com.turtrack.server.model.turtrack.TurtrackSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<TurtrackSubscription, Long> {
    Optional<TurtrackSubscription> findBySubscriptionId(String subscriptionId);
    Optional<TurtrackSubscription> findByCustomerId(String customerId);
}
