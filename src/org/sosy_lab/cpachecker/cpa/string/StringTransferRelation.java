// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.string;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JAssignment;
import org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.java.JDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JInitializer;
import org.sosy_lab.cpachecker.cfa.ast.java.JInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodInvocationAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodInvocationExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodOrConstructorInvocation;
import org.sosy_lab.cpachecker.cfa.ast.java.JParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JReferencedMethodInvocationExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.java.JSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JStatement;
import org.sosy_lab.cpachecker.cfa.ast.java.JVariableDeclaration;
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
import org.sosy_lab.cpachecker.cfa.types.java.JType;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.string.domains.AbstractStringDomain;
import org.sosy_lab.cpachecker.cpa.string.domains.DomainType;
import org.sosy_lab.cpachecker.cpa.string.utils.Aspect;
import org.sosy_lab.cpachecker.cpa.string.utils.Aspect.UnknownAspect;
import org.sosy_lab.cpachecker.cpa.string.utils.AspectSet;
import org.sosy_lab.cpachecker.cpa.string.utils.JStringVariableIdentifier;
import org.sosy_lab.cpachecker.cpa.string.utils.StringCpaUtilMethods;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class StringTransferRelation extends SingleEdgeTransferRelation {

  private final StringOptions options;
  private final Map<MemoryLocation, JReferencedMethodInvocationExpression> tempVariables;
  private String funcName;

  private JAspectListVisitor jalv;
  private JStringVariableVisitor jvv;

  private LogManager logger;

  public StringTransferRelation(
      LogManager pLogger,
      StringOptions pOptions,
      ImmutableMap<MemoryLocation, JReferencedMethodInvocationExpression> pTemporaryVars) {
    logger = pLogger;
    this.options = pOptions;
    tempVariables = pTemporaryVars;
  }

  @Override
  public Collection<StringState>
      getAbstractSuccessorsForEdge(AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge)
          throws CPATransferException, InterruptedException {

    StringState state = StringState.copyOf((StringState) pState);
    StringState successor = null;
    funcName = pCfaEdge.getPredecessor().getFunctionName();
    jalv = new JAspectListVisitor(options, state, funcName);
    jvv = new JStringVariableVisitor(funcName);

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
        successor = handleBlankEdge(state);
        break;

      case CallToReturnEdge:
        if (pCfaEdge instanceof JMethodSummaryEdge) {
          successor = handleJMethodSummaryEdge(state);
        }
        break;

      case FunctionCallEdge:
        if (pCfaEdge instanceof JMethodCallEdge) {
          final JMethodCallEdge fnkCall = (JMethodCallEdge) pCfaEdge;
          final FunctionEntryNode succ = fnkCall.getSuccessor();
          successor =
              handleJMethodCallEdge(
                  fnkCall.getArguments(),
                  succ.getFunctionParameters(),
                  state);
        }
        break;

      case FunctionReturnEdge:
        if (pCfaEdge instanceof JMethodReturnEdge) {
          final JMethodReturnEdge fnkReturnEdge = (JMethodReturnEdge) pCfaEdge;
          final JMethodSummaryEdge summaryEdge = fnkReturnEdge.getSummaryEdge();
          successor = handleJMethodReturnEdge(fnkReturnEdge, summaryEdge.getExpression(), state);
        }
        break;

      case ReturnStatementEdge:
        if (pCfaEdge instanceof JReturnStatementEdge) {
          successor = handleJReturnStatementEdge(state);
        }
        break;

      default:
        logger.log(Level.SEVERE, "No such edge existing");
    }

    if (successor != null) {
      if (pCfaEdge.getSuccessor() instanceof FunctionExitNode) {
        successor = StringState.copyOf(successor);
        state.clearLocalVariables(funcName);
      }
      return Collections.singleton(successor);
    } else {
      return ImmutableSet.of();
    }
  }

  // f(x)", that calls "f(int a)
  private StringState handleJMethodCallEdge(
      List<JExpression> pArguments,
      List<? extends AParameterDeclaration> parameters,
      StringState pState) {
    for (int i = 0; i < parameters.size(); i++) {
      JExpression exp = pArguments.get(i);
      if (StringCpaUtilMethods.isString(exp.getExpressionType())) {
        JParameterDeclaration param = (JParameterDeclaration) parameters.get(i);
        JStringVariableIdentifier jid = jvv.visit(param);
        AspectSet value = exp.accept(jalv);
        pState.updateVariable(jid, value);
      }
    }
    return pState;
  }

  // y=f(x)
  private StringState handleJMethodReturnEdge(
      JMethodReturnEdge pFnkReturnEdge,
      JMethodOrConstructorInvocation expSummary,
      StringState pState) {

    Optional<? extends AVariableDeclaration> returnVar =
        pFnkReturnEdge.getFunctionEntry().getReturnVariable();
    JStringVariableIdentifier retJid = null;

    if (returnVar.isPresent()) {
      if (StringCpaUtilMethods.isString(returnVar.get().getType())) {
        JVariableDeclaration jDecl = (JVariableDeclaration) returnVar.get();
        retJid = jvv.visit(jDecl);
      }
    }

    if (expSummary instanceof JMethodInvocationAssignmentStatement) {

      JMethodInvocationAssignmentStatement assignExp =
          ((JMethodInvocationAssignmentStatement) expSummary);
      JLeftHandSide op1 = assignExp.getLeftHandSide();
      AspectSet newValue = null;
      boolean valueExists = returnVar.isPresent() && pState.contains(retJid);
      Optional<JStringVariableIdentifier> jid = Optional.empty();
      if (valueExists) {
        newValue = pState.getAspectList(retJid);
        jid = Optional.of(retJid);
      }
      else {
        jid = Optional.of(jvv.visit(op1));
        JMethodInvocationExpression jmie = assignExp.getFunctionCallExpression();
        newValue = jalv.visit(jmie);
        // pState = handleJMethodCallEdge(jmie.getParameterExpressions(),);
      }

      if (jid.isPresent() && jid.orElseThrow().isString()) {
        return pState.updateVariable(jid.orElseThrow(), newValue);
      }
    }
    return pState;
  }

  // "return (x)"
  private StringState handleJReturnStatementEdge(StringState pState) {
    return pState;
  }

  private StringState handleJMethodSummaryEdge(StringState pState) {
    return pState;
  }

  private StringState handleBlankEdge(StringState pState) {
    return pState;
  }

  private StringState handleJStatemt(JStatement pJStat, StringState pState) {
    if (pJStat instanceof JAssignment) {
      JLeftHandSide jLeft = ((JAssignment) pJStat).getLeftHandSide();
      JRightHandSide jRight = ((JAssignment) pJStat).getRightHandSide();

      JType type = jLeft.getExpressionType();
      if (StringCpaUtilMethods.isString(type)) {
        JStringVariableIdentifier jid = jLeft.accept(jvv);
        AspectSet vaa = jRight.accept(jalv);
        return pState.updateVariable(jid, vaa);
      }
    }
    return pState;
  }

  private StringState handleJDeclaration(JSimpleDeclaration pJDecl, StringState pState) {
    if (pJDecl instanceof JVariableDeclaration) {
      if (!StringCpaUtilMethods.isString(pJDecl.getType())) {
        return pState;
      }
      JStringVariableIdentifier jid = jvv.visit(pJDecl);
      JInitializer init = (JInitializer) ((AVariableDeclaration) pJDecl).getInitializer();
      AspectSet aspectSet = getInitialValue(init);
      return pState.addVariable(jid, aspectSet);

    } else if (pJDecl instanceof JMethodDeclaration) {
      List<JParameterDeclaration> paramList = ((JMethodDeclaration) pJDecl).getParameters();
      for (JParameterDeclaration param : paramList) {
        pState = handleJDeclaration(param, pState);
      }
    }

    return pState;
  }

  private AspectSet getInitialValue(JInitializer pJ) {
    if (pJ instanceof JInitializerExpression) {
      JExpression init = ((JInitializerExpression) pJ).getExpression();
      AspectSet value = init.accept(jalv);
      return value;
    }
    return new AspectSet(ImmutableSet.of());
  }

  private StringState
      handleJAssumption(boolean truthAssumption, JAssumeEdge pCfaEdge, StringState pState) {
    JExpression exp = pCfaEdge.getExpression();

    boolean truthValue = false;
    if (exp instanceof JBinaryExpression) {
      JBinaryExpression binaryExpression = (JBinaryExpression) exp;
      JExpression operand1 = binaryExpression.getOperand1();
      JExpression operand2 = binaryExpression.getOperand2();
      JType op1Type = operand1.getExpressionType();
      JType op2Type = operand2.getExpressionType();

      // E.g. s1==s2 or s1 !=s2
      if (StringCpaUtilMethods.isString(op1Type) && StringCpaUtilMethods.isString(op2Type)) {
        truthValue =
            parseStringComparison(operand1, operand2, binaryExpression.getOperator(), pState);
      }
      // E.g. s1.length() == n
      // This case is handled in valuecpa, so we just pass the state
      else if (StringCpaUtilMethods.isString(op1Type) || StringCpaUtilMethods.isString(op2Type)) {
        return pState;
      }

      // E.g. s1.equals("foo") -> all methods on strings that return boolean
      else if (operand1 instanceof JIdExpression) {
        JIdExpression jidExp = (JIdExpression) operand1;
        if (StringCpaUtilMethods.isTemporaryVariable(jidExp)) {
          MemoryLocation memLoc = jvv.visit(jidExp).getMemLoc();
          JReferencedMethodInvocationExpression jrmie = tempVariables.get(memLoc);
          truthValue = handleStringMethodCall(jrmie, pState);
        }
      }
      // No strings used in assumption -> pass state
      else {
        return pState;
      }
    }

    if (truthValue == truthAssumption) {
      return pState;
    }
    return null;
  }

  private boolean
      handleStringMethodCall(JReferencedMethodInvocationExpression jrmie, StringState pState) {
    JIdExpression jid = jrmie.getReferencedVariable();
    JIdExpression funcNameExp = (JIdExpression) jrmie.getFunctionNameExpression();
    String stringFuncName = funcNameExp.getName();

    ImmutableList.Builder<JExpression> builder = new ImmutableList.Builder<>();
    for (JExpression jexp : jrmie.getParameterExpressions()) {
      builder.add(jexp);
    }
    ImmutableList<JExpression> parameters = builder.build();

    boolean result = false;
    // the different functions, TODO add more functions
    switch (stringFuncName) {
      case "equals": {
        result = parseStringComparison(jid, parameters.get(0), BinaryOperator.EQUALS, pState);
      }
        break;
      case "startsWith": {
        result = parsePrefixComparison(jid, parameters.get(0), pState);
      }
        break;
      case "endsWtih": {
        return parseSuffixComparison(jid, parameters.get(0), pState);
      }
      default:
        logger.log(Level.FINE, "This function was not implemented yet.");
        break;
    }
    return result;

  }

  private boolean
      parsePrefixComparison(JIdExpression pJid, JExpression pJExpression, StringState pState) {
    AspectSet first = parseExpressionToAspectList(pJid, pState);
    AspectSet second = parseExpressionToAspectList(pJExpression, pState);
    AbstractStringDomain<?> prefix = options.getDomain(DomainType.PREFFIX);
    if (prefix != null) {
      Aspect<?> aspectFirst = first.getAspect(DomainType.PREFFIX);
      Aspect<?> aspectSecond = second.getAspect(DomainType.PREFFIX);
      if (!(aspectSecond instanceof UnknownAspect)) {
        int secondLength = ((String) aspectSecond.getValue()).length();
        if (secondLength <= options.getPrefixLength()) {
          return prefix.isLessOrEqual(aspectFirst, aspectSecond)
              && prefix.isLessOrEqual(aspectSecond, aspectFirst);
        }
      }
    }
    return false;
  }

  private boolean
      parseSuffixComparison(JIdExpression pJid, JExpression pJExpression, StringState pState) {
    AspectSet first = parseExpressionToAspectList(pJid, pState);
    AspectSet second = parseExpressionToAspectList(pJExpression, pState);
    AbstractStringDomain<?> suffix = options.getDomain(DomainType.SUFFIX);
    if (suffix != null) {
      Aspect<?> aspectFirst = first.getAspect(DomainType.SUFFIX);
      Aspect<?> aspectSecond = second.getAspect(DomainType.SUFFIX);
      if (!(aspectSecond instanceof UnknownAspect)) {
        int secondLength = ((String) aspectSecond.getValue()).length();
        if (secondLength <= options.getSuffixLength()) {
          return suffix.isLessOrEqual(aspectFirst, aspectSecond)
              && suffix.isLessOrEqual(aspectSecond, aspectFirst);
        }
      }
    }
    return false;
  }

  // Compares if two aspect lists are the same
  private boolean parseStringComparison(
      JExpression pOperand1,
      JExpression pOperand2,
      BinaryOperator pBinaryOperator,
      StringState pState) {
    AspectSet first = parseExpressionToAspectList(pOperand1, pState);
    AspectSet second = parseExpressionToAspectList(pOperand2, pState);
    // Catch any case, that wasn't implemented yet
    if (first == null || second == null) {
      return false;
    }
    boolean equals = first.isLessOrEqual(second) && second.isLessOrEqual(second);
    switch (pBinaryOperator) {
      case EQUALS:
        return equals;
      case NOT_EQUALS:
        return !equals;
      default:
        break;
    }
    return false;
  }

  private AspectSet parseExpressionToAspectList(JExpression pOp, StringState pState) {
    if (pOp instanceof JLeftHandSide) {
      JStringVariableIdentifier jid = jvv.visit((JLeftHandSide) pOp);
      return pState.getAspectList(jid);
    }
    return pOp.accept(jalv);
  }
}
