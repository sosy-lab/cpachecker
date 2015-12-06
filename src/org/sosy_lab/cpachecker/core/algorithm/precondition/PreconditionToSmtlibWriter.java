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

import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.precondition.interfaces.PreconditionWriter;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.solver.api.BooleanFormula;

import com.google.common.base.Preconditions;

public class PreconditionToSmtlibWriter implements PreconditionWriter {

  private final FormulaManagerView fmgr;

  public PreconditionToSmtlibWriter(FormulaManagerView pFormulaManager) {
    fmgr = pFormulaManager;
  }

  public void writePrecondition(@Nonnull Appendable pWriteTo, @Nonnull BooleanFormula pPrecondition)
      throws IOException {
    Preconditions.checkNotNull(pWriteTo);

    final BooleanFormula precondition = fmgr.simplify(pPrecondition);

    // Write the formula in the SMT-LIB2 format to the target stream
    if (fmgr.getBooleanFormulaManager().isTrue(precondition)) {
      pWriteTo.append("(assert true)"); // Hack; dumpFormula might write an empty string in this case; TODO: General solution in dumpFormula required!
    } else {
      fmgr.dumpFormula(precondition).appendTo(pWriteTo);
    }
  }

  public void writePrecondition(Path pWriteTo, BooleanFormula pPrecondition, @Nonnull LogManager pCatchExceptionsTo) {
    Preconditions.checkNotNull(pCatchExceptionsTo);

    try {
      writePrecondition(pWriteTo, pPrecondition);

    } catch (Exception e) {
      pCatchExceptionsTo.logException(Level.WARNING, e, "Writing reaching paths failed!");
    }
  }

  @Override
  public void writePrecondition(Path pWriteTo, BooleanFormula pPrecondition) throws IOException, CPATransferException, InterruptedException {
    try (Writer w = Files.openOutputFile(pWriteTo)) {
      writePrecondition(w, pPrecondition);
    }
  }

}
