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
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;

public abstract class AbstractStateTransformer<T extends AbstractState> {

  private int executionCounter;
  private final String id;

  public AbstractStateTransformer(String pId) {
    id = pId;
    executionCounter = 0;
  }

  protected abstract BooleanFormula transform(T state, FormulaManagerView fmgr, AnalysisDirection direction, String uniqueVariableId);

  public abstract Class<T> getAbstractStateClass();

  public final BooleanFormula safeTransform(AbstractState state, FormulaManagerView fmgr, AnalysisDirection direction, String uniqueVariableId) {
    if (!getAbstractStateClass().isAssignableFrom(state.getClass())) {
      throw new AssertionError("Cannot transform member of class " + state.getClass() + " in a "
          + getAbstractStateClass() + " transformer");
    }
    return transform(getAbstractStateClass().cast(state), fmgr, direction, uniqueVariableId);
  }

  public final BooleanFormula uninstantiate(FormulaManagerView pFmgr, BooleanFormula pFormula, SSAMap pSSAMap, AnalysisDirection pDirection) {
    executionCounter++;
    Map<String, Formula> variableToFormula = pFmgr.extractVariables(pFormula);
    Map<Formula, Formula> substitutions = new HashMap<>();
    SSAMapBuilder builder = SSAMap.emptySSAMap().builder();
    for (Entry<String, Formula> stringFormulaEntry : variableToFormula.entrySet()) {
      String name = stringFormulaEntry.getKey();
      Formula formula = stringFormulaEntry.getValue();

      List<String> nameAndIndex = Splitter.on("@").limit(2).splitToList(name);
      if (nameAndIndex.size() < 2 || nameAndIndex.get(1).isEmpty() || name.contains(".")) {
        substitutions.put(formula, pFmgr.makeVariable(pFmgr.getFormulaType(formula), name));
        continue;
      }
      name = nameAndIndex.get(0);
      int index = Integer.parseInt(nameAndIndex.get(1));
      int highestIndex = pSSAMap.getIndex(name);
      if (index != highestIndex) {
        substitutions.put(formula, pFmgr.makeVariable(pFmgr.getFormulaType(formula),
            name + "." + id + "E" + executionCounter + pDirection.name().charAt(0) + index));
      } else {
        substitutions.put(formula, pFmgr.makeVariable(pFmgr.getFormulaType(formula), name));
        builder = builder.setIndex(name, pSSAMap.getType(name), 1);
      }
    }
    return pFmgr.instantiate(pFmgr.uninstantiate(pFmgr.substitute(pFormula, substitutions)), builder.build());
  }

}
