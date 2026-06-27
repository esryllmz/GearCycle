package com.gearcycle.device.domain.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum DeviceLifecycleStatus {

    INTAKEN,
    INSPECTED,
    IN_REPAIR,
    AWAITING_QUALITY_CHECK,
    AWAITING_SAFETY_SCREENING,
    REFURBISHED,
    READY_FOR_RESALE,
    AWAITING_INSPECTION,
    READY_FOR_AS_IS_SALE,
    DECOMMISSIONED,
    RECYCLED;

    private Set<DeviceLifecycleStatus> allowedTransitions = Collections.emptySet();

    static {
        INTAKEN.allowedTransitions = Collections.unmodifiableSet(EnumSet.of(INSPECTED));
        AWAITING_INSPECTION.allowedTransitions = Collections.unmodifiableSet(EnumSet.of(INSPECTED));
        INSPECTED.allowedTransitions = Collections.unmodifiableSet(
                EnumSet.of(IN_REPAIR, AWAITING_SAFETY_SCREENING, DECOMMISSIONED, RECYCLED));
        IN_REPAIR.allowedTransitions = Collections.unmodifiableSet(
                EnumSet.of(AWAITING_QUALITY_CHECK, DECOMMISSIONED, RECYCLED));
        AWAITING_QUALITY_CHECK.allowedTransitions = Collections.unmodifiableSet(
                EnumSet.of(REFURBISHED, IN_REPAIR, RECYCLED));
        AWAITING_SAFETY_SCREENING.allowedTransitions = Collections.unmodifiableSet(
                EnumSet.of(READY_FOR_AS_IS_SALE, DECOMMISSIONED, RECYCLED));
        REFURBISHED.allowedTransitions = Collections.unmodifiableSet(EnumSet.of(READY_FOR_RESALE));
        // READY_FOR_RESALE -> AWAITING_INSPECTION is valid at the status level,
        // but Device.reopenForInspection() is the only permitted call path — a
        // mandatory reason must accompany the transition.
        READY_FOR_RESALE.allowedTransitions = Collections.unmodifiableSet(EnumSet.of(AWAITING_INSPECTION));
        // No outgoing transitions defined for MVP; not classified as terminal.
        READY_FOR_AS_IS_SALE.allowedTransitions = Collections.emptySet();
        // Terminal statuses — no outgoing transitions ever.
        DECOMMISSIONED.allowedTransitions = Collections.emptySet();
        RECYCLED.allowedTransitions = Collections.emptySet();
    }

    public boolean canTransitionTo(DeviceLifecycleStatus target) {
        return allowedTransitions.contains(target);
    }

    public boolean isTerminal() {
        return this == DECOMMISSIONED || this == RECYCLED;
    }
}
