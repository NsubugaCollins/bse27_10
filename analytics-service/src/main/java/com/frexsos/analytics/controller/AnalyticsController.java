package com.frexsos.analytics.controller;

import com.frexsos.analytics.dto.FactoryAnalyticsDTO;
import com.frexsos.analytics.dto.PlatformSummaryDTO;
import com.frexsos.analytics.dto.ProductionTrendDTO;
import com.frexsos.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /**
     * Platform-wide summary KPIs across all factories.
     */
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN','ENGINEER','MANAGER')")
    public ResponseEntity<PlatformSummaryDTO> getPlatformSummary() {
        return ResponseEntity.ok(analyticsService.getPlatformSummary());
    }

    /**
     * Factory-specific KPI dashboard.
     */
    @GetMapping("/factories/{factoryId}")
    @PreAuthorize("hasAnyRole('ADMIN','ENGINEER','MANAGER','TECHNICIAN')")
    public ResponseEntity<FactoryAnalyticsDTO> getFactoryAnalytics(@PathVariable Long factoryId) {
        return ResponseEntity.ok(analyticsService.getFactoryAnalytics(factoryId));
    }

    /**
     * Production trend data for time-series charts.
     * Optional factoryId filter — omit to get all factories.
     */
    @GetMapping("/trends/production")
    @PreAuthorize("hasAnyRole('ADMIN','ENGINEER','MANAGER','TECHNICIAN')")
    public ResponseEntity<List<ProductionTrendDTO>> getProductionTrends(
            @RequestParam(required = false) Long factoryId) {
        return ResponseEntity.ok(analyticsService.getProductionTrends(factoryId));
    }

    /**
     * Energy consumption trends.
     */
    @GetMapping("/trends/energy")
    @PreAuthorize("hasAnyRole('ADMIN','ENGINEER','MANAGER','TECHNICIAN')")
    public ResponseEntity<List<ProductionTrendDTO>> getEnergyTrends(
            @RequestParam(required = false) Long factoryId) {
        return ResponseEntity.ok(analyticsService.getEnergyTrends(factoryId));
    }

    /**
     * Bottleneck frequency report — shows which process step causes most delays.
     */
    @GetMapping("/bottlenecks")
    @PreAuthorize("hasAnyRole('ADMIN','ENGINEER','MANAGER')")
    public ResponseEntity<Map<String, Long>> getBottleneckReport(
            @RequestParam(required = false) Long factoryId) {
        return ResponseEntity.ok(analyticsService.getBottleneckReport(factoryId));
    }
}
