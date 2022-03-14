// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState.getPredicateState;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocations;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.IntegerOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision.LocationInstance;
import org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicateMapWriter;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.FormulaMeasuring;
import org.sosy_lab.cpachecker.util.predicates.FormulaMeasuring.FormulaMeasures;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * This class provides the refinement strategy for the classical predicate abstraction (adding the
 * predicates from the interpolant to the precision and removing the relevant parts of the ARG).
 */
@Options(prefix = "cpa.predicate")
public class PredicateAbstractionRefinementStrategy extends RefinementStrategy
    implements StatisticsProvider {

  @Option(
      secure = true,
      name = "precision.sharing",
      description = "Where to apply the found predicates to?")
  private PredicateSharing predicateSharing = PredicateSharing.LOCATION;

  private enum PredicateSharing {
    GLOBAL, // at all locations
    SCOPE, // at all locations in the scope of the variable
    FUNCTION, // at all locations in the respective function
    LOCATION, // at all occurrences of the respective location
    LOCATION_INSTANCE, // at the n-th occurrence of the respective location in each path
    ;
  }

  @Option(
      secure = true,
      name = "refinement.predicateBasisStrategy",
      description =
          "Which predicates should be used as basis for the new precision that will be attached to"
              + " the refined part of the ARG:\n"
              + "ALL: Collect predicates from the complete ARG.\n"
              + "SUBGRAPH: Collect predicates from the removed subgraph of the ARG.\n"
              + "CUTPOINT: Only predicates from the cut-point's (pivot state) precision are"
              + " kept.\n"
              + "TARGET: Only predicates from the target state's precision are kept.")
  // Evaluations on sv-benchmarks in 2015, 2017, 2019, and 2020 showed that CUTPOINT can be much
  // slower because it needs too many refinements, the rest is better, with SUBGRAPH having slight
  // advantages. We do not want to use ALL by default because this would disable lazy abstraction.
  private PredicateBasisStrategy predicateBasisStrategy = PredicateBasisStrategy.SUBGRAPH;

  private enum PredicateBasisStrategy {
    ALL,
    SUBGRAPH,
    TARGET,
    CUTPOINT
  }

  @Option(
      secure = true,
      name = "refinement.restartAfterRefinements",
      description =
          "Do a complete restart (clearing the reached set) "
              + "after N refinements. 0 to disable, 1 for always.")
  @IntegerOption(min = 0)
  private int restartAfterRefinements = 0;

  @Option(
      secure = true,
      name = "refinement.sharePredicates",
      description =
          "During refinement, add all new predicates to the precisions "
              + "of all abstract states in the reached set.")
  private boolean sharePredicates = false;

  @Option(
      secure = true,
      name = "refinement.useBddInterpolantSimplification",
      description = "Use BDDs to simplify interpolants " + "(removing irrelevant predicates)")
  private boolean useBddInterpolantSimplification = false;

  @Option(
      secure = true,
      name = "refinement.dumpPredicates",
      description = "After each refinement, dump the newly found predicates.")
  private boolean dumpPredicates = false;

  @Option(
      secure = true,
      name = "refinement.dumpPredicatesFile",
      description = "File name for the predicates dumped after refinements.")
  @FileOption(Type.OUTPUT_FILE)
  private PathTemplate dumpPredicatesFile =
      PathTemplate.ofFormatString("refinement%04d-predicates.prec");

  protected int refinementCount = 0; // this is modulo restartAfterRefinements

  private boolean atomicPredicates = false;

  protected final LogManager logger;
  protected final PredicateAbstractionManager predAbsMgr;

  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final FormulaMeasuring formulaMeasuring;
  private final PredicateMapWriter precisionWriter;

  // statistics
  private StatCounter numberOfRefinementsWithStrategy2 =
      new StatCounter("Number of refs with location-based cutoff");
  private StatInt irrelevantPredsInItp =
      new StatInt(StatKind.SUM, "Number of irrelevant preds in interpolants");

  private StatTimer predicateCreation = new StatTimer(StatKind.SUM, "Predicate creation");
  private StatTimer precisionUpdate = new StatTimer(StatKind.SUM, "Precision update");
  protected StatTimer argUpdate = new StatTimer(StatKind.SUM, "ARG update");
  private StatTimer itpSimplification = new StatTimer(StatKind.SUM, "Itp simplification with BDDs");

  private StatInt simplifyDeltaConjunctions = new StatInt(StatKind.SUM, "Conjunctions Delta");
  private StatInt simplifyDeltaDisjunctions = new StatInt(StatKind.SUM, "Disjunctions Delta");
  private StatInt simplifyDeltaNegations = new StatInt(StatKind.SUM, "Negations Delta");
  private StatInt simplifyDeltaAtoms = new StatInt(StatKind.SUM, "Atoms Delta");
  private StatInt simplifyDeltaVariables = new StatInt(StatKind.SUM, "Variables Delta");
  private StatInt simplifyVariablesBefore = new StatInt(StatKind.SUM, "Variables Before");
  private StatInt simplifyVariablesAfter = new StatInt(StatKind.SUM, "Variables After");

  private class Stats implements Statistics {
    @Override
    public String getName() {
      return "Predicate-Abstraction Refiner";
    }

    @Override
    public void printStatistics(PrintStream out, Result pResult, UnmodifiableReachedSet pReached) {
      StatisticsWriter w0 = StatisticsWriter.writingStatisticsTo(out);
      StatisticsWriter w1 = w0.beginLevel();

      w1.put(predicateCreation)
          .ifUpdatedAtLeastOnce(itpSimplification)
          .put(itpSimplification)
          .beginLevel()
          .put(simplifyDeltaConjunctions)
          .put(simplifyDeltaDisjunctions)
          .put(simplifyDeltaNegations)
          .put(simplifyDeltaAtoms)
          .put(simplifyDeltaVariables)
          .put(simplifyVariablesBefore)
          .put(simplifyVariablesAfter);

      w1.put(precisionUpdate).put(argUpdate).spacer();

      PredicateAbstractionRefinementStrategy.this.printStatistics(out);

      w0.put(numberOfRefinementsWithStrategy2)
          .ifUpdatedAtLeastOnce(itpSimplification)
          .put(irrelevantPredsInItp);
    }
  }

  public PredicateAbstractionRefinementStrategy(
      final Configuration config,
      final LogManager pLogger,
      final PredicateAbstractionManager pPredAbsMgr,
      final Solver pSolver)
      throws InvalidConfigurationException {
    super(pSolver);

    config.inject(this, PredicateAbstractionRefinementStrategy.class);

    logger = pLogger;
    fmgr = pSolver.getFormulaManager();
    bfmgr = fmgr.getBooleanFormulaManager();
    predAbsMgr = pPredAbsMgr;
    formulaMeasuring = new FormulaMeasuring(fmgr);

    if (dumpPredicates && dumpPredicatesFile != null) {
      precisionWriter = new PredicateMapWriter(config, fmgr);
    } else {
      precisionWriter = null;
    }
  }

  private ListMultimap<LocationInstance, AbstractionPredicate> newPredicates;

  final void setUseAtomicPredicates(boolean pAtomicPredicates) {
    atomicPredicates = pAtomicPredicates;
  }

  @Override
  protected void startRefinementOfPath() {
    checkState(newPredicates == null);
    // needs to be a fully deterministic data structure,
    // thus a Multimap based on a LinkedHashMap
    // (we iterate over the keys)
    newPredicates = MultimapBuilder.linkedHashKeys().arrayListValues().build();
  }

  @Override
  protected final boolean performRefinementForState(
      BooleanFormula pInterpolant, ARGState interpolationPoint) throws InterruptedException {
    checkArgument(!bfmgr.isTrue(pInterpolant));

    predicateCreation.start();
    PredicateAbstractState predicateState = getPredicateState(interpolationPoint);
    PathFormula blockFormula = predicateState.getAbstractionFormula().getBlockFormula();

    Collection<AbstractionPredicate> localPreds = convertInterpolant(pInterpolant, blockFormula);
    for (CFANode loc : AbstractStates.extractLocations(interpolationPoint)) {
      int locInstance = predicateState.getAbstractionLocationsOnPath().get(loc);
      storePredicates(new LocationInstance(loc, locInstance), localPreds);
    }
    predicateCreation.stop();

    return false;
  }

  /**
   * see {@link PredicateAbstractionRefinementStrategy#storePredicates(LocationInstance, Collection)
   * storeNewPredicates(LocationInstance, Collection)}
   */
  protected void storePredicates(LocationInstance pLocInstance, AbstractionPredicate pPredicate) {
    storePredicates(pLocInstance, ImmutableSet.of(pPredicate));
  }

  /**
   * Store interpolants in a dedicated collection.
   *
   * @param pLocInstance The {@link LocationInstance} in which the predicates hold.
   * @param pPredicates The {@link AbstractionPredicate} retrieved from a spurious counterexample.
   */
  protected void storePredicates(
      LocationInstance pLocInstance, Collection<AbstractionPredicate> pPredicates) {
    checkState(newPredicates != null);
    newPredicates.putAll(pLocInstance, pPredicates);
  }

  /**
   * Get the predicates out of an interpolant.
   *
   * @param pInterpolant The interpolant formula.
   * @return A set of predicates.
   */
  private Collection<AbstractionPredicate> convertInterpolant(
      final BooleanFormula pInterpolant, PathFormula blockFormula) throws InterruptedException {

    BooleanFormula interpolant = pInterpolant;

    if (bfmgr.isTrue(interpolant)) {
      return ImmutableSet.of();
    }

    Collection<AbstractionPredicate> preds;

    int allPredsCount = 0;
    if (useBddInterpolantSimplification) {
      FormulaMeasures itpBeforeSimple = formulaMeasuring.measure(interpolant);

      itpSimplification.start();
      // need to call extractPredicates() for registering all predicates
      allPredsCount = predAbsMgr.getPredicatesForAtomsOf(interpolant).size();
      interpolant =
          predAbsMgr
              .asAbstraction(fmgr.uninstantiate(interpolant), blockFormula)
              .asInstantiatedFormula();
      itpSimplification.stop();

      FormulaMeasures itpAfterSimple = formulaMeasuring.measure(interpolant);
      simplifyDeltaAtoms.setNextValue(itpAfterSimple.getAtoms() - itpBeforeSimple.getAtoms());
      simplifyDeltaDisjunctions.setNextValue(
          itpAfterSimple.getDisjunctions() - itpBeforeSimple.getDisjunctions());
      simplifyDeltaConjunctions.setNextValue(
          itpAfterSimple.getConjunctions() - itpBeforeSimple.getConjunctions());
      simplifyDeltaNegations.setNextValue(
          itpAfterSimple.getNegations() - itpBeforeSimple.getNegations());
      simplifyDeltaVariables.setNextValue(
          itpAfterSimple.getVariables().size() - itpBeforeSimple.getVariables().size());
      simplifyVariablesBefore.setNextValue(itpBeforeSimple.getVariables().size());
      simplifyVariablesAfter.setNextValue(itpAfterSimple.getVariables().size());
    }

    if (atomicPredicates) {
      preds = predAbsMgr.getPredicatesForAtomsOf(interpolant);

      if (useBddInterpolantSimplification) {
        irrelevantPredsInItp.setNextValue(allPredsCount - preds.size());
      }

    } else {
      preds = ImmutableList.of(predAbsMgr.getPredicateFor(interpolant));
    }
    assert !preds.isEmpty()
        : "Interpolant without relevant predicates: "
            + pInterpolant
            + "; simplified to "
            + interpolant;

    logger.log(Level.FINEST, "Got predicates", preds);

    return preds;
  }

  @Override
  protected void finishRefinementOfPath(
      ARGState pUnreachableState,
      List<ARGState> pAffectedStates,
      ARGReachedSet pReached,
      List<ARGState> abstractionStatesTrace,
      boolean pRepeatedCounterexample)
      throws CPAException, InterruptedException {

    Pair<PredicatePrecision, ARGState> newPrecAndRefinementRoot =
        computeNewPrecision(pUnreachableState, pAffectedStates, pReached, pRepeatedCounterexample);

    PredicatePrecision newPrecision = newPrecAndRefinementRoot.getFirst();
    ARGState refinementRoot = newPrecAndRefinementRoot.getSecond();

    updateARG(newPrecision, refinementRoot, pReached);

    newPredicates = null;
  }

  protected void updateARG(
      PredicatePrecision pNewPrecision, ARGState pRefinementRoot, ARGReachedSet pReached)
      throws InterruptedException {

    argUpdate.start();

    List<Precision> precisions = new ArrayList<>(2);
    List<Predicate<? super Precision>> precisionTypes = new ArrayList<>(2);

    precisions.add(pNewPrecision);
    precisionTypes.add(Predicates.instanceOf(PredicatePrecision.class));

    UnmodifiableReachedSet reached = pReached.asReachedSet();

    if (isValuePrecisionAvailable(pReached, pRefinementRoot)) {
      precisions.add(mergeAllValuePrecisionsFromSubgraph(pRefinementRoot, reached));
      precisionTypes.add(VariableTrackingPrecision.isMatchingCPAClass(ValueAnalysisCPA.class));
    }

    pReached.removeSubtree(pRefinementRoot, precisions, precisionTypes);

    assert (refinementCount > 0) || reached.size() == 1;

    if (sharePredicates) {
      pReached.updatePrecisionGlobally(
          pNewPrecision, Predicates.instanceOf(PredicatePrecision.class));
    }

    argUpdate.stop();
  }

  private Pair<PredicatePrecision, ARGState> computeNewPrecision(
      ARGState pUnreachableState,
      List<ARGState> pAffectedStates,
      ARGReachedSet pReached,
      boolean pRepeatedCounterexample)
      throws RefinementFailedException {

    { // Add predicate "false" to unreachable location
      // or add "false" to each location of the combination of locations
      for (CFANode loc : extractLocations(pUnreachableState)) {
        int locInstance =
            getPredicateState(pUnreachableState).getAbstractionLocationsOnPath().get(loc);
        storePredicates(new LocationInstance(loc, locInstance), predAbsMgr.makeFalsePredicate());
      }
      pAffectedStates.add(pUnreachableState);
    }

    // get previous precision
    UnmodifiableReachedSet reached = pReached.asReachedSet();
    PredicatePrecision targetStatePrecision =
        extractPredicatePrecision(reached.getPrecision(reached.getLastState()));

    ARGState refinementRoot =
        getRefinementRoot(
            pAffectedStates,
            pRepeatedCounterexample,
            reached,
            targetStatePrecision.getLocalPredicates());

    // now create new precision
    precisionUpdate.start();
    PredicatePrecision basePrecision =
        getBasePrecision(reached, refinementRoot, targetStatePrecision);

    logger.log(Level.ALL, "Old predicate map is", basePrecision);
    logger.log(Level.ALL, "New predicates are", newPredicates);

    PredicatePrecision newPrecision = addPredicatesToPrecision(basePrecision);

    logger.log(Level.ALL, "Predicate map now is", newPrecision);
    logger.log(Level.ALL, "Difference of predicates is", newPrecision.subtract(basePrecision));

    assert basePrecision.calculateDifferenceTo(newPrecision) == 0
        : "We forgot predicates during refinement!";
    assert targetStatePrecision.calculateDifferenceTo(newPrecision) == 0
        : "We forgot predicates during refinement!";

    if (dumpPredicates && dumpPredicatesFile != null) {
      dumpNewPredicates();
    }

    precisionUpdate.stop();

    return Pair.of(newPrecision, refinementRoot);
  }

  protected ARGState getRefinementRoot(
      List<ARGState> pAffectedStates,
      boolean pRepeatedCounterexample,
      UnmodifiableReachedSet reached,
      ImmutableSetMultimap<CFANode, AbstractionPredicate> pTargetStatePredicates)
      throws RefinementFailedException {
    ARGState refinementRoot =
        getPivotState(pAffectedStates, pTargetStatePredicates, pRepeatedCounterexample);

    // check whether we should restart
    refinementCount++;
    if (restartAfterRefinements > 0 && refinementCount >= restartAfterRefinements) {
      ARGState root = (ARGState) reached.getFirstState();
      // we have to use the child as the refinementRoot
      assert root.getChildren().size() == 1 : "ARG root should have exactly one child";
      refinementRoot = Iterables.getLast(root.getChildren());

      logger.log(
          Level.FINEST,
          "Restarting analysis after",
          refinementCount,
          "refinements by clearing the ARG.");
      refinementCount = 0;

    } else {
      logger.log(Level.FINEST, "Removing everything below", refinementRoot, "from ARG.");
    }
    return refinementRoot;
  }

  private PredicatePrecision getBasePrecision(
      UnmodifiableReachedSet reached,
      ARGState refinementRoot,
      PredicatePrecision targetStatePrecision) {
    PredicatePrecision basePrecision;
    switch (predicateBasisStrategy) {
      case ALL:
        basePrecision = findAllPredicatesFromSubgraph((ARGState) reached.getFirstState(), reached);
        break;
      case SUBGRAPH:
        basePrecision = findAllPredicatesFromSubgraph(refinementRoot, reached);
        break;
      case TARGET:
        basePrecision = targetStatePrecision;
        break;
      case CUTPOINT:
        basePrecision = extractPredicatePrecision(reached.getPrecision(refinementRoot));
        break;
      default:
        throw new AssertionError("unknown strategy for predicate basis.");
    }
    return basePrecision;
  }

  protected PredicatePrecision addPredicatesToPrecision(PredicatePrecision basePrecision) {
    PredicatePrecision newPrecision;
    switch (predicateSharing) {
      case GLOBAL:
        newPrecision = basePrecision.addGlobalPredicates(newPredicates.values());
        break;
      case SCOPE:
        Set<AbstractionPredicate> globalPredicates = new HashSet<>();
        ListMultimap<LocationInstance, AbstractionPredicate> localPredicates =
            ArrayListMultimap.create();

        splitInLocalAndGlobalPredicates(globalPredicates, localPredicates);

        newPrecision = basePrecision.addGlobalPredicates(globalPredicates);
        newPrecision =
            newPrecision.addLocalPredicates(mergePredicatesPerLocation(localPredicates.entries()));

        break;
      case FUNCTION:
        newPrecision =
            basePrecision.addFunctionPredicates(
                mergePredicatesPerFunction(newPredicates.entries()));
        break;
      case LOCATION:
        newPrecision =
            basePrecision.addLocalPredicates(mergePredicatesPerLocation(newPredicates.entries()));
        break;
      case LOCATION_INSTANCE:
        newPrecision = basePrecision.addLocationInstancePredicates(newPredicates.entries());
        break;
      default:
        throw new AssertionError();
    }
    return newPrecision;
  }

  private PredicatePrecision extractPredicatePrecision(Precision oldPrecision)
      throws IllegalStateException {
    PredicatePrecision oldPredicatePrecision =
        Precisions.extractPrecisionByType(oldPrecision, PredicatePrecision.class);
    checkState(
        oldPredicatePrecision != null,
        "Could not find the PredicatePrecision for the error element");
    return oldPredicatePrecision;
  }

  private void splitInLocalAndGlobalPredicates(
      Set<AbstractionPredicate> globalPredicates,
      ListMultimap<LocationInstance, AbstractionPredicate> localPredicates) {

    for (Map.Entry<LocationInstance, AbstractionPredicate> predicate : newPredicates.entries()) {
      if (predicate.getValue().getSymbolicAtom().toString().contains("::")) {
        localPredicates.put(predicate.getKey(), predicate.getValue());
      } else {
        globalPredicates.add(predicate.getValue());
      }
    }
  }

  private ARGState getPivotState(
      List<ARGState> pAffectedStates,
      ImmutableSetMultimap<CFANode, AbstractionPredicate> pTargetStatePredicates,
      boolean pRepeatedCounterexample)
      throws RefinementFailedException {
    // We have two different strategies for the pivot state: set it to
    // the first interpolation point or set it to highest location in the ARG
    // where the same CFANode appears.
    // Both work, so this is a heuristics question to get the best performance.
    // My benchmark showed, that at least for the benchmarks-lbe examples it is
    // best to use strategy one iff newPredicatesFound.
    // TODO right now this works only with location-specific predicates, not with other values of
    // cpa.predicate.precision.sharing
    boolean newPredicatesFound;
    if (newPredicates != null) {
      newPredicatesFound = false;
      for (Map.Entry<LocationInstance, AbstractionPredicate> entry : newPredicates.entries()) {
        if (!pTargetStatePredicates.containsEntry(entry.getKey().getLocation(), entry.getValue())) {
          newPredicatesFound = true;
          break;
        }
      }
    } else {
      newPredicatesFound = true;
    }

    ARGState firstInterpolationPoint = pAffectedStates.get(0);
    if (!newPredicatesFound) {
      if (pRepeatedCounterexample) {
        throw new RefinementFailedException(
            RefinementFailedException.Reason.RepeatedCounterexample, null);
      }
      numberOfRefinementsWithStrategy2.inc();

      CFANode firstInterpolationPointLocation =
          AbstractStates.extractLocation(firstInterpolationPoint);

      logger.log(
          Level.FINEST,
          "Found spurious counterexample,",
          "trying strategy 2: remove everything below node",
          firstInterpolationPointLocation,
          "from ARG.");

      // find top-most element in path with location == firstInterpolationPointLocation,
      // this is not necessary equal to firstInterpolationPoint
      ARGState current = firstInterpolationPoint;
      while (!current.getParents().isEmpty()) {
        current = Iterables.get(current.getParents(), 0);

        if (getPredicateState(current).isAbstractionState()) {
          CFANode loc = AbstractStates.extractLocation(current);
          if (loc.equals(firstInterpolationPointLocation)) {
            firstInterpolationPoint = current;
          }
        }
      }
    }
    return firstInterpolationPoint;
  }

  /**
   * Collect all precisions in the subgraph below refinementRoot and merge their predicates.
   *
   * @return a new precision with all these predicates.
   */
  public static PredicatePrecision findAllPredicatesFromSubgraph(
      ARGState refinementRoot, UnmodifiableReachedSet reached) {
    return PredicatePrecision.unionOf(
        ARGUtils.getNonCoveredStatesInSubgraph(refinementRoot).transform(reached::getPrecision));
  }

  private void dumpNewPredicates() {
    Path precFile = dumpPredicatesFile.getPath(precisionUpdate.getUpdateCount());
    try (Writer w = IO.openOutputFile(precFile, Charset.defaultCharset())) {
      precisionWriter.writePredicateMap(
          ImmutableSetMultimap.copyOf(newPredicates),
          ImmutableSetMultimap.of(),
          ImmutableSetMultimap.of(),
          ImmutableSet.of(),
          newPredicates.values(),
          w);
    } catch (IOException e) {
      logger.logUserException(Level.WARNING, e, "Could not dump precision to file");
    }
  }

  private boolean isValuePrecisionAvailable(final ARGReachedSet pReached, ARGState root) {
    if (!pReached.asReachedSet().contains(root)) {
      return false;
    }
    return Precisions.extractPrecisionByType(
            pReached.asReachedSet().getPrecision(root), VariableTrackingPrecision.class)
        != null;
  }

  private VariableTrackingPrecision mergeAllValuePrecisionsFromSubgraph(
      ARGState refinementRoot, UnmodifiableReachedSet reached) {

    VariableTrackingPrecision rootPrecision =
        Precisions.extractPrecisionByType(
            reached.getPrecision(refinementRoot), VariableTrackingPrecision.class);

    // find all distinct precisions to merge them
    Set<Precision> precisions = Sets.newIdentityHashSet();
    for (ARGState state : ARGUtils.getNonCoveredStatesInSubgraph(refinementRoot)) {
      // covered states are not in reached set
      precisions.add(reached.getPrecision(state));
    }

    for (Precision prec : precisions) {
      rootPrecision =
          rootPrecision.join(
              Precisions.extractPrecisionByType(prec, VariableTrackingPrecision.class));
    }

    return rootPrecision;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(new Stats());
  }

  private static Iterable<Map.Entry<String, AbstractionPredicate>> mergePredicatesPerFunction(
      Iterable<Map.Entry<LocationInstance, AbstractionPredicate>> predicates) {
    return from(predicates)
        .transform(e -> Maps.immutableEntry(e.getKey().getFunctionName(), e.getValue()));
  }

  private static Iterable<Map.Entry<CFANode, AbstractionPredicate>> mergePredicatesPerLocation(
      Iterable<Map.Entry<LocationInstance, AbstractionPredicate>> predicates) {
    return from(predicates)
        .transform(e -> Maps.immutableEntry(e.getKey().getLocation(), e.getValue()));
  }
}
