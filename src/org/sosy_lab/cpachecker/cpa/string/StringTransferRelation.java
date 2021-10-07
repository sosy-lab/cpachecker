// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.string;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JAssignment;
import org.sosy_lab.cpachecker.cfa.ast.java.JDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JInitializer;
import org.sosy_lab.cpachecker.cfa.ast.java.JInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodOrConstructorInvocation;
import org.sosy_lab.cpachecker.cfa.ast.java.JRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.java.JStatement;
import org.sosy_lab.cpachecker.cfa.ast.java.JVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.java.JAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JMethodCallEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JMethodReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JMethodSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JStatementEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.string.utils.HelperMethods;
import org.sosy_lab.cpachecker.cpa.string.utils.JVariableIdentifier;
import org.sosy_lab.cpachecker.cpa.string.utils.ValueAndAspects;
import org.sosy_lab.cpachecker.cpa.string.utils.ValueAndAspects.UnknownValueAndAspects;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class StringTransferRelation extends SingleEdgeTransferRelation
// extends ForwardingTransferRelation<StringState, StringState, Precision>
{

  private StringOptions options;
  private List<JVariableIdentifier> variables;
  private String funcName;
  private JStringValueVisitor jValVis;
  private JVariableVisitor jVarNameVis;
  private JVariableVisitor jVis;
  private LogManager logger;

  public StringTransferRelation(LogManager pLogger, StringOptions pOptions) {
    logger = pLogger;
    this.options = pOptions;
    variables = new LinkedList<>();
  }

  @Override
  public Collection<StringState>
      getAbstractSuccessorsForEdge(AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge)
          throws CPATransferException, InterruptedException {
    StringState state = StringState.copyOf((StringState) pState);
    StringState successor = null;
    jValVis = new JStringValueVisitor(options);
    jVarNameVis = new JVariableVisitor();
    // jVis = new JVariableVisitor();
    funcName = pCfaEdge.getPredecessor().getFunctionName();

    switch (pCfaEdge.getEdgeType()) {
      case DeclarationEdge:
        if (pCfaEdge instanceof JDeclarationEdge) {
          JDeclaration jDecl = ((JDeclarationEdge) pCfaEdge).getDeclaration();
            successor = handleJDeclaration(jDecl, state);
        }
        break;
      case StatementEdge:
        if (pCfaEdge instanceof JStatementEdge) {
          JStatement jStat = ((JStatementEdge) pCfaEdge).getStatement();
          successor = handleJStatemt(jStat, state);
        }
        break;
      case AssumeEdge:
        if (pCfaEdge instanceof JAssumeEdge) {
          successor = handleJAssumption(((JAssumeEdge) pCfaEdge).getTruthAssumption(), state);
        }
        break;
      case BlankEdge:
        successor = handleBlankEdge((BlankEdge) pCfaEdge, state);
        break;
      case CallToReturnEdge:
        if (pCfaEdge instanceof JMethodSummaryEdge) {
          successor = handleJMethodSummaryEdge((JMethodSummaryEdge) pCfaEdge, state);
        }
        break;
      case FunctionCallEdge:
        if (pCfaEdge instanceof JMethodCallEdge) {
          final JMethodCallEdge fnkCall = (JMethodCallEdge) pCfaEdge;
          final FunctionEntryNode succ = fnkCall.getSuccessor();
          final String calledFunctionName = succ.getFunctionName();
          successor =
              handleJMethodCallEdge(
                  fnkCall,
                  fnkCall.getArguments(),
                  succ.getFunctionParameters(),
                  calledFunctionName,
                  succ,
                  state);
        }
        break;

      case FunctionReturnEdge:
        if (pCfaEdge instanceof JMethodReturnEdge) {
          final String callerFunctionName = pCfaEdge.getSuccessor().getFunctionName();
          final JMethodReturnEdge fnkReturnEdge = (JMethodReturnEdge) pCfaEdge;
          final JMethodSummaryEdge summaryEdge = fnkReturnEdge.getSummaryEdge();
          successor =
              handleJMethodReturnEdge(
                  fnkReturnEdge,
                  summaryEdge,
                  summaryEdge.getExpression(),
                  callerFunctionName,
                  state);
        }
        break;
      case ReturnStatementEdge:
        if (pCfaEdge instanceof JReturnStatementEdge) {
          final JReturnStatementEdge returnEdge = (JReturnStatementEdge) pCfaEdge;
          successor = handleJReturnStatementEdge(returnEdge, state);
        }
        break;

      default:
        logger.log(Level.SEVERE, "No such edge existing");
    }
    if (successor != null) {
      return Collections.singleton(successor);
    }
    else {
      // TODO change to fail
      return ImmutableSet.of();
    }
  }

  private StringState handleJMethodCallEdge(
      JMethodCallEdge pFnkCall,
      List<JExpression> pArguments,
      List<? extends AParameterDeclaration> parameters,
      String pCalledFunctionName,
      FunctionEntryNode pSucc, StringState pState) {
    // TODO Auto-generated method stub
    if(pSucc.getReturnVariable().isPresent()) {
      AVariableDeclaration decl= pSucc.getReturnVariable().get();
      pState = handleJDeclaration((JDeclaration) decl, pState);
    }
    for (int i = 0; i < parameters.size(); i++) {

      JExpression exp = pArguments.get(i);

      if (HelperMethods.isString(exp.getExpressionType())) {
      AParameterDeclaration param = parameters.get(i);
        MemoryLocation formalParamName =
            MemoryLocation.forLocalVariable(pCalledFunctionName, param.getName());
        JVariableIdentifier jid = new JVariableIdentifier(param.getType(), formalParamName);
        ValueAndAspects value = exp.accept(jValVis);
        pState.updateVariable(jid, value);
      }
    }
    return pState;
  }

  private StringState handleJMethodReturnEdge(
      JMethodReturnEdge pFnkReturnEdge,
      JMethodSummaryEdge pSummaryEdge,
      JMethodOrConstructorInvocation pExpression,
      String pCallerFunctionName,
      StringState pState) {
    // TODO Auto-generated method stub

    return pState;
  }

  private StringState
      handleJReturnStatementEdge(JReturnStatementEdge pEdge, StringState pState) {
    // TODO Auto-generated method stub
    // Optional<JExpression> expOpt = pEdge.getExpression();
    // if (expOpt.isPresent()) {
    // if (HelperMethods.isString(expOpt.get().getExpressionType())) {
    //
    // }
    // }
    return pState;
  }

  private StringState handleJMethodSummaryEdge(JMethodSummaryEdge pCfaEdge, StringState pState) {
    // TODO Auto-generated method stub
    return pState;
  }

  private StringState handleBlankEdge(BlankEdge pCfaEdge, StringState pState) {
    if (pCfaEdge.getSuccessor() instanceof FunctionExitNode) {
      StringState state = StringState.copyOf(pState);
      state.clearAFunction(funcName);
      return state;
    }
    return pState;
  }

  private StringState handleJStatemt(JStatement pJStat, StringState pState) {
    if (pJStat instanceof JAssignment) {
      JLeftHandSide jLeft = ((JAssignment) pJStat).getLeftHandSide();
      JRightHandSide jRight = ((JAssignment) pJStat).getRightHandSide();
      JVariableIdentifier jid = jLeft.accept(jVarNameVis);
      if (jid.isString()) {
        ValueAndAspects vaa = jRight.accept(jValVis);
        return pState.updateVariable(jid, vaa);
        }
    }
    return pState;
  }

  private StringState handleJDeclaration(JDeclaration pJDecl, StringState pState) {
    if (!(pJDecl instanceof JVariableDeclaration) || !(HelperMethods.isString(pJDecl.getType()))) {
      return pState;
    }
    JVariableDeclaration jDecl = (JVariableDeclaration) pJDecl;
    JVariableIdentifier jid =
        new JVariableIdentifier(jDecl.getType(), MemoryLocation.forDeclaration(jDecl));
    JInitializer init = (JInitializer) jDecl.getInitializer();
    ValueAndAspects value = getInitialValue(init);
    variables.add(jid);
    return pState.addVariable(jid, value);
  }

  private ValueAndAspects getInitialValue(JInitializer pJ) {
    if (pJ instanceof JInitializerExpression) {
      JExpression init = ((JInitializerExpression) pJ).getExpression();
      ValueAndAspects value = init.accept(jValVis);
      return value;
    }
    return UnknownValueAndAspects.getInstance();
  }

  private StringState handleJAssumption(boolean truthAssumption, StringState pState) {
    if (truthAssumption) {
      return pState;
    }
    return null;
  }
}
