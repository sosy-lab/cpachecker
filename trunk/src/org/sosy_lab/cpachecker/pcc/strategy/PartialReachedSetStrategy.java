// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.pcc.strategy;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.pcc.PartialReachedConstructionAlgorithm;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.PropertyChecker.PropertyCheckerCPA;
import org.sosy_lab.cpachecker.cpa.location.LocationCPABackwards;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.PartialCertificateTypeProvider;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Pair;

@Options(prefix = "pcc.partial")
public class PartialReachedSetStrategy extends ReachedSetStrategy {

  private final PartialReachedConstructionAlgorithm certificateConstructor;

  @Option(
      secure = true,
      description =
          "Enables proper PCC but may not work correctly for heuristics. Stops adding newly"
              + " computed elements to reached set if size saved in proof is reached. If another"
              + " element must be added, stops certificate checking and returns false.")
  protected boolean stopAddingAtReachedSetSize = false;

  protected int savedReachedSetSize;

  public PartialReachedSetStrategy(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      Path pProofFile,
      @Nullable PropertyCheckerCPA pCpa)
      throws InvalidConfigurationException {
    super(pConfig, pLogger, pShutdownNotifier, pProofFile, pCpa);
    pConfig.inject(this, PartialReachedSetStrategy.class);

    certificateConstructor =
        new PartialCertificateTypeProvider(pConfig, true).getPartialCertificateConstructor();
  }

  @Override
  public void constructInternalProofRepresentation(
      final UnmodifiableReachedSet pReached, final ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    savedReachedSetSize = pReached.size();
    reachedSet = certificateConstructor.computePartialReachedSet(pReached, pCpa);
    orderReachedSetByLocation(reachedSet);
  }

  @Override
  protected Object getProofToWrite(
      UnmodifiableReachedSet pReached, ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    constructInternalProofRepresentation(pReached, pCpa);
    return Pair.of(pReached.size(), reachedSet);
  }

  @Override
  protected void prepareForChecking(Object pReadProof) throws InvalidConfigurationException {
    if (CPAs.retrieveCPA(cpa, LocationCPABackwards.class) != null) {
      throw new InvalidConfigurationException(
          "Partial reached set not supported as certificate for backward analysis");
    }
    if (!(pReadProof instanceof Pair)) {
      throw new InvalidConfigurationException(
          "Proof Type requires pair of reached set size and reached set as set of abstract"
              + " states.");
    }
    try {
      @SuppressWarnings("unchecked")
      Pair<Integer, AbstractState[]> proof = (Pair<Integer, AbstractState[]>) pReadProof;
      savedReachedSetSize = proof.getFirst();
      super.prepareForChecking(proof.getSecond());
    } catch (ClassCastException e) {
      throw new InvalidConfigurationException(
          "Proof Type requires pair of reached set size and reached set as set of abstract"
              + " states.");
    }
  }

  @Override
  public boolean checkCertificate(final ReachedSet pReachedSet)
      throws CPAException, InterruptedException {
    int certificateSize = 0;

    List<AbstractState> certificate = new ArrayList<>(savedReachedSetSize);
    certificate.addAll(Arrays.asList(reachedSet));

    /*also restrict stop to elements of same location as analysis does*/
    StopOperator stop = cpa.getStopOperator();
    Precision initialPrec = pReachedSet.getPrecision(pReachedSet.getFirstState());

    // check initial element
    AbstractState initialState = pReachedSet.popFromWaitlist();
    assert (initialState == pReachedSet.getFirstState() && pReachedSet.size() == 1);

    try {
      stats.stopTimer.start();
      if (!stop.stop(
          initialState,
          statesPerLocation.get(AbstractStates.extractLocation(initialState)),
          initialPrec)) {
        logger.log(
            Level.FINE,
            "Initial element not in partial reached set.",
            "Add to elements whose successors ",
            "must be computed.");
        certificate.add(initialState);
      }
    } catch (CPAException e) {
      logger.logException(Level.FINE, e, "Stop check failed for initial element.");
      return false;
    } finally {
      stats.stopTimer.stop();
    }

    // check if elements form transitive closure
    Collection<? extends AbstractState> successors;
    while (certificateSize < certificate.size()) {

      shutdownNotifier.shutdownIfNecessary();
      stats.countIterations++;

      try {
        stats.transferTimer.start();
        successors =
            cpa.getTransferRelation()
                .getAbstractSuccessors(certificate.get(certificateSize++), initialPrec);
        stats.transferTimer.stop();

        for (AbstractState succ : successors) {
          try {
            stats.stopTimer.start();
            if (!stop.stop(
                succ, statesPerLocation.get(AbstractStates.extractLocation(succ)), initialPrec)) {
              logger.log(
                  Level.FINE,
                  "Successor ",
                  succ,
                  " not in partial reached set.",
                  "Add to elements whose successors ",
                  "must be computed.");
              if (stopAddingAtReachedSetSize && savedReachedSetSize == certificate.size()) {
                return false;
              }
              certificate.add(succ);
            }
          } finally {
            stats.stopTimer.stop();
          }
        }
      } catch (CPATransferException e) {
        logger.logException(Level.FINE, e, "Computation of successors failed.");
        return false;
      } catch (CPAException e) {
        logger.logException(Level.FINE, e, "Stop check failed for successor.");
        return false;
      }
    }
    stats.propertyCheckingTimer.start();
    try {
      return cpa.getPropChecker().satisfiesProperty(certificate);
    } finally {
      stats.propertyCheckingTimer.stop();
    }
  }
}
