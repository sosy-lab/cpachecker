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
package org.sosy_lab.cpachecker.cpa.ifcsecurity.dependencytracking;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCCodeException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Class for maintaining the variable dependencies of control flows
 */
public class BlockGuard implements Cloneable, Serializable{

  private static final long serialVersionUID = 7481418201286823199L;

  /**
   * Internal Stack: that contains relevant information to all active control flow
   */
  private List<Pair<Pair<CFANode,CFANode>, Pair<Pair<CExpression,Boolean>,SortedSet<Variable>>>> contextstack=new ArrayList<>();


    /**
     * Constructs an empty BlockGuard
     */
    public BlockGuard(){

    }

    /**
     * Constructs an BlockGuard with the given <i>pContextstack</i>
     * @param pContextstack stack that contains relevant information to all active control flow
     */
    private BlockGuard(List<Pair<Pair<CFANode,CFANode>, Pair<Pair<CExpression,Boolean>,SortedSet<Variable>>>> pContextstack){
      this.contextstack=pContextstack;
    }

    @Override
    public BlockGuard clone(){
      try {
        super.clone();
      } catch (CloneNotSupportedException e) {
        //    logger.logUserException(Level.WARNING, e, "");
      }
      //Copy internal structure
      List<Pair<Pair<CFANode,CFANode>, Pair< Pair<CExpression,Boolean>,SortedSet<Variable>>>> tmpallcontexts=new ArrayList<>();
      for(int i=0;i<contextstack.size();i++){
        Pair<Pair<CFANode,CFANode>, Pair<Pair<CExpression,Boolean>, SortedSet<Variable>>> value = contextstack.get(i);
        CFANode node=value.first.first;
        CFANode node2=value.first.second;
        CExpression expr=value.second.first.first;
        boolean truth=value.second.first.second.booleanValue();
        SortedSet<Variable> scs=value.second.second;
        SortedSet<Variable> secsec=new TreeSet<>();
        for(Variable sc: scs){
          secsec.add(sc);
        }

        Pair<CExpression,Boolean> secfirst=new Pair<>(expr,truth);
        Pair<Pair<CExpression,Boolean>,SortedSet<Variable>> sec= new Pair<>(secfirst,secsec);
        Pair<CFANode,CFANode> first=new Pair<> (node,node2);
        Pair<Pair<CFANode,CFANode>, Pair<Pair<CExpression,Boolean>,SortedSet<Variable>>> elem=new Pair<> (first, sec);
        tmpallcontexts.add(elem);
      }
      //create copy
      BlockGuard tmp=new BlockGuard(tmpallcontexts);
      return tmp;
    }

    /**
     * Puts a new Control Dependency on the Stack
     * @param pNodeV Source of the Edge
     * @param pNodeW Sink of the Edge
     * @param pExpression Expression of the control branch
     * @param pValue Truth value of the expression if the branch is taken
     */
    public void addDependancy(CFANode pNodeV, CFANode pNodeW, CExpression pExpression, boolean pValue) throws UnsupportedCCodeException{
      VariableDependancy visitor=new VariableDependancy();
      pExpression.accept(visitor);
      SortedSet<Variable> varl=visitor.getResult();

      int size=contextstack.size();
      if(size>0){
        SortedSet<Variable> end=contextstack.get(size-1).second.second;
        for(Variable var: end){
          varl.add(var);
        }
      }

      Pair<CExpression,Boolean> secfirst=new Pair<>(pExpression,pValue);
      Pair<Pair<CExpression,Boolean>,SortedSet<Variable>> sec= new Pair<>(secfirst,varl);
      Pair<CFANode,CFANode> first=new Pair<>(pNodeV,pNodeW);
      Pair<Pair<CFANode,CFANode>, Pair<Pair<CExpression,Boolean>,SortedSet<Variable>>> elem=new Pair<> (first, sec);
      contextstack.add(elem);
    }

   /**
    * Removes all Control Dependency information from the context stack that does not influence <i>currentNode</i>.
    * @param pCurrentNode Node for which controlDependencies that stacks should be reduced to.
    * @param pDependencies Control Dependencies of <i>currentNode</i>.
    */
   public void changeContextStack(CFANode pCurrentNode, TreeSet<CFANode> pDependencies){
     int size=contextstack.size();
     for(int i=size;i>0;i--){
       Pair<Pair<CFANode, CFANode>, Pair<Pair<CExpression, Boolean>, SortedSet<Variable>>> elem=contextstack.get(i-1);
       CFANode first=elem.first.first;
       if(!(pDependencies.contains(first))){
         contextstack.remove(i-1);
       }
     }
   }

   /**
    * Return the current number of Control Dependencies on the Stack.
    * @return The current number of Control Dependencies on the Stack.
    */
   public int getSize(){
     return contextstack.size();
   }

   /**
    * Returns the Variables that influence the expression of top Control Dependency of the stack.
    * @return The Variables that influence the expression of top Control Dependency of the stack.
    */
   public SortedSet<Variable> getTopVariables(){
     int index=contextstack.size()-1;
     SortedSet<Variable> tmp;
     if(index>=0){
       tmp=contextstack.get(index).second.second;
     }
     else{
       tmp=new TreeSet<>();
     }
     return tmp;
   }

   /**
    * Replace the set of the Variables that influence the expression of top Control Dependency of the stack by the set <i>vars</i>.
    * @param pVars Replacing set of Variables.
    */
   public void changeTopVariables(SortedSet<Variable> pVars){
     int index=contextstack.size()-1;
     if(index>=0){
       contextstack.get(index).second.second=pVars;
     }
   }

   /**
    * Returns all Variable that influence the <i>index</i> expression of any Control Dependency of the stack.
    * @param pIndex Index of the Control Dependency
    * @return All Variable that influence the <i>index</i> expression of any Control Dependency of the stack.
    */
   public SortedSet<Variable> getVariables(int pIndex){
     SortedSet<Variable> tmp=contextstack.get(pIndex).second.second;
     return tmp;
   }

   @Override
  public boolean equals(Object pOther){
     if(pOther==null){
       return false;
     }
     if(this==pOther){
       return true;
     }
     if (!(pOther instanceof BlockGuard)) {
       return false;
     }
     BlockGuard bother=(BlockGuard) pOther;
     boolean value=true;

     for(int i=contextstack.size()-1;i>=0;i++){
       int index=contextstack.size()-1;
       int index2=bother.contextstack.size()-1;
       if(!(index==index2)){
         return false;
       }
       CFANode from=contextstack.get(index).first.first;
       CFANode from2=bother.contextstack.get(index).first.first;
       CFANode to=contextstack.get(index).first.second;
       CFANode to2=bother.contextstack.get(index).first.second;
       if(!((from.equals(from2)) && (to.equals(to2)))){
         value=false;
         break;
       }
     }
     return value;
  }

   /**
    * Combines two BlockGuards by intersection.
    * @param pOther the other Blockguard.
    * @return A new BlockGuard that is the intersection of both BlockGuards.
    */
   public BlockGuard meet(BlockGuard pOther) throws UnsupportedCCodeException{
     BlockGuard result=this.clone();


     int size1=this.contextstack.size();
     int size2=pOther.contextstack.size();
     for(int i=0;i<((size1<size2)?size1:size2);i++){
       Pair<Pair<CFANode,CFANode>, Pair<Pair<CExpression,Boolean>, SortedSet<Variable>>> value = this.contextstack.get(i);
       Pair<Pair<CFANode,CFANode>, Pair<Pair<CExpression,Boolean>, SortedSet<Variable>>> othervalue = pOther.contextstack.get(i);

       Pair<CFANode,CFANode> edge=value.first;
       Pair<CFANode,CFANode> otheredge=othervalue.first;
       CFANode node=value.first.first;
       CFANode node2=value.first.second;
//       CFANode othernode=othervalue.first.first;
//       CFANode othernode2=othervalue.first.second;

       if(!(edge.equals(otheredge))){
         return result;
       }

       CExpression expr=value.second.first.first;
       CExpression otherexpr=othervalue.second.first.first;
       boolean truth=value.second.first.second;
       boolean othertruth=othervalue.second.first.second;
//       if(expr==null || otherexpr==null){
         if(expr==null || otherexpr == null || !(expr.equals(otherexpr)) || !(truth==othertruth)){
           return result;
         }
//         return result;
//       }


       SortedSet<Variable> variables=value.second.second;
       SortedSet<Variable> othervariables=othervalue.second.second;


       if(!(variables.equals(othervariables))){
         return result;
       }
       result.addDependancy(node, node2, expr, truth);
     }
     return result;
   }

    @Override
    public String toString(){
      return contextstack.toString();
    }

   @Override
    public int hashCode() {
      // TODO Auto-generated method stub
      return super.hashCode();
    }

  /**
    * Internal Class for Representing a 2-Pair
    * @param <T> Type of the first element
    * @param <E> Type of the second element
    */
  static class Pair<T,E> implements Serializable{

    private static final long serialVersionUID = 921854014515969561L;
    /**
     * first element
     */
    private T first;
    /**
     * second element
     */
    private E second;

    /**
     * Generates a new 2-Pair
     * @param first The first element of the 2-Pair.
     * @param second The second element of the 2-Pair.
     */
    public Pair(T first, E second){
      this.first=first;
      this.second=second;
    }

    /**
     * Returns the first element of the 2-Pair.
     * @return The first element of the 2-Pair.
     */
    public T getFirst(){
      return first;
    }

    /**
     * Returns the second element of the 2-Pair.
     * @return The second element of the 2-Pair.
     */
    public E getSecond(){
      return second;
    }

    @Override
    public boolean equals(Object obj){
      if(obj == null){
        return false;
      }
      if(!(obj instanceof Pair)){
        return false;
      }
      @SuppressWarnings("unchecked")
      Pair<T,E> other=(Pair<T,E>) obj;
      return (first.equals(other.first) && second.equals(other.second));
    }

    @Override
    public String toString(){
      return "["+((first==null)?"Null":((first instanceof CExpression)?((CExpression)first).toASTString() :first.toString()))+","+((second==null)?"Null":((second instanceof CExpression)?((CExpression)second).toASTString():(second.toString())))+"]";
    }

    @Override
    public int hashCode() {
      // TODO Auto-generated method stub
      return super.hashCode();
    }
  }

}
