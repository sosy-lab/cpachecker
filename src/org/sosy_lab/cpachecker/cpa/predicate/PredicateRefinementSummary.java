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
package org.sosy_lab.cpachecker.cpa.predicate;

import static org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision.*;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicateMapWriter;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;


class PredicateRefinementSummary {

  static enum PredicateSharing {
    GLOBAL,            // at all locations
    FUNCTION,          // at all locations in the respective function
    LOCATION,          // at all occurrences of the respective location
    LOCATION_INSTANCE, // at the n-th occurrence of the respective location in each path
    ;
  }

  static class PredicatePrecisionDelta {

    private final int refinementNumber;
    private final Collection<Property> forProperties;
    private final ListMultimap<Pair<CFANode, Integer>, AbstractionPredicate> newPredicates;

    public PredicatePrecisionDelta(Collection<Property> pProps, int pRefinementNumber) {
      // needs to be a fully deterministic data structure,
      // thus a Multimap based on a LinkedHashMap
      // (we iterate over the keys)
      newPredicates = Multimaps.newListMultimap(
          new LinkedHashMap<Pair<CFANode, Integer>, Collection<AbstractionPredicate>>(),
            new Supplier<List<AbstractionPredicate>>() {
              @Override
              public List<AbstractionPredicate> get() {
                return new ArrayList<>();
              }
            });

      forProperties = pProps;
      refinementNumber = pRefinementNumber;
    }

    public void put(CFANode pLocation, int pLocationInstance, AbstractionPredicate pPredicate) {
      newPredicates.put(Pair.of(pLocation, pLocationInstance), pPredicate);
    }

    public void putAll(CFANode pLocation, int pLocationInstance, Collection<AbstractionPredicate> pPredicate) {
      newPredicates.putAll(Pair.of(pLocation, pLocationInstance), pPredicate);
    }

    public Collection<Property> getForProperties() {
      return forProperties;
    }

    public Collection<AbstractionPredicate> getPredicates() {
      return newPredicates.values();
    }

    public ListMultimap<String, AbstractionPredicate> getPredicatesPerFunction() {
      return mergePredicatesPerFunction(newPredicates);
    }

    public boolean containsOtherThan(PredicatePrecision pPrec) {
      return !pPrec.getLocalPredicates().entries().containsAll(newPredicates.entries());
    }

    public ListMultimap<CFANode, AbstractionPredicate> getPredicatesPerLocation() {
      return mergePredicatesPerLocation(newPredicates);
    }

    public PredicatePrecision mergeToPrecision(PredicatePrecision pPrec, PredicateSharing pMode) {
      switch (pMode) {
      case GLOBAL: return pPrec.addGlobalPredicates(getPredicates());
      case FUNCTION: return pPrec.addFunctionPredicates(getPredicatesPerFunction());
      case LOCATION: return pPrec.addLocalPredicates(getPredicatesPerLocation());
      case LOCATION_INSTANCE: return pPrec.addLocationInstancePredicates(newPredicates);
      default:
        throw new AssertionError();
      }
    }

    public void dumpPredicates(PredicateMapWriter precisionWriter, Path pPrecFile) throws IOException {
      try (Writer w = Files.openOutputFile(pPrecFile)) {
        precisionWriter.writePredicateMap(
            ImmutableSetMultimap.copyOf(newPredicates),
            ImmutableSetMultimap.<CFANode, AbstractionPredicate>of(),
            ImmutableSetMultimap.<String, AbstractionPredicate>of(),
            ImmutableSet.<AbstractionPredicate>of(),
            getPredicates(), w);
      }
    }

  }

}
