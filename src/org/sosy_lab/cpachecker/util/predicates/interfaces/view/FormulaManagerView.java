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
package org.sosy_lab.cpachecker.util.predicates.interfaces.view;


import static com.google.common.base.Preconditions.*;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.Appender;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BitvectorFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BitvectorFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FunctionFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.IntegerFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.RationalFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.UnsafeFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.replacing.ReplacingFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;

@Options(prefix="cpa.predicate")
public class FormulaManagerView {

  public interface LoadManagers {
    public BooleanFormulaManagerView wrapManager(BooleanFormulaManager manager);
    public NumeralFormulaManagerView<IntegerFormula, IntegerFormula> wrapIntegerManager(NumeralFormulaManager<IntegerFormula, IntegerFormula> manager);
    public NumeralFormulaManagerView<NumeralFormula, RationalFormula> wrapRationalManager(NumeralFormulaManager<NumeralFormula, RationalFormula> manager);
    public BitvectorFormulaManagerView wrapManager(BitvectorFormulaManager manager);
    public FunctionFormulaManagerView wrapManager(FunctionFormulaManager pManager);
  }

  private static LoadManagers DEFAULTMANAGERS =
      new LoadManagers() {
        @Override
        public BitvectorFormulaManagerView wrapManager(BitvectorFormulaManager pManager) {
          return new BitvectorFormulaManagerView(pManager);
        }

        @Override
        public NumeralFormulaManagerView<IntegerFormula, IntegerFormula> wrapIntegerManager(NumeralFormulaManager<IntegerFormula, IntegerFormula> pManager) {
          return new NumeralFormulaManagerView<>(pManager);
        }

        @Override
        public NumeralFormulaManagerView<NumeralFormula, RationalFormula> wrapRationalManager(NumeralFormulaManager<NumeralFormula, RationalFormula> pManager) {
          return new NumeralFormulaManagerView<>(pManager);
        }

        @Override
        public BooleanFormulaManagerView wrapManager(BooleanFormulaManager pManager) {
          return new BooleanFormulaManagerView(pManager);
        }

        @Override
        public FunctionFormulaManagerView wrapManager(FunctionFormulaManager pManager) {
          return new FunctionFormulaManagerView(pManager);
        }
      };

  public static enum Theory {
    INTEGER,
    RATIONAL,
    BITVECTOR,
    ;
  }

  private BitvectorFormulaManagerView bitvectorFormulaManager;
  private NumeralFormulaManagerView<IntegerFormula, IntegerFormula> integerFormulaManager;
  private NumeralFormulaManagerView<NumeralFormula, RationalFormula> rationalFormulaManager;
  private BooleanFormulaManagerView booleanFormulaManager;

  private FormulaManager manager;

  private FunctionFormulaManagerView functionFormulaManager;

  @Option(name = "formulaDumpFilePattern", description = "where to dump interpolation and abstraction problems (format string)")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path formulaDumpFile = Paths.get("%s%04d-%s%03d.smt2");
  private String formulaDumpFilePattern;

  @Option(description="try to add some useful static-learning-like axioms for "
    + "bitwise operations (which are encoded as UFs): essentially, "
    + "we simply collect all the numbers used in bitwise operations, "
    + "and add axioms like (0 & n = 0)")
  private boolean useBitwiseAxioms = false;

  @Option(description="Theory to use as backend for bitvectors."
      + " If different from BITVECTOR, the specified theory is used to approximate bitvectors."
      + " This can be used for solvers that do not support bitvectors, or for increased performance.")
  private Theory encodeBitvectorAs = Theory.RATIONAL;

  @Option(description="Allows to ignore Concat and Extract Calls when Bitvector theory was replaced with Integer or Rational.")
  private boolean ignoreExtractConcat = true;

  private LogManager logger;

  public FormulaManagerView(LoadManagers loadManagers, FormulaManager baseManager) {
    init(loadManagers, baseManager);
  }

  private void init(LoadManagers loadManagers, FormulaManager baseManager) {
    manager = baseManager;
    bitvectorFormulaManager = loadManagers.wrapManager(baseManager.getBitvectorFormulaManager());
    bitvectorFormulaManager.couple(this);
    integerFormulaManager = loadManagers.wrapIntegerManager(baseManager.getIntegerFormulaManager());
    integerFormulaManager.couple(this);
    rationalFormulaManager = loadManagers.wrapRationalManager(baseManager.getRationalFormulaManager());
    rationalFormulaManager.couple(this);
    booleanFormulaManager = loadManagers.wrapManager(baseManager.getBooleanFormulaManager());
    booleanFormulaManager.couple(this);
    functionFormulaManager = loadManagers.wrapManager(baseManager.getFunctionFormulaManager());
    functionFormulaManager.couple(this);
  }


  public FormulaManagerView(LoadManagers loadManagers, FormulaManager baseManager, Configuration config, LogManager pLogger) throws InvalidConfigurationException {
    config.inject(this);
    if (encodeBitvectorAs != Theory.BITVECTOR) {
      baseManager =
          new ReplacingFormulaManager(baseManager, encodeBitvectorAs, ignoreExtractConcat);
    }

    init(loadManagers, baseManager);
    logger = pLogger;

    if (formulaDumpFile != null) {
      formulaDumpFilePattern = formulaDumpFile.toAbsolutePath().getPath();
    } else {
      formulaDumpFilePattern = null;
    }
  }

  public FormulaManagerView(FormulaManager wrapped, Configuration config, LogManager pLogger) throws InvalidConfigurationException {
    this(DEFAULTMANAGERS, wrapped, config, pLogger);
  }

  public FormulaManagerView(FormulaManager wrapped) {
    this(DEFAULTMANAGERS, wrapped);
  }

  public Path formatFormulaOutputFile(String function, int call, String formula, int index) {
    if (formulaDumpFilePattern == null) {
      return null;
    }

    return Paths.get(String.format(formulaDumpFilePattern, function, call, formula, index));
  }

  public void dumpFormulaToFile(BooleanFormula f, Path outputFile) {
    if (outputFile != null) {
      try {
        Files.writeFile(outputFile, this.dumpFormula(f));
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Failed to save formula to file");
      }
    }
  }

  /**
   * Helper method for creating variables of the given type.
   * @param formulaType the type of the variable.
   * @param name the name of the variable.
   * @return the created variable.
   */
  @SuppressWarnings("unchecked")
  public <T extends Formula> T makeVariable(FormulaType<T> formulaType, String name) {
    Formula t;
    if (formulaType.isBooleanType()) {
      t = booleanFormulaManager.makeVariable(name);
    } else if (formulaType.isIntegerType()) {
      t = integerFormulaManager.makeVariable(name);
    } else if (formulaType.isRationalType()) {
      t = rationalFormulaManager.makeVariable(name);
    } else if (formulaType.isBitvectorType()) {
      FormulaType.BitvectorType impl = (FormulaType.BitvectorType) formulaType;
      t = bitvectorFormulaManager.makeVariable(impl.getSize(), name);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return (T) t;
  }

  /**
   * Make a variable of the given type.
   * @param formulaType
   * @param value
   * @return
   */
  @SuppressWarnings("unchecked")
  public <T extends Formula> T makeNumber(FormulaType<T> formulaType, long value) {
    Formula t;
    if (formulaType.isIntegerType()) {
      t = integerFormulaManager.makeNumber(value);
    } else if (formulaType.isRationalType()) {
      t = rationalFormulaManager.makeNumber(value);
    } else if (formulaType.isBitvectorType()) {
      t = bitvectorFormulaManager.makeBitvector((FormulaType<BitvectorFormula>)formulaType, value);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return (T) t;
  }

  /**
   * Make a variable of the given type.
   * @param formulaType
   * @param value
   * @return
   */
  @SuppressWarnings("unchecked")
  public <T extends Formula> T makeNumber(FormulaType<T> formulaType, BigInteger value) {
    Formula t;
    if (formulaType.isIntegerType()) {
      t = integerFormulaManager.makeNumber(value);
    } else if (formulaType.isRationalType()) {
      t = rationalFormulaManager.makeNumber(value);
    } else if (formulaType.isBitvectorType()) {
      t = bitvectorFormulaManager.makeBitvector((FormulaType<BitvectorFormula>)formulaType, value);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return (T) t;
  }

  /**
   * Make a variable of the given type.
   * @param formulaType
   * @param value
   * @return
   */
  @SuppressWarnings("unchecked")
  public <T extends Formula> T makeNumber(FormulaType<T> formulaType, String value) {
    Formula t;
    if (formulaType.isIntegerType()) {
      t = integerFormulaManager.makeNumber(value);
    } else if (formulaType.isRationalType()) {
      t = rationalFormulaManager.makeNumber(value);
    } else if (formulaType.isBitvectorType()) {
      t = bitvectorFormulaManager.makeBitvector((FormulaType<BitvectorFormula>)formulaType, value);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return (T) t;
  }

  @SuppressWarnings("unchecked")
  public  <T extends Formula> T makeNegate(T pNum) {
    Class<T> clazz = getInterface(pNum);
    Formula t;
    if (clazz==BooleanFormula.class) {
      throw new IllegalArgumentException();
    } else if (clazz == IntegerFormula.class) {
      t = integerFormulaManager.negate((IntegerFormula)pNum);
    } else if (clazz == RationalFormula.class) {
      t = rationalFormulaManager.negate((RationalFormula)pNum);
    } else if (clazz == BitvectorFormula.class) {
      t = bitvectorFormulaManager.negate((BitvectorFormula)pNum);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return (T) t;
  }

  @SuppressWarnings("unchecked")
  public  <T extends Formula> T makePlus(T pForm, T pAugend) {
    Class<T> clazz1 = getInterface(pForm);
    Class<T> clazz2 = getInterface(pAugend);
    Formula t;
    if (clazz1 == IntegerFormula.class && clazz2 == IntegerFormula.class) {
      t = integerFormulaManager.add((IntegerFormula)pForm, (IntegerFormula)pAugend);
    } else if (clazz1 == NumeralFormula.class && clazz2 == NumeralFormula.class) {
      t = rationalFormulaManager.add((NumeralFormula)pForm, (NumeralFormula)pAugend);
    } else if (clazz1 == BitvectorFormula.class && clazz2 == BitvectorFormula.class) {
      t = bitvectorFormulaManager.add((BitvectorFormula)pForm, (BitvectorFormula)pAugend);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return (T) t;
  }

  @SuppressWarnings("unchecked")
  public <T extends Formula> T makeMinus(T pF1, T pF2) {
    Class<T> clazz1 = getInterface(pF1);
    Class<T> clazz2 = getInterface(pF2);
    Formula t;
    if (clazz1 == IntegerFormula.class && clazz2 == IntegerFormula.class) {
      t = integerFormulaManager.subtract((IntegerFormula) pF1, (IntegerFormula) pF2);
    } else if (clazz1 == NumeralFormula.class && clazz2 == NumeralFormula.class) {
      t = rationalFormulaManager.subtract((NumeralFormula) pF1, (NumeralFormula) pF2);
    } else if (clazz1 == BitvectorFormula.class && clazz2 == BitvectorFormula.class) {
      t = bitvectorFormulaManager.subtract((BitvectorFormula) pF1, (BitvectorFormula) pF2);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return (T) t;
  }
  @SuppressWarnings("unchecked")
  public  <T extends Formula> T makeMultiply(T pForm, T pAugend) {
    Class<T> clazz1 = getInterface(pForm);
    Class<T> clazz2 = getInterface(pAugend);
    Formula t;
    if (clazz1 == IntegerFormula.class && clazz2 == IntegerFormula.class) {
      t = integerFormulaManager.multiply((IntegerFormula) pForm, (IntegerFormula) pAugend);
    } else if (clazz1 == NumeralFormula.class && clazz2 == NumeralFormula.class) {
      t = rationalFormulaManager.multiply((NumeralFormula) pForm, (NumeralFormula) pAugend);
    } else if (clazz1 == BitvectorFormula.class && clazz2 == BitvectorFormula.class) {
      t = bitvectorFormulaManager.multiply((BitvectorFormula) pForm, (BitvectorFormula) pAugend);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return (T) t;
  }

  @SuppressWarnings("unchecked")
  public <T extends Formula> T  makeDivide(T pF1, T pF2, boolean pSigned) {
    Class<T> clazz1 = getInterface(pF1);
    Class<T> clazz2 = getInterface(pF2);
    Formula t;
    if (clazz1 == IntegerFormula.class && clazz2 == IntegerFormula.class) {
      t = integerFormulaManager.divide((IntegerFormula) pF1, (IntegerFormula) pF2);
    } else if (clazz1 == NumeralFormula.class && clazz2 == NumeralFormula.class) {
      t = rationalFormulaManager.divide((NumeralFormula) pF1, (NumeralFormula) pF2);
    } else if (clazz1 == BitvectorFormula.class && clazz2 == BitvectorFormula.class) {
      t = bitvectorFormulaManager.divide((BitvectorFormula) pF1, (BitvectorFormula) pF2, pSigned);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return (T) t;
  }

  @SuppressWarnings("unchecked")
  public <T extends Formula> T  makeModulo(T pF1, T pF2, boolean pSigned) {
    Class<T> clazz1 = getInterface(pF1);
    Class<T> clazz2 = getInterface(pF2);
    Formula t;
    if (clazz1 == IntegerFormula.class && clazz2 == IntegerFormula.class) {
      t = integerFormulaManager.modulo((IntegerFormula) pF1, (IntegerFormula) pF2);
    } else if (clazz1 == NumeralFormula.class && clazz2 == NumeralFormula.class) {
      t = rationalFormulaManager.modulo((NumeralFormula) pF1, (NumeralFormula) pF2);
    } else if (clazz1 == BitvectorFormula.class && clazz2 == BitvectorFormula.class) {
      t = bitvectorFormulaManager.modulo((BitvectorFormula) pF1, (BitvectorFormula) pF2, pSigned);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return (T) t;
  }

  @SuppressWarnings("unchecked")
  public <T extends Formula> T makeNot(T pF1) {
    Class<T> clazz = getInterface(pF1);
    Formula t;
    if (clazz==BooleanFormula.class) {
      t = booleanFormulaManager.not((BooleanFormula)pF1);
    } else if (clazz == NumeralFormula.class) {
      throw new IllegalArgumentException();
    } else if (clazz == BitvectorFormula.class) {
      t = bitvectorFormulaManager.not((BitvectorFormula)pF1);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return (T) t;
  }

  @SuppressWarnings("unchecked")
  public <T extends Formula> T makeAnd(T pF1, T pF2) {
    Class<T> clazz = getInterface(pF1);
    Formula t;
    if (clazz==BooleanFormula.class) {
      t = booleanFormulaManager.and((BooleanFormula)pF1, (BooleanFormula)pF2);
    } else if (clazz == IntegerFormula.class) {
      throw new IllegalArgumentException();
    } else if (clazz == RationalFormula.class) {
      throw new IllegalArgumentException();
    } else if (clazz == BitvectorFormula.class) {
      t = bitvectorFormulaManager.and((BitvectorFormula)pF1, (BitvectorFormula)pF2);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return (T) t;
  }

  @SuppressWarnings("unchecked")
  public <T extends Formula> T makeOr(T pF1, T pF2) {
    Class<T> clazz = getInterface(pF1);
    Formula t;
    if (clazz==BooleanFormula.class) {
      t = booleanFormulaManager.or((BooleanFormula)pF1, (BooleanFormula)pF2);
    } else if (clazz == IntegerFormula.class) {
      throw new IllegalArgumentException();
    } else if (clazz == RationalFormula.class) {
      throw new IllegalArgumentException();
    } else if (clazz == BitvectorFormula.class) {
      t = bitvectorFormulaManager.or((BitvectorFormula)pF1, (BitvectorFormula)pF2);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return (T) t;
  }


  @SuppressWarnings("unchecked")
  public <T extends Formula> T makeXor(T pF1, T pF2) {
    Class<T> clazz = getInterface(pF1);
    Formula t;
    if (clazz==BooleanFormula.class) {
      t = booleanFormulaManager.xor((BooleanFormula)pF1, (BooleanFormula)pF2);
    } else if (clazz == IntegerFormula.class) {
      throw new IllegalArgumentException();
    } else if (clazz == RationalFormula.class) {
      throw new IllegalArgumentException();
    } else if (clazz == BitvectorFormula.class) {
      t = bitvectorFormulaManager.xor((BitvectorFormula)pF1, (BitvectorFormula)pF2);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return (T) t;
  }

  @SuppressWarnings("unchecked")
  public <T extends Formula> T makeShiftLeft(T pF1, T pF2) {
    Class<T> clazz = getInterface(pF1);
    Formula t;
    if (clazz==BooleanFormula.class) {
      throw new IllegalArgumentException();
    } else if (clazz == IntegerFormula.class) {
      throw new IllegalArgumentException();
    } else if (clazz == RationalFormula.class) {
      throw new IllegalArgumentException();
    } else if (clazz == BitvectorFormula.class) {
      t = bitvectorFormulaManager.shiftLeft((BitvectorFormula)pF1, (BitvectorFormula)pF2);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return (T) t;
  }

  @SuppressWarnings("unchecked")
  public <T extends Formula> T makeShiftRight(T pF1, T pF2, boolean signed) {
    Class<T> clazz = getInterface(pF1);
    Formula t;
    if (clazz==BooleanFormula.class) {
      throw new IllegalArgumentException();
    } else if (clazz == IntegerFormula.class) {
      throw new IllegalArgumentException();
    } else if (clazz == RationalFormula.class) {
      throw new IllegalArgumentException();
    } else if (clazz == BitvectorFormula.class) {
      t = bitvectorFormulaManager.shiftRight((BitvectorFormula)pF1, (BitvectorFormula)pF2, signed);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return (T) t;
  }

  /**
   * Returns a term representing the selection of pFormula[pMsb:pLsb].
   * @param pFormula
   * @param pMsb
   * @param pLsb
   * @return
   */
  @SuppressWarnings("unchecked")
  public <T extends Formula> T makeExtract(T pFormula, int pMsb, int pLsb) {
    checkArgument(pLsb >= 0);
    checkArgument(pMsb >= pLsb);
    Class<T> clazz = getInterface(pFormula);
    Formula t;
    if (clazz==BooleanFormula.class) {
      throw new IllegalArgumentException();
    } else if (clazz == IntegerFormula.class) {
      throw new IllegalArgumentException();
    } else if (clazz == RationalFormula.class) {
      throw new IllegalArgumentException();
    } else if (clazz == BitvectorFormula.class) {
      t = bitvectorFormulaManager.extract((BitvectorFormula)pFormula, pMsb, pLsb);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return (T) t;
  }

  @SuppressWarnings("unchecked")
  public <T extends Formula> T makeConcat(T pFormula, T pAppendFormula) {
    Class<T> clazz = getInterface(pFormula);
    Formula t;
    if (clazz==BooleanFormula.class) {
      throw new IllegalArgumentException();
    } else if (clazz == IntegerFormula.class) {
      throw new IllegalArgumentException();
    } else if (clazz == RationalFormula.class) {
      throw new IllegalArgumentException();
    } else if (clazz == BitvectorFormula.class) {
      t = bitvectorFormulaManager.concat((BitvectorFormula)pFormula, (BitvectorFormula)pAppendFormula);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return (T) t;
  }

  public <T extends Formula> T makeConcat(List<T> formulas) {
    checkArgument(!formulas.isEmpty());
    T conc = null;
    for (T t : formulas) {
      if (conc == null) {
        conc = t;
      } else {
        conc = makeConcat(conc, t);
      }
    }
    return conc;
  }

  @SuppressWarnings("unchecked")
  public <T extends Formula> T makeExtend(T pFormula, int pExtensionBits, boolean pSigned) {
    checkArgument(pExtensionBits >= 0);
    Class<T> clazz = getInterface(pFormula);
    Formula t;
    if (clazz==BooleanFormula.class) {
      throw new IllegalArgumentException();
    } else if (clazz == IntegerFormula.class) {
      throw new IllegalArgumentException();
    } else if (clazz == RationalFormula.class) {
      throw new IllegalArgumentException();
    } else if (clazz == BitvectorFormula.class) {
      t = bitvectorFormulaManager.extend((BitvectorFormula)pFormula, pExtensionBits, pSigned);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return (T) t;
  }

  public  <T extends Formula> BooleanFormula makeEqual(T pLhs, T pRhs) {
    Class<T> clazz1 = getInterface(pLhs);
    Class<T> clazz2 = getInterface(pRhs);
    BooleanFormula t;
    if (clazz1==BooleanFormula.class) {
      t = booleanFormulaManager.equivalence((BooleanFormula)pLhs, (BooleanFormula)pRhs);
    } else if (clazz1 == IntegerFormula.class && clazz2 == IntegerFormula.class) {
      t = integerFormulaManager.equal((IntegerFormula)pLhs, (IntegerFormula)pRhs);
    } else if (clazz1 == NumeralFormula.class && clazz2 == NumeralFormula.class) {
      t = rationalFormulaManager.equal((NumeralFormula)pLhs, (NumeralFormula)pRhs);
    } else if (clazz1 == BitvectorFormula.class) {
      t = bitvectorFormulaManager.equal((BitvectorFormula)pLhs, (BitvectorFormula)pRhs);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return t;
  }

  public  <T extends Formula> BooleanFormula makeLessOrEqual(T pLhs, T pRhs, boolean signed) {
    Class<T> clazz1 = getInterface(pLhs);
    Class<T> clazz2 = getInterface(pRhs);
    BooleanFormula t;
    if (clazz1 == IntegerFormula.class && clazz2 == IntegerFormula.class) {
      t = integerFormulaManager.lessOrEquals((IntegerFormula)pLhs, (IntegerFormula)pRhs);
    } else if (clazz1 == NumeralFormula.class && clazz2 == NumeralFormula.class) {
      t = rationalFormulaManager.lessOrEquals((NumeralFormula)pLhs, (NumeralFormula)pRhs);
    } else if (clazz1 == BitvectorFormula.class && clazz2 == BitvectorFormula.class) {
      t = bitvectorFormulaManager.lessOrEquals((BitvectorFormula)pLhs, (BitvectorFormula)pRhs, signed);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return t;
  }
  public  <T extends Formula> BooleanFormula makeLessThan(T pLhs, T pRhs, boolean signed) {
    Class<T> clazz1 = getInterface(pLhs);
    Class<T> clazz2 = getInterface(pRhs);
    BooleanFormula t;
    if (clazz1 == IntegerFormula.class && clazz2 == IntegerFormula.class) {
      t = integerFormulaManager.lessThan((IntegerFormula) pLhs, (IntegerFormula) pRhs);
    } else if (clazz1 == NumeralFormula.class && clazz2 == NumeralFormula.class) {
      t = rationalFormulaManager.lessThan((NumeralFormula) pLhs, (NumeralFormula) pRhs);
    } else if (clazz1 == BitvectorFormula.class && clazz2 == BitvectorFormula.class) {
      t = bitvectorFormulaManager.lessThan((BitvectorFormula) pLhs, (BitvectorFormula) pRhs, signed);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return t;
  }

  public  <T extends Formula> BooleanFormula makeGreaterThan(T pLhs, T pRhs, boolean signed) {
    Class<T> clazz1 = getInterface(pLhs);
    Class<T> clazz2 = getInterface(pRhs);
    BooleanFormula t;
    if (clazz1 == IntegerFormula.class && clazz2 == IntegerFormula.class) {
      t = integerFormulaManager.greaterThan((IntegerFormula) pLhs, (IntegerFormula) pRhs);
    } else if (clazz1 == NumeralFormula.class && clazz2 == NumeralFormula.class) {
      t = rationalFormulaManager.greaterThan((NumeralFormula) pLhs, (NumeralFormula) pRhs);
    } else if (clazz1 == BitvectorFormula.class && clazz2 == BitvectorFormula.class) {
      t = bitvectorFormulaManager.greaterThan((BitvectorFormula) pLhs, (BitvectorFormula) pRhs, signed);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return t;
  }

  public <T extends Formula> BooleanFormula makeGreaterOrEqual(T pLhs, T pRhs, boolean signed) {
    Class<T> clazz1 = getInterface(pLhs);
    Class<T> clazz2 = getInterface(pRhs);
    BooleanFormula t;
    if (clazz1 == IntegerFormula.class && clazz2 == IntegerFormula.class) {
      t = integerFormulaManager.greaterOrEquals((IntegerFormula) pLhs, (IntegerFormula) pRhs);
    } else if (clazz1 == NumeralFormula.class && clazz2 == NumeralFormula.class) {
      t = rationalFormulaManager.greaterOrEquals((NumeralFormula) pLhs, (NumeralFormula) pRhs);
    } else if (clazz1 == BitvectorFormula.class && clazz2 == BitvectorFormula.class) {
      t = bitvectorFormulaManager.greaterOrEquals((BitvectorFormula) pLhs, (BitvectorFormula) pRhs, signed);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return t;
  }

  public <T extends Formula> T makeVariable(FormulaType<T> formulaType, String name, int idx) {
    return makeVariable(formulaType, makeName(name, idx));
  }

  public NumeralFormulaManagerView<IntegerFormula, IntegerFormula> getIntegerFormulaManager() {
    return integerFormulaManager;
  }

  public NumeralFormulaManagerView<NumeralFormula, RationalFormula> getRationalFormulaManager() {
    return rationalFormulaManager;
  }

  public BooleanFormulaManagerView getBooleanFormulaManager() {
    return booleanFormulaManager;
  }

  public BitvectorFormulaManagerView getBitvectorFormulaManager() {
    return bitvectorFormulaManager;
  }

  public FunctionFormulaManagerView getFunctionFormulaManager() {
    return functionFormulaManager;
  }

  public UnsafeFormulaManager getUnsafeFormulaManager() {
    return manager.getUnsafeFormulaManager(); // Unsafe
  }

  public <T extends Formula> FormulaType<T> getFormulaType(T pFormula) {
    checkNotNull(pFormula);
    return manager.getFormulaType(pFormula);
  }

  @SuppressWarnings("unchecked")
  <T extends Formula> T wrapInView(T formula) {
    Class<T> formulaType = AbstractFormulaManager.getInterfaceHelper(formula);
    if (BooleanFormula.class == formulaType) {
      return (T) booleanFormulaManager.wrapInView((BooleanFormula) formula);
    }
    if (IntegerFormula.class == (formulaType)) {
      return (T) integerFormulaManager.wrapInView((IntegerFormula) formula);
    }
    if (RationalFormula.class == (formulaType)) {
      return (T) rationalFormulaManager.wrapInView((RationalFormula) formula);
    }
    if (BitvectorFormula.class == (formulaType)) {
      return (T) bitvectorFormulaManager.wrapInView((BitvectorFormula) formula);
    }
    throw new IllegalArgumentException("Invalid class");
  }

  public <T extends Formula> BooleanFormula assignment(T left, T right) {
    left = extractFromView(left);
    right = extractFromView(right);
    FormulaType<T> lformulaType = this.getFormulaType(left);
    FormulaType<T> rformulaType = this.getFormulaType(right);
    if (lformulaType != rformulaType) {
      throw new IllegalArgumentException("Can't assign different types! (" + lformulaType + " and " + rformulaType + ")");
    }

    return makeEqual(left, right);
  }

  @SuppressWarnings("unchecked")
  <T extends Formula> T extractFromView(T formula) {
    Class<T> formulaType = AbstractFormulaManager.getInterfaceHelper(formula);
    if (BooleanFormula.class == formulaType) {
      return (T) booleanFormulaManager.extractFromView((BooleanFormula) formula);
    }
    if (IntegerFormula.class == (formulaType)) {
      return (T) integerFormulaManager.extractFromView((IntegerFormula) formula);
    }
    if (RationalFormula.class == (formulaType)) {
      return (T) rationalFormulaManager.extractFromView((RationalFormula) formula);
    }
    if (BitvectorFormula.class == (formulaType)) {
      return (T) bitvectorFormulaManager.extractFromView((BitvectorFormula) formula);
    }

    throw new IllegalArgumentException("Invalid class");
  }

  <T extends Formula> Class<T> getInterface(T pInstance) {
    return manager.getInterface(extractFromView(pInstance));
  }

  public BooleanFormula parse(String pS) throws IllegalArgumentException {
    return wrapInView(manager.parse(pS));
  }

  public <T extends Formula> T  instantiate(T fView, SSAMap ssa) {
    T f = extractFromView(fView);
    T endResult = myInstanciate(ssa, f);
    return wrapInView(endResult);
  }

  // the character for separating name and index of a value
  private static final String INDEX_SEPARATOR = "@";

  public static String makeName(String name, int idx) {
    return name + INDEX_SEPARATOR + idx;
  }

  private <T extends Formula> T myInstanciate(SSAMap ssa, T f) {
    UnsafeFormulaManager unsafeManager = manager.getUnsafeFormulaManager();
    Deque<Formula> toProcess = new ArrayDeque<>();
    Map<Formula, Formula> cache = new HashMap<>();

    toProcess.push(f);
    while (!toProcess.isEmpty()) {
      final Formula tt = toProcess.peek();
      if (cache.containsKey(tt)) {
        toProcess.pop();
        continue;
      }

      if (unsafeManager.isVariable(tt)) {
        toProcess.pop();
        String name = unsafeManager.getName(tt);
        int idx = ssa.getIndex(name);
        if (idx > 0) {
          // ok, the variable has an instance in the SSA, replace it
          Formula newt = unsafeManager.replaceName(tt, makeName(name, idx));
          cache.put(tt, newt);
        } else {
          // the variable is not used in the SSA, keep it as is
          cache.put(tt, tt);
        }

      } else {
        boolean childrenDone = true;
        int arity = unsafeManager.getArity(tt);
        Formula[] newargs = new Formula[arity];
        for (int i = 0; i < newargs.length; ++i) {
          Formula c = unsafeManager.getArg(tt, i);
          Formula newC = cache.get(c);
          if (newC != null) {
            newargs[i] = newC;
          } else {
            toProcess.push(c);
            childrenDone = false;
          }
        }

        if (childrenDone) {
          toProcess.pop();
          Formula newt;

          if (unsafeManager.isUF(tt)) {
            String name = unsafeManager.getName(tt);
            assert name != null;

            if (ufCanBeLvalue(name)) {
              final int idx = ssa.getIndex(name);
              if (idx > 0) {
                // ok, the variable has an instance in the SSA, replace it
                newt = unsafeManager.replaceArgsAndName(tt, makeName(name, idx), newargs);
              } else {
                newt = unsafeManager.replaceArgs(tt, newargs);
              }
            } else {
              newt = unsafeManager.replaceArgs(tt, newargs);
            }
          } else {
            newt = unsafeManager.replaceArgs(tt, newargs);
          }

          cache.put(tt, newt);
        }
      }
    }

    Formula result = cache.get(f);
    assert result != null;
    return unsafeManager.typeFormula(manager.getFormulaType(f), result);
  }

  private boolean ufCanBeLvalue(String name) {
    return name.startsWith("*");
  }

  public <T extends Formula> T uninstantiate(T pF) {
    return wrapInView(myUninstantiate(extractFromView(pF)));
  }

  // various caches for speeding up expensive tasks
  //
  // cache for splitting arithmetic equalities in extractAtoms
  private final Map<Formula, Boolean> arithCache = new HashMap<>();

  // cache for uninstantiating terms (see uninstantiate() below)
  private final Map<Formula, Formula> uninstantiateCache = new HashMap<>();

  public static Pair<String, Integer> parseName(final String name) {
    String[] s = name.split(INDEX_SEPARATOR);
    if (s.length == 2) {
      return Pair.of(s[0], Integer.parseInt(s[1]));
    } else if (s.length == 1) {
      return Pair.of(s[0], null);
    } else {
      throw new IllegalArgumentException("Not an instantiated variable nor constant: " + name);
    }
  }

  private <T extends Formula> T myUninstantiate(T f) {

    UnsafeFormulaManager unsafeManager = manager.getUnsafeFormulaManager();
    Map<Formula, Formula> cache = uninstantiateCache;
    Deque<Formula> toProcess = new ArrayDeque<>();

    toProcess.push(f);
    while (!toProcess.isEmpty()) {
      final Formula tt = toProcess.peek();
      if (cache.containsKey(tt)) {
        toProcess.pop();
        continue;
      }

      if (unsafeManager.isVariable(tt)) {
        String name = parseName(unsafeManager.getName(tt)).getFirst();

        Formula newt = unsafeManager.replaceName(tt, name);
        cache.put(tt, newt);

      } else {
        boolean childrenDone = true;
        int arity = unsafeManager.getArity(tt);
        Formula[] newargs = new Formula[arity];
        for (int i = 0; i < newargs.length; ++i) {
          Formula c = unsafeManager.getArg(tt, i);
          Formula newC = cache.get(c);
          if (newC != null) {
            newargs[i] = newC;
          } else {
            toProcess.push(c);
            childrenDone = false;
          }
        }

        if (childrenDone) {
          toProcess.pop();
          Formula newt;
          if (unsafeManager.isUF(tt)) {
            String name = unsafeManager.getName(tt);
            assert name != null;

            if (ufCanBeLvalue(name)) {
              name = parseName(name).getFirst();

              newt = unsafeManager.replaceArgsAndName(tt, name, newargs);
            } else {
              newt = unsafeManager.replaceArgs(tt, newargs);
            }
          } else {
            newt = unsafeManager.replaceArgs(tt, newargs);
          }

          cache.put(tt, newt);
        }
      }
    }

    Formula result = cache.get(f);
    assert result != null;
    return unsafeManager.typeFormula(manager.getFormulaType(f), result);
  }

  public Collection<BooleanFormula> extractAtoms(BooleanFormula f, boolean splitArithEqualities, boolean conjunctionsOnly) {
    Collection<BooleanFormula> unwrapped = myExtractAtoms(extractFromView(f), splitArithEqualities, conjunctionsOnly);

    List<BooleanFormula> atoms = new ArrayList<>(unwrapped.size());
    for (Iterator<BooleanFormula> iterator = unwrapped.iterator(); iterator.hasNext();) {
      BooleanFormula booleanFormula = iterator.next();
      atoms.add(wrapInView(booleanFormula));
    }

    return atoms;
  }

  private Collection<BooleanFormula> myExtractAtoms(BooleanFormula f, boolean splitArithEqualities,
      boolean conjunctionsOnly) {
    BooleanFormulaManager rawBooleanManager = manager.getBooleanFormulaManager();
    BitvectorFormulaManager rawBitpreciseManager = manager.getBitvectorFormulaManager();
    NumeralFormulaManager<NumeralFormula, RationalFormula> rawNumericManager = manager.getRationalFormulaManager();
    UnsafeFormulaManager unsafeManager = manager.getUnsafeFormulaManager();

    Set<BooleanFormula> handled = new HashSet<>();
    List<BooleanFormula> atoms = new ArrayList<>();

    Deque<BooleanFormula> toProcess = new ArrayDeque<>();
    toProcess.push(f);
    handled.add(f);

    while (!toProcess.isEmpty()) {
      BooleanFormula tt = toProcess.pop();
      assert handled.contains(tt);

      if (rawBooleanManager.isTrue(tt) || rawBooleanManager.isFalse(tt)) {
        continue;
      }

      if (unsafeManager.isAtom(tt)) {
        tt = myUninstantiate(tt);

        if (splitArithEqualities
            && myIsPurelyArithmetic(tt)) {
          if (rawNumericManager.isEqual(tt)) {
            RationalFormula a0 = unsafeManager.typeFormula(FormulaType.RationalType, unsafeManager.getArg(tt, 0));
            RationalFormula a1 = unsafeManager.typeFormula(FormulaType.RationalType, unsafeManager.getArg(tt, 1));

            BooleanFormula tt1 = rawNumericManager.lessOrEquals(a0, a1);
            //SymbolicFormula tt2 = encapsulate(t2);
            handled.add(tt1);
            //cache.add(tt2);
            atoms.add(tt1);
            //atoms.add(tt2);
            atoms.add(tt);
          } else if (rawBitpreciseManager.isEqual(tt)) {
            // NOTE: the type doesn't matter in the current implementations under this situation,
            // however if it does in the future we will have to add an (unsafe) api to read the bitlength (at least)
            FormulaType<BitvectorFormula> type = FormulaType.BitvectorType.getBitvectorType(32);
            BitvectorFormula a0 = unsafeManager.typeFormula(type, unsafeManager.getArg(tt, 0));
            BitvectorFormula a1 = unsafeManager.typeFormula(type, unsafeManager.getArg(tt, 1));

            BooleanFormula tt1 = rawBitpreciseManager.lessOrEquals(a0, a1, true);
            //SymbolicFormula tt2 = encapsulate(t2);
            handled.add(tt1);
            //cache.add(tt2);
            atoms.add(tt1);
            //atoms.add(tt2);
            atoms.add(tt);
            }
        } else {
          atoms.add(tt);
        }

      } else if (conjunctionsOnly
          && !(rawBooleanManager.isNot(tt) || rawBooleanManager.isAnd(tt))) {
        // conjunctions only, but formula is neither "not" nor "and"
        // treat this as atomic
        atoms.add(myUninstantiate(tt));

      } else {
        // ok, go into this formula
        for (int i = 0; i < unsafeManager.getArity(tt); ++i) {
          BooleanFormula c = unsafeManager.typeFormula(FormulaType.BooleanType, unsafeManager.getArg(tt, i));
          if (handled.add(c)) {
            toProcess.push(c);
          }
        }
      }
    }

    return atoms;
  }

  public boolean isPurelyArithmetic(Formula f) {
    return myIsPurelyArithmetic(extractFromView(f));
  }

  // returns true if the given term is a pure arithmetic term
  private boolean myIsPurelyArithmetic(Formula f) {
    Boolean result = arithCache.get(f);
    if (result != null) { return result; }

    UnsafeFormulaManager unsafeManager = manager.getUnsafeFormulaManager();

    boolean res = true;
    if (unsafeManager.isUF(f)) {
      res = false;

    } else {
      int arity = unsafeManager.getArity(f);
      for (int i = 0; i < arity; ++i) {
        res |= myIsPurelyArithmetic(unsafeManager.getArg(f, i));
        if (!res) {
          break;
        }
      }
    }
    arithCache.put(f, res);
    return res;
  }

  public Set<String> extractVariables(Formula f) {
    return myExtractVariables(extractFromView(f));
  }

  private Set<String> myExtractVariables(Formula f) {
    UnsafeFormulaManager unsafeManager = manager.getUnsafeFormulaManager();
    Set<Formula> seen = new HashSet<>();
    Set<String> vars = new HashSet<>();

    Deque<Formula> toProcess = new ArrayDeque<>();
    toProcess.push(f);

    while (!toProcess.isEmpty()) {
      Formula t = toProcess.pop();

//      if ( msat_term_is_true(msatEnv, t) || msat_term_is_false(msatEnv, t)) {
//        continue;
//      }

      if (unsafeManager.isVariable(t)) {
        vars.add(unsafeManager.getName(t));
      } else {
        // ok, go into this formula
        for (int i = 0; i < unsafeManager.getArity(t); ++i) {
          Formula c = unsafeManager.getArg(t, i);

          if (seen.add(c)) {
            toProcess.push(c);
          }
        }
      }
    }

    return vars;
  }

  public Appender dumpFormula(Formula pT) {
    return manager.dumpFormula(extractFromView(pT));
  }

  public boolean checkSyntacticEntails(Formula leftFormula, Formula rightFormula) {
    return myCheckSyntacticEntails(extractFromView(leftFormula), extractFromView(rightFormula));
  }

  private boolean myCheckSyntacticEntails(Formula leftFormula, Formula rightFormula) {

    UnsafeFormulaManager unsafeManager = manager.getUnsafeFormulaManager();
    Deque<Formula> toProcess = new ArrayDeque<>();
    Set<Formula> seen = new HashSet<>();

    toProcess.push(rightFormula);
    while (!toProcess.isEmpty()) {
      final Formula rightSubFormula = toProcess.pop();

      if (rightSubFormula.equals(leftFormula)) { return true; }

      if (! unsafeManager.isVariable(rightSubFormula)) {
        int args = unsafeManager.getArity(rightSubFormula);
        for (int i = 0; i < args; ++i) {
          Formula arg = unsafeManager.getArg(rightSubFormula, i);
          if (!seen.contains(arg)) {
            toProcess.add(arg);
            seen.add(arg);
          }
        }
      }
    }

    return false;
  }

  public boolean isPurelyConjunctive(BooleanFormula t) {
    return myIsPurelyConjunctive(extractFromView(t));
  }

  private boolean myIsPurelyConjunctive(BooleanFormula t) {
    UnsafeFormulaManager unsafeManager = manager.getUnsafeFormulaManager();

    BooleanFormulaManager rawBooleanManager = manager.getBooleanFormulaManager();

    if (unsafeManager.isAtom(t) || unsafeManager.isUF(t)) {
      // term is atom
      return true;

    } else if (rawBooleanManager.isNot(t)) {
      t = unsafeManager.typeFormula(FormulaType.BooleanType, unsafeManager.getArg(t, 0));
      return (unsafeManager.isUF(t) || unsafeManager.isAtom(t));

    } else if (rawBooleanManager.isAnd(t)) {
      for (int i = 0; i < unsafeManager.getArity(t); ++i) {
        if (!myIsPurelyConjunctive(unsafeManager.typeFormula(FormulaType.BooleanType, unsafeManager.getArg(t, i)))) {
          return false;
        }
      }
      return true;

    } else {
      return false;
    }
  }

  public static final String BitwiseAndUfName = "_&_";
  public static final String BitwiseOrUfName ="_!!_"; // SMTInterpol does not allow "|" to be used
  public static final String BitwiseXorUfName ="_^_";
  public static final String BitwiseNotUfName ="_~_";
  public static final String MultUfName ="_*_";
  public static final String DivUfName ="_/_";
  public static final String ModUfName ="_%_";

  // returns a formula with some "static learning" about some bitwise
  // operations, so that they are (a bit) "less uninterpreted"
  // Currently it add's the following formulas for each number literal n that
  // appears in the formula: "(n & 0 == 0) and (0 & n == 0)"
  // But only if an bitwise "and" occurs in the formula.
  private BooleanFormula myGetBitwiseAxioms(Formula f) {
    UnsafeFormulaManager unsafeManager = manager.getUnsafeFormulaManager();
    BooleanFormulaManager rawBooleanManager = manager.getBooleanFormulaManager();

    Deque<Formula> toProcess = new ArrayDeque<>();
    Set<Formula> seen = new HashSet<>();
    Set<Formula> allLiterals = new HashSet<>();

    boolean andFound = false;

    toProcess.add(f);
    while (!toProcess.isEmpty()) {
      final Formula tt = toProcess.pollLast();

      if (unsafeManager.isNumber(tt)) {
        allLiterals.add(tt);
      }
      if (unsafeManager.isUF(tt)) {
        if (unsafeManager.getName(tt).equals(BitwiseAndUfName) && !andFound) {
          andFound = true;
        }
//        FunctionSymbol funcSym = ((ApplicationTerm) t).getFunction();
//        andFound = bitwiseAndUfDecl.equals(funcSym.getName());
      }
      int arity = unsafeManager.getArity(tt);
      for (int i = 0; i < arity; ++i) {
        Formula c = unsafeManager.getArg(tt, i);
        if (seen.add(c)) {
          // was not already contained in seen
          toProcess.add(c);
        }
      }
    }

    BooleanFormula result = rawBooleanManager.makeBoolean(true);
    BitvectorFormulaManager bitMgr = manager.getBitvectorFormulaManager();
    if (andFound) {
      // Note: We can assume that we have no real bitvectors here, so size should be not important
      // If it ever should be we can just add an method to the unsafe-manager to read the size.
      BitvectorFormula z = bitMgr.makeBitvector(1, 0);
      FormulaType<BitvectorFormula> type = FormulaType.BitvectorType.getBitvectorType(1);
      //Term z = env.numeral("0");
      for (Formula nn : allLiterals) {
        BitvectorFormula n = unsafeManager.typeFormula(type, nn);
        BitvectorFormula u1 = bitMgr.and(z, n);
        BitvectorFormula u2 = bitMgr.and(n, z);
        //Term u1 = env.term(bitwiseAndUfDecl, n, z);
        //Term u2 = env.term(bitwiseAndUfDecl, z, n);
        //Term e1;
        //e1 = env.term("=", u1, z);
        BooleanFormula e1 = bitMgr.equal(u1, z);
        //Term e2 = env.term("=", u2, z);
        BooleanFormula e2 = bitMgr.equal(u2, z);
        BooleanFormula a = booleanFormulaManager.and(e1, e2);
        //Term a = env.term("and", e1, e2);

        result = booleanFormulaManager.and(result, a); //env.term("and", result, a);
      }
    }
    return result;
  }

    // returns a formula with some "static learning" about some bitwise
    public BooleanFormula getBitwiseAxioms(Formula f) {
      return wrapInView(myGetBitwiseAxioms(extractFromView(f)));
    }


  public String getVersion() {
    return manager.getVersion();
  }


  public boolean useBitwiseAxioms() {
    return useBitwiseAxioms;
  }

  private BooleanFormula myCreatePredicateVariable(String pName) {
    return manager.getBooleanFormulaManager().makeVariable(pName);
//    UnsafeFormulaManager unsafeManager = manager.getUnsafeFormulaManager();
//    BooleanFormulaManager rawBooleanManager = manager.getBooleanFormulaManager();
//
//    //long t = getTerm(atom);
//
//    String repr = unsafeManager.isAtom(atom)
//        ? unsafeManager.getTermRepr(atom)  : ("#" + unsafeManager.getTermId( atom));
//    return rawBooleanManager.makeVariable("\"PRED" + repr + "\"");
  }

  public BooleanFormula createPredicateVariable(String pName) {
    return wrapInView(myCreatePredicateVariable(pName));
  }
}
