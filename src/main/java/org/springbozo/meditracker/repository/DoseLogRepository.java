package org.springbozo.meditracker.repository;

import org.springbozo.meditracker.model.DoseLog;
import org.springbozo.meditracker.model.enums.DoseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DoseLogRepository extends JpaRepository<DoseLog, Integer> {

    @Query("select d from DoseLog d where d.prescription.id = :prescriptionId and d.scheduledAt = :scheduledAt")
    Optional<DoseLog> findOne(@Param("prescriptionId") int prescriptionId, @Param("scheduledAt") Instant scheduledAt);

    @Query("select d from DoseLog d where d.prescription.id in :ids and d.scheduledAt >= :from and d.scheduledAt < :to")
    List<DoseLog> findForPrescriptions(@Param("ids") Collection<Integer> prescriptionIds,
                                       @Param("from") Instant from, @Param("to") Instant to);

    // reminded, still not logged, and no follow-up yet
    @Query("""
            select d from DoseLog d join fetch d.prescription p join fetch p.medicine
            join fetch p.patient pt join fetch pt.user
            where d.status = :status and p.active = true and d.notifiedAt is not null and d.followedUpAt is null
              and d.scheduledAt <= :dueBefore and d.scheduledAt > :notOlderThan
            """)
    List<DoseLog> findNeedingFollowUp(@Param("status") DoseStatus status,
                                      @Param("dueBefore") Instant dueBefore, @Param("notOlderThan") Instant notOlderThan);
}
