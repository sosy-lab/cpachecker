// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.counterexample;

import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Iterator;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteStatePath.ConcreteStatePathNode;

/**
 * This class is used as a path of {@link CFAEdge} cfa edges and {@link ConcreteState} concrete
 * States.
 *
 * <p>It represents a concrete path to an error location in the program. The cfa edges represent the
 * series of statements that lead to the error location. The concrete states hold the values of the
 * variables along the path.
 *
 * <p>An object of this class can be used to generate a {@link CFAPathWithAssumptions} path with
 * concrete assignments. In those paths, the right hand side expressions of the assigments are
 * resolved where possible for each assignment along the path.
 */
public final class ConcreteStatePath implements Iterable<ConcreteStatePathNode> {

  private final List<ConcreteStatePathNode> list;

  /**
   * A object of this class can be constructed, when a list of pairs of concrete states {@link
   * ConcreteState} and cfa edges {@link CFAEdge} are given.
   *
   * @param pList a list of pairs of concrete States {@link ConcreteState} and cfa edges {@link
   *     CFAEdge}.
   */
  public ConcreteStatePath(List<ConcreteStatePathNode> pList) {
    list = ImmutableList.copyOf(pList);
  }

  @Override
  public Iterator<ConcreteStatePathNode> iterator() {
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
    return "ConcreteStatePath:" + list;
  }

  public abstract static class ConcreteStatePathNode {

    private final CFAEdge cfaEdge;

    protected ConcreteStatePathNode(CFAEdge pCfaEdge) {
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
      return "[" + getCfaEdge() + " " + concreteState + "]";
    }
  }

  /** Marker class for not-finished states (exist where the ARG has holes) */
  public static final class IntermediateConcreteState extends SingleConcreteState {

    public IntermediateConcreteState(CFAEdge pCfaEdge, ConcreteState pConcreteState) {
      super(pCfaEdge, pConcreteState);
    }
  }
}
