// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries.loops;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.AbstractStrategy;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.GhostCFA;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategiesEnum;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategyDependencies.StrategyDependency;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

public class LoopStrategy extends AbstractStrategy {

  protected static final CSimpleType SIGNED_LONG_INT = CNumericTypes.SIGNED_LONG_INT;
  protected StrategiesEnum strategyEnum;

  protected LoopStrategy(
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      StrategyDependency pStrategyDependencies,
      StrategiesEnum pStrategyEnum,
      CFA pCFA) {
    super(pLogger, pShutdownNotifier, pStrategyDependencies, pCFA);
    strategyEnum = pStrategyEnum;
  }

  @Override
  public Optional<GhostCFA> summarize(final CFANode loopStartNode) {
    return Optional.empty();
  }

  protected final Optional<CFANode> determineLoopHead(final CFANode loopStartNode) {
    return determineLoopHead(
        loopStartNode,
        x -> summaryFilter.filter(x, ImmutableSet.of(StrategiesEnum.BASE, this.strategyEnum)));
  }

  protected static Set<AVariableDeclaration> getModifiedNonLocalVariables(Loop loop) {
    Set<AVariableDeclaration> modifiedVariables = loop.getModifiedVariables();
    Set<String> outofScopeVariables =
        FluentIterable.from(HavocStrategy.getOutOfScopeVariables(loop))
            .transform(x -> x.getQualifiedName())
            .toSet();
    modifiedVariables =
        FluentIterable.from(modifiedVariables)
            .filter(x -> !outofScopeVariables.contains(x.getQualifiedName()))
            .toSet();
    return modifiedVariables;
  }

  private static final Optional<CFANode> determineLoopHead(
      final CFANode loopStartNode, Predicate<? super CFAEdge> filterFunction) {
    List<CFAEdge> filteredOutgoingEdges =
        FluentIterable.from(loopStartNode.getLeavingEdges()).filter(filterFunction).toList();

    if (filteredOutgoingEdges.size() != 1) {
      return Optional.empty();
    }

    if (!isLoopInit(loopStartNode)) {
      return Optional.empty();
    }

    CFANode loopHead = filteredOutgoingEdges.get(0).getSuccessor();
    return Optional.of(loopHead);
  }

  /**
   * Returns true in case this node is the init of a loop, i.e., the single edge leaving this node
   * is the "while" blank edge marking the beginning of the loop.
   */
  public static final boolean isLoopInit(final CFANode node) {
    return !FluentIterable.from(node.getLeavingEdges())
        .filter(x -> x.getDescription().equals("while"))
        .isEmpty();
  }
}
