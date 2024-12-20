// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.instrumentation;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

public class LoopInfoUtils {

  private static final String EXPRESSION_PLACEHOLDER = "$";

  public static ImmutableSet<NormalLoopInfo> getAllNormalLoopInfos(
      CFA pCfa, CProgramScope pCProgramScope) {
    Set<NormalLoopInfo> allNormalLoopInfos = new HashSet<>();
    ImmutableSet<String> allGlobalVariables = getAllGlobalVariables(pCfa);
    ImmutableMap<String, ImmutableMap<String, String>> allStructInfos = getAllStructInfos(pCfa);
    ImmutableMap<String, ImmutableMap<String, String>> allDecomposedStructsWithPlaceHolder =
        decomposeAllStructs(allStructInfos);

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
      Set<String> liveVariables = new HashSet<>(allGlobalVariables);
      Set<String> variablesDeclaredInsideLoop = new HashSet<>();
      Map<String, String> liveVariablesAndTypes = new LinkedHashMap<>();
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

      /**
       * If there are multiple for-loops with multiple declarations of the same loop variable, for
       * example, i, calling pCProgramScope.lookupVariable(i) throws an exception. therefore,
       * retrieving the type of the loop variable i must be handled seperately.
       */
      boolean isForLoop =
          loop.getIncomingEdges().stream().findAny().orElseThrow().getRawAST().isPresent();
      if (isForLoop) {
        AAstNode initializationExpression =
            loop.getIncomingEdges().stream().findAny().orElseThrow().getRawAST().orElseThrow();
        if (initializationExpression instanceof CSimpleDeclaration) {
          CSimpleDeclaration loopVariableDeclaration =
              (CSimpleDeclaration) initializationExpression;
          String originalLoopVariable = loopVariableDeclaration.getOrigName();
          String qualifiedLoopVariable = loopVariableDeclaration.getQualifiedName();
          String type = loopVariableDeclaration.getType().toString();

          liveVariables.remove(qualifiedLoopVariable);
          liveVariablesAndTypes.put(originalLoopVariable, type);
        }
      }

      // Decompose each variable into primitive expressions
      for (String variable : liveVariables) {
        Entry<String, String> preprocessedVariableAndType =
            preprocess(variable, pCProgramScope.lookupVariable(variable).getType().toString());
        String preprocessedVariable = preprocessedVariableAndType.getKey();
        String preprocessedType = preprocessedVariableAndType.getValue();
        liveVariablesAndTypes.putAll(
            decompose(preprocessedVariable, preprocessedType, allDecomposedStructsWithPlaceHolder));
      }

      for (Integer loopLocation : loopLocations) {
        allNormalLoopInfos.add(
            new NormalLoopInfo(loopLocation, loop, ImmutableMap.copyOf(liveVariablesAndTypes)));
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

  public static ImmutableSet<NormalLoopInfo> includeAllTheOuterLiveVariablesInNestedLoop(
      Set<NormalLoopInfo> pNormalLoopInfo) {
    Set<NormalLoopInfo> updatedLoopInfo = new HashSet<>();
    for (NormalLoopInfo info : pNormalLoopInfo) {
      ImmutableSet<NormalLoopInfo> outerLoops =
          pNormalLoopInfo.stream()
              .filter(l -> l.loop().isOuterLoopOf(info.loop()))
              .collect(ImmutableSet.toImmutableSet());
      Map<String, String> updatedLiveVariables = new LinkedHashMap<>(info.liveVariablesAndTypes());

      for (ImmutableMap<String, String> liveVariables :
          transformedImmutableSetCopy(outerLoops, l -> l.liveVariablesAndTypes())) {
        updatedLiveVariables.putAll(liveVariables);
      }
      updatedLoopInfo.add(
          new NormalLoopInfo(
              info.loopLocation(), info.loop(), ImmutableMap.copyOf(updatedLiveVariables)));
    }
    return ImmutableSet.copyOf(updatedLoopInfo);
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

  private static Entry<String, String> preprocess(String pVariable, String pType) {
    if (!pType.contains("*") && !pType.contains("]")) {
      return new SimpleEntry<>(
          pVariable.contains("::")
              ? Iterables.get(Splitter.on("::").split(pVariable), 1)
              : pVariable,
          pType);
    }

    StringBuilder reversedVariableSb =
        new StringBuilder(
                pVariable.contains("::")
                    ? Iterables.get(Splitter.on("::").split(pVariable), 1)
                    : pVariable)
            .reverse();
    StringBuilder reversedTypeSb = new StringBuilder(pType).reverse();

    while (reversedTypeSb.charAt(0) == '*' || reversedTypeSb.charAt(0) == ']') {

      if (reversedTypeSb.charAt(0) == '*') {
        reversedVariableSb.append('*');
        reversedTypeSb.deleteCharAt(reversedTypeSb.length() - 1);
        reversedTypeSb.delete(0, 2);

        if (reversedTypeSb.charAt(0) == ']') {
          reversedVariableSb.insert(0, ')');
          reversedVariableSb.append('(');
        }
      } else {
        int indexOfFirstLeftBracket = reversedTypeSb.indexOf("[");
        String reversedSize = reversedTypeSb.substring(1, indexOfFirstLeftBracket);

        reversedVariableSb.insert(0, "[");
        reversedVariableSb.insert(0, reversedSize);
        reversedVariableSb.insert(0, "]");

        reversedTypeSb.deleteCharAt(reversedTypeSb.length() - 1);
        reversedTypeSb.delete(0, indexOfFirstLeftBracket + 2);
      }
    }

    return new SimpleEntry<>(
        reversedVariableSb.reverse().toString(), reversedTypeSb.reverse().toString());
  }

  private static ImmutableMap<String, String> decompose(
      String pPreprocessedVariable,
      String pPreprocessedType,
      Map<String, ImmutableMap<String, String>> allDecomposedStructs) {
    Map<String, String> temp = new LinkedHashMap<>();
    if (!pPreprocessedType.startsWith("struct ")) {
      temp.put(pPreprocessedVariable, pPreprocessedType);
    } else {
      allDecomposedStructs
          .get(pPreprocessedType)
          .entrySet()
          .forEach(
              e ->
                  temp.put(
                      e.getKey().replace(EXPRESSION_PLACEHOLDER, pPreprocessedVariable),
                      e.getValue()));
    }

    return expandArrays(ImmutableMap.copyOf(temp));
  }

  private static ImmutableMap<String, String> expandArrays(
      ImmutableMap<String, String> pExpressionsAndTypes) {
    Map<String, String> result = new LinkedHashMap<>();

    for (Entry<String, String> expressionAndType : pExpressionsAndTypes.entrySet()) {
      String expression = expressionAndType.getKey();
      String type = expressionAndType.getValue();

      if (!expression.contains("[")) {
        result.put(expression, type);
        continue;
      } else {
        List<Integer> ranges = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\[(\\d+)\\]");
        Matcher matcher = pattern.matcher(expression);

        StringBuffer basePartBuffer = new StringBuffer();
        while (matcher.find()) {
          ranges.add(Integer.parseInt(matcher.group(1)));
          matcher.appendReplacement(basePartBuffer, "[%d]");
        }
        matcher.appendTail(basePartBuffer);
        String basePart = basePartBuffer.toString();

        ImmutableList<ImmutableList<Integer>> combinations =
            generateCombinations(ImmutableList.copyOf(ranges));
        for (ImmutableList<Integer> combination : combinations) {
          result.put(String.format(basePart, combination.toArray()), type);
        }
      }
    }

    return ImmutableMap.copyOf(result);
  }

  private static ImmutableList<ImmutableList<Integer>> generateCombinations(
      ImmutableList<Integer> pRanges) {
    return generateCombinationsHf(pRanges, ImmutableList.of(), 0);
  }

  private static ImmutableList<ImmutableList<Integer>> generateCombinationsHf(
      ImmutableList<Integer> pRanges, ImmutableList<Integer> pCurrent, int pDepth) {
    List<ImmutableList<Integer>> result = new ArrayList<>();
    List<Integer> current = new ArrayList<>();
    pCurrent.forEach(e -> current.add(e));

    if (pDepth == pRanges.size()) {
      result.add(pCurrent);
      return ImmutableList.copyOf(result);
    }

    for (int i = 0; i < pRanges.get(pDepth); i++) {
      current.add(i);
      result.addAll(generateCombinationsHf(pRanges, ImmutableList.copyOf(current), pDepth + 1));
      current.remove(current.size() - 1);
    }

    return ImmutableList.copyOf(result);
  }

  private static ImmutableMap<String, ImmutableMap<String, String>> decomposeAllStructs(
      ImmutableMap<String, ImmutableMap<String, String>> pAllStructInfos) {
    Map<String, ImmutableMap<String, String>> allDecomposedStructs = new HashMap<>();

    for (String struct : pAllStructInfos.keySet()) {
      Map<String, String> expressionAndType = new HashMap<>();
      expressionAndType.put(EXPRESSION_PLACEHOLDER, struct);
      allDecomposedStructs.put(
          struct,
          decomposeStructExpressions(ImmutableMap.copyOf(expressionAndType), pAllStructInfos));
    }

    return ImmutableMap.copyOf(allDecomposedStructs);
  }

  private static ImmutableMap<String, String> decomposeStructExpressions(
      ImmutableMap<String, String> pExpressionsAndTypes,
      ImmutableMap<String, ImmutableMap<String, String>> pAllStructInfos) {
    Map<String, String> result = new LinkedHashMap<>();

    for (Entry<String, String> expressionAndType : pExpressionsAndTypes.entrySet()) {
      String expression = expressionAndType.getKey();
      String type = expressionAndType.getValue();

      if (!type.startsWith("struct ")) {
        result.put(expression, type);
      } else {
        Map<String, String> currentDecomposedParts = new LinkedHashMap<>();

        for (Entry<String, String> structMember : pAllStructInfos.get(type).entrySet()) {
          StringBuilder decomposedExpression = new StringBuilder(structMember.getKey());
          String decomposedType = structMember.getValue();

          decomposedExpression.insert(decomposedExpression.lastIndexOf("*") + 1, expression + '.');
          currentDecomposedParts.put(decomposedExpression.toString(), decomposedType);
        }

        result.putAll(
            decomposeStructExpressions(
                ImmutableMap.copyOf(currentDecomposedParts), pAllStructInfos));
      }
    }

    return ImmutableMap.copyOf(result);
  }

  private static ImmutableMap<String, ImmutableMap<String, String>> getAllStructInfos(CFA pCfa) {
    Map<String, ImmutableMap<String, String>> allStructInfos = new HashMap<>();

    for (CFAEdge cfaEdge :
        pCfa.edges().stream()
            .filter(e -> !e.toString().startsWith("/"))
            .toList()) { // Exclude the edges from the imported files
      Optional<AAstNode> aAstNodeOp = cfaEdge.getRawAST();
      if (aAstNodeOp.isPresent() && aAstNodeOp.orElseThrow() instanceof CComplexTypeDeclaration) {
        String cComplexTypeDeclaration =
            ((CComplexTypeDeclaration) aAstNodeOp.orElseThrow()).toString();

        if (cComplexTypeDeclaration.startsWith(
            "struct ")) { // A C complex type can also be an enum by definition in CPAchecker
          String structName;
          Map<String, String> members = new LinkedHashMap<>();

          // Every string representation of an AST node for a struct declaration has the same
          // format, which serves as the basis for the following modification.
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

    for (CFAEdge cfaEdge :
        pCfa.edges().stream()
            .filter(e -> !e.toString().startsWith("/"))
            .toList()) { // Exclude the edges from the imported files
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
