// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.coverage;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableCollection;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class PredicateCoverageCPATransferRelation extends AbstractSingleWrapperTransferRelation {

  private final TransferRelation predicateTransferRelation;
  private Instant startTime = Instant.MIN;
  private final Map<Long, Double> timeStampsPerCoverage;

  PredicateCoverageCPATransferRelation(
      TransferRelation pDelegateTransferRelation, Map<Long, Double> pTimeStampsPerCoverage) {
    super(pDelegateTransferRelation);
    predicateTransferRelation = pDelegateTransferRelation;
    timeStampsPerCoverage = pTimeStampsPerCoverage;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(
      AbstractState state, Precision precision) throws CPATransferException, InterruptedException {
    return predicateTransferRelation.getAbstractSuccessors(state, precision);
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState state, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {
    if (startTime.equals(Instant.MIN)) {
      startTime = Instant.now();
    }
    double predicateCoverage = calculatePredicateCoverage(precision);
    addTimeStamps(predicateCoverage);
    return predicateTransferRelation.getAbstractSuccessorsForEdge(state, precision, cfaEdge);
  }

  private void addTimeStamps(double predicateCoverage) {
    long durationInNanos = Duration.between(startTime, Instant.now()).toNanos();
    long durationInMicros = TimeUnit.NANOSECONDS.toMicros(durationInNanos);
    timeStampsPerCoverage.put(durationInMicros, predicateCoverage);
  }

  private double calculatePredicateCoverage(Precision precision) {
    if (!(precision instanceof PredicatePrecision)) {
      return 0.0;
    }
    Set<BooleanFormula> programPredicates = new HashSet<>();
    PredicatePrecision predicatePrecision = (PredicatePrecision) precision;
    addPredicatesToSet(programPredicates, predicatePrecision.getFunctionPredicates().values());
    addPredicatesToSet(programPredicates, predicatePrecision.getLocalPredicates().values());
    addPredicatesToSet(programPredicates, predicatePrecision.getGlobalPredicates());

    return programPredicates.size();
  }

  private void addPredicatesToSet(
      Set<BooleanFormula> allPredicates, ImmutableCollection<AbstractionPredicate> predicates) {
    allPredicates.addAll(
        Collections2.transform(
            predicates, pAbstractionPredicate -> pAbstractionPredicate.getSymbolicAtom()));
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState state,
      Iterable<AbstractState> otherStates,
      @Nullable CFAEdge cfaEdge,
      Precision precision)
      throws CPATransferException, InterruptedException {
    return predicateTransferRelation.strengthen(state, otherStates, cfaEdge, precision);
  }
}
