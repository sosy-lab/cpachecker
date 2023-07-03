// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.traceabstraction;

import static com.google.common.base.Verify.verify;

import java.util.Objects;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;

class IndexedAbstractionPredicate implements Comparable<IndexedAbstractionPredicate> {

  private final int index;
  private final AbstractionPredicate predicate;

  IndexedAbstractionPredicate(final int pIndex, final AbstractionPredicate pPredicate) {
    index = pIndex;
    predicate = pPredicate;
  }

  AbstractionPredicate getPredicate() {
    return predicate;
  }

  @Override
  public int hashCode() {
    return Objects.hash(index, predicate);
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }

    return pOther instanceof IndexedAbstractionPredicate other
        && index == other.index
        && Objects.equals(predicate, other.predicate);
  }

  @Override
  public int compareTo(IndexedAbstractionPredicate pOther) {
    verify(
        index != pOther.index || predicate.equals(pOther.predicate),
        "IndexedAbstractionPredicates that have the same indices should be equal%n"
            + "This: %s%nOther: %s",
        this,
        pOther);
    return Integer.compare(index, pOther.index);
  }

  @Override
  public String toString() {
    return String.format("%d: %s", index, predicate);
  }
}
