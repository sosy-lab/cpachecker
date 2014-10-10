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
import org.sosy_lab.cpachecker.core.counterexample.Model;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.OptEnvironment;

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
  public void setObjective(Formula objective) {
    logger.log(Level.FINE, "Setting objective: " + objective);
    wrapped.setObjective(objective);
  }

  @Override
  public OptResult maximize() throws InterruptedException {
    logger.log(Level.FINE, "Performing maximization");
    return wrapped.maximize();
  }

  @Override
  public Model getModel() throws SolverException {
    return wrapped.getModel();
  }

  @Override
  public void close() {
    wrapped.close();
    logger.log(Level.FINER, "closed");
  }
}
