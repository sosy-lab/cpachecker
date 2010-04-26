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
package org.sosy_lab.cpachecker.cpa.assumptions.collector.progressobserver;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.sosy_lab.cpachecker.util.assumptions.AvoidanceReportingElement;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;

/**
 * Class to represent the abstract element of the analysis controller.
 * @author g.theoduloz
 */
public class ProgressObserverElement implements AbstractElement, AvoidanceReportingElement {

  private final List<StopHeuristicsData> data;

  /** Bottom */
  public static final ProgressObserverElement getBottom(ProgressObserverCPA cpa)
  {
    ArrayList<StopHeuristicsData> data = new ArrayList<StopHeuristicsData>(cpa.getEnabledHeuristics().size());
    for (StopHeuristics<? extends StopHeuristicsData> h : cpa.getEnabledHeuristics()) {
      data.add(h.getBottom());
    }
    return new ProgressObserverElement(data);
  }

  /** Top */
  public static final ProgressObserverElement getTop(ProgressObserverCPA cpa)
  {
    ArrayList<StopHeuristicsData> data = new ArrayList<StopHeuristicsData>(cpa.getEnabledHeuristics().size());
    for (StopHeuristics<? extends StopHeuristicsData> h : cpa.getEnabledHeuristics()) {
      data.add(h.getTop());
    }
    return new ProgressObserverElement(data);
  }

  /** Initial */
  public static final ProgressObserverElement getInitial(ProgressObserverCPA cpa, CFANode node)
  {
    return new ProgressObserverElement(cpa, node);
  }

  @Override
  public boolean isError() {
    return false;
  }

  public ProgressObserverElement(ProgressObserverCPA a, CFANode node)
  {
    this(new ArrayList<StopHeuristicsData>(a.getEnabledHeuristics().size()));

    for (StopHeuristics<? extends StopHeuristicsData> h : a.getEnabledHeuristics()) {
      data.add(h.getInitialData(node));
    }
  }

  ProgressObserverElement(List<StopHeuristicsData> d)
  {
    data = d;
  }

  public List<StopHeuristicsData> getComponents()
  {
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
  public String toString() {
    StringBuffer buffer = new StringBuffer();
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

}
