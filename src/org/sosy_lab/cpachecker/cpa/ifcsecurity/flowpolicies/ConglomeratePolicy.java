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

import java.io.Serializable;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Class for specifiying a Security Policy
 * @param <E> Type of Security Class Elements
 */
public class ConglomeratePolicy<E extends Comparable<? super E>> implements Serializable{

  private static final long serialVersionUID = 4589552203012815677L;
  /**
   * Internal Variable: allowed information flows relation
   */
  private SortedSet<Edge<E>> edges=new TreeSet<>();

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
   * @return All allowed information flows relation.
   */
  public SortedSet<Edge<E>> getEdges(){
    return edges;
  }

  protected void setEdges(SortedSet<Edge<E>> pEdges) {
    edges = pEdges;
  }


}
