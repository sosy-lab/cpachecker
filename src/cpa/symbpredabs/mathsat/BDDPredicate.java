/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package cpa.symbpredabs.mathsat;

import cpa.symbpredabs.Predicate;

/**
 * A predicate represented as a BDD variable
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class BDDPredicate implements Predicate {
    private int bdd; // this is the BDD representing this single variable.
                     // That is, a node with variable varindex and two
                     // children 0 and 1
    private int varindex; // this is the variable itself

    public BDDPredicate(int var, int idx) {
        bdd = var;
        varindex = idx;
    }

    public int getBDD() {
        return bdd;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof BDDPredicate) {
            return varindex == ((BDDPredicate)o).varindex;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return varindex;
    }

    @Override
    public String toString() {
        return "BDD(" + varindex + ")";
    }
}
