// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.svlibsafetyspec;

import com.google.common.collect.ImmutableSet;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithBAM;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.specification.SvLibSpecificationInformation;

@Options(prefix = "svlib.specification")
public class SvLibSafetySpecCPA extends AbstractCPA implements ConfigurableProgramAnalysisWithBAM {

  private final LogManager logger;
  private final CFA cfa;
  private final SvLibSpecificationInformation specInfo;

  @Option(
      secure = true,
      description =
          "Should we do witness validation or not?\n"
              + "Witness validation means that we require the witness to be "
              + "sufficient to proof all the specifications, which means "
              + "that we only follow paths where modular abstractions "
              + "are used, not exploring the original program on our own.\n"
              + "In case this option we additionally explore all paths "
              + "of the original program in addition to the paths using "
              + "modular abstractions.")
  private boolean doWitnessValidation = false;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(SvLibSafetySpecCPA.class);
  }

  private SvLibSafetySpecCPA(
      CFA pCFA, SvLibSpecificationInformation pSpecInfo, LogManager pLogger, Configuration pConfig)
      throws InvalidConfigurationException {
    super("sep", "sep", null);
    pConfig.inject(this);
    cfa = pCFA;
    specInfo = pSpecInfo;
    logger = pLogger;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return new SvLibSafetySpecTransferRelation(cfa, specInfo, logger, doWitnessValidation);
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    return new SvLibSafetySpecAssumptionState(ImmutableSet.of(), ImmutableSet.of(), false);
  }
}
