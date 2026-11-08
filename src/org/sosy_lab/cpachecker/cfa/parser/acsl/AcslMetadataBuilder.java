// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.acsl;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.SetMultimap;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ParseResult;
import org.sosy_lab.cpachecker.cfa.ast.AbstractSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslLogicDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AAcslAnnotation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslAssertion;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslBehavior;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslFunctionContract;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslLogicDefinitionAnnotation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslLoopAnnotation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslStatementContract;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.acsl.AcslMetadata;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.parser.acsl.AcslComment.AcslCommentType;
import org.sosy_lab.cpachecker.cfa.parser.acsl.AcslMetadataException.AcslMetadataCreationException;
import org.sosy_lab.cpachecker.cfa.parser.acsl.AcslMetadataException.AcslNodeMappingException;
import org.sosy_lab.cpachecker.cfa.parser.acsl.AcslNodeMappingUtils.StatementNodes;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.AcslParser;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.AcslParser.AcslParseException;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.AcslScope;
import org.sosy_lab.cpachecker.util.Pair;

/** Creates the ACSL metadata of a CFA from the ACSL comments of the program. */
public final class AcslMetadataBuilder {

  private AcslMetadataBuilder() {}

  /**
   * Creates the acsl metadata for a CFA from its parse result. Parses all acsl comments into their
   * corresponding data structures. Maps assertions, loop annotations and function contracts to CFA
   * nodes.
   *
   * @param pCFA The CFA for the current source program.
   * @param pResult The parse result which contains all acsl comments and their locations in the
   *     source file
   * @param logger The logger for warnings about the annotations
   * @return The acsl metadata from all acsl comments for the CFA.
   */
  public static AcslMetadata createAcslMetadata(CFA pCFA, ParseResult pResult, LogManager logger) {
    CProgramScope programScope = new CProgramScope(pCFA, logger);
    CProgramScope globalScope =
        programScope.withVariablesInScope(
            FluentIterable.from(pResult.globalDeclarations())
                .transform(Pair::getFirst)
                .filter(CVariableDeclaration.class)
                .toSet());

    AcslScope acslScope = AcslScope.mutableCopy(AcslScope.empty());
    ImmutableList<AcslComment> allComments =
        ImmutableList.copyOf(
            pResult
                .acslComments()
                .orElseThrow(
                    () ->
                        new AcslMetadataCreationException(
                            "The parse result has no acsl comments.")));

    ImmutableSet.Builder<AcslLogicDeclaration> globalDeclarationBuilder = ImmutableSet.builder();
    ImmutableSetMultimap.Builder<@NonNull CFANode, AcslAssertion> assertionBuilder =
        ImmutableSetMultimap.builder();
    ImmutableSetMultimap.Builder<@NonNull CFANode, AcslLoopAnnotation> loopAnnotationBuilder =
        ImmutableSetMultimap.builder();
    ImmutableSetMultimap.Builder<@NonNull CFANode, AcslFunctionContract> functionContractBuilder =
        ImmutableSetMultimap.builder();
    ImmutableSetMultimap.Builder<@NonNull CFANode, AcslStatementContract> statementContractBuilder =
        ImmutableSetMultimap.builder();
    ImmutableSetMultimap.Builder<@NonNull CFANode, AcslStatementContract>
        statementContractEndBuilder = ImmutableSetMultimap.builder();
    // The behaviors that statement contracts can refer to with "for"
    SetMultimap<String, String> behaviorsOfFunctions = HashMultimap.create();
    SetMultimap<FileLocation, String> behaviorsOfStatements = HashMultimap.create();

    for (AcslComment comment : allComments) {
      CProgramScope scopeForNode = globalScope;
      AcslCommentType commentType;
      Optional<CFANode> n = Optional.empty();

      try {
        commentType = AcslParser.acslCommentToCommentType(comment.getComment());
      } catch (AcslParseException e) {
        throw new AcslMetadataCreationException(e.getMessage());
      }

      if (commentType == AcslCommentType.UNKNOWN) {
        throw new AcslMetadataCreationException("Acsl comment " + comment + " is of unknown type.");
      }

      if (commentType != AcslCommentType.LOGIC_DEF) {
        // For other annotations that are not logic definitions we need to do the node mapping
        // before
        // the parsing.
        CFANode currentNode;
        try {
          n = AcslNodeMappingUtils.addAcslToNodeMapping(comment, pCFA);
        } catch (AcslParseException e) {
          throw new AcslNodeMappingException(
              "Could not map acsl annotation " + comment + " to node: " + e);
        }
        currentNode =
            n.orElseThrow(
                () ->
                    new AcslMetadataException(
                        "Acsl function contract at " + comment.fileLocation()));
        // Only the variables and parameters in scope at the node are visible
        Optional<FluentIterable<AbstractSimpleDeclaration>> variablesInScope =
            pCFA.getAstCfaRelation().getVariablesAndParametersInScope(currentNode);
        if (variablesInScope.isPresent()) {
          FluentIterable<CSimpleDeclaration> visibleVariables =
              variablesInScope.orElseThrow().filter(CSimpleDeclaration.class);
          if (commentType == AcslCommentType.STATEMENT_CONTRACT
              || (commentType == AcslCommentType.FUNCTION_CONTRACT
                  && AcslNodeMappingUtils.isInsideFunction(comment, pCFA))) {
            // A statement contract can also refer to the variables that its statement declares
            visibleVariables =
                visibleVariables.append(
                    FluentIterable.from(
                            AcslNodeMappingUtils.nodesForStatementContract(comment, pCFA).edges())
                        .filter(CDeclarationEdge.class)
                        .transform(CDeclarationEdge::getDeclaration)
                        .filter(CVariableDeclaration.class));
          }
          scopeForNode = programScope.withVariablesInScope(visibleVariables.toSet());
        } else {
          logger.log(
              Level.WARNING,
              "Could not find out which variables are in scope for the CFA Node "
                  + currentNode
                  + " while parsing the ACSL annotations corresponding to it");
        }
        scopeForNode = scopeForNode.withFunctionScope(currentNode.getFunctionName());
      }

      AAcslAnnotation annotation;
      try {
        annotation =
            AcslParser.parseAcslComment(
                comment.getComment(), comment.fileLocation(), scopeForNode, acslScope);
      } catch (AcslParseException e) {
        throw new AcslMetadataCreationException(
            "Could not parse acsl annotation " + comment + ": " + e);
      }

      if (annotation instanceof AcslFunctionContract contract
          && AcslNodeMappingUtils.isInsideFunction(comment, pCFA)) {
        annotation =
            new AcslStatementContract(contract.getFileLocation(), ImmutableList.of(), contract);
      }

      switch (annotation) {
        case AcslStatementContract pStatementContract -> {
          StatementNodes nodes = AcslNodeMappingUtils.nodesForStatementContract(comment, pCFA);
          String functionName = nodes.startNodes().iterator().next().getFunctionName();
          Set<String> declaredBehaviors =
              FluentIterable.from(behaviorsOfStatements.entries())
                  .filter(entry -> contains(entry.getKey(), comment.fileLocation()))
                  .transform(Entry::getValue)
                  .append(behaviorsOfFunctions.get(functionName))
                  .toSet();
          if (!declaredBehaviors.containsAll(pStatementContract.getEnclosingBehaviors())) {
            logger.log(
                Level.WARNING,
                "Ignoring ACSL statement contract",
                comment,
                "because it refers to behaviors that are not declared by enclosing contracts.");
            continue;
          }
          for (CFANode start : nodes.startNodes()) {
            statementContractBuilder.put(start, pStatementContract);
          }
          for (CFANode end : nodes.endNodes()) {
            statementContractEndBuilder.put(end, pStatementContract);
          }
          behaviorsOfStatements.putAll(
              nodes.statementLocation(),
              Lists.transform(
                  pStatementContract.getContract().getBehaviors(), AcslBehavior::getName));
        }
        case AcslAssertion pAssertion ->
            assertionBuilder.put(
                n.orElseThrow(
                    () ->
                        new AcslMetadataCreationException(
                            "Annotation at " + comment.fileLocation() + " has no CFA node.")),
                pAssertion);
        case AcslLoopAnnotation pLoopAnnotation ->
            loopAnnotationBuilder.put(
                n.orElseThrow(
                    () ->
                        new AcslMetadataCreationException(
                            "Annotation at " + comment.fileLocation() + " has no CFA node.")),
                pLoopAnnotation);
        case AcslFunctionContract pFunctionContract -> {
          CFANode entry =
              n.orElseThrow(
                  () ->
                      new AcslMetadataCreationException(
                          "Annotation at " + comment.fileLocation() + " has no CFA node."));
          functionContractBuilder.put(entry, pFunctionContract);
          behaviorsOfFunctions.putAll(
              entry.getFunctionName(),
              Lists.transform(pFunctionContract.getBehaviors(), AcslBehavior::getName));
        }
        case AcslLogicDefinitionAnnotation pLogicDef ->
            globalDeclarationBuilder.add(pLogicDef.getDeclaration());
        case null ->
            throw new AcslMetadataCreationException(
                "Annotation for acsl comment " + comment + " is null.");
        default ->
            throw new AcslMetadataCreationException(
                "Unexpected annotation type: "
                    + comment
                    + ". Only assertions, loop annotations, function contracts, and"
                    + " statement contracts are supported.");
      }
    }

    ImmutableSet<AcslLogicDeclaration> globalDeclarations = globalDeclarationBuilder.build();
    ImmutableSetMultimap<CFANode, AcslAssertion> assertions = assertionBuilder.build();
    ImmutableSetMultimap<CFANode, AcslLoopAnnotation> loopAnnotations =
        loopAnnotationBuilder.build();
    ImmutableSetMultimap<CFANode, AcslFunctionContract> functionContracts =
        functionContractBuilder.build();

    return new AcslMetadata(
        globalDeclarations,
        assertions,
        loopAnnotations,
        functionContracts,
        statementContractBuilder.build(),
        statementContractEndBuilder.build());
  }

  /** Whether the location of a statement contains the given location. */
  private static boolean contains(FileLocation pOuter, FileLocation pInner) {
    return pOuter.getNodeOffset() <= pInner.getNodeOffset()
        && pInner.getNodeOffset() < pOuter.getNodeOffset() + pOuter.getNodeLength();
  }
}
