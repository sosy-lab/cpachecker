// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.acsl;

public class LoopInvariant {

  private final ACSLPredicate predicate;

  public LoopInvariant(ACSLPredicate p) {
    predicate = p;
  }

  public ACSLPredicate getPredicate() {
    return predicate;
  }

  public LoopInvariant and(ACSLPredicate p) {
    return new LoopInvariant(predicate.and(p));
  }

  @Override
  public String toString() {
    return "loop invariant " + predicate.toString() + ';';
  }
}
