// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.execution;

import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState;
import org.sosy_lab.cpachecker.cpa.value.ExpressionValueVisitor;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.ValueAndType;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.symbolic.util.SymbolicIdentifierLocator;
import org.sosy_lab.cpachecker.cpa.value.symbolic.util.SymbolicValues;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;

/**
 * Picks one assignment for the nondeterministic values of a program, so that {@link ExecutionCPA}
 * can execute a program that has inputs.
 *
 * <p>The values of the inputs are symbolic (cf. {@code cpa.value.unknownValueHandling}) and the
 * {@link org.sosy_lab.cpachecker.cpa.constraints.ConstraintsCPA} collects the constraints that the
 * execution imposes on them. As soon as the execution depends on an input, i.e. as soon as a state
 * has more than one successor, we ask the SMT solver for one assignment of the inputs that reaches
 * the successor which is closest to a loop head or a function call, and continue the execution with
 * these concrete values. From then on the execution is fully determined again.
 *
 * <p>This explores one of the possible executions of the program, so a violation that is found is a
 * real one, but the absence of a violation does not prove anything: an analysis that samples must
 * never report TRUE.
 */
@Options(prefix = "cpa.execution")
class ExecutionSampler {

  @Option(
      secure = true,
      name = "sampleNondeterministicValues",
      description =
          "Execute programs with inputs by asking the SMT solver for one assignment of the inputs"
              + " instead of aborting on the first nondeterministic choice. This needs symbolic"
              + " values (cpa.value.unknownValueHandling=INTRODUCE_SYMBOLIC) and the ConstraintsCPA"
              + " in the CPA configuration. Only one of the possible executions is explored, so"
              + " the analysis can then only report FALSE, never TRUE.")
  private boolean sampleNondeterministicValues = false;

  @Option(
      secure = true,
      description =
          "Maximum number of assignments that are sampled during one execution. Sampling is"
              + " intended for programs whose inputs are read outside of any loop, where the"
              + " number of inputs is bounded by the program. This limit protects against the"
              + " unbounded number of solver calls that a program with inputs in a loop would"
              + " need. Use -1 for no limit.")
  private int maxSamples = 1000;

  private final LogManagerWithoutDuplicates logger;
  private final MachineModel machineModel;
  private final ExecutionStatistics stats;

  /**
   * Number of edges from a node to the nearest loop head or function call. Nodes from which neither
   * can be reached are not contained.
   */
  private final ImmutableMap<CFANode, Integer> distanceToInterestingLocation;

  /** Whether the program reads its inputs outside of all loops. */
  private final boolean inputsOutsideLoops;

  private int samples = 0;

  ExecutionSampler(Configuration pConfig, CFA pCfa, LogManager pLogger, ExecutionStatistics pStats)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    logger = new LogManagerWithoutDuplicates(pLogger);
    machineModel = pCfa.getMachineModel();
    stats = pStats;
    if (sampleNondeterministicValues) {
      distanceToInterestingLocation = computeDistances(pCfa);
      inputsOutsideLoops = checkInputsOutsideLoops(pCfa);
      if (!inputsOutsideLoops) {
        logger.log(
            Level.WARNING,
            "The program reads an input inside a loop, so the number of inputs is not bounded by"
                + " the program. ExecutionCPA samples the inputs anyway, but at most",
            maxSamples,
            "times.");
      }
    } else {
      distanceToInterestingLocation = ImmutableMap.of();
      inputsOutsideLoops = false;
    }
  }

  boolean isEnabled() {
    return sampleNondeterministicValues;
  }

  /** Whether an assignment was sampled, i.e. whether the analysis must not report TRUE. */
  boolean hasSampled() {
    return samples > 0;
  }

  int getSampleCount() {
    return samples;
  }

  /**
   * Fix the nondeterministic values of the given state such that its successor is determined.
   *
   * @param pState the state that has more than one successor
   * @param pSuccessors the successors of the state together with the edge that leads to them
   * @return a state that behaves like the given one but has concrete values for all inputs, or
   *     {@code null} if no assignment could be computed
   */
  @Nullable AbstractState sample(AbstractState pState, List<ExecutionStep> pSuccessors) {
    if (maxSamples >= 0 && samples >= maxSamples) {
      logger.logOnce(
          Level.WARNING,
          "Reached the limit of",
          maxSamples,
          "sampled assignments, aborting the execution.");
      return null;
    }
    ValueAnalysisState valueState =
        AbstractStates.extractStateByType(pState, ValueAnalysisState.class);
    if (valueState == null) {
      logger.logOnce(
          Level.WARNING,
          "Sampling nondeterministic values needs a value analysis in the CPA configuration.");
      return null;
    }

    ExecutionStep chosen = chooseSuccessor(pSuccessors);
    ConstraintsState constraints =
        AbstractStates.extractStateByType(chosen.successor(), ConstraintsState.class);
    if (constraints == null) {
      logger.logOnce(
          Level.WARNING,
          "Sampling nondeterministic values needs the ConstraintsCPA in the CPA configuration.");
      return null;
    }

    ValueAnalysisState concreteState =
        concretize(valueState, constraints.getModel(), chosen.edge().getPredecessor());
    AbstractState result = ExecutionStates.withValueState(pState, concreteState);
    if (result == null) {
      logger.logOnce(
          Level.WARNING,
          "Could not replace the values of the analysis, so the nondeterministic values of the"
              + " program cannot be sampled.");
      return null;
    }
    samples++;
    stats.sampledAssignments.inc();
    logger.log(
        Level.FINE,
        "Sampled an assignment of the inputs to follow the edge",
        chosen.edge(),
        "of",
        pSuccessors.size(),
        "possible successors.");
    return result;
  }

  /** Choose the successor that is closest to a loop head or a function call. */
  private ExecutionStep chooseSuccessor(List<ExecutionStep> pSuccessors) {
    ExecutionStep best = pSuccessors.getFirst();
    int bestDistance = distance(best.edge().getSuccessor());
    for (ExecutionStep step : pSuccessors.subList(1, pSuccessors.size())) {
      int stepDistance = distance(step.edge().getSuccessor());
      if (stepDistance < bestDistance) {
        best = step;
        bestDistance = stepDistance;
      }
    }
    return best;
  }

  private int distance(CFANode pNode) {
    return distanceToInterestingLocation.getOrDefault(pNode, Integer.MAX_VALUE);
  }

  /**
   * Replace every symbolic value of the state by the concrete value that the given model assigns to
   * it. Values that the model does not constrain get an arbitrary value, so that the resulting
   * state has no symbolic value left and the execution is deterministic from here on.
   */
  private ValueAnalysisState concretize(
      ValueAnalysisState pState, List<ValueAssignment> pModel, CFANode pLocation) {
    Map<SymbolicIdentifier, Value> assignment = new HashMap<>();
    for (ValueAssignment valueAssignment : pModel) {
      if (SymbolicValues.isSymbolicTerm(valueAssignment.getName())) {
        assignment.put(
            SymbolicValues.convertTermToSymbolicIdentifier(valueAssignment.getName()),
            SymbolicValues.convertToValue(valueAssignment));
      }
    }

    ValueAnalysisState result = ValueAnalysisState.copyOf(pState);
    Set<SymbolicIdentifier> identifiers = new HashSet<>();
    for (Entry<MemoryLocation, ValueAndType> entry : result.getConstants()) {
      if (entry.getValue().getValue() instanceof SymbolicValue symbolicValue) {
        identifiers.addAll(symbolicValue.accept(SymbolicIdentifierLocator.getInstance()));
      }
    }
    ExpressionValueVisitor visitor =
        new ExpressionValueVisitor(result, pLocation.getFunctionName(), machineModel, logger);
    for (SymbolicIdentifier identifier : identifiers) {
      result.assignConstant(
          identifier, assignment.getOrDefault(identifier, new NumericValue(0)), visitor);
    }
    return result;
  }

  /**
   * Compute for every node the number of edges to the nearest loop head or function call, by a
   * breadth-first search backwards from those locations.
   */
  private static ImmutableMap<CFANode, Integer> computeDistances(CFA pCfa) {
    Map<CFANode, Integer> distances = new HashMap<>();
    Deque<CFANode> waitlist = new ArrayDeque<>();
    for (CFANode node : pCfa.nodes()) {
      if (node.isLoopStart() || node.getLeavingEdges().anyMatch(ExecutionSampler::isCall)) {
        distances.put(node, 0);
        waitlist.add(node);
      }
    }
    while (!waitlist.isEmpty()) {
      CFANode node = waitlist.removeFirst();
      int distance = distances.get(node) + 1;
      for (CFAEdge edge : node.getAllEnteringEdges()) {
        CFANode predecessor = edge.getPredecessor();
        if (!distances.containsKey(predecessor)) {
          distances.put(predecessor, distance);
          waitlist.add(predecessor);
        }
      }
    }
    return ImmutableMap.copyOf(distances);
  }

  /**
   * Whether the edge calls a function. Calls to functions without a body count as well, because
   * they are just as interesting for an execution as the ones the analysis can enter.
   */
  private static boolean isCall(CFAEdge pEdge) {
    return pEdge instanceof FunctionCallEdge
        || (pEdge instanceof AStatementEdge statementEdge
            && statementEdge.getStatement() instanceof AFunctionCall);
  }

  /**
   * Whether all calls that may return a nondeterministic value are outside of every loop. Only then
   * is the number of inputs of the program bounded, and one assignment per input is enough.
   */
  private boolean checkInputsOutsideLoops(CFA pCfa) {
    Optional<LoopStructure> loopStructure = pCfa.getLoopStructure();
    if (loopStructure.isEmpty()) {
      return true;
    }
    ImmutableSet<CFANode> loopNodes =
        loopStructure.orElseThrow().getAllLoops().stream()
            .flatMap(loop -> loop.getLoopNodes().stream())
            .collect(toImmutableSet());

    for (CFAEdge edge : CFAUtils.allEdges(pCfa)) {
      if (loopNodes.contains(edge.getPredecessor()) && isCallToUndefinedFunction(edge, pCfa)) {
        logger.log(
            Level.INFO,
            "The call in line",
            edge.getFileLocation().getStartingLineInOrigin(),
            "may return a nondeterministic value and is inside a loop.");
        return false;
      }
    }
    return true;
  }

  /**
   * Whether the edge calls a function that has no body in the program, i.e. a function whose return
   * value the analysis does not know.
   */
  private static boolean isCallToUndefinedFunction(CFAEdge pEdge, CFA pCfa) {
    return pEdge instanceof AStatementEdge statementEdge
        && statementEdge.getStatement() instanceof AFunctionCall call
        && call.getFunctionCallExpression().getFunctionNameExpression()
            instanceof AIdExpression functionName
        && !pCfa.getAllFunctionNames().contains(functionName.getName());
  }
}
