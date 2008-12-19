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

import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.JoinOperator;
import cpa.common.interfaces.PartialOrder;

public class LocationDomain implements AbstractDomain
{
    private static class LocationBottomElement extends LocationElement
    {
      public LocationBottomElement() {
        super(null);
      }
    }

    private static class LocationTopElement extends LocationElement
    {

      public LocationTopElement() {
        super(null);
      }

    }

    private static class LocationPartialOrder implements PartialOrder
    {
      
      
        public boolean satisfiesPartialOrder (AbstractElement element1, AbstractElement element2)
        {
            if (element1.equals (element2))
                return true;

            if (element1 instanceof LocationBottomElement || element2 instanceof LocationTopElement)
                return true;

            return false;
        }
    }

    private static class LocationJoinOperator implements JoinOperator
    {
        public AbstractElement join (AbstractElement element1, AbstractElement element2)
        {
            // Useless code, but helps to catch bugs by causing cast exceptions
            LocationElement locElement1 = (LocationElement) element1;
            LocationElement locElement2 = (LocationElement) element2;

            if (locElement1.equals (locElement2))
                return locElement1;

            if (locElement1.equals(bottomElement))
                return locElement2;
            if (locElement2.equals(bottomElement))
                return locElement1;

            return topElement;
        }
    }

    private final static LocationBottomElement bottomElement = new LocationBottomElement ();
    private final static LocationTopElement topElement = new LocationTopElement ();
    private final static PartialOrder partialOrder = new LocationPartialOrder ();
    private final static JoinOperator joinOperator = new LocationJoinOperator ();

    public LocationDomain ()
    {

    }

    public AbstractElement getBottomElement ()
    {
        return bottomElement;
    }

	public boolean isBottomElement(AbstractElement element) {
		// TODO Auto-generated method stub
		return false;
	}

    public AbstractElement getTopElement ()
    {
        return topElement;
    }

    public JoinOperator getJoinOperator ()
    {
        return joinOperator;
    }

    public PartialOrder getPartialOrder ()
    {
        return partialOrder;
    }
}
