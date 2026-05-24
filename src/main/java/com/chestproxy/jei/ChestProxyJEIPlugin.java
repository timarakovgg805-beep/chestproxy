package com.chestproxy.jei;

import com.chestproxy.ChestProxyMod;
import mezz.jei.api.IJeiHelpers;
import mezz.jei.api.IJeiRuntime;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraftforge.fml.common.Loader;

@JEIPlugin
public class ChestProxyJEIPlugin implements IModPlugin {

    @Override
    public void register(IModRegistry registry) {
        ChestProxyMod.info("ChestProxy JEI plugin register() called");
        IJeiHelpers jeiHelpers = registry.getJeiHelpers();
        IRecipeTransferHandlerHelper transferHelper = jeiHelpers.recipeTransferHandlerHelper();

        registry.getRecipeTransferRegistry().addRecipeTransferHandler(
            new ChestCraftingTransferHandler(transferHelper),
            VanillaRecipeCategoryUid.CRAFTING
        );
        ChestProxyMod.info("ChestCraftingTransferHandler registered for ContainerWorkbench + CRAFTING");

        // Register for FastWorkbench containers to ensure chest-aware transfer is used
        if (Loader.isModLoaded("fastbench")) {
            try {
                Class<?> fbClass = Class.forName("shadows.fastbench.gui.ContainerFastBench");
                if (ContainerWorkbench.class.isAssignableFrom(fbClass)) {
                    Class<? extends ContainerWorkbench> containerFB = fbClass.asSubclass(ContainerWorkbench.class);
                    registry.getRecipeTransferRegistry().addRecipeTransferHandler(
                        new ChestCraftingTransferHandler(transferHelper, containerFB),
                        VanillaRecipeCategoryUid.CRAFTING
                    );
                    ChestProxyMod.info("Registered handler for FastWorkbench ContainerFastBench");
                }
            } catch (ClassNotFoundException ignored) {}

            try {
                Class<?> cfbClass = Class.forName("shadows.fastbench.gui.ClientContainerFastBench");
                if (ContainerWorkbench.class.isAssignableFrom(cfbClass)) {
                    Class<? extends ContainerWorkbench> clientCFB = cfbClass.asSubclass(ContainerWorkbench.class);
                    registry.getRecipeTransferRegistry().addRecipeTransferHandler(
                        new ChestCraftingTransferHandler(transferHelper, clientCFB),
                        VanillaRecipeCategoryUid.CRAFTING
                    );
                    ChestProxyMod.info("Registered handler for FastWorkbench ClientContainerFastBench");
                }
            } catch (ClassNotFoundException ignored) {}
        }
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        ChestProxyMod.info("ChestProxy JEI onRuntimeAvailable()");
    }
}
