// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.acsl;

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
    return "ensures " + predicate.toString() + ';';
  }
}
