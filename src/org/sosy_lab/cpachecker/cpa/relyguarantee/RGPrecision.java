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

import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;

  public class RGPrecision implements Precision {

    /* predicates for constructing ART */
    private final ImmutableSetMultimap<CFANode, AbstractionPredicate> artPredicateMap;
    private final ImmutableSet<AbstractionPredicate> artGlobalPredicates;
    /* predicates for appling env. transitinos */
    private final ImmutableSetMultimap<CFANode, AbstractionPredicate> envPredicateMap;
    private final ImmutableSet<AbstractionPredicate> envGlobalPredicates;

    public RGPrecision(ImmutableSetMultimap<CFANode, AbstractionPredicate> predicateMap, ImmutableSet<AbstractionPredicate> globalPredicates,
        ImmutableSetMultimap<CFANode, AbstractionPredicate> envPredicateMap, ImmutableSet<AbstractionPredicate> envGlobalPredicates) {
      assert predicateMap != null;
      assert globalPredicates != null;
      assert envPredicateMap != null;
      assert envGlobalPredicates != null;
      this.artPredicateMap = predicateMap;
      this.artGlobalPredicates = globalPredicates;
      this.envPredicateMap = envPredicateMap;
      this.envGlobalPredicates = envGlobalPredicates;
    }

    @Override
    public boolean equals(Object pObj) {
      if (pObj == this) {
        return true;
      } else if (!(pObj instanceof RGPrecision)) {
        return false;
      }

      RGPrecision other = (RGPrecision) pObj;
      return other.artPredicateMap.equals(artPredicateMap) &&
          other.artGlobalPredicates.equals(artGlobalPredicates) &&
          other.envPredicateMap.equals(envPredicateMap) &&
          other.envGlobalPredicates.equals(envGlobalPredicates);
    }

    @Override
    public String toString(){
      StringBuilder bldr = new StringBuilder();
      bldr.append("RGPrecision ");

      if (!artGlobalPredicates.isEmpty()){
        bldr.append("global ART: "+artGlobalPredicates+", ");
      }

      if (!artPredicateMap.isEmpty()){
        bldr.append("local ART: "+artPredicateMap+", ");
      }

      if (!envGlobalPredicates.isEmpty()){
        bldr.append("global env.: "+envGlobalPredicates+", ");
      }

      if (!envPredicateMap.isEmpty()){
        bldr.append("local env: "+envPredicateMap);
      }

      return bldr.toString();
    }

    public ImmutableSetMultimap<CFANode, AbstractionPredicate> getARTPredicateMap() {
      return this.artPredicateMap;
    }

    public ImmutableSet<AbstractionPredicate> getARTGlobalPredicates() {
      return this.artGlobalPredicates;
    }

    public ImmutableSet<AbstractionPredicate> getARTPredicates(CFANode pLoc) {
      return this.artPredicateMap.get(pLoc);
    }

    public ImmutableSetMultimap<CFANode, AbstractionPredicate> getEnvPredicateMap() {
      return envPredicateMap;
    }

    public ImmutableSet<AbstractionPredicate> getEnvGlobalPredicates() {
      return envGlobalPredicates;
    }



  }