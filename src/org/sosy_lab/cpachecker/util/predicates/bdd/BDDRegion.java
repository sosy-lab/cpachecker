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
package org.sosy_lab.cpachecker.util.predicates.bdd;

import net.sf.javabdd.BDD;

import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;

/**
 * Regions represented using BDDs
 */
public class BDDRegion implements Region {

    private final BDD bddRepr;

    public BDDRegion(BDD pBDD) {
        bddRepr = pBDD;
    }

    @Override
    public boolean isTrue() {
      return bddRepr.isOne();
    }

    @Override
    public boolean isFalse() {
      return bddRepr.isZero();
    }

    BDD getBDD() {
        return bddRepr;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof BDDRegion) {
            return bddRepr.equals(((BDDRegion)o).bddRepr);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return bddRepr.hashCode();
    }

    @Override
    public String toString() {
      return bddRepr.isOne() ? "true" : bddRepr.toString();
    }
}
