package com.eldrinn.tasknh.integration;

import net.minecraft.item.ItemStack;

import codechicken.nei.recipe.GuiCraftingRecipe;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Opens NEI recipe screens for a task icon.
 * References NEI classes directly, so callers must check {@link #isAvailable()} first
 * to keep the mod loadable without NEI.
 */
@SideOnly(Side.CLIENT)
public final class NEIRecipeIntegration {

    private static Boolean available;

    private NEIRecipeIntegration() {}

    public static boolean isAvailable() {
        if (available == null) available = Loader.isModLoaded("NotEnoughItems");
        return available;
    }

    /** Shows the recipes that produce the given stack. */
    public static void showRecipes(ItemStack stack) {
        GuiCraftingRecipe.openRecipeGui("item", stack);
    }
}
