// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

public class AssumesClause {
  private final ACSLPredicate predicate;

  public AssumesClause(ACSLPredicate acslPredicate) {
    predicate = acslPredicate;
  }

  public ACSLPredicate getPredicate() {
    return predicate;
  }

  public AssumesClause and(AssumesClause other) {
    return new AssumesClause(predicate.and(other.getPredicate()));
  }

  @Override
  public String toString() {
    return "assumes " + predicate + ';';
  }
}
