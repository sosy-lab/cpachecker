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

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.counterexample.Model;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.OptEnvironment;
import org.sosy_lab.cpachecker.util.predicates.z3.Z3NativeApiConstants.Z3_LBOOL;
import org.sosy_lab.cpachecker.util.rationals.Rational;

import com.google.common.base.Optional;
import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;

class Z3OptProver implements OptEnvironment {

  private final Z3FormulaManager mgr;
  private final Z3RationalFormulaManager rfmgr;
  private static final String Z3_INFINITY_REPRESENTATION = "oo";
  private long z3context;
  private long z3optContext;
  private final ShutdownNotifier shutdownNotifier;

  Z3OptProver(Z3FormulaManager pMgr, ShutdownNotifier pShutdownNotifier) {
    mgr = pMgr;
    rfmgr = (Z3RationalFormulaManager)pMgr.getRationalFormulaManager();
    z3context = mgr.getEnvironment();
    z3optContext = mk_optimize(z3context);
    optimize_inc_ref(z3context, z3optContext);
    shutdownNotifier = checkNotNull(pShutdownNotifier);
  }

  @Override
  public void addConstraint(BooleanFormula constraint) {
    Z3BooleanFormula z3Constraint = (Z3BooleanFormula) constraint;
    optimize_assert(z3context, z3optContext, z3Constraint.getFormulaInfo());
  }

  @Override
  public int maximize(Formula objective) {
    Z3Formula z3Objective = (Z3Formula)objective;
    return optimize_maximize(
        z3context, z3optContext, z3Objective.getFormulaInfo());
  }

  @Override
  public int minimize(Formula objective) {
    Z3Formula z3Objective = (Z3Formula) objective;
    return optimize_minimize(
        z3context, z3optContext, z3Objective.getFormulaInfo());
  }

  @Override
  public OptStatus check() throws InterruptedException, SolverException {
    try {
      int status = optimize_check(z3context, z3optContext);
      if (status == Z3_LBOOL.Z3_L_FALSE.status) {
        return OptStatus.UNSAT;
      } else if (status == Z3_LBOOL.Z3_L_UNDEF.status) {
        return OptStatus.UNDEF;
      } else {
        return OptStatus.OPT;
      }
    } catch (Z3SolverException e) {
      // check if it's a timeout
      shutdownNotifier.shutdownIfNecessary();
      throw e;
    }
  }

  @Override
  public void push() {
    optimize_push(z3context, z3optContext);
  }

  @Override
  public void pop() {
    optimize_pop(z3context, z3optContext);
  }

  @Override
  public Optional<Rational> upper(int handle, Rational epsilon) {
    long ast = optimize_get_upper(z3context, z3optContext, handle);
    if (isInfinity(ast)) {
      return Optional.absent();
    }
    return Optional.of(rationalFromZ3AST(replaceEpsilon(ast, epsilon)));
  }

  @Override
  public Optional<Rational> lower(int handle, Rational epsilon) {
    long ast = optimize_get_lower(z3context, z3optContext, handle);
    if (isInfinity(ast)) {
      return Optional.absent();
    }
    return Optional.of(rationalFromZ3AST(replaceEpsilon(ast, epsilon)));
  }

  @Override
  public Model getModel() throws SolverException {
    long z3model = optimize_get_model(z3context, z3optContext);
    return Z3Model.parseZ3Model(mgr, z3context, z3model);
  }

  public Formula evaluate(Formula expr) {
    Z3Formula input = (Z3Formula) expr;
    long z3model = optimize_get_model(z3context, z3optContext);
    model_inc_ref(z3context, z3model);

    PointerToLong out = new PointerToLong();
    boolean status = model_eval(z3context, z3model, input.getFormulaInfo(),
        true, out);
    Verify.verify(status, "Error during model evaluation");

    Formula outValue = mgr.getFormulaCreator().encapsulate(
        mgr.getFormulaType(expr), out.value
    );

    model_dec_ref(z3context, z3model);
    return outValue;
  }

  void setParam(String key, String value) {
    long keySymbol = mk_string_symbol(z3context, key);
    long valueSymbol = mk_string_symbol(z3context, value);
    long params = mk_params(z3context);
    params_set_symbol(z3context, params, keySymbol, valueSymbol);
    optimize_set_params(z3context, z3optContext, params);
  }

  /**
   * Dumps the optimized objectives and the constraints on the solver in the
   * SMT-lib format. Super-useful!
   */
  @Override
  public String toString() {
    return optimize_to_string(z3context, z3optContext);
  }

  @Override
  public void close() {
    optimize_dec_ref(z3context, z3optContext);
    z3context = 0;
    z3optContext = 0;
  }

  private boolean isInfinity(long ast) {
    return ast_to_string(z3context, ast).contains(Z3_INFINITY_REPRESENTATION);
  }

  /**
   * Replace the epsilon in the returned formula with a numeric value.
   */
  private long replaceEpsilon(long ast, Rational newValue) {
    Z3Formula z = new Z3RationalFormula(z3context, ast);

    Z3Formula epsFormula = (Z3Formula)rfmgr.makeVariable("epsilon");

    Z3Formula out = mgr.getUnsafeFormulaManager().substitute(
        z,
        ImmutableList.of(epsFormula),
        ImmutableList.of((Z3Formula)rfmgr.makeNumber(newValue.toString()))
    );
    return simplify(z3context, out.getFormulaInfo());

  }

  private Rational rationalFromZ3AST(long ast) {
    return Rational.ofString(get_numeral_string(z3context, ast));
  }
}
