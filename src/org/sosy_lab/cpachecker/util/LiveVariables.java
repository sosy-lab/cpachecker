/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.util;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.*;
import static com.google.common.collect.FluentIterable.from;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.configuration.TimeSpanOption;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.livevar.LiveVariablesCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.resources.ResourceLimit;
import org.sosy_lab.cpachecker.util.resources.ResourceLimitChecker;
import org.sosy_lab.cpachecker.util.resources.WalltimeLimit;

import com.google.common.base.Equivalence;
import com.google.common.base.Equivalence.Wrapper;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Optional;
import com.google.common.collect.Collections2;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Ordering;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;

public class LiveVariables {

  public enum EvaluationStrategy {
    FUNCTION_WISE, GLOBAL
  }

  /**
   * Equivalence implementation especially for the use with live variables. We
   * have to use this wrapper, because of the storageType in CVariableDeclarations
   * which do not always have to be the same for exactly the same variable (e.g.
   * one declaration is extern, and afterwards the real declaration is following which
   * then has storageType auto, for live variables we need to consider them as one.
   */
  public static final Equivalence<ASimpleDeclaration> LIVE_DECL_EQUIVALENCE = new Equivalence<ASimpleDeclaration>() {

    @Override
    protected boolean doEquivalent(ASimpleDeclaration pA, ASimpleDeclaration pB) {
      if (pA instanceof CVariableDeclaration && pB instanceof CVariableDeclaration) {
        return ((CVariableDeclaration)pA).equalsWithoutStorageClass(pB);
      } else {
        return pA.equals(pB);
      }
    }

    @Override
    protected int doHash(ASimpleDeclaration pT) {
      if (pT instanceof CVariableDeclaration) {
        return ((CVariableDeclaration)pT).hashCodeWithOutStorageClass();
      } else {
        return pT.hashCode();
      }
    }
  };

  @Options(prefix="liveVar")
  private static class LiveVariablesConfiguration {

    @Option(toUppercase=true,
        description="By changing this option one can adjust the way how"
            + " live variables are created. Function-wise means that each"
            + " function is handled separately, global means that the whole"
            + " cfa is used for the computation.", secure=true)
    private EvaluationStrategy evaluationStrategy = EvaluationStrategy.FUNCTION_WISE;

    @Option(secure=true, description="Overall timelimit for collecting the liveness information."
        + "(use seconds or specify a unit; 0 for infinite)")
    @TimeSpanOption(codeUnit=TimeUnit.NANOSECONDS,
                    defaultUserUnit=TimeUnit.SECONDS,
                    min=0)
    private TimeSpan overallLivenessCheckTime = TimeSpan.ofNanos(0);

    @Option(secure=true, description="Timelimit for collecting the liveness information with one approach,"
        + " (p.e. if global analysis is selected and fails in the specified timelimit the function wise approach"
        + " will have the same time-limit afterwards to compute the live variables)."
        + "(use seconds or specify a unit; 0 for infinite)")
    @TimeSpanOption(codeUnit=TimeUnit.NANOSECONDS,
                    defaultUserUnit=TimeUnit.SECONDS,
                    min=0)
    private TimeSpan partwiseLivenessCheckTime = TimeSpan.ofSeconds(20);

    public LiveVariablesConfiguration(Configuration config) throws InvalidConfigurationException {
      config.inject(this);
    }
  }

  /**
   * This class regards every variable as live on every position in the program.
   */
  private static class AllVariablesAsLiveVariables extends LiveVariables {

    private FluentIterable<String> allVariables;
    private FluentIterable<ASimpleDeclaration> allVariableDecls;

    private AllVariablesAsLiveVariables(CFA cfa, List<Pair<ADeclaration, String>> globalsList) {
      super();
      checkNotNull(cfa);
      checkNotNull(globalsList);

      Set<ASimpleDeclaration> globalVars = FluentIterable.from(globalsList)
                                                          .transform(DECLARATION_FILTER)
                                                          .filter(notNull())
                                                          .filter(not(or(instanceOf(CTypeDeclaration.class),
                                                                         instanceOf(CFunctionDeclaration.class)))).toSet();

      final CFATraversal.EdgeCollectingCFAVisitor edgeCollectingVisitor = new CFATraversal.EdgeCollectingCFAVisitor();
      CFATraversal.dfs().traverseOnce(cfa.getMainFunction(), edgeCollectingVisitor);
      FluentIterable<ADeclarationEdge> edges = from(edgeCollectingVisitor.getVisitedEdges()).filter(ADeclarationEdge.class);

      // we have no information which variable is live at a certain node, so
      // when asked about the variables for a certain node, we return the whole
      // set of all variables of the analysed program
      allVariableDecls = edges.transform(new Function<ADeclarationEdge, ASimpleDeclaration>() {

        @Override
        public ASimpleDeclaration apply(ADeclarationEdge pInput) {
          return pInput.getDeclaration();
        }}).append(globalVars);


      allVariables = allVariableDecls.transform(ASimpleDeclaration.GET_QUALIFIED_NAME);
    }

    @Override
    public boolean isVariableLive(ASimpleDeclaration pVariable, CFANode pLocation) {
      return true;
    }

    @Override
    public boolean isVariableLive(String pVarName, CFANode pLocation) {
      return true;
    }

    @Override
    public FluentIterable<String> getLiveVariableNamesForNode(CFANode pNode) {
      return allVariables;
    }

    @Override
    public FluentIterable<ASimpleDeclaration> getLiveVariablesForNode(CFANode pNode) {
      return allVariableDecls;
    }
  }

  /**
   * constructor for creating the AllVariablesAsLiveVariables Object, should
   *not be used elsewhere
   */
  private LiveVariables() {
    variableClassification = null;
    liveVariablesStrings = null;
    globalVariables = null;
    evaluationStrategy = null;
    language = null;
    liveVariables = null;
    globalVariablesStrings = null;
  }

  // For ensuring deterministic behavior, all collections should be sorted!
  private final ImmutableSetMultimap<CFANode, Equivalence.Wrapper<ASimpleDeclaration>> liveVariables; // sorted by construction
  private final ImmutableSortedSet<Equivalence.Wrapper<ASimpleDeclaration>> globalVariables;
  private final VariableClassification variableClassification;
  private final EvaluationStrategy evaluationStrategy;
  private final Language language;

  /** For efficient access to the string representation of the declarations
   * we use these maps additionally.
   */
  private final ImmutableSetMultimap<CFANode, String> liveVariablesStrings; // sorted by construction
  private final ImmutableSortedSet<String> globalVariablesStrings;

  private LiveVariables(Multimap<CFANode, Equivalence.Wrapper<ASimpleDeclaration>> pLiveVariables,
                        VariableClassification pVariableClassification,
                        Set<Equivalence.Wrapper<ASimpleDeclaration>> pGlobalVariables,
                        EvaluationStrategy pEvaluationStrategy,
                        Language pLanguage) {

    Ordering<Equivalence.Wrapper<ASimpleDeclaration>> declarationOrdering = Ordering.natural().onResultOf(FROM_EQUIV_WRAPPER_TO_STRING);

    // ImmutableSortedSetMultimap does not exist, in order to create a sorted immutable Multimap
    // we sort it and create an immutable copy (Guava's Immutable* classes guarantee to keep the order).
    SortedSetMultimap<CFANode, Equivalence.Wrapper<ASimpleDeclaration>> sortedLiveVariables =
        TreeMultimap.create(Ordering.natural(), declarationOrdering);
    sortedLiveVariables.putAll(pLiveVariables);
    liveVariables = ImmutableSetMultimap.copyOf(sortedLiveVariables);
    assert pLiveVariables.size() == liveVariables.size() : "ASimpleDeclarations with identical qualified names";

    globalVariables = ImmutableSortedSet.copyOf(declarationOrdering, pGlobalVariables);
    assert pGlobalVariables.size() == globalVariables.size() : "Global ASimpleDeclarations with identical qualified names";

    variableClassification = pVariableClassification;
    evaluationStrategy = pEvaluationStrategy;
    language = pLanguage;

    globalVariablesStrings = ImmutableSortedSet.copyOf(Collections2.transform(globalVariables, FROM_EQUIV_WRAPPER_TO_STRING));

    liveVariablesStrings = ImmutableSetMultimap.copyOf(Multimaps.transformValues(liveVariables, FROM_EQUIV_WRAPPER_TO_STRING));
  }

  public boolean isVariableLive(ASimpleDeclaration variable, CFANode location) {
    String varName = variable.getQualifiedName();
    final Wrapper<ASimpleDeclaration> wrappedDecl = LIVE_DECL_EQUIVALENCE.wrap(variable);

    if (globalVariables.contains(wrappedDecl)
        || (language == Language.C
             && variableClassification.getAddressedVariables().contains(varName))
        || (evaluationStrategy == EvaluationStrategy.FUNCTION_WISE
            && !varName.startsWith(location.getFunctionName()))) {
      return true;
    }

    // check if a variable is live at a given point
    return liveVariables.containsEntry(location, wrappedDecl);
  }

  public boolean isVariableLive(final String varName, CFANode location) {
    if (globalVariablesStrings.contains(varName)
        || (language == Language.C
             && variableClassification.getAddressedVariables().contains(varName))
        || (evaluationStrategy == EvaluationStrategy.FUNCTION_WISE
            && !varName.startsWith(location.getFunctionName()))) {
      return true;
    }

    // check if a variable is live at a given point
    return liveVariablesStrings.containsEntry(location, varName);
  }

  /**
   * Return an iterable of all live variables at a given CFANode
   * without duplicates and with deterministic iteration order.
   */
  public FluentIterable<ASimpleDeclaration> getLiveVariablesForNode(CFANode pNode) {
    return from(liveVariables.get(pNode)).append(globalVariables).transform(
        FROM_EQUIV_WRAPPER);
  }

  /**
   * @return iterable of all variables which are alive at at least one node.
   */
  public FluentIterable<ASimpleDeclaration> getAllLiveVariables() {
    return from(ImmutableSet.copyOf(liveVariables.values())).append(globalVariables)
        .transform(FROM_EQUIV_WRAPPER);

  }

  /**
   * Return an iterable of all names of live variables at a given CFANode
   * without duplicates and with deterministic iteration order.
   */
  public FluentIterable<String> getLiveVariableNamesForNode(CFANode pNode) {
    return from(liveVariablesStrings.get(pNode)).append(globalVariablesStrings);
  }

  public static Optional<LiveVariables> createWithAllVariablesAsLive(final List<Pair<ADeclaration, String>> globalsList,
                                                           final MutableCFA pCFA) {
    return Optional.of((LiveVariables)new AllVariablesAsLiveVariables(pCFA, globalsList));
  }

  public static Optional<LiveVariables> create(final Optional<VariableClassification> variableClassification,
                                               final List<Pair<ADeclaration, String>> globalsList,
                                               final MutableCFA pCFA,
                                               final LogManager logger,
                                               final ShutdownNotifier shutdownNotifier,
                                               final Configuration config) throws InvalidConfigurationException {
    checkNotNull(variableClassification);
    checkNotNull(globalsList);
    checkNotNull(pCFA);
    checkNotNull(logger);
    checkNotNull(shutdownNotifier);

    // we cannot make any assumptions about c programs where we do not know
    // about the addressed variables
    if (pCFA.getLanguage() == Language.C && !variableClassification.isPresent()) {
      return Optional.of((LiveVariables)new AllVariablesAsLiveVariables(pCFA, globalsList));
    }

    // we need a cfa with variableClassification, thus we create one now
    CFA cfa = pCFA.makeImmutableCFA(variableClassification);

    // create configuration object, so that we know which analysis strategy should
    // be chosen later on
    LiveVariablesConfiguration liveVarConfig = new LiveVariablesConfiguration(config);

    ShutdownNotifier liveVarsNotifier = ShutdownNotifier.createWithParent(shutdownNotifier);
    List<ResourceLimit> limits;
    if (liveVarConfig.overallLivenessCheckTime.isEmpty()) {
      limits = Collections.emptyList();
    } else {
      limits = Collections.singletonList((ResourceLimit)WalltimeLimit.fromNowOn(liveVarConfig.overallLivenessCheckTime));
    }
    ResourceLimitChecker limitChecker = new ResourceLimitChecker(liveVarsNotifier, limits);

    limitChecker.start();
    LiveVariables liveVarObject = create0(variableClassification.orNull(), globalsList, logger, shutdownNotifier, cfa, liveVarConfig);
    limitChecker.cancel();

    return Optional.of(liveVarObject);
  }

  private static LiveVariables create0(final VariableClassification variableClassification,
                                                 final List<Pair<ADeclaration, String>> globalsList,
                                                 final LogManager logger,
                                                 final ShutdownNotifier shutdownNotifier,
                                                 final CFA cfa,
                                                 final LiveVariablesConfiguration config) throws AssertionError {
    // prerequisites for creating the live variables
    Set<Wrapper<ASimpleDeclaration>> globalVariables;
    switch (config.evaluationStrategy) {
    case FUNCTION_WISE: globalVariables = FluentIterable.from(globalsList)
                                                        .transform(DECLARATION_FILTER)
                                                        .filter(notNull())
                                                        .filter(not(or(instanceOf(CTypeDeclaration.class),
                                                                       instanceOf(CFunctionDeclaration.class))))
                                                        .transform(TO_EQUIV_WRAPPER)
                                                        .toSet();
      break;
    case GLOBAL: globalVariables = Collections.emptySet(); break;
    default:
      throw new AssertionError("Unhandled case statement: " + config.evaluationStrategy);
    }

    ShutdownNotifier liveVarsNotifier = ShutdownNotifier.createWithParent(shutdownNotifier);
    List<ResourceLimit> limits;
    if (config.partwiseLivenessCheckTime.isEmpty()) {
      limits = Collections.emptyList();
    } else {
      limits = Collections.singletonList((ResourceLimit)WalltimeLimit.fromNowOn(config.partwiseLivenessCheckTime));
    }
    ResourceLimitChecker limitChecker = new ResourceLimitChecker(liveVarsNotifier, limits);

    Optional<AnalysisParts> parts = getNecessaryAnalysisComponents(cfa, logger, liveVarsNotifier, config.evaluationStrategy);
    Multimap<CFANode, Wrapper<ASimpleDeclaration>> liveVariables = null;

    limitChecker.start();

    // create live variables
    if (parts.isPresent()) {
      liveVariables = addLiveVariablesFromCFA(cfa, logger, parts.get(), config.evaluationStrategy);
    }

    limitChecker.cancel();

    // when the analysis did not finish or could even not be created we return
    // an absent optional, but before we try the function-wise analysis if we
    // did not yet use it
    if (liveVariables == null && config.evaluationStrategy != EvaluationStrategy.FUNCTION_WISE) {
      logger.log(Level.INFO, "Global live variables collection failed, fallback to function-wise analysis.");
      config.evaluationStrategy = EvaluationStrategy.FUNCTION_WISE;
      return create0(variableClassification, globalsList, logger, shutdownNotifier, cfa, config);
    } else if (liveVariables == null) {
      return new AllVariablesAsLiveVariables(cfa, globalsList);
    }

    return new LiveVariables(liveVariables,
                             variableClassification,
                             globalVariables,
                             config.evaluationStrategy,
                             cfa.getLanguage());
  }

  private final static Function<Pair<ADeclaration, String>, ASimpleDeclaration> DECLARATION_FILTER =
      new Function<Pair<ADeclaration, String>, ASimpleDeclaration>() {
        @Override
        public ASimpleDeclaration apply(Pair<ADeclaration, String> pInput) {
          return pInput.getFirst();
      }};

  public final static Function<ASimpleDeclaration, Equivalence.Wrapper<ASimpleDeclaration>> TO_EQUIV_WRAPPER =
      new Function<ASimpleDeclaration, Equivalence.Wrapper<ASimpleDeclaration>>() {
    @Override
    public Equivalence.Wrapper<ASimpleDeclaration> apply(ASimpleDeclaration pInput) {
      return LIVE_DECL_EQUIVALENCE.wrap(pInput);
  }};

  private final static Function<Equivalence.Wrapper<ASimpleDeclaration>, ASimpleDeclaration> FROM_EQUIV_WRAPPER =
      new Function<Equivalence.Wrapper<ASimpleDeclaration>, ASimpleDeclaration>() {
    @Override
    public ASimpleDeclaration apply(Equivalence.Wrapper<ASimpleDeclaration> pInput) {
      return pInput.get();
    }};

  public final static Function<Equivalence.Wrapper<ASimpleDeclaration>, String> FROM_EQUIV_WRAPPER_TO_STRING =
      Functions.compose(ASimpleDeclaration.GET_QUALIFIED_NAME, FROM_EQUIV_WRAPPER);

  private static Multimap<CFANode, Wrapper<ASimpleDeclaration>> addLiveVariablesFromCFA(final CFA pCfa, final LogManager logger,
                                              AnalysisParts analysisParts, EvaluationStrategy evaluationStrategy) {

    Optional<LoopStructure> loopStructure = pCfa.getLoopStructure();

    // put all FunctionExitNodes into the waitlist
    final Collection<FunctionEntryNode> functionHeads;
    switch (evaluationStrategy) {
    case FUNCTION_WISE: functionHeads = pCfa.getAllFunctionHeads(); break;
    case GLOBAL: functionHeads = Collections.singleton(pCfa.getMainFunction()); break;
    default: throw new AssertionError("Unhandeld case statement: " + evaluationStrategy);
    }

    for (FunctionEntryNode node : functionHeads) {
      FunctionExitNode exitNode = node.getExitNode();
      if (pCfa.getAllNodes().contains(exitNode)) {
        analysisParts.reachedSet.add(analysisParts.cpa.getInitialState(exitNode, StateSpacePartition.getDefaultPartition()),
                                     analysisParts.cpa.getInitialPrecision(exitNode, StateSpacePartition.getDefaultPartition()));
      }
    }

    if(loopStructure.isPresent()){
      LoopStructure structure = loopStructure.get();
      ImmutableCollection<Loop> loops = structure.getAllLoops();

      for (Loop l : loops) {

        // we need only one loop head for each loop, as we are doing a merge
        // afterwards during the analysis, and we do never stop besides when
        // there is coverage (we have no target states)
        // additionally we have to remove all functionCallEdges from the outgoing
        // edges because the LoopStructure is not able to say that loops with
        // function calls inside have no outgoing edges
        if (from(l.getOutgoingEdges()).filter(not(instanceOf(FunctionCallEdge.class))).isEmpty()) {
          CFANode functionHead = l.getLoopHeads().iterator().next();
          analysisParts.reachedSet.add(analysisParts.cpa.getInitialState(functionHead, StateSpacePartition.getDefaultPartition()),
                                       analysisParts.cpa.getInitialPrecision(functionHead, StateSpacePartition.getDefaultPartition()));
        }
      }
    }

    logger.log(Level.INFO, "Starting live variables collection ...");
    try {
      do {
        analysisParts.algorithm.run(analysisParts.reachedSet);
      } while (analysisParts.reachedSet.hasWaitingState());

    } catch (CPAException | InterruptedException e) {
      logger.logUserException(Level.WARNING, e, "Could not compute live variables.");
      return null;
    }

    logger.log(Level.INFO, "Stopping live variables collection ...");

    LiveVariablesCPA liveVarCPA = ((WrapperCPA) analysisParts.cpa).retrieveWrappedCpa(LiveVariablesCPA.class);

    return liveVarCPA.getLiveVariables();
  }

  private static Optional<AnalysisParts> getNecessaryAnalysisComponents(final CFA cfa,
      final LogManager logger,
      final ShutdownNotifier shutdownNotifier,
      final EvaluationStrategy evaluationStrategy) {

    try {
      Configuration config;
      switch (evaluationStrategy) {
        case FUNCTION_WISE: config = getLocalConfiguration(); break;
        case GLOBAL: config = getGlobalConfiguration(); break;
        default: throw new AssertionError("Unhandled case statement: " + evaluationStrategy);
      }

      ReachedSetFactory reachedFactory = new ReachedSetFactory(config,
                                                               logger);
      ConfigurableProgramAnalysis cpa = new CPABuilder(config,
                                                       logger,
                                                       shutdownNotifier,
                                                       reachedFactory).buildCPAWithSpecAutomatas(cfa);
      Algorithm algorithm = CPAAlgorithm.create(cpa,
                                                logger,
                                                config,
                                                shutdownNotifier);
      ReachedSet reached = reachedFactory.create();
      return Optional.of(new AnalysisParts(cpa, algorithm, reached));

    } catch (InvalidConfigurationException | CPAException e) {
      // this should never happen, but if it does we continue the
      // analysis without having the live variable analysis
      logger.logUserException(Level.WARNING, e, "An error occured during the creation"
          + " of the necessary CPA parts for the live variables analysis.");
      return Optional.absent();
    }
  }


  private static Configuration getGlobalConfiguration() throws InvalidConfigurationException {
    ConfigurationBuilder configBuilder = Configuration.builder();
    configBuilder.setOption("analysis.traversal.order", "BFS");
    configBuilder.setOption("analysis.traversal.usePostorder", "true");
    configBuilder.setOption("analysis.traversal.useCallstack", "true");
    configBuilder.setOption("cpa", "cpa.composite.CompositeCPA");
    configBuilder.setOption("CompositeCPA.cpas", "cpa.location.LocationCPABackwardsNoTargets,"
                                               + "cpa.callstack.CallstackCPABackwards,"
                                               + "cpa.livevar.LiveVariablesCPA");
    configBuilder.setOption("cpa.location.followFunctionCalls", "true");
    configBuilder.setOption("cpa.liveVar.assumeGlobalVariablesAreAlwaysLive", "false");

    return configBuilder.build();
  }

  private static Configuration getLocalConfiguration() throws InvalidConfigurationException {
    ConfigurationBuilder configBuilder = Configuration.builder();
    configBuilder.setOption("analysis.traversal.order", "BFS");
    configBuilder.setOption("analysis.traversal.usePostorder", "true");
    configBuilder.setOption("cpa", "cpa.composite.CompositeCPA");
    configBuilder.setOption("CompositeCPA.cpas", "cpa.location.LocationCPABackwardsNoTargets,"
                                               + "cpa.livevar.LiveVariablesCPA");
    configBuilder.setOption("cpa.location.followFunctionCalls", "false");
    configBuilder.setOption("cpa.liveVar.assumeGlobalVariablesAreAlwaysLive", "true");

    return configBuilder.build();
  }

  private static class AnalysisParts {

    private final ConfigurableProgramAnalysis cpa;
    private final Algorithm algorithm;
    private final ReachedSet reachedSet;

    private AnalysisParts(ConfigurableProgramAnalysis pCPA, Algorithm pAlgorithm, ReachedSet pReachedSet) {
      cpa = pCPA;
      algorithm = pAlgorithm;
      reachedSet = pReachedSet;
    }
  }
}
