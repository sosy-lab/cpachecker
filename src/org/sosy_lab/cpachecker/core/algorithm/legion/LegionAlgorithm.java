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
package org.sosy_lab.cpachecker.core.algorithm.legion;

import java.util.logging.Level;

import com.google.common.collect.ImmutableList;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import java.util.Collection;

public class LegionAlgorithm implements Algorithm {
    private final Algorithm algorithm;
    private final LogManager logger;
    private final int maxIterations;

    public LegionAlgorithm(final Algorithm algorithm, final LogManager pLogger) {
        this.algorithm = algorithm;
        this.logger = pLogger;
        this.maxIterations = 10;
    }

    @Override
    public AlgorithmStatus run(ReachedSet reachedSet)
            throws CPAException, InterruptedException,
            CPAEnabledAnalysisPropertyViolationException {
        logger.log(Level.INFO, "Running legion algorithm");

        AlgorithmStatus status = AlgorithmStatus.NO_PROPERTY_CHECKED;
        for (int i = 0; i < this.maxIterations; i++) {
            this.logger.log(Level.INFO, "Legion iteration " + i);

            // Run algorithm and collect result
            status = algorithm.run(reachedSet);

            // If an error was found, exit
            Collection<Property> violatedProperties = reachedSet.getViolatedProperties();
            // TODO: pull out of status if already found error
            if (!violatedProperties.isEmpty()) {
                this.logger.log(Level.WARNING, "Found violated property in input after " + i + " iterations.");
                break;
            }

            // If no error was found, contained states are checked if non-deterministic (=random)
            // decisions where made. States with non-deterministic followers can be retried to make
            // different decisions.
            for (AbstractState state : reachedSet.asCollection()) {
                ImmutableList<AbstractState> wrappedStates =
                        ((AbstractSingleWrapperState) state).getWrappedStates();

                for (AbstractState as : wrappedStates) {
                    if (this.nonDeterministicStateContained(as)) {
                        for (ARGState previous : ((ARGState) state).getParents()) {
                            reachedSet.reAddToWaitlist(previous);
                            this.logger.log(
                                    Level.INFO,
                                    "Added state to waitlist: " + previous.toString());
                        }
                    }
                }
            }
        }
        return status;
    }

    boolean nonDeterministicStateContained(AbstractState as) {
        CompositeState cs = (CompositeState) as;
        ValueAnalysisState vls =
                (ValueAnalysisState) cs.getContainedState(ValueAnalysisState.class);
        return vls.nonDeterministicMark;
    }
}
