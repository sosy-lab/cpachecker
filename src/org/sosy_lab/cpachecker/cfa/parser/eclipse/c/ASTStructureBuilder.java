// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.eclipse.c;

import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.sosy_lab.cpachecker.cfa.CSourceOriginMapping;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.ast.ASTStructure;
import org.sosy_lab.cpachecker.util.ast.IfStructure;
import org.sosy_lab.cpachecker.util.ast.IterationStructure;

public class ASTStructureBuilder {

  private final ASTLocationClassifier classifier;

  final Set<IfStructure> ifStructures = new HashSet<>();
  final Set<IterationStructure> iterationStructures = new HashSet<>();

  public ASTStructureBuilder(CSourceOriginMapping pSourceOriginMapping) {
    classifier = new ASTLocationClassifier(pSourceOriginMapping);
  }

  public ASTStructure getASTStructure() {
    return new ASTStructure(
        ifStructures, iterationStructures, classifier.statementOffsetsToLocations);
  }

  public void analyze(IASTTranslationUnit pTranslationUnit) {
    pTranslationUnit.accept(classifier);
  }

  public void updateStructures(ImmutableSet<CFAEdge> pEdges) {
    updateIfStructures(pEdges);
    updateIterationStructures(pEdges);
  }

  private void updateIfStructures(ImmutableSet<CFAEdge> pEdges) {
    for (FileLocation loc : classifier.ifLocations) {
      ifStructures.add(
          new IfStructure(
              loc,
              classifier.ifCondition.get(loc),
              classifier.ifThenClause.get(loc),
              Optional.ofNullable(classifier.ifElseClause.get(loc)),
              pEdges));
    }
  }

  private void updateIterationStructures(ImmutableSet<CFAEdge> pEdges) {
    for (FileLocation loc : classifier.loopLocations) {
      iterationStructures.add(
          new IterationStructure(
              loc,
              Optional.ofNullable(classifier.loopParenthesesBlock.get(loc)),
              Optional.ofNullable(classifier.loopControllingExpression.get(loc)),
              classifier.loopBody.get(loc),
              Optional.ofNullable(classifier.loopInitializer.get(loc)),
              Optional.ofNullable(classifier.loopIterationStatement.get(loc)),
              pEdges));
    }
  }
}
