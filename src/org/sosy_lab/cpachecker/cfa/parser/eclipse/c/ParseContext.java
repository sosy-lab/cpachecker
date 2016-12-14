/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cfa.parser.eclipse.c;

import static com.google.common.base.Verify.verify;

import com.google.common.base.CharMatcher;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Strings;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTProblem;
import org.eclipse.cdt.core.dom.ast.IASTProblemHolder;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.sosy_lab.cpachecker.cfa.CSourceOriginMapping;
import org.sosy_lab.cpachecker.cfa.CSourceOriginMapping.CodePosition;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;

class ParseContext {

  private static final CharMatcher SEMICOLON = CharMatcher.is(';');

  private final Function<String, String> niceFileNameFunction;

  private final CSourceOriginMapping sourceOriginMapping;

  ParseContext(
      Function<String, String> pNiceFileNameFunction, CSourceOriginMapping pSourceOriginMapping) {
    niceFileNameFunction = pNiceFileNameFunction;
    sourceOriginMapping = pSourceOriginMapping;
  }

  static ParseContext dummy() {
    return new ParseContext(Functions.identity(), new CSourceOriginMapping());
  }

  CFAGenerationRuntimeException parseError(String msg, CAstNode astNode) {
    throw new CFAGenerationRuntimeException(
        astNode == null
            ? msg
            : (astNode.getFileLocation() + ": " + msg + ": " + astNode.toASTString()));
  }

  CFAGenerationRuntimeException parseError(String msg, IASTNode node) {
    throw new CFAGenerationRuntimeException(node == null ? msg : createMessage(msg, node));
  }

  CFAGenerationRuntimeException parseError(IASTProblem problem) {
    throw new CFAGenerationRuntimeException(createMessage(problem.getMessage(), problem));
  }

  <P extends IASTProblemHolder & IASTNode> CFAGenerationRuntimeException parseError(P problem) {
    throw new CFAGenerationRuntimeException(
        createMessage(problem.getProblem().getMessage(), problem));
  }

  private String createMessage(String msg, IASTNode node) {
    StringBuilder sb = new StringBuilder();

    FileLocation fileLocation = getLocation(node);
    if (fileLocation != FileLocation.DUMMY) {
      sb.append(fileLocation);
      sb.append(": ");
    }

    if (Strings.isNullOrEmpty(msg)) {
      sb.append("Problem");
    } else {
      sb.append(msg);
    }
    sb.append(": ");

    String rawSignature = node.getRawSignature();
    sb.append(rawSignature);

    // search the ast node for the whole statement / declaration / line
    IASTNode fullLine = node;
    while ((fullLine != null)
        && !(fullLine instanceof IASTStatement)
        && !(fullLine instanceof IASTDeclaration)) {

      fullLine = fullLine.getParent();
    }

    if (fullLine != null && fullLine != node) {
      String lineRawSignature = fullLine.getRawSignature();

      String codeWithoutWhitespace = CharMatcher.whitespace().removeFrom(rawSignature);
      String lineWithoutWhitespace = CharMatcher.whitespace().removeFrom(lineRawSignature);

      // remove all whitespaces and trailing semicolons for comparison
      codeWithoutWhitespace = SEMICOLON.trimFrom(codeWithoutWhitespace);
      lineWithoutWhitespace = SEMICOLON.trimFrom(lineWithoutWhitespace);

      if (!codeWithoutWhitespace.equals(lineWithoutWhitespace)) {
        sb.append(" (full line is ");
        sb.append(lineRawSignature);
        sb.append(")");
      }
    }

    return sb.toString();
  }

  /** This function returns the converted file-location of an IASTNode. */
  FileLocation getLocation(final IASTNode n) {
    IASTFileLocation l = n.getFileLocation();

    if (l == null) {
      return FileLocation.DUMMY;
    }

    final String fileName = l.getFileName();
    final int startingLineInInput = l.getStartingLineNumber();
    final int endingLineInInput = l.getEndingLineNumber();

    final CodePosition startingInOrigin =
        sourceOriginMapping.getOriginLineFromAnalysisCodeLine(fileName, startingLineInInput);
    final int startingLineInOrigin = startingInOrigin.getLineNumber();

    final CodePosition endingInOrigin =
        sourceOriginMapping.getOriginLineFromAnalysisCodeLine(fileName, endingLineInInput);
    verify(
        startingInOrigin.getFileName().equals(endingInOrigin.getFileName()),
        "Unexpected token '%s' of class %s spanning files %s and %s",
        n.getRawSignature(),
        n.getClass().getSimpleName(),
        startingInOrigin.getFileName(),
        endingInOrigin.getFileName());
    final int endingLineInOrigin = endingInOrigin.getLineNumber();

    final String originFileName = startingInOrigin.getFileName();

    return new FileLocation(
        originFileName,
        mapFileNameToNameForHumans(originFileName),
        l.getNodeOffset(),
        l.getNodeLength(),
        startingLineInInput,
        endingLineInInput,
        startingLineInOrigin,
        endingLineInOrigin);
  }

  /**
   * Given a file name, returns a "nice" representation of it. This should be used for situations
   * where the name is going to be presented to the user (like in error messages). The result may be
   * the empty string, if for example CPAchecker only uses one file (we expect the user to know its
   * name in this case).
   */
  String mapFileNameToNameForHumans(String fileName) {
    return niceFileNameFunction.apply(fileName);
  }
}
