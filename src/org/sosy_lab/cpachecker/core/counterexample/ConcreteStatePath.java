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
package org.sosy_lab.cpachecker.core.counterexample;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteStatePath.ConcerteStatePathNode;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

/**
 * This class is used as a path of {@link CFAEdge} cfa edges
 * and {@link ConcreteState} concrete States.
 *
 * It represents a concrete path to an error location in the program.
 * The cfa edges represent the series of statements that lead to the
 * error location. The concrete states hold the values of the
 * variables along the path.
 *
 * An object of this class can be used to generate
 * a {@link CFAPathWithAssignments} path with concrete assignments.
 * In those paths, the right hand side expressions of the assigments
 * are resolved where possible for each assignment along the path.
 *
 */
public final class ConcreteStatePath implements Iterable<ConcerteStatePathNode> {

  private final List<ConcerteStatePathNode> list;

  /**
   * A object of this class can be constructed, when a list
   * of pairs of concrete states {@link ConcreteState} and
   * cfa edges {@link CFAEdge} are given.
   *
   * @param pList a list of pairs of concrete States {@link ConcreteState}
   *  and cfa edges {@link CFAEdge}.
   */
  public ConcreteStatePath(List<ConcerteStatePathNode> pList) {
    list = ImmutableList.copyOf(pList);
  }

  @Override
  public final Iterator<ConcerteStatePathNode> iterator() {
    return list.iterator();
  }

  /**
   * This method can be used to construct a pair of {@link ConcreteState}
   * concrete states and {@link CFAEdge} cfa edges.
   *
   * The concrete state represents the state of the program
   * after the statement of the {@link CFAEdge} is executed.
   * A {@link ConcreteStatePath} path can be constructed
   * by using a series of these pairs.
   *
   * Note that no {@link MultiEdge} edges are allowed as
   * parameter for this method. Use the method
   * 'valueOfPathNode(List<ConcreteState> pConcreteStates, MultiEdge multiEdge)'
   * instead.
   *
   * @param pConcreteState the concrete state of the resulting pair.
   * @param cfaEdge the cfa edge of the resulting pair.
   *
   * @return Returns a pair of {@link ConcreteState}
   *  concrete states and {@link CFAEdge} cfa edges, represented
   *  as {@link ConcerteStatePathNode} node of {@link ConcreteStatePath} path.
   */
  public static ConcerteStatePathNode valueOfPathNode(ConcreteState pConcreteState, CFAEdge cfaEdge) {

    Preconditions.checkArgument(cfaEdge.getEdgeType() != CFAEdgeType.MultiEdge);
    return new SingleConcreteState(cfaEdge, pConcreteState);
  }

  /**
   * This method is used to constuct a list of pairs of {@link ConcreteState}
   * concrete states and {@link CFAEdge} cfa edges.
   *
   * {@link MultiEdge} Multi edges contain a list of cfa edges.
   * The concrete state i of the given list of concrete states represents
   * the program state after the statement i, represented by the i-th cfa edge contained
   * in the multi edge, is executed.
   *
   * @param pConcreteStates the list of concrete states representing the program states.
   * @param multiEdge a list of cfa edges representing statements in the program
   * @return Returns a sub path of the program {@link MultiConcreteState}, represented by
   * the given list of concrete states {@link ConcreteState} and cfa edges {@link MultiEdge}.
   */
  public static ConcerteStatePathNode valueOfPathNode(List<ConcreteState> pConcreteStates, MultiEdge multiEdge) {

    List<CFAEdge> edges = multiEdge.getEdges();

    assert pConcreteStates.size() == edges.size();

    List<SingleConcreteState> result = new ArrayList<>(pConcreteStates.size());

    int concreteStateCounter = 0;
    for (CFAEdge edge : edges) {
      result.add(new SingleConcreteState(edge, pConcreteStates.get(concreteStateCounter)));
      concreteStateCounter++;
    }

    return new MultiConcreteState(multiEdge, result);
  }

  public int size() {
    return list.size();
  }

  @Override
  public boolean equals(Object pObj) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int hashCode() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String toString() {
    return "ConcreteStatePath:" + list.toString();
  }

  public static abstract class ConcerteStatePathNode {

    private final CFAEdge cfaEdge;

    public ConcerteStatePathNode(CFAEdge pCfaEdge) {
      cfaEdge = pCfaEdge;
    }

    public CFAEdge getCfaEdge() {
      return cfaEdge;
    }
  }

  static final class SingleConcreteState extends ConcerteStatePathNode {

    private final ConcreteState concreteState;

    public SingleConcreteState(CFAEdge cfaEdge, ConcreteState pConcreteState) {
      super(cfaEdge);
      concreteState = pConcreteState;
      assert concreteState != null;
    }

    public ConcreteState getConcreteState() {
      return concreteState;
    }

    @Override
    public String toString() {
      return "[" + getCfaEdge().toString() + " " + concreteState.toString() + "]";
    }
  }

  static final class MultiConcreteState extends ConcerteStatePathNode implements Iterable<SingleConcreteState> {

    private final List<SingleConcreteState> concreteStates;

    public MultiConcreteState(MultiEdge pCfaEdge, List<SingleConcreteState> pConcreteStates) {
      super(pCfaEdge);
      concreteStates = ImmutableList.copyOf(pConcreteStates);
    }

    @Override
    public MultiEdge getCfaEdge() {
      return (MultiEdge) super.getCfaEdge();
    }

    @Override
    public Iterator<SingleConcreteState> iterator() {
      return concreteStates.iterator();
    }
  }
}