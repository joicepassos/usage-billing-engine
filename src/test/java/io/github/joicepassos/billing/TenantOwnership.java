package io.github.joicepassos.billing;

import io.github.joicepassos.billing.shared.tenancy.TenantAgnostic;
import jakarta.persistence.Entity;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import org.hibernate.annotations.TenantId;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;

/** Rule: every entity has a {@code @TenantId} field or is explicitly {@link TenantAgnostic}. See ADR 0008. */
final class TenantOwnership {

    private TenantOwnership() {
    }

    static List<Class<?>> entitiesIn(String basePackage) {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
        return scanner.findCandidateComponents(basePackage).stream()
                .map(BeanDefinition::getBeanClassName)
                .<Class<?>>map(name -> ClassUtils.resolveClassName(name, TenantOwnership.class.getClassLoader()))
                .toList();
    }

    static List<Class<?>> violations(Collection<Class<?>> entities) {
        return entities.stream()
                .filter(type -> !type.isAnnotationPresent(TenantAgnostic.class))
                .filter(type -> !hasTenantIdField(type))
                .toList();
    }

    private static boolean hasTenantIdField(Class<?> type) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            if (Stream.of(current.getDeclaredFields()).anyMatch(TenantOwnership::isTenantId)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isTenantId(Field field) {
        return field.isAnnotationPresent(TenantId.class);
    }
}
