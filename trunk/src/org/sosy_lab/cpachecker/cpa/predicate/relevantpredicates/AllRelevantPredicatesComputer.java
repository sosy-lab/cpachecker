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
package org.sosy_lab.cpachecker.cpa.predicate.relevantpredicates;

import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;

import java.util.Collection;

/**
 * This is a dummy computer, that considers all predicates as relevant.
 * BAM-predicate-analysis using this RelevantPredicateComputer will run
 * very similar to the default predicate-analysis without BAM,
 * because the abstractions and precisions are nearly not reduced.
 * In this case the only beneficial part of BAM is the BAM-cache.
 */
public class AllRelevantPredicatesComputer implements RelevantPredicatesComputer {

  /** We have no inner state, thus a singleton-instance is sufficient. */
  public static final AllRelevantPredicatesComputer INSTANCE = new AllRelevantPredicatesComputer();

  private AllRelevantPredicatesComputer() {}

  @Override
  public Collection<AbstractionPredicate> getRelevantPredicates(
      Block pContext, Collection<AbstractionPredicate> pPredicates) {
    return pPredicates;
  }
}
