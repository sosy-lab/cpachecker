// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.distance;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;

@Options(prefix = "cpa.distance")
public class DistanceCPA implements ConfigurableProgramAnalysis {

  public enum TransferMode {
    CHEAP,
    EXPENSIVE
  }

  @Option(name = "maxDistance", description = "maximum distance to be tracked by DistanceCPA")
  private int maxDistance = 3;

  @Option(name = "transferMode", description = "transfer mode to be used by DistanceCPA")
  private TransferMode transferMode = TransferMode.CHEAP;

  private CounterexampleInfo counterexampleInfo;

  public DistanceCPA(Configuration pConfig, LogManager pLogManager)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    if (maxDistance < 0) {
      pLogManager.logf(
          Level.INFO,
          "DistanceCPA: maxDistance is negative (%d), setting to %d",
          maxDistance,
          Integer.MAX_VALUE);
      maxDistance = Integer.MAX_VALUE;
    }
  }

  public void init(CounterexampleInfo pCounterexampleInfo) {
    counterexampleInfo = pCounterexampleInfo;
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return new FlatLatticeDomain();
  }

  @Override
  public TransferRelation getTransferRelation() {
    return new DistanceTransferRelation(transferMode);
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
    ImmutableSet<@NonNull CFANode> visitedNodes =
        FluentIterable.from(counterexampleInfo.getTargetPath().getFullPath())
            .filter(e -> e != null)
            .transformAndConcat(e -> ImmutableSet.of(e.getPredecessor(), e.getSuccessor()))
            .toSet();
    return new DistanceAbstractState(
        visitedNodes.contains(node) ? 0 : 1, visitedNodes, maxDistance);
  }

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(DistanceCPA.class);
  }
}
