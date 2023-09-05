// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import com.google.common.base.Splitter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.block.BlockState.StrengtheningInfo;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;

public class PredicateOperatorUtil {

  private static final String INDEX_SEPARATOR = ".";
  private static final String FUNCTION_SEPARATOR = "::";
  private static final String RETURN_VAR_NAME = "__retval__";

  private PredicateOperatorUtil() {}

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

  /**
   * Uninstantiated a path formula by only keeping the variable with the highest SSA index. All
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
      int highestIndex = ssaMap.getIndex(name);
      if (index != highestIndex) {
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

  /**
   * Uninstantiated a path formula by removing all SSA indices. The lowest SSA index strips the
   * variable name such that x@1 -> x All other SSA indices are mutated such that x@n -> x.n This
   * does not change the semantics of the formula but allow the formula to be used as a condition.
   *
   * @param pPathFormula an arbitrary path formula
   * @param pFormulaManagerView the formula manager with the correct context
   * @return a boolean formula with no instantiated variables and an SSA map containing all
   *     variables mapped to index 1.
   */
  public static SubstitutedBooleanFormula uninstantiateInfer(
      PathFormula pPathFormula, FormulaManagerView pFormulaManagerView) {
    BooleanFormula booleanFormula = pPathFormula.getFormula();
    SSAMap ssaMap = pPathFormula.getSsa();
    Map<String, Formula> variableToFormula = pFormulaManagerView.extractVariables(booleanFormula);
    Map<Formula, Formula> substitutions = new HashMap<>();
    for (Entry<String, Formula> stringFormulaEntry : variableToFormula.entrySet()) {
      String name = stringFormulaEntry.getKey();
      Formula formula = stringFormulaEntry.getValue();

      List<String> nameAndIndex =
          Splitter.on(FormulaManagerView.INDEX_SEPARATOR).limit(2).splitToList(name);
      if (nameAndIndex.size() < 2
          || nameAndIndex.get(1).isEmpty()
          || name.contains(INDEX_SEPARATOR)
          || nameAndIndex.get(1).equals("1")) {
        substitutions.put(
            formula,
            pFormulaManagerView.makeVariable(pFormulaManagerView.getFormulaType(formula), name));
        continue;
      }
      name = nameAndIndex.get(0);
      int index = Integer.parseInt(nameAndIndex.get(1));
      substitutions.put(
          formula,
          pFormulaManagerView.makeVariable(
              pFormulaManagerView.getFormulaType(formula), name + INDEX_SEPARATOR + index));
    }
    return new SubstitutedBooleanFormula(
        pFormulaManagerView.uninstantiate(
            pFormulaManagerView.substitute(booleanFormula, substitutions)),
        ssaMap);
  }

  /* linkFormula is used by infer to link the variables created during strengthening back to
   * the most recent SSA index from an original formula
   * If no variables with INDEX_SEPARATOR (.) exist in the formula the formula remains unchanged
   */
  public static PathFormula linkedFormula(
      PathFormula pNewFormula,
      BooleanFormula pStrengtheningFormula,
      FormulaManagerView pFormulaManagerView,
      Set<StrengtheningInfo> pStrInfo) {

    Map<String, Formula> strengtheningFormulaMap =
        pFormulaManagerView.extractVariables(pStrengtheningFormula);

    Map<String, Formula> newFormulaMap =
        pFormulaManagerView.extractVariables(pNewFormula.getFormula());

    BooleanFormula formulaBuilder = pFormulaManagerView.getBooleanFormulaManager().makeTrue();

    for (StrengtheningInfo strengtheningInfo : pStrInfo) {

      // First we need to link the parameters
      Map<String, Formula> params = strengtheningInfo.params();
      for (Entry<String, Formula> entry : params.entrySet()) {
        if (strengtheningFormulaMap.containsKey(entry.getKey())) {
          Formula lhs = strengtheningFormulaMap.get(entry.getKey());
          Formula rhs = entry.getValue();
          BooleanFormula madeEqual = pFormulaManagerView.makeEqual(lhs, rhs);
          formulaBuilder = pFormulaManagerView.makeAnd(formulaBuilder, madeEqual);
        }
      }

      // Next link return values
      // The strengthening function might have uninstantiated rturn values
      // We need to link those to the instantiated return values in the new formula
      String varName =
          strengtheningInfo.strengtheningFunction() + FUNCTION_SEPARATOR + RETURN_VAR_NAME;
      Optional<Formula> maybeRhs = highestUninstantiatedIndex(strengtheningFormulaMap, varName);
      Optional<Formula> maybeLhs = declaredFormula(newFormulaMap, varName, pNewFormula.getSsa());
      if (maybeRhs.isPresent() && maybeLhs.isPresent()) {
        Formula rhs = maybeRhs.orElseThrow();
        Formula lhs = maybeLhs.orElseThrow();
        BooleanFormula madeEqual = pFormulaManagerView.makeEqual(lhs, rhs);
        formulaBuilder = pFormulaManagerView.makeAnd(formulaBuilder, madeEqual);
      }
    }

    // newFormula ^ builder
    BooleanFormula newBool = pNewFormula.getFormula();
    BooleanFormula linkedFormula = pFormulaManagerView.makeAnd(newBool, formulaBuilder);
    PathFormula finalFormula = pNewFormula.withFormula(linkedFormula);

    return finalFormula;
  }

  // utility method that searches for for the highest <idx> for a variable of the form
  // <varName>.<idx>
  // The lowest idx will be of the form <varName>, so if nothing is found then we return pVarName
  private static Optional<Formula> highestUninstantiatedIndex(
      Map<String, Formula> pVarsWithIndices, String pVarName) {
    // Iterate through all variables and find the highest index for the variable
    Splitter splitter = Splitter.on(INDEX_SEPARATOR).limit(2);
    Optional<String> maybeKey =
        pVarsWithIndices.keySet().stream()
            .filter(
                v -> {
                  List<String> nameAndIndex = splitter.splitToList(v);
                  return nameAndIndex.get(0).equals(pVarName);
                })
            .max(
                Comparator.comparingInt(
                    x -> {
                      List<String> nameAndIndex = splitter.splitToList(x);
                      if (nameAndIndex.size() < 2) {
                        return 0;
                      }
                      return Integer.parseInt(nameAndIndex.get(1));
                    }));
    if (maybeKey.isPresent()) {
      return Optional.of(pVarsWithIndices.get(maybeKey.orElseThrow()));
    } else {
      return Optional.empty();
    }
  }

  private static Optional<Formula> declaredFormula(
      Map<String, Formula> pVarsWithIndices, String pVarName, SSAMap pSSA) {
    if (!pSSA.containsVariable(pVarName)) {
      return Optional.empty();
    }
    int index = pSSA.getIndex(pVarName);
    String varWithIndex = pVarName + FormulaManagerView.INDEX_SEPARATOR + index;

    if (!pVarsWithIndices.containsKey(varWithIndex)) {
      return Optional.empty();
    }

    return Optional.of(pVarsWithIndices.get(varWithIndex));
  }

  public record SubstitutedBooleanFormula(BooleanFormula booleanFormula, SSAMap ssaMap) {}
}
