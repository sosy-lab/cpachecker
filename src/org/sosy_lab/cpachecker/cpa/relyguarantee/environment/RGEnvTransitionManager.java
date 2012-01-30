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

import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGAbstractElement;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGEnvCandidate;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGEnvTransition;
import org.sosy_lab.cpachecker.cpa.relyguarantee.refinement.InterpolationTreeNode;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;

import com.google.common.collect.Multimap;


/**
 * Provides functions for handling environmental transitions.
 */
public interface RGEnvTransitionManager {

  /**
   * Generate an environmental transition from the candidate using the given precision.
   * @param cand
   * @param globalPreds
   * @param localPreds
   * @return
   */
  RGEnvTransition generateEnvTransition(RGEnvCandidate cand, Collection<AbstractionPredicate> globalPreds, Multimap<CFANode, AbstractionPredicate> localPreds);

  /**
   * Returns a path formula representing the effect of applying the transition on the element.
   * The formula is meant for abstraction.
   * @param pf
   * @param et
   * @return
   */
  PathFormula formulaForAbstraction(RGAbstractElement elem, RGEnvTransition et);


  /**
   * Returns a path formula representing the effect of applying the transition on the element.
   * The environmental part of the result is primed unique number of times.
   * The formula is meant for refinement.
   * @param pf
   * @param et
   * @return
   */
  PathFormula formulaForRefinement(RGAbstractElement elem, RGEnvTransition et, int unique);

  /**
   * Extracts predicates from the iterpolated node.
   * @param itp
   * @param node
   * @return
   */
  Collection<AbstractionPredicate> getPredicates(Formula itp, InterpolationTreeNode node);


  /**
   * Returns true  if et1 is less or equal to et2.
   * @param c1
   * @param c2
   * @return
   */
  boolean isLessOrEqual(RGEnvTransition et1, RGEnvTransition et2);

  /**
   * Returns true  if c1 is less or equal to c2.
   * @param c1
   * @param c2
   * @return
   */
  boolean isLessOrEqual(RGEnvCandidate c1, RGEnvCandidate c2);

}
