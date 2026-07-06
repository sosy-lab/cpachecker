// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.acsltoformula;

import java.math.BigDecimal;
import java.math.BigInteger;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryPredicate.AcslBinaryPredicateOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTerm.AcslBinaryTermOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTermPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTermPredicate.AcslBinaryTermExpressionOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBooleanLiteralPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBooleanLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBuiltinLogicType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslCExpression;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIntegerLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslRealLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTernaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTernaryTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryPredicate.AcslUnaryExpressionOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryTerm.AcslUnaryTermOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeQualifiers;

public class AcslTestBuilder {

  private static final FileLocation DUMMY_LOC = FileLocation.DUMMY;
  private static final AcslType DEFAULT_TYPE = AcslBuiltinLogicType.INTEGER;

  private AcslTerm binaryTerm(
      AcslTerm left, AcslTerm right, AcslType type, AcslBinaryTermOperator op) {

    return new AcslBinaryTerm(DUMMY_LOC, type, left, right, op);
  }

  private AcslPredicate binaryTermPred(
      AcslTerm left, AcslTerm right, AcslBinaryTermExpressionOperator op) {

    return new AcslBinaryTermPredicate(DUMMY_LOC, left, right, op);
  }

  private AcslPredicate binaryPred(
      AcslPredicate left, AcslPredicate right, AcslBinaryPredicateOperator op) {

    return new AcslBinaryPredicate(DUMMY_LOC, left, right, op);
  }

  // Terms
  public AcslTerm plus(AcslTerm left, AcslTerm right) {
    return binaryTerm(left, right, DEFAULT_TYPE, AcslBinaryTermOperator.PLUS);
  }

  public AcslTerm minus(AcslTerm left, AcslTerm right) {
    return binaryTerm(left, right, DEFAULT_TYPE, AcslBinaryTermOperator.MINUS);
  }

  public AcslTerm multiply(AcslTerm left, AcslTerm right) {
    return binaryTerm(left, right, DEFAULT_TYPE, AcslBinaryTermOperator.MULTIPLY);
  }

  public AcslTerm multiply(AcslTerm left, AcslTerm right, AcslType type) {
    return binaryTerm(left, right, type, AcslBinaryTermOperator.MULTIPLY);
  }

  public AcslTerm unaryMinus(AcslTerm term) {
    return new AcslUnaryTerm(DUMMY_LOC, DEFAULT_TYPE, term, AcslUnaryTermOperator.MINUS);
  }

  public AcslTerm unaryPlus(AcslTerm term) {
    return new AcslUnaryTerm(DUMMY_LOC, DEFAULT_TYPE, term, AcslUnaryTermOperator.PLUS);
  }

  public AcslTerm unaryNegation(AcslTerm term) {
    return new AcslUnaryTerm(DUMMY_LOC, DEFAULT_TYPE, term, AcslUnaryTermOperator.NEGATION);
  }

  public AcslTerm unaryNegation(AcslTerm term, AcslType type) {
    return new AcslUnaryTerm(DUMMY_LOC, type, term, AcslUnaryTermOperator.NEGATION);
  }

  public AcslTerm ite(AcslPredicate condition, AcslTerm trueTerm, AcslTerm falseTerm) {
    return new AcslTernaryTerm(DUMMY_LOC, condition, trueTerm, falseTerm);
  }

  // Predicates
  public AcslPredicate eq(AcslTerm left, AcslTerm right) {
    return binaryTermPred(left, right, AcslBinaryTermExpressionOperator.EQUALS);
  }

  public AcslPredicate neq(AcslTerm left, AcslTerm right) {
    return binaryTermPred(left, right, AcslBinaryTermExpressionOperator.NOT_EQUALS);
  }

  public AcslPredicate leq(AcslTerm left, AcslTerm right) {
    return binaryTermPred(left, right, AcslBinaryTermExpressionOperator.LESS_EQUAL);
  }

  public AcslPredicate lt(AcslTerm left, AcslTerm right) {
    return binaryTermPred(left, right, AcslBinaryTermExpressionOperator.LESS_THAN);
  }

  public AcslPredicate geq(AcslTerm left, AcslTerm right) {
    return binaryTermPred(left, right, AcslBinaryTermExpressionOperator.GREATER_EQUAL);
  }

  public AcslPredicate gt(AcslTerm left, AcslTerm right) {
    return binaryTermPred(left, right, AcslBinaryTermExpressionOperator.GREATER_THAN);
  }

  public AcslPredicate and(AcslPredicate left, AcslPredicate right) {
    return binaryPred(left, right, AcslBinaryPredicateOperator.AND);
  }

  public AcslPredicate or(AcslPredicate left, AcslPredicate right) {
    return binaryPred(left, right, AcslBinaryPredicateOperator.OR);
  }

  public AcslPredicate equivalent(AcslPredicate left, AcslPredicate right) {
    return binaryPred(left, right, AcslBinaryPredicateOperator.EQUIVALENT);
  }

  public AcslPredicate implies(AcslPredicate left, AcslPredicate right) {
    return binaryPred(left, right, AcslBinaryPredicateOperator.IMPLICATION);
  }

  public AcslPredicate not(AcslPredicate pred) {
    return new AcslUnaryPredicate(DUMMY_LOC, pred, AcslUnaryExpressionOperator.NEGATION);
  }

  public AcslPredicate ite(
      AcslPredicate condition, AcslPredicate trueTerm, AcslPredicate falseTerm) {
    return new AcslTernaryPredicate(DUMMY_LOC, condition, trueTerm, falseTerm);
  }

  // Literals
  public AcslIntegerLiteralTerm integer(int value) {
    return new AcslIntegerLiteralTerm(
        DUMMY_LOC, AcslBuiltinLogicType.INTEGER, BigInteger.valueOf(value));
  }

  public AcslRealLiteralTerm real(double value) {
    return new AcslRealLiteralTerm(DUMMY_LOC, AcslBuiltinLogicType.REAL, BigDecimal.valueOf(value));
  }

  public AcslBooleanLiteralTerm bool(boolean value) {
    return new AcslBooleanLiteralTerm(DUMMY_LOC, value);
  }

  public AcslBooleanLiteralPredicate boolPred(boolean value) {
    return new AcslBooleanLiteralPredicate(DUMMY_LOC, value);
  }

  // Arrays
  private CSimpleType cBasicInt() {
    return new CSimpleType(
        CTypeQualifiers.NONE, CBasicType.INT, false, false, true, false, false, false, false);
  }

  public AcslCExpression arrayAcslCExpression(
      CType type, CSimpleDeclaration arrayDecl, CExpression subscriptExpr) {
    return new AcslCExpression(
        FileLocation.DUMMY,
        new CArraySubscriptExpression(
            FileLocation.DUMMY,
            type,
            new CIdExpression(FileLocation.DUMMY, arrayDecl),
            subscriptExpr));
  }

  public AcslCExpression arrayAcslCExpression(CType type, CSimpleDeclaration arrayDecl, int index) {
    return new AcslCExpression(
        FileLocation.DUMMY,
        new CArraySubscriptExpression(
            FileLocation.DUMMY,
            type,
            new CIdExpression(FileLocation.DUMMY, arrayDecl),
            new CIntegerLiteralExpression(
                FileLocation.DUMMY, cBasicInt(), BigInteger.valueOf(index))));
  }
}
