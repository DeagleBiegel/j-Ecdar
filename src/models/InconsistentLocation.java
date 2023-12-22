package models;

public class InconsistentLocation extends SymbolicLocation {
    @Override
    public String getName() {
        return "inc-loc";
    }

    @Override
    public boolean getIsInitial() {
        return false;
    }

    @Override
    public boolean getIsUrgent() {
        return false;
    }

    @Override
    public boolean getIsUniversal() {
        return false;
    }

    @Override
    public boolean getIsInconsistent() {
        return true;
    }

    @Override
    public int getY() {
        return 0;
    }

    @Override
    public int getX() {
        return 0;
    }

    @Override
    public String getEnterTestCode(){ return ""; }

    @Override
    public String getExitTestCode(){ return ""; }

    public CDD getInvariant() {
        // TODO the new clock should be <= 0
        return CDD.cddZero();
    }
}
