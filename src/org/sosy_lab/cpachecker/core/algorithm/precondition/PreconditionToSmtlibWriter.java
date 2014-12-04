/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.precondition;

import java.io.IOException;
import java.io.Writer;
import java.util.logging.Level;

import javax.annotation.Nonnull;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.precondition.interfaces.PreconditionWriter;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.base.Preconditions;

@Options
public class PreconditionToSmtlibWriter implements PreconditionWriter {

  private final FormulaManagerView fmgr;

  public PreconditionToSmtlibWriter(CFA pCfa, Configuration pConfig, LogManager pLogger, FormulaManagerView pFormulaManager)
          throws InvalidConfigurationException {

    pConfig.inject(this);
    fmgr = pFormulaManager;
  }

  public void writePrecondition(@Nonnull Appendable pWriteTo, @Nonnull ReachedSet pReached) throws IOException, CPATransferException, InterruptedException {
    Preconditions.checkNotNull(pWriteTo);

    BooleanFormula precondition = fmgr.simplify(
        fmgr.makeNot(PreconditionUtils.getPreconditionFromReached(
            fmgr, pReached,
            PreconditionPartition.VIOLATING)));

    // Write the formula in the SMT-LIB2 format to the target stream
    fmgr.dumpFormula(precondition).appendTo(pWriteTo);
  }

  @Override
  public void writePrecondition(Path pWriteTo, ReachedSet pReached) throws IOException {

    try (Writer w = Files.openOutputFile(pWriteTo)) {
      writePrecondition(pWriteTo, pReached);
    }
  }

  public void writePrecondition(Path pWriteTo, ReachedSet pReached, @Nonnull LogManager pCatchExceptionsTo) {
    Preconditions.checkNotNull(pCatchExceptionsTo);

    try {
      writePrecondition(pWriteTo, pReached);

    } catch (Exception e) {
      pCatchExceptionsTo.logException(Level.WARNING, e, "Writing reaching paths failed!");
    }
  }

}
