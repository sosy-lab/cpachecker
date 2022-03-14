// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types;

import java.io.Serializable;

public interface Type extends Serializable {

  /**
   * Return a string representation of a variable declaration with a given name and this type.
   *
   * <p>Example: If this type is array of int, and we call <code>toASTString("foo")</code>, the
   * result is
   *
   * <pre>int foo[]</pre>
   *
   * .
   *
   * @param declarator The name of the variable to declare.
   * @return A string representation of this type.
   */
  String toASTString(String declarator);
}
