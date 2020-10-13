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
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import java.util.Set;

public class LegionAlgorithm implements Algorithm {
    private final Algorithm algorithm;
    private final LogManager logger;

    public LegionAlgorithm(final Algorithm algorithm, final LogManager pLogger) {
        this.algorithm = algorithm;
        this.logger = pLogger;
    }

    @Override
    public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException, CPAEnabledAnalysisPropertyViolationException {
        logger.log(Level.INFO, "Running legion algorithm");

        for (int i=0; i < 10; i++){
            algorithm.run(reachedSet);
            Set<AbstractState> collection = reachedSet.asCollection();

            int trues = 0;
            int falses = 0;
            for (AbstractState state : collection){
                
                ImmutableList<AbstractState> wrappedStates = ((AbstractSingleWrapperState)state).getWrappedStates();

                for (AbstractState as : wrappedStates) {
                    CompositeState cs = (CompositeState) as;
                    ValueAnalysisState vls = (ValueAnalysisState) cs.getContainedState(ValueAnalysisState.class);
                    if (vls.nonDeterministicMark) {
                        reachedSet.reAddToWaitlist(state);
                        // ----------------("Added to waitlist");
                        trues += 1;
                    } else {
                        falses += 1;
                    }
                }
                
                // Set<Entry<MemoryLocation, ValueAndType>> constants = vls.getConstants();
                // for (Entry<MemoryLocation, ValueAndType> c : constants){
                    //     Value v = c.getValue().getValue();
                    
                    //     ------------------(v.toString());
                    // }
            }
            // ------------------("True: " + trues + " False: " + falses);
            // ------------------("Testing");
        }
            
        return AlgorithmStatus.NO_PROPERTY_CHECKED;
    }
}

// /usr/app/bin:/usr/app/lib/apron.jar:/usr/app/lib/edu.cmu.sei.rtss.jldd.jar:/usr/app/lib/gmp.jar:/usr/app/lib/jna.jar:/usr/app/lib/jpl.jar:/usr/app/lib/jsylvan.jar:/usr/app/lib/meconsole010.jar:/usr/app/lib/java/runtime/CVC4.jar:/usr/app/lib/java/runtime/animal-sniffer-annotations.jar:/usr/app/lib/java/runtime/antlr4-runtime.jar:/usr/app/lib/java/runtime/batik-awt-util.jar:/usr/app/lib/java/runtime/batik-constants.jar:/usr/app/lib/java/runtime/batik-dom.jar:/usr/app/lib/java/runtime/batik-ext.jar:/usr/app/lib/java/runtime/batik-i18n.jar:/usr/app/lib/java/runtime/batik-svggen.jar:/usr/app/lib/java/runtime/batik-util.jar:/usr/app/lib/java/runtime/batik-xml.jar:/usr/app/lib/java/runtime/checker-qual.jar:/usr/app/lib/java/runtime/com.microsoft.z3.jar:/usr/app/lib/java/runtime/common.jar:/usr/app/lib/java/runtime/error_prone_annotations.jar:/usr/app/lib/java/runtime/failureaccess.jar:/usr/app/lib/java/runtime/guava.jar:/usr/app/lib/java/runtime/icu4j.jar:/usr/app/lib/java/runtime/j2objc-annotations.jar:/usr/app/lib/java/runtime/java-cup-runtime.jar:/usr/app/lib/java/runtime/java-smt.jar:/usr/app/lib/java/runtime/javabdd.jar:/usr/app/lib/java/runtime/jdd.jar:/usr/app/lib/java/runtime/jhoafparser.jar:/usr/app/lib/java/runtime/jna.jar:/usr/app/lib/java/runtime/jsr305.jar:/usr/app/lib/java/runtime/lasso-ranker.jar:/usr/app/lib/java/runtime/listenablefuture.jar:/usr/app/lib/java/runtime/llvm-j.jar:/usr/app/lib/java/runtime/org.eclipse.cdt.core.jar:/usr/app/lib/java/runtime/org.eclipse.core.contenttype.jar:/usr/app/lib/java/runtime/org.eclipse.core.jobs.jar:/usr/app/lib/java/runtime/org.eclipse.core.resources.jar:/usr/app/lib/java/runtime/org.eclipse.core.runtime.jar:/usr/app/lib/java/runtime/org.eclipse.equinox.common.jar:/usr/app/lib/java/runtime/org.eclipse.equinox.preferences.jar:/usr/app/lib/java/runtime/org.eclipse.jdt.core.jar:/usr/app/lib/java/runtime/org.eclipse.osgi.jar:/usr/app/lib/java/runtime/pjbdd.jar:/usr/app/lib/java/runtime/princess-parser_2.12.jar:/usr/app/lib/java/runtime/princess-smt-parser_2.12.jar:/usr/app/lib/java/runtime/princess_2.12.jar:/usr/app/lib/java/runtime/scala-library.jar:/usr/app/lib/java/runtime/scala-parser-combinators_2.12.jar:/usr/app/lib/java/runtime/smtinterpol.jar:/usr/app/lib/java/runtime/spotbugs-annotations.jar:/usr/app/lib/java/runtime/ultimate-core-rcp.jar:/usr/app/lib/java/runtime/ultimate-core.jar:/usr/app/lib/java/runtime/ultimate-icfg-transformer.jar:/usr/app/lib/java/runtime/ultimate-java-cup.jar:/usr/app/lib/java/runtime/ultimate-library-smtlib.jar:/usr/app/lib/java/runtime/ultimate-model-checker-utils.jar:/usr/app/lib/java/runtime/ultimate-model.jar:/usr/app/lib/java/runtime/ultimate-smt-solver-bridge.jar:/usr/app/lib/java/runtime/ultimate-util.jar:/usr/app/lib/java/test/auto-value-annotations.jar:/usr/app/lib/java/test/byte-buddy-agent.jar:/usr/app/lib/java/test/byte-buddy.jar:/usr/app/lib/java/test/checker-compat-qual.jar:/usr/app/lib/java/test/checker-qual.jar:/usr/app/lib/java/test/diffutils.jar:/usr/app/lib/java/test/error_prone_annotations.jar:/usr/app/lib/java/test/failureaccess.jar:/usr/app/lib/java/test/guava-testlib.jar:/usr/app/lib/java/test/guava.jar:/usr/app/lib/java/test/hamcrest-core.jar:/usr/app/lib/java/test/j2objc-annotations.jar:/usr/app/lib/java/test/jsr305.jar:/usr/app/lib/java/test/junit.jar:/usr/app/lib/java/test/listenablefuture.jar:/usr/app/lib/java/test/mockito-core.jar:/usr/app/lib/java/test/objenesis.jar:/usr/app/lib/java/test/truth-java8-extension.jar:/usr/app/lib/java/test/truth.jar
