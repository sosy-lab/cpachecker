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
package org.sosy_lab.cpachecker.cpa.relyguarantee.refinement.lazy;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGPrecision;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;

import com.google.common.base.Preconditions;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;

/**
 * A pivot for data refinement.
 */
public class DataPivot implements Pivot{

  private final ARTElement element;
  private final SetMultimap<CFANode, AbstractionPredicate> artPredicateMap;
  private final SetMultimap<CFANode, AbstractionPredicate> envPredicateMap;
  private final Set<AbstractionPredicate> artGlobalPredicates;
  private final  Set<AbstractionPredicate> envGlobalPredicates;

  public DataPivot(ARTElement element){
    assert !element.getLocalParents().isEmpty() : "Inital element cannot be a pivot";

    this.element = element;
    this.artPredicateMap  = LinkedHashMultimap.<CFANode, AbstractionPredicate>create();
    this.envPredicateMap = LinkedHashMultimap.<CFANode, AbstractionPredicate>create();
    this.artGlobalPredicates = new LinkedHashSet<AbstractionPredicate>();
    this.envGlobalPredicates = new LinkedHashSet<AbstractionPredicate>();

  }

  @Override
  public ARTElement getElement() {
    return element;
  }

  @Override
  public int getTid() {
    return element.getTid();
  }

  public SetMultimap<CFANode, AbstractionPredicate> getArtPredicateMap() {
    return artPredicateMap;
  }

  public SetMultimap<CFANode, AbstractionPredicate> getEnvPredicatesMap() {
    return envPredicateMap;
  }

  public Set<AbstractionPredicate> getArtGlobalPredicates() {
    return artGlobalPredicates;
  }

  public Set<AbstractionPredicate> getEnvGlobalPredicates() {
    return envGlobalPredicates;
  }

  public boolean addArtPredicates(CFANode loc, Collection<AbstractionPredicate> preds){
    return artPredicateMap.putAll(loc, preds);
  }

  public boolean addArtPredicates(Multimap<CFANode, AbstractionPredicate> preds){
    return artPredicateMap.putAll(preds);
  }

  public boolean addEnvPredicates(CFANode loc, Collection<AbstractionPredicate> preds){
    return envPredicateMap.putAll(loc, preds);
  }

  public boolean addEnvPredicates(Multimap<CFANode, AbstractionPredicate> preds){
    return envPredicateMap.putAll(preds);
  }

  public boolean addARTGlobalPredicates(Set<AbstractionPredicate> preds){
    return artGlobalPredicates.addAll(preds);
  }

  public boolean addEnvGlobalPredicates(Set<AbstractionPredicate> preds){
    return envGlobalPredicates.addAll(preds);
  }


  @Override
  public void addPrecisionOf(Pivot pivot){
    Preconditions.checkArgument(pivot instanceof DataPivot);
    DataPivot datPiv = (DataPivot) pivot;

    artPredicateMap.putAll(datPiv.artPredicateMap);
    envPredicateMap.putAll(datPiv.envPredicateMap);
    artGlobalPredicates.addAll(datPiv.artGlobalPredicates);
    envGlobalPredicates.addAll(datPiv.envGlobalPredicates);

  }

  public void addRGPrecision(RGPrecision prec) {
    artPredicateMap.putAll(prec.getARTPredicateMap());
    envPredicateMap.putAll(prec.getEnvPredicateMap());
    artGlobalPredicates.addAll(prec.getARTGlobalPredicates());
    envGlobalPredicates.addAll(prec.getEnvGlobalPredicates());
  }


  @Override
  public String toString(){
    StringBuilder bldr = new StringBuilder();

    bldr.append( "LocationPivot element id: "+element.getElementId()+", ");

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
  public boolean equals(Object ob){
    if (ob instanceof DataPivot){
      DataPivot dpiv = (DataPivot) ob;
      return dpiv.element.equals(element) &&
          dpiv.artPredicateMap.equals(artPredicateMap) &&
          dpiv.artGlobalPredicates.equals(artGlobalPredicates) &&
          dpiv.envPredicateMap.equals(envPredicateMap) &&
          dpiv.envGlobalPredicates.equals(envGlobalPredicates);
    }

    return false;
  }

  @Override
  public int hashCode(){
    return element.hashCode() + 3 *
        (this.artPredicateMap.hashCode() + 11 *
            this.envPredicateMap.hashCode());
  }

}
