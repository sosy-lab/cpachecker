/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.blocking.test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.util.blocking.container.ReducedEdge;
import org.sosy_lab.cpachecker.util.blocking.container.ReducedFunction;
import org.sosy_lab.cpachecker.util.blocking.container.ReducedNode;

@SuppressWarnings("unused")
public class ReducedFunctionTest {

  private static class ReducedFunctionUnderTest extends ReducedFunction {
    public ReducedFunctionUnderTest(ReducedNode pEntryNode, ReducedNode pExitNode) {
      super(pEntryNode, pExitNode);
    }
  }

  @Test
  public void testAddAndRemoveEdges() {
    ReducedNode entryNode = new ReducedNode(null);
    ReducedNode exitNode = new ReducedNode(null);

    ReducedNode n1 = new ReducedNode(null);
    ReducedNode n2 = new ReducedNode(null);
    ReducedNode n3 = new ReducedNode(null);

    ReducedFunctionUnderTest funct = new ReducedFunctionUnderTest(entryNode, exitNode);

    ReducedEdge e1 = funct.addEdge(n1, n2);
    ReducedEdge e2 = funct.addEdge(n2, n3);

    funct.removeEdge(n1, n2, e1);
    funct.removeEdge(n2, n3, e2);

    assertEquals(2, funct.getNumOfActiveNodes());
    assertEquals(2, funct.getAllActiveNodes().size());
    assertEquals(1, funct.getLeavingEdges(n1).length);
    assertEquals(1, funct.getNumEnteringEdges(n3));
    assertEquals(0, funct.getLeavingEdges(n3).length);
  }

  @Test
  public void testUltimate() {
    ReducedNode entryNode = new ReducedNode(new CFANode(0, "test"));
    ReducedNode exitNode = new ReducedNode(new CFANode(100, "test"));

    ReducedNode n4 = new ReducedNode(new CFANode(4, "loophead"));
    ReducedNode n5 = new ReducedNode(new CFANode(5, "test"));
    ReducedNode n6 = new ReducedNode(new CFANode(6, "test"));
    ReducedNode n7 = new ReducedNode(new CFANode(7, "test"));
    ReducedNode n8 = new ReducedNode(new CFANode(8, "test"));
    ReducedNode n9 = new ReducedNode(new CFANode(9, "test"));
    ReducedNode n20 = new ReducedNode(new CFANode(20, "test"));
    ReducedNode n21 = new ReducedNode(new CFANode(21, "test"));
    ReducedNode n22 = new ReducedNode(new CFANode(22, "test"));
    ReducedNode n23 = new ReducedNode(new CFANode(23, "test"));
    ReducedNode n24 = new ReducedNode(new CFANode(24, "test"));
    ReducedNode n25 = new ReducedNode(new CFANode(25, "test"));

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
    ReducedNode entryNode = new ReducedNode(null);
    ReducedNode exitNode = new ReducedNode(null);

    ReducedNode n1 = new ReducedNode(null);
    ReducedNode n2 = new ReducedNode(null);
    ReducedNode n3 = new ReducedNode(null);
    ReducedNode n4 = new ReducedNode(null);
    ReducedNode n5 = new ReducedNode(null);

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

    assertEquals(funct.getLeavingEdges(n1).length, 1);
    assertEquals(funct.getLeavingEdges(n5).length, 0);
  }

  @Test
  public void test_MultipleEdgesBetweenTwoNodes() {
    ReducedNode entryNode = new ReducedNode(null);
    ReducedNode exitNode = new ReducedNode(null);

    ReducedFunctionUnderTest funct = new ReducedFunctionUnderTest(entryNode, exitNode);

    funct.addEdge(entryNode, exitNode);
    funct.addEdge(entryNode, exitNode);

    assertEquals(2, funct.getNumLeavingEdges(entryNode));
    assertEquals(2, funct.getNumEnteringEdges(exitNode));
  }

}
