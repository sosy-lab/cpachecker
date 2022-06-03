// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.pcc.strategy;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;

public abstract class SequentialReadStrategy extends AbstractStrategy {

  protected SequentialReadStrategy(Configuration pConfig, LogManager pLogger, Path pProofFile)
      throws InvalidConfigurationException {
    super(pConfig, pLogger, pProofFile);
  }

  @Override
  protected void writeProofToStream(
      ObjectOutputStream out, UnmodifiableReachedSet reached, ConfigurableProgramAnalysis pCpa)
      throws IOException, InvalidConfigurationException {
    out.writeObject(getProofToWrite(reached, pCpa));
  }

  @Override
  protected void readProofFromStream(ObjectInputStream in)
      throws ClassNotFoundException, InvalidConfigurationException, IOException {
    prepareForChecking(in.readObject());
  }

  protected abstract Object getProofToWrite(
      UnmodifiableReachedSet pReached, ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException;

  protected abstract void prepareForChecking(Object pReadObject)
      throws InvalidConfigurationException;
}
