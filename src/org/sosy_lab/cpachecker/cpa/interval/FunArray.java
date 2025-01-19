// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.interval;

import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class FunArray {

  private final FunArrayBound head;

  public FunArray(FunArrayBound pHead) {
    this.head = pHead;
  }

  public FunArray(List<Interval> intervals) {
    this.head = FunArrayBound.fromList(intervals, 0);
  }

  public FunArray(CExpression length) {
    this.head = FunArrayBound.fromEmpty(length);
  }

  public FunArray() {
    this.head = null;
  }

  public Interval arrayAccess(CExpression index, ExpressionValueVisitor visitor)
      throws UnrecognizedCodeException {
    if (head == null) {
      return Interval.UNBOUND;
    }
    return head.arrayAccess(index, visitor);
  }

  @Override
  public String toString() {
    if (head != null) {
      return head.getArrayString();
    }
    return "⊤";
  }
}
