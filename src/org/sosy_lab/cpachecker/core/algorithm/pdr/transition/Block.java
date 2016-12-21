/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.pdr.transition;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;

public interface Block {

  /**
   * Gets the block predecessor state.
   * This is always the real predecessor state in terms of the program,
   * independent of the direction of the analysis that created the block.
   *
   * @return the block predecessor state.
   */
  AbstractState getPredecessor();

  /**
   * Gets the block successor state.
   * This is always the real successor state in terms of the program,
   * independent of the direction of the analysis that created the block.
   *
   * @return the block successor state.
   */
  AbstractState getSuccessor();

  /**
   * Gets the block predecessor location.
   * This is always the real predecessor location in terms of the program,
   * independent of the direction of the analysis that created the block.
   *
   * @return the block predecessor location.
   */
  CFANode getPredecessorLocation();

  /**
   * Gets the block successor location.
   * This is always the real successor location in terms of the program,
   * independent of the direction of the analysis that created the block.
   *
   * @return the block successor location.
   */
  CFANode getSuccessorLocation();

  /**
   * Gets the block formula.
   * Depending on the {analysis direction (see {@link Block#getDirection()}),
   * the SSA indices of the variables may be either
   * ascending (if the direction is {@link AnalysisDirection#FORWARD} or
   * descending (if the direction is {@link AnalysisDirection#BACKWARD}
   * from the block predecessor.
   *
   * @return the block formula.
   */
  BooleanFormula getFormula();

  /**
   * Gets the path-formula context for variables at the block predecessor,
   * i.e. for the so-called "unprimed" variables of this transition.
   *
   * @return the path-formula context for variables at the block predecessor,
   * i.e. for the so-called "unprimed" variables of this transition.
   */
  PathFormula getUnprimedContext();

  /**
   * Gets the path-formula context for variables at the block successor,
   * i.e. for the so-called "primed" variables of the transition.
   *
   * @return the path-formula context for variables at the block predecessor,
   * i.e. for the so-called "primed" variables of the transition.
   */
  PathFormula getPrimedContext();

  /**
   * Gets the direction the block formula was created in.
   * This does not affect the predecessor and successor states of the block,
   * but it does affect the order in which the SSA indices are assigned to the
   * variables in the block formula (see {@link Block#getFormula()}).
   *
   * @return the direction the block formula was created in.
   */
  AnalysisDirection getDirection();

  /**
   * Gets the reached set the block was created from.
   *
   * @return the reached set the block was created from.
   */
  ReachedSet getReachedSet();
}
