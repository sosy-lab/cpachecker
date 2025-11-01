// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.witnesses;

import com.google.common.base.Verify;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.graph.SuccessorsFunction;
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

public class ArgAnalysisUtils {

  /**
   * A class to keep track of parent child relations between abstract states which enter a function
   * and those which exit it
   */
  public record FunctionEntryExitPair(ARGState entry, ARGState exit) {}

  /** A data structure for collecting the relevant information for a witness from an ARG */
  public static class CollectedARGStates {
    public Multimap<CFANode, ARGState> loopInvariants = HashMultimap.create();
    public Multimap<CFANode, ARGState> functionCallInvariants = HashMultimap.create();
    public Multimap<FunctionEntryNode, ARGState> functionContractRequires = HashMultimap.create();
    public Multimap<FunctionExitNode, FunctionEntryExitPair> functionContractEnsures =
        HashMultimap.create();
  }

  /**
   * Analyzes the ARG during its traversal by collecting the states relevant to exporting a witness
   */
  private static class RelevantARGStateCollector {

    private final CollectedARGStates collectedStates = new CollectedARGStates();

    // TODO: This needs to be improved once we implement setjump/longjump
    /** The callstack of the order in which the function entry points where traversed */
    private ListMultimap<AFunctionDeclaration, ARGState> functionEntryStatesCallStack =
        ArrayListMultimap.create();

    /** Enables the recovery of the callstack when an ARGState has multiple children */
    private final Map<ARGState, ListMultimap<AFunctionDeclaration, ARGState>> callStackRecovery =
        new HashMap<>();

    void analyze(ARGState pSuccessor) {
      if (!pSuccessor.getParents().isEmpty()) {
        ARGState parent = pSuccessor.getParents().getFirst();
        if (callStackRecovery.containsKey(parent)) {
          // Copy the saved callstack, since we want to return to the state we had before the
          // branching
          functionEntryStatesCallStack = ArrayListMultimap.create(callStackRecovery.get(parent));
        }
      }

      for (LocationState state :
          AbstractStates.asIterable(pSuccessor).filter(LocationState.class)) {
        CFANode node = state.getLocationNode();
        FluentIterable<CFAEdge> leavingEdges = node.getLeavingEdges();
        if (node.isLoopStart()) {
          collectedStates.loopInvariants.put(node, pSuccessor);
        } else if (leavingEdges.size() == 1
            && leavingEdges.anyMatch(FunctionCallEdge.class::isInstance)) {
          collectedStates.functionCallInvariants.put(node, pSuccessor);
        } else if (node instanceof FunctionEntryNode functionEntryNode) {
          functionEntryStatesCallStack.put(functionEntryNode.getFunctionDefinition(), pSuccessor);
          collectedStates.functionContractRequires.put(functionEntryNode, pSuccessor);
        } else if (node instanceof FunctionExitNode functionExitNode) {
          List<ARGState> functionEntryNodes = functionEntryStatesCallStack.get(node.getFunction());
          Verify.verify(!functionEntryNodes.isEmpty());
          collectedStates.functionContractEnsures.put(
              functionExitNode,
              new FunctionEntryExitPair(functionEntryNodes.removeLast(), pSuccessor));
        }

        if (pSuccessor.getChildren().size() > 1 && !callStackRecovery.containsKey(pSuccessor)) {
          callStackRecovery.put(pSuccessor, ArrayListMultimap.create(functionEntryStatesCallStack));
        }
      }
    }

    CollectedARGStates getCollectedStates() {
      return collectedStates;
    }
  }

  /** How to traverse the ARG */
  private static class ARGSuccessorFunction implements SuccessorsFunction<ARGState> {

    @Override
    public Iterable<ARGState> successors(ARGState node) {
      return node.getChildren();
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
    RelevantARGStateCollector statesCollector = new RelevantARGStateCollector();
    for (ARGState state :
        Traverser.forGraph(new ARGSuccessorFunction()).depthFirstPreOrder(pRootState)) {
      statesCollector.analyze(state);
    }

    return statesCollector.getCollectedStates();
  }
}
