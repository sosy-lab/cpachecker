// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

import java.io.Serial;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class CArrayRangeDesignator extends CDesignator {

  @Serial private static final long serialVersionUID = -2956484289176841585L;
  private final AExpression rangeFloor;
  private final AExpression rangeCeiling;

  public CArrayRangeDesignator(
      final FileLocation pFileLocation,
      final CExpression pRangeFloor,
      final CExpression pRangeCeiling) {
    super(pFileLocation);
    rangeFloor = pRangeFloor;
    rangeCeiling = pRangeCeiling;
  }

  public CExpression getFloorExpression() {
    return (CExpression) rangeFloor;
  }

  public CExpression getCeilExpression() {
    return (CExpression) rangeCeiling;
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "["
        + rangeFloor.toASTString(pAAstNodeRepresentation)
        + " ... "
        + rangeCeiling.toASTString(pAAstNodeRepresentation)
        + "]";
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return toASTString(pAAstNodeRepresentation);
  }

  @Override
  public <R, X extends Exception> R accept(CDesignatorVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(CAstNodeVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(rangeCeiling);
    result = prime * result + Objects.hashCode(rangeFloor);
    result = prime * result + super.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof CArrayRangeDesignator other
        && super.equals(obj)
        && Objects.equals(other.rangeCeiling, rangeCeiling)
        && Objects.equals(other.rangeFloor, rangeFloor);
  }
}
