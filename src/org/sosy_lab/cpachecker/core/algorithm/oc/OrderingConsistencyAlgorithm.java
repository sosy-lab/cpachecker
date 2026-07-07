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
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.TargetLocationCandidateInvariant;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
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

  @Option(secure = true, description = "loop bound of the first exploration round")
  private int initialLoopBound = 5;

  @Option(secure = true, description = "loop bound increase between iterative-deepening rounds")
  private int loopBoundStep = 5;

  @Option(
      secure = true,
      description =
          "give up when the loop bound exceeds this value; a negative value deepens without bound")
  private int finalLoopBound = -1;

  private final Algorithm innerAlgorithm;
  private final ConfigurableProgramAnalysis topCpa;
  private final OrderingConsistencyCPA ocCpa;
  private final CFA cfa;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final OcStatistics statistics = new OcStatistics();

  public OrderingConsistencyAlgorithm(
      Algorithm pInnerAlgorithm,
      ConfigurableProgramAnalysis pCpa,
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      CFA pCfa)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    if (loopBoundStep < 1) {
      throw new InvalidConfigurationException("oc.loopBoundStep must be positive");
    }
    innerAlgorithm = pInnerAlgorithm;
    topCpa = pCpa;
    ocCpa =
        CPAs.retrieveCPAOrFail(
            pCpa, OrderingConsistencyCPA.class, OrderingConsistencyAlgorithm.class);
    cfa = pCfa;
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    AlgorithmStatus status = AlgorithmStatus.SOUND_AND_PRECISE;
    int bound = initialLoopBound;
    while (true) {
      shutdownNotifier.shutdownIfNecessary();
      statistics.rounds++;
      statistics.lastLoopBound = bound;
      ocCpa.resetExploration(bound);
      resetReachedSet(pReachedSet);

      statistics.explorationTimer.start();
      try {
        // the inner algorithm returns at every target state; drain the waitlist completely
        do {
          status = status.update(innerAlgorithm.run(pReachedSet));
        } while (pReachedSet.hasWaitingState());
      } finally {
        statistics.explorationTimer.stop();
      }

      RoundResult result = solveRound();
      if (result == RoundResult.VIOLATION) {
        logger.log(Level.INFO, "Ordering-consistency analysis found a consistent violation.");
        return status; // target states stay in the reached set
      }
      if (result == RoundResult.SAFE) {
        TargetLocationCandidateInvariant.INSTANCE.assumeTruth(pReachedSet);
        return status;
      }

      // safe up to the bound, but a feasible execution may have been cut: deepen or give up
      if (finalLoopBound >= 0 && bound >= finalLoopBound) {
        logger.log(
            Level.WARNING,
            "Ordering-consistency loop bound exhausted at "
                + bound
                + "; the safety result is not sound.");
        TargetLocationCandidateInvariant.INSTANCE.assumeTruth(pReachedSet);
        return status.withSound(false);
      }
      int next = bound + loopBoundStep;
      bound = finalLoopBound >= 0 ? Math.min(next, finalLoopBound) : next;
    }
  }

  private enum RoundResult {
    /** A consistent violating execution exists within the bound. */
    VIOLATION,
    /** No violation, and no feasible execution was cut: the safe verdict is sound. */
    SAFE,
    /** No violation within the bound, but a feasible execution was cut at the loop bound. */
    CUT
  }

  private RoundResult solveRound() throws CPAException, InterruptedException {
    OcExplorationRegistry registry = ocCpa.getRegistry();
    statistics.eventCount = registry.getEvents().size();
    statistics.instanceCount = registry.getInstances().size();
    boolean truncated = registry.isTruncated();
    boolean hasErrorEvents =
        registry.getEvents().stream().anyMatch(e -> e.kind() == EventKind.ERROR);
    if (!hasErrorEvents && !truncated) {
      return RoundResult.SAFE;
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
    try {
      if (hasErrorEvents) {
        try (ProverEnvironment prover =
            solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
          for (BooleanFormula constraint : constraints) {
            prover.addConstraint(constraint);
          }
          for (BooleanFormula constraint : encoder.getJoinConstraints()) {
            prover.addConstraint(constraint);
          }
          prover.addConstraint(encoder.getErrorConstraint());
          boolean violation =
              switch (encoding) {
                case CLOCKS -> !prover.isUnsat();
                case REFINEMENT -> runRefinementLoop(prover, encoder, bfmgr);
              };
          if (violation) {
            if (dumpViolationModel) {
              dumpViolationModel(prover, encoder);
            }
            return RoundResult.VIOLATION;
          }
        }
      }
      if (!truncated) {
        return RoundResult.SAFE;
      }
      return unwindingAssertionHolds(encoder, constraints, bfmgr)
          ? RoundResult.SAFE
          : RoundResult.CUT;
    } catch (SolverException e) {
      throw new CPAException("solving the ordering-consistency encoding failed", e);
    } finally {
      statistics.solvingTimer.stop();
    }
  }

  /**
   * The unwinding assertion: no feasible execution reaches any loop-bound cut point, so the
   * structural truncation is harmless and a safe verdict is sound. Checking without the ordering
   * constraints over-approximates reachability of the cuts, which errs only towards deepening.
   */
  private boolean unwindingAssertionHolds(
      OcEncoder pEncoder, List<BooleanFormula> pConstraints, BooleanFormulaManagerView pBfmgr)
      throws SolverException, InterruptedException {
    List<BooleanFormula> cutGuards = pEncoder.getTruncationGuards();
    if (cutGuards.isEmpty()) {
      return false; // conservative: the flag is set but no cut point was recorded
    }
    try (ProverEnvironment prover = ocCpa.getSolver().newProverEnvironment()) {
      for (BooleanFormula constraint : pConstraints) {
        prover.addConstraint(constraint);
      }
      prover.addConstraint(pBfmgr.or(cutGuards));
      boolean holds = prover.isUnsat();
      if (holds) {
        statistics.unwindingProofs++;
      }
      return holds;
    }
  }

  private void resetReachedSet(ReachedSet pReachedSet) throws InterruptedException {
    pReachedSet.clear();
    CFANode entry = cfa.getMainFunction();
    StateSpacePartition partition = StateSpacePartition.getDefaultPartition();
    pReachedSet.add(
        topCpa.getInitialState(entry, partition), topCpa.getInitialPrecision(entry, partition));
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
    private int rounds;
    private int lastLoopBound;
    private int unwindingProofs;

    @Override
    public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
      pOut.println("Deepening rounds:                    " + rounds);
      pOut.println("Final loop bound:                    " + lastLoopBound);
      pOut.println("Unwinding-assertion proofs:          " + unwindingProofs);
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
