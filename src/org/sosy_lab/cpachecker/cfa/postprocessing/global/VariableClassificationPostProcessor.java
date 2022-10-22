// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.global;

import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CfaPostProcessor;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassificationBuilder;

public final class VariableClassificationPostProcessor implements CfaPostProcessor {

  private final Configuration configuration;

  public VariableClassificationPostProcessor(Configuration pConfiguration) {
    configuration = pConfiguration;
  }

  @Override
  public MutableCFA process(MutableCFA pCfa, LogManager pLogger) {

    try {
      VariableClassificationBuilder builder =
          new VariableClassificationBuilder(configuration, pLogger);
      VariableClassification variableClassification = builder.build(pCfa);
      pCfa.setVariableClassification(variableClassification);
    } catch (UnrecognizedCodeException | InvalidConfigurationException ex) {
      pLogger.log(Level.WARNING, ex);
    }

    return pCfa;
  }
}
