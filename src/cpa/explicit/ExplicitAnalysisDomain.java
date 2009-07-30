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
package cpa.explicit;

import java.util.HashMap;
import java.util.Map;

import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.JoinOperator;
import cpa.common.interfaces.PartialOrder;

public class ExplicitAnalysisDomain implements AbstractDomain {

  private static class ExplicitAnalysisBottomElement extends ExplicitAnalysisElement
  {
    @Override
    public String toString() {
      return "<ExplicitAnalysis BOTTOM>";
    }
  }

  private static class ExplicitAnalysisTopElement extends ExplicitAnalysisElement 
  {
    @Override
    public String toString() {
      return "<ExplicitAnalysis TOP>";
    }
  }

  private static class ExplicitAnalysisPartialOrder implements PartialOrder
  {
    // returns true if element1 < element2 on lattice
    public boolean satisfiesPartialOrder(AbstractElement newElement, AbstractElement reachedElement)
    {
      ExplicitAnalysisElement explicitAnalysisElementNew = (ExplicitAnalysisElement) newElement;
      ExplicitAnalysisElement explicitAnalysisElementReached = (ExplicitAnalysisElement) reachedElement;

      System.out.println("===============");
      System.out.println(explicitAnalysisElementNew);
      System.out.println("---------------");
      System.out.println(explicitAnalysisElementReached);
      System.out.println("===============");
      System.exit(0);
      
      if (explicitAnalysisElementNew == bottomElement) {
        return true;
      } else if (explicitAnalysisElementReached == topElement) {
        return true;
      } else if (explicitAnalysisElementReached == bottomElement) {
        // we should not put this in the reached set
        assert(false);
        return false;
      } else if (explicitAnalysisElementNew == topElement) {
        return false;
      }

      Map<String, Integer> constantsMapNew = explicitAnalysisElementNew.getConstantsMap();
      Map<String, Integer> constantsMapReached = explicitAnalysisElementReached.getConstantsMap();

      if(constantsMapNew.size() < constantsMapReached.size()){
        return false;
      }

      for(String key:constantsMapReached.keySet()){
        if(!constantsMapNew.containsKey(key)){
          return false;
        }
        int val1 = constantsMapNew.get(key).intValue();
        int val2 = constantsMapReached.get(key).intValue();
        if(val1 != val2){
          return false;
        }
      }
      return true;
    }
  }

  private static class ExplicitAnalysisJoinOperator implements JoinOperator
  {
    public AbstractElement join(AbstractElement element1, AbstractElement element2)
    {
      ExplicitAnalysisElement explicitAnalysisElement1 = (ExplicitAnalysisElement) element1;
      ExplicitAnalysisElement explicitAnalysisElement2 = (ExplicitAnalysisElement) element2;

      Map<String, Integer> constantsMap1 = explicitAnalysisElement1.getConstantsMap();
      Map<String, Integer> constantsMap2 = explicitAnalysisElement2.getConstantsMap();

      Map<String, Integer> referencesMap1 = explicitAnalysisElement1.getNoOfReferences();
      Map<String, Integer> referencesMap2 = explicitAnalysisElement2.getNoOfReferences();
      
      Map<String, Integer> newConstantsMap = new HashMap<String, Integer>();
      Map<String, Integer> newReferencesMap = new HashMap<String, Integer>();
      
      for(String key:constantsMap2.keySet()){
        // if there is the same variable
        if(constantsMap1.containsKey(key)){
          // if they have different values
          if(constantsMap1.get(key) != constantsMap2.get(key)){
            newReferencesMap.put(key, Math.max(referencesMap1.get(key), referencesMap2.get(key)));
          }
          // if values are the same
          else{
            newConstantsMap.put(key, constantsMap1.get(key));
           newReferencesMap.put(key, Math.max(referencesMap1.get(key), referencesMap2.get(key)));
          }
        }
        // if there first map does not contain the variable
        else {
          newReferencesMap.put(key, referencesMap2.get(key));
        }
      }

      return new ExplicitAnalysisElement(newConstantsMap, newReferencesMap);
    }
  }

  private final static ExplicitAnalysisBottomElement bottomElement = new ExplicitAnalysisBottomElement ();
  private final static ExplicitAnalysisTopElement topElement = new ExplicitAnalysisTopElement ();
  private final static PartialOrder partialOrder = new ExplicitAnalysisPartialOrder ();
  private final static JoinOperator joinOperator = new ExplicitAnalysisJoinOperator ();

  public ExplicitAnalysisDomain ()
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
