// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.state_transformer;

import com.google.common.base.Splitter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;

public class PredicateAbstractStateTransformer extends AbstractStateTransformer<PredicateAbstractState> {

  @Override
  protected BooleanFormula transform(
      PredicateAbstractState state, FormulaManagerView fmgr, AnalysisDirection direction, String uniqueVariableId) {
    PathFormula pathFormula = state.getPathFormula();
    SSAMap ssaMap = pathFormula.getSsa();
    Map<String, Formula> variableToFormula = fmgr.extractVariables(pathFormula.getFormula());
    Map<Formula, Formula> substitutions = new HashMap<>();
    for (Entry<String, Formula> stringFormulaEntry : variableToFormula.entrySet()) {
      String name = stringFormulaEntry.getKey();
      Formula formula = stringFormulaEntry.getValue();
      List<String> nameAndIndex = Splitter.on("@").limit(2).splitToList(name);
      if (nameAndIndex.size() < 2 || nameAndIndex.get(1).isEmpty() || name.contains(".")) {
        substitutions.put(formula, fmgr.makeVariable(fmgr.getFormulaType(formula), name));
        continue;
      }
      name = nameAndIndex.get(0);
      int index = Integer.parseInt(nameAndIndex.get(1));
      int highestIndex = ssaMap.getIndex(name);
      if (index != highestIndex) {
        substitutions.put(formula, fmgr.makeVariable(fmgr.getFormulaType(formula),
            name + "." + uniqueVariableId + direction.name() + index));
      } else {
        substitutions.put(formula, fmgr.makeVariable(fmgr.getFormulaType(formula), name));
      }
    }
    return fmgr.substitute(pathFormula.getFormula(), substitutions);
  }

  @Override
  public Class<PredicateAbstractState> getAbstractStateClass() {
    return PredicateAbstractState.class;
  }
}
