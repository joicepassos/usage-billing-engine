package io.github.joicepassos.billing.metering.internal;

import io.github.joicepassos.billing.shared.web.ConflictException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/metrics")
@SecurityRequirement(name = "apiKey")
class MetricController {

    private final BillableMetricRepository metrics;
    private final Clock clock;

    MetricController(BillableMetricRepository metrics, Clock clock) {
        this.metrics = metrics;
        this.clock = clock;
    }

    @PostMapping
    @Transactional
    @Operation(summary = "Register a billable metric")
    ResponseEntity<MetricResponse> create(@Valid @RequestBody CreateMetric request) {
        if (metrics.existsByCode(request.code())) {
            throw duplicate(request.code());
        }
        BillableMetric metric = new BillableMetric(UUID.randomUUID(), request.code(), request.name().strip(),
                Aggregation.fromApi(request.aggregation()), clock.instant());
        try {
            metrics.saveAndFlush(metric);
        } catch (DataIntegrityViolationException e) {
            throw duplicate(request.code());
        }
        return ResponseEntity.created(URI.create("/v1/metrics/" + metric.id())).body(MetricResponse.of(metric));
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "List the tenant's billable metrics")
    List<MetricResponse> list() {
        return metrics.findAllByOrderByCodeAsc().stream().map(MetricResponse::of).toList();
    }

    private static ConflictException duplicate(String code) {
        return new ConflictException("A metric with code '" + code + "' already exists");
    }

    record CreateMetric(
            @NotBlank @Size(max = 64) @Pattern(regexp = "[a-z][a-z0-9_]*",
                    message = "must start with a letter and contain only lower-case letters, digits and _") String code,
            @NotBlank @Size(max = 200) String name,
            @NotBlank @Pattern(regexp = "sum|count|max|unique",
                    message = "must be one of sum, count, max, unique") String aggregation) {
    }

    record MetricResponse(UUID id, String code, String name, String aggregation) {

        static MetricResponse of(BillableMetric metric) {
            return new MetricResponse(metric.id(), metric.code(), metric.name(), metric.aggregation().toApi());
        }
    }
}
