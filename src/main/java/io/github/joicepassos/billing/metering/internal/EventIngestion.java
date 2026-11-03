package io.github.joicepassos.billing.metering.internal;

import io.github.joicepassos.billing.metering.internal.IngestionRequests.UsageEventRequest;
import io.github.joicepassos.billing.metering.internal.IngestionResult.EventResult;
import io.github.joicepassos.billing.shared.BillingProperties;
import io.github.joicepassos.billing.shared.tenancy.TenantContext;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Batch ingestion (ADR 0003 and its amendment 1). In one transaction: business checks, billing period assignment,
 * a claim of every transaction id in the idempotency ledger, then the insert of the events that were new.
 */
@Service
class EventIngestion {

    static final Duration MAX_CLOCK_SKEW = Duration.ofMinutes(5);

    private final BillableMetricRepository metrics;
    private final UsageEventStore store;
    private final Clock clock;
    private final PeriodAssignment periods;

    EventIngestion(BillableMetricRepository metrics, UsageEventStore store, Clock clock, BillingProperties billing) {
        this.metrics = metrics;
        this.store = store;
        this.clock = clock;
        this.periods = new PeriodAssignment(billing.zone(), billing.graceWindow());
    }

    @Transactional
    IngestionResult ingest(List<UsageEventRequest> events) {
        UUID tenantId = TenantContext.current().orElseThrow(() -> new IllegalStateException("No tenant in context"));
        Instant receivedAt = clock.instant();
        Map<String, Aggregation> aggregations = metrics
                .findByCodeIn(events.stream().map(UsageEventRequest::metricCode).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(BillableMetric::code, BillableMetric::aggregation));

        EventResult[] results = new EventResult[events.size()];
        Map<String, UsageEventStore.NewEvent> firstByTransaction = new LinkedHashMap<>();
        Map<Integer, UsageEventStore.NewEvent> repeatedInBatch = new HashMap<>();

        for (int i = 0; i < events.size(); i++) {
            UsageEventRequest request = events.get(i);
            String problem = businessProblem(request, aggregations, receivedAt);
            if (problem != null) {
                results[i] = EventResult.rejected(request.transactionId(), problem);
                continue;
            }
            UsageEventStore.NewEvent event = toNewEvent(request, receivedAt);
            if (firstByTransaction.putIfAbsent(event.transactionId(), event) != null) {
                repeatedInBatch.put(i, event);
            }
        }

        List<UsageEventStore.NewEvent> candidates = List.copyOf(firstByTransaction.values());
        Set<String> inserted = store.claim(tenantId, candidates, receivedAt);
        Map<String, UsageEventStore.LedgerEntry> existing = store.ledger(tenantId,
                candidates.stream().map(UsageEventStore.NewEvent::transactionId)
                        .filter(id -> !inserted.contains(id)).toList());
        store.insertEvents(tenantId,
                candidates.stream().filter(event -> inserted.contains(event.transactionId())).toList(), receivedAt);

        Map<String, EventResult> outcomeByTransaction = new HashMap<>();
        for (UsageEventStore.NewEvent event : candidates) {
            outcomeByTransaction.put(event.transactionId(), inserted.contains(event.transactionId())
                    ? EventResult.accepted(event.transactionId(), YearMonth.from(event.billingPeriod()),
                            event.occurredAt())
                    : againstLedger(event, existing.get(event.transactionId())));
        }
        for (int i = 0; i < events.size(); i++) {
            if (results[i] != null) {
                continue;
            }
            String transactionId = events.get(i).transactionId();
            UsageEventStore.NewEvent repeated = repeatedInBatch.get(i);
            EventResult first = outcomeByTransaction.get(transactionId);
            results[i] = repeated == null ? first : againstFirst(repeated, firstByTransaction.get(transactionId), first);
        }
        return IngestionResult.of(List.of(results));
    }

    private String businessProblem(UsageEventRequest request, Map<String, Aggregation> aggregations,
            Instant receivedAt) {
        Aggregation aggregation = aggregations.get(request.metricCode());
        if (aggregation == null) {
            return "Unknown metric '" + request.metricCode() + "'. Register it with POST /v1/metrics first.";
        }
        if (aggregation == Aggregation.UNIQUE && (request.uniqueKey() == null || request.uniqueKey().isBlank())) {
            return "Metric '" + request.metricCode() + "' counts unique values, so uniqueKey is required.";
        }
        if (request.occurredAt().isAfter(receivedAt.plus(MAX_CLOCK_SKEW))) {
            return "occurredAt is more than 5 minutes in the future.";
        }
        return null;
    }

    private UsageEventStore.NewEvent toNewEvent(UsageEventRequest request, Instant receivedAt) {
        // timestamptz keeps microseconds; truncating first keeps the hash stable for retries.
        Instant occurredAt = request.occurredAt().truncatedTo(ChronoUnit.MICROS);
        String uniqueKey = request.uniqueKey() == null || request.uniqueKey().isBlank() ? null : request.uniqueKey();
        return new UsageEventStore.NewEvent(request.transactionId(), request.customerExternalId(),
                request.metricCode(), request.quantity().stripTrailingZeros().toPlainString(), occurredAt, uniqueKey,
                periods.assign(occurredAt, receivedAt).month().atDay(1),
                PayloadHash.of(request.customerExternalId(), request.metricCode(), request.quantity(), occurredAt,
                        uniqueKey));
    }

    private static EventResult againstLedger(UsageEventStore.NewEvent event, UsageEventStore.LedgerEntry entry) {
        if (entry == null || !entry.payloadHash().equals(event.payloadHash())) {
            return EventResult.conflict(event.transactionId());
        }
        return EventResult.duplicate(event.transactionId(), YearMonth.from(entry.billingPeriod()), entry.occurredAt());
    }

    private static EventResult againstFirst(UsageEventStore.NewEvent repeated, UsageEventStore.NewEvent first,
            EventResult firstOutcome) {
        if (!repeated.payloadHash().equals(first.payloadHash())) {
            return EventResult.conflict(repeated.transactionId());
        }
        return switch (firstOutcome.status()) {
            case ACCEPTED, DUPLICATE -> EventResult.duplicate(repeated.transactionId(), firstOutcome.billingPeriod(),
                    firstOutcome.occurredAt());
            case CONFLICT, REJECTED -> firstOutcome;
        };
    }
}
