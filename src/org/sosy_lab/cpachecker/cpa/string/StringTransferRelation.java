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
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JAssignment;
import org.sosy_lab.cpachecker.cfa.ast.java.JDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.java.JInitializer;
import org.sosy_lab.cpachecker.cfa.ast.java.JInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodInvocationExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodInvocationStatement;
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
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class StringTransferRelation extends SingleEdgeTransferRelation
// extends ForwardingTransferRelation<StringState, StringState, Precision>
{

  private Configuration config;
  private StringOptions options;
  private List<JVariableIdentifier> variables;
  private String funcName;
  private JStringValueVisitor jValVis;
  private JVariableVisitor jVarNameVis;
  private JVariableVisitor jVis;
  private LogManager logger;

  public StringTransferRelation(LogManager pLogger) {
    logger = pLogger;
    variables = new LinkedList<>();
    jValVis = new JStringValueVisitor();
    jVarNameVis = new JVariableVisitor();
  }

  @Override
  public Collection<StringState>
      getAbstractSuccessorsForEdge(AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge)
          throws CPATransferException, InterruptedException {
    StringState state = StringState.copyOf((StringState) pState);
    StringState successor = null;
    jValVis = new JStringValueVisitor();
    jVarNameVis = new JVariableVisitor();
    // jVis = new JVariableVisitor();
    funcName = pCfaEdge.getPredecessor().getFunctionName();
    // Ask if interface
    // if (!(pCfaEdge instanceof JEdge)) {
    // return ImmutableSet.of();
    // }
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
        // logger.log(Level.SEVERE, "No such edge existing");
    }
    if (successor == null) {
      return ImmutableSet.of();
    } else {
      return Collections.singleton(successor);
    }
  }

  private StringState handleJMethodCallEdge(
      JMethodCallEdge pFnkCall,
      List<JExpression> pArguments,
      List<? extends AParameterDeclaration> pList,
      String pCalledFunctionName,
      StringState pState) {
    // TODO Auto-generated method stub
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
      handleJReturnStatementEdge(JReturnStatementEdge pReturnEdge, StringState pState) {
    // TODO Auto-generated method stub
    return pState;
  }

  private StringState handleJMethodSummaryEdge(JMethodSummaryEdge pCfaEdge, StringState pState) {
    // TODO Auto-generated method stub
    return pState;
  }

  private StringState handleBlankEdge(BlankEdge pCfaEdge, StringState pState) {
    if (pCfaEdge.getSuccessor() instanceof FunctionExitNode) {
      StringState state = StringState.copyOf(pState);
      // state.clearAFunction(funcName);
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
    if (pJStat instanceof JExpressionStatement) {
    }
    if (pJStat instanceof JMethodInvocationStatement) {
      JMethodInvocationExpression jmexp =
          ((JMethodInvocationStatement) pJStat).getFunctionCallExpression();
      for (JExpression exp : jmexp.getParameterExpressions()) {
        if (HelperMethods.isString(exp.getExpressionType())) {
          // JVariableIdentifier jid = exp.accept(jVarNameVis);
          // TODO value ?
          // pState.addVariable(jid);
        }
      }
    }
    return pState;
  }

  private @Nullable StringState handleJDeclaration(JDeclaration pJDecl, StringState pState) {
    if (!(pJDecl instanceof JVariableDeclaration) || !(HelperMethods.isString(pJDecl.getType()))) {
      return pState;
    }
    JVariableDeclaration jDecl = (JVariableDeclaration) pJDecl;
    JVariableIdentifier jid =
        new JVariableIdentifier(jDecl.getType(), MemoryLocation.forDeclaration(jDecl));
    variables.add(jid);
    JInitializer init = (JInitializer) jDecl.getInitializer();
    String value = getInitialValue(init, pState);
    return pState.addVariable(jid, value);
  }

  private String getInitialValue(JInitializer pJ, StringState state) {
    if (pJ instanceof JInitializerExpression) {
      JExpression init = ((JInitializerExpression) pJ).getExpression();
      String value = init.accept(jValVis).getValue();
      // // ((JInitializerExpression) pJ).getExpression().toQualifiedASTString();
      // Optional<JVariableIdentifier> inMap = state.isVariableInMap(value);
      // if (inMap.isPresent()) {
      // value = state.getStringsAndAspects().get(inMap.get()).getValue();
      // }
      return value;
    }
    return "";
  }

  private @Nullable StringState handleJAssumption(boolean truthAssumption, StringState pState) {
    if (truthAssumption) {
      return pState;
    }
    return pState;
  }

  private String calcValueForAssigBin(JLeftHandSide jL, JRightHandSide jR, StringState state) {
    String val = jR.toASTString();
    Optional<JVariableIdentifier> inMap = state.isVariableInMap(val);
    if (inMap.isPresent()) {
      val = state.getStringsAndAspects().get(inMap.get()).getValue();
    }
    return "";
  }
}
