// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.instrumentation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.instrumentation.InstrumentationAutomaton.InstrumentationProperty;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.Pair;

/**
 * This algorithm instruments a CFA of program using intrumentation operator and instrumentation
 * automaton.
 *
 * <p>Currently supported transformations are only no-overflow and termination to reachability.
 */
@Options(prefix = "instrumentation")
public class InstrumentationOperatorAlgorithm implements Algorithm {
  private final CFA cfa;
  private final LogManager logger;
  private final CProgramScope cProgramScope;

  @Option(
      secure = true,
      description =
          "toggle the strategy to determine the hardcoded instrumentation automaton to be used\n"
              + "TERMINATION: transform termination to reachability\n"
              + "NOOVERFLOW: transform no-overflow to reachability")
  private InstrumentationProperty instrumentationProperty = InstrumentationProperty.TERMINATION;

  public InstrumentationOperatorAlgorithm(CFA pCfa, Configuration pConfig, LogManager pLogger)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    cfa = pCfa;
    logger = pLogger;
    cProgramScope = new CProgramScope(pCfa, pLogger);
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    // Output the collected CFA information into AllCFAInfos
    try (BufferedWriter writer =
        Files.newBufferedWriter(new File("output/AllLoopInfos.txt").toPath(), StandardCharsets.UTF_8)) {
      StringBuilder allLoopInfos = new StringBuilder();
      // For some properties we construct more automata to more effectively track variables within
      // the scope. This map is used to map the automata to concrete line numbers in the code.
      Map<Integer, InstrumentationAutomaton> mapAutomataToLocations = new HashMap<>();
      Map<CFANode, Integer> mapLoopHeadsToLineNumbers = LoopInfoUtils.getMapOfLoopHeadsToLineNumbers(cfa);

      if (instrumentationProperty == InstrumentationProperty.TERMINATION) {
        for (NormalLoopInfo info : LoopInfoUtils.getAllNormalLoopInfos(cfa, cProgramScope)) {
          mapAutomataToLocations.put(info.loopLocation(),
                                     new InstrumentationAutomaton(instrumentationProperty,
                                                                  info.liveVariablesAndTypes()));
        }
      }

      // MAIN INSTRUMENTATION OPERATOR ALGORITHM
      // Initialize the search
      List<Pair<CFANode, InstrumentationState>> waitlist = new ArrayList<>();
      Set<Pair<CFANode, InstrumentationState>> reachlist = new HashSet<>();
      waitlist.add(Pair.of(cfa.getMetadata().getMainFunctionEntry(), new InstrumentationState()));

      while (!waitlist.isEmpty()) {
          Pair<CFANode, InstrumentationState> currentPair = waitlist.remove(waitlist.size() - 1);
          reachlist.add(currentPair);
          CFANode currentNode = currentPair.getFirst();
          InstrumentationState currentState = currentPair.getSecond();

          // Handling a trivial case, when the state does not match the node
          assert currentState != null;
          if (!currentState.stateMatchesCfaNode(currentNode, cfa)) {
            // If the current state was dummy, we have to look for an automaton that matches the
            // CFANode
            if (currentState.toString().equals("DUMMY") &&
                mapLoopHeadsToLineNumbers.containsKey(currentNode)) {
              Pair<CFANode, InstrumentationState> newPair = Pair.of(currentNode,
                  mapAutomataToLocations
                      .get(mapLoopHeadsToLineNumbers
                          .get(currentNode))
                      .getInitialState());
              waitlist.add(newPair);
            } else {

              assert currentNode != null;
              for (CFANode succ : getSuccessorsOfANode(currentNode)) {
                Pair<CFANode, InstrumentationState> newPair = Pair.of(succ, currentState);
                if (!reachlist.contains(newPair)) {
                  waitlist.add(newPair);
                }
              }
            }
          } else {
            assert currentNode != null;
            for (int i = 0; i < currentNode.getNumLeavingEdges(); i++) {
              CFAEdge edge = currentNode.getLeavingEdge(i);
            }
          }


      }
      writer.write(allLoopInfos.toString());
    } catch (IOException e) {
      logger.logException(Level.SEVERE, e, "The creation of file AllCFAInfos.txt failed!");
    }
    return AlgorithmStatus.NO_PROPERTY_CHECKED;
  }

  private Set<CFANode> getSuccessorsOfANode(CFANode pCFANode) {
    Set<CFANode> successors = new HashSet<>();
    for (int i = 0; i < pCFANode.getNumLeavingEdges(); i++) {
      successors.add(pCFANode.getLeavingEdge(i).getSuccessor());
    }
    return successors;
  }
}
