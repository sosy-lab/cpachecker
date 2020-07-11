// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.faultlocalization.formula;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.sosy_lab.common.collect.MapsDifference;
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
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
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
  private List<Selector> distinctSelectors;
  private FormulaEntries entries;

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
    private String filter = "main";

    //Usage: If a variable is contained in the post-condition it may be useful to ignore it in the pre-condition
    @Option(secure=true, name="ignore",
        description="do not add variables to alternative precondition (separate by commas)")
    private String ignore = "";

    //Usage: If a variable is contained in the post-condition it may be useful to ignore it in the pre-condition
    @Option(secure=true, name="disable",
        description="do not create selectors for this variables (separate by commas)")
    private String disable = "";

    @Option(
        secure = true,
        name = "altpre",
        description = "force alternative pre condition")
    private boolean forcePre = false;

    @Option(
        secure = true,
        name = "uniqueselectors",
        description = "equal statements on the same line get the same selector")
    private boolean reduceSelectors = false;

    public TraceFormulaOptions(Configuration pConfiguration) throws InvalidConfigurationException {
      pConfiguration.inject(this);
    }

    public String getFilter() {
      return filter;
    }

    public String getDisable() {
      return disable;
    }

    public String getIgnore() {
      return ignore;
    }

    public boolean isReduceSelectors() {
      return reduceSelectors;
    }

    public void setReduceSelectors(boolean pB) {
      reduceSelectors = pB;
    }
  }

  /**
   * Creates the trace formula for a given list of CFAEdges.
   * Additionally creates a trace formula with selectors.
   * @param pContext commonly used objects
   * @param pOptions set options for trace formula
   * @param pEdges counterexample
   */
  public TraceFormula(FormulaContext pContext, TraceFormulaOptions pOptions, List<CFAEdge> pEdges)
      throws CPATransferException, InterruptedException, SolverException {
    isAlwaysUnsat = false;
    options = pOptions;
    edges = pEdges;
    context = pContext;
    entries = new FormulaEntries();
    negated = new ArrayList<>();
    distinctSelectors =  new ArrayList<>();
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

  public List<Selector> getDistinctSelectors() {
    return distinctSelectors;
  }

  public List<Selector> getRelevantSelectors() {
    if (options.reduceSelectors) {
      return distinctSelectors;
    }
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

  public BooleanFormula getAtom(int i) {
    return entries.atoms.get(i);
  }

  public int traceSize() {
    return entries.size();
  }

  /**
   * Create all trace formulas at once.
   */
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

    // If however the pre condition conjugated with the post-condition is UNSAT
    // tell the main algorithm that this formula is always unsat.
    // happens in programs like: x = 0, y = x, x == 0
    if(context.getSolver().isUnsat(bmgr.and(precondition, postcondition))){
      isAlwaysUnsat = true;
    }

    // loop enrollment causes selectors for each statement instead of actual "lines" in the program
    // less selectors improve the runtime so it may be useful to enable this option
    if (options.reduceSelectors) {
      Map<String, List<Selector>> friends =
          getSelectors().stream()
              .collect(Collectors.groupingBy(s ->
                  s.correspondingEdge().getFileLocation().getStartingLineInOrigin() + " " +
                  s.correspondingEdge().getDescription() + " " +
                  s.correspondingEdge().getEdgeType()));
      friends.forEach(
          (e, v) -> {
            Selector first = v.remove(0);
            distinctSelectors.add(first);
            v.forEach(s -> s.changeSelectorFormula(first));
          });
    }

    // Calculate formulas
    // No isPresent-check needed, because the Selector always exists (see the construction process above).
    BooleanFormula implicationFormula =
        entries.atoms.stream()
            .map(a -> bmgr.implication(Selector.of(a).orElseThrow().getFormula(), a))
            .collect(bmgr.toConjunction());
    actualForm = bmgr.and(bmgr.and(entries.atoms), postcondition);
    implicationForm = bmgr.and(implicationFormula, postcondition);
  }

  /**
   * Calculate the boolean formulas for every edge including the SSA-maps and the selectors.
   * @param altPre Creates the alternative precondition on the fly regardless of set options
   */
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

    Preconditions.checkState(errorStartingLine != -1, "No error condition found");

    // add the current edge formula to the previous one to update the ssa-map, then split them again
    // and add the new part as atom to the entry list.
    // all edges that are at the same line as the last assume edge are added to "negate"
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

    // disable selectors
    if (!options.disable.isEmpty()) {
      List<String> disabled = Splitter.on(",").splitToList(options.disable);
      for (int i = 0; i < entries.size(); i++) {
        String formulaString = entries.atoms.get(i).toString();
        Selector selector = entries.selectors.get(i);
        for (String dable : disabled) {
          if (dable.contains("::")) {
            if (formulaString.contains(dable)) {
              selector.disable();
            }
          } else {
            if (formulaString.contains("::" + dable)) {
              selector.disable();
            }
          }
        }
      }
    }
  }

  /**
   * Calculate the precondition of the program based on a model or the alternative precondition.
   * @param negate all last assume edges
   * @param altPre the alternative precondition
   * @return either the calculated precondition or the alternative precondition
   */
  private BooleanFormula calculatePreCondition(List<BooleanFormula> negate, AlternativePrecondition altPre) throws SolverException, InterruptedException {
    // Create pre condition as model of the actual formula.
    // If the program is has a bug the model is guaranteed to be existent.
    BooleanFormulaManager bmgr = context.getSolver().getFormulaManager().getBooleanFormulaManager();
    BooleanFormula precond = bmgr.makeTrue();
    try (ProverEnvironment prover = context.getProver()) {
      prover.push(bmgr.and(bmgr.and(entries.atoms), bmgr.and(negate)));
      Preconditions.checkArgument(!prover.isUnsat(), "a model has to be existent");
      for (ValueAssignment modelAssignment : prover.getModelAssignments()) {
        BooleanFormula formula = modelAssignment.getAssignmentAsFormula();
        if(formula.toString().contains("__VERIFIER_nondet")){
          precond = bmgr.and(precond, formula);
        }
      }
    }

    // Check if alternative precondition is required and remove corresponding entries.
    if (options.forcePre || (bmgr.isTrue(precond)
        && !context.getSolver().isUnsat(bmgr.and(altPre.toFormula(), postcondition)))) {
      for (BooleanFormula booleanFormula : altPre.getPreCondition()) {
        int index = entries.atoms.indexOf(booleanFormula);
        entries.removeAll(index);
      }
      entries.maps.add(0, altPre.preConditionMap);
      return bmgr.and(precond, altPre.toFormula());
    }
    entries.maps.add(0, SSAMap.emptySSAMap());
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
    return context.getConverter().convert(actualForm);
  }

  private class AlternativePrecondition{

    private Map<Formula, Integer> variableToIndexMap;
    private List<BooleanFormula> preCondition;
    private List<String> ignore;
    private List<String> filter;
    private SSAMap preConditionMap;

    AlternativePrecondition(){
      variableToIndexMap = new HashMap<>();
      preCondition = new ArrayList<>();
      preConditionMap = SSAMap.emptySSAMap();
      if(options.ignore.isBlank()){
        ignore = ImmutableList.of();
      } else {
        ignore = Splitter.on(",").splitToList(options.ignore);
      }
      if(options.filter.isBlank()){
        filter = ImmutableList.of();
      } else {
        filter = Splitter.on(",").splitToList(options.filter);
      }
    }

    void add(BooleanFormula formula, SSAMap currentMap, CFAEdge edge){
      FormulaManagerView fmgr = context.getSolver().getFormulaManager();
      Map<String, Formula> formulaVariables = fmgr.extractVariables(formula);
      Set<String> uninstantiatedVariables = fmgr.extractVariables(fmgr.uninstantiate(formula)).keySet();
      SSAMap toMerge = currentMap;
      // check if formula is accepted.
      if(isAccepted(formula, currentMap, edge, formulaVariables)){
        // remove all other elements from SSAMap if formula is a declaration not using other variables (e.g. int a = 5; int[] d = {1,2,3})
        for (String variable : toMerge.allVariables()) {
          if (!uninstantiatedVariables.contains(variable)) {
            toMerge = toMerge.builder().deleteVariable(variable).build();
          }
        }
        // merge the maps to obtain a SSAMap that represents the inital state (pre-condition)
        preConditionMap = SSAMap.merge(preConditionMap, toMerge, MapsDifference.collectMapsDifferenceTo(new ArrayList<>()));
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
    private boolean isAccepted(BooleanFormula formula, SSAMap currentMap, CFAEdge pEdge, Map<String, Formula> variables){
      if(!pEdge.getEdgeType().equals(CFAEdgeType.DeclarationEdge)){
        return false;
      }

      //check if variable is ignored
      for (String ign : ignore) {
        if(ign.contains("::")){
          if(formula.toString().contains(ign + "@")) {
            return false;
          }
        } else {
          if(formula.toString().contains("::" + ign + "@")) {
            return false;
          }
        }
      }

      //check if variable is in desired scope
      boolean filtered = false;
      for (String f : filter) {
        if (formula.toString().contains(f+"::")){
          filtered = true;
          break;
        }
      }
      if(!filtered) {
        return false;
      }

      //only accept declarations like int a = 2; and not int b = a + 2;
      //int a[] = {3,4,5} will be accepted too (that's why we filter __ADDRESS_OF_)
      if(variables.entrySet().stream().filter(v -> !v.getKey().contains("__ADDRESS_OF_")).count() != 1){
        return false;
      }
      Map<Formula, Integer> index = new HashMap<>();
      for (Formula value : variables.values()) {
        Formula uninstantiated = context.getSolver().getFormulaManager().uninstantiate(value);
        index.put(uninstantiated, currentMap.getIndex(uninstantiated.toString()));
      }
      for (Entry<Formula, Integer> entry: index.entrySet()) {
        if (!variableToIndexMap.containsKey(entry.getKey())) {
          variableToIndexMap.put(entry.getKey(), entry.getValue());
        } else {
          return false;
        }
      }
      return true;
    }
  }

  private static class FormulaEntries{
    private List<SSAMap> maps;
    private List<Selector> selectors;
    private List<BooleanFormula> atoms;

    FormulaEntries(){
      maps = new ArrayList<>();
      selectors = new ArrayList<>();
      atoms = new ArrayList<>();
    }

    void addEntry(SSAMap pMap, Selector pSelector, BooleanFormula pAtom){
      maps.add(pMap);
      selectors.add(pSelector);
      atoms.add(pAtom);
    }

    void removeAll(int i){
      maps.remove(i);
      selectors.remove(i);
      atoms.remove(i);
    }

    SSAMap getSsaMap(int i){
      return maps.get(i);
    }

    int size() {
      return atoms.size();
    }
  }
}
