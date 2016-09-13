/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.termination;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Preconditions;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.RankingRelation;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithDummyLocation;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

@Immutable
public class TerminationState extends AbstractSingleWrapperState
    implements AbstractStateWithDummyLocation, FormulaReportingState, Graphable {

  private static final long serialVersionUID = 4L;

  /**
   * The location where the loop of the lasso was entered
   * or <code>null</code> iff this state is part of the stem
   */
  private final @Nullable CFANode hondaLocation;

  private final boolean dummyLocation;

  private final Collection<CFAEdge> enteringEdges;

  @Nullable private final Set<Property> violatedProperties;

  @Nullable private final RankingRelation unsatisfiedRankingRelation;

  private TerminationState(
      AbstractState pWrappedState,
      @Nullable CFANode pHondaLocation,
      boolean pDummyLocation,
      Collection<CFAEdge> pEnteringEdges,
      @Nullable Set<Property> pviolatedProperties,
      @Nullable RankingRelation pUnsatisfiedRankingRelation) {
    super(checkNotNull(pWrappedState));
    Preconditions.checkArgument(pDummyLocation || pEnteringEdges.isEmpty());
    hondaLocation = pHondaLocation;
    dummyLocation = pDummyLocation;
    enteringEdges = checkNotNull(pEnteringEdges);
    violatedProperties = pviolatedProperties;
    unsatisfiedRankingRelation = pUnsatisfiedRankingRelation;
  }

  private TerminationState(
      AbstractState pWrappedState,
      @Nullable CFANode pHondaLocation,
      boolean pDummyLocation,
      Collection<CFAEdge> pEnteringEdges) {
    this(pWrappedState, pHondaLocation, pDummyLocation, pEnteringEdges, null, null);
  }

  /**
   * Creates a new {@link TerminationState} that is part of the
   * lasso's stem and has no dummy location.
   *
   * @param pWrappedState
   *          the {@link AbstractState} to wrap
   * @return the created {@link TerminationState}
   */
  public static TerminationState createStemState(AbstractState pWrappedState) {
    return new TerminationState(pWrappedState, null, false, Collections.emptyList());
  }

  /**
   * Creates a new {@link TerminationState} from this {@link TerminationState}
   * but with the given <code>pWrappedState</code>.
   *
   * @param pWrappedState
   *            the {@link AbstractState} to wrap
   * @return the created {@link TerminationState}
   */
  public TerminationState withWrappedState(AbstractState pWrappedState) {
    return new TerminationState(pWrappedState, hondaLocation, dummyLocation, enteringEdges);
  }

  /**
   * Creates a new {@link TerminationState} that is the first state of the lasso's loop.
   * @param pHondaLocation the first location of the loop
   * @return the created {@link TerminationState}
   */
  public TerminationState enterLoop(CFANode pHondaLocation) {
    checkArgument(isPartOfStem(), "% is entered the lasso's loop at %s", this, hondaLocation);
    return new TerminationState(getWrappedState(), pHondaLocation, dummyLocation, enteringEdges);
  }

  /**
   * Creates a new {@link TerminationState} with a dummy location and  the given entering edges.
   *
   * @param pEnteringEdges
   *         the edges entering the location represented by the created state
   * @return the created {@link TerminationState}
   */
  public TerminationState withDummyLocation(Collection<CFAEdge> pEnteringEdges) {
    return new TerminationState(getWrappedState(), hondaLocation, true, pEnteringEdges);
  }

  /**
   * Creates a new {@link TerminationState} with the given violated properties.
   *
   * @param pViolatedProperties
   *         the edges entering the location represented by the created state
   * @return the created {@link TerminationState}
   */
  public TerminationState withViolatedProperties(Set<Property> pViolatedProperties) {
    Preconditions.checkNotNull(pViolatedProperties);
    Preconditions.checkArgument(!pViolatedProperties.isEmpty());
    return new TerminationState(
        getWrappedState(), hondaLocation, dummyLocation, enteringEdges, pViolatedProperties, null);
  }

  /**
   * Creates a new {@link TerminationState} with the given unsatisfied {@link RankingRelation}
   *
   * @param pUnsatisfiedRankingRelation
   *         the {@link RankingRelation} not satisfied at this state
   * @return the created {@link TerminationState}
   */
  public TerminationState withUnsatisfiedRankingRelation(
      RankingRelation pUnsatisfiedRankingRelation) {
    Preconditions.checkNotNull(pUnsatisfiedRankingRelation);
    return new TerminationState(
        getWrappedState(),
        hondaLocation,
        dummyLocation,
        enteringEdges,
        violatedProperties,
        pUnsatisfiedRankingRelation);
  }

  @Override
  public boolean isDummyLocation() {
    return dummyLocation;
  }

  @Override
  public Collection<CFAEdge> getEnteringEdges() {
    return enteringEdges;
  }

  /**
   * @return <code>true</code> iff this {@link TerminationState} is part of the lasso's loop.
   */
  public boolean isPartOfLoop() {
    return hondaLocation != null;
  }

  /**
   * @return <code>true</code> iff this {@link TerminationState} is part of the lasso's stem.
   */
  public boolean isPartOfStem() {
    return hondaLocation == null;
  }

  public CFANode getHondaLocation() {
    return hondaLocation;
  }

  @Override
  public BooleanFormula getFormulaApproximation(FormulaManagerView pManager) {
    if (unsatisfiedRankingRelation == null) {
      return pManager.getBooleanFormulaManager().makeBoolean(true);
    } else {
      return pManager.makeNot(unsatisfiedRankingRelation.asFormulaFromOtherSolver(pManager));
    }
  }

  @Override
  public boolean isTarget() {
    return violatedProperties != null || super.isTarget();
  }

  @Override
  public Set<Property> getViolatedProperties() throws IllegalStateException {
    if (violatedProperties != null) {
      return violatedProperties;
    } else {
      return super.getViolatedProperties();
    }
  }

  @Override
  public String toDOTLabel() {
    StringBuilder sb = new StringBuilder();
    if (isPartOfStem()) {
      sb.append("stem");
    } else {
      sb.append("loop");
    }

    if (getWrappedState() instanceof Graphable) {
      sb.append("\n");
      sb.append(((Graphable) getWrappedState()).toDOTLabel());
    }

    return sb.toString();
  }

  @Override
  public boolean shouldBeHighlighted() {
    if (getWrappedState() instanceof Graphable) {
      return ((Graphable) getWrappedState()).shouldBeHighlighted();
    }
    return false;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(TerminationState.class.getSimpleName());
    if (isPartOfStem()) {
      sb.append("(stem)");
    } else {
      sb.append("(loop)");
    }

    sb.append(" ");
    sb.append(getWrappedState());

    return sb.toString();
  }

  /**
   * Throws {@link UnsupportedOperationException}.
   * @param out unused
   */
  private void writeObject(ObjectOutputStream out) {
    throw new UnsupportedOperationException(
        TerminationState.class.getSimpleName() + "does not support serialization.");
  }

  /**
   * Throws {@link UnsupportedOperationException}.
   * @param in unused
   */
  private void readObject(ObjectInputStream in) {
    throw new UnsupportedOperationException(
        TerminationState.class.getSimpleName() + "does not support serialization.");
  }
}
