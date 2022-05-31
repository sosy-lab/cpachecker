// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.toState;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import org.sosy_lab.common.collect.Collections3;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.Pair;
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
    private final ImmutableMap<Pair<ARGState, CFAEdge>, PathFormula> branchingFormulas;

    public BlockFormulas(List<BooleanFormula> pFormulas) {
      this(pFormulas, ImmutableMap.of());
    }

    public static BlockFormulas createFromPathFormulas(List<PathFormula> pPathFormulas) {
      return new BlockFormulas(
          Collections3.transformedImmutableListCopy(pPathFormulas, PathFormula::getFormula));
    }

    /**
     * Create an instance. Cf. the getters for documentation about the meaning of the parameters. In
     * most cases it is sufficient to call {@link #BlockFormulas(List)}.
     */
    public BlockFormulas(
        List<BooleanFormula> pFormulas,
        Map<Pair<ARGState, CFAEdge>, PathFormula> pBranchingFormulas) {
      formulas = ImmutableList.copyOf(pFormulas);
      branchingFormulas = ImmutableMap.copyOf(pBranchingFormulas);
    }

    /** The list of path formulas, each representing a block along an ARG path. */
    public ImmutableList<BooleanFormula> getFormulas() {
      return formulas;
    }

    /**
     * An optional mapping from branching points in the ARG to the expression that is attached to
     * the respective assume edge (with the correct context like as SSA indices such that these
     * formulas are compatible with the block formulas). If empty, the meaning is that these
     * formulas can be recreated from the PredicateAbstractStates that are inside the ARGStates.
     */
    public ImmutableMap<Pair<ARGState, CFAEdge>, PathFormula> getBranchingFormulas() {
      return branchingFormulas;
    }

    public int getSize() {
      return formulas.size();
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
