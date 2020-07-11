// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.faultlocalization.formula;

import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;

public class FormulaContext {

  private Solver solver;
  private PathFormulaManagerImpl manager;
  private ProverEnvironment prover;
  private ExpressionConverter converter;

  /**
   * This class maintains the most often used objects in fault localization.
   * @param pSolver the solver for BooleanFormulas
   * @param pManager manager to concatenate CFAEdges to a boolean formula
   * @param pConverter converter to convert formulas to infix notation.
   */
  public FormulaContext(Solver pSolver, PathFormulaManagerImpl pManager, ExpressionConverter pConverter) {
    solver = pSolver;
    manager = pManager;
    prover = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS);
    converter = pConverter;
  }

  public ExpressionConverter getConverter() {
    return converter;
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
}
