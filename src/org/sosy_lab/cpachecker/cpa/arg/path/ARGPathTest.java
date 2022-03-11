// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg.path;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.sosy_lab.cpachecker.cfa.model.CFANode.newDummyCFANode;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class ARGPathTest {

  // for the builder tests
  private CFAEdge edge;
  private ARGState state;

  // for the full path and path iterator tests
  private List<CFAEdge> edges;
  private List<CFAEdge> innerEdges;
  private static final int STATE_POS_1 = 0; // position of first ARGState in ARGPath
  private static final int STATE_POS_2 = 1; // position of second ARGState in ARGPath
  private static final int STATE_POS_3 = 4; // position of third ARGState in ARGPath
  private ARGState firstARGState;
  private ARGState secondARGState;
  private ARGState thirdARGState;
  private ARGState lastARGState;
  private ARGPath path;

  @Before
  public void setup() {
    // setup for the builder tests
    edge =
        new BlankEdge(
            "", FileLocation.DUMMY, newDummyCFANode("test"), newDummyCFANode("test"), "test");
    edge.getSuccessor().addEnteringEdge(edge);
    edge.getPredecessor().addLeavingEdge(edge);
    LocationState tmp = Mockito.mock(LocationState.class);
    Mockito.when(tmp.getLocationNode()).thenReturn(edge.getPredecessor());
    Mockito.when(tmp.getLocationNodes()).thenReturn(Collections.singleton(edge.getPredecessor()));
    state = new ARGState(tmp, null);

    // setup for the full path and path iterator tests

    // Build a cfa-path, this is simply a chain of 10 edges
    edges = new ArrayList<>();
    CFANode firstNode = newDummyCFANode("test");

    for (int i = 0; i < 10; i++) {
      CFANode secondNode = newDummyCFANode("test");
      CFAEdge dummyEdge = new BlankEdge("", FileLocation.DUMMY, firstNode, secondNode, "test");
      edges.add(dummyEdge);
      firstNode.addLeavingEdge(dummyEdge);
      secondNode.addEnteringEdge(dummyEdge);
      firstNode = secondNode;
    }

    // mock location states for ARGPath
    LocationState firstState = Mockito.mock(LocationState.class);
    Mockito.when(firstState.getLocationNode()).thenReturn(edges.get(STATE_POS_1).getPredecessor());
    Mockito.when(firstState.getLocationNodes())
        .thenReturn(Collections.singleton(edges.get(STATE_POS_1).getPredecessor()));
    LocationState secondState = Mockito.mock(LocationState.class);
    Mockito.when(secondState.getLocationNode()).thenReturn(edges.get(STATE_POS_2).getPredecessor());
    Mockito.when(secondState.getLocationNodes())
        .thenReturn(Collections.singleton(edges.get(STATE_POS_2).getPredecessor()));
    LocationState thirdState = Mockito.mock(LocationState.class);
    Mockito.when(thirdState.getLocationNode()).thenReturn(edges.get(STATE_POS_3).getPredecessor());
    Mockito.when(thirdState.getLocationNodes())
        .thenReturn(Collections.singleton(edges.get(STATE_POS_3).getPredecessor()));

    // last ARGState is the end of the CFA-path we created before
    LocationState lastState = Mockito.mock(LocationState.class);
    Mockito.when(lastState.getLocationNode())
        .thenReturn(edges.get(edges.size() - 1).getSuccessor());
    Mockito.when(lastState.getLocationNodes())
        .thenReturn(Collections.singleton(edges.get(edges.size() - 1).getSuccessor()));

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

    builder.removeLast();
    assertThat(builder.edges).isEmpty();
    assertThat(builder.states).isEmpty();
  }

  @Test
  public void testDefaultBuilderBuild() {
    ARGPathBuilder builder = ARGPath.builder();
    builder.add(state, edge);
    LocationState tmp = Mockito.mock(LocationState.class);
    Mockito.when(tmp.getLocationNode()).thenReturn(edge.getSuccessor());
    Mockito.when(tmp.getLocationNodes()).thenReturn(Collections.singleton(edge.getSuccessor()));
    ARGState secondState = new ARGState(tmp, null);

    List<ARGState> states = new ArrayList<>();
    states.add(state);
    states.add(secondState);
    List<CFAEdge> newEdges = new ArrayList<>();
    newEdges.add(edge);
    ARGPath newPath = new ARGPath(states, newEdges);

    assertThat(builder.build(secondState)).isEqualTo(newPath);
    assertThat(builder.edges).containsExactly(edge);
    assertThat(builder.states).containsExactly(state);
  }

  @Test
  public void testReverseBuilderBuild() {
    ARGPathBuilder builder = ARGPath.reverseBuilder();
    LocationState tmp = Mockito.mock(LocationState.class);
    Mockito.when(tmp.getLocationNode()).thenReturn(edge.getSuccessor());
    Mockito.when(tmp.getLocationNodes()).thenReturn(Collections.singleton(edge.getSuccessor()));
    ARGState secondState = new ARGState(tmp, null);
    builder.add(secondState, edge);

    List<ARGState> states = new ArrayList<>();
    states.add(state);
    states.add(secondState);
    List<CFAEdge> newEdges = new ArrayList<>();
    newEdges.add(edge);
    ARGPath newPath = new ARGPath(states, newEdges);

    assertThat(builder.build(state)).isEqualTo(newPath);
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
    assertThat(path.getInnerEdges()).isEqualTo(innerEdges);
  }

  @Test
  public void testGetPrefixInclusive() {
    assertThat(path.pathIterator().getPrefixInclusive())
        .isEqualTo(ARGPath.builder().build(firstARGState));
    assertThat(path.reversePathIterator().getPrefixInclusive()).isEqualTo(path);

    PathIterator it = path.pathIterator();
    it.advance();
    assertThat(it.getPrefixInclusive())
        .isEqualTo(ARGPath.builder().add(firstARGState, edges.get(0)).build(secondARGState));
  }

  @Test
  public void testGetPrefixExclusive() {
    PathIterator it = path.pathIterator();
    it.advance();
    assertThat(it.getPrefixExclusive()).isEqualTo(ARGPath.builder().build(firstARGState));
  }

  @Test(expected = IllegalStateException.class)
  public void testGetPrefixExclusiveFails() {
    path.pathIterator().getPrefixExclusive();
  }

  @Test
  public void testGetSuffixInclusive() {
    assertThat(path.reversePathIterator().getSuffixInclusive())
        .isEqualTo(ARGPath.builder().build(lastARGState));
    assertThat(path.pathIterator().getSuffixInclusive()).isEqualTo(path);

    PathIterator it = path.reversePathIterator();
    it.advance();
    assertThat(it.getSuffixInclusive())
        .isEqualTo(ARGPath.builder().add(thirdARGState, null).build(lastARGState));
  }

  @Test
  public void testGetSuffixExclusive() {
    PathIterator it = path.reversePathIterator();
    it.advance();
    assertThat(it.getSuffixExclusive()).isEqualTo(ARGPath.builder().build(lastARGState));
  }

  @Test(expected = IllegalStateException.class)
  public void testGetSuffixExclusiveFails() {
    path.reversePathIterator().getSuffixExclusive();
  }

  @SuppressFBWarnings(
      value = "DE_MIGHT_IGNORE",
      justification =
          "We want the the excpetions to be thrown in the unit test,"
              + " and we are sure that we can continue afterwards.")
  @Test
  public void testFullPathIterator() {
    // test fullPath iterator
    PathIterator pathIt = path.fullPathIterator();
    for (int i = 0; i < edges.size(); i++) {
      assertThat(pathIt.getOutgoingEdge()).isEqualTo(edges.get(i));

      switch (i) {
        case STATE_POS_1:
          assertThrows(
              "getPreviousAbstractState should throw"
                  + " while not having advanced the iterator by one position",
              IllegalStateException.class,
              pathIt::getPreviousAbstractState);
          assertThat(pathIt.getAbstractState()).isEqualTo(firstARGState);
          assertThat(pathIt.getNextAbstractState()).isEqualTo(secondARGState);
          assertThat(pathIt.getPrefixInclusive().asStatesList()).containsExactly(firstARGState);
          assertThrows(
              "getPrefixExclusive should throw"
                  + " while not having advanced the iterator by one position",
              IllegalStateException.class,
              pathIt::getPrefixExclusive);
          break;
        case STATE_POS_2:
          assertThat(pathIt.getPreviousAbstractState()).isEqualTo(firstARGState);
          assertThat(pathIt.getAbstractState()).isEqualTo(secondARGState);
          assertThat(pathIt.getNextAbstractState()).isEqualTo(thirdARGState);
          assertThat(pathIt.getPrefixInclusive().asStatesList())
              .containsExactly(firstARGState, secondARGState);
          assertThat(pathIt.getPrefixExclusive().asStatesList()).containsExactly(firstARGState);
          break;
        case STATE_POS_3:
          assertThat(pathIt.getPreviousAbstractState()).isEqualTo(secondARGState);
          assertThat(pathIt.getAbstractState()).isEqualTo(thirdARGState);
          assertThat(pathIt.getNextAbstractState()).isEqualTo(lastARGState);
          assertThat(pathIt.getPrefixInclusive().asStatesList())
              .containsExactly(firstARGState, secondARGState, thirdARGState);
          assertThat(pathIt.getPrefixExclusive().asStatesList())
              .containsExactly(firstARGState, secondARGState);
          break;
        default:
          assertThrows(
              "getAbstractState should throw while in the middle of a hole in the path",
              IllegalStateException.class,
              pathIt::getAbstractState);
          if (i < STATE_POS_3) {
            assertThat(pathIt.getPreviousAbstractState()).isEqualTo(secondARGState);
            assertThat(pathIt.getNextAbstractState()).isEqualTo(thirdARGState);
            assertThat(pathIt.getPrefixInclusive().asStatesList())
                .containsExactly(firstARGState, secondARGState, thirdARGState);
            assertThat(pathIt.getPrefixExclusive().asStatesList())
                .containsExactly(firstARGState, secondARGState);
          } else {
            assertThat(pathIt.getPreviousAbstractState()).isEqualTo(thirdARGState);
            assertThat(pathIt.getNextAbstractState()).isEqualTo(lastARGState);
            assertThat(pathIt.getPrefixInclusive().asStatesList())
                .containsExactly(firstARGState, secondARGState, thirdARGState, lastARGState);
            assertThat(pathIt.getPrefixExclusive().asStatesList())
                .containsExactly(firstARGState, secondARGState, thirdARGState);
          }
          break;
      }

      pathIt.advance();
    }

    assertThrows(
        "getNextAbstractState should throw if the iterator is on its last element",
        IllegalStateException.class,
        pathIt::getNextAbstractState);
  }

  @Test
  public void testReverseFullPathIterator() {
    PathIterator pathIt = path.reverseFullPathIterator();

    assertThrows(
        "getNextAbstractState should throw if the iterator is on its last element",
        IllegalStateException.class,
        pathIt::getNextAbstractState);

    // pathIt is on the last state, we want the outgoing edge of it, so we adance it once
    pathIt.advance();
    for (int i = edges.size() - 1; i >= 0; i--) {
      assertThat(pathIt.getOutgoingEdge()).isEqualTo(edges.get(i));

      switch (i) {
        case STATE_POS_1:
          assertThrows(
              "getPreviousAbstractState should throw"
                  + " while not having advanced the iterator by one position",
              IllegalStateException.class,
              pathIt::getPreviousAbstractState);
          assertThat(pathIt.getAbstractState()).isEqualTo(firstARGState);
          assertThat(pathIt.getNextAbstractState()).isEqualTo(secondARGState);
          break;
        case STATE_POS_2:
          assertThat(pathIt.getPreviousAbstractState()).isEqualTo(firstARGState);
          assertThat(pathIt.getAbstractState()).isEqualTo(secondARGState);
          assertThat(pathIt.getNextAbstractState()).isEqualTo(thirdARGState);
          break;
        case STATE_POS_3:
          assertThat(pathIt.getPreviousAbstractState()).isEqualTo(secondARGState);
          assertThat(pathIt.getAbstractState()).isEqualTo(thirdARGState);
          assertThat(pathIt.getNextAbstractState()).isEqualTo(lastARGState);
          break;
        default:
          assertThrows(
              "getAbstractState should throw while in the middle of a whole in the path",
              IllegalStateException.class,
              pathIt::getAbstractState);
          if (i < STATE_POS_3) {
            assertThat(pathIt.getPreviousAbstractState()).isEqualTo(secondARGState);
            assertThat(pathIt.getNextAbstractState()).isEqualTo(thirdARGState);
          } else {
            assertThat(pathIt.getPreviousAbstractState()).isEqualTo(thirdARGState);
            assertThat(pathIt.getNextAbstractState()).isEqualTo(lastARGState);
          }
          break;
      }

      if (i > 0) {
        pathIt.advance();
      }
    }
  }

  @Test
  public void testFullPathIterator2() {
    // go to last element and then use rewind to test its functionality
    PathIterator pathIt = path.fullPathIterator();
    while (pathIt.hasNext()) {
      pathIt.advance();
    }

    assertThrows(
        "getNextAbstractState should throw if the iterator is on its last element",
        IllegalStateException.class,
        pathIt::getNextAbstractState);

    // pathIt is on the last state, we want the outgoing edge of it, so we adance it once
    pathIt.rewind();
    for (int i = edges.size() - 1; i >= 0; i--) {
      assertThat(pathIt.getOutgoingEdge()).isEqualTo(edges.get(i));

      switch (i) {
        case STATE_POS_1:
          assertThrows(
              "getPreviousAbstractState should throw"
                  + "while not having advanced the iterator by one position",
              IllegalStateException.class,
              pathIt::getPreviousAbstractState);
          assertThat(pathIt.getAbstractState()).isEqualTo(firstARGState);
          assertThat(pathIt.getNextAbstractState()).isEqualTo(secondARGState);
          break;
        case STATE_POS_2:
          assertThat(pathIt.getPreviousAbstractState()).isEqualTo(firstARGState);
          assertThat(pathIt.getAbstractState()).isEqualTo(secondARGState);
          assertThat(pathIt.getNextAbstractState()).isEqualTo(thirdARGState);
          break;
        case STATE_POS_3:
          assertThat(pathIt.getPreviousAbstractState()).isEqualTo(secondARGState);
          assertThat(pathIt.getAbstractState()).isEqualTo(thirdARGState);
          assertThat(pathIt.getNextAbstractState()).isEqualTo(lastARGState);
          break;
        default:
          assertThrows(
              "getAbstractState should throw while in the middle of a whole in the path",
              IllegalStateException.class,
              pathIt::getAbstractState);
          if (i < STATE_POS_3) {
            assertThat(pathIt.getPreviousAbstractState()).isEqualTo(secondARGState);
            assertThat(pathIt.getNextAbstractState()).isEqualTo(thirdARGState);
          } else {
            assertThat(pathIt.getPreviousAbstractState()).isEqualTo(thirdARGState);
            assertThat(pathIt.getNextAbstractState()).isEqualTo(lastARGState);
          }
          break;
      }

      if (i > 0) {
        pathIt.rewind();
      }
    }
  }

  @SuppressFBWarnings(
      value = "DE_MIGHT_IGNORE",
      justification =
          "We want the the excpetions to be thrown in the unit test,"
              + " and we are sure that we can continue afterwards.")
  @Test
  public void testReverseFullPathIterator2() {
    // go to the end of the reverse full iterator and then test rewind functionality
    PathIterator pathIt = path.reverseFullPathIterator();
    while (pathIt.hasNext()) {
      pathIt.advance();
    }

    for (int i = 0; i < edges.size(); i++) {
      assertThat(pathIt.getOutgoingEdge()).isEqualTo(edges.get(i));

      switch (i) {
        case STATE_POS_1:
          assertThrows(
              "getPreviousAbstractState should throw"
                  + " while not having advanced the iterator by one position",
              IllegalStateException.class,
              pathIt::getPreviousAbstractState);
          assertThat(pathIt.getAbstractState()).isEqualTo(firstARGState);
          assertThat(pathIt.getNextAbstractState()).isEqualTo(secondARGState);
          assertThat(pathIt.getPrefixInclusive().asStatesList()).containsExactly(firstARGState);
          assertThrows(
              "getPrefixExclusive should throw"
                  + " while not having advanced  the iterator by one position",
              IllegalStateException.class,
              pathIt::getPrefixExclusive);
          break;
        case STATE_POS_2:
          assertThat(pathIt.getPreviousAbstractState()).isEqualTo(firstARGState);
          assertThat(pathIt.getAbstractState()).isEqualTo(secondARGState);
          assertThat(pathIt.getNextAbstractState()).isEqualTo(thirdARGState);
          assertThat(pathIt.getPrefixInclusive().asStatesList())
              .containsExactly(firstARGState, secondARGState);
          assertThat(pathIt.getPrefixExclusive().asStatesList()).containsExactly(firstARGState);
          break;
        case STATE_POS_3:
          assertThat(pathIt.getPreviousAbstractState()).isEqualTo(secondARGState);
          assertThat(pathIt.getAbstractState()).isEqualTo(thirdARGState);
          assertThat(pathIt.getNextAbstractState()).isEqualTo(lastARGState);
          assertThat(pathIt.getPrefixInclusive().asStatesList())
              .containsExactly(firstARGState, secondARGState, thirdARGState);
          assertThat(pathIt.getPrefixExclusive().asStatesList())
              .containsExactly(firstARGState, secondARGState);
          break;
        default:
          assertThrows(
              "getAbstractState should throw while in the middle of a hole in the path",
              IllegalStateException.class,
              pathIt::getAbstractState);
          if (i < STATE_POS_3) {
            assertThat(pathIt.getPreviousAbstractState()).isEqualTo(secondARGState);
            assertThat(pathIt.getNextAbstractState()).isEqualTo(thirdARGState);
            assertThat(pathIt.getPrefixInclusive().asStatesList())
                .containsExactly(firstARGState, secondARGState, thirdARGState);
            assertThat(pathIt.getPrefixExclusive().asStatesList())
                .containsExactly(firstARGState, secondARGState);
          } else {
            assertThat(pathIt.getPreviousAbstractState()).isEqualTo(thirdARGState);
            assertThat(pathIt.getNextAbstractState()).isEqualTo(lastARGState);
            assertThat(pathIt.getPrefixInclusive().asStatesList())
                .containsExactly(firstARGState, secondARGState, thirdARGState, lastARGState);
            assertThat(pathIt.getPrefixExclusive().asStatesList())
                .containsExactly(firstARGState, secondARGState, thirdARGState);
          }
          break;
      }

      pathIt.rewind();
    }

    assertThrows(
        "getNextAbstractState should throw if the iterator is on its last element",
        IllegalStateException.class,
        pathIt::getNextAbstractState);
  }

  @Test
  public void testPathPositionDefault() {
    // pathiterator -> fullpathiterator
    PathIterator it = path.pathIterator();
    it.advance();

    assertThat(it.getIncomingEdge())
        .isEqualTo(it.getPosition().fullPathIterator().getIncomingEdge());

    it.advance();
    assertThat(it.getPosition().fullPathIterator().getIncomingEdge().getPredecessor())
        .isNotEqualTo(AbstractStates.extractLocation(firstARGState));

    // fullpathiterator -> iterator
    it = path.fullPathIterator();
    it.advance();
    PathIterator it2 = it.getPosition().iterator();

    assertThat(it.getIncomingEdge()).isEqualTo(it2.getIncomingEdge());

    it.advance();
    assertThat(it2.getIncomingEdge()).isEqualTo(it.getPosition().iterator().getIncomingEdge());

    // fullpathiterator -> fullpathiterator
    assertThat(it.getPosition().fullPathIterator().getOutgoingEdge())
        .isEqualTo(it.getOutgoingEdge());
  }

  @Test
  public void testPathPositionReverse() {
    // pathiterator -> fullpathiterator
    PathIterator it = path.reversePathIterator();
    while (it.advanceIfPossible()) {}
    it.rewind();

    assertThat(it.getIncomingEdge())
        .isEqualTo(it.getPosition().reverseFullPathIterator().getIncomingEdge());

    it.rewind();
    assertThat(it.getPosition().fullPathIterator().getIncomingEdge().getPredecessor())
        .isNotEqualTo(AbstractStates.extractLocation(firstARGState));

    // fullpathiterator -> iterator
    it = path.reverseFullPathIterator();
    while (it.advanceIfPossible()) {}
    it.rewind();

    PathIterator it2 = it.getPosition().reverseIterator();

    assertThat(it.getIncomingEdge()).isEqualTo(it2.getIncomingEdge());

    it.rewind();
    assertThat(it2.getIncomingEdge())
        .isEqualTo(it.getPosition().reverseIterator().getIncomingEdge());

    // fullpathiterator -> fullpathiterator
    assertThat(it.getPosition().reverseFullPathIterator().getOutgoingEdge())
        .isEqualTo(it.getOutgoingEdge());
  }
}
