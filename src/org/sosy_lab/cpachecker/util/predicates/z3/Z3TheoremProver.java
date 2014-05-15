/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sosy_lab.cpachecker.util.predicates.z3.Z3NativeApi.*;
import static org.sosy_lab.cpachecker.util.predicates.z3.Z3NativeApiConstants.*;

import java.util.Collection;
import java.util.List;

import org.sosy_lab.common.time.NestedTimer;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.counterexample.Model;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager.RegionCreator;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager.RegionBuilder;

import com.google.common.base.Preconditions;

public class Z3TheoremProver implements ProverEnvironment {

  private Z3FormulaManager mgr;
  private long z3context;
  private long z3solver;
  private final Z3SmtLogger smtLogger;
  private int level = 0;

  public Z3TheoremProver(Z3FormulaManager mgr) {
    this.mgr = mgr;
    this.z3context = mgr.getEnvironment();
    this.z3solver = mk_solver(z3context);
    solver_inc_ref(z3context, z3solver);
    this.smtLogger = mgr.getSmtLogger();
  }

  @Override
  public void push(BooleanFormula f) {
    level++;

    Preconditions.checkArgument(z3context != 0);
    solver_push(z3context, z3solver);
    long e = Z3FormulaManager.getZ3Expr(f);

    if (mgr.simplifyFormulas) {
      e = simplify(z3context, e);
      inc_ref(z3context, e);
    }

    solver_assert(z3context, z3solver, e);

    smtLogger.logPush(1);
    smtLogger.logAssert(e);
  }

  @Override
  public void pop() {
    level--;

    Preconditions.checkArgument(solver_get_num_scopes(z3context, z3solver) >= 1);
    solver_pop(z3context, z3solver, 1);

    smtLogger.logPop(1);
  }

  @Override
  public boolean isUnsat() {
    int result = solver_check(z3context, z3solver);
    Preconditions.checkArgument(result != Z3_L_UNDEF);

    smtLogger.logCheck();

    return result == Z3_L_FALSE;
  }

  @Override
  public Model getModel() throws SolverException {
    Z3Model model = new Z3Model(mgr, z3context, z3solver);
    Model m = model.createZ3Model();
    return m;
  }

  @Override
  public List<BooleanFormula> getUnsatCore() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void close() {
    Preconditions.checkArgument(z3context != 0);
    Preconditions.checkArgument(z3solver != 0);
    Preconditions.checkArgument(solver_get_num_scopes(z3context, z3solver) >= 0, "a negative number of scopes is not allowed");

    while (level > 0) { // TODO do we need this?
      pop();
    }

    //solver_reset(z3context, z3solver);
    solver_dec_ref(z3context, z3solver);
    z3context = 0;
    z3solver = 0;
  }

  @Override
  public AllSatResult allSat(Collection<BooleanFormula> formulas,
      RegionCreator rmgr, Timer solveTime, NestedTimer enumTime) {
    checkNotNull(rmgr);
    checkNotNull(solveTime);
    checkNotNull(enumTime);
    Preconditions.checkArgument(z3context != 0);

    // create new allSatResult
    Z3AllSatResult result = new Z3AllSatResult(rmgr, solveTime, enumTime);

    // unpack formulas to terms
    long[] importantFormulas = new long[formulas.size()];
    int i = 0;
    for (BooleanFormula impF : formulas) {
      importantFormulas[i++] = Z3FormulaManager.getZ3Expr(impF);
    }

    solveTime.start();
    solver_push(z3context, z3solver);
    smtLogger.logPush(1);
    smtLogger.logCheck();

    while (solver_check(z3context, z3solver) == Z3_L_TRUE) {
      long[] valuesOfModel = new long[importantFormulas.length];
      long z3model = solver_get_model(z3context, z3solver);

      smtLogger.logGetModel();

      for (int j = 0; j < importantFormulas.length; j++) {
        long funcDecl = get_app_decl(z3context, importantFormulas[j]);
        long valueOfExpr = model_get_const_interp(z3context, z3model, funcDecl);

        if (isOP(z3context, valueOfExpr, Z3_OP_FALSE)) {
          valuesOfModel[j] = mk_not(z3context, importantFormulas[j]);
          inc_ref(z3context, valuesOfModel[j]);
        } else {
          valuesOfModel[j] = importantFormulas[j];
        }
      }

      // add model to BDD
      result.callback(valuesOfModel);

      long negatedModel = mk_not(z3context, mk_and(z3context, valuesOfModel));
      inc_ref(z3context, negatedModel);
      solver_assert(z3context, z3solver, negatedModel);

      smtLogger.logAssert(negatedModel);
      smtLogger.logCheck();
    }

    if (solveTime.isRunning()) {
      solveTime.stop();
    } else {
      enumTime.stopOuter();
    }

    // we pushed some levels on assertionStack, remove them and delete solver
    solver_pop(z3context, z3solver, 1);
    smtLogger.logPop(1);

    return result;
  }

  /**
   * this class is used to build the predicate abstraction of a formula
   */
  class Z3AllSatResult implements AllSatResult {

    private final RegionCreator rmgr;
    private final RegionBuilder builder;

    private final Timer solveTime;
    private final NestedTimer enumTime;
    private Timer regionTime = null;

    private int count = 0;

    private Region formula;

    public Z3AllSatResult(RegionCreator rmgr, Timer pSolveTime, NestedTimer pEnumTime) {
      this.rmgr = rmgr;
      this.solveTime = pSolveTime;
      this.enumTime = pEnumTime;
      builder = rmgr.newRegionBuilder(ShutdownNotifier.create()); // TODO real instance
    }

    @Override
    public int getCount() {
      return count;
    }

    @Override
    public Region getResult() throws InterruptedException {
      if (formula == null) {
        enumTime.startBoth();
        try {
          formula = builder.getResult();
          builder.close();
        } finally {
          enumTime.stopBoth();
        }
      }
      return formula;
    }

    public void callback(long[] model) {
      if (count == 0) {
        solveTime.stop();
        enumTime.startOuter();
        regionTime = enumTime.getCurentInnerTimer();
      }

      regionTime.start();

      // the abstraction is created simply by taking the disjunction
      // of all the models found by the all-sat-loop, and storing them in a BDD
      // first, let's create the BDD corresponding to the model
      builder.startNewConjunction();
      for (long t : model) {
        if (isOP(z3context, t, Z3_OP_NOT)) {
          t = get_app_arg(z3context, t, 0);
          builder.addNegativeRegion(rmgr.getPredicate(encapsulate(t)));
        } else {
          builder.addPositiveRegion(rmgr.getPredicate(encapsulate(t)));
        }
      }
      builder.finishConjunction();

      count++;

      regionTime.stop();
    }

    private BooleanFormula encapsulate(long pT) {
      return mgr.encapsulate(BooleanFormula.class, pT);
    }
  }
}
