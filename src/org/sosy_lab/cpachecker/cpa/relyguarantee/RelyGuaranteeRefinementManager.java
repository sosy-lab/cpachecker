/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.relyguarantee;

import static com.google.common.collect.Iterables.skip;
import static com.google.common.collect.Lists.transform;
import static org.sosy_lab.cpachecker.util.AbstractElements.extractElementByType;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.Vector;
import java.util.regex.Pattern;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Triple;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.art.ARTUtils;
import org.sosy_lab.cpachecker.cpa.art.Path;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateRefinementManager;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RelyGuaranteeAbstractElement.AbstractionElement;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RelyGuaranteeRefiner.RelyGuaranteeRefinerStatistics;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.InterpolatingTheoremProver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.TheoremProver;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;


@Options(prefix="cpa.relyguarantee")
public class RelyGuaranteeRefinementManager<T1, T2> extends PredicateRefinementManager<T1, T2>  {

  private static final String BRANCHING_PREDICATE_NAME = "__ART__";
  private static final Pattern BRANCHING_PREDICATE_NAME_PATTERN = Pattern.compile(
      "^.*" + BRANCHING_PREDICATE_NAME + "(?=\\d+$)");

  @Option(name="refinement.interpolatingProver", toUppercase=true, values={"MATHSAT", "CSISAT"},
      description="which interpolating solver to use for interpolant generation?")
      private String whichItpProver = "MATHSAT";

  @Option(name="refinement.itpEnvSkip",
      description="Detect and skip interpolation branches that don't give new predicates.")
      private boolean itpEnvSkip = false;

  @Option(name="refinement.DAGRefinement",
      description="Extracts interpolants from a DAG representation of threads and environmental transitions.")
      private boolean DAGRefinement = true;

  @Option(name="refinement.dumpDAGfile",
      description="Dump a DAG representation of interpolation formulas to the choosen file. Valid only with DAGRefinement")
      private String dagFilePredix = "test/output/itpDAG";

  @Option(name="refinement.splitItpAtoms",
      description="split arithmetic equalities when extracting predicates from interpolants")
      private boolean splitItpAtoms = false;

  @Option(description="Print debugging info?")
  private boolean debug=false;



  private static RelyGuaranteeRefinementManager<?,?> rgRefManager;

  /**
   * Singleton instance of RelyGuaranteeRefinementManager.
   * @param pRmgr
   * @param pFmgr
   * @param pPmgr
   * @param pThmProver
   * @param pItpProver
   * @param pAltItpProver
   * @param pConfig
   * @param pLogger
   * @param globalVariables
   * @return
   * @throws InvalidConfigurationException
   */
  public static RelyGuaranteeRefinementManager<?, ?> getInstance(RegionManager pRmgr, FormulaManager pFmgr, PathFormulaManager pPmgr, TheoremProver pThmProver,
      InterpolatingTheoremProver<?> pItpProver, InterpolatingTheoremProver<?> pAltItpProver, Configuration pConfig,
      LogManager pLogger) throws InvalidConfigurationException {
    if (rgRefManager == null){
      rgRefManager = new RelyGuaranteeRefinementManager(pRmgr, pFmgr, pPmgr, pThmProver, pItpProver, pAltItpProver, pConfig, pLogger);
    }
    return rgRefManager;
  }

  public RelyGuaranteeRefinementManager(RegionManager pRmgr, FormulaManager pFmgr, PathFormulaManager pPmgr, TheoremProver pThmProver,
      InterpolatingTheoremProver<T1> pItpProver, InterpolatingTheoremProver<T2> pAltItpProver, Configuration pConfig,
      LogManager pLogger) throws InvalidConfigurationException {
    super(pRmgr, pFmgr, pPmgr, pThmProver, pItpProver, pAltItpProver, pConfig, pLogger);
    pConfig.inject(this, RelyGuaranteeRefinementManager.class);
  }


  /**
   * Counterexample analysis and predicate discovery.
   * This method is just an helper to delegate the actual work
   * This is used to detect timeouts for interpolation
   * @param stats
   * @throws CPAException
   * @throws InterruptedException
   */
  public CounterexampleTraceInfo buildRgCounterexampleTrace(final ARTElement targetElement, final ReachedSet[] reachedSets, int tid, RelyGuaranteeRefinerStatistics stats) throws CPAException, InterruptedException {
    // if we don't want to limit the time given to the solver
    //return buildCounterexampleTraceWithSpecifiedItp(pAbstractTrace, elementsOnPath, firstItpProver);

    if (this.whichItpProver.equals("MATHSAT")){
      if (this.DAGRefinement){
        return interpolateDagsMathsat(targetElement,  reachedSets, tid, firstItpProver, stats);
      } else {
        //return interpolateTreeMathsat(targetElement,  reachedSets, tid, firstItpProver, stats);
        throw new InterruptedException("Curretly only DAG refinement is supported.");
      }

    } else {
      throw new InterruptedException("Unknown iterpolating prover "+whichItpProver);
    }

  }


  protected Path computePath(ARTElement pLastElement, ReachedSet pReached) throws InterruptedException, CPAException {
    return ARTUtils.getOnePathTo(pLastElement);
  }

  protected List<Triple<ARTElement, CFANode, RelyGuaranteeAbstractElement>> transformFullPath(Path pPath) throws CPATransferException {
    List<Triple<ARTElement, CFANode, RelyGuaranteeAbstractElement>> result = Lists.newArrayList();

    for (ARTElement ae : transform(pPath, Pair.<ARTElement>getProjectionToFirst())) {
      RelyGuaranteeAbstractElement pe = extractElementByType(ae, RelyGuaranteeAbstractElement.class);
      if (pe instanceof AbstractionElement) {
        CFANode loc = AbstractElements.extractLocation(ae);
        result.add(Triple.of(ae, loc, pe));
      }
    }

    //assert  pPath.getLast().getFirst() == result.get(result.size()-1).getFirst();
    return result;
  }

  protected List<Triple<ARTElement, CFANode, RelyGuaranteeAbstractElement>> transformPath(Path pPath) throws CPATransferException {
    List<Triple<ARTElement, CFANode, RelyGuaranteeAbstractElement>> result = Lists.newArrayList();

    for (ARTElement ae : skip(transform(pPath, Pair.<ARTElement>getProjectionToFirst()), 1)) {
      RelyGuaranteeAbstractElement pe = extractElementByType(ae, RelyGuaranteeAbstractElement.class);
      if (pe instanceof AbstractionElement) {
        CFANode loc = AbstractElements.extractLocation(ae);
        result.add(Triple.of(ae, loc, pe));
      }
    }

    //assert  pPath.getLast().getFirst() == result.get(result.size()-1).getFirst();
    return result;
  }

  /**
   * Returns a formula encoding rely guarantee transitions to the target element, including interactions with other threads.
   * @param target
   * @param reachedSets
   * @param threadNo
   * @param context
   * @return
   * @throws CPAException
   * @throws InterruptedException
   */
  /*public List<InterpolationBlock> getTreeForElement(ARTElement target, ReachedSet[] reachedSets, int threadNo) throws InterruptedException, CPAException{

    if (debug){
      System.out.println();
      System.out.println("--> Calling for "+target.getElementId()+" in thread "+threadNo+" <----");
    }

    assert !target.isDestroyed() && reachedSets[threadNo].contains(target);


    List<InterpolationBlock> rgResult = new ArrayList<InterpolationBlock>();


    // get the set of ARTElement that have been abstracted
    Path cfaPath = computePath(target, reachedSets[threadNo]);
    List<Triple<ARTElement, CFANode, RelyGuaranteeAbstractElement>> path = transformPath(cfaPath);

    if (debug){
      System.out.println("The error trace in thread "+threadNo+" is:\n"+cfaPath);
      System.out.print("Abstraction elements are: ");
      if (path.isEmpty()){
        System.out.println("none");
      } else {
        System.out.println();
      }
      for (Triple<ARTElement, CFANode, RelyGuaranteeAbstractElement> triple : path){
        System.out.println("- id:"+triple.getFirst().getElementId()+" at loc "+triple.getSecond());
      }
    }

    // the maximum number of primes that appears in any formula block
    int offset = 0;
    for (Triple<ARTElement, CFANode, RelyGuaranteeAbstractElement> triple : path){
      ARTElement artElement = triple.getFirst();
      assert triple.getThird() instanceof AbstractionElement;
      AbstractionElement rgElement = (AbstractionElement) triple.getThird();

      if (debug){
        printEnvEdgesApplied(artElement, rgElement.getOldEdgeMap().keySet());
      }

      // map : env edge applied -> the rest path formula of the env. trace
      Map<Integer, Integer> adjustmentMap = new HashMap<Integer, Integer>();

      // get the blocks for environmental transitions
      for(Integer primedNo: rgElement.getOldPrimedMap().keySet()){
        RelyGuaranteeCFAEdge rgEdge = rgElement.getOldPrimedMap().get(primedNo);
        ARTElement sourceElement = rgEdge.getSourceARTElement();
        int sourceThread = rgEdge.getSourceTid();

        // TODO caching
        List<InterpolationBlock> envPF = getTreeForElement(sourceElement, reachedSets, sourceThread);

        offset++;
        if (primedNo != offset){
          adjustmentMap.put(primedNo, offset);
        }
        // prime the blocks so paths are unique
        Pair<Integer, List<InterpolationBlock>> pair = primeInterpolationBlocks(envPF, offset);
        envPF = pair.getSecond();
        // remember the remaining blocks
        rgResult.addAll(envPF);
        offset = Math.max(offset, pair.getFirst());
      }

      PathFormula currentPF = rgElement.getAbstractionFormula().getBlockPathFormula();
      if (!adjustmentMap.isEmpty()){
        currentPF = pmgr.adjustPrimedNo(currentPF, adjustmentMap);
      }
      InterpolationBlock currentBlock = new InterpolationBlock(currentPF, 0, artElement, new ArrayDeque<Integer>());
      rgResult.add(currentBlock);

      // if the were no env. transitions and primeNo is zero, then the result should be equal to path formula constructed in the ordinary way{
      assert !rgElement.getOldPrimedMap().isEmpty() || currentPF.equals(rgElement.getAbstractionFormula().getBlockPathFormula());

    }
    return rgResult;
  }*/

  /**
   * Writes a DOT file for DAG representation of ARTs and environemntal transitions.
   */
  private void dumpDag(List<InterpolationDagNode> roots, String file) {
    FileWriter fstream;
    try {
      fstream = new FileWriter(file);
      BufferedWriter out = new BufferedWriter(fstream);;
      String s = DOTDagBuilder.generateDOT(roots);
      out.write(s);
      out.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Constructs DAGS that contain the ART error trace plus one of env. branches involved.
   * @param reachedSets
   * @param errorElem
   * @param tid
   * @return
   * @throws InterruptedException
   * @throws CPAException
   */
  public List<InterpolationDag> getDags(ReachedSet[] reachedSets, ARTElement errorElem, int tid) throws InterruptedException, CPAException {

    List<InterpolationDag> dags = new Vector<InterpolationDag>();

    // get the full DAG for the error element
    InterpolationDag mainDag = new InterpolationDag();
    IntegerWrapper newUnique = new IntegerWrapper(reachedSets.length);
    getRootsForElement(reachedSets, errorElem, tid, mainDag, newUnique);

    System.out.println();
    System.out.println("Main DAG:");
    for (Pair<Integer, Integer> key : mainDag.getNodeMap().keySet()){
      System.out.println(mainDag.getNodeMap().get(key));
    }


    if (debug){
      mainDag.writeToDOT(dagFilePredix+"_main.dot");
    }

    // get node on the path from the root to the error element
    InterpolationDagNode mainDagErrorNode = mainDag.getNode(tid, errorElem.getElementId());

    List<Pair<Integer, Integer>> errorPath = mainDag.getModularPathToNode(mainDagErrorNode);

    // remove all node for thread tid, that are not on the error path
    mainDag.retainNodesInThread(errorPath, tid);

    if (debug){
      mainDag.writeToDOT(dagFilePredix+"_reduced.dot");
    }

    // TODO extend it to >2 threads
    int otherTid = tid == 0 ? 1 : 0;
    List<List<Pair<Integer, Integer>>> branches = mainDag.getBranchesInThread(otherTid);


    if (branches.isEmpty()){
      // there is only the error thread
      dags.add(mainDag);
    }

    for (int i=0; i<branches.size(); i++){
      List<Pair<Integer, Integer>> branch = branches.get(i);
      InterpolationDag dag = new InterpolationDag(mainDag);
      dag.retainNodesInThread(branch, otherTid);

      /*
       *  Find env. transitions that couldn't have generated by this branch.
       *  Change application such that they are unsatisfiable.
       */
      List<RelyGuaranteeCFAEdge> toRemove = edgesOutOfThread(dag, otherTid);
      ListMultimap<InterpolationDagNode, RelyGuaranteeCFAEdge> appliedMap = dag.getAppliedEnvEdges(tid);

      for (InterpolationDagNode appNode : appliedMap.keySet()){
        PathFormula fc = makeUnsatApplicationConstraint(appNode, toRemove);

        if (!fc.getFormula().isTrue()){
          fc = pmgr.makeAnd(appNode.getPathFormula(), fc);
          dag.replacePathFormulaInNode(appNode, fc);
        }
      }

      /*
       * Do the same with the error branch.
       */
      toRemove.clear();
      appliedMap.clear();

      toRemove  = edgesOutOfThread(dag, tid);
      appliedMap = dag.getAppliedEnvEdges(otherTid);

      System.out.println();
      System.out.println("To remove "+toRemove);
      if (!toRemove.isEmpty()){
        System.out.println();
      }


      dags.add(dag);
    }

    return dags;
  }

  /**
   * Returns the list of env. transitions coming from thread tid, which couldn't have
   * be generated or covered by other transitions from this thread.
   * @param dag
   * @param tid
   * @return
   */
  private List<RelyGuaranteeCFAEdge> edgesOutOfThread(InterpolationDag dag, int tid) {
    // TODO works for two threads only
    assert dag.getRoots().size() <= 2;
    int otherThread = tid == 0 ? 1 : 0; // TODO -> otherTid

    List<RelyGuaranteeCFAEdge> toRemove = new Vector<RelyGuaranteeCFAEdge>();

    // get edges generate by thread tid
    ListMultimap<InterpolationDagNode, RelyGuaranteeCFAEdge> appliedMap = dag.getAppliedEnvEdges(otherThread);

    // get ssa map of tid
    List<InterpolationDagNode> leaves = dag.getLeavesInThread(tid);

    SSAMap topssa = SSAMap.emptySSAMap();
    for (InterpolationDagNode leaf : leaves){
      SSAMap leafssa = leaf.getPathFormula().getSsa();
      topssa = SSAMap.merge(topssa, leafssa);
    }

    // if an edge has ssa index higher than the source thread - then it out of thread
    for (RelyGuaranteeCFAEdge edge : appliedMap.values()){

      if (edge.getSourceTid() != tid){
        continue;
      }

      SSAMap edgessa = edge.getPathFormula().getSsa();
      boolean removeEdge = false;

      for (String var : edgessa.allVariables()){
        Pair<String, Integer> data = PathFormula.getPrimeData(var);
        if (data.getSecond() == tid){
          int edgeIdx = edgessa.getIndex(var);
          int branchIdx = topssa.getIndex(var);
          if (edgeIdx > branchIdx){
            removeEdge = true;
            break;
          }
        }
      }
      if (removeEdge && !toRemove.contains(edge)){
        toRemove.add(edge);
      }
    }

    return toRemove;
  }

  /**
   * Returns a path formula that makes all  application of env. edges from the list unsatisfiable.
   * @param node
   * @param toRemove
   * @return
   */
  private PathFormula makeUnsatApplicationConstraint(InterpolationDagNode node, List<RelyGuaranteeCFAEdge> toRemove) {

    PathFormula allPf = pmgr.makeEmptyPathFormula();

    for (RelyGuaranteeCFAEdge edge : toRemove){

      if (!node.getEnvPrimes().containsKey(edge)){
        continue;
      }

      // reconstruct how env. edge looks like in the node
      Integer oUniqueThis   = edge.getUniquePrimeThis();
      Integer oUniqueOther  = edge.getUniquePrimeOther();
      assert oUniqueThis    != null;
      assert oUniqueOther   != null;
      Integer nUniqueThis   = node.getEnvPrimes().get(edge);
      assert  nUniqueThis   != null;
      Integer nUniqueOther  = nUniqueThis+1;

      Map<Integer, Integer> adjustmentMap = new HashMap<Integer, Integer>();
      adjustmentMap.put(oUniqueThis, nUniqueThis);
      adjustmentMap.put(oUniqueOther, nUniqueOther);
      PathFormula appPf = pmgr.adjustPrimedNo(edge.getPathFormula(), adjustmentMap);

      Formula eeF      = fmgr.makeNot(appPf.getFormula());
      appPf            = new PathFormula(eeF,  appPf.getSsa(), appPf.getLength());
      allPf            = pmgr.makeAnd(allPf, appPf);
    }

    return allPf;
  }

  /**
   * Extends the given DAG by parts of ART and env. transition that lead to the target.
   * Env. transitions discovered are uniquely primed.
   * @param reachedSets
   * @param target
   * @param tid
   * @param dag
   * @param newUnique
   * @throws InterruptedException
   * @throws CPAException
   */
  public void getRootsForElement(ReachedSet[] reachedSets, ARTElement target, int tid, InterpolationDag dag, IntegerWrapper newUnique) throws InterruptedException, CPAException {
    assert !target.isDestroyed() && reachedSets[tid].contains(target);
    assert dag != null;

    if (debug){
      System.out.println();
      System.out.println("Constructing DAG node for ("+tid+" ,"+target.getElementId()+")");
    }

    // get the abstraction elements on the path from the root to the error element
    Path cfaPath = computePath(target, reachedSets[tid]);
    List<Triple<ARTElement, CFANode, RelyGuaranteeAbstractElement>> path = transformFullPath(cfaPath);

    // exit if the target has already been added to the target
    Triple<ARTElement, CFANode, RelyGuaranteeAbstractElement> lastTriple = path.get(path.size()-1);
    ARTElement lastARTElem = lastTriple.getFirst();
    if (dag.getNodeMap().containsKey(Pair.of(tid, lastARTElem.getElementId()))){
      if (debug){
        System.out.println("Done before.");
      }
      return;
    }

    if (debug){
      System.out.println("Elements on the error path:");
    }

    // previous DAG node
    InterpolationDagNode lastNode = null;

    for (Triple<ARTElement, CFANode, RelyGuaranteeAbstractElement> triple : path){
      ARTElement artElement = triple.getFirst();
      AbstractionElement rgElement = (AbstractionElement) triple.getThird();
      assert rgElement.tid == tid;

      // create a node or use a cached one for this element
      boolean newNode = false;
      InterpolationDagNode node = dag.getNode(tid, artElement.getElementId());
      if (node == null){
        node = new InterpolationDagNode(rgElement.getAbstractionFormula().getBlockPathFormula(), tid, artElement, tid);
        dag.getNodeMap().put(Pair.of(tid, artElement.getElementId()), node);
        newNode = true;

        if (lastNode == null){
          // add the root
          assert !dag.getRoots().contains(node);
          dag.getRoots().add(node);
        } else {
          // make the previous node a parent of this one
          if (lastNode.getChildren().contains(node)){
            System.out.println("DEBUG: "+node+", "+lastNode);
          }
          assert !lastNode.getChildren().contains(node);
          lastNode.getChildren().add(node);
          assert !node.getParents().contains(lastNode);
          node.getParents().add(lastNode);
        }
      }

      if(debug && newNode){
        System.out.println("\t-"+node.toString()+" (new)");
      }
      if(debug && !newNode){
        System.out.println("\t-"+node.toString()+" (cached)");
      }



      // how to rename variables in the path formula
      Map<Integer, Integer> adjustmentMap = new HashMap<Integer, Integer>();

      if (debug && !rgElement.getOldEdgeMap().keySet().isEmpty() ){
        System.out.print("\t env. tr. from id:");
        for(RelyGuaranteeCFAEdge rgEdge : rgElement.getOldEdgeMap().keySet()){
          ARTElement sourceARTElement = rgEdge.getSourceARTElement();
          System.out.print(sourceARTElement.getElementId()+" ");
        }
        System.out.println();
      }

      // rename env. transitions to their source thread numbers
      for(RelyGuaranteeCFAEdge rgEdge : rgElement.getOldEdgeMap().keySet()){
        ARTElement sourceARTElement = rgEdge.getSourceARTElement();
        Integer sourceTid           = rgEdge.getSourceTid();
        assert sourceTid != tid;

        // construct missing nodes for the source element
        getRootsForElement(reachedSets, sourceARTElement, sourceTid, dag, newUnique);

        // get nodes for the last abstraction for the source element.
        Path envCfaPath = computePath(sourceARTElement, reachedSets[sourceTid]);
        List<Triple<ARTElement, CFANode, RelyGuaranteeAbstractElement>> envPath = transformFullPath(envCfaPath);
        ARTElement sourceAbstrElement = envPath.get(envPath.size()-1).getFirst();

        InterpolationDagNode envNode = dag.getNode(sourceTid, sourceAbstrElement.getElementId());
        assert envNode != null;


        // envNode is a parent of node
        if (!envNode.getChildren().contains(node)){
          envNode.getChildren().add(node);
          assert !node.getParents().contains(envNode);
          node.getParents().add(envNode);
        }

        int currentUniqueThis   = rgEdge.getUniquePrimeThis();
        int currentUniqueOther  = rgEdge.getUniquePrimeOther();
        adjustmentMap.put(currentUniqueThis, newUnique.getValue());
        adjustmentMap.put(currentUniqueOther, newUnique.getValue()+1);
        node.getEnvPrimes().put(rgEdge, newUnique.getValue());
        newUnique.setValue(newUnique.getValue()+2);

      }

      // rename the formula if the node is new
      if (newNode && !adjustmentMap.isEmpty()){
        PathFormula adjustedPf = pmgr.adjustPrimedNo(rgElement.getAbstractionFormula().getBlockPathFormula(), adjustmentMap);
        node = dag.replacePathFormulaInNode(node, adjustedPf);
      }

      lastNode = node;
    }

    if (debug){
      dag.dagAssertions();
    }
  }

  /**
   * Mutable integer object.
   */
  class IntegerWrapper {
    private int i;

    public IntegerWrapper(int i){
      this.i = i;
    }

    public int getValue() {
      return i;
    }

    public void setValue(int pI) {
      i = pI;
    }
  }

  /**
   * Returns a DAG representation of the formula trances that are involved in reaching the target state.
   * @param errorElement
   * @param reachedSets
   * @param errorTid
   * @return
   * @throws InterruptedException
   * @throws CPAException
   */
  /* public Pair<List<InterpolationDagNode>,  Multimap<Integer, Integer>> getDagForElement(ARTElement errorElement, ReachedSet[] reachedSets, Integer errorTid) throws InterruptedException, CPAException {
    Map<Pair<Integer, Integer>, InterpolationDagNode> nodeMap = new HashMap<Pair<Integer, Integer>, InterpolationDagNode>();
    Multimap<Integer, Integer> traceMap = HashMultimap.create();
    return Pair.of(getDagForElement(errorElement, reachedSets, errorTid, nodeMap, traceMap).getFirst(), traceMap);
  }*/


  /**
   * Returns a DAG representation of the formula trances that are involved in reaching the target state.
   * @param errorElement
   * @param reachedSets
   * @param threadNo
   * @param context
   * @return
   * @throws CPAException
   * @throws InterruptedException
   */
  /* public Triple<List<InterpolationDagNode>, ARTElement, Integer> getDagForElement(ARTElement target, ReachedSet[] reachedSets, Integer tid, Map<Pair<Integer, Integer>, InterpolationDagNode> nodeMap, Multimap<Integer, Integer> traceMap) throws InterruptedException, CPAException{

    assert !target.isDestroyed() && reachedSets[tid].contains(target);

    if (debug){
      System.out.println();
      System.out.println("Constructing DAG for id:"+target.getElementId()+" in thread "+tid);
    }

    // TODO change roots to a set
    // the result
    List<InterpolationDagNode> roots = new Vector<InterpolationDagNode>(1);

    // get the abstraction elements on the path from the root to the target
    Path cfaPath = computePath(target, reachedSets[tid]);
    List<Triple<ARTElement, CFANode, RelyGuaranteeAbstractElement>> path = transformFullPath(cfaPath);

    InterpolationDagNode predecessorNode  = null;
    boolean predecessorCached             = false;
    boolean newBranch                     = false;
    // id of the current trace
    Integer traceNo = null;

    if (debug){
      System.out.println("Elements on the path:");
    }

    for (Triple<ARTElement, CFANode, RelyGuaranteeAbstractElement> triple : path) {
      ARTElement artElement = triple.getFirst();
      AbstractionElement rgElement = (AbstractionElement) triple.getThird();

      InterpolationDagNode node = null;
      // specifies how to change prime numbers in path formulas
      Map<Integer, Integer> adjustmentMap = new HashMap<Integer, Integer>(rgElement.getPrimedMap().size());

      // add the element to the DAG, if it's not there already
      Pair<Integer,Integer> dagkey = new Pair<Integer,Integer>(tid,artElement.getElementId());

      if (nodeMap.containsKey(dagkey)){
        // use the existing node,
        node = nodeMap.get(dagkey);

        predecessorCached = true;
        traceNo = node.getTraceNo();

        // if node is a root, then add it to 'roots' list
        if (predecessorNode == null){
          roots.add(node);
        }

        if(debug){
          System.out.println("\t-"+node.toString()+" (cached)");
        }

      } else {
        // create a new node
        if (predecessorNode == null){
          // this is the first branch discovered in this thread or we have branched from another thread
          traceNo = traceMap.values().size();
          traceMap.put(tid, traceNo);
        } else if (predecessorCached){
          // create a new branch if the predecessor was cached and it already has some children in the same thread
          for (InterpolationDagNode child : predecessorNode.getChildren()){
            if (child.getTid() == tid){
              traceNo   = traceMap.values().size();
              traceMap.put(tid, traceNo);
              newBranch = true;
            }
          }
        } else {
          // we continue in the same branch
        }

        predecessorCached = false;

        // formulas with no primes becomes 'traceNo' times primed
        adjustmentMap.put(0, traceNo);

        assert traceNo != null;

        List<InterpolationDagNode> children = new Vector<InterpolationDagNode>();
        List<InterpolationDagNode> parents = new Vector<InterpolationDagNode>();
        PathFormula pf = rgElement.getAbstractionFormula().getBlockPathFormula();
        node = new InterpolationDagNode(pf, traceNo, artElement, children, parents, tid);
        nodeMap.put(dagkey, node);

        // if node is a root, then add it to 'roots' list
        if (predecessorNode == null){
          roots.add(node);
        }

        // add the current node as a child to the predecessor
        if (predecessorNode != null) {
          assert !predecessorNode.getChildren().contains(node);
          predecessorNode.getChildren().add(node);
          assert !node.getParents().contains(predecessorNode);
          node.getParents().add(predecessorNode);
        }

        if(debug){
          System.out.println("\t-"+node.toString()+" (new)");
        }

        if (debug && !rgElement.getOldPrimedMap().values().isEmpty() ){
          for(RelyGuaranteeCFAEdge rgEdge : rgElement.getOldPrimedMap().values()){
            ARTElement sourceARTElement = rgEdge.getSourceARTElement();
            System.out.println("\t env. tr. from id:"+sourceARTElement.getElementId()+" was applied");
          }
        }


        // get a node for every source of environmental transitions applied
        // envPrimeNo is the prime number of the environmental path formula fragment in the current formula block
        for(Integer envPrimeNo : rgElement.getOldPrimedMap().keySet()){
          RelyGuaranteeCFAEdge rgEdge = rgElement.getOldPrimedMap().get(envPrimeNo);
          ARTElement sourceARTElement = rgEdge.getSourceARTElement();
          Integer sourceTid           = rgEdge.getSourceTid();

          // recursively call getDagForElement to get the DAG source of the transition
          Triple<List<InterpolationDagNode>, ARTElement, Integer> envPair = getDagForElement(sourceARTElement, reachedSets, sourceTid, nodeMap, traceMap);
          List<InterpolationDagNode> envRoots = envPair.getFirst();
          ARTElement sourceAbstractionElement = envPair.getSecond();
          Integer envTraceNo                  = envPair.getThird();

          // add new roots
          for (InterpolationDagNode root : envRoots){
            if (!roots.contains(root)){
              roots.add(root);
            }
          }

          // get the node that created the transition
          Pair<Integer, Integer> key      = Pair.of(sourceTid, sourceAbstractionElement.getElementId());
          InterpolationDagNode sourceNode = nodeMap.get(key);
          assert sourceNode != null;


          // make an edge from the source node to the application
          List<InterpolationDagNode> envChildren = sourceNode.getChildren();
          if (!envChildren.contains(node)){
            envChildren.add(node);
            assert !node.getParents().contains(sourceNode);
            node.getParents().add(sourceNode);
          }

          // TODO write comment
          assert !rgElement.getOldPrimedMap().containsKey(envPrimeNo+1);
          adjustmentMap.put(envPrimeNo+1, envTraceNo);

          int freshTraceNo = traceMap.values().size();
          traceMap.put(tid, freshTraceNo);
          adjustmentMap.put(envPrimeNo, freshTraceNo);
        }

        // adjust the prime numbers
        PathFormula adjustedPf = pmgr.adjustPrimedNo(node.getPathFormula(), adjustmentMap);

        // TODO put in path formula manager
        // if it is an new branch then add equalities that link the last indexes in the previous node
        // with the the variables in the new branch
        if (newBranch){
          assert traceNo > 0;
          assert predecessorNode != null;
          predecessorCached = false;
          PathFormula predPf  = predecessorNode.getPathFormula();
          Integer predTraceNo = predecessorNode.getTraceNo();

          SSAMapBuilder ssaBuilder = adjustedPf.getSsa().builder();
          // formula with equalities
          Formula eq  = fmgr.makeTrue();
          // get the equalities
          for (String pVarName : predPf.getSsa().allVariables()){
            Pair<String, Integer> data = PathFormula.getPrimeData(pVarName);
            if (data.getSecond() == predTraceNo){
              Integer idx     = predPf.getSsa().getIndex(pVarName);
              String  varName = data.getFirst()+"^"+traceNo;
              Formula pVar    = fmgr.makeVariable(pVarName, idx);
              Formula var     = fmgr.makeVariable(varName, idx);
              Formula newEq   = fmgr.makeEqual(pVar, var);
              eq              = fmgr.makeAnd(eq, newEq);

              // add SSA index if necessary
              assert ssaBuilder.getIndex(pVarName) == -1;
              ssaBuilder.setIndex(pVarName, idx);
            }
          }

          PathFormula eqPf  = new PathFormula(eq, ssaBuilder.build(), 0);
          adjustedPf        = pmgr.makeAnd(adjustedPf, eqPf);

          if (debug){
            System.out.println("\t equalities "+eqPf);
          }
        }

        // TODO should be uncommented to work
        //node.setPathFormula(adjustedPf);

        if (debug && traceNo>0){
          assert adjustedPf.getFormula().isFalse() || adjustedPf.getFormula().isTrue() || adjustedPf.toString().contains("^"+traceNo);
        }
      }

      predecessorNode = node;
      newBranch       = false;
    }

    assert roots.size() <= reachedSets.length;

    if (debug){
      System.out.println();
    }

    assert (!roots.isEmpty() && predecessorNode != null && traceNo != null);

    return Triple.of(roots, predecessorNode.getArtElement(), traceNo);


  }*/

  /**
   * Check the correctness of a Dag
   * @param roots
   */
  void dagAssertions(List<InterpolationDagNode> roots){
    for (InterpolationDagNode node : roots){
      Integer traceNo = node.getTraceNo();
      PathFormula pf = node.getPathFormula();
      // check if the traceNo is OK
      if (traceNo > 0){

        //assert pf.getFormula().isFalse() || pf.getFormula().isTrue() || pf.toString().contains("^"+traceNo);
        // check if traceNo appears in the children of node
        for (InterpolationDagNode child : node.getChildren()){

          PathFormula childPf = child.getPathFormula();
          //assert childPf.getFormula().isFalse() || childPf.getFormula().isTrue() || childPf.toString().contains("^"+traceNo);
        }
      }
      // check parent - children relationships
      for (InterpolationDagNode child : node.getChildren()){
        if (!child.getParents().contains(node)){
          System.out.println();
        }
        assert child.getParents().contains(node);
      }

      dagAssertions(node.getChildren());
    }
  }

  /**
   * Prime interpolation block given number of times. Returns prime blocks and the maximum prime number after the operation.
   * @param pEnvPF
   * @param pOffset
   * @return
   */
  private Pair<Integer, List<InterpolationBlock>> primeInterpolationBlocks(List<InterpolationBlock> envPF, int offset) {
    int maximumPrimedNo = 0;
    List<InterpolationBlock> primedBlocks = new Vector<InterpolationBlock>();
    for (InterpolationBlock ib : envPF){
      PathFormula newPF = pmgr.primePathFormula(ib.getPathFormula(), offset);
      int newTraceNo = ib.getTraceNo()+offset;
      // create a new context stack
      Deque<Integer> oldContext = ib.getContext();
      Deque<Integer> newContext = new ArrayDeque<Integer>();
      assert oldContext != null;
      for (int item : oldContext){
        newContext.addFirst(item+offset);
      }
      newContext.addFirst(0);

      maximumPrimedNo = Math.max(maximumPrimedNo, newPF.getPrimedNo());
      ARTElement artElement = ib.getArtElement();
      primedBlocks.add(new InterpolationBlock(newPF, newTraceNo, artElement, newContext));
    }
    return Pair.of(maximumPrimedNo, primedBlocks);
  }

  /**
   * Returns the maximum number of primes that appear in any block.
   * @param pEnvPF
   * @return
   */
  /* private int getPrimedNo(List<InterpolationBlock> envPF) {
    int offset = 0;
    for (InterpolationBlock ib : envPF){
      offset = Math.max(offset, ib.getPrimedNo());
    }
    return offset;
  }*/

  // TODO For testing
  private void printEnvEdgesApplied(ARTElement artElement, Collection<RelyGuaranteeCFAEdge>  set) {

    if (!set.isEmpty()){
      System.out.println("Env edges applied at id:"+artElement.getElementId());
      for (RelyGuaranteeCFAEdge edge : set){
        System.out.println("- edge "+edge );
      }
    }
  }


  /**
   * Check correctnes of interpolationMaps
   * @param interpolationMaps
   * @param interpolationPathFormulas
   * @param mark
   */
  private void assertionInterpolationMaps(Map<Integer, InterpolationMap> interpolationMaps, List<InterpolationBlock> interpolationPathFormulas, int mark) {
    // check that every block is covered by one and only one interpolation map
    int sum = 0;
    for (Integer key: interpolationMaps.keySet()){
      sum = sum + interpolationMaps.get(key).intMap.values().size();
    }
    // last block is ommited
    assert sum == interpolationPathFormulas.size()-1;
    // a map always has asks for interpolant for the last formula
    for (Integer key : interpolationMaps.keySet()){
      InterpolationMap interpolationMap = interpolationMaps.get(key);

      assert interpolationMap.intMap.containsKey(interpolationMap.formulaList.size()-1);
    }

  }

  private <T> CounterexampleTraceInfo interpolateDagsMathsat(ARTElement targetElement,  ReachedSet[] reachedSets, int tid , InterpolatingTheoremProver<T> itpProver, RelyGuaranteeRefinerStatistics stats) throws CPAException, InterruptedException{

    stats.formulaTimer.start();

    // get DAGs for the error trace and env. branches involved
    stats.formulaTimer.start();
    List<InterpolationDag> dags = getDags(reachedSets, targetElement, tid);
    stats.formulaTimer.stop();

    assert !dags.isEmpty();

    if (debug){
      for (int i=0; i<dags.size(); i++){
        dags.get(i).writeToDOT(dagFilePredix+"_dag"+i+".dot");
        dagAssertions(dags.get(i).getRoots());
      }
    }

    CounterexampleTraceInfo info = new CounterexampleTraceInfo();
    // interpolate DAGs
    int i=0;
    do {
      InterpolationDag dag = dags.get(i);
      DAGInterpolationResult res = interpolateDag(dag, itpProver, stats);
      if (res.isSpurious()){
        for (ARTElement artElem : res.getPredMap().keys()){
          info.addPredicatesForRefinement(artElem, res.getPredMap().get(artElem));
        }
      } else {
        info = new CounterexampleTraceInfo(false);
        break;
      }

      i++;
    } while(i<dags.size());

    return info;
  }

  private <T> DAGInterpolationResult interpolateDag(InterpolationDag dag, InterpolatingTheoremProver<T> itpProver, RelyGuaranteeRefinerStatistics stats) throws InterruptedException{

    List<InterpolationDagNode> topNodes = topSortDag(dag.getRoots());

    if (debug){
      System.out.println();
      System.out.println("Interpolation formulas - element id [thread, trace, location]: formula, env. formulas");
      int j=0;
      for (InterpolationDagNode node : topNodes){
        CFANode loc = AbstractElements.extractLocation(node.getArtElement());
        System.out.println("\t-id:"+node.getArtElement().getElementId()+" [t"+node.getTid()+", "+node.getTraceNo()+", "+loc+"]:\t"+node.getPathFormula());
        j++;
      }
    }

    refStats.cexAnalysisSolverTimer.start();

    List<T> interpolationIds      = new Vector<T>(topNodes.size());
    boolean spurious  = false;
    DAGInterpolationResult result = null;

    if (debug){
      System.out.println();
      System.out.println("Interpolants, predicates:");
    }

    // get interpolants in the order of topological sort
    int i=0;
    while(i<topNodes.size()){
      InterpolationDagNode node = topNodes.get(i);

      // get formula list, where
      //Pair<List<InterpolationDagNode>, Integer> pair = parentSortDag(roots, node);
      Pair<List<Formula>, Integer> pair = parentSortDag2(dag.getRoots(), node);
      //Pair<List<Formula>, Integer> pair = childrenSortDag(roots, node);

      // prepare formula list
      stats.interpolationTimer.start();
      itpProver.init();
      for (Formula fr : pair.getFirst()){
        T id = itpProver.addFormula(fr);
        interpolationIds.add(id);
      }

      // check if spurious
      spurious = itpProver.isUnsat();
      stats.unsatChecks++;

      if (!spurious){
        // Feasible error path
        assert result == null;
        result = new DAGInterpolationResult(Boolean.FALSE);
        itpProver.reset();
        stats.interpolationTimer.stop();

        if (debug){
          System.out.println("\tFeasible error trace.");
        }
        break;

      } else {
        // spurious counterexample, get interpolants for 'node'
        ARTElement artElement = node.getArtElement();
        CFANode loc = AbstractElements.extractLocation(artElement);
        int idx = pair.getSecond();

        Formula itp = itpProver.getInterpolant(interpolationIds.subList(0, idx+1));
        // non-modular predicates
        // Set<AbstractionPredicate> predicates = getDagPredicates(itp, node.getTid(), traceMap);
        // try-to-be modular predicates
        Set<AbstractionPredicate> preds = getDagsPredicates(itp, node);

        itpProver.reset();
        stats.interpolationTimer.stop();

        if (debug){
          System.out.println("\t-id:"+artElement.getElementId()+" "+itp+", "+preds);
        }

        if (result == null){
          result = new DAGInterpolationResult(Boolean.TRUE);
        }

        // add predicates
        result.addPredicate(node.getArtElement(), preds);

        if (itp.isFalse()){
          i = topNodes.size();
        } else {
          // replace
          PathFormula oldPf = node.getPathFormula();
          PathFormula newPf = new PathFormula(itp, oldPf.getSsa(), oldPf.getLength());
          dag.replacePathFormulaInNode(node, newPf);
        }
        i++;
      }
    }

    refStats.cexAnalysisTimer.stop();

    return result;
  }



  /**
   * Counterexample or interpolates.
   * @param pAbstractTrace
   * @param pElementsOnPath
   * @param pFirstItpProver
   * @return
   */
  /*private <T> CounterexampleTraceInfo interpolateDagMathsat(ARTElement targetElement,  ReachedSet[] reachedSets, int tid , InterpolatingTheoremProver<T> itpProver, RelyGuaranteeRefinerStatistics stats) throws CPAException, InterruptedException{
    logger.log(Level.FINEST, "Building counterexample trace");

    // get the DAG representation of the abstractions and env. transitions involved
    stats.formulaTimer.start();
    Pair<List<InterpolationDagNode>, Multimap<Integer, Integer>> dag = getDagForElement(targetElement, reachedSets, tid);
    List<InterpolationDagNode> roots = dag.getFirst();
    Multimap<Integer, Integer> traceMap = dag.getSecond();

    assert roots.size() >= 1 && roots.size() <= reachedSets.length;

    if (debug){
      dumpDag(roots, dagFilePredix);
      dagAssertions(roots);
    }

    // get the nodes in topological order
    //List<InterpolationDagNode> topNodes = topSortDag(roots);
    List<InterpolationDagNode> topNodes = topSortDag(roots);

    stats.formulaTimer.stop();
    stats.formulaNo = topNodes.size();

    if (debug){
      System.out.println();
      System.out.println("Interpolation formulas - element id [thread, trace]: formula, env. formulas");
      int j=0;
      for (InterpolationDagNode node : topNodes){
        System.out.println("\t-id:"+node.getArtElement().getElementId()+" [t"+node.getTid()+", "+node.getTraceNo()+"]: "+node.getPathFormula());
        j++;
      }
    }

    refStats.cexAnalysisSolverTimer.start();

    CounterexampleTraceInfo info  = null;
    List<T> interpolationIds      = new Vector<T>(topNodes.size());
    boolean spurious  = false;

    if (debug){
      System.out.println();
      System.out.println("Interpolants, predicates:");
    }

    // get interpolants in the order of topological sort
    int i=0;
    while(i<topNodes.size()){
      InterpolationDagNode node = topNodes.get(i);

      // get formula list, where
      //Pair<List<InterpolationDagNode>, Integer> pair = parentSortDag(roots, node);
      Pair<List<Formula>, Integer> pair = parentSortDag2(roots, node);
      //Pair<List<Formula>, Integer> pair = childrenSortDag(roots, node);

      // prepare formula list
      stats.interpolationTimer.start();
      itpProver.init();
      for (Formula fr : pair.getFirst()){
        T id = itpProver.addFormula(fr);
        interpolationIds.add(id);
      }

      // check if spurious
      spurious = itpProver.isUnsat();
      stats.unsatChecks++;

      if (!spurious){
        // Feasible error path
        assert info == null;
        info = new CounterexampleTraceInfo(false);
        itpProver.reset();
        stats.interpolationTimer.stop();

        if (debug){
          System.out.println("\tFeasbile error trace.");
        }
        break;

      } else {
        // spurious counterexample, get interpolants for 'node'
        ARTElement artElement = node.getArtElement();
        CFANode loc = AbstractElements.extractLocation(artElement);
        int idx = pair.getSecond();

        Formula itp = itpProver.getInterpolant(interpolationIds.subList(0, idx+1));
        // non-modular predicates
        // Set<AbstractionPredicate> predicates = getDagPredicates(itp, node.getTid(), traceMap);
        // try-to-be modular predicates
        ListMultimap<ARTElement, AbstractionPredicate> predMap = getModularDagPredicates(itp, node);

        itpProver.reset();
        stats.interpolationTimer.stop();

        if (debug){
          //System.out.println("\t-id:"+artElement.getElementId()+"\t ["+loc+"]: "+itp+",\t"+predicates);

          if (predMap.keySet().isEmpty()){
            System.out.println("\t-id:"+artElement.getElementId()+" itp "+itp);
          } else {
            System.out.println("\t-id:"+artElement.getElementId()+" itp "+itp+"  predicates: ");
          }
          for (ARTElement artElem : predMap.keySet()){
            System.out.println("\t\tid:"+artElem.getElementId()+", "+loc+", "+predMap.get(artElem));
          }
        }


        if (info == null){
          info = new CounterexampleTraceInfo();
        }

        // info.addPredicatesForRefinement(artElement, predicates);
        for (ARTElement artElem : predMap.keySet()){
          info.addPredicatesForRefinement(artElem, predMap.get(artElem));
        }


        if (itp.isFalse()){
          i = topNodes.size();
        } else {
          // TODO the line below should be uncomment for thsi to work
          //roots = replaceDag(roots, node, itp);
        }
        i++;
      }
    }

    refStats.cexAnalysisTimer.stop();

    return info;
  }*/


  // TODO do sth
  /* List<InterpolationDagNode> replaceDag(List<InterpolationDagNode> roots, InterpolationDagNode node, Formula itp){
    // replace node by itp
    PathFormula oldPf = node.getPathFormula();
    PathFormula newPf = new PathFormula(itp, oldPf.getSsa(), oldPf.getLength());
    node.setPathFormula(newPf);

    return roots;
  }*/

  /*List<InterpolationDagNode> replaceDag2(List<InterpolationDagNode> roots, InterpolationDagNode node, Formula itp){
    // get parents
    List<InterpolationDagNode> parents = new Vector<InterpolationDagNode>();
    parents.addAll(node.getParents());

    int i=0;
    while(i<parents.size()){
      InterpolationDagNode n = parents.get(i);
      for (InterpolationDagNode parent : n.getParents()){
        if (!parents.contains(parent)){
          parents.add(parent);
        }
      }
      i++;
    }


    // remove parents
    for (InterpolationDagNode n: parents){
      for (InterpolationDagNode child : n.getChildren()){
        child.getParents().remove(n);
      }
      n.getChildren().clear();
    }

    // replace node by itp
    PathFormula oldPf = node.getPathFormula();
    PathFormula newPf = new PathFormula(itp, oldPf.getSsa(), oldPf.getLength());
    node.setPathFormula(newPf);

    // exchange roots
    List<InterpolationDagNode> newRoots = new Vector<InterpolationDagNode>(roots.size());
    for (InterpolationDagNode root : roots){
      if (parents.contains(root)){
        if (!newRoots.contains(node)){
          newRoots.add(node);
        }
      } else {
        newRoots.add(root);
      }
    }

    if (debug){
      dagAssertions(newRoots);
    }

    return newRoots;
  }*/

  /**
   * Return the nodes of the DAG in topogical order.
   */
  private List<InterpolationDagNode> topSortDag(List<InterpolationDagNode> roots) {

    Set<InterpolationDagNode> visited = new HashSet<InterpolationDagNode>();
    List<InterpolationDagNode> topList = new Vector<InterpolationDagNode>();
    topList.addAll(roots);

    int i=0;
    while(i<topList.size()){
      InterpolationDagNode node = topList.get(i);
      for (InterpolationDagNode child : node.getChildren()){
        if (!visited.contains(child) && topList.containsAll(child.getParents())){
          visited.add(child);
          topList.add(child);
        }
      }
      i++;
    }

    return topList;
  }



  private Pair<List<Formula>, Integer> childrenSortDag(List<InterpolationDagNode> roots, InterpolationDagNode target){

    List<InterpolationDagNode> children = new Vector<InterpolationDagNode>();
    Set<InterpolationDagNode> visisted = new HashSet<InterpolationDagNode>();
    Deque<InterpolationDagNode> toProcess = new LinkedList<InterpolationDagNode>();
    List<Formula> formulas = new Vector<Formula>();
    Integer idx = null;

    toProcess.addAll(target.getChildren());
    // find all nodes that can be reached from target
    while(!toProcess.isEmpty()){
      InterpolationDagNode node = toProcess.poll();
      if (!visisted.contains(node)){
        visisted.add(node);
        children.add(node);
        for (InterpolationDagNode child : node.getChildren()){
          toProcess.add(child);
        }
      }
    }


    idx = children.size()-1;

    // add children
    if (false){
      System.out.println();
      System.out.println("Children of "+target.getArtElement().getElementId()+": ");
      for (InterpolationDagNode child : children){
        System.out.print("["+child.getArtElement().getElementId()+"], ");
      }
      System.out.println();
      System.out.println("Rest: ");
    }


    // add the rest of the nodes
    visisted.clear();
    assert toProcess.isEmpty();
    toProcess.addAll(roots);

    while (!toProcess.isEmpty()){
      InterpolationDagNode node = toProcess.poll();
      if (!visisted.contains(node)){
        visisted.add(node);

        if (!children.contains(node)){
          if (false){
            System.out.print("["+node.getArtElement().getElementId()+"], ");
          }

          formulas.add(node.getPathFormula().getFormula());
        }

        for (InterpolationDagNode child : node.getChildren()){
          toProcess.add(child);
        }
      }
    }

    if (false){
      System.out.println();
    }


    idx = formulas.size() - 1;

    // add children

    for (InterpolationDagNode child : children){
      formulas.add(child.getPathFormula().getFormula());
    }




    return Pair.of(formulas, idx);
  }



  private Pair<List<Formula>, Integer> parentSortDag2(List<InterpolationDagNode> roots, InterpolationDagNode target) {
    boolean print = false && debug;

    List<Formula> pcList = new Vector<Formula>();
    Set<InterpolationDagNode> visitedUpTo = new HashSet<InterpolationDagNode>();
    Deque<InterpolationDagNode> toProcess = new LinkedList<InterpolationDagNode>();
    int idx = -1;

    if (print){
      System.out.print("(");
    }

    toProcess.addLast(target);;
    // add the target and its parents
    while(!toProcess.isEmpty()){
      InterpolationDagNode node = toProcess.poll();
      if (!visitedUpTo.contains(node)){
        visitedUpTo.add(node);
        // add block path formula
        pcList.add(node.getPathFormula().getFormula());
        if (print){
          System.out.print(node.getArtElement().getElementId()+" ");
        }
        idx++;

        // process parents
        for (InterpolationDagNode parent : node.getParents()){
          toProcess.addLast(parent);
        }
      }
    }
    if (print){
      System.out.print("), (");
    }


    // add the rest of the nodes
    toProcess.clear();
    toProcess.addAll(roots);
    Set<InterpolationDagNode> visitedAfter = new HashSet<InterpolationDagNode>();


    while(!toProcess.isEmpty()){
      InterpolationDagNode node = toProcess.poll();
      if (!visitedAfter.contains(node)){
        visitedAfter.add(node);

        if (!visitedUpTo.contains(node)){
          pcList.add(node.getPathFormula().getFormula());
          if (print){
            System.out.print(node.getArtElement().getElementId()+" ");
          }
        }

        for (InterpolationDagNode child : node.getChildren()){
          toProcess.addLast(child);
        }
      }
    }

    if (print){
      System.out.println(")");
    }
    return Pair.of(pcList, idx);
  }

  /**
   *
   * @param roots
   * @param target
   * @return
   */
  private Pair<List<InterpolationDagNode>, Integer> parentSortDag(List<InterpolationDagNode> roots, InterpolationDagNode target) {

    List<InterpolationDagNode> pcList = new Vector<InterpolationDagNode>();
    Set<InterpolationDagNode> visited = new HashSet<InterpolationDagNode>();

    if (roots.isEmpty()) {
      return Pair.of(pcList, -1);
    }

    pcList.add(target);
    visited.add(target);

    // add the target and its parents
    int i=0;
    while(i<pcList.size()){
      InterpolationDagNode node = pcList.get(i);
      for (InterpolationDagNode parent : node.getParents()){
        if (!visited.contains(parent)){
          visited.add(parent);
          pcList.add(parent);
        }
      }
      i++;
    }

    // add the rest of nodes in any order
    List<InterpolationDagNode> toProcess  = new Vector<InterpolationDagNode>();
    toProcess.addAll(roots);
    int j=0;
    while(j<toProcess.size()){
      InterpolationDagNode next = toProcess.get(j);
      if (!pcList.contains(next)){
        pcList.add(next);
      }
      for (InterpolationDagNode child : next.getChildren()){
        toProcess.add(child);
      }
      j++;
    }

    return Pair.of(pcList, i-1);
  }




  /**
   * Return the nodes of the DAG in topogical order.
   */
  private Pair<List<InterpolationDagNode>, Integer> pcSortDag(List<InterpolationDagNode> roots) {

    List<InterpolationDagNode> pcList = new Vector<InterpolationDagNode>();
    if (roots.isEmpty()) {
      return Pair.of(pcList, 0);
    }

    InterpolationDagNode node = roots.get(0);

    Integer tid = roots.get(0).getTid();
    int i=0;
    while(node!=null){
      // check if all parents belong to the same thread
      boolean breakWhile = false;
      for (InterpolationDagNode parent : node.getParents()){
        if (parent.getTid() != tid){
          breakWhile = true;
          break;
        }
      }

      if(breakWhile){
        break;
      }

      pcList.add(node);

      // parent are OK, add the first child
      InterpolationDagNode nextNode = null;
      if (!node.getChildren().isEmpty()){
        nextNode = node.getChildren().get(0);
      }

      node = nextNode;

      i++;
    }



    return Pair.of(pcList, i-1);
  }

  /**
   * Counterexample or interpolates.
   * @param pAbstractTrace
   * @param pElementsOnPath
   * @param pFirstItpProver
   * @return
   */
  /*private <T> CounterexampleTraceInfo interpolateTreeMathsat(ARTElement targetElement,  ReachedSet[] reachedSets, int threadNo , InterpolatingTheoremProver<T> itpProver, RelyGuaranteeRefinerStatistics stats) throws CPAException, InterruptedException{
    logger.log(Level.FINEST, "Building counterexample trace");

    // get the rely guarantee path for the element
    stats.formulaTimer.start();
    List<InterpolationBlock> interpolationBlocks = getTreeForElement(targetElement, reachedSets, threadNo);

    stats.formulaTimer.stop();
    stats.formulaNo = interpolationBlocks.size();

    if (debug){
      System.out.println();
      System.out.println("Interpolation block [trace no, context no, abstraction element]: formulas:");
      int j=0;
      for (InterpolationBlock ib : interpolationBlocks){
        System.out.println("\t- blk "+j+" "+ib);
        j++;
      }
    }

    refStats.cexAnalysisSolverTimer.start();

    // prepare the initial list of formulas
    stats.interpolationTimer.start();
    itpProver.init();
    List<T> interpolationIds= new Vector<T>(interpolationBlocks.size());
    for (InterpolationBlock ib : interpolationBlocks){
      T id = itpProver.addFormula(ib.getPathFormula().getFormula());
      interpolationIds.add(id);
    }

    CounterexampleTraceInfo info;
    boolean spurious = itpProver.isUnsat();
    stats.unsatChecks++;

    if (spurious){
      /* Here interpolants are computed.
   *
   * To obtain well-scoped predicates, we use the technique described in Section 5.2 of
   * "Predicates from Proofs" by Henzinger et al. The prover may not generate the
   * strongest interpolants, therefore when we switch to an environmental formula trace,
   * we substitute the part of the main trace covered so far by the the interpolant at
   * the end of the part.
   *
   * In the example below, Psi is the interpolant for (A, B^C^D). Then, the interpolant
   * for B is computed as (B, Psi^C^D) and for C is (A^B^C, D) or equivalently (B^Psi^C, D).
   *
   *                 |
   *                 B  environmental thread
   *                 |
   *                ---
   *                 |
   *         --D--|--C--|--A-- main thread
   *                   Psi
   */
  /*
      info = new CounterexampleTraceInfo();
      // the last formula's trace no.
      int previousPrimedNo = 0;
      // how shorter is the current formula list compared to the original one
      int offset = 0;
      // itpContext stores last interpolants from other traces
      // map: trace no. -> last interpolant from that trace
      SortedMap<Integer, Formula> itpContext = new TreeMap<Integer, Formula>();
      Formula lastItp = fmgr.makeTrue();

      Deque<Integer> outerContext = null;
      Deque<Integer> previousContext = new ArrayDeque<Integer>();

      if (debug){
        System.out.println();
        System.out.println("Interpolants/predicates after blocks:");
      }


      // block index
      int i=0;
      while (i<interpolationBlocks.size()-1) {

        InterpolationBlock ib = interpolationBlocks.get(i);
        Integer primedNo = ib.getTraceNo();
        Deque<Integer> context = ib.getContext();
        ARTElement artElement = ib.getArtElement();
        CFANode loc = AbstractElements.extractLocation(artElement);

        if (i > 0 && lastItp.isFalse()){
          // if the last interpolant was false, then the interpolants below are empty
          itpContext.clear();
          Set<AbstractionPredicate> predicates = new HashSet<AbstractionPredicate>();

          if (debug){
            System.out.println("\t- blk "+i+" ["+loc+"]: "+lastItp+",\t"+predicates);
          }

          info.addPredicatesForRefinement(artElement, predicates);
        } else if (primedNo == previousPrimedNo){
          // we continue in the same branch - take the next interpolant from the current formula list
          lastItp = itpProver.getInterpolant(interpolationIds.subList(itpContext.size(), i-offset+1));
          Set<AbstractionPredicate> predicates = getPredicates(lastItp);

          if (debug){
            System.out.println("\t- blk "+i+" ["+loc+"]: "+lastItp+",\t"+predicates);
          }

          info.addPredicatesForRefinement(artElement, predicates);
        } else if (primedNo < previousPrimedNo){
          // we return to a previous trace - the formula list remains the same, but context gets possibly smaller

          // remove all contexts between from previousPrimeNo downto primedNo
          for (int j=previousPrimedNo; j >= primedNo; j--){
            itpContext.remove(j);
          }
          lastItp = itpProver.getInterpolant(interpolationIds.subList(itpContext.size(), i-offset+1));
          Set<AbstractionPredicate> predicates = getPredicates(lastItp);

          if (debug){
            System.out.println("\t- blk "+i+" ["+loc+"]: "+lastItp+",\t"+predicates);
          }

          info.addPredicatesForRefinement(artElement, predicates);
        } else {
          // we switch to an new, possibly parallel, trace - the context gets bigger and the list of formulas for interpolation changes


          if (itpEnvSkip && i>0) {
            // detect branches that may be skipped and generate empty predicates for them
            Integer skip = skipUselessBranches2(i, interpolationBlocks, previousPrimedNo, lastItp, info, itpProver, interpolationIds, itpContext, offset);
            if (skip!=null){
              // skip blocks
              i = skip;
              continue;
            }
          }

          itpProver.reset();
          stats.interpolationTimer.stop();
          stats.interpolationTimer.start();
          itpProver.init();

          // adjust the context and the offset
          if (lastItp!=null){
            itpContext.put(previousPrimedNo, lastItp);
          }
          offset = i-itpContext.size();

          // build a new formula list, where interpolants are used instead of covered formulas
          interpolationIds.clear();
          for (Formula f : itpContext.values()){
            T id = itpProver.addFormula(f);
            interpolationIds.add(id);
          }
          for (int k=i; k<interpolationBlocks.size(); k++){
            Formula f = interpolationBlocks.get(k).getPathFormula().getFormula();
            T id = itpProver.addFormula(f);
            interpolationIds.add(id);
          }
          assert interpolationIds.size()-itpContext.size()+i == interpolationBlocks.size();

          // get the interpolant
          spurious = itpProver.isUnsat();
          assert spurious;
          stats.unsatChecks++;

          lastItp = itpProver.getInterpolant(interpolationIds.subList(itpContext.size(), i-offset+1));
          Set<AbstractionPredicate> predicates = getPredicates(lastItp);

          if (debug){
            System.out.println("\t- blk "+i+" ["+loc+"]: "+lastItp+",\t"+predicates);
          }

          info.addPredicatesForRefinement(artElement, predicates);
        }



        previousPrimedNo = primedNo;
        i++;
      }
      assert itpContext.isEmpty();
    }
    else {
      // this is a real bug, notify the user
      info = new CounterexampleTraceInfo(false);
    }

    itpProver.reset();
    stats.interpolationTimer.stop();
    refStats.cexAnalysisTimer.stop();

    return info;
  }*/


  /**
   *
   * @param <T>
   * @param i
   * @param interpolationBlocks
   * @param previousPrimedNo
   * @param lastItp
   * @param info
   * @param itpProver
   * @param interpolationIds
   * @return
   */
  private <T> Integer  skipUselessBranches(int i, List<InterpolationBlock> interpolationBlocks, int previousPrimedNo, Formula lastItp, CounterexampleTraceInfo info, InterpolatingTheoremProver<T> itpProver, List<T> interpolationIds, SortedMap<Integer, Formula> itpContext, int offset ) {

    Integer returnBlock = null;
    for (int j=i+1; j<interpolationBlocks.size()-1; j++){
      InterpolationBlock fb = interpolationBlocks.get(j);
      if (fb.getTraceNo() == previousPrimedNo){
        returnBlock = j;
        break;
      }
    }
    System.out.println("The return for blk "+i+" is blk "+returnBlock);
    if (returnBlock != null){
      Formula itp = itpProver.getInterpolant(interpolationIds.subList(itpContext.size(), returnBlock-offset+1));
      //System.out.println("\tInterpolant for ["+itpContext.size()+","+(returnBlock-offset)+"] is "+itp);
      // check if lastItp => itp, then the branch does not give any usefull predicates
      Formula check = fmgr.makeNot(itp);
      check = fmgr.makeAnd(lastItp, check);

      thmProver.init();
      boolean valid = thmProver.isUnsat(check);
      thmProver.reset();

      if (valid){
        // lastItp => itp, so  the branches does not give any usefull predicates
        // skip to block (returnBlock+1) and put empty predicates in between
        System.out.println("Skip to blk "+(returnBlock+1)+" since " + lastItp +" => "+itp);
        for (int j=i; j<returnBlock; j++){
          ARTElement jARTElement = interpolationBlocks.get(j).getArtElement();
          CFANode jLoc = AbstractElements.extractLocation(jARTElement);
          Set<AbstractionPredicate> predicates = new HashSet<AbstractionPredicate>();

          if (debug){
            System.out.println("\t- blk "+j+" ["+jLoc+"]: skip,\t"+predicates);
          }

          info.addPredicatesForRefinement(jARTElement, predicates);
        }
      }
      else {
        returnBlock = null;
      }
    }

    return returnBlock;
  }

  /**
   *
   * @param <T>
   * @param i
   * @param interpolationBlocks
   * @param previousPrimedNo
   * @param lastItp
   * @param info
   * @param itpProver
   * @param interpolationIds
   * @return
   */
  private <T> Integer  skipUselessBranches2(int i, List<InterpolationBlock> interpolationBlocks, int previousPrimedNo, Formula lastItp, CounterexampleTraceInfo info, InterpolatingTheoremProver<T> itpProver, List<T> interpolationIds, SortedMap<Integer, Formula> itpContext, int offset ) {

    assert i>=1;

    // the call could have been nested - check which context should be inspected
    Deque<Integer> previousContext = interpolationBlocks.get(i-1).getContext();
    Deque<Integer> currentContext = interpolationBlocks.get(i).getContext();
    Deque<Integer> contextDiff = new ArrayDeque<Integer>(currentContext);
    contextDiff.removeAll(previousContext);
    contextDiff.remove(previousPrimedNo);
    int primedNo = interpolationBlocks.get(i).getTraceNo();
    contextDiff.addLast(primedNo);

    assert !contextDiff.isEmpty();

    for (int context : contextDiff){
      Integer returnBlock = null;
      for (int j=i; j<interpolationBlocks.size()-1; j++){
        InterpolationBlock fb = interpolationBlocks.get(j);
        if (fb.getTraceNo() == context) {
          returnBlock = j;
        }
      }
      assert returnBlock != null;

      // get the interpolants for the returnBlock
      Formula itp = itpProver.getInterpolant(interpolationIds.subList(itpContext.size(), returnBlock-offset+1));
      // check if lastItp => itp,

      Formula check = fmgr.makeNot(itp);
      check = fmgr.makeAnd(lastItp, check);
      //System.out.println("RB: "+returnBlock+", itp: "+itp);

      thmProver.init();
      boolean valid = thmProver.isUnsat(check);
      thmProver.reset();

      if (valid){
        // lastItp => itp, so  the branches does not give any usefull predicates
        // skip to block (returnBlock+1) and put empty predicates in between
        System.out.println("Skip to blk "+(returnBlock+1)+" since " + lastItp +" => "+itp);
        for (int j=i; j<=returnBlock; j++){
          ARTElement jARTElement = interpolationBlocks.get(j).getArtElement();
          CFANode jLoc = AbstractElements.extractLocation(jARTElement);
          Formula skipItp = itpProver.getInterpolant(interpolationIds.subList(itpContext.size(), j-offset+1));
          Set<AbstractionPredicate> predicates = getPredicates(skipItp);

          if (debug){
            System.out.println("\t- blk "+j+" ["+jLoc+"]: skip "+skipItp+",\t"+predicates);
          }

          info.addPredicatesForRefinement(jARTElement, predicates);
        }
        return returnBlock+1;
      }


      //System.out.println("The return for c:" + context +" is blk "+returnBlock);
    }

    return null;
  }

  /**
   * Divides the interpolant into atom formulas and unprimes them.
   * @param itp
   * @param ib
   * @return
   */
  private Set<AbstractionPredicate> getPredicates(Formula itp) {

    Set <AbstractionPredicate> result = new HashSet<AbstractionPredicate>();
    // TODO maybe handling of non-atomic predicates
    if (!itp.isTrue()){
      if (itp.isFalse()){
        // add false
        AbstractionPredicate atomPredicate = amgr.makeFalsePredicate();
        result.add(atomPredicate);
      }
      else {
        Collection<Formula> atoms = null;
        atoms = fmgr.extractAtoms(itp, splitItpAtoms, false);

        for (Formula atom : atoms){
          Formula unprimedAtom = fmgr.unprimeFormula(atom);
          AbstractionPredicate atomPredicate = amgr.makePredicate(unprimedAtom);
          result.add(atomPredicate);
        }
      }
    }
    return result;
  }


  /**
   * Divides the interpolant into atom formulas and renames them according to traceMap
   * @param itp
   * @param traceMap
   * @return
   */
  private Set<AbstractionPredicate> getDagPredicates(Formula itp, Integer tid,  Multimap<Integer, Integer> traceMap) {

    Set <AbstractionPredicate> result = new HashSet<AbstractionPredicate>();
    // TODO maybe handling of non-atomic predicates
    if (!itp.isTrue()){
      if (itp.isFalse()){
        // add false
        AbstractionPredicate atomPredicate = amgr.makeFalsePredicate();
        result.add(atomPredicate);
      }
      else {
        Collection<Formula> atoms = null;
        atoms = fmgr.extractAtoms(itp, splitItpAtoms, false);

        for (Formula fr : atoms){
          // TODO fix
          //   Formula atom = fmgr.extractNonmodularFormula(fr, tid, traceMap);
          AbstractionPredicate atomPredicate = amgr.makePredicate(fr);
          result.add(atomPredicate);
        }
      }
    }
    return result;
  }


  // TODO very prototyping
  private Set<AbstractionPredicate> getDagsPredicates(Formula itp, InterpolationDagNode node) {

    Set <AbstractionPredicate> result = new HashSet<AbstractionPredicate>();

    int tid = node.getTid();
    int otherTid = tid == 0 ? 1 : 0;

    // TODO maybe handling of non-atomic predicates
    if (!itp.isTrue()){
      if (itp.isFalse()){
        // add false
        AbstractionPredicate atomPredicate = amgr.makeFalsePredicate();
        result.add(atomPredicate);
      }
      else {
        List<Formula> atoms = fmgr.extractNonModularAtoms(itp, node.getTid());

        for (Formula atom : atoms){
          if (atom.toString().contains("^"+otherTid)){
            System.out.print("");
          }

          AbstractionPredicate atomPredicate = amgr.makePredicate(atom);
          result.add(atomPredicate);
        }
      }
    }
    return result;
  }


  private ListMultimap<ARTElement, AbstractionPredicate> getModularDagPredicates(Formula itp, InterpolationDagNode node) {

    ListMultimap<ARTElement, AbstractionPredicate> predMap = LinkedListMultimap.create();
    // we assume that false is already in global predicates

    if (!itp.isTrue()){
      Collection<Formula> atoms = fmgr.extractAtoms(itp, splitItpAtoms, false);

      for (Formula atom : atoms){
        Set<Integer> primes = fmgr.howManyPrimes(atom);
        if (primes.size() != 2){
          System.out.println("DEBUG: "+itp);
        }
        assert primes.size() == 2;
        Integer primeNo = null;
        for (Integer pn : primes){
          if (pn!=null){
            primeNo = pn;
            break;
          }
        }

        List<ARTElement> sourceART = new Vector<ARTElement>();
        // find the source ARTElement
        if (node.getTraceNo() == primeNo){
          sourceART.add(node.getArtElement());
        } else {
          for (InterpolationDagNode parent : node.getParents()){
            if (parent.getTraceNo() == primeNo){
              sourceART.add(parent.getArtElement());
            }
          }
        }

        assert sourceART != null;

        Formula unprimedAtom = fmgr.unprimeFormula(atom);
        AbstractionPredicate atomPredicate = amgr.makePredicate(unprimedAtom);
        for (ARTElement artElem : sourceART){
          predMap.put(artElem, atomPredicate);
        }

      }

    }

    return predMap;
  }


  /**
   * Contains interpolation formula in correct order and a map from interpolant number to ART element.
   */
  class InterpolationMap {

    // formulas by their number
    List<Integer> formulaList;
    // interpolant number -> ARTElement
    Map<Integer, ARTElement> intMap;

    public InterpolationMap(){
      formulaList = new Vector<Integer>();
      intMap = new HashMap<Integer, ARTElement>();
    }
  }



}

// Old, but useful code
/*if (lastItp != null){

 *  An optimization - if interpolants before and after environmental trace are the same, then they can taken as true
 *  e.g. if interpolant for (A, B^C^D) <=> interpolant for (A^B, C^D) , then interpolant for B can be taken as true.
 *  We find the block, where traces return the trace we where before (identified by previousPrimedNo). The block after
 *  the return point satisfies one of three conditions: 1) its context is null, 2) its context is lower than previousPrimedNo
 *  or 3) its context is like previousPrimedNo, but the trace no. increases.


  // returnI is the number of the last block, before the traces returns
  int returnBlock = -1;

  int jTraceNo = primedNo;
  for (int j=i; j<interpolationBlocks.size()-1; j++){
    InterpolationBlock fb = interpolationBlocks.get(j+1);

    if (fb.getContext() == null || fb.getContext() < previousPrimedNo || (fb.getContext() == previousPrimedNo && fb.getTraceNo() > jTraceNo) ){
      returnBlock = j;
      break;
    }

    jTraceNo = fb.getTraceNo();
  }
  assert returnBlock >= i;
  System.out.println("The return for blk "+i+" is blk "+returnBlock);
}*/


