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
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
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
import org.sosy_lab.cpachecker.cpa.ifcsecurity.dependencytracking.Variable;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;


/**
 * CPA-Transfer-Relation for enforcing a Security Policy
 */
@Options(prefix="cpa.ifcsecurity")
public class PolicyEnforcementRelation<E extends Comparable<? super E>> extends ForwardingTransferRelation<PolicyEnforcementState<E>, PolicyEnforcementState<E>, Precision>  {

  @SuppressWarnings("unused")
  private LogManager logger;
  @SuppressWarnings("unused")
  private final ShutdownNotifier shutdownNotifier;

  /**
   * Internal Variable: Signalize whether at every state all Variables/Functions should be checked for a Security Violation or only at the end <br>
   * <b> 0</b> if at every state <br>
   * <b> 1</b> if only at end states
   */
  //0 all, 1 end
  @Option(secure=true, name="statestocheck", toUppercase=true, description="which states shall be checked")
  private int statestocheck=0;

  /**
   * Constructor for Transfer-Relation of ControlDependencyTracker
   * @param pLogger Logger
   * @param pShutdownNotifier ShutdownNotifier
   * @param statestocheck Signalize whether at every state all Variables/Functions should be checked for a Security Violation or only at the end
   */
  public PolicyEnforcementRelation(LogManager pLogger, ShutdownNotifier pShutdownNotifier,
      int statestocheck, Configuration pConfig) throws InvalidConfigurationException {
    pConfig.inject(this);
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    this.statestocheck=statestocheck;
  }

  @Override
  protected PolicyEnforcementState<E> handleAssumption(CAssumeEdge pCfaEdge, CExpression pExpression, boolean pTruthAssumption)
      throws CPATransferException {
    return state.clone();
  }

  @Override
  protected PolicyEnforcementState<E> handleFunctionCallEdge(CFunctionCallEdge pCfaEdge,
      List<CExpression> pArguments, List<CParameterDeclaration> pParameters,
      String pCalledFunctionName) throws CPATransferException {
    return state.clone();
  }

  @Override
  protected PolicyEnforcementState<E> handleFunctionReturnEdge(CFunctionReturnEdge pCfaEdge,
      CFunctionSummaryEdge pFnkCall, CFunctionCall pSummaryExpr, String pCallerFunctionName)
          throws CPATransferException {
    return state.clone();
  }

  @Override
  protected PolicyEnforcementState<E> handleDeclarationEdge(CDeclarationEdge pCfaEdge, CDeclaration pDecl)
      throws CPATransferException {
    PolicyEnforcementState<E> result=state.clone();
    //Handle Variable-Declarations
    if (pDecl instanceof CVariableDeclaration) {
      CVariableDeclaration dec = (CVariableDeclaration) pDecl;
      String left = dec.getQualifiedName();
      Variable var=new Variable(left);
      //Treat global Variables different from lokal Variables
      if(pDecl.isGlobal()){
        //Add if not in allowedsecurityclassmapping
        if(!(result.getAllowedsecurityclassmapping().containsKey(var))){
          result.getAllowedsecurityclassmapping().put(var,result.getDefaultlevel());
          SortedSet<E> his=new TreeSet<>();
          his.add(result.getDefaultlevel());
          result.getContentsecurityclasslevels().put(var, his);
        }
        assert(result.getAllowedsecurityclassmapping().containsKey(var));
        result.getIsglobal().put(var,true);
      }
      else{
        result.getAllowedsecurityclassmapping().put(var,result.getDefaultlevel());
        SortedSet<E> his=new TreeSet<>();
        his.add(result.getDefaultlevel());
        result.getContentsecurityclasslevels().put(var, his);
        result.getIsglobal().put(var,false);
      }
    }

    //Handle Function-Declaration
    if (pDecl instanceof CFunctionDeclaration) {
      CFunctionDeclaration dec = (CFunctionDeclaration) pDecl;
      //Add Function Parameter
      List<CParameterDeclaration> param = dec.getParameters();
      for(CParameterDeclaration par:param){
        String name=par.getQualifiedName();
        Variable var=new Variable(name);
        //Add if not in allowedsecurityclassmapping
        if(!(result.getAllowedsecurityclassmapping().containsKey(var))){
          result.getAllowedsecurityclassmapping().put(var,result.getDefaultlevel());
          SortedSet<E> his=new TreeSet<>();
          his.add(result.getDefaultlevel());
          result.getContentsecurityclasslevels().put(var, his);
          result.getIsglobal().put(var,false);
        }
      }
      //Add Functionname
      String fname=dec.getQualifiedName();
      Variable fvar=new Variable(fname);
      //Add if not in allowedsecurityclassmapping
      if(!(result.getAllowedsecurityclassmapping().containsKey(fvar))){
        result.getAllowedsecurityclassmapping().put(fvar,result.getDefaultlevel());
        SortedSet<E> his=new TreeSet<>();
        his.add(result.getDefaultlevel());
        result.getContentsecurityclasslevels().put(fvar, his);
        result.getIsglobal().put(fvar,false);
      }
    }


    return result;
  }

  @Override
  protected PolicyEnforcementState<E> handleStatementEdge(CStatementEdge pCfaEdge, CStatement pStatement)
      throws CPATransferException {
    return state.clone();
  }

  @Override
  protected PolicyEnforcementState<E> handleReturnStatementEdge(CReturnStatementEdge pCfaEdge)
      throws CPATransferException {
    return state.clone();
  }

  @Override
  @SuppressWarnings("unchecked")
  protected PolicyEnforcementState<E> handleBlankEdge(BlankEdge pCfaEdge) {
    return state.clone();
  }

  @Override
  protected PolicyEnforcementState<E> handleFunctionSummaryEdge(CFunctionSummaryEdge pCfaEdge) throws CPATransferException {
    return state.clone();
  }


  @Override
  public Collection<? extends AbstractState> strengthen(AbstractState pState, List<AbstractState> pOtherStates,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException, InterruptedException {
    assert(pState instanceof PolicyEnforcementState);
    @SuppressWarnings("unchecked")
    PolicyEnforcementState<E> state=((PolicyEnforcementState<E>) pState);

    for(AbstractState aState: pOtherStates){

      //DependencyTrackerCPA strengthens the Security Classes contained in a variable/function
      if(aState instanceof DependencyTrackerState){

        DependencyTrackerState otherstate=(DependencyTrackerState)aState;
        Map<Variable, SortedSet<Variable>> deps = otherstate.getDependencies();

        for(Entry<Variable, SortedSet<Variable>> entry: deps.entrySet()){
          Variable var=entry.getKey();
          SortedSet<E> history = new TreeSet<>();
          //Reflexivity
          history.add(state.getAllowedsecurityclassmapping().get(var));
          SortedSet<Variable> vardep=deps.get(var);
          //Add Dependancies to history
          for(Variable d: vardep){
            history.add(state.getAllowedsecurityclassmapping().get(d));
          }
          state.getContentsecurityclasslevels().put(var, history);
        }
      }
      //Set if the state should be checked for Security Violation
      if(aState instanceof LocationState){
        if(statestocheck==1){
          LocationState otherstate=(LocationState)aState;
          if(otherstate.getLocationNode().getNumLeavingEdges()!=0){
            state.setCheckthis(false);
          }
        }
      }

    }
    return Collections.singleton(state);
  }

}
