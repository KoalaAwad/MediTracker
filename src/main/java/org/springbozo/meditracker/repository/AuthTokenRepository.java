package org.springbozo.meditracker.repository;

import org.springbozo.meditracker.model.AuthToken;
import org.springbozo.meditracker.model.enums.AuthTokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface AuthTokenRepository extends JpaRepository<AuthToken, Integer> {
    Optional<AuthToken> findByTokenHashAndType(String tokenHash, AuthTokenType type);

    boolean existsByUserIdAndTypeAndCreatedAtAfter(int userId, AuthTokenType type, LocalDateTime after);

    // drop older unused tokens so only the newest emailed link works
    @Modifying
    @Query("delete from AuthToken t where t.user.Id = :userId and t.type = :type and t.usedAt is null")
    void deleteUnusedByUserIdAndType(@Param("userId") int userId, @Param("type") AuthTokenType type);
}
