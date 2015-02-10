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
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
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

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Collections2;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

public class LiveVariables {

  public enum EvaluationStrategy {
    FUNCTION_WISE, GLOBAL;
  }

  @Options(prefix="liveVar")
  private static class LiveVariablesConfiguration {

    @Option(toUppercase=true,
        description="By changing this option one can adjust the way how"
            + " live variables are created. Function-wise means that each"
            + " function is handled separately, global means that the whole"
            + " cfa is used for the computation.", secure=true)
    private EvaluationStrategy evaluationStrategy = EvaluationStrategy.FUNCTION_WISE;

    public LiveVariablesConfiguration(Configuration config) throws InvalidConfigurationException {
      config.inject(this);
    }
  }

  private final Multimap<CFANode, ASimpleDeclaration> liveVariables;
  private final Set<ASimpleDeclaration> globalVariables;
  private final VariableClassification variableClassification;
  private final EvaluationStrategy evaluationStrategy;
  private final Language language;

  /** For efficient access to the string representation of the declarations
   * we use these maps additionally.
   */
  private final Multimap<CFANode, String> liveVariablesStrings;
  private final Set<String> globalVariablesStrings;

  private LiveVariables(Multimap<CFANode, ASimpleDeclaration> pLiveVariables,
                        VariableClassification pVariableClassification,
                        Set<ASimpleDeclaration> pGlobalVariables,
                        EvaluationStrategy pEvaluationStrategy,
                        Language pLanguage) {
    liveVariables = pLiveVariables;
    globalVariables = pGlobalVariables;
    variableClassification = pVariableClassification;
    evaluationStrategy = pEvaluationStrategy;
    language = pLanguage;

    globalVariablesStrings = FluentIterable.from(globalVariables).transform(new Function<ASimpleDeclaration, String>() {
      @Override
      public String apply(ASimpleDeclaration pInput) {
        return pInput.getQualifiedName();
      }}).toSet();

    liveVariablesStrings = HashMultimap.<CFANode, String>create();
    for (Entry<CFANode, ASimpleDeclaration> e : liveVariables.entries()) {
      liveVariablesStrings.put(e.getKey(), e.getValue().getQualifiedName());
    }
  }

  public boolean isVariableLive(ASimpleDeclaration variable, CFANode location) {
    String varName = variable.getQualifiedName();

    if (globalVariables.contains(variable)
        || (language == Language.C
             && variableClassification.getAddressedVariables().contains(varName))
        || (evaluationStrategy == EvaluationStrategy.FUNCTION_WISE
            && !varName.startsWith(location.getFunctionName()))) {
      return true;
    }

    // check if a variable is live at a given point
    return liveVariables.containsEntry(location, variable);
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

  public Set<ASimpleDeclaration> getLiveVariablesForNode(CFANode node) {
    return ImmutableSet.<ASimpleDeclaration>builder().addAll(liveVariables.get(node)).addAll(globalVariables).build();
  }

  public Set<String> getLiveVariableNamesForNode(CFANode pNode) {
    return Sets.newHashSet(
      Collections2.transform(getLiveVariablesForNode(pNode), new Function<ASimpleDeclaration, String>() {
        @Override
        public String apply(ASimpleDeclaration pDecl) {
          return pDecl.getQualifiedName();
        }
      }));
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
      return Optional.absent();
    }

    // we need a cfa with variableClassification, thus we create one now
    CFA cfa = pCFA.makeImmutableCFA(variableClassification);

    // create configuration object, so that we know which analysis strategy should
    // be chosen later on
    LiveVariablesConfiguration liveVarConfig = new LiveVariablesConfiguration(config);

    return create0(variableClassification.orNull(), globalsList, logger, shutdownNotifier, cfa, liveVarConfig.evaluationStrategy);
  }

  private static Optional<LiveVariables> create0(final VariableClassification variableClassification,
                                                 final List<Pair<ADeclaration, String>> globalsList,
                                                 final LogManager logger,
                                                 final ShutdownNotifier shutdownNotifier,
                                                 final CFA cfa,
                                                 final EvaluationStrategy eval) throws AssertionError {
    // prerequisites for creating the live variables
    Set<ASimpleDeclaration> globalVariables;
    switch (eval) {
    case FUNCTION_WISE: globalVariables = FluentIterable.from(globalsList)
                                                        .transform(DECLARATION_FILTER)
                                                        .filter(notNull())
                                                        .filter(not(or(instanceOf(CTypeDeclaration.class),
                                                                       instanceOf(CFunctionDeclaration.class))))
                                                        .toSet();
      break;
    case GLOBAL: globalVariables = Collections.emptySet(); break;
    default:
      throw new AssertionError("Unhandled case statement: " + eval);
    }

    Optional<AnalysisParts> parts = getNecessaryAnalysisComponents(cfa, logger, shutdownNotifier, eval);
    Multimap<CFANode, ASimpleDeclaration> liveVariables = null;

    // create live variables
    if (parts.isPresent()) {
      liveVariables = addLiveVariablesFromCFA(cfa, logger, parts.get(), eval);
    }

    // when the analysis did not finish or could even not be created we return
    // an absent optional, but before we try the function-wise analysis if we
    // did not yet use it
    if (liveVariables == null && eval != EvaluationStrategy.FUNCTION_WISE) {
      logger.log(Level.INFO, "Global live variables collection failed, fallback to function-wise analysis.");
      return create0(variableClassification, globalsList, logger, shutdownNotifier, cfa, EvaluationStrategy.FUNCTION_WISE);
    } else if (liveVariables == null) {
      return Optional.absent();
    }

    return Optional.of(new LiveVariables(liveVariables,
                                         variableClassification,
                                         globalVariables,
                                         eval,
                                         cfa.getLanguage()));
  }

  private final static Function<Pair<ADeclaration, String>, ASimpleDeclaration> DECLARATION_FILTER =
      new Function<Pair<ADeclaration, String>, ASimpleDeclaration>() {
        @Override
        public ASimpleDeclaration apply(Pair<ADeclaration, String> pInput) {
          return pInput.getFirst();
      }};


  private static Multimap<CFANode, ASimpleDeclaration> addLiveVariablesFromCFA(final CFA pCfa, final LogManager logger,
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
