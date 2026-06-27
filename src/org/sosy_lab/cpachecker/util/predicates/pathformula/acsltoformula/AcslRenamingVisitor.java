// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.acsltoformula;

import java.util.HashMap;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslArraySubscriptTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslAtTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTermPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBooleanLiteralPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBooleanLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslCExpression;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslCharLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslExistsPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslForallPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslFunctionCallTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIdPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIdTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIntegerLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslOldPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslOldTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicateApplicationPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicateVisitor;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslRealLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslResultTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslStringLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTermVisitor;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTernaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTernaryTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslValidPredicate;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.SubstitutingCAstNodeVisitor;
import org.sosy_lab.cpachecker.exceptions.NoException;

public class AcslRenamingVisitor
    implements AcslPredicateVisitor<AcslPredicate, NoException>,
        AcslTermVisitor<AcslTerm, NoException> {

  private final Map<AcslSimpleDeclaration, AcslSimpleDeclaration> renamingMap;
  private Map<String, String> nameRenamingMap;

  public AcslRenamingVisitor(Map<AcslSimpleDeclaration, AcslSimpleDeclaration> pRenamingMap) {
    this.renamingMap = pRenamingMap;
    this.nameRenamingMap = new HashMap<>();
    for (Map.Entry<AcslSimpleDeclaration, AcslSimpleDeclaration> entry : pRenamingMap.entrySet()) {
      this.nameRenamingMap.put(entry.getKey().getName(), entry.getValue().getName());
    }
  }

  @Override
  public AcslPredicate visit(AcslBinaryPredicate pBinaryExpression) throws NoException {
    return new AcslBinaryPredicate(
        pBinaryExpression.getFileLocation(),
        pBinaryExpression.getOperand1().accept(this),
        pBinaryExpression.getOperand2().accept(this),
        pBinaryExpression.getOperator());
  }

  @Override
  public AcslPredicate visit(AcslUnaryPredicate pAcslUnaryPredicate) throws NoException {
    return new AcslUnaryPredicate(
        pAcslUnaryPredicate.getFileLocation(),
        pAcslUnaryPredicate.getOperand().accept(this),
        pAcslUnaryPredicate.getOperator());
  }

  @Override
  public AcslPredicate visit(AcslIdPredicate pAcslIdPredicate) throws NoException {
    return pAcslIdPredicate;
  }

  @Override
  public AcslPredicate visit(AcslBinaryTermPredicate pAcslBinaryTermPredicate) throws NoException {
    return new AcslBinaryTermPredicate(
        pAcslBinaryTermPredicate.getFileLocation(),
        pAcslBinaryTermPredicate.getOperand1().accept(this),
        pAcslBinaryTermPredicate.getOperand2().accept(this),
        pAcslBinaryTermPredicate.getOperator());
  }

  @Override
  public AcslPredicate visit(AcslOldPredicate pAcslOldPredicate) throws NoException {
    return new AcslOldPredicate(
        pAcslOldPredicate.getFileLocation(), pAcslOldPredicate.getExpression().accept(this));
  }

  @Override
  public AcslPredicate visit(AcslBooleanLiteralPredicate pAcslBooleanLiteralPredicate)
      throws NoException {
    return pAcslBooleanLiteralPredicate;
  }

  @Override
  public AcslPredicate visit(AcslTernaryPredicate pAcslTernaryPredicate) throws NoException {
    return new AcslTernaryPredicate(
        pAcslTernaryPredicate.getFileLocation(),
        pAcslTernaryPredicate.getCondition().accept(this),
        pAcslTernaryPredicate.getResultIfTrue().accept(this),
        pAcslTernaryPredicate.getResultIfFalse().accept(this));
  }

  @Override
  public AcslPredicate visit(AcslValidPredicate pAcslValidPredicate) throws NoException {
    return pAcslValidPredicate;
  }

  @Override
  public AcslPredicate visit(AcslForallPredicate pForallPredicate) throws NoException {
    return new AcslForallPredicate(
        pForallPredicate.getFileLocation(),
        pForallPredicate.getBinders(),
        pForallPredicate.getPredicate().accept(this));
  }

  @Override
  public AcslPredicate visit(AcslExistsPredicate pAcslExistsPredicate) throws NoException {
    return new AcslExistsPredicate(
        pAcslExistsPredicate.getFileLocation(),
        pAcslExistsPredicate.getBinders(),
        pAcslExistsPredicate.getPredicate().accept(this));
  }

  @Override
  public AcslPredicate visit(AcslPredicateApplicationPredicate pAcslPredicateApplicationPredicate)
      throws NoException {
    return new AcslPredicateApplicationPredicate(
        pAcslPredicateApplicationPredicate.getFileLocation(),
        pAcslPredicateApplicationPredicate.getPredicateDeclaration(),
        pAcslPredicateApplicationPredicate.getParameters().stream()
            .map(arg -> arg.accept(this))
            .toList());
  }

  @Override
  public AcslTerm visit(AcslUnaryTerm pAcslUnaryTerm) throws NoException {
    return new AcslUnaryTerm(
        pAcslUnaryTerm.getFileLocation(),
        pAcslUnaryTerm.getExpressionType(),
        pAcslUnaryTerm.getOperand().accept(this),
        pAcslUnaryTerm.getOperator());
  }

  @Override
  public AcslTerm visit(AcslStringLiteralTerm pAcslStringLiteralTerm) throws NoException {
    return pAcslStringLiteralTerm;
  }

  @Override
  public AcslTerm visit(AcslRealLiteralTerm pAcslRealLiteralTerm) throws NoException {
    return pAcslRealLiteralTerm;
  }

  @Override
  public AcslTerm visit(AcslCharLiteralTerm pAcslCharLiteralTerm) throws NoException {
    return pAcslCharLiteralTerm;
  }

  @Override
  public AcslTerm visit(AcslIntegerLiteralTerm pAcslIntegerLiteralTerm) throws NoException {
    return pAcslIntegerLiteralTerm;
  }

  @Override
  public AcslTerm visit(AcslBooleanLiteralTerm pAcslBooleanLiteralTerm) {
    return pAcslBooleanLiteralTerm;
  }

  @Override
  public AcslTerm visit(AcslBinaryTerm pAcslBinaryTerm) throws NoException {
    return new AcslBinaryTerm(
        pAcslBinaryTerm.getFileLocation(),
        pAcslBinaryTerm.getExpressionType(),
        pAcslBinaryTerm.getOperand1().accept(this),
        pAcslBinaryTerm.getOperand2().accept(this),
        pAcslBinaryTerm.getOperator());
  }

  @Override
  public AcslTerm visit(AcslIdTerm pAcslIdTerm) throws NoException {
    AcslSimpleDeclaration declaration = pAcslIdTerm.getDeclaration();
    AcslSimpleDeclaration renamed = renamingMap.get(declaration);
    if (renamed != null) {
      return new AcslIdTerm(pAcslIdTerm.getFileLocation(), renamed);
    } else {
      return pAcslIdTerm;
    }
  }

  @Override
  public AcslTerm visit(AcslOldTerm pAcslOldTerm) throws NoException {
    return new AcslOldTerm(pAcslOldTerm.getFileLocation(), pAcslOldTerm.getTerm().accept(this));
  }

  @Override
  public AcslTerm visit(AcslResultTerm pAcslResultTerm) throws NoException {
    return pAcslResultTerm;
  }

  @Override
  public AcslTerm visit(AcslAtTerm pAcslAtTerm) throws NoException {
    return new AcslAtTerm(
        pAcslAtTerm.getFileLocation(), pAcslAtTerm.getTerm().accept(this), pAcslAtTerm.getLabel());
  }

  @Override
  public AcslTerm visit(AcslTernaryTerm pAcslTernaryTerm) throws NoException {
    return new AcslTernaryTerm(
        pAcslTernaryTerm.getFileLocation(),
        pAcslTernaryTerm.getCondition().accept(this),
        pAcslTernaryTerm.getResultIfTrue().accept(this),
        pAcslTernaryTerm.getResultIfFalse().accept(this));
  }

  @Override
  public AcslTerm visit(AcslFunctionCallTerm pAcslFunctionCallTerm) throws NoException {
    return new AcslFunctionCallTerm(
        pAcslFunctionCallTerm.getFileLocation(),
        pAcslFunctionCallTerm.getExpressionType(),
        pAcslFunctionCallTerm.getFunctionNameExpression(),
        pAcslFunctionCallTerm.getParameterExpressions().stream()
            .map(arg -> arg.accept(this))
            .toList(),
        pAcslFunctionCallTerm.getDeclaration());
  }

  @Override
  public AcslTerm visit(AcslArraySubscriptTerm pAcslArraySubscriptTerm) throws NoException {
    return new AcslArraySubscriptTerm(
        pAcslArraySubscriptTerm.getFileLocation(),
        pAcslArraySubscriptTerm.getExpressionType(),
        pAcslArraySubscriptTerm.getArrayExpression().accept(this),
        pAcslArraySubscriptTerm.getSubscriptExpression().accept(this));
  }

  @Override
  public AcslTerm visit(AcslCExpression pAcslCExpression) {
    SubstitutingCAstNodeVisitor cRenamingVisitor =
        new SubstitutingCAstNodeVisitor(this::renameCExpression);

    return new AcslCExpression(
        pAcslCExpression.getFileLocation(),
        (CExpression) pAcslCExpression.getCExpression().accept(cRenamingVisitor));
  }

  private CAstNode renameCExpression(CAstNode node) {
    switch (node) {
      case CIdExpression idExpr -> {
        String newName = nameRenamingMap.get(idExpr.getName());
        if (newName == null) {
          return null;
        }
        return new CIdExpression(
            idExpr.getFileLocation(),
            idExpr.getExpressionType(),
            newName,
            (CSimpleDeclaration) renameCExpression(idExpr.getDeclaration()));
      }
      case CParameterDeclaration decl -> {
        String newName = nameRenamingMap.get(decl.getName());
        if (newName == null) {
          return null;
        }
        return new CParameterDeclaration(decl.getFileLocation(), decl.getType(), newName);
      }
      case CVariableDeclaration decl -> {
        String newName = nameRenamingMap.get(decl.getName());
        if (newName == null) {
          return null;
        }
        return new CVariableDeclaration(
            decl.getFileLocation(),
            decl.isGlobal(),
            decl.getCStorageClass(),
            decl.getType(),
            newName,
            newName,
            newName,
            decl.getInitializer());
      }
      case CFunctionDeclaration decl -> {
        String newName = nameRenamingMap.get(decl.getName());
        if (newName == null) {
          return null;
        }
        return new CFunctionDeclaration(
            decl.getFileLocation(),
            decl.getType(),
            newName,
            decl.getParameters(),
            decl.getAttributes());
      }
      case null, default -> {
        return null;
      }
    }
  }
}
