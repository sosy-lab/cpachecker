// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import com.google.common.base.Joiner;
import java.util.List;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.Type;

public abstract sealed class AcslQuantifiedPredicate implements AcslPredicate
    permits AcslExistsPredicate, AcslForallPredicate {

  private final FileLocation fileLocation;
  private final List<AcslParameterDeclaration> binders;
  private final AcslPredicate predicate;

  AcslQuantifiedPredicate(
      FileLocation pFileLocation,
      List<AcslParameterDeclaration> pBinders,
      AcslPredicate pPredicate) {
    fileLocation = pFileLocation;
    binders = pBinders;
    predicate = pPredicate;
  }

  public List<AcslParameterDeclaration> getBinders() {
    return binders;
  }

  public AcslPredicate getPredicate() {
    return predicate;
  }

  @Override
  public Type getExpressionType() {
    return AcslBuiltinLogicType.BOOLEAN;
  }

  @Override
  public FileLocation getFileLocation() {
    return fileLocation;
  }

  String toString(String quantifier) {
    StringBuilder sb = new StringBuilder();
    sb.append(quantifier);
    sb.append(" ");
    sb.append(Joiner.on(", ").join(binders));
    sb.append(": ");
    sb.append(predicate);
    return sb.toString();
  }

  @Override
  public int hashCode() {
    final int prime = 3;
    int result = 7;
    result = prime * result + Objects.hash(fileLocation);
    result = prime * result + Objects.hash(binders);
    result = prime * result + Objects.hash(predicate);
    result = prime * result + super.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof AcslQuantifiedPredicate other
        && Objects.equals(other.fileLocation, fileLocation)
        && Objects.equals(other.binders, binders)
        && Objects.equals(other.predicate, predicate);
  }
}
