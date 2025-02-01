// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom;

import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;

public interface SeqASTNode {
  /**
   * It is generally preferred to lazily (i.e. inside this function implementation) initialize
   * objects such as {@link CStatement}s even if they are only dependent on {@code final} variables.
   */
  String toASTString();
}
