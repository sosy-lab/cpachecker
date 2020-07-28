// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.acsl;

public class ACSLAssertion implements ACSLAnnotation {

  private final AssertionKind kind;
  private final ACSLPredicate predicate;

  public ACSLAssertion(AssertionKind pKind, ACSLPredicate p) {
    kind = pKind;
    predicate = p.simplify();
  }

  @Override
  public ACSLPredicate getPredicateRepresentation() {
    return predicate;
  }

  @Override
  public ACSLPredicate getCompletenessPredicate() {
    return ACSLPredicate.getTrue();
  }

  @Override
  public String toString() {
    return kind.toString() + ' ' + predicate.toString() + ';';
  }

  public enum AssertionKind {
    ASSERT("assert"),
    CHECK("check");

    private final String name;

    AssertionKind(String pName) {
      name = pName;
    }

    @Override
    public String toString() {
      return name;
    }
  }
}
