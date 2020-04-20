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
import com.google.common.collect.ImmutableList;
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

  private FormulaContext context;

  private List<CFAEdge> edges;
  private List<BooleanFormula> negated;
  private FormulaEntries entries;

  // all available formulas
  private BooleanFormula implicationForm;
  private BooleanFormula actualForm;

  private BooleanFormula postcondition;
  private BooleanFormula precondition;
  private List<String> preconditionSymbols;

  /**
   * If post- and pre-condition are UNSAT set this flag to true.
   */
  private boolean isAlwaysUnsat;

  private TraceFormulaOptions options;

  @Options(prefix="traceformula")
  public static class TraceFormulaOptions {
    @Option(secure=true, name="filter",
        description="filter the alternative precondition by scopes")
    private String filter = "main";

    //Usage: If a variable is contained in the post-condition it may be useful to ignore it in the pre-condition
    @Option(secure=true, name="ignore",
        description="do not add variables to alternative precondition (separate by commas)")
    private String ignore = "";

    //Usage: If a variable is contained in the post-condition it may be useful to ignore it in the pre-condition
    @Option(secure=true, name="ban",
        description="do not create selectors for this variables (separate by commas)")
    private String ban = "";

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
    entries = new FormulaEntries(context);
    negated = new ArrayList<>();
    preconditionSymbols = new ArrayList<>();
    createTraceFormulas();
  }

  public boolean isAlwaysUnsat() {
    return isAlwaysUnsat;
  }

  public BooleanFormula getPostCondition() {
    return postcondition;
  }

  public BooleanFormula getPreCondition() {
    return precondition;
  }

  public List<Selector> getSelectors() {
    return entries.selectors;
  }

  public BooleanFormula getImplicationForm() {
    return implicationForm;
  }

  public BooleanFormula getActualForm() {
    return actualForm;
  }

  public List<BooleanFormula> getAtoms() {
    return entries.atoms;
  }

  public List<CFAEdge> getEdges() {
    return edges;
  }

  public List<BooleanFormula> getNegated() {
    return negated;
  }

  public SSAMap getSsaMap(int i){
    return entries.getSsaMap(i);
  }

  public List<String> getPreconditionSymbols() {
    return preconditionSymbols;
  }

  public BooleanFormula getAtom(int i) {
    return entries.atoms.get(i);
  }

  public int traceSize() {
    return entries.size();
  }

  //TODO prover precondition does not guarantee that corresponding edge is excluded in the report as possible resource.
  //TODO altpre cannot be used if undet_X() are in the formula
  private void createTraceFormulas()
      throws CPATransferException, InterruptedException, SolverException {

    BooleanFormulaManager bmgr = context.getSolver().getFormulaManager().getBooleanFormulaManager();
    if (edges.isEmpty()) {
      actualForm = bmgr.makeFalse();
      implicationForm = bmgr.makeFalse();
      return;
    }

    AlternativePrecondition altPre = new AlternativePrecondition();
    calculateEntries(altPre);

    // Create post-condition
    postcondition = bmgr.not(bmgr.and(negated));

    // Create pre-condition
    precondition = calculatePreCondition(negated, altPre);

    //If however the pre condition conjugated with the post-condition is UNSAT tell the main algorithm that this formula is always unsat.
    if(context.getSolver().isUnsat(bmgr.and(precondition, postcondition))){
      isAlwaysUnsat = true;
    }

    // calculate formulas
    // No isPresent check needed, because the Selector always exists.
    BooleanFormula implicationFormula =
        entries.atoms.stream()
            .map(a -> bmgr.implication(Selector.of(a).get().getFormula(), a))
            .collect(bmgr.toConjunction());
    actualForm = bmgr.and(bmgr.and(entries.atoms), postcondition);
    implicationForm = bmgr.and(implicationFormula, postcondition);
  }

  private void calculateEntries(AlternativePrecondition altPre) throws CPATransferException, InterruptedException {
    PathFormulaManagerImpl manager = context.getManager();
    BooleanFormulaManager bmgr = context.getSolver().getFormulaManager().getBooleanFormulaManager();
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
        negated.add(currentAtom);
      } else {
        entries.addEntry(current.getSsa(), Selector.makeSelector(context, currentAtom, e), currentAtom);
        altPre.add(currentAtom, current.getSsa(), e);
      }
    }

    // enable banned selectors
    if (!options.ban.equals("")) {
      List<String> banned = Splitter.on(",").splitToList(options.ban);
      for (int i = 0; i < entries.size(); i++) {
        String formulaString = entries.atoms.get(i).toString();
        Selector selector = entries.selectors.get(i);
        for (String s : banned) {
          if (s.contains("::")) {
            if (formulaString.contains(s)) {
              selector.disable();
            }
          } else {
            if (formulaString.contains("::" + s)) {
              selector.disable();
            }
          }
        }
      }
    }
  }

  private BooleanFormula calculatePreCondition(List<BooleanFormula> negate, AlternativePrecondition altPre) throws SolverException, InterruptedException {
    // Create pre condition as model of the actual formula.
    // If the program is has a bug the model is guaranteed to be existent.
    BooleanFormulaManager bmgr = context.getSolver().getFormulaManager().getBooleanFormulaManager();
    List<BooleanFormula> preconditions = new ArrayList<>();
    BooleanFormula precond = bmgr.makeTrue();
    try (ProverEnvironment prover = context.getProver()) {
      prover.push(bmgr.and(bmgr.and(entries.atoms), bmgr.and(negate)));
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

    // Check if alternative precondition is required and remove corresponding entries.
    //TODO: it is possible that a forced pre condition gets denied. Tell the user that this happened
    if((options.forceAlternativePreCondition || bmgr.isTrue(precond)) && !context.getSolver().isUnsat(bmgr.and(altPre.toFormula(), postcondition))){
      for (BooleanFormula booleanFormula : altPre.getPreCondition()) {
        int index = entries.atoms.indexOf(booleanFormula);
        entries.remove(index);
      }
      return altPre.toFormula();
    }

    // If the calculated pre-condition has to be used filter only the nondet assignments
    for (BooleanFormula booleanFormula : preconditions) {
      String nondetString ="";
      for(String symbol: Splitter.on(" ").split(booleanFormula.toString())){
        if (symbol.contains("__VERIFIER_nondet")){
          nondetString = symbol;
          preconditionSymbols.add(nondetString);
          break;
        }
      }
      for (int i = 0; i < entries.size(); i++) {
        String formulaString = entries.atoms.get(i).toString();
        Selector selector = entries.selectors.get(i);
        if(formulaString.contains(nondetString)){
          selector.enable();
        }
      }
    }

    return precond;
  }

  public BooleanFormula slice(int start, int end) {
    BooleanFormulaManager bmgr = context.getSolver().getFormulaManager().getBooleanFormulaManager();
    BooleanFormula formula = bmgr.makeTrue();
    for (int i = start; i < end; i++) {
      formula = bmgr.and(entries.atoms.get(i), formula);
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
    private List<String> ignore;

    AlternativePrecondition(){
      variableToIndexMap = new HashMap<>();
      preCondition = new ArrayList<>();
      if(options.ignore.equals("")){
        ignore = ImmutableList.of();
      } else {
        ignore = Splitter.on(",").splitToList(options.ignore);
      }
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
      for (String s : ignore) {
        if(s.contains("::")){
          if(formula.toString().contains(s)) {
            return false;
          }
        } else {
          if(formula.toString().contains("::" + s)) {
            return false;
          }
        }
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

  private static class FormulaEntries{
    private List<SSAMap> maps;
    private List<Selector> selectors;
    private List<BooleanFormula> atoms;
    private FormulaContext context;

    FormulaEntries(FormulaContext pContext){
      maps = new ArrayList<>();
      selectors = new ArrayList<>();
      atoms = new ArrayList<>();
      context = pContext;
    }

    void addEntry(SSAMap pMap, Selector pSelector, BooleanFormula pAtom){
      maps.add(pMap);
      selectors.add(pSelector);
      atoms.add(pAtom);
    }

    void remove(int i){
      maps.remove(i);
      selectors.remove(i);
      atoms.remove(i);
    }

    SSAMap getSsaMap(int i){
      int size = maps.size();
      if(size == 0 || i <= 0){
        return context.getManager().makeEmptyPathFormula().getSsa();
      }
      if (i >= size){
        return maps.get(size-1);
      }
      return maps.get(i);
    }

    int size() {
      return atoms.size();
    }
  }
}
