// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;

import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.TestAlgorithmFactory.AnalysisComponents;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.automaton.ControlAutomatonCPA;
import org.sosy_lab.cpachecker.cpa.automaton.ObserverAutomatonCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;

public class ViolationWitnessAlgorithm implements Algorithm{

  private final Configuration config;
  private final LogManager logger;
  private final CFA cfa;
  private final Specification specification;
  private final ConfigurableProgramAnalysis cpa;

  public ViolationWitnessAlgorithm(
    Configuration pConfig, LogManager pLogger, CFA pCfa, Specification pSpecification, ConfigurableProgramAnalysis pCPA)
    throws InvalidConfigurationException {
    config = pConfig;
    config.inject(this);
    logger = pLogger;
    cfa = pCfa;
    specification = pSpecification;
    cpa = pCPA;
  }



  private boolean runcheck(AnalysisComponents pcomp){
      return !FluentIterable.from(pcomp.reached()).filter(AbstractStates::isTargetState).isEmpty();
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    // get cpa
    // extract specification automaton
    // create copy with inverted states and replace it in the CPA
  try {
    ImmutableList.Builder<ConfigurableProgramAnalysis> ctrlcpalist = ImmutableList.builder();
    for(ConfigurableProgramAnalysis c : CPAs.asIterable(cpa)){
      if(c instanceof ControlAutomatonCPA ctrlcpa){
          ctrlcpalist.add(ctrlcpa.invert());
      }
      else {
        ctrlcpalist.add(c);
      }
    }

    CompositeCPA cpaSpec = new CompositeCPA(config, cfa, ctrlcpalist.build()); 
    AnalysisComponents aSpec = TestAlgorithmFactory.createAlgorithm(logger, specification, cfa, config, ShutdownManager.create(), cpaSpec);
    boolean resultSpec = runcheck(aSpec);
    System.out.println(resultSpec);
  } catch (CPATransferException e) {
    throw new CPAException("Transfer not possible", e);
  } catch (InvalidConfigurationException e) {
    throw new CPAException("Configuration invalid", e);
  } catch (InterruptedException e) {
    throw new CPAException("User interrupted execution", e);
  }
    // extract violation automaton
    // create copy with inverted states and replace it in the CPA

  try{
    ImmutableList.Builder<ConfigurableProgramAnalysis> obscpalist = ImmutableList.builder();
    for(ConfigurableProgramAnalysis c : CPAs.asIterable(cpa)){
      if(c instanceof ObserverAutomatonCPA obscpa){
        obscpalist.add(obscpa.invert());
      }
      else {
        obscpalist.add(c);
      }
    }

    CompositeCPA cpaWitness = new CompositeCPA(config, cfa, obscpalist.build()); 
    AnalysisComponents aWitness = TestAlgorithmFactory.createAlgorithm(logger, specification, cfa, config, ShutdownManager.create(), cpaWitness);
    boolean resultWitness = runcheck(aWitness);
    System.out.println(resultWitness);
  } catch (CPATransferException e) {
    throw new CPAException("Transfer not possible", e);
  } catch (InvalidConfigurationException e) {
    throw new CPAException("Configuration invalid", e);
  } catch (InterruptedException e) {
    throw new CPAException("User interrupted execution", e);
  }
  return AlgorithmStatus.SOUND_AND_PRECISE;
    // run CPA
  }
}
