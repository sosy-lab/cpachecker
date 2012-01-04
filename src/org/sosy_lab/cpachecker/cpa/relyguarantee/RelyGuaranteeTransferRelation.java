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
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
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
  protected final  PredicateAbstractionManager formulaManager;
  protected  MathsatFormulaManager manager;

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
    manager = (MathsatFormulaManager)pCpa.formulaManager;
    formulaManager = cpa.predicateManager;
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
      //doAbstraction = true;

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
   * previous pathFormula.
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
      throw new UnrecognizedCFAEdgeException("Combined edges currently not supported");
      /*RelyGuaranteeApplicationInfo appInfo = element.getAppInfo();
      RelyGuaranteeCombinedCFAEdge rgEdge = (RelyGuaranteeCombinedCFAEdge) edge;
      pair = handleCombinedEnvFormula(oldPathFormula, rgEdge);*/
    }
    else {
      // local application
      //Map<RelyGuaranteeCFAEdge, PathFormula> edgeMap = new HashMap<RelyGuaranteeCFAEdge, PathFormula>();
      PathFormula newPf = pfManager.makeAnd(oldPathFormula, edge, cpa.getTid());
      pair = Pair.of(newPf, null);
    }


    return pair;
  }


  // Create a path formula from an env. edge and a local pathFormula
  private Pair<PathFormula, RelyGuaranteeApplicationInfo>  handleEnvFormula(PathFormula localPf, RelyGuaranteeCFAEdge rgEdge, RelyGuaranteeAbstractElement element) throws CPATransferException {

    RelyGuaranteeApplicationInfo appInfo = element.getAppInfo();
    if (appInfo == null){
      appInfo = new RelyGuaranteeApplicationInfo();
    }

    // renamed & apply env transition
    Pair<PathFormula, Integer> pair = renamedEnvApplication(localPf, rgEdge, cpa.getTid());
    PathFormula appPf = pair.getFirst();

    // remember env application for refinement
    appInfo.putEnvApplication(pair.getSecond(), rgEdge);

    // add local formula
    appPf = pfManager.makeAnd(localPf, appPf);

    if (debug){
      System.out.println("\tby pf '"+appPf+"'");
    }

    return Pair.of(appPf, appInfo);
  }



  /**
   *  Create a path formula from combined environmental edges.
   * @param localPf
   * @param edge
   * @return
   * @throws CPATransferException
   */
 /*private Pair<PathFormula, RelyGuaranteeApplicationInfo>  handleCombinedEnvFormula(PathFormula localPf, RelyGuaranteeCombinedCFAEdge edge) throws CPATransferException {
    // holds if env. applications are abstracted
    int tid = cpa.getTid();
    assert localPf.getPrimedNo() == tid;
    assert edge.getEnvEdges().size() >= 1;

    RelyGuaranteeApplicationInfo appInfo = new RelyGuaranteeApplicationInfo(localPf);

    // construct path formulas after applying env. transitions and merge them
    PathFormula combinedPf = pfManager.makeEmptyPathFormula();
    for (int i=0; i<edge.getEdgeNo(); i++){
      RelyGuaranteeCFAEdge rgEdge = edge.getEnvEdges().get(i);

      // remember env application for refinement
      PathFormula appPf = envApplicationPF(localPf, rgEdge, tid);
      appInfo.putEnvApplication(rgEdge, appPf);

      if (!combinedPf.getFormula().isTrue()){
        combinedPf = pfManager.makeRelyGuaranteeOr(combinedPf, appPf, tid);
      } else {
        combinedPf = appPf;
      }
    }

    combinedPf = pfManager.makeAnd(localPf, combinedPf);

    if (debug){
      System.out.println("\tby pf '"+combinedPf+"'");
    }

    return Pair.of(combinedPf, appInfo);
  }*/

  /**
   * Returns a path formula representing the effect of applying env. edge on a path formula.
   * This formula is renamed to an unique number, which is given globally.
   * @param localPf
   * @param rgEdge
   * @param tid
   * @return
   * @throws CPATransferException
   */
  private Pair<PathFormula, Integer> renamedEnvApplication(PathFormula localPf, RelyGuaranteeCFAEdge rgEdge, int tid) throws CPATransferException {

    // apply
    PathFormula appPf = envApplicationPF(localPf, rgEdge, tid);

    // rename
    Map<Integer, Integer> aMap = new HashMap<Integer, Integer>();
    Integer id = uniqueId;
    aMap.put(rgEdge.getSourceTid(), id);
    appPf = pfManager.adjustPrimedNo(appPf, aMap);

    // increment unique number
    uniqueId++;

    return Pair.of(appPf, id);
  }

  /**
   * Returns a path formula representing the effect of applying env. edge on a path formula.
   * @param localPf path formula of the destination element
   * @param edge environmental edge
   * @param tid destination thread
   * @return
   * @throws CPATransferException
   */
  public PathFormula envApplicationPF(PathFormula localPf, RelyGuaranteeCFAEdge rgEdge, int tid) throws CPATransferException{

    // take the filter of the env transition
    PathFormula appPf = rgEdge.getFilter();

    // build equalities over last values in the filter and local pf
    // TODO shorter equalities for abstraction
    PathFormula eqPf    = pfManager.makePrimedEqualities(localPf, tid, appPf, rgEdge.getSourceTid());
    appPf = pfManager.makeAnd(appPf, eqPf);

    // apply the env. operation in thread tid
    appPf = pfManager.makeAnd(appPf, rgEdge.getLocalEdge(), tid);

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
      athreshold = (this.manager.countAtoms(pf.getFormula()) >= atomThreshold) ;
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
