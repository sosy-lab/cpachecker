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
    //Join(P,Q)= TODO Description
    SortedSet<E> range1=getDomain(pThisPol);
    SortedSet<E> range2=getDomain(pOtherPol);
    SortedSet<E> newrange=SetUtil.union(range1,range2);
    ConglomeratePolicy<E> result=new ConglomeratePolicy<>();
    SortedSet<SortedSet<E>> newpowerset=SetUtil.getPowerSet(newrange);
    for(SortedSet<E> set:newpowerset){
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
     //Meet(P,Q)=(P|(R(P) intersect R(Q)) Union (Q|(R(P) intersect R(Q))
     SortedSet<E> range1=getDomain(pThisPol);
     SortedSet<E> range2=getDomain(pOtherPol);
     SortedSet<E> newrange=SetUtil.intersect(range1,range2);
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
   * Restricts a Policy to a set <i>classes</i> ignoring all other SecurityClasses that are not in the set.
   * @param pThisPol A Policy
   * @param pClasses Set of only those Security Classes that should be considered
   * @return the abstracted Policy
   */
  public ConglomeratePolicy<E> abstracted (ConglomeratePolicy<E> pThisPol, SortedSet<E> pClasses){
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
    SortedSet<E> range=getDomain(pThisPol);
    ConglomeratePolicy<E> toppol=new TopPolicy<>(range);
    ConglomeratePolicy<E> botpol=new BottomPolicy<>(range);
    botpol.setEdges(SetUtil.setminus(botpol.getEdges(),pThisPol.getEdges()));
    result=union(toppol,botpol);
    return result;
  }

  /**
   * Computes the Domain of the Policy (All Security Classes to which at least one security class can flow)
   * @param pThisPol A Policy
   * @return The Domain of the policy.
   */
  public SortedSet<E> getDomain(ConglomeratePolicy<E> pThisPol){
    SortedSet<E> result=new TreeSet<>();
    for(Edge<E> edge: pThisPol.getEdges()){
      result.add(edge.getFrom());
    }
    return result;
  }

  /**
   * Computes the Range of the Policy (All Security Classes that flow to at least one security class)
   * @param pThisPol A Policy
   * @return The Range of the policy.
   */
  public SortedSet<E> getRange(ConglomeratePolicy<E> pThisPol){
    SortedSet<E> result=new TreeSet<>();
    for(Edge<E> edge: pThisPol.getEdges()){
      for(E elem: edge.getTo()){
        result.add(elem);
      }
    }
    return result;
  }


}
