// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.summaries;

import static org.sosy_lab.cpachecker.util.AbstractStates.extractStateByType;

import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * {@link SummaryAlgorithm} implements configurable code summarization.<br/>
 * Status is tracked in #796.<p>
 *
 * For now, it is limited to<br/>
 * a) functions (summaries are only prepared for complete functions, not arbitrary code blocks),
 *    <br/>
 * b) information from symbolic execution (which must be executed prior)<br/>
 * c) simple textual summaries just represented as plain {@link String}.
 */
public class SummaryAlgorithm implements Algorithm {

  /**
   * Algorithm which performs the symbolic execution.
   */
  private final Algorithm execution;

  /**
   * Log manager to produce log messages.
   */
  private final LogManager logger;

  /**
   * Shutdown notifier which is used check whether shutdown has been requested.
   */
  @SuppressWarnings("FieldCanBeLocal")
  private final ShutdownNotifier shutdownNotifier;

  /**
   * CPAchecker configuration with which the tool currently operates.
   */
  @SuppressWarnings("FieldCanBeLocal")
  private final Configuration config;

  /**
   * Formula manager for formula management.
   */
  @SuppressWarnings({"FieldCanBeLocal", "unused"})
  private final FormulaManagerView fmgr;

  private SummaryAlgorithm(
      final Algorithm pSymbolicExecution,
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier) throws InvalidConfigurationException {
    execution = pSymbolicExecution;
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    config = pConfig;

    final Solver solver = Solver.create(config, logger, shutdownNotifier);
    fmgr = solver.getFormulaManager();
  }

  public static Algorithm create(
      Algorithm pSymbolicExecution,
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    return new SummaryAlgorithm(pSymbolicExecution, pConfig, pLogger, pShutdownNotifier);
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {
    AlgorithmStatus status = execution.run(reachedSet);

    logger.log(Level.INFO, "Starting function summarization ...");

    for(AbstractState state : reachedSet) {
      final ValueAnalysisState valueState = extractStateByType(state, ValueAnalysisState.class);
      final LocationState locationState = extractStateByType(state, LocationState.class);

      final boolean requiredStatesExist = (locationState != null) && (valueState != null);

      if (requiredStatesExist && locationState.getLocationNode() instanceof FunctionExitNode) {
        createSummaryForState(locationState, valueState);
      }
    }

    logger.log(Level.INFO, "Stopping function summarization (finished) ...");
    return status;
  }

  /**
   * Internal utility method to generate a summary from {@link LocationState} and
   * {@link ValueAnalysisState} of an abstract state.<br/>
   * Summaries are currently represented as just plain {@link String}, and can only be created at
   * function exit locations.
   *
   * @param locationState Location information of the abstract state. Used to derive the function
   *                      signature in the summary.
   *
   * @param valueState Value information of the abstract state. Used to produce the actual summary.
   */
  private void createSummaryForState(
      final LocationState locationState, final ValueAnalysisState valueState) {
    final Optional<MemoryLocation> returnValue =
        valueState.getTrackedMemoryLocations().stream()
        .filter(memoryLocation -> memoryLocation.getIdentifier().equals("__retval__"))
        .findFirst();

    if(returnValue.isEmpty())
    {
      return;
    }

    final Value value = valueState.getValueAndTypeFor(returnValue.get()).getValue();

    if (value instanceof SymbolicValue) {
      final SymbolicValue symbolicValue = (SymbolicValue) value;

      final SymbolicValueToSummaryTransformer transformer =
        new SymbolicValueToSummaryTransformer(locationState, valueState);

        String summary = symbolicValue.accept(transformer).toString();
        logger.log(Level.FINE, summary, "\n");
    }
  }
}