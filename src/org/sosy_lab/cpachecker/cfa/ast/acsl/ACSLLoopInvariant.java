// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

public class ACSLLoopInvariant {

  private final ACSLPredicate predicate;

  public ACSLLoopInvariant(ACSLPredicate p) {
    predicate = p;
  }

  public ACSLPredicate getPredicate() {
    return predicate;
  }

  public ACSLLoopInvariant and(ACSLPredicate p) {
    return new ACSLLoopInvariant(predicate.and(p));
  }

  @Override
  public String toString() {
    return "loop invariant " + predicate + ';';
  }
}
