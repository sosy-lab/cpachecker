// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.graph;

import static com.google.common.base.Preconditions.checkNotNull;

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
import org.sosy_lab.cpachecker.util.PrepareNextIterator;
import org.sosy_lab.cpachecker.util.UnmodifiableSetView;

final class FunctionFilteringCfaNetwork extends AbstractCfaNetwork {

  private final CFA cfa;
  private final ImmutableSet<String> functions;

  FunctionFilteringCfaNetwork(CFA pCfa, Set<String> pFunctions) {

    cfa = checkNotNull(pCfa);
    functions = ImmutableSet.copyOf(pFunctions);
  }

  @Override
  public Set<CFAEdge> inEdges(CFANode pNode) {
    return new UnmodifiableSetView<>() {

      @Override
      public Iterator<CFAEdge> iterator() {
        return CFAUtils.allEnteringEdges(pNode)
            .filter(edge -> functions.contains(edge.getPredecessor().getFunctionName()))
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
            .filter(edge -> functions.contains(edge.getSuccessor().getFunctionName()))
            .iterator();
      }
    };
  }

  @Override
  public EndpointPair<CFANode> incidentNodes(CFAEdge pEdge) {
    return EndpointPair.ordered(pEdge.getPredecessor(), pEdge.getSuccessor());
  }

  @Override
  public Set<CFANode> nodes() {
    return new UnmodifiableSetView<>() {

      @Override
      public Iterator<CFANode> iterator() {
        return new PrepareNextIterator<>() {

          private final Set<CFANode> waitlisted =
              functions.stream()
                  .map(function -> cfa.getAllFunctions().get(function))
                  .filter(Objects::nonNull)
                  .collect(Collectors.toCollection(HashSet::new));
          private final Deque<CFANode> waitlist = new ArrayDeque<>(waitlisted);

          @Override
          protected @Nullable CFANode prepareNext() {

            while (!waitlist.isEmpty()) {

              CFANode node = waitlist.remove();

              for (CFANode successor : CFAUtils.allSuccessorsOf(node)) {
                if (functions.contains(successor.getFunctionName()) && waitlisted.add(successor)) {
                  waitlist.add(successor);
                }
              }

              return node;
            }

            return null;
          }
        };
      }
    };
  }
}
