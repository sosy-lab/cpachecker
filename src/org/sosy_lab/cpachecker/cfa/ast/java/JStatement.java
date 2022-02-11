// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.java;

import org.sosy_lab.cpachecker.cfa.ast.AStatement;

/**
 * Interface of classes that represent java statements.
 *
 */
@SuppressWarnings("serial") // we cannot set a UID for an interface
public interface JStatement extends AStatement, JAstNode {

  <R, X extends Exception> R accept(JStatementVisitor<R, X> pV) throws X;

  @Override
  default <R, X extends Exception> R accept(JAstNodeVisitor<R, X> pV) throws X {
    return accept((JStatementVisitor<R, X>) pV);
  }
}
