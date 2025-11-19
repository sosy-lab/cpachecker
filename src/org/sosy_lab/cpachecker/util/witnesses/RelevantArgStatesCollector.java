// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.witnesses;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Verify;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
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

public class RelevantArgStatesCollector {

  /**
   * A class to keep track of parent child relations between abstract states which enter a function
   * and those which exit it
   */
  public record FunctionEntryExitPair(ARGState entry, ARGState exit) {}

  /** A data structure for collecting the relevant information for a witness from an ARG */
  public record CollectedARGStates(
      Multimap<CFANode, ARGState> loopInvariants,
      Multimap<CFANode, ARGState> functionCallInvariants,
      Multimap<FunctionEntryNode, ARGState> functionContractRequires,
      Multimap<FunctionExitNode, FunctionEntryExitPair> functionContractEnsures) {

    public CollectedARGStates {
      checkNotNull(loopInvariants);
      checkNotNull(functionCallInvariants);
      checkNotNull(functionContractRequires);
      checkNotNull(functionContractEnsures);
    }

    public CollectedARGStates immutableCopy() {
      return new CollectedARGStates(
          ImmutableListMultimap.copyOf(loopInvariants),
          ImmutableListMultimap.copyOf(functionCallInvariants),
          ImmutableListMultimap.copyOf(functionContractRequires),
          ImmutableListMultimap.copyOf(functionContractEnsures));
    }
  }

  /**
   * Collect the relevant states from the ARG starting at the given root state for the export of a
   * witness
   *
   * @param pRootState the state for where the traversal of the ARG should start for the collection
   *     of the information
   * @return the collected information about the ARG
   */
  public static CollectedARGStates getRelevantStates(ARGState pRootState) {
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
          List<ARGState> functionEntryNodes = functionEntryStatesCallStack.get(node.getFunction());
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
