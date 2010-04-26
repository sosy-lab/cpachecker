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
package org.sosy_lab.cpachecker.cpa.octagon;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.util.octagon.LibraryAccess;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.JoinOperator;
import org.sosy_lab.cpachecker.core.interfaces.PartialOrder;

public class OctDomain implements AbstractDomain{
  
  public static long totaltime = 0;

  private static class OctBottomElement extends OctElement
  {
    public OctBottomElement ()
    {
      super ();
    }
  }

  private static class OctTopElement extends OctElement
  {
    public OctTopElement ()
    {
      //super (LibraryAccess.universe(Variables.numOfVars));
    }
  }

  private static class OctPartialOrder implements PartialOrder
  {
    public boolean satisfiesPartialOrder (AbstractElement element1, AbstractElement element2)
    {
      
      Map<OctElement, Set<OctElement>> covers = new HashMap<OctElement, Set<OctElement>>();
      
      long start = System.currentTimeMillis();
      OctElement octElement1 = (OctElement) element1;
      OctElement octElement2 = (OctElement) element2;

      if(OctConstants.useLazyIncAlgorithm){
        int result = LibraryAccess.isInLazy(octElement1, octElement2);
        if(result == 1) {
          totaltime = totaltime + (System.currentTimeMillis() - start);
          return true;
        }
        else if(result == 2) {
          totaltime = totaltime + (System.currentTimeMillis() - start);
          return false;
        }
        else{
          System.out.println(" Result is--> " + result);
          assert(false);
          return false;
        }
      }
      else{
        if(covers.containsKey(octElement2) && ((HashSet<OctElement>)(covers.get(octElement2))).contains(octElement1)){
          return true;
        }
        
        boolean included = LibraryAccess.isIn(octElement1, octElement2);
        if(included){
          Set<OctElement> s;
          if (covers.containsKey(octElement2)) {
            s = covers.get(octElement2);
          } else {
            s = new HashSet<OctElement>();
          }
          s.add(octElement1);
          covers.put(octElement2, s);
        }
        totaltime = totaltime + (System.currentTimeMillis() - start);
        return included;
      }
    }
  }

  private static class OctJoinOperator implements JoinOperator
  {
    public AbstractElement join (AbstractElement element1, AbstractElement element2)
    {
      // TODO fix
      OctElement octEl1 = (OctElement) element1;
      OctElement octEl2 = (OctElement) element2;
      return LibraryAccess.widening(octEl1, octEl2);
    }
  }

  private final static OctBottomElement bottomElement = new OctBottomElement ();
  private final static OctTopElement topElement = new OctTopElement ();
  private final static PartialOrder partialOrder = new OctPartialOrder ();
  private final static JoinOperator joinOperator = new OctJoinOperator ();

  public OctDomain ()
  {

  }

  public AbstractElement getBottomElement ()
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
