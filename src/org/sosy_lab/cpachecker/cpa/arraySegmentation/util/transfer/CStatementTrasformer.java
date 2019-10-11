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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.simplification.ExpressionSimplificationVisitor;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.ArraySegment;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.ArraySegmentationState;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.ErrorSegmentation;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.ExtendedCompletLatticeAbstractState;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.FinalSegment;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.UnreachableSegmentation;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.usageAnalysis.UsageAnalysisTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class CStatementTrasformer<T extends ExtendedCompletLatticeAbstractState<T>> {

  private TransformationHelper<T> helper;
  private LogManager logger;
  ExpressionSimplificationVisitor visitor;

  public CStatementTrasformer(LogManager pLogger, ExpressionSimplificationVisitor pVisitor) {
    this.helper = new TransformationHelper<>(pLogger);
    this.logger = pLogger;
    this.visitor = pVisitor;

  }

  /**
   *
   * @param state a COPY of the current state
   * @param pStatement the statement to apply the transformation on
   * @return a modified version of the state NOT A COPY!
   * @throws CPATransferException if some errors occurred during cleanup
   */
  public @Nullable ArraySegmentationState<T>
      transform(ArraySegmentationState<T> state, CStatement pStatement)
          throws CPATransferException {

    // Check, if the LHS is a variable, else return
    if (pStatement instanceof CExpressionAssignmentStatement
        && ((CExpressionAssignmentStatement) pStatement)
            .getLeftHandSide() instanceof CIdExpression) {
      CExpressionAssignmentStatement stmt = (CExpressionAssignmentStatement) pStatement;
      CIdExpression var = (CIdExpression) stmt.getLeftHandSide();

      // Check, if the RHS contains the Var (reassignment)
      if (isReplacement(stmt.getRightHandSide(), var)) {
        // Case 1
        return replace(var, stmt.getRightHandSide(), state);
      } else {
        // Case 2
        return reassign(var, stmt.getRightHandSide(), state);
      }

    } // Handle FunctionCalls
    else if (pStatement instanceof CFunctionCallAssignmentStatement) {
      CFunctionCallAssignmentStatement call = (CFunctionCallAssignmentStatement) pStatement;
      // If a variable is assigned the return value of a function call, we loose all information
      // about the variable. Only in case if the variable is SIZE, we can reuse the assumption that
      // all variables in the segment are smaller to SIZE
      if (call.getLeftHandSide() instanceof CIdExpression
          && ((CIdExpression) call.getLeftHandSide()).equals(state.getSizeVar())) {
        // First, remove the expression and than add it to the end
        state = helper.cleanExprFromSegBounds((CIdExpression) call.getLeftHandSide(), state);
        if (null != state) {
          // Check, if the current last segment contains any analysis information, if not, add _|_?
          // to it. Anyway, mark it as potentially empty

          List<ArraySegment<T>> segments = new ArrayList<>(state.getSegments());
          int posCurrenLast = segments.size() - 1;
          T analysisInformation = segments.get(posCurrenLast).getAnalysisInformation();
          if (analysisInformation.equals(state.gettEmptyElement())) {
            segments.get(posCurrenLast)
                .setAnalysisInformation(state.gettEmptyElement().getBottomElement());
          }
          segments.get(posCurrenLast).setPotentiallyEmpty(true);
          ArrayList<AExpression> bounds = new ArrayList<>();
          bounds.add(call.getLeftHandSide());
          ArraySegment<T> lastSegment =
              new ArraySegment<>(
                  bounds,
                  state.gettEmptyElement(),
                  false,
                  new FinalSegment<>(state.gettEmptyElement()),
                  state.getLanguage());
          state.addSegment(lastSegment, state.getSegments().get(state.getSegments().size() - 1));

          return state;
        } else {
          throw new CPATransferException("Could not cleanup the segment bound");

        }
      } else if (call.getLeftHandSide() instanceof CIdExpression) {
        // Remove all occurrences of the variable

        state = helper.cleanExprFromSegBounds((CIdExpression) call.getLeftHandSide(), state);
        if (null != state) {
          return state;
        } else {
          throw new CPATransferException("Could not cleanup the segment bound");
        }
      }

    }
    return state;
  }

  public ArraySegmentationState<T>
      reassign(CIdExpression pVar, CExpression pRightHandSide, ArraySegmentationState<T> state) {
    CExpression canoncialForm = getCanonicalForm(pRightHandSide);
    List<ArraySegment<T>> exprList = new ArrayList<>();
    List<ArraySegment<T>> segments = new ArrayList<>(state.getSegments());
    for (ArraySegment<T> s : segments) {
      for (AExpression e : s.getSegmentBound()) {
        if (e.equals(canoncialForm)) {
          exprList.add(s);
        }
      }
    }
    if (exprList.size() > 1) {
      logger.log(
          Level.FINE,
          UsageAnalysisTransferRelation.PREFIX
              + "THe segmentation is invalid, since the expression that should be reassigned is present twice."
              + "Hence, the error symbol is returned. Current State is: "
              + state.toDOTLabel()
              + " for the expression :"
              + pVar.toASTString()
              + " := "
              + pRightHandSide.toASTString());
      return new ErrorSegmentation<>(state);
    } else if (exprList.size() == 1) {
      // Here, we are changing the ordering ( in the original transfer relation, the elements are
      // added firstly, than the others are removed. Anyway, changing these two steps leads to the
      // Exact same results!
      state = helper.cleanExprFromSegBounds(pVar, state);
      if (null == state) {
        logger.log(
            Level.FINE,
            UsageAnalysisTransferRelation.PREFIX
                + "The cleanup for the  current segmentation and expression "
                + pVar.toASTString()
                + " has failed. The error label is returned");
        return new ErrorSegmentation<>(state);
      }
      // Add pVar to pRightHandSide
      exprList.get(0).addSegmentBound(pVar);
      return state;
    } else {
      // The expression pRightHandSide is not present, hence check, where to split the segment
      // bounds. We are using the following assumptions:
      // 1. The variable pVar is smaller or equal to all expression in the last segment bound
      // 2. The expression pRightHandSide evaluates to a integer!
      // 3. The first segment contains the integer value 0!

      // TODO: Remove strong assumption, that pRightHandSide evaluates to integer
      if (canoncialForm instanceof CIntegerLiteralExpression) {

        state = helper.cleanExprFromSegBounds(pVar, state);
        if (null == state) {

          logger.log(
              Level.FINE,
              UsageAnalysisTransferRelation.PREFIX
                  + "The cleanup for the current segmentation and expression "
                  + pVar.toASTString()
                  + " has failed. The error label is returned");
          return new ErrorSegmentation<>(state);
        }

        // Get the greatest element strictly smaller than pRightHandSide
        // Since by assumption only one variable is tracked, all other expressions evaluate either
        // to an integer value, contains the variable pVar or are the last element!
        BigInteger valueOfExpr = ((CIntegerLiteralExpression) canoncialForm).getValue();

        // We can start at the second element, since by assumption 0 is always present and hence e >
        // 0
        boolean isAdded = false;
        for (int i = 1; i < segments.size(); i++) {
          ArraySegment<T> s = segments.get(i);
          BigInteger curValue = s.evaluateToInteger(visitor);
          if (curValue.compareTo(valueOfExpr) > 0) {
            // This is the first segment that is greater than the one needs to be added, hence add
            // it between the previous and this segment
            ArraySegment<T> prevSeg = segments.get(i - 1);
            List<AExpression> segBounds = new ArrayList<>();
            segBounds.add(pVar);
            segBounds.add(pRightHandSide);
            ArraySegment<T> newSeg =
                new ArraySegment<>(
                    segBounds,
                    prevSeg.getAnalysisInformation(),
                    prevSeg.isPotentiallyEmpty(),
                    s,
                    state.getLanguage());
            state.addSegment(newSeg, prevSeg);
            isAdded = true;
          }
        }
        // taking the assumption into account, that the variable pVar is smaller or equal to all
        // expression in the last segment bound, we can add it before the last segment!
        // We need to assume that there are at least two segments present. IN case that only a
        // single segment is present, nothing can be done!

        if (!isAdded && segments.size() > 1) {
          ArraySegment<T> prevSeg = segments.get(segments.size() - 2);
          List<AExpression> segBounds = new ArrayList<>();
          segBounds.add(pVar);
          segBounds.add(pRightHandSide);
          ArraySegment<T> newSeg =
              new ArraySegment<>(
                  segBounds,
                  prevSeg.getAnalysisInformation(),
                  prevSeg.isPotentiallyEmpty(),
                  segments.get(segments.size() - 1),
                  state.getLanguage());
          state.addSegment(newSeg, prevSeg);
        } else if (state.getSizeVar().equals(pVar)
            && segments.size() == 1
            && valueOfExpr.compareTo(BigInteger.ZERO) > 0) {
          // Reassignments of the SIZE, if the segmentation only contains the segmentation
          // containing 0
          ArraySegment<T> prevSeg = segments.get(0);
          // Since there is only one element present, set the bottom analysis information and mark
          // it as not empty, since the value SIZE is assigned to is greater to 0
          prevSeg.setAnalysisInformation(state.gettEmptyElement().getBottomElement());
          List<AExpression> segBounds = new ArrayList<>();
          segBounds.add(pVar);
          segBounds.add(pRightHandSide);
          ArraySegment<T> newSeg =
              new ArraySegment<>(
                  segBounds,
                  state.gettEmptyElement(),
                  prevSeg.isPotentiallyEmpty(),
                  segments.get(segments.size() - 1),
                  state.getLanguage());
          state.addSegment(newSeg, prevSeg);
        } else {
          // At this point, we know that: 1. 0 = SIZE, and the variable pVar := x , x \in N & x > 0.
          // If x would have been equal to 0, then pVar would have been added. Hence, the assumption
          // pVar <= SIZE is violated and the unreachable Segment is returned!
          return new UnreachableSegmentation<>(state);
        }
      } else {

        // TODO: Avoid this case
        return state;
      }

    }
    return state;
  }

  /**
   * Replace the variable pVar in all segment bounds with the inverse of pRightHandSide (meaning if
   * RHS = i+1 --> replaced with i-1 and vice versa)
   *
   * @param pVar the variable to replace
   * @param pRightHandSide the replacement
   * @param state where to replace
   * @return the state with replaced variable
   */
  public ArraySegmentationState<T> replace(
      CIdExpression pVar,
      CExpression pRightHandSide,
      @Nullable ArraySegmentationState<T> state) {
    CExpression reversedExpr = reverseIfNeccessary(pRightHandSide);
    CExpression canoncialForm = getCanonicalForm(reversedExpr);
    for (int i = 0; i < state.getSegments().size(); i++) {
      ArraySegment<T> s = state.getSegments().get(i);
      s.replaceVar(pVar, canoncialForm, visitor);
    }
    return state;
  }

  private CExpression reverseIfNeccessary(CExpression pRightHandSide) {
    if (pRightHandSide instanceof CBinaryExpression) {
      CBinaryExpression binary = (CBinaryExpression) pRightHandSide;
      switch (binary.getOperator()) {
        case PLUS:
          return new CBinaryExpression(
              binary.getFileLocation(),
              binary.getExpressionType(),
              binary.getCalculationType(),
              binary.getOperand1(),
              binary.getOperand2(),
              CBinaryExpression.BinaryOperator.MINUS);
        case MINUS:
          return new CBinaryExpression(
              binary.getFileLocation(),
              binary.getExpressionType(),
              binary.getCalculationType(),
              binary.getOperand1(),
              binary.getOperand2(),
              CBinaryExpression.BinaryOperator.PLUS);
        default:
          return binary;
      }
    }
    return pRightHandSide;
  }

  private boolean isReplacement(CExpression pRHS, CIdExpression pVar) {
    if (pRHS instanceof CIdExpression && pRHS.equals(pVar)) {
      return true;
    } else if (pRHS instanceof CBinaryExpression) {
      CBinaryExpression expr = (CBinaryExpression) pRHS;
      return isReplacement(expr.getOperand1(), pVar) || isReplacement(expr.getOperand2(), pVar);
    }
    return false;
  }

  private CExpression getCanonicalForm(CExpression pExpr) {
    CExpression returnExpr = pExpr;

    if (pExpr instanceof CAddressOfLabelExpression) {
      returnExpr = visitor.visit((CAddressOfLabelExpression) pExpr);
    } else if (pExpr instanceof CBinaryExpression) {
      returnExpr = visitor.visit((CBinaryExpression) pExpr);
    } else if (pExpr instanceof CCastExpression) {
      returnExpr = visitor.visit((CCastExpression) pExpr);
    } else if (pExpr instanceof CTypeIdExpression) {
      returnExpr = visitor.visit((CTypeIdExpression) pExpr);
    } else if (pExpr instanceof CUnaryExpression) {
      returnExpr = visitor.visit((CUnaryExpression) pExpr);
    }
    if (returnExpr instanceof CIntegerLiteralExpression) {
      CType type = ((CIntegerLiteralExpression) returnExpr).getExpressionType();
      if (type instanceof CSimpleType) {
        CSimpleType simpleType = (CSimpleType) type;
        if (simpleType.getType().equals(CBasicType.INT)) {
          CSimpleType newSimpleType =
              new CSimpleType(
                  false,
                  false,
                  CBasicType.INT,
                  false,
                  false,
                  false,
                  false,
                  false,
                  false,
                  false);
          return CIntegerLiteralExpression
              .createDummyLiteral(((CIntegerLiteralExpression) returnExpr).asLong(), newSimpleType);
        }
      }
    }

    return returnExpr;
  }

}
