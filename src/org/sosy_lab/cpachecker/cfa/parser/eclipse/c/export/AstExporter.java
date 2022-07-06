// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.eclipse.c.export;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import org.eclipse.cdt.core.dom.ast.ASTGenericVisitor;
import org.eclipse.cdt.core.dom.ast.ExpansionOverlapsBoundaryException;
import org.eclipse.cdt.core.dom.ast.IASTComment;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.parser.IToken;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.c.export.CCfaEdgeStatement.GlobalDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.c.export.EclipseCWriter.FunctionExportInformation;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.c.export.EclipseCWriter.GlobalExportInformation;

class AstExporter extends ASTGenericVisitor {

  private final Appendable dest;

  private final GlobalExportInformation exportInfo;
  private final ImmutableSet<SimpleFileLocation> removedFileLocations;
  private final ImmutableMultimap<SimpleFileLocation, GlobalDeclaration>
      addedGlobalDeclarationsByFileLocation;
  private final ImmutableMap<SimpleFileLocation, FunctionExportInformation>
      functionInfoByFileLocation;

  private ImmutableMultimap<SimpleFileLocation, CCfaEdgeStatement>
      addedStatementsOfCurrentFunctionByFileLocation;
  private boolean headerOfCurrentFunctionChanged;

  private String originalCode;
  private List<SimpleFileLocation> allOriginalFileLocations;
  private SortedMap<SimpleFileLocation, IASTComment> remainingOriginalCommentsByFileLocation;
  private Set<SimpleFileLocation> fileLocationsOfExportedTokens;

  AstExporter(final Appendable pDestination, final GlobalExportInformation pExportInfo) {
    super(true);

    dest = pDestination;
    exportInfo = pExportInfo;
    removedFileLocations = collectRemovedFileLocations();
    addedGlobalDeclarationsByFileLocation = collectAddedGlobalDeclarationsByFileLocation();
    functionInfoByFileLocation = collectFunctionInfoByFileLocation();
  }

  void write(final String pString) throws IOException {
    dest.append(pString);
  }

  @Override
  protected int genericVisit(final IASTNode pNode) {
    final IASTFileLocation fileLoc = pNode.getFileLocation();

    if (fileLoc == null) {
      assert pNode.getRawSignature().isEmpty();
      return PROCESS_SKIP;
    }

    if (removedFileLocations.contains(SimpleFileLocation.convertToSimpleFileLocation(fileLoc))) {
      // TODO this is more complicated when statements can be replaced during CFA traversal
      return PROCESS_SKIP;
    }

    try {
      // TODO add potential new labels and omit removed labels

      final int nodeOffset = pNode.getFileLocation().getNodeOffset();
      exportSyntax(pNode.getLeadingSyntax(), nodeOffset);

      if (pNode.getChildren().length == 0) {
        exportSyntax(pNode.getSyntax(), nodeOffset);
      }

      return PROCESS_CONTINUE;

    } catch (final IOException | ExpansionOverlapsBoundaryException pE) {
      return PROCESS_ABORT;
    }
  }

  @Override
  protected int genericLeave(final IASTNode pNode) {
    final SimpleFileLocation fileLoc =
        SimpleFileLocation.convertToSimpleFileLocation(pNode.getFileLocation());

    try {
      if (addedStatementsOfCurrentFunctionByFileLocation != null
          && addedStatementsOfCurrentFunctionByFileLocation.containsKey(fileLoc)) {

        for (final CCfaEdgeStatement statement :
            addedStatementsOfCurrentFunctionByFileLocation.get(fileLoc)) {
          dest.append(statement.exportToCCode());
        }
      }

      exportSyntax(
          pNode.getTrailingSyntax(),
          pNode.getFileLocation().getNodeOffset() + pNode.getFileLocation().getNodeLength());
      return PROCESS_CONTINUE;

    } catch (final IOException | ExpansionOverlapsBoundaryException pE) {
      return PROCESS_ABORT;
    }
  }

  @Override
  public int visit(final IASTTranslationUnit pTranslationUnit) {

    try {
      originalCode = pTranslationUnit.getRawSignature();
      final ImmutableMap<SimpleFileLocation, IASTComment> originalCommentsByFileLocation =
          createMapOfOriginalComments(pTranslationUnit);
      final ImmutableMap<SimpleFileLocation, IToken> originalTokensByFileLocationOffset =
          createMapOfOriginalTokens(pTranslationUnit);
      allOriginalFileLocations =
          ImmutableList.<SimpleFileLocation>builder()
              .addAll(originalCommentsByFileLocation.keySet())
              .addAll(originalTokensByFileLocationOffset.keySet())
              .build()
              .stream()
              .sorted()
              .collect(ImmutableList.toImmutableList());
      remainingOriginalCommentsByFileLocation = new TreeMap<>(originalCommentsByFileLocation);
      fileLocationsOfExportedTokens = new TreeSet<>();

      assert !allOriginalFileLocations.isEmpty();
      final String leadingWhitespaceOfFirstFileLoc =
          getLeadingWhitespaceBeforeFileLocation(allOriginalFileLocations.get(0));
      dest.append(leadingWhitespaceOfFirstFileLoc);
      return PROCESS_CONTINUE;

    } catch (final IOException pE) {
      return PROCESS_ABORT;
    }
  }

  @Override
  public int leave(final IASTTranslationUnit pTranslationUnit) {
    return PROCESS_CONTINUE;
  }

  @Override
  public int visit(final IASTDeclaration pDeclaration) {

    if (pDeclaration instanceof IASTFunctionDefinition) {
      final SimpleFileLocation fileLoc =
          SimpleFileLocation.convertToSimpleFileLocation(pDeclaration.getFileLocation());

      if (!functionInfoByFileLocation.containsKey(fileLoc)) {
        // function is not part of the CFA anymore
        return PROCESS_SKIP;
      }

      final FunctionExportInformation functionInfo = functionInfoByFileLocation.get(fileLoc);
      assert functionInfo != null;
      addedStatementsOfCurrentFunctionByFileLocation =
          collectAddedStatementsByFileLocation(functionInfo);
      headerOfCurrentFunctionChanged = removedFileLocations.contains(fileLoc);

      if (headerOfCurrentFunctionChanged) {
        try {
          final String functionHeader =
              functionInfo
                  .getFunctionEntryNode()
                  .getFunctionDefinition()
                  .toASTString()
                  .replace(";", " ");
          dest.append(functionHeader);
          return PROCESS_CONTINUE;

        } catch (final IOException pE) {
          return PROCESS_ABORT;
        }
      }
    }

    return super.visit(pDeclaration);
  }

  @Override
  public int leave(final IASTDeclaration pDeclaration) {

    if (pDeclaration.getParent() instanceof IASTTranslationUnit) {
      // declaration is global

      final SimpleFileLocation fileLoc =
          SimpleFileLocation.convertToSimpleFileLocation(pDeclaration.getFileLocation());

      if (addedGlobalDeclarationsByFileLocation.containsKey(fileLoc)) {

        for (final GlobalDeclaration globalDeclaration :
            addedGlobalDeclarationsByFileLocation.get(fileLoc)) {

          try {
            dest.append(globalDeclaration.exportToCCode());

          } catch (IOException pE) {
            return PROCESS_ABORT;
          }
        }
      }
    }

    return super.leave(pDeclaration);
  }

  @Override
  public int visit(final IASTDeclSpecifier pDeclarationSpecifier) {

    if (pDeclarationSpecifier.getParent() instanceof IASTFunctionDefinition
        && headerOfCurrentFunctionChanged) {
      return PROCESS_SKIP;
    }

    return super.visit(pDeclarationSpecifier);
  }

  @Override
  public int visit(final IASTDeclarator pDeclarator) {

    if (pDeclarator.getParent() instanceof IASTFunctionDefinition
        && headerOfCurrentFunctionChanged) {
      return PROCESS_SKIP;
    }

    return super.visit(pDeclarator);
  }

  @Override
  public int visit(final IASTStatement pASTStatement) {

    if (pASTStatement instanceof IASTCompoundStatement
        && pASTStatement.getParent() instanceof IASTFunctionDefinition) {
      // the compound statement is the body of a function definition

      final SimpleFileLocation funcDefFileLoc =
          SimpleFileLocation.convertToSimpleFileLocation(
              pASTStatement.getParent().getFileLocation());
      assert functionInfoByFileLocation.containsKey(funcDefFileLoc)
          : "Function body should not have been visited";
      final FunctionExportInformation functionInfo = functionInfoByFileLocation.get(funcDefFileLoc);
      assert functionInfo != null;

      for (final CCfaEdgeStatement statement :
          functionInfo.getNewStatementsByFileLocation().get(Optional.empty())) {

        try {
          dest.append(statement.exportToCCode());

        } catch (final IOException pE) {
          return PROCESS_ABORT;
        }
      }
    }
    return super.visit(pASTStatement);
  }

  private void exportSyntax(final IToken pToken, final int pAstNodeOffset) throws IOException {
    IToken currentToken = pToken;

    while (currentToken != null) {
      final SimpleFileLocation tokenFileLoc =
          SimpleFileLocation.getSimpleFileLocationOfTokenWithOffset(currentToken, pAstNodeOffset);
      final Set<SimpleFileLocation> fileLocsOfExportedComments = new HashSet<>();

      for (final Map.Entry<SimpleFileLocation, IASTComment> commentWithFileLoc :
          remainingOriginalCommentsByFileLocation.entrySet()) {
        final SimpleFileLocation commentFileLoc = commentWithFileLoc.getKey();
        final IASTComment comment = commentWithFileLoc.getValue();

        if (commentFileLoc.compareTo(tokenFileLoc) > 0) {
          // this comment and all further comments appear after currentToken
          break;
        }

        fileLocsOfExportedComments.add(commentFileLoc);
        dest.append(comment.getRawSignature());

        final String trailingWhitespaceAfterComment =
            getTrailingWhitespaceAfterFileLocation(commentFileLoc);
        dest.append(trailingWhitespaceAfterComment);
      }

      for (final SimpleFileLocation fileLocOfExportedComment : fileLocsOfExportedComments) {
        remainingOriginalCommentsByFileLocation.remove(fileLocOfExportedComment);
      }

      if (fileLocationsOfExportedTokens.add(tokenFileLoc)) {
        dest.append(currentToken.getImage());

        final String trailingWhitespaceAfterToken =
            getTrailingWhitespaceAfterFileLocation(tokenFileLoc);
        dest.append(trailingWhitespaceAfterToken);
      }

      currentToken = currentToken.getNext();
    }
  }

  private String getLeadingWhitespaceBeforeFileLocation(final SimpleFileLocation pFileLoc) {
    final int index = allOriginalFileLocations.indexOf(pFileLoc);
    assert index >= 0;
    final int beginningOfLeadingWhitespace;

    if (index > 0) {
      final SimpleFileLocation previousFileLoc = allOriginalFileLocations.get(index - 1);
      beginningOfLeadingWhitespace = previousFileLoc.getEndOffset();

    } else {
      // pFileLoc is first file location
      beginningOfLeadingWhitespace = 0;
    }

    final String leadingWhitespace =
        originalCode.substring(beginningOfLeadingWhitespace, pFileLoc.getBeginningOffset());
    assert leadingWhitespace.isBlank();
    return leadingWhitespace;
  }

  private String getTrailingWhitespaceAfterFileLocation(final SimpleFileLocation pFileLoc) {
    final int index = allOriginalFileLocations.indexOf(pFileLoc);
    assert index >= 0;
    final int endOfTrailingWhitespace;

    if (index + 1 < allOriginalFileLocations.size()) {
      final SimpleFileLocation nextFileLoc = allOriginalFileLocations.get(index + 1);
      endOfTrailingWhitespace = nextFileLoc.getBeginningOffset();

    } else {
      // comment was at last file location
      endOfTrailingWhitespace = originalCode.length();
    }

    final String trailingWhitespace =
        originalCode.substring(pFileLoc.getEndOffset(), endOfTrailingWhitespace);
    assert trailingWhitespace.isBlank();
    return trailingWhitespace;
  }

  private ImmutableSet<SimpleFileLocation> collectRemovedFileLocations() {
    final ImmutableSet.Builder<SimpleFileLocation> builder = ImmutableSortedSet.naturalOrder();

    for (final CFAEdge edge : exportInfo.getTransformationRecords().getMissingEdges()) {
      builder.add(SimpleFileLocation.convertToSimpleFileLocation(edge.getFileLocation()));
    }

    return builder.build();
  }

  private ImmutableMultimap<SimpleFileLocation, GlobalDeclaration>
      collectAddedGlobalDeclarationsByFileLocation() {

    final Multimap<SimpleFileLocation, GlobalDeclaration> result =
        MultimapBuilder.treeKeys().arrayListValues().build();

    for (final Map.Entry<Optional<FileLocation>, GlobalDeclaration> entry :
        exportInfo.getNewGlobalDeclarationsByFileLocation().entries()) {

      final Optional<FileLocation> optFileLoc = entry.getKey();
      final GlobalDeclaration globalDeclaration = entry.getValue();

      if (optFileLoc.isEmpty()) {
        // these added GlobalDeclarations are handled elsewhere
        continue;
      }

      final SimpleFileLocation simpleFileLoc =
          SimpleFileLocation.convertToSimpleFileLocation(optFileLoc.orElseThrow());
      result.put(simpleFileLoc, globalDeclaration);
    }

    return ImmutableListMultimap.copyOf(result);
  }

  private ImmutableMap<SimpleFileLocation, FunctionExportInformation>
      collectFunctionInfoByFileLocation() {

    final ImmutableMap.Builder<SimpleFileLocation, FunctionExportInformation> builder =
        ImmutableSortedMap.naturalOrder();

    for (final Map.Entry<Optional<FileLocation>, FunctionExportInformation> entry :
        exportInfo.getFunctionsByFileLocation().entries()) {

      final Optional<FileLocation> optFileLoc = entry.getKey();
      final FunctionExportInformation functionInfo = entry.getValue();

      if (optFileLoc.isEmpty()) {
        // these functions are handled elsewhere
        continue;
      }

      final SimpleFileLocation simpleFileLoc =
          SimpleFileLocation.convertToSimpleFileLocation(optFileLoc.orElseThrow());
      builder.put(simpleFileLoc, functionInfo);
    }

    return builder.buildOrThrow();
  }

  private ImmutableMultimap<SimpleFileLocation, CCfaEdgeStatement>
      collectAddedStatementsByFileLocation(final FunctionExportInformation pFunctionInfo) {

    final Multimap<SimpleFileLocation, CCfaEdgeStatement> result =
        MultimapBuilder.treeKeys().arrayListValues().build();

    for (final Map.Entry<Optional<FileLocation>, CCfaEdgeStatement> entry :
        pFunctionInfo.getNewStatementsByFileLocation().entries()) {

      final Optional<FileLocation> optFileLoc = entry.getKey();
      final CCfaEdgeStatement statement = entry.getValue();

      if (optFileLoc.isEmpty()) {
        // these added CCfaEdgeStatements are handled elsewhere
        continue;
      }

      final SimpleFileLocation simpleFileLoc =
          SimpleFileLocation.convertToSimpleFileLocation(optFileLoc.orElseThrow());
      result.put(simpleFileLoc, statement);
    }

    return ImmutableListMultimap.copyOf(result);
  }

  private static ImmutableMap<SimpleFileLocation, IASTComment> createMapOfOriginalComments(
      final IASTTranslationUnit pOriginalAst) {

    final ImmutableSortedMap.Builder<SimpleFileLocation, IASTComment> builder =
        ImmutableSortedMap.naturalOrder();

    for (final IASTComment comment : pOriginalAst.getComments()) {
      final SimpleFileLocation fileLoc =
          SimpleFileLocation.convertToSimpleFileLocation(comment.getFileLocation());
      builder.put(fileLoc, comment);
    }

    return builder.buildOrThrow();
  }

  private static ImmutableMap<SimpleFileLocation, IToken> createMapOfOriginalTokens(
      final IASTTranslationUnit pOriginalAst) {

    final ImmutableSortedMap.Builder<SimpleFileLocation, IToken> builder =
        ImmutableSortedMap.naturalOrder();

    try {
      IToken token = pOriginalAst.getSyntax();

      while (token != null) {
        final SimpleFileLocation fileLoc =
            SimpleFileLocation.getSimpleFileLocationOfTokenWithOffset(
                token, pOriginalAst.getFileLocation().getNodeOffset());
        builder.put(fileLoc, token);
        token = token.getNext();
      }

    } catch (ExpansionOverlapsBoundaryException pE) {
      throw new AssertionError();
    }

    return builder.buildOrThrow();
  }

  private static class SimpleFileLocation implements Comparable<SimpleFileLocation> {

    private final int beginningOffset;
    private final int endOffset;

    private SimpleFileLocation(final int pBeginningOffset, final int pEndOffset) {
      beginningOffset = pBeginningOffset;
      endOffset = pEndOffset;
    }

    private static SimpleFileLocation convertToSimpleFileLocation(final FileLocation pFileLoc) {
      final int beginningOffset = pFileLoc.getNodeOffset();
      final int endOffset = beginningOffset + pFileLoc.getNodeLength();
      return new SimpleFileLocation(beginningOffset, endOffset);
    }

    // TODO maybe it would be better to convert IASTFileLocations to FileLocations instead of using
    //  SimpleFileLocations
    private static SimpleFileLocation convertToSimpleFileLocation(final IASTFileLocation pFileLoc) {
      final int beginningOffset = pFileLoc.getNodeOffset();
      final int endOffset = beginningOffset + pFileLoc.getNodeLength();
      return new SimpleFileLocation(beginningOffset, endOffset);
    }

    private static SimpleFileLocation getSimpleFileLocationOfTokenWithOffset(
        final IToken pToken, final int pAstNodeOffset) {

      final int beginningOffset = pAstNodeOffset + pToken.getOffset();
      final int endOffset = pAstNodeOffset + pToken.getEndOffset();
      return new SimpleFileLocation(beginningOffset, endOffset);
    }

    private int getBeginningOffset() {
      return beginningOffset;
    }

    private int getEndOffset() {
      return endOffset;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 7;
      result = prime * result + beginningOffset;
      result = prime * result + endOffset;
      return result;
    }

    @Override
    public boolean equals(final Object pOther) {
      if (this == pOther) {
        return true;
      }

      if (!(pOther instanceof SimpleFileLocation)) {
        return false;
      }

      final SimpleFileLocation other = (SimpleFileLocation) pOther;
      return other.beginningOffset == beginningOffset && other.endOffset == endOffset;
    }

    @Override
    public int compareTo(final SimpleFileLocation pOther) {
      return ComparisonChain.start()
          .compare(beginningOffset, pOther.beginningOffset)
          .compare(endOffset, pOther.endOffset)
          .result();
    }

    @Override
    public String toString() {
      return "[" + beginningOffset + "," + endOffset + "]";
    }
  }
}
