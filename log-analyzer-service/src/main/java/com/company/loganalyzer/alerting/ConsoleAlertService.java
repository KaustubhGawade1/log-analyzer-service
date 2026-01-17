package com.company.loganalyzer.alerting;

import com.company.loganalyzer.model.IncidentEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ConsoleAlertService implements AlertService {

    private static final Logger log = LoggerFactory.getLogger(ConsoleAlertService.class);

    @Override
    public void sendAlert(IncidentEntity incident) {
        log.error("╔═══════════════════════════════════════════════════════════════╗");
        log.error("║                 [ALERT] NEW INCIDENT DETECTED                 ║");
        log.error("╠═══════════════════════════════════════════════════════════════╣");
        log.error("║ ID:          {}", incident.getId());
        log.error("║ Service:     {}", incident.getServiceName());
        log.error("║ Type:        {}", incident.getType());
        log.error("║ Description: {}", incident.getDescription());
        log.error("╚═══════════════════════════════════════════════════════════════╝");
    }
}
