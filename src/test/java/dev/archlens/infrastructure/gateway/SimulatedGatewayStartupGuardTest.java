package dev.archlens.infrastructure.gateway;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SimulatedGatewayStartupGuardTest {

    @Test
    void refusesProductionBootWhenSimulatedGatewayFeedsFallback() {
        // NORMAL (produção) + fallback ligado + sem inferência externa + sem modo demo.
        assertTrue(SimulatedGatewayStartupGuard.mustRefuseBoot(true, true, false, false));
    }

    @Test
    void allowsProductionBootWithExplicitDemoMode() {
        assertFalse(SimulatedGatewayStartupGuard.mustRefuseBoot(true, true, false, true));
    }

    @Test
    void allowsProductionBootWithRealExternalInference() {
        assertFalse(SimulatedGatewayStartupGuard.mustRefuseBoot(true, true, true, false));
    }

    @Test
    void allowsProductionBootWhenFallbackDisabled() {
        assertFalse(SimulatedGatewayStartupGuard.mustRefuseBoot(true, false, false, false));
    }

    @Test
    void doesNotRefuseInNonProductionLaunch() {
        // DEV/TEST (normalLaunch=false) nunca recusa, independentemente do resto.
        assertFalse(SimulatedGatewayStartupGuard.mustRefuseBoot(false, true, false, false));
    }
}
