// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.ifcsecurity.flowpolicies;

import java.util.NavigableSet;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.util.SetUtil;

/**
 * A class for specifying the Bottom Policy for a given Domain according to Policy Algebra .
 * @param <E> Type of Security Class Elements
 */
public class BottomPolicy<E extends Comparable<? super E>> extends ConglomeratePolicy<E>{


  private static final long serialVersionUID = -7984893733529641680L;

  /**
   * Construct a Bottom Policy for the given domain <i>sets</i>
   *
   * @param pSets Domain
   */
  public BottomPolicy(NavigableSet<E> pSets) {
    NavigableSet<NavigableSet<E>> powersets = SetUtil.getPowerSet(pSets);
    Edge<E> edge;
    for(E elem:pSets){
      for (NavigableSet<E> set : powersets) {
        edge=new Edge<>(elem,set);
        addEdge(edge);
      }
    }
  }
}
