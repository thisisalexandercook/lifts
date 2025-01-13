package org.lift.models;

public enum WorkoutType {
    PUSH,
    PULL,
    LEGS,
    CARDIO,
    REST,
    MISC;

    @Override
    public String toString() {
        return name().toLowerCase(); 
    }
}
