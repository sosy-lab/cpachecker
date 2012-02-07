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
package org.sosy_lab.cpachecker.cpa.relyguarantee.refinement;

import java.util.Collection;

import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.SetMultimap;


/**
 * Stores information on the result of rely-guarantee interpolation -
 * either new predicates that eliminate a spurious counterexample or
 * trace(s) that witness the error.
 */
public class InterpolationTreeResult {

  private final boolean isSpurious;
  private final SetMultimap<ARTElement, AbstractionPredicate> artMap;
  private final SetMultimap<ARTElement, AbstractionPredicate> envMap;
  private final InterpolationTree tree;


  public InterpolationTreeResult(boolean spurious){
    assert spurious;
    this.isSpurious = spurious;
    this.artMap = LinkedHashMultimap.create();
    this.envMap = LinkedHashMultimap.create();
    this.tree = null;
  }

  public InterpolationTreeResult(boolean spurious, InterpolationTree tree){
    assert !spurious;

    this.isSpurious = spurious;
    this.tree = tree;
    this.artMap = null;
    this.envMap = null;
  }

  public boolean isSpurious(){
    return isSpurious;
  }

  public void addPredicatesForRefinement(ARTElement e, Collection<AbstractionPredicate> preds) {
    artMap.putAll(e, preds);
  }

  public void addEnvPredicatesForRefinement(ARTElement e,  Collection<AbstractionPredicate> preds) {
    envMap.putAll(e, preds);
  }

  public Collection<ARTElement> getEnvPredicatesForRefinmentKeys() {
    return envMap.keySet();
  }

  public Collection<ARTElement> getPredicatesForRefinmentKeys() {
    return artMap.keySet();
  }

  public Collection<AbstractionPredicate> getPredicatesForRefinement(ARTElement e) {
    return artMap.get(e);
  }

  public Collection<AbstractionPredicate> getEnvPredicatesForRefinement(ARTElement e) {
    return envMap.get(e);
  }

  public InterpolationTree getTree() {
    return tree;
  }

}
