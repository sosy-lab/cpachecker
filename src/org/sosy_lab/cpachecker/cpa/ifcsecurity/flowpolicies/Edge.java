// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.ifcsecurity.flowpolicies;

import com.google.common.collect.ComparisonChain;

import org.sosy_lab.cpachecker.cpa.ifcsecurity.util.InternalSetComparator;

import java.io.Serializable;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Class for specifiying an Information Flow Relation
 * @param <E> Type of Security Class Elements
 */
public class Edge<E extends Comparable<? super E>> implements Comparable<Edge<E>>, Serializable{

  private static final long serialVersionUID = -8116012552727270178L;

  /**
   * Security Level, which can have information of Security Levels <i>to</i>
   */
  private E from;
  /**
   * Security Levels, which can flow to Security Levels <i>from</i>
   */
  private SortedSet<E> to;


  /**
   * Constructs a new Information Flow Relation
   * @param pFrom Security Level, which can have information of Security Levels <i>to</i>
   * @param pTo Security Levels, which can flow to Security Levels <i>from</i>
   */
  public Edge(E pFrom, SortedSet<E> pTo){
    SortedSet<E> toclone = new TreeSet<>(pTo);
    toclone.add(pFrom);
    this.from=pFrom;
    this.to=toclone;
  }

  @Override
  public int compareTo(Edge<E> pOtherEdge) {
    return ComparisonChain.start()
        .compare(from, pOtherEdge.from)
        .<SortedSet<E>>compare(to, pOtherEdge.to, new InternalSetComparator<E>())
        .result();
  }

  @Override
  public boolean equals(Object pObj) {
    if (this == pObj) {
      return true;
    }
    if (pObj instanceof Edge) {
      Edge<?> other = (Edge<?>) pObj;
      return from.equals(other.from)
          && to.equals(other.to);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(from, to);
  }

  @Override
  public String toString(){
    return "("+from.toString()+","+to.toString()+")";
  }

  protected E getFrom() {
    return from;
  }


  protected void setFrom(E pFrom) {
    from = pFrom;
  }


  protected SortedSet<E> getTo() {
    return to;
  }


  protected void setTo(SortedSet<E> pTo) {
    to = pTo;
  }



}