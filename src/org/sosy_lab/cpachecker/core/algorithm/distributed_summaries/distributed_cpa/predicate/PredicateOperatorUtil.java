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
import java.util.UUID;
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

    public static UniqueIndexProvider withUUID() {
      return new UniqueIndexProvider(UUID.randomUUID().toString());
    }

    public String extend(String pCurrentId) {
      if (pCurrentId.contains("__VERIFIER_nondet") || pCurrentId.contains("!")) {
        return uniquePrefix + "." + index++ + "#" + pCurrentId;
      }
      return pCurrentId;
    }

    @Override
    public String toString() {
      return "UniqueIndexProvider{" + "uniquePrefix='" + uniquePrefix + '\'' + '}';
    }
  }

  private static class IdIndexProvider extends UniqueIndexProvider {

    private static IdIndexProvider instance;

    private IdIndexProvider() {
      super("ID");
    }

    static IdIndexProvider getInstance() {
      if (instance == null) {
        instance = new IdIndexProvider();
      }
      return instance;
    }

    @Override
    public String extend(String pCurrentId) {
      return pCurrentId;
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

  public static SubstitutedBooleanFormula uninstantiate(
      PathFormula pPathFormula, FormulaManagerView pFormulaManagerView) {
    return uninstantiate(pPathFormula, pFormulaManagerView, IdIndexProvider.getInstance());
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
      PathFormula pPathFormula,
      FormulaManagerView pFormulaManagerView,
      UniqueIndexProvider pUniqueIndexProvider) {
    BooleanFormula booleanFormula = pPathFormula.getFormula();
    SSAMap ssaMap = pPathFormula.getSsa();
    Map<String, Formula> variableToFormula = pFormulaManagerView.extractVariables(booleanFormula);
    SSAMapBuilder builder = SSAMap.emptySSAMap().builder();
    Map<Formula, Formula> substitutions = new HashMap<>();

    boolean alreadyUninstantiated = true;
    for (Entry<String, Formula> stringFormulaEntry : variableToFormula.entrySet()) {
      if (!pFormulaManagerView
          .uninstantiate(stringFormulaEntry.getValue())
          .equals(stringFormulaEntry.getValue())) {
        alreadyUninstantiated = false;
        break;
      }
    }

    if (alreadyUninstantiated) {
      SSAMapBuilder mapBuilder = SSAMap.emptySSAMap().builder();
      for (String variable : ssaMap.allVariables()) {
        mapBuilder.setIndex(variable, ssaMap.getType(variable), 1);
      }
      return new SubstitutedBooleanFormula(booleanFormula, mapBuilder.build());
    }

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
            pFormulaManagerView.makeVariable(
                pFormulaManagerView.getFormulaType(formula), pUniqueIndexProvider.extend(name)));
        continue;
      }
      name = nameAndIndex.get(0);
      int index = Integer.parseInt(nameAndIndex.get(1));
      int highestIndex = ssaMap.getIndex(name);
      if (index != highestIndex) {
        String newName = name + INDEX_SEPARATOR + index;
        substitutions.put(
            formula,
            pFormulaManagerView.makeVariable(pFormulaManagerView.getFormulaType(formula), newName));
        builder.setIndex(newName, ssaMap.getType(name), 1);
      } else {
        substitutions.put(
            formula,
            pFormulaManagerView.makeVariable(pFormulaManagerView.getFormulaType(formula), name, 1));
        builder = builder.setIndex(name, ssaMap.getType(name), 1);
      }
    }
    SSAMap ssaMapFinal = builder.build();
    return new SubstitutedBooleanFormula(
        pFormulaManagerView.uninstantiate(
            pFormulaManagerView.substitute(booleanFormula, substitutions)),
        ssaMapFinal);
  }

  public record SubstitutedBooleanFormula(BooleanFormula booleanFormula, SSAMap ssaMap) {}
}
