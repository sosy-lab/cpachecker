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
import org.sosy_lab.cpachecker.core.interfaces.JoinOperator;
import org.sosy_lab.cpachecker.core.interfaces.PartialOrder;

public class ExplicitAnalysisDomain implements AbstractDomain {

  private static class ExplicitAnalysisBottomElement extends ExplicitAnalysisElement
  {
    @Override
    public String toString() {
      return "<ExplicitAnalysis BOTTOM>";
    }
    
    @Override
    public boolean equals(Object pOther) {
      if (this == pOther) {
        return true;
      }
      
      if (pOther == null) {
        return false;
      }
      
      return (getClass().equals(pOther.getClass()));
    }
    
    @Override
    public int hashCode() {
      return Integer.MIN_VALUE;
    }
  }

  private static class ExplicitAnalysisTopElement extends ExplicitAnalysisElement
  {
    @Override
    public String toString() {
      return "<ExplicitAnalysis TOP>";
    }
    
    @Override
    public boolean equals(Object pOther) {
      if (this == pOther) {
        return true;
      }
      
      if (pOther == null) {
        return false;
      }
      
      return (getClass().equals(pOther.getClass()));
    }
    
    @Override
    public int hashCode() {
      return Integer.MAX_VALUE;
    }
  }

  private static class ExplicitAnalysisPartialOrder implements PartialOrder
  {
    // returns true if element1 < element2 on lattice
    @Override
    public boolean satisfiesPartialOrder(AbstractElement newElement, AbstractElement reachedElement)
    {
      ExplicitAnalysisElement explicitAnalysisElementNew = (ExplicitAnalysisElement) newElement;
      ExplicitAnalysisElement explicitAnalysisElementReached = (ExplicitAnalysisElement) reachedElement;

//      ("===============");
//      System.out.println(explicitAnalysisElementNew);
//      System.out.println("---------------");
//      SystemSystem.out.println.out.println(explicitAnalysisElementReached);
//      System.out.println("===============");
//      System.exit(0);

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
  }

  private static class ExplicitAnalysisJoinOperator implements JoinOperator
  {
    @Override
    public AbstractElement join(AbstractElement element1, AbstractElement element2)
    {
      ExplicitAnalysisElement explicitAnalysisElement1 = (ExplicitAnalysisElement) element1;
      ExplicitAnalysisElement explicitAnalysisElement2 = (ExplicitAnalysisElement) element2;

      Map<String, Long> constantsMap1 = explicitAnalysisElement1.getConstantsMap();
      Map<String, Long> constantsMap2 = explicitAnalysisElement2.getConstantsMap();

      Map<String, Integer> referencesMap1 = explicitAnalysisElement1.getNoOfReferences();
      Map<String, Integer> referencesMap2 = explicitAnalysisElement2.getNoOfReferences();

      Map<String, Long> newConstantsMap = new HashMap<String, Long>();
      Map<String, Integer> newReferencesMap = new HashMap<String, Integer>();

      newReferencesMap.putAll(referencesMap1);

      for(String key:constantsMap2.keySet()){
        // if there is the same variable
        if(constantsMap1.containsKey(key)){
          // if they have same values, set the value to it
          if(constantsMap1.get(key) == constantsMap2.get(key)){
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
      return new ExplicitAnalysisElement(newConstantsMap, newReferencesMap, explicitAnalysisElement2.getPreviousElement());
    }
  }

  private final static ExplicitAnalysisBottomElement bottomElement = new ExplicitAnalysisBottomElement ();
  private final static ExplicitAnalysisTopElement topElement = new ExplicitAnalysisTopElement ();
  private final static PartialOrder partialOrder = new ExplicitAnalysisPartialOrder ();
  private final static JoinOperator joinOperator = new ExplicitAnalysisJoinOperator ();

  public ExplicitAnalysisDomain ()
  {

  }

  @Override
  public ExplicitAnalysisElement getBottomElement ()
  {
    return bottomElement;
  }

  @Override
  public ExplicitAnalysisElement getTopElement ()
  {
    return topElement;
  }

  @Override
  public JoinOperator getJoinOperator ()
  {
    return joinOperator;
  }

  @Override
  public PartialOrder getPartialOrder ()
  {
    return partialOrder;
  }
}
