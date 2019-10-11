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
package org.sosy_lab.cpachecker.cpa.arraySegmentation;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.AUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpression;
import org.sosy_lab.cpachecker.cfa.simplification.ExpressionSimplificationVisitor;

public class ArraySegment<T extends ClonableLatticeAbstractState<T>> implements Serializable {

  private static final long serialVersionUID = -2967663836098685037L;
  private List<AExpression> segmentBound;
  private T analysisInformation;
  private boolean isPotentiallyEmpty;
  private ArraySegment<T> nextSegment;
  private final Language language;

  public ArraySegment(
      List<AExpression> pSegmentBound,
      T pAnalysisInformation,
      boolean pIsPotentiallyEmpty,
      ArraySegment<T> pNextSegment,
      Language pLanguage) {
    super();

    if (pAnalysisInformation == null) {
      throw new IllegalArgumentException("The analysis information cannot be null");
    }

    segmentBound = pSegmentBound;
    analysisInformation = pAnalysisInformation;
    isPotentiallyEmpty = pIsPotentiallyEmpty;
    nextSegment = pNextSegment;
    language = pLanguage;
  }

  public ArraySegment(ArraySegment<T> pS) {
    segmentBound = new ArrayList<>(pS.getSegmentBound());
    analysisInformation = pS.getAnalysisInformation().getDeepCopy();
    isPotentiallyEmpty = pS.isPotentiallyEmpty;
    nextSegment = pS.getNextSegment();
    language = pS.getLanguage();
  }

  public List<AExpression> getSegmentBound() {
    return Collections.unmodifiableList(segmentBound);
  }

  public void setSegmentBound(List<AExpression> pSegmentBound) {
    segmentBound = pSegmentBound;
  }

  public T getAnalysisInformation() {
    return analysisInformation;
  }

  public void setAnalysisInformation(T pAnalysisInformation) {
    analysisInformation = pAnalysisInformation;
  }

  public boolean isPotentiallyEmpty() {
    return isPotentiallyEmpty;
  }

  public void setPotentiallyEmpty(boolean pIsPotentiallyEmpty) {
    isPotentiallyEmpty = pIsPotentiallyEmpty;
  }

  public ArraySegment<T> getNextSegment() {
    return nextSegment;
  }

  public void setNextSegment(ArraySegment<T> pNextSegment) {
    nextSegment = pNextSegment;
  }

  public Language getLanguage() {
    return language;
  }

  @SuppressWarnings("unchecked")
  @Override
  public ArraySegment<T> clone() {
    List<AExpression> boundsCopy = new ArrayList<>(segmentBound.size());
    this.segmentBound.parallelStream().forEach(e -> boundsCopy.add(e));
    // TODO: Add deep copying!
    return new ArraySegment<>(
        boundsCopy,
        this.analysisInformation,
        this.isPotentiallyEmpty,
        null,
        language);
  }

  /**
   * Remove the elements @param toRemove from the list of segment bounds
   *
   * @param toRemove to be removed
   * @return the reduced segment
   */
  public ArraySegment<T> removeExprFromBound(List<AExpression> toRemove) {
    this.segmentBound.removeAll(toRemove);
    return this;

  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(" {");
    for (int i = 0; i < this.segmentBound.size() - 1; i++) {
      builder.append(this.segmentBound.get(i).toString());
      builder.append(" , ");
    }
    if (this.segmentBound.size() > 0) {
      builder.append(this.segmentBound.get(segmentBound.size() - 1).toString());
    }
    builder.append("} ");
    if (this.analysisInformation != null) {
      builder.append(this.analysisInformation.toString());
    }
    if (isPotentiallyEmpty) {
      builder.append(" ?");
    }

    return builder.toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((analysisInformation == null) ? 0 : analysisInformation.hashCode());
    result = prime * result + (isPotentiallyEmpty ? 1231 : 1237);
    result = prime * result + ((segmentBound == null) ? 0 : segmentBound.hashCode());
    return result;
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    ArraySegment<T> other = (ArraySegment<T>) obj;
    if (analysisInformation == null) {
      if (other.analysisInformation != null) {
        return false;
      }
    } else if (!analysisInformation.equals(other.analysisInformation)) {
      return false;
    }
    if (isPotentiallyEmpty != other.isPotentiallyEmpty) {
      return false;
    }
    if (segmentBound == null) {
      if (other.segmentBound != null) {
        return false;
      }
    }
    // Since the ordering is irrelevant, use a Hashset
    else if (!Objects
        .deepEquals(new HashSet<>(segmentBound), new HashSet<>(other.getSegmentBound()))) {
      return false;
    }
    return true;
  }

  /**
   * In this first version, the array segment is strengthen (meaning made more precise), iff the
   * expression is of the form i < SIZE or i<= SIZE, where i the the dedicated variable for
   * accessing array elements and size is the length of the array In that case and if the array
   * segment is of the form {i, ...} p_i ? {SIZE}, the question-mark can be removed!
   *
   * @param pE the expression used to strengthen the segment
   * @return the (Strengthened) segment
   */
  public ArraySegment<T> strengthn(AExpression pE) {
    if (language.equals(Language.C)) {
      if (pE instanceof CBinaryExpression) {
        CBinaryExpression expr = (CBinaryExpression) pE;
        // If the expression is a variable
        if (expr.getOperand1() instanceof CIdExpression &&
        // it is present in the segment bounds
            this.segmentBound.contains(expr.getOperand1()) &&
            // The segment has a next segment bound that is not the final one
            !(this.nextSegment instanceof FinalSegment) &&
            // and the other operator is present in the segment bounds of the following segment
            this.nextSegment.getSegmentBound().contains(expr.getOperand2()) &&
            // and the operator is <
            expr.getOperator().equals(CBinaryExpression.BinaryOperator.LESS_THAN) &&
            // and the segment is potentially empty
            this.isPotentiallyEmpty) {
          // then, we can remove the ?
          this.isPotentiallyEmpty = false;
        }
        // Else If the expression is a variable
        else if (expr.getOperand2() instanceof CIdExpression &&
        // it is present in the following segment bounds
            this.segmentBound.contains(expr.getOperand2()) &&
            // The segment has a next segment bound that is not the final one
            !(this.nextSegment instanceof FinalSegment) &&
            // and the other operand is present in this segment bound
            this.nextSegment.getSegmentBound().contains(expr.getOperand1()) &&
            // and the operator is >
            expr.getOperator().equals(CBinaryExpression.BinaryOperator.GREATER_THAN) &&
            // and the segment is potentially empty
            this.isPotentiallyEmpty) {
          // then, we can remove the ?
          this.isPotentiallyEmpty = false;
        }
      }
      // TODO: Add more complex strengthening functions
      // TODO: Add a extension for java programs!
      return this;
    } else {
      throw new UnsupportedOperationException();
    }
  }

  public void addSegmentBound(AExpression pExpr) {
    this.segmentBound.add(pExpr);
  }

  public void addSegmentBounds(Collection<AExpression> pArraySegment) {
    this.segmentBound.addAll(pArraySegment);
  }

  public ArraySegment<T> replaceVar(
      AExpression pVar,
      AExpression pReplacement,
      ExpressionSimplificationVisitor visitor) {

    List<AExpression> modifiedBound = new ArrayList<>();

    for (int i = 0; i < this.segmentBound.size(); i++) {
      AExpression e = this.segmentBound.get(i);
      if (contains(e, pVar)) {
        if (((e instanceof CExpression) && !(pReplacement instanceof CExpression))
            || ((e instanceof JExpression) && !(pReplacement instanceof JExpression))) {
          throw new IllegalArgumentException(
              "Cannot replace a java exprssion by a C expression or vice versa");
        }
        modifiedBound.add(update(e, pVar, pReplacement));
      } else {
        modifiedBound.add(e);
      }
    }

    // Simplify the expression in the segment bound
    if (language.equals(Language.C)) {
      ArrayList<AExpression> simplifiedList = new ArrayList<>();
      modifiedBound.forEach(e -> simplifiedList.add(getSimplified(e, visitor)));
      this.segmentBound = simplifiedList;
      return this;
    } else {
      throw new UnsupportedOperationException();
    }
  }

  private AExpression getSimplified(AExpression pExpr, ExpressionSimplificationVisitor pVisitor) {
    AExpression returnExpr = pExpr;
    if (pExpr instanceof CAddressOfLabelExpression) {
      returnExpr = pVisitor.visit((CAddressOfLabelExpression) pExpr);
    } else if (pExpr instanceof CBinaryExpression) {
      returnExpr = pVisitor.visit((CBinaryExpression) pExpr);
    } else if (pExpr instanceof CCastExpression) {
      returnExpr = pVisitor.visit((CCastExpression) pExpr);
    } else if (pExpr instanceof CTypeIdExpression) {
      returnExpr = pVisitor.visit((CTypeIdExpression) pExpr);
    } else if (pExpr instanceof CUnaryExpression) {
      returnExpr = pVisitor.visit((CUnaryExpression) pExpr);
    }
    return returnExpr;
  }

  private AExpression update(AExpression toReplace, AExpression var, AExpression replacement) {

    if (toReplace.equals(var)) {
      return replacement;
    } else if (toReplace instanceof CBinaryExpression) {
      // TODO: Extend to unary and other expressions
      CBinaryExpression expr = (CBinaryExpression) toReplace;
      return new CBinaryExpression(
          expr.getFileLocation(),
          expr.getExpressionType(),
          expr.getCalculationType(),
          (CExpression) update(expr.getOperand1(), var, replacement),
          (CExpression) update(expr.getOperand2(), var, replacement),
          expr.getOperator());
    } else if (toReplace instanceof JBinaryExpression) {
      JBinaryExpression expr = (JBinaryExpression) toReplace;
      return new JBinaryExpression(
          expr.getFileLocation(),
          expr.getExpressionType(),
          (JExpression) update(expr.getOperand1(), var, replacement),
          (JExpression) update(expr.getOperand2(), var, replacement),
          expr.getOperator());
    } else {
      return toReplace;
    }
  }

  private boolean contains(AExpression pE, AExpression pVar) {
    if (pVar.equals(pE)) {
      return true;
    } else if (pE instanceof ABinaryExpression) {
      return contains(((ABinaryExpression) pE).getOperand1(), pVar)
          || contains(((ABinaryExpression) pE).getOperand2(), pVar);
    }
    return false;
  }

  /**
   * Removes the all expressions from the segment bound, containing subExpr
   *
   * @param subExpr to check for
   * @return the modified ArraySegment
   */
  public ArraySegment<T> removeExprContainingSubExpr(AIdExpression subExpr) {
    List<AExpression> toRemove = new ArrayList<>();
    for (AExpression e : this.segmentBound) {
      if (contains(e, subExpr)) {
        toRemove.add(e);
      }
    }
    this.segmentBound.removeAll(toRemove);
    return this;
  }

  /**
   * Computes, if the segment bound contains any constant values
   *
   * @param pVisitor used to simplify the expressions
   * @return either -1, if no constant value is found or the constant value
   */
  public BigInteger evaluateToInteger(ExpressionSimplificationVisitor pVisitor) {
    if (language.equals(Language.C)) {
      for (AExpression e : this.segmentBound) {

        if (e instanceof CBinaryExpression) {
          CExpression simplified = pVisitor.visit((CBinaryExpression) e);
          if (simplified instanceof CIntegerLiteralExpression) {
            return ((CIntegerLiteralExpression) simplified).getValue();
          }
        } else if (e instanceof CIntegerLiteralExpression) {
          return ((CIntegerLiteralExpression) e).getValue();

        }
      }
      return BigInteger.valueOf(-1);
    } else {
      throw new UnsupportedOperationException();
    }
  }

  public Collection<? extends AIdExpression> getVariablesPresent() {
    Set<AIdExpression> res = new HashSet<>();
    this.segmentBound.parallelStream().forEach(b -> res.addAll(getVarsFromExpr(b)));
    return res;
  }

  private Collection<? extends AIdExpression> getVarsFromExpr(AExpression pExpr) {
    Set<AIdExpression> res = new HashSet<>();
    if (pExpr instanceof ABinaryExpression) {
      res.addAll(getVarsFromExpr(((ABinaryExpression) pExpr).getOperand1()));
      res.addAll(getVarsFromExpr(((ABinaryExpression) pExpr).getOperand2()));
    }
    if (pExpr instanceof AUnaryExpression) {
      res.addAll(getVarsFromExpr(((AUnaryExpression) pExpr).getOperand()));
    }
    if (pExpr instanceof AIdExpression) {
      res.add((AIdExpression) pExpr);
    }
    return res;
  }

}
