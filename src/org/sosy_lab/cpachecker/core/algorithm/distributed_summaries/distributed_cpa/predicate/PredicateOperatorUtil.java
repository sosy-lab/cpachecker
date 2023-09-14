// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import java.util.Collection;
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

  public static final String INDEX_SEPARATOR = ".";

  private PredicateOperatorUtil() {}

  public static class UniqueIndexProvider {
    private long index;
    private final String uniquePrefix;

    public UniqueIndexProvider(String pUniquePrefix) {
      uniquePrefix = pUniquePrefix;
      index = 0;
    }

    public String extend(String pCurrentId) {
      return uniquePrefix + "." + index++ + "#" + pCurrentId;
    }

    @Override
    public String toString() {
      return "UniqueIndexProvider{" + "uniquePrefix='" + uniquePrefix + '\'' + '}';
    }
  }

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

  private static Map<String, Integer> findMinimumIndices(Collection<String> variablesWithIndex) {
    ImmutableMap.Builder<String, Integer> minIndex = ImmutableMap.builder();
    for (String name : variablesWithIndex) {
      List<String> nameAndIndex =
          Splitter.on(FormulaManagerView.INDEX_SEPARATOR).limit(2).splitToList(name);
      if (nameAndIndex.size() == 2 && !nameAndIndex.get(1).isEmpty()) {
        minIndex.put(nameAndIndex.get(0), 1);
      }
    }
    return minIndex.buildKeepingLast();
  }

  public static PathFormula makeAndByShiftingSecond(
      FormulaManagerView pFormulaManger,
      PathFormula pFormula,
      PathFormula pShift,
      UniqueIndexProvider pIndexProvider) {
    SSAMap formulaSsa = pFormula.getSsa();
    SSAMap shiftSSA = pShift.getSsa();
    ImmutableMap.Builder<Formula, Formula> substitutions = ImmutableMap.builder();
    Map<String, Formula> indicesToShift = pFormulaManger.extractVariables(pShift.getFormula());
    Map<String, Integer> minIndex = findMinimumIndices(indicesToShift.keySet());
    for (Entry<String, Formula> stringFormulaEntry : indicesToShift.entrySet()) {
      String name = stringFormulaEntry.getKey();
      Formula formula = stringFormulaEntry.getValue();
      List<String> nameAndIndex =
          Splitter.on(FormulaManagerView.INDEX_SEPARATOR).limit(2).splitToList(name);
      if (nameAndIndex.size() < 2 || nameAndIndex.get(1).isEmpty()) {
        substitutions.put(
            formula,
            pFormulaManger.makeVariable(
                pFormulaManger.getFormulaType(formula), pIndexProvider.extend(name)));
        continue;
      }
      name = nameAndIndex.get(0);
      int updatedIndex = Integer.parseInt(nameAndIndex.get(1));
      if (formulaSsa.containsVariable(name)) {
        updatedIndex += formulaSsa.getIndex(name) - minIndex.get(name);
      }
      substitutions.put(
          formula,
          pFormulaManger.makeVariable(
              pFormulaManger.getFormulaType(formula),
              name + FormulaManagerView.INDEX_SEPARATOR + updatedIndex));
    }
    SSAMapBuilder updatedSSAMap = SSAMap.emptySSAMap().builder();
    for (String variable : formulaSsa.allVariables()) {
      int offset = 0;
      if (shiftSSA.containsVariable(variable)) {
        offset = shiftSSA.getIndex(variable) - minIndex.getOrDefault(variable, 0);
      }
      updatedSSAMap.setIndex(
          variable, formulaSsa.getType(variable), formulaSsa.getIndex(variable) + offset);
    }
    for (String variable : shiftSSA.allVariables()) {
      if (!formulaSsa.containsVariable(variable)) {
        updatedSSAMap.setIndex(variable, shiftSSA.getType(variable), shiftSSA.getIndex(variable));
      }
    }
    BooleanFormula substituted =
        pFormulaManger.substitute(pShift.getFormula(), substitutions.buildOrThrow());
    BooleanFormula updated =
        pFormulaManger.getBooleanFormulaManager().and(pFormula.getFormula(), substituted);
    return pShift
        .withContext(updatedSSAMap.build(), pFormula.getPointerTargetSet())
        .withFormula(updated);
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

  /**
   * Uninstantiates a path formula by only keeping the variable with the highest SSA index. All
   * other variables are renamed to variable.index. This does not change the semantics of the
   * formula but allow the formula to be used as condition.
   *
   * @param pPathFormula an arbitrary path formula
   * @param pFormulaManagerView the formula manager with the correct context
   * @return a boolean formula with no instantiated variables and an SSA map containing all
   *     variables mapped to index 1.
   */
  public static SubstitutedBooleanFormula uninstantiate(
      PathFormula pPathFormula, FormulaManagerView pFormulaManagerView) {
    BooleanFormula booleanFormula = pPathFormula.getFormula();
    SSAMap ssaMap = pPathFormula.getSsa();
    Map<String, Formula> variableToFormula = pFormulaManagerView.extractVariables(booleanFormula);
    SSAMapBuilder builder = SSAMap.emptySSAMap().builder();
    Map<Formula, Formula> substitutions = new HashMap<>();
    Map<String, Integer> lowestIndices = findMinimumIndices(variableToFormula.keySet());
    for (Entry<String, Formula> stringFormulaEntry : variableToFormula.entrySet()) {
      String name = stringFormulaEntry.getKey();
      Formula formula = stringFormulaEntry.getValue();

      List<String> nameAndIndex =
          Splitter.on(FormulaManagerView.INDEX_SEPARATOR).limit(2).splitToList(name);
      if (nameAndIndex.size() < 2
          || nameAndIndex.get(1).isEmpty()
          || name.contains(INDEX_SEPARATOR)) {
        substitutions.put(
            formula,
            pFormulaManagerView.makeVariable(pFormulaManagerView.getFormulaType(formula), name));
        continue;
      }
      name = nameAndIndex.get(0);
      int index = Integer.parseInt(nameAndIndex.get(1));
      int minIndex = lowestIndices.getOrDefault(name, -1);
      if (index != minIndex) {
        substitutions.put(
            formula,
            pFormulaManagerView.makeVariable(
                pFormulaManagerView.getFormulaType(formula), name + INDEX_SEPARATOR + index));
      } else {
        substitutions.put(
            formula,
            pFormulaManagerView.makeVariable(pFormulaManagerView.getFormulaType(formula), name, 1));
        builder = builder.setIndex(name, ssaMap.getType(name), 1);
      }
    }
    SSAMap ssaMapFinal = builder.build();
    // TODO add support for PTS
    return new SubstitutedBooleanFormula(
        pFormulaManagerView.uninstantiate(
            pFormulaManagerView.substitute(booleanFormula, substitutions)),
        ssaMapFinal);
  }

  public static PathFormula uninstantiate(
      PathFormula pPathFormula,
      FormulaManagerView pFormulaManagerView,
      PathFormulaManager pPathFormulaManager) {
    SubstitutedBooleanFormula substituted = uninstantiate(pPathFormula, pFormulaManagerView);
    return pPathFormulaManager.makeAnd(
        pPathFormulaManager.makeEmptyPathFormulaWithContext(
            substituted.ssaMap(), PointerTargetSet.emptyPointerTargetSet()),
        substituted.booleanFormula());
  }

  public record SubstitutedBooleanFormula(BooleanFormula booleanFormula, SSAMap ssaMap) {}
}
