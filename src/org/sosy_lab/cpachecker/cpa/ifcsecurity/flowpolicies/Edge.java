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
package org.sosy_lab.cpachecker.cpa.ifcsecurity.flowpolicies;

import org.sosy_lab.cpachecker.cpa.ifcsecurity.util.SetUtil;

import java.io.Serializable;
import java.util.Comparator;
import java.util.SortedSet;

/**
 * Class for specifiying an Information Flow Relation
 * @param <E> Type of Security Class Elements
 */
public class Edge<E extends Comparable<? super E>> implements Comparable<Edge<E>>, Serializable{

  private static final long serialVersionUID = -8116012552727270178L;

  /**
   * Internal Variable: Comparator that is used for comparing two Edges
   */
  private Comparator<Edge<E>> comp=new EdgeComparator<>();

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
   * @param from Security Level, which can have information of Security Levels <i>to</i>
   * @param to Security Levels, which can flow to Security Levels <i>from</i>
   */
  public Edge(E from, SortedSet<E> to){
    SetUtil<E> setutil=new SetUtil<>();
    SortedSet<E> toclone = setutil.clone(to);
    toclone.add(from);
    this.from=from;
    this.to=toclone;
  }

  @Override
  public int compareTo(Edge<E> otheredge) {
    return comp.compare(this, otheredge);
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