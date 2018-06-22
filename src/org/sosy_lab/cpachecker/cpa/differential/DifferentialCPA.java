/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.differential;

import com.google.common.collect.ImmutableSet;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.core.Specification;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.differential.modifications.ModificationsCPA;
import org.sosy_lab.cpachecker.cpa.differential.modifications.ModificationsState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFATraversal;

public class DifferentialCPA implements ConfigurableProgramAnalysis {

  private final Configuration config;
  private final TransferRelation transferRelation;
  private AbstractState initialState;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(DifferentialCPA.class);
  }

  public DifferentialCPA(
      CFA pCfa, Configuration pConfig, LogManager pLogger, ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    config = pConfig;

    try {

      ModificationInfo modInfo = getModifications(pLogger, pShutdownNotifier, pCfa);

      transferRelation = new DifferentialTransferRelation(modInfo);

      if (modInfo.getNodesWithModification().isEmpty()) {
        pLogger.log(Level.INFO, "No differences between programs");
        initialState = DifferentialState.MODIFIED_NOT_REACHABLE;
      } else if (modInfo.getNodesWithModification().contains(pCfa.getMainFunction())) {
        pLogger.log(Level.INFO, "Programs may differ completely, full re-computation necessary");
        initialState = DifferentialState.MODIFIED;
      } else {
        initialState = DifferentialState.MODIFIED_REACHABLE;
      }

    } catch (InterruptedException | CPAException pE) {
      throw new AssertionError(pE);
    }
  }

  private ModificationInfo getModifications(
      LogManager pLogger, ShutdownNotifier pShutdownNotifier, CFA pCfa)
      throws InvalidConfigurationException, InterruptedException, CPAException {

    ImmutableSet<CFANode> modifiedNodes = getModifiedNodes(pLogger, pShutdownNotifier, pCfa);

    ImmutableSet.Builder<CFANode> modifiedReachableFrom = ImmutableSet.builder();
    for (CFANode modification : modifiedNodes) {
      modifiedReachableFrom.addAll(
          CFATraversal.dfs().backwards().collectNodesReachableFrom(modification));
    }

    return new ModificationInfo(modifiedNodes, modifiedReachableFrom.build());
  }

  private ImmutableSet<CFANode> getModifiedNodes(
      LogManager pLogger, ShutdownNotifier pShutdownNotifier, CFA pCfa)
      throws InvalidConfigurationException, CPAException, InterruptedException {
    String modificationsConfigPath = "getModifications.properties";
    Configuration modificationsConfig =
        Configuration.builder()
            .loadFromResource(ModificationsCPA.class, modificationsConfigPath)
            .copyOptionFromIfPresent(config, "differential.program")
            .build();
    ReachedSetFactory reachedFactory = new ReachedSetFactory(modificationsConfig, pLogger);
    ConfigurableProgramAnalysis cpa =
        new CPABuilder(modificationsConfig, pLogger, pShutdownNotifier, reachedFactory)
            .buildCPAs(pCfa, Specification.alwaysSatisfied(), new AggregatedReachedSets());
    Algorithm algorithm = CPAAlgorithm.create(cpa, pLogger, modificationsConfig, pShutdownNotifier);
    ReachedSet reached = reachedFactory.create();

    FunctionEntryNode mainFunction = pCfa.getMainFunction();

    StateSpacePartition partition = StateSpacePartition.getDefaultPartition();
    AbstractState initialStateOfModifications = cpa.getInitialState(mainFunction, partition);
    Precision initialPrecisionOfModifications = cpa.getInitialPrecision(mainFunction, partition);
    reached.add(initialStateOfModifications, initialPrecisionOfModifications);

    // populate reached set
    algorithm.run(reached);
    assert !reached.hasWaitingState()
        : "CPA algorithm finished, but waitlist not empty: " + reached.getWaitlist();

    ImmutableSet.Builder<CFANode> modifiedNodes = ImmutableSet.builder();
    for (AbstractState s : reached) {
      assert s instanceof ARGState : "AbstractState of reached set not a composite state: " + s;
      ModificationsState modificationsState =
          AbstractStates.extractStateByType(s, ModificationsState.class);
      if (modificationsState.hasModification()) {
        modifiedNodes.add(modificationsState.getLocationInGivenCfa());
      }
    }

    return modifiedNodes.build();
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return new FlatLatticeDomain();
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transferRelation;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return MergeSepOperator.getInstance();
  }

  @Override
  public StopOperator getStopOperator() {
    return new StopSepOperator(getAbstractDomain());
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    return initialState;
  }

  static class ModificationInfo {
    private final ImmutableSet<CFANode> nodesWithModification;
    private final ImmutableSet<CFANode> nodesModificationReachableFrom;

    public ModificationInfo(
        ImmutableSet<CFANode> pEdgesWithModification,
        ImmutableSet<CFANode> pNodesModificationReachableFrom) {
      nodesWithModification = pEdgesWithModification;
      nodesModificationReachableFrom = pNodesModificationReachableFrom;
    }

    public ImmutableSet<CFANode> getNodesWithModification() {
      return nodesWithModification;
    }

    public ImmutableSet<CFANode> getNodesModificationReachableFrom() {
      return nodesModificationReachableFrom;
    }
  }
}
