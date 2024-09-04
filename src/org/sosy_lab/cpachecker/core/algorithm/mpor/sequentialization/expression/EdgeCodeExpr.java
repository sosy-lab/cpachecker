// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

public class EdgeCodeExpr implements SeqExpression {

  private final CFAEdge edge;

  public EdgeCodeExpr(CFAEdge pEdge) {
    edge = pEdge;
  }

  @Override
  public String createString() {
    return edge.getCode();
  }
}
