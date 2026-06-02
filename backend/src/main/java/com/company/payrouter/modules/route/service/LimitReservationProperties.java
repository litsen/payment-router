package com.company.payrouter.modules.route.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "pay-router.limit-reservation")
public class LimitReservationProperties {
    private boolean enabled = false;
    private int reservationCheckDelaySeconds = 300;
    private int compensationIntervalSeconds = 60;
    private int compensationBatchSize = 100;
}
