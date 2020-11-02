// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.summaries;

import static org.sosy_lab.cpachecker.util.AbstractStates.extractStateByType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.location.LocationCPA;
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
 * Status tracked in  <a href="https://gitlab.com/sosy-lab/software/cpachecker/-/issues/796"
 * target="_top">#796</a>.
 * <p>
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
   * Shutdown notifier to check whether shutdown has been requested.
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
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    AlgorithmStatus status = execution.run(pReachedSet);

    logger.log(Level.INFO, "Starting function summarization ...");

    final Map<FunctionEntryNode, ValueAnalysisState> entryStates = new HashMap<>();

    for (final AbstractState state : pReachedSet) {
      final LocationState locationState = extractStateByType(state, LocationState.class);
      final ValueAnalysisState valueState = extractStateByType(state, ValueAnalysisState.class);

      assert (locationState != null && valueState != null);

      if (locationState.getLocationNode() instanceof FunctionEntryNode) {
        final FunctionEntryNode entryNode = (FunctionEntryNode) locationState.getLocationNode();
        entryStates.put(entryNode, valueState);
      }

      if (locationState.getLocationNode() instanceof FunctionExitNode) {
        final FunctionExitNode exitNode = (FunctionExitNode) locationState.getLocationNode();
        final ValueAnalysisState entryState = entryStates.get(exitNode.getEntryNode());

        final AFunctionDeclaration function = exitNode.getFunction();
        createSummaryForStates(function, entryState, valueState);
      }
    }

    logger.log(Level.INFO, "Stopping function summarization (finished) ...");
    return status;
  }

  /**
   * Internal utility method to generate a summary for the control flow within the pFunction defined
   * by {@link AFunctionDeclaration}, between the two {@link ValueAnalysisState} <code>pEntryState
   * </code> and <code>pExitState</code>. Summaries are currently represented as plain {@link
   * String}, and logged with <code>Level.FINE</code>.
   *
   * @param pFunction Declaration of the pFunction for which the summary is created.
   * @param pEntryState Value information of the abstract state in which the pFunction is entered.
   *     <br>
   *     If {@link LocationCPA} was active in parallel, the corresponding {@link LocationState} in
   *     the composite abstract state is a {@link FunctionEntryNode}.
   * @param pExitState Value information of the abstract state in which the pFunction is left.<br>
   *     If {@link LocationCPA} was active in parallel, the corresponding {@link LocationState} in
   *     the composite abstract state is a {@link FunctionExitNode}.
   */
  private void createSummaryForStates(
      final AFunctionDeclaration pFunction,
      final ValueAnalysisState pEntryState,
      final ValueAnalysisState pExitState) {

    final Optional<MemoryLocation> returnValue =
        pExitState.getTrackedMemoryLocations().stream()
            .filter(memoryLocation -> memoryLocation.getIdentifier().equals("__retval__"))
            .findFirst();

    assert returnValue.isPresent();

    final Value value = pExitState.getValueFor(returnValue.get());

    if (value instanceof SymbolicValue) {
      final SymbolicValue symbolicValue = (SymbolicValue) value;

      final SymbolicValueToSummaryTransformer transformer =
          new SymbolicValueToSummaryTransformer(pFunction, pEntryState);

      String summary = symbolicValue.accept(transformer).toString();
      logger.log(Level.FINE, summary, "\n");
    }
  }
}