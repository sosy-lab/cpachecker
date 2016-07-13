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
package org.sosy_lab.cpachecker.core.interfaces;

import org.sosy_lab.cpachecker.cfa.ast.AReturnStatement;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;

import java.util.List;

/**
 * Sub-interface for {@link AbstractState}s that marks states
 * with an assumption.
 * This is intended for other CPAs to use in their strengthen operator,
 * such that all the other CPAs can add these assumptions to their abstract state.
 */
public interface AbstractStateWithAssumptions extends AbstractState {

  /**
   * Get the list of assumptions represented as AssumeEdges.
   *
   * Assumptions about function return value are transformed from
   * "return N;" to "[retVar == N]", where "retVar" is the name of a pseudo variable
   * (just as {@link AReturnStatement#asAssignment()} does.
   *
   * The CFANodes attached to the produced edges are not real nodes
   * and should not be used. In particular, there is no guarantee that the list
   * of edges corresponds to a connected chain of nodes and edges.
   *
   * @param functionName the function name where the assumptions are
   * @return A (possibly empty list) of assume edges.
   */
  List<? extends AssumeEdge> getAsAssumeEdges(String functionName);
}