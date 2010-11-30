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
package org.sosy_lab.cpachecker.cpa.interval;

import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;

public class IntervalAnalysisDomain implements AbstractDomain
{
    @Override
    public boolean isLessOrEqual(AbstractElement currentAbstractElement, AbstractElement reachedAbstractElement)
    {
      IntervalAnalysisElement currentElement = (IntervalAnalysisElement) currentAbstractElement;
      IntervalAnalysisElement reachedElement = (IntervalAnalysisElement) reachedAbstractElement;

      Map<String, Interval> currentIntervals = currentElement.getIntervals();
      Map<String, Interval> reachedIntervals = reachedElement.getIntervals();

      if(currentIntervals.size() < reachedIntervals.size())
        return false;

      // the partial order is not satisfied if any key of the reached element's map is missing in the current element's map or the value of a common key differs
      for(String key : reachedIntervals.keySet())
      {
        if(!currentIntervals.containsKey(key) || !reachedIntervals.get(key).contains(currentIntervals.get(key)))
          return false;
      }

      // return true if element1 < element2 on lattice
      return true;
    }

    @Override
    public AbstractElement join(AbstractElement currentAbstractElement, AbstractElement reachedAbstractElement)
    {
      IntervalAnalysisElement currentElement  = (IntervalAnalysisElement) currentAbstractElement;
      IntervalAnalysisElement reachedElement  = (IntervalAnalysisElement) reachedAbstractElement;

      Map<String, Interval> currentIntervals  = currentElement.getIntervals();
      Map<String, Interval> reachedIntervals  = reachedElement.getIntervals();

      Map<String, Integer> currentReferences  = currentElement.getNoOfReferences();
      Map<String, Integer> reachedReferences  = reachedElement.getNoOfReferences();

      Map<String, Interval> newIntervals      = new HashMap<String, Interval>();
      Map<String, Integer> newReferences      = new HashMap<String, Integer>();

      newReferences.putAll(currentReferences);

      for(String key : reachedIntervals.keySet())
      {
        if(currentIntervals.containsKey(key))
        {
          Interval currentInterval = currentIntervals.get(key);
          Interval reachedInterval = reachedIntervals.get(key);
//System.out.println("current key: " + key);
//System.out.println("currentInterval: " + currentInterval);
//System.out.println("reachedInterval: " + reachedInterval);
          Interval union = currentInterval.union(reachedInterval);
//System.out.println("union: " + union);
//System.out.println();
          newIntervals.put(key, union);

          // update the references
          newReferences.put(key, Math.max(currentReferences.get(key), reachedReferences.get(key)));
        }

        // if the first map does not contain the variable, update the references
        else
          newReferences.put(key, reachedReferences.get(key));
      }

      return new IntervalAnalysisElement(newIntervals, newReferences, currentElement.getPreviousElement());
    }

}
