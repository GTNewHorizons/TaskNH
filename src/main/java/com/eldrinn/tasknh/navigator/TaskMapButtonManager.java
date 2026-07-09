package com.eldrinn.tasknh.navigator;

import net.minecraft.util.ResourceLocation;

import com.gtnewhorizons.navigator.api.model.SupportedMods;
import com.gtnewhorizons.navigator.api.model.buttons.ButtonManager;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class TaskMapButtonManager extends ButtonManager {

    public static final TaskMapButtonManager INSTANCE = new TaskMapButtonManager();

    @Override
    public ResourceLocation getIcon(SupportedMods mod, String theme) {
        return new ResourceLocation("tasknh", "textures/navigator/task_icon.png");
    }

    @Override
    public String getButtonText() {
        return "Tasks";
    }
}
