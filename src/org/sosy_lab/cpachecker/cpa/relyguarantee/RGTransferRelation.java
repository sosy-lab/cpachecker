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
import java.util.List;
import java.util.Map;
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
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGAbstractElement.ComputeAbstractionElement;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.RGEnvTransitionManager;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGCFAEdge;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.bdd.BDDRegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.SSAMapManager;
import org.sosy_lab.cpachecker.util.predicates.mathsat.MathsatFormulaManager;

/**
 * Transfer relation for symbolic predicate abstraction. First it computes
 * the strongest post for the given CFA edge. Afterwards it optionally
 * computes an abstraction.
 */
@Options(prefix="cpa.rg")
public class RGTransferRelation  implements TransferRelation {

  @Option(name="blk.threshold",
      description="maximum blocksize before abstraction is forced\n"
        + "(non-negative number, special values: 0 = don't check threshold, 1 = SBE)")
        private int absBlockSize = 0;

  @Option(description="Print debugging info?")
  private boolean debug=false;

  @Option(name="blk.functions",
      description="force abstractions on function call/return")
      private boolean absOnFunction = true;

  @Option(name="blk.loops",
      description="force abstractions for each loop iteration")
      private boolean absOnLoop = true;

  @Option(name="blk.envEdge",
      description="abstract after applying an enviornmental edge")
    private boolean absOnEnvEdge = true;

  @Option(name="blk.requireThresholdAndLBE",
      description="require that both the threshold and (functions or loops) "
        + "have to be fulfilled to compute an abstraction")
        private boolean absOnlyIfBoth = false;


  @Option(description="check satisfiability when a target state has been found (should be true)")
  private boolean targetStateSatCheck = true;



  // statistics
  /*public final Timer postTimer = new Timer();
  public final Timer satCheckTimer = new Timer();
  public final Timer pathFormulaTimer = new Timer();
  public final Timer strengthenTimer = new Timer();
  public final Timer strengthenCheckTimer = new Timer();*/
  public final Timer pfConstructionTimer = new Timer();

  public int numBlkFunctions = 0;
  public int numBlkLoops = 0;
  public int numBlkThreshold = 0;
  public int numAtomThreshold = 0;
  public int numSatChecksFalse = 0;
  public int numStrengthenChecksFalse = 0;

  private final LogManager logger;
  private final RGAbstractionManager absManager;
  private final PathFormulaManager pfManager;
  private final MathsatFormulaManager fManager;
  private final RegionManager rManager;
  private final AbstractionManager aManager;
  private final SSAMapManager ssaManager;
  private final RGEnvTransitionManager etManager;

  //private final MathsatFormulaManager manager;
  private final RGCPA cpa;


  /** Unique number for env. applications in this thread */
  private int uniqueId;

  public RGTransferRelation(RGCPA pCpa) throws InvalidConfigurationException {
    pCpa.getConfiguration().inject(this, RGTransferRelation.class);

    logger = pCpa.logger;
    cpa = pCpa;
    absManager = pCpa.absManager;
    pfManager = pCpa.pfManager;
    fManager = (MathsatFormulaManager)pCpa.fManager;
    rManager = BDDRegionManager.getInstance();
    aManager = AbstractionManagerImpl.getInstance(rManager, fManager, pfManager, cpa.getConfiguration(), logger);
    ssaManager = pCpa.ssaManager;
    etManager = pCpa.etManager;
    uniqueId = 1;
  }

  @Override
  public Collection<? extends AbstractElement> getAbstractSuccessors(AbstractElement pElement,
      Precision pPrecision, CFAEdge edge) throws CPATransferException, InterruptedException {

    RGAbstractElement element = (RGAbstractElement) pElement;
    CFANode loc = edge.getSuccessor();

    // Check whether abstraction is false.
    // Such elements might get created when precision adjustment computes an abstraction.
    if (element.getAbstractionFormula().asFormula().isFalse()) {
      return Collections.emptySet();
    }


    // calculate strongest post
    Pair<PathFormula, RGApplicationInfo> pair = convertEdgeToPathFormula(element, edge);
    PathFormula newPF = pair.getFirst();
    logger.log(Level.ALL, "New path formula is", newPF);

    // check whether to do abstraction
    boolean doAbstraction = isBlockEnd(loc, newPF, edge);

    RGAbstractElement succ;
    if (doAbstraction) {
      succ = new RGAbstractElement.ComputeAbstractionElement(pair.getFirst(), element.getAbstractionFormula(), loc, cpa.getTid(), pair.getSecond());

    } else {
      succ = new RGAbstractElement(pair.getFirst(), element.getAbstractionFormula(), cpa.getTid(), pair.getSecond());
    }

    return Collections.singleton(succ);
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
  public Pair<PathFormula, RGApplicationInfo> convertEdgeToPathFormula(RGAbstractElement element, CFAEdge edge) throws CPATransferException {

    pfConstructionTimer.start();

    Pair<PathFormula, RGApplicationInfo> pair = null;
    if (edge.getEdgeType() == CFAEdgeType.RelyGuaranteeCFAEdge){
      // single edge
      RGCFAEdge rgEdge = (RGCFAEdge) edge;
      pair = handleEnvFormula(element, rgEdge);
    }
    else if (edge.getEdgeType() == CFAEdgeType.RelyGuaranteeCombinedCFAEdge){
      // combined edges
     // RGCombinedCFAEdge rgEdge = (RGCombinedCFAEdge) edge;
     // pair = handleCombinedEnvFormula(oldPathFormula, rgEdge, element);
    }
    else {
      // local application
      PathFormula oldPf = element.getPathFormula();
      PathFormula newPf = pfManager.makeAnd(oldPf, edge);
      pair = Pair.of(newPf, null);
    }

    pfConstructionTimer.stop();
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
  private Pair<PathFormula, RGApplicationInfo>  handleEnvFormula(RGAbstractElement element, RGCFAEdge rgEdge) throws CPATransferException {

    RGApplicationInfo appInfo = element.getAppInfo();
    if (appInfo == null){
      appInfo = new RGApplicationInfo();
    }

    // check if the transition has been applied before
    if (appInfo.envMap.containsValue(rgEdge.getRgEnvTransition())){
      assert false;
    }

    // renamed & apply env transition
    Pair<PathFormula, PathFormula> pair = renamedEnvApplication(element, rgEdge, appInfo);
    PathFormula appPf = pair.getFirst();
    PathFormula refPf = pair.getSecond();

    // add local formula
    PathFormula localPf = element.getPathFormula();
    if (!appPf.getFormula().isFalse()){
      appPf = pfManager.makeAnd(appPf, localPf);
      refPf = pfManager.makeAnd(refPf, localPf);
      appInfo.setRefinementFormula(refPf);
    }

    /*if (debug){
      System.out.println("\tby pf '"+appPf+"'");
    }*/

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
  /*private Pair<PathFormula, RGApplicationInfo> handleCombinedEnvFormula(PathFormula localPf, RGCombinedCFAEdge rgCombEdge, RGAbstractElement element) throws CPATransferException {
    RGApplicationInfo appInfo = element.getAppInfo();
    assert false;
    if (appInfo == null){
      appInfo = new RGApplicationInfo();
    }

    // renamed & apply env transition
    PathFormula combinedAbstractionPf = null;
    PathFormula combinedRefinementPf = null;
    for (RGCFAEdge2 rgEdge: rgCombEdge.getEnvEdges()){

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
        /*
        combinedAbstractionPf = pfManager.makeRelyGuaranteeOr(appPf, combinedAbstractionPf, cpa.getTid());
        combinedRefinementPf  = pfManager.makeRelyGuaranteeOr(refPf, combinedRefinementPf, cpa.getTid());*/
 /*     }

    }

    if (combinedAbstractionPf == null){
      // all transitions have been applied
      return null;
    }

    // add local formula
    combinedAbstractionPf = pfManager.makeAnd(combinedAbstractionPf, localPf);
    combinedRefinementPf = pfManager.makeAnd(combinedRefinementPf, localPf);

    if (debug){
      System.out.println("\tby pf '"+combinedAbstractionPf+"'");
    }

    return Pair.of(combinedAbstractionPf, appInfo);
  }*/


  /**
   * Returns a two path formulas representing the effect of an applying env.
   * The first one is meant for abstraction and the second one for refinement.
   * @param localPf
   * @param rgEdge
   * @param pAppInfo
   * @param appInfo
   * @param tid
   * @return
   * @throws CPATransferException
   */
  private Pair<PathFormula, PathFormula> renamedEnvApplication(RGAbstractElement element, RGCFAEdge rgEdge, RGApplicationInfo appInfo) throws CPATransferException {
    assert appInfo != null;

    // get abstraction & refinement formulas
    PathFormula appPf = etManager.formulaForAbstraction(element, rgEdge.getRgEnvTransition(), uniqueId);

    PathFormula refPf = null;
    if (appPf.getFormula().isFalse()){
      refPf = pfManager.makeFalsePathFormula();
    } else {
      // TODO somehow using unique instead of 1 makes bakery.simple not stop
      refPf = etManager.formulaForRefinement(element, rgEdge.getRgEnvTransition(), 1);
      Map<Integer, Integer> rMap = new HashMap<Integer, Integer>(1);
      rMap.put(1, uniqueId);
      refPf = pfManager.changePrimedNo(refPf, rMap);
    }

    // remember the application
    appInfo.putEnvApplication(uniqueId, rgEdge.getRgEnvTransition());

    // increment unique number
    uniqueId++;

    return Pair.of(appPf, refPf);
  }


  /**
   * Check whether an abstraction should be computed.
   *
   * This method implements the blk operator from the paper
   * "Adjustable Block-Encoding" [Beyer/Keremoglu/Wendler FMCAD'10].
   *
   * @param succLoc successor CFA location.
   * @param edge
   * @param thresholdReached if the maximum block size has been reached
   * @return true if succLoc is an abstraction location. For now a location is
   * an abstraction location if it has an incoming loop-back edge, if it is
   * the start node of a function or if it is the call site from a function call.
   */
  protected boolean isBlockEnd(CFANode succLoc, PathFormula pf, CFAEdge edge) {
    boolean result = false;

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

    if (this.absOnEnvEdge){
      boolean isEnvEdge = (edge.getEdgeType() == CFAEdgeType.RelyGuaranteeCFAEdge || edge.getEdgeType() == CFAEdgeType.RelyGuaranteeCombinedCFAEdge);
      result = result || isEnvEdge;
    }


    return result;
  }


  @Override
  public Collection<? extends AbstractElement> strengthen(AbstractElement pElement,
      List<AbstractElement> otherElements, CFAEdge edge, Precision pPrecision) throws CPATransferException {

    //strengthenTimer.start();
    try {

      RGAbstractElement element = (RGAbstractElement)pElement;
      if (element instanceof RGAbstractElement.AbstractionElement) {
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
      //strengthenTimer.stop();
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

  private RGAbstractElement strengthen(RGAbstractElement pElement,
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
  private RGAbstractElement replacePathFormula(RGAbstractElement oldElement, PathFormula newPathFormula) {
    if (oldElement instanceof ComputeAbstractionElement) {
      ComputeAbstractionElement cOldElement = (ComputeAbstractionElement) oldElement;
      CFANode loc = cOldElement.getLocation();
      return new RGAbstractElement.ComputeAbstractionElement(newPathFormula, cOldElement.getAbstractionFormula(), loc,  cpa.getTid(), cOldElement.getAppInfo());
    } else {
      assert !(oldElement instanceof RGAbstractElement.AbstractionElement);
      return new RGAbstractElement(newPathFormula, oldElement.getAbstractionFormula(), cpa.getTid());
    }
  }

  protected RGAbstractElement strengthenSatCheck(RGAbstractElement pElement) {
    logger.log(Level.FINEST, "Checking for feasibility of path because error has been found");



    //strengthenCheckTimer.start();
    PathFormula pathFormula = pElement.getPathFormula();
    boolean unsat = absManager.unsat(pElement.getAbstractionFormula(), pathFormula);
    //strengthenCheckTimer.stop();

    if (unsat) {
      numStrengthenChecksFalse++;
      logger.log(Level.FINEST, "Path is infeasible.");
      return null;
    } else {
      // although this is not an abstraction location, we fake an abstraction
      // because refinement code expects it to be like this
      logger.log(Level.FINEST, "Last part of the path is not infeasible.");

      // set abstraction to true (we don't know better)
      AbstractionFormula abs = absManager.makeTrueAbstractionFormula(pathFormula);

      PathFormula newPathFormula = pfManager.makeEmptyPathFormula(pathFormula);

      // TODO check if correct
      return new RGAbstractElement.AbstractionElement(newPathFormula, abs , cpa.getTid(), pElement.getPathFormula(), pElement.getAppInfo());
    }
  }
}
