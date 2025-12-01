package com.maxlananas.btemap.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.List;
import java.util.function.BiConsumer;

public class WaypointScreen extends Screen {

    private final Screen parent;
    private int scrollOffset = 0;
    private int selectedIndex = -1;
    private TextFieldWidget nameField;
    private int selectedColor = 0;

    private final BiConsumer<Double, Double> onTeleport;

    public WaypointScreen(Screen parent, BiConsumer<Double, Double> onTeleport) {
        super(Text.literal("Waypoints"));
        this.parent = parent;
        this.onTeleport = onTeleport;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;

        nameField = new TextFieldWidget(this.textRenderer, centerX - 100, this.height - 75, 150, 20, Text.literal("Name"));
        nameField.setMaxLength(32);
        nameField.setPlaceholder(Text.literal("Waypoint name"));
        this.addDrawableChild(nameField);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Color"), button -> {
            selectedColor = (selectedColor + 1) % WaypointManager.COLORS.length;
            updateSelectedWaypoint();
        }).dimensions(centerX + 55, this.height - 75, 45, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Delete"), button -> {
            if (selectedIndex >= 0) {
                WaypointManager.removeWaypoint(selectedIndex);
                selectedIndex = -1;
                nameField.setText("");
            }
        }).dimensions(centerX - 100, this.height - 50, 60, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Teleport"), button -> {
            if (selectedIndex >= 0) {
                WaypointManager.Waypoint wp = WaypointManager.getWaypoint(selectedIndex);
                if (wp != null && onTeleport != null) {
                    onTeleport.accept(wp.lat, wp.lon);
                    this.close();
                }
            }
        }).dimensions(centerX - 35, this.height - 50, 70, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Clear All"), button -> {
            WaypointManager.clearAll();
            selectedIndex = -1;
        }).dimensions(centerX + 40, this.height - 50, 60, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Close"), button -> this.close())
                .dimensions(centerX - 40, this.height - 25, 80, 20).build());
    }

    private void updateSelectedWaypoint() {
        if (selectedIndex >= 0) {
            String name = nameField.getText().trim();
            if (!name.isEmpty()) {
                WaypointManager.updateWaypoint(selectedIndex, name,
                        WaypointManager.COLORS[selectedColor],
                        WaypointManager.ICONS[selectedIndex % WaypointManager.ICONS.length]);
            }
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        int listWidth = 280;
        int listX = centerX - listWidth / 2;
        int listY = 35;
        int listHeight = this.height - 120;
        int itemHeight = 30;

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, 15, 0xFFFFFF);

        context.fill(listX - 3, listY - 3, listX + listWidth + 3, listY + listHeight + 3, 0xFF222222);
        context.drawBorder(listX - 3, listY - 3, listWidth + 6, listHeight + 6, 0xFFFFFFFF);

        List<WaypointManager.Waypoint> waypoints = WaypointManager.getWaypoints();

        if (waypoints.isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer, "No waypoints", centerX, listY + listHeight / 2, 0x888888);
        } else {
            context.enableScissor(listX, listY, listX + listWidth, listY + listHeight);

            int y = listY - scrollOffset;
            for (int i = 0; i < waypoints.size(); i++) {
                if (y + itemHeight > listY && y < listY + listHeight) {
                    WaypointManager.Waypoint wp = waypoints.get(i);
                    boolean isSelected = (i == selectedIndex);
                    boolean isHovered = mouseX >= listX && mouseX < listX + listWidth &&
                            mouseY >= y && mouseY < y + itemHeight;

                    int bgColor = isSelected ? 0xFF444488 : (isHovered ? 0xFF333333 : 0xFF2A2A2A);
                    context.fill(listX, y, listX + listWidth, y + itemHeight - 2, bgColor);

                    context.fill(listX + 5, y + 5, listX + 20, y + 20, wp.color | 0xFF000000);

                    context.drawTextWithShadow(this.textRenderer, wp.name, listX + 28, y + 5, 0xFFFFFF);

                    String coords = String.format("%.4f, %.4f", wp.lat, wp.lon);
                    context.drawTextWithShadow(this.textRenderer, coords, listX + 28, y + 16, 0x888888);
                }
                y += itemHeight;
            }

            context.disableScissor();
        }

        if (selectedIndex >= 0) {
            context.fill(centerX + 55, this.height - 90, centerX + 75, this.height - 78,
                    WaypointManager.COLORS[selectedColor] | 0xFF000000);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int centerX = this.width / 2;
        int listWidth = 280;
        int listX = centerX - listWidth / 2;
        int listY = 35;
        int listHeight = this.height - 120;
        int itemHeight = 30;

        if (mouseX >= listX && mouseX < listX + listWidth && mouseY >= listY && mouseY < listY + listHeight) {
            int clickedIndex = (int) ((mouseY - listY + scrollOffset) / itemHeight);
            List<WaypointManager.Waypoint> waypoints = WaypointManager.getWaypoints();

            if (clickedIndex >= 0 && clickedIndex < waypoints.size()) {
                selectedIndex = clickedIndex;
                WaypointManager.Waypoint wp = waypoints.get(clickedIndex);
                nameField.setText(wp.name);

                for (int i = 0; i < WaypointManager.COLORS.length; i++) {
                    if (WaypointManager.COLORS[i] == wp.color) {
                        selectedColor = i;
                        break;
                    }
                }
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int listHeight = this.height - 120;
        int itemHeight = 30;
        int maxScroll = Math.max(0, WaypointManager.getWaypoints().size() * itemHeight - listHeight);

        scrollOffset -= (int) (verticalAmount * 20);
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset));

        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 && nameField.isFocused()) {
            updateSelectedWaypoint();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        updateSelectedWaypoint();
        this.client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}