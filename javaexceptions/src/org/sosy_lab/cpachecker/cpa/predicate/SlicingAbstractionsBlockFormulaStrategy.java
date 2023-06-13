// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import com.google.common.collect.ImmutableSet;
import java.util.List;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.predicate.SlicingAbstractionsUtils.AbstractionPosition;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;

/**
 * BlockFormulaStrategy for graph-like ARGs where the formulas have to be generated on the fly.
 * Intended for use with {@link SlicingAbstractionsStrategy}
 */
@Options(prefix = "cpa.predicate.refinement")
public class SlicingAbstractionsBlockFormulaStrategy extends BlockFormulaStrategy {

  @Option(
      secure = true,
      description = "Enable/Disable adding partial state invariants into the PathFormulas")
  private ImmutableSet<AbstractionPosition> includePartialInvariants = AbstractionPosition.BOTH;

  private PathFormulaManager pfmgr;
  private Solver solver;

  public SlicingAbstractionsBlockFormulaStrategy(
      Solver solver, Configuration pConfig, PathFormulaManager pPfmgr)
      throws InvalidConfigurationException {
    pfmgr = pPfmgr;
    this.solver = solver;
    pConfig.inject(this);
  }

  @Override
  BlockFormulas getFormulasForPath(final ARGState pRoot, final List<ARGState> pPath)
      throws CPATransferException, InterruptedException {
    return BlockFormulas.createFromPathFormulas(
        SlicingAbstractionsUtils.getFormulasForPath(
            pfmgr, solver, pRoot, pPath, includePartialInvariants));
  }
}
