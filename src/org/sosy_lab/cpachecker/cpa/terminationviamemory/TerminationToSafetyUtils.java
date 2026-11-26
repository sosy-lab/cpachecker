// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.terminationviamemory;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sosy_lab.cpachecker.core.algorithm.termination.validation.well_foundedness.TransitionInvariantUtils.CURR_KEYWORD;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.algorithm.termination.validation.well_foundedness.TransitionInvariantUtils;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class TerminationToSafetyUtils {

  public static void shareTheSolverBetweenCPAs(ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException, InterruptedException, CPAException {
    checkNotNull(pCpa);

    TerminationToReachCPA terminationCPA =
        CPAs.retrieveCPAOrFail(pCpa, TerminationToReachCPA.class, TerminationToSafetyUtils.class);
    PredicateCPA predicateCPA =
        CPAs.retrieveCPAOrFail(pCpa, PredicateCPA.class, TerminationToSafetyUtils.class);

    terminationCPA.setSolverAndManagers(
        predicateCPA.getSolver(), predicateCPA.getPathFormulaManager());
  }

  /**
   * Renames the variables of the given invariant to the variables of the state where an iteration
   * of a loop starts, i.e., the variables with the suffix {@link
   * TransitionInvariantUtils#CURR_KEYWORD}.
   */
  static BooleanFormula inCurrentState(BooleanFormula pInvariant, FormulaManagerView pFmgr) {
    return pFmgr.substitute(
        pInvariant,
        ImmutableMap.copyOf(
            Maps.asMap(
                ImmutableSet.copyOf(pFmgr.extractVariables(pInvariant).values()),
                variable ->
                    pFmgr.makeVariable(
                        pFmgr.getFormulaType(variable),
                        pFmgr.uninstantiate(variable).toString() + CURR_KEYWORD))));
  }
}
