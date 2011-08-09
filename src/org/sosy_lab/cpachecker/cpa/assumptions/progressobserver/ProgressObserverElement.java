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
package org.sosy_lab.cpachecker.cpa.assumptions.progressobserver;

import java.util.Iterator;
import java.util.List;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.util.assumptions.AvoidanceReportingElement;
import org.sosy_lab.cpachecker.util.assumptions.HeuristicToFormula;
import org.sosy_lab.cpachecker.util.assumptions.HeuristicToFormula.PreventingHeuristicType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;

import com.google.common.collect.ImmutableList;
/**
 * Class to represent the abstract element of the analysis controller.
 */
public class ProgressObserverElement implements AbstractElement, AvoidanceReportingElement {

  private final ImmutableList<StopHeuristicsData> data;

  ProgressObserverElement(List<StopHeuristicsData> d) {
    data = ImmutableList.copyOf(d);
  }

  public ImmutableList<StopHeuristicsData> getComponents() {
    return data;
  }

  /** Is this element less than the given element? */
  public boolean isLessThan(ProgressObserverElement other)
  {
    Iterator<StopHeuristicsData> it1 = data.iterator();
    Iterator<StopHeuristicsData> it2 = other.data.iterator();

    while (it1.hasNext()) {
      assert it2.hasNext();
      if (!it1.next().isLessThan(it2.next()))
        return false;
    }
    assert !it2.hasNext();

    return true;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;

    if (other instanceof ProgressObserverElement) {
      ProgressObserverElement o = (ProgressObserverElement) other;
      boolean bottom1 = false;
      boolean bottom2 = false;
      boolean mismatch = false;

      Iterator<StopHeuristicsData> it1 = data.iterator();
      Iterator<StopHeuristicsData> it2 = o.data.iterator();
      while (it1.hasNext()) {
        if (!it2.hasNext()) return false;

        StopHeuristicsData d1 = it1.next();
        StopHeuristicsData d2 = it2.next();

        if (!bottom1 && d1.isBottom()) {
          if (bottom2 || d2.isBottom()) {
            return true;
          } else {
            bottom1 = true;
            mismatch = true;
          }
        } else if (!bottom2 && d2.isBottom()) {
          if (bottom1) {
            return true;
          } else {
            bottom2 = true;
            mismatch = true;
          }
        } else {
          if ((!mismatch) && (!d1.equals(d2)))
            mismatch = true;
        }
      }
      if (it2.hasNext()) return false;
      return !mismatch;
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    // TODO add better hash code, this is correct but not good for HashMaps
    return 0;
  }

  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder();
    boolean first = true;
    for (StopHeuristicsData d : data) {
      if (first)
        first = false;
      else
        buffer.append('\n');
      buffer.append(d.getClass().getSimpleName()).append(": ").append(d.toString());
    }
    return buffer.toString();
  }

  @Override
  public boolean mustDumpAssumptionForAvoidance() {
    // returns true if the current element is the same as bottom
    for (StopHeuristicsData d : data) {
      if (d.isBottom())
        return true;
    }

    return false;
  }

  @Override
  public Formula getReasonFormula(FormulaManager manager) {
    if (mustDumpAssumptionForAvoidance()) {

      Formula result = manager.makeFalse();
      for (StopHeuristicsData d : data) {
        if (d.isBottom()) {
          Pair<PreventingHeuristicType, Long> preventingCondition =
            Pair.of(d.getHeuristicType(), d.getThreshold());
          String preventingHeuristicStringFormula = HeuristicToFormula.getFormulaStringForHeuristic(preventingCondition);

          result = manager.makeOr(result, manager.parse(preventingHeuristicStringFormula));
        }
      }
      return result;

    } else {
      return manager.makeTrue();
    }
  }

  public <T extends StopHeuristicsData> T getStopHeuristicDataByType(Class<T> pType){
    for (StopHeuristicsData shd: getComponents()) {
      if (pType.isInstance(shd)) {
        return pType.cast(shd);
      }
    }
    return null;
  }

}
