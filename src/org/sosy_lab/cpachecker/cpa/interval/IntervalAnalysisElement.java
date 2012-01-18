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
package org.sosy_lab.cpachecker.cpa.interval;

import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;

public class IntervalAnalysisElement implements AbstractElement
{
  /**
   * the intervals of the element
   */
  private Map<String, Interval> intervals;

  /**
   * the reference counts of the element
   */
  private Map<String, Integer> referenceCounts;

  /**
   * the element from the previous context, used solely for return edges
   */
  private final IntervalAnalysisElement previousElement;

  /**
   *  This method acts as the default constructor, which initializes the intervals and reference counts to empty maps and the previous element to null.
   */
  public IntervalAnalysisElement()
  {
    this(new HashMap<String, Interval>(), new HashMap<String, Integer>(), null);
  }

  /**
   * This method acts as constructor, which initializes the intervals and reference counts to empty maps and the previous element to the respective object.
   *
   * @param previousElement from the previous context
   */
  public IntervalAnalysisElement(IntervalAnalysisElement previousElement)
  {
    this(new HashMap<String, Interval>(), new HashMap<String, Integer>(), previousElement);
  }

  /**
   * This method acts as constructor, which initializes the intervals, the reference counts and the previous element to the respective objects.
   *
   * @param intervals the intervals
   * @param referencesMap the reference counts
   * @param previousElement from the previous context
   */
  public IntervalAnalysisElement(Map<String, Interval> intervals, Map<String, Integer> referencesMap, IntervalAnalysisElement previousElement)
  {
    this.intervals        = intervals;

    this.referenceCounts  = referencesMap;

    this.previousElement  = previousElement;
  }

  /**
   * This method returns the intervals of a given variable.
   *
   * @param variableName the name of the variable
   * @return the intervals of the variable
   */
  // see ExplicitElement::getValueFor
  public Interval getInterval(String variableName)
  {
    return intervals.get(variableName);
  }

  /**
   * This method returns the reference count for a given variable.
   *
   * @param variableName of the variable to query the reference count on
   * @return the reference count of the variable, or 0 if the the variable is not yet referenced
   */
  private Integer getReferenceCount(String variableName)
  {
    return (referenceCounts.containsKey(variableName)) ? referenceCounts.get(variableName) : 0;
  }

  /**
   * This method determines whether or not the reference count for a given variable exceeds a given threshold.
   *
   * @param variableName the name of the variable
   * @param threshold the threshold
   * @return true, if the reference count of the variable exceeds the given threshold, else false
   */
  @Deprecated
  public boolean exceedsThreshold(String variableName, Integer threshold)
  {
    Integer referenceCount = (referenceCounts.containsKey(variableName)) ? referenceCounts.get(variableName) : 0;

    return referenceCount > threshold;
  }

  /**
   * This method returns the previous element
   *
   * @return the previous element
   */
  public IntervalAnalysisElement getPreviousElement()
  {
    return previousElement;
  }

  /**
   * This method determines if this element contains an interval for a variable.
   *
   * @param variableName the name of the variable
   * @return true, if this element contains an interval for the given variable
   */
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
      int referenceCount = getReferenceCount(variableName);

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
      intervals.remove(variableName);

    return this;
  }

  /**
   * This element joins this element with a reached element.
   *
   * @param reachedElement the reached element to join this element with
   * @return a new element representing the join of this element and the reached element
   */
  public IntervalAnalysisElement join(IntervalAnalysisElement reachedElement)
  {
    Map<String, Interval> newIntervals = new HashMap<String, Interval>();
    Map<String, Integer> newReferences = new HashMap<String, Integer>();

    newReferences.putAll(referenceCounts);

    for(String variableName : reachedElement.intervals.keySet())
    {
      if(intervals.containsKey(variableName))
      {
        // update the interval
        newIntervals.put(variableName, getInterval(variableName).union(reachedElement.getInterval(variableName)));

        // update the references
        newReferences.put(variableName, Math.max(getReferenceCount(variableName), reachedElement.getReferenceCount(variableName)));
      }

      // if the first map does not contain the variable, update the references
      else
        newReferences.put(variableName, reachedElement.getReferenceCount(variableName));
    }

    return new IntervalAnalysisElement(newIntervals, newReferences, previousElement);
  }

  /**
   * This method decides if this element is less or equal than the reached element, based on the order imposed by the lattice.
   *
   * @param reachedElement the reached element
   * @return true, if this element is less or equal than the reached element, based on the order imposed by the lattice
   */
  public boolean isLessOrEqual(IntervalAnalysisElement reachedElement)
  {
    // this element is not less or equal than the reached element, if it contains less intervals
    if(intervals.size() < reachedElement.intervals.size())
      return false;

    // also, this element is not less or equal than the reached element, if any one interval of the reached element is not contained in this element,
    // or if the interval of the reached element is not wider than the respective interval of this element
    for(String variableName : reachedElement.intervals.keySet())
    {
      if(!intervals.containsKey(variableName) || !reachedElement.getInterval(variableName).contains(getInterval(variableName)))
        return false;
    }

    // else, this element < reached element on the lattice
    return true;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#clone()
   */
  @Override
  public IntervalAnalysisElement clone()
  {
    IntervalAnalysisElement newElement = new IntervalAnalysisElement(previousElement);

    // clone the intervals ...
    for(String variableName : intervals.keySet())
      newElement.intervals.put(variableName, getInterval(variableName).clone());

    // ... and clone the reference count
    for(String variableName : referenceCounts.keySet())
      newElement.referenceCounts.put(variableName, getReferenceCount(variableName));

    return newElement;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
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

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode()
  {
    return intervals.hashCode();
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("[\n");

    for (Map.Entry<String, Interval> entry: intervals.entrySet()) {
      String key = entry.getKey();
      sb.append(" <");
      sb.append(key);
      sb.append(" = ");
      sb.append(entry.getValue());
      sb.append(" :: ");
      sb.append(getReferenceCount(key));
      sb.append(">\n");
    }

    return sb.append("] size->  ").append(intervals.size()).toString();
  }
}