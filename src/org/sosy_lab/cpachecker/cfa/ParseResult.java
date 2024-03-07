// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.TreeMultimap;
import java.nio.file.Path;
import java.util.List;
import java.util.NavigableMap;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.util.SyntacticBlock;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.ast.ASTStructure;

/**
 * Class representing the result of parsing a C file before function calls are bound to their
 * targets.
 *
 * <p>It consists of a map that stores the CFAs for each function and a list of declarations of
 * global variables.
 *
 * <p>This class is immutable, but it does not ensure that it's content also is. It is recommended
 * to use it only as a "transport" data class, not for permanent storage.
 */
public class ParseResult {

  private Optional<ASTStructure> astStructure = Optional.empty();

  private Optional<List<FileLocation>> commentLocations = Optional.empty();
  private Optional<List<SyntacticBlock>> blocks = Optional.empty();

  private final NavigableMap<String, FunctionEntryNode> functions;

  private final TreeMultimap<String, CFANode> cfaNodes;

  private final List<Pair<ADeclaration, String>> globalDeclarations;

  private final List<Path> fileNames;

  public ParseResult(
      NavigableMap<String, FunctionEntryNode> pFunctions,
      TreeMultimap<String, CFANode> pCfaNodes,
      List<Pair<ADeclaration, String>> pGlobalDeclarations,
      List<Path> pFileNames) {
    functions = pFunctions;
    cfaNodes = pCfaNodes;
    globalDeclarations = pGlobalDeclarations;
    fileNames = ImmutableList.copyOf(pFileNames);
  }

  public boolean isEmpty() {
    return functions.isEmpty();
  }

  public NavigableMap<String, FunctionEntryNode> getFunctions() {
    return functions;
  }

  public TreeMultimap<String, CFANode> getCFANodes() {
    return cfaNodes;
  }

  public ImmutableSet<CFAEdge> getEdges() {
    return FluentIterable.from(cfaNodes.values())
        .transformAndConcat(CFAUtils::allLeavingEdges)
        .toSet();
  }

  public List<Pair<ADeclaration, String>> getGlobalDeclarations() {
    return globalDeclarations;
  }

  public List<Path> getFileNames() {
    return fileNames;
  }

  public Optional<ASTStructure> getASTStructure() {
    return astStructure;
  }

  public void setASTStructure(ASTStructure pAstStructure) {
    astStructure = Optional.of(pAstStructure);
  }

  public Optional<List<FileLocation>> getCommentLocations() {
    return commentLocations;
  }

  public void setCommentLocations(List<FileLocation> pCommentLocations) {
    commentLocations = Optional.of(pCommentLocations);
  }

  public Optional<List<SyntacticBlock>> getBlocks() {
    return blocks;
  }

  public void setBlocks(List<SyntacticBlock> pBlocks) {
    blocks = Optional.of(pBlocks);
  }
}
