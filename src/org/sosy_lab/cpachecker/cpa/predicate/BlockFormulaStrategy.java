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
package org.sosy_lab.cpachecker.cpa.predicate;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.toState;

import com.google.common.base.Function;

import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.java_smt.api.BooleanFormula;

import java.util.List;

/**
 * This class represents a strategy to get the sequence of block formulas
 * from an ARG path.
 * This class implements the trivial strategy (just get the formulas from the states),
 * but for example {@link BlockFormulaSlicer} implements a more refined strategy.
 * Typically {@link PredicateCPARefinerFactory} automatically creates the desired strategy.
 */
public class BlockFormulaStrategy {

  static final Function<PredicateAbstractState, BooleanFormula> GET_BLOCK_FORMULA =
      e -> {
        checkArgument(e.isAbstractionState());
        return e.getAbstractionFormula().getBlockFormula().getFormula();
      };

  /**
   * Get the block formulas from a path.
   * @param argRoot The initial element of the analysis (= the root element of the ARG)
   * @param abstractionStates A list of all abstraction elements
   * @return A list of block formulas for this path.
   * @throws CPATransferException If CFA edges cannot be analyzed
   *    (should not happen because the main analyses analyzed them successfully).
   * @throws InterruptedException On shutdown request.
   */
  List<BooleanFormula> getFormulasForPath(ARGState argRoot, List<ARGState> abstractionStates)
      throws CPATransferException, InterruptedException {
    return from(abstractionStates)
        .transform(toState(PredicateAbstractState.class))
        .transform(GET_BLOCK_FORMULA)
        .toList();
  }
}
