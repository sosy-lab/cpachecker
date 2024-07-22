// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.instrumentation;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

/**
 * This algorithm extracts loop information from a C program.
 *
 * <p>There are two kinds of loops, which are classified not by their syntax but by their
 * instrumentation processes. The first is normal loops(for, while, do-while, and goto loops). The
 * second is recursion.
 */
public class LocateLoopAndLiveVariableAlgorithm implements Algorithm {
  private final CFA cfa;
  private final LogManager logger;
  private final CProgramScope cProgramScope;

  public LocateLoopAndLiveVariableAlgorithm(CFA pCfa, LogManager pLogger) {
    if (pCfa.getLoopStructure().orElseThrow().getAllLoops().isEmpty()
        && LoopStructure.getRecursions(pCfa).isEmpty()) {
      throw new IllegalArgumentException("Program does not contain loops!");
    }
    cfa = pCfa;
    logger = pLogger;
    cProgramScope = new CProgramScope(pCfa, pLogger);
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    // Output the collected loop information to a file
    try (BufferedWriter writer =
        Files.newBufferedWriter(Paths.get("output/AllLoopInfos.txt"), StandardCharsets.UTF_8)) {
      StringBuilder allLoopInfos = new StringBuilder();

      for (NormalLoopInfo loopInfo : getAllNormalLoopInfos()) {
        allLoopInfos.append(
            String.format(
                "NormalLoop    %d    %s%n",
                loopInfo.loopLocation(), loopInfo.liveVariablesAndTypes()));
      }

      for (RecursionInfo recursionInfo : getAllRecursionInfos()) {
        allLoopInfos.append(
            String.format(
                "Recursion    %s    %d    %s    %s%n",
                recursionInfo.functionName(),
                recursionInfo.locationOfDefinition(),
                recursionInfo.locationOfRecursiveCalls(),
                recursionInfo.parameters()));
      }

      writer.write(allLoopInfos.toString());
    } catch (IOException e) {
      logger.logException(Level.SEVERE, e, "The creation of file AllLoopInfos.txt failed!");
    }
    return AlgorithmStatus.NO_PROPERTY_CHECKED;
  }

  private ImmutableSet<NormalLoopInfo> getAllNormalLoopInfos() {
    Set<NormalLoopInfo> allNormalLoopInfos = new HashSet<>();

    for (Loop loop : cfa.getLoopStructure().orElseThrow().getAllLoops()) {
      // Determine loop locations. There may be more than one, as some loops have multiple
      // loop heads, e.g., goto loop.
      List<Integer> loopLocations = new ArrayList<>();
      for (CFANode cfaNode : loop.getLoopHeads()) {
        loopLocations.add(
            CFAUtils.allEnteringEdges(cfaNode)
                .first()
                .get()
                .getFileLocation()
                .getStartingLineInOrigin());
      }

      // Determine the names of all variables used except those declared inside the loop
      Set<String> liveVariables = new HashSet<>();
      Set<String> variablesDeclaredInsideLoop = new HashSet<>();
      Map<String, String> liveVariablesAndTypes = new HashMap<>();
      for (CFAEdge cfaEdge : loop.getInnerLoopEdges()) {
        if (cfaEdge.getRawAST().isPresent()) {
          AAstNode aAstNode = cfaEdge.getRawAST().orElseThrow();
          if (aAstNode instanceof CSimpleDeclaration) {
            variablesDeclaredInsideLoop.add(((CSimpleDeclaration) aAstNode).getQualifiedName());
          } else {
            liveVariables.addAll(getVariablesFromAAstNode(cfaEdge.getRawAST().orElseThrow()));
          }
        }
      }
      liveVariables.removeAll(variablesDeclaredInsideLoop);
      liveVariables.removeIf(
          e ->
              e.contains("::")
                  && Iterables.get(Splitter.on("::").split(e), 1).startsWith("__CPAchecker_TMP_"));

      // Determine type of each variable
      for (String variable : liveVariables) {
        String type = cProgramScope.lookupVariable(variable).getType().toString();
        liveVariablesAndTypes.put(
            variable.contains("::")
                ? Iterables.get(Splitter.on("::").split(variable), 1)
                : variable,
            type.startsWith("(") ? type.substring(1, type.length() - 2) + "*" : type);
      }

      for (Integer loopLocation : loopLocations) {
        allNormalLoopInfos.add(
            new NormalLoopInfo(loopLocation, ImmutableMap.copyOf(liveVariablesAndTypes)));
      }
    }

    return ImmutableSet.copyOf(allNormalLoopInfos);
  }

  private ImmutableSet<String> getVariablesFromAAstNode(AAstNode pAAstNode) {
    Set<String> variables = new HashSet<>();

    if (pAAstNode instanceof CExpression) {
      CFAUtils.getVariableNamesOfExpression(((CExpression) pAAstNode))
          .forEach(e -> variables.add(e));

    } else if (pAAstNode instanceof CExpressionStatement) {
      CExpression cExpression = ((CExpressionStatement) pAAstNode).getExpression();
      CFAUtils.getVariableNamesOfExpression(cExpression).forEach(e -> variables.add(e));

    } else if (pAAstNode instanceof CExpressionAssignmentStatement) {
      CLeftHandSide cLeftHandSide = ((CExpressionAssignmentStatement) pAAstNode).getLeftHandSide();
      CFAUtils.getVariableNamesOfExpression(cLeftHandSide).forEach(e -> variables.add(e));

      CExpression cRightHandSide = ((CExpressionAssignmentStatement) pAAstNode).getRightHandSide();
      CFAUtils.getVariableNamesOfExpression(cRightHandSide).forEach(e -> variables.add(e));

    } else if (pAAstNode instanceof CFunctionCallStatement) {
      CFunctionCallStatement cFunctionCallStatement = (CFunctionCallStatement) pAAstNode;
      cFunctionCallStatement
          .getFunctionCallExpression()
          .getParameterExpressions()
          .forEach(e -> CFAUtils.getVariableNamesOfExpression(e).forEach(n -> variables.add(n)));

    } else if (pAAstNode instanceof CFunctionCallAssignmentStatement) {
      CFunctionCallAssignmentStatement cFunctionCallAssignmentStatement =
          (CFunctionCallAssignmentStatement) pAAstNode;

      CLeftHandSide cLeftHandSide = cFunctionCallAssignmentStatement.getLeftHandSide();
      CFAUtils.getVariableNamesOfExpression(cLeftHandSide).forEach(e -> variables.add(e));

      CFunctionCallExpression cRightHandSide = cFunctionCallAssignmentStatement.getRightHandSide();
      cRightHandSide
          .getParameterExpressions()
          .forEach(e -> CFAUtils.getVariableNamesOfExpression(e).forEach(n -> variables.add(n)));
    }

    return ImmutableSet.copyOf(variables);
  }

  private ImmutableSet<RecursionInfo> getAllRecursionInfos() {
    Set<RecursionInfo> allRecursionInfos = new HashSet<>();

    for (Loop recursion : LoopStructure.getRecursions(cfa)) {
      FunctionEntryNode function =
          getFunctionFromFunctionCallEdge(
              recursion.getIncomingEdges().stream().findFirst().orElseThrow());

      String functionName = function.getFunctionName();
      int locationOfDefinition;
      Set<Integer> locationOfRecursiveCalls = new HashSet<>();
      List<String> parameters = new ArrayList<>();

      // Determine location Of definition
      locationOfDefinition = function.getFileLocation().getStartingLineInOrigin();

      // Determine location of recursive calls
      for (CFAEdge cfaEdge : recursion.getInnerLoopEdges()) {
        if (cfaEdge.getDescription().startsWith(functionName + "(")) {
          locationOfRecursiveCalls.add(cfaEdge.getFileLocation().getStartingLineInOrigin());
        }
      }

      // Determine parameters
      function.getFunctionParameters().forEach(e -> parameters.add(e.toString()));

      allRecursionInfos.add(
          new RecursionInfo(
              functionName,
              locationOfDefinition,
              ImmutableSet.copyOf(locationOfRecursiveCalls),
              ImmutableList.copyOf(parameters)));

      // Check if the recursion is a mutual recursion.
      // If so, add the information of another function(s).
      Set<FunctionEntryNode> otherFuntionsCalledInCurrentRecursion = new HashSet<>();
      for (CFAEdge cfaEdge : recursion.getInnerLoopEdges()) {
        if (cfaEdge.getEdgeType() == CFAEdgeType.FunctionCallEdge
            && !getFunctionFromFunctionCallEdge(cfaEdge).equals(function)) {
          otherFuntionsCalledInCurrentRecursion.add(getFunctionFromFunctionCallEdge(cfaEdge));
        }
      }
      for (FunctionEntryNode otherFuntion : otherFuntionsCalledInCurrentRecursion) {
        int startLine = otherFuntion.getFileLocation().getStartingLineInOrigin();
        int endLine = otherFuntion.getFileLocation().getEndingLineInOrigin();
        Set<Integer> locationOfRecursiveCallsOfOtherFunction = new HashSet<>();

        if (recursion.getInnerLoopEdges().stream()
            .anyMatch(
                e ->
                    e.getFileLocation().getStartingLineInOrigin() >= startLine
                        && e.getFileLocation().getStartingLineInOrigin() <= endLine
                        && e.getDescription().startsWith(functionName + "("))) {
          for (CFAEdge cfaEdge : recursion.getInnerLoopEdges()) {
            if (cfaEdge.getDescription().startsWith(otherFuntion.getFunctionName() + "(")) {
              locationOfRecursiveCallsOfOtherFunction.add(
                  cfaEdge.getFileLocation().getStartingLineInOrigin());
            }
          }

          allRecursionInfos.add(
              new RecursionInfo(
                  "*" + otherFuntion.getFunctionName(),
                  startLine,
                  ImmutableSet.copyOf(locationOfRecursiveCallsOfOtherFunction),
                  ImmutableList.copyOf(parameters)));
        }
      }
    }

    return ImmutableSet.copyOf(allRecursionInfos);
  }

  private FunctionEntryNode getFunctionFromFunctionCallEdge(CFAEdge pCfaEdge) {
    if (pCfaEdge.getEdgeType() != CFAEdgeType.FunctionCallEdge) {
      throw new IllegalArgumentException(
          "The type of the given CFA edge must be \"FunctionCallEdge\"");
    }

    AAstNode astNode = pCfaEdge.getRawAST().orElseThrow();
    if (astNode instanceof CFunctionCallStatement) {
      CFunctionCallStatement cFunctionCallStatement = (CFunctionCallStatement) astNode;
      return cfa.getAllFunctions()
          .get(
              cFunctionCallStatement
                  .getFunctionCallExpression()
                  .getFunctionNameExpression()
                  .toString());
    } else if (astNode instanceof CFunctionCallAssignmentStatement) {
      CFunctionCallAssignmentStatement cFunctionCallAssignmentStatement =
          (CFunctionCallAssignmentStatement) astNode;
      CFunctionCallExpression cRightHandSide = cFunctionCallAssignmentStatement.getRightHandSide();
      return cfa.getAllFunctions().get(cRightHandSide.getFunctionNameExpression().toString());
    } else {
      throw new Error("An unexpected error occurred!");
    }
  }
}

/**
 * Represents a container for normal loop information(for, while, do-while, and goto loop).
 *
 * @param loopLocation the line number where the loop is located
 * @param liveVariablesAndTypes the mapping from variable names used, but not declared, in the loop
 *     to their types
 */
record NormalLoopInfo(int loopLocation, ImmutableMap<String, String> liveVariablesAndTypes) {}

/**
 * Represents a container for recursion information.
 *
 * @param functionName the name of the function
 * @param locationOfDefinition the line number where the function is defined
 * @param locationOfRecursiveCalls a set of line numbers where the recursive calls occur
 * @param parameters the function's parameters(type + name)
 */
record RecursionInfo(
    String functionName,
    int locationOfDefinition,
    ImmutableSet<Integer> locationOfRecursiveCalls,
    ImmutableList<String> parameters) {}
