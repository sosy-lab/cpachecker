// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.ast;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

class ASTLocationClassifier extends ASTVisitor {
  final Map<Integer, FileLocation> statementOffsetsToLocations = new HashMap<>();
  final Set<FileLocation> compoundLocations = new HashSet<>();

  private final Set<FileLocation> labelLocations = new HashSet<>();
  private final Set<FileLocation> declarationLocations = new HashSet<>();
  Set<Integer> declarationStartOffsets = new HashSet<>();

  final Set<FileLocation> loopLocations = new HashSet<>();
  final Map<FileLocation, FileLocation> loopControllingExpression = new HashMap<>();
  final Map<FileLocation, FileLocation> loopInitializer = new HashMap<>();
  final Map<FileLocation, FileLocation> loopIterationStatement = new HashMap<>();
  final Map<FileLocation, FileLocation> loopParenthesesBlock = new HashMap<>();
  final Map<FileLocation, FileLocation> loopBody = new HashMap<>();

  final Set<FileLocation> ifLocations = new HashSet<>();
  final Map<FileLocation, FileLocation> ifCondition = new HashMap<>();
  final Map<FileLocation, FileLocation> ifThenClause = new HashMap<>();
  final Map<FileLocation, FileLocation> ifElseClause = new HashMap<>();

  private final Map<String, Path> fileNames = new HashMap<>();

  public ASTLocationClassifier() {
    super(true);
  }

  @SuppressWarnings("unused")
  public ASTLocationClassifier(List<Path> pFileNames) {
    super(true);
    indexFileNames(pFileNames);
  }

  public void update() {
    declarationStartOffsets =
        transformedImmutableSetCopy(declarationLocations, x -> x.getNodeOffset());
  }

  public void indexFileNames(List<Path> pFileNames) {
    for (Path path : pFileNames) {
      fileNames.put(path.getFileName().toString(), path);
    }
  }

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
    statementOffsetsToLocations.put(loc.getNodeOffset(), loc);
    return PROCESS_CONTINUE;
  }

  @Override
  public int visit(IASTDeclaration declaration) {
    declarationLocations.add(getLocation(declaration));
    return PROCESS_CONTINUE;
  }

  private FileLocation getLocation(IASTNode node) {
    IASTFileLocation iloc = node.getFileLocation();
    Path path = fileNames.getOrDefault(iloc.getFileName(), Path.of("#none#"));
    FileLocation loc =
        new FileLocation(
            path,
            iloc.getNodeOffset(),
            iloc.getNodeLength(),
            iloc.getStartingLineNumber(),
            iloc.getEndingLineNumber());
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
    FileLocation parenthesesBlockLocation = null;
    if (controllingExpression.isPresent()) {
      loopControllingExpression.put(loc, getLocation(controllingExpression.orElseThrow()));
      parenthesesBlockLocation = getLocation(controllingExpression.orElseThrow());
    }
    if (initializer.isPresent()) {
      parenthesesBlockLocation =
          FileLocationUtils.merge(parenthesesBlockLocation, getLocation(initializer.orElseThrow()));
      loopInitializer.put(loc, getLocation(initializer.orElseThrow()));
    }
    if (iteration.isPresent()) {
      parenthesesBlockLocation =
          FileLocationUtils.merge(parenthesesBlockLocation, getLocation(iteration.orElseThrow()));
      loopIterationStatement.put(loc, getLocation(iteration.orElseThrow()));
    }
    if (parenthesesBlockLocation != null) {
      loopParenthesesBlock.put(loc, parenthesesBlockLocation);
    }
  }
}
