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
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.util.CFAUtils;

/** A {@link CfaNetwork} representing a single function specified by its function entry node. */
final class SingleFunctionCfaNetwork extends AbstractCfaNetwork {

  private final FunctionEntryNode functionEntryNode;

  private SingleFunctionCfaNetwork(FunctionEntryNode pFunctionEntryNode) {
    functionEntryNode = pFunctionEntryNode;
  }

  static CfaNetwork forFunction(FunctionEntryNode pFunctionEntryNode) {
    return new SingleFunctionCfaNetwork(checkNotNull(pFunctionEntryNode));
  }

  private boolean isSuperEdge(CFAEdge pEdge) {
    return pEdge instanceof FunctionCallEdge || pEdge instanceof FunctionReturnEdge;
  }

  @Override
  public Set<CFANode> nodes() {
    return new UnmodifiableSetView<>() {

      @Override
      public Iterator<CFANode> iterator() {
        return new AbstractIterator<>() {

          private final Set<CFANode> waitlisted = new HashSet<>(ImmutableSet.of(functionEntryNode));
          private final Deque<CFANode> waitlist = new ArrayDeque<>(waitlisted);

          @Override
          protected @Nullable CFANode computeNext() {
            while (!waitlist.isEmpty()) {

              CFANode node = waitlist.remove();

              for (CFAEdge outEdge : outEdges(node)) {
                CFANode successor = successor(outEdge);
                if (waitlisted.add(successor)) {
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
        return CFAUtils.allEnteringEdges(pNode).filter(edge -> !isSuperEdge(edge)).iterator();
      }
    };
  }

  @Override
  public Set<CFAEdge> outEdges(CFANode pNode) {
    return new UnmodifiableSetView<>() {

      @Override
      public Iterator<CFAEdge> iterator() {
        return CFAUtils.allLeavingEdges(pNode).filter(edge -> !isSuperEdge(edge)).iterator();
      }
    };
  }

  @Override
  public EndpointPair<CFANode> incidentNodes(CFAEdge pEdge) {
    return EndpointPair.ordered(pEdge.getPredecessor(), pEdge.getSuccessor());
  }
}
