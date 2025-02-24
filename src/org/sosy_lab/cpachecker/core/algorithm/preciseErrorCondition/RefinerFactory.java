// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.preciseErrorCondition;

import java.util.logging.Level;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.java_smt.SolverContextFactory.Solvers;

public class RefinerFactory {
  public static Refiner createRefiner(
      RefinementStrategy pRefinementStrategy,
      FormulaContext pContext,
      Solvers pQuantifierSolver,
      Boolean pWithFormatter)
      throws InvalidConfigurationException, CPATransferException, InterruptedException {
    return switch (pRefinementStrategy) {
      case GENERATE_MODEL -> {
        pContext.getLogger().log(Level.INFO, "Initializing GenerateModelRefiner...");
        yield new GenerateModelRefiner(pContext, pWithFormatter);
      }
      case ALLSAT -> {
        pContext.getLogger().log(Level.INFO, "Initializing AllSatRefiner...");
        yield new AllSatRefiner(pContext, pWithFormatter);
      }
      case QUANTIFIER_ELIMINATION -> {
        pContext.getLogger().log(Level.INFO, "Initializing QuantifierEliminationRefiner...");
        yield new QuantiferEliminationRefiner(pContext, pQuantifierSolver, pWithFormatter);
      }
    };
  }
}