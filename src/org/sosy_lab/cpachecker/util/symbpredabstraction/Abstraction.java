/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.symbpredabstraction;

import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.Region;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormula;

/**
 * Instances of this class should hold the same formula in two representations:
 * as a Region and as a SymbolicFormula
 * 
 * The former one has no SSA indices, while the latter DOES have SSA indices added.
 * 
 * Abstractions are not considered equal even if they have the same formula. 
 */
public class Abstraction {

  private final Region region;
  private final SymbolicFormula symbolicFormula;

  /**
   * The formula of the block directly before this abstraction.
   * (This formula was used to create this abstraction).
   */
  private final SymbolicFormula blockFormula;
  
  private static int nextId = 0;
  private final int id = nextId++;
  
  public Abstraction(Region pFirst, SymbolicFormula pSecond,
      SymbolicFormula pBlockFormula) {
    this.region = pFirst;
    this.symbolicFormula = pSecond;
    this.blockFormula = pBlockFormula;
  }

  public boolean isTrue() {
    return asSymbolicFormula().isTrue();
  }
  
  public boolean isFalse() {
    return asSymbolicFormula().isFalse();
  }
  
  public Region asRegion() {
    return region;
  }
  
  public SymbolicFormula asSymbolicFormula() {
    return symbolicFormula;
  }
  
  public SymbolicFormula getBlockFormula() {
    return blockFormula;
  }
  
  public int getId() {
    return id;
  }
  
  @Override
  public String toString() {
    return "ABS" + id + ": " + symbolicFormula;
  }
}
