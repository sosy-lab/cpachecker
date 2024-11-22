// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Equivalence.Wrapper;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableSet;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.IntegerOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.conditions.path.AssignmentsInPathCondition.UniqueAssignmentsInPathConditionState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.smg2.abstraction.SMGCPAAbstractionManager;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGException;
import org.sosy_lab.cpachecker.cpa.smg2.util.ValueAndValueSize;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.LiveVariables;
import org.sosy_lab.cpachecker.util.smg.datastructures.PersistentSet;
import org.sosy_lab.cpachecker.util.smg.graph.SMGHasValueEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

public class SMGPrecisionAdjustment implements PrecisionAdjustment {

  @Options(prefix = "cpa.smg2.abstraction")
  public static class PrecAdjustmentOptions {

    @Option(secure = true, description = "restrict abstraction computations to branching points")
    private boolean alwaysAtBranch = false;

    @Option(secure = true, description = "restrict abstraction computations to join points")
    private boolean alwaysAtJoin = false;

    @Option(
        secure = true,
        description = "restrict abstraction computations to function calls/returns")
    private boolean alwaysAtFunction = false;

    @Option(
        secure = true,
        description =
            "If enabled, abstraction computations at loop-heads are enabled. List abstraction has"
                + " to be enabled for this.")
    private boolean alwaysAtLoop = false;

    @Option(
        secure = true,
        description =
            "toggle liveness abstraction. Is independent of CEGAR, but dependent on the CFAs"
                + " liveness variables being tracked. Might be unsound for stack-based memory"
                + " structures like arrays.")
    private boolean doLivenessAbstraction = true;

    @Option(
        secure = true,
        description =
            "toggle memory sensitive liveness abstraction. Liveness abstraction is supposed to"
                + " simply abstract all variables away (invalidating memory) when unused, even if"
                + " there is valid outside pointers on them. With this option enabled, it is first"
                + " checked if there is a valid address still pointing to the variable before"
                + " removing it. Liveness abstraction might be unsound without this option.")
    private boolean doEnforcePointerSensitiveLiveness = true;

    @Option(
        secure = true,
        description =
            "restrict liveness abstractions to nodes with more than one entering and/or leaving"
                + " edge")
    private boolean onlyAtNonLinearCFA = false;

    @Option(
        secure = true,
        description =
            "skip abstraction computations until the given number of iterations are reached,"
                + " after that decision is based on then current level of determinism,"
                + " setting the option to -1 always performs abstraction computations")
    @IntegerOption(min = -1)
    private int iterationThreshold = -1;

    @Option(
        secure = true,
        description =
            "threshold for level of determinism, in percent, up-to which abstraction computations "
                + "are performed (and iteration threshold was reached)")
    @IntegerOption(min = 0, max = 100)
    @SuppressFBWarnings(value = "URF_UNREAD_FIELD", justification = "false alarm")
    private int determinismThreshold = 85;

    @Option(
        secure = true,
        name = "listAbstractionMinimumLengthThreshold",
        description =
            "The minimum list segments directly following each other with the same value needed to"
                + " abstract them.Minimum is 2.")
    private int listAbstractionMinimumLengthThreshold = 4;

    @Option(
        secure = true,
        name = "listAbstractionMaximumIncreaseLengthThreshold",
        description =
            "The minimum list segments that are needed for abstraction may be increased during the"
                + " analysis based on a heuristic in fixed sized loops. This is the maximum"
                + " increase that is allowed. E.g. all lists with the length given here are"
                + " abstracted in any case. If you want to prevent dynamic increase of list"
                + " abstraction min threshold set this to the same value as"
                + " listAbstractionMinimumLengthThreshold.")
    private int listAbstractionMaximumIncreaseLengthThreshold = 6;

    @Option(
        secure = true,
        name = "abstractHeapValues",
        description = "If heap values are to be abstracted based on CEGAR.")
    private boolean abstractHeapValues = false;

    @Option(
        secure = true,
        name = "abstractProgramVariables",
        description = "Abstraction of program variables via CEGAR.")
    private boolean abstractProgramVariables = false;

    @Option(
        secure = true,
        name = "abstractLinkedLists",
        description = "Abstraction of all detected linked lists at loop heads.")
    private boolean abstractLinkedLists = true;

    @Option(
        secure = true,
        name = "removeUnusedConstraints",
        description = "Periodically removes unused constraints from the state.")
    private boolean cleanUpUnusedConstraints = false;

    // TODO: the goal is to set this in a CEGAR loop one day
    @Option(
        secure = true,
        name = "abstractConcreteValuesAboveThreshold",
        description =
            "Periodically removes concrete values from the memory model and replaces them with"
                + " symbolic values. Only the newest concrete values above this threshold are"
                + " removed. For negative numbers this option is ignored. Note: 0 also removes the"
                + " null value, reducing impacting null dereference or free soundness. Currently"
                + " only supported for given value 0.")
    private int abstractConcreteValuesAboveThreshold = -1;

    private final @Nullable ImmutableSet<CFANode> loopHeads;

    public PrecAdjustmentOptions(Configuration config, CFA pCfa)
        throws InvalidConfigurationException {
      config.inject(this);

      if (alwaysAtLoop && pCfa.getAllLoopHeads().isPresent()) {
        loopHeads = pCfa.getAllLoopHeads().orElseThrow();
      } else {
        loopHeads = null;
      }
    }

    public boolean getCleanUpUnusedConstraints() {
      return cleanUpUnusedConstraints;
    }

    public int getAbstractConcreteValuesAboveThreshold() {
      return abstractConcreteValuesAboveThreshold;
    }

    public int getListAbstractionMinimumLengthThreshold() {
      return listAbstractionMinimumLengthThreshold;
    }

    public boolean isEnforcePointerSensitiveLiveness() {
      return doEnforcePointerSensitiveLiveness;
    }

    public int getListAbstractionMaximumIncreaseLengthThreshold() {
      return listAbstractionMaximumIncreaseLengthThreshold;
    }

    public void incListAbstractionMinimumLengthThreshold() {
      listAbstractionMinimumLengthThreshold++;
    }

    /**
     * This method determines whether to abstract at each location.
     *
     * @return true, if an abstraction should be computed at each location, else false
     */
    private boolean abstractAtEachLocation() {
      return !alwaysAtBranch && !alwaysAtJoin && !alwaysAtFunction && !alwaysAtLoop;
    }

    private boolean abstractAtBranch(LocationState location) {
      return alwaysAtBranch && location.getLocationNode().getNumLeavingEdges() > 1;
    }

    private boolean abstractAtJoin(LocationState location) {
      return alwaysAtJoin && location.getLocationNode().getNumEnteringEdges() > 1;
    }

    private boolean abstractAtFunction(LocationState location) {
      return alwaysAtFunction
          && (location.getLocationNode() instanceof FunctionEntryNode
              || location.getLocationNode().getEnteringSummaryEdge() != null);
    }

    private boolean abstractAtLoop(LocationState location) {
      checkState(!alwaysAtLoop || loopHeads != null);
      return alwaysAtLoop && loopHeads.contains(location.getLocationNode());
    }
  }

  public static class PrecAdjustmentStatistics implements Statistics {

    final StatCounter abstractions = new StatCounter("Number of abstraction computations");
    private StatTimer totalLivenessTimer;
    private StatTimer totalAbstractionTimer;
    private StatTimer totalEnforcePathTimer;

    @Override
    public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
      renewTimers();
      StatisticsWriter writer = StatisticsWriter.writingStatisticsTo(pOut);
      writer.put(abstractions);
      writer.put(totalLivenessTimer);
      writer.put(totalAbstractionTimer);
      writer.put(totalEnforcePathTimer);
    }

    @Override
    public String getName() {
      return "SMGPrecisionAdjustment";
    }

    public void renewTimers() {
      totalLivenessTimer = new StatTimer("Total time for liveness abstraction");
      totalAbstractionTimer = new StatTimer("Total time for abstraction computation");
      totalEnforcePathTimer = new StatTimer("Total time for path thresholds");
    }
  }

  private final SMGCPAStatistics stats;
  private final PrecAdjustmentOptions options;
  private final Optional<LiveVariables> liveVariables;
  private final Optional<ImmutableSet<CFANode>> maybeLoops;
  private Optional<Set<CFANode>> maybeLoopLeavers;

  // for statistics
  private final StatCounter abstractions;
  private final StatTimer totalLiveness;
  private final StatTimer totalAbstraction;
  private final StatTimer totalEnforcePath;

  private Set<Value> concreteValuesSavedInSMGBelowThreashold = new HashSet<>();

  @SuppressFBWarnings(value = "URF_UNREAD_FIELD", justification = "false alarm")
  private boolean performPrecisionBasedAbstraction = false;

  public SMGPrecisionAdjustment(
      final SMGCPAStatistics pStats,
      final CFA pCfa,
      final PrecAdjustmentOptions pOptions,
      final PrecAdjustmentStatistics pStatistics) {

    options = pOptions;
    stats = pStats;
    liveVariables = pCfa.getLiveVariables();
    maybeLoops = pCfa.getAllLoopHeads();
    maybeLoopLeavers = Optional.empty();

    abstractions = pStatistics.abstractions;
    pStatistics.renewTimers();
    totalLiveness = pStatistics.totalLivenessTimer;
    totalAbstraction = pStatistics.totalAbstractionTimer;
    totalEnforcePath = pStatistics.totalEnforcePathTimer;
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(
      AbstractState pState,
      Precision pPrecision,
      UnmodifiableReachedSet pStates,
      Function<AbstractState, AbstractState> projection,
      AbstractState fullState)
      throws CPAException, InterruptedException {

    return prec(
        (SMGState) pState,
        (VariableTrackingPrecision) pPrecision,
        AbstractStates.extractStateByType(fullState, LocationState.class),
        AbstractStates.extractStateByType(fullState, UniqueAssignmentsInPathConditionState.class));
  }

  private Optional<PrecisionAdjustmentResult> prec(
      final SMGState pState,
      VariableTrackingPrecision pPrecision,
      LocationState location,
      UniqueAssignmentsInPathConditionState assignments) {
    SMGState resultState = pState;

    if ((options.doLivenessAbstraction || options.abstractProgramVariables)
        && liveVariables.isPresent()) {
      totalLiveness.start();
      resultState = enforceLiveness(pState, location, resultState);
      totalLiveness.stop();
    }

    // compute the abstraction based on the value-analysis precision
    totalAbstraction.start();
    if (performPrecisionBasedAbstraction()) {
      resultState = enforcePrecision(resultState, location, pPrecision);
    }
    totalAbstraction.stop();

    // compute the abstraction for assignment thresholds
    if (assignments != null) {
      totalEnforcePath.start();
      if (options.abstractProgramVariables) {
        resultState = enforcePathThreshold(resultState, assignments);
      }
      totalEnforcePath.stop();
    }

    if (options.getAbstractConcreteValuesAboveThreshold() >= 0) {
      resultState =
          enforceConcreteValueThreshold(
              resultState, options.getAbstractConcreteValuesAboveThreshold());
    }

    if (options.abstractLinkedLists && checkAbstractListAt(location)) {
      // Abstract Lists at loop heads
      try {
        resultState =
            new SMGCPAAbstractionManager(
                    resultState, options.getListAbstractionMinimumLengthThreshold(), stats)
                .findAndAbstractLists();
      } catch (SMGException e) {
        // Do nothing. This should never happen anyway
        throw new RuntimeException(e);
      }
    }

    if (options.getCleanUpUnusedConstraints()) {
      resultState = resultState.removeOldConstraints();
    }

    return Optional.of(new PrecisionAdjustmentResult(resultState, pPrecision, Action.CONTINUE));
  }

  private boolean isLoopHead(LocationState location) {
    return maybeLoops.isPresent() && maybeLoops.orElseThrow().contains(location.getLocationNode());
  }

  @SuppressWarnings("unused")
  private boolean isLoopLeaving(LocationState location) {
    if (maybeLoopLeavers.isEmpty()) {
      ImmutableSet.Builder<CFANode> builder = ImmutableSet.builder();
      for (Set<CFANode> bla : getAllLoopHeadNodesToLeavingNodes().orElseThrow().values()) {
        builder.addAll(bla);
      }
      // TODO: clean this up once it is sufficiently debugged
      maybeLoopLeavers = Optional.ofNullable(builder.build());
    }
    // Just left a loop.
    // We detect this by checking if the current location is a node that is following a loop head
    // Abstracting here allows us to subsume most lists into the first abstracted that left the loop
    return maybeLoopLeavers.isPresent()
        && maybeLoopLeavers.orElseThrow().contains(location.getLocationNode());
  }

  private Optional<Map<CFANode, Set<CFANode>>> getAllLoopHeadNodesToLeavingNodes() {
    if (maybeLoops.isEmpty()) {
      return Optional.empty();
    }
    Map<CFANode, Set<CFANode>> loopHeadToLoopLeavers = new HashMap<>();
    for (CFANode loopHeadNode : maybeLoops.orElseThrow()) {
      Set<CFANode> cache = new HashSet<>();
      for (int i = 0; i < loopHeadNode.getNumLeavingEdges(); i++) {
        CFAEdge leavingEdge = loopHeadNode.getLeavingEdge(i);
        int loopBeginLine = leavingEdge.getFileLocation().getStartingLineInOrigin();
        int loopEndLine = leavingEdge.getFileLocation().getEndingLineInOrigin();
        // The first CFAEdge that exceeds loopEndLine is a loop exiting location
        Optional<CFANode> maybeLoopLeavingNode =
            getLeavingNode(leavingEdge.getSuccessor(), loopBeginLine, loopEndLine, cache);
        if (maybeLoopLeavingNode.isPresent()) {
          CFANode loopLeavingNode = maybeLoopLeavingNode.orElseThrow();
          if (loopHeadToLoopLeavers.containsKey(loopHeadNode)) {
            loopHeadToLoopLeavers.get(loopHeadNode).add(loopLeavingNode);
          } else {
            Set<CFANode> newSet = new HashSet<>();
            newSet.add(loopLeavingNode);
            loopHeadToLoopLeavers.put(loopHeadNode, newSet);
          }
        }
      }
      Preconditions.checkArgument(loopHeadToLoopLeavers.containsKey(loopHeadNode));
    }
    return Optional.of(loopHeadToLoopLeavers);
  }

  private Optional<CFANode> getLeavingNode(
      CFANode node, int loopBeginLine, int loopEndLine, Set<CFANode> visited) {
    if (visited.contains(node)) {
      return Optional.empty();
    }
    visited.add(node);
    for (int i = 0; i < node.getNumLeavingEdges(); i++) {
      CFAEdge leavingEdge = node.getLeavingEdge(i);
      if (leavingEdge.getFileLocation().getStartingLineInOrigin() > loopEndLine) {
        // Loop leaving edge
        return Optional.of(node);
      }
      if (leavingEdge.getFileLocation().getStartingLineInOrigin() < loopBeginLine) {
        // Loop leaving edge (e.g. a goTo)
        return Optional.of(node);
      }
      Optional<CFANode> recNode =
          getLeavingNode(leavingEdge.getSuccessor(), loopBeginLine, loopEndLine, visited);
      if (recNode.isPresent()) {
        return recNode;
      }
    }
    return Optional.empty();
  }

  private boolean checkAbstractListAt(LocationState location) {
    return options.abstractAtFunction(location) || isLoopHead(location);
  }

  /**
   * This method decides whether to perform abstraction computations. These are computed if the
   * iteration threshold is deactivated, or if the level of determinism ever gets below the
   * threshold for the level of determinism.
   *
   * @return true, if abstractions should be computed, else false
   */
  private boolean performPrecisionBasedAbstraction() {
    // always compute abstraction if option is disabled
    if (options.iterationThreshold == -1) {
      return true;
    }

    // else, delay abstraction computation as long as iteration threshold is not reached
    if (stats.getCurrentNumberOfIterations() < options.iterationThreshold) {
      return false;
    }

    // else, always compute abstraction if computed abstraction before
    if (performPrecisionBasedAbstraction) {
      return true;
    }

    // else, determine current setting and return that
    performPrecisionBasedAbstraction =
        stats.getCurrentLevelOfDeterminism() < options.determinismThreshold;

    return performPrecisionBasedAbstraction;
  }

  private SMGState enforceLiveness(
      final SMGState pState, LocationState location, SMGState initialState) {
    CFANode actNode = location.getLocationNode();
    SMGState currentState = initialState;
    boolean hasMoreThanOneEnteringLeavingEdge =
        actNode.getNumEnteringEdges() > 1 || actNode.getNumLeavingEdges() > 1;

    if (!options.onlyAtNonLinearCFA || hasMoreThanOneEnteringLeavingEdge) {
      boolean onlyBlankEdgesEntering = true;
      for (int i = 0; i < actNode.getNumEnteringEdges() && onlyBlankEdgesEntering; i++) {
        onlyBlankEdgesEntering = location.getLocationNode().getEnteringEdge(i) instanceof BlankEdge;
      }

      // when there are only blank edges that lead to this state, then we can
      // skip the abstraction, after a blank edge there cannot be a variable
      // less live
      if (!onlyBlankEdgesEntering) {
        for (MemoryLocation variable : pState.getTrackedMemoryLocations()) {
          String qualifiedVarName = variable.getQualifiedName();
          if (!liveVariables
              .orElseThrow()
              .isVariableLive(qualifiedVarName, location.getLocationNode())) {
            if (!options.isEnforcePointerSensitiveLiveness()) {
              // TODO: LiveVariablesCPA fails to track stack based memory correctly and invalidates
              //  e.g. arrays to early. Hence isEnforcePointerInsensitiveLiveness = true is unsound!
              currentState = currentState.invalidateVariable(variable);

            } else {
              // Don't invalidate memory that may have valid outside pointers to it that may keep it
              // alive!
              Optional<SMGObject> maybeVarObj =
                  currentState.getMemoryModel().getObjectForVariable(qualifiedVarName);
              if (maybeVarObj.isPresent()) {
                // Does not contain itself
                Set<SMGObject> allObjsPointingTowards =
                    currentState
                        .getMemoryModel()
                        .getSmg()
                        .getAllSourcesForPointersPointingTowards(maybeVarObj.orElseThrow())
                        .stream()
                        .filter(o -> !o.equals(maybeVarObj.orElseThrow()))
                        .collect(ImmutableSet.toImmutableSet());
                if (allObjsPointingTowards.isEmpty()) {
                  currentState = currentState.invalidateVariable(variable);
                }
              }
            }
          }
        }
      }
    }
    return currentState;
  }

  /**
   * This method performs an abstraction computation on the current state.
   *
   * @param location the current location
   * @param state the current state
   * @param precision the current precision
   */
  private SMGState enforcePrecision(
      final SMGState state, LocationState location, VariableTrackingPrecision precision) {

    SMGState currentState = state;
    if (options.abstractAtEachLocation()
        || options.abstractAtBranch(location)
        || options.abstractAtJoin(location)
        || options.abstractAtFunction(location)
        || options.abstractAtLoop(location)) {

      if (options.abstractProgramVariables) {
        for (MemoryLocation memoryLocation :
            currentState.getMemoryModel().getMemoryLocationsAndValuesForSPCWithoutHeap().keySet()) {
          CType type = currentState.getMemoryModel().getTypeOfVariable(memoryLocation);
          if (location != null
              && !precision.isTracking(memoryLocation, type, location.getLocationNode())) {
            currentState = currentState.copyAndForget(memoryLocation).getState();
          }
        }
      }
      if (precision instanceof SMGPrecision smgPrecision && options.abstractHeapValues) {
        currentState = currentState.enforceHeapValuePrecision(smgPrecision.getTrackedHeapValues());
      }

      abstractions.inc();
    }
    return currentState;
  }

  /**
   * This method abstracts variables that exceed the threshold of assignments along the current
   * path.
   *
   * @param state the state to abstract
   * @param assignments the assignment information
   */
  private SMGState enforcePathThreshold(
      final SMGState state, UniqueAssignmentsInPathConditionState assignments) {

    SMGState currentState = state;
    // forget the value for all variables that exceed their threshold
    for (Entry<MemoryLocation, ValueAndValueSize> e :
        currentState.getMemoryModel().getMemoryLocationsAndValuesForSPCWithoutHeap().entrySet()) {
      MemoryLocation memoryLocation = e.getKey();
      assignments.updateAssignmentInformation(memoryLocation, e.getValue().getValue());

      if (assignments.exceedsThreshold(memoryLocation)) {
        currentState = currentState.copyAndForget(memoryLocation).getState();
      }
    }
    return currentState;
  }

  /**
   * Ordered list of concrete values that have been saved in the SMG, in order of saving. Note: does
   * NOT delete values when removed from the SMG!
   */
  private Set<Value> getConcreteValueAllowed() {
    return concreteValuesSavedInSMGBelowThreashold;
  }

  /**
   * Ordered list of concrete values that have been saved in the SMG, in order of saving. Note: does
   * NOT delete values when removed from the SMG!
   */
  private void updateConcreteValueOrder(SMGState state, int numOfConcreteValuesAllowed) {
    // +2 because of 0, 0.0 and 0.0 (float and double) are always present
    if (concreteValuesSavedInSMGBelowThreashold.size() < numOfConcreteValuesAllowed + 2) {
      for (Wrapper<Value> wValue : state.getMemoryModel().getValueToSMGValueMapping().keySet()) {
        if (wValue.get() != null && wValue.get().isNumericValue()) {
          concreteValuesSavedInSMGBelowThreashold.add(wValue.get());
        }
      }
    }
  }

  private SMGState enforceConcreteValueThreshold(
      final SMGState state, int numOfConcreteValuesAllowed) {
    updateConcreteValueOrder(state, numOfConcreteValuesAllowed);
    // TODO: try to remove 0 only for a non pointer type
    SMGState currentState = state;
    // Gather all concrete values first and filter out all above the threshold
    ImmutableBiMap<SMGValue, Wrapper<Value>> mapping =
        currentState.getMemoryModel().getValueToSMGValueMapping().inverse();
    // remove all concrete values above the threshold (will be replaced by symbolics by reading)
    for (Entry<SMGObject, PersistentSet<SMGHasValueEdge>> objAndHVEs :
        currentState.getMemoryModel().getSmg().getSMGObjectsWithSMGHasValueEdges().entrySet()) {
      SMGObject object = objAndHVEs.getKey();
      Set<SMGHasValueEdge> edgesToRemove = new HashSet<>();
      for (SMGHasValueEdge hve : objAndHVEs.getValue()) {
        SMGValue smgValue = hve.hasValue();
        Wrapper<Value> wValue = mapping.get(smgValue);
        if (wValue != null && wValue.get().isNumericValue()) {
          if (!getConcreteValueAllowed().contains(wValue.get())) {
            edgesToRemove.add(hve);
          }
        }
      }
      currentState =
          currentState.copyAndReplaceMemoryModel(
              currentState
                  .getMemoryModel()
                  .copyWithNewSMG(
                      currentState
                          .getMemoryModel()
                          .getSmg()
                          .copyAndRemoveHVEdges(edgesToRemove, object)));
    }
    return currentState;
  }
}
