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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;

/**
 * Class for maintaining the variable dependencies of control flows
 */
public class BlockGuard implements Cloneable, Serializable{

  private static final long serialVersionUID = 7481418201286823199L;

  /**
   * Internal Stack: that contains relevant information to all active control flow
   */
  /*
   * Prototype:
   * contextStack(i)=((s,t),((expr,value),(newV,presentV))
   */
  private List<Pair<Pair<CFANode,CFANode>, Pair<Pair<CExpression,Boolean>,Pair<SortedSet<Variable>,SortedSet<Variable>>>>> contextStack=new ArrayList<>();


    /**
     * Constructs an empty BlockGuard
     */
    public BlockGuard(){

    }

    /**
     * Constructs an BlockGuard with the given <i>pContextstack</i>
     * @param pContextStack stack that contains relevant information to all active control flow
     */
    private BlockGuard(List<Pair<Pair<CFANode,CFANode>, Pair<Pair<CExpression,Boolean>,Pair<SortedSet<Variable>,SortedSet<Variable>>>>> pContextStack){
      this.contextStack=pContextStack;
    }

    @Override
    public BlockGuard clone(){
      try {
        super.clone();
      } catch (CloneNotSupportedException e) {
        //    logger.logUserException(Level.WARNING, e, "");
      }

      /*
       * Copy internal structure
       */
      List<Pair<Pair<CFANode,CFANode>, Pair< Pair<CExpression,Boolean>,Pair<SortedSet<Variable>,SortedSet<Variable>>>>> resultContextStack=new ArrayList<>();
      for(int i=0;i<contextStack.size();i++){

        /*
         * contextStack(i)=((s,t),((expr,value),(newV,presentV))
         */
         Pair<Pair<CFANode, CFANode>, Pair<Pair<CExpression, Boolean>, Pair<SortedSet<Variable>, SortedSet<Variable>>>> contextElem =
            contextStack.get(i);

        /*
         * Edge (s,t)
         */
        CFANode source=contextElem.getFirst().getFirst();
        CFANode target=contextElem.getFirst().getSecond();

        /*
         * (expr, value)
         *
         * I(expr)=value
         */
        CExpression expr=contextElem.getSecond().getFirst().getFirst();
        boolean value=contextElem.getSecond().getFirst().getSecond().booleanValue();

        /*
         * contextDep=(newV,presentV)
         */
        SortedSet<Variable> contextDeps_new=contextElem.getSecond().getSecond().getFirst();
        SortedSet<Variable> contextDeps_present=contextElem.getSecond().getSecond().getSecond();

        /*
         * resultContextDep=contextDep
         */
        SortedSet<Variable> resultContextDeps_first=new TreeSet<>();
        for(Variable contextDep: contextDeps_new){
          resultContextDeps_first.add(contextDep);
        }
        SortedSet<Variable> resultContextDeps_second=new TreeSet<>();
        for(Variable contextDep: contextDeps_present){
          resultContextDeps_second.add(contextDep);
        }
        Pair<SortedSet<Variable>,SortedSet<Variable>> resultContextDeps = new Pair<>(resultContextDeps_first,resultContextDeps_second);


        /*
         * resultContextStack(i)=((s,t),((expr,value),(newV,presentV))
         */
        Pair<CExpression,Boolean> resultExprEdge=new Pair<>(expr,value);
        Pair<Pair<CExpression,Boolean>,Pair<SortedSet<Variable>,SortedSet<Variable>>> sec= new Pair<>(resultExprEdge,resultContextDeps);
        Pair<CFANode,CFANode> first=new Pair<> (source,target);
        Pair<Pair<CFANode,CFANode>, Pair<Pair<CExpression,Boolean>,Pair<SortedSet<Variable>,SortedSet<Variable>>>> resultContext=new Pair<> (first, sec);
        resultContextStack.add(resultContext);
      }

      /*
       * Create Copy
       */
      BlockGuard result=new BlockGuard(resultContextStack);
      return result;
    }

    /**
     * Puts a new Control Dependency on the topelement of the Stack
     * @param pSource Source of the Edge
     * @param pTarget Sink of the Edge
     * @param pExpression Expression of the control branch
     * @param pValue Truth value of the expression if the branch is taken
     */
  public void
      addDependancy(CFANode pSource, CFANode pTarget, CExpression pExpression, boolean pValue)
          throws UnsupportedCodeException {
      /*
       * Add resultContext=((pSource,pTarget),((pExpression,pValue),(resultDeps_new,resultDeps_present))
       *
       * with
       * resultDeps_new=fv(expr)
       * resultDeps_present= contextDeps_new(top) union contextDeps_present(top)
       *
       */

      VariableDependancy visitor=new VariableDependancy();
      pExpression.accept(visitor);

      /*
       * resultDeps_new=fv(expr)
       */
      SortedSet<Variable> resultDeps_new=visitor.getResult();
      SortedSet<Variable> resultDeps_present=new TreeSet<>();

      int size=contextStack.size();
      if(size>0){

        /*
         * contextDeps(top)
         */
        Pair<SortedSet<Variable>,SortedSet<Variable>> contextDeps=contextStack.get(size-1).getSecond().getSecond();
        SortedSet<Variable> contextDeps_first = contextDeps.getFirst();
        SortedSet<Variable> contextDeps_second = contextDeps.getSecond();

        /*
         * resultDeps_present= contextDeps_new(top) union contextDeps_present(top)
         */
        for(Variable contextDep: contextDeps_first){
          resultDeps_present.add(contextDep);
        }
        for(Variable contextDep: contextDeps_second){
          resultDeps_present.add(contextDep);
        }
      }

      /*
       * resultDeps=(resultDeps_new,resultDeps_present)
       */
      Pair<SortedSet<Variable>,SortedSet<Variable>> resultDeps=new Pair<>(resultDeps_new,resultDeps_present);

      /*
       * resultContext=((pSource,pTarget),((pExpression,pValue),(resultDeps_new,resultDeps_present))
       */
      Pair<CExpression,Boolean> resultExprEdge=new Pair<>(pExpression,pValue);
      Pair<Pair<CExpression,Boolean>,Pair<SortedSet<Variable>,SortedSet<Variable>>> sec= new Pair<>(resultExprEdge,resultDeps);
      Pair<CFANode,CFANode> first=new Pair<>(pSource,pTarget);
      Pair<Pair<CFANode,CFANode>, Pair<Pair<CExpression,Boolean>,Pair<SortedSet<Variable>,SortedSet<Variable>>>> resultContext=new Pair<> (first, sec);

      /*
       * Add resultContext
       */
      contextStack.add(resultContext);
    }

   /**
    *
    * Removes all Control Dependency information from the context stack that does not influence <i>pCurrentNode</i>.
    * @param pCurrentNode Target-Node that should be refined
    * @param pDominators Dominators of <i>pCurrentNode</i>.
    */
   public void refineContextStack(CFANode pCurrentNode, TreeSet<CFANode> pDominators){
     /*
      * Removes control dependencies contextStack(i) that are not valid anymore
      *
      * Remove contextStack(i) from contextStack
      * iff
      * s(i) not in pDominators
      *
      * with
      * contextStack(i)=((s,t),((expr,value),(newV,presentV))
      */
     int size=contextStack.size();
     for(int i=size;i>0;i--){
       Pair<Pair<CFANode, CFANode>, Pair<Pair<CExpression, Boolean>, Pair<SortedSet<Variable>,SortedSet<Variable>>>> contextStack_i=contextStack.get(i-1);
       CFANode source=contextStack_i.getFirst().getFirst();
       if(!(pDominators.contains(source))){
         contextStack.remove(i-1);
       }
     }
   }

   /**
    * Return the current number of Control Dependencies on the Stack.
    * @return The current number of Control Dependencies on the Stack.
    */
   public int getSize(){
     return contextStack.size();
   }

   public Pair<CFANode,CFANode> getTopEdge(){
     int size=contextStack.size();
     if(size>0){
       Pair<Pair<CFANode, CFANode>, Pair<Pair<CExpression, Boolean>, Pair<SortedSet<Variable>,SortedSet<Variable>>>> contextStack_top=contextStack.get(size-1);
       return contextStack_top.getFirst();
     }
     return null;
   }

   /**
    * Returns the Variables that influence the expression of top Control Dependency of the stack.
    * @return The Variables that influence the expression of top Control Dependency of the stack.
    */
   public SortedSet<Variable> getTopVariables(){
     /*
      * resultDeps= contextDeps_new(top) union contextDeps_present(top)
      */
     SortedSet<Variable> resultDeps=new TreeSet<>();
     resultDeps.addAll(getTopVariables_new());
     resultDeps.addAll(getTopVariables_present());
     return resultDeps;
   }

   /**
    * Returns the Variables that influence the expression of top Control Dependency of the stack.
    * @return The Variables that influence the expression of top Control Dependency of the stack.
    */
   public SortedSet<Variable> getTopVariables_new(){
     int index=contextStack.size()-1;
     SortedSet<Variable> tmp=new TreeSet<>();
     if(index>=0){
       tmp=contextStack.get(index).getSecond().getSecond().getFirst();
     }
     return tmp;
   }

   /**
    * Returns the Variables that influence the expression of top Control Dependency of the stack.
    * @return The Variables that influence the expression of top Control Dependency of the stack.
    */
   public SortedSet<Variable> getTopVariables_present(){
     int index=contextStack.size()-1;
     SortedSet<Variable> tmp=new TreeSet<>();
     if(index>=0){
       tmp=contextStack.get(index).getSecond().getSecond().getSecond();
     }
     return tmp;
   }

   /**
    * Replace the set of the Variables that influence the expression of top Control Dependency of the stack by the set <i>vars</i>.
    * @param pVars Replacing set of Variables.
    */
   public void refineTopVariables_new(SortedSet<Variable> pVars){
     int index=contextStack.size()-1;
     if(index>=0){
       contextStack.get(index).getSecond().getSecond().setFirst(pVars);
     }
   }

   /**
    * Returns all Variable that influence the <i>index</i> expression of any Control Dependency of the stack.
    * @param pIndex Index of the Control Dependency
    * @return All Variable that influence the <i>index</i> expression of any Control Dependency of the stack.
    */
   public SortedSet<Variable> getVariables(int pIndex){
     SortedSet<Variable> result=new TreeSet<>();
     result.addAll(contextStack.get(pIndex).getSecond().getSecond().getFirst());
     result.addAll(contextStack.get(pIndex).getSecond().getSecond().getSecond());
     return result;
   }

   public SortedSet<Variable> getVariables_new(int pIndex){
     return contextStack.get(pIndex).getSecond().getSecond().getFirst();
   }

   public SortedSet<Variable> getVariables_present(int pIndex){
     return contextStack.get(pIndex).getSecond().getSecond().getSecond();
   }



   @Override
  public boolean equals(Object pOther){
     //1. Size same length
     //2. Elements the same
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

     if (this.contextStack.size()!=bother.contextStack.size()){
       return false;
     }
     for(int i=contextStack.size()-1;i>=0;i--){
       CFANode from=contextStack.get(i).getFirst().getFirst();
       CFANode from2=bother.contextStack.get(i).getFirst().getFirst();
       CFANode to=contextStack.get(i).getFirst().getSecond();
       CFANode to2=bother.contextStack.get(i).getFirst().getSecond();
       if(!((from.equals(from2)) && (to.equals(to2)))){
         value=false;
         break;
       }
     }
     return value;
  }

  public boolean isLessOrEqual(BlockGuard pOther){
    if (pOther == null) {
      return false;
    }
    if(this==pOther){
      return true;
    }
    // boolean value=true;

    int thisSize=this.contextStack.size();
    int otherSize=pOther.contextStack.size();
    if(thisSize>otherSize){
      return false;
    }
    for(int i=0;i<thisSize;i++){
      /*
       * take smallest substack that is contained in both
       */
      Pair<Pair<CFANode,CFANode>, Pair<Pair<CExpression,Boolean>, Pair<SortedSet<Variable>,SortedSet<Variable>>>> thisContext = this.contextStack.get(i);
      Pair<Pair<CFANode,CFANode>, Pair<Pair<CExpression,Boolean>, Pair<SortedSet<Variable>,SortedSet<Variable>>>> otherContext = pOther.contextStack.get(i);

      /*
       * Check
       *
       * thisEdge=otherEdge
       *
       */
      Pair<CFANode,CFANode> thisEdge=thisContext.getFirst();
      Pair<CFANode,CFANode> otherEdge=otherContext.getFirst();
      if(!(thisEdge.equals(otherEdge))){
        return false;
      }


      /*
       * Check
       *
       * thisExpr=otherExpr
       * and
       * thisValue=otherValue
       *
       */
      CExpression thisExpr=thisContext.getSecond().getFirst().getFirst();
      CExpression otherExpr=otherContext.getSecond().getFirst().getFirst();
      boolean thisValue=thisContext.getSecond().getFirst().getSecond();
      boolean otherValue=otherContext.getSecond().getFirst().getSecond();
      if(thisExpr==null || otherExpr == null || !(thisExpr.equals(otherExpr)) || !(thisValue==otherValue)){
        return false;
      }

      /*
       * Check
       *
       * thisExpr=otherExpr
       * and
       * thisValue=otherValue
       *
       */
      SortedSet<Variable> thisVariables_new=thisContext.getSecond().getSecond().getFirst();
      SortedSet<Variable> otherVariables_new=otherContext.getSecond().getSecond().getFirst();
      SortedSet<Variable> thisVariables_present=thisContext.getSecond().getSecond().getSecond();
      SortedSet<Variable> otherVariables_present=otherContext.getSecond().getSecond().getSecond();


          if (!(otherVariables_new.containsAll(
              thisVariables_new))) {
            return false;
            }

          if (!(otherVariables_present.containsAll(
              thisVariables_present))) {
            return false;
            }
    }
    return true;


  }


   /**
    * Combines two BlockGuards by intersection.
    * @param pOther the other Blockguard.
    * @return A new BlockGuard that is the intersection of both BlockGuards.
    */
  public BlockGuard meet(BlockGuard pOther) throws UnsupportedCodeException {
     BlockGuard result=new BlockGuard();

     int thisSize=this.contextStack.size();
     int otherSize=pOther.contextStack.size();
     for(int i=0;i<((thisSize<otherSize)?thisSize:otherSize);i++){
       /*
        * take smallest substack that is contained in both
        */
       Pair<Pair<CFANode,CFANode>, Pair<Pair<CExpression,Boolean>, Pair<SortedSet<Variable>,SortedSet<Variable>>>> thisContext = this.contextStack.get(i);
       Pair<Pair<CFANode,CFANode>, Pair<Pair<CExpression,Boolean>, Pair<SortedSet<Variable>,SortedSet<Variable>>>> otherContext = pOther.contextStack.get(i);

       /*
        * Check
        *
        * thisEdge=otherEdge
        *
        */
       Pair<CFANode,CFANode> thisEdge=thisContext.getFirst();
       Pair<CFANode,CFANode> otherEdge=otherContext.getFirst();
       if(!(thisEdge.equals(otherEdge))){
         return result;
       }

       /*
        * Check
        *
        * thisExpr=otherExpr
        * and
        * thisValue=otherValue
        *
        */
       CExpression thisExpr=thisContext.getSecond().getFirst().getFirst();
       CExpression otherExpr=otherContext.getSecond().getFirst().getFirst();
       boolean thisValue=thisContext.getSecond().getFirst().getSecond();
       boolean otherValue=otherContext.getSecond().getFirst().getSecond();
       if(thisExpr==null || otherExpr == null || !(thisExpr.equals(otherExpr)) || !(thisValue==otherValue)){
         return result;
       }

       /*
        * Check
        *
        * thisExpr=otherExpr
        * and
        * thisValue=otherValue
        *
        */
       SortedSet<Variable> thisVariables_new=thisContext.getSecond().getSecond().getFirst();
       SortedSet<Variable> otherVariables_new=otherContext.getSecond().getSecond().getFirst();
       SortedSet<Variable> thisVariables_present=thisContext.getSecond().getSecond().getSecond();
       SortedSet<Variable> otherVariables_present=otherContext.getSecond().getSecond().getSecond();
       if(!(thisVariables_new.equals(otherVariables_new)) || !(thisVariables_present.equals(otherVariables_present))){
         return result;
       }

       /*
        * Keep Dependency
        */
       result.addDependancy(thisEdge.getFirst(), thisEdge.getSecond(), thisExpr, thisValue);
     }
     return result;
   }

    @Override
    public String toString(){
      return contextStack.toString();
    }

   @Override
    public int hashCode() {
      // TODO Auto-generated method stub
      return super.hashCode();
    }



}
