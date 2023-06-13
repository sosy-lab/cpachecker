// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;

/** Representation of a "return" statement, potentially including a return value. */
public interface AReturnStatement extends AAstNode {

  /** The return value, if present (i.e., the "exp" in "return exp;"). */
  Optional<? extends AExpression> getReturnValue();

  /**
   * If this statement has a return value, this method creates a representation of this statement in
   * form of an assignment of the return value to a special variable (i.e., something like
   * "__retval__ = exp;"). This special variable is the same as the one returned by {@link
   * FunctionEntryNode#getReturnVariable()}.
   */
  Optional<? extends AAssignment> asAssignment();
}
