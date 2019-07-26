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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
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
import org.sosy_lab.cpachecker.cpa.ifcsecurity.dependencytracking.Pair;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.dependencytracking.Variable;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.dependencytracking.VariableDependancy;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.precision.DependencyPrecision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;



/**
 * CPA-Transfer-Relation for tracking the Active Control Dependencies
 */
public class ControlDependencyTrackerRelation extends ForwardingTransferRelation<ControlDependencyTrackerState, ControlDependencyTrackerState, Precision> {

  @SuppressWarnings("unused")
  private LogManager logger;
  @SuppressWarnings("unused")
  private final ShutdownNotifier shutdownNotifier;
  /**
   * Internal Variable: Control Dependencies
   */
  private Map<CFANode, TreeSet<CFANode>> rcd;

  /**
   * Constructor for Transfer-Relation of ControlDependencyTracker
   * @param pLogger Logger
   * @param pShutdownNotifier ShutdownNotifier
   * @param pRcd ControlDependencies
   */
  public ControlDependencyTrackerRelation(LogManager pLogger, ShutdownNotifier pShutdownNotifier, Map<CFANode, TreeSet<CFANode>> pRcd) {
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    rcd=pRcd;
  }

  public void addExpr(ControlDependencyTrackerState state, CFANode node, CExpression pExpression)
      throws UnsupportedCodeException {
    VariableDependancy visitor=new VariableDependancy();
    pExpression.accept(visitor);
    SortedSet<Variable> value=visitor.getResult();
    if(!state.getContexts().containsKey(node)){
      state.getContexts().put(node,new TreeSet<>());
    }
    state.getuRcontexts().put(node, value);
  }


  public void refine(ControlDependencyTrackerState state, TreeSet<CFANode> pDominators){
    for( Entry<CFANode, SortedSet<Variable>> entry: state.getuRcontexts().entrySet()){
      CFANode node=entry.getKey();
      if(!pDominators.contains(node)){
        state.getuRcontexts().put(node,new TreeSet<Variable>());
      }
    }
    for( Entry<CFANode, SortedSet<Variable>> entry: state.getContexts().entrySet()){
      CFANode node=entry.getKey();
      if(!pDominators.contains(node)){
        state.getContexts().put(node,new TreeSet<Variable>());
      }
    }
  }

  @Override
  protected ControlDependencyTrackerState handleAssumption(CAssumeEdge pCfaEdge, CExpression pExpression, boolean pTruthAssumption)
      throws CPATransferException {
    //Add a new Controldependency to track
    ControlDependencyTrackerState result=state.clone();
    CFANode from=pCfaEdge.getPredecessor();
    CFANode to=pCfaEdge.getSuccessor();
//    result.getContexts().addDependancy(from, to, pExpression, pTruthAssumption);
    addExpr(result,from,pExpression);

    TreeSet<CFANode> cd = rcd.get(to);
    refine(result,cd);

    return result;
  }

  @Override
  protected ControlDependencyTrackerState handleFunctionCallEdge(CFunctionCallEdge pCfaEdge,
      List<CExpression> pArguments, List<CParameterDeclaration> pParameters,
      String pCalledFunctionName) throws CPATransferException {
    ControlDependencyTrackerState result=state.clone();
    CFANode from=pCfaEdge.getPredecessor();
    CFANode to=pCfaEdge.getSuccessor();

    TreeSet<CFANode> cdbefore = rcd.get(from);
    TreeSet<CFANode> cd = rcd.get(to);

    if(cdbefore.containsAll(cd) && cd.containsAll(cdbefore)){
      return state;
    }

    refine(result,cd);



    return result;
  }

  @Override
  protected ControlDependencyTrackerState handleFunctionReturnEdge(CFunctionReturnEdge pCfaEdge,
      CFunctionSummaryEdge pFnkCall, CFunctionCall pSummaryExpr, String pCallerFunctionName)
          throws CPATransferException {
    ControlDependencyTrackerState result=state.clone();
    CFANode from=pCfaEdge.getPredecessor();
    CFANode to=pCfaEdge.getSuccessor();

    TreeSet<CFANode> cdbefore = rcd.get(from);
    TreeSet<CFANode> cd = rcd.get(to);

    if(cdbefore.containsAll(cd) && cd.containsAll(cdbefore)){
      return state;
    }

    refine(result,cd);



    return result;
  }

  @Override
  protected ControlDependencyTrackerState handleDeclarationEdge(CDeclarationEdge pCfaEdge, CDeclaration pDecl)
      throws CPATransferException {

    ControlDependencyTrackerState result=state.clone();
    CFANode from=pCfaEdge.getPredecessor();
    CFANode to=pCfaEdge.getSuccessor();

    TreeSet<CFANode> cdbefore = rcd.get(from);
    TreeSet<CFANode> cd = rcd.get(to);

    if(cdbefore.containsAll(cd) && cd.containsAll(cdbefore)){
      return state;
    }

    refine(result,cd);



    return result;
  }

  @Override
  protected ControlDependencyTrackerState handleStatementEdge(CStatementEdge pCfaEdge, CStatement pStatement)
      throws CPATransferException {
    ControlDependencyTrackerState result=state.clone();
    CFANode from=pCfaEdge.getPredecessor();
    CFANode to=pCfaEdge.getSuccessor();

    TreeSet<CFANode> cdbefore = rcd.get(from);
    TreeSet<CFANode> cd = rcd.get(to);

    if(cdbefore.containsAll(cd) && cd.containsAll(cdbefore)){
      return state;
    }

    refine(result,cd);



    return result;
  }

  @Override
  protected ControlDependencyTrackerState handleReturnStatementEdge(CReturnStatementEdge pCfaEdge)
      throws CPATransferException {
    ControlDependencyTrackerState result=state.clone();
    CFANode from=pCfaEdge.getPredecessor();
    CFANode to=pCfaEdge.getSuccessor();

    TreeSet<CFANode> cdbefore = rcd.get(from);
    TreeSet<CFANode> cd = rcd.get(to);

    if(cdbefore.containsAll(cd) && cd.containsAll(cdbefore)){
      return state;
    }

    refine(result,cd);



    return result;
  }

  @Override
  @SuppressWarnings("unchecked")
  protected ControlDependencyTrackerState handleBlankEdge(BlankEdge pCfaEdge) {
    //Clone for Merge reasons

    ControlDependencyTrackerState result=state.clone();
    CFANode from=pCfaEdge.getPredecessor();
    CFANode to=pCfaEdge.getSuccessor();

    TreeSet<CFANode> cdbefore = rcd.get(from);
    TreeSet<CFANode> cd = rcd.get(to);

    if(cdbefore.containsAll(cd) && cd.containsAll(cdbefore)){
      return state;
    }

    refine(result,cd);



    return result;
  }

  @Override
  protected ControlDependencyTrackerState handleFunctionSummaryEdge(CFunctionSummaryEdge pCfaEdge) throws CPATransferException {
    ControlDependencyTrackerState result=state.clone();
    CFANode from=pCfaEdge.getPredecessor();
    CFANode to=pCfaEdge.getSuccessor();

    TreeSet<CFANode> cdbefore = rcd.get(from);
    TreeSet<CFANode> cd = rcd.get(to);

    if(cdbefore.containsAll(cd) && cd.containsAll(cdbefore)){
      return state;
    }

    refine(result,cd);



    return result;
  }


  @Override
  public Collection<? extends AbstractState> strengthen(AbstractState pState, List<AbstractState> pOtherStates,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException, InterruptedException {
    assert pState instanceof ControlDependencyTrackerState;

    DependencyPrecision prec = (DependencyPrecision) pPrecision;
    ControlDependencyTrackerState state=(ControlDependencyTrackerState)pState;

    /*
     * Refine ContextStack
     * Remove unvalid Control Dependencies
     */
    CFANode currentNode=pCfaEdge.getSuccessor();
    CFANode preNode=pCfaEdge.getPredecessor();
    TreeSet<CFANode> cd = rcd.get(currentNode);

    refine(state,cd);

    if(pCfaEdge instanceof CAssumeEdge){
      /*
       *
       */
      Pair<CFANode, CFANode> currentEdge= new Pair<>(pCfaEdge.getPredecessor(), pCfaEdge.getSuccessor());

        for(AbstractState aState: pOtherStates){
          /*
           * DependencyTrackerCPA strengthens the information contained in Variables of a control branch
           */
          if(aState instanceof DependencyTrackerState){

            DependencyTrackerState dState=(DependencyTrackerState) aState;
            /*
             * Deps
             */
            Map<Variable, SortedSet<Variable>> deps = dState.getDependencies();

            SortedSet<Variable> context=state.getContexts().get(preNode);
            SortedSet<Variable> urContext=state.getuRcontexts().get(preNode);

            for(Variable var: urContext){
              if(deps.containsKey(var)){
                for(Variable dvar:deps.get(var)){
                  if(prec.isTracked(pCfaEdge.getSuccessor(),var,dvar)){
                   context.add(dvar);
                  }
                }
              }
            else{
              if(prec.isTracked(pCfaEdge.getSuccessor(),var,var)){
                context.add(var);
              }
            }
            }
            state.getuRcontexts().put(preNode,new TreeSet<>());
          }
        }
      }

    return Collections.singleton(pState);
  }


}
