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

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;

public class IntervalAnalysisElement implements AbstractElement
{
  private Map<String, Interval> intervals;

  private Map<String, Integer> referenceCounts;

  // element from the previous context, used solely for return edges
  private final IntervalAnalysisElement previousElement;

  public IntervalAnalysisElement()
  {
    this(new HashMap<String, Interval>(), new HashMap<String, Integer>(), null);
  }

  public IntervalAnalysisElement(IntervalAnalysisElement previousElement)
  {
    this(new HashMap<String, Interval>(), new HashMap<String, Integer>(), previousElement);
  }

  public IntervalAnalysisElement(Map<String, Interval> intervals, Map<String, Integer> referencesMap, IntervalAnalysisElement previousElement)
  {
    this.intervals        = intervals;

    this.referenceCounts  = referencesMap;

    this.previousElement  = previousElement;
  }

  public Map<String, Interval> getIntervals()
  {
    return intervals;
  }

  public Map<String, Integer> getNoOfReferences()
  {
    return referenceCounts;
  }

  // see ExplicitElement::getValueFor
  public Interval getInterval(String variableName)
  {
    return intervals.get(variableName);
  }

  public IntervalAnalysisElement getPreviousElement()
  {
    return previousElement;
  }

  public boolean contains(String variableName)
  {
    return intervals.containsKey(variableName);
  }

  /**
   * This method assigns an interval to a variable and puts it in the map.
   *
   * @param variableName name of the variable
   * @param interval the interval to be assigned
   * @param pThreshold threshold from property explicitAnalysis.threshold
   * @return this
   */
  // see ExplicitElement::assignConstant
  public IntervalAnalysisElement addInterval(String variableName, Interval interval, int pThreshold)
  {
    // only add the interval if it is not already present
    if(!intervals.containsKey(variableName) || !intervals.get(variableName).equals(interval))
    {
      int referenceCount = (referenceCounts.containsKey(variableName)) ? referenceCounts.get(variableName) : 0;

      if(referenceCount < pThreshold)
      {
        referenceCounts.put(variableName, referenceCount + 1);

        intervals.put(variableName, interval);
      }

      else
        removeInterval(variableName);
    }

    return this;
  }

  /**
   * This method removes the interval for a given variable.
   *
   * @param variableName the name of the variable whose interval should be removed
   * @return this
   */
  // see ExplicitElement::forget
  public IntervalAnalysisElement removeInterval(String variableName)
  {
    if(intervals.containsKey(variableName))
    {
      // TODO: why no reduction of referenceCount?
      intervals.remove(variableName);
    }

    return this;
  }

  @Override
  public IntervalAnalysisElement clone()
  {
    IntervalAnalysisElement newElement = new IntervalAnalysisElement(previousElement);

    // clone the intervals ...
    for(String variableName : intervals.keySet())
      newElement.intervals.put(variableName, intervals.get(variableName).clone());

    // ... and clone the reference count
    for(String variableName : referenceCounts.keySet())
      newElement.referenceCounts.put(variableName, referenceCounts.get(variableName).intValue());

    return newElement;
  }

  @Override
  public boolean equals(Object other)
  {
    if(this == other)
      return true;

    if(other == null || !getClass().equals(other.getClass()))
      return false;

    IntervalAnalysisElement otherElement = (IntervalAnalysisElement)other;

    if(intervals.size() != otherElement.intervals.size())
      return false;

    if(previousElement != otherElement.previousElement)
      return false;

    for(String variableName : intervals.keySet())
    {
      if(!otherElement.intervals.containsKey(variableName) || !otherElement.intervals.get(variableName).equals(intervals.get(variableName)))
        return false;
    }

    return true;
  }

  @Override
  public int hashCode()
  {
    return intervals.hashCode();
  }

  @Override
  public String toString()
  {
    String result = "[";

    for (String key: intervals.keySet())
      result += " <" + key + " = " + intervals.get(key) + " :: " + referenceCounts.get(key) + "> ";

    return result + "] size->  " + intervals.size();
  }
}