// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.ast.java.JAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibAstNodeVisitor;

public sealed interface AcslAstNode extends AAstNode
    permits AcslLabel,
        AcslLogicDefinition,
        AcslMemoryLocationSet,
        AcslPredicate,
        AcslSimpleDeclaration,
        AcslTerm {

  <R, X extends Exception> R accept(AcslAstNodeVisitor<R, X> v) throws X;

  @Deprecated // Call accept() directly
  @SuppressWarnings("unchecked") // should not be necessary, but javac complains otherwise
  @Override
  default <
          R,
          R1 extends R,
          R2 extends R,
          R3 extends R,
          R4 extends R,
          X1 extends Exception,
          X2 extends Exception,
          X3 extends Exception,
          X4 extends Exception,
          V extends
              CAstNodeVisitor<R1, X1> & JAstNodeVisitor<R2, X2> & SvLibAstNodeVisitor<R3, X3>
                  & AcslAstNodeVisitor<R4, X4>>
      R accept_(V pV) throws X4 {
    return accept(pV);
  }
}
