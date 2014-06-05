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


public final class ConcreteStatePath implements Iterable<ConcerteStatePathNode> {

  private final List<ConcerteStatePathNode> list;

  public ConcreteStatePath(List<ConcerteStatePathNode> pList) {
    list = ImmutableList.copyOf(pList);
  }

  @Override
  public final Iterator<ConcerteStatePathNode> iterator() {
    return list.iterator();
  }

  public static ConcerteStatePathNode valueOfPathNode(ConcreteState pConcreteState, CFAEdge cfaEdge) {

    Preconditions.checkArgument(cfaEdge.getEdgeType() != CFAEdgeType.MultiEdge);
    return new SingleConcreteState(cfaEdge, pConcreteState);
  }

  public static ConcerteStatePathNode valueOfPathNode(ConcreteState[] pConcreteStates, MultiEdge multiEdge) {

    List<CFAEdge> edges = multiEdge.getEdges();

    Preconditions.checkArgument(edges.size() == pConcreteStates.length);

    List<SingleConcreteState> result = new ArrayList<>(pConcreteStates.length);

    int concreteStateCounter = 0;
    for (CFAEdge edge : edges) {
      result.add(new SingleConcreteState(edge, pConcreteStates[concreteStateCounter]));
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