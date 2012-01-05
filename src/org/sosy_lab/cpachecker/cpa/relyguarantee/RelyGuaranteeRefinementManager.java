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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import org.sosy_lab.common.NestedTimer;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Timer;
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
import org.sosy_lab.cpachecker.cpa.relyguarantee.RelyGuaranteeAbstractElement.AbstractionElement;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RelyGuaranteeRefiner.RelyGuaranteeRefinerStatistics;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.interfaces.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.InterpolatingTheoremProver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.TheoremProver;

import com.google.common.collect.Lists;


@Options(prefix="cpa.relyguarantee")
public class RelyGuaranteeRefinementManager<T1, T2>  {

  private static final String BRANCHING_PREDICATE_NAME = "__ART__";
  private static final Pattern BRANCHING_PREDICATE_NAME_PATTERN = Pattern.compile(
      "^.*" + BRANCHING_PREDICATE_NAME + "(?=\\d+$)");

  @Option(description="only use the atoms from the interpolants as predicates, "
      + "and not the whole interpolant")
    private boolean atomicPredicates = true;

  @Option(name="refinement.interpolatingProver", toUppercase=true, values={"MATHSAT", "CSISAT"},
      description="which interpolating solver to use for interpolant generation?")
      private String whichItpProver = "MATHSAT";

  @Option(name="refinement.itpEnvSkip",
      description="Detect and skip interpolation branches that don't give new predicates.")
      private boolean itpEnvSkip = false;

  @Option(name="refinement.refinementMethod",
      description="How to refine the counterexample DAG: 0 - unfoald to a tree, 1 - insert env. edges")
      private int refinementMethod = 1;

  @Option(name="refinement.dumpDAGfile",
      description="Dump a DAG representation of interpolation formulas to the choosen file.")
      private String dagFilePredix = "test/output/itpDAG";

  @Option(name="refinement.splitItpAtoms",
      description="split arithmetic equalities when extracting predicates from interpolants")
      private boolean splitItpAtoms = false;

  @Option(description="Print debugging info?")
  private boolean debug=false;

  @Option(description="Abstract environmental transitions using their own predicates:"
      + "0 - don't abstract, 1 - abstract filter, 2 - abstract filter and operation.")
  private int abstractEnvTransitions = 2;

  public final PredStats stats;
  public final RefStats refStats;
  protected final InterpolatingTheoremProver<T1> firstItpProver;
  private final InterpolatingTheoremProver<T2> secondItpProver;
  private final LogManager logger;
  private final FormulaManager fmgr;
  private final PathFormulaManager pmgr;
  private final AbstractionManager amgr;
  private final TheoremProver thmProver;
  private static int  uniqueNumber = 90;



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
    pConfig.inject(this, RelyGuaranteeRefinementManager.class);
    this.logger = pLogger;
    this.amgr = AbstractionManagerImpl.getInstance(pRmgr, pFmgr, pPmgr, pConfig, pLogger);
    this.fmgr = pFmgr;
    this.pmgr = pPmgr;
    this.thmProver = pThmProver;
    this.stats = new PredStats();
    this.refStats = new RefStats();
    this.firstItpProver = pItpProver;
    this.secondItpProver = pAltItpProver;

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
      if (this.refinementMethod == 0){
        return interpolateTreeMathsat(targetElement,  reachedSets, tid, firstItpProver, stats);
      } else if  (this.refinementMethod == 1){
        return interpolateInsertMathsat(targetElement,  reachedSets, tid, firstItpProver, stats);
      } else {
        throw new UnsupportedOperationException("Curretly only tree refinement is supported.");
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
  /*public List<InterpolationDag> getDags(ReachedSet[] reachedSets, ARTElement errorElem, int tid) throws InterruptedException, CPAException {

    List<InterpolationDag> dags = new Vector<InterpolationDag>();

    // TODO extend it to >2 threads
    int otherTid = tid == 0 ? 1 : 0;

    // get the full DAG for the error element
    InterpolationDag mainDag = new InterpolationDag();
    getRootsForElement(reachedSets, errorElem, tid, mainDag);

    if (debug){
      System.out.println();
      System.out.println("Main DAG:");
      for (InterpolationDagNodeKey key : mainDag.getNodeMap().keySet()){
        System.out.println(mainDag.getNodeMap().get(key));
      }
      mainDag.writeToDOT(dagFilePredix+"_main.dot");
    }

    // get nodes on the path from the root to the error element
    InterpolationDagNode mainDagErrorNode = mainDag.getNode(tid, errorElem.getElementId());
    assert mainDagErrorNode != null;

    List<InterpolationDagNodeKey> errorPath = mainDag.getModularPathToNode(mainDagErrorNode);


    // remove all node for thread tid, that are not on the error path
    mainDag.retainNodesInThread(errorPath, tid);

    // remove all applications from the removed nodes
    retainApplications(mainDag, otherTid, errorPath);

    if (debug){
      mainDag.writeToDOT(dagFilePredix+"_reduced.dot");
    }


    List<List<InterpolationDagNodeKey>> branches = mainDag.getBranchesInThread(otherTid);


    if (branches.isEmpty()){
      // there is only the error thread
      dags.add(mainDag);
    }

    for (int i=0; i<branches.size(); i++){
      List<InterpolationDagNodeKey> branch = branches.get(i);
      InterpolationDag dag = new InterpolationDag(mainDag);
      dag.retainNodesInThread(branch, otherTid);

      /*
       * Reconstruct formulas in other threads, so they don't contain env. applications
       * from removed nodes.
       */
   /*   retainApplications(dag, tid, branch);
      dags.add(dag);
    }

    return dags;
  }*/

  /**
   * In thread tid of the dag, remove all env. applications that don't originate
   * from the given set.
   * @param pMainDag
   * @param pOtherTid
   * @param pErrorPath
   */
 /* private void retainApplications(InterpolationDag dag, int tid, Collection<InterpolationDagNodeKey> retain) {

    Set<InterpolationDagNode> nodes = dag.getNodesInThread(tid);

    Set<ARTElement> artRetain = new HashSet<ARTElement>(retain.size());
    for (InterpolationDagNodeKey key : retain){
      InterpolationDagNode nd = dag.getNode(key);
      artRetain.add(nd.getArtElement());
    }

    for(InterpolationDagNode node : nodes){
      retainApplications(node, artRetain);
    }
  }*/

  /**
   * Remove all env. applications from the node's path formula that don't originate
   * from the given set.
   * @param node
   * @param artRetain
   */
 /* private void retainApplications(InterpolationDagNode node, Set<ARTElement> artRetain) {

    RelyGuaranteeApplicationInfo appInfo = node.getAppInfo();

    if (appInfo == null){
      return;
    }

    // construct path formula with applications from the 'retain' set
    PathFormula appPf = new PathFormula(fmgr.makeFalse(), node.getPathFormula().getSsa(), 0, 0);
    int tid = node.getTid();

    boolean noApplications = true;

    for (RelyGuaranteeCFAEdge rgEdge : appInfo.envMap.keySet()){
      if (artRetain.contains(rgEdge.getLastARTAbstractionElement())){
        noApplications = false;
        PathFormula envPf = appInfo.envMap.get(rgEdge);
        appPf = pmgr.makeRelyGuaranteeOr(appPf, envPf, tid);
      }
    }
    // TODO same sanity check, i.e. SSA map
    if (noApplications){
      appPf = pmgr.makeOr(appPf, appInfo.localPf);
    } else {
      appPf = pmgr.makeAnd(appPf, appInfo.localPf);
    }


    node.setPathFormula(appPf);
  }*/

/*
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
  }*/





  /**
   * Extends the given DAG by parts of ART and env. transition that lead to the target.
   * @param reachedSets
   * @param target
   * @param tid
   * @param dag
   * @param newUnique
   * @throws InterruptedException
   * @throws CPAException
   */
  public void getRootsForElement(ReachedSet[] reachedSets, ARTElement target, int tid, InterpolationDag dag) throws InterruptedException, CPAException {
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

    // the previous DAG node
    InterpolationDagNode lastNode = null;

    for (Triple<ARTElement, CFANode, RelyGuaranteeAbstractElement> triple : path){
      ARTElement artElement = triple.getFirst();
      AbstractionElement rgElement = (AbstractionElement) triple.getThird();
      assert rgElement.tid == tid;

      // create a node or use a cached one for this element
      boolean newNode = false;
      InterpolationDagNode node = dag.getNode(tid, artElement.getElementId());
      if (node == null){
        node = new InterpolationDagNode(artElement, rgElement.getAbstractionFormula().getBlockPathFormula(), rgElement.getBlockAppInfo(), tid);
        dag.getNodeMap().put(node.getKey(), node);
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

      // retrieve info about env. edges applied
      Map<Integer, RelyGuaranteeCFAEdge> envMap = null;
      if (rgElement.getBlockAppInfo() != null){
        envMap = rgElement.getBlockAppInfo().getEnvMap();
      } else {
        envMap = new HashMap<Integer, RelyGuaranteeCFAEdge>(0);
      }

      if (debug && !envMap.isEmpty() ){
        System.out.print("\t env. tr. from id:");
        for(Integer id: envMap.keySet()){
          RelyGuaranteeCFAEdge rgEdge = envMap.get(id);
          //ARTElement lastARTAbstraction = rgEdge.getLastARTAbstractionElement();
          System.out.print(rgEdge.getSourceARTElement().getElementId()+" ");
        }
        System.out.println();
      }

      // rename env. transitions to their source thread numbers
      for(Integer id : envMap.keySet()){
        RelyGuaranteeCFAEdge rgEdge = envMap.get(id);
        ARTElement sourceARTElement = rgEdge.getSourceARTElement();
        Integer sourceTid           = rgEdge.getSourceTid();
        assert sourceTid != tid;

        // construct missing nodes for the source element
        getRootsForElement(reachedSets, sourceARTElement, sourceTid, dag);

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
      }

      lastNode = node;
    }

    if (debug){
      dag.dagAssertions();
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
      PathFormula pf = node.getPathFormula();
      // check if the traceNo is OK
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

  /*
   *
   *              Interpolate Tree
   *
   */

  /**
   * Checks if the target element is reachable by unwinding error DAG into a tree. If the element
   * is unreachable, then interpolants are found.
   * @param targetElement
   * @param reachedSets
   * @param tid
   * @param itpProver
   * @param stats
   * @return
   * @throws CPAException
   * @throws InterruptedException
   */
  private <T> CounterexampleTraceInfo interpolateTreeMathsat(ARTElement targetElement,  ReachedSet[] reachedSets, int tid , InterpolatingTheoremProver<T> itpProver, RelyGuaranteeRefinerStatistics stats) throws CPAException, InterruptedException{

     stats.formulaTimer.start();

     /*
      * Prepare formulas for interpolation
      */

    // get the full DAG for the error element
    InterpolationDag dag = new InterpolationDag();
    getRootsForElement(reachedSets, targetElement, tid, dag);

    if (debug){
      System.out.println();
      System.out.println("Main DAG:");
      for (InterpolationDagNodeKey key : dag.getNodeMap().keySet()){
        System.out.println(dag.getNodeMap().get(key));
      }
      dag.writeToDOT(dagFilePredix+"_main.dot");
    }

    InterpolationTree tree = unwindDag(dag, targetElement);
    List<InterpolationTreeNode> topList = tree.topSort();

    stats.formulaTimer.stop();
    stats.formulaNo = topList.size();

    if (debug){
      tree.writeToDOT(dagFilePredix+"_tree.dot");
    }

    if (debug){
      System.out.println();
      System.out.println("Interpolation formulas - element: formula");
      for (InterpolationTreeNode node : topList){
        System.out.println("\t-"+node+": "+node.getPathFormula());
      }
    }

    /*
     * Interpolate formulas.
     */

    refStats.cexAnalysisSolverTimer.start();
    CounterexampleTraceInfo info  = new CounterexampleTraceInfo();

    if (debug){
      System.out.println();
      System.out.println("Interpolants:");
    }

    stats.interpolationTimer.start();

    InterpolationTreeNode prevNode = null;

    Map<InterpolationTreeNode, T> idMap = new HashMap<InterpolationTreeNode, T>(tree.size());

    for (InterpolationTreeNode node : topList) {

      if (prevNode == null || !node.uniqueId.equals(prevNode.uniqueId)){
        /*
         *  New branch taken
         */


        // reconstruct formulas
        if (prevNode != null){
          itpProver.reset();
          tree.removeAncestorsOf(prevNode);
        }
        itpProver.init();
        idMap.clear();
        List<InterpolationTreeNode> nodeTopList = tree.topSort();
        for (InterpolationTreeNode n : nodeTopList){
          T id = itpProver.addFormula(n.getPathFormula().getFormula());
          idMap.put(n, id);
        }

        // run the prover
        boolean spurious = itpProver.isUnsat();
        stats.unsatChecks++;

        if (!spurious){
          // if trace is feasible, then it should be detected in the first iteration
          assert prevNode == null;
          info = new CounterexampleTraceInfo(false);

          if (debug){
            System.out.println("\tFeasbile error trace.");
          }

          break;
        }
      }

      // find the list of id that correspond to A-part of interpolation formulas
      List<InterpolationTreeNode> ancList = tree.getAncestorsOf(node);
      ancList.add(0, node);
      List<T> idList = new Vector<T>(ancList.size());
      for (InterpolationTreeNode ancNode : ancList){
        T id = idMap.get(ancNode);
        idList.add(id);
      }

      Formula itp = itpProver.getInterpolant(idList);

      // replace the node with the interpolant and drop all children
      PathFormula oldPF = node.getPathFormula();
      PathFormula newPF = new PathFormula(itp, oldPF.getSsa(), oldPF.getLength());
      node.setPathFormula(newPF);

      if (debug){
        System.out.print("\t-"+node+": "+itp);
      }

      // rename the interpolant to its source and divide into atoms
      if (!itp.isTrue()){
        Map<Integer, Integer> rMap = new HashMap<Integer, Integer>(node.children.size()+1);
        rMap.put(node.uniqueId, node.tid);
        for (InterpolationTreeNode child : node.children){
          rMap.put(child.uniqueId, child.tid);
        }

        itp = fmgr.adjustedPrimedNo(itp, rMap);
      }
      Set<AbstractionPredicate> preds = getPredicates(itp);

      // add predicates to ART precision or to env. precision
      assert node.isARTAbstraction || node.isEnvAbstraction;
      if (node.isARTAbstraction){
        info.addPredicatesForRefinement(node.artElement, preds);
      }
      if (node.isEnvAbstraction){
        assert abstractEnvTransitions == 1 || abstractEnvTransitions == 2;
        info.addEnvPredicatesForRefinement(node.artElement, preds);
      }

      if (itp.isFalse()){
        break;
      }

      if (debug){
        System.out.println("\t"+preds);
      }

      prevNode = node;
    }

    itpProver.reset();
    stats.interpolationTimer.stop();
    refStats.cexAnalysisTimer.stop();

    return info;
  }


  private InterpolationTree unwindDag(InterpolationDag dag, ARTElement target) throws CPATransferException{
    Pair<InterpolationTree, Integer> pair = unwindDag(dag, target, new Integer(10));
    return pair.getFirst();
  }

  /**
   * Unwinds env. applications in the DAG into separate branches. The branches are renamed starting from
   * the unique id. The result is a tree structure ready for interpolation and a new unique id.
   * @param dag
   * @param target
   * @param uniqueId
   * @return
   * @throws CPATransferException
   */
  private Pair<InterpolationTree, Integer> unwindDag(InterpolationDag dag, ARTElement target, Integer uniqueId) throws CPATransferException {
    // tree to be created
    InterpolationTree tree = new InterpolationTree();

    // next free unique id
    Integer appUniqueId = uniqueId + 1;

    // find the node corresponding to the target
    Integer elemId = target.getElementId();
    RelyGuaranteeAbstractElement rgTarget = AbstractElements.extractElementByType(target, RelyGuaranteeAbstractElement.class);
    Integer tid = rgTarget.tid;

    // how to rename path formula parts to their unique ids
    Map<Integer, Integer> renameMap = new HashMap<Integer, Integer>();

    // the local branch from the root to the target
    List<InterpolationTreeNode> lBranch = null;

    if (rgTarget instanceof RelyGuaranteeAbstractElement.AbstractionElement){
      // the target is an abstraction point
      InterpolationDagNode targetNode = dag.getNode(tid, elemId);
      List<InterpolationDagNode> lPath = dag.getModularPathToNode(targetNode);
      Collections.reverse(lPath);

      // convert the path to a tree branch
      lBranch = new Vector<InterpolationTreeNode>(lPath.size());

      for (InterpolationDagNode node : lPath){
        assert node != null;
        InterpolationTreeNode treeNode = new InterpolationTreeNode(node, uniqueId);
        lBranch.add(treeNode);
      }

    } else {
      // it's NOT an abstraction element, so only its last abstraction element is in the DAG
      assert abstractEnvTransitions == 1 || abstractEnvTransitions == 2;
      ARTElement aARTElement = RelyGuaranteeEnvironment.findLastAbstractionARTElement(target);
      assert aARTElement != null;
      InterpolationDagNode aNode = dag.getNode(tid, aARTElement.getElementId());
      assert aNode != null;

      List<InterpolationDagNode> lPath = dag.getModularPathToNode(aNode);
      Collections.reverse(lPath);

      lBranch = new Vector<InterpolationTreeNode>(lPath.size()+1);

      // construct a node for the target
      InterpolationTreeNode treeNode = new InterpolationTreeNode(target, rgTarget.getPathFormula(), rgTarget.getAppInfo(), rgTarget.getTid(), uniqueId, false, false);
      lBranch.add(treeNode);

      for (InterpolationDagNode node : lPath){
        assert node != null;
        treeNode = new InterpolationTreeNode(node, uniqueId);
        lBranch.add(treeNode);
      }
    }

    //  local path from the root to the target
    InterpolationTreeNode previous = null;


    for (InterpolationTreeNode treeNode : lBranch){

      if (previous != null){
        treeNode.parent = previous;
        previous.children.add(treeNode);
      }

      renameMap.clear();
      renameMap.put(tid, uniqueId);

      // add the node to the tree
      tree.addNode(treeNode);

      // construct and attach subtrees for env. applications
      RelyGuaranteeApplicationInfo appInfo = treeNode.getAppInfo();

      if (appInfo != null){
        for (Integer id : appInfo.getEnvMap().keySet()){
          RelyGuaranteeCFAEdge rgEdge = appInfo.getEnvMap().get(id);

          // create a sub-tree for the the element that generated the transition
          ARTElement sARTElement = null;
          if (abstractEnvTransitions == 1 || abstractEnvTransitions == 2){
            // env. transitions were abstracted, so use their source element
            sARTElement = rgEdge.getSourceARTElement();
          } else {
            // env. transitions were unabstracted, so use the last abstraction point
            sARTElement = rgEdge.getLastARTAbstractionElement();
          }

          int sTid = rgEdge.getSourceTid();
          Pair<InterpolationTree, Integer> pair = unwindDag(dag, sARTElement, appUniqueId);
          InterpolationTree appTree = pair.getFirst();
          InterpolationTreeNode appNode = appTree.getNode(sTid, sARTElement.getElementId(), appUniqueId);
          assert appNode != null && appNode.parent == null;

          if (abstractEnvTransitions == 2){
            // if edge was abstracted, then apply it for interpolation
            PathFormula newPf = pmgr.makeAnd(appNode.getPathFormula(), rgEdge.getLocalEdge(), appUniqueId);
            appNode.setPathFormula(newPf);
          }

          // mark the root of the sub-tree as an env. abstraction
          appNode.setEnvAbstraction(true);

          // add the sub tree to the main tree
          treeNode.children.add(appNode);
          appNode.parent = treeNode;
          tree.addSubTree(appTree);

          renameMap.put(id, appUniqueId);

          // get the next free unique id
          appUniqueId = pair.getSecond();


        }
      }

      // rename the node's path formula
      PathFormula nodePF = treeNode.getPathFormula();
      nodePF = pmgr.adjustPrimedNo(nodePF, renameMap);
      treeNode.setPathFormula(nodePF);

      previous = treeNode;
    }

    // get the abstraction elements on the path from the root to the error element
    uniqueId++;
    return Pair.of(tree, appUniqueId);
  }


 /*
  *
  *              Interpolate by Insertion
  *
  */

  /**
   * Interpolate counterexample DAG by inserting edges in place of env. applications.
   * @param targetElement
   * @param reachedSets
   * @param tid
   * @param itpProver
   * @param stats
   * @return
   * @throws InterruptedException
   * @throws CPAException
   */
  private <T> CounterexampleTraceInfo interpolateInsertMathsat(ARTElement targetElement, ReachedSet[] reachedSets, int tid, InterpolatingTheoremProver<T> itpProver, RelyGuaranteeRefinerStatistics stats) throws InterruptedException, CPAException {
    stats.formulaTimer.start();

    /*
     * Prepare formulas for interpolation
     */

   // get the full DAG for the error element
   InterpolationDag dag = new InterpolationDag();
   getRootsForElement(reachedSets, targetElement, tid, dag);

   if (debug){
     System.out.println();
     System.out.println("Main DAG:");
     for (InterpolationDagNodeKey key : dag.getNodeMap().keySet()){
       System.out.println(dag.getNodeMap().get(key));
     }
     dag.writeToDOT(dagFilePredix+"_main.dot");
   }

   InterpolationTree tree = unwindDag(dag, targetElement);
   List<InterpolationTreeNode> topList = tree.topSort();

   stats.formulaTimer.stop();
   stats.formulaNo = topList.size();

   if (debug){
     tree.writeToDOT(dagFilePredix+"_tree.dot");
   }

   if (debug){
     System.out.println();
     System.out.println("Interpolation formulas - element: formula");
     for (InterpolationTreeNode node : topList){
       System.out.println("\t-"+node+": "+node.getPathFormula());
     }
   }

   /*
    * Interpolate formulas.
    */

   refStats.cexAnalysisSolverTimer.start();
   CounterexampleTraceInfo info  = new CounterexampleTraceInfo();

   if (debug){
     System.out.println();
     System.out.println("Interpolants:");
   }

   stats.interpolationTimer.start();

   if (tree.size() > 100){
     System.out.println();
   }

   InterpolationTreeNode prevNode = null;

   Map<InterpolationTreeNode, T> idMap = new HashMap<InterpolationTreeNode, T>(tree.size());

   for (InterpolationTreeNode node : topList) {

     if (prevNode == null || !node.uniqueId.equals(prevNode.uniqueId)){
       /*
        *  New branch taken
        */


       // reconstruct formulas
       if (prevNode != null){
         itpProver.reset();
         tree.removeAncestorsOf(prevNode);
       }
       itpProver.init();
       idMap.clear();
       List<InterpolationTreeNode> nodeTopList = tree.topSort();
       for (InterpolationTreeNode n : nodeTopList){
         T id = itpProver.addFormula(n.getPathFormula().getFormula());
         idMap.put(n, id);
       }

       // run the prover
       boolean spurious = itpProver.isUnsat();
       stats.unsatChecks++;

       if (!spurious){
         // if trace is feasible, then it should be detected in the first iteration
         assert prevNode == null;
         info = new CounterexampleTraceInfo(false);

         if (debug){
           System.out.println("\tFeasbile error trace.");
         }

         break;
       }
     }

     // find the list of id that correspond to A-part of interpolation formulas
     List<InterpolationTreeNode> ancList = tree.getAncestorsOf(node);
     ancList.add(0, node);
     List<T> idList = new Vector<T>(ancList.size());
     for (InterpolationTreeNode ancNode : ancList){
       T id = idMap.get(ancNode);
       idList.add(id);
     }

     Formula itp = itpProver.getInterpolant(idList);

     // replace the node with the interpolant and drop all children
     PathFormula oldPF = node.getPathFormula();
     PathFormula newPF = new PathFormula(itp, oldPF.getSsa(), oldPF.getLength());
     node.setPathFormula(newPF);

     if (debug){
       System.out.print("\t-"+node+": "+itp);
     }

     // rename the interpolant to its source and divide into atoms
     Map<Integer, Integer> rMap = new HashMap<Integer, Integer>(node.children.size()+1);
     if (!itp.isTrue()){
       rMap.put(node.uniqueId, node.tid);
       for (InterpolationTreeNode child : node.children){
         rMap.put(child.uniqueId, child.tid);
       }


       itp = fmgr.adjustedPrimedNo(itp, rMap);
     }

     Set<AbstractionPredicate> preds = null;
     // add predicates to ART precision or to env. precision
     assert node.isARTAbstraction || node.isEnvAbstraction;
     if (node.isARTAbstraction){
       preds = getPredicates(itp);
       info.addPredicatesForRefinement(node.artElement, preds);
     }
     if (node.isEnvAbstraction){
       assert abstractEnvTransitions == 1 || abstractEnvTransitions == 2;

       if (this.abstractEnvTransitions == 1){
         preds = getPredicates(itp);
         info.addEnvPredicatesForRefinement(node.artElement, preds);
       } else if (this.abstractEnvTransitions == 2){
         // TODO trick to get an adjusted SSA map
         PathFormula ssaPf = pmgr.makeEmptyPathFormula(node.getPathFormula());
         ssaPf = pmgr.adjustPrimedNo(ssaPf, rMap);
         preds = getNextValPredicates(itp, ssaPf.getSsa());
         info.addEnvPredicatesForRefinement(node.artElement, preds);
       }
     }

     if (itp.isFalse()){
       break;
     }

     if (debug){
       System.out.println("\t"+preds);
     }

     prevNode = node;
   }

   itpProver.reset();
   stats.interpolationTimer.stop();
   refStats.cexAnalysisTimer.stop();

   return info;
  }



  /*private <T> CounterexampleTraceInfo interpolateDagsMathsat(ARTElement targetElement,  ReachedSet[] reachedSets, int tid , InterpolatingTheoremProver<T> itpProver, RelyGuaranteeRefinerStatistics stats) throws CPAException, InterruptedException{

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
  }*/



  private <T> DAGInterpolationResult interpolateDag(InterpolationDag dag, InterpolatingTheoremProver<T> itpProver, RelyGuaranteeRefinerStatistics stats) throws InterruptedException{

    List<InterpolationDagNodeKey> topNodes = dag.topSort2();

    if (debug){
      System.out.println();
      System.out.println("Interpolation formulas - element id [thread, location]: formula, env. formulas");
      int j=0;
      for (InterpolationDagNodeKey key : topNodes){
        InterpolationDagNode node = dag.getNode(key);
        CFANode loc = AbstractElements.extractLocation(node.getArtElement());
        System.out.println("\t-id:"+node.getArtElement().getElementId()+" [t"+node.getTid()+", "+loc+"]:\t"+node.getPathFormula());
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

    //  interpolants in the order of topological sort
    int i=0;
    while(i<topNodes.size()-1){
      InterpolationDagNodeKey key = topNodes.get(i);
      InterpolationDagNode node = dag.getNode(key);

      // TODO write comment
      Pair<List<InterpolationDagNode>, Integer> pair = parentSort(dag.getRoots(), node);

      // prepare formula list
      stats.interpolationTimer.start();
      itpProver.init();
      interpolationIds.clear();

      // push formulas for interpolation;
      // some formulas are renamed to ensure that interpolant is modular
      int tid = node.getTid();
      int otherTid = tid == 0 ? 1 : 0;

      Map<Integer, Integer> reprimeMap = new HashMap<Integer, Integer>(2);
      reprimeMap.put(otherTid, uniqueNumber+1);

      if (debug){
        System.out.println();
        System.out.println("Renamed formulas for :"+node.key);
      }

      for (int j=0; j<pair.getFirst().size(); j++){
        InterpolationDagNode nd = pair.getFirst().get(j);
        Formula fr = nd.getPathFormula().getFormula();
        // rename A-part formulas to ensure well-scope predicate
        if (j <= pair.getSecond()){
          // rename variables from the thread tid if their indexes are not the latests
          SSAMap ssa = node.getPathFormula().getSsa();
          SSAMap cleanSSA = SSAMap.retainPrime(ssa, tid);
          // rename all variables from other threads
          fr = fmgr.renameIndexes(fr, cleanSSA, uniqueNumber);
          fr = fmgr.adjustedPrimedNo(fr, reprimeMap);
        }

        if (debug){
          System.out.println("\t"+nd.key+" : "+fr);
        }

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

        //System.out.println("DEBUG: "+pair.getSecond());
        /*for (Formula fr: pair.getFirst()){
          System.out.println(fr);
        }*/
        //System.out.println("DEBUG2:"+interpolationIds.subList(0, idx+1));
        Formula itp = itpProver.getInterpolant(interpolationIds.subList(0, idx+1));
        // non-modular predicates
        Set<AbstractionPredicate> preds = getDagPredicates(itp, node);
        //System.out.println("\t DEBUG: "+itp+"\t->\t"+preds);
        // try-to-be modular predicates
        //Set<AbstractionPredicate> preds = getDagPredicatesSkipNonModular(itp, node);

        itpProver.reset();
        stats.interpolationTimer.stop();

        if (debug){
          loc = AbstractElements.extractLocation(node.getArtElement());
 //         System.out.println("\t-id:"+node.getArtElement().getElementId()+" [t"+node.getTid()+", "+node.getTraceNo()+", "+loc+"]:\t"+node.getPathFormula());
          System.out.println("\t-id:"+artElement.getElementId()+" ["+node.getTid()+", "+loc+"]: \t"+itp+", "+preds);
        }

        if (result == null){
          result = new DAGInterpolationResult(Boolean.TRUE);
        }

        // add predicates
        result.addPredicate(node.getArtElement(), preds);

        if (itp.isFalse()){
          i = topNodes.size();
        } else {
          // replace formula in the interpolated node
          PathFormula oldPf = node.getPathFormula();
          PathFormula newPf = new PathFormula(itp, oldPf.getSsa(), oldPf.getLength());
          node.setPathFormula(newPf);

          // add itp to the the children nodes:

          List<InterpolationDagNodeKey> childrenKeys = new Vector<InterpolationDagNodeKey>(node.getChildren().size());
          for (InterpolationDagNode child : node.getChildren()){
            childrenKeys.add(child.getKey());
          }

          for (InterpolationDagNodeKey childKey : childrenKeys){
            InterpolationDagNode child = dag.getNode(childKey);
            PathFormula oldChildPf = child.getPathFormula();
            Formula newChildF = fmgr.makeAnd(oldChildPf.getFormula(), itp);
            PathFormula newChildPf = new PathFormula(newChildF, oldChildPf.getSsa(), oldChildPf.getLength());
            child.setPathFormula(newChildPf);
          }

          // TODO experimental
          dag.removeNode(node.key);
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









  private Pair<List<InterpolationDagNode>, Integer> parentSort(List<InterpolationDagNode> roots, InterpolationDagNode target) {
    boolean print = false && debug;

    List<InterpolationDagNode> nodeList = new Vector<InterpolationDagNode>();
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
        nodeList.add(node);
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
          nodeList.add(node);
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
    return Pair.of(nodeList, idx);
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

    if (spurious){*/
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
   * Divides the interpolant into atoms and removes indexes.
   * @param itp
   * @param ib
   * @return
   */
  private Set<AbstractionPredicate> getPredicates(Formula itp) {

    Set <AbstractionPredicate> result = new HashSet<AbstractionPredicate>();
    // TODO maybe handling of non-atomic predicates
    if (!itp.isTrue() && !itp.isFalse()){

      Collection<Formula> atoms = null;
      atoms = fmgr.extractAtoms(itp, splitItpAtoms, false);

      for (Formula atom : atoms){
        //Formula unprimedAtom = fmgr.unprimeFormula(atom);
        AbstractionPredicate atomPredicate = amgr.makePredicate(atom);
        result.add(atomPredicate);
      }

    }
    return result;
  }

  /**
   * Divides the interpolant into atoms. Variables with indexes equal to the SSA map
   * are given '#' suffix.
   * @param itp
   * @param ssa
   * @return
   */
  private Set<AbstractionPredicate> getNextValPredicates(Formula itp, SSAMap ssa) {

    Set <AbstractionPredicate> result = new HashSet<AbstractionPredicate>();
    // TODO maybe handling of non-atomic predicates
    if (!itp.isTrue() && !itp.isFalse()){

      Collection<Formula> atoms = null;
      atoms = fmgr.extractNextValAtoms(itp, ssa);

      for (Formula atom : atoms){
        //Formula unprimedAtom = fmgr.unprimeFormula(atom);
        AbstractionPredicate atomPredicate = amgr.makePredicate(atom);
        result.add(atomPredicate);
      }

    }
    return result;
  }


  /**
   * Extract interpolants from a formula. The interpolants may be non-modular.
   * @param itp
   * @param node
   * @return
   */
  private Set<AbstractionPredicate> getDagPredicates(Formula itp, InterpolationDagNode node) {

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
          // testing
          Set<Integer> primes = fmgr.howManyPrimes(atom);

          // Prints non-modular predicates
          if (debug && primes.contains(otherTid)){
            System.out.println("\t\t\t non-modular atom: "+atom+" from itp: "+itp);
          }

          AbstractionPredicate atomPredicate = amgr.makePredicate(atom);
          result.add(atomPredicate);
        }
      }
    }
    return result;
  }

  /**
   * Extract interpolants from a formula. Purly non-modular predicates are skipped.
   * @param itp
   * @param node
   * @return
   */
  private Set<AbstractionPredicate> getDagPredicatesSkipNonModular(Formula itp, InterpolationDagNode node) {

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
          // testing
          Set<Integer> primes = fmgr.howManyPrimes(atom);

          //assert !primes.contains(tid) || !primes.contains(otherTid);

          if (primes.contains(otherTid) && !primes.contains(tid)){
            if (debug){
              System.out.println("\t skipped interpolant "+atom);
            }
            continue;
          } else if (primes.contains(otherTid) && primes.contains(tid)){
            System.out.print("");
          }

          AbstractionPredicate atomPredicate = amgr.makePredicate(atom);
          result.add(atomPredicate);
        }
      }
    }
    return result;
  }

  /**
   * Create predicates for all atoms in a formula.
   */
  @SuppressWarnings("deprecation")
  public List<AbstractionPredicate> getAtomsAsPredicates(Formula f) {
    Collection<Formula> atoms;
    if (atomicPredicates) {
      atoms = fmgr.extractAtoms(f, splitItpAtoms, false);
    } else {
      atoms = Collections.singleton(fmgr.uninstantiate(f));
    }

    List<AbstractionPredicate> preds = new ArrayList<AbstractionPredicate>(atoms.size());

    for (Formula atom : atoms) {
      preds.add(amgr.makePredicate(atom));
    }
    return preds;
  }


  /*private ListMultimap<ARTElement, AbstractionPredicate> getModularDagPredicates(Formula itp, InterpolationDagNode node) {

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
  }*/


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

  public static class PredStats {
    public int numCallsAbstraction = 0;
    public int numSymbolicAbstractions = 0;
    public int numSatCheckAbstractions = 0;
    public int numCallsAbstractionCached = 0;
    public final NestedTimer abstractionTime = new NestedTimer(); // outer: solve time, inner: bdd time

    public long allSatCount = 0;
    public int maxAllSatCount = 0;
    public Timer extractTimer = new Timer();
  }

  public static class RefStats {
    public final Timer cexAnalysisTimer = new Timer();
    public final Timer cexAnalysisSolverTimer = new Timer();
    public final Timer cexAnalysisGetUsefulBlocksTimer = new Timer();
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


