/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.ProcessExecutor;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.InterpolatingTheoremProver;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;


/**
 * Wrapper class around CSIsat for interpolant generation.
 *
 * This class has some restrictions compared to the {@link InterpolatingTheoremProver}.
 *
 * It does not support to arbitrarily select formulas for groups A and B, instead
 * A has to be a contiguous range of formulas at the beginning of the list of formulas
 * (this means that the whole list is split at one point into A and B).
 *
 * Also, incremental satisfiability checks are not supported. After a call to
 * {@link #isUnsat()} which returned true, only calls to {@link #getInterpolant(List)}
 * are supported until {@link #reset()} has been called.
 */
public class CSIsatInterpolatingProver implements InterpolatingTheoremProver<Integer> {

  private static final Joiner FORMULAS_JOINER = Joiner.on("; ");
  private static final String[] CSISAT_CMDLINE = {"csisat", "-syntax", "infix", "-round", "-LAsolver", "simplex"};

  private class CSIsatExecutor extends ProcessExecutor<IOException> {

    private boolean satisfiable = false;

    public CSIsatExecutor() throws IOException {
      super(CSIsatInterpolatingProver.this.logger, IOException.class, CSISAT_CMDLINE);
    }

    @Override
    public void handleErrorOutput(String line) throws IOException {
      if (line.startsWith("Satisfiable: ")) {
        satisfiable = true;
      } else {
        super.handleErrorOutput(line);
      }
    }

    @Override
    public void handleOutput(String line) throws IOException {
      Formula itp = smgr.parseInfix(line);
      interpolants.add(itp);
      logger.log(Level.ALL, "Parsed interpolant", line, "as", itp);
    }

    public void writeFormulas(List<Formula> formulas) throws IOException {
      String formulasStr = FORMULAS_JOINER.join(formulas);

      logger.log(Level.ALL, "Interpolation problem is", formulasStr);

      print(formulasStr);
      sendEOF();
    }
  }

  private final FormulaManager smgr;
  private final LogManager logger;

  public CSIsatInterpolatingProver(FormulaManager pSmgr, LogManager pLogger) {
    Preconditions.checkNotNull(pSmgr);
    Preconditions.checkNotNull(pLogger);

    smgr = pSmgr;
    logger = pLogger;
  }

  private final List<Formula> formulas = new ArrayList<Formula>();
  private final List<Formula> interpolants = new ArrayList<Formula>();

  @Override
  public void init() {
    Preconditions.checkState(formulas.isEmpty());
    Preconditions.checkState(interpolants.isEmpty());
  }

  @Override
  public Integer addFormula(Formula pF) {
    Preconditions.checkNotNull(pF);
    Preconditions.checkState(interpolants.isEmpty(), "Cannot add formulas after calling unsat, call reset!");

    formulas.add(pF);
    return formulas.size()-1;
  }

  @Override
  public Formula getInterpolant(List<Integer> pFormulasOfA) {
    Preconditions.checkState(!interpolants.isEmpty(), "isUnsat needs to be called first!");

    int i = 0;
    for (Integer aIdx : pFormulasOfA) {
      Preconditions.checkArgument(aIdx.equals(i++), "CSIsatInterpolatingProver only accepts a contigous range at the beginning of the formulas for A");
    }
    i = i-1;
    Preconditions.checkElementIndex(i, interpolants.size(), "Invalid index for interpolant");

    return interpolants.get(i);
  }


  @Override
  public boolean isUnsat() throws InterruptedException {
    Preconditions.checkState(interpolants.isEmpty(), "Cannot call isUnsat after it returned true once!");

    CSIsatExecutor csisat;
    try {
      csisat = new CSIsatExecutor();
      csisat.writeFormulas(formulas);
      csisat.join();
    } catch (IOException e) {
      logger.logException(Level.SEVERE, e, "Error during invocation of CSIsat interpolating theorem prover!");
      throw new UnsupportedOperationException(e);
    }

    if (csisat.satisfiable) {
      logger.log(Level.FINEST, "CSIsat result: satisfiable");
      assert interpolants.isEmpty();
      return false;

    } else {
      if (interpolants.size() != formulas.size()-1) {
        logger.log(Level.SEVERE, "CSIsat failed to generate interpolants");
        throw new UnsupportedOperationException();
      }
      logger.log(Level.FINEST, "CSIsat result: unsatisfiable,", interpolants.size(), " interpolants found.");

      return true;
    }
  }

  @Override
  public void reset() {
    formulas.clear();
    interpolants.clear();
  }

  @Override
  public Model getModel() {
    return new Model(smgr);
  }

}