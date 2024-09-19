// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.instrumentation;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
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
    cfa = pCfa;
    logger = pLogger;
    cProgramScope = new CProgramScope(pCfa, pLogger);
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    // Output the collected loop information to a file
    try (BufferedWriter writer =
        Files.newBufferedWriter(
            new File("output/AllLoopInfos.txt").toPath(), StandardCharsets.UTF_8)) {
      ImmutableSet<NormalLoopInfo> allNormalLoopInfos =
          LoopInfoUtils.getAllNormalLoopInfos(cfa, cProgramScope);
      ImmutableSet<RecursionInfo> allRecursionInfos = getAllRecursionInfos();

      StringBuilder allLoopInfos = new StringBuilder();
      for (NormalLoopInfo loopInfo : allNormalLoopInfos) {
        allLoopInfos.append(
            String.format(
                "NormalLoop    %d    %s%n",
                loopInfo.loopLocation(), loopInfo.liveVariablesAndTypes()));
      }
      for (RecursionInfo recursionInfo : allRecursionInfos) {
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
    checkArgument(
        pCfaEdge.getEdgeType() == CFAEdgeType.FunctionCallEdge,
        "The type of the given CFA edge must be \"FunctionCallEdge\"");

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
