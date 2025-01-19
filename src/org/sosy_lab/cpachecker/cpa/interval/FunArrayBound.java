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
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.interval.FunArrayBound.FunArrayNonTailBound;
import org.sosy_lab.cpachecker.cpa.interval.FunArrayBound.FunArrayTailBound;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public abstract sealed class FunArrayBound permits FunArrayTailBound, FunArrayNonTailBound {

  private static final CType CONCRETE_INDEX_TYPE = new CSimpleType(
      false, false, CBasicType.INT, false, false, true, false, false, false, false);

  private final CExpression indexExpression;

  protected FunArrayBound(CExpression pIndexExpression) {
    indexExpression = pIndexExpression;
  }

  protected FunArrayBound(long indexLiteral) {
    this(getLiteralIndexExpression(indexLiteral));
  }

  static FunArrayBound fromList(List<Interval> list, int index) {
    if (index >= list.size()) {
      return new FunArrayTailBound(getLiteralIndexExpression(index));
    } else {
      return new FunArrayNonTailBound(index, list.get(index), fromList(list, index + 1));
    }
  }

  static FunArrayBound fromEmpty(CExpression length) {
    FunArrayTailBound tail = new FunArrayTailBound(length);
    return new FunArrayNonTailBound(0, Interval.UNBOUND, tail);
  }

  abstract String getArrayString();
  abstract Interval arrayAccess(CExpression index, ExpressionValueVisitor visitor) throws UnrecognizedCodeException;

  public CExpression getIndexExpression() {
    return indexExpression;
  }

  private static CExpression getLiteralIndexExpression(long indexLiteral) {
    return CIntegerLiteralExpression.createDummyLiteral(indexLiteral, CONCRETE_INDEX_TYPE);
  }

  final static class FunArrayTailBound extends FunArrayBound{

    FunArrayTailBound(CExpression pIndexExpression) {
      super(pIndexExpression);
    }

    @Override
    String getArrayString() {
      return String.format("{%s}", getIndexExpression());
    }

    @Override
    Interval arrayAccess(CExpression index, ExpressionValueVisitor visitor) throws UnrecognizedCodeException {
      Interval indexInterval = index.accept(visitor);
      Interval boundInterval = getIndexExpression().accept(visitor);
      if (indexInterval.mayBeGreaterOrEqualThan(boundInterval)) {
        return Interval.UNBOUND;
      } else {
        return Interval.EMPTY;
      }
    }
  }

  final static class FunArrayNonTailBound extends FunArrayBound {

    private final Interval succeedingSegmentValue;
    private final FunArrayBound successorBound;

    FunArrayNonTailBound(int boundIndex, Interval pSucceedingSegmentValue, FunArrayBound pSuccessorBound) {
      super(boundIndex);
      this.successorBound = pSuccessorBound;
      this.succeedingSegmentValue = pSucceedingSegmentValue;
    }

    public Interval arrayAccess(CExpression index, ExpressionValueVisitor visitor) throws UnrecognizedCodeException {

      Interval accessIndexValue = index.accept(visitor);
      Interval boundIndexValue = getIndexExpression().accept(visitor);

      if (boundIndexValue.isGreaterThan(accessIndexValue)) {
        return Interval.EMPTY;
      }

      if (accessIndexValue.mayBeGreaterOrEqualThan(boundIndexValue)) {
        return successorBound.arrayAccess(index, visitor).union(succeedingSegmentValue);
      }

      return successorBound.arrayAccess(index, visitor);
    }

    protected String getArrayString() {
      return String.format("{%s} %s %s", getIndexExpression(), succeedingSegmentValue, successorBound.getArrayString());
    }
  }

}
