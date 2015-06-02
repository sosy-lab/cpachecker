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
package org.sosy_lab.cpachecker.util.predicates.interfaces.view;

import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.Model;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.OptEnvironment;

import com.google.common.base.Optional;

/**
 * Wrapper for {@link OptEnvironment} which unwraps the objective formula.
 */
public class OptEnvironmentView implements OptEnvironment {
  private final OptEnvironment delegate;
  private final FormulaWrappingHandler wrappingHandler;

  public OptEnvironmentView(
      OptEnvironment pDelegate,
      FormulaManagerView pFormulaManager
  ) {
    delegate = pDelegate;
    wrappingHandler = pFormulaManager.getFormulaWrappingHandler();
  }


  @Override
  public void addConstraint(BooleanFormula constraint) {
    delegate.addConstraint(constraint);
  }

  @Override
  public int maximize(Formula objective) {
    return delegate.maximize(wrappingHandler.unwrap(objective));
  }

  @Override
  public int minimize(Formula objective) {
    return delegate.minimize(wrappingHandler.unwrap(objective));
  }

  @Override
  public OptStatus check()
      throws InterruptedException, SolverException {
    return delegate.check();
  }

  @Override
  public void push() {
    delegate.push();

  }

  @Override
  public void pop() {
    delegate.pop();
  }

  @Override
  public Optional<Rational> upper(int handle, Rational epsilon) {
    return delegate.upper(handle, epsilon);
  }

  @Override
  public Optional<Rational> lower(int handle, Rational epsilon) {
    return delegate.lower(handle, epsilon);
  }

  @Override
  public Model getModel() throws SolverException {
    return delegate.getModel();
  }

  @Override
  public void close() {
    delegate.close();
  }

  /**
   * Note: the return value is not wrapped as we expect it to be a simple
   * expression.
   */
  public Formula evaluate(Formula f) {
    return  delegate.evaluate(wrappingHandler.unwrap(f));
  }

  /**
   * @return String representation of the constraint system in the SMT-lib
   * format.
   */
  public String dump() {
    return delegate.dump();
  }
}
