/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.blocking;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

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

    assertEquals(2, funct.getNumOfActiveNodes());
    assertEquals(2, funct.getAllActiveNodes().size());
    assertEquals(1, funct.getLeavingEdges(n1).size());
    assertEquals(1, funct.getNumEnteringEdges(n3));
    assertEquals(0, funct.getLeavingEdges(n3).size());
  }

  @Test
  public void testUltimate() {
    ReducedNode entryNode = new ReducedNode(new CFANode("test"), false);
    ReducedNode exitNode = new ReducedNode(new CFANode( "test"), false);

    ReducedNode n4 = new ReducedNode(new CFANode("loophead"), false);
    ReducedNode n5 = new ReducedNode(new CFANode("test"), false);
    ReducedNode n6 = new ReducedNode(new CFANode("test"), false);
    ReducedNode n7 = new ReducedNode(new CFANode("test"), false);
    ReducedNode n8 = new ReducedNode(new CFANode("test"), false);
    ReducedNode n9 = new ReducedNode(new CFANode("test"), false);
    ReducedNode n20 = new ReducedNode(new CFANode("test"), false);
    ReducedNode n21 = new ReducedNode(new CFANode("test"), false);
    ReducedNode n22 = new ReducedNode(new CFANode("test"), false);
    ReducedNode n23 = new ReducedNode(new CFANode("test"), false);
    ReducedNode n24 = new ReducedNode(new CFANode("test"), false);
    ReducedNode n25 = new ReducedNode(new CFANode("test"), false);

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

    assertEquals(funct.getEntryNode(), entryNode);
    assertEquals(funct.getExitNode(), exitNode);

    assertEquals(funct.getNumLeavingEdges(n1), 1);
    assertEquals(funct.getNumLeavingEdges(n2), 1);
    assertEquals(funct.getNumLeavingEdges(n3), 1);
    assertEquals(funct.getNumLeavingEdges(n5), 0);

    assertEquals(funct.getLeavingEdges(n1).size(), 1);
    assertEquals(funct.getLeavingEdges(n5).size(), 0);
  }

  @Test
  public void test_MultipleEdgesBetweenTwoNodes() {
    ReducedNode entryNode = new ReducedNode(null, false);
    ReducedNode exitNode = new ReducedNode(null, false);

    ReducedFunctionUnderTest funct = new ReducedFunctionUnderTest(entryNode, exitNode);

    funct.addEdge(entryNode, exitNode);
    funct.addEdge(entryNode, exitNode);

    assertEquals(2, funct.getNumLeavingEdges(entryNode));
    assertEquals(2, funct.getNumEnteringEdges(exitNode));
  }

}
