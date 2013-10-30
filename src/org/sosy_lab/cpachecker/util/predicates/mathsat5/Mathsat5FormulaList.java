/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.mathsat5;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;

import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaList;

public class Mathsat5FormulaList implements FormulaList, Serializable {

  private static final long serialVersionUID = -6546625578464890862L;
  private transient long[] terms;

  /**
   * Do not modify the terms array afterwards, for performance reasons it's not copied!
   */
  Mathsat5FormulaList(long... terms) {
    for (long t : terms) {
      assert t != 0;
    }
    this.terms = terms;
  }

  /**
   * Do not modify the returned array, for performance reasons it's not copied!
   */
  long[] getTerms() {
    return terms;
  }

  @Override
  public String toString() {
    return Arrays.toString(terms);
  }

  @Override
  public boolean equals(Object pObj) {
    if (!(pObj instanceof Mathsat5FormulaList)) {
      return false;
    }
    return Arrays.equals(terms, ((Mathsat5FormulaList)pObj).terms);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(terms);
  }


  private void writeObject(java.io.ObjectOutputStream out) throws IOException {
    out.defaultWriteObject();
    out.writeInt(terms.length);
    for (int i = 0; i < terms.length; ++i) {
      out.writeObject(new Mathsat5Formula(terms[i]));
    }
  }

  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    int termCount = in.readInt();
    terms = new long[termCount];
    for (int i = 0; i < terms.length; ++i) {
      terms[i] = ((Mathsat5Formula)in.readObject()).getTerm();
    }
  }
}