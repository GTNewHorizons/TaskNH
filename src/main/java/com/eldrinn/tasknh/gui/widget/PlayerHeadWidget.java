package com.eldrinn.tasknh.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.texture.TextureManager;
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

    /** Purely decorative, so it must not swallow the hover highlight of the row it sits on. */
    @Override
    public boolean canHover() {
        return false;
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
        TextureManager textures = Minecraft.getMinecraft()
            .getTextureManager();
        textures.bindTexture(skin);
        // Legacy skins are 64x32, modern ones 64x64, so the height has to be read from the bound texture instead of
        // assumed. A height of 0 means the download hasn't produced a texture yet: fall back to the default skin.
        float texHeight = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
        if (texHeight <= 0f) {
            textures.bindTexture(AbstractClientPlayer.locationStevePng);
            texHeight = 32f;
        }
        // base face layer (U=8, V=8), then hat layer (U=40, V=8)
        Gui.func_152125_a(0, 0, 8f, 8f, 8, 8, s, s, 64f, texHeight);
        Gui.func_152125_a(0, 0, 40f, 8f, 8, 8, s, s, 64f, texHeight);
    }
}
