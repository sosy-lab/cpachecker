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

/**
 * A class for specifying the Bottom Policy for a given Domain according to Policy Algebra .
 * @param <E> Type of Security Class Elements
 */
public class BottomPolicy<E extends Comparable<? super E>> extends ConglomeratePolicy<E>{


  private static final long serialVersionUID = -7984893733529641680L;

  /**
   * Construct a Bottom Policy for the given domain <i>sets</i>
   * @param pSets Domain
   */
  public BottomPolicy(SortedSet<E> pSets){
    SortedSet<SortedSet<E>> powersets = SetUtil.getPowerSet(pSets);
    Edge<E> edge;
    for(E elem:pSets){
      for(SortedSet<E> set:powersets){
        edge=new Edge<>(elem,set);
        addEdge(edge);
      }
    }
  }
}
