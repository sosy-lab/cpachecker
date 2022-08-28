package org.sosy_lab.cpachecker.cpa.cer.refiner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.AbstractARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.cer.CERCPA;
import org.sosy_lab.cpachecker.cpa.cer.CERCPAStatistics;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionRefinementStrategy;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisInterpolant;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisRefiner;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisStrongestPostOperator;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ValueAnalysisFeasibilityChecker;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ValueAnalysisPrefixProvider;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.refinement.GenericPrefixProvider;
import org.sosy_lab.cpachecker.util.refinement.InterpolationTree;
import org.sosy_lab.cpachecker.util.refinement.PathExtractor;
import org.sosy_lab.cpachecker.util.refinement.StrongestPostOperator;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.cpachecker.util.statistics.ThreadSafeTimerContainer.TimerWrapper;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Multimap;

@Options
public class CERValueAnalysisRefiner extends ValueAnalysisRefiner {

    @Option(
        secure = true,
        name = "trackValuePrecision",
        description = "whether to contain precision information for the value analysis")
    private boolean tracksPrecision = true;

    @Option(
        secure = true,
        description = "whether or not to do lazy-abstraction",
        name = "cpa.value.refinement.restart",
        toUppercase = true)
    private RestartStrategy restartStrategy = RestartStrategy.PIVOT;

    private final ValueAnalysisFeasibilityChecker cerChecker;
    private final ShutdownNotifier shutdownNotifier;

    private CERRefinerReport report;
    private final CERCPA cerCpa;
    private final CERCPAStatistics statistics;
    private final TimerWrapper checkerTimer;
    private final TimerWrapper refinementTimer;

    public static Refiner create(final ConfigurableProgramAnalysis pCpa)
            throws InvalidConfigurationException {
        return AbstractARGBasedRefiner.forARGBasedRefiner(create0(pCpa), pCpa);
    }

    public static ARGBasedRefiner create0(final ConfigurableProgramAnalysis pCpa)
            throws InvalidConfigurationException {

        final CERCPA cerCpa =
                CPAs.retrieveCPAOrFail(pCpa, CERCPA.class, CERValueAnalysisRefiner.class);
        final ValueAnalysisCPA valueAnalysisCpa =
                CPAs.retrieveCPAOrFail(pCpa, ValueAnalysisCPA.class, CERValueAnalysisRefiner.class);

        valueAnalysisCpa.injectRefinablePrecision();

        final LogManager logger = valueAnalysisCpa.getLogger();
        final Configuration config = valueAnalysisCpa.getConfiguration();
        final CFA cfa = valueAnalysisCpa.getCFA();

        final StrongestPostOperator<ValueAnalysisState> strongestPostOp =
                new ValueAnalysisStrongestPostOperator(logger, config, cfa);

        final ValueAnalysisFeasibilityChecker checker =
                new CERValueAnalysisFeasibilityChecker(strongestPostOp, logger, cfa, config);

        final GenericPrefixProvider<ValueAnalysisState> prefixProvider =
                new ValueAnalysisPrefixProvider(
                        logger,
                        cfa,
                        config,
                        valueAnalysisCpa.getShutdownNotifier());

        return new CERValueAnalysisRefiner(
                checker,
                strongestPostOp,
                new PathExtractor(logger, config),
                prefixProvider,
                config,
                logger,
                valueAnalysisCpa.getShutdownNotifier(),
                cfa,
                cerCpa);
    }

    CERValueAnalysisRefiner(
            final ValueAnalysisFeasibilityChecker pFeasibilityChecker,
            final StrongestPostOperator<ValueAnalysisState> pStrongestPostOperator,
            final PathExtractor pPathExtractor,
            final GenericPrefixProvider<ValueAnalysisState> pPrefixProvider,
            final Configuration pConfig,
            final LogManager pLogger,
            final ShutdownNotifier pShutdownNotifier,
            final CFA pCfa,
            final CERCPA pCerCPA)
            throws InvalidConfigurationException {

        super(
                pFeasibilityChecker,
                pStrongestPostOperator,
                pPathExtractor,
                pPrefixProvider,
                pConfig,
                pLogger,
                pShutdownNotifier,
                pCfa);

        pConfig.inject(this, CERValueAnalysisRefiner.class);
        cerChecker = pFeasibilityChecker;
        cerCpa = pCerCPA;
        shutdownNotifier = pShutdownNotifier;
        statistics = cerCpa.getStatistics();
        checkerTimer = statistics.getFeasibilityCheckerTimer().getNewTimer();
        refinementTimer = statistics.getRefinementTimer().getNewTimer();
    }

    @Override
    protected boolean isErrorPathFeasible(ARGPath pErrorPath)
            throws CPAException, InterruptedException {
        report = new CERRefinerReport(pErrorPath);
        checkerTimer.start();
        boolean result = cerChecker.isFeasible(pErrorPath);
        checkerTimer.stop();
        return result;
    }

    @Override
    protected InterpolationTree<ValueAnalysisState, ValueAnalysisInterpolant>
            obtainInterpolants(ARGPath pTargetPath) throws CPAException, InterruptedException {
        refinementTimer.start();
        return super.obtainInterpolants(pTargetPath);
    }

    @Override
    protected void refineUsingInterpolants(
            final ARGReachedSet pReached,
            final InterpolationTree<ValueAnalysisState, ValueAnalysisInterpolant> pInterpolationTree)
            throws InterruptedException {
        final UnmodifiableReachedSet reached = pReached.asReachedSet();
        final boolean predicatePrecisionIsAvailable = isPredicatePrecisionAvailable(reached);

        Map<ARGState, List<Precision>> refinementInformation = new LinkedHashMap<>();
        Collection<ARGState> refinementRoots =
                pInterpolationTree.obtainRefinementRoots(restartStrategy);
        report.setRefinementRoots(refinementRoots);
        report.setCutOffRoots(pInterpolationTree.obtainCutOffRoots());
        report.setInterpolationTree(pInterpolationTree);

        for (ARGState root : refinementRoots) {
            shutdownNotifier.shutdownIfNecessary();
            root = relocateRefinementRoot(root, predicatePrecisionIsAvailable);

            if (refinementRoots.size() == 1
                    && isSimilarRepeatedRefinement(
                            pInterpolationTree.extractPrecisionIncrement(root).values())) {
                root = relocateRepeatedRefinementRoot(root);
            }

            List<Precision> precisions = new ArrayList<>(2);

            VariableTrackingPrecision basePrecision;
            switch (basisStrategy) {
                case ALL:
                    basePrecision =
                            mergeValuePrecisionsForSubgraph(
                                    (ARGState) reached.getFirstState(),
                                    reached);
                    break;
                case SUBGRAPH:
                    basePrecision = mergeValuePrecisionsForSubgraph(root, reached);
                    break;
                case TARGET:
                    basePrecision =
                            extractValuePrecision(reached.getPrecision(reached.getLastState()));
                    break;
                case CUTPOINT:
                    basePrecision = extractValuePrecision(reached.getPrecision(root));
                    break;
                default:
                    throw new AssertionError("unknown strategy for predicate basis.");
            }

            // merge the value precisions of the subtree, and refine it
            Multimap<CFANode, MemoryLocation> precisionInc =
                    pInterpolationTree.extractPrecisionIncrement(root);
            precisions.add(basePrecision.withIncrement(precisionInc));
            if (tracksPrecision) {
                report.setPrecisionInc(precisionInc);
            }

            // merge the predicate precisions of the subtree, if available
            if (predicatePrecisionIsAvailable) {
                precisions.add(
                        PredicateAbstractionRefinementStrategy
                                .findAllPredicatesFromSubgraph(root, reached));
            }

            refinementInformation.put(root, precisions);
        }

        for (Entry<ARGState, List<Precision>> info : refinementInformation.entrySet()) {
            shutdownNotifier.shutdownIfNecessary();
            List<Predicate<? super Precision>> precisionTypes = new ArrayList<>(2);

            precisionTypes
                    .add(VariableTrackingPrecision.isMatchingCPAClass(ValueAnalysisCPA.class));
            if (predicatePrecisionIsAvailable) {
                precisionTypes.add(Predicates.instanceOf(PredicatePrecision.class));
            }

            pReached.removeSubtree(info.getKey(), info.getValue(), precisionTypes);
        }

        refinementTimer.stop();

        if (report != null) {
            // update the store and the refinement root states
            cerCpa.update(report);
        }
    }

    @Override
    protected VariableTrackingPrecision extractValuePrecision(Precision pPrecision) {
        VariableTrackingPrecision prec =
                (VariableTrackingPrecision) Precisions.asIterable(pPrecision)
                        .firstMatch(pPrec -> pPrec instanceof VariableTrackingPrecision)
                        .get();
        return prec;
    }
}
