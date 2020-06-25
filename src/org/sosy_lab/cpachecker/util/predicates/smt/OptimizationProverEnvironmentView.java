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
package org.sosy_lab.cpachecker.util.predicates.smt;

import java.util.Optional;
import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.OptimizationProverEnvironment;
import org.sosy_lab.java_smt.api.SolverException;

/** Wrapper for {@link OptimizationProverEnvironment} which unwraps the objective formula. */
class OptimizationProverEnvironmentView extends BasicProverEnvironmentView<Void>
    implements OptimizationProverEnvironment {

  private final OptimizationProverEnvironment delegate;
  private final FormulaWrappingHandler wrappingHandler;

  OptimizationProverEnvironmentView(
      OptimizationProverEnvironment pDelegate,
      FormulaManagerView pFormulaManager
  ) {
    super(pDelegate, pFormulaManager.getFormulaWrappingHandler());
    delegate = pDelegate;
    wrappingHandler = pFormulaManager.getFormulaWrappingHandler();
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
  public Optional<Rational> upper(int handle, Rational epsilon) {
    return delegate.upper(handle, epsilon);
  }

  @Override
  public Optional<Rational> lower(int handle, Rational epsilon) {
    return delegate.lower(handle, epsilon);
  }
}
