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
package org.sosy_lab.cpachecker.cpa.arraySegmentation.util;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.simplification.ExpressionSimplificationVisitor;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.ArraySegment;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.ArraySegmentationState;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.ExtendedCompletLatticeAbstractState;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.UnreachableSegmentation;

public class SegmentationReachabilityChecker<T extends ExtendedCompletLatticeAbstractState<T>> {

  LogManager logger;

  public SegmentationReachabilityChecker(LogManager pLogger) {
    super();
    logger = pLogger;
  }

  /**
   * Checks, if the given segmentation is reachable w.r.t. Definition 4.10 given by Jan Haltermann
   * in his Masther thesis. Currently, only the case 1-4 are implement, neither case 5 nor
   * transitive dependecies are checked.
   *
   * @param pSegmentation the segmentation
   * @param pVar the variable on the LHS of the expression
   * @param pOp2 he RHS of the expression
   * @param pOperator the operator of the expression
   * @param pLogger used for loggign
   * @param pVisitor statement simplifaction visitor to compute cannonical form
   * @return true, if the segmentation is reachable, false otherwise
   */
  public @Nullable ArraySegmentationState<T> checkReachability(
      ArraySegmentationState<T> pSegmentation,
      AIdExpression pVar,
      AExpression pOp2,
      BinaryOperator pOperator,
      LogManager pLogger,
      ExpressionSimplificationVisitor pVisitor) {

    int segOfVar = pSegmentation.getSegBoundContainingExpr(pVar);
    int segOfExpr = pSegmentation.getSegBoundContainingExpr(pOp2);
    List<ArraySegment<T>> segments = new ArrayList<>(pSegmentation.getSegments());
    // Case 1: If e = (i = c), i and c are present in different segment bounds and there is a
    // segment {e j }d j {e k } between the segment bounds containing i and c that is not marked
    // with ’?’,
    if (segOfExpr != -1
        && segOfVar != -1
        && segOfExpr != segOfVar
        && pOperator.equals(BinaryOperator.EQUALS)) {
      int min = Integer.min(segOfExpr, segOfVar);
      int max = Integer.max(segOfExpr, segOfVar);
      for (int i = min; i < max; i++) {
        if (!segments.get(i).isPotentiallyEmpty()) {
          return new UnreachableSegmentation<>(pSegmentation);
        }
      }
    }

    // Case 2: if e = (i = c) and there is a second expression e 0 , such that i and e 0 are present
    // in one segment bound, but c != e 0 holds
    // Check if the RHS evaluates to a integer value
    AExpression valueOfpOp2 = getValueOrNull(pOp2, pVisitor);
    if (pOperator.equals(BinaryOperator.EQUALS)
        && valueOfpOp2 != null
        && valueOfpOp2 instanceof CIntegerLiteralExpression) {
      BigInteger v = ((CIntegerLiteralExpression) valueOfpOp2).getValue();
      if (segOfVar != -1) {
        ArraySegment<T> segment = segments.get(segOfVar);
        if (segment.getSegmentBound()
            .parallelStream()
            .anyMatch(
                s -> s instanceof CIntegerLiteralExpression
                    && !((CIntegerLiteralExpression) s).getValue().equals(v))) {
          return new UnreachableSegmentation<>(pSegmentation);
        }
      }
    }

    // Case 3: e = (i != c) and there is a segment bound containing the expressions c and i.
    if (segOfExpr == segOfVar && pOperator.equals(BinaryOperator.NOT_EQUALS)) {
      return new UnreachableSegmentation<>(pSegmentation);
    }

    // Case 4: The ordering of segment bounds implies that i ≤ c, but e = (i > c) or vice versa
    if ((pOperator.equals(BinaryOperator.GREATER_THAN) && segOfVar < segOfExpr)
        || (pOperator.equals(BinaryOperator.LESS_THAN) && segOfVar > segOfExpr)) {
      return new UnreachableSegmentation<>(pSegmentation);
    }

    // TODO: implement Case 5: Between two segment bounds e 1 ,e 2 with | e 1 −e 2 |= n are more
    // than n-1 segment bounds not marked with ’?’ and thus more than n segments present.
    // if (segOfExpr != -1
    // && segOfVar != -1
    // && segOfExpr != segOfVar) {
    // int min = Integer.min(segOfExpr, segOfVar);
    // int max = Integer.max(segOfExpr, segOfVar);
    // //Check if there are more elements marked as "not potentially empty = defentlyEmpty"
    // if (segments.subList(min, max+1).parallelStream().filter(s ->
    // !s.isPotentiallyEmpty()).count() > ) {
    //
    // }
    // }

    // // Case 6: The variable is present in a segmentation with a constant, but the expression
    // implies
    // // that it is greater than the constant
    // TODO: Currently, this is not working, comparing equals is false
    // AExpression simplifiedRHS = getValueOrNull(pOp2, pVisitor);
    // if (segOfVar >= 0 && simplifiedRHS instanceof AIntegerLiteralExpression) {
    // ArraySegment<T> segmentOfVar = pSegmentation.getSegments().get(segOfVar);
    // if (segmentOfVar.getSegmentBound()
    // .stream()
    // .anyMatch(s -> s instanceof AIntegerLiteralExpression)) {
    // AIntegerLiteralExpression constOfSegOfVar =
    // (AIntegerLiteralExpression) segmentOfVar.getSegmentBound()
    // .stream()
    // .filter(s -> s instanceof AIntegerLiteralExpression)
    // .collect(Collectors.toList())
    // .get(0);
    // if (!constOfSegOfVar.getValue()
    // .equals(((AIntegerLiteralExpression) simplifiedRHS).getValue())) {
    // return new UnreachableSegmentation<>(pSegmentation);
    // }
    // }
    // }
    return pSegmentation;

  }

  private static AExpression
      getValueOrNull(AExpression pOp2, ExpressionSimplificationVisitor visitor) {
    if (pOp2 instanceof CExpression) {
      CExpression returnExpr = null;
      if (pOp2 instanceof CAddressOfLabelExpression) {
        returnExpr = visitor.visit((CAddressOfLabelExpression) pOp2);
      } else if (pOp2 instanceof CBinaryExpression) {
        returnExpr = visitor.visit((CBinaryExpression) pOp2);
      } else if (pOp2 instanceof CCastExpression) {
        returnExpr = visitor.visit((CCastExpression) pOp2);
      } else if (pOp2 instanceof CTypeIdExpression) {
        returnExpr = visitor.visit((CTypeIdExpression) pOp2);
      } else if (pOp2 instanceof CUnaryExpression) {
        returnExpr = visitor.visit((CUnaryExpression) pOp2);
      } else if (pOp2 instanceof CIntegerLiteralExpression) {
        returnExpr = (CIntegerLiteralExpression) pOp2;
      }
      return returnExpr;
    } else {
      throw new UnsupportedOperationException();
    }
  }

}
