// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.ExpressionToFormulaVisitor;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.ExpressionToFormulaVisitor.ValidatedFScanFParameter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.FormulaEncodingOptions;

/**
 * This function contains utility methods for handling built-in functions in CPAchecker. It provides
 * some methods to identify, process, and manage built-in functions.
 */
public class BuiltinFunctionsHandling {
  public static CFunctionCallAssignmentStatement createNondetCallForScanfOverapproximation(
      CFunctionCallExpression e, CFAEdge pEdge) throws UnrecognizedCodeException {
    final List<CExpression> parameters = e.getParameterExpressions();

    ValidatedFScanFParameter receivingParameter =
        ExpressionToFormulaVisitor.validateFscanfParameters(parameters, e, pEdge);

    if (receivingParameter.receiver() instanceof CUnaryExpression unaryParameter) {
      UnaryOperator operator = unaryParameter.getOperator();
      CExpression operand = unaryParameter.getOperand();
      if (operator.equals(UnaryOperator.AMPER) && operand instanceof CIdExpression idExpression) {
        // For simplicity, we start with the case where only parameters of the form "&id" occur
        CType variableType = idExpression.getExpressionType();

        if (!ExpressionToFormulaVisitor.isCompatibleWithScanfFormatString(
            receivingParameter.format(), variableType, pEdge)) {
          throw new UnsupportedCodeException(
              "fscanf with receiving type <-> format specifier mismatch is not supported.",
              pEdge,
              e);
        }

        CFunctionDeclaration nondetFun =
            new CFunctionDeclaration(
                pEdge.getFileLocation(),
                CFunctionType.functionTypeWithReturnType(variableType),
                FormulaEncodingOptions.INTERNAL_NONDET_FUNCTION_NAME,
                ImmutableList.of(),
                ImmutableSet.of());
        CIdExpression nondetFunctionName =
            new CIdExpression(
                pEdge.getFileLocation(), variableType, nondetFun.getName(), nondetFun);

        CFunctionCallExpression rhs =
            new CFunctionCallExpression(
                pEdge.getFileLocation(),
                variableType,
                nondetFunctionName,
                ImmutableList.of(),
                nondetFun);
        return new CFunctionCallAssignmentStatement(pEdge.getFileLocation(), idExpression, rhs);
      } else {
        throw new UnsupportedCodeException(
            "Currently, only fscanf with a single parameter of the form &id is supported.",
            pEdge,
            e);
      }
    } else {
      throw new UnsupportedCodeException(
          "Currently, only fscanf with a single parameter of the form &id is supported.", pEdge, e);
    }
  }
}
