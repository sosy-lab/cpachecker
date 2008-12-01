package cfa.objectmodel;


public class CFAVariableInfo
{
    private String name;
    private String type;

    private boolean isConst;
    private boolean isVolatile;

    private boolean isLong;
    private boolean isLongLong;
    private boolean isShort;
    private boolean isUnsigned;

    // TODO-GeoffZ: Implement a better means, so that each indirection level can be const, etc
    private int indirectionLevel;

    public static final String UNKNOWN_TYPE = "unknown";
    public CFAVariableInfo (String name)
    {
        this.name = name;

        type = UNKNOWN_TYPE;
        isConst = false;
        isVolatile = false;

        isLong = false;
        isLongLong = false;
        isShort = false;
        isUnsigned = false;

        indirectionLevel = 0;
    }

    public String getName ()
    {
        return name;
    }

    public String getType ()
    {
        return type;
    }

    public void setType (String type)
    {
        this.type = type;
    }

    public boolean isConst ()
    {
        return isConst;
    }

    public boolean isVolatile ()
    {
        return isVolatile;
    }

    public boolean isLong ()
    {
        return isLong;
    }

    public boolean isLongLong ()
    {
        return isLongLong;
    }

    public boolean isShort ()
    {
        return isShort;
    }

    public boolean isUnsigned ()
    {
        return isUnsigned;
    }

    public int getIndirectionLevel ()
    {
        return indirectionLevel;
    }

    public void setIsConst (boolean isConst)
    {
        this.isConst = isConst;
    }

    public void setIsVolatile (boolean isVolatile)
    {
        this.isVolatile = isVolatile;
    }

    public void setIsLong (boolean isLong)
    {
        this.isLong = isLong;
    }

    public void setIsLongLong (boolean isLongLong)
    {
        this.isLongLong = isLongLong;
    }

    public void setIsShort (boolean isShort)
    {
        this.isShort = isShort;
    }

    public void setIsUnsigned (boolean isUnsigned)
    {
        this.isUnsigned = isUnsigned;
    }

    public void setIndirectionLevel (int indirectionLevel)
    {
        this.indirectionLevel = indirectionLevel;
    }
}
