package com.sammy.minedevice.client.phone;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

final class PhoneSettingsSurfaceRenderer {
    private static final int PAGE_FILL = 0xFFF8FAFC;
    private static final int HEADER_FILL = 0xFF1E293B;
    private static final int CARD_FILL = 0xFFFFFFFF;
    private static final int CARD_EDGE = 0xFFE2E8F0;
    private static final int CARD_HOVER = 0xFFF1F5F9;

    private static final int TEXT_PRIMARY = 0xFF111827;
    private static final int TEXT_SECONDARY = 0xFF64748B;
    private static final int TEXT_HEADER = 0xFFFFFFFF;

    private static final int ICON_BLUE = 0xFF3B82F6;
    private static final int ICON_GREEN = 0xFF10B981;
    private static final int ICON_ORANGE = 0xFFF97316;
    private static final int ICON_PURPLE = 0xFF8B5CF6;

    private PhoneSettingsSurfaceRenderer() {
    }

    static void renderSettingsSurface(PhoneScreen screen, GuiGraphics guiGraphics) {
        renderSettingsSurface(screen, guiGraphics, -1, -1);
    }

    static void renderSettingsSurface(PhoneScreen screen, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }

        Font font = minecraft.font;
        UiRect contentBounds = screen.getMediaSurfaceBounds();

        // Extend header to touch phone frame
        int headerExpand = Math.max(4, Math.round(6 * screen.scale));
        int headerLeft = contentBounds.left - headerExpand;
        int headerRight = contentBounds.right() + headerExpand;
        int contentLeft = contentBounds.left;
        int contentRight = contentBounds.right();
        int contentTop = contentBounds.top;
        int contentBottom = contentBounds.bottom();

        // Temporarily disabled scissor to test if it blocks navigation buttons
        // guiGraphics.enableScissor(headerLeft, contentTop, headerRight, contentBottom);

        guiGraphics.fill(headerLeft, contentTop, headerRight, contentBottom, PAGE_FILL);

        int headerHeight = Math.max(28, Math.round(34 * screen.scale));
        int headerBottom = Math.min(contentBottom, contentTop + headerHeight);

        guiGraphics.fill(headerLeft, contentTop, headerRight, headerBottom, HEADER_FILL);

        Component title = Component.translatable("screen.minedevice.phone.title.settings");
        int titleX = headerLeft + Math.max(4, Math.round(12 * screen.scale));
        int titleY = contentTop + Math.max(1, (headerHeight - font.lineHeight) / 2);
        guiGraphics.drawString(font, title, titleX, titleY, TEXT_HEADER, false);

        int paddingX = Math.max(4, Math.round(8 * screen.scale));
        int itemLeft = contentLeft + paddingX;
        int itemRight = contentRight - paddingX;
        int itemWidth = itemRight - itemLeft;
        int cardGap = Math.max(3, Math.round(4 * screen.scale));
        int cardY = headerBottom + cardGap;

        // --- Display name card ---
        int cardHeight = Math.max(32, Math.round(38 * screen.scale));
        renderCardBackground(guiGraphics, itemLeft, cardY, itemRight, cardY + cardHeight);

        int cardPad = Math.max(4, Math.round(5 * screen.scale));
        Component labelText = Component.translatable("screen.minedevice.phone.settings.display_name.label");
        float labelScale = Math.min(0.9F, (float) (cardHeight / 2) / Math.max(1, font.lineHeight));
        PhoneScreenDraw.drawScaledText(guiGraphics, font, labelText, itemLeft + cardPad, cardY + cardPad, TEXT_SECONDARY, false, labelScale);

        String currentName = screen.getMyDisplayName();
        boolean editing = screen.settingsEditingDisplayName;
        String valueStr = editing ? (screen.settingsDisplayNameBuffer + "|") : (currentName.isEmpty()
                ? minecraft.player != null ? minecraft.player.getGameProfile().getName() : "" : currentName);
        Component valueText = Component.literal(valueStr);

        int valueY = cardY + cardPad + PhoneScreenDraw.scaledTextHeight(font, labelScale) + Math.max(2, Math.round(3 * screen.scale));
        int valueMaxWidth = itemWidth - cardPad * 2 - Math.max(16, Math.round(20 * screen.scale));
        float valueScale = PhoneScreenDraw.textScaleToFit(font, valueText, Math.max(1, valueMaxWidth), 0.35F);
        int valueColor = editing ? ICON_BLUE : (currentName.isEmpty() ? TEXT_SECONDARY : TEXT_PRIMARY);
        PhoneScreenDraw.drawScaledText(guiGraphics, font, valueText, itemLeft + cardPad, valueY, valueColor, false, valueScale);

        // Edit button area (right side)
        if (!editing) {
            UiRect editBtnBounds = getSettingsEditButtonBounds(screen, itemLeft, cardY, itemRight, cardY + cardHeight);
            guiGraphics.fill(editBtnBounds.left, editBtnBounds.top, editBtnBounds.right(), editBtnBounds.bottom(), ICON_BLUE);
            Component editLabel = Component.translatable("screen.minedevice.phone.settings.display_name.edit");
            float btnScale = PhoneScreenDraw.textScaleToFit(font, editLabel, editBtnBounds.width - cardPad, 0.25F);
            int btnTw = PhoneScreenDraw.scaledTextWidth(font, editLabel, btnScale);
            int btnTh = PhoneScreenDraw.scaledTextHeight(font, btnScale);
            PhoneScreenDraw.drawScaledText(guiGraphics, font, editLabel,
                    editBtnBounds.left + (editBtnBounds.width - btnTw) / 2,
                    editBtnBounds.top + (editBtnBounds.height - btnTh) / 2,
                    0xFFFFFFFF, false, btnScale);
        } else {
            // Save / Cancel hints
            Component saveHint = Component.translatable("screen.minedevice.phone.settings.display_name.save_hint");
            float hintScale = PhoneScreenDraw.textScaleToFit(font, saveHint, Math.max(1, itemWidth - cardPad * 2), 0.25F);
            int hintY = cardY + cardHeight - PhoneScreenDraw.scaledTextHeight(font, hintScale) - cardPad;
            PhoneScreenDraw.drawScaledText(guiGraphics, font, saveHint, itemLeft + cardPad, hintY, TEXT_SECONDARY, false, hintScale);
        }

        // My number card
        int numCardY = cardY + cardHeight + cardGap;
        int numCardH = Math.max(24, Math.round(28 * screen.scale));
        renderCardBackground(guiGraphics, itemLeft, numCardY, itemRight, numCardY + numCardH);
        Component myNumLabel = Component.translatable("screen.minedevice.phone.settings.my_number");
        String myNumber = screen.getOwnPhoneNumber();
        float myNumLabelScale = labelScale;
        float myNumScale = Math.min(0.85F, myNumLabelScale);
        PhoneScreenDraw.drawScaledText(guiGraphics, font, myNumLabel, itemLeft + cardPad,
                numCardY + (numCardH - PhoneScreenDraw.scaledTextHeight(font, myNumLabelScale)) / 2,
                TEXT_SECONDARY, false, myNumLabelScale);
        Component myNumText = Component.literal(myNumber);
        int myNumW = PhoneScreenDraw.scaledTextWidth(font, myNumText, myNumScale);
        PhoneScreenDraw.drawScaledText(guiGraphics, font, myNumText,
                itemRight - cardPad - myNumW,
                numCardY + (numCardH - PhoneScreenDraw.scaledTextHeight(font, myNumScale)) / 2,
                TEXT_PRIMARY, false, myNumScale);

        // guiGraphics.disableScissor();
    }

    static boolean handleClick(PhoneScreen screen, double mouseX, double mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) return false;
        UiRect contentBounds = screen.getMediaSurfaceBounds();
        int contentLeft = contentBounds.left;
        int contentRight = contentBounds.right();
        int contentTop = contentBounds.top;
        int headerExpand = Math.max(4, Math.round(6 * screen.scale));
        int headerHeight = Math.max(28, Math.round(34 * screen.scale));
        int headerBottom = contentTop + headerHeight;
        int paddingX = Math.max(4, Math.round(8 * screen.scale));
        int itemLeft = contentLeft + paddingX;
        int itemRight = contentRight - paddingX;
        int cardGap = Math.max(3, Math.round(4 * screen.scale));
        int cardY = headerBottom + cardGap;
        int cardHeight = Math.max(32, Math.round(38 * screen.scale));

        if (!screen.settingsEditingDisplayName) {
            UiRect editBtnBounds = getSettingsEditButtonBounds(screen, itemLeft, cardY, itemRight, cardY + cardHeight);
            if (editBtnBounds.contains(mouseX, mouseY)) {
                screen.startSettingsNameEdit();
                return true;
            }
        }
        return false;
    }

    private static UiRect getSettingsEditButtonBounds(PhoneScreen screen, int itemLeft, int cardTop, int itemRight, int cardBottom) {
        int cardPad = Math.max(4, Math.round(5 * screen.scale));
        int btnWidth = Math.max(18, Math.round(24 * screen.scale));
        int btnHeight = Math.max(10, Math.round(12 * screen.scale));
        int btnX = itemRight - cardPad - btnWidth;
        int btnY = cardTop + (cardBottom - cardTop - btnHeight) / 2;
        return new UiRect(btnX, btnY, btnWidth, btnHeight);
    }

    private static void renderCardBackground(GuiGraphics guiGraphics, int left, int top, int right, int bottom) {
        guiGraphics.fill(left, top, right, bottom, CARD_EDGE);
        guiGraphics.fill(left + 1, top + 1, right - 1, bottom - 1, CARD_FILL);
    }

    private static void renderSettingsItem(
            GuiGraphics guiGraphics,
            Font font,
            int left,
            int top,
            int right,
            int height,
            String icon,
            String title,
            String subtitle,
            int iconColor,
            double mouseX,
            double mouseY
    ) {
        boolean hovering = mouseX >= left && mouseX < right && mouseY >= top && mouseY < top + height;

        guiGraphics.fill(left, top, right, top + height, hovering ? CARD_HOVER : CARD_FILL);
        guiGraphics.fill(left, top, right, top + 1, CARD_EDGE);
        guiGraphics.fill(left, top + height - 1, right, top + height, CARD_EDGE);
        guiGraphics.fill(left, top, left + 1, top + height, CARD_EDGE);
        guiGraphics.fill(right - 1, top, right, top + height, CARD_EDGE);

        int iconSize = Math.min(22, Math.max(16, height - 16));
        int iconLeft = left + 8;
        int iconTop = top + (height - iconSize) / 2;

        guiGraphics.fill(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize, iconColor);

        Component iconText = Component.literal(icon);
        int iconTextX = iconLeft + (iconSize - font.width(iconText)) / 2;
        int iconTextY = iconTop + (iconSize - font.lineHeight) / 2;
        guiGraphics.drawString(font, iconText, iconTextX, iconTextY, 0xFFFFFFFF, false);

        int textLeft = iconLeft + iconSize + 8;
        int textRight = right - 8;
        int textWidth = Math.max(1, textRight - textLeft);

        String fittedTitle = fitText(font, title, textWidth);
        String fittedSubtitle = fitText(font, subtitle, textWidth);

        int titleY = top + 8;
        int subtitleY = titleY + font.lineHeight + 2;

        guiGraphics.drawString(font, Component.literal(fittedTitle), textLeft, titleY, TEXT_PRIMARY, false);
        guiGraphics.drawString(font, Component.literal(fittedSubtitle), textLeft, subtitleY, TEXT_SECONDARY, false);
    }

    private static String fitText(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }

        String ellipsis = "...";
        int ellipsisWidth = font.width(ellipsis);

        if (ellipsisWidth >= maxWidth) {
            return "";
        }

        String fitted = text;
        while (!fitted.isEmpty() && font.width(fitted) + ellipsisWidth > maxWidth) {
            fitted = fitted.substring(0, fitted.length() - 1);
        }

        return fitted + ellipsis;
    }
}
