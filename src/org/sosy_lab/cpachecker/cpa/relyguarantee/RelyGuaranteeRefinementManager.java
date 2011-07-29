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

import static com.google.common.collect.Lists.transform;
import static org.sosy_lab.cpachecker.util.AbstractElements.extractElementByType;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException.Reason;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.Model;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.RelyGuaranteePathFormulaBuilder;
import org.sosy_lab.cpachecker.util.predicates.RelyGuaranteePathFormulaConstructor;
import org.sosy_lab.cpachecker.util.predicates.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.InterpolatingTheoremProver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.TheoremProver;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;


@Options(prefix="cpa.relyguarantee.refinement")
public class RelyGuaranteeRefinementManager<T1, T2> extends PredicateRefinementManager<T1, T2>  {

  private static final String BRANCHING_PREDICATE_NAME = "__ART__";
  private static final Pattern BRANCHING_PREDICATE_NAME_PATTERN = Pattern.compile(
      "^.*" + BRANCHING_PREDICATE_NAME + "(?=\\d+$)");

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
  private RelyGuaranteePathFormulaConstructor pathFormulaConstructor;


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
    this.pathFormulaConstructor = RelyGuaranteePathFormulaConstructor.getInstance(pConfig, pLogger);

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
    return buildRgCounterexampleTraceWithSpecifiedItp(targetElement,  reachedSets, threadNo, firstItpProver);
  }


  /**
   * Counterexample analysis and predicate discovery.
   * @param pAbstractTrace abstract trace of the error path
   * @param pItpProver interpolation solver used
   * @return counterexample info with predicated information
   * @throws CPAException
   */
  private <T> CounterexampleTraceInfo buildCounterexampleTraceWithSpecifiedItp(
      List<RelyGuaranteeAbstractElement> pAbstractTrace, Set<ARTElement> elementsOnPath, InterpolatingTheoremProver<T> pItpProver) throws CPAException, InterruptedException {

    logger.log(Level.FINEST, "Building counterexample trace");

    List<Formula> f = getFormulasForTrace(pAbstractTrace);

    if (useBitwiseAxioms) {
      Formula bitwiseAxioms = fmgr.makeTrue();

      for (Formula fm : f) {
        Formula a = fmgr.getBitwiseAxioms(fm);
        if (!a.isTrue()) {
          bitwiseAxioms = fmgr.makeAnd(bitwiseAxioms, a);
        }
      }

      if (!bitwiseAxioms.isTrue()) {
        logger.log(Level.ALL, "DEBUG_3", "ADDING BITWISE AXIOMS TO THE",
            "LAST GROUP: ", bitwiseAxioms);
        int lastIndex = f.size()-1;
        f.set(lastIndex, fmgr.makeAnd(f.get(lastIndex), bitwiseAxioms));
      }
    }

    f = Collections.unmodifiableList(f);

    logger.log(Level.ALL, "Counterexample trace formulas:", f);

    if (maxRefinementSize > 0) {
      Formula cex = fmgr.makeTrue();
      for (Formula formula : f) {
        cex = fmgr.makeAnd(cex, formula);
      }
      int size = fmgr.dumpFormula(cex).length();
      if (size > maxRefinementSize) {
        logger.log(Level.FINEST, "Skipping refinement because input formula is", size, "bytes large.");
        throw new RefinementFailedException(Reason.TooMuchUnrolling, null);
      }
    }

    logger.log(Level.FINEST, "Checking feasibility of counterexample trace");

    // now f is the DAG formula which is satisfiable iff there is a
    // concrete counterexample

    // create a working environment
    pItpProver.init();

    refStats.cexAnalysisSolverTimer.start();

    if (shortestTrace && getUsefulBlocks) {
      f = Collections.unmodifiableList(getUsefulBlocks(f, useSuffix, useZigZag));
    }

    if (dumpInterpolationProblems) {
      int k = 0;
      for (Formula formula : f) {
        String dumpFile = String.format(formulaDumpFilePattern,
                    "interpolation", refStats.cexAnalysisTimer.getNumberOfIntervals(), "formula", k++);
        dumpFormulaToFile(formula, new File(dumpFile));
      }
    }

    List<T> itpGroupsIds = new ArrayList<T>(f.size());
    for (int i = 0; i < f.size(); i++) {
      itpGroupsIds.add(null);
    }

    boolean spurious;
    if (getUsefulBlocks || !shortestTrace) {
      // check all formulas in f at once

      for (int i = useSuffix ? f.size()-1 : 0;
      useSuffix ? i >= 0 : i < f.size(); i += useSuffix ? -1 : 1) {

        itpGroupsIds.set(i, pItpProver.addFormula(f.get(i)));
      }
      spurious = pItpProver.isUnsat();

    } else {
      spurious = checkInfeasabilityOfShortestTrace(f, itpGroupsIds, pItpProver);
    }
    assert itpGroupsIds.size() == f.size();
    assert !itpGroupsIds.contains(null); // has to be filled completely

    logger.log(Level.FINEST, "Counterexample trace is", (spurious ? "infeasible" : "feasible"));

    CounterexampleTraceInfo info;

    if (spurious) {
      info = new CounterexampleTraceInfo();

      // the counterexample is spurious. Extract the predicates from
      // the interpolants

      // how to partition the trace into (A, B) depends on whether
      // there are function calls involved or not: in general, A
      // is the trace from the entry point of the current function
      // to the current point, and B is everything else. To implement
      // this, we keep track of which function we are currently in.
      // if we don't want "well-scoped" predicates, A always starts at the beginning
      Deque<Integer> entryPoints = null;
      if (wellScopedPredicates) {
        entryPoints = new ArrayDeque<Integer>();
        entryPoints.push(0);
      }
      boolean foundPredicates = false;

      for (int i = 0; i < f.size()-1; ++i) {
        // last iteration is left out because B would be empty
        final int start_of_a = (wellScopedPredicates ? entryPoints.peek() : 0);
        RelyGuaranteeAbstractElement e = pAbstractTrace.get(i);

        logger.log(Level.ALL, "Looking for interpolant for formulas from",
            start_of_a, "to", i);

        refStats.cexAnalysisSolverTimer.start();
        Formula itp = pItpProver.getInterpolant(itpGroupsIds.subList(start_of_a, i+1));
        refStats.cexAnalysisSolverTimer.stop();

        if (dumpInterpolationProblems) {
          String dumpFile = String.format(formulaDumpFilePattern,
                  "interpolation", refStats.cexAnalysisTimer.getNumberOfIntervals(), "interpolant", i);
          dumpFormulaToFile(itp, new File(dumpFile));
        }

        if (itp.isTrue()) {
          logger.log(Level.ALL, "For step", i, "got no interpolant.");

        } else {
          foundPredicates = true;
          Collection<AbstractionPredicate> preds;

          if (itp.isFalse()) {
            preds = ImmutableSet.of(amgr.makeFalsePredicate());
          } else {
            preds = getAtomsAsPredicates(itp);
          }
          assert !preds.isEmpty();
          info.addPredicatesForRefinement(e, preds);

          logger.log(Level.ALL, "For step", i, "got:",
              "interpolant", itp,
              "predicates", preds);

          if (dumpInterpolationProblems) {
            String dumpFile = String.format(formulaDumpFilePattern,
                        "interpolation", refStats.cexAnalysisTimer.getNumberOfIntervals(), "atoms", i);
            Collection<Formula> atoms = Collections2.transform(preds,
                new Function<AbstractionPredicate, Formula>(){
                      @Override
                      public Formula apply(AbstractionPredicate pArg0) {
                        return pArg0.getSymbolicAtom();
                      }
                });
            printFormulasToFile(atoms, new File(dumpFile));
          }
        }

        // TODO wellScopedPredicates have been disabled

        // TODO the following code relies on the fact that there is always an abstraction on function call and return

        // If we are entering or exiting a function, update the stack
        // of entry points
        // TODO checking if the abstraction node is a new function
//        if (wellScopedPredicates && e.getAbstractionLocation() instanceof CFAFunctionDefinitionNode) {
//          entryPoints.push(i);
//        }
          // TODO check we are returning from a function
//        if (wellScopedPredicates && e.getAbstractionLocation().getEnteringSummaryEdge() != null) {
//          entryPoints.pop();
//        }
      }

      if (!foundPredicates) {
        throw new RefinementFailedException(RefinementFailedException.Reason.InterpolationFailed, null);
      }

    } else {
      // this is a real bug, notify the user

      // get the branchingFormula and add it to the solver environment
      // this formula contains predicates for all branches we took
      // this way we can figure out which branches make a feasible path
      Formula branchingFormula = buildBranchingFormula(elementsOnPath);
      pItpProver.addFormula(branchingFormula);

      Map<Integer, Boolean> preds;
      Model model;

      // need to ask solver for satisfiability again,
      // otherwise model doesn't contain new predicates
      boolean stillSatisfiable = !pItpProver.isUnsat();
      if (!stillSatisfiable) {
        logger.log(Level.WARNING, "Could not get precise error path information because of inconsistent reachingPathsFormula!");

        int k = 0;
        for (Formula formula : f) {
          String dumpFile =
              String.format(formulaDumpFilePattern, "interpolation",
                      refStats.cexAnalysisTimer.getNumberOfIntervals(), "formula", k++);
          dumpFormulaToFile(formula, new File(dumpFile));
        }
        String dumpFile =
            String.format(formulaDumpFilePattern, "interpolation",
                refStats.cexAnalysisTimer.getNumberOfIntervals(), "formula", k++);
        dumpFormulaToFile(branchingFormula, new File(dumpFile));

        preds = Maps.newTreeMap();
        model = new Model(fmgr);

      } else {
        model = pItpProver.getModel();

        if (model.isEmpty()) {
          logger.log(Level.WARNING, "No satisfying assignment given by solver!");
          preds = Maps.newTreeMap();
        } else {
          preds = getPredicateValuesFromModel(model);
        }
      }

      info = new CounterexampleTraceInfo(f, model, preds);
    }

    pItpProver.reset();

    // update stats
    refStats.cexAnalysisTimer.stop();

    logger.log(Level.ALL, "Counterexample information:", info);

    return info;

  }





  protected Path computePath(ARTElement pLastElement, ReachedSet pReached) throws InterruptedException, CPAException {
    return ARTUtils.getOnePathTo(pLastElement);
  }

  protected List<Triple<ARTElement, CFANode, RelyGuaranteeAbstractElement.AbstractionElement>> transformPath(Path pPath) throws CPATransferException {
    List<Triple<ARTElement, CFANode, RelyGuaranteeAbstractElement.AbstractionElement>> result = Lists.newArrayList();

    for (ARTElement ae : transform(pPath, Pair.<ARTElement>getProjectionToFirst())) {
      RelyGuaranteeAbstractElement pe = extractElementByType(ae, RelyGuaranteeAbstractElement.class);
      if (pe instanceof AbstractionElement) {
        CFANode loc = AbstractElements.extractLocation(ae);
        result.add(Triple.of(ae, loc, (AbstractionElement)pe));
      }
    }

    // TODO does not have to be true under rely guarantee
    //assert pPath.getLast().getFirst() == result.get(result.size()-1).getFirst();
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
  public List<PathFormula> getRGFormulaForElement(ARTElement target, ReachedSet[] reachedSets, int threadNo) throws InterruptedException, CPAException{

    assert reachedSets[threadNo].contains(target);
    // get the set of ARTElement that have been abstracted
    Path cfaPath = computePath(target, reachedSets[threadNo]);
    System.out.println("The error trace in thread "+threadNo+" is "+cfaPath);
    List<Triple<ARTElement, CFANode, RelyGuaranteeAbstractElement.AbstractionElement>> path = transformPath(cfaPath);

    System.out.println("Abstraction elements are: ");
    for (Triple<ARTElement, CFANode, RelyGuaranteeAbstractElement.AbstractionElement> triple : path){
      System.out.println("- id:"+triple.getFirst().getElementId()+" at loc "+triple.getSecond());
    }

    List<PathFormula> rgResult = new ArrayList<PathFormula>(path.size());

    for (Triple<ARTElement, CFANode, RelyGuaranteeAbstractElement.AbstractionElement> triple : path){
      AbstractionElement ae = triple.getThird();
      RelyGuaranteePathFormulaBuilder builder = ae.getOldPathBuilder();
      PathFormula builderPF = pathFormulaConstructor.constructFromEdges(builder);
      // in any case builder formula with default env. values and path formula should be the same
      assert builderPF.equals(ae.getAbstractionFormula().getBlockPathFormula());


      List<RelyGuaranteeCFAEdge> rgEdges = pathFormulaConstructor.getRelyGuaranteeCFAEdges(builder);
      printEnvEdgesApplied(rgEdges);

      if (rgEdges.size() == 0){
        //rgResult.add(ae.getAbstractionFormula().getBlockPathFormula());
      } else {
        System.out.println("Some env tr. have been applied");
        // some env have been applied, so the template has to be instantiated
        //List<PathFormula> envFormulas = instantiateTemplateByEnvTransitions(template, reachedSets);
        //rgResult.addAll(envFormulas);
      }
    }
    return rgResult;
  }

  /**
   * Returns a path formula instantiated from the template by env transitions
   * and primed path formulas that generated those env transitions
   * @param pTemplate
   * @return
   * @throws CPAException
   * @throws InterruptedException
   */
  private List<PathFormula> instantiateTemplateByEnvTransitions(RelyGuaranteeFormulaTemplate template, ReachedSet[] reachedSets) throws InterruptedException, CPAException {
    List<PathFormula> rgResult = new ArrayList<PathFormula>();
    // the effect of applying all env transitions - it consists of applied operator and equalities
    PathFormula overallEnvEffectPF = template.getLocalPathFormula();
    // the highest number of primes in the env path formulas
    int maximumPrimedNo = 0;

    for (Pair<RelyGuaranteeCFAEdge, PathFormula> pair : template.getEnvTransitionApplied()){
      // get the path formula that generated the env transition
      RelyGuaranteeCFAEdge rgEdge = pair.getFirst();
      int sourceThr = rgEdge.getSourceTid();
      ARTElement envTarget = rgEdge.getSourceARTElement();
      List<PathFormula> envPFList = getRGFormulaForElement(envTarget, reachedSets, sourceThr);

      // prime env path formulas so they do not collide
      List<PathFormula> primedEnvPFList = new Vector<PathFormula>();
      int newMaximumPrimedNo = 0;
      for (PathFormula envPF : envPFList){
        PathFormula primedPF = pmgr.primePathFormula(envPF, maximumPrimedNo+1);;
        newMaximumPrimedNo  = Math.max(newMaximumPrimedNo , primedPF.getPrimedNo());
        primedEnvPFList.add(primedPF);
      }

      // the effect of applying this env transition
      PathFormula envEffectPF = applyEnvToInstance(pair.getFirst(), pair.getSecond(), primedEnvPFList, template);
      overallEnvEffectPF = pmgr.makeAnd(overallEnvEffectPF, envEffectPF);
      rgResult.addAll(primedEnvPFList);
    }


    rgResult.add(overallEnvEffectPF);
    return rgResult;
  }





  private PathFormula applyEnvToInstance(RelyGuaranteeCFAEdge rgEdge, PathFormula contextPF, List<PathFormula> envPFList, RelyGuaranteeFormulaTemplate template) throws CPATransferException {
    // concatenate one path formula
    PathFormula envPF = pmgr.makeEmptyPathFormula();
    for (PathFormula pf : envPFList){
      envPF = pmgr.makeAnd(envPF, pf);
    }
    // build equalities
    PathFormula eq = pmgr.buildEqualitiesOverVariables(contextPF, envPF, globalVariables);
    // apply the operation
    Triple<Formula, SSAMap, Integer> data = pmgr.makeAnd2(contextPF, rgEdge.getLocalEdge());
    PathFormula op = new PathFormula(data.getFirst(), data.getSecond(), data.getThird());

    PathFormula result = pmgr.makeAnd(eq, op);
    return result;
  }


  // TODO For testing
  private void printEnvEdgesApplied(List<RelyGuaranteeCFAEdge> rgEdges) {

    if (rgEdges.isEmpty()){
      System.out.println("Env edges applied: none");
    } else {
      System.out.println("Env edges applied:");
      for (RelyGuaranteeCFAEdge rgEdge : rgEdges){
        System.out.println("- "+rgEdge);
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
  private <T> CounterexampleTraceInfo buildRgCounterexampleTraceWithSpecifiedItp(ARTElement targetElement,  ReachedSet[] reachedSets, int threadNo , InterpolatingTheoremProver<T> pItpProver) throws CPAException, InterruptedException{
    logger.log(Level.FINEST, "Building counterexample trace");





    Path cfaPath = computePath(targetElement, reachedSets[threadNo]);
    Set<ARTElement> elementsOnPath = ARTUtils.getAllElementsOnPathsTo(targetElement);
    List<Triple<ARTElement, CFANode, RelyGuaranteeAbstractElement.AbstractionElement>> path = transformPath(cfaPath);
    List<RelyGuaranteeAbstractElement> abstractTrace = Lists.transform(path, Triple.<RelyGuaranteeAbstractElement>getProjectionToThird());

    List<Formula> f = getFormulasForTrace(abstractTrace);
    List<PathFormula> pf = getRGFormulaForElement(targetElement, reachedSets, threadNo);

    if (useBitwiseAxioms) {
      Formula bitwiseAxioms = fmgr.makeTrue();

      for (Formula fm : f) {
        Formula a = fmgr.getBitwiseAxioms(fm);
        if (!a.isTrue()) {
          bitwiseAxioms = fmgr.makeAnd(bitwiseAxioms, a);
        }
      }

      if (!bitwiseAxioms.isTrue()) {
        logger.log(Level.ALL, "DEBUG_3", "ADDING BITWISE AXIOMS TO THE",
            "LAST GROUP: ", bitwiseAxioms);
        int lastIndex = f.size()-1;
        f.set(lastIndex, fmgr.makeAnd(f.get(lastIndex), bitwiseAxioms));
      }
    }

    f = Collections.unmodifiableList(f);

    logger.log(Level.ALL, "Counterexample trace formulas:", f);

    if (maxRefinementSize > 0) {
      Formula cex = fmgr.makeTrue();
      for (Formula formula : f) {
        cex = fmgr.makeAnd(cex, formula);
      }
      int size = fmgr.dumpFormula(cex).length();
      if (size > maxRefinementSize) {
        logger.log(Level.FINEST, "Skipping refinement because input formula is", size, "bytes large.");
        throw new RefinementFailedException(Reason.TooMuchUnrolling, null);
      }
    }

    logger.log(Level.FINEST, "Checking feasibility of counterexample trace");

    // now f is the DAG formula which is satisfiable iff there is a
    // concrete counterexample

    // create a working environment
    pItpProver.init();

    refStats.cexAnalysisSolverTimer.start();

    if (shortestTrace && getUsefulBlocks) {
      f = Collections.unmodifiableList(getUsefulBlocks(f, useSuffix, useZigZag));
    }

    if (dumpInterpolationProblems) {
      int k = 0;
      for (Formula formula : f) {
        String dumpFile = String.format(formulaDumpFilePattern,
            "interpolation", refStats.cexAnalysisTimer.getNumberOfIntervals(), "formula", k++);
        dumpFormulaToFile(formula, new File(dumpFile));
      }
    }

    List<T> itpGroupsIds = new ArrayList<T>(f.size());
    for (int i = 0; i < f.size(); i++) {
      itpGroupsIds.add(null);
    }

    boolean spurious;
    if (getUsefulBlocks || !shortestTrace) {
      // check all formulas in f at once

      for (int i = useSuffix ? f.size()-1 : 0;
      useSuffix ? i >= 0 : i < f.size(); i += useSuffix ? -1 : 1) {

        itpGroupsIds.set(i, pItpProver.addFormula(f.get(i)));
      }
      spurious = pItpProver.isUnsat();

    } else {
      spurious = checkInfeasabilityOfShortestTrace(f, itpGroupsIds, pItpProver);
    }
    assert itpGroupsIds.size() == f.size();
    assert !itpGroupsIds.contains(null); // has to be filled completely

    logger.log(Level.FINEST, "Counterexample trace is", (spurious ? "infeasible" : "feasible"));

    CounterexampleTraceInfo info;

    if (spurious) {
      info = new CounterexampleTraceInfo();

      // the counterexample is spurious. Extract the predicates from
      // the interpolants

      // how to partition the trace into (A, B) depends on whether
      // there are function calls involved or not: in general, A
      // is the trace from the entry point of the current function
      // to the current point, and B is everything else. To implement
      // this, we keep track of which function we are currently in.
      // if we don't want "well-scoped" predicates, A always starts at the beginning
      Deque<Integer> entryPoints = null;
      if (wellScopedPredicates) {
        entryPoints = new ArrayDeque<Integer>();
        entryPoints.push(0);
      }
      boolean foundPredicates = false;

      for (int i = 0; i < f.size()-1; ++i) {
        // last iteration is left out because B would be empty
        final int start_of_a = (wellScopedPredicates ? entryPoints.peek() : 0);
        RelyGuaranteeAbstractElement e = abstractTrace.get(i);

        logger.log(Level.ALL, "Looking for interpolant for formulas from",
            start_of_a, "to", i);

        refStats.cexAnalysisSolverTimer.start();
        Formula itp = pItpProver.getInterpolant(itpGroupsIds.subList(start_of_a, i+1));
        refStats.cexAnalysisSolverTimer.stop();

        if (dumpInterpolationProblems) {
          String dumpFile = String.format(formulaDumpFilePattern,
              "interpolation", refStats.cexAnalysisTimer.getNumberOfIntervals(), "interpolant", i);
          dumpFormulaToFile(itp, new File(dumpFile));
        }

        if (itp.isTrue()) {
          logger.log(Level.ALL, "For step", i, "got no interpolant.");

        } else {
          foundPredicates = true;
          Collection<AbstractionPredicate> preds;

          if (itp.isFalse()) {
            preds = ImmutableSet.of(amgr.makeFalsePredicate());
          } else {
            preds = getAtomsAsPredicates(itp);
          }
          assert !preds.isEmpty();
          info.addPredicatesForRefinement(e, preds);

          logger.log(Level.ALL, "For step", i, "got:",
              "interpolant", itp,
              "predicates", preds);

          if (dumpInterpolationProblems) {
            String dumpFile = String.format(formulaDumpFilePattern,
                "interpolation", refStats.cexAnalysisTimer.getNumberOfIntervals(), "atoms", i);
            Collection<Formula> atoms = Collections2.transform(preds,
                new Function<AbstractionPredicate, Formula>(){
              @Override
              public Formula apply(AbstractionPredicate pArg0) {
                return pArg0.getSymbolicAtom();
              }
            });
            printFormulasToFile(atoms, new File(dumpFile));
          }
        }

        // TODO wellScopedPredicates have been disabled

        // TODO the following code relies on the fact that there is always an abstraction on function call and return

        // If we are entering or exiting a function, update the stack
        // of entry points
        // TODO checking if the abstraction node is a new function
        //        if (wellScopedPredicates && e.getAbstractionLocation() instanceof CFAFunctionDefinitionNode) {
        //          entryPoints.push(i);
        //        }
        // TODO check we are returning from a function
        //        if (wellScopedPredicates && e.getAbstractionLocation().getEnteringSummaryEdge() != null) {
        //          entryPoints.pop();
        //        }
      }

      if (!foundPredicates) {
        throw new RefinementFailedException(RefinementFailedException.Reason.InterpolationFailed, null);
      }

    } else {
      // this is a real bug, notify the user

      // get the branchingFormula and add it to the solver environment
      // this formula contains predicates for all branches we took
      // this way we can figure out which branches make a feasible path
      Formula branchingFormula = buildBranchingFormula(elementsOnPath);
      pItpProver.addFormula(branchingFormula);

      Map<Integer, Boolean> preds;
      Model model;

      // need to ask solver for satisfiability again,
      // otherwise model doesn't contain new predicates
      boolean stillSatisfiable = !pItpProver.isUnsat();
      if (!stillSatisfiable) {
        logger.log(Level.WARNING, "Could not get precise error path information because of inconsistent reachingPathsFormula!");

        int k = 0;
        for (Formula formula : f) {
          String dumpFile =
            String.format(formulaDumpFilePattern, "interpolation",
                refStats.cexAnalysisTimer.getNumberOfIntervals(), "formula", k++);
          dumpFormulaToFile(formula, new File(dumpFile));
        }
        String dumpFile =
          String.format(formulaDumpFilePattern, "interpolation",
              refStats.cexAnalysisTimer.getNumberOfIntervals(), "formula", k++);
        dumpFormulaToFile(branchingFormula, new File(dumpFile));

        preds = Maps.newTreeMap();
        model = new Model(fmgr);

      } else {
        model = pItpProver.getModel();

        if (model.isEmpty()) {
          logger.log(Level.WARNING, "No satisfying assignment given by solver!");
          preds = Maps.newTreeMap();
        } else {
          preds = getPredicateValuesFromModel(model);
        }
      }

      info = new CounterexampleTraceInfo(f, model, preds);
    }

    pItpProver.reset();

    // update stats
    refStats.cexAnalysisTimer.stop();

    logger.log(Level.ALL, "Counterexample information:", info);

    return info;
  }
}
