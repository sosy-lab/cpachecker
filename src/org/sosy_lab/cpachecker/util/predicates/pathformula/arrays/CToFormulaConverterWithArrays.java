/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.util.predicates.pathformula.arrays;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.VariableClassification;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ErrorConditions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.Constraints;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaTypeHandler;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.FormulaEncodingOptions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSetBuilder;

import com.google.common.base.Optional;


public class CToFormulaConverterWithArrays extends CtoFormulaConverter {

  public CToFormulaConverterWithArrays(FormulaEncodingOptions pOptions, FormulaManagerView pFmgr,
      MachineModel pMachineModel, Optional<VariableClassification> pVariableClassification, LogManager pLogger,
      ShutdownNotifier pShutdownNotifier, CtoFormulaTypeHandler pTypeHandler, AnalysisDirection pDirection) {
    super(pOptions, pFmgr, pMachineModel, pVariableClassification, pLogger, pShutdownNotifier, pTypeHandler, pDirection);
  }

  @Override
  protected CRightHandSideVisitor<Formula, UnrecognizedCCodeException> createCRightHandSideVisitor(
      CFAEdge pEdge, String pFunction,
      SSAMapBuilder ssa, PointerTargetSetBuilder pts,
      Constraints constraints, ErrorConditions errorConditions) {
    return new ExpressionToFormulaVisitorWithArrays(this, fmgr, machineModel, pEdge, pFunction, ssa, constraints);
  }

  @Override
  protected BooleanFormula makeDeclaration(CDeclarationEdge pEdge, String pFunction, SSAMapBuilder pSsa,
      PointerTargetSetBuilder pPts, Constraints pConstraints, ErrorConditions pErrorConditions)
      throws CPATransferException, InterruptedException {

    return super.makeDeclaration(pEdge, pFunction, pSsa, pPts, pConstraints, pErrorConditions);
  }

  @Override
  protected BooleanFormula makeAssignment(CLeftHandSide pLhs, CLeftHandSide pLhsForChecking, CRightHandSide pRhs,
      CFAEdge pEdge, String pFunction, SSAMapBuilder pSsa, PointerTargetSetBuilder pPts, Constraints pConstraints,
      ErrorConditions pErrorConditions) throws UnrecognizedCCodeException, InterruptedException {

    return super.makeAssignment(pLhs, pLhsForChecking, pRhs, pEdge, pFunction, pSsa, pPts, pConstraints, pErrorConditions);
  }

  @Override
  protected BooleanFormula makePredicate(CExpression pExp, boolean pIsTrue, CFAEdge pEdge, String pFunction,
      SSAMapBuilder pSsa, PointerTargetSetBuilder pPts, Constraints pConstraints, ErrorConditions pErrorConditions)
      throws UnrecognizedCCodeException, InterruptedException {

    return super.makePredicate(pExp, pIsTrue, pEdge, pFunction, pSsa, pPts, pConstraints, pErrorConditions);
  }

  @Override
  protected BooleanFormula makeReturn(Optional<CAssignment> pAssignment, CReturnStatementEdge pEdge, String pFunction,
      SSAMapBuilder pSsa, PointerTargetSetBuilder pPts, Constraints pConstraints, ErrorConditions pErrorConditions)
      throws CPATransferException, InterruptedException {

    return super.makeReturn(pAssignment, pEdge, pFunction, pSsa, pPts, pConstraints, pErrorConditions);
  }

  Formula makeVariableForMe(String pName, CType pType, SSAMapBuilder pSsa) {
    return makeVariable(pName, pType, pSsa);
  }

  @Override
  protected Formula makeCast(CType pFromType, CType pToType, Formula pFormula, Constraints pConstraints, CFAEdge pEdge)
      throws UnrecognizedCCodeException {

    CType fromType = pFromType.getCanonicalType();
    if (fromType instanceof CArrayType) {
      fromType = ((CArrayType) fromType).getType();
    }

    return super.makeCast(fromType, pToType, pFormula, pConstraints, pEdge);
  }

  Formula visibleMakeCast(CType pFromType, CType pToType, Formula pFormula, Constraints pConstraints, CFAEdge pEdge)
      throws UnrecognizedCCodeException {
    return makeCast(pFromType, pToType, pFormula, pConstraints, pEdge);
  }



}
