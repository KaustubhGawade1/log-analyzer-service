package com.company.loganalyzer.alerting;

import com.company.loganalyzer.model.IncidentEntity;

public interface AlertService {
    void sendAlert(IncidentEntity incident);
}
