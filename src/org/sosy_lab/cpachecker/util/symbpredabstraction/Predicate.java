/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.symbpredabstraction;

import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.AbstractFormula;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormula;

import com.google.common.base.Preconditions;

/**
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 *
 * A generic representation of a predicate
 */
public class Predicate {

  private final AbstractFormula abstractVariable;
  private final SymbolicFormula symbolicVariable;
  private final SymbolicFormula symbolicAtom;

  public Predicate(AbstractFormula pAbstractVariable,
      SymbolicFormula pSymbolicVariable, SymbolicFormula pSymbolicAtom) {
    abstractVariable = Preconditions.checkNotNull(pAbstractVariable);
    symbolicVariable = Preconditions.checkNotNull(pSymbolicVariable);
    symbolicAtom = Preconditions.checkNotNull(pSymbolicAtom);
  }

  /**
   * Returns an formula representing this predicate.
   * @return an abstract formula
   */
  public AbstractFormula getAbstractVariable() {
    return abstractVariable;
  }
  
  public SymbolicFormula getSymbolicVariable() {
    return symbolicVariable;
  }
  
  public SymbolicFormula getSymbolicAtom() {
    return symbolicAtom;
  }
  
  @Override
  public boolean equals(Object pObj) {
    if (pObj == this) {
      return true;
    } else if (!(pObj instanceof Predicate)) {
      return false;
    } else {
      Predicate other = (Predicate)pObj;
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
}
