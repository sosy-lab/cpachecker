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
package org.sosy_lab.cpachecker.cpa.predicate.relevantpredicates;

import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.blocks.ReferencedVariable;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;

import java.util.Collection;
import java.util.Set;


/**
 * Computes set of irrelevant predicates of a block by identifying the variables that do not occur in the block.
 */

public class OccurrenceComputer extends AbstractRelevantPredicatesComputer<Block> {

  public OccurrenceComputer(FormulaManagerView pFmgr) {
    super(pFmgr);
  }

  @Override
  protected Block precompute(Block pContext, Collection<AbstractionPredicate> pPredicates) {
    return pContext;
  }

  @Override
  protected boolean isRelevant(Block context, AbstractionPredicate predicate) {
    Set<String> variables = fmgr.extractVariableNames(predicate.getSymbolicAtom());
    for (ReferencedVariable var : context.getReferencedVariables()) {

      // short cut
      if (variables.contains(var.getName())) {
        return true;
      }

      // here we have to handle some imprecise information,
      // because the encoding of variables in predicates and referencedVariables might differ.
      // Examples: "__ADDRESS_OF_xyz" vs "xyz", "ssl3_accept::s->state" vs "ssl3_accept::s"
      // This handling causes an over-approximation of the set of variables, because
      // a predicate-variable "f" is relevant, if "foo" is one of the referenced variables.
      for (String variable : variables) {
        if (variable.contains(var.getName())) {
          return true;
        }
      }
    }

    return false;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof OccurrenceComputer && super.equals(o);
  }
}
