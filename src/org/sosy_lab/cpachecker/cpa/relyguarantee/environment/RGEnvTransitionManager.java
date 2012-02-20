/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.relyguarantee.environment;

import java.util.Collection;

import org.sosy_lab.cpachecker.cpa.relyguarantee.RGAbstractElement;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGEnvCandidate;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGEnvTransition;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;


/**
 * Provides functions for handling environmental transitions.
 */
public interface RGEnvTransitionManager {

  /**
   * Generate an environmental transition from the candidate using the given precision.
   * @param cand
   * @param predicates
   * @return
   */
  RGEnvTransition generateEnvTransition(RGEnvCandidate cand, Collection<AbstractionPredicate> predicates);

  /**
   * Returns a path formula representing the effect of applying the transition on the element.
   * The environmental part of the result is primed unique number of times.
   * The formula is meant for abstraction.
   * @param pf
   * @param et
   * @return
   */
  PathFormula formulaForAbstraction(RGAbstractElement elem, RGEnvTransition et, int unique) throws CPATransferException;


  /**
   * Returns a path formula representing the effect of applying the transition on the element.
   * The environmental part of the result is primed unique number of times.
   * The formula is meant for refinement.
   * @param pf
   * @param et
   * @return
   */
  PathFormula formulaForRefinement(RGAbstractElement elem, RGEnvTransition et, int unique) throws CPATransferException;

  /**
   * Returns true only if et1 is less or equal to et2.
   * @param c1
   * @param c2
   * @return
   */
  boolean isLessOrEqual(RGEnvTransition et1, RGEnvTransition et2);

  /**
   * Return true if the transition is less or equal to every other transition.
   * @param et
   * @return
   */
  boolean isBottom(RGEnvTransition et);

}
