/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cfa.ast;

import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;

import java.util.Optional;

/**
 * Representation of a "return" statement,
 * potentially including a return value.
 */
public interface AReturnStatement extends AAstNode {

  /**
   * The return value, if present
   * (i.e., the "exp" in "return exp;").
   */
  public Optional<? extends AExpression> getReturnValue();

  /**
   * If this statement has a return value,
   * this method creates a representation of this statement in form of an assignment
   * of the return value to a special variable
   * (i.e., something like "__retval__ = exp;").
   * This special variable is the same as the one returned by
   * {@link FunctionEntryNode#getReturnVariable()}.
   */
  public Optional<? extends AAssignment> asAssignment();
}
