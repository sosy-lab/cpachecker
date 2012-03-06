/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.relyguarantee.refinement;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.Triple;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.ParallelCFAS;
import org.sosy_lab.cpachecker.cfa.ThreadCFA;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.AssumeEdge;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.art.ARTUtils;
import org.sosy_lab.cpachecker.cpa.art.Path;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGAbstractElement;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGAbstractionManager;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGCPA;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGLocationMapping;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.RGEnvTransitionManager;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.RGEnvironmentManager;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGCFAEdge;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGEnvTransition;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException.Reason;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.cpachecker.util.predicates.Model;
import org.sosy_lab.cpachecker.util.predicates.Model.AssignableTerm;
import org.sosy_lab.cpachecker.util.predicates.Model.TermType;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.SSAMapManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.TheoremProver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.TheoremProver.AllSatPredicates;
import org.sosy_lab.cpachecker.util.predicates.mathsat.MathsatFormulaManager;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * Handles refinement of locations.
 */

@Options(prefix="cpa.rg.refinement")
public class RGLocationRefinementManager implements StatisticsProvider{

  @Option(description="Print debugging info?")
  private boolean debug=false;

  @Option(toUppercase=true, values={"NONE", "WITNESS","MONOTONIC", "NONMONOTONIC"},
      description="Perform refinement of control locations if a feasible counterexample is found.")
  private String locationRefinement =  "MONOTONIC";

  private static final String BRANCHING_PREDICATE_NAME = "__ART__";
  private static final Pattern BRANCHING_PREDICATE_NAME_PATTERN = Pattern.compile(
      "^.*" + BRANCHING_PREDICATE_NAME + "(?=\\d+$)");

  private final FormulaManager fManager;
  private final PathFormulaManager pfManager;
  private final RGEnvTransitionManager etManager;
  private final RGAbstractionManager absManager;
  private final SSAMapManager ssaManager;
  private final TheoremProver thmProver;
  private final RegionManager rManager;
  private final RGEnvironmentManager envManager;
  private ParallelCFAS pcfa;
  private final int tidNo;
  private final LogManager logger;
  private final Stats stats;

  /** Nodes that always have to belong to the first location class, i.e. all global nodes and execution start. */
  private final Set<CFANode> nodesAlwaysInClassOne;
  /** Map: node.toString() -> node */
  private final Map<String, CFANode> nodeMap;
  /** Map : node -> its thread */
  private final Map<CFANode, Integer> nodeToTidMap;

  private static RGLocationRefinementManager singleton;
  private static Pattern varRegex = Pattern.compile("^PRED_(N\\d+)_(\\d+)$");

  public static RGLocationRefinementManager getInstance(FormulaManager pFManager, PathFormulaManager pPfManager, RGEnvTransitionManager pEtManager, RGAbstractionManager absManager, SSAMapManager pSsaManager,TheoremProver pThmProver, RegionManager pRManager, RGEnvironmentManager envManager, ParallelCFAS pPcfa, Configuration pConfig, LogManager pLogger) throws InvalidConfigurationException {
    if (singleton == null){
      singleton = new RGLocationRefinementManager(pFManager, pPfManager, pEtManager, absManager, pSsaManager, pThmProver, pRManager, envManager, pPcfa, pConfig, pLogger);
    }
    return singleton;
  }


  private RGLocationRefinementManager(FormulaManager pFManager, PathFormulaManager pPfManager, RGEnvTransitionManager pEtManager, RGAbstractionManager absManager, SSAMapManager pSsaManager,TheoremProver pThmProver,  RegionManager pRManager, RGEnvironmentManager envManager, ParallelCFAS pcfa, Configuration pConfig, LogManager pLogger) throws InvalidConfigurationException {
    pConfig.inject(this, RGLocationRefinementManager.class);

    this.fManager = pFManager;
    this.pfManager = pPfManager;
    this.etManager = pEtManager;
    this.absManager = absManager;
    this.ssaManager = pSsaManager;
    this.thmProver = pThmProver;
    this.rManager = pRManager;
    this.envManager = envManager;
    this.pcfa = pcfa;
    this.tidNo = pcfa.getThreadNo();
    this.logger  = pLogger;
    this.stats = new Stats();

    this.nodeMap = initializeNodeMap(pcfa);
    this.nodesAlwaysInClassOne = new HashSet<CFANode>();
    this.nodeToTidMap = new HashMap<CFANode, Integer>();

    for (ThreadCFA cfa : pcfa){
      this.nodesAlwaysInClassOne.addAll(cfa.globalDeclNodes);
      this.nodesAlwaysInClassOne.add(cfa.executionStart);

      for (CFANode node : cfa.getAllNodes()){
        this.nodeToTidMap.put(node, cfa.getTid());
      }
    }
  }


  private Map<String, CFANode> initializeNodeMap(ParallelCFAS pcfa) {

    Map<String, CFANode> nodeMap = new HashMap<String, CFANode>();

    for (ThreadCFA cfa : pcfa){
      for (CFANode node : cfa.getAllNodes()){
        nodeMap.put(node.toString(), node);
      }
    }

    return nodeMap;
  }


  /**
   * Analyze the feasible counterexample. Depending on the configuration
   * it doesn't do anything, finds a witness or refines on locations.
   * @param counterexample
   * @return
   * @throws CPATransferException
   * @throws RefinementFailedException
   */
  public InterpolationTreeResult refine(InterpolationTreeResult counterexample) throws CPATransferException, RefinementFailedException {


    InterpolationTreeResult result;

    if (counterexample.isSpurious() || locationRefinement.equals("NONE")){
      // already refined or nothing to do
      result = counterexample;
    }
    else if (locationRefinement.equals("WITNESS")){
      result = findWitness(counterexample);
    }
    else {
      result = findLocationMistmaches(counterexample);
    }

    stats.locRefTimer.stop();
    return result;
  }




  /**
   * Adds a counterexample path the argument. This path is may not be feasible w.r.t. locations.
   * In other words, it might be a false positive.
   * @param cexample
   * @return
   * @throws CPATransferException
   */
  private InterpolationTreeResult findWitness(InterpolationTreeResult cexample) throws CPATransferException {

    Collection<Path> paths = getErrorPathsForTrunk(cexample.getTree());
    assert !paths.isEmpty() : "Couldn't find a witness path.";

    Path pi = paths.iterator().next();

    InterpolationTreeResult newCexample = InterpolationTreeResult.feasibleWithWitness(cexample.getTree(), pi);
    return newCexample;
  }


  private InterpolationTreeResult findLocationMistmaches(InterpolationTreeResult cexample) throws CPATransferException {

    Collection<Path> paths = getErrorPathsForTrunk(cexample.getTree());
    Map<Path, List<Triple<ARTElement, CFANode, CFANode>>> pathRefMap = new HashMap<Path, List<Triple<ARTElement, CFANode, CFANode>>>();

    for (Path pi : paths){
      List<Triple<ARTElement, CFANode, CFANode>> threadInq = findLocationInequalities(pi, false);

      if (threadInq.isEmpty()){
        // concreate, feasible error path found
        InterpolationTreeResult newCexample = InterpolationTreeResult.feasibleWithWitness(cexample.getTree(), pi);
        return newCexample;
      }

      pathRefMap.put(pi, threadInq);
    }

    InterpolationTreeResult newCexample = InterpolationTreeResult.spurious();
    newCexample.addLocationMapForRefinement(pathRefMap);
    return newCexample;
  }


 /* /**
   * Monotonic refinement - a new equivalence class is created for every mismatching location.
   * @param counterexample
   * @return
   * @throws CPATransferException
   * @throws RefinementFailedException

  private InterpolationTreeResult monotonicRefinement(InterpolationTreeResult counterexample) throws CPATransferException, RefinementFailedException {
    stats.locRefTimer.start();
    stats.iterations++;
    Collection<Path> paths = getErrorPathsForTrunk(counterexample.getTree());

    Collection<Pair<CFANode, CFANode>> inqSet = new LinkedHashSet<Pair<CFANode, CFANode>>();

    for (Path pi : paths){
      Collection<Pair<CFANode, CFANode>> threadInq = findLocationInequalities(pi, true);

      if (threadInq.isEmpty()){
        // concreate, feasible error path found
        counterexample.setCounterexamplePath(pi);
        return counterexample;
      }

      inqSet.addAll(threadInq);
    }

    // path is spurious, find new location mapping
    RGLocationMapping refLocationMapping = monotonicLocationMapping(envManager.getLocationMapping(), inqSet);
    counterexample = new InterpolationTreeResult(true);
    counterexample.setRefinedLocationMapping(refLocationMapping);

    stats.locClNo = refLocationMapping.getClassNo();

    return counterexample;
  }*/

  /*
   * Non-monotonic refinement like described in "Non-monotonic Refinement of Control
   * Abstraction for Concurrent Programs" by Gupta.
   * @param counterexample
   * @return
   * @throws CPATransferException

  private InterpolationTreeResult nonMonotonicRefinement(InterpolationTreeResult counterexample) throws CPATransferException {
    stats.locRefTimer.start();
    stats.iterations++;

    InterpolationTree tree = counterexample.getTree();
    Collection<Path> paths = getErrorPathsForTrunk(tree);

    for (Path pi : paths){
      Collection<Pair<CFANode, CFANode>> threadInq = findLocationInequalities(pi, false);

      if (threadInq.isEmpty()){
        // concrete, feasible error path found
        counterexample.setCounterexamplePath(pi);
        return counterexample;
      }

      globalInqMap.putAll(pi, threadInq);
    }

    // path is spurious, find new location mapping
    RGLocationMapping refLocationMapping = nonMonotonicLocationMapping(globalInqMap);
    counterexample = new InterpolationTreeResult(true);
    counterexample.setRefinedLocationMapping(refLocationMapping);

    stats.locClNo = refLocationMapping.getClassNo();

    return counterexample;
  }*/


  /**
   * Finds the minimal number of equivalence class that puts splits mistmatching locations using a SAT solver.
   * @param inqMap
   * @param tree
   * @return
   */
  public Pair<RGLocationMapping, Integer> nonMonotonicLocationMapping(Multimap<Path, Pair<CFANode, CFANode>> inqMap, int minClNo) {

    int clNo = minClNo;
    thmProver.init();

    boolean unsat;
    do {
      Formula fr = buildNonMonotonicFormulaSAT(inqMap, clNo);
      thmProver.push(fr);

      stats.solverTimer.start();
      unsat = thmProver.isUnsat(fManager.makeTrue());
      stats.solverTimer.stop();

      clNo++;
    } while (unsat);


    Model model = thmProver.getModel();
    thmProver.reset();

    RGLocationMapping lm = RGLocationMappingSAT(model);
    return Pair.of(lm, clNo-1);
  }

  /**
   * Builds a linear-arithmetic formula that is satisfiable if the mismatching locations can be split
   * in "the next power of 2 that is >= clNo" number of classes.
   * @param inqMap
   * @param clNo
   * @return
   */
  private Formula buildNonMonotonicFormulaLA(Multimap<Path, Pair<CFANode, CFANode>> inqMap, int clNo) {
    stats.formulaTimer.start();

    Formula f = fManager.makeTrue();
    Formula one = fManager.makeNumber(1);
    Formula max = fManager.makeNumber(clNo);
    Set<Formula> variablesUsed = new HashSet<Formula>();

    for (Path pi : inqMap.keySet()){
      Formula fdis = fManager.makeFalse();
      Collection<Pair<CFANode, CFANode>> misColl = inqMap.get(pi);

      for (Pair<CFANode, CFANode> pair : misColl){
        String first = MathsatFormulaManager.makeName(pair.getFirst().toString(), 0);
        String second = MathsatFormulaManager.makeName(pair.getSecond().toString(), 0);

        Formula va = fManager.makeVariable(first);
        Formula vb = fManager.makeVariable(second);
        variablesUsed.add(va);
        variablesUsed.add(vb);

        Formula fmis = fManager.makeNot(fManager.makeEqual(va, vb));
        fdis = fManager.makeOr(fdis, fmis);
      }

      f = fManager.makeAnd(f, fdis);
    }

    for (Formula var : variablesUsed){
      Formula geq = fManager.makeGeq(var, one);
      Formula leq = fManager.makeLeq(var, max);
      f = fManager.makeAnd(f, leq);
      f = fManager.makeAnd(f, geq);
    }

    stats.formulaTimer.stop();
    return f;
  }


  /**
   * Builds a propositional formula that is satisfiable if the mismatching locations can be split
   * in "the next power of 2 that is >= clNo" number of classes.
   * @param inqMap
   * @param clNo
   * @return
   */
  private Formula buildNonMonotonicFormulaSAT(Multimap<Path, Pair<CFANode, CFANode>> inqMap, int clNo) {
    stats.formulaTimer.start();
    Formula f = fManager.makeTrue();

    int bitNo = 0;
    int powerOf2 = 1;
    while (powerOf2 < clNo){
      bitNo++;
      powerOf2 *= 2;
    }

    for (Path pi : inqMap.keySet()){
      Formula dis = buildDisjunctionOverMistmachesSAT(inqMap.get(pi), bitNo);
      f = fManager.makeAnd(f, dis);
    }

    stats.formulaTimer.stop();
    return f;
  }

  /**
   * Builds disjunction over all mismatching locations using the given number of bits.
   * @param inqColl
   * @param bitNo
   * @return
   */
  private Formula buildDisjunctionOverMistmachesSAT(Collection<Pair<CFANode, CFANode>> inqColl, int bitNo) {
    Formula f = fManager.makeFalse();

    for (Pair<CFANode, CFANode> pair : inqColl){
      Formula fmis = buildMistmachSAT(pair.getFirst(), pair.getSecond(), bitNo);
      f = fManager.makeOr(f, fmis);
    }

    return f;
  }


  /**
   * Encodes a mismatching pair of locations using the given number of bits.
   * @param first
   * @param second
   * @param bitNo
   * @return
   */
  private Formula buildMistmachSAT(CFANode first, CFANode second, int bitNo) {

    Formula f = fManager.makeTrue();

    for (int i=1; i<=bitNo; i++){
      Formula b1i = getVariable(first, i);
      Formula b2i = getVariable(second, i);
      Formula xor = fManager.makeNot(fManager.makeEquivalence(b1i, b2i));
      f = fManager.makeAnd(f, xor);
    }

    return f;
  }


  /**
   * Creates a predicate variable for bit i of the location.
   * @param node
   * @param i
   * @return
   */
  private Formula getVariable(CFANode node, int i) {
    String name = "_"+node+"_"+i;
    return fManager.makePredicateVariable(name, 0);
  }


  private RGLocationMapping RGLocationMappingLA(Model model) {

    Map<CFANode, Integer> locMapping = new HashMap<CFANode, Integer>();
    Set<CFANode> allNodes = new HashSet<CFANode>(nodeMap.values());


    /* retrive location classes from the model*/
    for (AssignableTerm term : model.keySet()){
      assert term.getType() == TermType.Integer;
      Integer classNo = (Integer) model.get(term);

      CFANode node = nodeMap.get(term.getName());
      locMapping.put(node, classNo);

      allNodes.remove(node);
    }

    /* put remaining nodes in class 1*/
    for (CFANode node : allNodes){
      locMapping.put(node, 1);
    }


    return RGLocationMapping.copyOf(locMapping);
  }

  /**
   * Creates a location from the SAT model.
   * @param pModel
   * @return
   */
  private RGLocationMapping RGLocationMappingSAT(Model model) {
    // TODO ensure that execution start is always in class one.
    Map<CFANode, Integer> locMapping = new HashMap<CFANode, Integer>();
    Set<CFANode> allNodes = new HashSet<CFANode>(nodeMap.values());

    // map: location -> (bit number, value)
    Multimap<CFANode, Pair<Integer, Boolean>> predMap = HashMultimap.create();

    /* get bit values */
    for (AssignableTerm term : model.keySet()){
      assert term.getType() == TermType.Boolean;
      Boolean value = (Boolean) model.get(term);

      Pair<CFANode, Integer> pair = parseSATVariable(term.getName());
      CFANode node = pair.getFirst();
      Integer bitNo = pair.getSecond();
      predMap.put(node, Pair.of(bitNo, value));
    }

    /* retrive location classes from the bits */
    for (CFANode node : predMap.keySet()){
      Integer locCl = bitsToNumber(predMap.get(node))+1;
      locMapping.put(node, locCl);
      allNodes.remove(node);
    }


    /* put remaining nodes in class 1*/
    for (CFANode node : allNodes){
      locMapping.put(node, 1);
    }


    return RGLocationMapping.copyOf(locMapping);
  }


  /**
   * Bit to integer representation.
   * @param collection
   * @return
   */
  private Integer bitsToNumber(Collection<Pair<Integer, Boolean>> collection) {
    int value = 0;
    for (Pair<Integer, Boolean> pair : collection){
      if (pair.getSecond()){

        int powerOf2 = 1;
        for (int i=2; i<=pair.getFirst(); i++){
          powerOf2 *= 2;
        }

        value += powerOf2;
      }
    }

    return value;
  }


  /**
   * Retrieves CFANode and bit number from predicate name.
   * @param name
   * @return
   */
  private Pair<CFANode, Integer> parseSATVariable(String name) {
    Matcher mt = varRegex.matcher(name);
    if (mt.find()){
      String nodeName = mt.group(1);
      CFANode node = nodeMap.get(nodeName);
      Integer bitNo = Integer.parseInt(mt.group(2));
      return Pair.of(node, bitNo);
    }

    throw new UnsupportedOperationException("Couldn't parse "+name);
  }




  /**
   * Refines old location mapping by putting mistmatching nodes in different equivalence classes.
   * @param oldLM
   * @param inqColl
   * @return
   * @throws RefinementFailedException
   */
  public RGLocationMapping monotonicLocationMapping(RGLocationMapping oldLM, Collection<Pair<CFANode, CFANode>> inqColl) throws RefinementFailedException {
    Integer topCl = oldLM.getClassNo();

    HashMap<CFANode, Integer> newLM = new HashMap<CFANode, Integer>(oldLM.getMap());

    // put one of the mistmatching nodes in a new classe
    for (Pair<CFANode, CFANode> inq : inqColl){
      // make sure that global decl. nodes and execution start always belong to class 1
      CFANode node = inq.getSecond();

      if (nodesAlwaysInClassOne.contains(node)){
        node = inq.getFirst();

        if (nodesAlwaysInClassOne.contains(node)){
          throw new RefinementFailedException(Reason.LocationRefinementFailed, null);
        }
      }

      newLM.put(node, ++topCl);
    }

    return RGLocationMapping.copyOf(newLM);
  }


  /**
   * Traverses the path and finds mismatching locations that make it spurious.
   * The map is empty if the path is feasible w.r.t to locations.
   * @param pi
   * @param stopAfterFirst find only the first inequality
   * @return
   */
  private List<Triple<ARTElement, CFANode, CFANode>> findLocationInequalities(Path pi, boolean stopAfterFirst) {

    List<Triple<ARTElement, CFANode, CFANode>> inqList = new Vector<Triple<ARTElement, CFANode, CFANode>>();

    ARTElement first = pi.get(0).getFirst();
    // tid of the ART where the path begins
    int stid = first.getTid();

    // expected locations
    CFANode[] pc = new CFANode[tidNo];

    /* initialize local thread to start of global declarations
     * and other threads to start of execution */
    for (int i=0; i<tidNo; i++){
      if (i == stid){
        pc[i] = pcfa.getCfas().get(i).getInitalNode();
      } else {
        pc[i] = pcfa.getCfas().get(i).getExecutionStart();
      }
    }

    // execute
    for (Pair<ARTElement, CFAEdge> pair : pi){
      ARTElement element = pair.getFirst();
      CFAEdge edge = pair.getSecond();

      int tid = nodeToTidMap.get(edge.getPredecessor());
      CFANode loc = edge.getPredecessor();

      if (pc[tid].equals(loc)){
        pc[tid] = edge.getSuccessor();
      } else {
        // mismatch pc[tid] != loc
        Pair<CFANode, CFANode> mismatch = Pair.of(pc[tid], loc);
        Triple<ARTElement, CFANode, CFANode> mistmatch = Triple.of(element, pc[tid], loc);
        inqList.add(mistmatch);

        if (stopAfterFirst){
          break;
        }

        pc[tid] = edge.getSuccessor();
      }
    }

    return inqList;
  }


  private Collection<Path> getErrorPathsForTrunk(InterpolationTree tree) throws CPATransferException {

    List<InterpolationTreeNode> trunk = tree.getTrunk();
    Collection<ARTElement> allElems = getAllARTElementsOnBranch(trunk, tree);
    Map<Pair<ARTElement, CFAEdge>, Formula> predMap = getBranchingPredicates(allElems);

    // one model describes one path - no branching is specified by the empty model
    Collection<RGBranchingModel> bModels;

    if (!predMap.isEmpty()){
      bModels = getBranchingModels(predMap, tree);

    } else {
      RGBranchingModel emptyModel = new RGBranchingModel();
      bModels = Collections.singleton(emptyModel);
    }

    Collection<Path> paths = getPathsForBranch(trunk, allElems, bModels);
    return paths;
  }

  /**
   * Determines all feasible combinations of branches specified in predMap.
   * @param predMap
   * @param bf
   * @param tree
   * @return
   * @throws CPATransferException
   */
  private Collection<RGBranchingModel> getBranchingModels(Map<Pair<ARTElement, CFAEdge>, Formula> predMap, InterpolationTree tree) throws CPATransferException {

    Formula bf = buildBranchingFormula(predMap, tree);

    System.out.println("bf: "+bf);

    for (InterpolationTreeNode node : tree){
      Formula f = node.getPathFormula().getFormula();
      System.out.println("\t-"+node+" "+f);
      bf = fManager.makeAnd(bf,f);
    }

    AllSatPredicates allPredSat = thmProver.allSatPredicate(bf, predMap.values());

    assert allPredSat.getCount() < Integer.MAX_VALUE;

    Collection<RGBranchingModel> bModels = new Vector<RGBranchingModel>(allPredSat.getCount());

    for (Map<Formula, Boolean> predModel : allPredSat.getPredicateModels()){
      RGBranchingModel bModel = getBranchningModel(predMap, predModel);
      bModels.add(bModel);
    }

    return bModels;
  }


  /**
   * Constructs a branching model by composing the predicate map with the predicate model.
   * @param predMap
   * @param predModel
   * @return
   */
  private RGBranchingModel getBranchningModel(Map<Pair<ARTElement, CFAEdge>, Formula> predMap, Map<Formula, Boolean> predModel) {

    RGBranchingModel bModel = new RGBranchingModel();

    for (Pair<ARTElement, CFAEdge> key : predMap.keySet()){
      Formula pred = predMap.get(key);
      Boolean value = predModel.get(pred);
      assert value != null;
      bModel.put(key, value);
    }

    return bModel;
  }


  /**
   * Construct all paths on the branch using the map.
   * @param branch
   * @param elems
   * @param bModels
   * @return
   */
  private Collection<Path> getPathsForBranch(List<InterpolationTreeNode> branch, Collection<ARTElement> elems, Collection<RGBranchingModel> bModels) {
    assert !bModels.isEmpty();

    Collection<Path> pis = new Vector<Path>(bModels.size());
    ARTElement start  = branch.get(branch.size()-1).getArtElement();
    ARTElement stop = branch.get(0).getArtElement();

    // get path for each model
    for (RGBranchingModel bModel : bModels){
      Path pi = getPathBetween(start, stop, elems, bModel);
      pathPrinter(pi);
      System.out.println("\n");
      pis.add(pi);
    }

    return pis;
  }


  /**
   * Constructs the paths between start and stop, whose elements belong to the set of elements.
   * Branches are resolved using the map, which should specify exactly one path.
   * @param start
   * @param stop
   * @param elems
   * @param bModel
   * @return
   */
  private Path getPathBetween(ARTElement start, ARTElement stop, Collection<ARTElement> elems, RGBranchingModel bModel) {
    if (stop == null){
      return null;
    }
    assert !stop.isDestroyed();

    // paths for elements
    Map<ARTElement, Path> pathMap = new HashMap<ARTElement, Path>();
    Deque<ARTElement> queue = new LinkedList<ARTElement>();
    pathMap.put(start, new Path());

    queue.add(start);

    // DFS
    while(!queue.isEmpty()){
      ARTElement elem = queue.pop();
      assert !elem.isDestroyed();

      Path elemPath = pathMap.get(elem);

      if (elem.equals(stop)){
        continue;
      }

      // find children that are in elems
      Map<ARTElement, CFAEdge> childMap = new HashMap<ARTElement, CFAEdge>();
      for (ARTElement child : elem.getLocalChildren()){
        if (elems.contains(child)){
          CFAEdge edge = elem.getLocalChildMap().get(child);
          childMap.put(child, edge);
        }
      }

      ARTElement childTaken = null;
      if (childMap.size() > 1){
        // find the branch
        for (Entry<ARTElement, CFAEdge> entry : childMap.entrySet()){
          Pair<ARTElement, CFAEdge> key = Pair.of(elem, entry.getValue());
          boolean isTaken = bModel.get(key);
          if (isTaken){
            // one and only one branch can be taken
            assert childTaken == null;
            childTaken = entry.getKey();
          }
        }
      }
      else if (childMap.size() == 1){
        // extend the path
        Entry<ARTElement, CFAEdge> entry = childMap.entrySet().iterator().next();
        childTaken = entry.getKey();
      }

      assert childTaken != null;
      CFAEdge edge = childMap.get(childTaken);
      Path edgePath = pathsForEdge(edge, elem, bModel);
      Path childPath = new Path(elemPath);
      childPath.addAll(edgePath);
      pathMap.put(childTaken, childPath);
      queue.addLast(childTaken);
    }

    Path pi = pathMap.get(stop);
    assert pi != null;
    return pi;
  }


  /**
   * Constructs the path for a local or environmental edge.
   * @param edge
   * @param elem
   * @param bModel
   * @return
   */
  private Path pathsForEdge(CFAEdge edge, ARTElement elem, RGBranchingModel bModel) {
    if (edge.getEdgeType() == CFAEdgeType.RelyGuaranteeCFAEdge){
      // environmental edge
      RGCFAEdge rgEdge = (RGCFAEdge) edge;
      RGEnvTransition et = rgEdge.getRgEnvTransition();
      Path pi = new Path();
      pi.push(Pair.of(elem, et.getOperation()));
      return pi;
    }
    else {
      // local edge
      Path pi = new Path();
      pi.push(Pair.of(elem, edge));
      return pi;
    }
  }


  /**
   * Returns the edge between start and child.
   * @param start
   * @param child
   * @return
   */
  private CFAEdge getParentChildEdge(ARTElement start, ARTElement child) {
    CFAEdge edge = start.getLocalChildMap().get(child);
    return edge;
  }


  /**
   * Find all ART elements, local and environmental, on the branch.
   * @param tree
   * @param tree
   * @return
   */
  private Collection<ARTElement> getAllARTElementsOnBranch(List<InterpolationTreeNode> branch, InterpolationTree tree) {

    if (branch.isEmpty()){
      return Collections.emptySet();
    }

    InterpolationTreeNode lastNode = branch.get(0);
    ARTElement target = lastNode.getArtElement();
    Set<ARTElement> allElems = ARTUtils.getAllElementsOnPathsTo(target);

    // check if all nodes were found
    if (debug){
      for (InterpolationTreeNode node : branch){
        assert allElems.contains(node.getArtElement());
      }
    }

    // add generating nodes for env. applications
    for (InterpolationTreeNode node : branch){

      Map<Integer, RGEnvTransition> envMap = node.getEnvMap();

      for (RGEnvTransition et: envMap.values()){

        /* The elements for this env. transiton could have been trimmed out.
         * Add generating elements only if the last abstraction point for the transition's source
         * is an ancestor of the node.
         */
        ARTElement sourceElem = et.getSourceARTElement();
        ARTElement laElem = RGCPA.findLastAbstractionARTElement(sourceElem);

        List<InterpolationTreeNode> ancestors = tree.getChildrenOf(node);
        InterpolationTreeNode laNode = null;
        for (InterpolationTreeNode ancestor : ancestors){
          if (ancestor.getArtElement().equals(laElem)){
            laNode = ancestor;
            break;
          }
        }

        if (laNode != null){
          allElems.addAll(et.getGeneratingARTElements());
        }
      }
    }

    return allElems;
  }

  /**
   * Returns a map from branching CFA edges, both local and environmental, to their predicates.
   * @param elementsOnPath
   * @return
   * @throws CPATransferException
   */
  private Map<Pair<ARTElement, CFAEdge>, Formula> getBranchingPredicates(Collection<ARTElement> elementsOnPath) throws CPATransferException {

    Map<Pair<ARTElement, CFAEdge>, Formula> predMap = new HashMap<Pair<ARTElement, CFAEdge>, Formula>();

    for (final ARTElement pathElement : elementsOnPath) {
      if (pathElement.isDestroyed()){
        continue;
      }

      Set<ARTElement> children = new LinkedHashSet<ARTElement>(pathElement.getLocalChildMap().size());
      int localChildren = 0;

      // consider only the children that are on the path
      for (ARTElement child : pathElement.getLocalChildren()){
        if (elementsOnPath.contains(child)){
          children.add(child);
        }
      }

      if (children.size() > 1) {

        for (ARTElement child : children){
          CFAEdge edge = getParentChildEdge(pathElement, child);

          Pair<ARTElement, CFAEdge> key = Pair.of(pathElement, edge);

          if (edge.getEdgeType() == CFAEdgeType.RelyGuaranteeCFAEdge){
            String name = BRANCHING_PREDICATE_NAME+pathElement.getElementId()+"_"+child.getElementId();
            Formula pred = fManager.makePredicateVariable(name, 0);
            predMap.put(key, pred);
          }
          else if (edge instanceof AssumeEdge){
            localChildren++;
            if (localChildren > 2){
              throw new UnsupportedOperationException("Can't handle more than two local edges");
            }

            String name = BRANCHING_PREDICATE_NAME+pathElement.getElementId()+"_"+child.getElementId();
            Formula pred = fManager.makePredicateVariable(name, 0);
            predMap.put(key, pred);
          }
        }
      }
    }
    return predMap;
  }

  /**
   * Build a branching formula from the map.
   * @param tree
   * @param pBMap
   * @return
   * @throws CPATransferException
   */
  private Formula buildBranchingFormula(Map<Pair<ARTElement, CFAEdge>, Formula> predMap, InterpolationTree tree) throws CPATransferException {
    Formula bf = fManager.makeTrue();

    Map<Integer, Integer> rMap = new HashMap<Integer, Integer>(1);

    for (Pair<ARTElement, CFAEdge> key : predMap.keySet()){
      ARTElement elem = key.getFirst();
      Formula pred = predMap.get(key);
      RGAbstractElement rgElem = AbstractElements.extractElementByType(elem, RGAbstractElement.class);
      CFAEdge edge = key.getSecond();

      /*
       * Find nodes matching the tid and element id of the abstraction point. All these
       * nodes represent the same part of ART, so for branching its enough to first the
       * first one.
       */
      int tid;
      int elemId;

      if (edge.getEdgeType() == CFAEdgeType.RelyGuaranteeCFAEdge){
        RGCFAEdge rgEdge = (RGCFAEdge) edge;
        RGEnvTransition et = rgEdge.getRgEnvTransition();
        tid = et.getTid();
        elemId = et.getAbstractionElement().getElementId();
      }
      else if (edge.getEdgeType() == CFAEdgeType.AssumeEdge){
        ARTElement laElement = RGCPA.findLastAbstractionARTElement(elem);
        elemId = laElement.getElementId();
        tid = elem.getTid();
      }
      else {
        throw new UnsupportedOperationException("branching must be either Assume or RelyGuarantee type. Problem: "+edge);
      }

      Collection<InterpolationTreeNode> nodes = tree.getNodesByTidAndElementId(tid, elemId);
      InterpolationTreeNode node = nodes.iterator().next();
      int uniqueId = node.getUniqueId();

      /* Construct the edge formula */
      Formula edgeF;
      if (edge.getEdgeType() == CFAEdgeType.RelyGuaranteeCFAEdge){
        RGCFAEdge rgEdge = (RGCFAEdge) edge;
        edgeF = etManager.formulaForRefinement(rgElem, rgEdge.getRgEnvTransition(), uniqueId).getFormula();
      }
      else if (edge.getEdgeType() == CFAEdgeType.AssumeEdge){
        PathFormula pf = rgElem.getAbsPathFormula();
        pf = pfManager.makeEmptyPathFormula(pf);
        edgeF = pfManager.makeAnd(pf, edge).getFormula();
      }
      else {
        throw new UnsupportedOperationException("branching must be either Assume or RelyGuarantee type. Problem: "+edge);
      }

      /* Rename the edge formula */

      if (edge.getEdgeType() == CFAEdgeType.RelyGuaranteeCFAEdge){
        InterpolationTreeNode parentBranchNode = tree.getParentBranchId(node);

        if (parentBranchNode != null){
          rMap.put(-1, parentBranchNode.uniqueId);
        }
      } else {
        rMap.put(-1, uniqueId);
      }

      edgeF = fManager.changePrimedNo(edgeF, rMap);

      Formula equiv = fManager.makeEquivalence(pred, edgeF);
      bf = fManager.makeAnd(bf, equiv);
    }

    return bf;
  }

  private void pathPrinter(Path pi) {
    System.out.println();
    for (Pair<ARTElement, CFAEdge> pair : pi){
      ARTElement element = pair.getFirst();
      CFANode loc = element.retrieveLocationElement().getLocationNode();
      CFAEdge edge = pair.getSecond();
      System.out.println("("+element.getTid()+","+element.getElementId()+","+loc+")\t"+edge.getRawStatement());
    }
  }


  @Override
  public void collectStatistics(Collection<Statistics> scoll) {
    scoll.add(stats);
  }

  public static class Stats implements Statistics {
    public final Timer locRefTimer    = new Timer();
    public final Timer formulaTimer   = new Timer();
    public final Timer solverTimer    = new Timer();
    public int iterations             = 0;
    public int locClNo                = 1;



    @Override
    public String getName() {
      return "RGLocationRefinementManager";
    }

    @Override
    public void printStatistics(PrintStream out, Result pResult,ReachedSet pReached){
      out.println("time on location ref.:           " + locRefTimer);
      out.println("time on constraint solving:      " + solverTimer);
      out.println("time on location formulas:       " + formulaTimer);
      out.println("refinements no:                  " + formatInt(iterations));
      out.println("number of location classes:      " + formatInt(locClNo));
    }

    private String formatInt(int val){
      return String.format("  %7d", val);
    }

  }



}
