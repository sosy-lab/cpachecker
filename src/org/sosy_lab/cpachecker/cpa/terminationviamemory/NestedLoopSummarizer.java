// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.terminationviamemory;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.core.algorithm.termination.validation.well_foundedness.TransitionInvariantUtils.CURR_KEYWORD;
import static org.sosy_lab.cpachecker.core.algorithm.termination.validation.well_foundedness.TransitionInvariantUtils.PREV_KEYWORD;
import static org.sosy_lab.cpachecker.core.algorithm.termination.validation.well_foundedness.TransitionInvariantUtils.TRANS_INV_KEYWORD;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cpa.terminationviamemory.TerminationToReachState.LoopHeadVisit;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;

/**
 * Builds the formula of the last iteration of a loop in which the nested loops are overapproximated
 * for any number of their iterations.
 *
 * <p>A concrete execution of an iteration follows the ARG path of the iteration, except that it may
 * do more iterations of a nested loop where the ARG path leaves the nested loop: there, the
 * execution may go back to the abstract state of the last visit of the nested loop head, because it
 * covers further visits. A covered state has the same, non-empty transition invariants T as the
 * covering one, and T is inductive for the iterations that end in covered states. One or more
 * further iterations are thus overapproximated by T(s_entry, s_exit), where the variables that the
 * nested loop does not modify keep their values. Zero further iterations are the concrete iteration
 * of the ARG path. If T is empty, the state covers no other state, and the ARG path is exact.
 */
class NestedLoopSummarizer {

  private static final String SUMMARY_SUFFIX = "__summary_";

  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final CFA cfa;

  /** The qualified names of all program variables, unlike the variables of the formula encoding. */
  private final ImmutableSet<String> programVariables;

  private int summaryCounter = 0;

  public NestedLoopSummarizer(
      FormulaManagerView pFormulaManager, BooleanFormulaManagerView pBfmgr, CFA pCfa) {
    fmgr = pFormulaManager;
    bfmgr = pBfmgr;
    cfa = pCfa;
    programVariables = collectProgramVariables(pCfa);
  }

  /**
   * Returns the formula of the last iteration of the loop head of the given state, i.e., since the
   * previous visit of the same loop head, with the nested loops overapproximated.
   */
  public BooleanFormula summarizeLastIteration(
      TerminationToReachState pState, ProgramCounter pLoopHead) {
    ImmutableList<LoopHeadVisit> visits = pState.getLoopHeadVisits();
    int last = visits.size() - 1;
    checkState(last >= 0 && visits.get(last).loopHead().equals(pLoopHead));
    int previous = last - 1;
    while (previous >= 0 && !visits.get(previous).loopHead().equals(pLoopHead)) {
      previous--;
    }
    checkState(previous >= 0, "The loop head %s was not visited before", pLoopHead);

    // Build the formula from the end, such that the entry state of a nested loop in the later part
    // can be replaced by the exit state after its further iterations
    SSAMap end = visits.get(last).block().getSsa();
    Set<String> endsInSummary = new HashSet<>();
    BooleanFormula suffix = visits.get(last).block().getFormula();
    for (int i = last - 1; i > previous; i--) {
      LoopHeadVisit visit = visits.get(i);
      // Summarize further iterations of a nested loop only at the last visit of its head before
      // the path leaves the nested loop, since the path contains the earlier iterations exactly.
      // Transition invariants exist only from the second visit of the head on the path.
      if (!visit.transitionInvariants().isEmpty()
          && leavesLoopAfter(visits, i, last, pState.getPathSequence())) {
        suffix = summarizeFurtherIterations(visit, suffix, end, endsInSummary);
      }
      suffix = bfmgr.and(visit.block().getFormula(), suffix);
    }
    return suffix;
  }

  /**
   * Checks whether the path does not return to the head of the given visit inside its loop before
   * the iteration ends. Either the path leaves that loop, or the head is another head of the
   * iterated loop and the path reaches the iterated head first.
   */
  private boolean leavesLoopAfter(
      ImmutableList<LoopHeadVisit> pVisits, int pVisit, int pLast, ImmutableList<CFANode> pPath) {
    LoopHeadVisit visit = pVisits.get(pVisit);
    Set<CFANode> loopNodes =
        from(cfa.getLoopStructure()
                .orElseThrow()
                .getLoopsForLoopHead(visit.loopHead().location().getLocationNode()))
            .transformAndConcat(Loop::getLoopNodes)
            .toSet();
    for (int i = pVisit + 1; i <= pLast; i++) {
      if (pVisits.get(i).loopHead().equals(visit.loopHead())) {
        return !loopNodes.containsAll(pPath.subList(visit.pathIndex(), pVisits.get(i).pathIndex()));
      }
    }
    return true;
  }

  /**
   * Conjoins the given formula after the visit of a nested loop head with the overapproximation of
   * one or more further iterations of the nested loop. In the given formula, the entry state of the
   * modified variables is replaced by their exit state after these iterations.
   *
   * @param pEnd the SSA map at the end of the iteration
   * @param pEndsInSummary the variables whose end state in the iteration is the exit state of a
   *     later summary, extended by this summary
   */
  private BooleanFormula summarizeFurtherIterations(
      LoopHeadVisit pVisit, BooleanFormula pSuffix, SSAMap pEnd, Set<String> pEndsInSummary) {
    SSAMap entry = pVisit.block().getSsa();
    ImmutableSet<String> modifiedProgramVariables =
        getModifiedProgramVariables(pVisit.loopHead().location().getLocationNode());
    BooleanFormula transitionInvariant =
        bfmgr.and(
            pVisit.transitionInvariants().stream()
                .map(PartitionedRelationFormula::getFormula)
                .collect(ImmutableList.toImmutableList()));
    String summary = SUMMARY_SUFFIX + summaryCounter++;

    ImmutableMap<String, Formula> exits =
        makeExitVariables(
            getModifiedVariables(
                bfmgr.and(pVisit.block().getFormula(), pSuffix),
                transitionInvariant,
                modifiedProgramVariables),
            entry,
            pEnd,
            pEndsInSummary,
            summary);
    BooleanFormula furtherIterations =
        instantiateTransitionInvariant(transitionInvariant, exits, entry, summary);
    BooleanFormula suffix =
        replaceEntryByExit(pSuffix, exits, entry, modifiedProgramVariables, summary);
    return bfmgr.and(furtherIterations, suffix);
  }

  /**
   * Returns the types of the variables that the further iterations may modify and that occur in the
   * given formula or in the current state of the transition invariant.
   */
  private Map<String, FormulaType<?>> getModifiedVariables(
      BooleanFormula pFormula,
      BooleanFormula pTransitionInvariant,
      Set<String> pModifiedProgramVariables) {
    Map<String, FormulaType<?>> types = getTypes(pFormula);
    for (Map.Entry<String, Formula> variable :
        fmgr.extractVariables(pTransitionInvariant).entrySet()) {
      if (variable.getKey().contains(CURR_KEYWORD)) {
        types.putIfAbsent(
            variable.getKey().replace(CURR_KEYWORD, ""), fmgr.getFormulaType(variable.getValue()));
      }
    }
    types.keySet().removeIf(variable -> !isModified(variable, pModifiedProgramVariables));
    return types;
  }

  /**
   * Returns the exit variables of the given modified variables. The end state of a variable in the
   * iteration is its occurrence with the largest SSA index. So if the exit state is the end state,
   * i.e., the variable is not assigned later in the iteration, its exit variable gets an SSA index
   * above the others. Otherwise, it gets a fresh name without SSA index.
   */
  private ImmutableMap<String, Formula> makeExitVariables(
      Map<String, FormulaType<?>> pModifiedVariables,
      SSAMap pEntry,
      SSAMap pEnd,
      Set<String> pEndsInSummary,
      String pSummary) {
    ImmutableMap.Builder<String, Formula> exits = ImmutableMap.builder();
    pModifiedVariables.forEach(
        (variable, type) -> {
          int endIndex = getIndex(pEnd, variable);
          exits.put(
              variable,
              getIndex(pEntry, variable) == endIndex && pEndsInSummary.add(variable)
                  ? fmgr.makeVariable(type, variable, endIndex + 1)
                  : fmgr.makeVariable(type, getExitName(variable, pSummary)));
        });
    return exits.buildOrThrow();
  }

  /**
   * Returns T(s_entry, s_exit), where the other variables of T are existentially quantified for
   * each use of the summary.
   */
  private BooleanFormula instantiateTransitionInvariant(
      BooleanFormula pTransitionInvariant,
      Map<String, Formula> pExits,
      SSAMap pEntry,
      String pSummary) {
    BooleanFormula renamed =
        fmgr.renameFreeVariablesAndUFs(
            pTransitionInvariant,
            name -> name.contains(TRANS_INV_KEYWORD) ? name : getFreshName(name, pSummary));
    Map<Formula, Formula> states = new HashMap<>();
    for (Map.Entry<String, Formula> variable : fmgr.extractVariables(renamed).entrySet()) {
      FormulaType<?> type = fmgr.getFormulaType(variable.getValue());
      if (variable.getKey().contains(PREV_KEYWORD)) {
        String name = variable.getKey().replace(PREV_KEYWORD, "");
        states.put(variable.getValue(), makeEntryVariable(name, type, pEntry));
      } else if (variable.getKey().contains(CURR_KEYWORD)) {
        String name = variable.getKey().replace(CURR_KEYWORD, "");
        states.put(
            variable.getValue(),
            pExits.containsKey(name) ? pExits.get(name) : makeEntryVariable(name, type, pEntry));
      }
    }
    BooleanFormula instantiated = fmgr.substitute(renamed, states);
    if (fmgr.extractVariableNames(instantiated).stream()
        .anyMatch(name -> name.contains(TRANS_INV_KEYWORD))) {
      // Unexpected variables of the transition invariant, so only the frame is kept
      return bfmgr.makeTrue();
    }
    return instantiated;
  }

  /**
   * Replaces the entry state of the modified variables in the given formula by their exit state.
   */
  private BooleanFormula replaceEntryByExit(
      BooleanFormula pFormula,
      Map<String, Formula> pExits,
      SSAMap pEntry,
      Set<String> pModifiedProgramVariables,
      String pSummary) {
    Map<Formula, Formula> entryToExit = new HashMap<>();
    pExits.forEach(
        (variable, exit) ->
            entryToExit.put(makeEntryVariable(variable, fmgr.getFormulaType(exit), pEntry), exit));
    // The substitution covers only variables, so UFs, e.g., of a heap without SMT arrays, are
    // renamed
    return fmgr.renameFreeVariablesAndUFs(
        fmgr.substitute(pFormula, entryToExit),
        name -> {
          Pair<String, OptionalInt> parsed = FormulaManagerView.parseName(name);
          return parsed.getSecond().isPresent()
                  && isModified(parsed.getFirst(), pModifiedProgramVariables)
                  && parsed.getSecond().orElseThrow() == getIndex(pEntry, parsed.getFirst())
              ? getExitName(parsed.getFirst(), pSummary)
              : name;
        });
  }

  private Formula makeEntryVariable(String pVariable, FormulaType<?> pType, SSAMap pEntry) {
    return fmgr.makeVariable(pType, pVariable, getIndex(pEntry, pVariable));
  }

  private static String getExitName(String pVariable, String pSummary) {
    return pVariable + pSummary + "_exit";
  }

  /** Returns a name without SSA index that is unique for the given name and summary. */
  private static String getFreshName(String pName, String pSummary) {
    Pair<String, OptionalInt> parsed = FormulaManagerView.parseName(pName);
    return parsed.getSecond().isPresent()
        ? parsed.getFirst() + pSummary + "_" + parsed.getSecond().orElseThrow()
        : parsed.getFirst() + pSummary;
  }

  /** Variables that do not occur in the SSA map have the initial index 1. */
  private static int getIndex(SSAMap pSsa, String pVariable) {
    return pSsa.containsVariable(pVariable) ? pSsa.getIndex(pVariable) : 1;
  }

  /** Returns the types of the variables with an SSA index in the given formula. */
  private Map<String, FormulaType<?>> getTypes(BooleanFormula pFormula) {
    Map<String, FormulaType<?>> types = new HashMap<>();
    for (Map.Entry<String, Formula> variable : fmgr.extractVariables(pFormula).entrySet()) {
      Pair<String, OptionalInt> parsed = FormulaManagerView.parseName(variable.getKey());
      if (parsed.getSecond().isPresent()) {
        types.put(parsed.getFirst(), fmgr.getFormulaType(variable.getValue()));
      }
    }
    return types;
  }

  /**
   * Returns the qualified names of the variables that the loop with the given head may assign,
   * including in the functions that it calls.
   */
  private ImmutableSet<String> getModifiedProgramVariables(CFANode pLoopHead) {
    return from(cfa.getLoopStructure().orElseThrow().getLoopsForLoopHead(pLoopHead))
        .transformAndConcat(Loop::getModifiedVariables)
        .transform(CVariableDeclaration::getQualifiedName)
        .toSet();
  }

  /**
   * Checks whether a loop that assigns the given program variables may modify the given formula
   * variable. The heap and the variables of the formula encoding, e.g., for the results of memory
   * allocations, are always considered modified.
   */
  private boolean isModified(String pVariable, Set<String> pModifiedProgramVariables) {
    // The fields of structs that are not on the heap are encoded as variables "struct$field"
    int fieldSeparator = pVariable.indexOf('$');
    String variable = fieldSeparator < 0 ? pVariable : pVariable.substring(0, fieldSeparator);
    return !programVariables.contains(variable) || pModifiedProgramVariables.contains(variable);
  }

  private static ImmutableSet<String> collectProgramVariables(CFA pCfa) {
    ImmutableSet.Builder<String> variables = ImmutableSet.builder();
    for (CFAEdge edge : CFAUtils.allEdges(pCfa)) {
      if (edge instanceof CDeclarationEdge declarationEdge
          && declarationEdge.getDeclaration() instanceof CVariableDeclaration variable) {
        variables.add(variable.getQualifiedName());
      }
    }
    for (FunctionEntryNode function : pCfa.getAllFunctions().values()) {
      for (AParameterDeclaration parameter : function.getFunctionParameters()) {
        variables.add(parameter.getQualifiedName());
      }
      function
          .getReturnVariable()
          .ifPresent(variable -> variables.add(variable.getQualifiedName()));
    }
    return variables.build();
  }
}
