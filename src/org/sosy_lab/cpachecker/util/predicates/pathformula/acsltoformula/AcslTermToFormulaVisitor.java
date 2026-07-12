// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.acsltoformula;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Verify;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslArraySubscriptTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslAtTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBooleanLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslCExpression;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslCharLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslFunctionCallTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIdTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIntegerLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslOldTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslRealLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslResultTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslStringLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTermVisitor;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTernaryTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryTerm;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ErrorConditions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.acsltoformula.AcslTypeHelper.BinaryTermData;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.Constraints;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CToFormulaConverterWithPointerAliasing;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSetBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;

public class AcslTermToFormulaVisitor implements AcslTermVisitor<Formula, NoException> {

  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final SSAMapBuilder currentSsa;
  private final Optional<SSAMap>
      functionEntrySsa; // Optional SSA map for function-entry state (\old)
  private final CToFormulaConverterWithPointerAliasing ctoFormulaConverter;
  private final MachineModel machineModel;
  private final AcslTypeHelper typeHelper;
  private final PointerTargetSetBuilder
      ptsb; // needed for CRightHandSideVisitor to convert AcslCExpressions
  private final PointerTargetSet
      originalPts; // copy to ensure we do not accidentally modify the original pts
  private final Constraints
      constraints; // needed for CRightHandSideVisitor to convert AcslCExpressions

  public AcslTermToFormulaVisitor(
      FormulaManagerView pFmgr,
      SSAMapBuilder pCurrentSsa,
      CToFormulaConverterWithPointerAliasing pCtoFormulaConverter,
      MachineModel pMachineModel,
      PointerTargetSetBuilder pPtsb,
      Constraints pCon) {
    checkNotNull(pFmgr);
    checkNotNull(pCurrentSsa);
    checkNotNull(pMachineModel);
    this.fmgr = pFmgr;
    this.bfmgr = fmgr.getBooleanFormulaManager();
    this.currentSsa = pCurrentSsa;
    this.functionEntrySsa = Optional.empty();
    this.ctoFormulaConverter = pCtoFormulaConverter;
    this.machineModel = pMachineModel;
    this.typeHelper = new AcslTypeHelper(pMachineModel, pFmgr, pCtoFormulaConverter);
    this.ptsb = pPtsb;
    this.originalPts = pPtsb.build();
    this.constraints = pCon;
  }

  public AcslTermToFormulaVisitor(
      FormulaManagerView pFmgr,
      SSAMapBuilder pCurrentSsa,
      SSAMap pFunctionEntrySsa,
      CToFormulaConverterWithPointerAliasing pCtoFormulaConverter,
      MachineModel pMachineModel,
      PointerTargetSetBuilder pPtsb,
      Constraints pCon) {
    checkNotNull(pFmgr);
    checkNotNull(pCurrentSsa);
    checkNotNull(pMachineModel);
    this.fmgr = pFmgr;
    this.bfmgr = fmgr.getBooleanFormulaManager();
    this.currentSsa = pCurrentSsa;
    this.functionEntrySsa = Optional.ofNullable(pFunctionEntrySsa);
    this.ctoFormulaConverter = pCtoFormulaConverter;
    this.machineModel = pMachineModel;
    this.typeHelper = new AcslTypeHelper(pMachineModel, pFmgr, pCtoFormulaConverter);
    this.ptsb = pPtsb;
    this.originalPts = pPtsb.build();
    this.constraints = pCon;
  }

  @Override
  public Formula visit(AcslUnaryTerm pAcslUnaryTerm) throws NoException {
    // TODO handle pointer and address operators
    // TODO check negation vs bitwise complementation according to ACSL standard
    Formula operandFormula = pAcslUnaryTerm.getOperand().accept(this);

    return switch (pAcslUnaryTerm.getOperator()) {
      case SIZEOF -> throw new UnsupportedOperationException("Not yet implemented");
      case PLUS -> operandFormula; // unary plus should not change the value
      case MINUS -> fmgr.makeNegate(operandFormula);
      case POINTER_DEREFERENCE -> throw new UnsupportedOperationException("Not yet implemented");
      case ADDRESS_OF -> throw new UnsupportedOperationException("Not yet implemented");
      case NEGATION -> fmgr.makeNot(operandFormula);
    };
  }

  @Override
  public Formula visit(AcslStringLiteralTerm pAcslStringLiteralTerm) throws NoException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public Formula visit(AcslRealLiteralTerm pAcslRealLiteralTerm) throws NoException {
    return fmgr.getRationalFormulaManager().makeNumber(pAcslRealLiteralTerm.getValue());
  }

  @Override
  public Formula visit(AcslCharLiteralTerm pAcslCharLiteralTerm) throws NoException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public Formula visit(AcslIntegerLiteralTerm pAcslIntegerLiteralTerm) throws NoException {
    return fmgr.getIntegerFormulaManager().makeNumber(pAcslIntegerLiteralTerm.getValue());
  }

  @Override
  public Formula visit(AcslBooleanLiteralTerm pAcslBooleanLiteralTerm) {
    return fmgr.getBooleanFormulaManager().makeBoolean(pAcslBooleanLiteralTerm.getValue());
  }

  @Override
  public Formula visit(AcslBinaryTerm pAcslBinaryTerm) throws NoException {
    AcslType operand1Type = pAcslBinaryTerm.getOperand1().getExpressionType();
    AcslType operand2Type = pAcslBinaryTerm.getOperand2().getExpressionType();
    AcslType termType = pAcslBinaryTerm.getExpressionType();

    Formula operand1Formula = pAcslBinaryTerm.getOperand1().accept(this);
    Formula operand2Formula = pAcslBinaryTerm.getOperand2().accept(this);

    BinaryTermData result =
        typeHelper.handleBinaryTerm(
            termType, operand1Type, operand2Type, operand1Formula, operand2Formula);

    Boolean signed = result.signed();
    operand1Formula = result.f1();
    operand2Formula = result.f2();

    return switch (pAcslBinaryTerm.getOperator()) {
      // TODO make sure that fmgr really does bitwise and, or etc. here as I suspect
      case BINARY_AND -> fmgr.makeAnd(operand1Formula, operand2Formula);
      case BINARY_OR -> fmgr.makeOr(operand1Formula, operand2Formula);
      // bitwise a -> b is the same as bitwise not a or b
      case BINARY_IMPLICATION -> fmgr.makeOr(fmgr.makeNot(operand1Formula), operand2Formula);
      // bitwise a <-> b is the same as bitwise not (a xor b)
      case BINARY_EQUIVALENT -> fmgr.makeNot(fmgr.makeXor(operand1Formula, operand2Formula));
      case BINARY_XOR -> fmgr.makeXor(operand1Formula, operand2Formula);
      case PLUS -> fmgr.makePlus(operand1Formula, operand2Formula);
      case MINUS -> fmgr.makeMinus(operand1Formula, operand2Formula);
      case MULTIPLY -> fmgr.makeMultiply(operand1Formula, operand2Formula);
      case DIVIDE -> fmgr.makeDivide(operand1Formula, operand2Formula, signed);
      case MODULO -> fmgr.makeRemainder(operand1Formula, operand2Formula, signed);
      case SHIFT_LEFT -> fmgr.makeShiftLeft(operand1Formula, operand2Formula);
      case SHIFT_RIGHT -> fmgr.makeShiftRight(operand1Formula, operand2Formula, signed);
    };
  }

  @Override
  public Formula visit(AcslIdTerm pAcslIdTerm) throws NoException {
    AcslSimpleDeclaration variable = pAcslIdTerm.getDeclaration();
    String varName = variable.getName();
    // Quantifier variables do not need tracking in the SSA map, use index 1 for them
    int useIndex = varName.startsWith("ACSL#q") ? 1 : getIndex(varName, variable.getType());

    return fmgr.makeVariable(
        typeHelper.acslTypeToFormulaType(pAcslIdTerm.getExpressionType()), varName, useIndex);
  }

  @Override
  public Formula visit(AcslOldTerm pAcslOldTerm) throws NoException {
    if (functionEntrySsa.isEmpty()) {
      throw new UnsupportedOperationException(
          "\\old is not available without a SSA map at function entry");
    }

    AcslTermToFormulaVisitor oldVisitor =
        new AcslTermToFormulaVisitor(
            fmgr,
            functionEntrySsa.orElseThrow().builder(),
            functionEntrySsa.orElseThrow(),
            ctoFormulaConverter,
            machineModel,
            ptsb,
            constraints);

    return pAcslOldTerm.getTerm().accept(oldVisitor);
  }

  @Override
  public Formula visit(AcslResultTerm pAcslResultTerm) throws NoException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public Formula visit(AcslAtTerm pAcslAtTerm) throws NoException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public Formula visit(AcslTernaryTerm pAcslTernaryTerm) throws NoException {
    AcslPredicateToFormulaVisitor predicateVisitor =
        new AcslPredicateToFormulaVisitor(
            fmgr, this, functionEntrySsa, ctoFormulaConverter, machineModel, ptsb, constraints);
    BooleanFormula conditionFormula = pAcslTernaryTerm.getCondition().accept(predicateVisitor);
    Formula ifTrueFormula = pAcslTernaryTerm.getResultIfTrue().accept(this);
    Formula ifFalseFormula = pAcslTernaryTerm.getResultIfFalse().accept(this);

    return bfmgr.ifThenElse(conditionFormula, ifTrueFormula, ifFalseFormula);
  }

  @Override
  public Formula visit(AcslFunctionCallTerm pAcslFunctionCallTerm) throws NoException {

    AcslFunctionDeclaration declaration = pAcslFunctionCallTerm.getDeclaration();
    FormulaType<?> returnType =
        typeHelper.acslTypeToFormulaType((AcslType) declaration.getType().getReturnType());

    String functionName = "ACSL#" + declaration.getQualifiedName();

    List<Formula> params = new ArrayList<>();
    for (AcslTerm param : pAcslFunctionCallTerm.getParameterExpressions()) {
      params.add(param.accept(this));
    }

    // Calling declareAndCallUF multiple times with the same function name does not seem to create
    // any issues (see: AcslToFormulaVisitorsTest.testPredicateApplication(), where this also
    // happens)
    return fmgr.getFunctionFormulaManager().declareAndCallUF(functionName, returnType, params);
  }

  @Override
  public Formula visit(AcslArraySubscriptTerm pAcslArraySubscriptTerm) throws NoException {
    // TODO this will be needed for my thesis examples
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public Formula visit(AcslCExpression pAcslCExpression) {
    try {
      Formula f = cExpressionToFormula(pAcslCExpression.getCExpression(), ptsb);
      Verify.verify(
          ptsb.build().equals(originalPts)); // make sure we did not modify the original pts
      return f;
    } catch (UnrecognizedCodeException ex) {
      throw new RuntimeException(ex);
    }
  }

  /**
   * Returns the index of a variable in the ssa map, or creates one with value 1 for new variables
   * (A bit ugly because of code duplication, might remove later if there is another way)
   *
   * @return the index of the variable
   */
  private int getIndex(String name, AcslType type) {
    Type existingType = currentSsa.getType(name);
    if (existingType != null && !type.equals(existingType)) {
      throw new IllegalArgumentException(
          "Variable "
              + name
              + " has conflicting types: "
              + currentSsa.getType(name)
              + " and "
              + type);
    }

    int idx = currentSsa.getIndex(name);
    if (idx <= 0) {
      idx = 1; // uninitialized variable
      currentSsa.setIndex(name, type, idx);
    }
    return idx;
  }

  public Formula cExpressionToFormula(CExpression cExpr, PointerTargetSetBuilder pPts)
      throws UnrecognizedCodeException {

    CFAEdge dummyEdge =
        new BlankEdge(
            "",
            FileLocation.DUMMY,
            CFANode.newDummyCFANode("dummy-1"),
            CFANode.newDummyCFANode("dummy-2"),
            "Dummy Edge");

    ErrorConditions errorConditions = new ErrorConditions(bfmgr);

    CRightHandSideVisitor<Formula, UnrecognizedCodeException> exprVisitor =
        ctoFormulaConverter.createCRightHandSideVisitor(
            dummyEdge, "dummy-function-name", currentSsa, pPts, constraints, errorConditions);
    Formula f = cExpr.accept(exprVisitor);

    // TODO should the adding to the constraints really just be a side effect?
    return f;
  }
}
