// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.acsltoformula;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslArraySubscriptTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslAtTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBooleanLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslCType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslCharLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslFunctionCallTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslFunctionType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIdTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIntegerLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslLogicType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslOldTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPointerType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicateType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslRealLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslResultTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslSetType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslStringLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTermVisitor;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTernaryTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryTerm;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
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
  private CtoFormulaConverter ctoFormulaConverter;
  private MachineModel machineModel;

  public AcslTermToFormulaVisitor(
      FormulaManagerView pFmgr,
      SSAMapBuilder pCurrentSsa,
      CtoFormulaConverter pCtoFormulaConverter,
      MachineModel pMachineModel) {
    checkNotNull(pFmgr);
    checkNotNull(pCurrentSsa);
    checkNotNull(pMachineModel);
    this.fmgr = pFmgr;
    this.bfmgr = fmgr.getBooleanFormulaManager();
    this.currentSsa = pCurrentSsa;
    this.functionEntrySsa = Optional.empty();
    this.ctoFormulaConverter = pCtoFormulaConverter;
    this.machineModel = pMachineModel;
  }

  public AcslTermToFormulaVisitor(
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
    this.currentSsa = pCurrentSsa;
    this.functionEntrySsa = Optional.ofNullable(pFunctionEntrySsa);
    this.ctoFormulaConverter = pCtoFormulaConverter;
    this.machineModel = pMachineModel;
  }

  @Override
  public Formula visit(AcslUnaryTerm pAcslUnaryTerm) throws NoException {
    // TODO implementation definitely needed
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public Formula visit(AcslStringLiteralTerm pAcslStringLiteralTerm) throws NoException {
    // return ctoFormulaConverter.makeAnd(PathFormula.createManually(bfmgr.makeTrue(),
    // SSAMap.emptySSAMap(), PointerTargetSet.emptyPointerTargetSet(), 0), TODO create edge here,
    // ErrorConditions.dummyInstance(fmgr.getBooleanFormulaManager()));
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
    // TODO implementation definitely needed
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public Formula visit(AcslIdTerm pAcslIdTerm) throws NoException {
    AcslSimpleDeclaration variable = pAcslIdTerm.getDeclaration();
    String varName = variable.getQualifiedName();
    int useIndex = getIndex(varName, variable.getType());

    return fmgr.makeVariable(getFormulaType(pAcslIdTerm.getExpressionType()), varName, useIndex);
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
            functionEntrySsa.get().builder(),
            functionEntrySsa.get(),
            ctoFormulaConverter,
            machineModel);

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
            fmgr, this, currentSsa, functionEntrySsa, ctoFormulaConverter, machineModel);
    BooleanFormula conditionFormula = pAcslTernaryTerm.getCondition().accept(predicateVisitor);
    Formula ifTrueFormula = pAcslTernaryTerm.getResultIfTrue().accept(this);
    Formula ifFalseFormula = pAcslTernaryTerm.getResultIfFalse().accept(this);

    return bfmgr.ifThenElse(conditionFormula, ifTrueFormula, ifFalseFormula);
  }

  @Override
  public Formula visit(AcslFunctionCallTerm pAcslFunctionCallTerm) throws NoException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public Formula visit(AcslArraySubscriptTerm pAcslArraySubscriptTerm) throws NoException {
    throw new UnsupportedOperationException("Not yet implemented");
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

  private FormulaType<?> getFormulaType(AcslType acslType) {
    // TODO implement  more of the mapping
    return switch (acslType) {
      case AcslCType cType -> ctoFormulaConverter.getFormulaTypeFromType(cType.getType());
      case AcslFunctionType funcType ->
          throw new UnsupportedOperationException("Not yet implemented");
      case AcslLogicType logType -> throw new UnsupportedOperationException("Not yet implemented");
      case AcslPointerType poinType ->
          throw new UnsupportedOperationException("Not yet implemented");
      case AcslPredicateType predType -> FormulaType.BooleanType;
      case AcslSetType setType -> throw new UnsupportedOperationException("Not yet implemented");
    };
  }
}
