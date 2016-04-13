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

import com.google.common.base.Optional;

import org.sosy_lab.solver.SolverException;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.Model;
import org.sosy_lab.solver.api.ProverEnvironment;

import java.util.List;

import javax.annotation.Nullable;

/**
 * Wrapping handler for ProverEnvironment.
 */
public class ProverEnvironmentView implements ProverEnvironment{
  private final ProverEnvironment delegate;
  private final FormulaWrappingHandler wrappingHandler;

  public ProverEnvironmentView(ProverEnvironment pDelegate,
      FormulaWrappingHandler pWrappingHandler) {
    delegate = pDelegate;
    wrappingHandler = pWrappingHandler;
  }

  @Override
  public List<BooleanFormula> getUnsatCore() {
    return delegate.getUnsatCore();
  }

  @Override
  public <T> T allSat(AllSatCallback<T> callback,
      List<BooleanFormula> important)
      throws InterruptedException, SolverException {
    return delegate.allSat(callback, important);
  }

  @Override
  public boolean isUnsatWithAssumptions(List<BooleanFormula> assumptions)
      throws SolverException, InterruptedException {
    return delegate.isUnsatWithAssumptions(assumptions);
  }

  @Override
  public Optional<List<BooleanFormula>> unsatCoreOverAssumptions(
      List<BooleanFormula> assumptions)
      throws SolverException, InterruptedException {
    return delegate.unsatCoreOverAssumptions(assumptions);
  }

  @Nullable
  @Override
  public Void push(BooleanFormula f) {
    return delegate.push(f);
  }

  @Override
  public void pop() {
    delegate.pop();
  }

  @Override
  public Void addConstraint(BooleanFormula constraint) {
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
}
