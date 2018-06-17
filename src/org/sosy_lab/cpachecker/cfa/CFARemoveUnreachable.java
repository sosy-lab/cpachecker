/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cfa;

import static org.sosy_lab.cpachecker.cfa.CFACreationUtils.getLeavingEdges;
import static org.sosy_lab.cpachecker.cfa.CFACreationUtils.hasLeavingJumpExitEdge;

import de.uni_freiburg.informatik.ultimate.smtinterpol.util.ArrayQueue;
import java.util.Collection;
import java.util.HashSet;
import java.util.Queue;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.JumpExitEdge;

/** Remove edges and nodes that are unreachable because of a {@link JumpExitEdge}. */
public class CFARemoveUnreachable {
  protected MutableCFA cfa;

  public static void removeFrom(final MutableCFA pCfa) {
    new CFARemoveUnreachable(pCfa).traverse();
  }

  private CFARemoveUnreachable(final MutableCFA pCfa) {
    cfa = pCfa;
  }

  private void traverse() {
    for (String functionName : cfa.getAllFunctionNames()) {
      traverse(cfa.getFunctionHead(functionName));
    }
  }

  private void traverse(@Nonnull final CFANode pNode) {
    final HashSet<CFANode> visited = new HashSet<>();
    final Queue<CFANode> nodesToVisit = new ArrayQueue<>();
    nodesToVisit.add(pNode);
    while (!nodesToVisit.isEmpty()) {
      final CFANode current = nodesToVisit.remove();
      if (visited.contains(current)) {
        continue;
      }
      visited.add(current);
      if (hasUnreachableLeavingEdges(current)) {
        removeUnreachableLeavingEdges(current);
      }
      for (final CFAEdge leavingEdge : getLeavingEdges(current)) {
        nodesToVisit.add(leavingEdge.getSuccessor());
      }
    }
  }

  /**
   * Remove edges that are never taken and their successors that are only reachable by those edges.
   *
   * @param pNode The node from which unreachable edges and successors should be removed.
   */
  private void removeUnreachableLeavingEdges(final @Nonnull CFANode pNode) {
    final Queue<CFAEdge> unreachableLeavingEdges = new ArrayQueue<>();
    unreachableLeavingEdges.addAll(getUnreachableLeavingEdges(pNode));
    while (!unreachableLeavingEdges.isEmpty()) {
      final CFAEdge current = unreachableLeavingEdges.remove();
      CFACreationUtils.removeEdgeFromNodes(current);
      if (current.getSuccessor().getNumEnteringEdges() == 0) {
        cfa.removeNode(current.getSuccessor());
        unreachableLeavingEdges.addAll(getLeavingEdges(current.getSuccessor()));
      }
    }
  }

  private static boolean hasUnreachableLeavingEdges(final CFANode pNode) {
    return isFunctionExitNode(pNode) || hasLeavingJumpExitEdge(pNode);
  }

  private static boolean isFunctionExitNode(final CFANode pNode) {
    return pNode instanceof FunctionExitNode;
  }

  @Nonnull
  private static Collection<CFAEdge> getUnreachableLeavingEdges(final @Nonnull CFANode pNode) {
    return getLeavingEdges(pNode)
        .stream()
        .filter(leavingEdge -> !(leavingEdge instanceof JumpExitEdge))
        .collect(Collectors.toList());
  }
}
