// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.string;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
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
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodInvocationAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodOrConstructorInvocation;
import org.sosy_lab.cpachecker.cfa.ast.java.JReferencedMethodInvocationExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JRightHandSide;
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
import org.sosy_lab.cpachecker.cpa.string.utils.AspectList;
import org.sosy_lab.cpachecker.cpa.string.utils.AspectList.UnknownValueAndAspects;
import org.sosy_lab.cpachecker.cpa.string.utils.JStringVariableIdentifier;
import org.sosy_lab.cpachecker.cpa.string.utils.StringCpaUtilMethods;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class StringTransferRelation extends SingleEdgeTransferRelation {

  private StringOptions options;
  private List<JStringVariableIdentifier> localVariables;
  private Map<String, JReferencedMethodInvocationExpression> tempVariables;
  private String funcName;

  private JAspectListVisitor jalv;
  private JVariableVisitor jvv;

  private LogManager logger;

  public StringTransferRelation(LogManager pLogger, StringOptions pOptions) {
    logger = pLogger;
    this.options = pOptions;
    localVariables = new ArrayList<>();
    tempVariables = new HashMap<>();
  }

  @Override
  public Collection<StringState>
      getAbstractSuccessorsForEdge(AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge)
          throws CPATransferException, InterruptedException {

    StringState state = StringState.copyOf((StringState) pState);
    StringState successor = null;
    jalv = new JAspectListVisitor(options, state);
    jvv = new JVariableVisitor();
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
          final String calledFunctionName = succ.getFunctionName();
          successor =
              handleJMethodCallEdge(
                  fnkCall.getArguments(),
                  succ.getFunctionParameters(),
                  calledFunctionName,
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
      String pCalledFunctionName,
      StringState pState) {

    for (int i = 0; i < parameters.size(); i++) {
      JExpression exp = pArguments.get(i);
      if (StringCpaUtilMethods.isString(exp.getExpressionType())) {

        AParameterDeclaration param = parameters.get(i);
        MemoryLocation formalParamName =
            MemoryLocation.forLocalVariable(pCalledFunctionName, param.getName());
        JStringVariableIdentifier jid =
            new JStringVariableIdentifier(param.getType(), formalParamName);
        AspectList value = exp.accept(jalv);

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

    Optional<? extends AVariableDeclaration> returnVarName =
        pFnkReturnEdge.getFunctionEntry().getReturnVariable();
    JStringVariableIdentifier retJid = null;

    if (returnVarName.isPresent()) {
      if (StringCpaUtilMethods.isString(returnVarName.get().getType())) {

        retJid =
            new JStringVariableIdentifier(
                returnVarName.get().getType(),
                MemoryLocation.forDeclaration(returnVarName.get()));
      }
    }

    if (expSummary instanceof JMethodInvocationAssignmentStatement) {

      JMethodInvocationAssignmentStatement assignExp =
          ((JMethodInvocationAssignmentStatement) expSummary);
      AExpression op1 = assignExp.getLeftHandSide();
      AspectList newValue = null;
      boolean valueExists = returnVarName.isPresent() && pState.contains(retJid);
      Optional<JStringVariableIdentifier> jid = Optional.empty();

      if (valueExists) {
        newValue = pState.getAspectList(retJid);
        jid = Optional.of(retJid);
      }

      else {
        if (op1 instanceof JLeftHandSide) {
          jid = Optional.of(jvv.visit((JLeftHandSide) op1));
          // JMethodInvocationExpression jmie =
          newValue = jalv.visit(assignExp.getFunctionCallExpression());
        }
      }

      if (jid.isPresent() && jid.get().isString()) {
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
        AspectList vaa = jRight.accept(jalv);
        return pState.updateVariable(jid, vaa);
      }

      /*
       * We can't create an AST for the Methods in the Java Standard Library (that feature is not
       * implemented). Thus we have to use a Workaround: store all temporary variables, if their
       * referenced variable is a string. Then analyse that if possible. TODO: handle methods on
       * strings properly
       */
      if (pJStat instanceof JMethodInvocationAssignmentStatement) {
        if (jLeft instanceof JIdExpression) {
          JIdExpression tempVarJidexp = (JIdExpression) jLeft;
          if (StringCpaUtilMethods.isTemporaryVariable(tempVarJidexp)
              && (jRight instanceof JReferencedMethodInvocationExpression)) {
            JReferencedMethodInvocationExpression jrmie =
                (JReferencedMethodInvocationExpression) jRight;
            JIdExpression jid = jrmie.getReferencedVariable();
            if (StringCpaUtilMethods.isString(jid.getExpressionType())) {
              tempVariables.put(tempVarJidexp.getName(), jrmie);
            }
          }
        }
      }
    }
    return pState;
  }

  private StringState handleJDeclaration(JDeclaration pJDecl, StringState pState) {

    if (!(pJDecl instanceof JVariableDeclaration)
        || !StringCpaUtilMethods.isString(pJDecl.getType())) {
      return pState;
    }

    JVariableDeclaration jDecl = (JVariableDeclaration) pJDecl;
    JStringVariableIdentifier jid =
        new JStringVariableIdentifier(jDecl.getType(), MemoryLocation.forDeclaration(jDecl));

    JInitializer init = (JInitializer) jDecl.getInitializer();
    AspectList value = getInitialValue(init);
    localVariables.add(jid);

    return pState.addVariable(jid, value);
  }

  private AspectList getInitialValue(JInitializer pJ) {
    if (pJ instanceof JInitializerExpression) {
      JExpression init = ((JInitializerExpression) pJ).getExpression();
      AspectList value = init.accept(jalv);
      return value;
    }
    return UnknownValueAndAspects.getInstance();
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
          JReferencedMethodInvocationExpression jrmie = tempVariables.get(jidExp.getName());
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
    AspectList first = parseExpressionToAspectList(pJid, pState);
    AspectList second = parseExpressionToAspectList(pJExpression, pState);
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
    AspectList first = parseExpressionToAspectList(pJid, pState);
    AspectList second = parseExpressionToAspectList(pJExpression, pState);
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
    AspectList first = parseExpressionToAspectList(pOperand1, pState);
    AspectList second = parseExpressionToAspectList(pOperand2, pState);
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

  private AspectList parseExpressionToAspectList(JExpression pOp, StringState pState) {
    if (pOp instanceof JLeftHandSide) {
      JStringVariableIdentifier jid = jvv.visit((JLeftHandSide) pOp);
      return pState.getAspectList(jid);
    }
    return pOp.accept(jalv);
  }
}
