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

import static org.sosy_lab.cpachecker.util.predicates.z3.Z3NativeApiConstants.*;
import static org.sosy_lab.cpachecker.util.predicates.z3.Z3NativeApi.*;

import java.util.Collection;

import org.sosy_lab.common.NestedTimer;
import org.sosy_lab.common.Timer;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager.RegionCreator;
import org.sosy_lab.cpachecker.util.predicates.Model;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ProverEnvironment;

public class Z3TheoremProver implements ProverEnvironment {

  private Z3FormulaManager mgr;
  long z3context;
  long z3solver;

  Z3TheoremProver(long z3context, long z3solver) {
    this.z3context = z3context;
    this.z3solver = z3solver;
  }

  public Z3TheoremProver(Z3FormulaManager mgr) {
    this.mgr = mgr;
    this.z3context = mgr.getContext();
    this.z3solver = mk_solver(z3context);
    solver_inc_ref(z3context, z3solver);
  }

  @Override
  public void push(BooleanFormula pF) {
    solver_push(z3context, z3solver);
    solver_assert(z3context, z3solver, Z3FormulaManager.getZ3Expr(pF));
  }

  @Override
  public void pop() {
    assert (solver_get_num_scopes(z3context, z3solver) >= 1);
    solver_pop(z3context, z3solver, 1);
  }

  @Override
  public boolean isUnsat() {
    int result = solver_check(z3context, z3solver);
    assert (result != Z3_L_UNDEF);
    return result == Z3_L_FALSE;
  }

  @Override
  public Model getModel() throws SolverException {
    Z3Model model = new Z3Model(mgr, z3context, z3solver);
    return model.createZ3Model();
  }

  @Override
  public AllSatResult allSat(Collection<BooleanFormula> pImportant, RegionCreator pMgr, Timer pSolveTime,
      NestedTimer pEnumTime) {
    throw new AssertionError("not implemented");
  }

  @Override
  public void close() {
    assert (z3context != 0);
    assert (z3solver != 0);
    solver_reset(z3context, z3solver);
    solver_dec_ref(z3context, z3solver);
    // del_context(z3context); //TODO delete context? is it used somewhere else?
    z3context = 0;
    z3solver = 0;
  }
}
