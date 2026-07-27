// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.witnesses;

import com.google.common.base.Verify;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.graph.Traverser;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class ReachedSetArgStateCollector implements RelevantArgStatesCollector {

  @Override
  public CollectedARGStates getRelevantStates(ARGState pRootState) {
    {
      final CollectedARGStates collectedStates =
          new CollectedARGStates(
              HashMultimap.create(),
              HashMultimap.create(),
              HashMultimap.create(),
              HashMultimap.create());

      ListMultimap<AFunctionDeclaration, ARGState> functionEntryStatesCallStack =
          ArrayListMultimap.create();

      final Map<ARGState, ListMultimap<AFunctionDeclaration, ARGState>> callStackRecovery =
          new HashMap<>();

      // We need a depth first traversal, since we need to visit function entries before function
      // exits to maintain a correct call stack
      for (ARGState state :
          Traverser.forGraph(ARGState::getChildren).depthFirstPreOrder(pRootState)) {
        if (!state.getParents().isEmpty()) {
          ARGState parent = state.getParents().getFirst();
          if (callStackRecovery.containsKey(parent)) {
            // Copy the saved callstack, since we want to return to the state we had before the
            // branching
            functionEntryStatesCallStack = ArrayListMultimap.create(callStackRecovery.get(parent));
          }
        }

        for (LocationState locationState :
            AbstractStates.asIterable(state).filter(LocationState.class)) {
          CFANode node = locationState.getLocationNode();
          FluentIterable<CFAEdge> leavingEdges = node.getLeavingEdges();
          if (node.isLoopStart()) {
            collectedStates.loopInvariants().put(node, state);
          } else if (leavingEdges.size() == 1
              && leavingEdges.anyMatch(FunctionCallEdge.class::isInstance)) {
            collectedStates.functionCallInvariants().put(node, state);
          } else if (node instanceof FunctionEntryNode functionEntryNode) {
            functionEntryStatesCallStack.put(functionEntryNode.getFunctionDefinition(), state);
            collectedStates.functionContractRequires().put(functionEntryNode, state);
          } else if (node instanceof FunctionExitNode functionExitNode) {
            List<ARGState> functionEntryNodes =
                functionEntryStatesCallStack.get(node.getFunction());
            Verify.verify(!functionEntryNodes.isEmpty());
            collectedStates
                .functionContractEnsures()
                .put(
                    functionExitNode,
                    new FunctionEntryExitPair(functionEntryNodes.removeLast(), state));
          }

          if (state.getChildren().size() > 1 && !callStackRecovery.containsKey(state)) {
            callStackRecovery.put(state, ArrayListMultimap.create(functionEntryStatesCallStack));
          }
        }
      }

      return collectedStates.immutableCopy();
    }
  }
}
