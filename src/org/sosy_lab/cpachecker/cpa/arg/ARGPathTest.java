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
package org.sosy_lab.cpachecker.cpa.arg;

import static com.google.common.truth.Truth.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.ARGPathBuilder;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathIterator;
import org.sosy_lab.cpachecker.cpa.location.LocationState;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;


public class ARGPathTest {

  // for the builder tests
  private CFAEdge edge;
  private ARGState state;

  // for the full path and path iterator tests
  private List<CFAEdge> edges;
  private List<CFAEdge> innerEdges;
  private final static int STATE_POS_1 = 0; // position of first ARGState in ARGPath
  private final static int STATE_POS_2 = 1; // position of second ARGState in ARGPath
  private final static int STATE_POS_3 = 4; // position of third ARGState in ARGPath
  private ARGState firstARGState;
  private ARGState secondARGState;
  private ARGState thirdARGState;
  private ARGState lastARGState;
  private ARGPath path;

  @Before
  public void setup() {
    // setup for the builder tests
    edge = BlankEdge.buildNoopEdge(new CFANode("test"), new CFANode("test"));
    state = new ARGState(null, null);

    // setup for the full path and path iterator tests

    // Build a cfa-path, this is simply a chain of 10 edges
    edges = new ArrayList<>();
    CFANode firstNode = new CFANode("test");

    for (int i = 0; i < 10 ; i++) {
      CFANode secondNode = new CFANode("test");
      CFAEdge edge = BlankEdge.buildNoopEdge(firstNode, secondNode);
      edges.add(edge);
      firstNode.addLeavingEdge(edge);
      secondNode.addEnteringEdge(edge);
      firstNode = secondNode;
    }

    // mock location states for ARGPath
    LocationState firstState = Mockito.mock(LocationState.class);
    Mockito.when(firstState.getLocationNode()).thenReturn(edges.get(STATE_POS_1).getPredecessor());
    Mockito.when(firstState.getLocationNodes()).thenReturn(Collections.singleton(edges.get(STATE_POS_1).getPredecessor()));
    LocationState secondState = Mockito.mock(LocationState.class);
    Mockito.when(secondState.getLocationNode()).thenReturn(edges.get(STATE_POS_2).getPredecessor());
    Mockito.when(secondState.getLocationNodes()).thenReturn(Collections.singleton(edges.get(STATE_POS_2).getPredecessor()));
    LocationState thirdState = Mockito.mock(LocationState.class);
    Mockito.when(thirdState.getLocationNode()).thenReturn(edges.get(STATE_POS_3).getPredecessor());
    Mockito.when(thirdState.getLocationNodes()).thenReturn(Collections.singleton(edges.get(STATE_POS_3).getPredecessor()));

    // last ARGState is the end of the CFA-path we created before
    LocationState lastState = Mockito.mock(LocationState.class);
    Mockito.when(lastState.getLocationNode()).thenReturn(edges.get(edges.size()-1).getSuccessor());
    Mockito.when(lastState.getLocationNodes()).thenReturn(Collections.singleton(edges.get(edges.size()-1).getSuccessor()));

    // build argPath
    ARGPathBuilder builder = ARGPath.builder();
    innerEdges = new ArrayList<>();
    firstARGState = new ARGState(firstState, null);
    builder.add(firstARGState, edges.get(STATE_POS_1)); // edge connects to next ARGState directly
    innerEdges.add(edges.get(STATE_POS_1));
    secondARGState = new ARGState(secondState, null);
    innerEdges.add(null);
    builder.add(secondARGState, null);
    thirdARGState = new ARGState(thirdState, null);
    innerEdges.add(null);
    builder.add(thirdARGState, null);
    lastARGState = new ARGState(lastState, null);
    path = builder.build(lastARGState);
  }

  @Test
  public void testBuilderAdd() {
    ARGPathBuilder builder = ARGPath.builder();
    builder.add(state, edge);
    assertThat(builder.edges).containsExactly(edge);
    assertThat(builder.states).containsExactly(state);
  }

  @Test
  public void testBuilderRemove() {
    ARGPathBuilder builder = ARGPath.builder();
    builder.add(state, edge);

    CFAEdge secondEdge = BlankEdge.buildNoopEdge(new CFANode("test"), new CFANode("test"));
    ARGState secondState = new ARGState(null, null);
    builder.add(secondState, secondEdge);

    builder.removeLast();
    assertThat(builder.edges).containsExactly(edge);
    assertThat(builder.states).containsExactly(state);
  }

  @Test
  public void testDefaultBuilderBuild() {
    ARGPathBuilder builder = ARGPath.builder();
    builder.add(state, edge);
    ARGState secondState = new ARGState(null, null);

    List<ARGState> states = new ArrayList<>();
    states.add(state);
    states.add(secondState);
    List<CFAEdge> edges = new ArrayList<>();
    edges.add(edge);
    ARGPath path = new ARGPath(states, edges);

    assertThat(builder.build(secondState)).isEqualTo(path);
    assertThat(builder.edges).containsExactly(edge);
    assertThat(builder.states).containsExactly(state);
  }

  @Test
  public void testReverseBuilderBuild() {
    ARGPathBuilder builder = ARGPath.reverseBuilder();
    ARGState secondState = new ARGState(null, null);
    builder.add(secondState, edge);

    List<ARGState> states = new ArrayList<>();
    states.add(state);
    states.add(secondState);
    List<CFAEdge> edges = new ArrayList<>();
    edges.add(edge);
    ARGPath path = new ARGPath(states, edges);

    assertThat(builder.build(state)).isEqualTo(path);
    assertThat(builder.edges).containsExactly(edge);
    assertThat(builder.states).containsExactly(secondState);
  }

  @Test
  public void testBuilderSize() {
    ARGPathBuilder builder = ARGPath.builder();
    assertThat(builder.size()).isEqualTo(0);
    builder.add(state, edge);
    assertThat(builder.size()).isEqualTo(1);
    builder.add(state, edge);
    assertThat(builder.size()).isEqualTo(2);
  }

  @Test
  public void testGetFullPath() {
    List<CFAEdge> fullPath = path.getFullPath();
    assertThat(fullPath).isEqualTo(edges);
  }

  @Test
  public void testGetInnerEdges() {
    List<CFAEdge> innerEdges = path.getInnerEdges();
    assertThat(innerEdges).isEqualTo(this.innerEdges);

  }

  @SuppressFBWarnings(
      value="DE_MIGHT_IGNORE",
      justification="We want the the excpetions to be thrown in the unit test,"
          + " and we are sure that we can continue afterwards.")
  @Test
  public void testFullPathIterator() {
    // test fullPath iterator
    PathIterator pathIt = path.fullPathIterator();
    for (int i = 0; i < edges.size(); i++) {
      CFAEdge edge = edges.get(i);
      assertThat(pathIt.getOutgoingEdge()).isEqualTo(edge);

      if (i == STATE_POS_1) {
        try {
          ExpectedException thrown = ExpectedException.none();
          thrown.expect(IllegalStateException.class);
          pathIt.getPreviousAbstractState();
          thrown.reportMissingExceptionWithMessage("Calling getPreviousAbstractState should throw an exception while"
              + " not having advanced the iterator by one position.");
        } catch (Exception e) { /*do nothing we want to continue testing*/}

        assertThat(pathIt.getAbstractState()).isEqualTo(firstARGState);
        assertThat(pathIt.getNextAbstractState()).isEqualTo(secondARGState);
        assertThat(pathIt.getPrefixInclusive().asStatesList()).containsExactly(firstARGState);

        try {
          ExpectedException thrown = ExpectedException.none();
          thrown.expect(IllegalStateException.class);
          pathIt.getPrefixExclusive();
          thrown.reportMissingExceptionWithMessage("Calling getPrefixExclusive does not work while not having advanced"
              + " the iterator by one position.");
        } catch (Exception e) { /*do nothing we want to continue testing*/}

      } else if (i == STATE_POS_2) {
        assertThat(pathIt.getPreviousAbstractState()).isEqualTo(firstARGState);
        assertThat(pathIt.getAbstractState()).isEqualTo(secondARGState);
        assertThat(pathIt.getNextAbstractState()).isEqualTo(thirdARGState);
        assertThat(pathIt.getPrefixInclusive().asStatesList()).containsExactly(firstARGState, secondARGState);
        assertThat(pathIt.getPrefixExclusive().asStatesList()).containsExactly(firstARGState);

      } else if (i == STATE_POS_3) {
        assertThat(pathIt.getPreviousAbstractState()).isEqualTo(secondARGState);
        assertThat(pathIt.getAbstractState()).isEqualTo(thirdARGState);
        assertThat(pathIt.getNextAbstractState()).isEqualTo(lastARGState);
        assertThat(pathIt.getPrefixInclusive().asStatesList()).containsExactly(firstARGState, secondARGState, thirdARGState);
        assertThat(pathIt.getPrefixExclusive().asStatesList()).containsExactly(firstARGState, secondARGState);

      } else {
        try {
          ExpectedException thrown = ExpectedException.none();
          thrown.expect(IllegalStateException.class);
          pathIt.getAbstractState();
          thrown.reportMissingExceptionWithMessage("Calling getAbstractState should throw an exception while"
              + " in the middle of a whole in the path");
        } catch (Exception e) { /*do nothing we want to continue testing*/}

        if (i < STATE_POS_3) {
          assertThat(pathIt.getPreviousAbstractState()).isEqualTo(secondARGState);
          assertThat(pathIt.getNextAbstractState()).isEqualTo(thirdARGState);
          assertThat(pathIt.getPrefixInclusive().asStatesList()).containsExactly(firstARGState, secondARGState, thirdARGState);
          assertThat(pathIt.getPrefixExclusive().asStatesList()).containsExactly(firstARGState, secondARGState);
        } else if (i < edges.size() -1) {
          assertThat(pathIt.getPreviousAbstractState()).isEqualTo(thirdARGState);
          assertThat(pathIt.getNextAbstractState()).isEqualTo(lastARGState);
          assertThat(pathIt.getPrefixInclusive().asStatesList()).containsExactly(firstARGState, secondARGState, thirdARGState, lastARGState);
          assertThat(pathIt.getPrefixExclusive().asStatesList()).containsExactly(firstARGState, secondARGState, thirdARGState);
        } else {
          try {
            ExpectedException thrown = ExpectedException.none();
            thrown.expect(IllegalStateException.class);
            pathIt.getNextAbstractState();
            thrown.reportMissingExceptionWithMessage("Calling getNextAbstractState should throw an exception"
                + " if the iterator is on its last element.");
          } catch (Exception e) { /*do nothing we want to continue testing*/}
          assertThat(pathIt.getPrefixInclusive().asStatesList()).containsExactly(firstARGState, secondARGState, thirdARGState, lastARGState);
          assertThat(pathIt.getPrefixExclusive().asStatesList()).containsExactly(firstARGState, secondARGState, thirdARGState);
        }
      }

      pathIt.advance();
    }
  }

  @Test
  public void testReverseFullPathIterator() {
    PathIterator pathIt = path.reverseFullPathIterator();
    // pathIt is on the last state, we want the outgoing edge of it, so we adance it once
    pathIt.advance();
    for (int i = edges.size()-1; i >= 0; i--) {
      CFAEdge edge = edges.get(i);
      assertThat(pathIt.getOutgoingEdge()).isEqualTo(edge);

      if (i == STATE_POS_1) {
        try {
          ExpectedException thrown = ExpectedException.none();
          thrown.expect(IllegalStateException.class);
          pathIt.getPreviousAbstractState();
          thrown.reportMissingExceptionWithMessage("Calling getPreviousAbstractState should throw an exception while"
              + " not having advanced the iterator by one position.");
        } catch (Exception e) { /*do nothing we want to continue testing*/}

        assertThat(pathIt.getAbstractState()).isEqualTo(firstARGState);
        assertThat(pathIt.getNextAbstractState()).isEqualTo(secondARGState);

      } else if (i == STATE_POS_2) {
        assertThat(pathIt.getPreviousAbstractState()).isEqualTo(firstARGState);
        assertThat(pathIt.getAbstractState()).isEqualTo(secondARGState);
        assertThat(pathIt.getNextAbstractState()).isEqualTo(thirdARGState);

      } else if (i == STATE_POS_3) {
        assertThat(pathIt.getPreviousAbstractState()).isEqualTo(secondARGState);
        assertThat(pathIt.getAbstractState()).isEqualTo(thirdARGState);
        assertThat(pathIt.getNextAbstractState()).isEqualTo(lastARGState);

      } else {
        try {
          ExpectedException thrown = ExpectedException.none();
          thrown.expect(IllegalStateException.class);
          pathIt.getAbstractState();
          thrown.reportMissingExceptionWithMessage("Calling getAbstractState should throw an exception while"
              + " in the middle of a whole in the path");
        } catch (Exception e) { /*do nothing we want to continue testing*/}

        if (i < STATE_POS_3) {
          assertThat(pathIt.getPreviousAbstractState()).isEqualTo(secondARGState);
          assertThat(pathIt.getNextAbstractState()).isEqualTo(thirdARGState);
        } else if (i < edges.size() -1) {
          assertThat(pathIt.getPreviousAbstractState()).isEqualTo(thirdARGState);
          assertThat(pathIt.getNextAbstractState()).isEqualTo(lastARGState);
        } else {
          try {
            ExpectedException thrown = ExpectedException.none();
            thrown.expect(IllegalStateException.class);
            pathIt.getNextAbstractState();
            thrown.reportMissingExceptionWithMessage("Calling getNextAbstractState should throw an exception"
                + " if the iterator is on its last element.");
          } catch (Exception e) { /*do nothing we want to continue testing*/}
        }
      }

      if (i > 0) {
        pathIt.advance();
      }
    }
  }
}
