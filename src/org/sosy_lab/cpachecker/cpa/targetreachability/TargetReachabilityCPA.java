// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.targetreachability;

import com.google.common.collect.ImmutableSet;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
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
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.automaton.TargetLocationProvider;
import org.sosy_lab.cpachecker.util.automaton.TargetLocationProviderImpl;

/**
 * CPA which marks the nodes as skippable if they are not backwards reachable from the target state.
 */
@Options(prefix = "cpa.property_reachability")
public class TargetReachabilityCPA extends AbstractCPA implements StatisticsProvider, Statistics {
  @Option(
      secure = true,
      description =
          "Do not follow states which can not " + "syntactically lead to a target location")
  private boolean noFollowBackwardsUnreachable = true;

  private final Timer backwardsReachability = new Timer();
  private final ImmutableSet<CFANode> targetReachableFrom;

  private TargetReachabilityCPA(
      Configuration pConfig,
      ShutdownNotifier shutdownNotifier,
      LogManager pLogger,
      CFA pCfa,
      Specification pSpecification)
      throws InvalidConfigurationException {
    super(
        "join",
        "sep",
        new FlatLatticeDomain(ReachabilityState.RELEVANT_TO_TARGET),
        null /* never used */);
    pConfig.inject(this);
    targetReachableFrom = getReachableNodes(shutdownNotifier, pLogger, pCfa, pSpecification);
  }

  private ImmutableSet<CFANode> getReachableNodes(
      ShutdownNotifier shutdownNotifier,
      LogManager pLogger,
      CFA pCfa,
      Specification pSpecification) {
    TargetLocationProvider targetProvider =
        new TargetLocationProviderImpl(shutdownNotifier, pLogger, pCfa);
    if (noFollowBackwardsUnreachable) {
      backwardsReachability.start();
      ImmutableSet.Builder<CFANode> builder = ImmutableSet.builder();
      try {
        Set<CFANode> targetNodes =
            targetProvider.tryGetAutomatonTargetLocations(pCfa.getMainFunction(), pSpecification);
        for (CFANode target : targetNodes) {
          builder.addAll(CFATraversal.dfs().backwards().collectNodesReachableFrom(target));
        }
      } finally {
        backwardsReachability.stop();
      }
      return builder.build();
    } else {
      return ImmutableSet.copyOf(pCfa.getAllNodes());
    }
  }

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(TargetReachabilityCPA.class);
  }

  @Override
  public TransferRelation getTransferRelation() {
    return new TargetReachabilityTransferRelation(targetReachableFrom);
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition) {
    return ReachabilityState.RELEVANT_TO_TARGET;
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(this);
  }

  @Override
  public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {
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
