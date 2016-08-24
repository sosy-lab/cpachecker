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

import com.google.common.collect.ImmutableList;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteStatePath.ConcreteStatePathNode;

import java.util.Iterator;
import java.util.List;

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
 * a {@link CFAPathWithAssumptions} path with concrete assignments.
 * In those paths, the right hand side expressions of the assigments
 * are resolved where possible for each assignment along the path.
 *
 */
public final class ConcreteStatePath implements Iterable<ConcreteStatePathNode> {

  private final List<ConcreteStatePathNode> list;

  /**
   * A object of this class can be constructed, when a list
   * of pairs of concrete states {@link ConcreteState} and
   * cfa edges {@link CFAEdge} are given.
   *
   * @param pList a list of pairs of concrete States {@link ConcreteState}
   *  and cfa edges {@link CFAEdge}.
   */
  public ConcreteStatePath(List<ConcreteStatePathNode> pList) {
    list = ImmutableList.copyOf(pList);
  }

  @Override
  public final Iterator<ConcreteStatePathNode> iterator() {
    return list.iterator();
  }

  public int size() {
    return list.size();
  }

  @Override
  @SuppressFBWarnings("EQ_UNUSUAL")
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

  public static abstract class ConcreteStatePathNode {

    private final CFAEdge cfaEdge;

    public ConcreteStatePathNode(CFAEdge pCfaEdge) {
      cfaEdge = pCfaEdge;
    }

    public CFAEdge getCfaEdge() {
      return cfaEdge;
    }
  }

  public static class SingleConcreteState extends ConcreteStatePathNode {

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

  /**
   * Marker class for not-finished states (exist where the ARG has holes)
   */
  public static final class IntermediateConcreteState extends SingleConcreteState {

    public IntermediateConcreteState(CFAEdge pCfaEdge, ConcreteState pConcreteState) {
      super(pCfaEdge, pConcreteState);
    }

  }
}