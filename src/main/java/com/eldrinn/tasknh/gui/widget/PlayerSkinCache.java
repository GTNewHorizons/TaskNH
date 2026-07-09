package com.eldrinn.tasknh.gui.widget;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.util.ResourceLocation;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class PlayerSkinCache {

    public static final PlayerSkinCache INSTANCE = new PlayerSkinCache();

    private final Map<String, ResourceLocation> cache = new HashMap<>();

    private PlayerSkinCache() {}

    public ResourceLocation get(String playerName) {
        return cache.computeIfAbsent(playerName, name -> {
            ResourceLocation loc = AbstractClientPlayer.getLocationSkin(name);
            AbstractClientPlayer.getDownloadImageSkin(loc, name);
            return loc;
        });
    }

}
