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

import static org.sosy_lab.java_smt.test.ProverEnvironmentSubject.assertThat;

import java.util.Collection;
import java.util.NavigableSet;
import java.util.logging.Level;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverException;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;

@Options(prefix="cpa.value.legion")
public class LegionAlgorithm implements Algorithm {
    private final Algorithm algorithm;
    private final LogManager logger;
    private final int maxIterations;
    Solver solver;

    public LegionAlgorithm(
            final Algorithm algorithm,
            final LogManager pLogger,
            Configuration pConfig,
            ShutdownNotifier shutdownNotifier)
            throws InvalidConfigurationException {
        this.algorithm = algorithm;
        this.logger = pLogger;
        this.maxIterations = 1;
        // this.config
        pConfig.inject(this);
        this.solver = Solver.create(pConfig, logger, shutdownNotifier);
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
                this.logger.log(
                        Level.WARNING,
                        "Found violated property in input after " + (i + 1) + " iterations.");
                break;
            }

            // If no error was found, contained states are checked if non-deterministic (=random)
            // decisions where made. States with non-deterministic followers can be retried to make
            // different decisions.
            for (AbstractState state : reachedSet.asCollection()) {
                ValueAnalysisState vs =
                        AbstractStates.extractStateByType(state, ValueAnalysisState.class);

                if (vs.nonDeterministicMark) {
                    for (ARGState previous : ((ARGState) state).getParents()) {
                        reachedSet.reAddToWaitlist(previous);
                        try (ProverEnvironment prover =
                                solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
                            PredicateAbstractState ps = AbstractStates.extractStateByType(state, PredicateAbstractState.class);
                            BooleanFormula f = ps.getPathFormula().getFormula();
                            prover.push(f);
                            try {
                                logger.log(Level.INFO, "Pushed boolean formula: " + f.toString());
                                assertThat(prover).isSatisfiable();
                                // boolean isUnsat = prover.isUnsat();
                                // this.logger.log(Level.INFO, "Was unsat: " + isUnsat);
                                this.logger.log(Level.INFO, "Was Satisfiable");
                            } catch (SolverException ex){
                                this.logger.log(Level.WARNING, "Solver exception");
                            }
                        }
                        this.logger
                                .log(Level.INFO, "Added state to waitlist: " + previous.toString());
                    }
                }
            }
        }
        return status;
    }

    void predicateInfo(AbstractState as) {
        CompositeState cs = (CompositeState) as;
        PredicateAbstractState ps = (PredicateAbstractState) cs.getWrappedStates().asList().get(4);
        if (ps == null) {
            this.logger.log(Level.SEVERE, "No ps");
            return;
        }
        PathFormula f = ps.getPathFormula();
        this.logger.log(Level.SEVERE, "Formula: " + f);
        NavigableSet<String> vars = f.getSsa().allVariables();
        if (!vars.isEmpty()) {
            SSAMap map = f.getSsa();
            int index = map.getIndex("main::i");
            // m

            // this.logger.log(Level.SEVERE, f.getSsa().getIndex("main::i"));
            // this.logger.log(Level.SEVERE, vars.toArray()[0]);
        }
        // if (f.getSsa().containsVariable("main::i@2")){
        // }
    }
}
