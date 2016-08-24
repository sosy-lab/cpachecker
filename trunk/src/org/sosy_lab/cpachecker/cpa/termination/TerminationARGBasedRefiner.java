/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.termination;

import com.google.common.base.Preconditions;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.sosy_lab.common.configuration.ClassOption;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.AbstractARGBasedRefiner;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CPAs;

/**
 * Adds intermediate {@link CFAEdge}s created by {@link TerminationTransferRelation}
 * during the analysis to the {@link ARGPath} returned by
 * {@link #computePath(ARGState, ARGReachedSet)}.
 */
public class TerminationARGBasedRefiner extends AbstractARGBasedRefiner {

  @Options(prefix = "cpa.termination")
  private static class TerminationARGBasedRefinerConfig {

    @Option(
      secure = true,
      name = "refiner",
      required = true,
      description =
          "Which refinement algorithm to use? "
              + "(give class name, required for termination algorithm with CEGAR) "
              + "If the package name starts with 'org.sosy_lab.cpachecker.',"
              + " this prefix can be omitted."
    )
    @ClassOption(packagePrefix = "org.sosy_lab.cpachecker")
    @SuppressFBWarnings("NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
    private Refiner.Factory refinerFactory;

    public TerminationARGBasedRefinerConfig(Configuration config)
        throws InvalidConfigurationException {
      config.inject(this);
    }
  }

  public static TerminationARGBasedRefiner create(ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException, CPAException {
    TerminationCPA terminationCPA = CPAs.retrieveCPA(pCpa, TerminationCPA.class);
    if (terminationCPA == null) {
      throw new InvalidConfigurationException("Termination CPA needed for refinement");
    }

    Refiner.Factory refinerFactory =
        new TerminationARGBasedRefinerConfig(terminationCPA.getConfig()).refinerFactory;
    Refiner refiner = refinerFactory.create(pCpa);

    if (refiner instanceof AbstractARGBasedRefiner) {
      return new TerminationARGBasedRefiner((AbstractARGBasedRefiner) refiner, terminationCPA);
    } else {
      throw new InvalidConfigurationException(
          TerminationARGBasedRefiner.class.getSimpleName() + " requires ARGBasedRefiner.");
    }
  }

  private final TerminationCPA terminationCPA;

  private TerminationARGBasedRefiner(
      AbstractARGBasedRefiner pWrappedRefine, TerminationCPA pTerminationCPA) {
    super(pWrappedRefine);
    terminationCPA = Preconditions.checkNotNull(pTerminationCPA);
  }

  @Override
  protected ARGPath computePath(ARGState pLastElement, ARGReachedSet pReached)
      throws InterruptedException, CPATransferException {
    ARGPath basicPath = ARGUtils.getOnePathTo(pLastElement);
    return new TerminationARGPath(basicPath, terminationCPA.getTerminationInformation());
  }
}
