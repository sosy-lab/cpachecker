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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.util.EnhancedCExpressionSimplificationVisitor;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * A way to represent the segments of an array, where a specific property holds
 */
public class CPropertySpec<T extends ExtendedCompletLatticeAbstractState<T>> {

  private List<CGenericInterval> segmentintervals;
  private Map<AExpression, AExpression> lowerMappingOfExpr;
  private Map<AExpression, AExpression> upperMappingOfExpr;
  private T property;
  private Language language;
  private AExpression sizeVar;

  public CPropertySpec(
      ArraySegmentationState<T> state,
      T pProperty,
      EnhancedCExpressionSimplificationVisitor pVisitor,
      CBinaryExpressionBuilder pBuilder)
      throws CPAException {
    property = pProperty;
    this.language = state.getLanguage();
    sizeVar = state.getSizeVar();
    if (state instanceof UnreachableSegmentation) {
      if (property.equals(state.gettEmptyElement().getBottomElement())) {
        // Return a single interval with all array indices fulfilling the property
        segmentintervals = getFullInterval(state);
      } else {
        segmentintervals = new ArrayList<>();
      }
      lowerMappingOfExpr = new HashMap<>();
      upperMappingOfExpr = new HashMap<>();
      upperMappingOfExpr.put(state.getSizeVar(), state.getSizeVar());

    } else if (state instanceof ErrorSegmentation) {
      if (property.equals(state.gettEmptyElement().getTopElement())) {
        // Return a single interval with all array indices fulfilling the property
        segmentintervals = getFullInterval(state);
      } else {
        segmentintervals = new ArrayList<>();
      }
      lowerMappingOfExpr = new HashMap<>();
      upperMappingOfExpr = new HashMap<>();
      upperMappingOfExpr.put(state.getSizeVar(), state.getSizeVar());

    } else {
    segmentintervals = computeIntervals(state, property);
      computeMapping(state, pVisitor, pBuilder);
    }

  }

  private List<CGenericInterval> getFullInterval(ArraySegmentationState<T> pState) {
    return Collections.singletonList(
        new CGenericInterval(CIntegerLiteralExpression.ZERO, (CExpression) pState.getSizeVar()));
  }

  public CPropertySpec(
      List<CGenericInterval> pSegmentintervals,
      Map<AExpression, AExpression> pLowerMappingOfExpr,
      Map<AExpression, AExpression> pUpperMappingOfExpr,
      T pProperty,
      Language pLanguage,
      AIdExpression pSizeVar) {
    super();
    segmentintervals = pSegmentintervals;
    lowerMappingOfExpr = pLowerMappingOfExpr;
    upperMappingOfExpr = pUpperMappingOfExpr;
    property = pProperty;
    language = pLanguage;
    sizeVar = pSizeVar;
  }

  @SuppressWarnings("unchecked")
  private void computeMapping(
      ArraySegmentationState<T> pState,
      EnhancedCExpressionSimplificationVisitor pVisitor,
      CBinaryExpressionBuilder pBuilder)
      throws CPAException {
    Map<ArraySegment<T>, CGenericInterval> concreteValues = new HashMap<>();

    lowerMappingOfExpr = new HashMap<>();
    upperMappingOfExpr = new HashMap<>();

    List<ArraySegment<T>> segments = new ArrayList<>(pState.getSegments());

    // Firstly, store all known concrete values
    for (int i = 0; i < segments.size(); i++) {

      List<CExpression> simpleExpr =
          segments.get(i)
              .getSegmentBound()
              .parallelStream()
              .map(e -> pVisitor.visit((CExpression) e))
              .collect(Collectors.toList());

      Optional<CExpression> valueOpt =
          simpleExpr.parallelStream()
              .filter(e -> e instanceof CIntegerLiteralExpression)
              .findFirst();

      if (valueOpt.isPresent()) {
        CExpression value = valueOpt.get();
        concreteValues.put(segments.get(i), new CGenericInterval(value));
      }
    }
    concreteValues.put(
        segments.get(segments.size() - 1),
        new CGenericInterval((CExpression) pState.getSizeVar()));

    // Iterate through the full segmentations and search for the first constant value appearing
    for (int i = 0; i < segments.size(); i++) {

      List<CExpression> simpleExpr =
          segments.get(i)
              .getSegmentBound()
              .parallelStream()
              .map(e -> pVisitor.visit((CExpression) e))
              .collect(Collectors.toList());

      CExpression valueLow;
      CExpression valueHigh = null;
      // Using the assumption, that the first segment contains '0' as constant, we can add all
      // values variables and expressions from the first segment

      if (concreteValues.containsKey(segments.get(i))) {
        valueHigh = concreteValues.get(segments.get(i)).getHigh();
        valueLow = concreteValues.get(segments.get(i)).getLow();
      } else {
        // Determine the lower bound (the last segment has stored its value, that can be reused
        if (i == 0) {
          // Using the assumption, that the first segment contains '0' as constant, we have an
          // incorrect segmentation
          throw new CPAException("Te first segment needs to contian a constant");
        } else {
          if (segments.get(i - 1).isPotentiallyEmpty()) {
            valueLow = concreteValues.get(segments.get(i - 1)).getLow();
          } else {
            pVisitor.visit(
                valueLow =
                    pBuilder.buildBinaryExpression(
                        concreteValues.get(segments.get(i - 1)).getLow(),
                        CIntegerLiteralExpression.ONE,
                        CBinaryExpression.BinaryOperator.PLUS));
          }
        }
        // Determine the upper bound, therefore iterate through all following elements until
        // reached one with constant value or the last element. The counter counter determines, how
        // many "definitely not empty" segments are in between
        int counter = segments.get(i).isPotentiallyEmpty() ? 0 : 1;
        for (int j = i + 1; j < segments.size(); j++) {
          if (concreteValues.containsKey(segments.get(j))) {
            valueHigh =
                pVisitor.visit(
                    pBuilder.buildBinaryExpression(
                        concreteValues.get(segments.get(j)).getLow(),
                        CIntegerLiteralExpression.createDummyLiteral(counter, CNumericTypes.INT),
                        CBinaryExpression.BinaryOperator.MINUS));
            break;
          } else if (!segments.get(j).isPotentiallyEmpty()) {
            // IF there is no ?, the segment is bounded by the next concrete value minus "counter"
            counter++;
          }
        }
        // This case cannot occure, since size is added to concreteValues
        // if (valueHigh == null) {
        // // there are no concrete values except the
        // valueHigh =
        // pBuilder.buildBinaryExpression(
        // (CExpression) pState.getSizeVar(),
        // CIntegerLiteralExpression.createDummyLiteral(counter, CNumericTypes.INT),
        // CBinaryExpression.BinaryOperator.MINUS);
        // }

        // Add interval to the concreteValues
        concreteValues.putIfAbsent(segments.get(i), new CGenericInterval(valueLow, valueHigh));
      }
      AExpression valueHighFinal = valueHigh;
      simpleExpr.parallelStream()
          .filter(e -> !(e instanceof CIntegerLiteralExpression))
          .forEach(e -> {
            lowerMappingOfExpr.put(e, valueLow);
            upperMappingOfExpr.put(e, valueHighFinal);
          });

    }
  }

  /**
   * Compute all segment bounds having the desired element stored, in an ordered way
   *
   * @return list of intervals, for that the desired property holds
   */
  private List<CGenericInterval> computeIntervals(ArraySegmentationState<T> pState, T pProperty) {
    List<CGenericInterval> res = new ArrayList<>();

    for (ArraySegment<T> s : pState.getSegments()) {
      if (s.getAnalysisInformation().equals(pProperty)) {

        CExpression low = getMostConcreteValue(s.getSegmentBound(), pState.getSizeVar());
        CExpression high =
            getMostConcreteValue(s.getNextSegment().getSegmentBound(), pState.getSizeVar());
        CGenericInterval interval = new CGenericInterval(low, high);
        res.add(interval);
      }
    }
    // Now, unify intervals if possible (lower bound of one is upper bond of other)
    // [0,4), [4,8) --> [0,8)
    int bound = res.size() - 1;
    for (int i = 0; i < bound; i++) {
      CGenericInterval interval = res.get(i);
      if (interval.getHigh().equals(res.get(i + 1).getLow())) {
        interval.setHigh(res.get(i + 1).getHigh());
        res.remove(i + 1);
        bound--;
      }
    }
    return res;

  }

  private CExpression getMostConcreteValue(List<AExpression> pSegmentBound, AExpression pSizeVar) {
    Optional<AExpression> literal =
        pSegmentBound.parallelStream()
            .filter(p -> p instanceof CIntegerLiteralExpression)
            .findAny();
    if (literal.isPresent()) {
      return (CExpression) literal.get();
    } else {
      if (pSegmentBound.parallelStream().anyMatch(p -> p.equals(pSizeVar))) {
        return (CExpression) pSizeVar;
      }
      return (CExpression) pSegmentBound.get(0);
    }
  }

  public List<CGenericInterval> getOverApproxIntervals() {
    List<CGenericInterval> res = new ArrayList<>();
    for (CGenericInterval i : segmentintervals) {

      CExpression lower;
      CExpression upper;

      if (i.getLow() instanceof CIntegerLiteralExpression) {
        lower = i.getLow();
      } else {
        lower = (CExpression) this.lowerMappingOfExpr.get(i.getLow());
      }
      if (i.getHigh() instanceof CIntegerLiteralExpression) {
        upper = i.getHigh();
      } else {
        upper = (CExpression) this.upperMappingOfExpr.get(i.getHigh());
      }
      CGenericInterval maxInterval = new CGenericInterval(lower, upper);
      res.add(maxInterval);
    }
    return res;

  }

  @Override
  public int hashCode() {
    return Objects.hash(lowerMappingOfExpr, property, segmentintervals, sizeVar);
  }

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
    @SuppressWarnings("unchecked")
    CPropertySpec<T> other = (CPropertySpec<T>) obj;
    return Objects.equals(lowerMappingOfExpr, other.lowerMappingOfExpr)
        && Objects.equals(property, other.property)
        && Objects.equals(segmentintervals, other.segmentintervals)
        && Objects.equals(sizeVar, other.sizeVar);
  }

  public List<CGenericInterval> getSegmentintervals() {
    return segmentintervals;
  }

  public Map<AExpression, AExpression> getLowerMappingOfExpr() {
    return lowerMappingOfExpr;
  }

  public Map<AExpression, AExpression> getUpperMappingOfExpr() {
    return upperMappingOfExpr;
  }

  public T getProperty() {
    return property;
  }

  public Language getLanguage() {
    return language;
  }

  public AExpression getSizeVar() {
    return sizeVar;
  }

  @Override
  public String toString() {
    return "PropertySpec [segmentintervals="
        + segmentintervals
        + ", lowerMappingOfExpr="
        + lowerMappingOfExpr
        + ", upperMappingOfExpr="
        + upperMappingOfExpr
        + ", property="
        + property
        + "]";
  }

}
