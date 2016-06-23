/*
 *  CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.core.interfaces;

import java.util.List;

import javax.annotation.Nonnull;

import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.model.CFANode;


/**
 * Represents abstract states can introduce shadow code the analysis.
 */
public interface AbstractStateWithShadowCode extends AbstractState {

  /**
   * Get a (ordered) sequence of AST nodes that are considered "outgoing" from
   * the current shadow location by the CPA of this abstract state.
   *
   * @param pContinueTo   The sequence of {@link AAstNode}s is intended to terminate in this CFA location.
   *
   * @return A (possibly empty) iterable of shadow edges without duplicates.
   */
  List<AAstNode> getOutgoingShadowCode(@Nonnull CFANode pContinueTo);

}