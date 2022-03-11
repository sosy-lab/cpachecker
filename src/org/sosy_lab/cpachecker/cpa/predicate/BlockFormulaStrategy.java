// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.toState;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.collect.Collections3;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * This class represents a strategy to get the sequence of block formulas from an ARG path. This
 * class implements the trivial strategy (just get the formulas from the states), but for example
 * {@link BlockFormulaSlicer} implements a more refined strategy. Typically {@link
 * PredicateCPARefinerFactory} automatically creates the desired strategy.
 */
public class BlockFormulaStrategy {

  public static class BlockFormulas {
    private final ImmutableList<BooleanFormula> formulas;
    private @Nullable BooleanFormula branchingFormula;

    public BlockFormulas(List<BooleanFormula> pFormulas) {
      formulas = ImmutableList.copyOf(pFormulas);
    }

    public static BlockFormulas createFromPathFormulas(List<PathFormula> pPathFormulas) {
      return new BlockFormulas(
          Collections3.transformedImmutableListCopy(pPathFormulas, PathFormula::getFormula));
    }

    public BlockFormulas(List<BooleanFormula> pFormulas, BooleanFormula pBranchingFormula) {
      this(pFormulas);
      branchingFormula = pBranchingFormula;
    }

    public BlockFormulas withBranchingFormula(BooleanFormula pBranchingFormula) {
      checkState(branchingFormula == null);
      return new BlockFormulas(formulas, pBranchingFormula);
    }

    public ImmutableList<BooleanFormula> getFormulas() {
      return formulas;
    }

    public @Nullable BooleanFormula getBranchingFormula() {
      return branchingFormula;
    }

    public int getSize() {
      return formulas.size();
    }

    public boolean hasBranchingFormula() {
      return branchingFormula != null;
    }

    @Override
    public String toString() {
      return "BlockFormulas " + getFormulas();
    }
  }

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
  BlockFormulas getFormulasForPath(ARGState argRoot, List<ARGState> abstractionStates)
      throws CPATransferException, InterruptedException {
    return new BlockFormulas(
        from(abstractionStates)
            .transform(toState(PredicateAbstractState.class))
            .transform(PredicateAbstractState::getBlockFormula)
            .toList());
  }
}
