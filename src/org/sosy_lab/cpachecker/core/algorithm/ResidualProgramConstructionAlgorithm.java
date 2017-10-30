/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import javax.annotation.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.Specification;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.cpa.automaton.ControlAutomatonCPA;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackCPA;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackStateEqualsWrapper;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.location.LocationCPA;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.powerset.PowerSetCPA;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.cwriter.ARGToCTranslator;

@Options(prefix = "residualprogram")
public class ResidualProgramConstructionAlgorithm implements Algorithm, StatisticsProvider {

  public enum ResidualGenStrategy {
    SLICING,
    CONDITION,
    CONDITION_PLUS_CFA_FOLD,
    CONDITION_PLUS_LOOP_FOLD,
    COMBINATION
  }

  @Option(secure = true, name = "strategy",
      description = "which strategy to use to generate the residual program")
  private ResidualGenStrategy constructionStrategy = ResidualGenStrategy.CONDITION;

  @Option(secure = true, name = "file", description = "write residual program to file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path residualProgram = Paths.get("residualProgram.c");

  @Option(secure = true, name = "assumptionGuider",
      description = "set specification file to automaton which guides analysis along assumption produced by incomplete analysis,e.g., config/specification/AssumptionGuidingAutomaton.spc, to enable residual program from combination of program and assumption condition")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private @Nullable Path conditionSpec = null;

  @Option(secure = true, name = "assumptionFile", description = "set path to file which contains the condition")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private @Nullable Path condition = null;

  private final CFA cfa;
  private final Specification spec;
  protected final LogManager logger;
  protected final ShutdownNotifier shutdown;

  private CPAAlgorithm cpaAlgorithm;

  private final ARGToCTranslator translator;


  public ResidualProgramConstructionAlgorithm(final CFA pCfa, final Configuration pConfig,
      final LogManager pLogger, final ShutdownNotifier pShutdown, final Specification pSpec,
      final ConfigurableProgramAnalysis pCpa, final Algorithm pInnerAlgorithm)
      throws InvalidConfigurationException {
    this(pCfa, pConfig, pLogger, pShutdown, pSpec);

    if(pInnerAlgorithm instanceof CPAAlgorithm) {
      cpaAlgorithm = (CPAAlgorithm) pInnerAlgorithm;
    } else {
      throw new InvalidConfigurationException("For residual program generation, only the CPAAlgorithm is required.");
    }

    checkCPAConfiguration(pCpa);
  }

  protected ResidualProgramConstructionAlgorithm(final CFA pCfa, final Configuration pConfig,
      final LogManager pLogger, final ShutdownNotifier pShutdown, final Specification pSpec)
      throws InvalidConfigurationException {
    pConfig.inject(this, ResidualProgramConstructionAlgorithm.class);

    cfa = pCfa;
    logger = pLogger;
    shutdown = pShutdown;
    spec = pSpec;
    translator = new ARGToCTranslator(logger, pConfig);

    checkConfiguration();
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet)
      throws CPAException, InterruptedException, CPAEnabledAnalysisPropertyViolationException {
    Preconditions.checkState(checkInitialState(pReachedSet.getFirstState()),
        "CONDITION, CONDITION_PLUS_FOLD, and COMBINATION strategy require assumption automaton (condition) and assumption guiding automaton in specification");

    AlgorithmStatus status = AlgorithmStatus.SOUND_AND_PRECISE;
    status = status.withPrecise(false);

    logger.log(Level.INFO, "Start construction of residual program.");
    cpaAlgorithm.run(pReachedSet);

    ARGState argRoot = (ARGState) pReachedSet.getFirstState();

    CFANode mainFunction = AbstractStates.extractLocation(argRoot);
    assert (mainFunction != null);

    if (pReachedSet.hasWaitingState()) {
      logger.log(Level.SEVERE, "Analysis run to get structure of residual program is incomplete. ",
          "Ensure that you use cpa.automaton.breakOnTargetState=-1 in your configuration.");
      return status.withSound(false);
    }

    Set<ARGState> addPragma;
    switch (constructionStrategy) {
      case COMBINATION:
        addPragma = getAllTargetStates(pReachedSet);
        break;
      case SLICING:
        addPragma = getAllTargetStatesNotFullyExplored(pReachedSet);
        break;
      default: // CONDITION, CONDITION_PLUS_FOLD no effect
        addPragma = null;
    }

    logger.log(Level.INFO, "Write residual program to file.");
    if (!writeResidualProgram(argRoot, addPragma)) {
      try {
        Files.deleteIfExists(residualProgram);
      } catch (IOException e) {
      }
      throw new CPAException("Failed to write residual program.");
    }

    logger.log(Level.INFO, "Finished construction of residual program. ",
        "If the selected strategy is SLICING or COMBINATION, please continue with the slicing tool (Frama-C)");

    return status;
  }

  protected Set<ARGState> getAllTargetStates(final ReachedSet pReachedSet) {
    logger.log(Level.INFO, "All target states in residual program are relevant and will be considered in slicing.");
    return Sets.newHashSet(
        Iterables.filter(Iterables.filter(pReachedSet, ARGState.class), state -> state.isTarget()));
  }

  private Set<ARGState> getAllTargetStatesNotFullyExplored(final ReachedSet pNodesOfInlinedProg) {
    logger.log(Level.INFO, "Identify all target states in original program which are not fully explored according to condition and are relevant for slicing.");
    Multimap<CFANode, CallstackStateEqualsWrapper> unexploredTargetStates =
        getUnexploredTargetStates(
            AbstractStates.extractLocation(pNodesOfInlinedProg.getFirstState()));
    if (unexploredTargetStates == null) {
      logger.log(Level.WARNING,
          "Failed to identify target locations in program which have not been explored completely. ",
          "Assume that all target locations are unexplored.");
      return getAllTargetStates(pNodesOfInlinedProg);
    }
    return Sets.newHashSet(Iterables.filter(Iterables.filter(pNodesOfInlinedProg, ARGState.class),
        state -> unexploredTargetStates.containsEntry(AbstractStates.extractLocation(state),
            new CallstackStateEqualsWrapper(
                AbstractStates.extractStateByType(state, CallstackState.class)))));
  }

  private @Nullable Multimap<CFANode, CallstackStateEqualsWrapper> getUnexploredTargetStates(
      final CFANode mainFunction) {
    Preconditions.checkState(condition != null, "Please set option residualprogram.assumptionFile.");
    try {
      ConfigurationBuilder configBuilder = Configuration.builder();
      configBuilder.setOption("cpa", "cpa.arg.ARGCPA");
      configBuilder.setOption("ARGCPA.cpa", "cpa.composite.CompositeCPA");
      configBuilder.setOption("CompositeCPA.cpas",
          "cpa.location.LocationCPA,cpa.callstack.CallstackCPA");
      configBuilder.setOption("cpa.automaton.breakOnTargetState", "-1");
      Configuration config = configBuilder.build();

      CoreComponentsFactory coreComponents =
          new CoreComponentsFactory(config, logger, shutdown, new AggregatedReachedSets());

      Specification constrSpec = spec;
      List<Path> specList = Lists.newArrayList(constrSpec.getSpecFiles());
      specList.add(conditionSpec);
      specList.add(condition);
      constrSpec = Specification.fromFiles(spec.getProperties(),
          specList, cfa, config, logger);

      ConfigurableProgramAnalysis cpa = coreComponents.createCPA(cfa, constrSpec);

      ReachedSet reached = coreComponents.createReachedSet();
      reached.add(cpa.getInitialState(mainFunction, StateSpacePartition.getDefaultPartition()),
          cpa.getInitialPrecision(mainFunction, StateSpacePartition.getDefaultPartition()));

      Algorithm algo = CPAAlgorithm.create(cpa, logger, config, shutdown);
      algo.run(reached);

      if (reached.hasWaitingState()) {
        logger.log(Level.SEVERE, "Analysis run to get structure of residual program is incomplete");
        return null;
      }

      HashMultimap<CFANode, CallstackStateEqualsWrapper> result =
          HashMultimap.create(cfa.getAllNodes().size(), cfa.getNumberOfFunctions());

      for (AbstractState targetState : Iterables.filter(reached,
          state -> state instanceof Targetable && ((Targetable) state).isTarget())) {
        result.put(AbstractStates.extractLocation(targetState), new CallstackStateEqualsWrapper(
            AbstractStates.extractStateByType(targetState, CallstackState.class)));

      }
      return result;
    } catch (InvalidConfigurationException | CPAException | IllegalArgumentException
        | InterruptedException e1) {
      logger.log(Level.SEVERE, "Analysis to build structure of residual program failed", e1);
      return null;
    }
  }

  private ARGState foldARG(final ARGState pARGRoot) {
    ARGState newRoot;
    Map<Pair<LocationState, CallstackStateEqualsWrapper>, ARGState> foldedNodesToNewARGNode =
        new HashMap<>();

    Set<ARGState> seen = new HashSet<>();
    Deque<Pair<ARGState, Pair<LocationState, CallstackStateEqualsWrapper>>> toProcess =
        new ArrayDeque<>();
    ARGState currentARGState, newChild;
    Pair<LocationState, CallstackStateEqualsWrapper> foldedNode, foldedChild;

    foldedNode = Pair.of(AbstractStates.extractStateByType(pARGRoot, LocationState.class),
        new CallstackStateEqualsWrapper(
            AbstractStates.extractStateByType(pARGRoot, CallstackState.class)));
    seen.add(pARGRoot);
    toProcess.add(Pair.of(pARGRoot, foldedNode));

    newRoot = new ARGState(foldedNode.getFirst(), null);
    foldedNodesToNewARGNode.put(foldedNode, newRoot);

    while (!toProcess.isEmpty()) {
      currentARGState = toProcess.peek().getFirst();
      foldedNode = toProcess.pop().getSecond();

      for (ARGState child : currentARGState.getChildren()) {
        if (seen.add(child)) {
          foldedChild = Pair.of(AbstractStates.extractStateByType(child, LocationState.class),
              new CallstackStateEqualsWrapper(
                  AbstractStates.extractStateByType(child, CallstackState.class)));
          toProcess.add(Pair.of(child, foldedChild));

          newChild = foldedNodesToNewARGNode.get(foldedChild);

          if (newChild == null) {
            newChild =
                new ARGState(foldedChild.getFirst(), foldedNodesToNewARGNode.get(foldedNode));
            foldedNodesToNewARGNode.put(foldedChild, newChild);
          } else {
            if (!foldedNodesToNewARGNode.get(foldedNode).getChildren().contains(newChild)) {
              newChild.addParent(foldedNodesToNewARGNode.get(foldedNode));
            }
          }


        }
      }
    }

    return newRoot;
  }

  private ARGState foldARGAtLoops(final ARGState pRoot) {
    Preconditions.checkState(cfa.getAllLoopHeads().isPresent());
    Set<CFANode> loopHeads = cfa.getAllLoopHeads().get();

    Map<ARGState, Set<ARGState>> oldARGToFoldedState = new HashMap<>();
    Map<Integer, ARGState> foldedStateToNewARGState = new HashMap<>();
    Map<ARGState, Set<ARGState>> newARGToFoldedState = new HashMap<>();
    Map<Pair<CFANode, CallstackStateEqualsWrapper>, Set<ARGState>> loopHeadToFoldedARGState =
        new HashMap<>();

    Deque<ARGState> waitlist = new ArrayDeque<>();
    Set<ARGState> foldedNode;
    ARGState oldState, newState, newChild;
    CFANode loc;
    CFAEdge edge;
    Pair<CFANode, CallstackStateEqualsWrapper> inlinedLoc;

    foldedNode = new HashSet<>();
    foldedNode.add(pRoot);
    oldARGToFoldedState.put(pRoot, foldedNode);
    newState = new ARGState(pRoot.getWrappedState(), null);
    foldedStateToNewARGState.put(System.identityHashCode(foldedNode), newState);
    newARGToFoldedState.put(newState, foldedNode);
    loc = AbstractStates.extractLocation(pRoot);
    if(loopHeads.contains(loc)) {
      loopHeadToFoldedARGState.put(
          Pair.of(
              loc,
              new CallstackStateEqualsWrapper(
                  AbstractStates.extractStateByType(pRoot, CallstackState.class))),
          foldedNode);
    }
    waitlist.push(pRoot);

    while (!waitlist.isEmpty()) {
      oldState = waitlist.pop();
      newState =
          foldedStateToNewARGState.get(System.identityHashCode(oldARGToFoldedState.get(oldState)));

      for (ARGState child : oldState.getChildren()) {
        if (!oldARGToFoldedState.containsKey(child)) {
          loc = AbstractStates.extractLocation(child);

          if (loopHeads.contains(loc)) {
            inlinedLoc =
                Pair.of(
                    loc,
                    new CallstackStateEqualsWrapper(
                        AbstractStates.extractStateByType(child, CallstackState.class)));
            foldedNode = loopHeadToFoldedARGState.get(inlinedLoc);
            if (foldedNode == null) {
              foldedNode = new HashSet<>();
              loopHeadToFoldedARGState.put(inlinedLoc, foldedNode);
              newChild = new ARGState(child.getWrappedState(), null);
              assert (!foldedStateToNewARGState.containsKey(System.identityHashCode(foldedNode)));
              foldedStateToNewARGState.put(System.identityHashCode(foldedNode), newChild);
            }

          } else {
            foldedNode = null;
            for (ARGState newARGChild : newState.getChildren()) {
              edge = oldState.getEdgeToChild(child);
              if (edge != null && edge.equals(newState.getEdgeToChild(newARGChild))) {
                foldedNode = newARGToFoldedState.get(newARGChild);
                // there should be only one such child, thus break
                break;
              }
            }

            if (foldedNode == null) {
              foldedNode = new HashSet<>();
              newChild = new ARGState(child.getWrappedState(), null);
              assert (!foldedStateToNewARGState.containsKey(System.identityHashCode(foldedNode)));
              foldedStateToNewARGState.put(System.identityHashCode(foldedNode), newChild);
              newARGToFoldedState.put(newChild, foldedNode);
            }
          }

          oldARGToFoldedState.put(child, foldedNode);
          foldedNode.add(child);
          waitlist.push(child);
        }

        newChild =
            foldedStateToNewARGState.get(System.identityHashCode(oldARGToFoldedState.get(child)));
        newChild.addParent(newState);
      }
    }

    return foldedStateToNewARGState.get(System.identityHashCode(oldARGToFoldedState.get(pRoot)));
  }

  private String getResidualProgramText(final ARGState pARGRoot,
      @Nullable final Set<ARGState> pAddPragma) throws CPAException {
    if (constructionStrategy == ResidualGenStrategy.CONDITION_PLUS_CFA_FOLD) {
      Preconditions.checkState(pAddPragma == null);

      return translator.translateARG(foldARG(pARGRoot));
    } else if (constructionStrategy == ResidualGenStrategy.CONDITION_PLUS_LOOP_FOLD) {
      Preconditions.checkState(pAddPragma == null);

      return translator.translateARG(foldARGAtLoops(pARGRoot));
    }
    return translator.translateARG(pARGRoot, pAddPragma);
  }

  protected boolean writeResidualProgram(final ARGState pArgRoot,
      @Nullable final Set<ARGState> pAddPragma) throws InterruptedException {
    logger.log(Level.INFO, "Generate residual program");
    try (Writer writer = IO.openOutputFile(residualProgram, Charset.defaultCharset())) {
      writer.write(getResidualProgramText(pArgRoot, pAddPragma));
    } catch (IOException e) {
      logger.logUserException(Level.WARNING, e, "Could not write residual program to file");
      return false;
    } catch (CPAException e) {
      logger.logException(Level.SEVERE, e, "Failed to generate residual program.");
      return false;
    }
    String mainFunction = AbstractStates.extractLocation(pArgRoot).getFunctionName();
    assert (isValidResidualProgram(mainFunction));
    return true;
  }

  private boolean isValidResidualProgram(String mainFunction) throws InterruptedException {
    try {
      CFACreator cfaCreator = new CFACreator(
          Configuration.builder()
              .setOption("analysis.entryFunction", mainFunction)
              .setOption("parser.usePreprocessor", "true")
              .setOption("analysis.useLoopStructure", "false")
              .build(),
          logger, shutdown);
      cfaCreator.parseFileAndCreateCFA(Lists.newArrayList(residualProgram.toString()));
    } catch (InvalidConfigurationException e) {
      logger.log(Level.SEVERE, "Default configuration unsuitable for parsing residual program.", e);
      return false;
    } catch (IOException | ParserException e) {
      logger.log(Level.SEVERE, "No valid residual program generated. ", e);
      return false;
    }
    return true;
  }

  protected void checkConfiguration() throws InvalidConfigurationException {
    if (constructionStrategy == ResidualGenStrategy.SLICING) {
      if (conditionSpec == null || condition == null) {
        throw new InvalidConfigurationException(
          "When selection SLICING strategy, also the options residualprogram.assumptionGuider and residualprogram.assumptionFile must be set."); }
    }
  }

  private void checkCPAConfiguration(final ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    if (pCpa instanceof ARGCPA && ((ARGCPA) pCpa).getWrappedCPAs().get(0) instanceof CompositeCPA) {
      CompositeCPA comCpa = (CompositeCPA) ((ARGCPA) pCpa).getWrappedCPAs().get(0);

      boolean considersLocation = false, considersCallstack = false;
      for (ConfigurableProgramAnalysis innerCPA : comCpa.getWrappedCPAs()) {
        if (innerCPA instanceof LocationCPA) {
          considersLocation = true;
        } else if (innerCPA instanceof CallstackCPA) {
          considersCallstack = true;
        } else if (!(innerCPA instanceof ControlAutomatonCPA)) {
          if (innerCPA instanceof PowerSetCPA) {
            for (ConfigurableProgramAnalysis cpaInSetJoin : CPAs
                .asIterable(((PowerSetCPA) innerCPA).getWrappedCPAs().get(0))) {
              if (!(cpaInSetJoin instanceof ControlAutomatonCPA
                  || cpaInSetJoin instanceof CompositeCPA)) {
                throw new InvalidConfigurationException(
                      "The CompositeCPA may only consider LocationCPA, CallstackCPA, SetJoinCPA, and AutomatonCPAs.");
              }
            }
          } else {

            throw new InvalidConfigurationException(
                "The CompositeCPA may only consider LocationCPA, CallstackCPA, SetJoinCPA, and AutomatonCPAs.");
          }
        }
      }

      if (!considersLocation || !considersCallstack) { throw new InvalidConfigurationException(
          "For residual program generation location and callstack information is required."); }

    } else {
      throw new InvalidConfigurationException(
          "Require an ARGCPA which wraps a CompositeCPA for residual program generation.");
    }
  }

  private boolean checkInitialState(final AbstractState initState) {
    if (constructionStrategy == ResidualGenStrategy.CONDITION
        || constructionStrategy == ResidualGenStrategy.CONDITION_PLUS_CFA_FOLD
        || constructionStrategy == ResidualGenStrategy.CONDITION_PLUS_LOOP_FOLD
        || constructionStrategy == ResidualGenStrategy.COMBINATION) {
      boolean considersAssumption = false, considersAssumptionGuider = false;

      for (AbstractState component : AbstractStates.asIterable(initState)) {
        if (component instanceof AutomatonState) {
          if (((AutomatonState) component).getOwningAutomatonName().equals("AssumptionAutomaton")) {
            considersAssumption = true;
          }
          if (((AutomatonState) component).getOwningAutomatonName()
              .equals("AssumptionGuidingAutomaton")) {
            considersAssumptionGuider = true;
          }
        }
      }
      if (!considersAssumption || !considersAssumptionGuider) { return false; }
    }

    return true;
  }

  protected ResidualGenStrategy getStrategy() {
    return constructionStrategy;
  }

  protected Specification getSpecification() {
    return spec;
  }

  protected @Nullable Path getAssumptionGuider() {
    return conditionSpec;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    cpaAlgorithm.collectStatistics(pStatsCollection);
  }

}
