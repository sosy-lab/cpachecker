// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import com.google.common.base.Verify;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.TreeMultimap;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.acslDeprecated.util.SyntacticBlock;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.ast.AstCfaRelation;

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
public record ParseResult(
    NavigableMap<String, FunctionEntryNode> functions,
    TreeMultimap<String, CFANode> cfaNodes,
    List<Pair<ADeclaration, String>> globalDeclarations,
    List<Path> fileNames,
    Optional<AstCfaRelation> astStructure,
    Optional<List<FileLocation>> commentLocations,
    Optional<List<SyntacticBlock>> blocks,
    Optional<Map<CFANode, Set<AVariableDeclaration>>> cfaNodeToAstLocalVariablesInScope,
    Optional<Map<CFANode, Set<AParameterDeclaration>>> cfaNodeToAstParametersInScope) {

  public ParseResult(
      NavigableMap<String, FunctionEntryNode> pFunctions,
      TreeMultimap<String, CFANode> pCfaNodes,
      List<Pair<ADeclaration, String>> pGlobalDeclarations,
      List<Path> pFileNames) {
    this(
        pFunctions,
        pCfaNodes,
        pGlobalDeclarations,
        pFileNames,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty());
  }

  public ParseResult(
      NavigableMap<String, FunctionEntryNode> pFunctions,
      TreeMultimap<String, CFANode> pCfaNodes,
      List<Pair<ADeclaration, String>> pGlobalDeclarations,
      List<Path> pFileNames,
      List<FileLocation> pCommentLocations,
      List<SyntacticBlock> pBlocks) {
    this(
        pFunctions,
        pCfaNodes,
        pGlobalDeclarations,
        pFileNames,
        Optional.empty(),
        Optional.of(pCommentLocations),
        Optional.of(pBlocks),
        Optional.empty(),
        Optional.empty());
  }

  public boolean isEmpty() {
    return functions.isEmpty();
  }

  public ImmutableSet<CFAEdge> getCFAEdges() {
    return FluentIterable.from(cfaNodes.values())
        .transformAndConcat(CFAUtils::allLeavingEdges)
        .toSet();
  }

  public ParseResult withASTStructure(AstCfaRelation pAstCfaRelation) {
    Verify.verify(astStructure.isEmpty());
    return new ParseResult(
        functions,
        cfaNodes,
        globalDeclarations,
        fileNames,
        Optional.of(pAstCfaRelation),
        commentLocations,
        blocks,
        cfaNodeToAstLocalVariablesInScope,
        cfaNodeToAstParametersInScope);
  }

  public ParseResult withInScopeInformation(
      Map<CFANode, Set<AVariableDeclaration>> pCfaNodeToAstLocalVariablesInScope,
      Map<CFANode, Set<AParameterDeclaration>> pCfaNodeToAstParametersInScope) {
    Verify.verify(cfaNodeToAstLocalVariablesInScope.isEmpty());
    Verify.verify(cfaNodeToAstParametersInScope.isEmpty());
    return new ParseResult(
        functions,
        cfaNodes,
        globalDeclarations,
        fileNames,
        astStructure,
        commentLocations,
        blocks,
        Optional.of(pCfaNodeToAstLocalVariablesInScope),
        Optional.of(pCfaNodeToAstParametersInScope));
  }
}
