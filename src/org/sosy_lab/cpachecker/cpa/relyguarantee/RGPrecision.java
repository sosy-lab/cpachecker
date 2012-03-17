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
import java.util.List;
import java.util.Vector;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSetMultimap.Builder;

  public class RGPrecision implements Precision {

    /* predicates for constructing ART */
    private final ImmutableSetMultimap<CFANode, AbstractionPredicate> artPredicateMap;
    private final ImmutableSet<AbstractionPredicate> artGlobalPredicates;
    /* predicates for appling env. transitinos */
    private final ImmutableSetMultimap<CFANode, AbstractionPredicate> envPredicateMap;
    private final ImmutableSet<AbstractionPredicate> envGlobalPredicates;

    public RGPrecision(ImmutableSetMultimap<CFANode, AbstractionPredicate> predicateMap,
        ImmutableSet<AbstractionPredicate> globalPredicates,
        ImmutableSetMultimap<CFANode, AbstractionPredicate> envPredicateMap,
        ImmutableSet<AbstractionPredicate> envGlobalPredicates) {
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


    @Override
    public int hashCode(){
      return artPredicateMap.hashCode() + 7 *
          (artGlobalPredicates.hashCode() + 13 *
              (envPredicateMap.hashCode()) + 17 * envGlobalPredicates.hashCode());
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

    public  ImmutableSet<AbstractionPredicate> getEnvPredicates(CFANode pLoc) {
      return this.envPredicateMap.get(pLoc);
    }

    /**
     * Constructs a precision that contains all predicates from argument precisions.
     * @return
     */
    public static RGPrecision merge(Collection<RGPrecision> precs){

      if (precs.size() == 1){
        return precs.iterator().next();
      }

      ImmutableSetMultimap<CFANode, AbstractionPredicate> mARTMap =
          mergeARTMaps(precs);

      ImmutableSet<AbstractionPredicate> mARTGlob =
          mergeARTGlobals(precs);

      ImmutableSetMultimap<CFANode, AbstractionPredicate> mEnvMap =
          mergeEnvMaps(precs);

      ImmutableSet<AbstractionPredicate> mEnvGlob =
          mergeEnvGlobals(precs);

      return new RGPrecision(mARTMap, mARTGlob, mEnvMap, mEnvGlob);
    }


    private static ImmutableSetMultimap<CFANode, AbstractionPredicate> mergeARTMaps(Collection<RGPrecision> precs) {

      List<ImmutableSetMultimap<CFANode, AbstractionPredicate>> artMaps =
          new Vector<ImmutableSetMultimap<CFANode, AbstractionPredicate>>(precs.size());

      for (RGPrecision prec : precs){
        artMaps.add(prec.getARTPredicateMap());
      }

      return mergeMaps(artMaps);
    }

    private static ImmutableSet<AbstractionPredicate> mergeARTGlobals(Collection<RGPrecision> precs) {

      List<ImmutableSet<AbstractionPredicate>> artGlobals =
         new Vector<ImmutableSet<AbstractionPredicate>>(precs.size());

      for (RGPrecision prec : precs){
        artGlobals.add(prec.getARTGlobalPredicates());
      }

      return mergeSets(artGlobals);
    }

    private static ImmutableSetMultimap<CFANode, AbstractionPredicate> mergeEnvMaps(Collection<RGPrecision> precs) {

      List<ImmutableSetMultimap<CFANode, AbstractionPredicate>> envMaps =
          new Vector<ImmutableSetMultimap<CFANode, AbstractionPredicate>>(precs.size());

      for (RGPrecision prec : precs){
        envMaps.add(prec.getEnvPredicateMap());
      }

      return mergeMaps(envMaps);
    }

    private static ImmutableSet<AbstractionPredicate> mergeEnvGlobals(Collection<RGPrecision> precs) {

      List<ImmutableSet<AbstractionPredicate>> envGlobals =
         new Vector<ImmutableSet<AbstractionPredicate>>(precs.size());

      for (RGPrecision prec : precs){
        envGlobals.add(prec.getEnvGlobalPredicates());
      }

      return mergeSets(envGlobals);
    }

    private static  ImmutableSetMultimap<CFANode, AbstractionPredicate> mergeMaps(
        Collection<ImmutableSetMultimap<CFANode, AbstractionPredicate>> maps){

      Builder<CFANode, AbstractionPredicate> bldr = ImmutableSetMultimap.<CFANode, AbstractionPredicate>builder();

      for (ImmutableSetMultimap<CFANode, AbstractionPredicate> map : maps){
        bldr = bldr.putAll(map);
      }

      return bldr.build();
    }


    private static ImmutableSet<AbstractionPredicate> mergeSets(
        List<ImmutableSet<AbstractionPredicate>> sets){

      com.google.common.collect.ImmutableSet.Builder<AbstractionPredicate> bldr =
          ImmutableSet.<AbstractionPredicate>builder();

      for (ImmutableSet<AbstractionPredicate> set : sets){
        bldr = bldr.addAll(set);
      }

      return bldr.build();
    }





  }