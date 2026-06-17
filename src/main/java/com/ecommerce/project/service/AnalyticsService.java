package com.ecommerce.project.service;

import com.ecommerce.project.payload.AnalyticsResponse;
import org.springframework.stereotype.Service;

@Service
public interface AnalyticsService {
    AnalyticsResponse getAnalyticsData();
}
