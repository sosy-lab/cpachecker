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
package org.sosy_lab.cpachecker.cpa.assumptions.storage;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;

import com.google.common.base.Preconditions;

/**
 * Abstract element for the Collector CPA. Encapsulate a
 * symbolic formula
 */
public class AssumptionStorageState implements AbstractState {

  // this formula provides the assumption generated from other sources than heuristics,
  // e.g. assumptions for arithmetic overflow
  private final Formula assumption;

  // if a heuristic told us to stop the analysis, this formula provides the reason
  // if it is TRUE, there is no reason -> don't stop
  private final Formula stopFormula;

  // the assumption represented by this class is always the conjunction of "assumption" and "stopFormula"

  public AssumptionStorageState(Formula pAssumption, Formula pStopFormula) {
    assumption = Preconditions.checkNotNull(pAssumption);
    stopFormula = Preconditions.checkNotNull(pStopFormula);

    assert !assumption.isFalse(); // FALSE would mean "stop the analysis", but this should be signaled by stopFormula
  }

  public Formula getAssumption() {
    return assumption;
  }

  public Formula getStopFormula() {
    return stopFormula;
  }

  @Override
  public String toString() {
    return (stopFormula.isTrue() ? "" : "<STOP> ") + "assume: (" + assumption + " & " + stopFormula + ")";
  }

  public boolean isStop() {
    return !stopFormula.isTrue();
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof AssumptionStorageState) {
      AssumptionStorageState otherElement = (AssumptionStorageState) other;
      return assumption.equals(otherElement.assumption)
          && stopFormula.equals(otherElement.stopFormula);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return assumption.hashCode() + 17 * stopFormula.hashCode();
  }
}
