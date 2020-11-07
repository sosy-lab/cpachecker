// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.acsl;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class Old extends At {

  public Old(Identifier identifier) {
    super(identifier, ACSLDefaultLabel.OLD);
  }

  @Override
  public String toString() {
    return "\\old(" + getInner().toString() + ")";
  }

  @Override
  public CExpression accept(ACSLTermToCExpressionVisitor visitor) throws UnrecognizedCodeException {
    return visitor.visit(this);
  }

  @Override
  public boolean isAllowedIn(Class<?> clauseType) {
    return clauseType.equals(EnsuresClause.class) && getInner().isAllowedIn(clauseType);
  }
}
