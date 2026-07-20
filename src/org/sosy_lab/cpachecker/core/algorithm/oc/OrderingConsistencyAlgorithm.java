// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.oc;

import static com.google.common.base.Strings.nullToEmpty;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.TargetLocationCandidateInvariant;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.specification.Property;
import org.sosy_lab.cpachecker.core.specification.Property.CommonVerificationProperty;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.oc.EventKind;
import org.sosy_lab.cpachecker.cpa.oc.MemoryEvent;
import org.sosy_lab.cpachecker.cpa.oc.OcExplorationRegistry;
import org.sosy_lab.cpachecker.cpa.oc.OrderingConsistencyCPA;
import org.sosy_lab.cpachecker.cpa.oc.OrderingConsistencyState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.IntegerFormulaManagerView;
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
    /**
     * Lazy: solve, check the model's event graph for cycles, add conflict clauses, repeat.
     * Experimental — the derivation loop can reject real violations, so it may report a safe
     * verdict unsoundly; prefer {@link #CLOCKS}.
     */
    REFINEMENT,
    /** Eager: integer clock variables per event, single solver query. The sound default. */
    CLOCKS,
  }

  @Option(secure = true, description = "how the ordering constraints are decided")
  private EncodingMode encoding = EncodingMode.CLOCKS;

  /**
   * The kind of safety property the analysis checks. Derived from the {@code --spec} property file
   * (see {@link #targetPropertyOf}); with no specification it defaults to {@link #UNREACH_CALL},
   * matching the historical behaviour of detecting calls to the error function.
   */
  private enum TargetProperty {
    /** Reachability of a call to the error function (SV-COMP unreach-call). Always supported. */
    UNREACH_CALL,
    /** Two conflicting concurrent accesses (SV-COMP no-data-race). Requires the CLOCKS encoding. */
    DATA_RACE,
  }

  private final TargetProperty targetProperty;

  @Option(
      secure = true,
      description = "log the enabled events and read-from choices of a found violation model")
  private boolean dumpViolationModel = false;

  @Option(
      secure = true,
      description = "log every collected memory event before solving (debugging aid)")
  private boolean dumpEvents = false;

  @Option(
      secure = true,
      description = "export the found violation as an execution graph in DOT/graphviz format")
  private boolean exportExecutionGraph = true;

  @Option(secure = true, description = "file for the violation execution graph (DOT/graphviz)")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path executionGraphFile = Path.of("executionGraph.dot");

  @Option(
      secure = true,
      description =
          "export the found violation as a sequentialized counterexample: the enabled events in a"
              + " consistent execution order, with their CFA edges")
  private boolean exportSequentializedCounterexample = true;

  @Option(secure = true, description = "file for the sequentialized counterexample trace")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path sequentializedCounterexampleFile = Path.of("Counterexample.oc.txt");

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
      CFA pCfa,
      Specification pSpecification)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    if (loopBoundStep < 1) {
      throw new InvalidConfigurationException("oc.loopBoundStep must be positive");
    }
    Optional<TargetProperty> specTarget = targetPropertyOf(pSpecification);
    if (specTarget.isEmpty()) {
      pLogger.log(
          Level.INFO,
          "No property given; the ordering-consistency analysis checks unreach-call. Pass a"
              + " --spec property file to check something else.");
    }
    targetProperty = specTarget.orElse(TargetProperty.UNREACH_CALL);
    if (targetProperty == TargetProperty.DATA_RACE && encoding != EncodingMode.CLOCKS) {
      throw new InvalidConfigurationException(
          "the ordering-consistency data-race check needs the CLOCKS encoding (set"
              + " oc.encoding=CLOCKS)");
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

  /**
   * Maps the properties of the {@code --spec} specification to the built-in target the analysis can
   * decide. The ordering-consistency analysis does not use specification automata; it interprets
   * the property itself. An empty specification (no {@code --spec}) yields an empty result.
   * Properties the analysis cannot decide (overflow, memory safety/cleanup, deadlock, termination)
   * are rejected rather than silently mis-verified, since an unsound "safe" verdict would be worse
   * than an error.
   */
  private static Optional<TargetProperty> targetPropertyOf(Specification pSpecification)
      throws InvalidConfigurationException {
    EnumSet<TargetProperty> targets = EnumSet.noneOf(TargetProperty.class);
    for (Property property : pSpecification.getProperties()) {
      if (property instanceof CommonVerificationProperty common) {
        switch (common) {
          case REACHABILITY, REACHABILITY_ERROR, REACHABILITY_LABEL ->
              targets.add(TargetProperty.UNREACH_CALL);
          case DATA_RACE -> targets.add(TargetProperty.DATA_RACE);
          default ->
              throw new InvalidConfigurationException(
                  "the ordering-consistency analysis does not support the property '"
                      + common
                      + "'; supported: unreach-call, no-data-race");
        }
      } else {
        throw new InvalidConfigurationException(
            "the ordering-consistency analysis does not support the property '" + property + "'");
      }
    }
    if (targets.size() > 1) {
      throw new InvalidConfigurationException(
          "the ordering-consistency analysis checks one property at a time, but got " + targets);
    }
    return targets.isEmpty() ? Optional.empty() : Optional.of(targets.iterator().next());
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
        // the inner algorithm returns at every target state; drain the waitlist completely, and
        // between passes seed any newly created thread as a separate root (see seedThreadRoots)
        do {
          status = status.update(innerAlgorithm.run(pReachedSet));
          seedThreadRoots(pReachedSet);
        } while (pReachedSet.hasWaitingState());
      } finally {
        statistics.explorationTimer.stop();
      }

      RoundResult result = solveRound(pReachedSet);
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

  private RoundResult solveRound(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    OcExplorationRegistry registry = ocCpa.getRegistry();
    statistics.eventCount = registry.getEvents().size();
    statistics.instanceCount = registry.getInstances().size();
    boolean truncated = registry.isTruncated();

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
    if (dumpEvents) {
      StringBuilder dump = new StringBuilder("Collected memory events:");
      for (MemoryEvent event : registry.getEvents()) {
        dump.append(
            String.format(
                "%n  #%d T%d %s loc=%s cssa=%s mutex=%s region=%s fill=%s edge=%s",
                event.id(),
                event.instanceId(),
                event.kind(),
                event.memoryLocation(),
                event.cssaName(),
                event.mutexId(),
                event.regionId(),
                event.fill(),
                event.edge() == null ? "-" : event.edge().getDescription().replace('\n', ' ')));
      }
      logger.log(Level.INFO, dump.toString());
    }

    // the property-specific violation formula, and whether any candidate exists at all: without one
    // and without a truncated path the round is trivially safe
    boolean hasCandidates =
        switch (targetProperty) {
          case UNREACH_CALL ->
              registry.getEvents().stream().anyMatch(e -> e.kind() == EventKind.ERROR);
          case DATA_RACE -> !encoder.getRacePairs().isEmpty();
        };
    if (!hasCandidates && !truncated) {
      return RoundResult.SAFE;
    }
    BooleanFormula violationConstraint =
        switch (targetProperty) {
          case UNREACH_CALL -> encoder.getErrorConstraint();
          case DATA_RACE -> encoder.getDataRaceConstraint();
        };

    statistics.solvingTimer.start();
    try {
      if (hasCandidates) {
        try (ProverEnvironment prover =
            solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
          for (BooleanFormula constraint : constraints) {
            prover.addConstraint(constraint);
          }
          for (BooleanFormula constraint : encoder.getJoinConstraints()) {
            prover.addConstraint(constraint);
          }
          prover.addConstraint(violationConstraint);
          boolean violation =
              switch (encoding) {
                case CLOCKS -> !prover.isUnsat();
                case REFINEMENT -> runRefinementLoop(prover, encoder, bfmgr);
              };
          if (violation) {
            if (dumpViolationModel) {
              dumpViolationModel(prover, encoder);
            }
            if (exportExecutionGraph && executionGraphFile != null) {
              exportExecutionGraph(prover, encoder);
            }
            if (exportSequentializedCounterexample && sequentializedCounterexampleFile != null) {
              exportSequentializedCounterexample(prover, encoder);
            }
            attachCounterexample(pReachedSet, prover, encoder);
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

  /**
   * Adds the roots of threads created during the last exploration pass to the reached set as
   * separate parentless roots. A spawned thread has no CFA edge from its creator, so it is not a
   * control-flow successor; making it a root keeps the reached set's ARG a clean forest (one tree
   * per thread instance) and schedules the root for exploration in the next pass of the drain loop.
   */
  private void seedThreadRoots(ReachedSet pReachedSet) throws InterruptedException {
    StateSpacePartition partition = StateSpacePartition.getDefaultPartition();
    OrderingConsistencyState root;
    while ((root = ocCpa.pollPendingThreadRoot()) != null) {
      CFANode location = root.getLocationNode();
      pReachedSet.add(new ARGState(root, null), topCpa.getInitialPrecision(location, partition));
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
                nullToEmpty(event.cssaName()),
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

  /**
   * Writes the found violating execution as a DOT/graphviz execution graph: one node per enabled
   * event (grouped into a per-thread cluster), program-order edges, create/join cross edges, and
   * the chosen read-from edges. The file goes to the regular output folder and is suppressed in
   * benchmark mode (the {@link FileOption} path is nulled by {@code output.disable}).
   */
  private void exportExecutionGraph(ProverEnvironment pProver, OcEncoder pEncoder)
      throws SolverException, InterruptedException {
    var imgr = ocCpa.getSolver().getFormulaManager().getIntegerFormulaManager();
    OcExplorationRegistry registry = ocCpa.getRegistry();
    Map<Integer, String> instanceNames = new LinkedHashMap<>();
    registry.getInstances().forEach(i -> instanceNames.put(i.getId(), i.getFunctionName()));

    StringBuilder dot = new StringBuilder();
    dot.append("digraph ExecutionGraph {\n");
    dot.append("  rankdir=TB;\n");
    dot.append("  node [shape=box, fontname=\"monospace\"];\n");

    try (Model model = pProver.getModel()) {
      Set<Integer> enabled = new LinkedHashSet<>();
      Map<Integer, MemoryEvent> byId = new LinkedHashMap<>();
      Map<Integer, List<MemoryEvent>> byInstance = new LinkedHashMap<>();
      for (MemoryEvent event : pEncoder.getEvents()) {
        if (Boolean.TRUE.equals(model.evaluate(pEncoder.getFullGuard(event)))) {
          enabled.add(event.id());
          byId.put(event.id(), event);
          byInstance.computeIfAbsent(event.instanceId(), k -> new ArrayList<>()).add(event);
        }
      }

      for (Map.Entry<Integer, List<MemoryEvent>> entry : byInstance.entrySet()) {
        int instanceId = entry.getKey();
        dot.append(String.format("  subgraph cluster_T%d {%n", instanceId));
        dot.append(
            String.format(
                "    label=\"T%d %s\"; color=gray;%n",
                instanceId, instanceNames.getOrDefault(instanceId, "")));
        for (MemoryEvent event : entry.getValue()) {
          dot.append(
              String.format(
                  "    e%d [label=\"%s\"%s];%n",
                  event.id(), nodeLabel(event, model, imgr), nodeStyle(event)));
        }
        dot.append("  }\n");
      }

      for (int[] edge : pEncoder.getProgramOrderDagEdges()) {
        if (enabled.contains(edge[0]) && enabled.contains(edge[1])) {
          dot.append(String.format("  e%d -> e%d [label=\"po\"];%n", edge[0], edge[1]));
        }
      }
      for (var cross : pEncoder.getCrossPoEdges()) {
        if (enabled.contains(cross.from()) && enabled.contains(cross.to())) {
          String kind = byId.get(cross.from()).kind() == EventKind.CREATE ? "create" : "join";
          dot.append(
              String.format(
                  "  e%d -> e%d [label=\"%s\", style=dashed, color=blue, constraint=false];%n",
                  cross.from(), cross.to(), kind));
        }
      }
      for (var rf : pEncoder.getRfPairs()) {
        if (Boolean.TRUE.equals(model.evaluate(rf.variable()))) {
          dot.append(
              String.format(
                  "  e%d -> e%d [label=\"rf\", color=red, constraint=false];%n",
                  rf.write().id(), rf.read().id()));
        }
      }
    }
    dot.append("}\n");

    try {
      IO.writeFile(executionGraphFile, Charset.defaultCharset(), dot.toString());
    } catch (IOException e) {
      logger.logUserException(
          Level.WARNING, e, "Could not write the execution graph to " + executionGraphFile);
    }
  }

  /** A DOT node label: event id, thread, kind, clock (CLOCKS mode) and value/mutex/target. */
  private String nodeLabel(MemoryEvent event, Model model, IntegerFormulaManagerView imgr) {
    StringBuilder label =
        new StringBuilder("e" + event.id() + " T" + event.instanceId() + "\\n" + event.kind());
    if (encoding == EncodingMode.CLOCKS) {
      label.append(" @").append(model.evaluate(imgr.makeVariable("__oc_clk_" + event.id())));
    }
    if (event.variable() != null) {
      String name =
          event.memoryLocation() != null
              ? event.memoryLocation().toString()
              : String.valueOf(event.cssaName());
      Object value = model.evaluate(event.variable());
      label.append("\\n").append(name);
      if (value != null) {
        // an unconstrained value (e.g. a fill-init of a never-read cell) has no model assignment
        label.append(" = ").append(value);
      }
    } else if (event.mutexId() != null) {
      label.append("\\n").append(event.mutexId());
    } else if (event.otherInstanceId() != MemoryEvent.NO_INSTANCE) {
      label.append("\\nT").append(event.otherInstanceId());
    }
    return label.toString();
  }

  /** Highlights the reached error event; other events use the default box style. */
  private static String nodeStyle(MemoryEvent event) {
    return event.kind() == EventKind.ERROR ? ", style=filled, fillcolor=orangered" : "";
  }

  /**
   * Writes the found violation as a sequentialized counterexample: the enabled events in a
   * consistent execution order, each with its thread, kind, value, and CFA edge. This is the
   * violating execution flattened to a single interleaving — the CPAchecker-native counterexample
   * for a model-based analysis that has no single ARG path. Suppressed in benchmark mode.
   */
  private void exportSequentializedCounterexample(ProverEnvironment pProver, OcEncoder pEncoder)
      throws SolverException, InterruptedException {
    StringBuilder trace = new StringBuilder();
    try (Model model = pProver.getModel()) {
      List<MemoryEvent> ordered = orderedViolationEvents(model, pEncoder);
      trace.append(
          String.format(
              "Sequentialized ordering-consistency counterexample (%d events, encoding=%s):%n%n",
              ordered.size(), encoding));
      int step = 0;
      for (MemoryEvent event : ordered) {
        CFAEdge edge = event.edge() == null ? null : displayEdge(event, model);
        String edgeText =
            edge == null
                ? ""
                : String.format(
                    "%s  %s",
                    edge.getFileLocation(), edge.getDescription().replace('\n', ' ').strip());
        trace.append(
            String.format(
                "  #%-3d T%-2d %-11s %-26s %s%n",
                step, event.instanceId(), event.kind(), eventDetail(event, model), edgeText));
        step++;
      }
    }
    try {
      IO.writeFile(sequentializedCounterexampleFile, Charset.defaultCharset(), trace.toString());
    } catch (IOException e) {
      logger.logUserException(
          Level.WARNING,
          e,
          "Could not write the sequentialized counterexample to "
              + sequentializedCounterexampleFile);
    }
  }

  /**
   * Replicates the standard CPAchecker counterexample mechanism so the violation flows through the
   * existing infrastructure (counterexample export, report, ...). A regular analysis leaves a
   * target state whose parent chain to the root is the error path; here the "path" is one
   * interleaving of the whole execution, which crosses thread trees and so has no counterpart in
   * the forest-shaped reached set. We therefore build a synthetic {@link ARGPath} — a fresh chain
   * of location-wrapping ARG states linked by the ordered events' CFA edges — and attach it as a
   * {@link CounterexampleInfo} to the reached target state, exactly as {@code getOnePathTo} would
   * for a normal analysis. The explicit (non-null) edge list is the path's full path, which is what
   * the export prints.
   */
  private void attachCounterexample(
      ReachedSet pReachedSet, ProverEnvironment pProver, OcEncoder pEncoder)
      throws SolverException, InterruptedException {
    try (Model model = pProver.getModel()) {
      List<CFAEdge> edges = new ArrayList<>();
      // the event that ends the counterexample path (the reached error for unreach-call, the later
      // of the two racing accesses for no-data-race); its reached state becomes the CEX target
      MemoryEvent terminalEvent = terminalViolationEvent(model, pEncoder);
      if (terminalEvent == null) {
        return;
      }
      CFAEdge previousEdge = null;
      for (MemoryEvent event : orderedViolationEvents(model, pEncoder)) {
        if (event.edge() != null) {
          CFAEdge edge = displayEdge(event, model);
          // collapse consecutive events that map to the same edge: one statement can produce
          // several
          // events (e.g. a[k]=5 is a read of k and a write of a[k]) and a shared branch-condition
          // read
          // can be tagged on several reads of the same assume edge
          if (!edge.equals(previousEdge)) {
            edges.add(edge);
            previousEdge = edge;
          }
        }
        if (event.id() == terminalEvent.id()) {
          break; // the execution stops at the violating event
        }
      }
      if (targetProperty == TargetProperty.DATA_RACE) {
        // the exploration marks every syntactically reachable reach_error state as a target; those
        // are irrelevant to the data-race property and would otherwise leak into the verdict. Drop
        // them here, then mark the actual racing state below as the sole target.
        TargetLocationCandidateInvariant.INSTANCE.assumeTruth(pReachedSet);
      }
      // the reached state that carries the terminal event; it must be the last state of the path so
      // CounterexampleInfo accepts it (getTargetState().isTarget()). The terminal event need not
      // itself be a state's lastEventIds: several accesses chained on one edge (e.g. both operands
      // of `a != b`) share one successor state, and only the last of the chain is recorded there
      // (see OcExplorationRegistry#chainTerminalEventId).
      int terminalChainId = ocCpa.getRegistry().chainTerminalEventId(terminalEvent.id());
      ARGState target = null;
      for (AbstractState reached : pReachedSet) {
        OrderingConsistencyState ocState =
            AbstractStates.extractStateByType(reached, OrderingConsistencyState.class);
        if (reached instanceof ARGState argState
            && ocState != null
            && ocState.getLastEventIds().contains(terminalChainId)) {
          if (targetProperty == TargetProperty.DATA_RACE) {
            // no exploration-time target exists for a race; mark this reached state so the verdict
            // is FALSE (the reached set now contains a genuine target with a proper location)
            ocState.markRaceTarget();
          }
          target = argState;
          break;
        }
      }
      if (target == null || edges.isEmpty()) {
        return;
      }
      // a fresh linear chain of location states ending at the real target. Each state carries the
      // successor node of its incoming edge, so the full-path iterator (which advances a state
      // whenever the current edge's successor is that state's location) stays in lockstep with the
      // edge list — even across thread-jumps, where the edges are not CFA-connected. The real
      // target
      // already sits at the terminal edge's successor, matching the last edge. It is only appended
      // to
      // the state list, never parent-linked, so the reached-set ARG is left untouched.
      List<ARGState> states = new ArrayList<>();
      ARGState previous =
          new ARGState(ocCpa.locationStateFor(edges.getFirst().getPredecessor()), null);
      states.add(previous);
      for (int i = 1; i < edges.size(); i++) {
        ARGState next =
            new ARGState(ocCpa.locationStateFor(edges.get(i - 1).getSuccessor()), previous);
        states.add(next);
        previous = next;
      }
      states.add(target);
      target.addCounterexampleInformation(
          CounterexampleInfo.feasibleImprecise(new ARGPath(states, ImmutableList.copyOf(edges))));
    }
  }

  /**
   * The event that ends the counterexample path: the reached error event for unreach-call, or the
   * later (higher-clock) of the two racing accesses for no-data-race. Returns null if none is found
   * (the counterexample is then skipped, but the verdict already stands).
   */
  private MemoryEvent terminalViolationEvent(Model pModel, OcEncoder pEncoder) {
    return switch (targetProperty) {
      case UNREACH_CALL -> {
        MemoryEvent errorEvent = null;
        for (MemoryEvent event : pEncoder.getEvents()) {
          if (event.kind() == EventKind.ERROR
              && Boolean.TRUE.equals(pModel.evaluate(pEncoder.getFullGuard(event)))) {
            errorEvent = event;
          }
        }
        yield errorEvent;
      }
      case DATA_RACE -> {
        OcEncoder.RacePair race = findRace(pModel, pEncoder);
        if (race == null) {
          yield null;
        }
        var imgr = ocCpa.getSolver().getFormulaManager().getIntegerFormulaManager();
        BigInteger clock1 = pModel.evaluate(imgr.makeVariable("__oc_clk_" + race.access1().id()));
        BigInteger clock2 = pModel.evaluate(imgr.makeVariable("__oc_clk_" + race.access2().id()));
        yield clock1 != null && clock2 != null && clock1.compareTo(clock2) >= 0
            ? race.access1()
            : race.access2();
      }
    };
  }

  /**
   * A conflicting cross-instance access pair that is enabled and at adjacent clock values in the
   * model — i.e. the concrete data race the solver found — or null if none matches.
   */
  private OcEncoder.RacePair findRace(Model pModel, OcEncoder pEncoder) {
    var imgr = ocCpa.getSolver().getFormulaManager().getIntegerFormulaManager();
    for (OcEncoder.RacePair pair : pEncoder.getRacePairs()) {
      if (!Boolean.TRUE.equals(pModel.evaluate(pEncoder.getFullGuard(pair.access1())))
          || !Boolean.TRUE.equals(pModel.evaluate(pEncoder.getFullGuard(pair.access2())))) {
        continue;
      }
      if (pair.access1().isRegionAccess()
          && !Boolean.TRUE.equals(
              pModel.evaluate(pEncoder.sameAddress(pair.access1(), pair.access2())))) {
        continue;
      }
      BigInteger clock1 = pModel.evaluate(imgr.makeVariable("__oc_clk_" + pair.access1().id()));
      BigInteger clock2 = pModel.evaluate(imgr.makeVariable("__oc_clk_" + pair.access2().id()));
      if (clock1 != null
          && clock2 != null
          && clock1.subtract(clock2).abs().equals(BigInteger.ONE)) {
        return pair;
      }
    }
    return null;
  }

  /**
   * The enabled events of the violation model in a consistent execution order. In CLOCKS mode this
   * is the exact sequentially-consistent order given by the integer clocks; in REFINEMENT mode it
   * is a topological order of the happens-before relation (program order, thread create/join, and
   * the chosen read-from edges), which is acyclic for a consistent violation.
   */
  private List<MemoryEvent> orderedViolationEvents(Model pModel, OcEncoder pEncoder) {
    List<MemoryEvent> enabled = new ArrayList<>();
    for (MemoryEvent event : pEncoder.getEvents()) {
      if (Boolean.TRUE.equals(pModel.evaluate(pEncoder.getFullGuard(event)))) {
        enabled.add(event);
      }
    }
    if (encoding == EncodingMode.CLOCKS) {
      var imgr = ocCpa.getSolver().getFormulaManager().getIntegerFormulaManager();
      Map<Integer, BigInteger> clock = new HashMap<>();
      for (MemoryEvent event : enabled) {
        clock.put(event.id(), pModel.evaluate(imgr.makeVariable("__oc_clk_" + event.id())));
      }
      enabled.sort(
          Comparator.comparing(
                  (MemoryEvent e) -> clock.get(e.id()),
                  Comparator.nullsFirst(Comparator.naturalOrder()))
              .thenComparingInt(MemoryEvent::id));
      return enabled;
    }
    return topologicalOrder(enabled, pEncoder, pModel);
  }

  /**
   * A topological order of the happens-before relation over the enabled events (REFINEMENT mode).
   */
  private static List<MemoryEvent> topologicalOrder(
      List<MemoryEvent> pEnabled, OcEncoder pEncoder, Model pModel) {
    Map<Integer, MemoryEvent> byId = new LinkedHashMap<>();
    for (MemoryEvent event : pEnabled) {
      byId.put(event.id(), event);
    }
    Map<Integer, Set<Integer>> successors = new LinkedHashMap<>();
    Map<Integer, Integer> indegree = new HashMap<>();
    for (Integer id : byId.keySet()) {
      successors.put(id, new LinkedHashSet<>());
      indegree.put(id, 0);
    }
    List<int[]> hbEdges = new ArrayList<>(pEncoder.getProgramOrderDagEdges());
    for (var cross : pEncoder.getCrossPoEdges()) {
      hbEdges.add(new int[] {cross.from(), cross.to()});
    }
    for (var rf : pEncoder.getRfPairs()) {
      if (Boolean.TRUE.equals(pModel.evaluate(rf.variable()))) {
        hbEdges.add(new int[] {rf.write().id(), rf.read().id()});
      }
    }
    // coherence (write-serialization) and from-read edges make the linearization respect the order
    // of same-cell writes, so a read never appears before a co-later write of the value it did not
    // read. Restricted to scalar writes, where a write-serialization pair is unambiguously
    // same-cell
    // (region accesses would need the model's address equality, so they keep the
    // happens-before-only
    // order).
    for (var ws : pEncoder.getWsPairs()) {
      if (ws.write1().isRegionAccess()
          || !byId.containsKey(ws.write1().id())
          || !byId.containsKey(ws.write2().id())) {
        continue;
      }
      MemoryEvent from = null;
      MemoryEvent to = null;
      if (Boolean.TRUE.equals(pModel.evaluate(ws.var12()))) {
        from = ws.write1();
        to = ws.write2();
      } else if (Boolean.TRUE.equals(pModel.evaluate(ws.var21()))) {
        from = ws.write2();
        to = ws.write1();
      }
      if (from != null) {
        hbEdges.add(new int[] {from.id(), to.id()});
        for (var rf : pEncoder.getRfPairs()) {
          if (rf.write().id() == from.id()
              && byId.containsKey(rf.read().id())
              && Boolean.TRUE.equals(pModel.evaluate(rf.variable()))) {
            hbEdges.add(new int[] {rf.read().id(), to.id()});
          }
        }
      }
    }
    for (int[] edge : hbEdges) {
      if (byId.containsKey(edge[0])
          && byId.containsKey(edge[1])
          && successors.get(edge[0]).add(edge[1])) {
        indegree.merge(edge[1], 1, Integer::sum);
      }
    }
    // Kahn's algorithm; ties broken by event id so the trace is deterministic
    PriorityQueue<Integer> ready = new PriorityQueue<>();
    indegree.forEach(
        (id, degree) -> {
          if (degree == 0) {
            ready.add(id);
          }
        });
    List<MemoryEvent> ordered = new ArrayList<>();
    while (!ready.isEmpty()) {
      int id = ready.poll();
      ordered.add(byId.get(id));
      for (int next : successors.get(id)) {
        if (indegree.merge(next, -1, Integer::sum) == 0) {
          ready.add(next);
        }
      }
    }
    // a consistent violation is acyclic; if some events remain (unexpected cycle), append them by
    // id
    if (ordered.size() < pEnabled.size()) {
      Set<Integer> placed = new HashSet<>();
      for (MemoryEvent event : ordered) {
        placed.add(event.id());
      }
      byId.values().stream()
          .filter(event -> !placed.contains(event.id()))
          .sorted(Comparator.comparingInt(MemoryEvent::id))
          .forEach(ordered::add);
    }
    return ordered;
  }

  /**
   * The CFA edge to show for an event. For a branch-condition read the stored edge is the
   * (arbitrary) assume edge the shared read was attached to; the actual direction taken is
   * recovered by evaluating the branch condition in the model, so the counterexample shows the real
   * branch.
   */
  private CFAEdge displayEdge(MemoryEvent event, Model model) {
    OcExplorationRegistry.AssumeBranch branch = ocCpa.getRegistry().getAssumeBranch(event.id());
    if (branch != null) {
      return Boolean.TRUE.equals(model.evaluate(branch.condition()))
          ? branch.firstEdge()
          : branch.secondEdge();
    }
    return event.edge();
  }

  /**
   * One-line detail for a counterexample event: value for accesses, target for create/join, etc.
   */
  private String eventDetail(MemoryEvent event, Model model) {
    if (event.variable() != null) {
      String name =
          event.memoryLocation() != null
              ? event.memoryLocation().toString()
              : String.valueOf(event.cssaName());
      Object value = model.evaluate(event.variable());
      return value == null ? name : name + " = " + value;
    }
    if (event.mutexId() != null) {
      return event.mutexId();
    }
    if (event.otherInstanceId() != MemoryEvent.NO_INSTANCE) {
      return "-> T" + event.otherInstanceId();
    }
    return "";
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
