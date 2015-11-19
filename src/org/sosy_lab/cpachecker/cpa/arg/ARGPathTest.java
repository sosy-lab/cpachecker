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
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.ARGPathBuilder;


public class ARGPathTest {

  private CFAEdge edge;
  private ARGState state;

  @Before
  public void setup() {
    edge = BlankEdge.buildNoopEdge(new CFANode(""), new CFANode(""));
    state = new ARGState(null, null);
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

    CFAEdge secondEdge = BlankEdge.buildNoopEdge(new CFANode(""), new CFANode(""));
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
    CFAEdge secondEdge = null; // last edge is null
    ARGState secondState = new ARGState(null, null);

    List<ARGState> states = new ArrayList<>();
    states.add(state);
    states.add(secondState);
    List<CFAEdge> edges = new ArrayList<>();
    edges.add(edge);
    edges.add(secondEdge);
    ARGPath path = new ARGPath(states, edges);

    assertTrue(builder.build(secondState, secondEdge).equals(path));
    assertThat(builder.edges).containsExactly(edge);
    assertThat(builder.states).containsExactly(state);
  }

  @Test
  public void testReverseBuilderBuild() {
    ARGPathBuilder builder = ARGPath.reverseBuilder();
    CFAEdge secondEdge = null; // last edge is null
    ARGState secondState = new ARGState(null, null);
    builder.add(secondState, secondEdge);

    List<ARGState> states = new ArrayList<>();
    states.add(state);
    states.add(secondState);
    List<CFAEdge> edges = new ArrayList<>();
    edges.add(edge);
    edges.add(secondEdge);
    ARGPath path = new ARGPath(states, edges);

    assertTrue(builder.build(state, edge).equals(path));
    assertThat(builder.edges).containsExactly(secondEdge);
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

}
