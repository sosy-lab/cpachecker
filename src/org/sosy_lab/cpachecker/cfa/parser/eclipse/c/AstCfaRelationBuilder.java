// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.eclipse.c;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
    ImmutableMap.Builder<CFANode, Set<AVariableDeclaration>> localVariablesInInputProgram =
        ImmutableMap.builderWithExpectedSize(pCfaNodeToAstLocalVariablesInScope.size());
    ImmutableMap.Builder<CFANode, Set<AParameterDeclaration>> parametersInInputProgram =
        ImmutableMap.builderWithExpectedSize(pCfaNodeToAstParametersVariablesInScope.size());
    for (Entry<CFANode, Set<AVariableDeclaration>> entry :
        pCfaNodeToAstLocalVariablesInScope.entrySet()) {
      CFANode node = entry.getKey();
      Set<AVariableDeclaration> localVariables = entry.getValue();
      Set<AParameterDeclaration> parameters =
          Objects.requireNonNull(pCfaNodeToAstParametersVariablesInScope.get(node));
      Collection<ASimpleDeclaration> declarations =
          getDeclarationsInInputProgram(node, parameters, localVariables);
      // The CFA builder shares its sets among the nodes with the same scope
      if (declarations.size() == parameters.size() + localVariables.size()) {
        localVariablesInInputProgram.put(node, localVariables);
        parametersInInputProgram.put(node, parameters);
      } else {
        localVariablesInInputProgram.put(
            node, FluentIterable.from(declarations).filter(AVariableDeclaration.class).toSet());
        parametersInInputProgram.put(
            node, FluentIterable.from(declarations).filter(AParameterDeclaration.class).toSet());
      }
    }
    return new AstCfaRelation(
        getIfStructures(classifier),
        getIterationStructures(classifier),
        classifier.getStatementOffsetsToLocations(),
        getStatementStructures(classifier),
        localVariablesInInputProgram.buildOrThrow(),
        parametersInInputProgram.buildOrThrow(),
        pCfaNodeToAstLocalVariablesInScope,
        pCfaNodeToAstParametersVariablesInScope,
        pGlobalVariables,
        classifier.getExpressionLocations());
  }

  /**
   * Returns the parameters and local variables that the CFA builder recorded for the node without
   * the ones that go out of scope at the node, e.g., the variable of a for loop at the node after
   * the loop, and without the ones hidden by a declaration with the same name in an inner block.
   */
  private static Collection<ASimpleDeclaration> getDeclarationsInInputProgram(
      CFANode pNode,
      Set<AParameterDeclaration> pParameters,
      Set<AVariableDeclaration> pLocalVariables) {
    Set<? extends ASimpleDeclaration> outOfScope = pNode.getOutOfScopeVariables();
    Map<String, ASimpleDeclaration> declarationsByName = new LinkedHashMap<>();
    for (ASimpleDeclaration declaration : Iterables.concat(pParameters, pLocalVariables)) {
      if (!outOfScope.contains(declaration)) {
        declarationsByName.merge(
            declaration.getOrigName(), declaration, AstCfaRelationBuilder::getHidingDeclaration);
      }
    }
    return declarationsByName.values();
  }

  /**
   * Returns which of two declarations with the same name in scope at the same node hides the other
   * one. The parameters come first.
   */
  private static ASimpleDeclaration getHidingDeclaration(
      ASimpleDeclaration pFirst, ASimpleDeclaration pSecond) {
    if (pFirst instanceof AParameterDeclaration) {
      // A local variable with the name of a parameter is in an inner block
      return pSecond;
    }
    // The inner block, and thus the hiding declaration, starts after the hidden declaration
    FileLocation first = pFirst.getFileLocation();
    FileLocation second = pSecond.getFileLocation();
    assert first.isRealLocation()
            && second.isRealLocation()
            && first.getFileName().equals(second.getFileName())
            && first.getNodeOffset() != second.getNodeOffset()
        : "Cannot determine whether " + pFirst + " or " + pSecond + " hides the other one";
    return first.getNodeOffset() < second.getNodeOffset() ? pSecond : pFirst;
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
