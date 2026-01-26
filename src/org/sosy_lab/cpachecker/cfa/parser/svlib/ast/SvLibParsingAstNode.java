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

/**
 * This is the base interface for all AST nodes in the SV-LIB language which are present during the
 * parsing. Note that this represents all syntactic classes which can appear in an SV-LIB program
 * but not all the classes which appear in the CFA.In case you want the classes which can appear in
 * the CFA inside * of edges or as part of specifications ast module, and in particular {@link
 * org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibAstNode}.
 *
 * <p>VERY IMPORTANT: Do not use {@link
 * org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibParsingAstNode} anywhere in the CFA or to
 * represent some information relevant for an analysis. The parsing AST nodes are only to be used
 * during parsing and initial processing of SV-LIB files or for the export of witnesses or program
 * transformation. For an analysis, all relevant information must be converted into {@link
 * org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibAstNode} classes. This is crucial to ensure that the
 * CFA remains independent of the parsing process and does not carry unnecessary syntactic details.
 *
 * <p>For an overview of the SV-LIB language and its constructs, please refer to the SV-LIB
 * specification: https://gitlab.com/sosy-lab/benchmarking/sv-lib
 *
 * <p>Also see https://gitlab.com/sosy-lab/software/cpachecker/-/merge_requests/323 for a lot more
 * discussion on the design choices for the intergration of SV-LIB into CPAchecker.
 */
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
