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
package org.sosy_lab.cpachecker.exceptions;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.cfa.ast.IASTNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;

import com.google.common.base.CharMatcher;

/**
 * Exception thrown when a CPA cannot handle some C code attached to a CFAEdge.
 */
public class UnrecognizedCCodeException extends CPATransferException {

  private static final String MESSAGE = "Unrecognized C code";

  private static final long serialVersionUID = -8319167530363457020L;

  protected UnrecognizedCCodeException(String msg1, String msg2, CFAEdge edge, IASTNode astNode) {
    super(createMessage(msg1, msg2, edge, astNode));
  }

  public UnrecognizedCCodeException(String msg2, CFAEdge edge, IASTNode astNode) {
    super(createMessage(MESSAGE, msg2, edge, astNode));
  }

  public UnrecognizedCCodeException(String msg2, CFAEdge edge) {
    super(createMessage(MESSAGE, msg2, edge, edge.getRawAST()));
  }

  public UnrecognizedCCodeException(CFAEdge edge, IASTNode astNode) {
    super(createMessage(MESSAGE, null, edge, astNode));
  }


  protected static String createMessage(String msg1, String msg2, CFAEdge edge, IASTNode astNode) {
    checkNotNull(msg1);
    if (astNode == null) {
      astNode = edge.getRawAST();
    }
    checkNotNull(astNode, "Either edge or astNode need to be given");

    String code = astNode.toASTString();
    String rawCode = edge.getRawStatement();

    StringBuilder sb = new StringBuilder();
    sb.append(msg1);
    if (msg2 != null) {
      sb.append(" (");
      sb.append(msg2);
      sb.append(")");
    }
    sb.append(" in line ");
    sb.append(edge.getLineNumber());
    sb.append(": ");
    sb.append(code);

    String codeWithoutWhitespace = CharMatcher.WHITESPACE.removeFrom(code);
    String rawCodeWithoutWhitespace = CharMatcher.WHITESPACE.removeFrom(rawCode);

    if (!codeWithoutWhitespace.equals(rawCodeWithoutWhitespace)) {
      sb.append(" (line was originally ");
      sb.append(rawCode);
      sb.append(")");
    }

    return sb.toString();
  }
}
