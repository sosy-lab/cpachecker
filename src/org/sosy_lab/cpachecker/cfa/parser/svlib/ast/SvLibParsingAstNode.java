// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib.ast;

import java.io.Serializable;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public interface SvLibParsingAstNode extends Serializable {

  FileLocation getFileLocation();

  /**
   * Constructs a String representation of the AST represented by this node.
   *
   * @return AST string either using qualified names or pure names for local variables
   */
  String toASTString();

  <R, X extends Exception> R accept(SvLibParsingAstNodeVisitor<R, X> v) throws X;
}
