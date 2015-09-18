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
package org.sosy_lab.solver.logging;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.logging.Level;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.solver.Model;
import org.sosy_lab.solver.SolverException;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.Formula;
import org.sosy_lab.solver.api.ProverEnvironment;

/**
 * Wraps a prover environment with a logging object.
 */
public class LoggingProverEnvironment implements ProverEnvironment {

  private final ProverEnvironment wrapped;
  LogManager logger;
  int level = 0;

  public LoggingProverEnvironment(LogManager logger, ProverEnvironment pe) {
    this.wrapped = checkNotNull(pe);
    this.logger = checkNotNull(logger);
  }

  @Override
  public Void push(BooleanFormula f) {
    logger.log(Level.FINE, "up to level " + level++);
    logger.log(Level.FINE, "formula pushed:", f);
    return wrapped.push(f);
  }

  @Override
  public void pop() {
    logger.log(Level.FINER, "down to level " + level--);
    wrapped.pop();
  }

  @Override
  public boolean isUnsat() throws SolverException, InterruptedException {
    boolean result = wrapped.isUnsat();
    logger.log(Level.FINE, "unsat-check returned:", result);
    return result;
  }

  @Override
  public Model getModel() throws SolverException {
    Model m = wrapped.getModel();
    logger.log(Level.FINE, "model", m);
    return m;
  }

  @Override
  public List<BooleanFormula> getUnsatCore() {
    List<BooleanFormula> unsatCore = wrapped.getUnsatCore();
    logger.log(Level.FINE, "unsat-core", unsatCore);
    return unsatCore;
  }

  @Override
  public <T> T allSat(AllSatCallback<T> callback,
      List<BooleanFormula> important)
      throws InterruptedException, SolverException {
    T result = wrapped.allSat(callback, important);
    logger.log(Level.FINE, "allsat-result:", result);
    return result;
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

}