// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.annotations;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicate;

public class AcslAssertion extends AAcslAnnotation {

  private final AcslAssertionKind assertionKind;
  private final AcslPredicate predicate;

  AcslAssertion(
      FileLocation pFileLocation, AcslAssertionKind pAssertionKind, AcslPredicate pPredicate) {
    super(pFileLocation);
    assertionKind = pAssertionKind;
    predicate = pPredicate;
  }

  public AcslPredicate getPredicate() {
    return predicate;
  }

  public AcslAssertionKind getAssertionKind() {
    return assertionKind;
  }

  public enum AcslAssertionKind {
    ASSERTION("assert"),
    CHECK("check"),
    ADMIT("admit");

    private final String name;

    AcslAssertionKind(String pName) {
      name = pName;
    }

    @Override
    public String toString() {
      return name;
    }
  }
}
