/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package symbpredabstraction.mathsat;

import java.util.ArrayList;
import java.util.List;

import symbpredabstraction.interfaces.SymbolicFormula;
import symbpredabstraction.interfaces.TheoremProver;
import cmdline.CPAMain;


public class MathsatTheoremProver implements TheoremProver {
    private long absEnv;
    private long msatEnv;
    private MathsatSymbolicFormulaManager mmgr;
    private long curEnv;
    private boolean incremental;
    private int incrAbsEnvCount;
    private final int RESET_INCR_ABS_ENV_FREQUENCY = 100;
    private boolean needsTermCopy;

    public MathsatTheoremProver(MathsatSymbolicFormulaManager mgr,
            boolean incr) {
        msatEnv = mgr.getMsatEnv();
        mmgr = mgr;
        incremental = incr;
        absEnv = 0;
        incrAbsEnvCount = 0;
        curEnv = 0;
    }

    public boolean isUnsat(SymbolicFormula f) {
        push(f);
        int res = mathsat.api.msat_solve(curEnv);
        pop();
        assert(res != mathsat.api.MSAT_UNKNOWN);
        return res == mathsat.api.MSAT_UNSAT;
    }

    public void pop() {
        int ok = mathsat.api.msat_pop_backtrack_point(curEnv);
        assert(ok == 0);
    }

    public void push(SymbolicFormula f) {
        mathsat.api.msat_push_backtrack_point(curEnv);
        long t = ((MathsatSymbolicFormula)f).getTerm();
        if (needsTermCopy) {
            t = mathsat.api.msat_make_copy_from(curEnv, t, msatEnv);
        }
        mathsat.api.msat_assert_formula(curEnv, t);
    }

    private void initCartesian() {
        if (absEnv == 0 || !incremental) {
            absEnv = mathsat.api.msat_create_shared_env(msatEnv);
            mathsat.api.msat_add_theory(absEnv, mathsat.api.MSAT_UF);
            if (CPAMain.cpaConfig.getBooleanValue(
            "cpas.symbpredabs.mathsat.useIntegers")) {
                mathsat.api.msat_add_theory(absEnv, mathsat.api.MSAT_LIA);
                int ok = mathsat.api.msat_set_option(
                        absEnv, "split_eq", "false");
                assert(ok == 0);
            } else {
                mathsat.api.msat_add_theory(absEnv, mathsat.api.MSAT_LRA);
            }
            if (CPAMain.cpaConfig.getBooleanValue(
                    "cpas.symbpredabs.mathsat.useDtc")) {
                mathsat.api.msat_set_theory_combination(absEnv,
                        mathsat.api.MSAT_COMB_DTC);
            }
            // disable static learning. For small problems,
            // this is just overhead
            mathsat.api.msat_set_option(absEnv, "sl", "0");
            mathsat.api.msat_set_option(absEnv, "ghost_filter", "true");

            if (incremental) {
                mathsat.api.msat_push_backtrack_point(absEnv);
            }
        } else { // incremental
            if (++incrAbsEnvCount == RESET_INCR_ABS_ENV_FREQUENCY) {
                incrAbsEnvCount = 0;
                mathsat.api.msat_destroy_env(absEnv);
                initCartesian();
            } else {
                mathsat.api.msat_push_backtrack_point(absEnv);
            }
        }
    }

    private void initNormal(boolean shared) {
        if (shared) {
            curEnv = mathsat.api.msat_create_shared_env(msatEnv);
        } else {
            curEnv = mathsat.api.msat_create_env();
        }
        mathsat.api.msat_add_theory(curEnv, mathsat.api.MSAT_UF);
        if (CPAMain.cpaConfig.getBooleanValue(
        "cpas.symbpredabs.mathsat.useIntegers")) {
            mathsat.api.msat_add_theory(curEnv, mathsat.api.MSAT_LIA);
            int ok = mathsat.api.msat_set_option(
                    curEnv, "split_eq", "false");
            assert(ok == 0);
        } else {
            mathsat.api.msat_add_theory(curEnv, mathsat.api.MSAT_LRA);
        }
        if (CPAMain.cpaConfig.getBooleanValue(
                "cpas.symbpredabs.mathsat.useDtc")) {
            mathsat.api.msat_set_theory_combination(curEnv,
                    mathsat.api.MSAT_COMB_DTC);
        }
        // disable static learning. For small problems,
        // this is just overhead
        mathsat.api.msat_set_option(curEnv, "sl", "0");
        mathsat.api.msat_set_option(curEnv, "ghost_filter", "true");
    }

    public void init(int purpose) {
        switch (purpose) {
        case CARTESIAN_ABSTRACTION:
            initCartesian();
            curEnv = absEnv;
            break;
        default:
            initNormal(purpose != ENTAILMENT_CHECK);
            absEnv = 0;
        }
        needsTermCopy = purpose == ENTAILMENT_CHECK;
    }

    public void reset() {
        if (curEnv != absEnv) {
            mathsat.api.msat_destroy_env(curEnv);
        } else {
            if (!incremental) {
                mathsat.api.msat_destroy_env(curEnv);
            } else {
                mathsat.api.msat_pop_backtrack_point(curEnv);
            }
        }
    }

    private static class MathsatAllSatCallback implements mathsat.AllSatModelCallback {
        private final AllSatCallback toCall;

        public MathsatAllSatCallback(AllSatCallback tc) {
            toCall = tc;
        }

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
        long allsatEnv = mathsat.api.msat_create_shared_env(msatEnv);

        int theories = mmgr.getNeededTheories((MathsatSymbolicFormula)f);

        mathsat.api.msat_add_theory(allsatEnv, mathsat.api.MSAT_UF);
        if ((theories & MathsatSymbolicFormulaManager.THEORY_ARITH) != 0) {
            if (CPAMain.cpaConfig.getBooleanValue(
                    "cpas.symbpredabs.mathsat.useIntegers")) {
                mathsat.api.msat_add_theory(allsatEnv, mathsat.api.MSAT_LIA);
                int ok = mathsat.api.msat_set_option(allsatEnv, "split_eq",
                        "true");
                assert(ok == 0);
            } else {
                mathsat.api.msat_add_theory(allsatEnv, mathsat.api.MSAT_LRA);
            }
            if (CPAMain.cpaConfig.getBooleanValue(
                    "cpas.symbpredabs.mathsat.useDtc")) {
                mathsat.api.msat_set_theory_combination(allsatEnv,
                        mathsat.api.MSAT_COMB_DTC);
            }
        }
        mathsat.api.msat_set_option(allsatEnv, "sl", "0");

        long formula = ((MathsatSymbolicFormula)f).getTerm();
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
