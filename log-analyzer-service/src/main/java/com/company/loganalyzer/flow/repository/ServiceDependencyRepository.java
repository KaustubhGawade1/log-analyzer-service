package com.company.loganalyzer.flow.repository;

import com.company.loganalyzer.flow.model.ServiceDependencyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for service dependency persistence and queries.
 */
@Repository
public interface ServiceDependencyRepository extends JpaRepository<ServiceDependencyEntity, Long> {

    Optional<ServiceDependencyEntity> findBySourceServiceAndTargetServiceAndEndpointPattern(
            String sourceService, String targetService, String endpointPattern);

    List<ServiceDependencyEntity> findBySourceService(String sourceService);

    List<ServiceDependencyEntity> findByTargetService(String targetService);

    @Query("SELECT DISTINCT d.sourceService FROM ServiceDependencyEntity d UNION SELECT DISTINCT d.targetService FROM ServiceDependencyEntity d")
    List<String> findAllServices();

    @Query("SELECT d FROM ServiceDependencyEntity d WHERE d.errorRate > :threshold ORDER BY d.errorRate DESC")
    List<ServiceDependencyEntity> findHighErrorRateDependencies(@Param("threshold") double threshold);

    @Query("SELECT d FROM ServiceDependencyEntity d WHERE d.avgLatencyMs > :threshold ORDER BY d.avgLatencyMs DESC")
    List<ServiceDependencyEntity> findHighLatencyDependencies(@Param("threshold") double threshold);

    @Query("SELECT d.sourceService, COUNT(d) FROM ServiceDependencyEntity d GROUP BY d.sourceService ORDER BY COUNT(d) DESC")
    List<Object[]> getServiceFanOutCounts();

    @Query("SELECT d.targetService, COUNT(d) FROM ServiceDependencyEntity d GROUP BY d.targetService ORDER BY COUNT(d) DESC")
    List<Object[]> getServiceFanInCounts();

    @Query("SELECT d FROM ServiceDependencyEntity d ORDER BY d.requestCount DESC")
    List<ServiceDependencyEntity> findMostUsedDependencies();
}
