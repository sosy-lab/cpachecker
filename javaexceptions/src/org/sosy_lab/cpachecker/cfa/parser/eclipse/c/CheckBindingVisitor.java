// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.eclipse.c;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArrayDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CArrayRangeDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatorVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatementVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.util.BuiltinFunctions;

/**
 * This class can traverse through an AST and log a warning for all undefined identifiers which are
 * referenced.
 */
class CheckBindingVisitor
    implements CRightHandSideVisitor<Void, CFAGenerationRuntimeException>,
        CInitializerVisitor<Void, CFAGenerationRuntimeException>,
        CStatementVisitor<Void, CFAGenerationRuntimeException>,
        CDesignatorVisitor<Void, CFAGenerationRuntimeException> {

  private final LogManager logger;

  private final Set<String> printedWarnings = new HashSet<>();

  private boolean foundUndefinedIdentifiers = false;

  CheckBindingVisitor(LogManager pLogger) {
    logger = pLogger;
  }

  public boolean foundUndefinedIdentifiers() {
    return foundUndefinedIdentifiers;
  }

  @Override
  public Void visit(CArraySubscriptExpression e) {
    e.getArrayExpression().accept(this);
    e.getSubscriptExpression().accept(this);
    return null;
  }

  @Override
  public Void visit(CBinaryExpression e) {
    e.getOperand1().accept(this);
    e.getOperand2().accept(this);
    return null;
  }

  @Override
  public Void visit(CCastExpression e) {
    e.getOperand().accept(this);
    return null;
  }

  @Override
  public Void visit(CComplexCastExpression e) {
    e.getOperand().accept(this);
    return null;
  }

  @Override
  public Void visit(CFieldReference e) {
    e.getFieldOwner().accept(this);
    return null;
  }

  @Override
  public Void visit(CIdExpression e) {
    if (e.getDeclaration() == null) {
      if (printedWarnings.add(e.getName())) {
        logger.log(
            Level.WARNING,
            "Undefined identifier",
            e.getName(),
            "found, first referenced in",
            e.getFileLocation());
        foundUndefinedIdentifiers = true;
      }
    }
    return null;
  }

  @Override
  public Void visit(CCharLiteralExpression e) {
    return null;
  }

  @Override
  public Void visit(CFloatLiteralExpression e) {
    return null;
  }

  @Override
  public Void visit(CIntegerLiteralExpression e) {
    return null;
  }

  @Override
  public Void visit(CStringLiteralExpression e) {
    return null;
  }

  @Override
  public Void visit(CImaginaryLiteralExpression e) {
    e.getValue().accept(this);
    return null;
  }

  @Override
  public Void visit(CTypeIdExpression e) {
    return null;
  }

  @Override
  public Void visit(CUnaryExpression e) {
    e.getOperand().accept(this);
    return null;
  }

  @Override
  public Void visit(CPointerExpression e) {
    e.getOperand().accept(this);
    return null;
  }

  @Override
  public Void visit(CFunctionCallExpression e) {
    if (e.getFunctionNameExpression() instanceof CIdExpression) {
      CIdExpression f = (CIdExpression) e.getFunctionNameExpression();
      CType expressionType = f.getExpressionType().getCanonicalType();
      assert expressionType instanceof CFunctionType
              || (expressionType instanceof CPointerType
                  && ((CPointerType) expressionType).getType() instanceof CFunctionType)
          : "Invalid function call: Type of expression "
              + f.getName()
              + " in line "
              + e.getFileLocation().getEndingLineNumber()
              + " is not a valid function type (neither a plain function nor a function-pointer).";

      if (f.getDeclaration() == null) {
        if (!BuiltinFunctions.isBuiltinFunction(f.getName()) // GCC builtin functions
            && printedWarnings.add(f.getName())) {
          logger.log(
              Level.WARNING,
              "Undefined function",
              f.getName(),
              "found, first called in",
              e.getFileLocation());
        }
      }

    } else {
      e.getFunctionNameExpression().accept(this);
    }
    for (CExpression param : e.getParameterExpressions()) {
      param.accept(this);
    }
    return null;
  }

  @Override
  public Void visit(CInitializerExpression e) {
    e.getExpression().accept(this);
    return null;
  }

  @Override
  public Void visit(CInitializerList e) {
    for (CInitializer init : e.getInitializers()) {
      init.accept(this);
    }
    return null;
  }

  @Override
  public Void visit(CExpressionStatement e) {
    e.getExpression().accept(this);
    return null;
  }

  @Override
  public Void visit(CExpressionAssignmentStatement e) {
    e.getLeftHandSide().accept(this);
    e.getRightHandSide().accept(this);
    return null;
  }

  @Override
  public Void visit(CFunctionCallAssignmentStatement e) {
    e.getLeftHandSide().accept(this);
    e.getRightHandSide().accept(this);
    return null;
  }

  @Override
  public Void visit(CFunctionCallStatement e) {
    e.getFunctionCallExpression().accept(this);
    return null;
  }

  @Override
  public Void visit(CDesignatedInitializer e) throws CFAGenerationRuntimeException {
    for (CDesignator designator : e.getDesignators()) {
      designator.accept(this);
    }
    e.getRightHandSide().accept(this);
    return null;
  }

  @Override
  public Void visit(CArrayDesignator e) throws CFAGenerationRuntimeException {
    e.getSubscriptExpression().accept(this);
    return null;
  }

  @Override
  public Void visit(CArrayRangeDesignator e) throws CFAGenerationRuntimeException {
    e.getFloorExpression().accept(this);
    e.getCeilExpression().accept(this);
    return null;
  }

  @Override
  public Void visit(CFieldDesignator e) throws CFAGenerationRuntimeException {
    return null;
  }

  @Override
  public Void visit(CAddressOfLabelExpression e) throws CFAGenerationRuntimeException {
    return null;
  }
}
