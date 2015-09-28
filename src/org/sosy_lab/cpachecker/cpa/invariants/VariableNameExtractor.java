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
package org.sosy_lab.cpachecker.cpa.invariants;

import java.util.Collections;
import java.util.Map;

import javax.annotation.Nullable;

import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.java.JArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JFieldAccess;
import org.sosy_lab.cpachecker.cfa.ast.java.JIdExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ExpressionToFormulaVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.FormulaCompoundStateEvaluationVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.NumeralFormula;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;


public class VariableNameExtractor {

  private final String functionName;

  private final Map<? extends String, ? extends NumeralFormula<CompoundInterval>> environment;

  private final CompoundIntervalManagerFactory compoundIntervalManagerFactory;

  private final MachineModel machineModel;

  public VariableNameExtractor(
      final CompoundIntervalManagerFactory pCompoundIntervalManagerFactory,
      final MachineModel pMachineModel,
      final CFAEdge pEdge) {
    this(pCompoundIntervalManagerFactory, pMachineModel, pEdge, false, Collections.<String, NumeralFormula<CompoundInterval>>emptyMap());
  }

  public VariableNameExtractor(
      final CompoundIntervalManagerFactory pCompoundIntervalManagerFactory,
      final MachineModel pMachineModel,
      final CFAEdge pEdge,
      final Map<? extends String, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    this(pCompoundIntervalManagerFactory, pMachineModel, pEdge, false, pEnvironment);
  }

  public VariableNameExtractor(
      final CompoundIntervalManagerFactory pCompoundIntervalManagerFactory,
      final MachineModel pMachineModel,
      final CFAEdge pEdge,
      final boolean pUsePredecessorFunctionName,
      final Map<? extends String, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    this(pCompoundIntervalManagerFactory, pMachineModel, pUsePredecessorFunctionName ? pEdge.getPredecessor() : pEdge.getSuccessor(), pEnvironment);
  }

  private VariableNameExtractor(
      final CompoundIntervalManagerFactory pCompoundIntervalManagerFactory,
      final MachineModel pMachineModel,
      final CFANode pFunctionNode,
      final Map<? extends String, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    this(pCompoundIntervalManagerFactory, pMachineModel, pFunctionNode.getFunctionName(), pEnvironment);
  }

  public VariableNameExtractor(
      final CompoundIntervalManagerFactory pCompoundIntervalManagerFactory,
      final MachineModel pMachineModel,
      final String pFunctionName,
      final Map<? extends String, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    this.compoundIntervalManagerFactory = pCompoundIntervalManagerFactory;
    this.machineModel = pMachineModel;
    this.functionName = pFunctionName;
    this.environment = pEnvironment;
  }

  public String getVarName(AParameterDeclaration pParameterDeclaration) {
    String varName = pParameterDeclaration.getName();
    if (pParameterDeclaration instanceof CSimpleDeclaration) {
      CSimpleDeclaration decl = (CSimpleDeclaration) pParameterDeclaration;

      if (!(decl instanceof CDeclaration && ((CDeclaration) decl).isGlobal() || decl instanceof CEnumerator)) {
        varName = scope(varName);
      }
    }
    return varName;
  }

  public String getVarName(AExpression pLhs) throws UnrecognizedCodeException {
    if (pLhs instanceof AIdExpression) {
      return getVarName((AIdExpression) pLhs);
    } else if (pLhs instanceof CFieldReference) {
      CFieldReference fieldRef = (CFieldReference) pLhs;
      String varName = fieldRef.getFieldName();
      CExpression owner = fieldRef.getFieldOwner();
      return getFieldReferenceVarName(varName, owner, fieldRef.isPointerDereference());
    } else if (pLhs instanceof JFieldAccess) {
      JFieldAccess fieldRef = (JFieldAccess) pLhs;
      String varName = fieldRef.getName();
      JIdExpression owner = fieldRef.getReferencedVariable();
      return getFieldReferenceVarName(varName, owner, false);
    } else if (pLhs instanceof CArraySubscriptExpression) {
      CArraySubscriptExpression arraySubscript = (CArraySubscriptExpression) pLhs;
      CExpression subscript = arraySubscript.getSubscriptExpression();
      CExpression owner = arraySubscript.getArrayExpression();
      return getArraySubscriptVarName(owner, subscript);
    } else if (pLhs instanceof JArraySubscriptExpression) {
      JArraySubscriptExpression arraySubscript = (JArraySubscriptExpression) pLhs;
      JExpression subscript = arraySubscript.getSubscriptExpression();
      JExpression owner = arraySubscript.getArrayExpression();
      return getArraySubscriptVarName(owner, subscript);
    } else if (pLhs instanceof CPointerExpression) {
      CPointerExpression pe = (CPointerExpression) pLhs;
      if (pe.getOperand() instanceof CLeftHandSide) {
        return String.format("*(%s)", getVarName(pe.getOperand()));
      }
      return scope(pLhs.toString());
    } else if (pLhs instanceof CCastExpression) {
      CCastExpression cast = (CCastExpression) pLhs;
      return getVarName(cast.getOperand());
    } else if (pLhs instanceof JCastExpression) {
      JCastExpression cast = (JCastExpression) pLhs;
      return getVarName(cast.getOperand());
    } else if (pLhs instanceof CUnaryExpression && ((CUnaryExpression) pLhs).getOperator() == UnaryOperator.AMPER) {
      return String.format("&(%s)", getVarName(((CUnaryExpression) pLhs).getOperand()));
    } else {
      return scope(pLhs.toString()); // This actually seems wrong but is currently the only way to deal with some cases of pointer arithmetics
    }
  }

  private String getVarName(AIdExpression pIdExpression) {
    CIdExpression var = (CIdExpression) pIdExpression;
    String varName = var.getName();
    if (var.getDeclaration() != null) {
      CSimpleDeclaration decl = var.getDeclaration();

      if (!(decl instanceof CDeclaration && ((CDeclaration) decl).isGlobal() || decl instanceof CEnumerator)) {
        varName = scope(varName);
      }
    }
    return varName;
  }

  private String getFieldReferenceVarName(String pVarName, @Nullable AExpression pOwner,
      boolean pIsPointerDereference) throws UnrecognizedCodeException {
    String varName = pVarName;
    if (pOwner != null) {
      varName = getVarName(pOwner) + (pIsPointerDereference ? "->" : ".") + varName;
    }
    return varName;
  }

  private String getArraySubscriptVarName(AExpression pOwner, AExpression pSubscript) throws UnrecognizedCodeException {
    if (pSubscript instanceof CIntegerLiteralExpression) {
      CIntegerLiteralExpression literal = (CIntegerLiteralExpression) pSubscript;
      return String.format("%s[%d]", getVarName(pOwner), literal.asLong()).toString();
    }
    final CompoundInterval subscriptValue;
    ExpressionToFormulaVisitor expressionToFormulaVisitor =
        new ExpressionToFormulaVisitor(compoundIntervalManagerFactory, machineModel, this, environment);
    if (pSubscript instanceof CExpression) {
      subscriptValue = evaluate(((CExpression) pSubscript).accept(expressionToFormulaVisitor));
    } else if (pSubscript instanceof JExpression) {
      subscriptValue = evaluate(((JExpression) pSubscript).accept(expressionToFormulaVisitor));
    } else {
      subscriptValue = compoundIntervalManagerFactory.createCompoundIntervalManager(machineModel, pOwner.getExpressionType()).allPossibleValues();
    }
    if (subscriptValue.isSingleton()) {
      return String.format("%s[%d]", getVarName(pOwner), subscriptValue.getValue()).toString();
    }
    return String.format("%s[*]", getVarName(pOwner));
  }

  private CompoundInterval evaluate(NumeralFormula<CompoundInterval> pFormula) {
    return pFormula.accept(new FormulaCompoundStateEvaluationVisitor(compoundIntervalManagerFactory), environment);
  }

  private String scope(String pVar) {
    return scope(pVar, functionName);
  }

  public static String scope(String pVar, String pFunction) {
    return pFunction + "::" + pVar;
  }

  public boolean isFunctionScoped(String pScopedVariableName) {
    return isFunctionScoped(pScopedVariableName, functionName);
  }

  public static boolean isFunctionScoped(String pScopedVariableName, String pFunction) {
    return pScopedVariableName.startsWith(pFunction + "::");
  }

}
