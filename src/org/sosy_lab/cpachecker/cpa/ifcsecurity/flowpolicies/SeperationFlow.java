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
import org.sosy_lab.cpachecker.cpa.ifcsecurity.util.SetUtil;

/**
 * Class for constructing a Policy that only contains one specific Edge and all Reflexive Edges over the Domain of the Edge.
 * @param <E> Type of Security Class Elements
 */
public class SeperationFlow<E extends Comparable<? super E>> extends ConglomeratePolicy<E>{

  private static final long serialVersionUID = 2687695854333064398L;

  /**
   * Class for constructing a Policy that only contains one specific Edge and all Reflexive Edges
   * over the Domain of the Edge.
   *
   * @param pFrom the from Part for the Edge
   * @param pTo the to Part of the Edge
   */
  public SeperationFlow(E pFrom, NavigableSet<E> pTo) {
    Edge<E> edge=new Edge<>(pFrom,pTo);
    addEdge(edge);
    NavigableSet<E> alphabet = new TreeSet<>(pTo);
    alphabet.add(pFrom);
    ConglomeratePolicy<E> toppol=new TopPolicy<>(alphabet);
    this.setEdges(SetUtil.union(this.getEdges(),toppol.getEdges()));
  }
}
