package com.example.moviereservation.entity;

public enum ScreenType {
    STANDARD("Standard"),
    IMAX("IMAX"),
    Three_D("3D"),
    Four_DX("4DX"),
    VIP("VIP");

    private final String displayName;

    ScreenType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
