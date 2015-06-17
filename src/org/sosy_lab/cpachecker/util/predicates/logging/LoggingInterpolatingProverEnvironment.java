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

import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.Model;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.InterpolatingProverEnvironmentWithAssumptions;


public class LoggingInterpolatingProverEnvironment<T> implements InterpolatingProverEnvironmentWithAssumptions<T> {

  private final InterpolatingProverEnvironmentWithAssumptions<T> wrapped;
  private final LogManager logger;
  int level = 0;

  public LoggingInterpolatingProverEnvironment(LogManager logger, InterpolatingProverEnvironmentWithAssumptions<T> ipe) {
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
  public boolean isUnsat() throws InterruptedException, SolverException {
    boolean result = wrapped.isUnsat();
    logger.log(Level.FINE, "unsat-check returned:", result);
    return result;
  }

  @Override
  public boolean isUnsatWithAssumptions(List<BooleanFormula> pAssumptions) throws SolverException, InterruptedException {
    logger.log(Level.FINE, "assumptions:", pAssumptions);
    boolean result = wrapped.isUnsatWithAssumptions(pAssumptions);
    logger.log(Level.FINE, "unsat-check returned:", result);
    return result;
  }

  @Override
  public BooleanFormula getInterpolant(List<T> formulasOfA) throws SolverException {
    logger.log(Level.FINE, "formulasOfA:", formulasOfA);
    BooleanFormula bf = wrapped.getInterpolant(formulasOfA);
    logger.log(Level.FINE, "interpolant:", bf);
    return bf;
  }

  @Override
  public List<BooleanFormula> getSeqInterpolants(List<Set<T>> partitionedFormulas) {
    logger.log(Level.FINE, "formulasOfA:", partitionedFormulas);
    List<BooleanFormula> bf = wrapped.getSeqInterpolants(partitionedFormulas);
    logger.log(Level.FINE, "interpolants:", bf);
    return bf;
  }

  @Override
  public List<BooleanFormula> getTreeInterpolants(List<Set<T>> partitionedFormulas, int[] startOfSubTree) {
    logger.log(Level.FINE, "formulasOfA:", partitionedFormulas);
    logger.log(Level.FINE, "startOfSubTree:", startOfSubTree);
    List<BooleanFormula> bf = wrapped.getTreeInterpolants(partitionedFormulas, startOfSubTree);
    logger.log(Level.FINE, "interpolants:", bf);
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
