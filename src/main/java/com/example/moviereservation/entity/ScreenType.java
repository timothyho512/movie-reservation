package com.example.moviereservation.entity;

public enum ScreenType {
    STANDARD("Standard"),
    IMAX("IMAX"),
    DOLBY_ATMOS("Dolby Atmos"),
    THREE_D("3D"),
    FOUR_DX("4DX"),
    VIP("VIP");

    private final String displayName;

    ScreenType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
