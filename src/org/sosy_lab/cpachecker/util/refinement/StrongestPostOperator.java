/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.refinement;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import java.util.Deque;
import java.util.Optional;

/**
 * Interface for the strongest post-operator as used in CEGAR.
 */
public interface StrongestPostOperator<S extends ForgetfulState<?>> {

  /**
   * Computes the abstract state that represents the region of states reachable from the given state
   * by executing the given operation.
   * If the resulting abstract state is contradicting, that means that it represents no
   * concrete states, an empty <code>Optional</code> is returned.
   *
   *
   * @param origin the abstract state the computation is started at
   * @param precision th precision to use for the computation
   * @param operation the operation to perform
   * @return an <code>Optional</code> containing the computed abstract state. An empty
   *    <code>Optional</code>, if the resulting abstracted state is contradicting
   */
  Optional<S> getStrongestPost(
      S origin, Precision precision, CFAEdge operation) throws CPAException, InterruptedException;

  /**
   * Handles the scoping during a function call.
   * The usage of this method depends on {@link #handleFunctionReturn}.
   * The default implementation pushes the calling state onto the stack for later reuse
   * and then returns it without any change.
   * This method is important for the analysis of recursive procedures in combination with BAM.
   *
   * @param state the state before calling the function
   * @param edge the function-call-edge, where the parameters are assigned
   * @param callstack data-structure to store information about scopes,
   *        one new element should be pushed when executing this method.
   */
  S handleFunctionCall(S state, CFAEdge edge, Deque<S> callstack);

  /**
   * Handles the scoping during a function return.
   * The usage of this method depends on {@link #handleFunctionCall}.
   * The default implementation pops the (old) calling state from the stack
   * and rebuilds the return-state with information from the calling scope.
   * We override values of the function's local variables,
   * thus we assume that no local variable of the function-call is accessed
   * (not even deleted or removed from the state) after calling this method,
   * except the optional function-return-variable.
   * We will not touch the optional function-return-variable and
   * the user has to handle this with {@link #getStrongestPost}.
   * This method is important for the analysis of recursive procedures in combination with BAM.
   *
   * @param next the state before exiting the function
   *        (but after assigning a value to the return-variable, if necessary)
   * @param edge the function-return-edge,
   *        where the return-variable is copied into the calling scope and
   *        assigned to the left-hand-side of the function-call
   * @param callstack data-structure to store information about scopes,
   *        one old element should be popped when executing this method.
   */
  S handleFunctionReturn(S next, CFAEdge edge, Deque<S> callstack);

  S performAbstraction(S next, CFANode currNode, ARGPath errorPath, Precision precision);
}
