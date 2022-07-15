// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.invariants;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Equivalence;
import com.google.common.base.Equivalence.Wrapper;
import com.google.common.base.Predicates;
import com.google.common.base.Throwables;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.io.PrintStream;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import org.sosy_lab.common.Classes.UnexpectedCheckedException;
import org.sosy_lab.common.LazyFutureTask;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier.ShutdownRequestListener;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.ParallelAlgorithm.ConditionAdjustmentEventSubscriber;
import org.sosy_lab.cpachecker.core.algorithm.bmc.BMCAlgorithmForInvariantGeneration;
import org.sosy_lab.cpachecker.core.algorithm.bmc.BMCStatistics;
import org.sosy_lab.cpachecker.core.algorithm.bmc.CandidateGenerator;
import org.sosy_lab.cpachecker.core.algorithm.bmc.StaticCandidateProvider;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.CandidateInvariant;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.EdgeFormulaNegation;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.ExpressionTreeLocationInvariant;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.TargetLocationCandidateInvariant;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.ExpressionSubstitution;
import org.sosy_lab.cpachecker.util.ExpressionSubstitution.SubstitutionException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.WitnessInvariantsExtractor;
import org.sosy_lab.cpachecker.util.automaton.TargetLocationProvider;
import org.sosy_lab.cpachecker.util.resources.ResourceLimitChecker;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;
import org.sosy_lab.cpachecker.util.variableclassification.Partition;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;
import org.sosy_lab.java_smt.api.SolverException;

/** Generate invariants using k-induction. */
public class KInductionInvariantGenerator extends AbstractInvariantGenerator
    implements StatisticsProvider, ConditionAdjustmentEventSubscriber {

  @Options(prefix = "invariantGeneration.kInduction")
  public static class KInductionInvariantGeneratorOptions {

    @FileOption(Type.OPTIONAL_INPUT_FILE)
    @Option(
        secure = true,
        description =
            "Provides additional candidate invariants to the k-induction invariant generator.")
    private Path invariantsAutomatonFile = null;

    @Option(
        secure = true,
        description = "Guess some candidates for the k-induction invariant generator from the CFA.")
    private CfaCandidateInvariantExtractorFactories guessCandidatesFromCFA =
        CfaCandidateInvariantExtractorFactories.ASSUME_EDGES_PLAIN;

    @Option(
        secure = true,
        description =
            "For correctness-witness validation: Shut down if a candidate invariant is found to be"
                + " incorrect.")
    private boolean terminateOnCounterexample = false;

    @Option(
        secure = true,
        description = "Check candidate invariants in a separate thread asynchronously.")
    private boolean async = true;
  }

  private static class KInductionInvariantGeneratorStatistics extends BMCStatistics {

    final Timer invariantGeneration = new Timer();

    private Integer totalNumberOfCandidates = null;

    private int numberOfConfirmedCandidates = 0;

    @Override
    public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {
      StatisticsWriter writer = StatisticsWriter.writingStatisticsTo(out);
      writer.put("Time for invariant generation", invariantGeneration);
      if (totalNumberOfCandidates != null) {
        writer.put("Total number of candidates", totalNumberOfCandidates);
      }
      writer.put("Number of confirmed candidates", numberOfConfirmedCandidates);
      super.printStatistics(out, result, reached);
    }

    @Override
    public String getName() {
      return "k-Induction-based invariant generator";
    }
  }

  private final KInductionInvariantGeneratorStatistics stats =
      new KInductionInvariantGeneratorStatistics();

  private final BMCAlgorithmForInvariantGeneration algorithm;
  private final ConfigurableProgramAnalysis cpa;
  private final ReachedSetFactory reachedSetFactory;

  private final LogManager logger;
  private final ShutdownManager shutdownManager;

  private final boolean async;

  // After start(), this will hold a Future for the final result of the invariant generation.
  // We use a Future instead of just the atomic reference below
  // to be able to ask for termination and see thrown exceptions.
  private Future<Pair<InvariantSupplier, ExpressionTreeSupplier>> invariantGenerationFuture = null;

  @SuppressWarnings("UnnecessaryAnonymousClass") // ShutdownNotifier needs a strong reference
  private final ShutdownRequestListener shutdownListener =
      new ShutdownRequestListener() {

        @Override
        public void shutdownRequested(String pReason) {
          invariantGenerationFuture.cancel(true);
        }
      };

  public static KInductionInvariantGenerator create(
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownManager pShutdownManager,
      final CFA pCFA,
      final Specification specification,
      final ReachedSetFactory pReachedSetFactory,
      final TargetLocationProvider pTargetLocationProvider,
      final AggregatedReachedSets pAggregatedReachedSets)
      throws InvalidConfigurationException, CPAException, InterruptedException {

    KInductionInvariantGeneratorOptions options = new KInductionInvariantGeneratorOptions();
    pConfig.inject(options);

    return new KInductionInvariantGenerator(
        pConfig,
        pLogger.withComponentName("KInductionInvariantGenerator"),
        pShutdownManager,
        pCFA,
        specification,
        pReachedSetFactory,
        options.async,
        getCandidateInvariants(
            options,
            pConfig,
            pLogger,
            pCFA,
            pShutdownManager,
            pTargetLocationProvider,
            specification),
        pAggregatedReachedSets);
  }

  static KInductionInvariantGenerator create(
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownManager pShutdownManager,
      final CFA pCFA,
      final Specification specification,
      final ReachedSetFactory pReachedSetFactory,
      CandidateGenerator candidateGenerator,
      boolean pAsync)
      throws InvalidConfigurationException, CPAException, InterruptedException {

    return new KInductionInvariantGenerator(
        pConfig,
        pLogger.withComponentName("KInductionInvariantGenerator"),
        pShutdownManager,
        pCFA,
        specification,
        pReachedSetFactory,
        pAsync,
        candidateGenerator,
        AggregatedReachedSets.empty());
  }

  private KInductionInvariantGenerator(
      final Configuration config,
      final LogManager pLogger,
      final ShutdownManager pShutdownNotifier,
      final CFA cfa,
      final Specification specification,
      final ReachedSetFactory pReachedSetFactory,
      final boolean pAsync,
      final CandidateGenerator pCandidateGenerator,
      final AggregatedReachedSets pAggregatedReachedSets)
      throws InvalidConfigurationException, CPAException, InterruptedException {
    logger = pLogger;
    shutdownManager = pShutdownNotifier;

    reachedSetFactory = pReachedSetFactory;
    async = pAsync;

    if (pCandidateGenerator instanceof StaticCandidateProvider) {
      StaticCandidateProvider staticCandidateProvider =
          (StaticCandidateProvider) pCandidateGenerator;
      stats.totalNumberOfCandidates =
          FluentIterable.from(staticCandidateProvider.getAllCandidates())
              .filter(Predicates.not(Predicates.instanceOf(TargetLocationCandidateInvariant.class)))
              .size();
    }
    CandidateGenerator statisticsCandidateGenerator =
        new CandidateGenerator() {

          private final Set<CandidateInvariant> confirmedCandidates = new HashSet<>();

          @Override
          public boolean produceMoreCandidates() {
            return pCandidateGenerator.produceMoreCandidates();
          }

          @Override
          public Iterator<CandidateInvariant> iterator() {
            final Iterator<CandidateInvariant> it = pCandidateGenerator.iterator();
            return new Iterator<>() {

              @Override
              public boolean hasNext() {
                return it.hasNext();
              }

              @Override
              public CandidateInvariant next() {
                return it.next();
              }

              @Override
              public void remove() {
                it.remove();
              }
            };
          }

          @Override
          public boolean hasCandidatesAvailable() {
            return pCandidateGenerator.hasCandidatesAvailable();
          }

          @Override
          public Set<? extends CandidateInvariant> getConfirmedCandidates() {
            return pCandidateGenerator.getConfirmedCandidates();
          }

          @Override
          public void confirmCandidates(Iterable<CandidateInvariant> pCandidates) {
            pCandidateGenerator.confirmCandidates(pCandidates);
            for (CandidateInvariant invariant : pCandidates) {
              if (!(invariant instanceof TargetLocationCandidateInvariant)
                  && confirmedCandidates.add(invariant)) {
                ++stats.numberOfConfirmedCandidates;
              }
            }
          }
        };

    ShutdownManager childShutdown = ShutdownManager.createWithParent(shutdownManager.getNotifier());
    ResourceLimitChecker.fromConfiguration(config, logger, childShutdown).start();
    CPABuilder invGenBMCBuilder =
        new CPABuilder(config, logger, childShutdown.getNotifier(), pReachedSetFactory);

    cpa = invGenBMCBuilder.buildCPAs(cfa, specification, pAggregatedReachedSets);
    Algorithm cpaAlgorithm = CPAAlgorithm.create(cpa, logger, config, childShutdown.getNotifier());
    algorithm =
        new BMCAlgorithmForInvariantGeneration(
            cpaAlgorithm,
            cpa,
            config,
            logger,
            pReachedSetFactory,
            shutdownManager,
            cfa,
            specification,
            stats,
            statisticsCandidateGenerator,
            pAggregatedReachedSets);
  }

  @Override
  protected void startImpl(final CFANode initialLocation) {
    checkState(invariantGenerationFuture == null);

    Callable<Pair<InvariantSupplier, ExpressionTreeSupplier>> task =
        new InvariantGenerationTask(initialLocation);

    if (async) {
      // start invariant generation asynchronously
      ExecutorService executor = Executors.newSingleThreadExecutor();
      invariantGenerationFuture = executor.submit(task);
      executor.shutdown(); // will shutdown after task is finished

    } else {
      // create future for lazy synchronous invariant generation
      invariantGenerationFuture = new LazyFutureTask<>(task);
    }

    shutdownManager.getNotifier().registerAndCheckImmediately(shutdownListener);
  }

  private final AtomicBoolean cancelled = new AtomicBoolean();

  @Override
  public void cancel() {
    checkState(invariantGenerationFuture != null);
    shutdownManager.requestShutdown("Invariant generation cancel requested.");
    cancelled.set(true);
  }

  @Override
  public InvariantSupplier getSupplier() throws InterruptedException, CPAException {
    checkState(invariantGenerationFuture != null);

    if ((async && !invariantGenerationFuture.isDone()) || cancelled.get()) {
      // grab intermediate result that is available so far
      return algorithm.getCurrentInvariants();

    } else {
      try {
        return invariantGenerationFuture.get().getFirst();
      } catch (ExecutionException e) {
        Throwables.propagateIfPossible(
            e.getCause(), CPAException.class, InterruptedException.class);
        throw new UnexpectedCheckedException("invariant generation", e.getCause());
      } catch (CancellationException e) {
        shutdownManager.getNotifier().shutdownIfNecessary();
        throw e;
      }
    }
  }

  @Override
  public ExpressionTreeSupplier getExpressionTreeSupplier()
      throws InterruptedException, CPAException {
    checkState(invariantGenerationFuture != null);

    if ((async && !invariantGenerationFuture.isDone()) || cancelled.get()) {
      // grab intermediate result that is available so far
      return algorithm.getCurrentInvariantsAsExpressionTree();

    } else {
      try {
        return invariantGenerationFuture.get().getSecond();
      } catch (ExecutionException e) {
        Throwables.propagateIfPossible(
            e.getCause(), CPAException.class, InterruptedException.class);
        throw new UnexpectedCheckedException("invariant generation", e.getCause());
      } catch (CancellationException e) {
        shutdownManager.getNotifier().shutdownIfNecessary();
        throw e;
      }
    }
  }

  @Override
  public boolean isProgramSafe() {
    return algorithm.isProgramSafe();
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    algorithm.collectStatistics(pStatsCollection);
    pStatsCollection.add(stats);
  }

  private class InvariantGenerationTask
      implements Callable<Pair<InvariantSupplier, ExpressionTreeSupplier>> {

    private final CFANode initialLocation;

    private InvariantGenerationTask(final CFANode pInitialLocation) {
      initialLocation = checkNotNull(pInitialLocation);
    }

    @Override
    public Pair<InvariantSupplier, ExpressionTreeSupplier> call()
        throws InterruptedException, CPAException {
      stats.invariantGeneration.start();
      shutdownManager.getNotifier().shutdownIfNecessary();

      try {
        ReachedSet reachedSet =
            reachedSetFactory.createAndInitialize(
                cpa, initialLocation, StateSpacePartition.getDefaultPartition());
        algorithm.run(reachedSet);
        return Pair.of(
            algorithm.getCurrentInvariants(), algorithm.getCurrentInvariantsAsExpressionTree());

      } catch (SolverException e) {
        throw new CPAException("Solver Failure", e);
      } finally {
        stats.invariantGeneration.stop();
        CPAs.closeCpaIfPossible(cpa, logger);
        CPAs.closeIfPossible(algorithm, logger);
      }
    }
  }

  public static CandidateGenerator getCandidateInvariants(
      KInductionInvariantGeneratorOptions pOptions,
      Configuration pConfig,
      LogManager pLogger,
      CFA pCFA,
      final ShutdownManager pShutdownManager,
      TargetLocationProvider pTargetLocationProvider,
      Specification pSpecification)
      throws InvalidConfigurationException, CPAException, InterruptedException {

    final Set<CandidateInvariant> candidates = new LinkedHashSet<>();

    Iterables.addAll(
        candidates,
        pOptions.guessCandidatesFromCFA.create(
            pCFA, pSpecification, pTargetLocationProvider, pLogger));

    final Multimap<String, CFANode> candidateGroupLocations = HashMultimap.create();
    if (pOptions.invariantsAutomatonFile != null) {
      WitnessInvariantsExtractor extractor =
          new WitnessInvariantsExtractor(
              pConfig,
              pLogger,
              pCFA,
              pShutdownManager.getNotifier(),
              pOptions.invariantsAutomatonFile);
      extractor.extractCandidatesFromReachedSet(candidates, candidateGroupLocations);
    }

    candidates.add(TargetLocationCandidateInvariant.INSTANCE);

    if (pOptions.terminateOnCounterexample) {
      return new StaticCandidateProvider(candidates) {

        private boolean safetyPropertyConfirmed = false;

        @Override
        public Iterator<CandidateInvariant> iterator() {
          final Iterator<CandidateInvariant> iterator = super.iterator();
          return new Iterator<>() {

            private CandidateInvariant candidate;

            @Override
            public boolean hasNext() {
              return !safetyPropertyConfirmed && iterator.hasNext();
            }

            @Override
            public CandidateInvariant next() {
              if (safetyPropertyConfirmed) {
                throw new NoSuchElementException(
                    "No more candidates available: The safety property has already been"
                        + " confirmed.");
              }
              return candidate = iterator.next();
            }

            @Override
            public void remove() {
              if (candidate instanceof ExpressionTreeLocationInvariant) {
                ExpressionTreeLocationInvariant expressionTreeLocationInvariant =
                    (ExpressionTreeLocationInvariant) candidate;

                // Remove the location from the group
                String groupId = expressionTreeLocationInvariant.getGroupId();
                Collection<CFANode> remainingLocations = candidateGroupLocations.get(groupId);
                remainingLocations.remove(expressionTreeLocationInvariant.getLocation());

                // If no location remains, the invariant has been disproved at all possible
                // locations
                if (remainingLocations.isEmpty()) {
                  pShutdownManager.requestShutdown("Incorrect invariant: " + candidate);
                }
              }
              iterator.remove();
            }
          };
        }

        @Override
        public void confirmCandidates(Iterable<CandidateInvariant> pCandidates) {
          super.confirmCandidates(pCandidates);
          if (Iterables.contains(pCandidates, TargetLocationCandidateInvariant.INSTANCE)) {
            safetyPropertyConfirmed = true;
          }
        }
      };
    }
    return new StaticCandidateProvider(candidates);
  }

  /**
   * Gets the relevant assume edges.
   *
   * @param pTargetLocations the predetermined target locations.
   * @return the relevant assume edges.
   */
  private static Set<AssumeEdge> getRelevantAssumeEdges(Collection<CFANode> pTargetLocations) {
    final Set<AssumeEdge> assumeEdges = new LinkedHashSet<>();
    Set<CFANode> visited = new HashSet<>(pTargetLocations);
    Queue<CFANode> waitlist = new ArrayDeque<>(pTargetLocations);
    while (!waitlist.isEmpty()) {
      CFANode current = waitlist.poll();
      for (CFAEdge enteringEdge : CFAUtils.enteringEdges(current)) {
        CFANode predecessor = enteringEdge.getPredecessor();
        if (enteringEdge.getEdgeType() == CFAEdgeType.AssumeEdge) {
          assumeEdges.add((AssumeEdge) enteringEdge);
        } else if (visited.add(predecessor)) {
          waitlist.add(predecessor);
        }
      }
    }
    return assumeEdges;
  }

  @Override
  public void adjustmentSuccessful(ConfigurableProgramAnalysis pCpa) {
    algorithm.adjustmentSuccessful(pCpa);
  }

  @Override
  public void adjustmentRefused(ConfigurableProgramAnalysis pCpa) {
    algorithm.adjustmentRefused(pCpa);
  }

  private interface CfaCandidateInvariantExtractorFactory {

    Iterable<CandidateInvariant> create(
        CFA pCfa,
        Specification pSpecification,
        TargetLocationProvider pTargetLocationProvider,
        LogManager pLogger)
        throws InvalidConfigurationException;
  }

  private enum CfaCandidateInvariantExtractorFactories
      implements CfaCandidateInvariantExtractorFactory {
    NONE {

      @Override
      public Iterable<CandidateInvariant> create(
          CFA pCfa,
          Specification pSpecification,
          TargetLocationProvider pTargetLocationProvider,
          LogManager pLogger) {
        return ImmutableSet.of();
      }
    },

    ASSUME_EDGES_PLAIN {

      @Override
      public Iterable<CandidateInvariant> create(
          CFA pCfa,
          Specification pSpecification,
          TargetLocationProvider pTargetLocationProvider,
          LogManager pLogger)
          throws InvalidConfigurationException {
        Optional<ImmutableSet<CFANode>> loopHeads = pCfa.getAllLoopHeads();
        if (!loopHeads.isPresent()) {
          throw new InvalidConfigurationException(
              "Loop structure not available but required to generate candidate invariants.");
        }
        Set<AssumeEdge> assumeEdges =
            getRelevantAssumeEdges(
                pTargetLocationProvider.tryGetAutomatonTargetLocations(
                    pCfa.getMainFunction(), pSpecification));
        return asNegatedCandidateInvariants(assumeEdges, loopHeads.orElseThrow());
      }
    },

    ASSUME_EDGE_TEMPLATES {

      @Override
      public Iterable<CandidateInvariant> create(
          CFA pCfa,
          Specification pSpecification,
          TargetLocationProvider pTargetLocationProvider,
          LogManager pLogger)
          throws InvalidConfigurationException {
        if (!pCfa.getVarClassification().isPresent()) {
          throw new InvalidConfigurationException(
              "Variable classification not available but required to generate candidate"
                  + " invariants.");
        }
        Optional<ImmutableSet<CFANode>> loopHeads = pCfa.getAllLoopHeads();
        if (!loopHeads.isPresent()) {
          throw new InvalidConfigurationException(
              "Loop structure not available but required to generate candidate invariants.");
        }
        Set<AssumeEdge> baseAssumeEdges =
            getRelevantAssumeEdges(
                pTargetLocationProvider.tryGetAutomatonTargetLocations(
                    pCfa.getMainFunction(), pSpecification));
        Multimap<CFAEdge, String> idsOnEdges = LinkedHashMultimap.create();
        Map<String, AIdExpression> idExpressions = new HashMap<>();
        for (AssumeEdge baseAssumeEdge : baseAssumeEdges) {
          for (AIdExpression idExpression :
              CFAUtils.traverseRecursively(baseAssumeEdge.getExpression())
                  .filter(AIdExpression.class)) {
            ASimpleDeclaration decl = idExpression.getDeclaration();
            if (decl != null) {
              if (!(idExpression instanceof CExpression)) {
                throw new InvalidConfigurationException(
                    "Assume-edge templates are only supported for C code.");
              }
              idExpressions.put(decl.getQualifiedName(), idExpression);
              idsOnEdges.put(baseAssumeEdge, decl.getQualifiedName());
            }
          }
        }

        VariableClassification varClassification = pCfa.getVarClassification().orElseThrow();
        Equivalence<AssumeEdge> equivalence =
            new Equivalence<>() {

              @Override
              protected boolean doEquivalent(AssumeEdge pA, AssumeEdge pB) {
                return pA.getTruthAssumption() == pB.getTruthAssumption()
                    && pA.getExpression().equals(pB.getExpression());
              }

              @Override
              protected int doHash(AssumeEdge pEdge) {
                return Objects.hash(pEdge.getExpression(), pEdge.getTruthAssumption());
              }
            };

        Set<Wrapper<AssumeEdge>> assumeEdges = new LinkedHashSet<>();
        for (AssumeEdge baseAssumeEdge : baseAssumeEdges) {
          assumeEdges.add(equivalence.wrap(baseAssumeEdge));
        }
        Queue<AssumeEdge> waitlist = new ArrayDeque<>(baseAssumeEdges);
        Set<Partition> partitions = varClassification.getPartitions();
        CBinaryExpressionBuilder binExpBuilder =
            new CBinaryExpressionBuilder(pCfa.getMachineModel(), pLogger);
        while (!waitlist.isEmpty()) {
          AssumeEdge edge = waitlist.poll();
          List<AssumeEdge> successors = new ArrayList<>();
          AExpression expression = edge.getExpression();
          for (String idOnEdge : idsOnEdges.get(edge)) {
            AIdExpression variable = idExpressions.get(idOnEdge);
            if (variable != null) {
              for (Partition partition : partitions) {
                if (partition.getVars().contains(idOnEdge)) {
                  for (String s : partition.getVars()) {
                    if (!s.equals(idOnEdge)) {
                      AIdExpression substitute = idExpressions.get(s);
                      if (substitute == null) {
                        for (CFAEdge e : partition.getEdges().keySet()) {
                          for (AIdExpression idExpression :
                              FluentIterable.from(CFAUtils.getAstNodesFromCfaEdge(e))
                                  .transformAndConcat(CFAUtils::traverseRecursively)
                                  .filter(AIdExpression.class)) {
                            ASimpleDeclaration decl = idExpression.getDeclaration();
                            if (decl != null) {
                              idsOnEdges.put(e, decl.getQualifiedName());
                              if (substitute == null && decl.getQualifiedName().equals(s)) {
                                substitute = idExpression;
                              }
                            }
                          }
                        }
                      }
                      if (substitute != null && allowSubstitution(variable, substitute)) {
                        try {
                          CExpression newExpression =
                              ExpressionSubstitution.applySubstitution(
                                  (CExpression) expression,
                                  (CExpression) variable,
                                  (CExpression) substitute,
                                  binExpBuilder);
                          String raw = newExpression.toASTString();
                          if (!edge.getTruthAssumption()) {
                            raw = "!(" + raw + ")";
                          }
                          AssumeEdge newEdge =
                              new CAssumeEdge(
                                  raw,
                                  edge.getFileLocation(),
                                  edge.getPredecessor(),
                                  edge.getSuccessor(),
                                  newExpression,
                                  edge.getTruthAssumption());
                          successors.add(newEdge);
                        } catch (SubstitutionException e) {
                          throw new AssertionError(
                              String.format(
                                  "Invalid substitution of %s by %s", variable, substitute));
                        }
                      }
                    }
                  }
                }
                break;
              }
            }
          }
          for (AssumeEdge newEdge : successors) {
            if (assumeEdges.add(equivalence.wrap(newEdge))) {
              for (AIdExpression idExpression :
                  CFAUtils.traverseRecursively(newEdge.getExpression())
                      .filter(AIdExpression.class)) {
                ASimpleDeclaration decl = idExpression.getDeclaration();
                if (decl != null) {
                  idsOnEdges.put(newEdge, decl.getQualifiedName());
                }
              }
              waitlist.add(newEdge);
            }
          }
        }
        return asNegatedCandidateInvariants(
            FluentIterable.from(assumeEdges).transform(Wrapper::get), loopHeads.orElseThrow());
      }

      private boolean allowSubstitution(AIdExpression pVariable, AIdExpression pSubstitute) {
        if (pVariable.getExpressionType().equals(pSubstitute.getExpressionType())) {
          return true;
        }
        if (!(pVariable.getExpressionType() instanceof CType)
            || !(pSubstitute.getExpressionType() instanceof CType)) {
          return false;
        }
        CType typeA = ((CType) pVariable.getExpressionType()).getCanonicalType();
        CType typeB = ((CType) pSubstitute.getExpressionType()).getCanonicalType();
        return typeA.canBeAssignedFrom(typeB);
      }
    },

    LINEAR_TEMPLATES {

      @Override
      public Iterable<CandidateInvariant> create(
          CFA pCfa,
          Specification pSpecification,
          TargetLocationProvider pTargetLocationProvider,
          LogManager pLogger)
          throws InvalidConfigurationException {
        if (!pCfa.getVarClassification().isPresent()) {
          throw new InvalidConfigurationException(
              "Variable classification not available but required to generate candidate"
                  + " invariants.");
        }
        VariableClassification varClassification = pCfa.getVarClassification().orElseThrow();
        Optional<ImmutableSet<CFANode>> loopHeads = pCfa.getAllLoopHeads();
        if (!loopHeads.isPresent()) {
          throw new InvalidConfigurationException(
              "Loop structure not available but required to generate candidate invariants.");
        }
        MachineModel machineModel = pCfa.getMachineModel();

        Collection<String> vars =
            new ArrayList<>(
                Sets.intersection(
                    varClassification.getAssumedVariables().elementSet(),
                    varClassification.getIntAddVars()));
        Map<String, CIdExpression> idExpressions = new LinkedHashMap<>();
        NavigableSet<BigInteger> constants = new TreeSet<>();
        Multimap<CType, String> typePartitions = LinkedHashMultimap.create();
        Map<CIdExpression, AFunctionDeclaration> functions = new HashMap<>();
        for (String var : vars) {
          if (!idExpressions.containsKey(var)) {
            for (Partition partition : varClassification.getIntAddPartitions()) {
              if (partition.getVars().contains(var)) {
                constants.addAll(partition.getValues());
                for (CFAEdge e : partition.getEdges().keySet()) {
                  for (AIdExpression idExpression :
                      FluentIterable.from(CFAUtils.getAstNodesFromCfaEdge(e))
                          .transformAndConcat(CFAUtils::traverseRecursively)
                          .filter(AIdExpression.class)) {
                    if (!(idExpression instanceof CIdExpression)) {
                      throw new InvalidConfigurationException(
                          "Linear templates are only supported for C code.");
                    }
                    ASimpleDeclaration decl = idExpression.getDeclaration();
                    if (decl != null) {
                      CIdExpression id = (CIdExpression) idExpression;
                      idExpressions.put(decl.getQualifiedName(), id);
                      CType type = id.getExpressionType().getCanonicalType();
                      typePartitions.put(type, decl.getQualifiedName());
                      functions.put(id, e.getPredecessor().getFunction());
                      if (type instanceof CSimpleType) {
                        constants.add(machineModel.getMaximalIntegerValue((CSimpleType) type));
                      }
                    }
                  }
                }
                break;
              }
            }
          }
        }

        CBinaryExpressionBuilder binExpBuilder =
            new CBinaryExpressionBuilder(pCfa.getMachineModel(), pLogger);
        Multimap<AFunctionDeclaration, CExpression> instantiatedTemplates =
            LinkedHashMultimap.create();
        for (Map.Entry<CType, Collection<String>> typePartition :
            typePartitions.asMap().entrySet()) {
          CType type = typePartition.getKey();
          if (type instanceof CSimpleType) {
            Collection<String> variables = typePartition.getValue();
            BigInteger max = machineModel.getMaximalIntegerValue((CSimpleType) type);
            for (String x : variables) {
              CIdExpression xId = idExpressions.get(x);
              CSimpleDeclaration xDecl = xId.getDeclaration();
              if (!(xDecl instanceof CVariableDeclaration)) {
                continue;
              }
              CVariableDeclaration xVarDecl = (CVariableDeclaration) xDecl;
              AFunctionDeclaration function = functions.get(xId);
              for (String y : variables) {
                if (x.equals(y)) {
                  continue;
                }
                CIdExpression yId = idExpressions.get(y);
                AFunctionDeclaration yFunction = functions.get(yId);
                if (xVarDecl.isGlobal()) {
                  function = yFunction;
                } else {
                  CSimpleDeclaration yDecl = yId.getDeclaration();
                  if (!(yDecl instanceof CVariableDeclaration)) {
                    continue;
                  }
                  CVariableDeclaration yVarDecl = (CVariableDeclaration) yDecl;
                  if (yVarDecl.isGlobal()) {
                    function = yFunction;
                  } else if (!function.equals(yFunction)) {
                    continue;
                  }
                }
                for (BigInteger c : constants.subSet(BigInteger.ONE, max)) {
                  try {
                    CIntegerLiteralExpression cLit =
                        new CIntegerLiteralExpression(FileLocation.DUMMY, type, c);
                    CExpression cX =
                        binExpBuilder.buildBinaryExpression(xId, cLit, BinaryOperator.MULTIPLY);
                    CExpression cXLeqY =
                        binExpBuilder.buildBinaryExpression(cX, yId, BinaryOperator.LESS_EQUAL);
                    CExpression cXGeqY =
                        binExpBuilder.buildBinaryExpression(cX, yId, BinaryOperator.GREATER_EQUAL);
                    instantiatedTemplates.put(function, cXLeqY);
                    instantiatedTemplates.put(function, cXGeqY);
                  } catch (UnrecognizedCodeException e) {
                    throw new AssertionError(
                        String.format("Invalid template instantiation %s * %s <= %s", c, x, y));
                  }
                }
              }
            }
          }
        }

        List<AssumeEdge> assumeEdges = new ArrayList<>();
        for (Map.Entry<AFunctionDeclaration, Collection<CExpression>> entry :
            instantiatedTemplates.asMap().entrySet()) {
          AFunctionDeclaration function = entry.getKey();
          Collection<CExpression> expressions = entry.getValue();
          CFANode dummyPred = new CFANode(function);
          CFANode dummySucc = new CFANode(function);
          for (CExpression instantiatedTemplate : expressions) {
            String raw = "!(" + instantiatedTemplate.toASTString() + ")";
            CAssumeEdge dummyEdge =
                new CAssumeEdge(
                    raw, FileLocation.DUMMY, dummyPred, dummySucc, instantiatedTemplate, false);
            assumeEdges.add(dummyEdge);
          }
        }
        return asNegatedCandidateInvariants(assumeEdges, loopHeads.orElseThrow());
      }
    };
  }

  private static Iterable<CandidateInvariant> asNegatedCandidateInvariants(
      Iterable<AssumeEdge> pAssumeEdges, Set<CFANode> pLoopHeads) {
    return FluentIterable.from(pAssumeEdges)
        .transformAndConcat(
            e -> {
              return FluentIterable.from(pLoopHeads).transform(n -> new EdgeFormulaNegation(n, e));
            });
  }
}
