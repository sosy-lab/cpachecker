// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.sosy_lab.common.collect.MapsDifference;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.FormulaEntryList.FormulaEntry;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.Formula;

/**
 * The Alternative Precondition extracts edges of the form <code>datatype name = value;</code> * and
 * adds it to the precondition. Additionally all entries that will be part of the
 * AlternativePrecondition are removed from the entry set.
 */
public class AlternativePrecondition {

  /**
   * Find the alternative pre-condition for a given formula
   *
   * @param pFilter functions which will be part of the precondition
   * @param pIgnore variables that will not be part of the precondition
   * @param pDefaultPrecondition model of the trace formula where the post condition is negated
   * @param pFormulaContext the context
   * @param pEntries all available entries based on the counterexample
   * @return conjunct of the alternative precondition with the default precondition
   */
  public static BooleanFormula of(
      String pFilter,
      String pIgnore,
      BooleanFormula pDefaultPrecondition,
      FormulaContext pFormulaContext,
      FormulaEntryList pEntries) {
    AlternativePreconditionHelper altpre =
        new AlternativePreconditionHelper(pFormulaContext, pIgnore, pFilter);
    pEntries.removeIf(entry -> altpre.add(entry));
    pEntries.addEntry(0, -1, altpre.preConditionMap, null, null);
    BooleanFormulaManager bmgr =
        pFormulaContext.getSolver().getFormulaManager().getBooleanFormulaManager();
    return bmgr.and(altpre.toFormula(), pDefaultPrecondition);
  }

  static class AlternativePreconditionHelper {

    private Map<Formula, Integer> variableToIndexMap;
    private List<BooleanFormula> preCondition;
    private List<String> ignore;
    private List<String> filter;
    private SSAMap preConditionMap;
    private FormulaContext context;

    AlternativePreconditionHelper(FormulaContext pContext, String pIngnore, String pFilter) {
      context = pContext;
      variableToIndexMap = new HashMap<>();
      preCondition = new ArrayList<>();
      preConditionMap = SSAMap.emptySSAMap();
      if (pIngnore.isBlank()) {
        ignore = ImmutableList.of();
      } else {
        ignore = Splitter.on(",").splitToList(pIngnore);
      }
      if (pFilter.isBlank()) {
        filter = ImmutableList.of();
      } else {
        filter = Splitter.on(",").splitToList(pFilter);
      }
    }

    boolean add(FormulaEntry entry) {
      BooleanFormula formula = entry.getAtom();
      SSAMap currentMap = entry.getMap();
      CFAEdge edge = entry.getSelector().getEdge();

      FormulaManagerView fmgr = context.getSolver().getFormulaManager();
      Map<String, Formula> formulaVariables = fmgr.extractVariables(formula);
      Set<String> uninstantiatedVariables =
          fmgr.extractVariables(fmgr.uninstantiate(formula)).keySet();
      SSAMap toMerge = currentMap;
      // check if formula is accepted.
      if (isAccepted(formula, currentMap, edge, formulaVariables)) {
        // remove all other elements from SSAMap if formula is a declaration not using other
        // variables (e.g. int a = 5; int[] d = {1,2,3})
        for (String variable : toMerge.allVariables()) {
          if (!uninstantiatedVariables.contains(variable)) {
            toMerge = toMerge.builder().deleteVariable(variable).build();
          }
        }
        // merge the maps to obtain a SSAMap that represents the inital state (pre-condition)
        preConditionMap =
            SSAMap.merge(
                preConditionMap,
                toMerge,
                MapsDifference.collectMapsDifferenceTo(new ArrayList<>()));
        preCondition.add(formula);
        return true;
      }
      return false;
    }

    BooleanFormula toFormula() {
      return context.getSolver().getFormulaManager().getBooleanFormulaManager().and(preCondition);
    }

    /**
     * Accept all edges that contain statements where all operands have their minimal SSAIndex or
     * are constants.
     *
     * @param formula Check if this formula is accepted
     * @param currentMap The SSAMap for formula
     * @param pEdge The edge that can be converted to formula
     * @return is the formula accepted for the alternative precondition
     */
    private boolean isAccepted(
        BooleanFormula formula, SSAMap currentMap, CFAEdge pEdge, Map<String, Formula> variables) {
      if (!pEdge.getEdgeType().equals(CFAEdgeType.DeclarationEdge)) {
        return false;
      }

      // check if variable is ignored
      for (String ign : ignore) {
        if (ign.contains("::")) {
          if (formula.toString().contains(ign + "@")) {
            return false;
          }
        } else {
          if (formula.toString().contains("::" + ign + "@")) {
            return false;
          }
        }
      }

      // check if variable is in desired scope
      boolean filtered = false;
      for (String f : filter) {
        if (formula.toString().contains(f + "::")) {
          filtered = true;
          break;
        }
      }
      if (!filtered) {
        return false;
      }

      // only accept declarations like int a = 2; and not int b = a + 2;
      // int a[] = {3,4,5} will be accepted too (that's why we filter __ADDRESS_OF_)
      if (variables.entrySet().stream().filter(v -> !v.getKey().contains("__ADDRESS_OF_")).count()
          != 1) {
        return false;
      }
      Map<Formula, Integer> index = new HashMap<>();
      for (Formula value : variables.values()) {
        Formula uninstantiated = context.getSolver().getFormulaManager().uninstantiate(value);
        index.put(uninstantiated, currentMap.getIndex(uninstantiated.toString()));
      }
      for (Entry<Formula, Integer> entry : index.entrySet()) {
        if (!variableToIndexMap.containsKey(entry.getKey())) {
          variableToIndexMap.put(entry.getKey(), entry.getValue());
        } else {
          return false;
        }
      }
      return true;
    }
  }
}
