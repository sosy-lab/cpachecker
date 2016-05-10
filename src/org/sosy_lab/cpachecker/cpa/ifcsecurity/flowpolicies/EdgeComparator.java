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

import org.sosy_lab.cpachecker.cpa.ifcsecurity.util.InternalSetComparator;

import java.io.Serializable;
import java.util.Comparator;
import java.util.SortedSet;

/**
 * Comparator for comparing two Edges. Establish a Ordering depending on
 * 1. Comparator of Edge.from.
 * 2. Comparator of Edge.to.
 * @param <E> Type of Security Class Elements
 */
public class EdgeComparator<E extends Comparable<? super E>> implements Comparator<Edge<E>>, Serializable{
  private static final long serialVersionUID = 2957720262671604658L;
  /**
   * Comparator for Sets.
   */
  protected Comparator<SortedSet<E>> compset;

  /**
   * Constructs a new Comparator.
   */
  public EdgeComparator() {
    this.compset=new InternalSetComparator<>();
  }

  /**
   * Constructs a new Comparator.
   * @param pCompset Comparator for Sets.
   */
  public EdgeComparator(Comparator<SortedSet<E>> pCompset) {
    this.compset=pCompset;
  }

  @Override
  public int compare(Edge<E> pEdge1, Edge<E> pEdge2) {
    //to <
    if((pEdge1.getFrom()).compareTo(pEdge2.getFrom())<0) {
      return -1;
    }
    //to >
    if((pEdge1.getFrom()).compareTo(pEdge2.getFrom())>0) {
      return 1;
    }
    //to =
    if(compset.compare(pEdge1.getTo(),pEdge2.getTo())<0) {
      return -1;
    }
    if(compset.compare(pEdge1.getTo(),pEdge2.getTo())>0) {
      return 1;
    }
    return 0;
  }

}
