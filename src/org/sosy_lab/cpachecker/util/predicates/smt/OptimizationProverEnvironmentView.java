// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

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
      OptimizationProverEnvironment pDelegate, FormulaManagerView pFormulaManager) {
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
  public OptStatus check() throws InterruptedException, SolverException {
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
