/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.util.predicates;

import org.sosy_lab.cpachecker.util.predicates.regions.Region;
import org.sosy_lab.java_smt.api.BooleanFormula;

import com.google.common.base.Preconditions;

/**
 * A generic representation of a predicate
 */
public class AbstractionPredicate {

  private final Region abstractVariable;
  private final BooleanFormula symbolicVariable;
  private final BooleanFormula symbolicAtom;
  private final int variableNumber;

  AbstractionPredicate(Region pAbstractVariable,
      BooleanFormula pSymbolicVariable, BooleanFormula pSymbolicAtom,
      int variableNumber) {
    abstractVariable = Preconditions.checkNotNull(pAbstractVariable);
    symbolicVariable = Preconditions.checkNotNull(pSymbolicVariable);
    symbolicAtom = Preconditions.checkNotNull(pSymbolicAtom);
    this.variableNumber = variableNumber;
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
      AbstractionPredicate other = (AbstractionPredicate)pObj;
      return this.abstractVariable.equals(other.abstractVariable);
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

  public int getVariableNumber() {
    return variableNumber;
  }
}
