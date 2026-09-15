// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib;

import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.ast.java.JAstNodeVisitor;

/**
 * This is the base interface for all AST nodes in the SV-LIB language. Note that this represents
 * only the classes which can appear in the CFA inside of edges or as part of specifications. In
 * case you want the full AST including all syntactic constructs of SV-LIB, please refer to the
 * parser module, and in particular {@link
 * org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibParsingAstNode}.
 *
 * <p>VERY IMPORTANT: Do not use {@link
 * org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibParsingAstNode} anywhere in the CFA or to
 * represent some information in this package. The parsing AST nodes are only to be used during
 * parsing and initial processing of SV-LIB files. After that, all relevant information must be
 * converted into the AST nodes defined in this package. This is crucial to ensure that the CFA
 * remains independent of the parsing process and does not carry unnecessary syntactic details.
 *
 * <p>It extends the generic {@link AAstNode} interface and adds a method to accept visitors
 * specific to SV-LIB AST nodes.
 *
 * <p>For an overview of the SV-LIB language and its constructs, please refer to the SV-LIB
 * specification: https://gitlab.com/sosy-lab/benchmarking/sv-lib
 *
 * <p>Also see https://gitlab.com/sosy-lab/software/cpachecker/-/merge_requests/323 for a lot more *
 * discussion on the design choices for the intergration of SV-LIB into CPAchecker.
 */
public interface SvLibAstNode extends AAstNode {

  <R, X extends Exception> R accept(SvLibAstNodeVisitor<R, X> v) throws X;

  @Deprecated // Call accept() directly
  @SuppressWarnings("unchecked") // should not be necessary, but javac complains otherwise
  @Override
  default <
          R,
          R1 extends R,
          R2 extends R,
          R3 extends R,
          X1 extends Exception,
          X2 extends Exception,
          X3 extends Exception,
          V extends CAstNodeVisitor<R1, X1> & JAstNodeVisitor<R2, X2> & SvLibAstNodeVisitor<R3, X3>>
      R accept_(V pV) throws X3 {
    return accept(pV);
  }
}
