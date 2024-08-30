// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.instrumentation;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
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
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
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
      ImmutableSet<StructInfo> allLiveStructInfos =
          getAllLiveStructInfos(allNormalLoopInfos, allRecursionInfos);

      StringBuilder allLoopInfos = new StringBuilder();
      for (StructInfo structInfo : allLiveStructInfos) {
        allLoopInfos.append(
            String.format("Struct    %s    %s%n", structInfo.structName(), structInfo.members()));
      }
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

  private ImmutableSet<StructInfo> getAllLiveStructInfos(
      ImmutableSet<NormalLoopInfo> pAllNormalLoopInfos,
      ImmutableSet<RecursionInfo> pAllRecursionInfos) {

    Set<StructInfo> allLiveStructInfos = new HashSet<>();
    ImmutableSet<StructInfo> allStructInfos = getAllStructInfos();
    ImmutableSet<String> allLiveStructNames =
        getAllLiveStructNames(pAllNormalLoopInfos, pAllRecursionInfos);

    for (StructInfo structInfo : allStructInfos) {
      if (allLiveStructNames.contains(structInfo.structName())) {
        allLiveStructInfos.add(
            new StructInfo(
                structInfo.structName(), resolveStructsIn(structInfo.members(), allStructInfos)));
      }
    }

    return ImmutableSet.copyOf(allLiveStructInfos);
  }

  private ImmutableSet<String> getAllLiveStructNames(
      ImmutableSet<NormalLoopInfo> pAllNormalLoopInfos,
      ImmutableSet<RecursionInfo> pAllRecursionInfos) {

    Set<String> allLivestructNames = new HashSet<>();

    for (NormalLoopInfo normalLoopInfo : pAllNormalLoopInfos) {
      normalLoopInfo.liveVariablesAndTypes().entrySet().stream()
          .filter(e -> e.getValue().startsWith("struct "))
          .forEach(
              e -> allLivestructNames.add(Iterables.get(Splitter.on(' ').split(e.getValue()), 1)));
    }

    for (RecursionInfo recursionInfo : pAllRecursionInfos) {
      recursionInfo.parameters().stream()
          .filter(e -> e.startsWith("struct "))
          .forEach(e -> allLivestructNames.add(Iterables.get(Splitter.on(' ').split(e), 1)));
    }

    return ImmutableSet.copyOf(allLivestructNames);
  }

  private ImmutableSet<StructInfo> getAllStructInfos() {
    Set<StructInfo> allStructInfos = new HashSet<>();

    for (CFAEdge cfaEdge : cfa.edges()) {
      Optional<AAstNode> aAstNodeOp = cfaEdge.getRawAST();
      if (aAstNodeOp.isPresent() && aAstNodeOp.orElseThrow() instanceof CComplexTypeDeclaration) {
        String cComplexTypeDeclaration =
            ((CComplexTypeDeclaration) aAstNodeOp.orElseThrow()).toString();

        if (cComplexTypeDeclaration.startsWith(
            "struct ")) { // A C complex type can also be an enum by definition in CPAchecker
          String structName;
          Map<String, String> members = new HashMap<>();

          // Every string representation of an AST node for a struct declaration has the same
          // format, which the following modification is based on.
          cComplexTypeDeclaration =
              cComplexTypeDeclaration
                  .substring(0, cComplexTypeDeclaration.length() - 4)
                  .replace(";", "")
                  .replace("  ", "");
          List<String> structParts = Splitter.on('\n').splitToList(cComplexTypeDeclaration);
          structName = Iterables.get(Splitter.on(' ').split(structParts.get(0)), 1);
          for (int i = 1; i < structParts.size(); i++) {
            if (structParts.get(i).startsWith("struct ")) {
              members.put(
                  Iterables.get(Splitter.on(' ').split(structParts.get(i)), 2),
                  Iterables.get(Splitter.on(' ').split(structParts.get(i)), 0)
                      + " "
                      + Iterables.get(Splitter.on(' ').split(structParts.get(i)), 1));
            } else {
              members.put(
                  Iterables.get(Splitter.on(' ').split(structParts.get(i)), 1),
                  Iterables.get(Splitter.on(' ').split(structParts.get(i)), 0));
            }
          }

          allStructInfos.add(new StructInfo(structName, ImmutableMap.copyOf(members)));
        }
      }
    }

    return ImmutableSet.copyOf(allStructInfos);
  }

  private ImmutableMap<String, String> resolveStructsIn(
      ImmutableMap<String, String> pMembers, ImmutableSet<StructInfo> allStructInfos) {
    Map<String, String> ans = new HashMap<>();

    for (Entry<String, String> member : pMembers.entrySet()) {
      String name = member.getKey();
      String type = member.getValue();
      ans.put(name, type);

      if (type.startsWith("struct ")) {
        ans.remove(name);

        ImmutableMap<String, String> originalMembersUnderOneLevel =
            allStructInfos.stream()
                .filter(e -> type.endsWith(e.structName()))
                .findFirst()
                .orElseThrow()
                .members();
        Map<String, String> modifiedMembersUnderOneLevel = new HashMap<>();

        for (Entry<String, String> memberUnderOneLevel : originalMembersUnderOneLevel.entrySet()) {
          String nameUnderOneLevel = memberUnderOneLevel.getKey();
          String typeUnderOneLevel = memberUnderOneLevel.getValue();

          String nameUnderOneLevelWithoutAsterisk = nameUnderOneLevel.replace("*", "");
          int countOfAsterisk =
              nameUnderOneLevel.length() - nameUnderOneLevelWithoutAsterisk.length();

          modifiedMembersUnderOneLevel.put(
              "*".repeat(countOfAsterisk) + name + "." + nameUnderOneLevelWithoutAsterisk,
              typeUnderOneLevel);
        }

        ans.putAll(
            resolveStructsIn(ImmutableMap.copyOf(modifiedMembersUnderOneLevel), allStructInfos));
      }
    }

    return ImmutableMap.copyOf(ans);
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

/**
 * Represents a container for struct information.
 *
 * @param structName the name of the struct
 * @param members a map of member names to their types
 */
record StructInfo(String structName, ImmutableMap<String, String> members) {}
