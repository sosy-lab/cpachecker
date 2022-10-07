// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import com.google.common.base.Splitter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;

public class PredicateOperatorUtil {

  public static String extractFormulaString(
      BlockSummaryMessage pMessage,
      Class<? extends ConfigurableProgramAnalysis> pKey,
      FormulaManagerView pFormulaManagerView) {
    return pMessage
        .getAbstractState(pKey)
        .map(Object::toString)
        .orElse(
            pFormulaManagerView
                .dumpFormula(pFormulaManagerView.getBooleanFormulaManager().makeTrue())
                .toString());
  }

  public static PathFormula getPathFormula(
      String formula,
      PathFormulaManager pPathFormulaManager,
      FormulaManagerView pFormulaManagerView,
      PointerTargetSet pPointerTargetSet,
      SSAMap pSSAMap) {
    if (formula.isEmpty()) {
      return pPathFormulaManager.makeEmptyPathFormula();
    }
    BooleanFormula parsed = pFormulaManagerView.parse(formula);
    return pPathFormulaManager
        .makeEmptyPathFormulaWithContext(pSSAMap, pPointerTargetSet)
        .withFormula(parsed);
  }

  public static PathFormula uninstantiate(
      PathFormula pPathFormula,
      FormulaManagerView pFormulaManagerView,
      PathFormulaManager pPathFormulaManager) {
    BooleanFormula booleanFormula = pPathFormula.getFormula();
    SSAMap ssaMap = pPathFormula.getSsa();
    Map<String, Formula> variableToFormula = pFormulaManagerView.extractVariables(booleanFormula);
    SSAMapBuilder builder = SSAMap.emptySSAMap().builder();
    Map<Formula, Formula> substitutions = new HashMap<>();
    for (Entry<String, Formula> stringFormulaEntry : variableToFormula.entrySet()) {
      String name = stringFormulaEntry.getKey();
      Formula formula = stringFormulaEntry.getValue();

      List<String> nameAndIndex = Splitter.on("@").limit(2).splitToList(name);
      if (nameAndIndex.size() < 2 || nameAndIndex.get(1).isEmpty() || name.contains(".")) {
        substitutions.put(
            formula,
            pFormulaManagerView.makeVariable(pFormulaManagerView.getFormulaType(formula), name));
        continue;
      }
      name = nameAndIndex.get(0);
      int index = Integer.parseInt(nameAndIndex.get(1));
      int highestIndex = ssaMap.getIndex(name);
      if (index != highestIndex) {
        substitutions.put(
            formula,
            pFormulaManagerView.makeVariable(
                pFormulaManagerView.getFormulaType(formula), name + "." + index));
      } else {
        substitutions.put(
            formula,
            pFormulaManagerView.makeVariable(pFormulaManagerView.getFormulaType(formula), name, 1));
        builder = builder.setIndex(name, ssaMap.getType(name), 1);
      }
    }
    SSAMap ssaMapFinal = builder.build();
    return pPathFormulaManager.makeAnd(
        pPathFormulaManager.makeEmptyPathFormulaWithContext(
            ssaMapFinal, PointerTargetSet.emptyPointerTargetSet()),
        pFormulaManagerView.uninstantiate(
            pFormulaManagerView.substitute(booleanFormula, substitutions)));
  }
}
