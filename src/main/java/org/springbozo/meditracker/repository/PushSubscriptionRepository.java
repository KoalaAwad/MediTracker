package org.springbozo.meditracker.repository;

import org.springbozo.meditracker.model.PushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Integer> {

    @Query("select s from PushSubscription s where s.user.Id = :userId")
    List<PushSubscription> findByUser(@Param("userId") int userId);

    Optional<PushSubscription> findByEndpoint(String endpoint);

    @Modifying
    @Transactional
    @Query("delete from PushSubscription s where s.endpoint = :endpoint and s.user.Id = :userId")
    int deleteByEndpointAndUser(@Param("endpoint") String endpoint, @Param("userId") int userId);
}
