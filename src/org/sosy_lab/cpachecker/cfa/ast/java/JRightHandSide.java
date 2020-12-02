// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.java;

import org.sosy_lab.cpachecker.cfa.ast.ARightHandSide;

/**
 * Interface for all possible right-hand sides of an assignment.
 */
@SuppressWarnings("serial") // we cannot set a UID for an interface
public interface JRightHandSide extends ARightHandSide, JAstNode {

  <R, X extends Exception> R accept(JRightHandSideVisitor<R, X> pV) throws X;

  @Override
  default <R, X extends Exception> R accept(JAstNodeVisitor<R, X> pV) throws X {
    return accept((JRightHandSideVisitor<R, X>) pV);
  }
}
