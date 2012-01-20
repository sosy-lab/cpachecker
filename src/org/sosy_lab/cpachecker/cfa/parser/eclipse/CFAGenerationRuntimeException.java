/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.parser.eclipse;

import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTProblemHolder;
import org.eclipse.cdt.core.dom.ast.IASTStatement;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;

/**
 * Handles problems during CFA generation
 */
public class CFAGenerationRuntimeException extends RuntimeException {

  private static final long serialVersionUID = 6850681425709171716L;

  public CFAGenerationRuntimeException(String msg) {
    super(msg);
  }

  public CFAGenerationRuntimeException(Throwable cause) {
    super(cause.getMessage(), cause);
  }

  public CFAGenerationRuntimeException(String msg, IASTNode astNode) {
    this(astNode == null ? msg : createMessage(msg, astNode));
  }

  public CFAGenerationRuntimeException(String msg, org.sosy_lab.cpachecker.cfa.ast.IASTNode astNode) {
    this(astNode == null ? msg :
      (msg + " in line " + astNode.getFileLocation().getStartingLineNumber()
          + ": " + astNode.toASTString()));
  }

  public <P extends IASTProblemHolder & IASTNode> CFAGenerationRuntimeException(P problem) {
    this(createMessage(problem.getProblem().getMessage(), problem));
  }

  private static String createMessage(String msg, IASTNode node) {
    // search the ast node for the whole statement / declaration / line
    IASTNode fullLine = node;
    while ((fullLine != null)
        && !(fullLine instanceof IASTStatement)
        && !(fullLine instanceof IASTDeclaration)) {

      fullLine = fullLine.getParent();
    }

    String rawSignature = node.getRawSignature();
    StringBuilder sb = new StringBuilder();
    if (Strings.isNullOrEmpty(msg)) {
      sb.append("Problem");
    } else {
      sb.append(msg);
    }
    sb.append(" in line ");
    sb.append(node.getFileLocation().getStartingLineNumber());
    sb.append(": ");
    sb.append(rawSignature);

    if (fullLine != null && fullLine != node) {
      String lineRawSignature = fullLine.getRawSignature();
      String codeWithoutWhitespace = CharMatcher.WHITESPACE.removeFrom(rawSignature);
      String lineWithoutWhitespace = CharMatcher.WHITESPACE.removeFrom(lineRawSignature);

      if (!codeWithoutWhitespace.equals(lineWithoutWhitespace)) {
        sb.append(" (full line is ");
        sb.append(lineRawSignature);
        sb.append(")");
      }
    }

    return sb.toString();
  }
}
