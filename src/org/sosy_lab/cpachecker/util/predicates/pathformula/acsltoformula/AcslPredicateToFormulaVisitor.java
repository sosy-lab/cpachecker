// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.acsltoformula;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTermPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBooleanLiteralPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslCType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslExistsPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslForallPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIdPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslOldPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicateApplicationPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicateDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicateVisitor;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTernaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslValidPredicate;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;

public class AcslPredicateToFormulaVisitor
    implements AcslPredicateVisitor<BooleanFormula, NoException> {

  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final AcslTermToFormulaVisitor termVisitor;
  private final CtoFormulaConverter ctoFormulaConverter;

  @SuppressWarnings("unused") // I suspect currentSsa will be needed at some point
  private final SSAMapBuilder currentSsa;

  private final MachineModel machineModel;
  private final Optional<SSAMap>
      functionEntrySsa; // Optional SSA map for function-entry state (\old)

  public AcslPredicateToFormulaVisitor(
      FormulaManagerView pFmgr,
      SSAMapBuilder pCurrentSsa,
      CtoFormulaConverter pCtoFormulaConverter,
      MachineModel pMachineModel) {
    checkNotNull(pFmgr);
    checkNotNull(pCurrentSsa);
    checkNotNull(pMachineModel);
    this.fmgr = pFmgr;
    this.bfmgr = fmgr.getBooleanFormulaManager();
    this.termVisitor =
        new AcslTermToFormulaVisitor(pFmgr, pCurrentSsa, pCtoFormulaConverter, pMachineModel);
    this.currentSsa = pCurrentSsa;
    this.ctoFormulaConverter = pCtoFormulaConverter;
    this.functionEntrySsa = Optional.empty();
    this.machineModel = pMachineModel;
  }

  public AcslPredicateToFormulaVisitor(
      FormulaManagerView pFmgr,
      SSAMapBuilder pCurrentSsa,
      SSAMap pFunctionEntrySsa,
      CtoFormulaConverter pCtoFormulaConverter,
      MachineModel pMachineModel) {
    checkNotNull(pFmgr);
    checkNotNull(pCurrentSsa);
    checkNotNull(pMachineModel);
    this.fmgr = pFmgr;
    this.bfmgr = fmgr.getBooleanFormulaManager();
    this.termVisitor =
        new AcslTermToFormulaVisitor(
            pFmgr, pCurrentSsa, pFunctionEntrySsa, pCtoFormulaConverter, pMachineModel);
    this.currentSsa = pCurrentSsa;
    this.ctoFormulaConverter = pCtoFormulaConverter;
    this.functionEntrySsa = Optional.ofNullable(pFunctionEntrySsa);
    this.machineModel = pMachineModel;
  }

  // Constructor that should only be called by AcslTermToFormulaVisitor
  // this is required to create the condition in a ternary term, e.g., x > 0 ? 1 : 2
  protected AcslPredicateToFormulaVisitor(
      FormulaManagerView pFmgr,
      AcslTermToFormulaVisitor pTermVisitor,
      SSAMapBuilder pCurrentSsa,
      Optional<SSAMap> oFunctionEntrySsa,
      CtoFormulaConverter pCtoFormulaConverter,
      MachineModel pMachineModel) {
    checkNotNull(pFmgr);
    checkNotNull(pTermVisitor);
    checkNotNull(pMachineModel);
    this.fmgr = pFmgr;
    this.bfmgr = fmgr.getBooleanFormulaManager();
    this.termVisitor = pTermVisitor;
    this.currentSsa = pCurrentSsa;
    this.ctoFormulaConverter = pCtoFormulaConverter;
    this.functionEntrySsa = oFunctionEntrySsa;
    this.machineModel = pMachineModel;
  }

  @Override
  public BooleanFormula visit(AcslBinaryPredicate pBinaryExpression) throws NoException {
    BooleanFormula operand1Formula = pBinaryExpression.getOperand1().accept(this);
    BooleanFormula operand2Formula = pBinaryExpression.getOperand2().accept(this);

    return switch (pBinaryExpression.getOperator()) {
      case IMPLICATION -> bfmgr.implication(operand1Formula, operand2Formula);
      case EQUIVALENT -> bfmgr.equivalence(operand1Formula, operand2Formula);
      case AND -> bfmgr.and(operand1Formula, operand2Formula);
      case OR -> bfmgr.or(operand1Formula, operand2Formula);
    };
  }

  @Override
  public BooleanFormula visit(AcslUnaryPredicate pAcslUnaryPredicate) throws NoException {
    BooleanFormula operandFormula = pAcslUnaryPredicate.getOperand().accept(this);

    return switch (pAcslUnaryPredicate.getOperator()) {
      case NEGATION -> bfmgr.not(operandFormula);
    };
  }

  @Override
  public BooleanFormula visit(AcslIdPredicate pAcslIdPredicate) throws NoException {
    AcslPredicateDeclaration declaration = pAcslIdPredicate.getDeclaration();
    return bfmgr.makeVariable(declaration.getName());
  }

  @Override
  public BooleanFormula visit(AcslBinaryTermPredicate pAcslBinaryTermPredicate) throws NoException {
    Formula operand1Formula = pAcslBinaryTermPredicate.getOperand1().accept(termVisitor);
    Formula operand2Formula = pAcslBinaryTermPredicate.getOperand2().accept(termVisitor);

    boolean signed = true;

    // Bitvector case: signed is important
    if (operand1Formula instanceof BitvectorFormula
        && operand2Formula instanceof BitvectorFormula) {
      signed = isSigned(pAcslBinaryTermPredicate.getOperand1().getExpressionType());
    }

    return switch (pAcslBinaryTermPredicate.getOperator()) {
      case EQUALS -> fmgr.makeEqual(operand1Formula, operand2Formula);
      case NOT_EQUALS -> bfmgr.not(fmgr.makeEqual(operand1Formula, operand2Formula));
      case LESS_EQUAL -> fmgr.makeLessOrEqual(operand1Formula, operand2Formula, signed);
      case GREATER_EQUAL -> fmgr.makeGreaterOrEqual(operand1Formula, operand2Formula, signed);
      case LESS_THAN -> fmgr.makeLessThan(operand1Formula, operand2Formula, signed);
      case GREATER_THAN -> fmgr.makeGreaterThan(operand1Formula, operand2Formula, signed);
    };
  }

  @Override
  public BooleanFormula visit(AcslOldPredicate pAcslOldPredicate) throws NoException {
    if (functionEntrySsa.isEmpty()) {
      throw new UnsupportedOperationException(
          "\\old is not available without a SSA map at function entry");
    }

    AcslPredicateToFormulaVisitor oldVisitor =
        new AcslPredicateToFormulaVisitor(
            fmgr,
            functionEntrySsa.orElseThrow().builder(),
            functionEntrySsa.orElseThrow(),
            ctoFormulaConverter,
            machineModel);

    return pAcslOldPredicate.getExpression().accept(oldVisitor);
  }

  @Override
  public BooleanFormula visit(AcslBooleanLiteralPredicate pAcslBooleanLiteralPredicate)
      throws NoException {
    return bfmgr.makeBoolean(pAcslBooleanLiteralPredicate.getValue());
  }

  @Override
  public BooleanFormula visit(AcslTernaryPredicate pAcslTernaryPredicate) throws NoException {
    BooleanFormula conditionFormula = pAcslTernaryPredicate.getCondition().accept(this);
    BooleanFormula ifTrueFormula = pAcslTernaryPredicate.getResultIfTrue().accept(this);
    BooleanFormula ifFalseFormula = pAcslTernaryPredicate.getResultIfFalse().accept(this);

    // (condition AND trueFormula) OR ((NOT condition) AND falseFormula)
    return bfmgr.or(
        bfmgr.and(conditionFormula, ifTrueFormula),
        bfmgr.and(bfmgr.not(conditionFormula), ifFalseFormula));
  }

  @Override
  public BooleanFormula visit(AcslValidPredicate pAcslValidPredicate) throws NoException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public BooleanFormula visit(AcslForallPredicate pForallPredicate) throws NoException {
    // TODO implementation definitely needed
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public BooleanFormula visit(AcslExistsPredicate pAcslExistsPredicate) throws NoException {
    // TODO implementation definitely needed
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public BooleanFormula visit(AcslPredicateApplicationPredicate pAcslPredicateApplicationPredicate)
      throws NoException {
    // TODO implementation definitely needed
    throw new UnsupportedOperationException("Not yet implemented");
  }

  private boolean isSigned(AcslType type) {

    if (!(type instanceof AcslCType cType)) {
      return true;
    }
    CType underlyingCType = cType.getType().getCanonicalType();

    if (underlyingCType instanceof CSimpleType simpleType) {
      return machineModel.isSigned(simpleType);
    }

    return true;
    // add pointer type as unsigned => false
    // function, predicate, polymorphic types and set should cause exceptions
    // acsl builtin logic types are all signed => true
    // get this out of this class so the other visitor can use it too
  }
}
