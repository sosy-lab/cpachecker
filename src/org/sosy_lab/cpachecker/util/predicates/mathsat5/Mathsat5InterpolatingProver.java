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

import java.util.List;

import org.sosy_lab.cpachecker.util.predicates.Model;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.InterpolatingTheoremProver;

import com.google.common.base.Preconditions;

public class Mathsat5InterpolatingProver implements InterpolatingTheoremProver<Integer> {

    private final Mathsat5FormulaManager mgr;
    private long interpolEnv;
    private long cfg;

    private final boolean useSharedEnv;

    public Mathsat5InterpolatingProver(Mathsat5FormulaManager pMgr, boolean shared) {
        mgr = pMgr;
        interpolEnv = 0;
        useSharedEnv = shared;
    }

    @Override
    public void init() {
        Preconditions.checkState(interpolEnv == 0);

        cfg = Mathsat5NativeApi.msat_create_config();
        Mathsat5NativeApi.msat_set_option(cfg, "interpolation", "true");
        Mathsat5NativeApi.msat_set_option( cfg, "model_generation", "true");
        interpolEnv = mgr.createEnvironment(cfg, useSharedEnv, false);
    }

    @Override
    public Integer addFormula(Formula f) {
        Preconditions.checkState(interpolEnv != 0);

        long t = ((Mathsat5Formula)f).getTerm();
        if (!useSharedEnv) {
            t = Mathsat5NativeApi.msat_make_copy_from(interpolEnv, t, mgr.getMsatEnv());
        }
        int group = Mathsat5NativeApi.msat_create_itp_group(interpolEnv);
        Mathsat5NativeApi.msat_set_itp_group(interpolEnv, group);
        Mathsat5NativeApi.msat_assert_formula(interpolEnv, t);
        return group;
    }

    @Override
    public boolean isUnsat() {
        Preconditions.checkState(interpolEnv != 0);

        int res = Mathsat5NativeApi.msat_solve(interpolEnv);
        assert(res != Mathsat5NativeApi.MSAT_UNKNOWN);

        return res == Mathsat5NativeApi.MSAT_UNSAT;
    }

    @Override
    public Formula getInterpolant(List<Integer> formulasOfA) {
        Preconditions.checkState(interpolEnv != 0);

        int[] groupsOfA = new int[formulasOfA.size()];
        int i = 0;
        for (Integer f : formulasOfA) {
          groupsOfA[i++] = f;
        }
        long itp = Mathsat5NativeApi.msat_get_interpolant(interpolEnv, groupsOfA);
        assert(!Mathsat5NativeApi.MSAT_ERROR_TERM(itp));

        if (!useSharedEnv) {
            itp = Mathsat5NativeApi.msat_make_copy_from(mgr.getMsatEnv(), itp, interpolEnv);
        }
        return new Mathsat5Formula(mgr.getMsatEnv(), itp);
    }

    @Override
    public void reset() {
        Preconditions.checkState(interpolEnv != 0);

        Mathsat5NativeApi.msat_destroy_env(interpolEnv);
        interpolEnv = 0;
    }

    @Override
    public Model getModel() {
      Preconditions.checkState(interpolEnv != 0);

      return Mathsat5Model.createMathsatModel(interpolEnv, mgr);
    }

}