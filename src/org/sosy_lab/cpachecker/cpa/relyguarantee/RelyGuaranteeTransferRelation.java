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
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.AssumeEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageElement;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.GuardedEdgeAutomatonPredicateElement;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.productautomaton.ProductAutomatonElement;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractElement;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionManager;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;
import org.sosy_lab.cpachecker.fshell.fql2.translators.cfa.ToFlleShAssumeEdgeTranslator;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.cpachecker.util.ecp.ECPPredicate;
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
public class RelyGuaranteeTransferRelation  extends PredicateTransferRelation {

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

  private final LogManager logger;
  private final PredicateAbstractionManager formulaManager;
  private final PathFormulaManager pathFormulaManager;

  //private final MathsatFormulaManager manager;
  private final RelyGuaranteeCPA cpa;


  public RelyGuaranteeTransferRelation(RelyGuaranteeCPA pCpa) throws InvalidConfigurationException {
    pCpa.getConfiguration().inject(this, RelyGuaranteeTransferRelation.class);

    logger = pCpa.getLogger();
    cpa = pCpa;
    formulaManager = pCpa.getPredicateManager();
    pathFormulaManager = pCpa.getPathFormulaManager();
    manager = (MathsatFormulaManager)pCpa.getFormulaManager();
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
      PathFormula newPF = convertEdgeToPathFormula(element, edge);
      logger.log(Level.ALL, "New path formula is", newPF);

      Map<Integer, RelyGuaranteeCFAEdge> primedMap = new HashMap<Integer, RelyGuaranteeCFAEdge>(element.getPrimedMap());
      if (edge.getEdgeType() == CFAEdgeType.RelyGuaranteeCFAEdge){
       primedMap.put(element.getPathFormula().getPrimedNo()+1, ((RelyGuaranteeCFAEdge)edge));
      }

      // check whether to do abstraction
      boolean doAbstraction = isBlockEnd(loc, newPF, edge);

      if (doAbstraction) {
        return Collections.singleton(
            new RelyGuaranteeAbstractElement.ComputeAbstractionElement(newPF, element.getAbstractionFormula(), loc, edge, cpa.getThreadId(), primedMap));

      } else {
        return handleNonAbstractionFormulaLocation(newPF, element.getAbstractionFormula(), edge, primedMap);
      }


    } finally {
      postTimer.stop();
    }
  }

  /**
   * Does special things when we do not compute an abstraction for the
   * successor. This currently only envolves an optional sat check.
   * @param pPrimedMap
   */
  private Set<RelyGuaranteeAbstractElement> handleNonAbstractionFormulaLocation( PathFormula pathFormula, AbstractionFormula abstractionFormula, CFAEdge edge,  Map<Integer, RelyGuaranteeCFAEdge> pPrimedMap) {
    boolean satCheck = (satCheckBlockSize > 0) && (pathFormula.getLength() >= satCheckBlockSize);

    logger.log(Level.FINEST, "Handling non-abstraction location",
        (satCheck ? "with satisfiability check" : ""));

    if (satCheck) {
      satCheckTimer.start();

      boolean unsat = formulaManager.unsat(abstractionFormula, pathFormula);

      satCheckTimer.stop();

      if (unsat) {
        numSatChecksFalse++;
        logger.log(Level.FINEST, "Abstraction & PathFormula is unsatisfiable.");
        return Collections.emptySet();
      }
    }

    // create the new abstract element for non-abstraction location
    return Collections.singleton(
        new RelyGuaranteeAbstractElement(pathFormula, abstractionFormula, edge, cpa.getThreadId(), pPrimedMap));
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
  public PathFormula convertEdgeToPathFormula(RelyGuaranteeAbstractElement element, CFAEdge edge) throws CPATransferException {
    PathFormula oldPathFormula = element.getPathFormula();

    pathFormulaTimer.start();

    PathFormula newPathFormula = null;
    if (edge.getEdgeType() == CFAEdgeType.RelyGuaranteeCFAEdge){
      RelyGuaranteeCFAEdge rgEdge = (RelyGuaranteeCFAEdge) edge;
      assert !rgEdge.toString().contains("dummy");
      newPathFormula = handleEnvFormula(oldPathFormula, rgEdge);
    } else {
      newPathFormula = pathFormulaManager.makeAnd(oldPathFormula, edge, cpa.getThreadId());
    }


    return newPathFormula;
  }


  // Create a path formula from an env. edge and a local pathFormula
  private PathFormula  handleEnvFormula(PathFormula localPF, RelyGuaranteeCFAEdge edge) throws CPATransferException {
   PathFormula envPF = edge.getPathFormula();
   // prime the env. path formula so it does not collide with the local path formula
   int offset = localPF.getPrimedNo() + 1;
   PathFormula primedEnvPF = pathFormulaManager.primePathFormula(envPF, offset);
   // make equalities between the last global values in the local and env. path formula
   PathFormula matchedPF = pathFormulaManager.matchPaths(localPF, primedEnvPF, this.cpa.globalVariablesSet, offset);

   pathFormulaManager.inject(edge.getLocalEdge(), this.cpa.globalVariablesSet, offset, primedEnvPF.getSsa());
   // apply the strongest postcondition
   PathFormula finalPF = pathFormulaManager.makeAnd(matchedPF, edge.getLocalEdge());

   if (debug){
     System.out.println("\tby pf '"+finalPF+"'");
   }


    return finalPF;
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
  protected boolean isBlockEnd(CFANode succLoc, PathFormula pf, CFAEdge edge) {
    boolean result = false;

    if (edge.getEdgeType() == CFAEdgeType.RelyGuaranteeCFAEdge){
      return true;
    }

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

  private PredicateAbstractElement strengthen(CFANode pNode, PredicateAbstractElement pElement, GuardedEdgeAutomatonPredicateElement pAutomatonElement) throws CPATransferException {
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
  }
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

    PathFormula pf = pathFormulaManager.makeAnd(pElement.getPathFormula(), asmpt);

    //TODO the template is wrong
    return replacePathFormula(pElement, pf);
  }

  /**
   * Returns a new element with a given pathFormula. All other fields stay equal.
   */
  private RelyGuaranteeAbstractElement replacePathFormula(RelyGuaranteeAbstractElement oldElement, PathFormula newPathFormula) {
    if (oldElement instanceof RelyGuaranteeAbstractElement.ComputeAbstractionElement) {
      CFANode loc = ((RelyGuaranteeAbstractElement.ComputeAbstractionElement) oldElement).getLocation();
      return new RelyGuaranteeAbstractElement.ComputeAbstractionElement(newPathFormula, oldElement.getAbstractionFormula(), loc,  cpa.getThreadId(), oldElement.getPrimedMap() );
    } else {
      assert !(oldElement instanceof RelyGuaranteeAbstractElement.AbstractionElement);
      return new RelyGuaranteeAbstractElement(newPathFormula, oldElement.getAbstractionFormula(), cpa.getThreadId(), oldElement.getPrimedMap());
    }
  }

  protected RelyGuaranteeAbstractElement strengthenSatCheck(RelyGuaranteeAbstractElement pElement) {
    logger.log(Level.FINEST, "Checking for feasibility of path because error has been found");

    strengthenCheckTimer.start();
    PathFormula pathFormula = pElement.getPathFormula();
    boolean unsat = formulaManager.unsat(pElement.getAbstractionFormula(), pathFormula);
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
      AbstractionFormula abs = formulaManager.makeTrueAbstractionFormula(pathFormula);

      PathFormula newPathFormula = pathFormulaManager.makeEmptyPathFormula(pathFormula);

      // TODO check if correct
      return new RelyGuaranteeAbstractElement.AbstractionElement(newPathFormula, abs , cpa.getThreadId(), pElement.getPrimedMap());
    }
  }
}
