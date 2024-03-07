// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.eclipse.c;

import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Optional;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.sosy_lab.cpachecker.cfa.CSourceOriginMapping;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.ast.ASTStructure;
import org.sosy_lab.cpachecker.util.ast.IfStructure;
import org.sosy_lab.cpachecker.util.ast.IterationStructure;

public class ASTStructureBuilder {

  public static ASTStructure getASTStructure(
      CSourceOriginMapping pSourceOriginMapping,
      ImmutableSet<CFAEdge> pEdges,
      List<IASTTranslationUnit> pAsts) {
    ASTLocationClassifier classifier = new ASTLocationClassifier(pSourceOriginMapping);
    for (IASTTranslationUnit ast : pAsts) {
      ast.accept(classifier);
    }
    return new ASTStructure(
        getIfStructures(pEdges, classifier),
        getIterationStructures(pEdges, classifier),
        classifier.getStatementOffsetsToLocations());
  }

  private static ImmutableSet<IfStructure> getIfStructures(
      ImmutableSet<CFAEdge> pEdges, ASTLocationClassifier classifier) {
    ImmutableSet.Builder<IfStructure> ifStructures = new ImmutableSet.Builder<>();
    for (FileLocation loc : classifier.ifLocations) {
      ifStructures.add(
          new IfStructure(
              loc,
              classifier.ifCondition.get(loc),
              classifier.ifThenClause.get(loc),
              Optional.ofNullable(classifier.ifElseClause.get(loc)),
              pEdges));
    }
    return ifStructures.build();
  }

  private static ImmutableSet<IterationStructure> getIterationStructures(
      ImmutableSet<CFAEdge> pEdges, ASTLocationClassifier classifier) {
    ImmutableSet.Builder<IterationStructure> iterationStructures = new ImmutableSet.Builder<>();
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
    return iterationStructures.build();
  }
}
