package io.github.joicepassos.billing.metering.internal;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Derived JPQL queries only, so Hibernate applies the tenant restriction to each of them. */
interface BillableMetricRepository extends JpaRepository<BillableMetric, UUID> {

    boolean existsByCode(String code);

    List<BillableMetric> findByCodeIn(Collection<String> codes);

    List<BillableMetric> findAllByOrderByCodeAsc();
}
