/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.z3;

import java.util.Collection;

import org.sosy_lab.common.NestedTimer;
import org.sosy_lab.common.Timer;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager.RegionCreator;
import org.sosy_lab.cpachecker.util.predicates.Model;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.TheoremProver;

import com.google.common.base.Preconditions;
import com.microsoft.z3.Native;
import com.microsoft.z3.Z3Exception;
import com.microsoft.z3.enumerations.Z3_lbool;


public class Z3TheoremProver implements TheoremProver {

  private static final boolean USE_SHARED_ENV = true;

  private Z3FormulaManager mgr;
  private long ctx;

  public Z3TheoremProver(Z3FormulaManager pMgr) {
    this.mgr = pMgr;
    this.ctx = 0;
  }

  @Override
  public void init() {
    Preconditions.checkState(ctx == 0);

    try {
      long cfg = Native.mkConfig();
      Native.updateParamValue(cfg, "MODEL", "true");
      this.ctx = mgr.createContext(cfg);
      Native.delConfig(cfg);
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  public void push(BooleanFormula pF) {
    Preconditions.checkState(ctx != 0);

    try {
      Native.push(ctx);
      Native.assertCnstr(ctx, (long) Z3FormulaManager.getTerm(pF));
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  public void pop() {
    Preconditions.checkState(ctx != 0);

    try {
      Native.pop(ctx, 1);
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  public boolean isUnsat() {
    try {
      Native.LongPtr m = new Native.LongPtr();
      int res = Native.checkAndGetModel(ctx, m);
      if (m.value != 0)
        Native.delModel(ctx, m.value);
      assert (res != Z3_lbool.Z3_L_UNDEF.toInt());
      return (res == Z3_lbool.Z3_L_FALSE.toInt());
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  public Model getModel() throws SolverException {
    Preconditions.checkState(ctx != 0);

    return Z3Model.create(ctx, mgr);
  }

  @Override
  public void reset() {
    Preconditions.checkState(ctx != 0);

    try {
      Native.delContext(ctx);
      this.ctx = 0;
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  public AllSatResult allSat(BooleanFormula pF, Collection<BooleanFormula> pImportant, RegionCreator pMgr,
      Timer pSolveTime, NestedTimer pEnumTime) {
    // TODO Auto-generated method stub
    return null;
  }

}
