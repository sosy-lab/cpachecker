// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.eclipse.java;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.sosy_lab.cpachecker.cfa.ast.java.JAstNode;

/** Handles problems during CFA generation for Java program inputs */
class CFAGenerationRuntimeException extends RuntimeException {

  private static final long serialVersionUID = 6850281425209171013L;

  private static final CharMatcher SEMICOLON = CharMatcher.is(';');

  /**
   * Creates a new <code>CFAGenerationRuntimeException</code> with the provided detail message.
   *
   * @param msg the message the exception should save as detail message for later use
   * @see RuntimeException#RuntimeException(String)
   */
  public CFAGenerationRuntimeException(String msg) {
    super(msg);
  }

  /**
   * Creates a new <code>CFAGenerationRuntimeException</code> with the given cause of the exception.
   *
   * <p>This is useful for wrapping more detailed exceptions or errors.
   *
   * @param cause the cause this exception wraps
   * @see RuntimeException#RuntimeException(Throwable)
   */
  public CFAGenerationRuntimeException(Throwable cause) {
    super(cause.getMessage(), cause);
  }

  /**
   * Creates a new <code>CFAGenerationRuntimeException</code> with the provided detail message and
   * the given cause.
   *
   * @param message the detail message to save for later use
   * @param cause the cause of this exception
   * @see RuntimeException#RuntimeException(String, Throwable)
   */
  public CFAGenerationRuntimeException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Creates a new <code>CFAGenerationRuntimeException</code> with the provided message and a
   * detailed description of the given node as detail message.
   *
   * <p>The description of the given node follows the message in a new line.
   *
   * @param msg the message that precedes a description of the given node
   * @param astNode the node that will be described in this exception's message
   */
  public CFAGenerationRuntimeException(String msg, ASTNode astNode) {
    this(astNode == null ? msg : createMessage(msg, astNode));
  }

  /**
   * Creates a new <code>CFAGenerationRuntimeException</code> with the provided message and details
   * about the given {@link JAstNode} as detail message.
   *
   * @param msg the message that precedes a description of the given node
   * @param astNode the node that will be described in this exception's message
   */
  public CFAGenerationRuntimeException(String msg, JAstNode astNode) {
    this(
        astNode == null
            ? msg
            : (astNode.getFileLocation() + ": " + msg + ": " + astNode.toASTString()));
  }

  private static String createMessage(String msg, ASTNode node) {
    String rawSignature = node.toString();
    StringBuilder sb = new StringBuilder();

    sb.append("Line ");
    sb.append(node.getStartPosition());
    sb.append(": ");

    if (Strings.isNullOrEmpty(msg)) {
      sb.append("An unspecified problem occurred.");
    } else {
      sb.append(msg);
    }

    sb.append("\n");
    sb.append(rawSignature);

    // search the ast node for the whole statement / declaration / line
    ASTNode fullLine = node;
    while ((fullLine != null)
        && !(fullLine instanceof Statement)
        && !(fullLine instanceof BodyDeclaration)) {

      fullLine = fullLine.getParent();
    }

    if (fullLine != null && !fullLine.equals(node)) {
      String lineRawSignature = fullLine.toString();

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
}
