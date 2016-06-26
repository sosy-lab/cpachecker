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

import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Class for constructing a Policy that only contains all subset Edges of one specific Edge and all Reflexive Edges over the Domain of the Edge.
 * @param <E> Type of Security Class Elements
 */
public class AggregationFlow<E extends Comparable<? super E>> extends ConglomeratePolicy<E>{



  private static final long serialVersionUID = 2941727791174408022L;

  /**
   * Class for constructing a Policy that only contains all subset Edges of one specifc Edge and all Reflexive Edges over the Domain of the Edge.
   * @param pFrom the from Part for the Edge
   * @param pTo the to Part of the Edge
   */
  public AggregationFlow(E pFrom, SortedSet<E> pTo){
    SortedSet<SortedSet<E>> powersets = SetUtil.getPowerSet(pTo);
    Edge<E> edge;
    for(SortedSet<E> set:powersets){
      edge=new Edge<>(pFrom,set);
      addEdge(edge);
    }
    SortedSet<E> alphabet=new TreeSet<>(pTo);
    alphabet.add(pFrom);
    ConglomeratePolicy<E> toppol=new TopPolicy<>(alphabet);

    this.setEdges(SetUtil.union(this.getEdges(),toppol.getEdges()));
  }

}
