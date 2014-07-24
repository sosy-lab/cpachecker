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
import static org.sosy_lab.cpachecker.util.predicates.z3.Z3NativeApiConstants.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.sosy_lab.cpachecker.core.counterexample.Model;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.InterpolatingProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.z3.Z3NativeApi.PointerToLong;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Longs;

public class Z3InterpolatingProver implements InterpolatingProverEnvironment<Long> {

  private final Z3FormulaManager mgr;
  private long z3context;
  private long z3solver;
  private final Z3SmtLogger smtLogger;
  private int level = 0;
  private List<Long> assertedFormulas = new LinkedList<>();

  public Z3InterpolatingProver(Z3FormulaManager mgr) {
    this.mgr = mgr;
    this.z3context = mgr.getEnvironment();
    this.z3solver = mk_solver(z3context);
    solver_inc_ref(z3context, z3solver);
    this.smtLogger = mgr.getSmtLogger();
  }

  @Override
  public Long push(BooleanFormula f) {
    level++;

    long e = Z3FormulaManager.getZ3Expr(f);
    solver_push(z3context, z3solver);

    if (mgr.simplifyFormulas) {
      e = simplify(z3context, e);
      inc_ref(z3context, e);
    }

    solver_assert(z3context, z3solver, e);
    assertedFormulas.add(e);

    smtLogger.logPush(1);
    smtLogger.logInterpolationAssert(e);

    return e;
  }

  @Override
  public void pop() {
    level--;

    assertedFormulas.remove(assertedFormulas.size() - 1); // remove last
    solver_pop(z3context, z3solver, 1);

    smtLogger.logPop(1);
  }

  @Override
  public boolean isUnsat() {
    Preconditions.checkState(z3context != 0);
    Preconditions.checkState(z3solver != 0);
    int result = solver_check(z3context, z3solver);

    smtLogger.logCheck();

    Preconditions.checkState(result != Z3_LBOOL.Z3_L_UNDEF.status);
    return result == Z3_LBOOL.Z3_L_FALSE.status;
  }

  @Override
  public BooleanFormula getInterpolant(List<Long> formulasOfA) {

    // calc difference: formulasOfB := assertedFormulas - formulasOfA
    List<Long> formulasOfB = new ArrayList<>();
    for (long af : assertedFormulas) {
      if (!formulasOfA.contains(af)) {
        formulasOfB.add(af);
      }
    }

    // build 2 groups:  (and A1 A2 A3...) , (and B1 B2 B3...)
    assert formulasOfA.size() != 0;
    assert formulasOfB.size() != 0;
    long[] groupA = Longs.toArray(formulasOfA);
    long[] groupB = Longs.toArray(formulasOfB);
    long fA = mk_and(z3context, groupA);
    inc_ref(z3context, fA);
    long fB = mk_and(z3context, groupB);
    inc_ref(z3context, fB);

    // 2 groups -> 1 interpolant
    long[] itps = new long[1];
    itps[0] = 1; // initialize with value != 0

    PointerToLong labels = new PointerToLong();
    PointerToLong model = new PointerToLong();

    // next lines are not needed due to a direct implementation in the C-code.
    //    long options = mk_params(z3context);
    //    inc_ref(z3context, options);
    //    int[] parents = new int[0]; // this line is not working
    long[] theory = new long[0]; // do we need a theory?

    long[] interpolationFormulas = new long[] { fA, fB };

    smtLogger.logInterpolation(formulasOfA, formulasOfB, fA, fB);

    // get interpolant of groups
    int isSat = interpolateSeq(
        z3context, interpolationFormulas, itps, model, labels, 0, theory);

    assert isSat != Z3_LBOOL.Z3_L_TRUE.status;
    BooleanFormula f = mgr.encapsulate(BooleanFormula.class, itps[0]);

    // cleanup
    dec_ref(z3context, fA);
    dec_ref(z3context, fB);

    return f;
  }

  @Override
  public Model getModel() {
    Z3Model modelCreator = new Z3Model(mgr, z3context, z3solver);
    return modelCreator.createZ3Model();
  }

  @Override
  public void close() {
    Preconditions.checkState(z3context != 0);
    Preconditions.checkState(z3solver != 0);

    while (level > 0) { // TODO do we need this?
      pop();
    }

    assertedFormulas = null;
    //TODO solver_reset(z3context, z3solver);
    solver_dec_ref(z3context, z3solver);
    z3context = 0;
    z3solver = 0;
  }
}