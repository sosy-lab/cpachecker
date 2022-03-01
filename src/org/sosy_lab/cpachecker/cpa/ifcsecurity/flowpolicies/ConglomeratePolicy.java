// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.ifcsecurity.flowpolicies;

import java.io.Serializable;
import java.util.NavigableSet;
import java.util.TreeSet;

/**
 * Class for specifiying a Security Policy
 * @param <E> Type of Security Class Elements
 */
public class ConglomeratePolicy<E extends Comparable<? super E>> implements Serializable{

  private static final long serialVersionUID = 4589552203012815677L;
  /** Internal Variable: allowed information flows relation */
  private NavigableSet<Edge<E>> edges = new TreeSet<>();

  /**
   * Constructs an empty Security Policy
   */
  protected ConglomeratePolicy(){

  }

  /**
   * Allows the information flow relation described in <i>edge</i>.
   * @param pEdge A Information flow.
   */
  public void addEdge(Edge<E> pEdge){
    edges.add(pEdge);
  }

  /**
   * Remove the allowed information flow relation described in <i>edge</i>
   * @param pEdge A Information flow.
   */
  public void removeEdge(Edge<E> pEdge){
    edges.remove(pEdge);
  }



  @Override
  public boolean equals(Object pObj){
    if(this==pObj){
      return true;
    }
    if(pObj==null){
      return false;
    }
    if(!(pObj instanceof ConglomeratePolicy)){
      return false;
    }
    @SuppressWarnings("unchecked")
    ConglomeratePolicy<E> otherpol=(ConglomeratePolicy<E>) pObj;
    return this.edges.equals(otherpol.edges);
  }

  @Override
  public String toString(){
    return edges.toString();
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  /**
   * Returns all Allowed Information Flow Relations.
   *
   * @return All allowed information flows relation.
   */
  public NavigableSet<Edge<E>> getEdges() {
    return edges;
  }

  protected void setEdges(NavigableSet<Edge<E>> pEdges) {
    edges = pEdges;
  }


}
