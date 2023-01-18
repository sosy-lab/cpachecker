// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.validation.c;

import java.math.BigInteger;
import org.sosy_lab.cpachecker.cfa.CfaMetadata;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.graph.CfaNetwork;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.validation.AbstractCfaValidator;
import org.sosy_lab.cpachecker.cfa.validation.CfaValidationResult;
import org.sosy_lab.cpachecker.cfa.validation.CfaValidator;

/**
 * This {@link CfaValidator} ensures that all literal integer expressions are in the value range
 * permitted by the used {@link MachineModel}.
 */
public final class LiteralExpressionsInValueRange extends AbstractCfaValidator {

  private CfaValidationResult checkValueRanges(
      CExpression pExpression, MachineModel pMachineModel) {
    return pExpression.accept(new ExpressionValueRangeValidator(pMachineModel, pExpression));
  }

  private CfaValidationResult checkEdge(CFAEdge pEdge, MachineModel pMachineModel) {
    switch (pEdge.getEdgeType()) {
      case AssumeEdge:
        if (pEdge instanceof CAssumeEdge) {
          return checkValueRanges(((CAssumeEdge) pEdge).getExpression(), pMachineModel);
        }
        break;
      case DeclarationEdge:
        ADeclaration decl = ((ADeclarationEdge) pEdge).getDeclaration();
        if (decl instanceof CVariableDeclaration) {
          CInitializer init = ((CVariableDeclaration) decl).getInitializer();
          if (init instanceof CInitializerExpression) {
            return checkValueRanges(((CInitializerExpression) init).getExpression(), pMachineModel);
          }
        }
        break;
      case StatementEdge:
        AStatement stat = ((AStatementEdge) pEdge).getStatement();
        if (stat instanceof CExpressionStatement) {
          return checkValueRanges(((CExpressionStatement) stat).getExpression(), pMachineModel);
        }
        break;
      default: // TODO: add more checks
    }
    return pass();
  }

  @Override
  public CfaValidationResult check(CfaNetwork pCfaNetwork, CfaMetadata pCfaMetadata) {
    return CfaValidator.createEdgeValidator(edge -> checkEdge(edge, pCfaMetadata.getMachineModel()))
        .check(pCfaNetwork, pCfaMetadata);
  }

  private final class ExpressionValueRangeValidator
      extends DefaultCExpressionVisitor<CfaValidationResult, RuntimeException> {

    private final MachineModel machineModel;
    private final CExpression expressionForLogging;

    public ExpressionValueRangeValidator(MachineModel pMachineModel, CExpression pExpression) {
      machineModel = pMachineModel;
      expressionForLogging = pExpression;
    }

    private CfaValidationResult checkValueRange(CType pType, BigInteger pValue) {
      CSimpleType type = (CSimpleType) pType.getCanonicalType();
      if (machineModel.getMinimalIntegerValue(type).compareTo(pValue) > 0) {
        return fail(
            "value '%s' is too small for type '%s' in expression '%s' at %s",
            pValue,
            type,
            expressionForLogging.toASTString(),
            expressionForLogging.getFileLocation());
      }
      if (machineModel.getMaximalIntegerValue(type).compareTo(pValue) < 0) {
        return fail(
            "value '%s' is too large for type '%s' in expression '%s' at %s",
            pValue,
            type,
            expressionForLogging.toASTString(),
            expressionForLogging.getFileLocation());
      }
      return pass();
    }

    @Override
    public CfaValidationResult visit(CArraySubscriptExpression pArraySubscriptExpression) {
      return pArraySubscriptExpression.getSubscriptExpression().accept(this);
    }

    @Override
    public CfaValidationResult visit(CFieldReference pFieldReference) {
      return pFieldReference.getFieldOwner().accept(this);
    }

    @Override
    public CfaValidationResult visit(CPointerExpression pPointerExpression) {
      return pPointerExpression.getOperand().accept(this);
    }

    @Override
    public CfaValidationResult visit(CComplexCastExpression pComplexCastExpression) {
      return pComplexCastExpression.getOperand().accept(this);
    }

    @Override
    public CfaValidationResult visit(CBinaryExpression pBinaryExpression) {
      return pBinaryExpression
          .getOperand1()
          .accept(this)
          .combine(pBinaryExpression.getOperand2().accept(this));
    }

    @Override
    public CfaValidationResult visit(CCastExpression pCastExpression) {
      return pCastExpression.getOperand().accept(this);
    }

    @Override
    public CfaValidationResult visit(CCharLiteralExpression pLiteral) {
      return checkValueRange(
          pLiteral.getExpressionType(), BigInteger.valueOf(pLiteral.getCharacter()));
    }

    @Override
    public CfaValidationResult visit(CIntegerLiteralExpression pLiteral) {
      return checkValueRange(pLiteral.getExpressionType(), pLiteral.getValue());
    }

    @Override
    public CfaValidationResult visit(CUnaryExpression pUnaryExpression) {
      return pUnaryExpression.getOperand().accept(this);
    }

    @Override
    public CfaValidationResult visit(CImaginaryLiteralExpression pLiteralExpression) {
      return pLiteralExpression.getValue().accept(this);
    }

    @Override
    protected CfaValidationResult visitDefault(CExpression pExpression) {
      return pass(); // ignore the expression
    }
  }
}
