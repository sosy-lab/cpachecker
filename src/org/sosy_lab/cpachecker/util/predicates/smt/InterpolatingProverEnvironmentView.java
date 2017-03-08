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

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.InterpolatingProverEnvironment;
import org.sosy_lab.java_smt.api.InterpolationHandle;
import org.sosy_lab.java_smt.api.Model;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * Model wrapping for InterpolatingProverEnvironment
 */
class InterpolatingProverEnvironmentView implements InterpolatingProverEnvironment {

  private final InterpolatingProverEnvironment delegate;
  private final FormulaWrappingHandler wrappingHandler;

  InterpolatingProverEnvironmentView(
      InterpolatingProverEnvironment pDelegate, FormulaWrappingHandler pWrappingHandler) {
    delegate = pDelegate;
    wrappingHandler = pWrappingHandler;
  }

  @Override
  public InterpolationHandle push(BooleanFormula f) {
    return delegate.push(f);
  }

  @Override
  public void pop() {
    delegate.pop();
  }

  @Override
  public InterpolationHandle addConstraint(BooleanFormula constraint) {
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
  public ImmutableList<ValueAssignment> getModelAssignments() throws SolverException {
    return ProverEnvironmentView.fixModelAssignments(delegate.getModelAssignments());
  }

  @Override
  public void close() {
    delegate.close();
  }

  @Override
  public List<BooleanFormula> getSeqInterpolants(
      List<? extends Iterable<InterpolationHandle>> partitionedFormulas)
      throws SolverException, InterruptedException {
    return delegate.getSeqInterpolants(partitionedFormulas);
  }

  @Override
  public List<BooleanFormula> getTreeInterpolants(
      List<? extends Iterable<InterpolationHandle>> partitionedFormulas, int[] startOfSubTree)
      throws SolverException, InterruptedException {
    return delegate.getTreeInterpolants(partitionedFormulas, startOfSubTree);
  }

  @Override
  public boolean isUnsatWithAssumptions(Collection<BooleanFormula> assumptions)
      throws SolverException, InterruptedException {
    return delegate.isUnsatWithAssumptions(assumptions);
  }
}
