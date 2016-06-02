/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.targetreachability;

import com.google.common.collect.ImmutableSet;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.Specification;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;
import org.sosy_lab.cpachecker.core.defaults.MergeJoinOperator;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.automaton.TargetLocationProvider;
import org.sosy_lab.cpachecker.util.automaton.TargetLocationProviderImpl;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * CPA which marks the nodes as skippable if they are not backwards reachable
 * from the target state.
 */
@Options(prefix="cpa.property_reachability")
public class TargetReachabilityCPA extends SingleEdgeTransferRelation
    implements ConfigurableProgramAnalysis,
               StatisticsProvider,
               Statistics {
  @Option(secure=true, description="Do not follow states which can not "
      + "syntactically lead to a target location")
  private boolean noFollowBackwardsUnreachable = true;

  private final Timer backwardsReachability = new Timer();
  private final ImmutableSet<CFANode> targetReachableFrom;

  private final AbstractDomain abstractDomain;
  private final MergeOperator mergeOperator;
  private final StopOperator stopOperator;

  @SuppressWarnings("unused")
  private TargetReachabilityCPA(
      Configuration pConfig,
      ShutdownNotifier shutdownNotifier,
      LogManager pLogger,
      CFA pCfa,
      ReachedSetFactory pReachedSetFactory,
      Specification pSpecification)
      throws InvalidConfigurationException, CPAException {
    pConfig.inject(this);

    abstractDomain = new FlatLatticeDomain(ReachabilityState.RELEVANT_TO_TARGET);
    mergeOperator = new MergeJoinOperator(abstractDomain);
    stopOperator = new StopSepOperator(abstractDomain);

    TargetLocationProvider targetProvider =
        new TargetLocationProviderImpl(pReachedSetFactory, shutdownNotifier, pLogger, pCfa);
    if (noFollowBackwardsUnreachable) {
      backwardsReachability.start();
      ImmutableSet.Builder<CFANode> builder = ImmutableSet.builder();
      try {
        Set<CFANode> targetNodes =
            targetProvider.tryGetAutomatonTargetLocations(pCfa.getMainFunction(), pSpecification);
        for (CFANode target : targetNodes) {
          builder.addAll(CFATraversal.dfs().backwards()
              .collectNodesReachableFrom(target));
        }
      } finally {
        backwardsReachability.stop();
      }
      targetReachableFrom = builder.build();
    } else {
      targetReachableFrom = ImmutableSet.copyOf(pCfa.getAllNodes());
    }
  }

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(TargetReachabilityCPA.class);
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return abstractDomain;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return this;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return mergeOperator;
  }

  @Override
  public StopOperator getStopOperator() {
    return stopOperator;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return StaticPrecisionAdjustment.getInstance();
  }

  @Override
  public AbstractState getInitialState(
      CFANode node, StateSpacePartition partition) {
    return ReachabilityState.RELEVANT_TO_TARGET;
  }

  @Override
  public Precision getInitialPrecision(
      CFANode node, StateSpacePartition partition) {
    return SingletonPrecision.getInstance();
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState state, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {
    if (targetReachableFrom.contains(cfaEdge.getSuccessor())) {
      return Collections.singleton(ReachabilityState.RELEVANT_TO_TARGET);
    } else {
      return Collections.singleton(ReachabilityState.IRRELEVANT_TO_TARGET);
    }
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(this);
  }

  @Override
  public void printStatistics(
      PrintStream out, Result result, ReachedSet reached) {
    out.println(
        "Time spent in pre-calculating backwards-reachable nodes: "
            + backwardsReachability.prettyFormat());
  }

  @Nullable
  @Override
  public String getName() {
    return "Property Reachability Statistics";
  }
}
