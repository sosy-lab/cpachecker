/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package org.sosy_lab.cpachecker.util.symbpredabstraction;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.InterpolatingTheoremProver;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormula;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormulaManager;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;

import org.sosy_lab.cpachecker.core.LogManager;

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
  private static final String CSISAT_CMDLINE = "csisat -syntax infix -round -LAsolver simplex";

  private final SymbolicFormulaManager smgr;
  private final LogManager logger;
  
  public CSIsatInterpolatingProver(SymbolicFormulaManager pSmgr, LogManager pLogger) {
    Preconditions.checkNotNull(pSmgr);
    Preconditions.checkNotNull(pLogger);
    
    smgr = pSmgr;
    logger = pLogger;    
  }
  
  private final List<SymbolicFormula> formulas = new ArrayList<SymbolicFormula>();
  private final List<SymbolicFormula> interpolants = new ArrayList<SymbolicFormula>();
  
  @Override
  public void init() {
    Preconditions.checkState(formulas.isEmpty());
    Preconditions.checkState(interpolants.isEmpty());
  }

  @Override
  public Integer addFormula(SymbolicFormula pF) {
    Preconditions.checkNotNull(pF);
    Preconditions.checkState(interpolants.isEmpty(), "Cannot add formulas after calling unsat, call reset!");
    
    formulas.add(pF);
    return formulas.size()-1;
  }

  @Override
  public SymbolicFormula getInterpolant(List<Integer> pFormulasOfA) {
    Preconditions.checkState(!interpolants.isEmpty(), "isUnsat needs to be called first!");
    
    int i = 0;
    for (Integer aIdx : pFormulasOfA) {
      Preconditions.checkArgument(aIdx.equals(i++), "CSIsatInterpolatingProver only accepts a contigous range at the beginning of the formulas for A");
    }
    i = i-1;
    Preconditions.checkArgument(i < interpolants.size(), "Invalid index for interpolant");
    
    return interpolants.get(i);
  }

  private String toCSIsat(List<SymbolicFormula> formulas) {
    String formulasStr = FORMULAS_JOINER.join(formulas);
    return formulasStr.replaceAll("@", "__at__")
                      .replaceAll("::", "__colon__")
                      .replaceAll("!", "not ")
                      ;
  }
  
  private SymbolicFormula fromCSIsat(String f) {
    String itp = f.replaceAll("__at__", "@")
                  .replaceAll("__colon__", "::")
                  .replaceAll(" not ", " ! ")
                  ;
    return smgr.parseInfix(itp);
  }
  
  @Override
  public boolean isUnsat() {
    Preconditions.checkState(interpolants.isEmpty(), "Cannot call isUnsat after it returned true once!");
    
    String formulasStr = toCSIsat(formulas);
        
    logger.log(Level.FINEST, "Calling CSIsat");
    logger.log(Level.ALL, "Interpolation problem is", formulasStr);
    
    Process csisat = null;
    BufferedReader csisatOut = null;
    BufferedReader csisatErr = null;
    PrintWriter csisatIn = null;
    try {
      try {
        csisat = Runtime.getRuntime().exec(CSISAT_CMDLINE);
        csisatOut = new BufferedReader(new InputStreamReader(csisat.getInputStream()));
        csisatErr = new BufferedReader(new InputStreamReader(csisat.getErrorStream()));
        csisatIn = new PrintWriter(csisat.getOutputStream());
        
        csisatIn.write(formulasStr);
        csisatIn.close();
        
        String line;
        while ((line = csisatOut.readLine()) != null) {
          if (line.startsWith("Satisfiable: ")) {
            logger.log(Level.FINEST, "CSIsat result: satisfiable");
            assert interpolants.isEmpty();
            return false;
          }
          
          SymbolicFormula itp = fromCSIsat(line);
          interpolants.add(itp);
          logger.log(Level.ALL, "Parsed interpolant", line, "as", itp);
        }
        
        StringBuilder errorText = new StringBuilder();
        while ((line = csisatErr.readLine()) != null) {
          errorText.append(line);
          errorText.append('\n');
        }
        if (errorText.length() != 0) {
          logger.log(Level.WARNING, "CSIsat produced unexpected output:\n", errorText);
        }
        
        if (interpolants.size() != formulas.size()-1) {
          logger.log(Level.SEVERE, "CSIsat failed to generate interpolants");
          throw new UnsupportedOperationException();
        }
        
        logger.log(Level.FINEST, "CSIsat result: unsatisfiable,", interpolants.size(), " interpolants found.");
        
      } catch (IOException e) {
        logger.logException(Level.SEVERE, e, "Error during invocation of CSIsat interpolating theorem prover!");
        throw new UnsupportedOperationException(e);
      }
    } finally {
      if (csisat != null) {
        csisat.destroy();
      }
      if (csisatErr != null) {
        try {
          csisatErr.close();
        } catch (IOException e) {
          logger.logException(Level.SEVERE, e, "Error during closing of input stream, ignored.");
        }
      }
      if (csisatOut != null) {
        try {
          csisatOut.close();
        } catch (IOException e) {
          logger.logException(Level.SEVERE, e, "Error during closing of input stream, ignored.");
        }
      }
      if (csisatIn != null) {
        csisatIn.close();
      }
    }
    
    return true;
  }

  @Override
  public void reset() {
    formulas.clear();
    interpolants.clear();
  }
}