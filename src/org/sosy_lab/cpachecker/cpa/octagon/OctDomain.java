/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.octagon.Octagon;
import org.sosy_lab.cpachecker.util.octagon.OctagonManager;

import com.google.common.collect.BiMap;

class OctDomain implements AbstractDomain{

  static long totaltime = 0;

  @Override
  public boolean isLessOrEqual(AbstractState element1, AbstractState element2) {

    Map<OctElement, Set<OctElement>> covers = new HashMap<OctElement, Set<OctElement>>();

    long start = System.currentTimeMillis();
    OctElement octElement1 = (OctElement) element1;
    OctElement octElement2 = (OctElement) element2;

    if(covers.containsKey(octElement2) && ((HashSet<OctElement>)(covers.get(octElement2))).contains(octElement1)){
      return true;
    }

    int result = OctagonManager.isIncludedInLazy(octElement1.getOctagon(), octElement2.getOctagon());
    if(result == 1) {
      totaltime = totaltime + (System.currentTimeMillis() - start);
      return true;
    }
    else if(result == 2) {
      totaltime = totaltime + (System.currentTimeMillis() - start);
      return false;
    }
    else{
      assert(result == 3);
      boolean included = OctagonManager.isIncludedIn(octElement1.getOctagon(), octElement2.getOctagon());
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

  @Override
  public AbstractState join(AbstractState element1, AbstractState element2) {
    OctElement octEl1 = (OctElement) element1;
    OctElement octEl2 = (OctElement) element2;
    Octagon newOctagon = OctagonManager.union(octEl1.getOctagon(), octEl2.getOctagon());
    BiMap<String, Integer> newMap =
      octEl1.sizeOfVariables() > octEl2.sizeOfVariables()? octEl1.getVariableToIndexMap() : octEl2.getVariableToIndexMap();

      // TODO should it be null
      return new OctElement(newOctagon, newMap, null);
      // TODO add widening
      //    return LibraryAccess.widening(octEl1, octEl2);
  }
}
