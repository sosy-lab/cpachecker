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

import java.util.List;

import org.sosy_lab.cpachecker.util.predicates.Model;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.InterpolatingTheoremProver;

import com.google.common.base.Preconditions;

public class MathsatInterpolatingProver implements InterpolatingTheoremProver<Integer> {

    private final MathsatFormulaManager mgr;
    private long env;

    private final boolean useSharedEnv;

    public MathsatInterpolatingProver(MathsatFormulaManager pMgr, boolean shared) {
        mgr = pMgr;
        env = 0;
        useSharedEnv = shared;
    }

    @Override
    public void init() {
        Preconditions.checkState(env == 0);

        env = mgr.createEnvironment(useSharedEnv, false);

        int ok = NativeApi.msat_init_interpolation(env);
        assert(ok == 0);
    }

    @Override
    public Integer addFormula(Formula f) {
        Preconditions.checkState(env != 0);

        long t = ((MathsatFormula)f).getTerm();
        if (!useSharedEnv) {
            t = NativeApi.msat_make_copy_from(env, t, mgr.getMsatEnv());
        }
        int group = NativeApi.msat_create_itp_group(env);
        NativeApi.msat_set_itp_group(env, group);
        NativeApi.msat_push_backtrack_point(env);
        NativeApi.msat_assert_formula(env, t);
        return group;
    }

    @Override
    public void popFormula() {
      Preconditions.checkState(env != 0);
      int ok = NativeApi.msat_pop_backtrack_point(env);
      assert(ok == 0);
    }

    @Override
    public boolean isUnsat() {
        Preconditions.checkState(env != 0);

        int res = NativeApi.msat_solve(env);
        assert(res != NativeApi.MSAT_UNKNOWN);

        return res == NativeApi.MSAT_UNSAT;
    }

    @Override
    public Formula getInterpolant(List<Integer> formulasOfA) {
        Preconditions.checkState(env != 0);

        int[] groupsOfA = new int[formulasOfA.size()];
        int i = 0;
        for (Integer f : formulasOfA) {
          groupsOfA[i++] = f;
        }
        long itp = NativeApi.msat_get_interpolant(env, groupsOfA);
        assert(!NativeApi.MSAT_ERROR_TERM(itp));
        if (!useSharedEnv) {
            itp = NativeApi.msat_make_copy_from(mgr.getMsatEnv(), itp, env);
        }
        return new MathsatFormula(itp);
    }

    @Override
    public void reset() {
        Preconditions.checkState(env != 0);

        NativeApi.msat_destroy_env(env);
        env = 0;
    }

    @Override
    public Model getModel() {
      Preconditions.checkState(env != 0);

      return MathsatModel.createMathsatModel(env, mgr, useSharedEnv);
    }

}