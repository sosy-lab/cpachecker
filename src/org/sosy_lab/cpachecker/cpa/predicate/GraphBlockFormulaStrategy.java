/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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

import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * BlockFormulaStrategy for graph-like ARGs where
 * the formulas have to be generated on the fly.
 * Intended for use with {@link SlicingAbstractionsStrategy}
 */
@Options(prefix = "cpa.predicate.refinement")
public class GraphBlockFormulaStrategy extends BlockFormulaStrategy {

  @Option(
      secure = true,
      description = "Enable/Disable adding partial state invariants into the PathFormulas"
    )
  private boolean includePartialInvariants = true;

  private PathFormulaManager pfmgr;
  private Solver solver;

  @SuppressWarnings("options")
  public GraphBlockFormulaStrategy(Solver solver, Configuration pConfig, PathFormulaManager pPfmgr) throws InvalidConfigurationException {
    this.pfmgr = pPfmgr;
    this.solver = solver;
    pConfig.inject(this);
  }

  @Override
  List<BooleanFormula> getFormulasForPath(final ARGState pRoot, final List<ARGState> pPath)
      throws CPATransferException, InterruptedException {

    final List<BooleanFormula> abstractionFormulas = new ArrayList<>();
    final List<PathFormula>pathFormulas = new ArrayList<>();

    // Handle root state separately:
    pathFormulas.add(pfmgr.makeEmptyPathFormula());
    abstractionFormulas.add(pathFormulas.get(0).getFormula());

    for (int i = 0; i< pPath.size()-1; i++) {
      ARGState parent = pPath.get(i);
      ARGState child = pPath.get(i+1);

      PathFormula previousPathFormula = pathFormulas.get(i);
      CFAEdge edge = parent.getEdgeToChild(child);

      //calculate the PathFormula from parent to child with the right SSA indices (from previousPathFormula):
      PathFormula newPathFormula;
      if (includePartialInvariants) {
        BooleanFormula partialInvariant = PredicateAbstractState.getPredicateState(parent).getAbstractionFormula().asFormula();
        partialInvariant = solver.getFormulaManager().instantiate(partialInvariant,previousPathFormula.getSsa());
        newPathFormula = pfmgr.makeAnd(pfmgr.makeEmptyPathFormula(previousPathFormula),partialInvariant);
        newPathFormula = pfmgr.makeAnd(newPathFormula, edge);
      } else {
        newPathFormula = pfmgr.makeAnd(pfmgr.makeEmptyPathFormula(previousPathFormula),edge);
      }

      // update lists:
      pathFormulas.add(newPathFormula);
      abstractionFormulas.add(newPathFormula.getFormula());
    }

    return abstractionFormulas;
  }

}