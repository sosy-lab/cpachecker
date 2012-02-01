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
package org.sosy_lab.cpachecker.cpa.relyguarantee.refinement;

import static com.google.common.collect.Iterables.skip;
import static com.google.common.collect.Lists.transform;
import static org.sosy_lab.cpachecker.util.AbstractElements.extractElementByType;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Pattern;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.Triple;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.art.ARTUtils;
import org.sosy_lab.cpachecker.cpa.art.Path;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGAbstractElement;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGAbstractElement.AbstractionElement;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGApplicationInfo;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.RGEnvironmentManager;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGCFAEdge;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGEnvTransition;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGFullyAbstracted;
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
import org.sosy_lab.cpachecker.util.predicates.interfaces.SSAMapManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.TheoremProver;

import com.google.common.collect.Lists;


@Options(prefix="cpa.rg")
public class RGRefinementManager<T1, T2> implements StatisticsProvider {

  private static final String BRANCHING_PREDICATE_NAME = "__ART__";
  private static final Pattern BRANCHING_PREDICATE_NAME_PATTERN = Pattern.compile(
      "^.*" + BRANCHING_PREDICATE_NAME + "(?=\\d+$)");

  @Option(name="refinement.interpolatingProver", toUppercase=true, values={"MATHSAT", "CSISAT"},
      description="which interpolating solver to use for interpolant generation?")
      private String whichItpProver = "MATHSAT";

  @Option(name="refinement.dumpDAGfile",
      description="Dump a DAG representation of interpolation formulas to the choosen file.")
      private String dagFilePredix = "test/output/itpDAG";

  @Option(name="refinement.splitItpAtoms",
      description="split arithmetic equalities when extracting predicates from interpolants")
      private boolean splitItpAtoms = false;

  @Option(description="Print debugging info?")
  private boolean debug=false;

  @Option(toUppercase=true, values={"FA", "SA", "ST"},
      description="How to abstract environmental transitions:"
      + "ST - no abstraction, SA - precondition abstracted only, FA - precondition and operation abstracted")
  private String abstractEnvTransitions = "FA";

  @Option(description="Limit of nodes in an interpolation tree (0 - no limit).")
  private int itpTreeNodeLimit = 0;

  public final Stats stats;
  protected final InterpolatingTheoremProver<T1> firstItpProver;
  private final InterpolatingTheoremProver<T2> secondItpProver;
  private final LogManager logger;
  private final FormulaManager fmgr;
  private final PathFormulaManager pmgr;
  private final AbstractionManager amgr;
  private final SSAMapManager ssaManager;
  private final TheoremProver thmProver;
  private static int  uniqueNumber = 90;



  private static RGRefinementManager<?,?> rgRefManager;

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
   * @param ables
   * @return
   * @throws InvalidConfigurationException
   */
  public static RGRefinementManager<?, ?> getInstance(RegionManager pRmgr, FormulaManager pFmgr, SSAMapManager pSsaManager, PathFormulaManager pPmgr, TheoremProver pThmProver,
      InterpolatingTheoremProver<?> pItpProver, InterpolatingTheoremProver<?> pAltItpProver, Configuration pConfig,
      LogManager pLogger) throws InvalidConfigurationException {
    if (rgRefManager == null){
      rgRefManager = new RGRefinementManager(pRmgr, pFmgr, pSsaManager, pPmgr, pThmProver, pItpProver, pAltItpProver, pConfig, pLogger);
    }
    return rgRefManager;
  }

  public RGRefinementManager(RegionManager pRmgr, FormulaManager pFmgr, SSAMapManager pSsaManager, PathFormulaManager pPmgr, TheoremProver pThmProver,
      InterpolatingTheoremProver<T1> pItpProver, InterpolatingTheoremProver<T2> pAltItpProver, Configuration pConfig,
      LogManager pLogger) throws InvalidConfigurationException {
    pConfig.inject(this, RGRefinementManager.class);
    this.logger = pLogger;
    this.amgr = AbstractionManagerImpl.getInstance(pRmgr, pFmgr, pPmgr, pConfig, pLogger);
    this.fmgr = pFmgr;
    this.ssaManager = pSsaManager;
    this.pmgr = pPmgr;
    this.thmProver = pThmProver;
    this.firstItpProver = pItpProver;
    this.secondItpProver = pAltItpProver;
    this.stats = new Stats();

  }


  /**
   * Counterexample analysis and predicate discovery.
   * This method is just an helper to delegate the actual work
   * This is used to detect timeouts for interpolation
   * @param stats
   * @throws CPAException
   * @throws InterruptedException
   */
  public CounterexampleTraceInfo buildRgCounterexampleTrace(final ARTElement targetElement, final ReachedSet[] reachedSets, int tid) throws CPAException, InterruptedException {
    // if we don't want to limit the time given to the solver
    //return buildCounterexampleTraceWithSpecifiedItp(pAbstractTrace, elementsOnPath, firstItpProver);

    if (this.whichItpProver.equals("MATHSAT")){
      return interpolateTree(targetElement,  reachedSets, tid, firstItpProver);
    } else {
      throw new InterruptedException("Unknown iterpolating prover "+whichItpProver);
    }

  }






  protected Path computePath(ARTElement pLastElement) throws InterruptedException, CPAException {
    return ARTUtils.getOnePathTo(pLastElement);
  }

  protected List<Triple<ARTElement, CFANode, RGAbstractElement>> transformFullPath(Path pPath) throws CPATransferException {
    List<Triple<ARTElement, CFANode, RGAbstractElement>> result = Lists.newArrayList();

    for (ARTElement ae : transform(pPath, Pair.<ARTElement>getProjectionToFirst())) {
      RGAbstractElement pe = extractElementByType(ae, RGAbstractElement.class);
      if (pe instanceof AbstractionElement) {
        CFANode loc = AbstractElements.extractLocation(ae);
        result.add(Triple.of(ae, loc, pe));
      }
    }

    //assert  pPath.getLast().getFirst() == result.get(result.size()-1).getFirst();
    return result;
  }

  protected List<Triple<ARTElement, CFANode, RGAbstractElement>> transformPath(Path pPath) throws CPATransferException {
    List<Triple<ARTElement, CFANode, RGAbstractElement>> result = Lists.newArrayList();

    for (ARTElement ae : skip(transform(pPath, Pair.<ARTElement>getProjectionToFirst()), 1)) {
      RGAbstractElement pe = extractElementByType(ae, RGAbstractElement.class);
      if (pe instanceof AbstractionElement) {
        CFANode loc = AbstractElements.extractLocation(ae);
        result.add(Triple.of(ae, loc, pe));
      }
    }

    //assert  pPath.getLast().getFirst() == result.get(result.size()-1).getFirst();
    return result;
  }



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
   * Extends the given DAG by parts of ART and env. transition that lead to the target.
   * @param reachedSets
   * @param target
   * @param tid
   * @param dag
   * @param newUnique
   * @throws InterruptedException
   * @throws CPAException
   */
  public void constructDagForElement(ReachedSet[] reachedSets, ARTElement target, int tid, InterpolationDag dag) throws InterruptedException, CPAException {
    // Note: in case of abstracted operations, the target may be cover, ergo not in the reached set.
    if (target.isDestroyed()){
      System.out.println();
    }
    assert !target.isDestroyed();

    if (debug){
      System.out.println();
      System.out.println("Constructing DAG node for ("+tid+" ,"+target.getElementId()+")");
    }

    // get the abstraction elements on the path from the root to the error element
    Path cfaPath = computePath(target);
    List<Triple<ARTElement, CFANode, RGAbstractElement>> path = transformFullPath(cfaPath);

    // exit if the target has already been added to the target
    Triple<ARTElement, CFANode, RGAbstractElement> lastTriple = path.get(path.size()-1);
    ARTElement lastARTElem = lastTriple.getFirst();
    if (dag.getNodeMap().containsKey(Pair.of(tid, lastARTElem.getElementId()))){
      return;
    }


    // the previous DAG node
    InterpolationDagNode lastNode = null;

    for (Triple<ARTElement, CFANode, RGAbstractElement> triple : path){
      ARTElement artElement = triple.getFirst();
      AbstractionElement rgElement = (AbstractionElement) triple.getThird();
      assert rgElement.tid == tid;

      // create a node or use a cached one for this element
      boolean newNode = false;
      InterpolationDagNode node = dag.getNode(tid, artElement.getElementId());
      if (node == null){
        RGApplicationInfo appInfo = rgElement.getBlockAppInfo();
        PathFormula pf;
        if (appInfo != null){
          pf = appInfo.getRefinementPf();
        } else {
          pf = rgElement.getAbstractionFormula().getBlockPathFormula();
        }

        node = new InterpolationDagNode(artElement, pf, appInfo, tid);
        dag.getNodeMap().put(node.getKey(), node);
        newNode = true;

        if (lastNode == null){
          // add the root
          assert !dag.getRoots().contains(node);
          dag.getRoots().add(node);
        } else {
          // make the previous node a parent of this one
          assert !lastNode.getChildren().contains(node);
          lastNode.getChildren().add(node);
          assert !node.getParents().contains(lastNode);
          node.getParents().add(lastNode);
        }
      }


      // retrieve info about env. edges applied
      Map<Integer, RGEnvTransition> envMap = null;
      if (rgElement.getBlockAppInfo() != null){
        envMap = rgElement.getBlockAppInfo().getEnvMap();
      } else {
        envMap = new HashMap<Integer, RGEnvTransition>(0);
      }


      // rename env. transitions to their source thread numbers
      for(Integer id : envMap.keySet()){
        RGEnvTransition rgEdge = envMap.get(id);
        ARTElement targetARTElement = rgEdge.getSourceARTElement();
        Integer sourceTid           = rgEdge.getTid();
        assert sourceTid != tid;

        // construct missing nodes for the source element
        constructDagForElement(reachedSets, targetARTElement, sourceTid, dag);

        // get nodes for the last abstraction for the source element.
        Path envCfaPath = computePath(targetARTElement);
        List<Triple<ARTElement, CFANode, RGAbstractElement>> envPath = transformFullPath(envCfaPath);
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
   * Check the correctness of a Dag
   * @param roots
   */
  void dagAssertions(List<InterpolationDagNode> roots){
    for (InterpolationDagNode node : roots){
      PathFormula pf = node.getPathFormula();
      // check if the traceNo is OK
      // check parent - children relationships
      for (InterpolationDagNode child : node.getChildren()){

        assert child.getParents().contains(node);
      }

      dagAssertions(node.getChildren());
    }
  }



  // TODO For testing
  private void printEnvEdgesApplied(ARTElement artElement, Collection<RGCFAEdge>  set) {

    if (!set.isEmpty()){
      System.out.println("Env edges applied at id:"+artElement.getElementId());
      for (RGCFAEdge edge : set){
        System.out.println("- edge "+edge );
      }
    }
  }



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
  private <T> CounterexampleTraceInfo interpolateTree(ARTElement targetElement, ReachedSet[] reachedSets, int tid, InterpolatingTheoremProver<T> itpProver) throws InterruptedException, CPAException {
    stats.formulaTimer.start();

    /*
     * Prepare formulas for interpolation
     */

   // get the full DAG for the error element
   InterpolationDag dag = new InterpolationDag();
   constructDagForElement(reachedSets, targetElement, tid, dag);

   if (debug){
     dag.writeToDOT(dagFilePredix+"_main.dot");
   }

   InterpolationTree tree = unwindDag(dag, targetElement);

   if (debug){
     tree.writeToDOT(dagFilePredix+"_tree.dot");
   }

   trimInterpolationTree(tree, itpTreeNodeLimit);

   if (debug){
     tree.writeToDOT(dagFilePredix+"_trimmed.dot");
   }

   List<InterpolationTreeNode> topList = tree.topSort();

   stats.formulaTimer.stop();
   stats.formulaNo = topList.size();



   if (debug){
     System.out.println();
     System.out.println("Interpolation formulas - element: formula\t\t size:"+tree.size());
     for (InterpolationTreeNode node : topList){
       System.out.println("\t-"+node+": "+node.getPathFormula());
     }
   }

   /*
    * Interpolate formulas.
    */

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
     Map<Integer, Integer> rMap = new HashMap<Integer, Integer>(node.children.size()+1);
     if (!itp.isTrue()){
       rMap.put(node.uniqueId, -1);
       for (InterpolationTreeNode child : node.children){
         rMap.put(child.uniqueId, -1);
       }
     }

     Set<AbstractionPredicate> preds = getPredicates(itp, node, rMap);

     if (node.isARTAbstraction){
       info.addPredicatesForRefinement(node.artElement, preds);
     }
     if (node.isEnvAbstraction){
       info.addEnvPredicatesForRefinement(node.artElement, preds);
     }





     // add predicates to ART precision or to env. precision
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

   return info;
  }


  /**
   * Unwinds env. applications in the DAG into separate branches.
   * @param dag
   * @param target
   * @return
   * @throws CPATransferException
   */
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
    RGAbstractElement rgTarget = AbstractElements.extractElementByType(target, RGAbstractElement.class);
    Integer tid = rgTarget.tid;

    // how to rename path formula parts to their unique ids
    Map<Integer, Integer> renameMap = new HashMap<Integer, Integer>();

    // the local branch from the root to the target
    List<InterpolationTreeNode> lBranch = null;

    if (rgTarget instanceof RGAbstractElement.AbstractionElement){

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
      assert abstractEnvTransitions.equals("SA") || abstractEnvTransitions.equals("FA");
      ARTElement aARTElement = RGEnvironmentManager.findLastAbstractionARTElement(target);
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
      renameMap.put(-1, uniqueId);

      // add the node to the tree
      tree.addNode(treeNode);

      // construct and attach subtrees for env. applications
      RGApplicationInfo appInfo = treeNode.getAppInfo();

      if (appInfo != null){
        for (Integer id : appInfo.getEnvMap().keySet()){
          RGEnvTransition rgEdge = appInfo.getEnvMap().get(id);

          // create a sub-tree for the the element that generated the transition
          ARTElement sARTElement = rgEdge.getSourceARTElement();

          int sTid = rgEdge.getTid();
          Pair<InterpolationTree, Integer> pair = unwindDag(dag, sARTElement, appUniqueId);
          InterpolationTree appTree = pair.getFirst();
          InterpolationTreeNode appNode = appTree.getNode(sTid, sARTElement.getElementId(), appUniqueId);
          assert appNode != null && appNode.parent == null;

          if (abstractEnvTransitions.equals("FA")){
            // TODO make more elegant
            RGFullyAbstracted fa = (RGFullyAbstracted) rgEdge;
            PathFormula newPf = appNode.getPathFormula();
            // rename filter SSAMap
            SSAMap fSsa = fa.getHighSSA();
            HashMap<Integer, Integer> rMap2 = new HashMap<Integer, Integer>(1);
            rMap2.put(-1, appUniqueId);
            SSAMap rfSsa = ssaManager.changePrimeNo(fSsa, rMap2);

            // add equivalences so the path formula's indexes match the filter
            Pair<Pair<Formula, Formula>, SSAMap> equivs = ssaManager.mergeSSAMaps(newPf.getSsa(), rfSsa);
            Formula newrF = this.fmgr.makeAnd(newPf.getFormula(), equivs.getFirst().getFirst());
            newPf = new PathFormula(newrF, equivs.getSecond(), 0);

            appNode.setPathFormula(newPf);
          }

          // mark the root of the sub-tree as an env. abstraction
          if (abstractEnvTransitions.equals("SA") || abstractEnvTransitions.equals("FA")){
            appNode.setEnvAbstraction(true);
          }

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
      nodePF = pmgr.changePrimedNo(nodePF, renameMap);
      treeNode.setPathFormula(nodePF);

      previous = treeNode;
    }

    // get the abstraction elements on the path from the root to the error element
    uniqueId++;
    return Pair.of(tree, appUniqueId);
  }


  /**
   * If interpolation tree has more nodes then the limit, then some branches will be trimmed.
   * Cut-off are replaced by their abstraction.
   * @param tree
   * @param itpTreeNodeLimit
   * @return
   */
  private void trimInterpolationTree(InterpolationTree tree, int limit) {
    if (tree.size() <= limit || limit == 0){
      return;
    }


    // calculate the maximum number of nodes per env. application to the trunk
    List<InterpolationTreeNode> trunk = tree.getTrunk();

    int envApp = 0;
    for (InterpolationTreeNode node : trunk){
      RGApplicationInfo appInfo = node.getAppInfo();
      if (appInfo != null){
        envApp += appInfo.envMap.keySet().size();
      }
    }

    float s = (limit - trunk.size())/envApp;
    int nodePerApp = Math.round(s);
    nodePerApp = Math.max(nodePerApp, 1);

    if (debug){
      System.out.print("Trimmed interpolation tree from "+tree.size()+" to "+limit+" (per application:"+nodePerApp+")");
    }


    // trim env. applicaions
    for (InterpolationTreeNode node : trunk){
      for (InterpolationTreeNode child : node.children){
        if (!child.uniqueId.equals(node.uniqueId)){
          trimBranch(tree, child, nodePerApp);
        }
      }
    }
  }

  /**
   * Removes nodes in the branch over the limit. Removed nodes are replaced by their abstractions.
   * @param tree
   * @param start
   * @param limit
   */
  private void trimBranch(InterpolationTree tree, InterpolationTreeNode start, int limit) {

    Set<InterpolationTreeNode> nodes = tree.bfs(start, limit);
    assert nodes.size() <= limit;
    Set<InterpolationTreeNode> toDelete = new HashSet<InterpolationTreeNode>();

    // add abstraction to nodes with missing children
    Map<Integer, Integer> rMap = new HashMap<Integer, Integer>();
    for (InterpolationTreeNode node : nodes){
      Formula replacement = fmgr.makeTrue();
      for (InterpolationTreeNode child : node.children){
        if (!nodes.contains(child)){
          // find abstraction for the missing child
          Formula abs = null;
          if (child.isARTAbstraction){
             RGAbstractElement rgElem = AbstractElements.extractElementByType(node.artElement, RGAbstractElement.class);
            assert rgElem != null;
            abs = rgElem.getAbstractionFormula().asFormula();
            // rename the abstract to its branch id
            rMap.clear();
            rMap.put(-1, child.uniqueId);
            abs  = fmgr.changePrimedNo(abs, rMap);
          } else if (child.isEnvAbstraction){
            abs = child.getPathFormula().getFormula();
          }


          replacement = fmgr.makeAnd(replacement, abs);
          toDelete.add(child);
        }
      }

      if (!replacement.isTrue()){

        Formula newF = fmgr.makeAnd(node.getPathFormula().getFormula(), replacement);
        PathFormula newPf = new PathFormula(newF, node.getPathFormula().getSsa(), 0);
        node.setPathFormula(newPf);
      }
    }

    // delete trimmed nodes
    for (InterpolationTreeNode del : toDelete){
      tree.removeSubtree(del);
    }
  }

  /**
   * Extract interpolants for the node. The map describes how the interpolants should be renamed.
   * @param itp
   * @param node
   * @param rMap
   * @return
   */
  private Set<AbstractionPredicate> getPredicates(Formula itp, InterpolationTreeNode node, Map<Integer, Integer> rMap) {

    assert node.isARTAbstraction || node.isEnvAbstraction;

    Formula rItp = fmgr.changePrimedNo(itp, rMap);

    Set<AbstractionPredicate> preds = null;

    if (node.isARTAbstraction){
      preds = getPredicates(rItp);
    }

    if (node.isEnvAbstraction){
      assert abstractEnvTransitions.equals("SA") || abstractEnvTransitions.equals("FA");

      if (this.abstractEnvTransitions.equals("SA")){
        preds = getPredicates(rItp);
      } else if (this.abstractEnvTransitions.equals("FA")){
        SSAMap rSSA = ssaManager.changePrimeNo(node.getPathFormula().getSsa(), rMap);
        preds = getNextValPredicates(rItp, rSSA);
      }
    }

    return preds;
  }


  /**
   * Divides the interpolant into atoms and removes indexes.
   * @param itp
   * @param ib
   * @return
   */
  private Set<AbstractionPredicate> getPredicates(Formula itp) {

    Set <AbstractionPredicate> result = new HashSet<AbstractionPredicate>();
    if (!itp.isTrue() && !itp.isFalse()){

      Collection<Formula> atoms = null;
      atoms = fmgr.extractAtoms(itp, splitItpAtoms, false);

      for (Formula atom : atoms){
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
    if (!itp.isTrue() && !itp.isFalse()){
      Collection<Formula> atoms = null;
      atoms = fmgr.extractNextValueAtoms(itp, ssa);

      for (Formula atom : atoms){
        AbstractionPredicate atomPredicate = amgr.makePredicate(atom);
        result.add(atomPredicate);
      }

    }
    return result;
  }

  @Override
  public void collectStatistics(Collection<Statistics> scoll) {
    scoll.add(stats);
  }

  public static class Stats implements Statistics {
    public final Timer interpolationTimer  = new Timer();
    public final Timer formulaTimer        = new Timer();

    public int formulaNo                = 0;
    public int unsatChecks              = 0;

    @Override
    public String getName() {
      return "RGRefinementManager";
    }

    @Override
    public void printStatistics(PrintStream out, Result pResult,ReachedSet pReached){
      out.println("interpolation fomulas:           " + formatInt(formulaNo));
      out.println("unsat checks:                    " + formatInt(unsatChecks));
      out.println("time on constructing formulas:   " + formulaTimer);
      out.println("total time on interpolation:     " + interpolationTimer+" (max: "+interpolationTimer.printMaxTime()+")");
    }

    private String formatInt(int val){
      return String.format("  %7d", val);
    }
  }



}



