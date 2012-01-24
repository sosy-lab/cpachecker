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
package org.sosy_lab.cpachecker.util.predicates;

import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;

/**
 * Instances of this class should hold the same formula in two representations:
 * as a Region and as a Formula
 *
 * The former one has no SSA indices, while the latter DOES have SSA indices added.
 *
 * Abstractions are not considered equal even if they have the same formula.
 */
public class AbstractionFormula {

  private final Region region;
  private final Formula formula;

  /**
   * The formula of the block directly before this abstraction.
   * (This formula was used to create this abstraction).
   */
  private final Formula blockFormula;

  private static int nextId = 0;
  private final int id = nextId++;

  public AbstractionFormula(Region pFirst, Formula pSecond,
      Formula pBlockFormula) {
    this.region = pFirst;
    this.formula = pSecond;
    this.blockFormula = pBlockFormula;
  }

  public boolean isTrue() {
    return asFormula().isTrue();
  }

  public boolean isFalse() {
    return asFormula().isFalse();
  }

  public Region asRegion() {
    return region;
  }

  public Formula asFormula() {
    return formula;
  }

  public Formula getBlockFormula() {
    return blockFormula;
  }

  @Override
  public String toString() {
    return "ABS" + id + ": " + formula;
  }
}
