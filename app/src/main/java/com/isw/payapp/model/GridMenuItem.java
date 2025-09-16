package com.isw.payapp.model;

public class GridMenuItem {
    private final int iconResId;
    private final String title;
    private final int actionId;

    public GridMenuItem(int iconResId, String title, int actionId) {
        this.iconResId = iconResId;
        this.title = title;
        this.actionId = actionId;
    }

    public int getIconResId() {
        return iconResId;
    }

    public String getTitle() {
        return title;
    }

    public int getActionId() {
        return actionId;
    }
}
