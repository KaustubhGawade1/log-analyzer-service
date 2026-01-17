package com.company.loganalyzer.repository;

import com.company.loganalyzer.model.LogDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LogRepository extends ElasticsearchRepository<LogDocument, String> {
    List<LogDocument> findByServiceName(String serviceName);

    List<LogDocument> findByClusterId(String clusterId);
}
