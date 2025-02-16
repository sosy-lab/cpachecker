// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.instrumentation;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
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
public class SequentializationOperatorAlgorithm implements Algorithm {
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

  @Option(secure = true, description = "Where to write machine readable new edges.")
  @FileOption(Type.OUTPUT_FILE)
  private Path newEdgesFile = Path.of("newEdgesInfo.txt");

  public SequentializationOperatorAlgorithm(CFA pCfa, Configuration pConfig, LogManager pLogger)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    cfa = pCfa;
    logger = pLogger;
    cProgramScope = new CProgramScope(pCfa, pLogger);
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    // Collect all the information about the new edges for the instrumented CFA
    Set<String> newEdges = new HashSet<>();

    // For some properties we construct more automata to more effectively track variables within
    // the scope. This map is used to map the automata to concrete line numbers in the code.
    Map<Integer, InstrumentationAutomaton> mapAutomataToLocations = new HashMap<>();
    Map<CFANode, Integer> mapNodesToLineNumbers;

    if (instrumentationProperty == InstrumentationProperty.TERMINATION
        || instrumentationProperty == InstrumentationProperty.TERMINATIONWITHCOUNTERS
        || instrumentationProperty == InstrumentationProperty.ONESTEPREACHABILITY) {
      int index = 0;
      mapNodesToLineNumbers = LoopInfoUtils.getMapOfLoopHeadsToLineNumbers(cfa);
      // We have to track what variables have already been defined
      Map<String, String> alreadyDefinedVariables = new HashMap<>();
      for (NormalLoopInfo info :
          LoopInfoUtils.includeAllTheOuterLiveVariablesInNestedLoop(
                  LoopInfoUtils.getAllNormalLoopInfos(cfa, cProgramScope))
              .stream()
              .sorted((info1, info2) -> Integer.compare(info1.loopLocation(), info2.loopLocation()))
              .collect(ImmutableSet.toImmutableSet())) {
        try {
          mapAutomataToLocations.put(
              info.loopLocation(),
              new InstrumentationAutomaton(
                  instrumentationProperty,
                  info.liveVariablesAndTypes(),
                  substractDeclaredVariables(info.liveVariablesAndTypes(), alreadyDefinedVariables),
                  index));
        } catch (IllegalArgumentException e) {
          logger.log(Level.SEVERE, "Unrecognized property to instrument.");
        }
        if (instrumentationProperty == InstrumentationProperty.TERMINATIONWITHCOUNTERS) {
          alreadyDefinedVariables.putAll(info.liveVariablesAndTypes());
        }
        index += 1;
      }
    } else {
      mapNodesToLineNumbers = ImmutableMap.of(cfa.getMainFunction(), 0);
      mapAutomataToLocations.put(
          0,
          new InstrumentationAutomaton(
              instrumentationProperty, ImmutableMap.of(), ImmutableMap.of(), 0));
    }
    // MAIN INSTRUMENTATION OPERATOR ALGORITHM
    // Initialize the search
    List<Pair<CFANode, InstrumentationState>> waitlist = new ArrayList<>();
    Set<Pair<CFANode, InstrumentationState>> reachlist = new HashSet<>();
    Map<CFANode, String> mapDecomposedOperationsCondition = new HashMap<>();
    waitlist.add(Pair.of(cfa.getMetadata().getMainFunctionEntry(), new InstrumentationState()));

    while (!waitlist.isEmpty()) {
      Pair<CFANode, InstrumentationState> currentPair = waitlist.remove(waitlist.size() - 1);
      reachlist.add(currentPair);
      CFANode currentNode = currentPair.getFirst();
      InstrumentationState currentState = currentPair.getSecond();

      // Handling a trivial case, when the state does not match the node
      assert currentState != null;
      if (!currentState.stateMatchesCfaNode(currentNode)) {
        // If the current state was dummy, we have to look for an automaton that matches the
        // CFANode
        if (currentState.toString().equals("DUMMY")
            && mapNodesToLineNumbers.containsKey(currentNode)) {
          isThePairNew(
              currentNode,
              mapAutomataToLocations.get(mapNodesToLineNumbers.get(currentNode)).getInitialState(),
              waitlist,
              reachlist);
        }
        assert currentNode != null;
        for (CFANode succ : getSuccessorsOfANode(currentNode)) {
          isThePairNew(succ, currentState, waitlist, reachlist);
        }
      } else {
        assert currentNode != null;
        boolean matched = false;
        for (int i = 0; i < currentNode.getNumLeavingEdges(); i++) {
          CFAEdge edge = currentNode.getLeavingEdge(i);
          for (InstrumentationTransition transition :
              currentState.getAutomatonOfTheState().getTransitions(currentState)) {
            ImmutableList<String> matchedVariables =
                transition.getPattern().matchThePattern(edge, mapDecomposedOperationsCondition);
            if (matchedVariables != null) {
              if (canBeDecomposed(
                      edge,
                      transition,
                      waitlist,
                      mapDecomposedOperationsCondition,
                      matchedVariables)
                  || isThePairNew(currentNode, transition.getDestination(), waitlist, reachlist)) {
                // If there is a self-loop in the automaton, we have to move forward.
                matched = matched || !transition.getDestination().equals(transition.getSource());
              }
              newEdges.add(
                  computeLineNumberBasedOnTransition(transition, edge)
                      + "|||"
                      + transition.getOperation().insertVariablesInsideOperation(matchedVariables));
            }
          }
        }
        if (!matched) {
          for (CFANode succ : getSuccessorsOfANode(currentNode)) {
            isThePairNew(succ, currentState, waitlist, reachlist);
          }
        }
      }
    }

    writeAllInformationIntoOutputFile(newEdges);
    return AlgorithmStatus.NO_PROPERTY_CHECKED;
  }

  /*
   * This method computes line number depending on the pattern. For example, if the pattern is
   * [!cond], then we want to add the edge only after the real statement in the program. Further, is
   * the line number could not be parsed and the source state of the transition is annotated with
   * INIT then the intended line number is 0. Moreover, if the order is BEFORE, we want to include
   * the edge one line before the real operation and similarly for AFTER.
   */
  private String computeLineNumberBasedOnTransition(
      InstrumentationTransition pTransition, CFAEdge pEdge) {
    if (pTransition.getSource().isInitialAnnotation()) {
      return "1";
    }
    if (pTransition.getSource().isFunctionHeadAnnotation()) {
      return Integer.toString(
          pEdge.getPredecessor().getFunction().getFileLocation().getStartingLineInOrigin() + 1);
    }
    try {
      // TODO: The location should be computed differently !
      int location;
      String fileLocation = pEdge.getFileLocation().toString();
      if (pTransition.getPattern().toString().equals("[!cond]")) {
        fileLocation = pEdge.getSuccessor().getLeavingEdge(0).getFileLocation().toString();
      }
      fileLocation = fileLocation.replaceFirst("line ", "");
      fileLocation =
          Iterables.get(Splitter.on('-').split(fileLocation), 0).replaceFirst("lines ", "");
      location = Integer.parseInt(fileLocation);
      if (pTransition.getOrderAsString().equals("AFTER")) {
        location += 1;
      }
      return Integer.toString(location);
    } catch (NumberFormatException e) {
      logger.logException(Level.SEVERE, e, "The line number is not Integer !");
    }
    return "";
  }

  private void writeAllInformationIntoOutputFile(Set<String> newEdges) {
    // Output the collected CFA information into newEdgesInfo
    try {
      IO.writeFile(newEdgesFile, StandardCharsets.UTF_8, String.join("\n", newEdges));
    } catch (IOException e) {
      logger.logException(Level.SEVERE, e, "The creation of file newEdgesInfo.txt failed!");
    }
  }

  /*
   * Checks if the CFAEdge contains a complex expression that can be decomposed into smaller pieces
   * and then creates arbitrary CFANodes in between with edges containing the simpler decomposed
   * expressions.
   */
  private boolean canBeDecomposed(
      CFAEdge pCFAEdge,
      InstrumentationTransition pTransition,
      List<Pair<CFANode, InstrumentationState>> pWaitlist,
      Map<CFANode, String> pDecomposedMap,
      ImmutableList<String> pMatchedVariables) {
    if (!pTransition.getPattern().toString().equals("ADD")
        && !pTransition.getPattern().toString().equals("SUB")
        && !pTransition.getPattern().toString().equals("MUL")
        && !pTransition.getPattern().toString().equals("MOD")
        && !pTransition.getPattern().toString().equals("DIV")
        && !pTransition.getPattern().toString().equals("SHIFT")
        && !pTransition.getPattern().toString().equals("EQ")
        && !pTransition.getPattern().toString().equals("GEQ")
        && !pTransition.getPattern().toString().equals("GR")
        && !pTransition.getPattern().toString().equals("LEQ")
        && !pTransition.getPattern().toString().equals("LS")
        && !pTransition.getPattern().toString().equals("NEQ")
        && !pTransition.getPattern().toString().equals("RSHIFT")
        && !pTransition.getPattern().toString().equals("OR")
        && !pTransition.getPattern().toString().equals("AND")
        && !pTransition.getPattern().toString().equals("XOR")
        && !pTransition.getPattern().toString().equals("NEG")) {
      return false;
    }

    AAstNode astNode = pCFAEdge.getRawAST().orElseThrow();
    if (astNode instanceof CFunctionCallAssignmentStatement) {
      decomposeFunction(
          pCFAEdge,
          pTransition,
          pWaitlist,
          pDecomposedMap,
          pMatchedVariables,
          (CFunctionCallAssignmentStatement) astNode);
      return true;
    }
    CExpression expression = LoopInfoUtils.extractExpression(astNode);
    CExpression operand1;
    CExpression operand2;

    if (expression instanceof CBinaryExpression) {
      operand1 = ((CBinaryExpression) expression).getOperand1();
      operand2 = ((CBinaryExpression) expression).getOperand2();
    } else {
      operand1 = ((CUnaryExpression) expression).getOperand();
      operand2 = ((CUnaryExpression) expression).getOperand();
    }

    if (!(operand1 instanceof CBinaryExpression || operand1 instanceof CUnaryExpression)
        && !(operand2 instanceof CBinaryExpression || operand2 instanceof CUnaryExpression)) {
      return false;
    }

    String condition = pMatchedVariables.size() != 5 ? "1" : pMatchedVariables.get(2);
    if (condition.contains("__CPAchecker_TMP")) {
      condition = "1";
    }
    CFANode node1 = CFANode.newDummyCFANode();
    CFANode node2 = CFANode.newDummyCFANode();

    node1.addLeavingEdge(
        new CStatementEdge(
            "(" + operand1.toASTString() + ")",
            new CExpressionStatement(pCFAEdge.getFileLocation(), operand1),
            pCFAEdge.getFileLocation(),
            node1,
            pCFAEdge.getSuccessor()));
    node2.addLeavingEdge(
        new CStatementEdge(
            "(" + operand2.toASTString() + ")",
            new CExpressionStatement(pCFAEdge.getFileLocation(), operand2),
            pCFAEdge.getFileLocation(),
            node2,
            pCFAEdge.getSuccessor()));

    pDecomposedMap.put(node1, condition);
    pDecomposedMap.put(node2, condition);

    pWaitlist.add(Pair.of(node1, pTransition.getSource()));
    pWaitlist.add(Pair.of(node2, pTransition.getSource()));
    return true;
  }

  /* Decomposes a CFAEdge with function call. */
  private void decomposeFunction(
      CFAEdge pCFAEdge,
      InstrumentationTransition pTransition,
      List<Pair<CFANode, InstrumentationState>> pWaitlist,
      Map<CFANode, String> pDecomposedMap,
      ImmutableList<String> pMatchedVariables,
      CFunctionCallAssignmentStatement pCallAssignmentStatement) {
    String condition = pMatchedVariables.size() != 5 ? "1" : pMatchedVariables.get(2);
    List<CExpression> parameters =
        pCallAssignmentStatement.getFunctionCallExpression().getParameterExpressions();

    for (CExpression parameter : parameters.subList(1, parameters.size())) {
      CFANode node1 = CFANode.newDummyCFANode();

      node1.addLeavingEdge(
          new CStatementEdge(
              parameter.toASTString(),
              new CExpressionStatement(pCFAEdge.getFileLocation(), parameter),
              pCFAEdge.getFileLocation(),
              node1,
              pCFAEdge.getSuccessor()));
      pDecomposedMap.put(node1, condition);
      pWaitlist.add(Pair.of(node1, pTransition.getSource()));
    }
  }

  /*
   * Checks if the pair (pCFANode, pState) has already been explored or not. If not, then it adds
   * the state into waitlist.
   */
  private boolean isThePairNew(
      CFANode pCFANode,
      InstrumentationState pState,
      List<Pair<CFANode, InstrumentationState>> pWaitlist,
      Set<Pair<CFANode, InstrumentationState>> pReachSet) {
    Pair<CFANode, InstrumentationState> newPair = Pair.of(pCFANode, pState);
    if (!pReachSet.contains(newPair) && !pWaitlist.contains(newPair)) {
      pWaitlist.add(newPair);
      return true;
    }
    return false;
  }

  private Set<CFANode> getSuccessorsOfANode(CFANode pCFANode) {
    Set<CFANode> successors = new HashSet<>();
    for (int i = 0; i < pCFANode.getNumLeavingEdges(); i++) {
      successors.add(pCFANode.getLeavingEdge(i).getSuccessor());
    }
    return successors;
  }

  private ImmutableMap<String, String> substractDeclaredVariables(
      ImmutableMap<String, String> pLiveVariables, Map<String, String> pAlreadyDefinedVariables) {
    Map<String, String> difference = new HashMap<>();
    for (Map.Entry<String, String> entry : pLiveVariables.entrySet()) {
      if (!pAlreadyDefinedVariables.containsKey(entry.getKey())) {
        difference.put(entry.getKey(), pLiveVariables.get(entry.getKey()));
      }
    }
    return ImmutableMap.copyOf(difference);
  }
}
