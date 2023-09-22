// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.ast;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.core.runtime.CoreException;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CParser;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.c.EclipseCdtWrapper;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class ASTStructure {
  private EclipseCdtWrapper cdt;
  private final ASTLocationClassifier classifier;
  private final LogManager logger;
  private final CFA cfa;

  final Set<IfStructure> ifStructures = new HashSet<>();
  final Set<IterationStructure> iterationStructures = new HashSet<>();

  public ASTStructure(
      Configuration pConfig, ShutdownNotifier pShutdownNotifier, LogManager pLogger, CFA pCfa)
      throws InvalidConfigurationException, CoreException, InterruptedException, IOException {
    classifier = new ASTLocationClassifier();
    cdt = new EclipseCdtWrapper(CParser.Factory.getOptions(pConfig), pShutdownNotifier);
    logger = pLogger;
    cfa = pCfa;
    analyzeCFA();
  }

  private void analyzeCFA() throws CoreException, InterruptedException, IOException {
    classifier.indexFileNames(cfa.getFileNames());
    for (Path filename : cfa.getFileNames()) {
      final IASTTranslationUnit astUnit =
          cdt.getASTTranslationUnit(EclipseCdtWrapper.wrapFile(filename));
      astUnit.accept(classifier);
    }
    classifier.update();
    updateIfStructures(cfa);
    updateIterationStructures(cfa);
  }

  private void updateIfStructures(CFA pCfa) {
    // CFA is still mutable => prevent calculating edge set multiple times:
    ImmutableSet<CFAEdge> edges = CFAUtils.allEdges(pCfa).toSet();
    for (FileLocation loc : classifier.ifLocations) {
      ifStructures.add(
          new IfStructure(
              loc,
              classifier.ifCondition.get(loc),
              classifier.ifThenClause.get(loc),
              Optional.ofNullable(classifier.ifElseClause.get(loc)),
              edges));
    }
  }

  private void updateIterationStructures(CFA pCfa) {
    // CFA is still mutable => prevent calculating edge set multiple times:
    ImmutableSet<CFAEdge> edges = CFAUtils.allEdges(pCfa).toSet();
    for (FileLocation loc : classifier.loopLocations) {
      iterationStructures.add(
          new IterationStructure(
              loc,
              Optional.ofNullable(classifier.loopParenthesesBlock.get(loc)),
              Optional.ofNullable(classifier.loopControllingExpression.get(loc)),
              classifier.loopBody.get(loc),
              Optional.ofNullable(classifier.loopInitializer.get(loc)),
              Optional.ofNullable(classifier.loopIterationStatement.get(loc)),
              edges));
    }
  }

  public void analyzerReport() {
    logger.log(
        Level.INFO,
        "The following statement offsets where found:",
        ImmutableList.of(
            classifier.statementLocations.stream()
                .map(loc -> loc.getNodeOffset())
                .distinct()
                .sorted()
                .collect(ImmutableList.toImmutableList())));
    logger.log(
        Level.INFO,
        "The following compound statement offsets where found:",
        ImmutableList.of(
            classifier.compoundLocations.stream()
                .map(loc -> loc.getNodeOffset())
                .distinct()
                .sorted()
                .collect(ImmutableList.toImmutableList())));
    List<CFAEdge> statementStartEdges = new ArrayList<>();
    List<CFAEdge> declarationStartEdges = new ArrayList<>();
    List<CFAEdge> otherEdges = new ArrayList<>();
    for (CFAEdge edge : cfa.edges()) {
      if (startsAtStatement(edge)) {
        statementStartEdges.add(edge);
      } else if (startsAtDeclaration(edge)) {
        declarationStartEdges.add(edge);
      } else {
        otherEdges.add(edge);
      }
    }
    logger.log(
        Level.INFO,
        "The following",
        statementStartEdges.size(),
        " edges start at statements:\n",
        statementStartEdges.stream()
            .map(
                x ->
                    x.getFileLocation().getNodeOffset()
                        + ":"
                        + (x.getFileLocation().getNodeOffset()
                            + x.getFileLocation().getNodeLength())
                        + " "
                        + x)
            .collect(Collectors.joining("\n")));
    logger.log(
        Level.INFO,
        "The following",
        declarationStartEdges.size(),
        " edges (excklusively) start at declarations:\n",
        declarationStartEdges.stream()
            .map(
                x ->
                    x.getFileLocation().getNodeOffset()
                        + ":"
                        + (x.getFileLocation().getNodeOffset()
                            + x.getFileLocation().getNodeLength())
                        + " "
                        + x)
            .collect(Collectors.joining("\n")));
    logger.log(
        Level.INFO,
        "The following",
        otherEdges.size(),
        " edges DO NOT start at statements:\n",
        otherEdges.stream()
            .map(
                x ->
                    x.getFileLocation().getNodeOffset()
                        + ":"
                        + (x.getFileLocation().getNodeOffset()
                            + x.getFileLocation().getNodeLength())
                        + " "
                        + x)
            .collect(Collectors.joining("\n")));
    for (FileLocation ifLocation : classifier.ifLocations) {
      List<CFAEdge> edges = new ArrayList<>();
      for (CFAEdge e : cfa.edges()) {
        if (FileLocationUtils.entails(
            classifier.ifCondition.get(ifLocation), e.getFileLocation())) {
          edges.add(e);
        }
      }
      logger.log(
          Level.INFO,
          "The branching at "
              + classifier.ifCondition.get(ifLocation).getNodeOffset()
              + ":"
              + (classifier.ifCondition.get(ifLocation).getNodeOffset()
                  + classifier.ifCondition.get(ifLocation).getNodeLength())
              + " has the following edges:\n"
              + edges.stream()
                  .map(x -> x.getFileLocation().getNodeOffset() + " " + x)
                  .collect(Collectors.joining("\n")));
    }
  }

  public boolean startsAtStatement(CFAEdge edge) {
    return classifier.statementStartOffsets.contains(edge.getFileLocation().getNodeOffset());
  }

  public boolean startsAtDeclaration(CFAEdge edge) {
    return classifier.declarationStartOffsets.contains(edge.getFileLocation().getNodeOffset());
  }

  /*
   * a regular if only has one branching; a irregular if has a disjunction or conjunction or side
   * effects in the condition and thus has more complicated branching structure
   */
  @SuppressWarnings("unused")
  public boolean isRegularIf(CFAEdge edge) {
    return false;
  }

  public Set<IfStructure> getIfStructures() {
    return ifStructures;
  }

  public Set<IterationStructure> getIterationStructures() {
    return iterationStructures;
  }
}
