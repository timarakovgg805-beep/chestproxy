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
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        ChestProxyMod.info("ChestProxy JEI onRuntimeAvailable()");
    }
}
