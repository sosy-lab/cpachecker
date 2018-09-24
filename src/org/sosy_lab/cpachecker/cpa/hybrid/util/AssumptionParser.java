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
package org.sosy_lab.cpachecker.cpa.hybrid.util;

import java.util.Set;
import java.util.Collections;
import java.util.HashSet;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;

/**
 * This class provides methods for parsing string encoded assumptions into CExpressions
 */
public class AssumptionParser {

    private final String delimiter;

    public AssumptionParser(final String delimiter) {
        this.delimiter = delimiter;
    }

    /**
     * 
     * @param assumptions The string containing all defined string encoded assumptions
     * @return A set of parsed CExpressions
     */
    public Set<CExpression> parseMany(final String assumptions) {
        if(assumptions.isEmpty()) {
            return Collections.emptySet();
        }

        // set is used to skip duplicates
        Set<CExpression> assumptionSet = new HashSet<>();

        // iterate over all assumptions defined in the assumptions-string
        for(String assumption : assumptions.split(delimiter)) {
            assumptionSet.add(parseAssumption(assumption));
        }

        return assumptionSet;
    }

    /**
     * 
     * @param assumption The string encoded assumption to parse
     * @return A CExpression containing the assumtion information
     */
    public CExpression parseAssumption(final String assumption) {
        final String[] assumptionSplit = assumption.split("=");

        // there can only be a left hand side and a right hand side in an assignment
        assert(assumptionSplit.length == 2);

        // complex assumption on structs are defined with curly braces
        if(assumptionSplit[1].contains("{")) {
            return parseComplex(assumptionSplit);
        }

        return parseSimple(assumptionSplit);
    }

    private CExpression parseSimple(final String[] assumptionSplit) {
        // to construct a CIdExpression, we need more information of the variable
        return null;
    }

    private CExpression parseComplex(final String[] assumptionSplit) {
        return null;
    }
}