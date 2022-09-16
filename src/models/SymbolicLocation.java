package models;

public abstract class SymbolicLocation {
    public abstract String getName();

    public abstract CDD getInvariant();

    public abstract boolean getIsInitial();

    public abstract boolean getIsUrgent();

    public abstract boolean getIsUniversal();

    public abstract boolean getIsInconsistent();

    public abstract int getY();

    public abstract int getX();

    public abstract String getTestCode();
}