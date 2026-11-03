package io.github.joicepassos.billing.metering.internal;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Plain JDBC on purpose: a batch is one round trip per statement through {@code unnest}. These tables have no JPA
 * entity, so Hibernate's tenant filter does not apply: every statement here takes the tenant id explicitly.
 */
@Component
class UsageEventStore {

    private final JdbcTemplate jdbc;

    UsageEventStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** A new event ready to be stored. */
    record NewEvent(String transactionId, String customerExternalId, String metricCode, String quantity,
            Instant occurredAt, String uniqueKey, LocalDate billingPeriod, String payloadHash) {
    }

    /** What the ledger remembers about an accepted transaction. */
    record LedgerEntry(String payloadHash, Instant occurredAt, LocalDate billingPeriod) {
    }

    /** Claims the transaction ids in the ledger. Returns the ids this call inserted; the others already existed. */
    Set<String> claim(UUID tenantId, List<NewEvent> events, Instant acceptedAt) {
        String sql = """
                INSERT INTO ingested_transaction
                    (tenant_id, transaction_id, payload_hash, occurred_at, billing_period, accepted_at)
                SELECT ?, t.tx, decode(t.h, 'hex'), t.occ::timestamptz, t.period::date, ?
                FROM unnest(?::text[], ?::text[], ?::text[], ?::text[]) AS t(tx, h, occ, period)
                ON CONFLICT (tenant_id, transaction_id) DO NOTHING
                RETURNING transaction_id
                """;
        Set<String> inserted = new HashSet<>();
        jdbc.query(con -> {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setObject(1, tenantId);
            ps.setObject(2, utc(acceptedAt));
            ps.setArray(3, texts(con, events, NewEvent::transactionId));
            ps.setArray(4, texts(con, events, NewEvent::payloadHash));
            ps.setArray(5, texts(con, events, event -> event.occurredAt().toString()));
            ps.setArray(6, texts(con, events, event -> event.billingPeriod().toString()));
            return ps;
        }, rs -> {
            inserted.add(rs.getString(1));
        });
        return inserted;
    }

    Map<String, LedgerEntry> ledger(UUID tenantId, Collection<String> transactionIds) {
        Map<String, LedgerEntry> entries = new HashMap<>();
        if (transactionIds.isEmpty()) {
            return entries;
        }
        jdbc.query(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    SELECT transaction_id, encode(payload_hash, 'hex'), occurred_at, billing_period
                    FROM ingested_transaction
                    WHERE tenant_id = ? AND transaction_id = ANY(?::text[])
                    """);
            ps.setObject(1, tenantId);
            ps.setArray(2, con.createArrayOf("text", transactionIds.toArray(String[]::new)));
            return ps;
        }, rs -> {
            entries.put(rs.getString(1), new LedgerEntry(rs.getString(2),
                    rs.getObject(3, OffsetDateTime.class).toInstant(), rs.getObject(4, LocalDate.class)));
        });
        return entries;
    }

    void insertEvents(UUID tenantId, List<NewEvent> events, Instant receivedAt) {
        if (events.isEmpty()) {
            return;
        }
        String sql = """
                INSERT INTO usage_event (occurred_at, tenant_id, transaction_id, customer_external_id, metric_code,
                                         quantity, unique_key, billing_period, received_at, payload_hash)
                SELECT t.occ::timestamptz, ?, t.tx, t.cus, t.metric, t.qty::numeric, t.uk, t.period::date, ?,
                       decode(t.h, 'hex')
                FROM unnest(?::text[], ?::text[], ?::text[], ?::text[], ?::text[], ?::text[], ?::text[], ?::text[])
                    AS t(occ, tx, cus, metric, qty, uk, period, h)
                """;
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setObject(1, tenantId);
            ps.setObject(2, utc(receivedAt));
            ps.setArray(3, texts(con, events, event -> event.occurredAt().toString()));
            ps.setArray(4, texts(con, events, NewEvent::transactionId));
            ps.setArray(5, texts(con, events, NewEvent::customerExternalId));
            ps.setArray(6, texts(con, events, NewEvent::metricCode));
            ps.setArray(7, texts(con, events, NewEvent::quantity));
            ps.setArray(8, texts(con, events, NewEvent::uniqueKey));
            ps.setArray(9, texts(con, events, event -> event.billingPeriod().toString()));
            ps.setArray(10, texts(con, events, NewEvent::payloadHash));
            return ps;
        });
    }

    private static Array texts(Connection con, List<NewEvent> events, Function<NewEvent, String> field)
            throws SQLException {
        return con.createArrayOf("text", events.stream().map(field).toArray(String[]::new));
    }

    private static OffsetDateTime utc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }
}
