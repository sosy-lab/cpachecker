/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.ifcsecurity;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.dependencytracking.FunctionCallStatementDependancy;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.dependencytracking.Variable;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.dependencytracking.VariableDependancy;
import org.sosy_lab.cpachecker.cpa.pointer2.PointerState;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSet;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;

/**
 * CPA-Transfer-Relation for tracking which variables/functions are depends on which other variables/functions
 */
public class DependencyTrackerRelation extends ForwardingTransferRelation<DependencyTrackerState, DependencyTrackerState, Precision>  {

  @SuppressWarnings("unused")
  private LogManager logger;
  @SuppressWarnings("unused")
  private final ShutdownNotifier shutdownNotifier;

  public DependencyTrackerRelation(LogManager pLogger, ShutdownNotifier pShutdownNotifier) {
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
  }

  @Override
  protected DependencyTrackerState handleAssumption(CAssumeEdge pCfaEdge, CExpression pExpression, boolean pTruthAssumption)
      throws CPATransferException {
    return state.clone();
  }

  @Override
  protected DependencyTrackerState handleFunctionCallEdge(CFunctionCallEdge pCfaEdge,
      List<CExpression> pArguments, List<CParameterDeclaration> pParameters,
      String pCalledFunctionName) throws CPATransferException {

      /*
       * Function-Parameter-Dependencies on a call
       */
      DependencyTrackerState result=state.clone();
      for(int i=0;i<pParameters.size();i++){
        CParameterDeclaration par=pParameters.get(i);
        String name=par.getQualifiedName();
        Variable pvar=new Variable(name);
        SortedSet<Variable> deps=new TreeSet<>();

        CExpression expr=pArguments.get(i);
        VariableDependancy visitor = new VariableDependancy();
        expr.accept(visitor);
        SortedSet<Variable> vars=visitor.getResult();
        for(Variable var: vars){
            assert(state.getDependencies().containsKey(pvar));
            for(Variable c: state.getDependencies().get(var)){
              deps.add(c);
            }
        }
        result.getDependencies().put(pvar, deps);
      }
      return result;
  }

  @Override
  protected DependencyTrackerState handleFunctionReturnEdge(CFunctionReturnEdge pCfaEdge,
      CFunctionSummaryEdge pFnkCall, CFunctionCall pSummaryExpr, String pCallerFunctionName)
          throws CPATransferException {

    /*
     * Using of Function-Dependencies on a call return
     */
    DependencyTrackerState result=state.clone();
    Variable function=new Variable(functionName);
    if (pSummaryExpr instanceof CFunctionCallAssignmentStatement) {
      /*
       * Function-Assignment
       * -l=func(x);
       * -l depends on the dependencies of func
       */

      //Get dependency of function
      CFunctionCallAssignmentStatement funcExp = (CFunctionCallAssignmentStatement)pSummaryExpr;
      CExpression left=funcExp.getLeftHandSide();
      VariableDependancy visitor=new VariableDependancy();
      left.accept(visitor);
      SortedSet<Variable> varl=visitor.getResult();
      SortedSet<Variable> deps=new TreeSet<>();
      assert(state.getDependencies().containsKey(function));
      for(Variable c: state.getDependencies().get(function)){
        deps.add(c);
      }

      //Set dependency of variable
      for(Variable l: varl){
        result.getDependencies().put(l,deps);
      }
    } else if (pSummaryExpr instanceof CFunctionCallStatement) {
      /*
       * Function-Call-Statement
       * -func(x);
       * -nothing to do
       */

    } else {
      throw new UnrecognizedCCodeException("on function return", pCfaEdge, pSummaryExpr);
    }
    result.getDependencies().put(function, new TreeSet<Variable>());
    return result;
  }

  @Override
  protected DependencyTrackerState handleDeclarationEdge(CDeclarationEdge pCfaEdge, CDeclaration pDecl)
      throws CPATransferException {
    DependencyTrackerState result=state.clone();

    if (pDecl instanceof CVariableDeclaration) {
      /*
       * Variable Declaration
       */
      CVariableDeclaration dec = (CVariableDeclaration) pDecl;
      String left = dec.getQualifiedName();
      Variable var=new Variable(left);
      if(pDecl.isGlobal()){
        /*
         * Global Variable
         * -depends initially on itself
         */
        SortedSet<Variable> deps=new TreeSet<>();
        deps.add(var);
        result.getDependencies().put(var, deps);
      }
      else{
        /*
         * Local Variable
         * -depends initially on the dependencies of its initializer
         */
        SortedSet<Variable> deps=new TreeSet<>();
        if(dec.getInitializer()!=null){
          Variable tvar=new Variable(dec.getInitializer().toASTString());
          if(state.getDependencies().containsKey(tvar)){
            SortedSet<Variable> tdeps=state.getDependencies().get(tvar);
            for(Variable tdep:tdeps){
              deps.add(tdep);
            }
          }
        }
        result.getDependencies().put(var, deps);
      }
    }
    if (pDecl instanceof CFunctionDeclaration) {
      /*
       * Function Declaration
       */
      //Set Default-Dependancies of Return-Value
      CFunctionDeclaration dec = (CFunctionDeclaration) pDecl;
      String fname=dec.getQualifiedName();
      Variable fvar=new Variable(fname);
      SortedSet<Variable> fdeps=new TreeSet<>();
      result.getDependencies().put(fvar,fdeps);

      //Set Default-Dependancies of Parameter
      List<CParameterDeclaration> param = dec.getParameters();
      for(CParameterDeclaration par:param){
        String name=par.getQualifiedName();
        Variable var=new Variable(name);
        SortedSet<Variable> deps=new TreeSet<>();
        result.getDependencies().put(var, deps);
      }
    }

    return result;

  }

  @Override
  protected DependencyTrackerState handleStatementEdge(CStatementEdge pCfaEdge, CStatement pStatement)
      throws CPATransferException {
    DependencyTrackerState result=state.clone();
    CExpression left=null;
    CExpression right=null;
    if (pStatement instanceof CExpressionAssignmentStatement) {
      /*
       * ExpressionAssignmentStatement
       * -l=exp
       * -l depends on all variable/function dependencies in exp
       */
      left = ((CExpressionAssignmentStatement) pStatement).getLeftHandSide();
      right= ((CExpressionAssignmentStatement) pStatement).getRightHandSide();

      VariableDependancy visitor=new VariableDependancy();
      left.accept(visitor);
      SortedSet<Variable> varl=visitor.getResult();

      visitor=new VariableDependancy();
      right.accept(visitor);
      SortedSet<Variable> vars=visitor.getResult();

      for(Variable l: varl){
        assert(state.getDependencies().containsKey(l));
        SortedSet<Variable> deps=new TreeSet<>();
        for(Variable var: vars){
          assert(state.getDependencies().containsKey(var));
          for(Variable c: state.getDependencies().get(var)){
            deps.add(c);
          }
        }
        result.getDependencies().put(l, deps);
      }
    }
    if (pStatement instanceof CFunctionCallStatement) {
      /*
       * FunctionCallStatement
       * -send(par)
       * -send depends  on all variable/function dependencies in par
       */
      CFunctionCallStatement stmt = ((CFunctionCallStatement) pStatement);
      CFunctionCallExpression expr = stmt.getFunctionCallExpression();
      FunctionCallStatementDependancy visitor=new FunctionCallStatementDependancy();

       expr.accept(visitor);
       Variable function=visitor.getFunctionname();
       SortedSet<Variable> vars=visitor.getResult();

       assert(state.getDependencies().containsKey(function));
       SortedSet<Variable> deps=new TreeSet<>();
       for(Variable var: vars){
         assert(state.getDependencies().containsKey(var));
         for(Variable c: state.getDependencies().get(var)){
           deps.add(c);
         }
       }
       result.getDependencies().put(function, deps);
    }
    return result;
  }

  @Override
  protected DependencyTrackerState handleReturnStatementEdge(CReturnStatementEdge pCfaEdge)
      throws CPATransferException {
    //See Strengthen
    return state.clone();
  }

  @Override
  @SuppressWarnings("unchecked")
  protected DependencyTrackerState handleBlankEdge(BlankEdge pCfaEdge) {
    return state.clone();
  }

  @Override
  protected DependencyTrackerState handleFunctionSummaryEdge(CFunctionSummaryEdge pCfaEdge) throws CPATransferException {
    return state.clone();
  }

  @Override
  public Collection<? extends AbstractState> strengthen(AbstractState pState, List<AbstractState> pOtherStates,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException, InterruptedException {
    assert pState instanceof DependencyTrackerState;
    DependencyTrackerState state=(DependencyTrackerState)pState;


    /*
     * ControlDependancyTrackerState => Statement
     * if(h){
     *  l=3         Dep(l)=[h]
     * }
     *
     */
    strengthenExpressionAssignementStatement(state,pOtherStates,pCfaEdge,pPrecision);
    /*
     * ControlDependancyTrackerState => CFunctionCallAssignmentStatement
     * if(h){
     *  l=func(s)         Dep(l)=[h,Dep(func(s))]
     * }
     *
     */
    strengthenFunctionCallAssignmentStatement(state, pOtherStates, pCfaEdge, pPrecision);
    /*
     * ControlDependancyTrackerState => CFunctionCallStatement
     * if(h){
     *  func(s)         Dep(func)=[h,Dep(func(s))]
     * }
     *
     */
    strengthenFunctionCallStatement(state, pOtherStates, pCfaEdge, pPrecision);
    /*
     * CallstackState => CReturnStatementEdge
     * func(s){
     *  return val;       Dep(func)=[Dep(val)]
     * }
     */
    strengthenReturnStatementEdge(state, pOtherStates, pCfaEdge, pPrecision);
    //PointerCPA
    for(AbstractState astate: pOtherStates){
      if(astate instanceof PointerState){
        PointerState pointerstate=(PointerState)astate;
        Map<MemoryLocation, LocationSet>  map = pointerstate.getPointsToMap();
        for(Entry<MemoryLocation, LocationSet> memloc: map.entrySet()){
          String varname=memloc.getKey().getIdentifier();
          Variable var=new Variable(varname);
          LocationSet lset=memloc.getValue();
          for(Variable var2: state.getDependencies().keySet()){
            if(lset.mayPointTo(MemoryLocation.valueOf(var2.toString()))){
              SortedSet<Variable> varset1=state.getDependencies().get(var);
              SortedSet<Variable> varset2=state.getDependencies().get(var2);
              if(!(varset1.equals(varset2))){
                SortedSet<Variable> newvarset1=new TreeSet<>(varset2);
                state.getDependencies().put(var, newvarset1);
              }
            }
          }
        }
      }
    }

    return Collections.singleton(pState);
  }

  public void strengthenExpressionAssignementStatement(DependencyTrackerState pState, List<AbstractState> pOtherStates,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException{
    logger.log(Level.FINE,pPrecision);

    /*
     * ControlDependancyTrackerState => Statement
     * if(h){
     *  l=3         Dep(l)=[h]
     * }
     *
     */
    if(pCfaEdge instanceof CStatementEdge && ((CStatementEdge)pCfaEdge).getStatement() instanceof CExpressionAssignmentStatement){
      for(AbstractState astate: pOtherStates){
        if(astate instanceof ControlDependencyTrackerState){
          ControlDependencyTrackerState ostate=(ControlDependencyTrackerState) astate;
          CExpression left= ((CExpressionAssignmentStatement) ((CStatementEdge)pCfaEdge).getStatement()).getLeftHandSide();
          VariableDependancy visitor=new VariableDependancy();
          left.accept(visitor);
          SortedSet<Variable> varl=visitor.getResult();
          for(Variable var: varl){
            SortedSet<Variable> cvars=pState.getDependencies().get(var);
            int size=ostate.getGuards().getSize();
            for(int i=0;i<size;i++){
              SortedSet<Variable> nvars = ostate.getGuards().getVariables(i);
              for(Variable nvar:nvars){
                cvars.add(nvar);
              }
            }
          }
         }
      }
    }
  }

  public void strengthenFunctionCallAssignmentStatement(DependencyTrackerState pState, List<AbstractState> pOtherStates,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException {
    logger.log(Level.FINE,pPrecision);

    /*
     * ControlDependancyTrackerState => CFunctionCallAssignmentStatement
     * if(h){
     *  l=func(s)         Dep(l)=[h,Dep(func(s))]
     * }
     *
     */
    if(pCfaEdge instanceof CFunctionReturnEdge && (((CFunctionReturnEdge)pCfaEdge).getSummaryEdge().getExpression() instanceof CFunctionCallAssignmentStatement)){
      //TODO ADD DEPENDANCIES TO VARIABLE function if (h) y=f(x)
      for(AbstractState astate: pOtherStates){
        if(astate instanceof ControlDependencyTrackerState){
          ControlDependencyTrackerState ostate=(ControlDependencyTrackerState) astate;
          CExpression left= ((CFunctionCallAssignmentStatement) ((CFunctionReturnEdge)pCfaEdge).getSummaryEdge().getExpression()).getLeftHandSide();
          VariableDependancy visitor=new VariableDependancy();
          left.accept(visitor);
          SortedSet<Variable> varl=visitor.getResult();
          for(Variable var: varl){
            SortedSet<Variable> cvars=pState.getDependencies().get(var);
            int size=ostate.getGuards().getSize();
            for(int i=0;i<size;i++){
              SortedSet<Variable> nvars = ostate.getGuards().getVariables(i);
              for(Variable nvar:nvars){
                cvars.add(nvar);
              }
            }
          }
         }
      }
    }
  }

  public void strengthenFunctionCallStatement(DependencyTrackerState pState, List<AbstractState> pOtherStates,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException {
    logger.log(Level.FINE,pPrecision);

    /*
     * ControlDependancyTrackerState => CFunctionCallStatement
     * if(h){
     *  func(s)         Dep(func)=[h,Dep(func(s))]
     * }
     *
     */
    if(pCfaEdge instanceof CStatementEdge && ((CStatementEdge)pCfaEdge).getStatement() instanceof CFunctionCallStatement){
      for(AbstractState astate: pOtherStates){
        if(astate instanceof ControlDependencyTrackerState){
          ControlDependencyTrackerState ostate=(ControlDependencyTrackerState) astate;
          CStatement statement= (((CStatementEdge)pCfaEdge).getStatement());
          CFunctionCallStatement stmt = ((CFunctionCallStatement) statement);
          CFunctionCallExpression expr = stmt.getFunctionCallExpression();
          FunctionCallStatementDependancy visitor=new FunctionCallStatementDependancy();
          expr.accept(visitor);
          Variable function=visitor.getFunctionname();

          SortedSet<Variable> cvars=pState.getDependencies().get(function);
          int size=ostate.getGuards().getSize();
          for(int i=0;i<size;i++){
            SortedSet<Variable> nvars = ostate.getGuards().getVariables(i);
            for(Variable nvar:nvars){
              cvars.add(nvar);
            }
          }
         }
      }
    }
  }

  public void strengthenReturnStatementEdge(DependencyTrackerState pState, List<AbstractState> pOtherStates,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException {
    logger.log(Level.FINE,pPrecision);
    /*
     * CallstackState => CReturnStatementEdge
     * func(s){
     *  return val;       Dep(func)=[Dep(val)]
     * }
     */
   if(pCfaEdge instanceof CReturnStatementEdge){
      CReturnStatementEdge edge=(CReturnStatementEdge) pCfaEdge;
      for(AbstractState astate: pOtherStates){
        if(astate instanceof CallstackState){
          CallstackState ostate=(CallstackState) astate;
          String function = ostate.getCurrentFunction();
          Variable fvar=new Variable(function);
          SortedSet<Variable> deps=new TreeSet<>();
          if(edge.getExpression().isPresent()){
            //return x;
            CExpression expr=edge.getExpression().get();
            VariableDependancy visitor=new VariableDependancy();
            expr.accept(visitor);
            SortedSet<Variable> vars=visitor.getResult();
            for(Variable var: vars){
              assert(pState.getDependencies().containsKey(var));
              for(Variable c: pState.getDependencies().get(var)){
                deps.add(c);
              }
            }
          }
          else{
            //return;
            //DO NOTHING
          }
          pState.getDependencies().put(fvar, deps);
        }
      }
   }
  }

}
