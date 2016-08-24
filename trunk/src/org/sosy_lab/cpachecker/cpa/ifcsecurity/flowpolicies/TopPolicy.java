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

import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A class for specifying the Top Policy for a given Domain according to Policy Algebra .
 * @param <E> Type of Security Class Elements
 */
public class TopPolicy<E extends Comparable<? super E>> extends ConglomeratePolicy<E>{

  private static final long serialVersionUID = -1872546989274436435L;

  /**
   * Construct a Top Policy for the given domain <i>sets</i>
   * @param pSets Domain
   */
  public TopPolicy(SortedSet<E> pSets){
    SortedSet<E> set;
    Edge<E> edge;
    for(E elem:pSets){
      set=new TreeSet<>();
      set.add(elem);
      edge=new Edge<>(elem,set);
      addEdge(edge);
    }
  }
}
