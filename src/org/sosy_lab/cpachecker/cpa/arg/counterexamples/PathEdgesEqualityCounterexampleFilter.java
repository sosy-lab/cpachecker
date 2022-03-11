// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg.counterexamples;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;

/**
 * A {@link CounterexampleFilter} that defines paths as similar if they contain the exact same set
 * of {@link CFAEdge}s, but the order of the edges, and how many times they are visited along the
 * path, are irrelevant.
 *
 * <p>This filter subsumes {@link PathEqualityCounterexampleFilter}, so if you use this class, you
 * do not need to (additionally) use {@link PathEqualityCounterexampleFilter}.
 */
public class PathEdgesEqualityCounterexampleFilter
    extends AbstractSetBasedCounterexampleFilter<ImmutableSet<CFAEdge>> {

  public PathEdgesEqualityCounterexampleFilter(
      Configuration pConfig, LogManager pLogger, ConfigurableProgramAnalysis pCpa) {
    super(pConfig, pLogger, pCpa);
  }

  @Override
  protected Optional<ImmutableSet<CFAEdge>> getCounterexampleRepresentation(
      CounterexampleInfo counterexample) {
    return Optional.of(
        from(counterexample.getTargetPath().getInnerEdges()).filter(Predicates.notNull()).toSet());
  }
}
