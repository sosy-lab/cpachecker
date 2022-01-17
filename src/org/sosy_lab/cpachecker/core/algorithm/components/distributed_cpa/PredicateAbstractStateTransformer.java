// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.distributed_cpa;

import com.google.common.base.Splitter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.OptionalInt;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Payload;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;

class PredicateAbstractStateTransformer {

  private int executionCounter;
  private final AnalysisDirection direction;
  private final String workerId;
  private final SSAMap typeMap;

  public PredicateAbstractStateTransformer(String pWorkerId, AnalysisDirection pDirection, SSAMap pTypeMap) {
    direction = pDirection;
    workerId = pWorkerId;
    typeMap = pTypeMap;
  }

  public Payload encode(Collection<PredicateAbstractState> statesAtBlockEntry, FormulaManagerView fmgr) {
    if (statesAtBlockEntry.isEmpty()) {
      return Payload.builder().addEntry(PredicateCPA.class.getName(), fmgr.dumpFormula(fmgr.getBooleanFormulaManager().makeTrue()).toString()).build();
    }
    BooleanFormula formula = statesAtBlockEntry.stream().map(p -> {
      PathFormula currFormula = p.getPathFormula();
      return uninstantiate(fmgr, currFormula, direction);
    }).collect(fmgr.getBooleanFormulaManager().toDisjunction());
    return Payload.builder().addEntry(PredicateCPA.class.getName(), fmgr.dumpFormula(formula).toString()).build();
  }

  public PredicateAbstractState decode(Collection<String> messages, PredicateAbstractState previousState, PathFormulaManager manager, FormulaManagerView fmgr) {
    return PredicateAbstractState.mkNonAbstractionStateWithNewPathFormula(getBooleanFormula(messages, manager, fmgr), previousState);
  }



  private PathFormula getBooleanFormula(Collection<String> pUpdates, PathFormulaManager manager, FormulaManagerView fmgr) {
    if (pUpdates.isEmpty()) {
      return manager.makeEmptyPathFormula();
    }
    BooleanFormula disjunction = fmgr.getBooleanFormulaManager().makeFalse();
    SSAMapBuilder mapBuilder = SSAMap.emptySSAMap().builder();
    for (String message : pUpdates) {
      BooleanFormula parsed = parse(message, mapBuilder, fmgr);
      disjunction = fmgr.getBooleanFormulaManager().or(disjunction, parsed);
    }
    disjunction = fmgr.uninstantiate(disjunction);
    return manager.makeAnd(manager.makeEmptyPathFormulaWithContext(mapBuilder.build(),
        PointerTargetSet.emptyPointerTargetSet()), disjunction);
  }

  /**
   * pBuilder will be modified
   * @param formula formula to be parsed
   * @param pBuilder SSAMapBuilder storing information about the returned formula
   * @param fmgr the FormulaManager that is responsible for converting the formula string
   * @return a boolean formula representing the string formula
   */
  private BooleanFormula parse(String formula, SSAMapBuilder pBuilder, FormulaManagerView fmgr) {
    BooleanFormula parsed = fmgr.parse(formula);
    for (String variable : fmgr.extractVariables(parsed).keySet()) {
      Pair<String, OptionalInt> variableIndexPair = FormulaManagerView.parseName(variable);
      if (!variable.contains(".") && variableIndexPair.getSecond().isPresent()) {
        String variableName = variableIndexPair.getFirst();
        if (variableName != null) {
          pBuilder.setIndex(variableName, typeMap.getType(variableName),
              variableIndexPair.getSecond()
                  .orElse(1));
        }
      }
    }
    return parsed;
  }

  private BooleanFormula uninstantiate(FormulaManagerView pFmgr, PathFormula pPathFormula, AnalysisDirection pDirection) {
    executionCounter++;
    BooleanFormula booleanFormula = pPathFormula.getFormula();
    SSAMap ssaMap = pPathFormula.getSsa();
    Map<String, Formula> variableToFormula = pFmgr.extractVariables(booleanFormula);
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
      int highestIndex = ssaMap.getIndex(name);
      if (index != highestIndex) {
        substitutions.put(formula, pFmgr.makeVariable(pFmgr.getFormulaType(formula),
            name + "." + workerId + "E" + executionCounter + pDirection.name().charAt(0) + index));
      } else {
        substitutions.put(formula, pFmgr.makeVariable(pFmgr.getFormulaType(formula), name));
        builder = builder.setIndex(name, ssaMap.getType(name), 1);
      }
    }
    return pFmgr.instantiate(pFmgr.uninstantiate(pFmgr.substitute(booleanFormula, substitutions)), builder.build());
  }

}
