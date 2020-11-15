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
 * Class, that offers the Algebra for specification of arbitrary (finite) Security Policies.
 * @param <E> Security Class
 */
public class PolicyAlgebra<E extends Comparable<? super E>> {

  /**
   * Computes the Join-Operation of the Policy-Algebra
   * @param pThisPol A Policy
   * @param pOtherPol Another Policy
   * @return Join-Policy of the two Policy
   */
  public  ConglomeratePolicy<E> join (ConglomeratePolicy<E> pThisPol, ConglomeratePolicy<E> pOtherPol){
    // Join(P,Q)= TODO Description
    NavigableSet<E> range1 = getDomain(pThisPol);
    NavigableSet<E> range2 = getDomain(pOtherPol);
    NavigableSet<E> newrange = SetUtil.union(range1, range2);
    ConglomeratePolicy<E> result=new ConglomeratePolicy<>();
    NavigableSet<NavigableSet<E>> newpowerset = SetUtil.getPowerSet(newrange);
    for (NavigableSet<E> set : newpowerset) {
      for(E elem: newrange){
        //Check First
        boolean check1=true;
        if(range1.contains(elem)){
          Edge<E> edge=new Edge<>(elem,SetUtil.intersect(set, range1));
          if(!pThisPol.getEdges().contains(edge)){
            check1=false;
          }
        }

        //Check Second
        boolean check2=true;
        if(range2.contains(elem)){
          Edge<E> edge=new Edge<>(elem,SetUtil.intersect(set, range2));
          if(!pOtherPol.getEdges().contains(edge)){
            check2=false;
          }
        }
        //Check if First and Second hold
        if(check1 && check2){
          Edge<E> edge=new Edge<>(elem,set);
          result.addEdge(edge);
        }

      }
    }
    return result;
  }

  /**
   * Computes the Meet-Operation of the Policy-Algebra
   * @param pThisPol A Policy
   * @param pOtherPol Another Policy
   * @return Meet-Policy of the two Policy
   */
  public ConglomeratePolicy<E> meet (ConglomeratePolicy<E> pThisPol, ConglomeratePolicy<E> pOtherPol){
    // Meet(P,Q)=(P|(R(P) intersect R(Q)) Union (Q|(R(P) intersect R(Q))
    NavigableSet<E> range1 = getDomain(pThisPol);
    NavigableSet<E> range2 = getDomain(pOtherPol);
    NavigableSet<E> newrange = SetUtil.intersect(range1, range2);
     ConglomeratePolicy<E> result=union(abstracted(pThisPol,newrange),abstracted(pOtherPol,newrange));
     return result;
  }

  /**
   * Computes the Intersection of all allowed Edges (allowed Information Flow) of <i>thispol</i> and <i>otherpol</i>
   * @param pThisPol A Policy
   * @param pOtherPol Another Policy
   * @return Intersection-Policy of the two Policy
   */
  public ConglomeratePolicy<E> intersect (ConglomeratePolicy<E> pThisPol, ConglomeratePolicy<E> pOtherPol){
    ConglomeratePolicy<E> result = new ConglomeratePolicy<>();
    result.setEdges(SetUtil.intersect(pThisPol.getEdges(),pOtherPol.getEdges()));
    return result;
  }

  /**
   * Computes the Union of all allowed Edges (allowed Information Flow) of <i>thispol</i> and <i>otherpol</i>
   * @param pThisPol A Policy
   * @param pOtherPol Another Policy
   * @return Union-Policy of the two Policy
   */
  public ConglomeratePolicy<E> union (ConglomeratePolicy<E> pThisPol, ConglomeratePolicy<E> pOtherPol){
    ConglomeratePolicy<E> result = new ConglomeratePolicy<>();
    result.setEdges(SetUtil.union(pThisPol.getEdges(),pOtherPol.getEdges()));
    return result;
  }

  /**
   * Restricts a Policy to a set <i>classes</i> ignoring all other SecurityClasses that are not in
   * the set.
   *
   * @param pThisPol A Policy
   * @param pClasses Set of only those Security Classes that should be considered
   * @return the abstracted Policy
   */
  public ConglomeratePolicy<E> abstracted(
      ConglomeratePolicy<E> pThisPol, NavigableSet<E> pClasses) {
    ConglomeratePolicy<E> result = new ConglomeratePolicy<>();
    for(Edge<E> edge:pThisPol.getEdges()){
      if(pClasses.contains(edge.getFrom())){
        result.addEdge(new Edge<>(edge.getFrom(), SetUtil.intersect(edge.getTo(),pClasses)));
      }
    }
    return result;
  }

  /**
   * Computes the complemented Policy of <i>thispol</i> over the same domain.
   * @param pThisPol A Policy
   * @return Complement-Policy of the two Policy
   */
  public ConglomeratePolicy<E> complement (ConglomeratePolicy<E> pThisPol){
    ConglomeratePolicy<E> result;
    NavigableSet<E> range = getDomain(pThisPol);
    ConglomeratePolicy<E> toppol=new TopPolicy<>(range);
    ConglomeratePolicy<E> botpol=new BottomPolicy<>(range);
    botpol.setEdges(SetUtil.setminus(botpol.getEdges(),pThisPol.getEdges()));
    result=union(toppol,botpol);
    return result;
  }

  /**
   * Computes the Domain of the Policy (All Security Classes to which at least one security class
   * can flow)
   *
   * @param pThisPol A Policy
   * @return The Domain of the policy.
   */
  public NavigableSet<E> getDomain(ConglomeratePolicy<E> pThisPol) {
    NavigableSet<E> result = new TreeSet<>();
    for(Edge<E> edge: pThisPol.getEdges()){
      result.add(edge.getFrom());
    }
    return result;
  }

  /**
   * Computes the Range of the Policy (All Security Classes that flow to at least one security
   * class)
   *
   * @param pThisPol A Policy
   * @return The Range of the policy.
   */
  public NavigableSet<E> getRange(ConglomeratePolicy<E> pThisPol) {
    NavigableSet<E> result = new TreeSet<>();
    for(Edge<E> edge: pThisPol.getEdges()){
      result.addAll(edge.getTo());
    }
    return result;
  }


}
