// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.testtargets;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.DelegateAbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.util.CFAUtils;

@Options(prefix = "testcase")
public class TestTargetCPA extends AbstractCPA {

  private final TestTargetPrecisionAdjustment precisionAdjustment;
  private final TransferRelation transferRelation;

  @Option(
      secure = true,
      name = "generate.parallel",
      description = "set to true if run multiple test case generation instances in parallel")
  private boolean runParallel = false;

  @Option(
      secure = true,
      name = "targets.type", // adapt CPAMain.java if adjust name
      description = "Which CFA edges to use as test targets")
  private TestTargetType targetType = TestTargetType.ASSUME;

  @Option(
      secure = true,
      name = "targets.funName", // adapt CPAMain.java if adjust name
      description = "Name of target function if target type is FUN_CALL")
  private String targetFun = null;

  @Option(
      secure = true,
      name = "targets.optimization.strategy",
      description = "Which strategy to use to optimize set of test target edges")
  private TestTargetAdaption targetOptimization = TestTargetAdaption.NONE;

  @Option(
      secure = true,
      name = "targets.edge",
      description =
          "CFA edge if only a specific edge should be considered, e.g., in counterexample check")
  private String targetEdge = null;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(TestTargetCPA.class);
  }

  public TestTargetCPA(final CFA pCfa, final Configuration pConfig)
      throws InvalidConfigurationException {
    super("sep", "sep", DelegateAbstractDomain.<TestTargetState>getInstance(), null);

    pConfig.inject(this);
    if (targetType == TestTargetType.FUN_CALL && targetFun == null) {
      throw new InvalidConfigurationException(
          "If you choose target type to be FUN_CALL, you need to specify the target function.");
    }

    precisionAdjustment = new TestTargetPrecisionAdjustment();
    transferRelation =
        new TestTargetTransferRelation(
            targetEdge == null
                ? TestTargetProvider.getTestTargets(
                    pCfa, runParallel, targetType, targetFun, targetOptimization)
                : findTargetEdge(pCfa));
  }

  private Set<CFAEdge> findTargetEdge(final CFA pCfa) {
    Preconditions.checkNotNull(targetEdge);
    List<String> components = Splitter.on('#').splitToList(targetEdge);
    if (components.size() > 1) {
      try {
        int predNum = Integer.parseInt(components.get(0));
        int edgeID = Integer.parseInt(components.get(1));
        Optional<CFANode> pred =
            pCfa.getAllNodes().stream()
                .filter(node -> (node.getNodeNumber() == predNum))
                .findFirst();
        if (pred.isPresent()) {
          for (CFAEdge edge : CFAUtils.allLeavingEdges(pred.orElseThrow())) {
            if (System.identityHashCode(edge) == edgeID) {
              return ImmutableSet.of(edge);
            }
          }
        }

      } catch (NumberFormatException e) {
        return ImmutableSet.of();
      }
    }

    return ImmutableSet.of();
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transferRelation;
  }

  @Override
  public AbstractState getInitialState(final CFANode pNode, final StateSpacePartition pPartition)
      throws InterruptedException {
    return TestTargetState.noTargetState();
  }

  @Override
  public TestTargetPrecisionAdjustment getPrecisionAdjustment() {
    return precisionAdjustment;
  }
}
