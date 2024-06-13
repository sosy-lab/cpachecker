// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.eclipse.c;

import com.google.common.base.Verify;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDoStatement;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTIfStatement;
import org.eclipse.cdt.core.dom.ast.IASTLabelStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTWhileStatement;
import org.sosy_lab.cpachecker.cfa.CSourceOriginMapping;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

class AstLocationClassifier extends ASTVisitor {
  private final ImmutableMap.Builder<Integer, FileLocation> statementOffsetsToLocations =
      new ImmutableMap.Builder<>();
  private final Map<Integer, FileLocation> sanityCheckStatementOffsetsToLocations = new HashMap<>();

  private final ImmutableSet.Builder<FileLocation> statementLocations =
      new ImmutableSet.Builder<>();
  private final ImmutableSet.Builder<FileLocation> compoundLocations = new ImmutableSet.Builder<>();

  private final ImmutableSet.Builder<FileLocation> labelLocations = new ImmutableSet.Builder<>();
  private final ImmutableSet.Builder<FileLocation> declarationLocations =
      new ImmutableSet.Builder<>();

  private final ImmutableSet.Builder<FileLocation> loopLocations = new ImmutableSet.Builder<>();
  private final ImmutableMap.Builder<FileLocation, FileLocation> loopControllingExpression =
      new ImmutableMap.Builder<>();
  private final ImmutableMap.Builder<FileLocation, FileLocation> loopInitializer =
      new ImmutableMap.Builder<>();
  private final ImmutableMap.Builder<FileLocation, FileLocation> loopIterationStatement =
      new ImmutableMap.Builder<>();
  private final ImmutableMap.Builder<FileLocation, FileLocation> loopParenthesesBlock =
      new ImmutableMap.Builder<>();
  private final ImmutableMap.Builder<FileLocation, FileLocation> loopBody =
      new ImmutableMap.Builder<>();

  private final ImmutableSet.Builder<FileLocation> ifLocations = new ImmutableSet.Builder<>();
  private final ImmutableMap.Builder<FileLocation, FileLocation> ifCondition =
      new ImmutableMap.Builder<>();
  private final ImmutableMap.Builder<FileLocation, FileLocation> ifThenClause =
      new ImmutableMap.Builder<>();
  private final ImmutableMap.Builder<FileLocation, FileLocation> ifElseClause =
      new ImmutableMap.Builder<>();

  private final ImmutableMap.Builder<String, Path> fileNames = new ImmutableMap.Builder<>();

  public final CSourceOriginMapping sourceOriginMapping;

  public AstLocationClassifier(CSourceOriginMapping pSourceOriginMapping) {
    super(true);
    sourceOriginMapping = pSourceOriginMapping;
  }

  public ImmutableSortedMap<Integer, FileLocation> getStatementOffsetsToLocations() {
    // Using an ImmutableMap and then copying it into a ImmutableSortedMap is necessary,
    // since ImmutableSortedMap does not implement buildKeepingLast, which is necessary
    // when multiple statements are at the same initial offset. Currently, this is
    // necessary for: test/programs/simple/builtin_types_compatible_void.c
    return ImmutableSortedMap.copyOf(statementOffsetsToLocations.buildKeepingLast());
  }

  public void indexFileNames(List<Path> pFileNames) {
    for (Path path : pFileNames) {
      fileNames.put(path.getFileName().toString(), path);
    }
  }

  /**
   * Different than as defined in the C-Standard statement also include declarations. For example
   * `int i = 0;` is a statement according to the eclipse CDT Parser, but not in the C-Standard.
   */
  @Override
  public int visit(IASTStatement statement) {
    FileLocation loc = getLocation(statement);
    if (statement instanceof IASTCompoundStatement) {
      compoundLocations.add(loc);
    } else if (statement instanceof IASTLabelStatement) {
      labelLocations.add(loc);
    } else if (statement instanceof IASTWhileStatement
        || statement instanceof IASTDoStatement
        || statement instanceof IASTForStatement) {
      loopLocations.add(loc);
      handleIterationStatement(statement);
    } else if (statement instanceof IASTIfStatement) {
      ifLocations.add(loc);
      handleIfStatement(statement);
    }

    if (sanityCheckStatementOffsetsToLocations.containsKey(loc.getNodeOffset())) {
      Verify.verify(sanityCheckStatementOffsetsToLocations.get(loc.getNodeOffset()).equals(loc));
    }

    statementOffsetsToLocations.put(loc.getNodeOffset(), loc);
    sanityCheckStatementOffsetsToLocations.put(loc.getNodeOffset(), loc);
    statementLocations.add(loc);
    return PROCESS_CONTINUE;
  }

  @Override
  public int visit(IASTDeclaration declaration) {
    declarationLocations.add(getLocation(declaration));
    return PROCESS_CONTINUE;
  }

  private FileLocation getLocation(IASTNode node) {
    IASTFileLocation iloc = node.getFileLocation();
    Path path = Path.of(iloc.getFileName());
    FileLocation loc =
        new FileLocation(
            path,
            path.getFileName().toString(),
            iloc.getNodeOffset(),
            iloc.getNodeLength(),
            iloc.getStartingLineNumber(),
            iloc.getEndingLineNumber(),
            sourceOriginMapping.getStartColumn(
                path, iloc.getStartingLineNumber(), iloc.getNodeOffset()),
            sourceOriginMapping.getStartColumn(
                path, iloc.getEndingLineNumber(), iloc.getNodeOffset() + iloc.getNodeLength()),
            sourceOriginMapping
                .getOriginLineFromAnalysisCodeLine(path, iloc.getStartingLineNumber())
                .getLineNumber(),
            sourceOriginMapping
                .getOriginLineFromAnalysisCodeLine(path, iloc.getEndingLineNumber())
                .getLineNumber(),
            sourceOriginMapping.isMappingToIdenticalLineNumbers());
    return loc;
  }

  private void handleIfStatement(IASTNode pStatement) {
    FileLocation loc = getLocation(pStatement);
    if (pStatement instanceof IASTIfStatement ifStatement) {
      ifCondition.put(loc, getLocation(ifStatement.getConditionExpression()));
      ifThenClause.put(loc, getLocation(ifStatement.getThenClause()));
      if (ifStatement.getElseClause() != null) {
        ifElseClause.put(loc, getLocation(ifStatement.getElseClause()));
      }
    }
  }

  private void handleIterationStatement(IASTStatement statement) {
    FileLocation loc = getLocation(statement);
    IASTStatement body = null;
    Optional<IASTExpression> controllingExpression = Optional.empty();
    Optional<IASTStatement> initializer = Optional.empty();
    Optional<IASTExpression> iteration = Optional.empty();
    if (statement instanceof IASTWhileStatement whileStatement) {
      body = whileStatement.getBody();
      controllingExpression = Optional.of(whileStatement.getCondition());
    } else if (statement instanceof IASTDoStatement doStatement) {
      body = doStatement.getBody();
      controllingExpression = Optional.of(doStatement.getCondition());
    } else if (statement instanceof IASTForStatement forStatement) {
      body = forStatement.getBody();
      controllingExpression = Optional.ofNullable(forStatement.getConditionExpression());
      initializer = Optional.ofNullable(forStatement.getInitializerStatement());
      iteration = Optional.ofNullable(forStatement.getIterationExpression());
    } else {
      throw new UnsupportedOperationException("Unknown type of iteration statement");
    }
    // body and cond are not null at this point.
    loopBody.put(loc, getLocation(body));
    assert controllingExpression != null;
    FileLocation controllingExpressionLocation = null;
    FileLocation initializerLocation = null;
    FileLocation iterationLocation = null;
    if (controllingExpression.isPresent()) {
      controllingExpressionLocation = getLocation(controllingExpression.orElseThrow());
      loopControllingExpression.put(loc, controllingExpressionLocation);
    }
    if (initializer.isPresent()) {
      initializerLocation = getLocation(initializer.orElseThrow());
      loopInitializer.put(loc, initializerLocation);
    }
    if (iteration.isPresent()) {
      iterationLocation = getLocation(iteration.orElseThrow());
      loopIterationStatement.put(loc, iterationLocation);
    }
    if (controllingExpressionLocation != null
        || initializerLocation != null
        || iterationLocation != null) {
      FileLocation parenthesesBlockLocation =
          FileLocation.merge(
              FluentIterable.of(
                      controllingExpressionLocation, initializerLocation, iterationLocation)
                  .filter(Objects::nonNull)
                  .toList());
      loopParenthesesBlock.put(loc, parenthesesBlockLocation);
    }
  }

  public ImmutableSet<FileLocation> getLoopLocations() {
    return loopLocations.build();
  }

  public ImmutableSet<FileLocation> getIfLocations() {
    return ifLocations.build();
  }

  public ImmutableMap<FileLocation, FileLocation> getLoopControllingExpression() {
    return loopControllingExpression.buildOrThrow();
  }

  public ImmutableMap<FileLocation, FileLocation> getLoopInitializer() {
    return loopInitializer.buildOrThrow();
  }

  public ImmutableMap<FileLocation, FileLocation> getLoopIterationStatement() {
    return loopIterationStatement.buildOrThrow();
  }

  public ImmutableMap<FileLocation, FileLocation> getLoopParenthesesBlock() {
    return loopParenthesesBlock.buildOrThrow();
  }

  public ImmutableMap<FileLocation, FileLocation> getLoopBody() {
    return loopBody.buildOrThrow();
  }

  public ImmutableMap<FileLocation, FileLocation> getIfCondition() {
    return ifCondition.buildOrThrow();
  }

  public ImmutableMap<FileLocation, FileLocation> getIfThenClause() {
    return ifThenClause.buildOrThrow();
  }

  public ImmutableMap<FileLocation, FileLocation> getIfElseClause() {
    return ifElseClause.buildOrThrow();
  }

  public ImmutableSet<FileLocation> getDeclarationLocations() {
    return compoundLocations.build();
  }

  public ImmutableSet<FileLocation> getStatementLocations() {
    return statementLocations.build();
  }
}
