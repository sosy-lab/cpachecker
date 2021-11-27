// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.string;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JAssignment;
import org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.java.JDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JInitializer;
import org.sosy_lab.cpachecker.cfa.ast.java.JInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodInvocationAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodInvocationExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodOrConstructorInvocation;
import org.sosy_lab.cpachecker.cfa.ast.java.JReferencedMethodInvocationExpression;
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
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.string.domains.DomainType;
import org.sosy_lab.cpachecker.cpa.string.utils.Aspect;
import org.sosy_lab.cpachecker.cpa.string.utils.HelperMethods;
import org.sosy_lab.cpachecker.cpa.string.utils.JVariableIdentifier;
import org.sosy_lab.cpachecker.cpa.string.utils.ValueAndAspects;
import org.sosy_lab.cpachecker.cpa.string.utils.ValueAndAspects.UnknownValueAndAspects;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class StringTransferRelation extends SingleEdgeTransferRelation {

  private StringOptions options;
  private List<JVariableIdentifier> variables;
  private String funcName;
  private JStringValueVisitor jValVis;
  private JVariableVisitor jVarNameVis;
  // private JVariableVisitor jVis;
  private LogManager logger;
  private final MachineModel model;

  public StringTransferRelation(
      LogManager pLogger,
      StringOptions pOptions,
      MachineModel pMachineModel) {
    logger = pLogger;
    this.options = pOptions;
    variables = new ArrayList<>();
    model = pMachineModel;
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
          JAssumeEdge jae = (JAssumeEdge) pCfaEdge;
          successor = handleJAssumption(jae.getTruthAssumption(), jae, state);
        }
        break;
      case BlankEdge:
        successor = handleBlankEdge((BlankEdge) pCfaEdge, state);
        break;
      case CallToReturnEdge:
        if (pCfaEdge instanceof JMethodSummaryEdge) {
          successor = handleJMethodSummaryEdge(/* (JMethodSummaryEdge) pCfaEdge, */state);
        }
        break;
      case FunctionCallEdge:
        if (pCfaEdge instanceof JMethodCallEdge) {
          final JMethodCallEdge fnkCall = (JMethodCallEdge) pCfaEdge;
          final FunctionEntryNode succ = fnkCall.getSuccessor();
          final String calledFunctionName = succ.getFunctionName();
          successor =
              handleJMethodCallEdge(
                  // fnkCall,
                  fnkCall.getArguments(),
                  succ.getFunctionParameters(),
                  calledFunctionName,
                  succ,
                  state);
        }
        break;

      case FunctionReturnEdge:
        if (pCfaEdge instanceof JMethodReturnEdge) {
          // final String callerFunctionName = pCfaEdge.getSuccessor().getFunctionName();
          final JMethodReturnEdge fnkReturnEdge = (JMethodReturnEdge) pCfaEdge;
          final JMethodSummaryEdge summaryEdge = fnkReturnEdge.getSummaryEdge();
          successor =
              handleJMethodReturnEdge(
                  fnkReturnEdge,
                  // summaryEdge,
                  summaryEdge.getExpression(),
                  // callerFunctionName,
                  state);
        }
        break;
      case ReturnStatementEdge:
        if (pCfaEdge instanceof JReturnStatementEdge) {
          // final JReturnStatementEdge returnEdge = (JReturnStatementEdge) pCfaEdge;
          successor = handleJReturnStatementEdge(/* returnEdge, */ state);
        }
        break;

      default:
        logger.log(Level.SEVERE, "No such edge existing");
    }
    if (successor != null) {
      return Collections.singleton(successor);
    } else {
      return ImmutableSet.of();
    }
  }

  // f(x)", that calls "f(int a)
  private StringState handleJMethodCallEdge(
      // JMethodCallEdge pFnkCall,
      List<JExpression> pArguments,
      List<? extends AParameterDeclaration> parameters,
      String pCalledFunctionName,
      FunctionEntryNode pSucc,
      StringState pState) {
    if (pSucc.getReturnVariable().isPresent()) {
      AVariableDeclaration decl = pSucc.getReturnVariable().get();
      pState = handleJDeclaration((JDeclaration) decl, pState);
    }
    // if (pCalledFunctionName.contains("String_length")
    // && pFnkCall.getFunctionCallExpression() instanceof JReferencedMethodInvocationExpression) {
    // JReferencedMethodInvocationExpression jrmie =
    // (JReferencedMethodInvocationExpression) pFnkCall.getFunctionCallExpression();
    // return handleMethodCallStringLength(jrmie, pState);
    // }
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

  // y=f(x)
  private StringState handleJMethodReturnEdge(
      JMethodReturnEdge pFnkReturnEdge,
      // JMethodSummaryEdge pSummaryEdge,
      JMethodOrConstructorInvocation expSummary,
      // String pCallerFunctionName,
      StringState pState) {
    Optional<? extends AVariableDeclaration> returnVarName =
        pFnkReturnEdge.getFunctionEntry().getReturnVariable();
    JVariableIdentifier retJid = null;
    if (returnVarName.isPresent()) {
      if (HelperMethods.isString(returnVarName.get().getType())) {
        retJid =
            new JVariableIdentifier(
                returnVarName.get().getType(),
                MemoryLocation.forDeclaration(returnVarName.get()));
      }
    }
    if (expSummary instanceof JMethodInvocationAssignmentStatement) {
      JMethodInvocationAssignmentStatement assignExp =
          ((JMethodInvocationAssignmentStatement) expSummary);
      AExpression op1 = assignExp.getLeftHandSide();

      ValueAndAspects newValue = null;
      boolean valueExists = returnVarName.isPresent() && pState.contains(retJid);
      if (valueExists) {
        newValue = pState.getVaa(retJid);
      }
      Optional<JVariableIdentifier> jid = Optional.empty();
      if (op1 instanceof JLeftHandSide) {
        jid = Optional.of(jVarNameVis.visit((JLeftHandSide) op1));
      }
      if (jid.isPresent()) {
        return pState.updateVariable(jid.orElseThrow(), newValue);
      }
    }
    return pState;
  }

  // "return (x)"
  // not needed.
  private StringState handleJReturnStatementEdge(StringState pState) {
    return pState;
  }

  private StringState
      handleJMethodSummaryEdge(/* JMethodSummaryEdge summaryEdge, */StringState pState) {
    // TODO length, substring
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
    if (!(pJDecl instanceof JVariableDeclaration) || !HelperMethods.isString(pJDecl.getType())) {
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

  private StringState
      handleJAssumption(boolean truthAssumption, JAssumeEdge pCfaEdge, StringState pState) {
    JExpression exp = pCfaEdge.getExpression();
    if (exp instanceof JBinaryExpression) {
      JBinaryExpression jbe = (JBinaryExpression) exp;
      Pair<JMethodInvocationExpression, JExpression> pair =
          parseBinExpToJmieAndJe(jbe.getOperand1(), jbe.getOperand2());
      JExpression other;
      JMethodInvocationExpression jmie;
      if (pair != null) {
        jmie = pair.getFirst();
        other = pair.getSecond();
      } else {
        return pState;
      }
      do {
        if (jmie instanceof JReferencedMethodInvocationExpression
            && jmie.getDeclaration().getOrigName().contains("String_length")
            && options.containsDomain(DomainType.LENGTH)) {
          JReferencedMethodInvocationExpression jrmie =
              (JReferencedMethodInvocationExpression) jmie;
          JVariableIdentifier jid = jVarNameVis.visit(jrmie.getReferencedVariable());
          ValueAndAspects vaa = pState.getVaa(jid);
          if (!(logger instanceof LogManagerWithoutDuplicates)) {
            logger = new LogManagerWithoutDuplicates(logger);
          }
          Aevv vis = new Aevv(funcName, model, (LogManagerWithoutDuplicates) logger);
          Value va = other.accept(vis);
          if (truthAssumption) {
            pState = handleMethodCallStringLength(jbe.getOperator(), pState, jid, vaa, va);
          } else {
            pState = handleMethodCallStringLengthNegTA(jbe.getOperator(), pState, jid, vaa, va);
          }
        }
      } while (other instanceof JBinaryExpression);
    }
    if (truthAssumption) {
      return pState;
    }
    return null;
  }

  private Pair<JMethodInvocationExpression, JExpression>
      parseBinExpToJmieAndJe(JExpression first, JExpression second) {
    if (first instanceof JMethodInvocationExpression) {
      return Pair.of((JMethodInvocationExpression) first, second);
    }
    if (second instanceof JMethodInvocationExpression) {
      return Pair.of((JMethodInvocationExpression) second, first);
    } else {
      return null;
    }

  }

  private StringState handleMethodCallStringLengthNegTA(
      BinaryOperator pBinOp,
      StringState pState,
      JVariableIdentifier pJid,
      ValueAndAspects pVaa,
      Value pVa) {
    if (pVa.isNumericValue()) {
      Aspect<?> a = null;
      switch (pBinOp) {
        case LESS_EQUAL:
        case LESS_THAN:
        case NOT_EQUALS: {
          int val = pVa.asNumericValue().getNumber().intValue();
          a = options.getDomain(DomainType.LENGTH).addNewAspectOfThisDomain(Integer.toString(val));
        }
          break;
        case GREATER_EQUAL:
        case GREATER_THAN:
        case EQUALS:
        default:
          break;
      }
      if (a != null) {
        pState.updateVariable(pJid, pVaa.updateOneAspect(a));
      }
    }
    return pState;
  }

  private StringState handleMethodCallStringLength(
      BinaryOperator pBinOp,
      // JExpression jexp,
      StringState pState,
      JVariableIdentifier pJid,
      ValueAndAspects pVaa,
      Value pVa) {
    if (pVa.isNumericValue()) {
      Aspect<?> a = null;
      switch (pBinOp) {
        case GREATER_EQUAL:
        case GREATER_THAN:
        case EQUALS: {
          int val = pVa.asNumericValue().getNumber().intValue();
          a = options.getDomain(DomainType.LENGTH).addNewAspectOfThisDomain(Integer.toString(val));
        }
          break;
        case LESS_EQUAL:
        case LESS_THAN:
        case NOT_EQUALS:
        default:
          break;
      }
      if (a != null) {
        pState.updateVariable(pJid, pVaa.updateOneAspect(a));
      }
    }
    return pState;
  }
}
