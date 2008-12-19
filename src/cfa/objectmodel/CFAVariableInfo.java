/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
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
