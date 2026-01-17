package com.company.loganalyzer.repository;

import com.company.loganalyzer.model.IncidentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentRepository extends JpaRepository<IncidentEntity, Long> {
    List<IncidentEntity> findByServiceNameAndStatus(String serviceName, IncidentEntity.IncidentStatus status);

    Optional<IncidentEntity> findFirstByServiceNameAndStatusOrderByStartTimeDesc(String serviceName,
            IncidentEntity.IncidentStatus status);
}
