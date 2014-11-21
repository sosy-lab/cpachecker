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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.livevar.LiveVariablesCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.Multimap;


public class LiveVariables {


  private final Multimap<CFANode, String> liveVariables;
  private final Set<String> globalVariables;
  private final VariableClassification variableClassification;
  private final Set<String> alwaysLivePrefixes;

  private LiveVariables(Multimap<CFANode, String> pLiveVariables,
                        VariableClassification pVariableClassification,
                        Set<String> pGlobalVariables,
                        Set<String> pAlwaysLivePrefixes) {
    liveVariables = pLiveVariables;
    globalVariables = pGlobalVariables;
    variableClassification = pVariableClassification;
    alwaysLivePrefixes = pAlwaysLivePrefixes;
  }

  public boolean isVariableLive(String varName, CFANode location) {
    // all global, pseudo global (variables from other functions) and addressed
    // variables are always considered being live
    if (globalVariables.contains(varName)
        || variableClassification.getAddressedVariables().contains(varName)
        || !varName.startsWith(location.getFunctionName())
        || alwaysLivePrefixes.contains(location.getFunctionName())) {
      return true;

      // irrelevant variables from variable classification can be considered
      // as not being live
    } else if (!variableClassification.getRelevantVariables().contains(varName)) {
      return false;
    }

    // check if a variable is live at a given point
    return liveVariables.containsEntry(location, varName);
  }

  public static class LiveVariablesBuilder {

    private Multimap<CFANode, String> liveVariables = null;
    private VariableClassification variableClassification = null;
    private Set<String> globalVariables = null;
    private final Set<String> alwaysLivePrefixes = new HashSet<>();

    public Optional<LiveVariables> build() {
      // if not all parts are available we return an absent optional
      if (liveVariables == null
          || variableClassification == null
          || globalVariables == null) {
        return Optional.absent();
      }

      return Optional.of(new LiveVariables(liveVariables, variableClassification, globalVariables, alwaysLivePrefixes));
    }

    public void addLiveVariablesByVariableClassification(VariableClassification vc) {
      variableClassification = vc;
    }

    public void addLiveVariablesFromGlobalScope(List<Pair<ADeclaration, String>> pList) {
      globalVariables = FluentIterable.<Pair<ADeclaration, String>>from(pList)
                                      .transform(new Function<Pair<ADeclaration, String>, String>() {
                                                    @Override
                                                    public String apply(Pair<ADeclaration, String> pInput) {
                                                      return pInput.getFirst().getQualifiedName();
                                                    }}).filter(Predicates.notNull()).toSet();
    }

    public void addLiveVariablesFromCFA(final CFA pCfa, final LogManager logger,
                                        final ShutdownNotifier shutdownNotifier) {

      Optional<AnalysisParts> analysisPartsOpt = getNecessaryAnalysisComponents(pCfa, logger, shutdownNotifier);

      // without the analysis parts we cannot do the live variables analysis
      if (!analysisPartsOpt.isPresent()) {
        return;}

      AnalysisParts analysisParts = analysisPartsOpt.get();

      Optional<LoopStructure> loopStructure = pCfa.getLoopStructure();

      // put all FunctionExitNodes into the waitlist
      for (FunctionEntryNode node : pCfa.getAllFunctionHeads()) {
        FunctionExitNode exitNode = node.getExitNode();
        if (pCfa.getAllNodes().contains(exitNode)) {
          analysisParts.reachedSet.add(analysisParts.cpa.getInitialState(exitNode),
                                       analysisParts.cpa.getInitialPrecision(exitNode));

          // functionexitnode is not reachable due to (one or more) endless loops
          // in the code, thus we check them, too and insert the edges back to
          // the loophead, as starting nodes for our evaluation
        } else if(loopStructure.isPresent()){
          LoopStructure structure = loopStructure.get();
          ImmutableCollection<Loop> loops = structure.getLoopsForFunction(node.getFunctionName());
          boolean loopWithoutOutgoingEdgesFound = false;
          for (Loop l : loops) {
            if (l.getOutgoingEdges().isEmpty()) {
              loopWithoutOutgoingEdgesFound = true;
              for (CFANode n : l.getLoopHeads()) {
                analysisParts.reachedSet.add(analysisParts.cpa.getInitialState(n),
                                             analysisParts.cpa.getInitialPrecision(n));
              }
            }
          }

          if (!loopWithoutOutgoingEdgesFound && !loops.isEmpty()) {
            throw new AssertionError("Cannot handle live variables without having an edge to start for the function: " + node.getFunctionName());

            // no endless loops, but also the exitnode is not part of the cfa
            // -> everythin should be tracked
          } else {
            alwaysLivePrefixes.add(node.getFunctionName());
            logger.log(Level.INFO, "All variables live for function: " + node.getFunctionName());
          }

          // over-approximation here, we have to say that every variable
          // in this function is live, because we do not have any information
          // about them
        } else {
          alwaysLivePrefixes.add(node.getFunctionName());
          logger.log(Level.INFO, "All variables live for function: " + node.getFunctionName());
        }
      }

      logger.log(Level.INFO, "Starting live variables collection ...");
      try {
        do {
          analysisParts.algorithm.run(analysisParts.reachedSet);
        } while (analysisParts.reachedSet.hasWaitingState());

      } catch (CPAException | InterruptedException e) {
        logger.logUserException(Level.WARNING, e, "Could not compute live variables.");
        return;
      }

      logger.log(Level.INFO, "Stopping live variables collection ...");

      LiveVariablesCPA liveVarCPA = ((WrapperCPA) analysisParts.cpa).retrieveWrappedCpa(LiveVariablesCPA.class);

      liveVariables = liveVarCPA.getLiveVariables();
    }

    private static Optional<AnalysisParts> getNecessaryAnalysisComponents(final CFA cfa,
        final LogManager logger,
        final ShutdownNotifier shutdownNotifier) {

      try {
        ReachedSetFactory reachedFactory = new ReachedSetFactory(getLiveVariablesReachedConfiguration(),
                                                                 logger);
        ConfigurableProgramAnalysis cpa = new CPABuilder(getLiveVariablesCPAConfiguration(),
                                                         logger,
                                                         shutdownNotifier,
                                                         reachedFactory).buildCPAs(cfa);
        Algorithm algorithm = CPAAlgorithm.create(cpa,
                                                  logger,
                                                  Configuration.defaultConfiguration(),
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

    private static Configuration getLiveVariablesReachedConfiguration() throws InvalidConfigurationException {
      ConfigurationBuilder configBuilder = Configuration.builder();
      configBuilder.setOption("analysis.traversal.order", "BFS");
      configBuilder.setOption("analysis.traversal.usePostorder", "true");
      return configBuilder.build();
    }

    private static Configuration getLiveVariablesCPAConfiguration() throws InvalidConfigurationException {
      ConfigurationBuilder configBuilder = Configuration.builder();
      configBuilder.setOption("cpa", "cpa.arg.ARGCPA");
      configBuilder.setOption("ARGCPA.cpa", "cpa.composite.CompositeCPA");
      configBuilder.setOption("CompositeCPA.cpas", "cpa.location.LocationCPABackwardsNoTargets,"
          + " cpa.callstack.CallstackCPABackwards,"
          + " cpa.livevar.LiveVariablesCPA");
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
}
