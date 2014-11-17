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

import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.IADeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.livevar.LiveVariablesCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Multimap;


public class LiveVariables {


  private final Multimap<CFANode, String> liveVariables;
  private final Set<String> globalVariables;
  private final VariableClassification variableClassification;

  private LiveVariables(Multimap<CFANode, String> pLiveVariables, VariableClassification pVariableClassification, Set<String> pGlobalVariables) {
    liveVariables = pLiveVariables;
    globalVariables = pGlobalVariables;
    variableClassification = pVariableClassification;
  }

  public boolean isVariableLive(String varName, CFANode location) {
    if (globalVariables.contains(varName)
        || variableClassification.getAddressedVariables().contains(varName)) {
      return true;
    } else if (variableClassification.getIrrelevantVariables().contains(varName)) {
      return false;
    }
    boolean isContained = liveVariables.containsEntry(location, varName);
    if (!isContained) {
      System.out.println("Query for "+ varName + " at location " + location);
      System.out.println(liveVariables.get(location));
      System.out.println("-------------------------------------------");
    }
    return liveVariables.containsEntry(location, varName);
  }

  public static class LiveVariablesBuilder {

    private Multimap<CFANode, String> liveVariables = null;
    private VariableClassification variableClassification = null;
    private Set<String> globalVariables = null;

    public Optional<LiveVariables> build() {
      // if not all parts are available we return an absent optional
      if (liveVariables == null
          || variableClassification == null
          || globalVariables == null) {
        return Optional.absent();
      }

      return Optional.of(new LiveVariables(liveVariables, variableClassification, globalVariables));
    }

    public void addLiveVariablesByVariableClassification(VariableClassification vc) {
      variableClassification = vc;
    }

    public void addLiveVariablesFromGlobalScope(List<Pair<IADeclaration, String>> pList) {
      globalVariables = FluentIterable.<Pair<IADeclaration, String>>from(pList)
                                      .transform(new Function<Pair<IADeclaration, String>, String>() {
                                                    @Override
                                                    public String apply(Pair<IADeclaration, String> pInput) {
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

      // put all FunctionExitNodes into the waitlist
      for (FunctionEntryNode node : pCfa.getAllFunctionHeads()) {
        FunctionExitNode exitNode = node.getExitNode();
        analysisParts.reachedSet.add(analysisParts.cpa.getInitialState(exitNode),
            analysisParts.cpa.getInitialPrecision(exitNode));
      }

      logger.log(Level.INFO, "Starting live variables collection ...");
      try {
        do {
          analysisParts.algorithm.run(analysisParts.reachedSet);
        } while (analysisParts.reachedSet.hasWaitingState());

      } catch (CPAException | InterruptedException e) {
        logger.log(Level.WARNING, "Could not compute live variables.\n" + e.getMessage());
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
        CoreComponentsFactory factory =
            new CoreComponentsFactory(getLiveVariablesConfiguration(), logger, shutdownNotifier);
        ConfigurableProgramAnalysis cpa = factory.createCPA(cfa, null, false);
        Algorithm algorithm = factory.createAlgorithm(cpa, null, cfa, null);
        ReachedSet reached = factory.createReachedSet();
        return Optional.of(new AnalysisParts(cpa, algorithm, reached));
      } catch (InvalidConfigurationException | CPAException e) {
        // this should never happen, but if it does we continue the
        // analysis without having the live variable analysis
        return Optional.absent();
      }
    }

    private static Configuration getLiveVariablesConfiguration() throws InvalidConfigurationException {
      ConfigurationBuilder configBuilder = Configuration.builder();
      configBuilder.setOption("analysis.traversal.order", "BFS");
      configBuilder.setOption("analysis.traversal.usePostorder", "true");
      configBuilder.setOption("cpa", "cpa.arg.ARGCPA");
      configBuilder.setOption("ARGCPA.cpa", "cpa.composite.CompositeCPA");
      configBuilder.setOption("CompositeCPA.cpas", "cpa.location.LocationCPABackwardsNoTargets,"
          + " cpa.callstack.CallstackCPABackwards,"
          + " cpa.livevar.LiveVariablesCPA");
      configBuilder.setOption("output.disable", "true");
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
