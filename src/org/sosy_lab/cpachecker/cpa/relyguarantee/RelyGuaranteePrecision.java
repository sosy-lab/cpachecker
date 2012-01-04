/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.relyguarantee;

import java.util.Collection;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;

  public class RelyGuaranteePrecision implements Precision {

    //private final int id = idCounter++;
    //private static int idCounter = 0;
    private final ImmutableSetMultimap<CFANode, AbstractionPredicate> predicateMap;
    private ImmutableSet<AbstractionPredicate> globalPredicates;

    public RelyGuaranteePrecision(ImmutableSetMultimap<CFANode, AbstractionPredicate> predicateMap, Collection<AbstractionPredicate> globalPredicates) {
      assert predicateMap != null;
      this.predicateMap = predicateMap;
      this.globalPredicates = ImmutableSet.copyOf(globalPredicates);
    }

    public RelyGuaranteePrecision(Collection<AbstractionPredicate> globalPredicates) {
      predicateMap = ImmutableSetMultimap.of();
      this.globalPredicates = (globalPredicates == null ? ImmutableSet.<AbstractionPredicate>of() : ImmutableSet.copyOf(globalPredicates));
    }

    @Override
    public boolean equals(Object pObj) {
      if (pObj == this) {
        return true;
      } else if (!(pObj instanceof RelyGuaranteePrecision)) {
        return false;
      } else {
        return predicateMap.equals(((RelyGuaranteePrecision)pObj).predicateMap);
      }
    }

    @Override
    public String toString(){
      if (this.globalPredicates.isEmpty()){
        return predicateMap.toString();
      } else {
        return predicateMap + " global: "+this.globalPredicates;
      }
    }

    public SetMultimap<CFANode, AbstractionPredicate> getPredicateMap() {
      return this.predicateMap;
    }

    public Set<AbstractionPredicate> getGlobalPredicates() {
      return this.globalPredicates;
    }

    public Set<AbstractionPredicate> getPredicates(CFANode pLoc) {
      return this.predicateMap.get(pLoc);
    }

    public void setGlobalPredicates(ImmutableSet<AbstractionPredicate> newGlob) {
      this.globalPredicates = newGlob;

    }


  }