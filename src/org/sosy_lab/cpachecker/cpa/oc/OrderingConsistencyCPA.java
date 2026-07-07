// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.oc;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
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
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackCPA;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.location.LocationCPA;
import org.sosy_lab.cpachecker.cpa.oc.ThreadInstance.InstanceKey;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;

/**
 * CPA that explores every thread instance of a concurrent program separately, over all its paths
 * (no feasibility checks, bounded loop unrolling), and fills an {@link OcExplorationRegistry} for
 * the subsequent ordering-consistency check.
 */
@Options(prefix = "cpa.oc")
public class OrderingConsistencyCPA extends AbstractCPA implements AutoCloseable {

  @Option(
      secure = true,
      description =
          "maximum number of times a loop head may be entered on one path before the path is cut")
  private int maxLoopIterations = 5;

  @Option(secure = true, description = "calls to these functions are the reachability targets")
  private Set<String> errorFunctions = ImmutableSet.of("reach_error");

  private final CFA cfa;
  private final ImmutableSet<String> addressedVariables;
  private final Solver solver;
  private final PathFormulaManager pathFormulaManager;
  private final OcExplorationRegistry registry = new OcExplorationRegistry();
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
              MemoryEvent.NO_EVENT,
              ImmutableMap.of(),
              ImmutableMap.of(),
              ImmutableMap.of(),
              false);
    }
    return initialState;
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
