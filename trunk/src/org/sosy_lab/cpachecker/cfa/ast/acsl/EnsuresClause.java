// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import java.util.Objects;

public class EnsuresClause {

  private final ACSLPredicate predicate;

  public EnsuresClause(ACSLPredicate acslPredicate) {
    predicate = acslPredicate;
  }

  public ACSLPredicate getPredicate() {
    return predicate;
  }

  public EnsuresClause and(EnsuresClause other) {
    return new EnsuresClause(predicate.and(other.getPredicate()));
  }

  @Override
  public String toString() {
    return "ensures " + predicate + ';';
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    return other instanceof EnsuresClause that && Objects.equals(predicate, that.predicate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(predicate);
  }
}
