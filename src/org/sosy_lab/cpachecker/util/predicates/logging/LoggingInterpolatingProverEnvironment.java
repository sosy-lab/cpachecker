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

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.Model;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.InterpolatingProverEnvironment;


public class LoggingInterpolatingProverEnvironment<T> implements InterpolatingProverEnvironment<T> {

  private final InterpolatingProverEnvironment<T> wrapped;
  private final LogManager logger;
  int level = 0;

  public LoggingInterpolatingProverEnvironment(LogManager logger, InterpolatingProverEnvironment<T> ipe) {
    this.wrapped = ipe;
    this.logger = logger;
  }

  @Override
  public T push(BooleanFormula f) {
    level++;
    logger.log(Level.FINER, "up to level " + level);
    logger.log(Level.FINE, "formula pushed:", f);
    T result = wrapped.push(f);
    return result;
  }

  @Override
  public void pop() {
    level--;
    logger.log(Level.FINER, "down to level " + level);
    wrapped.pop();
  }

  @Override
  public boolean isUnsat() throws InterruptedException {
    boolean result = wrapped.isUnsat();
    logger.log(Level.FINE, "unsat-check returned:", result);
    return result;
  }

  @Override
  public BooleanFormula getInterpolant(List<T> formulasOfA) {
    logger.log(Level.FINE, "formulasOfA:", Arrays.toString(formulasOfA.toArray()));
    BooleanFormula bf = wrapped.getInterpolant(formulasOfA);
    logger.log(Level.FINE, "interpolant:", bf);
    return bf;
  }

  @Override
  public Model getModel() throws SolverException {
    Model m = wrapped.getModel();
    logger.log(Level.FINE, "model", m);
    return m;
  }

  @Override
  public void close() {
    wrapped.close();
    logger.log(Level.FINER, "closed");
  }

}
