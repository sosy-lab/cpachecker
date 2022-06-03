// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates;

import com.google.common.base.Preconditions;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;
import org.sosy_lab.java_smt.api.BooleanFormula;

/** A generic representation of a predicate */
public class AbstractionPredicate {

  private final Region abstractVariable;
  private final BooleanFormula symbolicVariable;
  private final BooleanFormula symbolicAtom;

  AbstractionPredicate(
      Region pAbstractVariable, BooleanFormula pSymbolicVariable, BooleanFormula pSymbolicAtom) {
    abstractVariable = Preconditions.checkNotNull(pAbstractVariable);
    symbolicVariable = Preconditions.checkNotNull(pSymbolicVariable);
    symbolicAtom = Preconditions.checkNotNull(pSymbolicAtom);
  }

  /**
   * Returns an formula representing this predicate.
   *
   * @return an abstract formula
   */
  public Region getAbstractVariable() {
    return abstractVariable;
  }

  public BooleanFormula getSymbolicVariable() {
    return symbolicVariable;
  }

  public BooleanFormula getSymbolicAtom() {
    return symbolicAtom;
  }

  @Override
  public boolean equals(Object pObj) {
    if (pObj == this) {
      return true;
    } else if (!(pObj instanceof AbstractionPredicate)) {
      return false;
    } else {
      AbstractionPredicate other = (AbstractionPredicate) pObj;
      return abstractVariable.equals(other.abstractVariable);
    }
  }

  @Override
  public int hashCode() {
    return abstractVariable.hashCode();
  }

  @Override
  public String toString() {
    return abstractVariable + " <-> " + symbolicVariable + " <-> " + symbolicAtom;
  }
}
