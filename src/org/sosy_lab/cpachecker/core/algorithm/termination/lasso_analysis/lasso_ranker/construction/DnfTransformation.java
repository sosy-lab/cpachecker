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
package org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.lasso_ranker.construction;

import com.google.common.collect.Lists;

import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView.BooleanFormulaTransformationVisitor;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.BooleanFormulaManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

class DnfTransformation extends BooleanFormulaTransformationVisitor {

  private final static int MAX_CLAUSES = 1_000_000;

  private final BooleanFormulaManager fmgr;

  DnfTransformation(FormulaManagerView pFmgr) {
    super(pFmgr);
    fmgr = pFmgr.getBooleanFormulaManager();
  }

  @Override
  public BooleanFormula visitAnd(List<BooleanFormula> pProcessedOperands) {
    Collection<BooleanFormula> clauses = Lists.newArrayList(fmgr.makeBoolean(true));

    for (BooleanFormula operands : pProcessedOperands) {
      Set<BooleanFormula> childOperators = fmgr.toDisjunctionArgs(operands, false);
      clauses =
          clauses
              .stream()
              .flatMap(c -> childOperators.stream().map(co -> fmgr.and(c, co)))
              .collect(Collectors.toCollection(ArrayList::new));

      // Give up and return original formula.
      if (clauses.size() > MAX_CLAUSES) {
        return fmgr.and(pProcessedOperands);
      }
    }

    return fmgr.or(clauses);
  }
}
