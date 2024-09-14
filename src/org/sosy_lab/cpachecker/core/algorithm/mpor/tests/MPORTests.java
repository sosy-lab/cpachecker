// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.tests;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.DirectedGraph;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqToken;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.CSimpleDeclarationSubstitution;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadNode;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

@SuppressWarnings("unused")
@SuppressFBWarnings({"UUF_UNUSED_FIELD", "URF_UNREAD_FIELD"})
public class MPORTests {

  MPORAlgorithm algorithm;

  public MPORTests(MPORAlgorithm pAlgorithm) {
    algorithm = pAlgorithm;
  }

  public static void testCommutativity(
      LogManager pLogManager,
      PredicateTransferRelation pPtr,
      PredicateAbstractState pAbstractState,
      ImmutableSet<CFAEdge> pGlobalAccesses)
      throws CPATransferException, InterruptedException {

    for (CFAEdge edgeA : pGlobalAccesses) {
      for (CFAEdge edgeB : pGlobalAccesses) {
        if (!edgeA.equals(edgeB)) {
          if (MPORUtil.doEdgesCommute(pPtr, pAbstractState, edgeA, edgeB)) {
            // pLogManager.log(
            // Level.INFO, "TRUE commute - " + edgeA.getCode() + " - " + edgeB.getCode());
          } else {
            // pLogManager.log(
            // Level.INFO, "FALSE commute - " + edgeA.getCode() + " - " + edgeB.getCode());
          }
        }
      }
    }
  }

  public static void testDirectedGraphSccs() {
    DirectedGraph<Integer> directedGraph = new DirectedGraph<>();
    directedGraph.addNode(0);
    directedGraph.addNode(1);
    directedGraph.addNode(2);
    directedGraph.addNode(3);
    directedGraph.addNode(4);
    directedGraph.addEdge(0, 1);
    directedGraph.addEdge(0, 2);
    directedGraph.addEdge(1, 2);
    directedGraph.addEdge(2, 3);
    directedGraph.addEdge(3, 4);
    directedGraph.addEdge(4, 3);
    ImmutableSet<ImmutableSet<Integer>> sccs = directedGraph.computeSCCs();
    ImmutableSet<Integer> maximalScc = sccs.iterator().next();
    assert (maximalScc.contains(3) && maximalScc.contains(4));
  }

  public static void testDirectedGraphCycles() {
    DirectedGraph<Integer> directedGraphA = new DirectedGraph<>();
    directedGraphA.addNode(0);
    directedGraphA.addNode(1);
    directedGraphA.addNode(2);
    directedGraphA.addEdge(0, 1);
    directedGraphA.addEdge(1, 2);
    directedGraphA.addEdge(2, 0);
    assert directedGraphA.containsCycle();

    DirectedGraph<Integer> directedGraphB = new DirectedGraph<>();
    directedGraphB.addNode(0);
    directedGraphB.addNode(1);
    directedGraphB.addNode(2);
    directedGraphB.addNode(3);
    directedGraphB.addNode(4);
    directedGraphB.addEdge(0, 1);
    directedGraphB.addEdge(0, 2);
    directedGraphB.addEdge(1, 2);
    directedGraphB.addEdge(2, 3);
    directedGraphB.addEdge(2, 4);
    directedGraphB.addEdge(4, 0);
    assert directedGraphB.containsCycle();

    DirectedGraph<Integer> directedGraphC = new DirectedGraph<>();
    directedGraphC.addNode(0);
    directedGraphC.addNode(1);
    directedGraphC.addNode(2);
    directedGraphC.addNode(3);
    directedGraphC.addNode(4);
    directedGraphC.addEdge(0, 1);
    directedGraphC.addEdge(0, 2);
    directedGraphC.addEdge(1, 2);
    directedGraphC.addEdge(2, 3);
    directedGraphC.addEdge(2, 4);
    assert !directedGraphC.containsCycle();
  }

  public static String generateProgram(
      ImmutableMap<MPORThread, CSimpleDeclarationSubstitution> pDecSubstitutions) {
    StringBuilder rProgram = new StringBuilder();
    rProgram.append(SeqUtil.createDeclarations(pDecSubstitutions));
    for (var entry : pDecSubstitutions.entrySet()) {
      MPORThread thread = entry.getKey();
      CSimpleDeclarationSubstitution substitution = entry.getValue();
      ImmutableMap<ThreadEdge, CFAEdge> edgeSubs = substitution.substituteEdges(thread);
      rProgram.append(SeqSyntax.NEWLINE);
      rProgram.append("=============== thread ").append(thread.id).append(" ===============");
      rProgram.append(SeqSyntax.NEWLINE).append(SeqSyntax.NEWLINE);
      for (ThreadNode threadNode : thread.cfa.threadNodes) {
        rProgram.append(SeqToken.CASE).append(SeqSyntax.SPACE);
        rProgram.append(threadNode.pc).append(SeqSyntax.COLON).append(SeqSyntax.SPACE);
        rProgram.append(SeqUtil.createCodeFromThreadNode(threadNode, edgeSubs));
        rProgram.append(SeqSyntax.NEWLINE);
      }
    }
    return rProgram.toString();
  }
}
