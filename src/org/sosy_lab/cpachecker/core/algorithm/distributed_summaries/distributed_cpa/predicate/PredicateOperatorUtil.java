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
import java.util.OptionalInt;
import java.util.UUID;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;

public class PredicateOperatorUtil {

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
      if (isFreshPerUse(pCurrentId)) {
        return uniquePrefix + "." + index++ + "#" + pCurrentId;
      }
      return pCurrentId;
    }

    @Override
    public String toString() {
      return "UniqueIndexProvider{" + "uniquePrefix='" + uniquePrefix + '\'' + '}';
    }
  }

  /**
   * Whether a variable of a violation condition stands for a fresh value every time the condition
   * is used, e.g., the result of a nondeterministic call. {@link UniqueIndexProvider} renames such
   * a variable apart on every use, so it never refers to a variable of the block that uses the
   * condition.
   */
  static boolean isFreshPerUse(String pVariableName) {
    return pVariableName.contains("__VERIFIER_nondet") || pVariableName.contains("!");
  }

  /**
   * Whether a variable of the given path formula is local to it, i.e., existentially quantified
   * from the point of view of a block that uses the formula as violation condition: {@link
   * #uninstantiate(PathFormula, FormulaManagerView, UniqueIndexProvider)} keeps only variables at
   * their latest SSA index as the interface and renames all others apart.
   */
  static boolean isLocalVariable(
      String pVariableName, SSAMap pSsa, FormulaManagerView pFormulaManagerView) {
    return isFreshPerUse(pVariableName) || pFormulaManagerView.isIntermediate(pVariableName, pSsa);
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
   * Gives boundary values stable names for comparison only. Private variables keep distinct,
   * deterministic names, including their SSA versions. These names must never be used to compose
   * conditions: actual condition uses still require fresh independent witnesses.
   */
  static BooleanFormula normalizeForComparison(PathFormula path, FormulaManagerView fmgr) {
    Map<Formula, Formula> substitutions = new HashMap<>();
    for (Entry<String, Formula> entry : fmgr.extractVariables(path.getFormula()).entrySet()) {
      String name = entry.getKey();
      Formula variable = entry.getValue();
      Pair<String, OptionalInt> parsed = FormulaManagerView.parseName(name);
      if (isFreshPerUse(name)
          || (parsed.getSecond().isPresent()
              && parsed.getSecond().orElseThrow() != path.getSsa().getIndex(parsed.getFirst()))) {
        // Escape injectively, without leaving an SSA separator in the new private name.
        String privateName = "__dss_compare!" + name.replace("#", "##").replace("@", "#at");
        substitutions.put(variable, fmgr.makeVariable(fmgr.getFormulaType(variable), privateName));
      } else {
        substitutions.put(variable, fmgr.uninstantiate(variable));
      }
    }
    return fmgr.substitute(path.getFormula(), substitutions);
  }

  public static SubstitutedBooleanFormula uninstantiate(
      PathFormula pPathFormula, FormulaManagerView pFormulaManagerView) {
    return uninstantiate(pPathFormula, pFormulaManagerView, UniqueIndexProvider.withUUID());
  }

  /**
   * Uninstantiates a path formula by only keeping the variable with the highest SSA index. All
   * other variables receive fresh private names. This does not change the semantics of the formula
   * but allow the formula to be used as condition.
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
              .equals(stringFormulaEntry.getValue())
          || isFreshPerUse(stringFormulaEntry.getKey())) {
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
      if (nameAndIndex.size() < 2 || nameAndIndex.get(1).isEmpty()) {
        substitutions.put(
            formula,
            pFormulaManagerView.makeVariable(
                pFormulaManagerView.getFormulaType(formula), pUniqueIndexProvider.extend(name)));
        continue;
      }
      if (isFreshPerUse(name)) {
        // The private name must not keep the index, otherwise it still looks instantiated and the
        // block that uses the condition fails to instantiate it.
        substitutions.put(
            formula,
            pFormulaManagerView.makeVariable(
                pFormulaManagerView.getFormulaType(formula),
                pUniqueIndexProvider.extend(nameAndIndex.getFirst() + "!" + nameAndIndex.get(1))));
        continue;
      }
      name = nameAndIndex.getFirst();
      int index = Integer.parseInt(nameAndIndex.get(1));
      int highestIndex = ssaMap.getIndex(name);
      if (index != highestIndex) {
        // Mark intermediate values as private on this and every subsequent use. A stable
        // name such as x.1 would accidentally identify witnesses of independent conditions.
        String newName = pUniqueIndexProvider.extend(name + "!" + index);
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
