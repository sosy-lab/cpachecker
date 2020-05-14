/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.cpa.arg.witnessexport.formatter;

import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import java.io.IOException;
import java.util.Deque;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.Edge;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.Witness;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;

public abstract class WitnessToOutputFormatter<T> {

  protected final Witness witness;

  protected WitnessToOutputFormatter(Witness pWitness) {
    witness = pWitness;
  }

  /**
   * Appends the formatted witness to the supplied {@link Appendable}.
   *
   * <p>The runtime of this method mainly depends on the size of the witness graph.
   *
   * <p>Calling this method several times should provide an identical output.
   *
   * <p>We do not assume any thread-safety.
   */
  public void appendTo(Appendable pTarget) throws IOException {
    initialize(pTarget);
    traverseGraph(pTarget);
    finish(pTarget);
  }

  private void traverseGraph(Appendable pTarget) throws IOException {
    String entryStateNodeId = witness.getEntryStateNodeId();
    Map<String, T> nodes = Maps.newHashMap();
    Deque<String> waitlist = Queues.newArrayDeque();
    waitlist.push(entryStateNodeId);
    T entryNode = createNewNode(entryStateNodeId, pTarget);
    addAndGetInvariantsData(entryNode, entryStateNodeId, pTarget);
    nodes.put(entryStateNodeId, entryNode);
    while (!waitlist.isEmpty()) {
      String source = waitlist.pop();
      for (Edge edge : witness.getLeavingEdges().get(source)) {
        T targetNode = nodes.get(edge.getTarget());
        if (targetNode == null) {
          targetNode = createNewNode(edge.getTarget(), pTarget);
          ExpressionTree<Object> invariant =
              addAndGetInvariantsData(targetNode, edge.getTarget(), pTarget);
          if (!ExpressionTrees.getFalse().equals(invariant)) {
            waitlist.push(edge.getTarget());
          }
          nodes.put(edge.getTarget(), targetNode);
        }
        createNewEdge(edge, nodes.get(source), targetNode, pTarget);
      }
    }
  }

  private ExpressionTree<Object> addAndGetInvariantsData(T t, String pStateId, Appendable pTarget) {
    if (!witness.getInvariantExportStates().contains(pStateId)) {
      return ExpressionTrees.getTrue();
    }
    ExpressionTree<Object> tree = witness.getStateInvariant(pStateId);
    if (!tree.equals(ExpressionTrees.getTrue())) {
      addInvariantsData(t, tree, witness.getStateScopes().get(pStateId), pTarget);
    }
    return tree;
  }

  /** This method is called BEFORE traversing the witness graph. */
  protected abstract void initialize(Appendable pTarget) throws IOException;

  /** This method is called AFTER traversing the witness graph. */
  protected abstract void finish(Appendable pTarget) throws IOException;

  /** @return a unique identifier for the new node. */
  protected abstract T createNewNode(String pNodeId, Appendable pTarget) throws IOException;

  protected abstract void createNewEdge(
      Edge pEdge, T pSourceNode, T pTargetNode, Appendable pTarget) throws IOException;

  protected abstract void addInvariantsData(
      T pNodeId, ExpressionTree<Object> pTree, @Nullable String pScope, Appendable pTarget);
}
