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
package org.sosy_lab.cpachecker.util.predicates.mathsat;

import java.io.ObjectStreamException;
import java.io.Serializable;

import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.predicates.ExtendedFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;


/**
 * A Formula represented as a MathSAT term.
 */
class MathsatFormula implements Formula, Serializable {

    private static final long serialVersionUID = 7662624283533815801L;
    private final long msatTerm;

    public MathsatFormula(long t) {
        if (NativeApi.MSAT_ERROR_TERM(t)) {
          throw new IllegalArgumentException("Error term is not a valid formula");
        }
        msatTerm = t;
    }

    @Override
    public boolean isFalse() {
        return NativeApi.msat_term_is_false(msatTerm) != 0;
    }

    @Override
    public boolean isTrue() {
        return NativeApi.msat_term_is_true(msatTerm) != 0;
    }

    @Override
    public String toString() {
        return NativeApi.msat_term_repr(msatTerm);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MathsatFormula)) {
          return false;
        }
        return msatTerm == ((MathsatFormula)o).msatTerm;
    }

    long getTerm() { return msatTerm; }

    @Override
    public int hashCode() {
        return (int)msatTerm;
    }

    private Object writeReplace() throws ObjectStreamException {
      return new SerialProxy(GlobalInfo.getInstance().getFormulaManager().dumpFormula(this));
    }

    private static class SerialProxy implements Serializable {
      private static final long serialVersionUID = 6889568471468710163L;
      private final String formulaStr;

      public SerialProxy(String pFormulaStr) {
        formulaStr = pFormulaStr.replaceAll("\"", "");
      }

      private Object readResolve() throws ObjectStreamException {
        ExtendedFormulaManager fm = GlobalInfo.getInstance().getFormulaManager();
        return fm.parse(formulaStr);
      }
    }
}
