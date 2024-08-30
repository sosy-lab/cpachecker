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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.ARightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
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
      Set<String> variablesDeclaredInsideLoop = new HashSet<>();
      Map<String, String> liveVariablesAndTypes = new HashMap<>();
      for (CFAEdge cfaEdge : loop.getInnerLoopEdges()) {
        if (cfaEdge.getRawAST().isPresent()) {
          AAstNode aAstNode = cfaEdge.getRawAST().orElseThrow();
          if (aAstNode instanceof CSimpleDeclaration) {
            variablesDeclaredInsideLoop.addAll(getVariablesFromAAstNode(aAstNode));
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
      liveVariables.addAll(allGlobalVariables);

      // Determine type of each variable
      for (String variable : liveVariables) {
        String type =
            Objects.requireNonNull(pCProgramScope.lookupVariable(variable)).getType().toString();
        variable =
            variable.contains("::")
                ? Iterables.get(Splitter.on("::").split(variable), 1)
                : variable;

        if (type.contains("*")) {
          String typeOfReferencedValue = type.replace("*", "").replace("(", "").replace(")", "");
          int numberOfAsterisks = (type.length() - typeOfReferencedValue.length()) / 3;
          String dereferencingVariable = "*".repeat(numberOfAsterisks) + variable;

          liveVariablesAndTypes.put(dereferencingVariable, typeOfReferencedValue);
        } else {
          liveVariablesAndTypes.put(variable, type);
        }
      }

      for (Integer loopLocation : loopLocations) {
        allNormalLoopInfos.add(
            new NormalLoopInfo(loopLocation, ImmutableMap.copyOf(liveVariablesAndTypes)));
      }
    }

    return ImmutableSet.copyOf(allNormalLoopInfos);
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

  // TODO: the method can return variables like stdin, stdout, and stderr
  private static ImmutableSet<String> getAllGlobalVariables(CFA pCfa) {
    Set<String> allGlobalVariables = new HashSet<>();

    for (CFAEdge cfaEdge : pCfa.edges()) {
      if (cfaEdge.getEdgeType() == CFAEdgeType.DeclarationEdge) {
        AAstNode aAstNode = cfaEdge.getRawAST().orElseThrow();
        getVariablesFromAAstNode(aAstNode).stream()
            .filter(e -> !e.contains("::"))
            .forEach(e -> allGlobalVariables.add(e));
      }
    }

    return ImmutableSet.copyOf(allGlobalVariables);
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

    } else if (pAAstNode instanceof CFunctionCallStatement cFunctionCallStatement) {
      cFunctionCallStatement
          .getFunctionCallExpression()
          .getParameterExpressions()
          .forEach(e -> CFAUtils.getVariableNamesOfExpression(e).forEach(n -> variables.add(n)));

    } else if (pAAstNode
        instanceof CFunctionCallAssignmentStatement cFunctionCallAssignmentStatement) {

      CLeftHandSide cLeftHandSide = cFunctionCallAssignmentStatement.getLeftHandSide();
      CFAUtils.getVariableNamesOfExpression(cLeftHandSide).forEach(e -> variables.add(e));

      CFunctionCallExpression cRightHandSide = cFunctionCallAssignmentStatement.getRightHandSide();
      cRightHandSide
          .getParameterExpressions()
          .forEach(e -> CFAUtils.getVariableNamesOfExpression(e).forEach(n -> variables.add(n)));

    } else if (pAAstNode instanceof CVariableDeclaration cVariableDeclaration) {
      variables.add(cVariableDeclaration.getQualifiedName());
    }

    return ImmutableSet.copyOf(variables);
  }
}
