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

import org.sosy_lab.cpachecker.util.predicates.regions.Region;


public class LDDRegion implements Region {

  private final LDD ldd;

  public LDDRegion(LDD ldd) {
    this.ldd = ldd;
  }

  LDD getLDD() {
    return this.ldd;
  }

  @Override
  public boolean isTrue() {
    return this.ldd.isOne();
  }

  @Override
  public boolean isFalse() {
    return this.ldd.isZero();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o instanceof LDDRegion) {
      return ldd.equals(((LDDRegion) o).ldd);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return ldd.hashCode();
  }

}
