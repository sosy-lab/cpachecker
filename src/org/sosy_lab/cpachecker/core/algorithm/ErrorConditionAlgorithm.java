// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.TestAlgorithmFactory.AnalysisComponents;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.cpa.automaton.ControlAutomatonCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;

public class ErrorConditionAlgorithm implements Algorithm {

  private final Configuration config;
  private final LogManager logger;
  private final CFA cfa;
  private final Specification specification;
  private final ConfigurableProgramAnalysis cpa;

  public ErrorConditionAlgorithm(
      Configuration pConfig,
      LogManager pLogger,
      CFA pCfa,
      Specification pSpecification,
      ConfigurableProgramAnalysis pCPA) {
    config = pConfig;
    logger = pLogger;
    cfa = pCfa;
    specification = pSpecification;
    cpa = pCPA;
  }

  private boolean runcheck(AnalysisComponents pcomp) throws CPAException, InterruptedException {
    AlgorithmStatus status = AlgorithmStatus.SOUND_AND_PRECISE;
    while (pcomp.reached().hasWaitingState() && status.isPrecise() && status.isSound()) {
      status = status.update(pcomp.algorithm().run(pcomp.reached()));
    }
    if (!status.isSound() || !status.isPrecise()) {
      throw new CPAException(
          "Error in CPA analysis: precise: " + status.isPrecise() + " sound: " + status.isSound());
    }
    for (AbstractState state : pcomp.reached()) {
      FluentIterable<AutomatonState> states = AbstractStates.asIterable(state).filter(AutomatonState.class);
      if (states.size() != 2) {
        throw new CPAException("More than two automaton states in reached set: " + states.toList());
      }
      int targetCounter = states.stream().mapToInt(i -> i.isTarget() ? 1 : 0).sum();
      if (targetCounter == 2) {
        return false;
      }
    }
    return true;
    //return FluentIterable.from(pcomp.reached()).filter(AbstractStates::isTargetState).isEmpty();
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    // get cpa
    // extract specification automaton
    // create copy with inverted states and replace it in the CPA
    try {
      ImmutableList.Builder<ConfigurableProgramAnalysis> ctrlcpalist = ImmutableList.builder();
      for (ConfigurableProgramAnalysis c : CPAs.asIterable(cpa)) {
        if (!(c instanceof ARGCPA) && !(c instanceof CompositeCPA)) {
          if (c instanceof ControlAutomatonCPA ctrlcpa) {
            if (ctrlcpa.getAutomatonName().startsWith("SVCOMP")) {
              ctrlcpalist.add(ctrlcpa.invert());
            } else {
              ctrlcpalist.add(ctrlcpa.addselfloop());
            }
          } else {
            ctrlcpalist.add(c);
          }
        }
      }

      Configuration predicateConfig =
          Configuration.builder().loadFromFile("config/predicateAnalysis.properties").build();
      ARGCPA cpaSpec =
          new ARGCPA(
              new CompositeCPA(config, cfa, ctrlcpalist.build()),
              config,
              logger,
              specification,
              cfa);
      AnalysisComponents aSpec =
          TestAlgorithmFactory.createAlgorithm(
              logger, specification, cfa, predicateConfig, ShutdownManager.create(), cpaSpec);
      boolean resultSpec = runcheck(aSpec);
      if (resultSpec) {
        logger.log(Level.INFO, "Witness complete");
      } else {
        logger.log(Level.INFO, "Witness incomplete");
      }


      ImmutableList.Builder<ConfigurableProgramAnalysis> obscpalist = ImmutableList.builder();
      for (ConfigurableProgramAnalysis c : CPAs.asIterable(cpa)) {
        if (!(c instanceof ARGCPA) && !(c instanceof CompositeCPA)) {
          if (c instanceof ControlAutomatonCPA obscpa) {
            if (obscpa.getAutomatonName().startsWith("WitnessAutomaton")) {
              obscpalist.add(obscpa.invert());
            } else {
              obscpalist.add(obscpa.addselfloop());
            }
          } else {
            obscpalist.add(c);
          }
        }
      }

      ARGCPA cpaWitness =
          new ARGCPA(
              new CompositeCPA(config, cfa, obscpalist.build()),
              config,
              logger,
              specification,
              cfa);
      AnalysisComponents aWitness =
          TestAlgorithmFactory.createAlgorithm(
              logger, specification, cfa, predicateConfig, ShutdownManager.create(), cpaWitness);
      boolean resultWitness = runcheck(aWitness);
      if (resultWitness) {
        logger.log(Level.INFO, "Witness sound");
      } else {
        logger.log(Level.INFO, "Witness unsound");
      }
      if(true){
        pReachedSet.clear();
        aWitness.reached().forEach(s -> pReachedSet.add(s, aWitness.reached().getPrecision(s)));
        pReachedSet.clearWaitlist();
        return AlgorithmStatus.SOUND_AND_PRECISE;
      }

    } catch (CPATransferException e) {
      throw new CPAException("Transfer not possible", e);
    } catch (InvalidConfigurationException e) {
      throw new CPAException("Configuration invalid", e);
    } catch (InterruptedException e) {
      throw new CPAException("User interrupted execution", e);
    } catch (IOException e) {
      throw new CPAException("predicateAnalysis properties not found", e);
    }
    // extract violation automaton
    // create copy with inverted states and replace it in the CPA
    return AlgorithmStatus.NO_PROPERTY_CHECKED;
    // run CPA
  }
}
