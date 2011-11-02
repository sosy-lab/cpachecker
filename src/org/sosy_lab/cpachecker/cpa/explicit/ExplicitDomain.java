/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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

public class ExplicitDomain implements AbstractDomain
{
  @Override
  public boolean isLessOrEqual(AbstractElement newElement, AbstractElement reachedElement)
  {
      // returns true if element1 < element2 on lattice
      ExplicitElement ExplicitElementNew = (ExplicitElement) newElement;
      ExplicitElement ExplicitElementReached = (ExplicitElement) reachedElement;

      if(ExplicitElementNew.getPreviousElement() != ExplicitElementReached.getPreviousElement())
        return false;

      Map<String, Long> constantsMapNew = ExplicitElementNew.getConstantsMap();
      Map<String, Long> constantsMapReached = ExplicitElementReached.getConstantsMap();

      // check whether constantsMapReached contains a subset of the mappings of constantsMapNew
      if (constantsMapNew.size() < constantsMapReached.size()) {
        return false;
      }

      for (Map.Entry<String, Long> entry : constantsMapReached.entrySet()) {
        Long newVal = constantsMapNew.get(entry.getKey());

        if (!entry.getValue().equals(newVal)) {
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

      int size = Math.min(constantsMap1.size(), constantsMap2.size());

      Map<String, Long> newConstantsMap = new HashMap<String, Long>(size);
      Map<String, Integer> newReferencesMap = new HashMap<String, Integer>(size);

      newReferencesMap.putAll(referencesMap1);

      for (Map.Entry<String, Long> entry2 : constantsMap2.entrySet()) {
        String key = entry2.getKey();
        Long value1 = constantsMap1.get(key);

        if (value1 != null) {
          // if there is the same variable
          if (value1.equals(entry2.getValue())) {
            newConstantsMap.put(key, value1);
          }

          // update references map
          newReferencesMap.put(key, Math.max(referencesMap1.get(key), referencesMap2.get(key)));

        } else {
          // if the first map does not contain the variable
          newReferencesMap.put(key, referencesMap2.get(key));
        }
      }
      return new ExplicitElement(newConstantsMap, newReferencesMap, ExplicitElement1.getPreviousElement());
  }
}
