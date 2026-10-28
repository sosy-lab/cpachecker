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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.common.graph.Traverser;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.ast.AstCfaRelation;

/** Traverses the ARG from the given root node to find all relevant ARG states for Witnesses */
public class RootExplorationArgStateCollector implements RelevantArgStatesCollector {

  private final ImmutableSet<CFANode> loopInvariantNodes;
  private final @Nullable AstCfaRelation astCfaRelation;

  /**
   * @param pLoopInvariantNodes the nodes at which loop invariants are collected, e.g., the heads of
   *     the loops of the {@link org.sosy_lab.cpachecker.util.LoopStructure}
   */
  public RootExplorationArgStateCollector(Set<CFANode> pLoopInvariantNodes) {
    this(pLoopInvariantNodes, null);
  }

  /**
   * @param pLoopInvariantNodes the nodes at which loop invariants are collected, e.g., the heads of
   *     the loops of the {@link org.sosy_lab.cpachecker.util.LoopStructure}
   * @param pAstCfaRelation what the nodes of the CFA stand for in the program, or null if that is
   *     unknown. A post-processing may have copied a node, for example when it unrolled a loop, and
   *     the states of all copies of a node belong to the one node that they stand for: an invariant
   *     there has to hold whenever the program is there, not only in one of the copies.
   */
  public RootExplorationArgStateCollector(
      Set<CFANode> pLoopInvariantNodes, @Nullable AstCfaRelation pAstCfaRelation) {
    loopInvariantNodes = ImmutableSet.copyOf(pLoopInvariantNodes);
    astCfaRelation = pAstCfaRelation;
  }

  /** The node of the program that the given node stands for. */
  private CFANode programNodeOf(CFANode pNode) {
    return astCfaRelation == null ? pNode : astCfaRelation.programNodeOf(pNode);
  }

  /**
   * The node of the program that the given node stands for, which is of the same kind: a copy is
   * built as the kind of node it is a copy of.
   */
  private <T extends CFANode> T programNodeOf(T pNode, Class<T> pKind) {
    CFANode programNode = programNodeOf(pNode);
    Verify.verify(
        pKind.isInstance(programNode),
        "%s is a copy of %s, which is not a %s",
        pNode,
        programNode,
        pKind.getSimpleName());
    return pKind.cast(programNode);
  }

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
          // Translate back to the node before all postprocessing to decide what to do with this
          CFANode programNode = programNodeOf(node);
          FluentIterable<CFAEdge> leavingEdges = node.getLeavingEdges();
          if (loopInvariantNodes.contains(programNode)) {
            collectedStates.loopInvariants().put(programNode, state);
          } else if (leavingEdges.size() == 1
              && leavingEdges.anyMatch(FunctionCallEdge.class::isInstance)) {
            collectedStates.functionCallInvariants().put(programNode, state);
          } else if (node instanceof FunctionEntryNode functionEntryNode) {
            functionEntryStatesCallStack.put(functionEntryNode.getFunctionDefinition(), state);
            collectedStates
                .functionContractRequires()
                .put(programNodeOf(functionEntryNode, FunctionEntryNode.class), state);
          } else if (node instanceof FunctionExitNode functionExitNode) {
            List<ARGState> functionEntryNodes =
                functionEntryStatesCallStack.get(node.getFunction());
            Verify.verify(!functionEntryNodes.isEmpty());
            collectedStates
                .functionContractEnsures()
                .put(
                    programNodeOf(functionExitNode, FunctionExitNode.class),
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
