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
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;

import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.mathsat5.Mathsat5FormulaList;


/**
 * A Formula represented as a MathSAT term.
 */
class Mathsat5Formula implements Formula, Serializable {

  private static final long serialVersionUID = 7662624283533815801L;
  private final long msatTerm;
  private final long msatEnv;

  public Mathsat5Formula(long e, long t) {
    if (Mathsat5NativeApi.MSAT_ERROR_TERM(t)) {
      throw new IllegalArgumentException("Error: term is not a valid mathsat term");
    }

    msatTerm = t;
    msatEnv = e;
  }


  public long getTermEnv() {
    return msatEnv;
  }

  @Override
  public boolean isFalse() {
    int isFalse = Mathsat5NativeApi.msat_term_is_false(msatEnv, msatTerm);
    boolean res = (isFalse != 0);
    return res;
  }

  @Override
  public boolean isTrue() {
    return Mathsat5NativeApi.msat_term_is_true(msatEnv, msatTerm) != 0;
  }

  @Override
  public String toString() {
    return Mathsat5NativeApi.msat_term_repr(msatTerm);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Mathsat5Formula)) { return false; }
    return msatTerm == ((Mathsat5Formula) o).msatTerm;
  }

  long getTerm() {
    return msatTerm;
  }

  @Override
  public int hashCode() {
    return (int) msatTerm;
  }

  protected int getArity() {
    return Mathsat5NativeApi.msat_term_arity(msatTerm);
  }

  protected long getArg(int index) {
    return Mathsat5NativeApi.msat_term_get_arg(msatTerm, index);
  }

  protected long getDeclaration() {
    return Mathsat5NativeApi.msat_term_get_decl(msatTerm);
  }

  protected long getReturnType() {
    return Mathsat5NativeApi.msat_decl_get_return_type(getDeclaration());
  }

  private Object writeReplace() throws ObjectStreamException {
    return new SerialProxy(this);
  }

  private static class SerialProxy implements Serializable {

    private static final long serialVersionUID = 6889568471468710163L;
    private transient static int storageIndex = -1;

    public SerialProxy(Formula pFormula) {
      if (storageIndex == -1) {
        storageIndex = GlobalInfo.getInstance().addHelperStorage(new MathsatFormulaStorage());
      }
      ((MathsatFormulaStorage) GlobalInfo.getInstance().getHelperStorage(storageIndex)).storeFormula(pFormula);
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
      out.defaultWriteObject();
      out.writeInt(storageIndex);
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
      in.defaultReadObject();
      storageIndex = in.readInt();
    }

    private Object readResolve() throws ObjectStreamException {
      return ((MathsatFormulaStorage) GlobalInfo.getInstance().getHelperStorage(storageIndex)).restoreFormula();
    }
  }

  private static class MathsatFormulaStorage implements Serializable {

    private static final long serialVersionUID = -3773448463181606622L;
    private transient ArrayList<Formula> formulaeStorage;

    public MathsatFormulaStorage() {
      formulaeStorage = new ArrayList<Formula>();
    }

    public void storeFormula(Formula f) {
      formulaeStorage.add(f);
    }

    public Formula restoreFormula() {
      Formula result = formulaeStorage.get(0);
      formulaeStorage.remove(0);

      return result;
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
      out.defaultWriteObject();

      //build overall formula using a uninterpreted predicate dummy
      //storageFormula = dummy(formula_1, formula_2, ...)
      long[] terms = new long[formulaeStorage.size()];
      for (int i = 0; i < formulaeStorage.size(); ++i) {
        terms[i] = ((Mathsat5Formula) formulaeStorage.get(i)).msatTerm;
      }
      Formula storageFormula =
          GlobalInfo.getInstance().getFormulaManager().makeUIP("dummy", new Mathsat5FormulaList(terms));

      String storageFormulaRepresentation = GlobalInfo.getInstance().getFormulaManager().dumpFormula(storageFormula);

      //avoid quotation marks in formulae
      storageFormulaRepresentation = storageFormulaRepresentation.replaceAll("\"", "");

      //work around for MathSat bug
      int index = storageFormulaRepresentation.indexOf("VAR dummy :");
      String pre = storageFormulaRepresentation.substring(0, index);
      String post = storageFormulaRepresentation.substring(storageFormulaRepresentation.indexOf("\n", index) + 1);
      storageFormulaRepresentation = pre + post;

      //write everything
      out.writeInt(formulaeStorage.size());
      out.writeObject(storageFormulaRepresentation);
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
      in.defaultReadObject();

      //work around for MathSat bug
      int storageSize = in.readInt();
      GlobalInfo.getInstance().getFormulaManager().declareUIP("dummy", storageSize);

      Formula storageFormula = GlobalInfo.getInstance().getFormulaManager().parse((String) in.readObject());

      //split storage formula
      Formula[] formulae = GlobalInfo.getInstance().getFormulaManager().getArguments(storageFormula);
      formulaeStorage = new ArrayList<Formula>(storageSize);
      for (Formula f : formulae) {
        formulaeStorage.add(f);
      }
    }
  }
}
