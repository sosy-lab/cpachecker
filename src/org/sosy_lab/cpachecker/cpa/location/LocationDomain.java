/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.location;

import org.sosy_lab.cpachecker.core.defaults.EqualityPartialOrder;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.JoinOperator;
import org.sosy_lab.cpachecker.core.interfaces.PartialOrder;

public class LocationDomain implements AbstractDomain
{
    private static class LocationBottomElement extends LocationElement
    {
      public LocationBottomElement() {
        super(null);
      }
      
      @Override
      public boolean equals(Object pOther) {
        if (pOther == null) {
          return false;
        }
        
        return (pOther instanceof LocationBottomElement);
      }
      
      @Override
      public int hashCode() {
        return Integer.MIN_VALUE;
      }
      
      @Override
      public String toString() {
        return "LocationBottomElement";
      }
    }

    private static class LocationTopElement extends LocationElement
    {

      public LocationTopElement() {
        super(null);
      }

      @Override
      public boolean equals(Object pOther) {
        if (pOther == null) {
          return false;
        }
        
        return (pOther instanceof LocationTopElement);
      }
      
      @Override
      public int hashCode() {
        return Integer.MAX_VALUE;
      }
      
      @Override
      public String toString() {
        return "LoctionTopElement";
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
    private final        PartialOrder partialOrder = new EqualityPartialOrder(this);
    private final static JoinOperator joinOperator = new LocationJoinOperator ();

    public LocationDomain ()
    {

    }

    public LocationElement getBottomElement ()
    {
        return bottomElement;
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
