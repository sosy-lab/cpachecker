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

import com.google.common.collect.ImmutableList;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.simplification.ExpressionSimplificationVisitor;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.formula.FormulaState;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.util.ArrayModificationException;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.util.EnhancedCExpressionSimplificationVisitor;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.util.SegmentationUnifier;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.util.transfer.CSegmentationModifier;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;
import org.sosy_lab.cpachecker.util.Pair;

public class ArraySegmentationState<T extends ExtendedCompletLatticeAbstractState<T>>
    implements Serializable, LatticeAbstractState<ArraySegmentationState<T>>, AbstractState,
    Graphable, AbstractQueryableState {

  protected static final long serialVersionUID = 85908607562101422L;

  protected List<ArraySegment<T>> segments;
  protected final SegmentationUnifier<T> unifier;

  protected List<AExpression> tLisOfArrayVariables;
  protected final AIdExpression tArray;
  protected final AExpression sizeVar;
  protected final T tEmptyElement;
  protected final Language language;

  protected final LogManager logger;
  protected boolean shouldBeHighlighted;
  protected boolean canBeEmpty;
  protected final String cpaName;
  protected final Predicate<ArraySegmentationState<T>> propertyPredicate;
  protected CallstackState callStack;
  protected FormulaState pathFormula;
  protected AExpression splitCondition;

  /**
   *
   * @param pSegments list of segments present
   * @param pEmptyElement an element that is not part of the lattice. It is used to avoid uses of
   *        null!
   * @param pLisOfArrayVariables containing all variables used to access array elements
   * @param pArray the array variable to be analyzed
   * @param pSizeVar the variable that denotes the length of the array
   * @param pLogger for logging
   */
  public ArraySegmentationState(
      List<ArraySegment<T>> pSegments,
      T pEmptyElement,
      List<AExpression> pLisOfArrayVariables,
      AIdExpression pArray,
      AExpression pSizeVar,
      Language pLanguage,
      boolean pCanBeEmpty,
      String pCpaName,
      Predicate<ArraySegmentationState<T>> pPropertyPredicate,
      LogManager pLogger,
      CallstackState pCallState,
      FormulaState pPathFormula,
      AExpression pSplitCondition) {
    super();
    segments = pSegments;
    // Check if the segment chain is correctly ordered
    for (int i = 0; i < segments.size() - 1; i++) {
      if (!segments.get(i).getNextSegment().equals(segments.get(i + 1))) {
        throw new IllegalArgumentException(
            "The succesor of the " + i + "th element is not correctly set!");
      }
      if (segments.size() > 0
          && !(segments.get(segments.size() - 1).getNextSegment() instanceof FinalSegment)) {
        throw new IllegalArgumentException(
            "The nextElement of the last element does not contain the specific FINAL_SEG_SYMBOL");
      }
    }
    unifier = new SegmentationUnifier<>();
    tEmptyElement = pEmptyElement;
    tLisOfArrayVariables = pLisOfArrayVariables;
    tArray = pArray;
    sizeVar = pSizeVar;
    language = pLanguage;
    this.canBeEmpty = pCanBeEmpty;
    cpaName = pCpaName;
    propertyPredicate = pPropertyPredicate;
    logger = pLogger;
    callStack = pCallState;
    pathFormula = pPathFormula;
    splitCondition = pSplitCondition;
  }

  /**
   * <b> IMPORTANT: Don't use this constructor as a copy constructor! Instead, use
   * {@link ArraySegmentationState#getDeepCopy()} </b> (Explanation: We cannot use a single copy
   * constructor for the ArraySegmentationState and the sub-types Error and UnreachableSegmentaiton.
   * Hence, to simplify the code in general, we decided to use a geetDeepCopy method instead of
   * checking at every position where the element needs to be cloned, which copy constructor needs
   * to be called
   *
   * Minimal constructor, only usable for error segmentation or unreachable segmentation, because
   * the attributes set to null are never requested
   *
   *
   *
   * @param pPreviousState the previous state, used to get all information needed
   */
  protected ArraySegmentationState(ArraySegmentationState<T> pPreviousState) {
    super();
    List<ArraySegment<T>> copiedElements = new ArrayList<>();
    pPreviousState.getSegments()
        .stream()
        .forEachOrdered(s -> copiedElements.add(new ArraySegment<>(s)));
    // Set the references to the next element for each segment:
    for (int i = 0; i < copiedElements.size() - 1; i++) {
      copiedElements.get(i).setNextSegment(copiedElements.get(i + 1));
    }
    segments = copiedElements;
    unifier = new SegmentationUnifier<>();
    tEmptyElement = pPreviousState.gettEmptyElement();
    tLisOfArrayVariables = new ArrayList<>(pPreviousState.gettListOfArrayVariables());
    tArray = pPreviousState.gettArray();
    sizeVar = pPreviousState.getSizeVar();
    language = pPreviousState.getLanguage();
    this.canBeEmpty = pPreviousState.canBeEmpty;
    cpaName = pPreviousState.getCPAName();
    propertyPredicate = pPreviousState.getPropertyPredicate();
    logger = pPreviousState.getLogger();
    callStack = pPreviousState.getCallStack();
    pathFormula = pPreviousState.getPathFormula();
    splitCondition = pPreviousState.getSplitCondition();
  }

  public ArraySegmentationState<T> getDeepCopy() {
    if (this instanceof ErrorSegmentation) {
      return new ErrorSegmentation<>(this);
    } else if (this instanceof UnreachableSegmentation) {
      return new UnreachableSegmentation<>(this);
    } else {
      return new ArraySegmentationState<>(this);
    }
  }

  /**
   * Returns a copy of the elements given as arguments
   */
  @Override
  public ArraySegmentationState<T> join(final ArraySegmentationState<T> pOther)
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
  public ArraySegmentationState<T> join(final ArraySegmentationState<T> pOther, boolean isLoopHead)
      throws CPAException, InterruptedException {

    // Some corner cases for error and unreachable segmentations
    if (this instanceof UnreachableSegmentation) {
      return pOther;
    } else if (pOther instanceof UnreachableSegmentation) {
      return this;
    } else if (pOther instanceof ErrorSegmentation) {
      return pOther;
    } else if (this instanceof ErrorSegmentation) {
      return this;
    }

    // Don't need to create a copy the elements to avoid side effects, since this is done during
    // unify
    ArraySegmentationState<T> first = this.getDeepCopy();
    ArraySegmentationState<T> second = pOther.getDeepCopy();

    if (this.splitCondition.equals(pOther.splitCondition)) {

      Pair<ArraySegmentationState<T>, ArraySegmentationState<T>> unifiedSegs =
          unifier.unifyMerge(
              first,
              second,
              tEmptyElement.getBottomElement(),
              tEmptyElement.getBottomElement(),
              isLoopHead);

      // TO get a more precise result, we check, if exectly one segment is empty. Then, returning
      // the
      // unified version of the other leads to a more precise result. To denote that, the flag
      // "canBeEmpty" is set for the non-empty segment
      if (first.isEmptyArray() && !second.isEmptyArray()) {
        @Nullable
        ArraySegmentationState<T> resSeg = unifiedSegs.getSecond();
        resSeg.setCanBeEmpty(true);
        return resSeg;
      } else if (!first.isEmptyArray() && second.isEmptyArray()) {
        @Nullable
        ArraySegmentationState<T> resSeg = unifiedSegs.getFirst();
        resSeg.setCanBeEmpty(true);
        return resSeg;
      }

      List<ArraySegment<T>> res = new ArrayList<>();
      List<ArraySegment<T>> firstSegs = new ArrayList<>(unifiedSegs.getFirst().getSegments());
      List<ArraySegment<T>> secondSegs = new ArrayList<>(unifiedSegs.getSecond().getSegments());

      if (firstSegs.isEmpty()) {
        throw new CPAException("The unification has fail!");
      }

      ArraySegment<T> firstSeg = firstSegs.get(firstSegs.size() - 1);
      ArraySegment<T> secondSeg = secondSegs.get(secondSegs.size() - 1);

      // Since the list needs to be concatenated, we will start backwards (last element has a
      // specific
      // next element). The last array segment only contains a segment bound, hence the analysis
      // information are set to null (they are null anyway), same for isPotentiallyEmpty (which is
      // false)

      ArraySegment<T> current = secondSeg;
      current.setAnalysisInformation(
          firstSeg.getAnalysisInformation().join(secondSeg.getAnalysisInformation()));
      current.setPotentiallyEmpty(false);
      current.setNextSegment(new FinalSegment<>(this.tEmptyElement));
      res.add(0, current);

      for (int i = firstSegs.size() - 2; i >= 0; i--) {
        firstSeg = firstSegs.get(i);
        secondSeg = secondSegs.get(i);

        ArraySegment<T> last = current;
        current =
            new ArraySegment<>(
                firstSeg.getSegmentBound(),
                firstSeg.getAnalysisInformation().join(secondSeg.getAnalysisInformation()),
                firstSeg.isPotentiallyEmpty() || secondSeg.isPotentiallyEmpty(),
                last,
                this.language);
        res.add(0, current);

      }

      ArraySegmentationState<T> mergedSeg =
          new ArraySegmentationState<>(
              res,
              this.tEmptyElement,
              this.tLisOfArrayVariables,
              this.tArray,
              sizeVar,
              language,
              unifiedSegs.getFirst().isCanBeEmpty() || unifiedSegs.getSecond().isCanBeEmpty(),
              this.cpaName,
              this.propertyPredicate,
              this.logger,
              pOther.getCallStack(),
              this.pathFormula.join(pOther.pathFormula),
              pOther.splitCondition);
      logger
          .log(
              ExtendedLocationArrayContentCPA.GENERAL_LOG_LEVEL,
              "Merged the elements " + first + " and " + second + "to " + mergedSeg);
      if (mergedSeg.equals(pOther)) {
        return pOther;
      } else {
        return mergedSeg;
      }
    } else {
      throw new CPAException("Cannot merge segmentations belonging to a differnet split!");
    }
  }

  @Override
  public boolean isLessOrEqual(ArraySegmentationState<T> pOther)
      throws CPAException, InterruptedException {

    Pair<ArraySegmentationState<T>, ArraySegmentationState<T>> unifiedSegs =
        unifier.unifyCompare(
            this,
            pOther,
            tEmptyElement.getBottomElement(),
            tEmptyElement.getBottomElement(),
            tEmptyElement.getMeetOperator());

    // Come corner cases for error and unreachable segmentations
    if (unifiedSegs.getFirst() instanceof UnreachableSegmentation) {
      return true;
    } else if (unifiedSegs.getSecond() instanceof UnreachableSegmentation) {
      return false;// since the first is not unreachable
    } else if (unifiedSegs.getSecond() instanceof ErrorSegmentation) {
      return true;
    } else if (unifiedSegs.getFirst() instanceof ErrorSegmentation) {
      return false; // since second is not error
    }

    List<ArraySegment<T>> firstSegs = new ArrayList<>(unifiedSegs.getFirst().getSegments());
    List<ArraySegment<T>> secondSegs = new ArrayList<>(unifiedSegs.getSecond().getSegments());

    if (firstSegs.isEmpty()) {
      throw new CPAException("The unification has fail!");
    }

    // Check if both segmentations are empty or not
    if (unifiedSegs.getFirst().canBeEmpty != unifiedSegs.getSecond().canBeEmpty) {
      return false;
    }

    for (int i = 0; i < firstSegs.size(); i++) {
      ArraySegment<T> firstSeg = firstSegs.get(i);
      ArraySegment<T> secondSeg = secondSegs.get(i);
      if (!firstSeg.getAnalysisInformation().isLessOrEqual(secondSeg.getAnalysisInformation())) {
        return false;
      }

      // Since |_| (=false) < ? (=true) , ?_i !<= ?_i', iff ?_i = true & ß_i' = false
      if (firstSeg.isPotentiallyEmpty() && !secondSeg.isPotentiallyEmpty()) {
        return false;
      }

    }
    return true;
  }

  public ArraySegmentationState<T> strengthn(Collection<AExpression> eColl) {
    for (AExpression e : eColl) {
      if (e instanceof CBinaryExpression || e instanceof JBinaryExpression) {
        this.segments.parallelStream().forEach(s -> s.strengthn(e));
      }
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
    if (this.getSegments().isEmpty()) {
      throw new CPAException(
          "The segmentation "
              + this.toString()
              + " does not conaint a single segmentation, hence the computation is aborted");
    }
    // By assumption, the segmentation contains at least one segment!
    ArraySegment<T> prevSegment = this.segments.get(0);
    List<ArraySegment<T>> newSegments = new ArrayList<>();

    for (int i = 1; i < this.segments.size(); i++) {
      ArraySegment<T> segment = segments.get(i);
      if (segment.getSegmentBound().isEmpty()) {
        prevSegment.setAnalysisInformation(
            prevSegment.getAnalysisInformation().join(segment.getAnalysisInformation()));
        prevSegment
            .setPotentiallyEmpty(prevSegment.isPotentiallyEmpty() || segment.isPotentiallyEmpty());
        prevSegment.setNextSegment(segment.getNextSegment());
      } else {
        // The segment bound is not empty, hence the previous segment can be stored, since it will
        // not be modified anymore
        newSegments.add(prevSegment);
        prevSegment = segment;
      }
    }
    newSegments.add(prevSegment);
    this.segments = newSegments;
  }

  /**
   *
   * @return true, if the array that is analyzed is currently empty, else false
   */
  public boolean isEmptyArray() {
    // If only one segment is present, by assumption the array analyzed is empty
    return segments.size() <= 1;
  }

  /**
   * Adds a segment at after the segment {@code after} and set the next parameter correctly.
   *
   * @param toAdd segment to add
   * @param after position to add after
   * @return true if the segment is added, false if after is not present
   */
  public boolean addSegment(ArraySegment<T> toAdd, ArraySegment<T> after) {
    if (!segments.contains(after)) {
      return false;
    }
    int posToAdd = segments.indexOf(after) + 1;
    ArraySegment<T> cur = segments.get((posToAdd - 1));
    toAdd.setNextSegment(cur.getNextSegment());
    cur.setNextSegment(toAdd);
    segments.add(posToAdd, toAdd);
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
   * @param pCfaEdge edge for logging
   * @return true, if the operation was successful
   */
  public boolean storeAnalysisInformationAtIndex(
      AExpression index,
      T analysisInfo,
      boolean newSegmentIsPotentiallyEmpty,
      MachineModel machineModel,
      ExpressionSimplificationVisitor pVisitor,
      CFAEdge pCfaEdge) {
    if (language.equals(Language.C) && index instanceof CExpression) {
      CSegmentationModifier<T> modifier =
          new CSegmentationModifier<>(logger, machineModel, pVisitor);
      try {
        ArraySegmentationState<T> res =
            modifier.storeAnalysisInformationAtIndex(
                this.getDeepCopy(),
                (CExpression) index,
                analysisInfo,
                newSegmentIsPotentiallyEmpty,
                pCfaEdge);
        this.segments = new ArrayList<>(res.getSegments());
      } catch (ArrayModificationException e) {
        return false;
      }
      return true;
    } else {
      throw new UnsupportedOperationException();
    }
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
    CSegmentationModifier<T> modifier = new CSegmentationModifier<>(logger, machineModel, pVisitor);
    try {
      ArraySegmentationState<T> res =
          modifier.storeAnalysisInformationAtIndexWithoutAddingBounds(
              this.getDeepCopy(),
              index,
              analysisInfo,
              newSegmentIsPotentiallyEmpty);
      this.segments = new ArrayList<>(res.getSegments());
    } catch (ArrayModificationException e) {
      return false;
    }
    return true;
  }

  public CPropertySpec<T> getSegmentsForProperty(
      T pProperty,
      EnhancedCExpressionSimplificationVisitor pVisitor,
      CBinaryExpressionBuilder pBuilder)
      throws CPAException {
    return new CPropertySpec<>(this, pProperty, pVisitor, pBuilder);
  }

  /**
   *
   * @return a list containing all AIDExpressions present in the segment bounds
   */
  public List<AIdExpression> getVariablesPresent() {
    Set<AIdExpression> res = new HashSet<>();
    this.segments.forEach(s -> res.addAll(s.getVariablesPresent()));
    return new ArrayList<>(res);
  }

  /**
   * Computes the set of all expressions, that are present in the segment bounds, where collecting
   * is started at "startOFCollection". THe function is mainly used during unification
   *
   * @param startOfCollection the element to start collection
   * @return all expressions
   */
  public Set<AExpression> getSegmentBounds(ArraySegment<T> startOfCollection) {
    Set<AExpression> result = new HashSet<>();
    if (segments.contains(startOfCollection)) {
      ArraySegment<T> cur = startOfCollection;
      for (int i = segments.indexOf(startOfCollection); i < segments.size(); i++) {
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

  public void setCanBeEmpty(boolean pB) {
    this.canBeEmpty = pB;
  }

  /**
   *
   * @param pOp2 the expression to search for
   * @return the position of the segmentation or -1, if not present
   */
  public int getSegBoundContainingExpr(AExpression pOp2) {
    for (int i = 0; i < this.segments.size(); i++) {
      if (segments.get(i).getSegmentBound().contains(pOp2)) {
        return i;
      }
    }
    return -1;
  }

  public ImmutableList<ArraySegment<T>> getSegments() {
    return ImmutableList.copyOf(segments);
  }

  public void setSegments(List<ArraySegment<T>> pSegments) {
    segments = pSegments;
  }

  public ImmutableList<AExpression> gettListOfArrayVariables() {
    return ImmutableList.copyOf(tLisOfArrayVariables);
  }

  public void settLisOfArrayVariables(List<AExpression> pTLisOfArrayVariables) {
    tLisOfArrayVariables = pTLisOfArrayVariables;
  }

  public AIdExpression gettArray() {
    return tArray;
  }

  public AExpression getSizeVar() {
    return sizeVar;
  }

  public T gettEmptyElement() {
    return tEmptyElement;
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

  public boolean isCanBeEmpty() {
    return canBeEmpty;
  }

  public Language getLanguage() {
    return language;
  }

  public Predicate<ArraySegmentationState<T>> getPropertyPredicate() {
    return propertyPredicate;
  }

  public CallstackState getCallStack() {
    return callStack;
  }

  public void setCallStack(CallstackState pCallStack) {
    callStack = pCallStack;
  }

  public FormulaState getPathFormula() {
    return pathFormula;
  }

  public void setPathFormula(FormulaState pPathFormula) {
    pathFormula = pPathFormula;
  }

  @Override
  public int hashCode() {
    return Objects.hash(segments, tEmptyElement);
  }

  public AExpression getSplitCondition() {
    return splitCondition;
  }

  public void setSplitCondition(AExpression pSplitCondition) {
    splitCondition = pSplitCondition;
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
    ArraySegmentationState<T> other = (ArraySegmentationState<T>) obj;
    return Objects.deepEquals(segments, other.segments)
        && Objects.equals(this.canBeEmpty, other.canBeEmpty)
        && Objects.equals(tEmptyElement, other.tEmptyElement);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    // TODO: Transform this to java
    if (this.language.equals(Language.C) && !splitCondition.equals(CIntegerLiteralExpression.ONE)) {
      builder.append(this.splitCondition.toASTString() + ": ");
    }
    this.segments.stream().forEachOrdered(s -> builder.append(s.toString()));
    if (canBeEmpty) {
      builder.append(" " + (char) 709 + " ARRAY_IS_EMPTY ");
    }
    return builder.toString();
  }

  @Override
  public String toDOTLabel() {
    return this.toString();
  }

  @Override
  public String getCPAName() {
    return this.cpaName;
  }

  @Override
  public boolean checkProperty(String pProperty) throws InvalidQueryException {
    return propertyPredicate.test(this);
  }

}
