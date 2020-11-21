/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.core.algorithm.legion.selection;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.legion.LegionPhaseStatistics;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;

/**
 * Selects a random state from the reached set which has a nondet-mark.
 */
public class RandomSelectionStrategy implements Selector {

    private LogManager logger;
    private Random random;
    private LegionPhaseStatistics stats;
    private Set<PathFormula> blacklisted;

    private static long random_seed = 1200709844L;
    
    public RandomSelectionStrategy(LogManager logger) {
        this.logger = logger;

        this.random = new Random(random_seed);
        this.stats = new LegionPhaseStatistics("selection");

        this.blacklisted = new HashSet<>();
    }

    @Override
    public PathFormula select(ReachedSet reachedSet) {
        this.stats.start();
        ArrayList<ARGState> nonDetStates = getNondetStates(reachedSet);
        PathFormula target;
        while (true) {
            ARGState state = nonDetStates.remove(this.random.nextInt(nonDetStates.size()));
            PredicateAbstractState ps =
                AbstractStates.extractStateByType(state, PredicateAbstractState.class);
            target = ps.getPathFormula();

            // If the just selected target is the last one,
            // No choice but to return it.
            if (nonDetStates.size() == 0){
                break;
            }

            // Otherwhise, check if it's blacklisted
            if (this.blacklisted.contains(target)){
                continue;
            } else {
                break;
            }

        }
        this.stats.finish();
        return target;
    }

    /**
     * Find all nondet-marked states from the reachedSet
     */
    ArrayList<ARGState> getNondetStates(ReachedSet reachedSet) {
        ArrayList<ARGState> nonDetStates = new ArrayList<>();
        for (AbstractState state : reachedSet.asCollection()) {
            ValueAnalysisState vs =
                    AbstractStates.extractStateByType(state, ValueAnalysisState.class);
            if (vs.nonDeterministicMark) {
                logger.log(Level.FINE, "Nondet state", vs.getConstants().toString());
                nonDetStates.add((ARGState)state);
            }
        }
        return nonDetStates;
    }

    /**
     * The random selector just blacklists states with a weight below zero.
     * Selection will then try to select a state not blacklisted, only
     * when there is no other choice it will return one.
     */
    @Override
    public void feedback(PathFormula pState, int pWeight) {
        if (pWeight < 0){
            this.blacklisted.add(pState);
        }
    }

    @Override
    public LegionPhaseStatistics getStats() {
        return this.stats;
    }

}
