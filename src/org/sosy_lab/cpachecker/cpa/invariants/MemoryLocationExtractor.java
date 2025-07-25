// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
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
import org.sosy_lab.cpachecker.cpa.invariants.formula.ExpressionToFormulaVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.FormulaCompoundStateEvaluationVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.NumeralFormula;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class MemoryLocationExtractor {

  private final String functionName;

  private final Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>>
      environment;

  private final CompoundIntervalManagerFactory compoundIntervalManagerFactory;

  private final MachineModel machineModel;

  public MemoryLocationExtractor(
      final CompoundIntervalManagerFactory pCompoundIntervalManagerFactory,
      final MachineModel pMachineModel,
      final CFAEdge pEdge) {
    this(pCompoundIntervalManagerFactory, pMachineModel, pEdge, false, ImmutableMap.of());
  }

  public MemoryLocationExtractor(
      final CompoundIntervalManagerFactory pCompoundIntervalManagerFactory,
      final MachineModel pMachineModel,
      final CFAEdge pEdge,
      final Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>>
          pEnvironment) {
    this(pCompoundIntervalManagerFactory, pMachineModel, pEdge, false, pEnvironment);
  }

  public MemoryLocationExtractor(
      final CompoundIntervalManagerFactory pCompoundIntervalManagerFactory,
      final MachineModel pMachineModel,
      final CFAEdge pEdge,
      final boolean pUsePredecessorFunctionName,
      final Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>>
          pEnvironment) {
    this(
        pCompoundIntervalManagerFactory,
        pMachineModel,
        pUsePredecessorFunctionName ? pEdge.getPredecessor() : pEdge.getSuccessor(),
        pEnvironment);
  }

  private MemoryLocationExtractor(
      final CompoundIntervalManagerFactory pCompoundIntervalManagerFactory,
      final MachineModel pMachineModel,
      final CFANode pFunctionNode,
      final Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>>
          pEnvironment) {
    this(
        pCompoundIntervalManagerFactory,
        pMachineModel,
        pFunctionNode.getFunctionName(),
        pEnvironment);
  }

  public MemoryLocationExtractor(
      final CompoundIntervalManagerFactory pCompoundIntervalManagerFactory,
      final MachineModel pMachineModel,
      final String pFunctionName,
      final Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>>
          pEnvironment) {
    compoundIntervalManagerFactory = pCompoundIntervalManagerFactory;
    machineModel = pMachineModel;
    functionName = pFunctionName;
    environment = pEnvironment;
  }

  public MemoryLocation getMemoryLocation(AExpression pLhs) throws UnrecognizedCodeException {
    if (pLhs instanceof AIdExpression aIdExpression) {
      return getMemoryLocation(aIdExpression);
    } else if (pLhs instanceof CFieldReference fieldRef) {
      String varName = fieldRef.getFieldName();
      CExpression owner = fieldRef.getFieldOwner();
      return getFieldReferenceMemoryLocation(varName, owner, fieldRef.isPointerDereference());
    } else if (pLhs instanceof JFieldAccess fieldRef) {
      String varName = fieldRef.getName();
      JIdExpression owner = fieldRef.getReferencedVariable();
      return getFieldReferenceMemoryLocation(varName, owner, false);
    } else if (pLhs instanceof CArraySubscriptExpression arraySubscript) {
      CExpression subscript = arraySubscript.getSubscriptExpression();
      CExpression owner = arraySubscript.getArrayExpression();
      return getArraySubscriptMemoryLocation(owner, subscript);
    } else if (pLhs instanceof JArraySubscriptExpression arraySubscript) {
      JExpression subscript = arraySubscript.getSubscriptExpression();
      JExpression owner = arraySubscript.getArrayExpression();
      return getArraySubscriptMemoryLocation(owner, subscript);
    } else if (pLhs instanceof CPointerExpression pe) {
      if (pe.getOperand() instanceof CLeftHandSide) {
        // TODO
        return MemoryLocation.parseExtendedQualifiedName(
            String.format("*(%s)", getMemoryLocation(pe.getOperand())));
      }
      // TODO
      return MemoryLocation.forLocalVariable(functionName, pLhs.toString());
    } else if (pLhs instanceof CCastExpression cast) {
      return getMemoryLocation(cast.getOperand());
    } else if (pLhs instanceof JCastExpression cast) {
      return getMemoryLocation(cast.getOperand());
    } else if (pLhs instanceof CUnaryExpression cUnaryExpression
        && cUnaryExpression.getOperator() == UnaryOperator.AMPER) {
      // TODO
      return MemoryLocation.parseExtendedQualifiedName(
          String.format("&(%s)", getMemoryLocation(cUnaryExpression.getOperand())));
    } else {
      // TODO
      // This actually seems wrong but is currently the only way to deal with some cases of pointer
      // arithmetics
      return MemoryLocation.forLocalVariable(functionName, pLhs.toString());
    }
  }

  private MemoryLocation getMemoryLocation(AIdExpression pIdExpression) {
    if (pIdExpression.getDeclaration() != null) {
      return MemoryLocation.forDeclaration(pIdExpression.getDeclaration());
    }
    return MemoryLocation.forIdentifier(pIdExpression.getName());
  }

  private MemoryLocation getFieldReferenceMemoryLocation(
      String pVarName, @Nullable AExpression pOwner, boolean pIsPointerDereference)
      throws UnrecognizedCodeException {
    String varName = pVarName;
    if (pOwner != null) {
      varName = getMemoryLocation(pOwner) + (pIsPointerDereference ? "->" : ".") + varName;
    }
    return MemoryLocation.fromQualifiedName(varName);
  }

  private MemoryLocation getArraySubscriptMemoryLocation(AExpression pOwner, AExpression pSubscript)
      throws UnrecognizedCodeException {

    // TODO: calculate correct memory locations

    if (pSubscript instanceof CIntegerLiteralExpression literal) {
      return MemoryLocation.parseExtendedQualifiedName(
          String.format("%s[%d]", getMemoryLocation(pOwner), literal.asLong()));
    }
    final CompoundInterval subscriptValue;
    ExpressionToFormulaVisitor expressionToFormulaVisitor =
        new ExpressionToFormulaVisitor(
            compoundIntervalManagerFactory, machineModel, this, environment);
    if (pSubscript instanceof CExpression cExpression) {
      subscriptValue = evaluate(cExpression.accept(expressionToFormulaVisitor));
    } else if (pSubscript instanceof JExpression jExpression) {
      subscriptValue = evaluate(jExpression.accept(expressionToFormulaVisitor));
    } else {
      subscriptValue =
          compoundIntervalManagerFactory
              .createCompoundIntervalManager(machineModel, pOwner.getExpressionType())
              .allPossibleValues();
    }
    if (subscriptValue.isSingleton()) {
      return MemoryLocation.parseExtendedQualifiedName(
          String.format("%s[%s]", getMemoryLocation(pOwner), subscriptValue.getValue()));
    }
    return MemoryLocation.parseExtendedQualifiedName(
        String.format("%s[*]", getMemoryLocation(pOwner)));
  }

  private CompoundInterval evaluate(NumeralFormula<CompoundInterval> pFormula) {
    return pFormula.accept(
        new FormulaCompoundStateEvaluationVisitor(compoundIntervalManagerFactory, false),
        environment);
  }

  public boolean isFunctionScoped(String pScopedVariableName) {
    return isFunctionScoped(pScopedVariableName, functionName);
  }

  public static boolean isFunctionScoped(String pScopedVariableName, String pFunction) {
    return pScopedVariableName.startsWith(pFunction + "::");
  }
}
