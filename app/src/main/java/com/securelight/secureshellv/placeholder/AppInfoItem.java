package com.securelight.secureshellv.placeholder;

import android.graphics.drawable.Drawable;

public class AppInfoItem {
    private boolean checked;
    private String packageName;
    private String name;
    private Drawable icon;

    public AppInfoItem(boolean checked, String packageName, String name, Drawable icon) {
        this.checked = checked;
        this.packageName = packageName;
        this.name = name;
        this.icon = icon;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public boolean isChecked() {
        return checked;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getName() {
        return name;
    }

    public Drawable getIcon() {
        return icon;
    }

    @Override
    public String toString() {
        return "AppInfoListItem{" +
                "checked=" + checked +
                ", packageName='" + packageName + '\'' +
                ", name='" + name + '\'' +
                ", icon='" + icon + '\'' +
                '}';
    }
}
