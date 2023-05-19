// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.termination;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.RankingRelation;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithDummyLocation;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

@javax.annotation.concurrent.Immutable // cannot prove deep immutability
public class TerminationState extends AbstractSingleWrapperState
    implements AbstractStateWithDummyLocation, FormulaReportingState, Graphable {

  /**
   * The location where the loop of the lasso was entered or <code>null</code> iff this state is
   * part of the stem
   */
  private final @Nullable CFANode hondaLocation;

  private final boolean dummyLocation;

  private final Collection<CFAEdge> enteringEdges;

  @Nullable private final Set<TargetInformation> targetInformation;

  @Nullable private final RankingRelation unsatisfiedRankingRelation;

  private TerminationState(
      AbstractState pWrappedState,
      @Nullable CFANode pHondaLocation,
      boolean pDummyLocation,
      Collection<CFAEdge> pEnteringEdges,
      @Nullable Set<TargetInformation> pTargetInformation,
      @Nullable RankingRelation pUnsatisfiedRankingRelation) {
    super(checkNotNull(pWrappedState));
    Preconditions.checkArgument(pDummyLocation || pEnteringEdges.isEmpty());
    hondaLocation = pHondaLocation;
    dummyLocation = pDummyLocation;
    enteringEdges = checkNotNull(pEnteringEdges);
    targetInformation = pTargetInformation;
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
   * Creates a new {@link TerminationState} that is part of the lasso's stem and has no dummy
   * location.
   *
   * @param pWrappedState the {@link AbstractState} to wrap
   * @return the created {@link TerminationState}
   */
  public static TerminationState createStemState(AbstractState pWrappedState) {
    return new TerminationState(pWrappedState, null, false, ImmutableList.of());
  }

  /**
   * Creates a new {@link TerminationState} from this {@link TerminationState} but with the given
   * <code>pWrappedState</code>.
   *
   * @param pWrappedState the {@link AbstractState} to wrap
   * @return the created {@link TerminationState}
   */
  public TerminationState withWrappedState(AbstractState pWrappedState) {
    return new TerminationState(pWrappedState, hondaLocation, dummyLocation, enteringEdges);
  }

  /**
   * Creates a new {@link TerminationState} that is the first state of the lasso's loop.
   *
   * @param pHondaLocation the first location of the loop
   * @return the created {@link TerminationState}
   */
  public TerminationState enterLoop(CFANode pHondaLocation) {
    checkArgument(isPartOfStem(), "%s is entered the lasso's loop at %s", this, hondaLocation);
    return new TerminationState(getWrappedState(), pHondaLocation, dummyLocation, enteringEdges);
  }

  /**
   * Creates a new {@link TerminationState} with a dummy location and the given entering edges.
   *
   * @param pEnteringEdges the edges entering the location represented by the created state
   * @return the created {@link TerminationState}
   */
  public TerminationState withDummyLocation(Collection<CFAEdge> pEnteringEdges) {
    return new TerminationState(getWrappedState(), hondaLocation, true, pEnteringEdges);
  }

  /**
   * Creates a new {@link TerminationState} with the given target information.
   *
   * @param pTargetInformation the edges entering the location represented by the created state
   * @return the created {@link TerminationState}
   */
  public TerminationState withTargetInformation(Set<TargetInformation> pTargetInformation) {
    Preconditions.checkNotNull(pTargetInformation);
    Preconditions.checkArgument(!pTargetInformation.isEmpty());
    return new TerminationState(
        getWrappedState(), hondaLocation, dummyLocation, enteringEdges, pTargetInformation, null);
  }

  /**
   * Creates a new {@link TerminationState} with the given unsatisfied {@link RankingRelation}
   *
   * @param pUnsatisfiedRankingRelation the {@link RankingRelation} not satisfied at this state
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
        targetInformation,
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

  /** Returns <code>true</code> iff this {@link TerminationState} is part of the lasso's loop. */
  public boolean isPartOfLoop() {
    return hondaLocation != null;
  }

  /** Returns <code>true</code> iff this {@link TerminationState} is part of the lasso's stem. */
  public boolean isPartOfStem() {
    return hondaLocation == null;
  }

  public CFANode getHondaLocation() {
    return hondaLocation;
  }

  @Override
  public BooleanFormula getFormulaApproximation(FormulaManagerView pManager) {
    if (unsatisfiedRankingRelation == null) {
      return pManager.getBooleanFormulaManager().makeTrue();
    } else {
      return pManager.makeNot(unsatisfiedRankingRelation.asFormulaFromOtherSolver(pManager));
    }
  }

  @Override
  public boolean isTarget() {
    return targetInformation != null || super.isTarget();
  }

  @Override
  public Set<TargetInformation> getTargetInformation() throws IllegalStateException {
    if (targetInformation != null) {
      return targetInformation;
    } else {
      return super.getTargetInformation();
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
}
