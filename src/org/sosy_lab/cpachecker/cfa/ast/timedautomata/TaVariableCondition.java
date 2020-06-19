// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
// 
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
// 
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.timedautomata;

import com.google.common.collect.ImmutableList;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

public class TaVariableCondition implements Serializable{
  private static final long serialVersionUID = -8941965731007766754L;
  private final List<TaVariableExpression> expressions;

  /** Creates a condition that represents a conjunction of expressions. */
  public TaVariableCondition(List<TaVariableExpression> pExpressions) {
    expressions = pExpressions;
  }

  public List<TaVariableExpression> getExpressions() {
    return ImmutableList.copyOf(expressions);
  }

  @Override
  public String toString() {
    return String.join(
        " AND ",
        expressions.stream().map(TaVariableExpression::toString).collect(Collectors.toSet()));
  }

}
