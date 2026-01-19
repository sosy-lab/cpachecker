// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import com.google.common.base.Verify;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.TreeMultimap;
import java.nio.file.Path;
import java.util.List;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.AcslComment;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.AcslMetadata;
import org.sosy_lab.cpachecker.cfa.ast.acslDeprecated.util.SyntacticBlock;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.svlib.SvLibCfaMetadata;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.ast.AstCfaRelation;

/**
 * Class representing the result of parsing a C file before function calls are bound to their
 * targets.
 *
 * <p>It consists of a map that stores the CFAs for each function and a list of declarations of
 * global variables.
 *
 * <p>This class is immutable, but it does not ensure that its content also is. It is recommended to
 * use it only as a "transport" data class, not for permanent storage.
 */
public record ParseResult(
    NavigableMap<String, FunctionEntryNode> functions,
    TreeMultimap<String, CFANode> cfaNodes,
    List<Pair<ADeclaration, String>> globalDeclarations,
    List<Path> fileNames,
    Optional<AstCfaRelation> astStructure,
    Optional<List<AcslComment>> acslComments,
    Optional<AcslMetadata> acslMetadata,
    Optional<List<SyntacticBlock>> blocks,
    Optional<ImmutableMap<CFANode, Set<AVariableDeclaration>>> cfaNodeToAstLocalVariablesInScope,
    Optional<ImmutableMap<CFANode, Set<AParameterDeclaration>>> cfaNodeToAstParametersInScope,
    // Only relevant for SV-LIB scripts
    Optional<SvLibCfaMetadata> svLibCfaMetadata) {

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
        Optional.empty(),
        Optional.empty(),
        Optional.empty());
  }

  public ParseResult(
      NavigableMap<String, FunctionEntryNode> pFunctions,
      TreeMultimap<String, CFANode> pCfaNodes,
      List<Pair<ADeclaration, String>> pGlobalDeclarations,
      List<Path> pFileNames,
      List<AcslComment> pAcslComments,
      AcslMetadata pAcslMetadata,
      List<SyntacticBlock> pBlocks) {
    this(
        pFunctions,
        pCfaNodes,
        pGlobalDeclarations,
        pFileNames,
        Optional.empty(),
        Optional.of(pAcslComments),
        Optional.of(pAcslMetadata),
        Optional.of(pBlocks),
        Optional.empty(),
        Optional.empty(),
        Optional.empty());
  }

  public ParseResult(
      NavigableMap<String, FunctionEntryNode> pFunctions,
      TreeMultimap<String, CFANode> pCfaNodes,
      List<Pair<ADeclaration, String>> pGlobalDeclarations,
      List<Path> pFileNames,
      SvLibCfaMetadata pSvLibCfaMetadata) {
    this(
        pFunctions,
        pCfaNodes,
        pGlobalDeclarations,
        pFileNames,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.of(pSvLibCfaMetadata));
  }

  public boolean isEmpty() {
    return functions.isEmpty();
  }

  public ParseResult withASTStructure(AstCfaRelation pAstCfaRelation) {
    Verify.verify(astStructure.isEmpty());
    return new ParseResult(
        functions,
        cfaNodes,
        globalDeclarations,
        fileNames,
        Optional.of(pAstCfaRelation),
        acslComments,
        acslMetadata,
        blocks,
        cfaNodeToAstLocalVariablesInScope,
        cfaNodeToAstParametersInScope,
        svLibCfaMetadata);
  }

  public ParseResult withInScopeInformation(
      ImmutableMap<CFANode, Set<AVariableDeclaration>> pCfaNodeToAstLocalVariablesInScope,
      ImmutableMap<CFANode, Set<AParameterDeclaration>> pCfaNodeToAstParametersInScope) {
    Verify.verify(cfaNodeToAstLocalVariablesInScope.isEmpty());
    Verify.verify(cfaNodeToAstParametersInScope.isEmpty());
    return new ParseResult(
        functions,
        cfaNodes,
        globalDeclarations,
        fileNames,
        astStructure,
        acslComments,
        acslMetadata,
        blocks,
        Optional.of(pCfaNodeToAstLocalVariablesInScope),
        Optional.of(pCfaNodeToAstParametersInScope),
        svLibCfaMetadata);
  }

  public ParseResult withFileNames(List<Path> pFileNames) {
    return new ParseResult(
        functions,
        cfaNodes,
        globalDeclarations,
        pFileNames,
        astStructure,
        acslComments,
        acslMetadata,
        blocks,
        cfaNodeToAstLocalVariablesInScope,
        cfaNodeToAstParametersInScope,
        svLibCfaMetadata);
  }

  public ParseResult withAcslComments(
      List<AcslComment> pAcslComments, List<SyntacticBlock> pBlocks) {
    return new ParseResult(
        functions,
        cfaNodes,
        globalDeclarations,
        fileNames,
        astStructure,
        Optional.of(pAcslComments),
        acslMetadata,
        Optional.of(pBlocks),
        cfaNodeToAstLocalVariablesInScope,
        cfaNodeToAstParametersInScope,
        Optional.empty());
  }

  public ParseResult withAcslMetadata(AcslMetadata pAcslMetadata) {
    Verify.verify(acslMetadata.isEmpty());
    return new ParseResult(
        functions,
        cfaNodes,
        globalDeclarations,
        fileNames,
        astStructure,
        acslComments,
        Optional.of(pAcslMetadata),
        blocks,
        cfaNodeToAstLocalVariablesInScope,
        cfaNodeToAstParametersInScope,
        Optional.empty());
  }
}
