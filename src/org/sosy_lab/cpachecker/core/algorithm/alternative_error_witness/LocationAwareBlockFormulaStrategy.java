// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.alternative_error_witness;

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.toState;

import java.util.List;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.predicate.BlockFormulaStrategy;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class LocationAwareBlockFormulaStrategy extends BlockFormulaStrategy{

  public LocationAwareBlockFormulaStrategy() {}

  /**
   * Get the block formulas from a path.
   *
   * @param argRoot The initial element of the analysis (= the root element of the ARG)
   * @param abstractionStates A list of all abstraction elements
   * @return A list of block formulas for this path.
   * @throws CPATransferException If CFA edges cannot be analyzed (should not happen because the
   *     main analyses analyzed them successfully).
   * @throws InterruptedException On shutdown request.
   */
  public LocationAwareBlockFormulas getLocatinoAwareFormulasForPath(
      ARGState argRoot, List<ARGState> abstractionStates)
      throws CPATransferException, InterruptedException {
    return new LocationAwareBlockFormulas(
        from(abstractionStates)
            .transform(toState(PredicateAbstractState.class))
            .transform(PredicateAbstractState::getBlockFormula)
            .toList(),
        abstractionStates
            .stream()
            .map(as -> AbstractStates.extractLocation(as))
            .collect(Collectors.toList()));
  }

}
