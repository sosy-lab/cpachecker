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
package org.sosy_lab.cpachecker.util.predicates.ldd;

import java.util.Collection;

import org.sosy_lab.common.NativeLibraries;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;

public class LDDRegionManager {

  static {
    NativeLibraries.loadLibrary("JLDD");
  }

  private final LDDFactory factory;

  private final LDDRegion trueFormula;
  private final LDDRegion falseFormula;

  public LDDRegionManager(int size) {
    this.factory = new LDDFactory(size);
    this.trueFormula = new LDDRegion(factory.one());
    this.falseFormula = new LDDRegion(factory.zero());
  }

  public boolean entails(Region pF1, Region pF2) {
    LDDRegion f1 = (LDDRegion) pF1;
    LDDRegion f2 = (LDDRegion) pF2;
    LDD imp = f1.getLDD().imp(f2.getLDD());
    return imp.isOne();
  }

  public LDDRegion makeTrue() {
    return trueFormula;
  }

  public LDDRegion makeFalse() {
    return falseFormula;
  }

  public LDDRegion makeNot(Region pF) {
    LDDRegion f = (LDDRegion) pF;
    return new LDDRegion(f.getLDD().negate());
  }

  public LDDRegion makeAnd(Region pF1, Region pF2) {
    LDDRegion f1 = (LDDRegion) pF1;
    LDDRegion f2 = (LDDRegion) pF2;
    return new LDDRegion(f1.getLDD().and(f2.getLDD()));
  }

  public LDDRegion makeOr(Region pF1, Region pF2) {
    LDDRegion f1 = (LDDRegion) pF1;
    LDDRegion f2 = (LDDRegion) pF2;
    return new LDDRegion(f1.getLDD().or(f2.getLDD()));
  }

  public LDDRegion makeExists(Region pF1, Region pF2) {
    LDDRegion f1 = (LDDRegion) pF1;
    LDDRegion f2 = (LDDRegion) pF2;
    return new LDDRegion(f1.getLDD().exists(f2.getLDD()));
  }

  public Region getIfThenElse(Region conditionRegion, Region positiveRegion, Region negativeRegion) {
    LDDRegion condition = (LDDRegion) conditionRegion;
    LDDRegion positive = (LDDRegion) positiveRegion;
    LDDRegion negative = (LDDRegion) negativeRegion;
    return new LDDRegion(condition.getLDD().makeIfThenElse(positive.getLDD(), negative.getLDD()));
  }

  public LDDRegion makeConstantAssignment(Collection<Pair<Integer, Integer>> varIndices, int varCount, int constValue) {
    return new LDDRegion(this.factory.makeConstantAssignment(varIndices, varCount, constValue));
  }

  public LDDRegion makeNode(Collection<Pair<Integer, Integer>> varCoeffs, int varCount, boolean leq, int constant) {
    return new LDDRegion(this.factory.makeNode(varCoeffs, varCount, leq, constant));
  }

  public LDDRegion makeXor(LDDRegion pAssumeToRegion, LDDRegion pAssumeToRegion2) {
    return new LDDRegion(pAssumeToRegion.getLDD().xor(pAssumeToRegion2.getLDD()));
  }

  public LDDRegion replace(Integer pInteger, Collection<Pair<Integer, Integer>> pIndexCoefficients, int varCount, int pConstant, LDDRegion pRegion) {
    return new LDDRegion(this.factory.replace(pRegion.getLDD(), pInteger, pIndexCoefficients, varCount, pConstant));
  }

}
