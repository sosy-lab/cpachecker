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
      FormulaContext context,
      Solvers quantifierSolver)
      throws InvalidConfigurationException, CPATransferException, InterruptedException {
    return switch (pRefinementStrategy) {
      case ALLSAT -> {
        context.getLogger().log(Level.INFO, "Initializing AllSatRefiner...");
        yield new AllSatRefiner(context);
      }
      case QUANTIFIER_ELIMINATION -> {
        context.getLogger().log(Level.INFO, "Initializing QuantifierEliminationRefiner...");
        yield new QuantiferEliminationRefiner(context, quantifierSolver);
      }
      case GENERATE_MODEL -> {
        context.getLogger().log(Level.INFO, "Initializing GenerateModelRefiner...");
        yield new GenerateModelRefiner(context);
      }
    };
  }
}