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

import java.util.ArrayList;
import java.util.Collection;
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
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.AssumeEdge;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.art.ARTUtils;
import org.sosy_lab.cpachecker.cpa.art.Path;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateRefinementManager;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RelyGuaranteeAbstractElement.AbstractionElement;
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

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;


@Options(prefix="cpa.relyguarantee.refinement")
public class RelyGuaranteeRefinementManager<T1, T2> extends PredicateRefinementManager<T1, T2>  {

  private static final String BRANCHING_PREDICATE_NAME = "__ART__";
  private static final Pattern BRANCHING_PREDICATE_NAME_PATTERN = Pattern.compile(
      "^.*" + BRANCHING_PREDICATE_NAME + "(?=\\d+$)");

  @Option(name="interpolatingProver", toUppercase=true, values={"MATHSAT", "CSISAT"},
      description="which interpolating solver to use for interpolant generation?")
      private String whichItpProver = "MATHSAT";

  @Option(description="apply deletion-filter to the abstract counterexample, to get "
    + "a minimal set of blocks, before applying interpolation-based refinement")
    private boolean getUsefulBlocks = false;

  @Option(name="shortestCexTrace",
      description="use incremental search in counterexample analysis, "
        + "to find the minimal infeasible prefix")
        private boolean shortestTrace = false;

  @Option(description="only use the atoms from the interpolants as predicates, "
    + "and not the whole interpolant")
    private boolean atomicPredicates = true;

  @Option(description="split arithmetic equalities when extracting predicates from interpolants")
  private boolean splitItpAtoms = false;

  @Option(name="shortestCexTraceUseSuffix",
      description="if shortestCexTrace is used, "
        + "start from the end with the incremental search")
        private boolean useSuffix = false;

  @Option(name="shortestCexTraceZigZag",
      description="if shortestCexTrace is used, "
        + "alternatingly search from start and end of the trace")
        private boolean useZigZag = false;

  @Option(name="addWellScopedPredicates",
      description="refinement will try to build 'well-scoped' predicates, "
        + "by cutting spurious traces as explained in Section 5.2 of the paper "
        + "'Abstractions From Proofs'\n(this does not work with function inlining).\n"
        + "THIS FEATURE IS CURRENTLY NOT AVAILABLE. ")
        private boolean wellScopedPredicates = false;

  @Option(description="dump all interpolation problems")
  private boolean dumpInterpolationProblems = false;

  @Option(name="timelimit",
      description="time limit for refinement (0 is infinitely long)")
      private long itpTimeLimit = 0;

  @Option(name="changesolverontimeout",
      description="try again with a second solver if refinement timed out")
      private boolean changeItpSolveOTF = false;

  @Option(description="skip refinement if input formula is larger than "
    + "this amount of bytes (ignored if 0)")
    private int maxRefinementSize = 0;


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
    this.globalVariables = globalVariables;
  }


  /**
   * Counterexample analysis and predicate discovery.
   * This method is just an helper to delegate the actual work
   * This is used to detect timeouts for interpolation
   * @throws CPAException
   * @throws InterruptedException
   */
  public CounterexampleTraceInfo buildRgCounterexampleTrace(final ARTElement targetElement, final ReachedSet[] reachedSets, int threadNo) throws CPAException, InterruptedException {
    // if we don't want to limit the time given to the solver
    //return buildCounterexampleTraceWithSpecifiedItp(pAbstractTrace, elementsOnPath, firstItpProver);
    if (this.whichItpProver.equals("MATHSAT")){
      return buildRgCounterexampleTraceWithMathSat(targetElement,  reachedSets, threadNo, firstItpProver);
    }
    else if (whichItpProver.equals("CSISAT")){
      return buildRgCounterexampleTraceWithCSISat(targetElement,  reachedSets, threadNo, firstItpProver);
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




  private List<Formula> getFormulasForTrace(List<RelyGuaranteeAbstractElement> abstractTrace) {
    // create the DAG formula corresponding to the abstract trace. We create
    // n formulas, one per interpolation group
    List<Formula> result = new ArrayList<Formula>(abstractTrace.size());

    for (RelyGuaranteeAbstractElement e : abstractTrace) {
      result.add(e.getAbstractionFormula().getBlockFormula());
    }
    return result;
  }

  private Formula buildBranchingFormula(Set<ARTElement> elementsOnPath) throws CPATransferException {
    // build the branching formula that will help us find the real error path
    Formula branchingFormula = fmgr.makeTrue();
    for (final ARTElement pathElement : elementsOnPath) {

      if (pathElement.getChildren().size() > 1) {
        if (pathElement.getChildren().size() > 2) {
          // can't create branching formula
          logger.log(Level.WARNING, "ART branching with more than two outgoing edges");
          return fmgr.makeTrue();
        }

        Iterable<CFAEdge> outgoingEdges = Iterables.transform(pathElement.getChildren(),
            new Function<ARTElement, CFAEdge>() {
          @Override
          public CFAEdge apply(ARTElement child) {
            return pathElement.getEdgeToChild(child);
          }
        });
        if (!Iterables.all(outgoingEdges, Predicates.instanceOf(AssumeEdge.class))) {
          logger.log(Level.WARNING, "ART branching without AssumeEdge");
          return fmgr.makeTrue();
        }

        AssumeEdge edge = null;
        for (CFAEdge currentEdge : outgoingEdges) {
          if (((AssumeEdge)currentEdge).getTruthAssumption()) {
            edge = (AssumeEdge)currentEdge;
            break;
          }
        }
        assert edge != null;

        Formula pred = fmgr.makePredicateVariable(BRANCHING_PREDICATE_NAME + pathElement.getElementId(), 0);

        // create formula by edge, be sure to use the correct SSA indices!
        RelyGuaranteeAbstractElement pe = AbstractElements.extractElementByType(pathElement, RelyGuaranteeAbstractElement.class);
        PathFormula pf = pe.getPathFormula();
        pf = pmgr.makeEmptyPathFormula(pf); // reset everything except SSAMap
        pf = pmgr.makeAnd(pf, edge);        // conjunct with edge

        Formula equiv = fmgr.makeEquivalence(pred, pf.getFormula());
        branchingFormula = fmgr.makeAnd(branchingFormula, equiv);
      }
    }
    return branchingFormula;
  }



  /**
   * Returns a formula encoding rely guarantee transitions to the target element, including interactions with other threads.
   * @param target
   * @param reachedSets
   * @param threadNo
   * @return
   * @throws CPAException
   * @throws InterruptedException
   */
  public List<InterpolationBlock> getRGFormulaForElement(ARTElement target, ReachedSet[] reachedSets, int threadNo) throws InterruptedException, CPAException{
    System.out.println();
    System.out.println("--> Calling for "+target.getElementId()+" in thread "+threadNo+" <----");
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
    System.out.println("The error trace in thread "+threadNo+" is:\n"+cfaPath);
    List<Triple<ARTElement, CFANode, RelyGuaranteeAbstractElement>> path = transformPath(cfaPath);

    System.out.println("Abstraction elements are: ");
    for (Triple<ARTElement, CFANode, RelyGuaranteeAbstractElement> triple : path){
      System.out.println("- id:"+triple.getFirst().getElementId()+" at loc "+triple.getSecond());
    }

    // the maximum number of primes that appears in any formula block
    int offset = 0;
    for (Triple<ARTElement, CFANode, RelyGuaranteeAbstractElement> triple : path){
      ARTElement artElement = triple.getFirst();
      assert triple.getThird() instanceof AbstractionElement;
      AbstractionElement rgElement = (AbstractionElement) triple.getThird();

      // get list of env edges applied to this block
      /* List<RelyGuaranteeEnvTransitionBuilder> rgEdges = pathFormulaConstructor.getEnvironmetalTransitions(builder);
      for (RelyGuaranteeEnvTransitionBuilder etb : rgEdges){
        assert rgElement.getOldblockEnvEdges().contains(etb.getEnvEdge());
      }
      for (RelyGuaranteeCFAEdge edge : rgElement.getOldblockEnvEdges()){
        boolean contains = false;
        for (RelyGuaranteeEnvTransitionBuilder etb : rgEdges){
          if (edge == etb.getEnvEdge()){
            contains = true;
          }
        }
        assert contains;
      }*/

      printEnvEdgesApplied(artElement, rgElement.getOldPrimedMap().values());
      // map : env edge applied -> the rest path formula of the env. trace
      Map<Integer, Integer> adjustmentMap = new HashMap<Integer, Integer>(rgElement.getPrimedMap().size());

      // get the blocks for environmental transitions
      for(Integer primedNo: rgElement.getOldPrimedMap().keySet()){
        RelyGuaranteeCFAEdge rgEdge = rgElement.getOldPrimedMap().get(primedNo);
        ARTElement sourceElement = rgEdge.getSourceARTElement();
        int sourceThread = rgEdge.getSourceTid();

        // TODO caching
        List<InterpolationBlock> envPF = getRGFormulaForElement(sourceElement, reachedSets, sourceThread);

        offset++;
        if (primedNo != offset){
          adjustmentMap.put(primedNo, offset);
        }
        // prime the blocks so paths paths are unique
        Pair<Integer, List<InterpolationBlock>> pair = primeInterpolationBlocks(envPF, offset);
        envPF = pair.getSecond();
        //InterpolationBlock lastBlock = envPF.remove(envPF.size()-1);
        // the top block should be enough to construct a correct local path
        //envTopMap.put(envBuilder, lastBlock.getPathFormula());
        // remember the remaining blocks
        //envRestMap.put(envBuilder, envPF);
        rgResult.addAll(envPF);
        // extend the scope this env. transition
        // scope.addAll(lastBlock.getScope());
        offset = pair.getFirst();
      }

      PathFormula currentPF = rgElement.getAbstractionFormula().getBlockPathFormula();
      if (!adjustmentMap.isEmpty()){
        currentPF = pmgr.adjustPrimedNo(currentPF, adjustmentMap);
      }
      InterpolationBlock currentBlock = new InterpolationBlock(currentPF, 0, artElement);
      rgResult.add(currentBlock);

      // if the were no env. transitions and primeNo is zero, then the result should be equal to path formula constructed in the ordinary way{
      assert !rgElement.getOldPrimedMap().isEmpty() || currentPF.equals(rgElement.getAbstractionFormula().getBlockPathFormula());

    }
    return rgResult;
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
      int newPrimedNo = ib.getPrimedNo()+offset;
      maximumPrimedNo = Math.max(maximumPrimedNo, newPrimedNo);
      ARTElement artElement = ib.getArtElement();
      primedBlocks.add(new InterpolationBlock(newPF, newPrimedNo, artElement));
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
   * Counterexample or interpolates
   * @param pAbstractTrace
   * @param pElementsOnPath
   * @param pFirstItpProver
   * @return
   */
  private <T> CounterexampleTraceInfo buildRgCounterexampleTraceWithCSISat(ARTElement targetElement,  ReachedSet[] reachedSets, int threadNo , InterpolatingTheoremProver<T> itpProver) throws CPAException, InterruptedException{
    logger.log(Level.FINEST, "Building counterexample trace");

    // get the rely guarantee path for the element
    List<InterpolationBlock> interpolationBlocks = getRGFormulaForElement(targetElement, reachedSets, threadNo);
    // List<ARTElement> abstractTrace = new Vector<ARTElement>(interpolationPathFormulas.size());
    System.out.println();
    System.out.println("Interpolation scopes/formulas:");
    int j=0;
    for (InterpolationBlock ib : interpolationBlocks){
      System.out.println("\t- "+j+" "+ib);
      j++;
    }

    logger.log(Level.ALL, "Counterexample trace formulas:", interpolationBlocks);
    logger.log(Level.FINEST, "Checking feasibility of counterexample trace");
    refStats.cexAnalysisSolverTimer.start();

    // CSISAT doesn't support groupping of formulas, so to obtain well-scopped predicates for environmetal formula trace
    // we have to create a separate list of formulas in the correct order

    // map from scope (number of primes) to  sets of formula in the correct with a map of interpolants
    Map<Integer, InterpolationMap> interpolationMaps = new HashMap<Integer, InterpolationMap>();

    int mark = 0;
    for (int i=0; i<interpolationBlocks.size()-1; i++ ){
      InterpolationBlock ib = interpolationBlocks.get(i);
      Integer primedNo = ib.getPrimedNo();
      ARTElement artElement = ib.getArtElement();

      if (primedNo < mark){
        // environmental formula trace is applied to a local formula trace
        InterpolationMap envMap = interpolationMaps.get(mark);
        InterpolationMap localMap = interpolationMaps.get(primedNo);

        if (envMap != null){
          localMap.formulaList.addAll(envMap.formulaList);
        }

        localMap.formulaList.add(i);
        int lastindex = localMap.formulaList.size()-1;
        localMap.intMap.put(lastindex, artElement);
        mark = primedNo;
      } else {
        // adding formula to the same interpolationMap or above
        InterpolationMap currentMap = interpolationMaps.get(primedNo);

        if (currentMap == null){
          currentMap = new InterpolationMap();
          interpolationMaps.put(primedNo, currentMap);
        }

        currentMap.formulaList.add(i);
        int lastindex = currentMap.formulaList.size()-1;
        currentMap.intMap.put(lastindex, artElement);
        mark = Math.max(mark, primedNo);
      }
    }

    // check correctness
    assertionInterpolationMaps(interpolationMaps, interpolationBlocks, mark);

    boolean spurious = true;
    CounterexampleTraceInfo info = new CounterexampleTraceInfo();
    Map<ARTElement, Pair<Formula, Set<AbstractionPredicate>>> printingPredicates = new HashMap<ARTElement, Pair<Formula, Set<AbstractionPredicate>>>();
    // get interpolants for each scope
    for (InterpolationMap interpolationMap : interpolationMaps.values()) {
      itpProver.init();

      // add conjunction of formulas to the prover
      List<T> formulaIds = new Vector<T>(interpolationBlocks.size());
      for (Integer formulaId : interpolationMap.formulaList){
        Formula f = interpolationBlocks.get(formulaId).getPathFormula().getFormula();
        T id = itpProver.addFormula(f);
        formulaIds.add(id);
      }
      // add remaining formulas
      for (int i=0; i<interpolationBlocks.size(); i++){
        if (!interpolationMap.formulaList.contains(i)){
          Formula f = interpolationBlocks.get(i).getPathFormula().getFormula();
          T id = itpProver.addFormula(f);
          formulaIds.add(id);
        }
      }

      assert formulaIds.size() == interpolationBlocks.size();

      // break if error trace is feasible
      if (!itpProver.isUnsat()){
        spurious = false;
        break;
      }

      // the trace is infeasible, so get interpolants
      for (Integer idx : interpolationMap.intMap.keySet()){
        // get the interpolants
        Formula itp = itpProver.getInterpolant(formulaIds.subList(0, idx+1));
        Set<AbstractionPredicate> preds = getPrecisionForElements(itp);
        // add interpolants to the result
        ARTElement artElement = interpolationMap.intMap.get(idx);
        printingPredicates.put(artElement, Pair.of(itp, preds));
        info.addPredicatesForRefinement(artElement, preds);
      }

      itpProver.reset();
    }

    if (spurious){
      // counterexample is spurious - print the interpolants
      System.out.println();
      System.out.println("Interpolants/predicates after blocks:");
      for (int i=0; i<interpolationBlocks.size()-1; i++){
        ARTElement element = interpolationBlocks.get(i).getArtElement();
        Pair<Formula, Set<AbstractionPredicate>> pair = printingPredicates.get(element);
        System.out.println("\t- "+i+": "+pair.getFirst()+",  \t"+pair.getSecond());
      }
    } else {
      // the error trace is feasible
      info = new CounterexampleTraceInfo(false);
    }

    return info;
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
   * Counterexample or interpolates
   * @param pAbstractTrace
   * @param pElementsOnPath
   * @param pFirstItpProver
   * @return
   */
  private <T> CounterexampleTraceInfo buildRgCounterexampleTraceWithMathSat(ARTElement targetElement,  ReachedSet[] reachedSets, int threadNo , InterpolatingTheoremProver<T> itpProver) throws CPAException, InterruptedException{
    logger.log(Level.FINEST, "Building counterexample trace");

    // get the rely guarantee path for the element
    List<InterpolationBlock> interpolationBlocks = getRGFormulaForElement(targetElement, reachedSets, threadNo);

    System.out.println();
    System.out.println("Interpolation block/[scope - abstraction element]/formulas:");
    int j=0;
    for (InterpolationBlock ib : interpolationBlocks){
      System.out.println("\t- blk "+j+" "+ib);
      j++;
    }

    refStats.cexAnalysisSolverTimer.start();

    // prepare the initial list of formulas
    itpProver.init();
    List<T> interpolationIds= new Vector<T>(interpolationBlocks.size());
    for (InterpolationBlock ib : interpolationBlocks){
      T id = itpProver.addFormula(ib.getPathFormula().getFormula());
      interpolationIds.add(id);
    }

    CounterexampleTraceInfo info;
    boolean spurious = itpProver.isUnsat();

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
      // the last formula trace scope
      int previousPrimedNo = 0;
      // how shorter is the current formula list compared to the original one
      int offset = 0;
      // map: scope below the current one -> last interpolant from that scope
      SortedMap<Integer, Formula> itpContext = new TreeMap<Integer, Formula>();
      Formula lastItp = null;

      System.out.println();
      System.out.println("Interpolants/predicates after blocks:");

      for (int i=0; i<interpolationBlocks.size()-1; i++ ){
        InterpolationBlock ib = interpolationBlocks.get(i);
        Integer primedNo = ib.getPrimedNo();
        ARTElement artElement = ib.getArtElement();

        if (lastItp!= null && lastItp.isFalse()){
          // if the last interpolant was false, then the interpolants below are empty
          itpContext.clear();
          Set<AbstractionPredicate> predicates = new HashSet<AbstractionPredicate>();
          System.out.println("\t- blk "+i+" : "+lastItp+",\t"+predicates);
          info.addPredicatesForRefinement(artElement, predicates);
        } else if (primedNo == previousPrimedNo){
          // we continue in the same branch - take the next formula from the current context
          lastItp = itpProver.getInterpolant(interpolationIds.subList(itpContext.size(), i-offset+1));
          Set<AbstractionPredicate> predicates = getPrecisionForElements(lastItp);
          System.out.println("\t- blk "+i+" : "+lastItp+",\t"+predicates);
          info.addPredicatesForRefinement(artElement, predicates);
        } else if (primedNo < previousPrimedNo){
          // we return to a previous branch - the formula list remains the same, but interpolant gets possibly smaller
          itpContext.remove(primedNo);
          lastItp = itpProver.getInterpolant(interpolationIds.subList(itpContext.size(), i-offset+1));
          Set<AbstractionPredicate> predicates = getPrecisionForElements(lastItp);
          System.out.println("\t- blk "+i+" : "+lastItp+",\t"+predicates);
          info.addPredicatesForRefinement(artElement, predicates);
        } else {
          // we switch to an environmental branch - the context get bigger and a new formula list is required
          itpProver.reset();
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
          lastItp = itpProver.getInterpolant(interpolationIds.subList(itpContext.size(), i-offset+1));
          Set<AbstractionPredicate> predicates = getPrecisionForElements(lastItp);
          System.out.println("\t- blk "+i+" : "+lastItp+",\t"+predicates);
          info.addPredicatesForRefinement(artElement, predicates);
        }
        previousPrimedNo = primedNo;
      }
      assert itpContext.isEmpty();
    }
    else {
      // this is a real bug, notify the user
      info = new CounterexampleTraceInfo(false);
    }

    itpProver.reset();
    refStats.cexAnalysisTimer.stop();

    return info;
  }

  /**
   * Constructs a formula list from interpolationBlocks to get an interpolant for block i.
   * The interpolants for parts already covered are given in itpContext and are used in the
   * formula list instead of these parts.
   *
   * @param <T>
   * @param interpolationBlocks
   * @param itpContext
   * @param i
   * @return
   */
  private List<Formula> constructFormulaList(List<InterpolationBlock> interpolationBlocks, List<Formula> itpContext, int i) {
    List<Formula> formulaList = new Vector<Formula>(interpolationBlocks.size());


    int mark = interpolationBlocks.get(i).getPrimedNo();
    for (int j=i; j<interpolationBlocks.size(); j++){
      int primedNo = interpolationBlocks.get(j).getPrimedNo();

      if (primedNo == mark){
        // we continue on the same level
        Formula f = interpolationBlocks.get(j).getPathFormula().getFormula();
        formulaList.add(f);
      } else if (primedNo < mark){
        // return to a previous trace - use the last interpolant from itpContext for the part

      } else {

      }
    }


    return formulaList;
  }

  /**
   * Divides the interpolant into atom formulas un unprimes them.
   * @param itp
   * @param ib
   * @return
   */
  private Set<AbstractionPredicate> getPrecisionForElements(Formula itp) {

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


