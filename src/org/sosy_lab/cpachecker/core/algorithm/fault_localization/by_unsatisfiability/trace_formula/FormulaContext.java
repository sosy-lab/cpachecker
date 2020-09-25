/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
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

package org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;

public class FormulaContext {

  private Solver solver;
  private PathFormulaManagerImpl manager;
  private ProverEnvironment prover;
  private CFA cfa;
  private LogManager logger;
  private Configuration configuration;
  private ShutdownNotifier shutdownNotifier;

  /**
   * This class maintains the most often used objects in fault localization.
   *
   * @param pSolver the solver for BooleanFormulas
   * @param pManager manager to concatenate CFAEdges to a boolean formula
   * @param pConfiguration configuration settings
   * @param pLogManager the logger
   * @param pMutableCFA the mutable CFA
   * @param pShutdownNotifier the shutdown notifier
   */
  public FormulaContext(
      Solver pSolver,
      PathFormulaManagerImpl pManager,
      CFA pMutableCFA,
      LogManager pLogManager,
      Configuration pConfiguration,
      ShutdownNotifier pShutdownNotifier) {
    solver = pSolver;
    manager = pManager;
    prover = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS);
    cfa = pMutableCFA;
    logger = pLogManager;
    configuration = pConfiguration;
    shutdownNotifier = pShutdownNotifier;
  }

  public Solver getSolver() {
    return solver;
  }

  public PathFormulaManagerImpl getManager() {
    return manager;
  }

  public ProverEnvironment getProver() {
    return prover;
  }

  public LogManager getLogger() {
    return logger;
  }

  public Configuration getConfiguration() {
    return configuration;
  }

  public CFA getMutableCFA() {
    return cfa;
  }

  public ShutdownNotifier getShutdownNotifier() {
    return shutdownNotifier;
  }
}
