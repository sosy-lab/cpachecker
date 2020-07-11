// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.faultlocalization;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.misc.MultiMap;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.formula.FormulaContext;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.formula.TraceFormula;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultContribution;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * First implementation of a FaultFixingAlgorithm.
 * Unused at the moment.
 * Missing Feature: restart with modified input program
 */
public class FaultFixingAlgorithm {

  private MultiMap<FaultContribution, BooleanFormula> fix;
  private TraceFormula traceFormula;
  private FormulaContext context;
  private BooleanFormulaManager bmgr;
  private FormulaManagerView fmgr;
  private List<CFAEdge> edges;

  private FaultFixingAlgorithm(
      TraceFormula pTraceFormula, FormulaContext pContext, Map<FaultContribution, Integer> pRankedMap)
      throws SolverException, InterruptedException {
    List<FaultContribution> pRankedList = pRankedMap.keySet().stream().sorted(Comparator.comparingInt(l -> pRankedMap.get(l))).collect(
        Collectors.toList());
    fix = new MultiMap<>();
    traceFormula = pTraceFormula;
    context = pContext;
    fmgr = context.getSolver().getFormulaManager();
    bmgr = fmgr.getBooleanFormulaManager();
    edges = traceFormula.getEdges();
    for (FaultContribution current : pRankedList) {
        switch (current.correspondingEdge().getEdgeType()) {
          case AssumeEdge:
            fixAssumeEdge(current);
            continue;
          case DeclarationEdge:
          case StatementEdge:
            fixStatementEdge(current);
            continue;
          case FunctionCallEdge:
          case FunctionReturnEdge:
          case ReturnStatementEdge:
          case CallToReturnEdge:
          case BlankEdge:
          default: continue;
        }
    }
  }

  public MultiMap<FaultContribution, BooleanFormula> getFix() {
    return fix;
  }

  public static MultiMap<FaultContribution, BooleanFormula> fix(
      TraceFormula traceFormula, FormulaContext context, Map<FaultContribution, Integer> rankedMap)
      throws SolverException, InterruptedException {
    return new FaultFixingAlgorithm(traceFormula, context, rankedMap).getFix();
  }

  /**
   * Extract a variable.
   * Add and subtract 1.
   * Look at the TraceFormula.
   *
   * @param errorLoc possible error location
   */
  private void fixStatementEdge(FaultContribution errorLoc) throws SolverException, InterruptedException {
    assert errorLoc.correspondingEdge().getEdgeType().equals(CFAEdgeType.DeclarationEdge)
        || errorLoc.correspondingEdge().getEdgeType().equals(CFAEdgeType.StatementEdge);

    List<BooleanFormula> atoms = new ArrayList<>(traceFormula.getAtoms());
    CFAEdge edge = errorLoc.correspondingEdge();
    int index = edges.indexOf(edge);
    BooleanFormula formula = bmgr.and(atoms.get(index));
    BooleanFormula copy = bmgr.and(formula);

    List<Formula> formulas = new ArrayList<>(fmgr.extractVariables(formula).values());
    Formula single = formulas.get(0);
    Map<Formula, Formula> substitute = new HashMap<>();
    substitute.put(
        single,
        fmgr.makeMinus(single, fmgr.makeNumber(FormulaType.getBitvectorTypeWithSize(32), 1)));

    Map<Formula, Formula> substitute2 = new HashMap<>();
    substitute2.put(
        single,
        fmgr.makePlus(single, fmgr.makeNumber(FormulaType.getBitvectorTypeWithSize(32), 1)));

    formula = fmgr.substitute(formula, substitute);
    copy = fmgr.substitute(copy, substitute2);

    atoms.remove(index);
    atoms.add(index, formula);

    if (context
        .getSolver()
        .isUnsat(bmgr.and(bmgr.and(atoms), bmgr.and(traceFormula.getNegated())))) {
      fix.map(errorLoc, formula);

    }

    atoms = new ArrayList<>(traceFormula.getAtoms());
    atoms.remove(index);
    atoms.add(index, copy);

    if (context
        .getSolver()
        .isUnsat(bmgr.and(bmgr.and(atoms), bmgr.and(traceFormula.getNegated())))) {
      fix.map(errorLoc, copy);
    }
    // substitute.put(single, fmgr.makePlus(single, fmgr.makeNumber(FormulaType.IntegerType, 1)));

  }

  /**
   * Experimental... only works on: a [boolean_op] b with a and b variables and not numbers
   *
   * @param errorLoc the error location
   */
  private void fixAssumeEdge(FaultContribution errorLoc) throws SolverException, InterruptedException {
    assert errorLoc.correspondingEdge().getEdgeType().equals(CFAEdgeType.AssumeEdge);

    List<BooleanFormula> atoms = new ArrayList<>(traceFormula.getAtoms());
    CFAEdge edge = errorLoc.correspondingEdge();
    int index = edges.indexOf(edge);
    BooleanFormula formula = atoms.get(index);

    List<Formula> formulas = new ArrayList<>(fmgr.extractVariables(formula).values());
    formulas.sort(Comparator.comparingInt(l -> formula.toString().indexOf(l.toString())));
    Formula left = formulas.get(0);
    Formula right = formulas.get(1);

    List<BooleanFormula> toCheck = new ArrayList<>();
    toCheck.add(fmgr.makeLessOrEqual(left, right, true));
    toCheck.add(fmgr.makeLessThan(left, right, true));
    toCheck.add(fmgr.makeGreaterThan(left, right, true));
    toCheck.add(fmgr.makeGreaterOrEqual(left, right, true));
    toCheck.add(fmgr.makeEqual(left, right));

    for (BooleanFormula replace : toCheck) {
      atoms.remove(index);
      atoms.add(index, replace);
      if (context
          .getSolver()
          .isUnsat(bmgr.and(bmgr.and(atoms), bmgr.and(traceFormula.getNegated())))) {
        fix.map(errorLoc, replace);
      }
    }
  }
}
