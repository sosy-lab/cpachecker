// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.instrumentation;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.ARightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

public class LoopInfoUtils {

  public static ImmutableSet<NormalLoopInfo> getAllNormalLoopInfos(
      CFA pCfa, CProgramScope pCProgramScope) {
    Set<NormalLoopInfo> allNormalLoopInfos = new HashSet<>();
    ImmutableMap<String, ImmutableMap<String, String>> allStructInfos = getAllStructInfos(pCfa);
    ImmutableSet<String> allGlobalVariables = getAllGlobalVariables(pCfa);

    for (Loop loop : pCfa.getLoopStructure().orElseThrow().getAllLoops()) {
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
      Map<String, String> liveVariablesAndTypes = new HashMap<>();
      for (CFAEdge cfaEdge : loop.getInnerLoopEdges()) {
        if (cfaEdge.getRawAST().isPresent()) {
          liveVariables.addAll(getVariablesFromAAstNode(cfaEdge.getRawAST().orElseThrow()));
        }
      }
      liveVariables.removeIf(
          e ->
              e.contains("::")
                  && Iterables.get(Splitter.on("::").split(e), 1).startsWith("__CPAchecker_TMP_"));
      liveVariables.addAll(allGlobalVariables);

      // Determine type of each variable
      for (String variable : liveVariables) {
        String type =
            pCProgramScope
                .lookupVariable(variable)
                .getType()
                .toString()
                .replace("(", "")
                .replace(")", "");
        int countOfAsterisk = type.length() - type.replace("*", "").length();
        type = type.replace("*", "");
        variable =
            "*".repeat(countOfAsterisk)
                + (variable.contains("::")
                    ? Iterables.get(Splitter.on("::").split(variable), 1)
                    : variable);

        if (type.startsWith("struct ")) {
          liveVariablesAndTypes.putAll(
              resolveStructIn(variable, allStructInfos.get(type), allStructInfos));
        } else {
          liveVariablesAndTypes.put(variable, type);
        }
      }

      for (Integer loopLocation : loopLocations) {
        allNormalLoopInfos.add(
            new NormalLoopInfo(loopLocation, loop, ImmutableMap.copyOf(liveVariablesAndTypes)));
      }
    }

    return ImmutableSet.copyOf(includeAllTheOuterLiveVariablesInNestedLoop(allNormalLoopInfos));
  }

  public static Map<CFANode, Integer> getMapOfLoopHeadsToLineNumbers(CFA pCfa) {
    Map<CFANode, Integer> mapLoopHeadToLineNumbers = new HashMap<>();

    for (Loop loop : pCfa.getLoopStructure().orElseThrow().getAllLoops()) {
      // Determine loop locations. There may be more than one, as some loops have multiple
      // loop heads, e.g., goto loop.
      for (CFANode cfaNode : loop.getLoopHeads()) {
        mapLoopHeadToLineNumbers.put(
            cfaNode,
            CFAUtils.allEnteringEdges(cfaNode)
                .first()
                .get()
                .getFileLocation()
                .getStartingLineInOrigin());
      }
    }
    return mapLoopHeadToLineNumbers;
  }

  @Nullable
  public static CExpression extractExpression(AAstNode pAAstNode) {
    if (pAAstNode instanceof CReturnStatement) {
      // return statement
      Optional<CExpression> optionalCExpression = ((CReturnStatement) pAAstNode).getReturnValue();
      if (optionalCExpression.isPresent()) {
        CExpression cExpression = optionalCExpression.orElseThrow();
        if (cExpression instanceof CBinaryExpression) {
          return cExpression;
        } else if (cExpression instanceof CUnaryExpression) {
          return cExpression;
        }
      }
    } else if (pAAstNode instanceof CAssignment) {
      // assignment
      ARightHandSide rightHandSide = ((CAssignment) pAAstNode).getRightHandSide();
      if (rightHandSide instanceof CFunctionCallExpression) {
        // function call expression
        List<CExpression> parameterExpressions =
            ((CFunctionCallExpression) rightHandSide).getParameterExpressions();
        for (CExpression expression : parameterExpressions) {
          if (expression instanceof CBinaryExpression) {
            return expression;
          }
          if (expression instanceof CUnaryExpression) {
            return expression;
          }
        }
      } else if (rightHandSide instanceof CBinaryExpression) {
        return (CBinaryExpression) rightHandSide;
      } else if (rightHandSide instanceof CUnaryExpression) {
        return (CUnaryExpression) rightHandSide;
      }
    } else if (pAAstNode instanceof CBinaryExpression) {
      // binary expression
      return (CBinaryExpression) pAAstNode;
    } else if (pAAstNode instanceof CExpressionStatement) {
      // binary expression
      return ((CExpressionStatement) pAAstNode).getExpression();
    } else if (pAAstNode instanceof CFunctionCall) {
      // function call
      CFunctionCallExpression cFunctionCallExpression =
          ((CFunctionCall) pAAstNode).getFunctionCallExpression();
      List<CExpression> parameterExpressions = cFunctionCallExpression.getParameterExpressions();
      for (CExpression expression : parameterExpressions) {
        if (expression instanceof CBinaryExpression) {
          return expression;
        }
        if (expression instanceof CUnaryExpression) {
          return expression;
        }
      }
    } else if (pAAstNode instanceof CVariableDeclaration) {
      // variable declaration
      CInitializer cInitializer = ((CVariableDeclaration) pAAstNode).getInitializer();
      if (cInitializer instanceof CInitializerExpression) {
        CExpression cExpression = ((CInitializerExpression) cInitializer).getExpression();
        if (cExpression instanceof CBinaryExpression) {
          return cExpression;
        }
      }
    }
    return null;
  }

  private static Set<NormalLoopInfo> includeAllTheOuterLiveVariablesInNestedLoop(
      Set<NormalLoopInfo> pNormalLoopInfo) {
    Set<NormalLoopInfo> updatedLoopInfo = new HashSet<>();
    for (NormalLoopInfo info : pNormalLoopInfo) {
      Set<NormalLoopInfo> outerLoops =
          pNormalLoopInfo.stream()
              .filter(l -> l.loop().isOuterLoopOf(info.loop()))
              .collect(Collectors.toSet());
      Map<String, String> updatedLiveVariables = new HashMap<>();
      updatedLiveVariables.putAll(info.liveVariablesAndTypes());

      for (ImmutableMap<String, String> liveVariables :
          outerLoops.stream().map(l -> l.liveVariablesAndTypes()).collect(Collectors.toSet())) {
        updatedLiveVariables.putAll(liveVariables);
      }
      updatedLoopInfo.add(
          new NormalLoopInfo(
              info.loopLocation(), info.loop(), ImmutableMap.copyOf(updatedLiveVariables)));
    }
    return updatedLoopInfo;
  }

  private static ImmutableSet<String> getVariablesFromAAstNode(AAstNode pAAstNode) {
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

  private static ImmutableMap<String, String> resolveStructIn(
      String pVariable,
      ImmutableMap<String, String> pMembers,
      ImmutableMap<String, ImmutableMap<String, String>> pAllStructInfos) {

    // Add pVariable in front of each name of pMembers
    Map<String, String> membersWithModifiedNames = new HashMap<>();
    for (String name : pMembers.keySet()) {
      StringBuilder modifiedName = new StringBuilder(name);
      int lastIndexOfAsterisk = modifiedName.lastIndexOf("*");
      if (lastIndexOfAsterisk == -1) {
        modifiedName.insert(0, pVariable + ".");
      } else {
        modifiedName.insert(lastIndexOfAsterisk, pVariable + ".");
      }

      membersWithModifiedNames.put(modifiedName.toString(), pMembers.get(name));
    }

    return resolveStructInHf(ImmutableMap.copyOf(membersWithModifiedNames), pAllStructInfos);
  }

  private static ImmutableMap<String, String> resolveStructInHf(
      ImmutableMap<String, String> pMembers,
      ImmutableMap<String, ImmutableMap<String, String>> pAllStructInfos) {
    Map<String, String> ans = new HashMap<>();

    for (Entry<String, String> member : pMembers.entrySet()) {
      String name = member.getKey();
      String type = member.getValue();
      ans.put(name, type);

      if (type.startsWith("struct ")) {
        ans.remove(name);

        ImmutableMap<String, String> originalMembersUnderOneLevel = pAllStructInfos.get(type);
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
            resolveStructInHf(ImmutableMap.copyOf(modifiedMembersUnderOneLevel), pAllStructInfos));
      }
    }

    return ImmutableMap.copyOf(ans);
  }

  private static ImmutableMap<String, ImmutableMap<String, String>> getAllStructInfos(CFA pCfa) {
    Map<String, ImmutableMap<String, String>> allStructInfos = new HashMap<>();

    for (CFAEdge cfaEdge : pCfa.edges()) {
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
                  .replace("  ", "")
                  .replace(" {", "");
          List<String> structParts = Splitter.on('\n').splitToList(cComplexTypeDeclaration);
          structName = structParts.get(0);
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

          allStructInfos.put(structName, ImmutableMap.copyOf(members));
        }
      }
    }

    return ImmutableMap.copyOf(allStructInfos);
  }

  private static ImmutableSet<String> getAllGlobalVariables(CFA pCfa) {
    Set<String> allGlobalVariables = new HashSet<>();
    Set<CFAEdge> internalEdges =
        new HashSet<>(); // Include only edges that occur in the source code,
    // excluding those from imported files.
    pCfa.edges().stream()
        .filter(e -> !e.toString().startsWith("/"))
        .forEach(e -> internalEdges.add(e));

    for (CFAEdge cfaEdge : internalEdges) {
      if (cfaEdge.getEdgeType() == CFAEdgeType.DeclarationEdge) {
        AAstNode aAstNode = cfaEdge.getRawAST().orElseThrow();
        if (aAstNode instanceof CVariableDeclaration) {
          CVariableDeclaration cVariableDeclaration = (CVariableDeclaration) aAstNode;
          if (cVariableDeclaration.isGlobal()) {
            String globalVariable = cVariableDeclaration.getQualifiedName();
            if (!globalVariable.startsWith("static__")) {
              allGlobalVariables.add(globalVariable);
            }
          }
        }
      }
    }
    return ImmutableSet.copyOf(allGlobalVariables);
  }
}
