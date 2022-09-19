// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.assumptions.genericassumptions;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.conditions.AssumptionReportingState;

/**
 * Abstract state for the generic assumption generator CPA; encapsulate a symbolic formula that
 * represents the assumption.
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
      return assumptions.equals(((GenericAssumptionsState) pObj).assumptions);
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
    return from(assumptions).transform(CExpression::toASTString).join(Joiner.on(", "));
  }
}
