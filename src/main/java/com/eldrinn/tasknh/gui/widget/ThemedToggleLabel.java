package com.eldrinn.tasknh.gui.widget;

import com.cleanroommc.modularui.api.ITheme;
import com.cleanroommc.modularui.theme.SelectableTheme;
import com.cleanroommc.modularui.theme.WidgetTheme;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.widgets.TextWidget;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * A label for one state of a {@link com.cleanroommc.modularui.widgets.ToggleButton}, colored from the
 * {@code toggleButton} theme entry: the base variant when unselected, {@link SelectableTheme#getSelected()}
 * when selected. A plain {@link TextWidget} resolves its own theme category instead of its parent's, so it
 * never sees {@code toggleButton}'s {@code selectedTextColor} - this is what lets a JSON theme recolor a
 * toggle button's label at all.
 */
@SideOnly(Side.CLIENT)
public class ThemedToggleLabel extends TextWidget<ThemedToggleLabel> {

    private final boolean selected;

    public ThemedToggleLabel(String label, boolean selected) {
        super(label);
        this.selected = selected;
    }

    @Override
    protected WidgetThemeEntry<?> getWidgetThemeInternal(ITheme theme) {
        return theme.getToggleButtonTheme();
    }

    @Override
    protected WidgetTheme getActiveWidgetTheme(WidgetThemeEntry<?> widgetTheme, boolean hover) {
        SelectableTheme selectableTheme = widgetTheme.expectType(SelectableTheme.class)
            .getTheme(hover);
        return selected ? selectableTheme.getSelected() : selectableTheme;
    }
}
