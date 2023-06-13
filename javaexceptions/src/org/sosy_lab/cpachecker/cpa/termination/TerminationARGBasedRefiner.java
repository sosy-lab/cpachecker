// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.termination;

import com.google.common.base.Preconditions;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.ClassOption;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.AbstractARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CPAs;

/**
 * Adds intermediate {@link CFAEdge}s created by {@link TerminationTransferRelation} during the
 * analysis to the {@link ARGPath} returned by {@link #computePath(ARGState, ARGReachedSet)}.
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
                + " this prefix can be omitted.")
    @ClassOption(packagePrefix = "org.sosy_lab.cpachecker")
    @SuppressFBWarnings("NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
    private Refiner.Factory refinerFactory;

    public TerminationARGBasedRefinerConfig(Configuration config)
        throws InvalidConfigurationException {
      config.inject(this);
    }
  }

  public static TerminationARGBasedRefiner create(
      ConfigurableProgramAnalysis pCpa, LogManager pLogger, ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    TerminationCPA terminationCPA =
        CPAs.retrieveCPAOrFail(pCpa, TerminationCPA.class, TerminationCPA.class);
    Refiner.Factory refinerFactory =
        new TerminationARGBasedRefinerConfig(terminationCPA.getConfig()).refinerFactory;
    Refiner refiner = refinerFactory.create(pCpa, pLogger, pShutdownNotifier);

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
