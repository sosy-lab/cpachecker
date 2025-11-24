// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.Type;

public final class AcslAssertionPredicate implements AcslPredicate {
  @Serial private static final long serialVersionUID = -6901608374567258698L;
  private final FileLocation fileLocation;
  private final AcslPredicate acslPredicate;
  private final AcslAssertionKind assertionKind;

  public AcslAssertionPredicate(
      FileLocation pFileLocation, AcslPredicate pAcslPredicate, AcslAssertionKind pAssertionKind) {
    fileLocation = pFileLocation;
    acslPredicate = pAcslPredicate;
    assertionKind = pAssertionKind;
  }

  public enum AcslAssertionKind {
    ASSERTION("assert"),
    CHECK("check"),
    ADMIT("admit");

    private final String assertionKind;

    AcslAssertionKind(String pAssertionKind) {
      assertionKind = pAssertionKind;
    }
  }

  @Override
  public <R, X extends Exception> R accept(AcslPredicateVisitor<R, X> v) throws X {
    return null;
  }

  @Override
  public Type getExpressionType() {
    return null;
  }

  @Override
  public <R, X extends Exception> R accept(AcslAstNodeVisitor<R, X> v) throws X {
    return null;
  }

  @Override
  public FileLocation getFileLocation() {
    return null;
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "";
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "";
  }
}
