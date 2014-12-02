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

import static org.sosy_lab.cpachecker.util.predicates.z3.Z3NativeApi.*;

import org.sosy_lab.cpachecker.core.counterexample.Model;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.OptEnvironment;
import org.sosy_lab.cpachecker.util.predicates.z3.Z3NativeApiConstants.Z3_LBOOL;
import org.sosy_lab.cpachecker.util.rationals.Rational;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

public class Z3OptProver implements OptEnvironment {

  private final Z3FormulaManager mgr;
  private static final String Z3_INFINITY_REPRESENTATION = "oo";
  private long z3context;
  private long z3optContext;

  private Optional<Integer> objectiveHandle;
  private Optional<Boolean> isMaximization;

  public Z3OptProver(Z3FormulaManager mgr) {
    this.mgr = mgr;
    z3context = mgr.getEnvironment();
    z3optContext = mk_optimize(z3context);
    optimize_inc_ref(z3context, z3optContext);

    objectiveHandle = Optional.absent();
    isMaximization = Optional.absent();
  }

  @Override
  public void addConstraint(BooleanFormula constraint) {
    Z3BooleanFormula z3Constraint = (Z3BooleanFormula) constraint;
    optimize_assert(z3context, z3optContext, z3Constraint.getExpr());
  }

  @Override
  public void maximize(Formula objective) {
    Preconditions.checkState(!objectiveHandle.isPresent());
    Z3Formula z3Objective = (Z3Formula) objective;
    int handle = optimize_maximize(
        z3context, z3optContext, z3Objective.getExpr());
    objectiveHandle = Optional.of(handle);
    isMaximization = Optional.of(true);
  }

  @Override
  public void minimize(Formula objective) {
    Preconditions.checkState(!objectiveHandle.isPresent());
    Z3Formula z3Objective = (Z3Formula) objective;
    int handle = optimize_minimize(
        z3context, z3optContext, z3Objective.getExpr());
    objectiveHandle = Optional.of(handle);
    isMaximization = Optional.of(false);
  }

  @Override
  public OptStatus check()
      throws InterruptedException, SolverException {
    // TODO: check whether we can switch constraints using push and pop.
    // because the objective seems to be essentially the
    // _constraint_.

    Preconditions.checkState(objectiveHandle.isPresent());

    int status = optimize_check(z3context, z3optContext);
    if (status == Z3_LBOOL.Z3_L_FALSE.status) {
      return OptStatus.UNSAT;
    } else if (status == Z3_LBOOL.Z3_L_UNDEF.status) {
      return OptStatus.UNDEF;
    } else {

      long out = optimize_get_upper(z3context, z3optContext,
          objectiveHandle.get());
      String outS = ast_to_string(z3context, out);

      // We use contains because we'll get negative infinity for minimization.
      if (outS.contains(Z3_INFINITY_REPRESENTATION)) {
        return OptStatus.UNBOUNDED;
      }
      return OptStatus.OPT;
    }
  }

  @Override
  public Rational upper(int epsilon) {
    Preconditions.checkState(objectiveHandle.isPresent());
    int idx = objectiveHandle.get();

    long ast = optimize_get_upper(z3context, z3optContext, idx);

    return rationalFromZ3AST(replaceEpsilon(ast, epsilon));
  }

  @Override
  public Rational lower(int epsilon) {
    Preconditions.checkState(objectiveHandle.isPresent());
    int idx = objectiveHandle.get();
    long ast = optimize_get_lower(z3context, z3optContext, idx);

    // TODO: change the epsilon value from 1 to a more suitable number.
    return rationalFromZ3AST(
        replaceEpsilon(ast, epsilon));
  }

  @Override
  public Rational value(int epsilon) {
    Preconditions.checkState(isMaximization.isPresent());
    if(isMaximization.get()) {
      return upper(epsilon);
    }
    return lower(epsilon);
  }

  @Override
  public Model getModel() throws SolverException {
    long z3model = optimize_get_model(z3context, z3optContext);
    return Z3Model.parseZ3Model(mgr, z3context, z3model);
  }

  @Override
  public void close() {
    optimize_dec_ref(z3context, z3optContext);
    z3context = 0;
    z3optContext = 0;
  }

  /**
   * Replace the epsilon in the returned formula with a numeric value.
   */
  private long replaceEpsilon(long ast, int newValue) {
    // TODO: due to the bug in Z3 only integral substitutions are allowed
    Z3Formula z = new Z3RationalFormula(z3context, ast);

    Z3Formula epsFormula =
        (Z3Formula)mgr.getIntegerFormulaManager().makeVariable("epsilon");

    // TODO: make the substitution for epsilon configurable.
    Z3Formula out = mgr.getUnsafeFormulaManager().substitute(
        z,
        ImmutableList.of(epsFormula),
        ImmutableList.of(
            (Z3Formula)mgr.getIntegerFormulaManager().makeNumber(newValue))
    );
    return simplify(z3context, out.getExpr());

  }

  private Rational rationalFromZ3AST(long ast) {
    return Rational.ofString(get_numeral_string(z3context, ast));
  }
}
