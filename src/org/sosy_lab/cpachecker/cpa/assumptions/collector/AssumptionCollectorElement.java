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
package org.sosy_lab.cpachecker.cpa.assumptions.collector;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormula;

import com.google.common.base.Preconditions;

/**
 * Abstract element for the Collector CPA. Encapsulate a
 * symbolic formula
 *
 * @author g.theoduloz
 */
public class AssumptionCollectorElement implements AbstractElement {
  
  private final SymbolicFormula assumption;

  public AssumptionCollectorElement(SymbolicFormula f) {
    assumption = Preconditions.checkNotNull(f);
  }

  /**
   * Return the invariant in this state.
   */
  public SymbolicFormula getCollectedAssumption() {
    return assumption;
  }

  @Override
  public String toString() {
    return (assumption.isFalse() ? "<STOP> " : "") + "assume: " + assumption;
  }

  public boolean isStop() {
    return assumption.isFalse();
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof AssumptionCollectorElement) {
      AssumptionCollectorElement otherElement = (AssumptionCollectorElement) other;
      return assumption.equals(otherElement.assumption);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return assumption.hashCode();
  }
}
