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
package org.sosy_lab.cpachecker.util.blocking;

import static org.junit.Assert.assertEquals;

import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;


public class BlockedCFAReducerTest {

  public class ReducerUnderTest extends BlockedCFAReducer {

    public ReducerUnderTest(Configuration pConfig) throws InvalidConfigurationException {
      super(pConfig);
    }

  }

  @Before
  public void setUp() throws Exception {}

  @Test
  public void testApplySequenceRule_SimpleSequence() {
   try {
     ReducedNode entryNode = new ReducedNode(new CFANode(1, "test"));
     ReducedNode exitNode = new ReducedNode(new CFANode(100, "test"));
     ReducedFunction funct = new ReducedFunction(entryNode, exitNode);

     ReducedNode n1 = new ReducedNode(new CFANode(2, "test"));
     ReducedNode n2 = new ReducedNode(new CFANode(3, "test"));
     ReducedNode n3 = new ReducedNode(new CFANode(4, "test"));
     ReducedNode n4 = new ReducedNode(new CFANode(5, "test"));
     ReducedNode n5 = new ReducedNode(new CFANode(6, "test"));

     funct.addEdge(entryNode, n1);
     funct.addEdge(n1, n2);
     funct.addEdge(n2, n3);
     funct.addEdge(n3, n4);
     funct.addEdge(n4, n5);
     funct.addEdge(n5, exitNode);

     ReducerUnderTest reducer = new ReducerUnderTest(null);
     assertEquals(reducer.applySequenceRule(funct), true);
     assertEquals(2, funct.getNumOfActiveNodes());

    } catch (InvalidConfigurationException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testApplySequenceRule_ForLoop() {
   try {
     ReducedNode entryNode = new ReducedNode(new CFANode(1, "test"));
     ReducedNode exitNode = new ReducedNode(new CFANode(100, "test"));
     ReducedFunction funct = new ReducedFunction(entryNode, exitNode);

     ReducedNode n1 = new ReducedNode(new CFANode(2, "test"));
     ReducedNode n2 = new ReducedNode(new CFANode(3, "test"));
     ReducedNode n3 = new ReducedNode(new CFANode(4, "test"));
     ReducedNode n4 = new ReducedNode(new CFANode(5, "loophead"));
     n4.getWrapped().setLoopStart();

     ReducedNode n5 = new ReducedNode(new CFANode(6, "test"));
     ReducedNode n6 = new ReducedNode(new CFANode(7, "test"));
     ReducedNode n7 = new ReducedNode(new CFANode(8, "test"));

     ReducedNode n8 = new ReducedNode(new CFANode(9, "test"));
     ReducedNode n9 = new ReducedNode(new CFANode(10, "test"));


     funct.addEdge(entryNode, n1);
     funct.addEdge(n1, n2);
     funct.addEdge(n2, n3);
     funct.addEdge(n3, n4);

     funct.addEdge(n4, n8);
     funct.addEdge(n4, n5);
     funct.addEdge(n5, n6);
     funct.addEdge(n6, n7);
     funct.addEdge(n7, n4);

     funct.addEdge(n8, n9);
     funct.addEdge(n9, exitNode);

     ReducerUnderTest reducer = new ReducerUnderTest(null);
     while (reducer.applySequenceRule(funct)) {
       ;
     }

     assertEquals(3, funct.getNumOfActiveNodes());
     assertEquals(2, funct.getNumLeavingEdges(n4));

    } catch (InvalidConfigurationException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testAllRules_ForLoopWithSequence_reduce() {
   try {
     ReducedNode entryNode = new ReducedNode(new CFANode(0, "test"));
     ReducedNode exitNode = new ReducedNode(new CFANode(100, "test"));
     ReducedFunction funct = new ReducedFunction(entryNode, exitNode);

     ReducedNode n1 = new ReducedNode(new CFANode(1, "test"));
     ReducedNode n2 = new ReducedNode(new CFANode(2, "test"));
     ReducedNode n3 = new ReducedNode(new CFANode(3, "test"));
     ReducedNode n4 = new ReducedNode(new CFANode(4, "loophead"));
     n4.getWrapped().setLoopStart();

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
     ReducedNode n26 = new ReducedNode(new CFANode(26, "test"));

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

     ReducerUnderTest reducer = new ReducerUnderTest(null);

     boolean sequenceApplied, choiceApplied;
     do {
       System.out.println("----------------");
       Map<ReducedNode, Map<ReducedNode, Set<ReducedEdge>>> inlinedCfa = funct.getInlinedCfa();
       reducer.printInlinedCfa(inlinedCfa, System.out);

       sequenceApplied = reducer.applySequenceRule(funct);
     } while (sequenceApplied);


     assertEquals(3, funct.getNumOfActiveNodes());
     assertEquals(2, funct.getNumLeavingEdges(n4));

    } catch (InvalidConfigurationException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testAllRules_ForLoopWithSequence() {
   try {
     ReducedNode entryNode = new ReducedNode(new CFANode(0, "test"));
     ReducedNode exitNode = new ReducedNode(new CFANode(100, "test"));
     ReducedFunction funct = new ReducedFunction(entryNode, exitNode);

     ReducedNode n1 = new ReducedNode(new CFANode(1, "test"));
     ReducedNode n2 = new ReducedNode(new CFANode(2, "test"));
     ReducedNode n3 = new ReducedNode(new CFANode(3, "test"));
     ReducedNode n4 = new ReducedNode(new CFANode(4, "loophead"));
     n4.getWrapped().setLoopStart();

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
     ReducedNode n26 = new ReducedNode(new CFANode(26, "test"));

     funct.addEdge(entryNode, n1);
     funct.addEdge(n1, n2);
     funct.addEdge(n2, n3);
     funct.addEdge(n3, n4);

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
     funct.addEdge(n25, n26);
     funct.addEdge(n26, exitNode);

     ReducerUnderTest reducer = new ReducerUnderTest(null);

     boolean sequenceApplied, choiceApplied;
     do {
       System.out.println("----------------");
       Map<ReducedNode, Map<ReducedNode, Set<ReducedEdge>>> inlinedCfa = funct.getInlinedCfa();
       reducer.printInlinedCfa(inlinedCfa, System.out);

       sequenceApplied = reducer.applySequenceRule(funct);
       choiceApplied = reducer.applyChoiceRule(funct);
     } while (sequenceApplied || choiceApplied);


     assertEquals(3, funct.getNumOfActiveNodes());
     assertEquals(2, funct.getNumLeavingEdges(n4));

    } catch (InvalidConfigurationException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testAllRules_ForLoopWithSequence2() {
   try {
     ReducedNode entryNode = new ReducedNode(new CFANode(0, "test"));
     ReducedNode exitNode = new ReducedNode(new CFANode(100, "test"));
     ReducedNode n4 = new ReducedNode(new CFANode(4, "loophead"));
     ReducedNode n6 = new ReducedNode(new CFANode(6, "test"));
     n4.getWrapped().setLoopStart();

     ReducedFunction funct = new ReducedFunction(entryNode, exitNode);


     funct.addEdge(entryNode, n4);
     funct.addEdge(n4, n6);
     funct.addEdge(n6, n4);
     funct.addEdge(n4, exitNode);

     ReducerUnderTest reducer = new ReducerUnderTest(null);

     boolean sequenceApplied, choiceApplied;
     do {
       System.out.println("----------------");
       Map<ReducedNode, Map<ReducedNode, Set<ReducedEdge>>> inlinedCfa = funct.getInlinedCfa();
       reducer.printInlinedCfa(inlinedCfa, System.out);

       sequenceApplied = reducer.applySequenceRule(funct);
       choiceApplied = reducer.applyChoiceRule(funct);
     } while (sequenceApplied || choiceApplied);


     assertEquals(3, funct.getNumOfActiveNodes());
     assertEquals(2, funct.getNumLeavingEdges(n4));

    } catch (InvalidConfigurationException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testAllRules_ForLoopWithSequence3() {
   try {
     ReducedNode entryNode = new ReducedNode(new CFANode(0, "test"));
     ReducedNode exitNode = new ReducedNode(new CFANode(100, "test"));
     ReducedNode n4 = new ReducedNode(new CFANode(4, "loophead"));
     ReducedNode n6 = new ReducedNode(new CFANode(6, "test"));
     ReducedNode n10 = new ReducedNode(new CFANode(10, "test"));
     n4.getWrapped().setLoopStart();

     ReducedFunction funct = new ReducedFunction(entryNode, exitNode);


     funct.addEdge(entryNode, n4);
     funct.addEdge(n6, n4);
     funct.addEdge(n4, n6);
     funct.addEdge(n4, n10);
     funct.addEdge(n10, exitNode);

     ReducerUnderTest reducer = new ReducerUnderTest(null);

     boolean sequenceApplied, choiceApplied;
     do {
       System.out.println("----------------");
       Map<ReducedNode, Map<ReducedNode, Set<ReducedEdge>>> inlinedCfa = funct.getInlinedCfa();
       reducer.printInlinedCfa(inlinedCfa, System.out);

       sequenceApplied = reducer.applySequenceRule(funct);
       choiceApplied = reducer.applyChoiceRule(funct);
     } while (sequenceApplied || choiceApplied);


     assertEquals(3, funct.getNumOfActiveNodes());
     assertEquals(2, funct.getNumLeavingEdges(n4));

    } catch (InvalidConfigurationException e) {
      e.printStackTrace();
    }
  }



  @Test
  public void testApplySequenceRule_RepeatUntilLoop() {
   try {
     ReducedNode entryNode = new ReducedNode(new CFANode(1, "test"));
     ReducedNode exitNode = new ReducedNode(new CFANode(100, "test"));
     ReducedFunction funct = new ReducedFunction(entryNode, exitNode);

     ReducedNode n1 = new ReducedNode(new CFANode(2, "test"));
     ReducedNode n2 = new ReducedNode(new CFANode(3, "test"));
     ReducedNode n3 = new ReducedNode(new CFANode(4, "test"));
     ReducedNode n4 = new ReducedNode(new CFANode(5, "loophead"));
     n4.getWrapped().setLoopStart();

     ReducedNode n5 = new ReducedNode(new CFANode(6, "test"));
     ReducedNode n6 = new ReducedNode(new CFANode(7, "test"));
     ReducedNode n7 = new ReducedNode(new CFANode(8, "test"));

     ReducedNode n8 = new ReducedNode(new CFANode(9, "test"));
     ReducedNode n9 = new ReducedNode(new CFANode(10, "test"));


     funct.addEdge(entryNode, n1);
     funct.addEdge(n1, n2);
     funct.addEdge(n2, n3);
     funct.addEdge(n3, n4);
     funct.addEdge(n4, n5);
     funct.addEdge(n5, n6);
     funct.addEdge(n6, n7);

     funct.addEdge(n7, n4);
     funct.addEdge(n7, n8);

     funct.addEdge(n8, n9);
     funct.addEdge(n9, exitNode);

     ReducerUnderTest reducer = new ReducerUnderTest(null);
     do  {
     } while (reducer.applySequenceRule(funct));

     assertEquals(3, funct.getNumOfActiveNodes());
     assertEquals(2, funct.getNumEnteringEdges(n4));

    } catch (InvalidConfigurationException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testApplySequenceRule_RepeatUntilLoop2() {
   try {
     ReducedNode entryNode = new ReducedNode(new CFANode(0, "test"));
     ReducedNode exitNode = new ReducedNode(new CFANode(100, "test"));
     ReducedFunction funct = new ReducedFunction(entryNode, exitNode);

     ReducedNode n5 = new ReducedNode(new CFANode(5, "loophead"));
     n5.getWrapped().setLoopStart();
     ReducedNode n8 = new ReducedNode(new CFANode(8, "test"));
     ReducedNode n9 = new ReducedNode(new CFANode(9, "test"));

     funct.addEdge(entryNode, n5);
     funct.addEdge(n5, n8);
     funct.addEdge(n8, n5);
     funct.addEdge(n8, n9);
     funct.addEdge(n9, exitNode);

     ReducerUnderTest reducer = new ReducerUnderTest(null);
     do  {
//       System.out.println("----------------");
//       Map<ReducedNode, Map<ReducedNode, ReducedEdge>> inlinedCfa = funct.getInlinedCfa();
//       reducer.printInlinedCfa(inlinedCfa, System.out);
     } while (reducer.applySequenceRule(funct));

     assertEquals(3, funct.getNumOfActiveNodes());
     assertEquals(2, funct.getNumEnteringEdges(n5));

    } catch (InvalidConfigurationException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testApplySequenceRule_RepeatUntilLoop3() {
   try {
     ReducedNode entryNode = new ReducedNode(new CFANode(0, "test"));
     ReducedNode exitNode = new ReducedNode(new CFANode(100, "test"));
     ReducedNode n4 = new ReducedNode(new CFANode(4, "loophead"));
     ReducedNode n7 = new ReducedNode(new CFANode(8, "test"));
     n4.getWrapped().setLoopStart();

     ReducedFunction funct = new ReducedFunction(entryNode, exitNode);

     funct.addEdge(entryNode, n4);
     funct.addEdge(n4, exitNode);
     funct.addEdge(n4, n7);
     funct.addEdge(n7, n4);

     ReducerUnderTest reducer = new ReducerUnderTest(null);
     do  {
//       System.out.println("----------------");
//       Map<ReducedNode, Map<ReducedNode, ReducedEdge>> inlinedCfa = funct.getInlinedCfa();
//       reducer.printInlinedCfa(inlinedCfa, System.out);
     } while (reducer.applySequenceRule(funct));

     assertEquals(3, funct.getNumOfActiveNodes());
     assertEquals(2, funct.getNumEnteringEdges(n4));

    } catch (InvalidConfigurationException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testApplySequenceRule_IfBranch() {
   try {
     ReducedNode entryNode = new ReducedNode(new CFANode(1, "test"));
     ReducedNode exitNode = new ReducedNode(new CFANode(10, "test"));

     ReducedNode n2 = new ReducedNode(new CFANode(2, "test"));
     ReducedNode n3 = new ReducedNode(new CFANode(3, "test"));
     ReducedNode n4 = new ReducedNode(new CFANode(4, "test"));
     ReducedNode n5 = new ReducedNode(new CFANode(5, "test"));
     ReducedNode n6 = new ReducedNode(new CFANode(6, "test"));
     ReducedNode n7 = new ReducedNode(new CFANode(7, "test"));
     ReducedNode n8 = new ReducedNode(new CFANode(8, "test"));
     ReducedNode n9 = new ReducedNode(new CFANode(9, "test"));

     ReducedFunction funct = new ReducedFunction(entryNode, exitNode);

     funct.addEdge(entryNode, n2);
     funct.addEdge(n2, n3);
     funct.addEdge(n3, n4);
     funct.addEdge(n3, n8);
     funct.addEdge(n8, n9);
     funct.addEdge(n9, n7);
     funct.addEdge(n4, n5);
     funct.addEdge(n5, n6);
     funct.addEdge(n6, n7);
     funct.addEdge(n7, exitNode);

     ReducerUnderTest reducer = new ReducerUnderTest(null);
     do  {
       System.out.println("----------------");
       Map<ReducedNode, Map<ReducedNode, Set<ReducedEdge>>> inlinedCfa = funct.getInlinedCfa();
       reducer.printInlinedCfa(inlinedCfa, System.out);
     } while (reducer.applySequenceRule(funct));

     for (ReducedNode n: funct.getAllActiveNodes()) {
       System.out.println(n.getWrapped().getLineNumber());
     }
     assertEquals(3, funct.getNumOfActiveNodes());

    } catch (InvalidConfigurationException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testApplyReductionSequences_IfBranch() {
   try {
     ReducedNode entryNode = new ReducedNode(new CFANode(1, "test"));
     ReducedNode exitNode = new ReducedNode(new CFANode(10, "test"));

     ReducedNode n2 = new ReducedNode(new CFANode(2, "test"));
     ReducedNode n3 = new ReducedNode(new CFANode(3, "test"));
     ReducedNode n4 = new ReducedNode(new CFANode(4, "test"));
     ReducedNode n1000 = new ReducedNode(new CFANode(1000, "test"));

     ReducedFunction funct = new ReducedFunction(entryNode, exitNode);

     funct.addEdge(entryNode, n2);
     funct.addEdge(n2, n1000);
     funct.addEdge(n2, n3);
     funct.addEdge(n3, n4);
     funct.addEdge(n2, n4);
     funct.addEdge(n4, exitNode);

     ReducerUnderTest reducer = new ReducerUnderTest(null);

     Map<ReducedNode, Map<ReducedNode, Set<ReducedEdge>>> inlinedCfa = funct.getInlinedCfa();
     reducer.printInlinedCfa(inlinedCfa, System.out);

     reducer.applyReductionSequences(funct);

     System.out.println("Result:");
     inlinedCfa = funct.getInlinedCfa();
     reducer.printInlinedCfa(inlinedCfa, System.out);

     assertEquals(3, funct.getNumOfActiveNodes());

    } catch (InvalidConfigurationException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testApplyChoiceRule_IfBranch_NoChange() {
   try {
     ReducedNode entryNode = new ReducedNode(new CFANode(1, "test"));
     ReducedNode exitNode = new ReducedNode(new CFANode(10, "test"));

     ReducedNode n2 = new ReducedNode(new CFANode(2, "test"));
     ReducedNode n3 = new ReducedNode(new CFANode(3, "test"));
     ReducedNode n4 = new ReducedNode(new CFANode(4, "test"));
     ReducedNode n5 = new ReducedNode(new CFANode(5, "test"));

     ReducedFunction funct = new ReducedFunction(entryNode, exitNode);

     funct.addEdge(entryNode, n2);
     funct.addEdge(n2, n3);
     funct.addEdge(n2, n4);
     funct.addEdge(n3, n5);
     funct.addEdge(n4, n5);
     funct.addEdge(n5, exitNode);

     ReducerUnderTest reducer = new ReducerUnderTest(null);
     do  {
     } while (reducer.applyChoiceRule(funct));

     assertEquals(6, funct.getNumOfActiveNodes());

    } catch (InvalidConfigurationException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testApplyChoiceRule_IfBranch_Change() {
   try {
     ReducedNode entryNode = new ReducedNode(new CFANode(1, "test"));
     ReducedNode exitNode = new ReducedNode(new CFANode(10, "test"));

     ReducedNode n2 = new ReducedNode(new CFANode(2, "test"));
     ReducedNode n3 = new ReducedNode(new CFANode(3, "test"));

     ReducedFunction funct = new ReducedFunction(entryNode, exitNode);

     funct.addEdge(entryNode, n2);
     funct.addEdge(n2, n3);
     funct.addEdge(n2, n3);
     funct.addEdge(n3, exitNode);

     ReducerUnderTest reducer = new ReducerUnderTest(null);
     do  {
     } while (reducer.applyChoiceRule(funct));

     assertEquals(4, funct.getNumOfActiveNodes());

    } catch (InvalidConfigurationException e) {
      e.printStackTrace();
    }
  }

}
