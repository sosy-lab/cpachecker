/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.cpa.arraySegmentation.util.transfer;

import com.google.common.base.Throwables;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.simplification.ExpressionSimplificationVisitor;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.ArraySegment;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.ArraySegmentationState;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.ExtendedCompletLatticeAbstractState;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.ExtendedLocationArrayContentCPA;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.FinalSegment;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.formula.FormulaRelation;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.usageAnalysis.UsageAnalysisTransferRelation;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.util.ArrayModificationException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.SolverException;

public class CSegmentationModifier<T extends ExtendedCompletLatticeAbstractState<T>> {

  private LogManager logger;
  private MachineModel machineModel;
  private ExpressionSimplificationVisitor visitor;
  private CBinaryExpressionBuilder builder;

  public CSegmentationModifier(
      LogManager pLogger,
      MachineModel pMachineModel,
      ExpressionSimplificationVisitor pVisitor) {
    super();
    logger = pLogger;
    machineModel = pMachineModel;
    visitor = pVisitor;
    builder = new CBinaryExpressionBuilder(machineModel, logger);
  }

  public ArraySegmentationState<T> storeAnalysisInformationAtIndex(
      ArraySegmentationState<T> state,
      CExpression pIndex,
      T pAnalysisInfo,
      boolean pNewSegmentIsPotentiallyEmpty,
      CFAEdge pCfaEdge)
      throws ArrayModificationException {

    // Check, if the expression used to access the array element is present in the current state
    int pos = state.getSegBoundContainingExpr(pIndex);
    if (pos < 0) {
      // Check, if we can compute an interval for the index being not present using the assumption
      // that 0 <= i and i <= SIZE and check, if the analysis information that hold be stored in the
      // interval is already present in the over-approximated interval computed for i
      boolean error = false;

      try {
        int lowerBound = computeLowerBound(state, pIndex, pCfaEdge);
        int upperBound = computeUpperBound(state, pIndex, pCfaEdge);
        for (int i = lowerBound; i < upperBound; i++) {
          if (!state.getSegments().get(i).getAnalysisInformation().equals(pAnalysisInfo)) {
            error = true;
          }
        }
        if (!error && upperBound > lowerBound) {
          return state;
        } else {
          String errorMsg =
              UsageAnalysisTransferRelation.PREFIX
                  + "Cannot create a usage since the variable "
                  + pIndex.toASTString()
                  + " is not present in the segmentation, hence the error symbol is returned. Current State is: "
                  + state.toDOTLabel();
          logger.log(ExtendedLocationArrayContentCPA.GENERAL_LOG_LEVEL, errorMsg);
          throw new ArrayModificationException(errorMsg);
        }

      } catch (SolverException | InterruptedException e) {
        String errorMsg =
            UsageAnalysisTransferRelation.PREFIX
                + "Cannot create a usage since the variable "
                + pIndex.toASTString()
                + " is not present in the segmentation, hence the error symbol is returned. Current State is: "
                + state.toDOTLabel();
        logger.log(ExtendedLocationArrayContentCPA.GENERAL_LOG_LEVEL, errorMsg);
        throw new ArrayModificationException(errorMsg);
      }
    } else {
      // Create a new segment after the segment containing the expression to access the array
      // elements and mark this as used
      ArraySegment<T> leftBound = state.getSegments().get(pos);
      CExpression exprPlus1;
      try {
        exprPlus1 =
            visitor.visit(
                builder.buildBinaryExpression(
                    pIndex,
                    CIntegerLiteralExpression.ONE,
                    CBinaryExpression.BinaryOperator.PLUS));
      } catch (UnrecognizedCodeException e) {
        String errorMsg =
            UsageAnalysisTransferRelation.PREFIX
                + "Cannot create a usage due to internal problems, hence the error symbol is returned. Current State is: "
                + state.toDOTLabel()
                + " for the index :"
                + pIndex.toString();
        logger.log(ExtendedLocationArrayContentCPA.GENERAL_LOG_LEVEL, errorMsg);
        throw new ArrayModificationException(errorMsg, e);
      }
      if (leftBound.getNextSegment() instanceof FinalSegment) {
        throw new ArrayModificationException(
            "Cannot add information for an index nt present in the array range!");
      }
      if (!leftBound.getNextSegment().getSegmentBound().contains(exprPlus1)) {
        // Add the segment bound
        List<AExpression> bounds = new ArrayList<>();
        bounds.add(exprPlus1);
        ArraySegment<T> newSeg =
            new ArraySegment<>(
                bounds,
                leftBound.getAnalysisInformation(),
                true,
                null,
                state.getLanguage());
        state.addSegment(newSeg, leftBound);
      }
      return storeAnalysisInformationAtIndexWithoutAddingBounds(
          state,
          pIndex,
          pAnalysisInfo,
          pNewSegmentIsPotentiallyEmpty);
    }
  }

  private int
      computeUpperBound(ArraySegmentationState<T> pState, CExpression pIndex, CFAEdge pCfaEdge)
          throws SolverException, InterruptedException {
    int index = pState.getSegments().size() - 1;
    BooleanFormula formula = pState.getPathFormula().getPathFormula().getFormula();
    FormulaRelation pr = pState.getPathFormula().getPr();
    Solver solver = pr.getSolver();

    // Compute for each segment present in the segmentation, if for any expression e it holds that:
    // pINdex < e is SAT, (< since e is the upper bound of the interval not included, hence
    // everything up to e need to fulfill the condition
    for (int i = pState.getSegments().size() - 1; i >= 0; i--) {
      ArraySegment<T> sb = pState.getSegments().get(i);
      for (AExpression e : sb.getSegmentBound()) {
        Optional<BooleanFormula> smaller =
            getBooleanFormula(
                pIndex,
                (CExpression) e,
                BinaryOperator.LESS_THAN,
                pr.getConverter(),
                pState.getPathFormula().getPathFormula(),
                pCfaEdge,
                pr.getFormulaManager());
        if (smaller.isPresent()) {
          if (solver.implies(formula, smaller.get())) {
            index = i;
            break;
          }
        }
      }
    }
    return index;
  }

  private int
      computeLowerBound(ArraySegmentationState<T> pState, CExpression pIndex, CFAEdge pCfaEdge)
          throws SolverException, InterruptedException {
    int index = 0;
    BooleanFormula formula = pState.getPathFormula().getPathFormula().getFormula();
    FormulaRelation pr = pState.getPathFormula().getPr();
    Solver solver = pr.getSolver();

    // Compute for each segment present in the segmentation, if for any expression e it holds that:
    // pINdex >= e is SAT (since the lower bound is included!)
    for (int i = 1; i < pState.getSegments().size(); i++) {
      ArraySegment<T> sb = pState.getSegments().get(i);
      for (AExpression e : sb.getSegmentBound()) {
        Optional<BooleanFormula> smaller =
            getBooleanFormula(
                pIndex,
                (CExpression) e,
                BinaryOperator.GREATER_EQUAL,
                pr.getConverter(),
                pState.getPathFormula().getPathFormula(),
                pCfaEdge,
                pr.getFormulaManager());
        if (smaller.isPresent()) {

          if (solver.implies(formula, smaller.get())) {
            index = i;
            break;

          }
        }
      }
    }
    return index;
  }

  public ArraySegmentationState<T> storeAnalysisInformationAtIndexWithoutAddingBounds(
      ArraySegmentationState<T> state,
      CExpression pIndex,
      T pAnalysisInfo,
      boolean pNewSegmentIsPotentiallyEmpty)
      throws ArrayModificationException {
    // Check, if the expression used to access the array element is present in the current state
    // Check, if index+1 is the following segment of the one containing pIndex

    CExpression exprPlus1;
    try {
      exprPlus1 =
          visitor.visit(
              builder.buildBinaryExpression(
                  pIndex,
                  CIntegerLiteralExpression.ONE,
                  CBinaryExpression.BinaryOperator.PLUS));
    } catch (UnrecognizedCodeException e) {
      String errorMsg =
          UsageAnalysisTransferRelation.PREFIX
              + "Cannot create a usage due to internal problems, hence the error symbol is returned. Current State is: "
              + state.toDOTLabel()
              + " for the index :"
              + pIndex.toString();
      logger.log(ExtendedLocationArrayContentCPA.GENERAL_LOG_LEVEL, errorMsg);
      throw new ArrayModificationException(errorMsg, e);
    }
    int pos = state.getSegBoundContainingExpr(pIndex);
    int posNext = state.getSegBoundContainingExpr(exprPlus1);

    if (pos < 0 || pos != posNext - 1) {
      String errorMsg =
          UsageAnalysisTransferRelation.PREFIX
              + "Cannot create a usage since the variable "
              + pIndex.toASTString()
              + " or "
              + pIndex.toASTString()
              + "+1 is not present in the segmentation, hence the error symbol is returned. Current State is: "
              + state.toDOTLabel();
      logger.log(ExtendedLocationArrayContentCPA.GENERAL_LOG_LEVEL, errorMsg);
      throw new ArrayModificationException(errorMsg);
    } else {

      ArraySegment<T> leftBound = state.getSegments().get(pos);
      leftBound.setAnalysisInformation(pAnalysisInfo);
      leftBound.setPotentiallyEmpty(pNewSegmentIsPotentiallyEmpty);

      return state;
    }

  }

  private Optional<BooleanFormula> getBooleanFormula(
      CExpression pExpr1,
      CExpression pExpr2,
      BinaryOperator pOp,
      CtoFormulaConverter converter,
      PathFormula pPathFormula,
      CFAEdge pCfaEdge,
      FormulaManagerView manager) {

    CBinaryExpression sizeGEqZero;
    try {
      sizeGEqZero = builder.buildBinaryExpression(pExpr1, pExpr2, pOp);

      Formula ifThenElseFormulaGEQ =
          converter.buildTermFromPathFormula(pPathFormula, sizeGEqZero, pCfaEdge);
      Optional<Triple<BooleanFormula, Formula, Formula>> bfGEQOptional =
          manager.splitIfThenElse(ifThenElseFormulaGEQ);
      if (bfGEQOptional.isPresent()) {
        return Optional.of(bfGEQOptional.get().getFirst());
      }
    } catch (UnrecognizedCodeException e) {
      logger.log(ExtendedLocationArrayContentCPA.GENERAL_LOG_LEVEL,
          Throwables.getStackTraceAsString(e));
      return Optional.empty();
    }
    return Optional.empty();

  }
}
