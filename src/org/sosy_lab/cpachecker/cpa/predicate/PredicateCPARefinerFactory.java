// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.cpa.arg.ARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.arg.AbstractARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerHeuristics.DelegatingRefinerHeuristic;
import org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerHeuristics.DelegatingRefinerHeuristicInterpolationRate;
import org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerHeuristics.DelegatingRefinerHeuristicRedundantPredicates;
import org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerHeuristics.DelegatingRefinerHeuristicRunNTimes;
import org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerHeuristics.DelegatingRefinerHeuristicStaticRefinement;
import org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerHeuristics.DelegatingRefinerHeuristicType;
import org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerHeuristics.DelegatingRefinerRefinerType;
import org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerHeuristics.HeuristicDelegatingRefinerRecord;
import org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerUtils.TrackingPredicateCPARefinementContext;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.predicates.PathChecker;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.refinement.PrefixProvider;
import org.sosy_lab.cpachecker.util.refinement.PrefixSelector;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;

/**
 * Factory for {@link PredicateCPARefiner}, the base class for most refiners for the PredicateCPA.
 */
@Options(prefix = "cpa.predicate.refinement")
public final class PredicateCPARefinerFactory {

  @Option(secure = true, description = "slice block formulas, experimental feature!")
  private boolean sliceBlockFormulas = false;

  @Option(
      secure = true,
      name = "graphblockformulastrategy",
      description = "BlockFormulaStrategy for graph-like ARGs (e.g. Slicing Abstractions)")
  private boolean graphBlockFormulaStrategy = false;

  @Option(
      secure = true,
      description =
          "use heuristic to extract predicates from the CFA statically on first refinement")
  private boolean performInitialStaticRefinement = false;

  @Option(
      secure = true,
      description =
          "use DelegatingRefiner to switch between refiners bases on a set of heuristics.")
  private boolean usePredicateDelegatingRefiner = false;

  @Option(
      secure = true,
      description =
          "List of heuristic-refiner pairs for the DelegatingRefiner. Only use when"
              + " usePredicateDelegatingRefiner = true.")
  private ImmutableList<String> heuristicRefinerPairs =
      ImmutableList.of(
          "STATIC:STATIC",
          "DEFAULT_N_TIMES:DEFAULT",
          "INTERPOLATION_RATE:DEFAULT",
          "REDUNDANT_PREDICATES:DEFAULT");

  @Option(
      secure = true,
      description =
          "Number of times the PredicateCPARefiner should run to collect data for subsequent"
              + " DelegatingRefiner heuristics ")
  private int defaultFixedRuns = 10;

  @Option(
      secure = true,
      description =
          "Acceptable number of interpolants generated per refinement for DelegatingRefiner"
              + " heuristic")
  private double defaultInterpolantRate = 7.0;

  @Option(
      secure = true,
      description =
          "Acceptable redundancy percentage for added predicates for DelegatingRefiner heuristic"
              + " (0.0 - 1.0)")
  private double acceptableRedundancyThreshold = 0.8;

  private final PredicateCPA predicateCpa;

  private @Nullable BlockFormulaStrategy blockFormulaStrategy = null;

  private ImmutableList<HeuristicDelegatingRefinerRecord> refinerRecords = null;

  private final TrackingPredicateCPARefinementContext refinementContext =
      new TrackingPredicateCPARefinementContext();

  /**
   * Create a factory instance.
   *
   * @param pCpa The CPA used for this whole analysis.
   * @throws InvalidConfigurationException If there is no PredicateCPA configured or if
   *     configuration is invalid.
   */
  @SuppressWarnings("options")
  public PredicateCPARefinerFactory(ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    predicateCpa =
        CPAs.retrieveCPAOrFail(checkNotNull(pCpa), PredicateCPA.class, PredicateCPARefiner.class);
    predicateCpa.getConfiguration().inject(this);
  }

  /**
   * Ensure that {@link PredicateStaticRefiner} is not used. This is mostly useful for
   * configurations where static refinements do not make sense, or a the predicate refiner is used
   * as a helper for other refinements and should always generate interpolants.
   *
   * @return this
   * @throws InvalidConfigurationException If static refinements are enabled by the configuration.
   */
  @CanIgnoreReturnValue
  public PredicateCPARefinerFactory forbidStaticRefinements() throws InvalidConfigurationException {
    if (performInitialStaticRefinement) {
      throw new InvalidConfigurationException(
          "Static refinement is not supported with the configured refiner, "
              + "please turn cpa.predicate.refinement.useStaticRefinement off.");
    }
    return this;
  }

  /**
   * Let the refiners created by this factory instance use the given {@link BlockFormulaStrategy}.
   * May be called only once, but does not need to be called (in this case the configuration will
   * determine the used BlockFormulaStrategy).
   *
   * @return this
   */
  @CanIgnoreReturnValue
  public PredicateCPARefinerFactory setBlockFormulaStrategy(
      BlockFormulaStrategy pBlockFormulaStrategy) {
    checkState(blockFormulaStrategy == null);
    blockFormulaStrategy = checkNotNull(pBlockFormulaStrategy);
    return this;
  }

  /**
   * Create a {@link PredicateCPARefiner}. This factory can be reused afterward.
   *
   * @param pRefinementStrategy The refinement strategy to use.
   * @return A fresh instance.
   */
  public ARGBasedRefiner create(RefinementStrategy pRefinementStrategy)
      throws InvalidConfigurationException {
    checkNotNull(pRefinementStrategy);

    Configuration config = predicateCpa.getConfiguration();
    LogManager logger = predicateCpa.getLogger();
    ShutdownNotifier shutdownNotifier = predicateCpa.getShutdownNotifier();
    Solver solver = predicateCpa.getSolver();
    PathFormulaManager pfmgr = predicateCpa.getPathFormulaManager();

    CFA cfa = predicateCpa.getCfa();
    MachineModel machineModel = cfa.getMachineModel();
    Optional<VariableClassification> variableClassification = cfa.getVarClassification();
    Optional<LoopStructure> loopStructure = cfa.getLoopStructure();

    PredicateAbstractionManager predAbsManager = predicateCpa.getPredicateManager();
    PredicateCPAInvariantsManager invariantsManager = predicateCpa.getInvariantsManager();

    PrefixProvider prefixProvider =
        new PredicateBasedPrefixProvider(config, logger, solver, shutdownNotifier);
    PrefixSelector prefixSelector =
        new PrefixSelector(variableClassification, loopStructure, logger);

    InterpolationManager interpolationManager =
        new InterpolationManager(
            pfmgr, solver, loopStructure, variableClassification, config, shutdownNotifier, logger);

    PathChecker pathChecker =
        new PathChecker(config, logger, shutdownNotifier, machineModel, pfmgr, solver);

    BlockFormulaStrategy bfs;
    if (blockFormulaStrategy != null) {
      if (sliceBlockFormulas) {
        throw new InvalidConfigurationException(
            "Block-formula slicing is not supported with this refiner, "
                + "please turn cpa.predicate.refinement.sliceBlockFormula off.");
      }
      bfs = blockFormulaStrategy;
    } else {
      if (sliceBlockFormulas) {
        bfs = new BlockFormulaSlicer(pfmgr);
      } else if (graphBlockFormulaStrategy) {
        bfs = new SlicingAbstractionsBlockFormulaStrategy(solver, config, pfmgr);
      } else {
        bfs = new BlockFormulaStrategy();
      }
    }

    // To add both refiners to the DelegatingRefiner, they temporarily need different identifiers
    // in the create method.

    ARGBasedRefiner defaultRefiner =
        new PredicateCPARefiner(
            config,
            logger,
            loopStructure,
            bfs,
            solver,
            pfmgr,
            interpolationManager,
            pathChecker,
            prefixProvider,
            prefixSelector,
            invariantsManager,
            pRefinementStrategy,
            refinementContext);

    ARGBasedRefiner staticRefiner = defaultRefiner;
    if (performInitialStaticRefinement) {
      staticRefiner =
          new PredicateStaticRefiner(
              config,
              logger,
              shutdownNotifier,
              solver,
              pfmgr,
              predAbsManager,
              bfs,
              interpolationManager,
              pathChecker,
              cfa,
              defaultRefiner);
    }
    ARGBasedRefiner refiner = staticRefiner;
    if (usePredicateDelegatingRefiner) {
      ImmutableMap<DelegatingRefinerRefinerType, Refiner> pRefinersAvailable =
          buildRefinerMap(
              AbstractARGBasedRefiner.forARGBasedRefiner(defaultRefiner, predicateCpa),
              AbstractARGBasedRefiner.forARGBasedRefiner(staticRefiner, predicateCpa));
      refinerRecords = createDelegatingRefinerConfig(pRefinersAvailable);
    }

    return refiner;
  }

  // Maps available refiner implementations to their corresponding enum identifiers to choose as
  // value for the DelegatingRefiner heuristics. To support a new refiner, please add an entry to
  // DelegatingRefinerRefinerType and extend this map accordingly.
  ImmutableMap<DelegatingRefinerRefinerType, Refiner> buildRefinerMap(
      Refiner defaultRefiner, Refiner staticRefiner) {
    return ImmutableMap.of(
        DelegatingRefinerRefinerType.DEFAULT,
        defaultRefiner,
        DelegatingRefinerRefinerType.STATIC,
        staticRefiner,
        DelegatingRefinerRefinerType.STOP,
        new PredicateStopRefiner());
  }

  // Creates a List of records for the DelegatingRefiner for what refiner to choose for which
  // heuristics, based on user input in the command-line.
  public ImmutableList<HeuristicDelegatingRefinerRecord> createDelegatingRefinerConfig(
      ImmutableMap<DelegatingRefinerRefinerType, Refiner> pRefinersAvailable)
      throws InvalidConfigurationException {

    ImmutableList.Builder<HeuristicDelegatingRefinerRecord> recordBuilder = ImmutableList.builder();

    for (String heuristicRefinerPair : heuristicRefinerPairs) {
      Iterable<String> rawPair = Splitter.on(':').trimResults().split(heuristicRefinerPair);
      ImmutableList<String> pairs = ImmutableList.copyOf(rawPair);
      if (pairs.size() != 2) {
        throw new InvalidConfigurationException(
            "Invalid heuristic-refiner format: "
                + heuristicRefinerPair
                + ". Please use this format: STATIC:STATIC,DEFAULT_N_TIMES:DEFAULT.");
      }

      DelegatingRefinerHeuristicType pHeuristicName;
      try {
        pHeuristicName =
            DelegatingRefinerHeuristicType.valueOf(pairs.getFirst().toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException unknownHeuristicType) {
        throw new InvalidConfigurationException(
            "Unknown heuristic type: "
                + pairs.getFirst()
                + ". Available heuristics: "
                + Joiner.on(",").join(EnumSet.allOf(DelegatingRefinerHeuristicType.class)),
            unknownHeuristicType);
      }

      DelegatingRefinerRefinerType pRefinerName;
      try {
        pRefinerName =
            DelegatingRefinerRefinerType.valueOf(pairs.getLast().toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException unknownRefinerType) {
        throw new InvalidConfigurationException(
            "Unknown refiner type: "
                + pairs.getLast()
                + ". Available refiners: "
                + pRefinersAvailable.keySet(),
            unknownRefinerType);
      }

      Refiner pRefiner = pRefinersAvailable.get(pRefinerName);
      if (pRefiner == null) {
        throw new InvalidConfigurationException(
            "Refiner must not be null. Available refiners: " + pRefinersAvailable.keySet());
      }
      DelegatingRefinerHeuristic pHeuristic =
          switch (pHeuristicName) {
            case STATIC -> new DelegatingRefinerHeuristicStaticRefinement();
            case DEFAULT_N_TIMES -> new DelegatingRefinerHeuristicRunNTimes(defaultFixedRuns);
            case INTERPOLATION_RATE ->
                new DelegatingRefinerHeuristicInterpolationRate(
                    refinementContext, predicateCpa.getLogger(), defaultInterpolantRate);
            case REDUNDANT_PREDICATES ->
                new DelegatingRefinerHeuristicRedundantPredicates(
                    acceptableRedundancyThreshold,
                    predicateCpa.getSolver().getFormulaManager(),
                    predicateCpa.getLogger());
            case STOP -> (pReached, pDeltas) -> true;
          };

      recordBuilder.add(new HeuristicDelegatingRefinerRecord(pHeuristic, pRefiner));
    }
    return recordBuilder.build();
  }

  public ImmutableList<HeuristicDelegatingRefinerRecord> getRefinerRecords() {
    if (refinerRecords == null) {
      return ImmutableList.of();
    }
    return refinerRecords;
  }
}
