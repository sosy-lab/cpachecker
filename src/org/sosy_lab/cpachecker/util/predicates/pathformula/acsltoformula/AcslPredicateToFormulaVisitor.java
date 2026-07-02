// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.acsltoformula;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTermPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBooleanLiteralPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslExistsPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslForallPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIdPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslOldPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicateApplicationPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicateDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicateVisitor;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTernaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslValidPredicate;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CToFormulaConverterWithPointerAliasing;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSetBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.QuantifiedFormulaManagerView;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;

public class AcslPredicateToFormulaVisitor
    implements AcslPredicateVisitor<BooleanFormula, NoException> {

  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final AcslTermToFormulaVisitor termVisitor;
  private final CToFormulaConverterWithPointerAliasing ctoFormulaConverter;
  private final AcslTypeHelper typeHelper;

  @SuppressWarnings("unused") // I suspect currentSsa will be needed at some point
  private final SSAMapBuilder currentSsa;

  private final MachineModel machineModel;
  private final PointerTargetSetBuilder ptsb;
  private final Optional<SSAMap>
      functionEntrySsa; // Optional SSA map for function-entry state (\old)

  // Counter for renaming binders in quantifiers to ensure unique names
  private int renamingCounter = 0;

  public AcslPredicateToFormulaVisitor(
      FormulaManagerView pFmgr,
      SSAMapBuilder pCurrentSsa,
      CToFormulaConverterWithPointerAliasing pCtoFormulaConverter,
      MachineModel pMachineModel,
      PointerTargetSetBuilder pPtsb) {
    checkNotNull(pFmgr);
    checkNotNull(pCurrentSsa);
    checkNotNull(pMachineModel);
    this.fmgr = pFmgr;
    this.bfmgr = fmgr.getBooleanFormulaManager();
    this.termVisitor =
        new AcslTermToFormulaVisitor(
            pFmgr, pCurrentSsa, pCtoFormulaConverter, pMachineModel, pPtsb);
    this.currentSsa = pCurrentSsa;
    this.ctoFormulaConverter = pCtoFormulaConverter;
    this.functionEntrySsa = Optional.empty();
    this.machineModel = pMachineModel;
    this.ptsb = pPtsb;
    this.typeHelper = new AcslTypeHelper(pMachineModel, pFmgr, pCtoFormulaConverter);
  }

  public AcslPredicateToFormulaVisitor(
      FormulaManagerView pFmgr,
      SSAMapBuilder pCurrentSsa,
      SSAMap pFunctionEntrySsa,
      CToFormulaConverterWithPointerAliasing pCtoFormulaConverter,
      MachineModel pMachineModel,
      PointerTargetSetBuilder pPtsb) {
    checkNotNull(pFmgr);
    checkNotNull(pCurrentSsa);
    checkNotNull(pMachineModel);
    this.fmgr = pFmgr;
    this.bfmgr = fmgr.getBooleanFormulaManager();
    this.termVisitor =
        new AcslTermToFormulaVisitor(
            pFmgr, pCurrentSsa, pFunctionEntrySsa, pCtoFormulaConverter, pMachineModel, pPtsb);
    this.currentSsa = pCurrentSsa;
    this.ctoFormulaConverter = pCtoFormulaConverter;
    this.functionEntrySsa = Optional.ofNullable(pFunctionEntrySsa);
    this.machineModel = pMachineModel;
    this.ptsb = pPtsb;
    this.typeHelper = new AcslTypeHelper(pMachineModel, pFmgr, pCtoFormulaConverter);
  }

  // Constructor that should only be called by AcslTermToFormulaVisitor
  // this is required to create the condition in a ternary term, e.g., x > 0 ? 1 : 2
  protected AcslPredicateToFormulaVisitor(
      FormulaManagerView pFmgr,
      AcslTermToFormulaVisitor pTermVisitor,
      SSAMapBuilder pCurrentSsa,
      Optional<SSAMap> oFunctionEntrySsa,
      CToFormulaConverterWithPointerAliasing pCtoFormulaConverter,
      MachineModel pMachineModel,
      PointerTargetSetBuilder pPtsb) {
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
    this.ptsb = pPtsb;
    this.typeHelper = new AcslTypeHelper(pMachineModel, pFmgr, pCtoFormulaConverter);
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
    AcslType operand1Type = pAcslBinaryTermPredicate.getOperand1().getExpressionType();
    AcslType operand2Type = pAcslBinaryTermPredicate.getOperand2().getExpressionType();

    Formula operand1Formula = pAcslBinaryTermPredicate.getOperand1().accept(termVisitor);
    Formula operand2Formula = pAcslBinaryTermPredicate.getOperand2().accept(termVisitor);

    boolean signed = true;

    // Bitvector case: signed is important
    if (operand1Formula instanceof BitvectorFormula
        && operand2Formula instanceof BitvectorFormula) {
      signed = typeHelper.isSigned(pAcslBinaryTermPredicate.getOperand1().getExpressionType());
    }

    if (!fmgr.getFormulaType(operand1Formula).equals(fmgr.getFormulaType(operand2Formula))) {
      AcslType commonType = AcslType.mostGeneralType(operand1Type, operand2Type);
      // TODO this is same typecasting as AcslBinaryTerm, so keep this up to date if you fixed the
      // other
      operand1Formula = typeHelper.convertFormulaType(operand1Formula, commonType);
      operand2Formula = typeHelper.convertFormulaType(operand2Formula, commonType);
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
            machineModel,
            ptsb);

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

    return bfmgr.ifThenElse(conditionFormula, ifTrueFormula, ifFalseFormula);
  }

  @Override
  public BooleanFormula visit(AcslValidPredicate pAcslValidPredicate) throws NoException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public BooleanFormula visit(AcslForallPredicate pForallPredicate) throws NoException {
    QuantifiedFormulaManagerView qfmr = fmgr.getQuantifiedFormulaManager();

    Map<AcslSimpleDeclaration, AcslSimpleDeclaration> renamingMap = new HashMap<>();
    List<AcslParameterDeclaration> newBinders = new ArrayList<>();
    for (AcslParameterDeclaration declaration : pForallPredicate.getBinders()) {
      String newName = "ACSL#q" + renamingCounter++ + "#" + declaration.getName();

      AcslParameterDeclaration renamed =
          new AcslParameterDeclaration(
              declaration.getFileLocation(), declaration.getType(), newName, newName);

      renamingMap.put(declaration, renamed);
      newBinders.add(renamed);
    }
    AcslRenamingVisitor renamingVisitor = new AcslRenamingVisitor(ImmutableMap.copyOf(renamingMap));

    AcslPredicate renamedBody = pForallPredicate.getPredicate().accept(renamingVisitor);
    BooleanFormula bodyF = renamedBody.accept(this);

    List<Formula> smtVars = new ArrayList<>();

    for (AcslParameterDeclaration decl : newBinders) {
      smtVars.add(createSmtVarFromBinder(decl));
    }

    return qfmr.forall(smtVars, bodyF);
  }

  @Override
  public BooleanFormula visit(AcslExistsPredicate pAcslExistsPredicate) throws NoException {
    QuantifiedFormulaManagerView qfmr = fmgr.getQuantifiedFormulaManager();

    Map<AcslSimpleDeclaration, AcslSimpleDeclaration> renamingMap = new HashMap<>();
    List<AcslParameterDeclaration> newBinders = new ArrayList<>();
    for (AcslParameterDeclaration declaration : pAcslExistsPredicate.getBinders()) {
      String newName = "ACSL#q" + renamingCounter++ + "#" + declaration.getName();

      AcslParameterDeclaration renamed =
          new AcslParameterDeclaration(
              declaration.getFileLocation(), declaration.getType(), newName, newName);

      renamingMap.put(declaration, renamed);
      newBinders.add(renamed);
    }
    AcslRenamingVisitor renamingVisitor = new AcslRenamingVisitor(ImmutableMap.copyOf(renamingMap));

    AcslPredicate renamedBody = pAcslExistsPredicate.getPredicate().accept(renamingVisitor);
    BooleanFormula bodyF = renamedBody.accept(this);

    List<Formula> smtVars = new ArrayList<>();

    for (AcslParameterDeclaration decl : newBinders) {
      smtVars.add(createSmtVarFromBinder(decl));
    }

    return qfmr.exists(smtVars, bodyF);
  }

  @Override
  public BooleanFormula visit(AcslPredicateApplicationPredicate pAcslPredicateApplicationPredicate)
      throws NoException {
    AcslPredicateDeclaration declaration =
        pAcslPredicateApplicationPredicate.getPredicateDeclaration();

    String predName = "ACSLPred#" + declaration.getQualifiedName();

    List<Formula> params = new ArrayList<>();
    for (AcslTerm param : pAcslPredicateApplicationPredicate.getParameters()) {
      params.add(param.accept(termVisitor));
    }

    return fmgr.getFunctionFormulaManager()
        .declareAndCallUF(predName, FormulaType.BooleanType, params);
  }

  private Formula createSmtVarFromBinder(AcslParameterDeclaration pDecl) {
    String varName = pDecl.getName();
    return fmgr.makeVariable(typeHelper.acslTypeToFormulaType(pDecl.getType()), varName);
  }
}
