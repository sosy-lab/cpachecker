/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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

import java.util.List;

import org.sosy_lab.cpachecker.util.predicates.Model;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.InterpolatingProverEnvironment;

import de.uni_freiburg.informatik.ultimate.logic.Term;

public class Z3InterpolatingProver implements InterpolatingProverEnvironment<Term> {

  private final Z3FormulaManager mgr;
  private final long z3context;
  private final long z3solver;

  public Z3InterpolatingProver(Z3FormulaManager mgr) {
    this.mgr = mgr;
    this.z3context = mgr.getContext();
    this.z3solver = mk_solver(z3context);
    solver_inc_ref(z3context, z3solver);

    // TODO create new interpolantion context?
  }

  @Override
  public Term push(BooleanFormula f) {
    throw new AssertionError("not implemented");
  }

  @Override
  public void pop() {
    throw new AssertionError("not implemented");
  }

  @Override
  public boolean isUnsat() {
    throw new AssertionError("not implemented");
    // TODO env.checkSat() == LBool.UNSAT;
  }

  @Override
  public BooleanFormula getInterpolant(List<Term> formulasOfA) {
    throw new AssertionError("not implemented");

  }

  @Override
  public void close() {
    throw new AssertionError("not implemented");
  }

  @Override
  public Model getModel() {
    Z3Model modelCreator = new Z3Model(mgr, z3context, z3solver);
    return modelCreator.createZ3Model();
  }
}