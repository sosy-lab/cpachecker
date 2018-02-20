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

import static org.sosy_lab.cpachecker.cpa.predicate.SlicingAbstractionsUtils.buildPathFormula;

import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * BlockFormulaStrategy for graph-like ARGs where
 * the formulas have to be generated on the fly.
 * Intended for use with {@link SlicingAbstractionsStrategy}
 */
@Options(prefix = "cpa.predicate.refinement")
public class SlicingAbstractionsBlockFormulaStrategy extends BlockFormulaStrategy {

  @Option(
      secure = true,
      description = "Enable/Disable adding partial state invariants into the PathFormulas"
    )
  private boolean includePartialInvariants = true;

  private PathFormulaManager pfmgr;
  private Solver solver;

  public SlicingAbstractionsBlockFormulaStrategy(Solver solver, Configuration pConfig, PathFormulaManager pPfmgr)
      throws InvalidConfigurationException {
    this.pfmgr = pPfmgr;
    this.solver = solver;
    pConfig.inject(this);
  }

  @Override
  BlockFormulas getFormulasForPath(final ARGState pRoot, final List<ARGState> pPath)
      throws CPATransferException, InterruptedException {

    final List<BooleanFormula> abstractionFormulas = new ArrayList<>();

    SSAMap startSSAMap = SSAMap.emptySSAMap().withDefault(1);
    PointerTargetSet startPts = PointerTargetSet.emptyPointerTargetSet();
    PathFormula currentPathFormula = buildPathFormula(pRoot, pPath.get(0), startSSAMap, startPts, solver, pfmgr,includePartialInvariants);
    abstractionFormulas.add(currentPathFormula.getFormula());

    for(int i = 0; i<pPath.size()-1; i++) {
      PathFormula oldPathFormula = currentPathFormula;
      currentPathFormula = buildPathFormula(pPath.get(i), pPath.get(i+1),
          oldPathFormula.getSsa(), oldPathFormula.getPointerTargetSet(), solver, pfmgr,includePartialInvariants);
      abstractionFormulas.add(currentPathFormula.getFormula());
    }

    return new BlockFormulas(abstractionFormulas);

  }

}