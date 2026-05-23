package com.chestproxy.jei;

import com.chestproxy.ChestProxyMod;
import com.chestproxy.config.ChestProxyConfig;
import com.chestproxy.util.ChestHelper;
import mezz.jei.api.gui.IGuiIngredient;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChestCraftingTransferHandler implements IRecipeTransferHandler<ContainerWorkbench> {

    private static final int CRAFTING_SLOTS = 9;
    private final IRecipeTransferHandlerHelper helper;

    public ChestCraftingTransferHandler(IRecipeTransferHandlerHelper helper) {
        this.helper = helper;
    }

    @Override
    public Class<ContainerWorkbench> getContainerClass() {
        return ContainerWorkbench.class;
    }

    @Nullable
    @Override
    public IRecipeTransferError transferRecipe(ContainerWorkbench container, IRecipeLayout recipeLayout, EntityPlayer player, boolean maxTransfer, boolean doTransfer) {
        ChestProxyMod.LOG.info(">>> transferRecipe called doTransfer={} enabled={}", doTransfer, ChestProxyConfig.isEnabled());

        Map<Integer, ? extends IGuiIngredient<ItemStack>> guiIngredients = recipeLayout.getItemStacks().getGuiIngredients();

        ItemStack expectedOutput = ItemStack.EMPTY;
        IGuiIngredient<ItemStack> outputIng = guiIngredients.get(0);
        if (outputIng != null && outputIng.getDisplayedIngredient() != null) {
            expectedOutput = outputIng.getDisplayedIngredient().copy();
        } else if (outputIng != null && outputIng.getAllIngredients() != null && !outputIng.getAllIngredients().isEmpty()) {
            expectedOutput = outputIng.getAllIngredients().get(0).copy();
        }
        ChestProxyMod.LOG.info("Expected output: {}", expectedOutput);

        List<ItemStack> inputs = new ArrayList<>(CRAFTING_SLOTS);
        for (int i = 0; i < CRAFTING_SLOTS; i++) {
            IGuiIngredient<ItemStack> ing = guiIngredients.get(i + 1);
            if (ing == null || ing.getAllIngredients() == null || ing.getAllIngredients().isEmpty()) {
                inputs.add(ItemStack.EMPTY);
            } else {
                ItemStack ingStack = ing.getAllIngredients().get(0).copy();
                inputs.add(ingStack);
                ChestProxyMod.LOG.debug("  JEI input slot {}: {}", i, ingStack);
            }
        }

        World world = player.world;
        MinecraftServer server = null;
        boolean isRemote = world.isRemote;
        if (isRemote) {
            server = FMLCommonHandler.instance().getMinecraftServerInstance();
            if (server != null) {
                world = server.getWorld(player.dimension);
            } else {
                ChestProxyMod.LOG.warn("Server world not available, using client world");
            }
        }

        List<IInventory> chests;
        if (ChestProxyConfig.isEnabled()) {
            chests = ChestHelper.findNearbyInventories(world, player.getPosition());
        } else {
            chests = Collections.emptyList();
        }

        if (!doTransfer) {
            return validateItems(player, chests, inputs, maxTransfer);
        }

        return performTransfer(container, player, world, server, chests, inputs, expectedOutput, isRemote, maxTransfer);
    }

    @Nullable
    private IRecipeTransferError validateItems(EntityPlayer player, List<IInventory> chests, List<ItemStack> inputs, boolean maxTransfer) {
        int maxCrafts = maxTransfer ? Integer.MAX_VALUE : 1;
        List<Integer> missingSlots = new ArrayList<>();
        // Group per item type for per-type validation
        Map<String, int[]> itemNeeded = new HashMap<>();
        for (int i = 0; i < CRAFTING_SLOTS; i++) {
            ItemStack needed = inputs.get(i);
            if (needed.isEmpty()) continue;

            int inInv = ChestHelper.countInInventory(player, needed);
            int inChests = ChestHelper.countInChests(chests, needed);
            int total = inInv + inChests;
            int possible = total / needed.getCount();

            ChestProxyMod.LOG.info("  Slot {} need {}x {} (inv={} chests={} total={} possible={})",
                i, needed.getCount(), needed, inInv, inChests, total, possible);

            if (possible < maxCrafts) {
                maxCrafts = possible;
            }
            if (total < needed.getCount()) {
                missingSlots.add(i + 1);
            }
            // Group by item type for per-type maxCrafts
            String key = needed.getItem().getRegistryName() + "@" + needed.getItemDamage();
            int[] arr = itemNeeded.get(key);
            if (arr == null) {
                arr = new int[]{0, (inInv + inChests)};
                itemNeeded.put(key, arr);
            }
            arr[0] += needed.getCount();
        }
        // Recalculate based on total per item type
        if (maxTransfer) {
            for (int[] arr : itemNeeded.values()) {
                if (arr[0] > 0) {
                    int possible = arr[1] / arr[0];
                    if (possible < maxCrafts) maxCrafts = possible;
                }
            }
        }
        if (maxTransfer && maxCrafts > 0 && maxCrafts < Integer.MAX_VALUE) {
            ChestProxyMod.LOG.info("  Can craft {}x", maxCrafts);
        }
        if (!missingSlots.isEmpty()) {
            return helper.createUserErrorForSlots("Missing items", missingSlots);
        }
        ChestProxyMod.LOG.info("  All items available");
        return null;
    }

    @Nullable
    private IRecipeTransferError performTransfer(ContainerWorkbench container, EntityPlayer player, World world, @Nullable MinecraftServer server, List<IInventory> chests, List<ItemStack> inputs, ItemStack expectedOutput, boolean isRemote, boolean maxTransfer) {
        // Calculate max crafts if shift is held
        int times = 1;
        if (maxTransfer) {
            Map<String, int[]> itemNeeded = new HashMap<>();
            times = Integer.MAX_VALUE;
            for (int i = 0; i < CRAFTING_SLOTS; i++) {
                ItemStack needed = inputs.get(i);
                if (needed.isEmpty()) continue;
                int inInv = ChestHelper.countInInventory(player, needed);
                int inChests = ChestHelper.countInChests(chests, needed);
                int possible = (inInv + inChests) / needed.getCount();
                if (possible < times) times = possible;
                // Group by item type for total extraction
                String key = needed.getItem().getRegistryName() + "@" + needed.getItemDamage();
                int[] arr = itemNeeded.get(key);
                if (arr == null) {
                    arr = new int[]{0, (inInv + inChests)};
                    itemNeeded.put(key, arr);
                }
                arr[0] += needed.getCount();
            }
            // Recalculate based on total per item type
            for (int[] arr : itemNeeded.values()) {
                if (arr[0] > 0) {
                    int possible = arr[1] / arr[0];
                    if (possible < times) times = possible;
                }
            }
            ChestProxyMod.LOG.info("Max transfer: crafting {}x", times);
        }

        // Phase 1: Extract total needed items from inventory + chests (one pass per item type)
        Map<String, int[]> extractMap = new HashMap<>();
        for (int i = 0; i < CRAFTING_SLOTS; i++) {
            ItemStack needed = inputs.get(i);
            if (needed.isEmpty()) continue;
            int needTotal = needed.getCount() * times;
            String key = needed.getItem().getRegistryName() + "@" + needed.getItemDamage();
            int[] arr = extractMap.get(key);
            if (arr == null) {
                arr = new int[]{0, 0, needed.getItemDamage()};
                extractMap.put(key, arr);
            }
            arr[0] += needTotal;
        }
        for (Map.Entry<String, int[]> entry : extractMap.entrySet()) {
            int needTotal = entry.getValue()[0];
            if (needTotal <= 0) continue;
            String itemName = entry.getKey();
            // Find a matching needed stack for this item type
            ItemStack template = null;
            for (int i = 0; i < CRAFTING_SLOTS; i++) {
                ItemStack n = inputs.get(i);
                if (!n.isEmpty() && (n.getItem().getRegistryName() + "@" + n.getItemDamage()).equals(itemName)) {
                    template = n;
                    break;
                }
            }
            if (template == null) continue;

            ChestProxyMod.LOG.info("Extracting {}x {} for all slots combined", needTotal, itemName);
            int remaining = needTotal;

            for (int j = 0; j < player.inventory.getSizeInventory(); j++) {
                if (remaining <= 0) break;
                ItemStack invStack = player.inventory.getStackInSlot(j);
                if (invStack.isEmpty() || !ChestHelper.canMerge(invStack, template)) continue;
                int toTake = Math.min(invStack.getCount(), remaining);
                invStack.splitStack(toTake);
                if (invStack.isEmpty()) {
                    player.inventory.setInventorySlotContents(j, ItemStack.EMPTY);
                }
                remaining -= toTake;
            }
            if (remaining > 0) {
                ChestHelper.extractFromChests(chests, template, remaining);
            }
        }

        // Phase 2: Fill grid (no extraction)
        for (int s = 0; s < CRAFTING_SLOTS; s++) {
            container.craftMatrix.setInventorySlotContents(s, ItemStack.EMPTY);
        }
        for (int i = 0; i < CRAFTING_SLOTS; i++) {
            ItemStack needed = inputs.get(i);
            if (needed.isEmpty()) continue;
            int gridCount = Math.min(needed.getCount() * times, 64);
            ItemStack gridStack = needed.copy();
            gridStack.setCount(gridCount);
            container.craftMatrix.setInventorySlotContents(i, gridStack);
            ChestProxyMod.LOG.info("Slot {}: grid {}x", i, gridCount);
        }

        ItemStack result = CraftingManager.findMatchingResult(container.craftMatrix, world);
        if (result.isEmpty() && !expectedOutput.isEmpty()) {
            result = expectedOutput.copy();
        }

        if (!result.isEmpty()) {
            container.craftResult.setInventorySlotContents(0, result.copy());
        }
        container.detectAndSendChanges();

        // SSP: sync to server container (per-type extraction, matching client logic)
        if (isRemote && server != null) {
            try {
                EntityPlayerMP serverPlayer = server.getPlayerList().getPlayerByUUID(player.getUniqueID());
                ChestProxyMod.LOG.info("  serverPlayer={}", serverPlayer);
                if (serverPlayer != null) {
                    if (serverPlayer.openContainer instanceof ContainerWorkbench) {
                        ContainerWorkbench sc = (ContainerWorkbench) serverPlayer.openContainer;
                        // Set grid on server
                        for (int s = 0; s < CRAFTING_SLOTS; s++) {
                            sc.craftMatrix.setInventorySlotContents(s, ItemStack.EMPTY);
                        }
                        for (int i = 0; i < CRAFTING_SLOTS; i++) {
                            ItemStack needed = inputs.get(i);
                            if (needed.isEmpty()) continue;
                            int gridCount = Math.min(needed.getCount() * times, 64);
                            ItemStack gridStack = needed.copy();
                            gridStack.setCount(gridCount);
                            sc.craftMatrix.setInventorySlotContents(i, gridStack);
                        }
                        if (!result.isEmpty()) {
                            sc.craftResult.setInventorySlotContents(0, result.copy());
                        }
                        // Extract from server player inventory (per-type, matching client logic)
                        Map<String, int[]> serverExtract = new HashMap<>();
                        for (int i = 0; i < CRAFTING_SLOTS; i++) {
                            ItemStack srvNeeded = inputs.get(i);
                            if (srvNeeded.isEmpty()) continue;
                            String key = srvNeeded.getItem().getRegistryName() + "@" + srvNeeded.getItemDamage();
                            int[] arr = serverExtract.get(key);
                            if (arr == null) {
                                arr = new int[]{0};
                                serverExtract.put(key, arr);
                            }
                            arr[0] += srvNeeded.getCount() * times;
                        }
                        for (int i = 0; i < CRAFTING_SLOTS; i++) {
                            ItemStack srvNeeded = inputs.get(i);
                            if (srvNeeded.isEmpty()) continue;
                            String key = srvNeeded.getItem().getRegistryName() + "@" + srvNeeded.getItemDamage();
                            int totalNeed = serverExtract.get(key)[0];
                            if (totalNeed <= 0) continue;
                            int remaining = totalNeed;
                            for (int j = 0; j < serverPlayer.inventory.getSizeInventory(); j++) {
                                if (remaining <= 0) break;
                                ItemStack invStack = serverPlayer.inventory.getStackInSlot(j);
                                if (invStack.isEmpty() || !ChestHelper.canMerge(invStack, srvNeeded)) continue;
                                int toTake = Math.min(invStack.getCount(), remaining);
                                invStack.splitStack(toTake);
                                if (invStack.isEmpty()) {
                                    serverPlayer.inventory.setInventorySlotContents(j, ItemStack.EMPTY);
                                }
                                remaining -= toTake;
                            }
                            serverExtract.get(key)[0] = 0; // Mark as done
                        }
                        sc.detectAndSendChanges();
                        ChestProxyMod.LOG.info("  Server container synced OK");
                    } else {
                        ChestProxyMod.LOG.warn("  Server openContainer not ContainerWorkbench: {}", serverPlayer.openContainer);
                    }
                }
            } catch (Throwable e) {
                ChestProxyMod.LOG.error("  Server sync error: {}", e.toString());
            }
        }

        // Debug dump
        ChestProxyMod.LOG.info("=== Craft Matrix Dump ===");
        for (int r = 0; r < 3; r++) {
            StringBuilder row = new StringBuilder("  Row " + r + ": ");
            for (int c = 0; c < 3; c++) {
                ItemStack s = container.craftMatrix.getStackInRowAndColumn(r, c);
                row.append("[").append(s.isEmpty() ? "EMPTY" : s.toString()).append("] ");
            }
            ChestProxyMod.LOG.info(row.toString());
        }

        ItemStack matchedResult = CraftingManager.findMatchingResult(container.craftMatrix, world);
        ChestProxyMod.LOG.info("Recipe match: {} | JEI: {}", matchedResult, expectedOutput);

        int matchCount = 0;
        for (IRecipe recipe : CraftingManager.REGISTRY) {
            if (recipe.matches(container.craftMatrix, world)) {
                matchCount++;
                ChestProxyMod.LOG.info("  Match #{}: {} -> {}", matchCount,
                    recipe.getRecipeOutput(), recipe.getRegistryName());
            }
        }
        ChestProxyMod.LOG.info("Total matches: {}", matchCount);

        return null;
    }
}
