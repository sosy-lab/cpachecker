// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.graph;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.EndpointPair;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.CFAUtils;

/** A {@link CfaNetwork} representing a set of functions specified by their function names. */
final class FunctionFilteringCfaNetwork extends AbstractCfaNetwork {

  private final CFA cfa;
  private final ImmutableSet<String> functionNames;

  private FunctionFilteringCfaNetwork(CFA pCfa, ImmutableSet<String> pFunctionNames) {
    cfa = pCfa;
    functionNames = pFunctionNames;
  }

  static CfaNetwork forFunctions(CFA pCfa, Set<String> pFunctionNames) {
    return new FunctionFilteringCfaNetwork(checkNotNull(pCfa), ImmutableSet.copyOf(pFunctionNames));
  }

  @Override
  public Set<CFANode> nodes() {
    return new UnmodifiableSetView<>() {

      @Override
      public Iterator<CFANode> iterator() {
        return new AbstractIterator<>() {

          private final Set<CFANode> waitlisted =
              functionNames.stream()
                  .map(function -> cfa.getAllFunctions().get(function))
                  .filter(Objects::nonNull)
                  .collect(Collectors.toCollection(HashSet::new));
          private final Deque<CFANode> waitlist = new ArrayDeque<>(waitlisted);

          @Override
          protected @Nullable CFANode computeNext() {

            while (!waitlist.isEmpty()) {

              CFANode node = waitlist.remove();

              for (CFANode successor : CFAUtils.allSuccessorsOf(node)) {
                if (functionNames.contains(successor.getFunctionName())
                    && waitlisted.add(successor)) {
                  waitlist.add(successor);
                }
              }

              return node;
            }

            return endOfData();
          }
        };
      }
    };
  }

  @Override
  public Set<CFAEdge> inEdges(CFANode pNode) {
    return new UnmodifiableSetView<>() {

      @Override
      public Iterator<CFAEdge> iterator() {
        return CFAUtils.allEnteringEdges(pNode)
            .filter(edge -> functionNames.contains(edge.getPredecessor().getFunctionName()))
            .iterator();
      }
    };
  }

  @Override
  public Set<CFAEdge> outEdges(CFANode pNode) {
    return new UnmodifiableSetView<>() {

      @Override
      public Iterator<CFAEdge> iterator() {
        return CFAUtils.allLeavingEdges(pNode)
            .filter(edge -> functionNames.contains(edge.getSuccessor().getFunctionName()))
            .iterator();
      }
    };
  }

  @Override
  public EndpointPair<CFANode> incidentNodes(CFAEdge pEdge) {
    return EndpointPair.ordered(pEdge.getPredecessor(), pEdge.getSuccessor());
  }
}
