// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.instrumentation;

import com.google.common.base.Splitter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
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
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.instrumentation.InstrumentationAutomaton.InstrumentationProperty;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.threading.ThreadingTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.ast.AstCfaRelation;

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
  // The edges added for global variables and parameters at the beginning of functions
  private final Set<CFAEdge> addedDeclarationEdges = new HashSet<>();
  // The added edges for parameters
  private final Set<CFAEdge> parameterDeclarationEdges = new HashSet<>();
  // The return statements of each function, computed when they are needed first
  private @Nullable ImmutableListMultimap<String, CFAEdge> returnEdges = null;
  // The qualified names of the variables declared in loops that are registered before their uses
  private final Set<String> variablesRegisteredInUses = new HashSet<>();
  // The edges that use these variables, with the line before which the variable is registered
  private final ListMultimap<CFAEdge, Pair<CVariableDeclaration, Integer>>
      usesOfVariablesRegisteredInUses = ArrayListMultimap.create();

  @Option(
      secure = true,
      description =
          """
          toggle the strategy to determine the hardcoded instrumentation automaton to be used
          """)
  private InstrumentationProperty instrumentationProperty = InstrumentationProperty.TERMINATION;

  @Option(
      secure = true,
      description =
          """
          parsed instrumentation automaton from yaml file provided by TransVer's UI part
          """)
  private String parsedInstrumentationAutomaton = "";

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
    List<String> newEdges = new ArrayList<>();

    // For some properties we construct more automata to more effectively track variables within
    // the scope. This map is used to map the automata to concrete line numbers in the code.
    Map<Integer, InstrumentationAutomaton> mapAutomataToLocations = new HashMap<>();
    Map<CFANode, Integer> mapNodesToLineNumbers;

    if (instrumentationProperty == InstrumentationProperty.TERMINATION
        || instrumentationProperty == InstrumentationProperty.TERMINATIONWITHABORTS
        || instrumentationProperty == InstrumentationProperty.TERMINATIONWITHCOUNTERS
        || instrumentationProperty == InstrumentationProperty.ONESTEPREACHABILITY) {
      int index = 0;
      mapNodesToLineNumbers = LoopInfoUtils.getMapOfLoopHeadsToLineNumbers(cfa);
      // TODO: Fix the recursion instrumentation
      if (!LoopStructure.getRecursions(cfa).isEmpty()) {
        throw new CPAException("Instrumentation of this type does not support recursion for now.");
      }
      // We have to track what variables have already been defined
      Map<String, String> alreadyDefinedVariables = new HashMap<>();
      for (NormalLoopInfo info :
          LoopInfoUtils.includeAllTheOuterLiveVariablesInNestedLoop(
                  LoopInfoUtils.getAllNormalLoopInfos(cfa, cProgramScope))
              .stream()
              .sorted((info1, info2) -> Integer.compare(info1.loopLocation(), info2.loopLocation()))
              .collect(ImmutableSet.toImmutableSet())) {
        try {
          if (!parsedInstrumentationAutomaton.isBlank()) {
            mapAutomataToLocations.put(
                info.loopLocation(),
                new InstrumentationAutomaton(
                    parsedInstrumentationAutomaton,
                    info.liveVariablesAndTypes(),
                    substractDeclaredVariables(
                        info.liveVariablesAndTypes(), alreadyDefinedVariables),
                    index));
          } else {
            mapAutomataToLocations.put(
                info.loopLocation(),
                new InstrumentationAutomaton(
                    instrumentationProperty,
                    info.liveVariablesAndTypes(),
                    substractDeclaredVariables(
                        info.liveVariablesAndTypes(), alreadyDefinedVariables),
                    index));
          }
        } catch (IllegalArgumentException e) {
          logger.log(Level.SEVERE, "Unrecognized property to instrument.");
        }
        if (instrumentationProperty == InstrumentationProperty.TERMINATIONWITHCOUNTERS) {
          alreadyDefinedVariables.putAll(info.liveVariablesAndTypes());
        }
        index += 1;
      }
    } else {
      if (instrumentationProperty == InstrumentationProperty.MEMSAFETY) {
        initializeEdgesWithGlobalVariables();
        initializeEdgesWithParameters();
      }
      mapNodesToLineNumbers = ImmutableMap.of(cfa.getMainFunction(), 0);
      ImmutableMap<String, String> variablesWithTypes =
          instrumentationProperty == InstrumentationProperty.DATA_RACE
              ? getVariablesWithTypesFromFullCFA()
              : ImmutableMap.of();
      if (!parsedInstrumentationAutomaton.isBlank()) {
        mapAutomataToLocations.put(
            0,
            new InstrumentationAutomaton(
                parsedInstrumentationAutomaton, variablesWithTypes, ImmutableMap.of(), 0));
      } else {
        mapAutomataToLocations.put(
            0,
            new InstrumentationAutomaton(
                instrumentationProperty, variablesWithTypes, ImmutableMap.of(), 0));
      }
      if (mapAutomataToLocations.get(0).hasTransitionWithPattern("loop_declar")) {
        initializeVariablesRegisteredInUses();
      }
    }
    // MAIN INSTRUMENTATION OPERATOR ALGORITHM
    // Initialize the search
    List<Pair<CFANode, InstrumentationState>> waitlist = new ArrayList<>();
    Set<Pair<CFANode, InstrumentationState>> reachlist = new HashSet<>();
    Map<CFANode, String> mapDecomposedOperationsCondition = new HashMap<>();
    waitlist.add(Pair.of(cfa.getMetadata().getMainFunctionEntry(), new InstrumentationState()));

    while (!waitlist.isEmpty()) {
      Pair<CFANode, InstrumentationState> currentPair = waitlist.removeLast();
      reachlist.add(currentPair);
      CFANode currentNode = currentPair.getFirst();
      InstrumentationState currentState = currentPair.getSecond();

      // Functions started as threads are not reachable by the edges of the CFA,
      // so they are searched from the state in which the thread is created
      for (CFANode threadEntry : getEntriesOfStartedThreads(currentNode)) {
        isThePairNew(threadEntry, currentState, waitlist, reachlist);
      }

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
            String patternName = transition.getPattern().toString();
            boolean declaresVariableRegisteredInUses = isRegisteredInUses(edge);
            if ((patternName.equals("loop_declar") && !declaresVariableRegisteredInUses)
                || ((patternName.equals("declar") || patternName.equals("ptr_declar"))
                    && declaresVariableRegisteredInUses)) {
              // The variables registered before their uses are handled by loop_declar and
              // loop_var_use instead of declar and ptr_declar
              continue;
            }
            ImmutableList<String> matchedVariables =
                transition
                    .getPattern()
                    .matchThePattern(edge, mapDecomposedOperationsCondition, cfa.getMachineModel());
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
              if (patternName.equals("scope_end") || patternName.equals("loop_var_use")) {
                // These operations are inserted at several lines, which depend on the variables
                for (String newEdge :
                    computeEdgesForLifetimesOfVariables(transition, edge, matchedVariables)) {
                  if (!newEdges.contains(newEdge)) {
                    newEdges.add(newEdge);
                  }
                }
                continue;
              }
              String lineNumber =
                  computeLineNumberBasedOnTransition(transition, edge, matchedVariables);
              String newEdge =
                  lineNumber
                      + "|||"
                      + transition
                          .getOperation()
                          .insertVariablesInsideOperation(
                              matchedVariables, transition.getPattern());
              if (instrumentationProperty == InstrumentationProperty.NOOVERFLOW) {
                if (newEdge.contains("__CPAchecker_TMP")) {
                  throw new CPAException(
                      "Matching for line with function calls or operations inside the loop"
                          + " condition is unsupported.");
                }
                CFANode loopHead = getLoopHeadOfEdge(edge.getPredecessor(), edge);
                if (loopHead.isLoopStart()) {
                  addEdgesAtTheLoopEnd(loopHead, transition, matchedVariables, newEdges);
                }
              }
              if (!newEdges.contains(newEdge)) {
                newEdges.add(newEdge);
              }
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
   * !cond, then the edge has the line number of the condition, but we want to add the operation
   * at the first statement after the negated branch (e.g., after the loop) for both orders. Further,
   * if the line number could not be parsed and the source state of the transition is annotated with
   * INIT then the intended line number is 0. Moreover, if the order is BEFORE, we want to include
   * the edge one line before the real operation and similarly for AFTER.
   */
  private String computeLineNumberBasedOnTransition(
      InstrumentationTransition pTransition,
      CFAEdge pEdge,
      ImmutableList<String> pMatchedVariables) {
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
      String functionName;
      if ((pTransition.getPattern().toString().equals("ptr_deref")
              || pTransition.getPattern().toString().equals("ptr_declar"))
          && !pMatchedVariables.isEmpty()) {
        String matchedVar = pMatchedVariables.getFirst();
        functionName = matchedVar;
        if (!matchedVar.contains("[") && !matchedVar.contains("->")) {
          functionName = "*" + functionName;
        }
        if (matchedVar.contains("->")) {
          functionName = Iterables.get(Splitter.on("->").split(functionName), 0);
        }
      } else {
        functionName = pTransition.getPattern().getFunctionName();
      }
      // The line after the negated branch is already the line at which the operation has to be
      // inserted, so it must not be moved by the order AFTER.
      Integer lineAfterBranch = null;
      if (pTransition.getPattern().toString().equals("!cond")) {
        lineAfterBranch = getLineAfterBranch(pEdge.getSuccessor());
      }
      if (lineAfterBranch != null) {
        location = lineAfterBranch;
      } else {
        fileLocation = fileLocation.replaceFirst("line ", "");
        fileLocation =
            Iterables.get(Splitter.on('-').split(fileLocation), 0).replaceFirst("lines ", "");
        location = Integer.parseInt(fileLocation);
        if (pTransition.getOrderAsString().equals("AFTER")
            && !addedDeclarationEdges.contains(pEdge)) {
          location += 1;
        }
      }
      return pTransition.getOrderAsString().equals("SAME_LINE")
          ? location + "|||S|||" + functionName + "|||"
          : Integer.toString(location);
    } catch (NumberFormatException e) {
      logger.logException(Level.SEVERE, e, "The line number is not Integer !");
    }
    return "";
  }

  /*
   * Computes the line at which an operation has to be inserted so that it is executed right after
   * the branch that leads to pNode. This is the line of the first following edge with a real
   * location, e.g., the first statement after a loop. Edges without a real location, e.g., the
   * default return of a function, are skipped. If no such edge exists, the operation is inserted
   * before the closing brace of the function. Returns null if the line cannot be determined.
   */
  private @Nullable Integer getLineAfterBranch(CFANode pNode) {
    CFANode node = pNode;
    Set<CFANode> visited = new HashSet<>();
    while (node.getNumLeavingEdges() > 0 && visited.add(node)) {
      CFAEdge edge = node.getLeavingEdge(0);
      if (edge.getFileLocation().isRealLocation()) {
        return edge.getFileLocation().getStartingLineInOrigin();
      }
      node = edge.getSuccessor();
    }
    if (node instanceof FunctionExitNode exitNode
        && exitNode.getEntryNode().getFileLocation().isRealLocation()) {
      return exitNode.getEntryNode().getFileLocation().getEndingLineInOrigin();
    }
    return null;
  }

  private void writeAllInformationIntoOutputFile(List<String> newEdges) {
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
    if (!pTransition.getPattern().toString().equals("ptr_deref")
        && !pTransition.getPattern().toString().equals("ptr_declar")
        && !pTransition.getPattern().toString().equals("ADD")
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
    if (astNode instanceof CFunctionCallAssignmentStatement
        || astNode instanceof CFunctionCallStatement) {
      decomposeFunction(
          pCFAEdge, pTransition, pWaitlist, pDecomposedMap, pMatchedVariables, astNode);
      return true;
    }
    if (astNode instanceof CExpressionAssignmentStatement pCExpressionAssignmentStatement) {
      decomposeAssignmentStatement(
          pCFAEdge,
          pTransition,
          pWaitlist,
          pDecomposedMap,
          pMatchedVariables,
          pCExpressionAssignmentStatement);
      return true;
    }
    if (astNode instanceof CVariableDeclaration pCVariableDeclaration
        && pTransition.getPattern().toString().equals("ptr_deref")
        && !addedDeclarationEdges.contains(pCFAEdge)) {
      // Dereferences in the initializer, e.g., in 'int x = *p;'
      decomposeInitializer(
          pCFAEdge,
          pTransition,
          pWaitlist,
          pDecomposedMap,
          pMatchedVariables,
          pCVariableDeclaration.getInitializer());
      return true;
    }
    if (astNode instanceof CReturnStatement pCReturnStatement
        && pTransition.getPattern().toString().equals("ptr_deref")
        && pCReturnStatement.getReturnValue().isPresent()) {
      // Dereferences in the returned expression, e.g., in 'return *p;'
      decomposeExpression(
          pCFAEdge,
          pTransition,
          pWaitlist,
          pDecomposedMap,
          pMatchedVariables,
          pCReturnStatement.getReturnValue().orElseThrow());
      return true;
    }

    // Operators are matched also inside casts, pointer patterns are not
    boolean isPointerPattern = pTransition.getPattern().toString().startsWith("ptr_");
    CExpression expression = LoopInfoUtils.extractExpression(astNode, !isPointerPattern);
    CExpression operand1;
    CExpression operand2;

    if (expression instanceof CBinaryExpression pCBinaryExpression) {
      operand1 = pCBinaryExpression.getOperand1();
      operand2 = pCBinaryExpression.getOperand2();
    } else if (expression instanceof CUnaryExpression pCUnaryExpression) {
      operand1 = pCUnaryExpression.getOperand();
      operand2 = pCUnaryExpression.getOperand();
    } else {
      return false;
    }

    if (operand1 instanceof CCastExpression pCCastExpression) {
      operand1 = pCCastExpression.getOperand();
    }
    if (operand2 instanceof CCastExpression pCCastExpression) {
      operand2 = pCCastExpression.getOperand();
    }

    String condition = pMatchedVariables.size() != 5 ? "1" : pMatchedVariables.get(2);
    if (condition.contains("__CPAchecker_TMP")) {
      condition = "1";
    }
    CFANode node1 = CFANode.newDummyCFANode();
    CFANode node2 = CFANode.newDummyCFANode();

    // In case the predecessor is a loop-head, we need to copy all the entering edges
    // to the dummy node, because they are relevant for instrumenting the end of the loop.
    CFANode possibleLoopHead = getLoopHeadOfEdge(pCFAEdge.getPredecessor(), pCFAEdge);
    if (possibleLoopHead.isLoopStart()) {
      node1.setLoopStart();
      node2.setLoopStart();
      for (CFAEdge enteringEdge : possibleLoopHead.getAllEnteringEdges()) {
        node1.addEnteringEdge(
            new CStatementEdge(
                "", null, enteringEdge.getFileLocation(), enteringEdge.getPredecessor(), node1));
        node2.addEnteringEdge(
            new CStatementEdge(
                "", null, enteringEdge.getFileLocation(), enteringEdge.getPredecessor(), node2));
      }
    }

    node1.addLeavingEdge(
        new CStatementEdge(
            operand1.toASTString(),
            new CExpressionStatement(pCFAEdge.getFileLocation(), operand1),
            pCFAEdge.getFileLocation(),
            node1,
            pCFAEdge.getSuccessor()));
    node2.addLeavingEdge(
        new CStatementEdge(
            operand2.toASTString(),
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

  /**
   * The declarations of global variables are not considered in the CFA starting from the main
   * function entry node. For transformations like memory safety, this is important. Therefore, we
   * have to add them in the beginning.
   *
   * <p>The added edges have the location of the first statement of main, and the code for them is
   * inserted before this statement also for the order AFTER (see {@link
   * #computeLineNumberBasedOnTransition}). After the statement would be too late, because the
   * statement can already use the global variables, or declare a local variable that shadows a
   * global variable with the same name.
   */
  private void initializeEdgesWithGlobalVariables() {
    CFANode mainNode = cfa.getMainFunction();
    mainNode = getFirstNodeOfFunctionBody(mainNode);
    FileLocation location = mainNode.getLeavingEdge(0).getFileLocation();
    for (AVariableDeclaration decl : cfa.getAstCfaRelation().getGlobalVariables().orElseThrow()) {
      CFANode dummy = CFANode.newDummyCFANode();
      CFAEdge newEdge =
          new CDeclarationEdge(decl.toASTString(), location, mainNode, dummy, (CDeclaration) decl);
      mainNode.addLeavingEdge(newEdge);
      addedDeclarationEdges.add(newEdge);
    }
  }

  /**
   * The parameters of a function are not declared by an edge of the CFA, but their address can be
   * taken like the address of a local variable. Therefore, we add their declarations before the
   * first statement of the function, in the same way as for global variables at the beginning of
   * main.
   */
  private void initializeEdgesWithParameters() {
    for (FunctionEntryNode entryNode : cfa.getAllFunctions().values()) {
      CFANode firstNode = getFirstNodeOfFunctionBody(entryNode);
      if (firstNode.getNumLeavingEdges() == 0) {
        continue;
      }
      FileLocation location = firstNode.getLeavingEdge(0).getFileLocation();
      if (!location.isRealLocation()
          || location.getStartingLineInOrigin()
              <= entryNode.getFileLocation().getStartingLineInOrigin()) {
        // There is no line in the body before the first statement, e.g., for 'void f(int x) {}'
        continue;
      }
      for (AParameterDeclaration parameter : entryNode.getFunctionParameters()) {
        if (parameter instanceof CParameterDeclaration cParameterDeclaration
            && !cParameterDeclaration.getName().isEmpty()) {
          CVariableDeclaration declaration = cParameterDeclaration.asVariableDeclaration();
          CFAEdge newEdge =
              new CDeclarationEdge(
                  declaration.toASTString(),
                  location,
                  firstNode,
                  CFANode.newDummyCFANode(),
                  declaration);
          firstNode.addLeavingEdge(newEdge);
          addedDeclarationEdges.add(newEdge);
          parameterDeclarationEdges.add(newEdge);
        }
      }
    }
  }

  /**
   * The part of the program in which the code for a local variable or a parameter is inserted: a
   * block, or the header and body of a for loop for a variable declared in its header.
   *
   * @param location the location of the block or the for loop
   * @param endsWithBrace whether the last line of the location is the closing brace of a block,
   *     before which the lifetime of the variable ends
   */
  private record Scope(FileLocation location, boolean endsWithBrace) {}

  /**
   * Returns the scope of the local variable or the parameter declared on the edge. For a parameter,
   * this is the block that contains the first statement of the function, before which the parameter
   * is registered, e.g., the inner block in 'void f(int x) { { ... } }'. There is no scope for the
   * added declarations of global variables.
   */
  private Optional<Scope> getScopeOfDeclaration(CFAEdge pEdge) {
    boolean isParameter = parameterDeclarationEdges.contains(pEdge);
    // For a parameter, the location of the first statement of the function
    FileLocation declaration = pEdge.getFileLocation();
    if ((addedDeclarationEdges.contains(pEdge) && !isParameter) || !declaration.isRealLocation()) {
      return Optional.empty();
    }
    FileLocation inner = declaration;
    while (true) {
      Optional<FileLocation> enclosing =
          cfa.getAstCfaRelation().getTightestEnclosingStatement(inner);
      if (enclosing.isEmpty()) {
        return Optional.empty();
      }
      FileLocation statement = enclosing.orElseThrow();
      if (statement.getStartingLineInOrigin() < declaration.getStartingLineInOrigin()) {
        // The block that contains the declaration, its last line is the closing brace
        return Optional.of(
            new Scope(
                statement,
                statement.getEndingLineInOrigin() > declaration.getEndingLineInOrigin()));
      }
      if (!isParameter && statement.getEndingLineInOrigin() > declaration.getEndingLineInOrigin()) {
        // A statement that starts at the line of the declaration, e.g., 'for (int i = 0; ...) {'
        return Optional.of(new Scope(statement, false));
      }
      // The statement of the declaration itself, e.g., for 'b' in 'int a, b;'
      inner = statement;
    }
  }

  /**
   * Computes the new edges for the patterns that depend on the lifetime of variables:
   *
   * <ul>
   *   <li>scope_end: The lifetime of the variable declared on the edge ends before the closing
   *       brace of its block and before every return statement in its scope. The returned value can
   *       use the variable, e.g., in 'return a[0];', so then it has to be computed before the
   *       operation. For such a return statement, an additional edge 'line|||R|||declaration' gives
   *       the declaration of a variable '__return_value' with the return type of the function,
   *       which TransVer uses to compute the value first. The declaration is empty for functions
   *       that return void.
   *   <li>loop_var_use: The operation is inserted for every variable declared in a loop that the
   *       edge uses, before the innermost statement that contains the use, see {@link
   *       #initializeVariablesRegisteredInUses}.
   * </ul>
   */
  private List<String> computeEdgesForLifetimesOfVariables(
      InstrumentationTransition pTransition, CFAEdge pEdge, ImmutableList<String> pMatched) {
    List<String> result = new ArrayList<>();
    if (pTransition.getPattern().toString().equals("loop_var_use")) {
      for (Pair<CVariableDeclaration, Integer> use : usesOfVariablesRegisteredInUses.get(pEdge)) {
        String operation =
            pTransition
                .getOperation()
                .insertVariablesInsideOperation(
                    InstrumentationPattern.getNameAndLine(use.getFirst()),
                    pTransition.getPattern());
        result.add(use.getSecond() + "|||" + operation);
      }
      return result;
    }
    Optional<Scope> scope = getScopeOfDeclaration(pEdge);
    if (scope.isEmpty()) {
      return result;
    }
    String operation =
        pTransition
            .getOperation()
            .insertVariablesInsideOperation(pMatched, pTransition.getPattern());
    FileLocation scopeLocation = scope.orElseThrow().location();
    if (scope.orElseThrow().endsWithBrace()) {
      result.add(scopeLocation.getEndingLineInOrigin() + "|||" + operation);
    }
    // The return statements after the declaration, for parameters from the first statement on
    int start =
        parameterDeclarationEdges.contains(pEdge)
            ? pEdge.getFileLocation().getNodeOffset()
            : pEdge.getFileLocation().getNodeOffset() + pEdge.getFileLocation().getNodeLength();
    int end = scopeLocation.getNodeOffset() + scopeLocation.getNodeLength();
    for (CFAEdge returnEdge : getReturnEdges().get(pEdge.getPredecessor().getFunctionName())) {
      FileLocation location = returnEdge.getFileLocation();
      if (location.getNodeOffset() >= start
          && location.getNodeOffset() + location.getNodeLength() <= end) {
        int line = location.getStartingLineInOrigin();
        result.add(line + "|||" + operation);
        getDeclarationForReturnedValue(returnEdge)
            .ifPresent(declaration -> result.add(line + "|||R|||" + declaration));
      }
    }
    return result;
  }

  /** Returns the return statements with a real location, grouped by the name of the function. */
  private ImmutableListMultimap<String, CFAEdge> getReturnEdges() {
    if (returnEdges == null) {
      ImmutableListMultimap.Builder<String, CFAEdge> builder = ImmutableListMultimap.builder();
      for (CFANode node : cfa.nodes()) {
        for (CFAEdge edge : node.getLeavingEdges()) {
          if (edge.getEdgeType() == CFAEdgeType.ReturnStatementEdge
              && edge.getFileLocation().isRealLocation()) {
            builder.put(node.getFunctionName(), edge);
          }
        }
      }
      returnEdges = builder.build();
    }
    return returnEdges;
  }

  /**
   * Decides whether the value returned by the return statement on the edge has to be computed
   * before the variables of the function are removed. This is the case if the value accesses memory
   * or calls a function, which could use the variables. If so, returns the declaration of the
   * variable for the value, or an empty string if the function returns void.
   */
  private Optional<String> getDeclarationForReturnedValue(CFAEdge pReturnEdge) {
    int line = pReturnEdge.getFileLocation().getStartingLineInOrigin();
    // The parts of the statement computed by earlier edges, e.g., calls in 'return f(x);'
    boolean computedBefore =
        pReturnEdge.getPredecessor().getEnteringEdges().stream()
            .anyMatch(
                edge ->
                    !(edge instanceof BlankEdge)
                        && edge.getFileLocation().isRealLocation()
                        && edge.getFileLocation().getStartingLineInOrigin() == line);
    boolean accessesMemory =
        pReturnEdge.getRawAST().orElse(null) instanceof CReturnStatement returnStatement
            && returnStatement.getReturnValue().isPresent()
            && CFAUtils.traverseRecursively(returnStatement.getReturnValue().orElseThrow())
                .anyMatch(
                    node ->
                        node instanceof CPointerExpression
                            || node instanceof CArraySubscriptExpression
                            || (node instanceof CFieldReference fieldReference
                                && fieldReference.isPointerDereference())
                            // Function calls are replaced by temporary variables
                            || (node instanceof CIdExpression idExpression
                                && idExpression.getName().contains("__CPAchecker_TMP")));
    if (!computedBefore && !accessesMemory) {
      return Optional.empty();
    }
    if (!(pReturnEdge.getPredecessor().getFunction() instanceof CFunctionDeclaration function)) {
      return Optional.empty();
    }
    CType returnType = function.getType().getReturnType();
    return Optional.of(
        returnType.getCanonicalType() instanceof CVoidType
            ? ""
            : returnType.toASTString("__return_value"));
  }

  /**
   * A local variable declared in a loop gets a new lifetime in every iteration, but usually the
   * same address. If it was registered at its declaration, a pointer to it from an earlier
   * iteration would be valid again, e.g., in
   *
   * <pre>{@code
   * for (int i = 0; i < 2; i++) {
   *   int a[10];
   *   if (i == 0) { p = a; } else { p[0] = 1; }
   * }
   * }</pre>
   *
   * <p>A pointer to the variable can only be obtained by using the name of the variable in the
   * current iteration. Therefore, such a variable is registered only right before the statements
   * that use it, and only once per lifetime (by the operation of loop_var_use), and loop_declar
   * instead of declar or ptr_declar matches its declaration. In the example, the array is
   * registered before 'p = a' in the first iteration, but not in the second iteration, so 'p[0]' is
   * not valid. If a use cannot be assigned to a statement after the declaration, e.g., for a
   * variable declared in the header of a for loop, the variable is registered at its declaration as
   * usual.
   */
  private void initializeVariablesRegisteredInUses() {
    if (cfa.getLoopStructure().isEmpty()) {
      return;
    }
    Set<CFANode> loopNodes = new HashSet<>();
    for (Loop loop : cfa.getLoopStructure().orElseThrow().getAllLoops()) {
      loopNodes.addAll(loop.getLoopNodes());
    }
    Map<String, Pair<CVariableDeclaration, Scope>> candidates = new HashMap<>();
    for (CFANode node : loopNodes) {
      for (CFAEdge edge : node.getLeavingEdges()) {
        if (edge instanceof CDeclarationEdge declarationEdge
            && declarationEdge.getDeclaration() instanceof CVariableDeclaration declaration
            && !declaration.isGlobal()
            && declaration.getCStorageClass() == CStorageClass.AUTO
            && !declaration.getName().contains("__CPAchecker_TMP")) {
          getScopeOfDeclaration(edge)
              .ifPresent(
                  scope ->
                      candidates.put(declaration.getQualifiedName(), Pair.of(declaration, scope)));
        }
      }
    }
    Set<String> registeredAtDeclaration = new HashSet<>();
    ListMultimap<CFAEdge, Pair<CVariableDeclaration, Integer>> uses = ArrayListMultimap.create();
    for (CFANode node : cfa.nodes()) {
      for (CFAEdge edge : node.getLeavingEdges()) {
        for (AAstNode astNode : CFAUtils.getAstNodesFromCfaEdge(edge)) {
          for (CIdExpression idExpression :
              CFAUtils.traverseRecursively(astNode).filter(CIdExpression.class)) {
            if (idExpression.getDeclaration() == null) {
              continue;
            }
            Pair<CVariableDeclaration, Scope> candidate =
                candidates.get(idExpression.getDeclaration().getQualifiedName());
            if (candidate == null) {
              continue;
            }
            CVariableDeclaration declaration = candidate.getFirst();
            Optional<Integer> line =
                getLineBeforeUse(edge.getFileLocation(), candidate.getSecond());
            if (line.isEmpty()
                || line.orElseThrow() <= declaration.getFileLocation().getEndingLineInOrigin()) {
              registeredAtDeclaration.add(declaration.getQualifiedName());
            } else if (!uses.containsEntry(edge, Pair.of(declaration, line.orElseThrow()))) {
              uses.put(edge, Pair.of(declaration, line.orElseThrow()));
            }
          }
        }
      }
    }
    for (Pair<CVariableDeclaration, Scope> candidate : candidates.values()) {
      if (!registeredAtDeclaration.contains(candidate.getFirst().getQualifiedName())) {
        variablesRegisteredInUses.add(candidate.getFirst().getQualifiedName());
      }
    }
    for (Map.Entry<CFAEdge, Pair<CVariableDeclaration, Integer>> use : uses.entries()) {
      if (variablesRegisteredInUses.contains(use.getValue().getFirst().getQualifiedName())) {
        usesOfVariablesRegisteredInUses.put(use.getKey(), use.getValue());
      }
    }
  }

  /**
   * Returns the line before which code is executed right before the use of a variable at the
   * location. This is the starting line of the innermost statement in the scope of the variable
   * that contains the use, e.g., of an if statement for a use in its condition. If another
   * statement ends on this line, e.g., in '} else if (a[0]) {', the enclosing statement is used
   * instead. Returns an empty Optional if there is no such statement in the scope.
   */
  private Optional<Integer> getLineBeforeUse(FileLocation pLocation, Scope pScope) {
    if (!pLocation.isRealLocation()) {
      return Optional.empty();
    }
    AstCfaRelation astCfaRelation = cfa.getAstCfaRelation();
    FileLocation scope = pScope.location();
    Optional<FileLocation> statement = astCfaRelation.getTightestStatementContaining(pLocation);
    while (statement.isPresent() && !statement.orElseThrow().equals(scope)) {
      FileLocation current = statement.orElseThrow();
      if (current.getNodeOffset() <= scope.getNodeOffset()
          || current.getNodeOffset() + current.getNodeLength()
              > scope.getNodeOffset() + scope.getNodeLength()) {
        // The location is not inside the scope
        return Optional.empty();
      }
      Optional<FileLocation> enclosing = astCfaRelation.getTightestEnclosingStatement(current);
      if (enclosing.isEmpty()) {
        return Optional.empty();
      }
      if (!astCfaRelation.startsOnLineOfPreviousStatement(current, enclosing.orElseThrow())) {
        return Optional.of(current.getStartingLineInOrigin());
      }
      statement = enclosing;
    }
    // The use is in the scope itself, e.g., in the header of a for loop
    return Optional.empty();
  }

  /** Decides whether the edge declares a variable that is registered before its uses. */
  private boolean isRegisteredInUses(CFAEdge pEdge) {
    return pEdge instanceof CDeclarationEdge declarationEdge
        && !addedDeclarationEdges.contains(pEdge)
        && variablesRegisteredInUses.contains(declarationEdge.getDeclaration().getQualifiedName());
  }

  /**
   * Returns the first node of the body of the function with the given entry node, i.e., the node
   * after the 'Function start' edge. For main, the declarations of global variables come before
   * this edge.
   */
  private CFANode getFirstNodeOfFunctionBody(CFANode pCFANode) {
    CFAEdge currentEdge = pCFANode.getLeavingEdge(0);
    while (!currentEdge.getDescription().contains("Function start")) {
      currentEdge = currentEdge.getSuccessor().getLeavingEdge(0);
    }
    return currentEdge.getSuccessor();
  }

  private void addEdgesAtTheLoopEnd(
      CFANode loopHead,
      InstrumentationTransition transition,
      ImmutableList<String> matchedVariables,
      List<String> newEdges) {
    for (CFAEdge incomingEdgeToTheLoop : loopHead.getAllEnteringEdges()) {
      String newEdge2;
      if (!incomingEdgeToTheLoop.getFileLocation().isRealLocation()) {
        newEdge2 =
            (Integer.parseInt(
                        computeLineNumberBasedOnTransition(
                            transition,
                            incomingEdgeToTheLoop.getPredecessor().getEnteringEdge(0),
                            matchedVariables))
                    + 1)
                + "|||"
                + transition
                    .getOperation()
                    .insertVariablesInsideOperation(matchedVariables, transition.getPattern());
      } else {
        newEdge2 =
            computeLineNumberBasedOnTransition(transition, incomingEdgeToTheLoop, matchedVariables)
                + "|||"
                + transition
                    .getOperation()
                    .insertVariablesInsideOperation(matchedVariables, transition.getPattern());
      }
      if (!newEdges.contains(newEdge2)) {
        newEdges.add(newEdge2);
      }
    }
  }

  private ImmutableMap<String, String> getVariablesWithTypesFromFullCFA() {
    ImmutableMap.Builder<String, String> builder = new ImmutableMap.Builder<>();
    for (String variable :
        cfa.getMetadata().getLiveVariables().orElseThrow().getAllLiveVariables().stream()
            .map(e -> e.getName())
            .toList()) {
      if (cProgramScope.variableNameInUse(variable)) {
        try {
          builder.put(variable, cProgramScope.lookupVariable(variable).getType().toString());
        } catch (Exception e) {
          logger.log(Level.WARNING, "There is a skipped variable without known type.");
        }
      }
    }
    return builder.buildOrThrow();
  }

  // Some of the statements in the loop heads are decomposed into multiple edges in CFA.
  // For example, a > b && b == a + 1 is divided into multiple edges.
  // We want to find the initial node representing the loop to find out that it is a loop.
  // This way, it is correct because TransVer formats the program to have all the statements
  // on one line.
  private CFANode getLoopHeadOfEdge(CFANode pCurrentNode, CFAEdge pCurrentEdge) {
    while (true) {
      if (pCurrentNode.isLoopStart() || pCurrentNode.getAllEnteringEdges().size() != 1) {
        return pCurrentNode;
      }
      CFAEdge prevEdge = pCurrentNode.getEnteringEdge(0);
      if (prevEdge.getFileLocation().getStartingLineNumber()
          != pCurrentEdge.getFileLocation().getStartingLineNumber()) {
        return pCurrentNode;
      }
      pCurrentEdge = prevEdge;
      pCurrentNode = pCurrentEdge.getPredecessor();
    }
  }

  /* Decomposes a CFAEdge with assignment statement. */
  /**
   * Adds an edge for every expression of the initializer of a declaration, so that the expressions
   * are matched in the same way as the right-hand side of an assignment.
   */
  private void decomposeInitializer(
      CFAEdge pCFAEdge,
      InstrumentationTransition pTransition,
      List<Pair<CFANode, InstrumentationState>> pWaitlist,
      Map<CFANode, String> pDecomposedMap,
      ImmutableList<String> pMatchedVariables,
      @Nullable CInitializer pInitializer) {
    if (pInitializer instanceof CInitializerExpression pCInitializerExpression) {
      decomposeExpression(
          pCFAEdge,
          pTransition,
          pWaitlist,
          pDecomposedMap,
          pMatchedVariables,
          pCInitializerExpression.getExpression());
    } else if (pInitializer instanceof CInitializerList pCInitializerList) {
      for (CInitializer initializer : pCInitializerList.getInitializers()) {
        decomposeInitializer(
            pCFAEdge, pTransition, pWaitlist, pDecomposedMap, pMatchedVariables, initializer);
      }
    } else if (pInitializer instanceof CDesignatedInitializer pCDesignatedInitializer) {
      decomposeInitializer(
          pCFAEdge,
          pTransition,
          pWaitlist,
          pDecomposedMap,
          pMatchedVariables,
          pCDesignatedInitializer.getRightHandSide());
    }
  }

  /**
   * Adds an edge for the expression, e.g., of an initializer or a return statement, so that it is
   * matched in the same way as the right-hand side of an assignment.
   */
  private void decomposeExpression(
      CFAEdge pCFAEdge,
      InstrumentationTransition pTransition,
      List<Pair<CFANode, InstrumentationState>> pWaitlist,
      Map<CFANode, String> pDecomposedMap,
      ImmutableList<String> pMatchedVariables,
      CExpression pExpression) {
    String condition = pMatchedVariables.size() != 5 ? "1" : pMatchedVariables.get(2);
    CFANode node = CFANode.newDummyCFANode();
    node.addLeavingEdge(
        new CStatementEdge(
            pExpression.toASTString(),
            new CExpressionStatement(pCFAEdge.getFileLocation(), pExpression),
            pCFAEdge.getFileLocation(),
            node,
            pCFAEdge.getSuccessor()));
    pDecomposedMap.put(node, condition);
    pWaitlist.add(Pair.of(node, pTransition.getSource()));
  }

  private void decomposeAssignmentStatement(
      CFAEdge pCFAEdge,
      InstrumentationTransition pTransition,
      List<Pair<CFANode, InstrumentationState>> pWaitlist,
      Map<CFANode, String> pDecomposedMap,
      ImmutableList<String> pMatchedVariables,
      CExpressionAssignmentStatement pAssignmentStatement) {
    String condition = pMatchedVariables.size() != 5 ? "1" : pMatchedVariables.get(2);
    CExpression leftSide = pAssignmentStatement.getLeftHandSide();
    CExpression rightSide = pAssignmentStatement.getRightHandSide();
    CFANode leftNode = CFANode.newDummyCFANode();
    CFANode rightNode = CFANode.newDummyCFANode();

    leftNode.addLeavingEdge(
        new CStatementEdge(
            leftSide.toASTString(),
            new CExpressionStatement(pCFAEdge.getFileLocation(), leftSide),
            pCFAEdge.getFileLocation(),
            leftNode,
            pCFAEdge.getSuccessor()));
    pDecomposedMap.put(leftNode, condition);
    pWaitlist.add(Pair.of(leftNode, pTransition.getSource()));

    rightNode.addLeavingEdge(
        new CStatementEdge(
            rightSide.toASTString(),
            new CExpressionStatement(pCFAEdge.getFileLocation(), rightSide),
            pCFAEdge.getFileLocation(),
            rightNode,
            pCFAEdge.getSuccessor()));
    pDecomposedMap.put(rightNode, condition);
    pWaitlist.add(Pair.of(rightNode, pTransition.getSource()));
  }

  /* Decomposes a CFAEdge with function call. */
  private void decomposeFunction(
      CFAEdge pCFAEdge,
      InstrumentationTransition pTransition,
      List<Pair<CFANode, InstrumentationState>> pWaitlist,
      Map<CFANode, String> pDecomposedMap,
      ImmutableList<String> pMatchedVariables,
      AAstNode pFunctionCallStatement) {
    String condition = pMatchedVariables.size() != 5 ? "1" : pMatchedVariables.get(2);
    List<CExpression> parameters;
    if (pFunctionCallStatement instanceof CFunctionCallStatement pCFunctionCallStatement) {
      parameters = pCFunctionCallStatement.getFunctionCallExpression().getParameterExpressions();
    } else {
      parameters =
          ((CFunctionCallAssignmentStatement) pFunctionCallStatement)
              .getFunctionCallExpression()
              .getParameterExpressions();
    }
    if (parameters.isEmpty()) {
      return;
    }
    for (CExpression parameter : parameters) {
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

  /**
   * Returns the entry nodes of the functions that the leaving edges of the node start as threads,
   * e.g., of {@code t_fun} for {@code pthread_create(&id, NULL, t_fun, NULL)}. The CFA has no edges
   * into these functions, because pthread_create is an external function.
   */
  private ImmutableSet<CFANode> getEntriesOfStartedThreads(CFANode pCFANode) {
    ImmutableSet.Builder<CFANode> threadEntries = ImmutableSet.builder();
    for (CFAEdge edge : pCFANode.getLeavingEdges()) {
      if (edge.getRawAST().isPresent()
          && edge.getRawAST().orElseThrow() instanceof CFunctionCall cFunctionCall
          && cFunctionCall.getFunctionCallExpression().getFunctionNameExpression()
              instanceof CIdExpression cFunctionName
          && cFunctionName.getName().equals(ThreadingTransferRelation.THREAD_START)) {
        List<CExpression> parameters =
            cFunctionCall.getFunctionCallExpression().getParameterExpressions();
        if (parameters.size() < 3) {
          continue;
        }
        // The start routine is the third parameter, usually '&t_fun' in the CFA
        CExpression startRoutine = parameters.get(2);
        while (startRoutine instanceof CCastExpression cCastExpression) {
          startRoutine = cCastExpression.getOperand();
        }
        if (startRoutine instanceof CUnaryExpression cUnaryExpression
            && cUnaryExpression.getOperator() == UnaryOperator.AMPER) {
          startRoutine = cUnaryExpression.getOperand();
        }
        if (startRoutine instanceof CIdExpression cIdExpression) {
          FunctionEntryNode threadEntry = cfa.getFunctionHead(cIdExpression.getName());
          if (threadEntry != null) {
            threadEntries.add(threadEntry);
          }
        }
      }
    }
    return threadEntries.build();
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
