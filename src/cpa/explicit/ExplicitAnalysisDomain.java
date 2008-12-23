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
  }

  private static class ExplicitAnalysisTopElement extends ExplicitAnalysisElement 
  {
  }

  private static class ExplicitAnalysisPartialOrder implements PartialOrder
  {
    // returns true if element1 < element2 on lattice
    public boolean satisfiesPartialOrder(AbstractElement element1, AbstractElement element2)
    {
      ExplicitAnalysisElement explicitAnalysisElement1 = (ExplicitAnalysisElement) element1;
      ExplicitAnalysisElement explicitAnalysisElement2 = (ExplicitAnalysisElement) element2;

      Map<String, Integer> constantsMap1 = explicitAnalysisElement1.getConstantsMap();
      Map<String, Integer> constantsMap2 = explicitAnalysisElement2.getConstantsMap();

      if(constantsMap1.size() > constantsMap2.size()){
        return false;
      }

      for(String key:constantsMap2.keySet()){
        if(!constantsMap1.containsKey(key) | 
            constantsMap1.get(key) != constantsMap2.get(key)){
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

      Map<String, Integer> newMap = new HashMap<String, Integer>();

      for(String key:constantsMap1.keySet()){
        newMap.put(key, constantsMap1.get(key));
      }

      for(String key:constantsMap2.keySet()){
        if(newMap.containsKey(key)){
          if(newMap.get(key) != constantsMap2.get(key)){
            newMap.remove(key);
          }
        }
        else {
          newMap.put(key, constantsMap2.get(key));
        }
      }
      return new ExplicitAnalysisElement(newMap);
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

  @Override
  public boolean isBottomElement(AbstractElement pElement) {
    return ((ExplicitAnalysisElement)pElement).isBottom();
  }
}
