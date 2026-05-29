package com.example.autoexplorer;

import com.example.autoexplorer.modules.AutoExplorerModule;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AutoExplorerAddon — Main entry point for the Meteor Client addon.
 *
 * Extends {@link MeteorAddon}. Meteor discovers this class via the
 * fabric.mod.json "meteor-client:addon" entry point, instantiates it,
 * and calls {@link #onInitialize()} during Fabric mod-loading.
 */
public class AutoExplorerAddon extends MeteorAddon {

    public static final Logger LOG = LoggerFactory.getLogger("AutoExplorerAddon");

    /**
     * The custom category shown in Meteor's module list sidebar.
     * Color is packed ARGB: 0xFF34C759 = opaque bright green.
     */
    public static final Category CATEGORY = new Category("Automation", 0xFF34C759);

    /**
     * Called by Meteor after Fabric has finished loading all mods.
     * Register every Module here.
     */
    @Override
    public void onInitialize() {
        LOG.info("AutoExplorerAddon initializing...");

        // Register our module into Meteor's module system.
        Modules.get().add(new AutoExplorerModule());

        LOG.info("AutoExplorerAddon initialized.");
    }

    /**
     * Called by Meteor to register custom categories.
     * This is the correct hook — do NOT call MeteorAddon.registerCategory() statically.
     */
    @Override
    public void onRegisterCategories() {
        // registerCategory is an instance method inherited from MeteorAddon.
        registerCategory(CATEGORY);
    }
}
