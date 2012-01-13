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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageElement;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionManager;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RelyGuaranteeAbstractElement.ComputeAbstractionElement;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.bdd.BDDRegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.mathsat.MathsatFormulaManager;

/**
 * Transfer relation for symbolic predicate abstraction. First it computes
 * the strongest post for the given CFA edge. Afterwards it optionally
 * computes an abstraction.
 */
@Options(prefix="cpa.relyguarantee")
public class RelyGuaranteeTransferRelation  implements TransferRelation {

  @Option(name="blk.threshold",
      description="maximum blocksize before abstraction is forced\n"
        + "(non-negative number, special values: 0 = don't check threshold, 1 = SBE)")
        private int absBlockSize = 0;

  @Option(description="Print debugging info?")
  private boolean debug=true;


  @Option(name="blk.atomThreshold",
      description="maximum number of atoms in a path formula before abstraction is forced\n"
        + "(non-negative number, special values: 0 = don't check threshold, 1 = SBE)")
        private int atomThreshold = 0;

  @Option(name="blk.functions",
      description="force abstractions on function call/return")
      private boolean absOnFunction = true;

  @Option(name="blk.loops",
      description="force abstractions for each loop iteration")
      private boolean absOnLoop = true;

  @Option(name="blk.requireThresholdAndLBE",
      description="require that both the threshold and (functions or loops) "
        + "have to be fulfilled to compute an abstraction")
        private boolean absOnlyIfBoth = false;

  @Option(name="satCheck",
      description="maximum blocksize before a satisfiability check is done\n"
        + "(non-negative number, 0 means never, if positive should be smaller than blocksize)")
        private int satCheckBlockSize = 0;

  @Option(description="check satisfiability when a target state has been found (should be true)")
  private boolean targetStateSatCheck = true;

  @Option(description="Abstract environmental transitions using their own predicates:"
      + "0 - don't abstract, 1 - abstract filter, 2 - abstract filter and operation.")
  private int abstractEnvTransitions = 2;


  // statistics
  public final Timer postTimer = new Timer();
  public final Timer satCheckTimer = new Timer();
  public final Timer pathFormulaTimer = new Timer();
  public final Timer strengthenTimer = new Timer();
  public final Timer strengthenCheckTimer = new Timer();

  public int numBlkFunctions = 0;
  public int numBlkLoops = 0;
  public int numBlkThreshold = 0;
  public int numAtomThreshold = 0;
  public int numSatChecksFalse = 0;
  public int numStrengthenChecksFalse = 0;

  private final LogManager logger;
  private final PredicateAbstractionManager paManager;
  private final PathFormulaManager pfManager;
  private final MathsatFormulaManager fManager;
  private final RegionManager rManager;
  private final AbstractionManager aManager;

  //private final MathsatFormulaManager manager;
  private final RelyGuaranteeCPA cpa;


  // unique number for env. applications in this thread
  private Integer uniqueId;

  public RelyGuaranteeTransferRelation(RelyGuaranteeCPA pCpa) throws InvalidConfigurationException {
    pCpa.getConfiguration().inject(this, RelyGuaranteeTransferRelation.class);

    logger = pCpa.logger;
    cpa = pCpa;
    paManager = pCpa.predicateManager;
    pfManager = pCpa.pathFormulaManager;
    fManager = (MathsatFormulaManager)pCpa.formulaManager;
    rManager = BDDRegionManager.getInstance();
    aManager = AbstractionManagerImpl.getInstance(rManager, fManager, pfManager, cpa.getConfiguration(), logger);
    uniqueId = 10;
  }

  @Override
  public Collection<? extends AbstractElement> getAbstractSuccessors(AbstractElement pElement,
      Precision pPrecision, CFAEdge edge) throws CPATransferException, InterruptedException {
    postTimer.start();

    try {

      RelyGuaranteeAbstractElement element = (RelyGuaranteeAbstractElement) pElement;
      CFANode loc = edge.getSuccessor();

      // Check whether abstraction is false.
      // Such elements might get created when precision adjustment computes an abstraction.
      if (element.getAbstractionFormula().asFormula().isFalse()) {
        return Collections.emptySet();
      }


      // calculate strongest post
      Pair<PathFormula, RelyGuaranteeApplicationInfo> pair = convertEdgeToPathFormula(element, edge);
      PathFormula newPF = pair.getFirst();
      logger.log(Level.ALL, "New path formula is", newPF);

      // check whether to do abstraction
      boolean doAbstraction = isBlockEnd(loc, newPF);
      boolean isEnvEdge = (edge.getEdgeType() == CFAEdgeType.RelyGuaranteeCFAEdge || edge.getEdgeType() == CFAEdgeType.RelyGuaranteeCombinedCFAEdge);

      doAbstraction = doAbstraction || isEnvEdge;
      doAbstraction = true;

      if (doAbstraction) {
        return Collections.singleton(
            new RelyGuaranteeAbstractElement.ComputeAbstractionElement(pair.getFirst(), element.getAbstractionFormula(), loc, edge, cpa.getTid(), pair.getSecond()));

      } else {
        return handleNonAbstractionFormulaLocation(pair.getFirst(), element.getAbstractionFormula(), edge, pair.getSecond());
      }

    } finally {
      postTimer.stop();
    }
  }


  /**
   * Does special things when we do not compute an abstraction for the
   * successor. This currently only involves an optional sat check.
   * @param pPrimedMap
   */
  private Set<RelyGuaranteeAbstractElement> handleNonAbstractionFormulaLocation(PathFormula pathFormula, AbstractionFormula abstractionFormula, CFAEdge edge, RelyGuaranteeApplicationInfo appInfo) {
    boolean satCheck = (satCheckBlockSize > 0) && (pathFormula.getLength() >= satCheckBlockSize);

    logger.log(Level.FINEST, "Handling non-abstraction location",
        (satCheck ? "with satisfiability check" : ""));

    if (satCheck) {
      satCheckTimer.start();

      boolean unsat = paManager.unsat(abstractionFormula, pathFormula);

      satCheckTimer.stop();

      if (unsat) {
        numSatChecksFalse++;
        logger.log(Level.FINEST, "Abstraction & PathFormula is unsatisfiable.");
        return Collections.emptySet();
      }
    }

    // create the new abstract element for non-abstraction location
    return Collections.singleton(new RelyGuaranteeAbstractElement(pathFormula, abstractionFormula, edge, cpa.getTid(), appInfo));
  }

  /**
   * Converts an edge into a formula and creates a conjunction of it with the
   * previous pathFormula. Returns null the the transitions has been appleid before
   *
   * This method implements the strongest post operator.
   *
   * @param pathFormula The previous pathFormula.
   * @param edge  The edge to analyze.
   * @return  The new pathFormula.
   * @throws UnrecognizedCFAEdgeException
   */
  public Pair<PathFormula, RelyGuaranteeApplicationInfo> convertEdgeToPathFormula(RelyGuaranteeAbstractElement element, CFAEdge edge) throws CPATransferException {
    PathFormula oldPathFormula = element.getPathFormula();

    pathFormulaTimer.start();

    Pair<PathFormula, RelyGuaranteeApplicationInfo> pair = null;
    if (edge.getEdgeType() == CFAEdgeType.RelyGuaranteeCFAEdge){
      // single edge
      RelyGuaranteeCFAEdge rgEdge = (RelyGuaranteeCFAEdge) edge;
      pair = handleEnvFormula(oldPathFormula, rgEdge, element);
    }
    else if (edge.getEdgeType() == CFAEdgeType.RelyGuaranteeCombinedCFAEdge){
      // combined edges
      RelyGuaranteeCombinedCFAEdge rgEdge = (RelyGuaranteeCombinedCFAEdge) edge;
      pair = handleCombinedEnvFormula(oldPathFormula, rgEdge, element);
    }
    else {
      // local application
      //Map<RelyGuaranteeCFAEdge, PathFormula> edgeMap = new HashMap<RelyGuaranteeCFAEdge, PathFormula>();
      PathFormula newPf = pfManager.makeAnd(oldPathFormula, edge, cpa.getTid());
      pair = Pair.of(newPf, null);
    }


    return pair;
  }

  /**
   * Create a path formula from an env. edge and a local pathFormula.
   * @param localPf
   * @param rgEdge
   * @param element
   * @return
   * @throws CPATransferException
   */
  private Pair<PathFormula, RelyGuaranteeApplicationInfo>  handleEnvFormula(PathFormula localPf, RelyGuaranteeCFAEdge rgEdge, RelyGuaranteeAbstractElement element) throws CPATransferException {

    RelyGuaranteeApplicationInfo appInfo = element.getAppInfo();
    if (appInfo == null){
      appInfo = new RelyGuaranteeApplicationInfo();
    }

    // check if the transition has been applied before
    if (appInfo.envMap.containsValue(rgEdge.getTemplate())){
      return null;
    }

    // renamed & apply env transition
    Pair<PathFormula, PathFormula> pair = renamedEnvApplication(localPf, rgEdge, appInfo, cpa.getTid());
    PathFormula appPf = pair.getFirst();
    PathFormula refPf = pair.getSecond();

    // add local formula
    appPf = pfManager.makeAnd(localPf, appPf);
    refPf = pfManager.makeAnd(localPf, refPf);

    appInfo.setRefinementFormula(refPf);

    if (debug){
      System.out.println("\tby pf '"+appPf+"'");
    }

    return Pair.of(appPf, appInfo);
  }

  /**
   * Create a path formula for an application of a comined env. edge.
   * @param localPf
   * @param rgEdge
   * @param element
   * @return
   * @throws CPATransferException
   */
  private Pair<PathFormula, RelyGuaranteeApplicationInfo> handleCombinedEnvFormula(PathFormula localPf, RelyGuaranteeCombinedCFAEdge rgCombEdge, RelyGuaranteeAbstractElement element) throws CPATransferException {
    RelyGuaranteeApplicationInfo appInfo = element.getAppInfo();
    if (appInfo == null){
      appInfo = new RelyGuaranteeApplicationInfo();
    }

    // renamed & apply env transition
    PathFormula combinedAbstractionPf = null;
    PathFormula combinedRefinementPf = null;
    for (RelyGuaranteeCFAEdge rgEdge: rgCombEdge.getEnvEdges()){

      // skip if the transition has been applied before
      if (appInfo.envMap.containsValue(rgEdge.getTemplate())){
        continue;
      }

      Pair<PathFormula, PathFormula> pair = renamedEnvApplication(localPf, rgEdge, appInfo, cpa.getTid());
      PathFormula appPf = pair.getFirst();
      PathFormula refPf = pair.getSecond();

      if (combinedAbstractionPf == null){
        combinedAbstractionPf = appPf;
        combinedRefinementPf = refPf;
      } else {
        combinedAbstractionPf = pfManager.makeRelyGuaranteeOr(appPf, combinedAbstractionPf, cpa.getTid());
        combinedRefinementPf  = pfManager.makeRelyGuaranteeOr(refPf, combinedRefinementPf, cpa.getTid());
      }

    }

    if (combinedAbstractionPf == null){
      // all transitions have been applied
      return null;
    }

    // add local formula
    combinedAbstractionPf = pfManager.makeAnd(localPf, combinedAbstractionPf);
    combinedRefinementPf = pfManager.makeAnd(localPf, combinedRefinementPf);

    if (debug){
      System.out.println("\tby pf '"+combinedAbstractionPf+"'");
    }

    return Pair.of(combinedAbstractionPf, appInfo);
  }


  /**
   * Returns a two path formulas representing the effect of an applying env.
   * The first one is meant for abstraction and the second one for refinement.
   * @param localPf
   * @param rgEdge
   * @param appInfo
   * @param tid
   * @return
   * @throws CPATransferException
   */
  private Pair<PathFormula, PathFormula> renamedEnvApplication(PathFormula localPf, RelyGuaranteeCFAEdge rgEdge, RelyGuaranteeApplicationInfo appInfo, int tid) throws CPATransferException {
    assert appInfo != null;

    // get abstraction & refinement formulas
    PathFormula appPf = envAbstractionPf(localPf, rgEdge, tid);
    PathFormula refPf = envRefinementPf(localPf, rgEdge, tid);

    // rename
    Map<Integer, Integer> aMap = new HashMap<Integer, Integer>();
    aMap.put(rgEdge.getSourceTid(), uniqueId);
    appPf = pfManager.adjustPrimedNo(appPf, aMap);
    refPf = pfManager.adjustPrimedNo(refPf, aMap);

    // remember the application
    appInfo.putEnvApplication(uniqueId, rgEdge.getTemplate());

    // increment unique number
    uniqueId++;

    return Pair.of(appPf, refPf);
  }

  /**
   * Returns a path formula representing the effect of applying env. edge on a path formula.
   * This formula is ment for abstraction.
   * @param localPf path formula of the destination element
   * @param edge environmental edge
   * @param tid destination thread
   * @return
   * @throws CPATransferException
   */
  public PathFormula envAbstractionPf(PathFormula localPf, RelyGuaranteeCFAEdge rgEdge, int tid) throws CPATransferException{

    PathFormula appPf = null;
    if (abstractEnvTransitions == 0 || abstractEnvTransitions == 1){
      // take the filter of the env transition
      appPf = rgEdge.getFilter();

      // build equalities over last values in the filter and local pf
      // TODO shorter equalities for abstraction
      PathFormula eqPf    = pfManager.makePrimedEqualities(localPf.getSsa(), tid, appPf.getSsa(), rgEdge.getSourceTid());
      appPf = pfManager.makeAnd(appPf, eqPf);

      // apply the env. operation in thread tid
      appPf = pfManager.makeAnd(appPf, rgEdge.getLocalEdge(), tid);
    } else if (abstractEnvTransitions == 2){
      // rename filter to tid
      Formula filter = rgEdge.getFilter().getFormula();
      Map<Integer, Integer> rMap = new HashMap<Integer, Integer>();
      rMap.put(rgEdge.getSourceTid(), tid);
      filter = fManager.adjustedPrimedNo(filter, rMap);

      // increment all indexes that the transition can change by 1, minimal index is 2
      int sourceTid = rgEdge.getSourceTid();
      Set<String> nlVars = new HashSet<String>(cpa.variables.globalVars);
      nlVars.addAll(cpa.variables.localVars.get(sourceTid));
      SSAMap oldSsa = localPf.getSsa();
      SSAMapBuilder newSsaBldr = SSAMap.emptySSAMap().builder();
      for (String var : nlVars){
        String pVar = var + PathFormula.PRIME_SYMBOL + tid;
        int idx = Math.max(oldSsa.getIndex(pVar)+1, 2);
        newSsaBldr.setIndex(pVar, idx);
      }

      // add remaining indexes from the path formula
      for (String var : oldSsa.allVariables()){
        if (newSsaBldr.getIndex(var) < 1){
          newSsaBldr.setIndex(var, oldSsa.getIndex(var));
        }
      }

      SSAMap newSsa = newSsaBldr.build();



      // instantiate the filter
      Formula iFilter = fManager.instantiateNextVal(filter, oldSsa, newSsa);
      appPf = new PathFormula(iFilter, newSsa, rgEdge.getFilter().getLength());
    }

    return appPf;
  }

  /**
   * Returns a path formula representing the effect of applying env. edge on a path formula.
   * This formula is ment for refinement.
   * @param localPf path formula of the destination element
   * @param edge environmental edge
   * @param tid destination thread
   * @return
   * @throws CPATransferException
   */
  public PathFormula envRefinementPf(PathFormula localPf, RelyGuaranteeCFAEdge rgEdge, int tid) throws CPATransferException{

    PathFormula appPf = null;
    if (abstractEnvTransitions == 0 || abstractEnvTransitions == 1){
      // take the filter of the env transition
      appPf = rgEdge.getFilter();

      // build equalities over last values in the filter and local pf
      // TODO shorter equalities for abstraction
      PathFormula eqPf    = pfManager.makePrimedEqualities(localPf.getSsa(), tid, appPf.getSsa(), rgEdge.getSourceTid());
      appPf = pfManager.makeAnd(appPf, eqPf);

      // apply the env. operation in thread tid
      appPf = pfManager.makeAnd(appPf, rgEdge.getLocalEdge(), tid);
    } else if (abstractEnvTransitions == 2){
      PathFormula filter = rgEdge.getFilter();
      PathFormula oldPf = rgEdge.getSourceEnvTransition().getPathFormula();

      // build equalities between the local variables and the variables that generated the transition
      PathFormula lowPf = pfManager.makePrimedEqualities(localPf.getSsa(), tid, oldPf.getSsa(), rgEdge.getSourceTid());

      // increment all indexes of all non-local variables by 1
      int sourceTid = rgEdge.getSourceTid();
      Set<String> nlVars = new HashSet<String>(cpa.variables.globalVars);
      nlVars.addAll(cpa.variables.localVars.get(sourceTid));
      SSAMap ssa = localPf.getSsa();
      SSAMapBuilder newSsaBldr = SSAMap.emptySSAMap().builder();
      for (String var : nlVars){
        String pVar = var + PathFormula.PRIME_SYMBOL + tid;
        int idx = Math.max(ssa.getIndex(pVar)+1, 2);
        newSsaBldr.setIndex(pVar, idx);
      }
      SSAMap newSsa = newSsaBldr.build();

      // build equalities between the highest indexes of the instantiated filter
      PathFormula hiPf = pfManager.makePrimedEqualities(newSsa, tid, filter.getSsa(), rgEdge.getSourceTid());

      appPf = pfManager.makeAnd(lowPf, hiPf);
    }

    return appPf;
  }

  /**
   * Check whether an abstraction should be computed.
   *
   * This method implements the blk operator from the paper
   * "Adjustable Block-Encoding" [Beyer/Keremoglu/Wendler FMCAD'10].
   *
   * @param succLoc successor CFA location.
   * @param thresholdReached if the maximum block size has been reached
   * @return true if succLoc is an abstraction location. For now a location is
   * an abstraction location if it has an incoming loop-back edge, if it is
   * the start node of a function or if it is the call site from a function call.
   */
  protected boolean isBlockEnd(CFANode succLoc, PathFormula pf) {
    boolean result = false;

   /* if (edge.getEdgeType() == CFAEdgeType.RelyGuaranteeCFAEdge || edge.getEdgeType() == CFAEdgeType.RelyGuaranteeCombinedCFAEdge){
      return true;
    }*/

    if (absOnLoop) {
      result = succLoc.isLoopStart();
      if (result) {
        numBlkLoops++;
      }
    }
    if (absOnFunction) {
      boolean function =
        (succLoc instanceof CFAFunctionDefinitionNode) // function call edge
        || (succLoc.getEnteringSummaryEdge() != null); // function return edge
      if (function) {
        result = true;
        numBlkFunctions++;
      }
    }

    // atom number threshold
    boolean athreshold = false;
    if(atomThreshold > 0) {
      athreshold = (this.fManager.countAtoms(pf.getFormula()) >= atomThreshold) ;
      if (athreshold) {
        numAtomThreshold++;
      }
    }


    // path length treshold
    if (absBlockSize > 0) {
      boolean threshold = (pf.getLength() >= absBlockSize);
      if (threshold) {
        numBlkThreshold++;
      }

      if (absOnlyIfBoth) {
        result = result && threshold;
      } else {
        result = result || threshold;
      }
    }

    return result || athreshold;
  }


  @Override
  public Collection<? extends AbstractElement> strengthen(AbstractElement pElement,
      List<AbstractElement> otherElements, CFAEdge edge, Precision pPrecision) throws CPATransferException {

    strengthenTimer.start();
    try {

      RelyGuaranteeAbstractElement element = (RelyGuaranteeAbstractElement)pElement;
      if (element instanceof RelyGuaranteeAbstractElement.AbstractionElement) {
        // can't do anything with this object because the path formula of
        // abstraction elements has to stay "true"
        return Collections.singleton(element);
      }

      boolean errorFound = false;
      for (AbstractElement lElement : otherElements) {
        if (lElement instanceof AssumptionStorageElement) {
          element = strengthen(element, (AssumptionStorageElement)lElement);
        }

        /*     if (lElement instanceof ConstrainedAssumeElement) {
          element = strengthen(edge.getSuccessor(), element, (ConstrainedAssumeElement)lElement);
        }*/

        if (AbstractElements.isTargetElement(lElement)) {
          errorFound = true;
        }
      }

      // check satisfiability in case of error
      // (not necessary for abstraction elements)
      if (errorFound && targetStateSatCheck) {
        element = strengthenSatCheck(element);
        if (element == null) {
          // successor not reachable
          return Collections.emptySet();
        }
      }

      return Collections.singleton(element);

    } finally {
      strengthenTimer.stop();
    }
  }

 /* private PredicateAbstractElement strengthen(CFANode pNode, PredicateAbstractElement pElement, GuardedEdgeAutomatonPredicateElement pAutomatonElement) throws CPATransferException {
    PathFormula pf = pElement.getPathFormula();

    for (ECPPredicate lPredicate : pAutomatonElement) {
      AssumeEdge lEdge = ToFlleShAssumeEdgeTranslator.translate(pNode, lPredicate);

      pf = convertEdgeToPathFormula(pf, lEdge);
    }

    return replacePathFormula(pElement, pf);
  }

  private PredicateAbstractElement strengthen(CFANode pNode, PredicateAbstractElement pElement, ProductAutomatonElement.PredicateElement pAutomatonElement) throws CPATransferException {
    PathFormula pf = pElement.getPathFormula();

    for (ECPPredicate lPredicate : pAutomatonElement.getPredicates()) {
      AssumeEdge lEdge = ToFlleShAssumeEdgeTranslator.translate(pNode, lPredicate);

      pf = convertEdgeToPathFormula(pf, lEdge);
    }

    return replacePathFormula(pElement, pf);
  }*/
  /*
  private PredicateAbstractElement strengthen(CFANode pNode, RelyGuaranteeAbstractElement pElement, ConstrainedAssumeElement pAssumeElement) throws CPATransferException {
    AssumeEdge lEdge = new AssumeEdge(pAssumeElement.getExpression().getRawSignature(), pNode.getLineNumber(), pNode, pNode, pAssumeElement.getExpression(), true);

    PathFormula pf = convertEdgeToPathFormula(pElement.getPathFormula(), lEdge);

    return replacePathFormula(pElement, pf);
  }*/

  private RelyGuaranteeAbstractElement strengthen(RelyGuaranteeAbstractElement pElement,
      AssumptionStorageElement pElement2) {

    Formula asmpt = pElement2.getAssumption();

    if (asmpt.isTrue() || asmpt.isFalse()) {
      // we don't add the assumption false in order to not forget the content of the path formula
      // (we need it for post-processing)
      return pElement;
    }

    PathFormula pf = pfManager.makeAnd(pElement.getPathFormula(), asmpt);

    //TODO the template is wrong
    return replacePathFormula(pElement, pf);
  }

  /**
   * Returns a new element with a given pathFormula. All other fields stay equal.
   */
  private RelyGuaranteeAbstractElement replacePathFormula(RelyGuaranteeAbstractElement oldElement, PathFormula newPathFormula) {
    if (oldElement instanceof ComputeAbstractionElement) {
      ComputeAbstractionElement cOldElement = (ComputeAbstractionElement) oldElement;
      CFANode loc = cOldElement.getLocation();
      return new RelyGuaranteeAbstractElement.ComputeAbstractionElement(newPathFormula, cOldElement.getAbstractionFormula(), loc,  cpa.getTid(), cOldElement.getAppInfo());
    } else {
      assert !(oldElement instanceof RelyGuaranteeAbstractElement.AbstractionElement);
      return new RelyGuaranteeAbstractElement(newPathFormula, oldElement.getAbstractionFormula(), cpa.getTid());
    }
  }

  protected RelyGuaranteeAbstractElement strengthenSatCheck(RelyGuaranteeAbstractElement pElement) {
    logger.log(Level.FINEST, "Checking for feasibility of path because error has been found");



    strengthenCheckTimer.start();
    PathFormula pathFormula = pElement.getPathFormula();
    boolean unsat = paManager.unsat(pElement.getAbstractionFormula(), pathFormula);
    strengthenCheckTimer.stop();

    if (unsat) {
      numStrengthenChecksFalse++;
      logger.log(Level.FINEST, "Path is infeasible.");
      return null;
    } else {
      // although this is not an abstraction location, we fake an abstraction
      // because refinement code expects it to be like this
      logger.log(Level.FINEST, "Last part of the path is not infeasible.");

      // set abstraction to true (we don't know better)
      AbstractionFormula abs = paManager.makeTrueAbstractionFormula(pathFormula);

      PathFormula newPathFormula = pfManager.makeEmptyPathFormula(pathFormula);

      // TODO check if correct
      return new RelyGuaranteeAbstractElement.AbstractionElement(newPathFormula, abs , cpa.getTid(), pElement.getPathFormula(), pElement.getAppInfo());
    }
  }
}
