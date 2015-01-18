/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.constraints;

import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

/**
 * State for Constraints Analysis. Stores constraints and whether they are solvable in a more efficient way than
 * {@link ConstraintsState}.
 */
public class StackUsingConstraintsState extends ConstraintsState {


  private ProverEnvironment proverEnvironment;

  /**
   * Create a new <code>StackUsingConstraintsState</code> object.
   */
  public StackUsingConstraintsState() {
    // DO NOTHING
  }

  private StackUsingConstraintsState(StackUsingConstraintsState pState) {
    super(pState);
    proverEnvironment = pState.proverEnvironment;
  }

  @Override
  public ConstraintsState copyOf() {
    return new StackUsingConstraintsState(this);
  }

  @Override
  public boolean isInitialized() {
    return proverEnvironment != null;
  }

  @Override
  public void initialize(Solver pSolver, FormulaManagerView pFormulaManager, FormulaCreator<?> pFormulaCreator) {
    super.initialize(pSolver, pFormulaManager, pFormulaCreator);
    proverEnvironment = pSolver.newProverEnvironment();
  }

  @Override
  public boolean add(Constraint pConstraint) {
    super.add(pConstraint);
    addToProverEnvironment(pConstraint, getFormulaCreator());
    return false;
  }

  private void addToProverEnvironment(Constraint pConstraint, FormulaCreator<? extends Formula> pFormulaCreator) {
    BooleanFormula formula = (BooleanFormula) pConstraint.accept(pFormulaCreator);

    proverEnvironment.push(formula);
  }

  @Override
  public boolean isUnsat() throws SolverException, InterruptedException {
    if (isEmpty()) {
      return false;
    } else {
      return proverEnvironment.isUnsat();
    }
  }
}
