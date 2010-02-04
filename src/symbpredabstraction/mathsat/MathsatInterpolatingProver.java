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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cpa.common.CPAchecker;

import symbpredabstraction.interfaces.InterpolatingTheoremProver;
import symbpredabstraction.interfaces.SymbolicFormula;


public class MathsatInterpolatingProver implements InterpolatingTheoremProver<Integer> {

    private final long msatEnv;
    private long env;
    private final boolean useSharedEnv;
    private final Map<Long, Long> copyFromCache;

    public MathsatInterpolatingProver(MathsatSymbolicFormulaManager mgr,
                                      boolean useSharing) {
        msatEnv = mgr.getMsatEnv();
        env = 0;
        useSharedEnv = useSharing;
        copyFromCache = new HashMap<Long, Long>();
    }

    @Override
    public void init() {
        assert copyFromCache.isEmpty();
        assert env == 0;

        if (useSharedEnv) {
            env = mathsat.api.msat_create_shared_env(msatEnv);
        } else {
            env = mathsat.api.msat_create_env();
        }

        mathsat.api.msat_add_theory(env, mathsat.api.MSAT_UF);
        mathsat.api.msat_add_theory(env, mathsat.api.MSAT_LRA);

        boolean theoryCombinationNeeded = CPAchecker.config.getBooleanValue(
                "cpas.symbpredabs.mathsat.useDtc");

        if (theoryCombinationNeeded) {
            mathsat.api.msat_set_theory_combination(env,
                    mathsat.api.MSAT_COMB_DTC);
        } else if (CPAchecker.config.getBooleanValue(
                "cpas.symbpredabs.mathsat.useIntegers")) {
            int ok = mathsat.api.msat_set_option(env, "split_eq", "true");
            assert(ok == 0);
        }
        int ok = mathsat.api.msat_set_option(env, "sl", "0");
        assert(ok == 0);

        mathsat.api.msat_init_interpolation(env);
    }

    @Override
    public Integer addFormula(SymbolicFormula f) {
        assert env != 0;
      
        long t = ((MathsatSymbolicFormula)f).getTerm();
        if (!useSharedEnv) {
            long t2;
            if (copyFromCache.containsKey(t)) {
                t2 = copyFromCache.get(t);
            } else {
                t2 = mathsat.api.msat_make_copy_from(env, t, msatEnv);
                copyFromCache.put(t, t2);
            }
            t = t2;
        }
        int group = mathsat.api.msat_create_itp_group(env);
        mathsat.api.msat_set_itp_group(env, group);
        mathsat.api.msat_assert_formula(env, t);
        return group;
    }

    @Override
    public boolean isUnsat() {
        int res = mathsat.api.msat_solve(env);
        assert(res != mathsat.api.MSAT_UNKNOWN);
        return res == mathsat.api.MSAT_UNSAT;
    }

    @Override
    public SymbolicFormula getInterpolant(List<Integer> formulasOfA) {
        assert env != 0;
      
        int[] groupsOfA = new int[formulasOfA.size()];
        int i = 0;
        for (Integer f : formulasOfA) {
          groupsOfA[i++] = f;
        }
        long itp = mathsat.api.msat_get_interpolant(env, groupsOfA);
        assert(!mathsat.api.MSAT_ERROR_TERM(itp));
        if (!useSharedEnv) {
            itp = mathsat.api.msat_make_copy_from(msatEnv, itp, env);
        }

        return new MathsatSymbolicFormula(itp);
    }

    @Override
    public void reset() {
        mathsat.api.msat_destroy_env(env);
        env = 0;
        copyFromCache.clear();
    }
}