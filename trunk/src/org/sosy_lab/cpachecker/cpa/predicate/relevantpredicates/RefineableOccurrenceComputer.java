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

import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSetMultimap.Builder;
import com.google.common.collect.Sets;

import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;

import java.util.Set;


/**
 * Computes set of irrelevant predicates of a block by identifying the variables that do not occur in the block.
 */

public class RefineableOccurrenceComputer extends OccurrenceComputer implements RefineableRelevantPredicatesComputer {

  private final ImmutableSetMultimap<Block, AbstractionPredicate> definitelyRelevantPredicates;

  public RefineableOccurrenceComputer(FormulaManagerView pFmgr) {
    super(pFmgr);
    definitelyRelevantPredicates = ImmutableSetMultimap.of();
  }

  private RefineableOccurrenceComputer(FormulaManagerView pFmgr,
      ImmutableSetMultimap<Block, AbstractionPredicate> pDefinitelyRelevantPredicates) {
    super(pFmgr);
    definitelyRelevantPredicates = pDefinitelyRelevantPredicates;
  }

  @Override
  protected boolean isRelevant(Block context, AbstractionPredicate predicate) {
    Set<AbstractionPredicate> relevantPredicates = definitelyRelevantPredicates.get(context);
    if (relevantPredicates != null && relevantPredicates.contains(predicate)) {
      return true;
    }

    return super.isRelevant(context, predicate);
  }

  @Override
  public RefineableOccurrenceComputer considerPredicatesAsRelevant(
      Block block, Set<AbstractionPredicate> predicates) {

    Set<AbstractionPredicate> newPreds = Sets.difference(predicates, definitelyRelevantPredicates.get(block));

    if (newPreds.isEmpty()) {
      return this;
    }

    Builder<Block, AbstractionPredicate> builder = ImmutableSetMultimap.builder();
    builder.putAll(definitelyRelevantPredicates);
    builder.putAll(block, newPreds);
    return new RefineableOccurrenceComputer(fmgr, builder.build());
  }

  @Override
  public String toString() {
    return "RefineableOccurrenceComputer (" + definitelyRelevantPredicates + ")";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof RefineableOccurrenceComputer) {
      RefineableOccurrenceComputer other = (RefineableOccurrenceComputer) o;
      return definitelyRelevantPredicates.equals(other.definitelyRelevantPredicates)
          && super.equals(o);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return super.hashCode() * 17 + definitelyRelevantPredicates.hashCode();
  }
}
