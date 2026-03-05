// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import com.google.common.collect.ImmutableList;
import java.io.Serial;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.Type;

public final class AcslPredicateApplicationPredicate implements AcslPredicate {

  private final FileLocation fileLocation;
  private final String predicate;
  private final ImmutableList<AcslTerm> parameters;

  public AcslPredicateApplicationPredicate(
      FileLocation pFileLocation, String pIdPredicate, List<AcslTerm> pParameters) {
    predicate = pIdPredicate;
    parameters = ImmutableList.copyOf(pParameters);
    fileLocation = pFileLocation;
  }

  @Serial private static final long serialVersionUID = 448373275534775671L;

  @Override
  public <R, X extends Exception> R accept(AcslPredicateVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public Type getExpressionType() {
    return AcslBuiltinLogicType.BOOLEAN;
  }

  @Override
  public <R, X extends Exception> R accept(AcslAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public FileLocation getFileLocation() {
    return fileLocation;
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    StringBuilder astString = new StringBuilder(predicate + "(");
    for (AcslTerm p : parameters) {
      astString.append(p);
      for (int i = 1; i < parameters.size(); i++) {
        astString.append(", ");
      }
    }
    astString.append(")");
    return astString.toString();
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "(" + toASTString(pAAstNodeRepresentation) + ")";
  }
}
