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
import org.sosy_lab.common.configuration.IntegerOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.InterpolatingTheoremProver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.TheoremProver;
import org.sosy_lab.cpachecker.util.predicates.mathsat.ArithmeticMathsatFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.mathsat.BitwiseMathsatFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.mathsat.MathsatFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.mathsat.MathsatInterpolatingProver;
import org.sosy_lab.cpachecker.util.predicates.mathsat.MathsatTheoremProver;
import org.sosy_lab.cpachecker.util.predicates.mathsat.YicesTheoremProver;
import org.sosy_lab.cpachecker.util.predicates.mathsat5.ArithmeticMathsat5FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.mathsat5.BitwiseMathsat5FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.mathsat5.Mathsat5FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.mathsat5.Mathsat5InterpolatingProver;
import org.sosy_lab.cpachecker.util.predicates.mathsat5.Mathsat5TheoremProver;
import org.sosy_lab.cpachecker.util.predicates.smtInterpol.ArithmeticSmtInterpolFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smtInterpol.SmtInterpolFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smtInterpol.SmtInterpolInterpolatingProver;
import org.sosy_lab.cpachecker.util.predicates.smtInterpol.SmtInterpolTheoremProver;

@Options(prefix="cpa.predicate")
public class FormulaManagerFactory {

  @Options(prefix="cpa.predicate.mathsat")
  private static class MathsatOptions {

    @Option(description="encode program variables as INTEGERs in MathSAT, instead of "
        + "using REALs. Since interpolation is not really supported by the laz solver, "
        + "when computing interpolants we still use the LA solver, "
        + "but encoding variables as ints might still be a good idea: "
        + "we can tighten strict inequalities, and split negated equalities")
    private boolean useIntegers = false;

    @Option(description="Encode program variables of bitvectors of a fixed size,"
        + "instead of using REALS. No interpolation and thus no refinement is"
        + "supported in this case.")
    private boolean useBitwise = false;

    @Option(description="With of the bitvectors if useBitwise is true.")
    @IntegerOption(min=1, max=128)
    private int bitWidth = 32;
  }

  @Options(prefix="cpa.predicate.smtinterpol")
  private static class SmtInterpolOptions {

    @Option(description="encode program variables as INTEGERs in SmtInterpol, "
        + "instead of using REALs.")
    private boolean useIntegers = false;
  }

  @Option(values={"MATHSAT4", "MATHSAT5", "YICES", "SMTINTERPOL"}, toUppercase=true,
      description="Whether to use MathSAT 4, MathSAT 5 or YICES (in combination with Mathsat 4) as SMT solver")
  private String solver = "MATHSAT4";

  private final FormulaManager fmgr;
  private final TheoremProver prover;

  public FormulaManagerFactory(Configuration config, LogManager logger) throws InvalidConfigurationException {
    config.inject(this);

    if (solver.equals("SMTINTERPOL")) {
      SmtInterpolOptions options = new SmtInterpolOptions();
      config.inject(options);
      fmgr = new ArithmeticSmtInterpolFormulaManager(config, logger, options.useIntegers);
      prover = new SmtInterpolTheoremProver((SmtInterpolFormulaManager) fmgr);
      return;
    }

    MathsatOptions options = new MathsatOptions();
    config.inject(options);

    if (options.useBitwise && options.useIntegers) {
      throw new InvalidConfigurationException("Can use either integers or bitvecors, not both!");
    }

    if (solver.equals("MATHSAT5")) {
      try {
        if (options.useBitwise) {
          fmgr = new BitwiseMathsat5FormulaManager(config, logger, options.bitWidth);

        } else {
          fmgr = new ArithmeticMathsat5FormulaManager(config, logger, options.useIntegers);
        }

      } catch (UnsatisfiedLinkError e) {
        if (e.getMessage() != null && e.getMessage().contains("mathsat5j")) {
          throw new InvalidConfigurationException("MathSAT 5 is not supported on your platform, please use another SMT solver.", e);
        } else {
          throw e;
        }
      }

      prover = new Mathsat5TheoremProver((Mathsat5FormulaManager) fmgr);

    } else {
      assert solver.equals("MATHSAT4") || solver.equals("YICES");

      try {
        if (options.useBitwise) {
          fmgr = new BitwiseMathsatFormulaManager(config, logger, options.bitWidth);

        } else {
          fmgr = new ArithmeticMathsatFormulaManager(config, logger, options.useIntegers);
        }

      } catch (UnsatisfiedLinkError e) {
        if (e.getMessage() != null && e.getMessage().contains("mathsatj")) {
          throw new InvalidConfigurationException("MathSAT 4 is not supported on your platform, please use another SMT solver.", e);
        } else {
          throw e;
        }
      }

      if (solver.equals("YICES")) {
        try {
          prover = new YicesTheoremProver((MathsatFormulaManager) fmgr, logger);

        } catch (UnsatisfiedLinkError e) {
          if (e.getMessage() != null && e.getMessage().contains("YicesLite")) {
            throw new InvalidConfigurationException("In order to use the Yices SMT solver, you need to install the library YicesLite.", e);
          } else {
            throw e;
          }
        }

      } else {
        assert solver.equals("MATHSAT4");
        prover = new MathsatTheoremProver((MathsatFormulaManager) fmgr);
      }
    }
  }

  public FormulaManager getFormulaManager() {
    return fmgr;
  }

  public TheoremProver createTheoremProver() {
    return prover;
  }

  public InterpolatingTheoremProver<?> createInterpolatingTheoremProver(boolean shared) {
    if (solver.equals("MATHSAT5")) {
      return new Mathsat5InterpolatingProver((Mathsat5FormulaManager) fmgr, shared);

    } else if (solver.equals("SMTINTERPOL")) {
      return new SmtInterpolInterpolatingProver((SmtInterpolFormulaManager) fmgr, shared);
    } else {
      assert solver.equals("MATHSAT4") || solver.equals("YICES");
      return new MathsatInterpolatingProver((MathsatFormulaManager) fmgr, shared);
    }
  }
}
