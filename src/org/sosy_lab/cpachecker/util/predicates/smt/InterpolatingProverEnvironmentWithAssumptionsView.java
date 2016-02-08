/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.smt;

import java.util.List;
import java.util.Set;

import org.sosy_lab.solver.SolverException;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.InterpolatingProverEnvironmentWithAssumptions;
import org.sosy_lab.solver.api.Model;

/**
 * Model wrapping for InterpolatingProverEnvironment
 */
public class InterpolatingProverEnvironmentWithAssumptionsView<E> implements
    InterpolatingProverEnvironmentWithAssumptions<E> {

  private final InterpolatingProverEnvironmentWithAssumptions<E> delegate;
  private final FormulaWrappingHandler wrappingHandler;

  public InterpolatingProverEnvironmentWithAssumptionsView(
      InterpolatingProverEnvironmentWithAssumptions<E> pDelegate,
      FormulaWrappingHandler pWrappingHandler) {
    delegate = pDelegate;
    wrappingHandler = pWrappingHandler;
  }

  @Override
  public E push(BooleanFormula f) {
    return delegate.push(f);
  }

  @Override
  public void pop() {
    delegate.pop();
  }

  @Override
  public E addConstraint(BooleanFormula constraint) {
    return delegate.addConstraint(constraint);
  }

  @Override
  public void push() {
    delegate.push();
  }

  @Override
  public boolean isUnsat() throws SolverException, InterruptedException {
    return delegate.isUnsat();
  }

  @Override
  public Model getModel() throws SolverException {
    return new ModelView(delegate.getModel(), wrappingHandler);
  }

  @Override
  public void close() {
    delegate.close();
  }

  @Override
  public BooleanFormula getInterpolant(List<E> formulasOfA)
      throws SolverException {
    return delegate.getInterpolant(formulasOfA);
  }

  @Override
  public List<BooleanFormula> getSeqInterpolants(
      List<Set<E>> partitionedFormulas) throws SolverException {
    return delegate.getSeqInterpolants(partitionedFormulas);
  }

  @Override
  public List<BooleanFormula> getTreeInterpolants(
      List<Set<E>> partitionedFormulas, int[] startOfSubTree) throws SolverException {
    return delegate.getTreeInterpolants(partitionedFormulas, startOfSubTree);
  }

  @Override
  public boolean isUnsatWithAssumptions(List<BooleanFormula> assumptions)
      throws SolverException, InterruptedException {
    return delegate.isUnsatWithAssumptions(assumptions);
  }
}
