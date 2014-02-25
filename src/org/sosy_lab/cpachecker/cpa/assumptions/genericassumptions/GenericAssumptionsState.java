/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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

import static com.google.common.collect.FluentIterable.from;

import java.util.List;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.conditions.AssumptionReportingState;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

/**
 * Abstract state for the generic assumption generator CPA;
 * encapsulate a symbolic formula that represents the
 * assumption.
 */
public class GenericAssumptionsState implements AbstractState, AssumptionReportingState {

  // The inner representation is an expression.
  private final ImmutableList<CExpression> assumptions;

  public GenericAssumptionsState(Iterable<CExpression> pAssumptions) {
    assumptions = ImmutableList.copyOf(pAssumptions);
  }

  @Override
  public List<CExpression> getAssumptions() {
    return assumptions;
  }

  @Override
  public boolean equals(Object pObj) {
    if (pObj instanceof GenericAssumptionsState) {
      return assumptions.equals(((GenericAssumptionsState)pObj).assumptions);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return assumptions.hashCode();
  }

  @Override
  public String toString() {
    return Joiner.on(", ").join(from(assumptions).transform(CExpression.TO_AST_STRING));
  }
}
