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
package org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat;

import java.util.ArrayList;
import java.util.List;

import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormula;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.TheoremProver;

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
        int res = mathsat.api.msat_solve(curEnv);
        pop();
        assert(res != mathsat.api.MSAT_UNKNOWN);
        return res == mathsat.api.MSAT_UNSAT;
    }

    @Override
    public void pop() {
        Preconditions.checkState(curEnv != 0);
        int ok = mathsat.api.msat_pop_backtrack_point(curEnv);
        assert(ok == 0);
    }

    @Override
    public void push(SymbolicFormula f) {
        Preconditions.checkState(curEnv != 0);
        mathsat.api.msat_push_backtrack_point(curEnv);
        long t = ((MathsatSymbolicFormula)f).getTerm();
        mathsat.api.msat_assert_formula(curEnv, t);
    }

    @Override
    public void init() {
        Preconditions.checkState(curEnv == 0);
  
        curEnv = mgr.createEnvironment(true, true);
    }
    
    @Override
    public void reset() {
        Preconditions.checkState(curEnv != 0);
        mathsat.api.msat_destroy_env(curEnv);
        curEnv = 0;
    }
    
    private static class MathsatAllSatCallback implements mathsat.AllSatModelCallback {
        private final AllSatCallback toCall;

        public MathsatAllSatCallback(AllSatCallback tc) {
            toCall = tc;
        }

        @Override
        public void callback(long[] model) {
            List<SymbolicFormula> outModel = new ArrayList<SymbolicFormula>(model.length);
            for (long t : model) {
              outModel.add(new MathsatSymbolicFormula(t));
            }
            toCall.modelFound(outModel);
        }
    }

    @Override
    public int allSat(SymbolicFormula f,
            List<SymbolicFormula> important, AllSatCallback callback) {
        long formula = ((MathsatSymbolicFormula)f).getTerm();
        
        long allsatEnv = mgr.createEnvironment(true, true);
        
        long[] imp = new long[important.size()];
        for (int i = 0; i < imp.length; ++i) {
            imp[i] = ((MathsatSymbolicFormula)important.get(i)).getTerm();
        }
        MathsatAllSatCallback func = new MathsatAllSatCallback(callback);
        mathsat.api.msat_assert_formula(allsatEnv, formula);
        int numModels = mathsat.api.msat_all_sat(allsatEnv, imp, func);

        mathsat.api.msat_destroy_env(allsatEnv);

        return numModels;
    }

}
