// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.eclipse.c;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.sosy_lab.cpachecker.cfa.CSourceOriginMapping;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
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
      ImmutableSet<CFAEdge> pEdges,
      List<IASTTranslationUnit> pAsts,
      Map<CFANode, Set<AVariableDeclaration>> pCfaNodeToAstLocalVariablesInScope,
      Map<CFANode, Set<AParameterDeclaration>> pCfaNodeToAstParametersVariablesInScope,
      Set<AVariableDeclaration> pGlobalVariables) {
    AstLocationClassifier classifier = new AstLocationClassifier(pSourceOriginMapping);
    for (IASTTranslationUnit ast : pAsts) {
      ast.accept(classifier);
    }
    return new AstCfaRelation(
        getIfStructures(pEdges, classifier),
        getIterationStructures(pEdges, classifier),
        classifier.getStatementOffsetsToLocations(),
        getStatementStructures(pEdges, classifier),
        pCfaNodeToAstLocalVariablesInScope,
        pCfaNodeToAstParametersVariablesInScope,
        pGlobalVariables,
        classifier.getExpressionLocations());
  }

  private static ImmutableSet<IfElement> getIfStructures(
      ImmutableSet<CFAEdge> pEdges, AstLocationClassifier classifier) {
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
              Optional.ofNullable(ifElseClause.get(loc)),
              pEdges));
    }
    return ifStructures.build();
  }

  private static ImmutableSet<IterationElement> getIterationStructures(
      ImmutableSet<CFAEdge> pEdges, AstLocationClassifier classifier) {
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
              Optional.ofNullable(loopIterationStatement.get(loc)),
              pEdges));
    }
    return iterationStructures.build();
  }

  @SuppressWarnings("unused")
  private static ImmutableSet<DeclarationElement> getDeclarationStructures(
      ImmutableSet<CFAEdge> pEdges, AstLocationClassifier classifier) {
    ImmutableSet.Builder<DeclarationElement> declarationStructures = new ImmutableSet.Builder<>();
    for (FileLocation loc : classifier.getDeclarationLocations()) {
      declarationStructures.add(new DeclarationElement(loc, pEdges));
    }
    return declarationStructures.build();
  }

  private static ImmutableSet<StatementElement> getStatementStructures(
      ImmutableSet<CFAEdge> pEdges, AstLocationClassifier classifier) {
    ImmutableSet.Builder<StatementElement> statementStructures = new ImmutableSet.Builder<>();
    for (FileLocation loc : classifier.getStatementLocations()) {
      statementStructures.add(new StatementElement(loc, pEdges));
    }
    return statementStructures.build();
  }
}
