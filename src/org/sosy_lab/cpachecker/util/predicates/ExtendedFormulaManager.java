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

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

import org.sosy_lab.common.Files;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;

import com.google.common.base.Joiner;

/**
 * This class provides several helper methods for handling formulas.
 */
@Options(prefix="cpa.predicate")
public final class ExtendedFormulaManager extends ForwardingFormulaManager {

  @Option(name = "formulaDumpFilePattern", description = "where to dump interpolation and abstraction problems (format string)")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private File formulaDumpFile = new File("%s%04d-%s%03d.msat");
  private final String formulaDumpFilePattern;

  @Option(description="try to add some useful static-learning-like axioms for "
    + "bitwise operations (which are encoded as UFs): essentially, "
    + "we simply collect all the numbers used in bitwise operations, "
    + "and add axioms like (0 & n = 0)")
  private boolean useBitwiseAxioms = false;


  private final LogManager logger;

  private static final Joiner LINE_JOINER = Joiner.on('\n');

  public ExtendedFormulaManager(FormulaManager pFmgr, Configuration config, LogManager pLogger) throws InvalidConfigurationException {
    super(pFmgr);
    config.inject(this, ExtendedFormulaManager.class);

    logger = pLogger;

    if (formulaDumpFile != null) {
      formulaDumpFilePattern = formulaDumpFile.getAbsolutePath();
    } else {
      formulaDumpFilePattern = null;
    }
  }

  @Override
  public FormulaManager getDelegate() {
    return super.getDelegate();
  }

  public boolean useBitwiseAxioms() {
    return useBitwiseAxioms;
  }

  /**
   * Helper method to create the conjunction of all formulas in a list.
   */
  public Formula makeConjunction(List<Formula> f) {
    Formula result = this.makeTrue();
    for (Formula formula : f) {
      result = this.makeAnd(result, formula);
    }
    return result;
  }

  /**
   * Makes an implication from p to q.
   *
   * @return (p ⇒ q)
   */
  public Formula makeImplication(Formula p, Formula q) {
    Formula left = makeNot(p);
    return makeOr(left, q);
  }

  /**
   * Makes a Formula in which f1 and f2 are not equal.
   *
   * @return (f1 != f2)
   */
  public Formula makeNotEqual(Formula f1, Formula f2) {
    return makeNot(makeEqual(f1, f2));
  }

  public File formatFormulaOutputFile(String function, int call, String formula, int index) {
    if (formulaDumpFilePattern == null) {
      return null;
    }

    return new File(String.format(formulaDumpFilePattern, function, call, formula, index));
  }

  public void dumpFormulaToFile(Formula f, File outputFile) {
    if (outputFile != null) {
      try {
        Files.writeFile(outputFile, this.dumpFormula(f));
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Failed to save formula to file");
      }
    }
  }

  public void printFormulasToFile(Iterable<Formula> f, File outputFile) {
    if (outputFile != null) {
      try {
        Files.writeFile(outputFile, LINE_JOINER.join(f));
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Failed to save formula to file");
      }
    }
  }
}