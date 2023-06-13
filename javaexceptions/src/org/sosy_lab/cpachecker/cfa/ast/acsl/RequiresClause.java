// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import java.util.Objects;

public class RequiresClause {

  private final ACSLPredicate predicate;

  public RequiresClause(ACSLPredicate acslPredicate) {
    predicate = acslPredicate;
  }

  public ACSLPredicate getPredicate() {
    return predicate;
  }

  public RequiresClause and(RequiresClause other) {
    return new RequiresClause(predicate.and(other.getPredicate()));
  }

  @Override
  public String toString() {
    return "requires " + predicate + ';';
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof RequiresClause)) {
      return false;
    }
    RequiresClause that = (RequiresClause) other;
    return Objects.equals(predicate, that.predicate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(predicate);
  }
}
