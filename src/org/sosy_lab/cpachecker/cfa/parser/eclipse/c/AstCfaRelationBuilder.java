// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.eclipse.c;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.BinaryOperator;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.sosy_lab.cpachecker.cfa.CSourceOriginMapping;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.ast.AstCfaRelation;
import org.sosy_lab.cpachecker.util.ast.DeclarationElement;
import org.sosy_lab.cpachecker.util.ast.IfElement;
import org.sosy_lab.cpachecker.util.ast.IterationElement;
import org.sosy_lab.cpachecker.util.ast.StatementElement;

class AstCfaRelationBuilder {

  public static AstCfaRelation getASTCFARelation(
      CSourceOriginMapping pSourceOriginMapping,
      List<IASTTranslationUnit> pAsts,
      ImmutableMap<CFANode, Set<AVariableDeclaration>> pCfaNodeToAstLocalVariablesInScope,
      ImmutableMap<CFANode, Set<AParameterDeclaration>> pCfaNodeToAstParametersVariablesInScope,
      ImmutableSet<AVariableDeclaration> pGlobalVariables) {
    AstLocationClassifier classifier = new AstLocationClassifier(pSourceOriginMapping);
    for (IASTTranslationUnit ast : pAsts) {
      ast.accept(classifier);
    }
    ImmutableMap<CFANode, Set<AVariableDeclaration>> localVariablesInInputProgram =
        getLocalVariablesInInputProgram(pCfaNodeToAstLocalVariablesInScope);
    return new AstCfaRelation(
        getIfStructures(classifier),
        getIterationStructures(classifier),
        classifier.getStatementOffsetsToLocations(),
        getStatementStructures(classifier),
        localVariablesInInputProgram,
        getParametersInInputProgram(
            pCfaNodeToAstParametersVariablesInScope, localVariablesInInputProgram),
        pCfaNodeToAstLocalVariablesInScope,
        pCfaNodeToAstParametersVariablesInScope,
        pGlobalVariables,
        classifier.getExpressionLocations());
  }

  /**
   * Removes from the local variables that the CFA builder recorded for each node the ones that go
   * out of scope at the node, e.g., the variable of a for loop at the node after the loop, and the
   * ones hidden by a declaration with the same name in an inner block.
   */
  private static ImmutableMap<CFANode, Set<AVariableDeclaration>> getLocalVariablesInInputProgram(
      ImmutableMap<CFANode, Set<AVariableDeclaration>> pLocalVariables) {
    ImmutableMap.Builder<CFANode, Set<AVariableDeclaration>> result =
        ImmutableMap.builderWithExpectedSize(pLocalVariables.size());
    for (Entry<CFANode, Set<AVariableDeclaration>> entry : pLocalVariables.entrySet()) {
      Set<? extends ASimpleDeclaration> outOfScope = entry.getKey().getOutOfScopeVariables();
      Map<String, AVariableDeclaration> innermostDeclarations = new LinkedHashMap<>();
      for (AVariableDeclaration variable : entry.getValue()) {
        if (!outOfScope.contains(variable)) {
          // A hidden declaration comes before the one that hides it
          innermostDeclarations.merge(
              variable.getOrigName(),
              variable,
              BinaryOperator.maxBy(
                  Comparator.comparingInt(
                      declaration -> declaration.getFileLocation().getNodeOffset())));
        }
      }
      result.put(
          entry.getKey(),
          innermostDeclarations.size() == entry.getValue().size()
              ? entry.getValue()
              : ImmutableSet.copyOf(innermostDeclarations.values()));
    }
    return result.buildOrThrow();
  }

  /**
   * Removes from the parameters that the CFA builder recorded for each node the ones that go out of
   * scope at the node and the ones hidden by a local variable with the same name.
   */
  private static ImmutableMap<CFANode, Set<AParameterDeclaration>> getParametersInInputProgram(
      ImmutableMap<CFANode, Set<AParameterDeclaration>> pParameters,
      ImmutableMap<CFANode, Set<AVariableDeclaration>> pLocalVariablesInInputProgram) {
    ImmutableMap.Builder<CFANode, Set<AParameterDeclaration>> result =
        ImmutableMap.builderWithExpectedSize(pParameters.size());
    for (Entry<CFANode, Set<AParameterDeclaration>> entry : pParameters.entrySet()) {
      Set<? extends ASimpleDeclaration> outOfScope = entry.getKey().getOutOfScopeVariables();
      ImmutableSet<String> localNames =
          transformedImmutableSetCopy(
              pLocalVariablesInInputProgram.getOrDefault(entry.getKey(), ImmutableSet.of()),
              AVariableDeclaration::getOrigName);
      ImmutableSet<AParameterDeclaration> parameters =
          FluentIterable.from(entry.getValue())
              .filter(
                  parameter ->
                      !outOfScope.contains(parameter)
                          && !localNames.contains(parameter.getOrigName()))
              .toSet();
      result.put(
          entry.getKey(),
          parameters.size() == entry.getValue().size() ? entry.getValue() : parameters);
    }
    return result.buildOrThrow();
  }

  private static ImmutableSet<IfElement> getIfStructures(AstLocationClassifier classifier) {
    ImmutableMap<FileLocation, FileLocation> ifCondition = classifier.getIfCondition();
    ImmutableMap<FileLocation, FileLocation> ifThenClause = classifier.getIfThenClause();
    ImmutableMap<FileLocation, FileLocation> ifElseClause = classifier.getIfElseClause();
    ImmutableSet.Builder<IfElement> ifStructures = new ImmutableSet.Builder<>();
    for (FileLocation loc : classifier.getIfLocations()) {
      ifStructures.add(
          new IfElement(
              loc,
              ifCondition.get(loc),
              ifThenClause.get(loc),
              Optional.ofNullable(ifElseClause.get(loc))));
    }
    return ifStructures.build();
  }

  private static ImmutableSet<IterationElement> getIterationStructures(
      AstLocationClassifier classifier) {
    Map<FileLocation, FileLocation> loopParenthesesBlock = classifier.getLoopParenthesesBlock();
    Map<FileLocation, FileLocation> loopControllingExpression =
        classifier.getLoopControllingExpression();
    Map<FileLocation, FileLocation> loopBody = classifier.getLoopBody();

    Map<FileLocation, FileLocation> loopInitializer = classifier.getLoopInitializer();
    Map<FileLocation, FileLocation> loopIterationStatement = classifier.getLoopIterationStatement();

    ImmutableSet.Builder<IterationElement> iterationStructures = new ImmutableSet.Builder<>();
    for (FileLocation loc : classifier.getLoopLocations()) {
      iterationStructures.add(
          new IterationElement(
              loc,
              Optional.ofNullable(loopParenthesesBlock.get(loc)),
              Optional.ofNullable(loopControllingExpression.get(loc)),
              loopBody.get(loc),
              Optional.ofNullable(loopInitializer.get(loc)),
              Optional.ofNullable(loopIterationStatement.get(loc))));
    }
    return iterationStructures.build();
  }

  @SuppressWarnings("unused")
  private static ImmutableSet<DeclarationElement> getDeclarationStructures(
      ImmutableSet<CFAEdge> pEdges, AstLocationClassifier classifier) {
    ImmutableSet.Builder<DeclarationElement> declarationStructures = new ImmutableSet.Builder<>();
    for (FileLocation loc : classifier.getDeclarationLocations()) {
      declarationStructures.add(new DeclarationElement(loc));
    }
    return declarationStructures.build();
  }

  private static ImmutableSet<StatementElement> getStatementStructures(
      AstLocationClassifier classifier) {
    ImmutableSet.Builder<StatementElement> statementStructures = new ImmutableSet.Builder<>();
    for (FileLocation loc : classifier.getStatementLocations()) {
      statementStructures.add(new StatementElement(loc));
    }
    return statementStructures.build();
  }
}
