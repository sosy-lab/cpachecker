// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.oc;

import java.io.PrintStream;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.TargetLocationCandidateInvariant;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.oc.EventKind;
import org.sosy_lab.cpachecker.cpa.oc.OcExplorationRegistry;
import org.sosy_lab.cpachecker.cpa.oc.OrderingConsistencyCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Model;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * Bounded ordering-consistency analysis: after the wrapped exploration has built the per-thread
 * event trees, the collected events are encoded into an SMT formula whose models are candidate
 * violating executions; consistency of the happens-before relation is either checked lazily with
 * conflict-clause refinement or encoded eagerly with integer clocks.
 */
@Options(prefix = "oc")
public class OrderingConsistencyAlgorithm implements Algorithm, StatisticsProvider {

  /** How the ordering constraints are decided. */
  public enum EncodingMode {
    /** Lazy: solve, check the model's event graph for cycles, add conflict clauses, repeat. */
    REFINEMENT,
    /** Eager: integer clock variables per event, single solver query. */
    CLOCKS,
  }

  @Option(secure = true, description = "how the ordering constraints are decided")
  private EncodingMode encoding = EncodingMode.REFINEMENT;

  @Option(
      secure = true,
      description = "log the enabled events and read-from choices of a found violation model")
  private boolean dumpViolationModel = false;

  private final Algorithm innerAlgorithm;
  private final OrderingConsistencyCPA ocCpa;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final OcStatistics statistics = new OcStatistics();

  public OrderingConsistencyAlgorithm(
      Algorithm pInnerAlgorithm,
      ConfigurableProgramAnalysis pCpa,
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    innerAlgorithm = pInnerAlgorithm;
    ocCpa =
        CPAs.retrieveCPAOrFail(
            pCpa, OrderingConsistencyCPA.class, OrderingConsistencyAlgorithm.class);
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    AlgorithmStatus status = AlgorithmStatus.SOUND_AND_PRECISE;
    statistics.explorationTimer.start();
    try {
      // the inner algorithm returns at every target state; drain the waitlist completely
      do {
        status = status.update(innerAlgorithm.run(pReachedSet));
      } while (pReachedSet.hasWaitingState());
    } finally {
      statistics.explorationTimer.stop();
    }

    OcExplorationRegistry registry = ocCpa.getRegistry();
    statistics.eventCount = registry.getEvents().size();
    statistics.instanceCount = registry.getInstances().size();
    boolean truncated = registry.isTruncated();
    boolean hasErrorEvents =
        registry.getEvents().stream().anyMatch(e -> e.kind() == EventKind.ERROR);
    if (!hasErrorEvents) {
      return finishSafe(pReachedSet, status, truncated);
    }

    Solver solver = ocCpa.getSolver();
    BooleanFormulaManagerView bfmgr = solver.getFormulaManager().getBooleanFormulaManager();

    statistics.encodingTimer.start();
    OcEncoder encoder;
    List<BooleanFormula> constraints;
    try {
      encoder =
          new OcEncoder(registry, solver.getFormulaManager(), encoding == EncodingMode.REFINEMENT);
      constraints = encoder.getBaseConstraints();
      if (encoding == EncodingMode.CLOCKS) {
        constraints.addAll(encoder.getClockConstraints());
      }
      statistics.rfCount = encoder.getRfPairs().size();
      statistics.wsCount = encoder.getWsPairs().size();
      statistics.csCount = encoder.getCsPairs().size();
    } finally {
      statistics.encodingTimer.stop();
    }

    statistics.solvingTimer.start();
    try (ProverEnvironment prover = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      for (BooleanFormula constraint : constraints) {
        prover.addConstraint(constraint);
      }
      boolean violation =
          switch (encoding) {
            case CLOCKS -> !prover.isUnsat();
            case REFINEMENT -> runRefinementLoop(prover, encoder, bfmgr);
          };
      if (violation) {
        logger.log(Level.INFO, "Ordering-consistency analysis found a consistent violation.");
        if (dumpViolationModel) {
          dumpViolationModel(prover, encoder);
        }
        return status; // target states stay in the reached set
      }
      return finishSafe(pReachedSet, status, truncated);
    } catch (SolverException e) {
      throw new CPAException("solving the ordering-consistency encoding failed", e);
    } finally {
      statistics.solvingTimer.stop();
    }
  }

  /** Logs the enabled events (with clock values in CLOCKS mode) and read-from choices. */
  private void dumpViolationModel(ProverEnvironment pProver, OcEncoder pEncoder)
      throws SolverException, InterruptedException {
    var imgr = ocCpa.getSolver().getFormulaManager().getIntegerFormulaManager();
    StringBuilder dump = new StringBuilder("violation model:");
    try (Model model = pProver.getModel()) {
      for (var event : pEncoder.getEvents()) {
        if (!Boolean.TRUE.equals(model.evaluate(pEncoder.getFullGuard(event)))) {
          continue;
        }
        Object clock =
            encoding == EncodingMode.CLOCKS
                ? model.evaluate(imgr.makeVariable("__oc_clk_" + event.id()))
                : "";
        Object value = event.variable() == null ? "" : model.evaluate(event.variable());
        dump.append(
            String.format(
                "%n  clk=%6s e%-3d T%d %-6s %-16s %s",
                clock,
                event.id(),
                event.instanceId(),
                event.kind(),
                event.cssaName() == null ? "" : event.cssaName(),
                value));
      }
      for (var rf : pEncoder.getRfPairs()) {
        if (Boolean.TRUE.equals(model.evaluate(rf.variable()))) {
          dump.append(
              String.format(
                  "%n  rf: e%d(%s) <- e%d(%s)",
                  rf.read().id(), rf.read().cssaName(), rf.write().id(), rf.write().cssaName()));
        }
      }
    }
    logger.log(Level.INFO, dump.toString());
  }

  /** Returns true if a consistent violating model exists. */
  private boolean runRefinementLoop(
      ProverEnvironment pProver, OcEncoder pEncoder, BooleanFormulaManagerView pBfmgr)
      throws SolverException, InterruptedException {
    while (true) {
      shutdownNotifier.shutdownIfNecessary();
      if (pProver.isUnsat()) {
        return false;
      }
      statistics.refinementIterations++;
      List<BooleanFormula> conflicts;
      try (Model model = pProver.getModel()) {
        conflicts = ConsistencyChecker.findConflicts(pEncoder, model, pBfmgr);
      }
      if (conflicts.isEmpty()) {
        return true;
      }
      for (BooleanFormula conflict : conflicts) {
        pProver.addConstraint(pBfmgr.not(conflict));
        statistics.conflictClauses++;
      }
    }
  }

  private AlgorithmStatus finishSafe(
      ReachedSet pReachedSet, AlgorithmStatus pStatus, boolean pTruncated) {
    TargetLocationCandidateInvariant.INSTANCE.assumeTruth(pReachedSet);
    if (pTruncated) {
      logger.log(
          Level.WARNING,
          "Ordering-consistency analysis cut some paths at the loop bound;"
              + " the safety result is not sound.");
      return pStatus.withSound(false);
    }
    return pStatus;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (innerAlgorithm instanceof StatisticsProvider provider) {
      provider.collectStatistics(pStatsCollection);
    }
    pStatsCollection.add(statistics);
  }

  private static final class OcStatistics implements Statistics {
    private final Timer explorationTimer = new Timer();
    private final Timer encodingTimer = new Timer();
    private final Timer solvingTimer = new Timer();
    private int eventCount;
    private int instanceCount;
    private int rfCount;
    private int wsCount;
    private int csCount;
    private int refinementIterations;
    private int conflictClauses;

    @Override
    public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
      pOut.println("Thread instances:                    " + instanceCount);
      pOut.println("Events:                              " + eventCount);
      pOut.println("Read-from pairs:                     " + rfCount);
      pOut.println("Write-serialization pairs:           " + wsCount);
      pOut.println("Critical-section pairs:              " + csCount);
      pOut.println("Refinement iterations:               " + refinementIterations);
      pOut.println("Conflict clauses:                    " + conflictClauses);
      pOut.println("Time for exploration:                " + explorationTimer);
      pOut.println("Time for encoding:                   " + encodingTimer);
      pOut.println("Time for solving:                    " + solvingTimer);
    }

    @Override
    public String getName() {
      return "Ordering-consistency algorithm";
    }
  }
}
