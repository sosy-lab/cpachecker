/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.explicit;

import static com.google.common.collect.Iterables.skip;
import static com.google.common.collect.Lists.transform;
import static org.sosy_lab.cpachecker.util.AbstractElements.extractElementByType;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.CounterexampleCPAChecker;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.art.ARTReachedSet;
import org.sosy_lab.cpachecker.cpa.art.Path;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitPrecision.CegarPrecision;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractElement;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateRefinementManager;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException.Reason;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.ExtendedFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.bdd.BDDRegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.TheoremProver;
import org.sosy_lab.cpachecker.util.predicates.interpolation.AbstractInterpolationBasedRefiner;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

@Options(prefix="cpa.explict.refiner")
public class ExplicitRefiner extends AbstractInterpolationBasedRefiner<Collection<AbstractionPredicate>, Pair<ARTElement, CFANode>> {

  final Timer precisionUpdate                               = new Timer();
  final Timer artUpdate                                     = new Timer();

  private final ExtendedFormulaManager fmgr;
  private final PathFormulaManager pathFormulaManager;

  private boolean predicateCpaAvailable                     = false;

  private boolean refinePredicatePrecision                  = false;

  private Set<Integer> pathHashes                           = new HashSet<Integer>();

  private Integer previousPathHash                          = null;

  private List<Pair<ARTElement, CFAEdge>> path              = null;

  private ExplicitCPA explicitCpa                           = null;

  @Option(description="whether or not to use explicit interpolation")
  boolean useExplicitInterpolation                          = false;

  @Option(description="whether or not to use the top most interpolation point")
  boolean useTopMostInterpolationPoint                      = true;

  @Option(description="whether or not to remove important variables again")
  boolean keepTrackingImportantVariables                          = true;

  @Option(description="whether or not to skip counter-example checks of variables that already are in the precision")
  boolean skipRedundantChecks                               = true;

  // statistics for explicit refinement
  private int numberOfExplicitRefinements                   = 0;
  private int numberOfPredicateRefinements                  = 0;
  private Timer timerSyntacticalPathAnalysis                = new Timer();

  // statistics for explicit interpolation
  private int numberOfCounterExampleChecks                  = 0;
  private int numberOfErrorPathElements                     = 0;
  private Timer timerCounterExampleChecks                   = new Timer();

  public static ExplicitRefiner create(ConfigurableProgramAnalysis pCpa) throws CPAException, InvalidConfigurationException {
    if (!(pCpa instanceof WrapperCPA)) {
      throw new InvalidConfigurationException(ExplicitRefiner.class.getSimpleName() + " could not find the ExplicitCPA");
    }

    ExplicitCPA explicitCpa = ((WrapperCPA)pCpa).retrieveWrappedCpa(ExplicitCPA.class);
    if (explicitCpa == null) {
      throw new InvalidConfigurationException(ExplicitRefiner.class.getSimpleName() + " needs a ExplicitCPA");
    }

    ExplicitRefiner refiner = initialiseExplicitRefiner(pCpa, explicitCpa.getConfiguration(), explicitCpa.getLogger());
    explicitCpa.getStats().addRefiner(refiner);

    return refiner;
  }

  private static ExplicitRefiner initialiseExplicitRefiner(ConfigurableProgramAnalysis pCpa, Configuration config, LogManager logger) throws CPAException, InvalidConfigurationException {
    FormulaManagerFactory factory               = null;
    ExtendedFormulaManager formulaManager       = null;
    PathFormulaManager pathFormulaManager       = null;
    Solver solver                               = null;
    AbstractionManager absManager               = null;
    PredicateRefinementManager manager          = null;

    PredicateCPA predicateCpa = ((WrapperCPA)pCpa).retrieveWrappedCpa(PredicateCPA.class);

    boolean predicateCpaInUse = predicateCpa != null;
    if (predicateCpaInUse) {
      factory                     = predicateCpa.getFormulaManagerFactory();
      formulaManager              = predicateCpa.getFormulaManager();
      pathFormulaManager          = predicateCpa.getPathFormulaManager();
      solver                      = predicateCpa.getSolver();
      absManager                  = predicateCpa.getAbstractionManager();
    } else {
      factory                     = new FormulaManagerFactory(config, logger);
      TheoremProver theoremProver = factory.createTheoremProver();
      RegionManager regionManager = BDDRegionManager.getInstance();
      formulaManager              = new ExtendedFormulaManager(factory.getFormulaManager(), config, logger);
      pathFormulaManager          = new PathFormulaManagerImpl(formulaManager, config, logger);
      solver                      = new Solver(formulaManager, theoremProver);
      absManager                  = new AbstractionManager(regionManager, formulaManager, config, logger);
    }

    manager = new PredicateRefinementManager(formulaManager,
        pathFormulaManager, solver, absManager, factory, config, logger);

    return new ExplicitRefiner(config, logger, pCpa, formulaManager, pathFormulaManager, manager, predicateCpaInUse);
  }

  protected ExplicitRefiner(final Configuration config, final LogManager logger,
      final ConfigurableProgramAnalysis pCpa, final ExtendedFormulaManager pFmgr,
      final PathFormulaManager pPathFormulaManager,
      final PredicateRefinementManager pInterpolationManager,
      final boolean predicateCpaInUse) throws CPAException, InvalidConfigurationException {

    super(config, logger, pCpa, pInterpolationManager);

    config.inject(this, ExplicitRefiner.class);

    this.fmgr                   = pFmgr;
    this.pathFormulaManager     = pPathFormulaManager;
    this.predicateCpaAvailable  = predicateCpaInUse;

    explicitCpa = ((WrapperCPA)pCpa).retrieveWrappedCpa(ExplicitCPA.class);
  }

  private Multimap<CFAEdge, String> determineReferencedVariableMapping(List<CFAEdge> cfaTrace) {
    AssignedVariablesCollector collector = new AssignedVariablesCollector();

    return collector.collectVars(cfaTrace);
  }

  private Multimap<CFANode, String> determinePrecisionIncrement(CegarPrecision precision) throws CPAException {
    timerCounterExampleChecks.start();

    Multimap<CFANode, String> increment = HashMultimap.create();

    Set<ARTElement> artTrace = new HashSet<ARTElement>();
    List<CFAEdge> cfaTrace = new ArrayList<CFAEdge>();
    for(Pair<ARTElement, CFAEdge> pathElement : path){
      artTrace.add(pathElement.getFirst());
      cfaTrace.add(pathElement.getSecond());
    }

    Set<String> irrelevantVariables = new HashSet<String>();

    Multimap<CFAEdge, String> referencedVariableMapping = determineReferencedVariableMapping(cfaTrace);

    for(int i = 0; i < path.size(); i++){
    //for(int i = path.size() - 1; i > 0; i--){

      Pair<ARTElement, CFAEdge> pathElement = path.get(i);
      numberOfErrorPathElements++;

      boolean feasible = false;

      Collection<String> referencedVariablesAtEdge = referencedVariableMapping.get(pathElement.getSecond());

      if(skipRedundantChecks) {
        int tracked = 0;
        // if all variables are already part of the precision, nothing more to do here
        for(String variableName : referencedVariablesAtEdge) {
          if(precision.allowsTrackingAt(pathElement.getSecond().getSuccessor(), variableName)) {
            tracked++;
          }
        }

        if(tracked != 0) {
          continue;
        }
      }

      if(!referencedVariablesAtEdge.isEmpty()) {
        numberOfCounterExampleChecks++;

        // variables to ignore in the current run
        irrelevantVariables.addAll(referencedVariablesAtEdge);

        try {
          // create a new CPA, which disallows tracking the "irrelevant variables"
          CounterexampleCPAChecker checker = new CounterexampleCPAChecker(Configuration.builder().setOption("counterexample.checker.ignoreGlobally", Joiner.on(",").join(irrelevantVariables)).build(),
                                                                        explicitCpa.getLogger(),
                                                                        new ReachedSetFactory(explicitCpa.getConfiguration(), explicitCpa.getLogger()),
                                                                        explicitCpa.getCFA());

          feasible = checker.checkCounterexample(path.get(0).getFirst(),
                                                  path.get(path.size() - 1).getFirst(),
                                                  artTrace);
        } catch (InterruptedException e) {
          throw new CPAException("counterexample-check failed: ", e);
        } catch (InvalidConfigurationException e) {
          throw new CPAException("counterexample-check failed: ", e);
        }
      }

      // in case the path becomes feasible ...
      if(feasible) {
        // ... add the "important" variables to the precision increment, and remove them from the irrelevant ones
        for(String importantVariable : referencedVariablesAtEdge) {
          increment.put(pathElement.getSecond().getSuccessor(), importantVariable);
          if(keepTrackingImportantVariables)
            irrelevantVariables.remove(importantVariable);
        }
      }
    }

    timerCounterExampleChecks.stop();

    return increment;
  }

  @Override
  protected void performRefinement(ARTReachedSet pReached, List<Pair<ARTElement, CFANode>> errorPath,
      CounterexampleTraceInfo<Collection<AbstractionPredicate>> counterexampleTraceInfo,
      boolean pRepeatedCounterexample) throws CPAException {

    precisionUpdate.start();

    // check if there was progress
    if (!hasMadeProgress()) {
      System.out.println(path);
      throw new RefinementFailedException(Reason.RepeatedCounterexample, null);
    }

    // get previous precision
    UnmodifiableReachedSet reached = pReached.asReachedSet();
    Precision oldPrecision = reached.getPrecision(reached.getLastElement());

    Pair<ARTElement, Precision> refinementResult =
            performRefinement(oldPrecision, errorPath, counterexampleTraceInfo);
    precisionUpdate.stop();

    artUpdate.start();

    ARTElement root = refinementResult.getFirst();

    logger.log(Level.FINEST, "Found spurious counterexample,",
        "trying strategy 1: remove everything below", root, "from ART.");

    pReached.removeSubtree(root, refinementResult.getSecond());

    artUpdate.stop();
  }

  @Override
  protected final List<Pair<ARTElement, CFANode>> transformPath(Path errorPath) {
    path = errorPath;

    // determine whether to refine explicit or predicate precision
    refinePredicatePrecision = determineRefinementStrategy();

    List<Pair<ARTElement, CFANode>> result = Lists.newArrayList();

    for (ARTElement ae : skip(transform(errorPath, Pair.<ARTElement>getProjectionToFirst()), 1)) {
      if(refinePredicatePrecision) {
        PredicateAbstractElement pe = extractElementByType(ae, PredicateAbstractElement.class);
        if (pe.isAbstractionElement()) {
          CFANode location = AbstractElements.extractLocation(ae);
          result.add(Pair.of(ae, location));
        }
      }
      else {
        result.add(Pair.of(ae, AbstractElements.extractLocation(ae)));
      }
    }

    assert errorPath.getLast().getFirst() == result.get(result.size()-1).getFirst();
    return result;
  }

  private static final Function<PredicateAbstractElement, Formula> GET_BLOCK_FORMULA
                = new Function<PredicateAbstractElement, Formula>() {
                    @Override
                    public Formula apply(PredicateAbstractElement e) {
                      assert e.isAbstractionElement();
                      return e.getAbstractionFormula().getBlockFormula();
                    };
                  };

  @Override
  protected List<Formula> getFormulasForPath(List<Pair<ARTElement, CFANode>> errorPath, ARTElement initialElement) throws CPATransferException {

    if(refinePredicatePrecision) {
      List<Formula> formulas = transform(errorPath,
          Functions.compose(
              GET_BLOCK_FORMULA,
          Functions.compose(
              AbstractElements.extractElementByTypeFunction(PredicateAbstractElement.class),
              Pair.<ARTElement>getProjectionToFirst())));

      return formulas;
    }

    else {
      PathFormula currentPathFormula = pathFormulaManager.makeEmptyPathFormula();

      List<Formula> formulas = new ArrayList<Formula>(path.size());

      // iterate over edges (not nodes)
      int i = 0;
      for (Pair<ARTElement, CFAEdge> pathElement : path) {
        i++;

        if(i == 1)
          continue;
        currentPathFormula = pathFormulaManager.makeAnd(currentPathFormula, pathElement.getSecond());

        formulas.add(currentPathFormula.getFormula());

        // reset the formula
        currentPathFormula = pathFormulaManager.makeEmptyPathFormula(currentPathFormula);
      }

      return formulas;
    }
  }

  private Pair<ARTElement, Precision> performRefinement(Precision oldPrecision,
      List<Pair<ARTElement, CFANode>> errorPath,
      CounterexampleTraceInfo<Collection<AbstractionPredicate>> pInfo) throws CPAException {

    Precision precision                           = null;
    Multimap<CFANode, String> precisionIncrement  = null;
    ARTElement interpolationPoint                 = null;

    if(useExplicitInterpolation) {
      precisionIncrement = determinePrecisionIncrement(extractExplicitPrecision(oldPrecision).getCegarPrecision());
      precision = createExplicitPrecision(extractExplicitPrecision(oldPrecision), precisionIncrement);
    }

    else {
      // create the mapping of CFA nodes to predicates, based on the counter example trace info
      PredicateMap predicateMap = new PredicateMap(pInfo.getPredicatesForRefinement(), errorPath);

      if(refinePredicatePrecision) {
        numberOfPredicateRefinements++;
        precision = createPredicatePrecision(extractPredicatePrecision(oldPrecision), predicateMap);
        interpolationPoint = predicateMap.firstInterpolationPoint.getFirst();
      }

      else {
        numberOfExplicitRefinements++;

        // determine the precision increment
        precisionIncrement = predicateMap.determinePrecisionIncrement(fmgr);

        ExplicitPrecision explicitPrecision = extractExplicitPrecision(oldPrecision);

        // get variables referenced by precision increment in the error path
        Multimap<CFANode, String> referencedVariablesInPath = determineReferencedVariablesInPath(explicitPrecision, precisionIncrement);

        // add those to the precision increment
        precisionIncrement.putAll(referencedVariablesInPath);

        // create the new precision
        precision = createExplicitPrecision(explicitPrecision, precisionIncrement);
      }
    }

    // when predicate refinement is done, the interpolation point has been set already
    // for explicit refinement it is done here
    if(interpolationPoint == null)
      interpolationPoint = determineInterpolationPoint(errorPath, precisionIncrement);

    return Pair.of(interpolationPoint, precision);
  }

  private ARTElement determineInterpolationPoint(List<Pair<ARTElement, CFANode>> errorPath, Multimap<CFANode, String> precisionIncrement) {
    ARTElement interpolationPoint = null;

    // just use first node in error path
    if(useExplicitInterpolation && useTopMostInterpolationPoint) {
      interpolationPoint = errorPath.get(0).getFirst();
    }

    // use the first node where new information is present
    else {
      for(Pair<ARTElement, CFAEdge> element : path) {
        if(precisionIncrement.containsKey(element.getSecond().getSuccessor())) {
          interpolationPoint = element.getFirst();
          break;
        }
      }
    }

    //    if(interpolationPoint != null)
      //System.out.println(errorPath);
    assert interpolationPoint != null;

    return interpolationPoint;
  }

  /**
   * This method extracts the explicit precision.
   *
   * @param precision the current precision
   * @return the explicit precision
   */
  private ExplicitPrecision extractExplicitPrecision(Precision precision) {
    ExplicitPrecision explicitPrecision = Precisions.extractPrecisionByType(precision, ExplicitPrecision.class);
    if(explicitPrecision == null) {
      throw new IllegalStateException("Could not find the ExplicitPrecision for the error element");
    }
    return explicitPrecision;
  }

  private ExplicitPrecision createExplicitPrecision(ExplicitPrecision oldPrecision,
      Multimap<CFANode, String> precisionIncrement) {

    ExplicitPrecision explicitPrecision = new ExplicitPrecision(oldPrecision);

    explicitPrecision.getCegarPrecision().addToMapping(precisionIncrement);

    return explicitPrecision;
  }

  /**
   * This method extracts the predicate precision.
   *
   * @param precision the current precision
   * @return the predicate precision, or null, if the PredicateCPA is not in use
   */
  private PredicatePrecision extractPredicatePrecision(Precision precision) {
    PredicatePrecision predicatePrecision = Precisions.extractPrecisionByType(precision, PredicatePrecision.class);
    if(predicatePrecision == null) {
      throw new IllegalStateException("Could not find the PredicatePrecision for the error element");
    }
    return predicatePrecision;
  }

  private PredicatePrecision createPredicatePrecision(PredicatePrecision oldPredicatePrecision,
                                                    PredicateMap predicateMap) {
    Multimap<CFANode, AbstractionPredicate> oldPredicateMap = oldPredicatePrecision.getPredicateMap();
    Set<AbstractionPredicate> globalPredicates = oldPredicatePrecision.getGlobalPredicates();

    ImmutableSetMultimap.Builder<CFANode, AbstractionPredicate> pmapBuilder = ImmutableSetMultimap.builder();
    pmapBuilder.putAll(oldPredicateMap);

    for(Map.Entry<CFANode, AbstractionPredicate> predicateAtLocation : predicateMap.getPredicateMapping().entries()) {
      pmapBuilder.putAll(predicateAtLocation.getKey(), predicateAtLocation.getValue());
    }

    return new PredicatePrecision(pmapBuilder.build(), globalPredicates);
  }

  private boolean determineRefinementStrategy() {
    return predicateCpaAvailable && pathHashes.contains(getErrorPathAsString(path).hashCode());
  }

  private boolean hasMadeProgress() {
    Integer errorTraceHash = getErrorPathAsString(path).hashCode();

    // in case a PredicateCPA is running, too
    // stop if the same error trace is found within two iterations
    if(predicateCpaAvailable && errorTraceHash.equals(previousPathHash)) {
      return false;
    }
    // in case only a ExplicitCPA is running:
    // stop if the same error trace is found twice
    else if(!predicateCpaAvailable && pathHashes.contains(errorTraceHash)) {
      return false;
    }

    previousPathHash = refinePredicatePrecision ? errorTraceHash : null;

    pathHashes.add(errorTraceHash);

    return true;
  }

  private Multimap<CFANode, String> determineReferencedVariablesInPath(ExplicitPrecision precision, Multimap<CFANode, String> precisionIncrement) {

    List<CFAEdge> cfaTrace = new ArrayList<CFAEdge>();
    for(int i = 0; i < path.size(); i++) {
      cfaTrace.add(path.get(i).getSecond());
    }

    timerSyntacticalPathAnalysis.start();

    // the referenced-variable-analysis has the done on basis of all variables in the precision plus the current increment
    Collection<String> referencingVariables = precision.getCegarPrecision().getVariablesInPrecision();
    referencingVariables.addAll(precisionIncrement.values());

    ReferencedVariablesCollector collector = new ReferencedVariablesCollector(referencingVariables);
    Multimap<CFANode, String> referencedVariables = collector.collectVariables(cfaTrace);
    timerSyntacticalPathAnalysis.stop();

    return referencedVariables;
  }

  private String getErrorPathAsString(List<Pair<ARTElement, CFAEdge>> errorPath)
  {
    StringBuilder sb = new StringBuilder();

    Function<Pair<?, ? extends CFAEdge>, CFAEdge> projectionToSecond = Pair.getProjectionToSecond();

    int index = 0;
    for (CFAEdge edge : Lists.transform(errorPath, projectionToSecond)) {
      sb.append(index + ": Line ");
      sb.append(edge.getLineNumber());
      sb.append(": ");
      sb.append(edge);
      sb.append("\n");

      index++;
    }

    return sb.toString();
  }

  @Override
  public void printStatistics(PrintStream out, Result result, ReachedSet reached) {
    super.printStatistics(out, result, reached);

    if(useExplicitInterpolation) {
      out.println("Explicit Interpolator:");
      out.println("  number of counter-example checks:         " + numberOfCounterExampleChecks);
      out.println("  total number of elements in error paths:  " + numberOfErrorPathElements);
      out.println("  percentage of elements checked:           " + (Math.round(((double)numberOfCounterExampleChecks / (double)numberOfErrorPathElements) * 10000) / 100.00) + "%");
      out.println("  max. time for singe check:            " + timerCounterExampleChecks.printMaxTime());
      out.println("  total time for checks:                " + timerCounterExampleChecks);
    } else {
      out.println("Explicit Refiner:");
      out.println("  number of explicit refinements:            " + numberOfExplicitRefinements);

      if(predicateCpaAvailable)
        out.println("  number of predicate refinements:           " + numberOfPredicateRefinements);

      out.println("  max. time for syntactical path analysis:   " + timerSyntacticalPathAnalysis.printMaxTime());
      out.println("  total time for syntactical path analysis:  " + timerSyntacticalPathAnalysis);
    }
  }
}
