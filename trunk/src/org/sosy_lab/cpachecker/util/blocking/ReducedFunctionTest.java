// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.blocking;

import static com.google.common.truth.Truth.assertThat;
import static org.sosy_lab.cpachecker.cfa.model.CFANode.newDummyCFANode;

import org.junit.Test;

@SuppressWarnings("unused")
public class ReducedFunctionTest {

  private static class ReducedFunctionUnderTest extends ReducedFunction {
    public ReducedFunctionUnderTest(ReducedNode pEntryNode, ReducedNode pExitNode) {
      super(pEntryNode, pExitNode);
    }
  }

  @Test
  public void testAddAndRemoveEdges() {
    ReducedNode entryNode = new ReducedNode(null, false);
    ReducedNode exitNode = new ReducedNode(null, false);

    ReducedNode n1 = new ReducedNode(null, false);
    ReducedNode n2 = new ReducedNode(null, false);
    ReducedNode n3 = new ReducedNode(null, false);

    ReducedFunctionUnderTest funct = new ReducedFunctionUnderTest(entryNode, exitNode);

    ReducedEdge e1 = funct.addEdge(n1, n2);
    ReducedEdge e2 = funct.addEdge(n2, n3);
    funct.addEdge(n1, n3);

    funct.removeEdge(n1, n2, e1);
    funct.removeEdge(n2, n3, e2);

    assertThat(funct.getNumOfActiveNodes()).isEqualTo(2);
    assertThat(funct.getAllActiveNodes()).hasSize(2);
    assertThat(funct.getLeavingEdges(n1)).hasSize(1);
    assertThat(funct.getNumEnteringEdges(n3)).isEqualTo(1);
    assertThat(funct.getLeavingEdges(n3)).hasSize(0);
  }

  @Test
  public void testUltimate() {
    ReducedNode entryNode = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode exitNode = new ReducedNode(newDummyCFANode("test"), false);

    ReducedNode n4 = new ReducedNode(newDummyCFANode("loophead"), false);
    ReducedNode n5 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n6 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n7 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n8 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n9 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n20 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n21 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n22 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n23 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n24 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n25 = new ReducedNode(newDummyCFANode("test"), false);

    ReducedFunction funct = new ReducedFunction(entryNode, exitNode);

    funct.addEdge(entryNode, n4);

    funct.addEdge(n4, n8);
    funct.addEdge(n4, n5);
    funct.addEdge(n5, n6);
    funct.addEdge(n6, n7);
    funct.addEdge(n7, n4);

    funct.addEdge(n8, n9);
    funct.addEdge(n9, n20);
    funct.addEdge(n20, n21);
    funct.addEdge(n21, n22);
    funct.addEdge(n22, n23);
    funct.addEdge(n23, n24);
    funct.addEdge(n24, n25);
    funct.addEdge(n25, exitNode);
  }

  @Test
  public void test() {
    ReducedNode entryNode = new ReducedNode(null, false);
    ReducedNode exitNode = new ReducedNode(null, false);

    ReducedNode n1 = new ReducedNode(null, false);
    ReducedNode n2 = new ReducedNode(null, false);
    ReducedNode n3 = new ReducedNode(null, false);
    ReducedNode n4 = new ReducedNode(null, false);
    ReducedNode n5 = new ReducedNode(null, false);

    ReducedFunctionUnderTest funct = new ReducedFunctionUnderTest(entryNode, exitNode);

    funct.addEdge(n1, n2);
    funct.addEdge(n2, n3);
    funct.addEdge(n3, n4);
    funct.addEdge(n4, n5);

    assertThat(funct.getEntryNode()).isEqualTo(entryNode);
    assertThat(funct.getExitNode()).isEqualTo(exitNode);

    assertThat(funct.getNumLeavingEdges(n1)).isEqualTo(1);
    assertThat(funct.getNumLeavingEdges(n2)).isEqualTo(1);
    assertThat(funct.getNumLeavingEdges(n3)).isEqualTo(1);
    assertThat(funct.getNumLeavingEdges(n5)).isEqualTo(0);

    assertThat(funct.getLeavingEdges(n1)).hasSize(1);
    assertThat(funct.getLeavingEdges(n5)).hasSize(0);
  }

  @Test
  public void test_MultipleEdgesBetweenTwoNodes() {
    ReducedNode entryNode = new ReducedNode(null, false);
    ReducedNode exitNode = new ReducedNode(null, false);

    ReducedFunctionUnderTest funct = new ReducedFunctionUnderTest(entryNode, exitNode);

    funct.addEdge(entryNode, exitNode);
    funct.addEdge(entryNode, exitNode);

    assertThat(funct.getNumLeavingEdges(entryNode)).isEqualTo(2);
    assertThat(funct.getNumEnteringEdges(exitNode)).isEqualTo(2);
  }
}
