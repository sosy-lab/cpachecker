/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.ci;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

import com.google.common.truth.Truth;

public class CustomInstructionTest {
  private CustomInstructionApplications cia;
  private Map<CFANode, AppliedCustomInstruction> cis;
  private CustomInstruction ci;
  private Constructor<?extends AbstractState> locConstructor;
  private CFA cfa;
  private CFANode startNode;
  private Collection<CFANode> endNodes;
  private ARGState start;
  private ARGState end;

  @Before
  public void init() throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, AppliedCustomInstructionParsingFailedException, IOException, ParserException, InterruptedException {
    // TestDataTools nutzen um CFA zu bekommen
    String testProgram = ""
        + "void main(int a){"
          + "return a;"
        + "}";
    cfa = null;
    new TestDataTools();
    cfa = TestDataTools.makeCFA(testProgram);

    locConstructor = LocationState.class.getDeclaredConstructor(CFANode.class, boolean.class);
    locConstructor.setAccessible(true);

    startNode = cfa.getMainFunction();
    endNodes = new HashSet<>();

    Queue<CFANode> queue = new ArrayDeque<>();
    queue.add(startNode);
    Set<CFANode> visitedNode = new HashSet<>();
    while (!queue.isEmpty()) {
      CFANode tmp = queue.poll();
      if (tmp.getNumLeavingEdges()==0) {
        endNodes.add(tmp);
      } else if (!visitedNode.contains(tmp)) {
        visitedNode.add(tmp);
        for (CFAEdge edge : CFAUtils.allLeavingEdges(tmp)) {
          queue.add(edge.getSuccessor());
        }
      }
    }

    List<String> input = new ArrayList<>();
    input.add("a");
    List<String> output = new ArrayList<>();
    output.add("b");
    ci = new CustomInstruction(startNode, endNodes, input, output, ShutdownNotifier.create());

    cis = new HashMap<>();
    AppliedCustomInstruction aci = null;
    try {
      aci = ci.inspectAppliedCustomInstruction(startNode);
    } catch (AppliedCustomInstructionParsingFailedException | InterruptedException e) {
      // TODO
    }

    // TODO
//    Truth.assertThat(aci).isNotNull();
    cis.put(startNode, aci);

    cia = new CustomInstructionApplications(cis, ci);
    start = new ARGState(locConstructor.newInstance(startNode, true), null);
  }

  @Test
  public void testIsStartState() throws CPAException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
    Truth.assertThat(cia.isStartState(start)).isTrue();
    ARGState notStart = new ARGState(locConstructor.newInstance(startNode.getLeavingEdge(0).getSuccessor(), true), start);
    Truth.assertThat(cia.isStartState(notStart)).isFalse();
  }

  @Test
  public void testIsEndState() throws CPAException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
    CFANode endNode = null;
    CFANode predEnd = null;
    CFANode predPredEnd = null;

    Queue<CFANode> queue = new ArrayDeque<>();
    queue.add(startNode);
    Set<CFANode> visitedNodes = new HashSet<>();
    while (endNode != null) {
      predPredEnd = predEnd;
      predEnd = endNode;
      CFANode tmp = queue.poll();
      if (tmp.getNumLeavingEdges()==0) {
        endNode = tmp;
        queue.clear();
      } else if (!visitedNodes.contains(tmp)) {
        visitedNodes.add(tmp);
        for (CFAEdge edge : CFAUtils.allLeavingEdges(tmp)) {
          queue.add(edge.getSuccessor());
        }
      }
    }

    ARGState pred = new ARGState(locConstructor.newInstance(predEnd, true), new ARGState(locConstructor.newInstance(predPredEnd, true), null));
    end = new ARGState(locConstructor.newInstance(endNode, true), pred);

    // TODO
//    Truth.assertThat(cia.isEndState(end, startNode)).isTrue();
//    Truth.assertThat(cia.isEndState(pred, startNode)).isFalse();
  }

  @Test
  public void testGetAppliedCustomInstruction() throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, CPAException {
    Truth.assertThat(cia.getAppliedCustomInstructionFor(start)).isEqualTo(cis.get(cfa));
      try {
        cia.getAppliedCustomInstructionFor(end);
      } catch (CPAException e) {
        Truth.assertThat(e).isInstanceOf(CPAException.class);
      }
  }
}
