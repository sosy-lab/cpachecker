// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.oc;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackCPA;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.location.LocationCPA;
import org.sosy_lab.cpachecker.cpa.oc.ThreadInstance.InstanceKey;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * CPA that explores every thread instance of a concurrent program separately, over all its paths
 * (no feasibility checks, bounded loop unrolling), and fills an {@link OcExplorationRegistry} for
 * the subsequent ordering-consistency check.
 */
@Options(prefix = "cpa.oc")
public class OrderingConsistencyCPA extends AbstractCPA implements AutoCloseable {

  @Option(secure = true, description = "calls to these functions are the reachability targets")
  private Set<String> errorFunctions = ImmutableSet.of("reach_error");

  // per exploration round, set by the algorithm's iterative deepening via resetExploration
  private int maxLoopIterations = 5;

  private final CFA cfa;
  private final ImmutableSet<String> addressedVariables;
  private final Solver solver;
  private final PathFormulaManager pathFormulaManager;
  private OcExplorationRegistry registry = new OcExplorationRegistry();
  // roots of newly created thread instances, discovered during exploration; the algorithm seeds
  // them into the reached set as separate parentless roots (the exploration is a forest, one tree
  // per thread instance) rather than chaining each thread under its spawner
  private final Deque<OrderingConsistencyState> pendingThreadRoots = new ArrayDeque<>();
  private final LocationCPA locationCPA;
  private final CallstackCPA callstackCPA;
  private final OrderingConsistencyTransferRelation transferRelation;

  private OrderingConsistencyState initialState;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(OrderingConsistencyCPA.class);
  }

  public OrderingConsistencyCPA(
      Configuration pConfig, LogManager pLogger, ShutdownNotifier pShutdownNotifier, CFA pCfa)
      throws InvalidConfigurationException {
    super("sep", "never", null);
    pConfig.inject(this);
    cfa = pCfa;
    addressedVariables =
        pCfa.getVarClassification().isPresent()
            ? ImmutableSet.copyOf(pCfa.getVarClassification().orElseThrow().getAddressedVariables())
            : ImmutableSet.of();
    solver = Solver.create(pConfig, pLogger, pShutdownNotifier);
    pathFormulaManager =
        new PathFormulaManagerImpl(
            solver.getFormulaManager(),
            pConfig,
            pLogger,
            pShutdownNotifier,
            pCfa,
            AnalysisDirection.FORWARD);
    locationCPA = LocationCPA.create(pCfa, pConfig);
    callstackCPA = new CallstackCPA(pConfig, pLogger);
    transferRelation = new OrderingConsistencyTransferRelation(this, pShutdownNotifier);
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transferRelation;
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    if (initialState == null) {
      ThreadInstance mainInstance =
          registry.newInstance(new InstanceKey(-1, pNode.getFunctionName(), 0));
      checkState(mainInstance.getId() == ThreadInstance.MAIN_INSTANCE_ID);
      initialState =
          new OrderingConsistencyState(
              mainInstance.getId(),
              locationCPA.getStateFactory().getState(pNode),
              (CallstackState) callstackCPA.getInitialState(pNode, pPartition),
              pathFormulaManager.makeEmptyPathFormula(),
              solver.getFormulaManager().getBooleanFormulaManager().makeTrue(),
              ImmutableList.of(),
              ImmutableMap.of(),
              ImmutableSet.of(),
              ImmutableMap.of(),
              ImmutableMap.of(),
              false);
    }
    return initialState;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return this::mergeStates;
  }

  @Override
  public StopOperator getStopOperator() {
    // covers exactly the states absorbed by a merge; nothing else is ever covered
    return (pState, pReached, pPrecision) -> ((OrderingConsistencyState) pState).isAbsorbed();
  }

  /**
   * Merges two states of the same unrolled program point into one whose guard is the disjunction of
   * the incoming guards, turning the per-thread exploration into a DAG. Never merges into an
   * already expanded state: its suffix would be explored a second time with overlapping guards
   * (duplicated events); a missed merge only falls back to tree-shaped, sound exploration.
   */
  private AbstractState mergeStates(
      AbstractState pState1, AbstractState pState2, Precision pPrecision)
      throws CPAException, InterruptedException {
    OrderingConsistencyState s1 = (OrderingConsistencyState) pState1;
    OrderingConsistencyState s2 = (OrderingConsistencyState) pState2;
    if (s1 == s2
        || s1.isAbsorbed()
        || s2.isExpanded()
        || !s1.getMergeKey().equals(s2.getMergeKey())) {
      return s2;
    }
    BooleanFormulaManagerView bfmgr = solver.getFormulaManager().getBooleanFormulaManager();
    BooleanFormula mergedGuard = bfmgr.or(s1.getGuard(), s2.getGuard());

    // reconcile the branches' local SSA indices: makeOr yields
    // (guard1 and bridge1) or (guard2 and bridge2), the guarded phi assignments of the join
    PathFormula merged =
        pathFormulaManager.makeOr(
            s1.getPathFormula().withFormula(s1.getGuard()),
            s2.getPathFormula().withFormula(s2.getGuard()));
    registry.addPathConstraint(bfmgr.implication(mergedGuard, merged.getFormula()));

    ImmutableList<Integer> lastEvents =
        ImmutableSet.copyOf(Iterables.concat(s1.getLastEventIds(), s2.getLastEventIds())).asList();
    s1.markAbsorbed(); // the stop operator covers s1, only the merged state continues
    return new OrderingConsistencyState(
        s2.getInstanceId(),
        s2.getLocationState(),
        s2.getCallstackState(),
        pathFormulaManager.makeEmptyPathFormulaWithContextFrom(merged),
        mergedGuard,
        lastEvents,
        s2.getCreateCounts(),
        s2.getLiveInstanceIds(),
        s2.getLoopCounts(),
        s2.getLockDepths(),
        false);
  }

  public Solver getSolver() {
    return solver;
  }

  public PathFormulaManager getPathFormulaManager() {
    return pathFormulaManager;
  }

  public OcExplorationRegistry getRegistry() {
    return registry;
  }

  /** Records the root of a freshly created thread instance for the algorithm to seed as a root. */
  void addPendingThreadRoot(OrderingConsistencyState pRoot) {
    pendingThreadRoots.add(pRoot);
  }

  /** Removes and returns the next discovered thread root, or {@code null} if none remain. */
  public OrderingConsistencyState pollPendingThreadRoot() {
    return pendingThreadRoots.poll();
  }

  /**
   * Discards all exploration results and prepares a fresh round with the given loop bound. The
   * fresh-name counter is carried over into the new registry: the solver (and its formula manager)
   * lives across all rounds, so restarting the counter at zero could mint a name already used, at a
   * different type, by an earlier round (see {@link OcExplorationRegistry#OcExplorationRegistry
   * (int)}).
   */
  public void resetExploration(int pLoopBound) {
    maxLoopIterations = pLoopBound;
    registry = new OcExplorationRegistry(registry.getNextCssaIndex());
    pendingThreadRoots.clear();
    initialState = null;
    transferRelation.resetRegistry(registry);
  }

  CFA getCfa() {
    return cfa;
  }

  /** Qualified names of variables whose address is taken somewhere in the program. */
  ImmutableSet<String> getAddressedVariables() {
    return addressedVariables;
  }

  LocationCPA getLocationCPA() {
    return locationCPA;
  }

  /**
   * A bare location abstract state at the given node, used to wrap the synthetic ARG states of a
   * sequentialized counterexample path (which interleaves several threads and therefore has no
   * counterpart in the exploration's reached set).
   */
  public AbstractState locationStateFor(CFANode pNode) {
    return locationCPA.getStateFactory().getState(pNode);
  }

  CallstackCPA getCallstackCPA() {
    return callstackCPA;
  }

  int getMaxLoopIterations() {
    return maxLoopIterations;
  }

  Set<String> getErrorFunctions() {
    return errorFunctions;
  }

  @Override
  public void close() {
    solver.close();
  }
}
