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
package org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.mathsat;

import static mathsat.api.*;

import java.util.Collection;

import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.AbstractFormulaManager;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormula;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.TheoremProver.AllSatResult;
import org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatTheoremProver.MathsatAllSatCallback;
import org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.interfaces.TheoremProver;

import com.google.common.base.Preconditions;

public class MathsatTheoremProver implements TheoremProver {
  
  private final MathsatSymbolicFormulaManager mgr;
  private long curEnv;

  public MathsatTheoremProver(MathsatSymbolicFormulaManager pMgr) {
    mgr = pMgr;
    curEnv = 0;
  }

  @Override
  public boolean isUnsat(SymbolicFormula f) {
    push(f);
    int res = msat_solve(curEnv);
    pop();
    assert(res != MSAT_UNKNOWN);
    return res == MSAT_UNSAT;
  }

  @Override
  public void pop() {
    Preconditions.checkState(curEnv != 0);
    int ok = msat_pop_backtrack_point(curEnv);
    assert(ok == 0);
  }

  @Override
  public void push(SymbolicFormula f) {
    Preconditions.checkState(curEnv != 0);
    msat_push_backtrack_point(curEnv);
    msat_assert_formula(curEnv, org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatSymbolicFormulaManager.getTerm(f));
  }

  @Override
  public void init() {
    Preconditions.checkState(curEnv == 0);

    curEnv = mgr.createEnvironment(true, true);
  }
  
  @Override
  public void reset() {
    Preconditions.checkState(curEnv != 0);
    msat_destroy_env(curEnv);
    curEnv = 0;
  }
  
  @Override
  public AllSatResult allSat(SymbolicFormula f, Collection<SymbolicFormula> important, 
                             FormulaManager fmgr, AbstractFormulaManager amgr) {
    long formula = org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatSymbolicFormulaManager.getTerm(f);
    
    long allsatEnv = mgr.createEnvironment(true, true);
    
    long[] imp = new long[important.size()];
    int i = 0;
    for (SymbolicFormula impF : important) {
      imp[i++] = org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatSymbolicFormulaManager.getTerm(impF);
    }
    MathsatAllSatCallback callback = new MathsatAllSatCallback(fmgr, amgr);
    msat_assert_formula(allsatEnv, formula);
    int numModels = msat_all_sat(allsatEnv, imp, callback);
    
    if (numModels == -1) {
      throw new RuntimeException("Error occurred during Mathsat allsat");
    
    } else if (numModels == -2) {
      // infinite models
      callback.setInfiniteNumberOfModels();

    } else {
      assert numModels == callback.getCount();
    }

    msat_destroy_env(allsatEnv);

    return callback;
  }
}
