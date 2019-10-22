/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.util;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.core.Specification;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.CandidateInvariant;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.ExpressionTreeLocationInvariant;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.ExpressionTreeLocationInvariant.ManagerKey;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.expressions.And;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.expressions.ToFormulaVisitor;

/**
 * This class extracts invariants from the correctness witness automaton. Calling {@link
 * WitnessInvariantsExtractor#analyzeWitness()} analyzes the witness first by conducting a
 * reachability analysis over the witness automaton. Subsequently, the invariants can be extracted
 * from the reached set.
 */
public class WitnessInvariantsExtractor {

  private Configuration config;
  private LogManager logger;
  private CFA cfa;
  private ShutdownNotifier shutdownNotifier;
  private ReachedSet reachedSet;
  private Specification automatonAsSpec;

  /**
   * Creates an instance of {@link WitnessInvariantsExtractor} and uses {@code pSpecification} and
   * {@code pPathToWitnessFile} to build the witness automaton so this automaton can be applied as
   * specification ({@code automatonAsSpec}) for the reachability analysis that is subsequently
   * called.
   *
   * @param pConfig the configuration
   * @param pSpecification the specification
   * @param pLogger the logger
   * @param pCFA the cfa
   * @param pShutdownNotifier the shutdown notifier
   * @param pPathToWitnessFile the path to the witness file
   * @throws InvalidConfigurationException if the configuration is invalid
   */
  public WitnessInvariantsExtractor(
      Configuration pConfig,
      Specification pSpecification,
      LogManager pLogger,
      CFA pCFA,
      ShutdownNotifier pShutdownNotifier,
      Path pPathToWitnessFile)
      throws InvalidConfigurationException, CPAException, InterruptedException {
    this.config = generateLocalConfiguration(pConfig);
    this.logger = pLogger;
    this.cfa = pCFA;
    this.shutdownNotifier = pShutdownNotifier;
    this.automatonAsSpec = buildSpecification(pSpecification, pPathToWitnessFile);
    analyzeWitness();
  }

  /**
   * Creates an instance of {@link WitnessInvariantsExtractor} and uses {@code pAutomaton} so {@code
   * pAutomaton} can be applied as specification ({@code automatonAsSpec}) for the reachability
   * analysis that is subsequently called.
   *
   * @param pConfig the configuration
   * @param pAutomaton the automaton used as specificatigiton
   * @param pLogger the logger
   * @param pCFA the cfa
   * @param pShutdownNotifier the shutdown notifier
   * @throws InvalidConfigurationException if the configuration is invalid
   * @throws CPAException if an error occurs during the reachability analysis
   */
  public WitnessInvariantsExtractor(
      Configuration pConfig,
      Automaton pAutomaton,
      LogManager pLogger,
      CFA pCFA,
      ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException, CPAException {
    this.config = generateLocalConfiguration(pConfig);
    this.logger = pLogger;
    this.cfa = pCFA;
    this.shutdownNotifier = pShutdownNotifier;
    this.automatonAsSpec = buildSpecification(pAutomaton);
    analyzeWitness();
  }

  private Configuration generateLocalConfiguration(Configuration pConfig)
      throws InvalidConfigurationException {
    ConfigurationBuilder configBuilder =
        Configuration.builder()
            .loadFromResource(WitnessInvariantsExtractor.class, "witness-analysis.properties");
    List<String> copyOptions =
        Arrays.asList(
            "analysis.machineModel",
            "analysis.programNames",
            "cpa.callstack.skipRecursion",
            "cpa.callstack.skipVoidRecursion",
            "cpa.callstack.skipFunctionPointerRecursion",
            "witness.strictChecking",
            "witness.checkProgramHash");
    for (String copyOption : copyOptions) {
      configBuilder.copyOptionFromIfPresent(pConfig, copyOption);
    }
    return configBuilder.build();
  }

  private Specification buildSpecification(Specification pSpecification, Path pathToWitnessFile)
      throws InvalidConfigurationException, InterruptedException {
    return Specification.fromFiles(
        pSpecification.getProperties(),
        ImmutableList.of(pathToWitnessFile),
        cfa,
        config,
        logger,
        shutdownNotifier);
  }

  private Specification buildSpecification(Automaton pAutomaton) {
    List<Automaton> automata = new ArrayList<>(1);
    automata.add(pAutomaton);
    return Specification.fromAutomata(automata);
  }

  private void analyzeWitness() throws InvalidConfigurationException, CPAException {
    ReachedSetFactory reachedSetFactory = new ReachedSetFactory(config, logger);
    reachedSet = reachedSetFactory.create();
    CPABuilder builder = new CPABuilder(config, logger, shutdownNotifier, reachedSetFactory);
    ConfigurableProgramAnalysis cpa =
        builder.buildCPAs(cfa, automatonAsSpec, new AggregatedReachedSets());
    CPAAlgorithm algorithm = CPAAlgorithm.create(cpa, logger, config, shutdownNotifier);
    CFANode rootNode = cfa.getMainFunction();
    StateSpacePartition partition = StateSpacePartition.getDefaultPartition();
    try {
      reachedSet.add(
          cpa.getInitialState(rootNode, partition), cpa.getInitialPrecision(rootNode, partition));
      algorithm.run(reachedSet);
    } catch (InterruptedException e) {
      // Candidate collection was interrupted,
      // but instead of throwing the exception here,
      // let it be thrown by the invariant generator.
    }
  }

  /**
   * Extracts the invariants with their corresponding CFA location from {@link:
   * WitnessInvariantsExtractor#reachedSet}. For two invariants at the same CFA location the
   * conjunction is applied for the two invariants.
   *
   * @param pInvariants the set of location invariants that stores the extracted location invariants
   */
  @SuppressWarnings("unchecked")
  public void extractInvariantsFromReachedSet(
      final Set<ExpressionTreeLocationInvariant> pInvariants) {
    Map<ManagerKey, ToFormulaVisitor> toCodeVisitorCache = Maps.newConcurrentMap();
    for (AbstractState abstractState : reachedSet) {
      if (shutdownNotifier.shouldShutdown()) {
        return;
      }
      CFANode location = AbstractStates.extractLocation(abstractState);
      for (AutomatonState automatonState :
          AbstractStates.asIterable(abstractState).filter(AutomatonState.class)) {
        ExpressionTree<AExpression> candidate = automatonState.getCandidateInvariants();
        String groupId = automatonState.getInternalStateName();
        ExpressionTreeLocationInvariant previousInv = null;
        if (!candidate.equals(ExpressionTrees.getTrue())) {
          for (ExpressionTreeLocationInvariant inv : pInvariants) {
            if (inv.getLocation().equals(location)) {
              previousInv = inv;
            }
          }
          ExpressionTree<AExpression> previousExpression = ExpressionTrees.getTrue();
          if (previousInv != null) {
            ExpressionTree<?> expr = previousInv.asExpressionTree();
            previousExpression = (ExpressionTree<AExpression>) expr;
            pInvariants.remove(previousInv);
          }
          pInvariants.add(
              new ExpressionTreeLocationInvariant(
                  groupId, location, And.of(previousExpression, candidate), toCodeVisitorCache));
        }
      }
    }
  }

  /**
   * Extracts the invariants from {@link: WitnessInvariantsExtractor#reachedSet} and stores it in
   * {@code pCandidates}. The invariants are regarded as candidates that can hold at several CFA
   * locations. Therefore, {@code pCandidateGroupLocations} is used that groups CFANodes by using a
   * groupID. For two invariants that are part of the same group the conjunction is applied for the
   * two invariants.
   *
   * @param pCandidates stores the invariants which are regarded as candidates
   * @param pCandidateGroupLocations stores as key the groupID with its associated CFANodes
   */
  public void extractCandidatesFromReachedSet(
      final Set<CandidateInvariant> pCandidates,
      final Multimap<String, CFANode> pCandidateGroupLocations) {
    Set<ExpressionTreeLocationInvariant> expressionTreeLocationInvariants = Sets.newHashSet();
    Map<String, ExpressionTree<AExpression>> expressionTrees = Maps.newHashMap();
    Set<CFANode> visited = Sets.newHashSet();
    Multimap<CFANode, ExpressionTreeLocationInvariant> potentialAdditionalCandidates =
        HashMultimap.create();
    Map<ManagerKey, ToFormulaVisitor> toCodeVisitorCache = Maps.newConcurrentMap();
    for (AbstractState abstractState : reachedSet) {
      if (shutdownNotifier.shouldShutdown()) {
        return;
      }
      Iterable<CFANode> locations = AbstractStates.extractLocations(abstractState);
      Iterables.addAll(visited, locations);
      for (AutomatonState automatonState :
          AbstractStates.asIterable(abstractState).filter(AutomatonState.class)) {
        ExpressionTree<AExpression> candidate = automatonState.getCandidateInvariants();
        String groupId = automatonState.getInternalStateName();
        pCandidateGroupLocations.putAll(groupId, locations);
        if (!candidate.equals(ExpressionTrees.getTrue())) {
          ExpressionTree<AExpression> previous = expressionTrees.get(groupId);
          if (previous == null) {
            previous = ExpressionTrees.getTrue();
          }
          expressionTrees.put(groupId, And.of(previous, candidate));
          for (CFANode location : locations) {
            potentialAdditionalCandidates.removeAll(location);
            ExpressionTreeLocationInvariant candidateInvariant =
                new ExpressionTreeLocationInvariant(
                    groupId, location, candidate, toCodeVisitorCache);
            expressionTreeLocationInvariants.add(candidateInvariant);
            // Check if there are any leaving return edges:
            // The predecessors are also potential matches for the invariant
            for (FunctionReturnEdge returnEdge :
                CFAUtils.leavingEdges(location).filter(FunctionReturnEdge.class)) {
              CFANode successor = returnEdge.getSuccessor();
              if (!pCandidateGroupLocations.containsEntry(groupId, successor)
                  && !visited.contains(successor)) {
                potentialAdditionalCandidates.put(
                    successor,
                    new ExpressionTreeLocationInvariant(
                        groupId, successor, candidate, toCodeVisitorCache));
              }
            }
          }
        }
      }
    }
    for (Map.Entry<CFANode, Collection<ExpressionTreeLocationInvariant>> potentialCandidates :
        potentialAdditionalCandidates.asMap().entrySet()) {
      if (!visited.contains(potentialCandidates.getKey())) {
        for (ExpressionTreeLocationInvariant candidateInvariant : potentialCandidates.getValue()) {
          pCandidateGroupLocations.put(
              candidateInvariant.getGroupId(), potentialCandidates.getKey());
          expressionTreeLocationInvariants.add(candidateInvariant);
        }
      }
    }
    for (ExpressionTreeLocationInvariant expressionTreeLocationInvariant :
        expressionTreeLocationInvariants) {
      for (CFANode location :
          pCandidateGroupLocations.get(expressionTreeLocationInvariant.getGroupId())) {
        pCandidates.add(
            new ExpressionTreeLocationInvariant(
                expressionTreeLocationInvariant.getGroupId(),
                location,
                expressionTrees.get(expressionTreeLocationInvariant.getGroupId()),
                toCodeVisitorCache));
      }
    }
  }
}
