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
package org.sosy_lab.cpachecker.cpa.monitor;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperElement;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.conditions.AvoidanceReportingElement;
import org.sosy_lab.cpachecker.util.assumptions.PreventingHeuristic;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;

import com.google.common.base.Preconditions;

public class MonitorElement extends AbstractSingleWrapperElement implements AvoidanceReportingElement {

  static enum TimeoutElement implements AbstractElement {
    INSTANCE;

    @Override
    public String toString() {
      return "Dummy element because computation timed out";
    }
  }

  private final long totalTimeOnPath;

  // stores what caused the element to go further (may be null)
  private final Pair<PreventingHeuristic, Long> preventingCondition;

  protected MonitorElement(AbstractElement pWrappedElement, long totalTimeOnPath) {
    this(pWrappedElement, totalTimeOnPath, null);
  }

  protected MonitorElement(AbstractElement pWrappedElement, long totalTimeOnPath,
      Pair<PreventingHeuristic, Long> preventingCondition) {
    super(pWrappedElement);
    Preconditions.checkArgument(!(pWrappedElement instanceof MonitorElement), "Don't wrap a MonitorCPA in a MonitorCPA, this makes no sense!");
    Preconditions.checkArgument(!(pWrappedElement == TimeoutElement.INSTANCE && preventingCondition == null), "Need a preventingCondition in case of TimeoutElement");
    this.totalTimeOnPath = totalTimeOnPath;
    this.preventingCondition = preventingCondition; // may be null
  }

  public long getTotalTimeOnPath() {
    return totalTimeOnPath;
  }

  @Override
  public boolean equals(Object pObj) {
    if (this == pObj) {
      return true;
    } else if (pObj instanceof MonitorElement) {
      MonitorElement otherElem = (MonitorElement)pObj;
      return this.getWrappedElement().equals(otherElem.getWrappedElement());
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return getWrappedElement().hashCode();
  }

  @Override
  public boolean mustDumpAssumptionForAvoidance() {
    // returns true if the current element is the same as bottom
    return preventingCondition != null;
  }

  Pair<PreventingHeuristic, Long> getPreventingCondition() {
    return preventingCondition;
  }

  @Override
  public String toString() {
    return "Total time: " + this.totalTimeOnPath
    + " Wrapped elem: " + getWrappedElements();
  }


  @Override
  public Formula getReasonFormula(FormulaManager manager) {

    if (mustDumpAssumptionForAvoidance()) {
      return preventingCondition.getFirst().getFormula(manager, preventingCondition.getSecond());
    } else {
      return manager.makeTrue();
    }
  }

}