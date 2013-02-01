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

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.InterpolatingTheoremProver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.mathsat5.Mathsat5FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.mathsat5.Mathsat5InterpolatingProver;
import org.sosy_lab.cpachecker.util.predicates.mathsat5.Mathsat5Settings;
import org.sosy_lab.cpachecker.util.predicates.mathsat5.Mathsat5TheoremProver;
import org.sosy_lab.cpachecker.util.predicates.smtInterpol.SmtInterpolFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smtInterpol.SmtInterpolInterpolatingProver;
import org.sosy_lab.cpachecker.util.predicates.smtInterpol.SmtInterpolTheoremProver;

@Options(prefix="cpa.predicate")
public class FormulaManagerFactory {

  private static final String MATHSAT5 = "MATHSAT5";
  private static final String SMTINTERPOL = "SMTINTERPOL";

  @Option(name="solver.useIntegers",
      description="Encode program variables as INTEGER variables, instead of "
      + "using REALs. Not all solvers might support this.")
  private boolean useIntegers = false;

  @Option(name="solver.useBitvectors",
      description="Encode program variables as bitvectors of a fixed size,"
      + "instead of using REALS. Not all solvers might support this.")
  private boolean useBitvectors = false;

//  @Option(name="solver.bitWidth",
//      description="With of the bitvectors if useBitvectors is true.")
//  @IntegerOption(min=1, max=128)
//  private int bitWidth = 32;
//
//  @Option(name="solver.signed",
//      description="Whether to use signed or unsigned variables if useBitvectors is true.")
//  private boolean signed = true;

  @Option(values={MATHSAT5, SMTINTERPOL}, toUppercase=true,
      description="Whether to use MathSAT 5 or SmtInterpol as SMT solver")
  private String solver = MATHSAT5;

  private final FormulaManager fmgr;

  public FormulaManagerFactory(Configuration config, LogManager logger) throws InvalidConfigurationException {
    config.inject(this);


    if (useBitvectors && useIntegers) {
      throw new InvalidConfigurationException("Can use either integers or bitvecors, not both!");
    }


    FormulaManager lFmgr;
    if (solver.equals(SMTINTERPOL)) {
      if (useBitvectors) {
        throw new InvalidConfigurationException("Using bitvectors for program variables is not supported when SMTInterpol is used.");
      }
      lFmgr = org.sosy_lab.cpachecker.util.predicates.smtInterpol.SmtInterpolFormulaManager
          .create(config, logger, useIntegers);

    } else {
      try {
        assert solver.equals(MATHSAT5);

        Mathsat5Settings settings = new Mathsat5Settings(config);
        lFmgr = Mathsat5FormulaManager.create(logger, settings);

      } catch (UnsatisfiedLinkError e) {
        throw new InvalidConfigurationException("The SMT solver " + solver
            + " is not available on this machine."
            + " You may experiment with SMTInterpol by setting cpa.predicate.solver=SMTInterpol.", e);
      }
    }

    fmgr = lFmgr;
  }

  public FormulaManager getFormulaManager() {
    return fmgr;
  }

  public ProverEnvironment newProverEnvironment() {
    if (solver.equals(SMTINTERPOL)) {
      return new SmtInterpolTheoremProver((SmtInterpolFormulaManager)fmgr);
    } else {
      return new Mathsat5TheoremProver((Mathsat5FormulaManager)fmgr);
    }
  }

  public InterpolatingTheoremProver<?> newProverEnvironmentWithInterpolation(boolean shared) {
    if (solver.equals(SMTINTERPOL)) {
      return new SmtInterpolInterpolatingProver((SmtInterpolFormulaManager) fmgr);
    } else {
      assert solver.equals(MATHSAT5);
      return new Mathsat5InterpolatingProver((Mathsat5FormulaManager) fmgr, shared);
    }
  }
}
