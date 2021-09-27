// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.traceabstraction;

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

    if (!(pOther instanceof IndexedAbstractionPredicate)) {
      return false;
    }

    IndexedAbstractionPredicate other = (IndexedAbstractionPredicate) pOther;
    return index == other.index && Objects.equals(predicate, other.predicate);
  }

  @Override
  public int compareTo(IndexedAbstractionPredicate pOther) {
    int result = Integer.compare(index, pOther.index);
    if (result == 0 && !predicate.equals(pOther.predicate)) {
      // The index is in principle the only significant value that should be checked for the
      // ordering.
      // However, if the index is equal, the contained predicate needs to be additionally checked
      // since otherwise objects of this class with the same index get wrongly replaced by objects
      // with different predicates. It makes no sense to order predicates in case of inequality, but
      // we still need to ensure that they fulfill the contract of proper comparable-implementations.
      return predicate
          .getAbstractVariable()
          .toString()
          .compareTo(pOther.predicate.getAbstractVariable().toString());
    }

    return result;
  }

  @Override
  public String toString() {
    return String.format("%d: %s", index, predicate);
  }
}
