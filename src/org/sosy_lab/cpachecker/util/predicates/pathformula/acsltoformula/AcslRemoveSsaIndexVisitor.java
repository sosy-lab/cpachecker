// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.acsltoformula;

import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslArraySubscriptTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslAtTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTermPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBooleanLiteralPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBooleanLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslCExpression;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslCVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslCharLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslExistsPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslForallPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslFunctionCallTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIdPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIdTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIntegerLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslOldPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslOldTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicateApplicationPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicateDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicateVisitor;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslRealLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslResultTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslStringLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTermVisitor;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTernaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTernaryTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslValidPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslVariableDeclaration;
import org.sosy_lab.cpachecker.exceptions.NoException;

public class AcslRemoveSsaIndexVisitor
    implements AcslPredicateVisitor<AcslPredicate, NoException>,
        AcslTermVisitor<AcslTerm, NoException> {

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
    AcslSimpleDeclaration declaration = renameDeclaration(pAcslIdPredicate.getDeclaration());
    return new AcslIdPredicate(
        pAcslIdPredicate.getFileLocation(), (AcslPredicateDeclaration) declaration);
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
    AcslSimpleDeclaration declaration = renameDeclaration(pAcslIdTerm.getDeclaration());
    return new AcslIdTerm(pAcslIdTerm.getFileLocation(), declaration);
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
    return pAcslCExpression; // For now, we don't rename C expressions, but we could implement this
    // if needed
  }

  private String removeSsaIndex(String name) {
    int index = name.lastIndexOf('@');
    if (index != -1) {
      return name.substring(0, index);
    }
    return name;
  }

  private AcslSimpleDeclaration renameDeclaration(AcslSimpleDeclaration declaration) {
    String newName = removeSsaIndex(declaration.getName());
    if (!newName.equals(declaration.getName())) {
      if (declaration instanceof AcslParameterDeclaration) {
        return new AcslParameterDeclaration(
            declaration.getFileLocation(), declaration.getType(), newName);
      } else if (declaration instanceof AcslDeclaration) {
        // AcslCVariableDeclaration, AcslLogicDeclaration, AcslTypeDeclaration,
        // AcslVariableDeclaration
        if (declaration instanceof AcslVariableDeclaration aDeclaration) {
          String origName = removeSsaIndex(aDeclaration.getOrigName());
          String qualName = removeSsaIndex(aDeclaration.getQualifiedName());
          return new AcslVariableDeclaration(
              aDeclaration.getFileLocation(),
              aDeclaration.isGlobal(),
              aDeclaration.getType(),
              newName,
              origName,
              qualName);
        } else if (declaration instanceof AcslCVariableDeclaration) {
          return declaration; // For now, we don't rename C variable declarations, but we could
          // implement this if needed
        } else if (declaration instanceof AcslPredicateDeclaration aPredDecl) {
          String origName = removeSsaIndex(aPredDecl.getOrigName());
          return new AcslPredicateDeclaration(
              aPredDecl.getFileLocation(),
              aPredDecl.getType(),
              newName,
              origName,
              aPredDecl.getPolymorphicTypes(),
              aPredDecl.getParameters().stream()
                  .map(AcslParameterDeclaration.class::cast)
                  .toList());
        } else if (declaration instanceof AcslTypeDeclaration) {
          return declaration;
        }
      }
    }
    return declaration;
  }
}
