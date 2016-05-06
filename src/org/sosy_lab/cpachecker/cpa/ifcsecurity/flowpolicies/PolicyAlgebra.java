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

import org.sosy_lab.cpachecker.cpa.ifcsecurity.util.SetUtil;


/**
 * Class, that offers the Algebra for specification of arbitrary (finite) Security Policies.
 * @param <E> Security Class
 */
public class PolicyAlgebra<E extends Comparable<? super E>> {

  /**
   * Utility for computation of Set-Operations over the Security-Class Elements.
   */
  private SetUtil<E> setutil=new SetUtil<>();
  /**
   * Utility for computation of Set-Operations over the Policy-Edges.
   */
  private SetUtil<Edge<E>> setutil2=new SetUtil<>();

  /**
   * Computes the Join-Operation of the Policy-Algebra
   * @param thispol A Policy
   * @param otherpol Another Policy
   * @return Join-Policy of the two Policy
   */
  public  ConglomeratePolicy<E> join (ConglomeratePolicy<E> thispol, ConglomeratePolicy<E> otherpol){
    //Join(P,Q)= TODO Description
    SortedSet<E> range1=getDomain(thispol);
    SortedSet<E> range2=getDomain(otherpol);
    SortedSet<E> newrange=setutil.union(range1,range2);
    ConglomeratePolicy<E> result=new ConglomeratePolicy<>();
    SortedSet<SortedSet<E>> newpowerset=setutil.getPowerSet(newrange);
    for(SortedSet<E> set:newpowerset){
      for(E elem: newrange){
        //Check First
        boolean check1=true;
        if(range1.contains(elem)){
          Edge<E> edge=new Edge<>(elem,setutil.intersect(set, range1));
          if(!thispol.getEdges().contains(edge)){
            check1=false;
          }
        }

        //Check Second
        boolean check2=true;
        if(range2.contains(elem)){
          Edge<E> edge=new Edge<>(elem,setutil.intersect(set, range2));
          if(!otherpol.getEdges().contains(edge)){
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
   * @param thispol A Policy
   * @param otherpol Another Policy
   * @return Meet-Policy of the two Policy
   */
  public ConglomeratePolicy<E> meet (ConglomeratePolicy<E> thispol, ConglomeratePolicy<E> otherpol){
     //Meet(P,Q)=(P|(R(P) intersect R(Q)) Union (Q|(R(P) intersect R(Q))
     SortedSet<E> range1=getDomain(thispol);
     SortedSet<E> range2=getDomain(otherpol);
     SortedSet<E> newrange=setutil.intersect(range1,range2);
     ConglomeratePolicy<E> result=union(abstracted(thispol,newrange),abstracted(otherpol,newrange));
     return result;
  }

  /**
   * Computes the Intersection of all allowed Edges (allowed Information Flow) of <i>thispol</i> and <i>otherpol</i>
   * @param thispol A Policy
   * @param otherpol Another Policy
   * @return Intersection-Policy of the two Policy
   */
  public ConglomeratePolicy<E> intersect (ConglomeratePolicy<E> thispol, ConglomeratePolicy<E> otherpol){
    ConglomeratePolicy<E> result = new ConglomeratePolicy<>();
    result.setEdges(setutil2.intersect(thispol.getEdges(),otherpol.getEdges()));
    return result;
  }

  /**
   * Computes the Union of all allowed Edges (allowed Information Flow) of <i>thispol</i> and <i>otherpol</i>
   * @param thispol A Policy
   * @param otherpol Another Policy
   * @return Union-Policy of the two Policy
   */
  public ConglomeratePolicy<E> union (ConglomeratePolicy<E> thispol, ConglomeratePolicy<E> otherpol){
    ConglomeratePolicy<E> result = new ConglomeratePolicy<>();
    result.setEdges(setutil2.union(thispol.getEdges(),otherpol.getEdges()));
    return result;
  }


  /**
   * Restricts a Policy to a set <i>classes</i> ignoring all other SecurityClasses that are not in the set.
   * @param thispol A Policy
   * @param classes Set of only those Security Classes that should be considered
   * @return the abstracted Policy
   */
  public ConglomeratePolicy<E> abstracted (ConglomeratePolicy<E> thispol, SortedSet<E> classes){
    ConglomeratePolicy<E> result = new ConglomeratePolicy<>();
    for(Edge<E> edge:thispol.getEdges()){
      if(classes.contains(edge.getFrom())){
        result.addEdge(new Edge<>(edge.getFrom(), setutil.intersect(edge.getTo(),classes)));
      }
    }
    return result;
  }

  /**
   * Computes the complemented Policy of <i>thispol</i> over the same domain.
   * @param thispol A Policy
   * @return Complement-Policy of the two Policy
   */
  public ConglomeratePolicy<E> complement (ConglomeratePolicy<E> thispol){
    ConglomeratePolicy<E> result = new ConglomeratePolicy<>();
    SortedSet<E> range=getDomain(thispol);
    ConglomeratePolicy<E> toppol=new TopPolicy<>(range);
    ConglomeratePolicy<E> botpol=new BottomPolicy<>(range);
    botpol.setEdges(setutil2.setminus(botpol.getEdges(),thispol.getEdges()));
    result=union(toppol,botpol);
    return result;
  }

  /**
   * Computes the Domain of the Policy (All Security Classes to which at least one security class can flow)
   * @param thispol A Policy
   * @return The Domain of the policy.
   */
  public SortedSet<E> getDomain(ConglomeratePolicy<E> thispol){
    SortedSet<E> result=new TreeSet<>();
    for(Edge<E> edge: thispol.getEdges()){
      result.add(edge.getFrom());
    }
    return result;
  }

  /**
   * Computes the Range of the Policy (All Security Classes that flow to at least one security class)
   * @param thispol A Policy
   * @return The Range of the policy.
   */
  public SortedSet<E> getRange(ConglomeratePolicy<E> thispol){
    SortedSet<E> result=new TreeSet<>();
    for(Edge<E> edge: thispol.getEdges()){
      for(E elem: edge.getTo()){
        result.add(elem);
      }
    }
    return result;
  }


}
