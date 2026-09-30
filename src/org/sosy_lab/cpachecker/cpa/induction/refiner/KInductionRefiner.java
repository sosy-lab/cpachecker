// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.induction.refiner;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.induction.InductionPrecision;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * Makes the CEGAR loop behave like k-induction: the abstractions are true, and every induction step
 * that the SAT queries of native k-induction cannot prove increases k by one. The queries are those
 * of the base case, the bounding assertions and the step case.
 */
public final class KInductionRefiner extends InductionRefiner {

  private final StatInt maxK = new StatInt(StatKind.MAX, "Maximal k");
  private final StatCounter provedByBoundingAssertions =
      new StatCounter("Proofs by unreachable induction starts");
  private final StatCounter provedByStepCase = new StatCounter("Proofs by the step case");

  private KInductionRefiner(ConfigurableProgramAnalysis pCpa, LogManager pLogger)
      throws InvalidConfigurationException {
    super(pCpa, pLogger);
  }

  public static Refiner create(ConfigurableProgramAnalysis pCpa, LogManager pLogger)
      throws InvalidConfigurationException {
    return new KInductionRefiner(pCpa, pLogger);
  }

  @Override
  protected Optional<ARGState> refine(
      ARGReachedSet pReached, List<ARGState> pTargets, List<ARGState> pCutOffs)
      throws CPAException, InterruptedException, SolverException {
    if (!pCutOffs.isEmpty()) {
      throw new CPAException("k-induction needs a precision that describes all abstractions");
    }
    List<ARGState> baseCaseTargets =
        from(pTargets).filter(t -> !inductionStateOf(t).isInInductionStep()).toList();
    Optional<ARGState> counterexample = findFeasibleTarget(baseCaseTargets);
    if (counterexample.isPresent()) {
      return counterexample;
    }
    removeUnreachable(pReached, baseCaseTargets);

    List<ARGState> stepTargets =
        from(pTargets).filter(t -> inductionStateOf(t).isInInductionStep()).toList();
    if (stepTargets.isEmpty()) {
      return Optional.empty();
    }
    Set<ARGState> inductionStarts = new LinkedHashSet<>();
    for (ARGState target : stepTargets) {
      inductionStarts.add(firstLoopHeadAbstraction(target).orElseThrow());
    }
    if (removeIfNoneReachable(pReached, inductionStarts, this::concretePathFormula)) {
      provedByBoundingAssertions.inc();
      return Optional.empty();
    }
    if (removeIfNoneReachable(pReached, stepTargets, this::lastBlockFromRegion)) {
      provedByStepCase.inc();
      return Optional.empty();
    }

    for (ARGState start : inductionStarts) {
      if (start.isDestroyed()) {
        // inside the subtree of another induction start that is explored again
        continue;
      }
      InductionPrecision precision =
          inductionPrecisionOf(pReached, start.getParents().getFirst())
              .withAll(p -> p.withK(p.k() + 1));
      precision.getLoops().values().forEach(p -> maxK.setNextValue(p.k()));
      reexplore(
          pReached,
          start,
          ImmutableList.of(precision),
          ImmutableList.of(Predicates.instanceOf(InductionPrecision.class)));
    }
    return Optional.empty();
  }

  @Override
  protected void printRefinerStatistics(StatisticsWriter pWriter) {
    pWriter.put(maxK).put(provedByBoundingAssertions).put(provedByStepCase);
  }
}
