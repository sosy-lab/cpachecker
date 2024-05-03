// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.constraint;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.cpa.smg2.SMGState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicExpression;

public class SymbolicExpressionAndSMGState {

  private final SymbolicExpression symExpr;

  private final SMGState state;

  private SymbolicExpressionAndSMGState(SymbolicExpression pSymExpr, SMGState pState) {
    checkNotNull(pSymExpr);
    checkNotNull(pState);
    symExpr = pSymExpr;
    state = pState;
  }

  public static SymbolicExpressionAndSMGState of(SymbolicExpression pSymExpr, SMGState pState) {
    return new SymbolicExpressionAndSMGState(pSymExpr, pState);
  }

  public SymbolicExpression getSymbolicExpression() {
    return symExpr;
  }

  public SMGState getState() {
    return state;
  }
}
