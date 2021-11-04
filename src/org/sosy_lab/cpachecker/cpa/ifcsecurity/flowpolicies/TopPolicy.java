// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.ifcsecurity.flowpolicies;

import java.util.NavigableSet;
import java.util.TreeSet;

/**
 * A class for specifying the Top Policy for a given Domain according to Policy Algebra .
 * @param <E> Type of Security Class Elements
 */
public class TopPolicy<E extends Comparable<? super E>> extends ConglomeratePolicy<E>{

  private static final long serialVersionUID = -1872546989274436435L;

  /**
   * Construct a Top Policy for the given domain <i>sets</i>
   *
   * @param pSets Domain
   */
  public TopPolicy(NavigableSet<E> pSets) {
    NavigableSet<E> set;
    Edge<E> edge;
    for(E elem:pSets){
      set=new TreeSet<>();
      set.add(elem);
      edge=new Edge<>(elem,set);
      addEdge(edge);
    }
  }
}
