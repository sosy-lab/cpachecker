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
package org.sosy_lab.cpachecker.cpa.explicit;

import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;

public class ExplicitDomain implements AbstractDomain {

  @Override
  public boolean isLessOrEqual(AbstractElement newElement, AbstractElement reachedElement) {
      // returns true if element1 < element2 on lattice
      ExplicitElement ExplicitElementNew = (ExplicitElement) newElement;
      ExplicitElement ExplicitElementReached = (ExplicitElement) reachedElement;

//      ("===============");
//      System.out.println(ExplicitElementNew);
//      System.out.println("---------------");
//      SystemSystem.out.println.out.println(ExplicitElementReached);
//      System.out.println("===============");
//      System.exit(0);

      Map<String, Long> constantsMapNew = ExplicitElementNew.getConstantsMap();
      Map<String, Long> constantsMapReached = ExplicitElementReached.getConstantsMap();

      if(constantsMapNew.size() < constantsMapReached.size()){
        return false;
      }

      for(String key:constantsMapReached.keySet()){
        if(!constantsMapNew.containsKey(key)){
          return false;
        }
        long val1 = constantsMapNew.get(key).longValue();
        long val2 = constantsMapReached.get(key).longValue();
        if(val1 != val2){
          return false;
        }
      }
      return true;
  }

  @Override
  public AbstractElement join(AbstractElement element1, AbstractElement element2) {
      ExplicitElement ExplicitElement1 = (ExplicitElement) element1;
      ExplicitElement ExplicitElement2 = (ExplicitElement) element2;

      Map<String, Long> constantsMap1 = ExplicitElement1.getConstantsMap();
      Map<String, Long> constantsMap2 = ExplicitElement2.getConstantsMap();

      Map<String, Integer> referencesMap1 = ExplicitElement1.getNoOfReferences();
      Map<String, Integer> referencesMap2 = ExplicitElement2.getNoOfReferences();

      Map<String, Long> newConstantsMap = new HashMap<String, Long>();
      Map<String, Integer> newReferencesMap = new HashMap<String, Integer>();

      newReferencesMap.putAll(referencesMap1);

      for(String key:constantsMap2.keySet()){
        // if there is the same variable
        if(constantsMap1.containsKey(key)){
          // if they have same values, set the value to it
          if(constantsMap1.get(key).equals(constantsMap2.get(key))){
            newConstantsMap.put(key, constantsMap1.get(key));
          }
          // update references map
          newReferencesMap.put(key, Math.max(referencesMap1.get(key), referencesMap2.get(key)));
        }
        // if the first map does not contain the variable
        else {
          newReferencesMap.put(key, referencesMap2.get(key));
        }
      }
      return new ExplicitElement(newConstantsMap, newReferencesMap, ExplicitElement1.getPreviousElement());
  }
}
