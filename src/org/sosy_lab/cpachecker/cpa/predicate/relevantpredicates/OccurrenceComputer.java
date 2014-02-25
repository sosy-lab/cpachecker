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

import java.util.Collection;

import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.blocks.ReferencedVariable;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;


/**
 * Computes set of irrelevant predicates of a block by identifying the variables that do not occur in the block.
 */

public class OccurrenceComputer extends AbstractRelevantPredicatesComputer<Block> {

  @Override
  protected Block precompute(Block pContext, Collection<AbstractionPredicate> pPredicates) {
    return pContext;
  }

  @Override
  protected boolean isRelevant(Block context, AbstractionPredicate predicate) {
    String predicateString = predicate.getSymbolicAtom().toString();

    for (ReferencedVariable var : context.getReferencedVariables()) {
      if (predicateString.contains(var.getName())) {
        //var occurs in the predicate, so better trace it
        //TODO: contains is a quite rough approximation; for example "foo <= 5" also contains "f", although the variable f does in fact not occur in the predicate.
        return true;
      }
    }
    return false;
  }
}