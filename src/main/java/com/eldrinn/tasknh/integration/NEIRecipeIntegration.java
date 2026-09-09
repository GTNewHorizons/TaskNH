package com.eldrinn.tasknh.integration;

import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.ModularUI;

import codechicken.nei.recipe.GuiCraftingRecipe;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Opens NEI recipe screens for a task icon.
 * References NEI classes directly, so callers must check {@link #isAvailable()} first
 * to keep the mod loadable without NEI.
 */
@SideOnly(Side.CLIENT)
public final class NEIRecipeIntegration {

    private NEIRecipeIntegration() {}

    public static boolean isAvailable() {
        return ModularUI.Mods.NEI.isLoaded();
    }

    /** Shows the recipes that produce the given stack. Returns false when the item has none. */
    public static boolean showRecipes(ItemStack stack) {
        return GuiCraftingRecipe.openRecipeGui("item", stack);
    }
}
