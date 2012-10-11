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

import static com.google.common.base.Preconditions.*;
import static org.sosy_lab.cpachecker.util.predicates.mathsat5.Mathsat5NativeApi.*;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Map;

import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.predicates.ExtendedFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;

import com.google.common.collect.Maps;


/**
 * A Formula represented as a MathSAT term.
 */
class Mathsat5Formula implements Formula, Serializable {

  private static final long serialVersionUID = 7662624283533815801L;
  private final long msatTerm;
  private final long msatEnv;

  public Mathsat5Formula(long e, long t) {
    assert e != 0;
    assert t != 0;

    msatTerm = t;
    msatEnv = e;
  }


  public long getTermEnv() {
    return msatEnv;
  }

  @Override
  public boolean isFalse() {
    return msat_term_is_false(msatEnv, msatTerm);
  }

  @Override
  public boolean isTrue() {
    return msat_term_is_true(msatEnv, msatTerm);
  }

  @Override
  public String toString() {
    return msat_term_repr(msatTerm);
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

  private Object writeReplace() throws ObjectStreamException {
    return new SerialProxy(this);
  }

  private static class SerialProxy implements Serializable {

    private static final long serialVersionUID = 6889568471468710163L;
    private transient static int storageIndex = -1;
    private transient int id = -1;

    public SerialProxy(Formula pFormula) {
      if (storageIndex == -1) {
        storageIndex = GlobalInfo.getInstance().addHelperStorage(new MathsatFormulaStorage());
      }
      MathsatFormulaStorage formulaStorage = (MathsatFormulaStorage) GlobalInfo.getInstance().getHelperStorage(storageIndex);
      id = formulaStorage.storeFormula(pFormula);
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
      checkState(storageIndex >= 0);
      checkState(id >= 0);
      out.defaultWriteObject();
      out.writeInt(storageIndex);
      out.writeInt(id);
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
      in.defaultReadObject();
      storageIndex = in.readInt();
      id = in.readInt();
      checkState(id >= 0);
    }

    private Object readResolve() throws ObjectStreamException {
      return ((MathsatFormulaStorage) GlobalInfo.getInstance().getHelperStorage(storageIndex)).restoreFormula(id);
    }
  }

  private static class MathsatFormulaStorage implements Serializable {

    private static final long serialVersionUID = -3773448463181606622L;
    private transient Map<String, Formula> formulaeStorage = Maps.newHashMap();

    private transient int nextId = 0;

    public int storeFormula(Formula f) {
      int id = nextId++;
      formulaeStorage.put("a" + id, f);
      return id;
    }

    public Formula restoreFormula(int id) {
      checkArgument(id >= 0);
      return checkNotNull(formulaeStorage.get("a" + id));
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
      out.defaultWriteObject();

      ExtendedFormulaManager fmgr = GlobalInfo.getInstance().getFormulaManager();
      String storageFormulaRepresentation = fmgr.dumpFormulas(formulaeStorage);

      out.writeObject(storageFormulaRepresentation);
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
      in.defaultReadObject();

      String data = (String) in.readObject();

      ExtendedFormulaManager fmgr = GlobalInfo.getInstance().getFormulaManager();
      formulaeStorage = fmgr.parseFormulas(data);
    }
  }
}
