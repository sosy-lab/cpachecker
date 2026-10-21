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
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Predicate;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
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
 * covering one, and T is inductive for the iterations that end in covered states. The further
 * iterations are thus overapproximated by s_exit = s_entry or T(s_entry, s_exit), where the
 * variables that the nested loop does not modify keep their values. If T is empty, the state covers
 * no other state, and the ARG path is exact.
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
      TerminationToReachState pState, Pair<LocationState, CallstackState> pLoopHead) {
    ImmutableList<LoopHeadVisit> visits = pState.getLoopHeadVisits();
    int last = visits.size() - 1;
    checkState(last >= 0 && visits.get(last).loopHead().equals(pLoopHead));
    int previous = last - 1;
    while (previous >= 0 && !visits.get(previous).loopHead().equals(pLoopHead)) {
      previous--;
    }
    checkState(previous >= 0, "The loop head %s was not visited before", pLoopHead);

    // Build the formula from the end, such that the later part can be shifted to the state after
    // the further iterations of a nested loop
    BooleanFormula suffix = visits.get(last).block().getFormula();
    for (int i = last - 1; i > previous; i--) {
      LoopHeadVisit visit = visits.get(i);
      // Summarize further iterations of a nested loop only at the last visit of its head before
      // the path leaves the nested loop, since the path contains the earlier iterations exactly.
      // Transition invariants exist only from the second visit of the head on the path. The
      // summary also allows zero further iterations, i.e., leaving the nested loop directly.
      if (!visit.transitionInvariants().isEmpty()
          && leavesLoopAfter(visits, i, last, pState.getPathSequence())) {
        suffix = summarizeFurtherIterations(visit, suffix);
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
    Set<CFANode> loopNodes = getLoopNodes(visit.loopHead().getFirst().getLocationNode());
    for (int i = pVisit + 1; i <= pLast; i++) {
      if (pVisits.get(i).loopHead().equals(visit.loopHead())) {
        return !loopNodes.containsAll(pPath.subList(visit.pathIndex(), pVisits.get(i).pathIndex()));
      }
    }
    return true;
  }

  private Set<CFANode> getLoopNodes(CFANode pLoopHead) {
    Set<CFANode> nodes = new HashSet<>();
    for (Loop loop : cfa.getLoopStructure().orElseThrow().getLoopsForLoopHead(pLoopHead)) {
      nodes.addAll(loop.getLoopNodes());
    }
    return nodes;
  }

  /**
   * Conjoins the given formula after the visit of a nested loop head with the overapproximation of
   * further iterations of the nested loop. The modified variables in the given formula are shifted
   * to the state after these iterations.
   */
  private BooleanFormula summarizeFurtherIterations(LoopHeadVisit pVisit, BooleanFormula pSuffix) {
    SSAMap entry = pVisit.block().getSsa();
    ImmutableSet<String> modifiedProgramVariables =
        getModifiedVariables(pVisit.loopHead().getFirst().getLocationNode());
    Predicate<String> modified = variable -> isModified(variable, modifiedProgramVariables);

    // The occurrences of a modified variable x at or after its entry index refer to the state
    // after the further iterations, which gets the index entry(x) + 1
    BooleanFormula shifted =
        fmgr.renameFreeVariablesAndUFs(
            pSuffix,
            name -> {
              Pair<String, OptionalInt> parsed = FormulaManagerView.parseName(name);
              String variable = parsed.getFirst();
              OptionalInt index = parsed.getSecond();
              if (index.isPresent()
                  && modified.test(variable)
                  && index.orElseThrow() >= getIndex(entry, variable)) {
                return variable + FormulaManagerView.INDEX_SEPARATOR + (index.orElseThrow() + 1);
              }
              return name;
            });

    // No further iterations: s_exit = s_entry
    Map<String, FormulaType<?>> types = getTypes(bfmgr.and(pVisit.block().getFormula(), shifted));
    List<BooleanFormula> identity = new ArrayList<>();
    for (Map.Entry<String, FormulaType<?>> variable : types.entrySet()) {
      if (modified.test(variable.getKey())) {
        int index = getIndex(entry, variable.getKey());
        identity.add(
            fmgr.assignment(
                fmgr.makeVariable(variable.getValue(), variable.getKey(), index + 1),
                fmgr.makeVariable(variable.getValue(), variable.getKey(), index)));
      }
    }

    // Further iterations: T(s_entry, s_exit)
    BooleanFormula transitionInvariant =
        bfmgr.and(
            pVisit.transitionInvariants().stream()
                .map(PartitionedRelationFormula::getFormula)
                .collect(ImmutableList.toImmutableList()));
    String freshSuffix = SUMMARY_SUFFIX + summaryCounter++;
    BooleanFormula instantiated =
        fmgr.renameFreeVariablesAndUFs(
            transitionInvariant,
            name -> {
              if (name.contains(PREV_KEYWORD)) {
                String variable = name.replace(PREV_KEYWORD, "");
                return variable + FormulaManagerView.INDEX_SEPARATOR + getIndex(entry, variable);
              }
              if (name.contains(CURR_KEYWORD)) {
                String variable = name.replace(CURR_KEYWORD, "");
                int index = getIndex(entry, variable) + (modified.test(variable) ? 1 : 0);
                return variable + FormulaManagerView.INDEX_SEPARATOR + index;
              }
              // Other variables are existentially quantified for each use of the summary
              Pair<String, OptionalInt> parsed = FormulaManagerView.parseName(name);
              String fresh = parsed.getFirst() + freshSuffix;
              return parsed.getSecond().isPresent()
                  ? fresh + FormulaManagerView.INDEX_SEPARATOR + parsed.getSecond().orElseThrow()
                  : fresh;
            });
    if (fmgr.extractVariableNames(instantiated).stream()
        .anyMatch(name -> name.contains(TRANS_INV_KEYWORD))) {
      // Unexpected variables of the transition invariant, so only the frame is kept
      instantiated = bfmgr.makeTrue();
    }

    return bfmgr.and(bfmgr.or(bfmgr.and(identity), instantiated), shifted);
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
  private ImmutableSet<String> getModifiedVariables(CFANode pLoopHead) {
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
