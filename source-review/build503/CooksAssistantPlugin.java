package net.runelite.client.plugins.microbot.cooksassistant;

import com.google.inject.Provides;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;

@PluginDescriptor(
        name = "Cook's Assistant",
        description = "Automates the Cook's Assistant quest in Lumbridge (bucket of milk, egg, pot of flour). Enable it while standing in Lumbridge.",
        tags = {"quest", "cooks assistant", "lumbridge", "microbot", "automation"},
        enabledByDefault = false
)
public class CooksAssistantPlugin extends Plugin {
    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(CooksAssistantPlugin.class);

    @Inject
    private CooksAssistantScript script;

    @Inject
    private CooksAssistantConfig config;

    // Build 417: overlay manager owns the diagnostic overlay lifecycle.
    @Inject
    private OverlayManager overlayManager;

    @Inject
    private CooksAssistantDiagOverlay diagOverlay;

    @Provides
    CooksAssistantConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(CooksAssistantConfig.class);
    }

    @Override
    protected void startUp() {
        log.info("[CooksAssistant] Plugin enabled -- starting script");
        // Build 417: register the diagnostic overlay. It renders nothing
        // until the controller sets an active layer (default LAYER_NONE).
        try {
            overlayManager.add(diagOverlay);
            script.setDiagOverlay(diagOverlay);
        } catch (Exception e) {
            log.warn("[CooksAssistant] Failed to register diag overlay", e);
        }
        script.run(config);
    }

    @Override
    protected void shutDown() {
        log.info("[CooksAssistant] Plugin disabled -- shutting down script");
        // Build 417: unregister the diagnostic overlay.
        try {
            overlayManager.remove(diagOverlay);
            script.setDiagOverlay(null);
        } catch (Exception e) {
            log.warn("[CooksAssistant] Failed to unregister diag overlay", e);
        }
        script.shutdown();
    }
}
