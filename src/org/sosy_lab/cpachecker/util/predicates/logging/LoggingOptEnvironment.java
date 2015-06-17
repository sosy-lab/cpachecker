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
package org.sosy_lab.cpachecker.util.predicates.logging;

import java.util.logging.Level;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.Model;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.OptEnvironment;

import com.google.common.base.Optional;

/**
 * Wrapper for an optimizing solver.
 */
public class LoggingOptEnvironment implements OptEnvironment {

  private final OptEnvironment wrapped;
  LogManager logger;

  public LoggingOptEnvironment(LogManager logger, OptEnvironment oe) {
    this.wrapped = oe;
    this.logger = logger;
  }

  @Override
  public void addConstraint(BooleanFormula constraint) {
    logger.log(Level.FINE, "Asserting: " + constraint);
    wrapped.addConstraint(constraint);
  }

  @Override
  public int maximize(Formula objective) {
    logger.log(Level.FINE, "Maximizing: " + objective);
    return wrapped.maximize(objective);
  }

  @Override
  public int minimize(Formula objective) {
    logger.log(Level.FINE, "Minimizing: " + objective);
    return wrapped.minimize(objective);
  }

  @Override
  public OptStatus check()
      throws InterruptedException, SolverException {
    logger.log(Level.FINE, "Performing optimization");
    return wrapped.check();
  }

  @Override
  public void push() {
    logger.log(Level.FINE, "Creating backtracking point");
    wrapped.push();
  }

  @Override
  public void pop() {
    logger.log(Level.FINE, "Backtracking one level");
    wrapped.pop();
  }

  @Override
  public Optional<Rational> upper(int handle, Rational epsilon) {
    return wrapped.upper(handle, epsilon);
  }

  @Override
  public Optional<Rational> lower(int handle, Rational epsilon) {
    return wrapped.lower(handle, epsilon);
  }

  @Override
  public Model getModel() throws SolverException {
    return wrapped.getModel();
  }

  @Override
  public Formula evaluate(Formula f) {
    return wrapped.evaluate(f);
  }

  @Override
  public void close() {
    wrapped.close();
    logger.log(Level.FINER, "closed");
  }

  @Override
  public String dump() {
    return wrapped.dump();
  }
}
