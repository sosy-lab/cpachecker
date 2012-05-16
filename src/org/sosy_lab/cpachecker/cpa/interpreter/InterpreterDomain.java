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
package org.sosy_lab.cpachecker.cpa.interpreter;

import java.util.Map;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;

public class InterpreterDomain implements AbstractDomain {

  @Override
  public boolean isLessOrEqual(AbstractElement newElement, AbstractElement reachedElement) {
      // returns true if element1 < element2 on lattice
      InterpreterElement explicitAnalysisElementNew = (InterpreterElement) newElement;
      InterpreterElement explicitAnalysisElementReached = (InterpreterElement) reachedElement;

      Map<String, Long> constantsMapNew = explicitAnalysisElementNew.getConstantsMap();
      Map<String, Long> constantsMapReached = explicitAnalysisElementReached.getConstantsMap();

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
  public AbstractElement join(AbstractElement pElement1, AbstractElement pElement2) {
      /*InterpreterElement explicitAnalysisElement1 = (InterpreterElement) element1;
      InterpreterElement explicitAnalysisElement2 = (InterpreterElement) element2;

      Map<String, Long> constantsMap1 = explicitAnalysisElement1.getConstantsMap();
      Map<String, Long> constantsMap2 = explicitAnalysisElement2.getConstantsMap();

      Map<String, Long> newConstantsMap = new HashMap<String, Long>();

      for(String key:constantsMap2.keySet()){
        // if there is the same variable
        if(constantsMap1.containsKey(key)){
          // if they have same values, set the value to it
          if(constantsMap1.get(key) == constantsMap2.get(key)){
            newConstantsMap.put(key, constantsMap1.get(key));
          }
        }
      }
      return new InterpreterElement(newConstantsMap, explicitAnalysisElement2.getPreviousElement());*/

      throw new UnsupportedOperationException();
  }
}
