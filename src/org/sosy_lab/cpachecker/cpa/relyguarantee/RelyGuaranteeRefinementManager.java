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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;
import java.util.logging.Level;
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
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.InterpolatingTheoremProver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.TheoremProver;

import com.google.common.collect.HashMultimap;
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

  @Option(name="refinement.splitItpAtoms",
      description="split arithmetic equalities when extracting predicates from interpolants")
      private boolean splitItpAtoms = false;

  @Option(description="Print debugging info?")
  private boolean debug=false;




  private Set<String> globalVariables;


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
      LogManager pLogger, Set<String> globalVariables) throws InvalidConfigurationException {
    if (rgRefManager == null){
      rgRefManager = new RelyGuaranteeRefinementManager(pRmgr, pFmgr, pPmgr, pThmProver, pItpProver, pAltItpProver, pConfig, pLogger, globalVariables);
    }
    return rgRefManager;
  }

  public RelyGuaranteeRefinementManager(RegionManager pRmgr, FormulaManager pFmgr, PathFormulaManager pPmgr, TheoremProver pThmProver,
      InterpolatingTheoremProver<T1> pItpProver, InterpolatingTheoremProver<T2> pAltItpProver, Configuration pConfig,
      LogManager pLogger, Set<String> globalVariables) throws InvalidConfigurationException {
    super(pRmgr, pFmgr, pPmgr, pThmProver, pItpProver, pAltItpProver, pConfig, pLogger);
    pConfig.inject(this, RelyGuaranteeRefinementManager.class);
    this.globalVariables = globalVariables;
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
        return interpolateDagMathsat(targetElement,  reachedSets, tid, firstItpProver, stats);
      } else {
        return interpolateTreeMathsat(targetElement,  reachedSets, tid, firstItpProver, stats);
      }

    } else {
      throw new InterruptedException("Unknown iterpolating prover "+whichItpProver);
    }

  }


  protected Path computePath(ARTElement pLastElement, ReachedSet pReached) throws InterruptedException, CPAException {
    return ARTUtils.getOnePathTo(pLastElement);
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
  public List<InterpolationBlock> getTreeForElement(ARTElement target, ReachedSet[] reachedSets, int threadNo) throws InterruptedException, CPAException{

    if (debug){
      System.out.println();
      System.out.println("--> Calling for "+target.getElementId()+" in thread "+threadNo+" <----");
    }

    assert !target.isDestroyed() && reachedSets[threadNo].contains(target);


    List<InterpolationBlock> rgResult = new ArrayList<InterpolationBlock>();

    /*if (target.isDestroyed()){
      // the env transition was generate in a part of ART that has been dropped by refinement, so return a false formula
      PathFormula falsePf = pmgr.makeFalsePathFormula();
      Set<InterpolationBlockScope> ibSet = new HashSet<InterpolationBlockScope>(1);
      ibSet.add(new InterpolationBlockScope(0, target));
      InterpolationBlock ib = new InterpolationBlock(falsePf, ibSet);
      rgResult.add(ib);
      return rgResult;
    }*/

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
        printEnvEdgesApplied(artElement, rgElement.getOldPrimedMap().values());
      }

      // map : env edge applied -> the rest path formula of the env. trace
      Map<Integer, Integer> adjustmentMap = new HashMap<Integer, Integer>(rgElement.getPrimedMap().size());

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
  public List<InterpolationDagNode> getDagForElement(ARTElement errorElement, ReachedSet[] reachedSets, Integer errorTid) throws InterruptedException, CPAException {
    Map<Pair<Integer, Integer>, InterpolationDagNode> nodeMap = new HashMap<Pair<Integer, Integer>, InterpolationDagNode>();
    Multimap<Integer, Integer> traceMap = HashMultimap.create();
    return getDagForElement(errorElement, reachedSets, errorTid, nodeMap, traceMap).getFirst();
  }

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
  public Pair<List<InterpolationDagNode>, ARTElement> getDagForElement(ARTElement target, ReachedSet[] reachedSets, Integer tid, Map<Pair<Integer, Integer>, InterpolationDagNode> nodeMap, Multimap<Integer, Integer> traceMap) throws InterruptedException, CPAException{

    assert !target.isDestroyed() && reachedSets[tid].contains(target);

    if (debug){
      System.out.println();
      System.out.println("--> Constructing DAG for id:"+target.getElementId()+" in thread "+tid+" <----");
    }

    // the result
    List<InterpolationDagNode> roots = new Vector<InterpolationDagNode>(1);

    // get the abstraction elements on the path from the root to the target
    Path cfaPath = computePath(target, reachedSets[tid]);
    List<Triple<ARTElement, CFANode, RelyGuaranteeAbstractElement>> path = transformPath(cfaPath);

    InterpolationDagNode predecessorNode = null;
    // true iff the node branches out from a formula trace that has been previously discovered
    boolean newBranch = false;
    // id of the current trace
    Integer traceNo = null;

    if (debug){
      System.out.println("Elements of the path:");
    }

    for (Triple<ARTElement, CFANode, RelyGuaranteeAbstractElement> triple : path) {
      ARTElement artElement = triple.getFirst();
      assert triple.getThird() instanceof AbstractionElement;
      AbstractionElement rgElement = (AbstractionElement) triple.getThird();

      // add the element to the DAG, if it's not there already
      InterpolationDagNode node = null;
      Pair<Integer,Integer> dagkey = new Pair<Integer,Integer>(tid,artElement.getElementId());

      if (nodeMap.containsKey(dagkey)){
        // use the existing node, which means the formula trace for the target branches at some point
        node = nodeMap.get(dagkey);
        newBranch = true;
        traceNo = node.getTraceNo();

        if(debug){
          System.out.println("\t-"+node.toString()+" (cached)");
        }

      } else {
        // create a new node
        if (predecessorNode == null || newBranch){
          // this is the first branch discovered in this thread or new have branched from another thread
          traceNo = traceMap.values().size();
          traceMap.put(tid, traceNo);
          newBranch = false;
        }

        assert traceNo != null;

        List<InterpolationDagNode> children = new Vector<InterpolationDagNode>();
        PathFormula pf = rgElement.getAbstractionFormula().getBlockPathFormula();
        node = new InterpolationDagNode(pf, traceNo, artElement, children);
        nodeMap.put(dagkey, node);

        // if node is a root, then add it to 'roots' list
        if (predecessorNode == null){
          roots.add(node);
        }

        // add the current node as the child to the predecessor
        if (predecessorNode != null) {
          assert !predecessorNode.getChildren().contains(node);
          predecessorNode.getChildren().add(node);
        }

        if(debug){
          System.out.println("\t-"+node.toString()+" (new)");
        }

      }



      if (debug && !rgElement.getOldPrimedMap().values().isEmpty() ){
        for(RelyGuaranteeCFAEdge rgEdge : rgElement.getOldPrimedMap().values()){
          ARTElement sourceARTElement = rgEdge.getSourceARTElement();
          System.out.println("\t env. tr. from id:"+sourceARTElement.getElementId()+" was applied");
        }
      }

      // get the node source element for the environmental transitions applied
      for(RelyGuaranteeCFAEdge rgEdge : rgElement.getOldPrimedMap().values()){
        ARTElement sourceARTElement = rgEdge.getSourceARTElement();
        Integer sourceTid = rgEdge.getSourceTid();



        // recursively call  getDagForElement to get the DAG source of the transition
        Pair<List<InterpolationDagNode>, ARTElement> envPair = getDagForElement(sourceARTElement, reachedSets, sourceTid, nodeMap, traceMap);
        List<InterpolationDagNode> envRoots = envPair.getFirst();
        ARTElement sourceAbstractionElement = envPair.getSecond();

        // add new roots
        for (InterpolationDagNode root : envRoots){
          if (!roots.contains(root)){
            roots.add(root);
          }
        }

        // get the node that created the transition
        Pair<Integer, Integer> key = Pair.of(sourceTid, sourceAbstractionElement.getElementId());
        InterpolationDagNode sourceNode = nodeMap.get(key);
        assert sourceNode != null;

        // make an edge from the source node to the application
        List<InterpolationDagNode> children = sourceNode.getChildren();
        if (!children.contains(node)){
          children.add(node);
        }
      }

      predecessorNode = node;
    }

    assert roots.size() <= reachedSets.length;

    return Pair.of(roots, predecessorNode.getArtElement());
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


  /**
   * Counterexample or interpolates.
   * @param pAbstractTrace
   * @param pElementsOnPath
   * @param pFirstItpProver
   * @return
   */
  private <T> CounterexampleTraceInfo interpolateDagMathsat(ARTElement targetElement,  ReachedSet[] reachedSets, int tid , InterpolatingTheoremProver<T> itpProver, RelyGuaranteeRefinerStatistics stats) throws CPAException, InterruptedException{
    logger.log(Level.FINEST, "Building counterexample trace");

    // get the rely guarantee path for the element
    stats.formulaTimer.start();
    List<InterpolationDagNode> roots = getDagForElement(targetElement, reachedSets, tid);

    List<InterpolationBlock> interpolationBlocks = getTreeForElement(targetElement, reachedSets, tid);


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
  }

  /**
   * Counterexample or interpolates.
   * @param pAbstractTrace
   * @param pElementsOnPath
   * @param pFirstItpProver
   * @return
   */
  private <T> CounterexampleTraceInfo interpolateTreeMathsat(ARTElement targetElement,  ReachedSet[] reachedSets, int threadNo , InterpolatingTheoremProver<T> itpProver, RelyGuaranteeRefinerStatistics stats) throws CPAException, InterruptedException{
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
   * Divides the interpolant into atom formulas un unprimes them.
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


