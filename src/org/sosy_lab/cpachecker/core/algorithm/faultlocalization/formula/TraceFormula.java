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

import com.google.common.base.Splitter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverException;

public class TraceFormula {

  private List<CFAEdge> edges;
  private List<Selector> selectors;
  private List<BooleanFormula> atoms;
  private List<BooleanFormula> negated;
  private List<SSAMap> ssaMaps;
  private FormulaContext context;

  private List<String> preconditionSymbols;

  // all available formulas
  private BooleanFormula implicationForm;
  private BooleanFormula actualForm;
  private BooleanFormula postcondition;
  private BooleanFormula precondition;

  /**
   * If post- and pre-condition are UNSAT set this flag to true.
   */
  private boolean isAlwaysUnsat;

  private TraceFormulaOptions options;

  @Options(prefix="traceformula")
  public static class TraceFormulaOptions {
    @Option(secure=true, name="filter",
        description="filter the alternative precondition by scopes")
    private String filter = "";

    @Option(
        secure = true,
        name = "altpre",
        description = "force alternative pre condition")
    private boolean forceAlternativePreCondition = false;

    public TraceFormulaOptions(Configuration pConfiguration) throws InvalidConfigurationException {
      pConfiguration.inject(this);
    }
  }

  public TraceFormula(FormulaContext pContext, TraceFormulaOptions pOptions, List<CFAEdge> pEdges)
      throws CPATransferException, InterruptedException, SolverException {
    isAlwaysUnsat = false;
    options = pOptions;
    edges = pEdges;
    context = pContext;
    selectors = new ArrayList<>();
    atoms = new ArrayList<>();
    ssaMaps = new ArrayList<>();
    preconditionSymbols = new ArrayList<>();
    createTraceFormulas();
  }

  public boolean isAlwaysUnsat() {
    return isAlwaysUnsat;
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

  //TODO prover precondition does not guarantee that corresponding edge is excluded in the report as possible resource.
  //TODO altpre cannot be used if undet_X() are in the formula
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

    /* Search last AssumeEdge to make it to the post condition.
     Imagine the program:
     int x = 0;
     if (x == 0) {
        x = 1;
       goto ERROR;
     }
     This would lead to the wrong post condition x != 1. (x != 0 is correct)

    Note that the following program cannot be analyzed because the post condition is x <= 0 but it should equal to x!=0.
    int x = 0;
    if (x == 0) {
       x = 1;
       while(x > 0) x--; <-- last AssumeEdge but that edge is not important.
       goto ERROR;
    } */

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
    AlternativePrecondition altPre = new AlternativePrecondition();
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
        altPre.add(currentAtom, current.getSsa(), e);
        selectors.add(Selector.makeSelector(context, currentAtom, e));
      }
    }
    // Create post-condition
    postcondition = bmgr.not(bmgr.and(negate));

    // Create pre-condition
    precondition = calculatePreCondition(negate, altPre);

    //If however the pre condition conjugated with the post-condition is UNSAT tell the main algorithm that this formula is always unsat.
    if(context.getSolver().isUnsat(bmgr.and(precondition, postcondition))){
      isAlwaysUnsat = true;
    }

    // No isPresent check needed, because the Selector always exists.
    BooleanFormula implicationFormula =
        atoms.stream()
            .map(a -> bmgr.implication(Selector.of(a).get().getFormula(), a))
            .collect(bmgr.toConjunction());

    // create traceformulas
    actualForm = bmgr.and(bmgr.and(atoms), postcondition);
    implicationForm = bmgr.and(implicationFormula, postcondition);
    negated = negate;
  }

  private BooleanFormula calculatePreCondition(List<BooleanFormula> negate, AlternativePrecondition altPre) throws SolverException, InterruptedException {
    // Create pre condition as model of the actual formula.
    // If the program is has a bug the model is guaranteed to be existent.
    BooleanFormulaManager bmgr = context.getSolver().getFormulaManager().getBooleanFormulaManager();
    List<BooleanFormula> preconditions = new ArrayList<>();
    BooleanFormula precond = bmgr.makeTrue();
    try (ProverEnvironment prover = context.getProver()) {
      prover.push(bmgr.and(bmgr.and(atoms), bmgr.and(negate)));
      if (!prover.isUnsat()) {
        for (ValueAssignment modelAssignment : prover.getModelAssignments()) {
          BooleanFormula formula = modelAssignment.getAssignmentAsFormula();
          if(formula.toString().contains("__VERIFIER_nondet")){
            preconditions.add(formula);
            precond = bmgr.and(precond, formula);
          }
        }
      } else {
        throw new AssertionError("a model has to be existent");
      }
    }

    //TODO: it is possible that a forced pre condition gets denied. Tell the user that this happened
    if((options.forceAlternativePreCondition || bmgr.isTrue(precond)) && !context.getSolver().isUnsat(bmgr.and(altPre.toFormula(), postcondition))){
      for (BooleanFormula booleanFormula : altPre.getPreCondition()) {
        int index = atoms.indexOf(booleanFormula);
        atoms.remove(index);
        selectors.remove(index);
        ssaMaps.remove(index);
      }
      return altPre.toFormula();
    }

    for (BooleanFormula booleanFormula : preconditions) {
      String nondetString ="";
      for(String symbol: Splitter.on(" ").split(booleanFormula.toString())){
        if (symbol.contains("__VERIFIER_nondet")){
          nondetString = symbol;
          preconditionSymbols.add(nondetString);
          break;
        }
      }
      for (int i = 0; i < atoms.size(); i++) {
        if(atoms.get(i).toString().contains(nondetString)){
          selectors.get(i).enable();
        }
      }
    }

    return precond;
  }

  public List<String> getPreconditionSymbols() {
    return preconditionSymbols;
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

  @Override
  public String toString() {
    return actualForm.toString();
  }

  private class AlternativePrecondition{

    private Map<Formula, Integer> variableToIndexMap;
    private List<BooleanFormula> preCondition;

    AlternativePrecondition(){
      variableToIndexMap = new HashMap<>();
      preCondition = new ArrayList<>();
    }

    void add(BooleanFormula formula, SSAMap currentMap, CFAEdge edge){
      //First check if variable is in desired scope, then check if formula is accepted.
      if(formula.toString().contains(options.filter + "::") && isAccepted(formula, currentMap, edge)){
        preCondition.add(formula);
      }
    }

    BooleanFormula toFormula(){
      return context.getSolver().getFormulaManager().getBooleanFormulaManager().and(preCondition);
    }

    public List<BooleanFormula> getPreCondition() {
      return preCondition;
    }

    /**
     * Accept all edges that contain statements where all operands have their minimal SSAIndex or are constants.
     * @param formula Check if this formula is accepted
     * @param currentMap The SSAMap for formula
     * @param pEdge The edge that can be converted to formula
     * @return is the formula accepted for the alternative precondition
     */
    private boolean isAccepted(BooleanFormula formula, SSAMap currentMap, CFAEdge pEdge){
      if(!pEdge.getEdgeType().equals(CFAEdgeType.DeclarationEdge)){
        return false;
      }
      Map<String, Formula> variables = context.getSolver().getFormulaManager().extractVariables(formula);
      //only accept declarations like int a = 2; and not int b = a + 2;
      //int a[] = {3,4,5} will be accepted too (thats why we filter __ADDRESS_OF_
      if(variables.entrySet().stream().filter(v -> !v.getKey().contains("__ADDRESS_OF_")).count() != 1){
        return false;
      }
      Map<Formula, Integer> index = new HashMap<>();
      for (String s : variables.keySet()) {
        Formula uninstantiated = context.getSolver().getFormulaManager().uninstantiate(variables.get(s));
        index.put(uninstantiated, currentMap.builder().getIndex(uninstantiated.toString()));
      }
      boolean isAccepted = true;
      for (Formula f : index.keySet()) {
        if (variableToIndexMap.get(f) == null) {
          variableToIndexMap.put(f, index.get(f));
        } else {
          int firstIndex = variableToIndexMap.get(f);
          if (firstIndex != index.get(f)) {
            isAccepted = false;
          }
        }
      }
      return isAccepted;
    }
  }
}
