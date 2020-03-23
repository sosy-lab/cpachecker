/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.core.algorithm.faultlocalization.formula;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.Model;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverException;

public class TraceFormula {

  private List<CFAEdge> edges;
  private List<Selector> selectors;
  private List<BooleanFormula> atoms;
  private List<BooleanFormula> negated;
  private List<SSAMap> ssaMaps;
  private FormulaContext context;

  // all available formulas
  private BooleanFormula implicationForm;
  private BooleanFormula actualForm;
  private BooleanFormula postcondition;
  private BooleanFormula precondition;

  public TraceFormula(FormulaContext pContext, List<CFAEdge> pEdges)
      throws CPATransferException, InterruptedException, SolverException {
    edges = pEdges;
    context = pContext;
    selectors = new ArrayList<>();
    atoms = new ArrayList<>();
    ssaMaps = new ArrayList<>();
    createTraceFormulas();
  }

  public List<SSAMap> getSsaMaps() {
    return ssaMaps;
  }

  public BooleanFormula getPostCondition() {
    return postcondition;
  }

  public BooleanFormula getPreCondition() {
    return precondition;
  }

  public List<Selector> getSelectors() {
    return selectors;
  }

  public BooleanFormula getImplicationForm() {
    return implicationForm;
  }

  public BooleanFormula getActualForm() {
    return actualForm;
  }

  public List<BooleanFormula> getAtoms() {
    return atoms;
  }

  public List<CFAEdge> getEdges() {
    return edges;
  }

  public List<BooleanFormula> getNegated() {
    return negated;
  }

  private void createTraceFormulas()
      throws CPATransferException, InterruptedException, SolverException {

    PathFormulaManagerImpl manager = context.getManager();
    BooleanFormulaManager bmgr = context.getSolver().getFormulaManager().getBooleanFormulaManager();
    if (edges.isEmpty()) {
      actualForm = bmgr.makeFalse();
      implicationForm = bmgr.makeFalse();
      return;
    }

    ArrayList<BooleanFormula> negate = new ArrayList<>();

    // Search last AssumeEdge to make it to the post condition.
    // Imagine the program:
    // int x = 0;
    // if (x == 0) {
    //    x = 1;
    //    goto ERROR;
    // }
    // This would lead to the wrong post condition x != 1. (x != 0 is correct)

    /* Note that the following program cannot be analyzed because the post condition is x <= 0 but it should equal to x!=0.
    int x = 0;
    if (x == 0) {
       x = 1;
       while(x > 0) x--; <-- last AssumeEdge but that edge is not important, dependence graph?.
       goto ERROR;
    }

    */
    int errorStartingLine = -1;
    for (int i = edges.size() - 1; i >= 0; i--) {
      if (edges.get(i).getEdgeType().equals(CFAEdgeType.AssumeEdge)) {
        errorStartingLine = edges.get(i).getFileLocation().getStartingLineInOrigin();
        break;
      }
    }

    if (errorStartingLine == -1) {
      throw new AssertionError("No error condition found");
    }

    PathFormula current = manager.makeEmptyPathFormula();
    ssaMaps.add(current.getSsa());
    // edges.removeIf(l -> !l.getEdgeType().equals(CFAEdgeType.AssumeEdge));
    for (CFAEdge e : edges) {
      BooleanFormula prev = current.getFormula();
      current = manager.makeAnd(current, e);
      List<BooleanFormula> formulaList =
          new ArrayList<>(bmgr.toConjunctionArgs(current.getFormula(), false));
      BooleanFormula currentAtom = formulaList.get(0);
      if (formulaList.size() == 2) {
        if (formulaList.get(0).equals(prev)) {
          currentAtom = formulaList.get(1);
        }
      }
      if (e.getFileLocation().getStartingLineInOrigin() == errorStartingLine
          && Objects.equals(e.getEdgeType(), CFAEdgeType.AssumeEdge)) {
        negate.add(currentAtom);
      } else {
        ssaMaps.add(current.getSsa());
        atoms.add(currentAtom);
        selectors.add(Selector.makeSelector(context, currentAtom, e));
      }
    }

    // Create pre condition as model of the actual formula.
    // If the program is has a bug the model is guaranteed to be existent.
    ProverEnvironment prover = context.getProver();
    prover.push(bmgr.and(bmgr.and(atoms), bmgr.and(negate)));
    if (!prover.isUnsat()) {
      precondition =
          prover.getModelAssignments().stream()
              .map(Model.ValueAssignment::getAssignmentAsFormula)
              .filter(l -> l.toString().contains("__VERIFIER_nondet"))
              .collect(bmgr.toConjunction());
      // precondition = context.getSolver().getFormulaManager().uninstantiate(precondition);
    } else {
      throw new AssertionError("a model has to be existent");
    }

    // No isPresent check needed, because the Selector always exists.
    BooleanFormula implicationFormula =
        atoms.stream()
            .map(a -> bmgr.implication(Selector.of(a).get().getFormula(), a))
            .collect(bmgr.toConjunction());

    // create traceformulas
    postcondition = bmgr.not(bmgr.and(negate));
    actualForm = bmgr.and(bmgr.and(atoms), postcondition);
    implicationForm = bmgr.and(implicationFormula, postcondition);
    negated = negate;
  }

  public BooleanFormula getAtom(int i) {
    return atoms.get(i);
  }

  public int traceSize() {
    return atoms.size();
  }

  public BooleanFormula slice(int start, int end) {
    BooleanFormulaManager bmgr = context.getSolver().getFormulaManager().getBooleanFormulaManager();
    BooleanFormula formula = bmgr.makeTrue();
    for (int i = start; i < end; i++) {
      formula = bmgr.and(atoms.get(i), formula);
    }
    return formula;
  }

  public BooleanFormula slice(int end) {
    return slice(0, end);
  }

  public String toString() {
    return ExpressionConverter.convert(actualForm);
  }
}
