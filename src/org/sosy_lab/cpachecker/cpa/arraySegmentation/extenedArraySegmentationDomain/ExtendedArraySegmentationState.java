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
package org.sosy_lab.cpachecker.cpa.arraySegmentation.extenedArraySegmentationDomain;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.simplification.ExpressionSimplificationVisitor;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.ArraySegment;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.ArraySegmentationState;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.ExtendedCompletLatticeAbstractState;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.ExtendedLocationArrayContentCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;
import org.sosy_lab.cpachecker.util.Pair;

public class ExtendedArraySegmentationState<T extends ExtendedCompletLatticeAbstractState<T>>
    implements Serializable, LatticeAbstractState<ExtendedArraySegmentationState<T>>, AbstractState,
    Graphable, AbstractQueryableState {

  private static final long serialVersionUID = -1949092179244419909L;
  private List<ArraySegmentationState<T>> segmentations;
  private LogManager logger;
  private boolean shouldBeHighlighted;

  /**
   *
   * @param pSegmentations list of segmentations present
   *
   * @param pLogger for logging
   */
  public ExtendedArraySegmentationState(
      List<ArraySegmentationState<T>> pSegmentations,
      LogManager pLogger) {
    super();
    checkArgument(!pSegmentations.isEmpty());
    segmentations = pSegmentations;
    logger = pLogger;
  }

  /**
   * Copy constructor
   *
   * @param pState the state to clone
   */
  public ExtendedArraySegmentationState(ExtendedArraySegmentationState<T> pState) {
    super();
    List<ArraySegmentationState<T>> copiedElements =
        new ArrayList<>(pState.getSegmentations().size());
    pState.getSegmentations().forEach(s -> copiedElements.add(s.getDeepCopy()));
    segmentations = copiedElements;
    logger = pState.getLogger();
  }

  /**
   * Returns a copy of the elements given as arguments
   */
  @Override
  public ExtendedArraySegmentationState<T> join(final ExtendedArraySegmentationState<T> pOther)
      throws CPAException, InterruptedException {
    return join(pOther, true);

  }

  /**
   *
   * <b> TO guarantee termination, it must be ensured that the flag isLoopHead is used correctly. If
   * no information present always use true!<\b>
   *
   * @param pOther the other segmentation for joining
   * @param isLoopHead flag to disable the extension of the unification algorithm that retains some
   *        segment bounds present only in one segmentation
   * @return he merged elements
   * @throws CPAException if the computation does not work
   * @throws InterruptedException else
   */
  public ExtendedArraySegmentationState<T>
      join(final ExtendedArraySegmentationState<T> pOther, boolean isLoopHead)
          throws CPAException, InterruptedException {

    ExtendedArraySegmentationState<T> firstSeg = this;
    ExtendedArraySegmentationState<T> secondSeg = pOther;
    if (this.segmentations.size() > pOther.getSegmentations().size()) {
      Pair<ExtendedArraySegmentationState<T>, ExtendedArraySegmentationState<T>> equalLengthSegs =
          matchLength(this, pOther);
      firstSeg = equalLengthSegs.getFirst();
      secondSeg = equalLengthSegs.getSecond();
    } else if (this.segmentations.size() < pOther.getSegmentations().size()) {
      Pair<ExtendedArraySegmentationState<T>, ExtendedArraySegmentationState<T>> equalLengthSegs =
          matchLength(pOther, this);
      secondSeg = equalLengthSegs.getFirst();
      firstSeg = equalLengthSegs.getSecond();
    }

    List<ArraySegmentationState<T>> mergedSegmentations = new ArrayList<>();
    for (int i = 0; i < firstSeg.getSegmentations().size(); i++) {
      mergedSegmentations
          .add(firstSeg.segmentations.get(i).join(secondSeg.getSegmentations().get(i)));
    }

    logger.log(
        ExtendedLocationArrayContentCPA.GENERAL_LOG_LEVEL,
        "Merged the elements " + firstSeg + " and " + secondSeg + "to " + mergedSegmentations);
    if (Objects.deepEquals(pOther.segmentations, mergedSegmentations)) {
      return pOther;
    } else {
      return new ExtendedArraySegmentationState<>(mergedSegmentations, logger);
    }
  }

  private Pair<ExtendedArraySegmentationState<T>, ExtendedArraySegmentationState<T>> matchLength(
      ExtendedArraySegmentationState<T> longer,
      ExtendedArraySegmentationState<T> shorter) {
    if (longer.getSegmentations().get(0).getLanguage().equals(Language.C)) {
      CIntegerLiteralExpression trueExpr = CIntegerLiteralExpression.ONE;
      List<ArraySegmentationState<T>> extendedShorter = new ArrayList<>();
      ArraySegmentationState<T> shorterSeg = shorter.getSegmentations().get(0);
      if (shorter.getSegmentations().size() == 1
          && shorterSeg.getSplitCondition().equals(trueExpr)) {

        for (int i = 0; i < longer.getSegmentations().size(); i++) {
          ArraySegmentationState<T> temp = shorterSeg.getDeepCopy();
          temp.setSplitCondition(longer.getSegmentations().get(i).getSplitCondition());
          extendedShorter.add(temp);
        }

        return Pair.of(longer, new ExtendedArraySegmentationState<>(extendedShorter, logger));
      } else {
        throw new UnsupportedOperationException(
            "Cannot splitt segmentations, if they have been alread splitt");
      }
    } else {
      throw new UnsupportedOperationException();
    }

  }

  @Override
  public boolean isLessOrEqual(ExtendedArraySegmentationState<T> pOther)
      throws CPAException, InterruptedException {

    ExtendedArraySegmentationState<T> firstSeg = this;
    ExtendedArraySegmentationState<T> secondSeg = pOther;
    if (this.segmentations.size() > pOther.getSegmentations().size()) {
      Pair<ExtendedArraySegmentationState<T>, ExtendedArraySegmentationState<T>> equalLengthSegs =
          matchLength(this, pOther);
      firstSeg = equalLengthSegs.getFirst();
      secondSeg = equalLengthSegs.getSecond();
    } else if (this.segmentations.size() < pOther.getSegmentations().size()) {
      Pair<ExtendedArraySegmentationState<T>, ExtendedArraySegmentationState<T>> equalLengthSegs =
          matchLength(pOther, this);
      secondSeg = equalLengthSegs.getFirst();
      firstSeg = equalLengthSegs.getSecond();
    }
    for (int i = 0; i < firstSeg.getSegmentations().size(); i++) {
      if (!firstSeg.segmentations.get(i).isLessOrEqual(secondSeg.getSegmentations().get(i))) {
        return false;
      }
    }
    return true;
  }

  public ExtendedArraySegmentationState<T> strengthn(Collection<AExpression> eColl) {
    for (ArraySegmentationState<T> seg : this.segmentations) {
      seg.strengthn(eColl);
    }
    return this;
  }

  /**
   * Iterate through all segments and check, if a segment has no expressions in its bound. in this
   * case, remove the segment bound and merge the information with the prior segment
   *
   * @throws InterruptedException if the join in the underlying domain fails
   * @throws CPAException if the segmentation is empty
   */
  public void joinSegmentsWithEmptySegmentBounds() throws CPAException, InterruptedException {
    for (ArraySegmentationState<T> seg : this.segmentations) {
      seg.joinSegmentsWithEmptySegmentBounds();
    }

  }

  /**
   * Adds a segment at after the segment {@code after} and set the next parameter correctly.
   *
   * @param toAdd segment to add
   * @param after position to add after
   * @return true if the segment is added, false if after is not present
   */
  public boolean addSegment(ArraySegment<T> toAdd, ArraySegment<T> after) {

    for (ArraySegmentationState<T> seg : this.segmentations) {
      if (!seg.addSegment(toAdd, after)) {
        return false;
      }
    }
    return true;
  }

  /**
   * <b>REPLACES</b> analysis information for a specific index. If information for the array index
   * "3" should be stored, a new segment bound continuing "3+1"=4 is added( if not already present)
   * directly right of the segment bound containing 3 and the information is stored for that segment
   * (hence holds for array element at index 3. If no segment bound should be added, use the method
   * {@link #storeAnalysisInformationAtIndexWithoutAddingBounds}
   *
   * @param index the index of the information to be stored
   * @param analysisInfo to be stored
   * @param newSegmentIsPotentiallyEmpty he information if the new segment is potentially empty
   *        (default is false)
   * @param machineModel of the computation
   * @param pVisitor to create expressions
   * @param pCfaEdge the current edge, needed for logging
   * @return true, if the operation was successful
   */
  public boolean storeAnalysisInformationAtIndex(
      AExpression index,
      T analysisInfo,
      boolean newSegmentIsPotentiallyEmpty,
      MachineModel machineModel,
      ExpressionSimplificationVisitor pVisitor,
      CFAEdge pCfaEdge) {

    for (ArraySegmentationState<T> seg : this.segmentations) {
      if (!seg.storeAnalysisInformationAtIndex(
          index,
          analysisInfo,
          newSegmentIsPotentiallyEmpty,
          machineModel,
          pVisitor,
          pCfaEdge)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Behaves like {@link #storeAnalysisInformationAtIndex}, but does not add new segment bounds. If
   * the segment bound index or index+1 is missing, false is returned and nothing is changed.
   *
   *
   * @param index the index of the information to be stored
   * @param analysisInfo to be stored
   * @param newSegmentIsPotentiallyEmpty he information if the new segment is potentially empty
   *        (default is false)
   * @return true, if the operation was successful
   */
  public boolean storeAnalysisInformationAtIndexWithoutAddingBounds(
      CExpression index,
      T analysisInfo,
      boolean newSegmentIsPotentiallyEmpty,
      MachineModel machineModel,
      ExpressionSimplificationVisitor pVisitor) {
    for (ArraySegmentationState<T> seg : this.segmentations) {
      if (!seg.storeAnalysisInformationAtIndexWithoutAddingBounds(
          index,
          analysisInfo,
          newSegmentIsPotentiallyEmpty,
          machineModel,
          pVisitor)) {
        return false;
      }
    }
    return true;

  }

  /**
   *
   * @return a list containing all AIDExpressions present in the segment bounds
   */
  public List<AIdExpression> getVariablesPresent() {
    Set<AIdExpression> res = new HashSet<>();
    this.segmentations.forEach(s -> res.addAll(s.getVariablesPresent()));
    return new ArrayList<>(res);
  }

  /**
   * Computes the set of all expressions, that are present in the segment bounds, where collecting
   * is started at "startOFCollection". THe function is mainly used during unification
   *
   * @param startOfCollection the element to start collection
   * @return all expressions
   */
  public Set<AExpression> getSegmentBounds(ArraySegment<T> startOfCollection, int position) {
    Set<AExpression> result = new HashSet<>();
    if (segmentations.get(position).getSegments().contains(startOfCollection)) {
      ArraySegment<T> cur = startOfCollection;
      for (int i = segmentations.get(position).getSegments().indexOf(startOfCollection);
          i < segmentations.get(position).getSegments().size();
          i++) {
        result.addAll(cur.getSegmentBound());
        cur = cur.getNextSegment();
      }
    }
    return result;
  }

  @Override
  public boolean shouldBeHighlighted() {
    return shouldBeHighlighted;
  }

  /**
   *
   * @param pOp2 the expression to search for
   * @return the position of the segmentation or -1, if not present
   */
  public int getSegBoundContainingExpr(AExpression pOp2, int position) {
    for (int i = 0; i < segmentations.get(position).getSegments().size(); i++) {
      if (segmentations.get(position).getSegments().get(i).getSegmentBound().contains(pOp2)) {
        return i;
      }
    }
    return -1;
  }

  public boolean isShouldBeHighlighted() {
    return shouldBeHighlighted;
  }

  public void setShouldBeHighlighted(boolean pShouldBeHighlighted) {
    shouldBeHighlighted = pShouldBeHighlighted;
  }

  public LogManager getLogger() {
    return logger;
  }

  public List<ArraySegmentationState<T>> getSegmentations() {
    return segmentations;
  }

  public void setSegmentations(List<ArraySegmentationState<T>> pSegmentations) {
    segmentations = pSegmentations;
  }

  @Override
  public int hashCode() {
    return Objects.hash(segmentations);
  }

  /**
   * This implementation checks syntactical equality. For a formal definition see Analyzing Data
   * Usage in Array Programs, page 30 By Jan Haltermann
   */
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
    ExtendedArraySegmentationState<T> other = (ExtendedArraySegmentationState<T>) obj;
    return Objects.deepEquals(segmentations, other.segmentations);
  }


  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();

    this.segmentations.stream()
        .forEachOrdered(s -> builder.append(s.toString() + System.lineSeparator()));
    return builder.toString();

  }

  @Override
  public String toDOTLabel() {
    return this.toString();
  }

  @Override
  public String getCPAName() {
    return this.segmentations.get(0).getCPAName();
  }

  @Override
  public boolean checkProperty(String pProperty) throws InvalidQueryException {
    for (ArraySegmentationState<T> seg : this.segmentations) {
      if (seg.checkProperty(pProperty)) {
        return true;
      }
    }
    return false;
  }

}
