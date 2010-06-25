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

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;


@Options(prefix="cpas.symbpredabs.mathsat")
public class MathsatTheoremProver implements TheoremProver {

    @Option
    private boolean useIntegers = false;

    @Option
    private boolean useDtc = false;

    private long absEnv;
    private long msatEnv;
    private MathsatSymbolicFormulaManager mmgr;
    private long curEnv;
    private boolean incremental;
    private int incrAbsEnvCount;
    private final int RESET_INCR_ABS_ENV_FREQUENCY = 100;
    private boolean needsTermCopy;

    public MathsatTheoremProver(MathsatSymbolicFormulaManager mgr,
            boolean incr, Configuration config) throws InvalidConfigurationException {
        config.inject(this);
        msatEnv = mgr.getMsatEnv();
        mmgr = mgr;
        incremental = incr;
        absEnv = 0;
        incrAbsEnvCount = 0;
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
        int ok = mathsat.api.msat_pop_backtrack_point(curEnv);
        assert(ok == 0);
    }

    @Override
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
            if (useIntegers) {
                mathsat.api.msat_add_theory(absEnv, mathsat.api.MSAT_LIA);
                int ok = mathsat.api.msat_set_option(
                        absEnv, "split_eq", "false");
                assert(ok == 0);
            } else {
                mathsat.api.msat_add_theory(absEnv, mathsat.api.MSAT_LRA);
            }
            if (useDtc) {
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
        if (useIntegers) {
            mathsat.api.msat_add_theory(curEnv, mathsat.api.MSAT_LIA);
            int ok = mathsat.api.msat_set_option(
                    curEnv, "split_eq", "false");
            assert(ok == 0);
        } else {
            mathsat.api.msat_add_theory(curEnv, mathsat.api.MSAT_LRA);
        }
        if (useDtc) {
            mathsat.api.msat_set_theory_combination(curEnv,
                    mathsat.api.MSAT_COMB_DTC);
        }
        // disable static learning. For small problems,
        // this is just overhead
        mathsat.api.msat_set_option(curEnv, "sl", "0");
        mathsat.api.msat_set_option(curEnv, "ghost_filter", "true");
    }

    @Override
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

    @Override
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
        long allsatEnv = mathsat.api.msat_create_shared_env(msatEnv);

        int theories = mmgr.getNeededTheories(f);

        mathsat.api.msat_add_theory(allsatEnv, mathsat.api.MSAT_UF);
        if ((theories & MathsatSymbolicFormulaManager.THEORY_ARITH) != 0) {
            if (useIntegers) {
                mathsat.api.msat_add_theory(allsatEnv, mathsat.api.MSAT_LIA);
                int ok = mathsat.api.msat_set_option(allsatEnv, "split_eq",
                        "true");
                assert(ok == 0);
            } else {
                mathsat.api.msat_add_theory(allsatEnv, mathsat.api.MSAT_LRA);
            }
            if (useDtc) {
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
