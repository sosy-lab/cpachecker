// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg.counterexamples;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;

/**
 * A {@link CounterexampleFilter} that defines paths as similar if their representation as a list of
 * {@link CFAEdge}s is equal.
 */
public class PathEqualityCounterexampleFilter
    extends AbstractSetBasedCounterexampleFilter<List<CFAEdge>> {

  public PathEqualityCounterexampleFilter(
      Configuration pConfig, LogManager pLogger, ConfigurableProgramAnalysis pCpa) {
    super(pConfig, pLogger, pCpa);
  }

  @Override
  protected Optional<List<CFAEdge>> getCounterexampleRepresentation(
      CounterexampleInfo counterexample) {
    return Optional.of(
        Collections.unmodifiableList(
            new ArrayList<>(counterexample.getTargetPath().getInnerEdges())));
  }
}
