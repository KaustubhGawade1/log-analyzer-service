package com.company.loganalyzer.flow.repository;

import com.company.loganalyzer.flow.model.ApiFlowGraph;
import com.company.loganalyzer.flow.model.FlowSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for flow snapshot persistence and queries.
 */
@Repository
public interface FlowSnapshotRepository extends JpaRepository<FlowSnapshotEntity, Long> {

    Optional<FlowSnapshotEntity> findByTraceId(String traceId);

    List<FlowSnapshotEntity> findByRootServiceOrderByCreatedAtDesc(String rootService);

    List<FlowSnapshotEntity> findByStatusOrderByCreatedAtDesc(ApiFlowGraph.FlowStatus status);

    @Query("SELECT f FROM FlowSnapshotEntity f WHERE f.createdAt >= :since ORDER BY f.createdAt DESC")
    List<FlowSnapshotEntity> findRecentFlows(@Param("since") Instant since);

    @Query("SELECT f FROM FlowSnapshotEntity f WHERE f.hasBottleneck = true ORDER BY f.createdAt DESC")
    List<FlowSnapshotEntity> findFlowsWithBottlenecks();

    @Query("SELECT f FROM FlowSnapshotEntity f WHERE f.rootService = :service AND f.createdAt >= :since ORDER BY f.createdAt DESC")
    List<FlowSnapshotEntity> findByServiceAndTimeRange(
            @Param("service") String service,
            @Param("since") Instant since);

    @Query("SELECT DISTINCT f.rootService FROM FlowSnapshotEntity f")
    List<String> findAllServices();

    @Query("SELECT f.status, COUNT(f) FROM FlowSnapshotEntity f WHERE f.createdAt >= :since GROUP BY f.status")
    List<Object[]> getStatusCounts(@Param("since") Instant since);

    @Query("SELECT COUNT(f) FROM FlowSnapshotEntity f WHERE f.createdAt >= :since")
    long countRecentFlows(@Param("since") Instant since);

    void deleteByCreatedAtBefore(Instant before);
}
