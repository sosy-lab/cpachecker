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
package org.sosy_lab.cpachecker.cpa.assumptions.genericassumptions;

import org.sosy_lab.cpachecker.cfa.ast.IASTExpression;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.conditions.AssumptionReportingState;

import com.google.common.base.Preconditions;

/**
 * Abstract element for the generic assumption generator CPA;
 * encapsulate a symbolic formula that represents the
 * assumption.
 */
public class GenericAssumptionsState implements AbstractState, AssumptionReportingState {

  // The inner representation is an expression.
  private final IASTExpression assumption;

  public GenericAssumptionsState(IASTExpression anAssumption)
  {
    Preconditions.checkNotNull(anAssumption);
    assumption = anAssumption;
  }

  @Override
  public IASTExpression getAssumption()
  {
    return assumption;
  }

  @Override
  public boolean equals(Object pObj) {
    if (pObj instanceof GenericAssumptionsState) {
      return assumption.equals(((GenericAssumptionsState)pObj).assumption);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return assumption.hashCode();
  }

  @Override
  public String toString() {
    return assumption.toASTString();
  }
}
