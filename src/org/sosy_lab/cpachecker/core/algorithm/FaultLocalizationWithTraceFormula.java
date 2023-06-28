// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.base.VerifyException;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.PrintStream;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Pattern;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.Optionals;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.FaultLocalizationInfoWithTraceFormula;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.FaultLocalizerWithTraceFormula;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.error_invariants.ErrorInvariantsAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.error_invariants.IntervalReportWriter;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.rankings.CallHierarchyScoring;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.rankings.EdgeTypeScoring;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.FormulaContext;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.InvalidCounterexampleException;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.TraceFormula;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.TraceFormula.TraceFormulaOptions;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.postcondition.FinalAssumeClusterPostConditionComposer;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.postcondition.FinalAssumeEdgePostConditionComposer;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.postcondition.FinalAssumeEdgesOnSameLinePostConditionComposer;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.postcondition.PostConditionComposer;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.precondition.PreConditionComposer;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.precondition.TruePreConditionComposer;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.precondition.VariableAssignmentPreConditionComposer;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.trace.Trace.TraceAtom;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.unsat.ModifiedMaxSatAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.unsat.OriginalMaxSatAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.unsat.SingleUnsatCoreAlgorithm;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultExplanation;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultLocalizationInfo;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultRankingUtils;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultScoring;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultInfo.InfoType;
import org.sosy_lab.cpachecker.util.faultlocalization.explanation.InformationProvider;
import org.sosy_lab.cpachecker.util.faultlocalization.explanation.NoContextExplanation;
import org.sosy_lab.cpachecker.util.faultlocalization.explanation.SuspiciousCalculationExplanation;
import org.sosy_lab.cpachecker.util.faultlocalization.ranking.MinimalLineDistanceScoring;
import org.sosy_lab.cpachecker.util.faultlocalization.ranking.SetSizeScoring;
import org.sosy_lab.cpachecker.util.faultlocalization.ranking.VariableCountScoring;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;
import org.sosy_lab.java_smt.api.SolverException;

@Options(prefix = "faultLocalization.by_traceformula")
public class FaultLocalizationWithTraceFormula
    implements Algorithm, StatisticsProvider, Statistics {

  public enum AlgorithmType {
    UNSAT,
    MAXSAT,
    MAXORG,
    ERRINV
  }

  enum PostConditionType {
    LAST_ASSUME_EDGE,
    LAST_ASSUME_EDGES_ON_SAME_LINE,
    LAST_ASSUME_EDGE_CLUSTER
  }

  enum PreConditionType {
    /**
     * Do not use any edge of the counterexample for creating the pre-condition. Instead, find
     * values that satisfy the counterexample for all nondeterministic variables. In case no
     * nondeterministic variables exist, return true.
     */
    NONDETERMINISTIC_VARIABLES_ONLY,
    /**
     * Combine {@link PreConditionType#NONDETERMINISTIC_VARIABLES_ONLY} with initial assignments.
     * Initial assignments are statement or declaration edges that assign a value to a variable for
     * the first time.
     */
    INITIAL_ASSIGNMENT,
    /** Precondition that represent {@code true}. */
    ALWAYS_TRUE
  }

  private final Algorithm algorithm;
  private final LogManager logger;
  private final FormulaContext context;
  private final TraceFormulaOptions options;

  private final FaultLocalizerWithTraceFormula faultAlgorithm;
  private final StatTimer totalTime = new StatTimer("Total time for fault localization");

  @Option(secure = true, name = "type", description = "which algorithm to use")
  private AlgorithmType algorithmType = AlgorithmType.UNSAT;

  @Option(
      secure = true,
      name = "maxsat.ban",
      description =
          "Do not show faults that contain a certain variable. Use, e.g., 'main::x' to ban variable"
              + " 'x' in the main function. Use, e.g., '::x' to ban all variables named 'x'. This"
              + " is especially useful to filter specific faults if the first run results in many"
              + " candidates. Provide a comma separated string to add variables, e.g.,"
              + " main::x,doStuff::y,::z")
  private List<String> ban = ImmutableList.of();

  @Option(
      description =
          "By default, the precondition only contains the failing variable assignment of all nondet"
              + " variables. Choose INITIAL_ASSIGNMENT to add assignments like '<datatype>"
              + " <variable-name> = <value>' to the precondition.")
  private PreConditionType preconditionType = PreConditionType.NONDETERMINISTIC_VARIABLES_ONLY;

  @Option(description = "which post-condition type to use")
  private PostConditionType postConditionType = PostConditionType.LAST_ASSUME_EDGES_ON_SAME_LINE;

  @Option(
      description = "whether to include variables beginning with __FAULT_LOCALIZATION_precondition")
  private boolean includeDeclared = true;

  @Option(description = "Whether the found counterexample needs to be precise")
  private boolean requirePreciseCounterexample = true;

  @Option(description = "Whether to stop searching for further faults if first fault was found.")
  private boolean stopAfterFirstFault = false;

  public FaultLocalizationWithTraceFormula(
      final Algorithm pStoreAlgorithm,
      final Configuration pConfig,
      final LogManager pLogger,
      final CFA pCfa,
      final ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {

    if (!(pStoreAlgorithm instanceof CounterexampleStoreAlgorithm)) {
      throw new InvalidConfigurationException("option CounterexampleStoreAlgorithm required");
    }

    logger = pLogger;

    // Options
    pConfig.inject(this);
    options = new TraceFormulaOptions(pConfig);
    checkOptions();

    // Parent algorithm
    algorithm = pStoreAlgorithm;

    // Create formula context
    Solver solver = Solver.create(pConfig, pLogger, pShutdownNotifier);
    PathFormulaManagerImpl manager =
        new PathFormulaManagerImpl(
            solver.getFormulaManager(),
            pConfig,
            pLogger,
            pShutdownNotifier,
            pCfa,
            AnalysisDirection.FORWARD);
    context = new FormulaContext(solver, manager, pCfa, logger, pConfig, pShutdownNotifier);

    faultAlgorithm =
        switch (algorithmType) {
          case MAXORG -> new OriginalMaxSatAlgorithm(stopAfterFirstFault);
          case MAXSAT -> new ModifiedMaxSatAlgorithm(stopAfterFirstFault);
          case ERRINV -> new ErrorInvariantsAlgorithm(pShutdownNotifier, pConfig, logger);
          case UNSAT -> new SingleUnsatCoreAlgorithm();
        };
  }

  public void checkOptions() throws InvalidConfigurationException {
    if (stopAfterFirstFault
        && (algorithmType == AlgorithmType.ERRINV || algorithmType == AlgorithmType.UNSAT)) {
      throw new InvalidConfigurationException(
          "The option 'stopAfterFirstFault' requires MAXORG or MAXSAT as algorithmType");
    }
    if (!algorithmType.equals(AlgorithmType.ERRINV) && options.makeFlowSensitive()) {
      throw new InvalidConfigurationException(
          "The option 'makeFlowSensitive' (flow-sensitive trace formula) requires the error"
              + " invariants algorithm");
    }
    if (algorithmType.equals(AlgorithmType.ERRINV) && !ban.isEmpty()) {
      throw new InvalidConfigurationException(
          "The option 'ban' cannot be used together with the error invariants algorithm. Use"
              + " MAX-SAT instead.");
    }
    if (!algorithmType.equals(AlgorithmType.MAXSAT) && options.isReduceSelectors()) {
      throw new InvalidConfigurationException(
          "The option 'reduceselectors' requires the MAX-SAT algorithm");
    }
    if (!options.getDisable().isEmpty() && algorithmType.equals(AlgorithmType.ERRINV)) {
      throw new InvalidConfigurationException(
          "The option 'ban' is not applicable for the error invariants algorithm");
    }
    if (!options.getDisable().isEmpty()) {
      if (options.getDisable().stream()
          .anyMatch(variable -> !Pattern.matches(".+::.+", variable))) {
        throw new InvalidConfigurationException(
            "The option 'traceformula.disable' needs scoped variables. Make sure to input the"
                + " variables with their scope as prefix, e.g. main::x i.e., function::variable.");
      }
    }
    if (!options.getExcludeFromPrecondition().isEmpty()) {
      if (options.getExcludeFromPrecondition().stream()
          .anyMatch(variable -> !Pattern.matches(".+::.+", variable))) {
        throw new InvalidConfigurationException(
            "The option 'traceformula.ignore' needs scoped variables. Make sure to input the"
                + " variables with their scope as prefix, e.g. main::x, i.e., function::variable.");
      }
    }
    if (!ban.isEmpty()) {
      if (ban.stream().anyMatch(variable -> !Pattern.matches(".+::.+", variable))) {
        throw new InvalidConfigurationException(
            "The option 'faultlocalization.by_traceformula.ban' needs scoped variables. Make sure"
                + " to input the variables with their scope as prefix, e.g. main::x, i.e.,"
                + " function::variable.");
      }
    }
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {

    AlgorithmStatus status = algorithm.run(reachedSet);
    totalTime.start();
    try {
      FluentIterable<CounterexampleInfo> counterExamples =
          Optionals.presentInstances(
              from(reachedSet)
                  .filter(AbstractStates::isTargetState)
                  .filter(ARGState.class)
                  .transform(ARGState::getCounterexampleInformation));

      // run algorithm for every error
      logger.log(Level.INFO, "Starting fault localization...");
      for (CounterexampleInfo info : counterExamples) {
        logger.log(Level.INFO, "Find explanations for fault #" + info.getUniqueId());
        if (info.isSpurious()) {
          logger.logf(
              Level.INFO,
              "Algorithm found a spurious counterexample. Cannot continue fault"
                  + " localization on counterexample %s...",
              info.getUniqueId());
          continue;
        }
        if (!requirePreciseCounterexample && !info.isPreciseCounterExample()) {
          logger.logf(
              Level.INFO,
              "Algorithm found an imprecise counterexample. Cannot continue fault localization on"
                  + " counterexample %s. Set"
                  + " faultLocalization.by_traceformula.requirePreciseCounterexample=false to run"
                  + " fault localization anyways.",
              info.getUniqueId());
          continue;
        }
        runAlgorithm(info, faultAlgorithm);
      }
      logger.log(Level.INFO, "Stopping fault localization...");
    } finally {
      totalTime.stop();
    }
    return status;
  }

  private List<CFAEdge> fromImpreciseCounterexample(CounterexampleInfo pInfo) {
    return pInfo.getTargetPath().getFullPath();
  }

  private List<CFAEdge> fromPreciseCounterexample(CounterexampleInfo pInfo) {
    // Run the algorithm and create a CFAPathWithAssumptions to the last reached state.
    CFAPathWithAssumptions assumptions = pInfo.getCFAPathWithAssignments();
    if (assumptions.isEmpty()) {
      logger.log(Level.INFO, "The analysis returned no assumptions for a precise counterexample.");
      return ImmutableList.of();
    }

    // Collect all edges that do not evaluate to true
    return transformedImmutableListCopy(assumptions, CFAEdgeWithAssumptions::getCFAEdge);
  }

  private FaultScoring getScoring(TraceFormula pTraceFormula) {
    return switch (algorithmType) {
        // fall-through
      case MAXORG, MAXSAT -> FaultRankingUtils.concatHeuristics(
          new VariableCountScoring(),
          new SetSizeScoring(),
          new MinimalLineDistanceScoring(
              pTraceFormula.getPostCondition().getEdgesForPostCondition().get(0)));
        // fall-through
      case ERRINV, UNSAT -> FaultRankingUtils.concatHeuristics(
          new EdgeTypeScoring(), new CallHierarchyScoring(pTraceFormula.getTrace().toEdgeList()));
      default -> throw new AssertionError("The specified algorithm type does not exist");
    };
  }

  private void runAlgorithm(CounterexampleInfo pInfo, FaultLocalizerWithTraceFormula pAlgorithm)
      throws CPAException, InterruptedException {
    try {
      List<CFAEdge> edgeList =
          pInfo.isPreciseCounterExample()
              ? fromPreciseCounterexample(pInfo)
              : fromImpreciseCounterexample(pInfo);
      if (edgeList.isEmpty()) {
        logger.log(
            Level.INFO,
            "Can't find relevant edges in the error trace. Fault Localization not possible.");
        return;
      }

      // Find correct scoring and correct trace formula for the specified algorithm
      final TraceFormula tf =
          TraceFormula.fromCounterexample(
              getPreConditionExtractor(), getPostConditionExtractor(), edgeList, context, options);

      if (!tf.isCalculationPossible()) {
        logger.log(
            Level.INFO,
            "Pre- and post-condition are unsatisfiable. No further analysis required. Most likely"
                + " the variables in your post-condition never change their value.");
        return;
      }

      Set<Fault> faults = pAlgorithm.run(context, tf);

      // ban specified variables from result set
      ImmutableSet.Builder<Fault> remainingFaults = ImmutableSet.builder();
      if (!algorithmType.equals(AlgorithmType.ERRINV)) {
        for (Fault fault : faults) {
          if (fault.stream()
              .noneMatch(
                  fc ->
                      ban.stream()
                          .anyMatch(b -> ((TraceAtom) fc).getFormula().toString().contains(b)))) {
            remainingFaults.add(fault);
          }
        }
        faults = remainingFaults.build();
      }

      for (Fault fault : faults) {
        FaultExplanation.explain(
            fault,
            NoContextExplanation.getInstance(),
            new InformationProvider(edgeList),
            new SuspiciousCalculationExplanation());
      }

      FaultLocalizationInfo info =
          new FaultLocalizationInfoWithTraceFormula(
              faults, getScoring(tf), tf, pInfo, algorithmType == AlgorithmType.ERRINV);

      if (algorithmType == AlgorithmType.ERRINV) {
        info.replaceHtmlWriter(new IntervalReportWriter(context.getSolver().getFormulaManager()));
      }

      info.getHtmlWriter().hideTypes(InfoType.RANK_INFO);
      info.apply();
      logger.log(Level.INFO, "Running " + pAlgorithm.getClass().getSimpleName() + ":\n" + info);

    } catch (SolverException e) {
      throw new CPAException(
          "The solver was not able to find the UNSAT-core of the path formula.", e);
    } catch (VerifyException e) {
      throw new CPAException(
          "No bugs found because the trace formula is satisfiable or the counterexample is"
              + " spurious.",
          e);
    } catch (InvalidConfigurationException e) {
      throw new CPAException("Incomplete analysis because of invalid configuration.", e);
    } catch (IllegalStateException e) {
      throw new CPAException(
          "The counterexample is spurious. Calculating interpolants is not possible.", e);
    } catch (InvalidCounterexampleException e) {
      throw new CPAException(
          "The counterexample is invalid and cannot be transformed to a proper unsatisfiable"
              + " TraceFormula.",
          e);
    }
  }

  private PostConditionComposer getPostConditionExtractor() {
    switch (postConditionType) {
      case LAST_ASSUME_EDGE:
        return new FinalAssumeEdgePostConditionComposer(context);
      case LAST_ASSUME_EDGES_ON_SAME_LINE:
        return new FinalAssumeEdgesOnSameLinePostConditionComposer(context);
      case LAST_ASSUME_EDGE_CLUSTER:
        return new FinalAssumeClusterPostConditionComposer(context);
      default:
        throw new AssertionError("Unknown post-condition type");
    }
  }

  private PreConditionComposer getPreConditionExtractor() {
    switch (preconditionType) {
      case NONDETERMINISTIC_VARIABLES_ONLY:
        return new VariableAssignmentPreConditionComposer(context, options, false, includeDeclared);
      case INITIAL_ASSIGNMENT:
        return new VariableAssignmentPreConditionComposer(context, options, true, includeDeclared);
      case ALWAYS_TRUE:
        return new TruePreConditionComposer(context);
      default:
        throw new AssertionError("Unknown precondition type: " + preconditionType);
    }
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(this);
    if (algorithm instanceof Statistics) {
      statsCollection.add((Statistics) algorithm);
    }
    if (algorithm instanceof StatisticsProvider) {
      ((StatisticsProvider) algorithm).collectStatistics(statsCollection);
    }
    if (faultAlgorithm instanceof Statistics) {
      statsCollection.add((Statistics) faultAlgorithm);
    }
    if (faultAlgorithm instanceof StatisticsProvider) {
      ((StatisticsProvider) faultAlgorithm).collectStatistics(statsCollection);
    }
  }

  @Override
  public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {
    StatisticsWriter.writingStatisticsTo(out).put(totalTime);
  }

  @Override
  public @Nullable String getName() {
    return getClass().getSimpleName();
  }
}
