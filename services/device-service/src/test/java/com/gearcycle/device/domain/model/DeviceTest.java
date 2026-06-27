package com.gearcycle.device.domain.model;

import com.gearcycle.device.domain.exception.InvalidDeviceStateException;
import com.gearcycle.device.domain.exception.InvalidLifecycleTransitionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DeviceTest {

    // -- Factory --

    @Test
    void intake_createsDeviceInIntakenStatus() {
        Device device = Device.intake(DeviceCategory.LAPTOP);

        assertEquals(DeviceLifecycleStatus.INTAKEN, device.getStatus());
        assertNotNull(device.getId());
        assertNotNull(device.getReceivedAt());
        assertEquals(DeviceCategory.LAPTOP, device.getCategory());
        assertNull(device.getNotes());
    }

    @Test
    void intake_nullCategory_throws() {
        assertThrows(InvalidDeviceStateException.class, () -> Device.intake(null));
    }

    // -- Valid transitions: intake path --

    @Test
    void transition_intakenToInspected_succeeds() {
        Device device = Device.intake(DeviceCategory.SMARTPHONE);
        device.transitionTo(DeviceLifecycleStatus.INSPECTED);
        assertEquals(DeviceLifecycleStatus.INSPECTED, device.getStatus());
    }

    // -- Valid transitions: repair path --

    @Test
    void transition_inspectedToInRepair_succeeds() {
        Device device = inspectedDevice();
        device.transitionTo(DeviceLifecycleStatus.IN_REPAIR);
        assertEquals(DeviceLifecycleStatus.IN_REPAIR, device.getStatus());
    }

    @Test
    void transition_inRepairToAwaitingQualityCheck_succeeds() {
        Device device = inRepairDevice();
        device.transitionTo(DeviceLifecycleStatus.AWAITING_QUALITY_CHECK);
        assertEquals(DeviceLifecycleStatus.AWAITING_QUALITY_CHECK, device.getStatus());
    }

    @Test
    void transition_awaitingQualityCheckToRefurbished_succeeds() {
        Device device = awaitingQualityCheckDevice();
        device.transitionTo(DeviceLifecycleStatus.REFURBISHED);
        assertEquals(DeviceLifecycleStatus.REFURBISHED, device.getStatus());
    }

    @Test
    void transition_awaitingQualityCheckToInRepairForRework_succeeds() {
        Device device = awaitingQualityCheckDevice();
        device.transitionTo(DeviceLifecycleStatus.IN_REPAIR);
        assertEquals(DeviceLifecycleStatus.IN_REPAIR, device.getStatus());
    }

    @Test
    void transition_refurbishedToReadyForResale_succeeds() {
        Device device = refurbishedDevice();
        device.transitionTo(DeviceLifecycleStatus.READY_FOR_RESALE);
        assertEquals(DeviceLifecycleStatus.READY_FOR_RESALE, device.getStatus());
    }

    // -- Valid transitions: as-is path --

    @Test
    void transition_inspectedToAwaitingSafetyScreening_succeeds() {
        Device device = inspectedDevice();
        device.transitionTo(DeviceLifecycleStatus.AWAITING_SAFETY_SCREENING);
        assertEquals(DeviceLifecycleStatus.AWAITING_SAFETY_SCREENING, device.getStatus());
    }

    @Test
    void transition_awaitingSafetyScreeningToReadyForAsIsSale_succeeds() {
        Device device = awaitingSafetyScreeningDevice();
        device.transitionTo(DeviceLifecycleStatus.READY_FOR_AS_IS_SALE);
        assertEquals(DeviceLifecycleStatus.READY_FOR_AS_IS_SALE, device.getStatus());
    }

    // -- Valid transitions: disposal paths --

    @Test
    void transition_inspectedToRecycled_succeeds() {
        Device device = inspectedDevice();
        device.transitionTo(DeviceLifecycleStatus.RECYCLED);
        assertEquals(DeviceLifecycleStatus.RECYCLED, device.getStatus());
    }

    @Test
    void transition_inspectedToDecommissioned_succeeds() {
        Device device = inspectedDevice();
        device.transitionTo(DeviceLifecycleStatus.DECOMMISSIONED);
        assertEquals(DeviceLifecycleStatus.DECOMMISSIONED, device.getStatus());
    }

    @Test
    void transition_inRepairToDecommissioned_succeeds() {
        Device device = inRepairDevice();
        device.transitionTo(DeviceLifecycleStatus.DECOMMISSIONED);
        assertEquals(DeviceLifecycleStatus.DECOMMISSIONED, device.getStatus());
    }

    @Test
    void transition_awaitingSafetyScreeningToRecycled_succeeds() {
        Device device = awaitingSafetyScreeningDevice();
        device.transitionTo(DeviceLifecycleStatus.RECYCLED);
        assertEquals(DeviceLifecycleStatus.RECYCLED, device.getStatus());
    }

    // -- Reopen path: READY_FOR_RESALE -> AWAITING_INSPECTION --

    @Test
    void reopenForInspection_transitionsToAwaitingInspectionAndStoresReason() {
        Device device = readyForResaleDevice();
        device.reopenForInspection("Hairline crack found on screen after packaging");
        assertEquals(DeviceLifecycleStatus.AWAITING_INSPECTION, device.getStatus());
        assertEquals("Hairline crack found on screen after packaging", device.getNotes());
    }

    @Test
    void reopenForInspection_nullReason_throws() {
        Device device = readyForResaleDevice();
        assertThrows(InvalidDeviceStateException.class, () -> device.reopenForInspection(null));
    }

    @Test
    void reopenForInspection_blankReason_throws() {
        Device device = readyForResaleDevice();
        assertThrows(InvalidDeviceStateException.class, () -> device.reopenForInspection("   "));
    }

    @Test
    void reopenForInspection_wrongStatus_throws() {
        Device device = inspectedDevice();
        assertThrows(InvalidLifecycleTransitionException.class,
                () -> device.reopenForInspection("some reason"));
    }

    @Test
    void transitionTo_readyForResaleToAwaitingInspection_throwsWithHintToUseReopen() {
        Device device = readyForResaleDevice();
        InvalidLifecycleTransitionException ex = assertThrows(
                InvalidLifecycleTransitionException.class,
                () -> device.transitionTo(DeviceLifecycleStatus.AWAITING_INSPECTION));
        assertTrue(ex.getMessage().contains("reopenForInspection"));
    }

    @Test
    void transition_awaitingInspectionToInspected_succeeds() {
        Device device = readyForResaleDevice();
        device.reopenForInspection("Battery bulging discovered during quality audit");
        device.transitionTo(DeviceLifecycleStatus.INSPECTED);
        assertEquals(DeviceLifecycleStatus.INSPECTED, device.getStatus());
    }

    // -- Terminal status enforcement --

    @Test
    void transition_fromDecommissioned_throws() {
        Device device = inspectedDevice();
        device.transitionTo(DeviceLifecycleStatus.DECOMMISSIONED);
        assertThrows(InvalidLifecycleTransitionException.class,
                () -> device.transitionTo(DeviceLifecycleStatus.RECYCLED));
    }

    @Test
    void transition_fromRecycled_throws() {
        Device device = inspectedDevice();
        device.transitionTo(DeviceLifecycleStatus.RECYCLED);
        assertThrows(InvalidLifecycleTransitionException.class,
                () -> device.transitionTo(DeviceLifecycleStatus.DECOMMISSIONED));
    }

    @Test
    void decommissioned_isTerminal() {
        assertTrue(DeviceLifecycleStatus.DECOMMISSIONED.isTerminal());
    }

    @Test
    void recycled_isTerminal() {
        assertTrue(DeviceLifecycleStatus.RECYCLED.isTerminal());
    }

    @Test
    void readyForAsIsSale_isNotTerminal() {
        assertFalse(DeviceLifecycleStatus.READY_FOR_AS_IS_SALE.isTerminal());
    }

    // -- Invalid transitions --

    @Test
    void transition_intakenDirectlyToInRepair_throws() {
        Device device = Device.intake(DeviceCategory.TABLET);
        assertThrows(InvalidLifecycleTransitionException.class,
                () -> device.transitionTo(DeviceLifecycleStatus.IN_REPAIR));
    }

    @Test
    void transition_inRepairDirectlyToRefurbished_throws() {
        Device device = inRepairDevice();
        assertThrows(InvalidLifecycleTransitionException.class,
                () -> device.transitionTo(DeviceLifecycleStatus.REFURBISHED));
    }

    @Test
    void transition_inRepairToReadyForAsIsSale_throws() {
        Device device = inRepairDevice();
        assertThrows(InvalidLifecycleTransitionException.class,
                () -> device.transitionTo(DeviceLifecycleStatus.READY_FOR_AS_IS_SALE));
    }

    @Test
    void transition_nullStatus_throws() {
        Device device = Device.intake(DeviceCategory.LAPTOP);
        assertThrows(InvalidDeviceStateException.class, () -> device.transitionTo(null));
    }

    // -- Helpers --

    private Device inspectedDevice() {
        Device device = Device.intake(DeviceCategory.LAPTOP);
        device.transitionTo(DeviceLifecycleStatus.INSPECTED);
        return device;
    }

    private Device inRepairDevice() {
        Device device = inspectedDevice();
        device.transitionTo(DeviceLifecycleStatus.IN_REPAIR);
        return device;
    }

    private Device awaitingQualityCheckDevice() {
        Device device = inRepairDevice();
        device.transitionTo(DeviceLifecycleStatus.AWAITING_QUALITY_CHECK);
        return device;
    }

    private Device awaitingSafetyScreeningDevice() {
        Device device = inspectedDevice();
        device.transitionTo(DeviceLifecycleStatus.AWAITING_SAFETY_SCREENING);
        return device;
    }

    private Device refurbishedDevice() {
        Device device = awaitingQualityCheckDevice();
        device.transitionTo(DeviceLifecycleStatus.REFURBISHED);
        return device;
    }

    private Device readyForResaleDevice() {
        Device device = refurbishedDevice();
        device.transitionTo(DeviceLifecycleStatus.READY_FOR_RESALE);
        return device;
    }
}
