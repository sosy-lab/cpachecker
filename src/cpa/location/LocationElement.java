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
package cpa.location;

import cfa.objectmodel.CFANode;
import cpa.common.interfaces.AbstractElementWithLocation;

public class LocationElement implements AbstractElementWithLocation
{
    private final CFANode locationNode;

    public LocationElement (CFANode locationNode)
    {
        this.locationNode = locationNode;
    }

    public CFANode getLocationNode ()
    {
        return locationNode;
    }

    @Override
    public boolean equals (Object other)
    {
        assert(other instanceof LocationElement);

        return locationNode.getNodeNumber () == ((LocationElement)other).locationNode.getNodeNumber ();
    }

    @Override
    public String toString ()
    {
        return Integer.toString (locationNode.getNodeNumber ());
    }

    @Override
    public int hashCode() {
    	return locationNode.getNodeNumber();
    }
}
