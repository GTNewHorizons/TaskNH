package com.eldrinn.tasknh.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.cleanroommc.modularui.api.ITheme;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.widget.Widget;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class PlayerHeadWidget extends Widget<PlayerHeadWidget> {

    private final String playerName;

    public PlayerHeadWidget(String playerName) {
        this.playerName = playerName;
    }

    @Override
    protected WidgetThemeEntry<?> getWidgetThemeInternal(ITheme theme) {
        return theme.getFallback();
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        ResourceLocation skin = PlayerSkinCache.INSTANCE.get(playerName);
        int s = getArea().width;
        GL11.glColor4f(1f, 1f, 1f, 1f);
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(skin);
        // base face layer (U=8, V=8), then hat layer (U=40, V=8)
        Gui.func_152125_a(0, 0, 8f, 8f, 8, 8, s, s, 64f, 32f);
        Gui.func_152125_a(0, 0, 40f, 8f, 8, 8, s, s, 64f, 32f);
    }
}
