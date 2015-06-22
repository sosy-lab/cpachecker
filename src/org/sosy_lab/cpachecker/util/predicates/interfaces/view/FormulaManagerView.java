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
import static com.google.common.collect.FluentIterable.from;

import java.io.IOException;
import java.io.PrintStream;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
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
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BitvectorFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BitvectorFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FloatingPointFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FloatingPointFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.IntegerFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.RationalFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.UnsafeFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * This class is the central entry point for all formula creation
 * and manipulation operations for client code.
 * It delegates to the actual solver package
 * and provides additional utilities.
 * The preferred way of instantiating this class is via
 * {@link Solver#create(Configuration, LogManager, org.sosy_lab.cpachecker.core.ShutdownNotifier)}.
 *
 *
 * This class and some of its related classes have supporting operations
 * for creating and manipulation formulas with SSA indices:
 * - {@link #makeVariable(FormulaType, String, int)} creates a variable with an SSA index
 * - {@link #instantiate(Formula, SSAMap)} adds SSA indices to variables in a formula
 * - {@link #uninstantiate(Formula)} removes all SSA indices from a formula
 *
 * The method {@link #parseName(String)} is also related to this, but should not be used!
 */
@Options(prefix="cpa.predicate")
public class FormulaManagerView implements StatisticsProvider {

  public static enum Theory {
    INTEGER,
    RATIONAL,
    BITVECTOR,
    FLOAT,
    ;
  }

  private final LogManager logger;

  private final FormulaManager manager;
  private final UnsafeFormulaManager unsafeManager;

  private final FormulaWrappingHandler wrappingHandler;
  private final BooleanFormulaManagerView booleanFormulaManager;
  private final BitvectorFormulaManagerView bitvectorFormulaManager;
  private final FloatingPointFormulaManagerView floatingPointFormulaManager;
  private final NumeralFormulaManagerView<IntegerFormula, IntegerFormula> integerFormulaManager;
  private NumeralFormulaManagerView<NumeralFormula, RationalFormula> rationalFormulaManager;
  private final FunctionFormulaManagerView functionFormulaManager;
  private final QuantifiedFormulaManagerView quantifiedFormulaManager;
  private final ArrayFormulaManagerView arrayFormulaManager;

  @Option(secure=true, name = "formulaDumpFilePattern", description = "where to dump interpolation and abstraction problems (format string)")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate formulaDumpFile = PathTemplate.ofFormatString("%s%04d-%s%03d.smt2");

  @Option(secure=true, description = "where to dump variables and their possible encoding")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path symbolEncodingFile = Paths.get("symbolEncoding.txt");
  private final SymbolEncoding symbolEncoding = new SymbolEncoding();

  @Option(secure=true, description="try to add some useful static-learning-like axioms for "
    + "bitwise operations (which are encoded as UFs): essentially, "
    + "we simply collect all the numbers used in bitwise operations, "
    + "and add axioms like (0 & n = 0)")
  private boolean useBitwiseAxioms = false;

  @Option(secure=true, description="Theory to use as backend for bitvectors."
      + " If different from BITVECTOR, the specified theory is used to approximate bitvectors."
      + " This can be used for solvers that do not support bitvectors, or for increased performance.")
  private Theory encodeBitvectorAs = Theory.INTEGER;

  @Option(secure=true, description="Theory to use as backend for floats."
      + " If different from FLOAT, the specified theory is used to approximate floats."
      + " This can be used for solvers that do not support floating-point arithmetic, or for increased performance.")
  private Theory encodeFloatAs = Theory.RATIONAL;

  @Option(secure=true, description="Allows to ignore Concat and Extract Calls when Bitvector theory was replaced with Integer or Rational.")
  private boolean ignoreExtractConcat = true;

  public FormulaManagerView(FormulaManagerFactory solverFactory, Configuration config, LogManager pLogger) throws InvalidConfigurationException {
    config.inject(this, FormulaManagerView.class);
    logger = pLogger;
    manager = checkNotNull(solverFactory.getFormulaManager());
    unsafeManager = manager.getUnsafeFormulaManager();
    wrappingHandler = new FormulaWrappingHandler(manager, encodeBitvectorAs, encodeFloatAs);

    BitvectorFormulaManager rawBitvectorFormulaManager;
    switch (encodeBitvectorAs) {
      case BITVECTOR:
        try {
          rawBitvectorFormulaManager = manager.getBitvectorFormulaManager();
        } catch (UnsupportedOperationException e) {
          throw new InvalidConfigurationException("The chosen SMT solver does not support the theory of bitvectors, "
              + "please choose another SMT solver "
              + "or use the option cpa.predicate.encodeBitvectorAs "
              + "to approximate bitvectors with another theory.",
              e);
        }
        break;
      case INTEGER:
        rawBitvectorFormulaManager = new ReplaceBitvectorWithNumeralAndFunctionTheory<>(wrappingHandler,
            manager.getIntegerFormulaManager(), manager.getFunctionFormulaManager(),
            ignoreExtractConcat);
        break;
      case RATIONAL:
        NumeralFormulaManager<NumeralFormula, RationalFormula> rmgr;
        try {
          rmgr = manager.getRationalFormulaManager();
        } catch (UnsupportedOperationException e) {
          throw new InvalidConfigurationException("The chosen SMT solver does not support the theory of rationals, "
              + "please choose another SMT solver "
              + "or use the option cpa.predicate.encodeBitvectorAs "
              + "to approximate bitvectors with another theory.",
              e);
        }
        rawBitvectorFormulaManager = new ReplaceBitvectorWithNumeralAndFunctionTheory<>(wrappingHandler,
            rmgr, manager.getFunctionFormulaManager(),
            ignoreExtractConcat);
      break;
      case FLOAT:
        throw new InvalidConfigurationException("Value FLOAT is not valid for option cpa.predicate.encodeBitvectorAs");
      default:
        throw new AssertionError();
    }
    bitvectorFormulaManager = new BitvectorFormulaManagerView(wrappingHandler, rawBitvectorFormulaManager, manager.getBooleanFormulaManager(), symbolEncoding);

    integerFormulaManager = new NumeralFormulaManagerView<>(wrappingHandler, manager.getIntegerFormulaManager());
    booleanFormulaManager = new BooleanFormulaManagerView(wrappingHandler, manager.getBooleanFormulaManager(), manager.getUnsafeFormulaManager());
    functionFormulaManager = new FunctionFormulaManagerView(wrappingHandler, manager.getFunctionFormulaManager(), symbolEncoding);
    quantifiedFormulaManager = new QuantifiedFormulaManagerView(wrappingHandler, manager.getQuantifiedFormulaManager(), booleanFormulaManager, integerFormulaManager);
    arrayFormulaManager = new ArrayFormulaManagerView(wrappingHandler, manager.getArrayFormulaManager());

    FloatingPointFormulaManager rawFloatingPointFormulaManager;
    switch (encodeFloatAs) {
    case FLOAT:
      try {
        rawFloatingPointFormulaManager = manager.getFloatingPointFormulaManager();
      } catch (UnsupportedOperationException e) {
        throw new InvalidConfigurationException(
            "The chosen SMT solver does not support the theory of floats, "
            + "please choose another SMT solver "
            + "or use the option cpa.predicate.encodeFloatAs "
            + "to approximate floats with another theory.",
            e);
      }
      break;
    case INTEGER:
      rawFloatingPointFormulaManager = new ReplaceFloatingPointWithNumeralAndFunctionTheory<>(
          wrappingHandler, manager.getIntegerFormulaManager(), manager.getFunctionFormulaManager(),
          manager.getBooleanFormulaManager());
      break;
    case RATIONAL:
      NumeralFormulaManager<NumeralFormula, RationalFormula> rmgr;
      try {
        rmgr = manager.getRationalFormulaManager();
      } catch (UnsupportedOperationException e) {
        throw new InvalidConfigurationException("The chosen SMT solver does not support the theory of rationals, "
            + "please choose another SMT solver "
            + "or use the option cpa.predicate.encodeFloatAs "
            + "to approximate floats with another theory.",
            e);
      }
      rawFloatingPointFormulaManager = new ReplaceFloatingPointWithNumeralAndFunctionTheory<>(
          wrappingHandler, rmgr, manager.getFunctionFormulaManager(),
          manager.getBooleanFormulaManager());
    break;
    case BITVECTOR:
      throw new InvalidConfigurationException("Value BITVECTOR is not valid for option cpa.predicate.encodeFloatAs");
    default:
      throw new AssertionError();
    }
    floatingPointFormulaManager = new FloatingPointFormulaManagerView(wrappingHandler, rawFloatingPointFormulaManager);
  }

  FormulaWrappingHandler getFormulaWrappingHandler() {
    return wrappingHandler;
  }

  private <T1 extends Formula, T2 extends Formula> T1 wrap(FormulaType<T1> targetType, T2 toWrap) {
    return wrappingHandler.wrap(targetType, toWrap);
  }

  private <T extends Formula> Formula unwrap(T f) {
    return wrappingHandler.unwrap(f);
  }

  public Path formatFormulaOutputFile(String function, int call, String formula, int index) {
    if (formulaDumpFile == null) {
      return null;
    }

    return formulaDumpFile.getPath(function, call, formula, index);
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
      t = getRationalFormulaManager().makeVariable(name);
    } else if (formulaType.isBitvectorType()) {
      FormulaType.BitvectorType impl = (FormulaType.BitvectorType) formulaType;
      t = bitvectorFormulaManager.makeVariable(impl.getSize(), name);
    } else if (formulaType.isFloatingPointType()) {
      t = floatingPointFormulaManager.makeVariable(name, (FormulaType.FloatingPointType)formulaType);
    } else if (formulaType.isArrayType()) {
      FormulaType.ArrayFormulaType<?,?> arrayType = (FormulaType.ArrayFormulaType<?,?>) formulaType;
      t = arrayFormulaManager.makeArray(name, arrayType.getIndexType(), arrayType.getElementType());
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
      t = getRationalFormulaManager().makeNumber(value);
    } else if (formulaType.isBitvectorType()) {
      t = bitvectorFormulaManager.makeBitvector((FormulaType<BitvectorFormula>)formulaType, value);
    } else if (formulaType.isFloatingPointType()) {
      t = floatingPointFormulaManager.makeNumber(value, (FormulaType.FloatingPointType)formulaType);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return (T) t;
  }

  /**
   * Make a number which type corresponds to the existing formula type.
   * // TODO: refactor all the {@code makeNumber} methods.
   */
  @SuppressWarnings("unchecked")
  public <T extends Formula> T makeNumber(T formula, Rational value) {
    Formula t;
    FormulaType<?> formulaType = getFormulaType(formula);
    if (formulaType.isIntegerType() && value.isIntegral()) {
      t = integerFormulaManager.makeNumber(value.toString());
    } else if (formulaType.isRationalType()) {
      t = getRationalFormulaManager().makeNumber(value.toString());
    } else if (value.isIntegral() && formulaType.isBitvectorType()) {
      t = bitvectorFormulaManager.makeBitvector((FormulaType<BitvectorFormula>)formulaType,
          new BigInteger(value.toString()));
    } else if (formulaType.isFloatingPointType()) {
      t = floatingPointFormulaManager.makeNumber(value, (FormulaType.FloatingPointType)formulaType);
    } else {
      throw new IllegalArgumentException("Not supported interface: " + formula);
    }

    return (T) t;
  }

  /**
   * Make a variable of the given type.
   */
  @SuppressWarnings("unchecked")
  public <T extends Formula> T makeNumber(FormulaType<T> formulaType, BigInteger value) {
    Formula t;
    if (formulaType.isIntegerType()) {
      t = integerFormulaManager.makeNumber(value);
    } else if (formulaType.isRationalType()) {
      t = getRationalFormulaManager().makeNumber(value);
    } else if (formulaType.isBitvectorType()) {
      t = bitvectorFormulaManager.makeBitvector((FormulaType<BitvectorFormula>)formulaType, value);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return (T) t;
  }

  @SuppressWarnings("unchecked")
  public  <T extends Formula> T makeNegate(T pNum) {
    Formula t;
    if (pNum instanceof IntegerFormula) {
      t = integerFormulaManager.negate((IntegerFormula)pNum);
    } else if (pNum instanceof RationalFormula) {
      t = getRationalFormulaManager().negate((RationalFormula)pNum);
    } else if (pNum instanceof BitvectorFormula) {
      t = bitvectorFormulaManager.negate((BitvectorFormula)pNum);
    } else if (pNum instanceof FloatingPointFormula) {
      t = floatingPointFormulaManager.negate((FloatingPointFormula)pNum);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return (T) t;
  }

  @SuppressWarnings("unchecked")
  public  <T extends Formula> T makePlus(T pF1, T pF2) {
    Formula t;
    if (pF1 instanceof IntegerFormula && pF2 instanceof IntegerFormula) {
      t = integerFormulaManager.add((IntegerFormula)pF1, (IntegerFormula)pF2);
    } else if (pF1 instanceof NumeralFormula && pF2 instanceof NumeralFormula) {
      t = rationalFormulaManager.add((NumeralFormula)pF1, (NumeralFormula)pF2);
    } else if (pF1 instanceof BitvectorFormula && pF2 instanceof BitvectorFormula) {
      t = bitvectorFormulaManager.add((BitvectorFormula)pF1, (BitvectorFormula)pF2);
    } else if (pF1 instanceof FloatingPointFormula && pF2 instanceof FloatingPointFormula) {
      t = floatingPointFormulaManager.add((FloatingPointFormula)pF1, (FloatingPointFormula)pF2);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return (T) t;
  }

  @SuppressWarnings("unchecked")
  public <T extends Formula> T makeMinus(T pF1, T pF2) {
    Formula t;
    if (pF1 instanceof IntegerFormula && pF2 instanceof IntegerFormula) {
      t = integerFormulaManager.subtract((IntegerFormula) pF1, (IntegerFormula) pF2);
    } else if (pF1 instanceof NumeralFormula && pF2 instanceof NumeralFormula) {
      t = getRationalFormulaManager().subtract((NumeralFormula) pF1, (NumeralFormula) pF2);
    } else if (pF1 instanceof BitvectorFormula && pF2 instanceof BitvectorFormula) {
      t = bitvectorFormulaManager.subtract((BitvectorFormula) pF1, (BitvectorFormula) pF2);
    } else if (pF1 instanceof FloatingPointFormula && pF2 instanceof FloatingPointFormula) {
      t = floatingPointFormulaManager.subtract((FloatingPointFormula)pF1, (FloatingPointFormula)pF2);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return (T) t;
  }
  @SuppressWarnings("unchecked")
  public  <T extends Formula> T makeMultiply(T pF1, T pF2) {
    Formula t;
    if (pF1 instanceof IntegerFormula && pF2 instanceof IntegerFormula) {
      t = integerFormulaManager.multiply((IntegerFormula) pF1, (IntegerFormula) pF2);
    } else if (pF1 instanceof NumeralFormula && pF2 instanceof NumeralFormula) {
      t = getRationalFormulaManager().multiply((NumeralFormula) pF1, (NumeralFormula) pF2);
    } else if (pF1 instanceof BitvectorFormula && pF2 instanceof BitvectorFormula) {
      t = bitvectorFormulaManager.multiply((BitvectorFormula) pF1, (BitvectorFormula) pF2);
    } else if (pF1 instanceof FloatingPointFormula && pF2 instanceof FloatingPointFormula) {
      t = floatingPointFormulaManager.multiply((FloatingPointFormula)pF1, (FloatingPointFormula)pF2);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return (T) t;
  }

  @SuppressWarnings("unchecked")
  public <T extends Formula> T  makeDivide(T pF1, T pF2, boolean pSigned) {
    Formula t;
    if (pF1 instanceof IntegerFormula && pF2 instanceof IntegerFormula) {
      t = integerFormulaManager.divide((IntegerFormula) pF1, (IntegerFormula) pF2);
    } else if (pF1 instanceof NumeralFormula && pF2 instanceof NumeralFormula) {
      t = getRationalFormulaManager().divide((NumeralFormula) pF1, (NumeralFormula) pF2);
    } else if (pF1 instanceof BitvectorFormula && pF2 instanceof BitvectorFormula) {
      t = bitvectorFormulaManager.divide((BitvectorFormula) pF1, (BitvectorFormula) pF2, pSigned);
    } else if (pF1 instanceof FloatingPointFormula && pF2 instanceof FloatingPointFormula) {
      t = floatingPointFormulaManager.divide((FloatingPointFormula)pF1, (FloatingPointFormula)pF2);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return (T) t;
  }

  @SuppressWarnings("unchecked")
  public <T extends Formula> T  makeModulo(T pF1, T pF2, boolean pSigned) {
    Formula t;
    if (pF1 instanceof IntegerFormula && pF2 instanceof IntegerFormula) {
      t = integerFormulaManager.modulo((IntegerFormula) pF1, (IntegerFormula) pF2);
    } else if (pF1 instanceof NumeralFormula && pF2 instanceof NumeralFormula) {
      t = getRationalFormulaManager().modulo((NumeralFormula) pF1, (NumeralFormula) pF2);
    } else if (pF1 instanceof BitvectorFormula && pF2 instanceof BitvectorFormula) {
      t = bitvectorFormulaManager.modulo((BitvectorFormula) pF1, (BitvectorFormula) pF2, pSigned);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return (T) t;
  }

  public <T extends Formula> BooleanFormula makeModularCongruence(T pF1, T pF2, long pModulo) {
    BooleanFormula t;
    if (pF1 instanceof IntegerFormula && pF2 instanceof IntegerFormula) {
      t = integerFormulaManager.modularCongruence((IntegerFormula) pF1, (IntegerFormula) pF2, pModulo);
    } else if (pF1 instanceof NumeralFormula && pF2 instanceof NumeralFormula) {
      t = getRationalFormulaManager().modularCongruence((NumeralFormula) pF1, (NumeralFormula) pF2, pModulo);
    } else if (pF1 instanceof BitvectorFormula && pF2 instanceof BitvectorFormula) {
      t = bitvectorFormulaManager.modularCongruence((BitvectorFormula) pF1, (BitvectorFormula) pF2, pModulo);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return t;
  }

  @SuppressWarnings("unchecked")
  public <T extends Formula> T makeNot(T pF1) {
    Formula t;
    if (pF1 instanceof BooleanFormula) {
      t = booleanFormulaManager.not((BooleanFormula)pF1);
    } else if (pF1 instanceof BitvectorFormula) {
      t = bitvectorFormulaManager.not((BitvectorFormula)pF1);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return (T) t;
  }

  @SuppressWarnings("unchecked")
  public <T extends Formula> T makeAnd(T pF1, T pF2) {
    Formula t;
    if (pF1 instanceof BooleanFormula && pF2 instanceof BooleanFormula) {
      t = booleanFormulaManager.and((BooleanFormula)pF1, (BooleanFormula)pF2);
    } else if (pF1 instanceof BitvectorFormula && pF2 instanceof BitvectorFormula) {
      t = bitvectorFormulaManager.and((BitvectorFormula)pF1, (BitvectorFormula)pF2);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return (T) t;
  }

  @SuppressWarnings("unchecked")
  public <T extends Formula> T makeOr(T pF1, T pF2) {
    Formula t;
    if (pF1 instanceof BooleanFormula && pF2 instanceof BooleanFormula) {
      t = booleanFormulaManager.or((BooleanFormula)pF1, (BooleanFormula)pF2);
    } else if (pF1 instanceof BitvectorFormula && pF2 instanceof BitvectorFormula) {
      t = bitvectorFormulaManager.or((BitvectorFormula)pF1, (BitvectorFormula)pF2);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return (T) t;
  }


  @SuppressWarnings("unchecked")
  public <T extends Formula> T makeXor(T pF1, T pF2) {
    Formula t;
    if (pF1 instanceof BooleanFormula && pF2 instanceof BooleanFormula) {
      t = booleanFormulaManager.xor((BooleanFormula)pF1, (BooleanFormula)pF2);
    } else if (pF1 instanceof BitvectorFormula && pF2 instanceof BitvectorFormula) {
      t = bitvectorFormulaManager.xor((BitvectorFormula)pF1, (BitvectorFormula)pF2);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return (T) t;
  }

  @SuppressWarnings("unchecked")
  public <T extends Formula> T makeShiftLeft(T pF1, T pF2) {
    Formula t;
    if (pF1 instanceof BitvectorFormula && pF2 instanceof BitvectorFormula) {
      t = bitvectorFormulaManager.shiftLeft((BitvectorFormula)pF1, (BitvectorFormula)pF2);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return (T) t;
  }

  @SuppressWarnings("unchecked")
  public <T extends Formula> T makeShiftRight(T pF1, T pF2, boolean signed) {
    Formula t;
    if (pF1 instanceof BitvectorFormula && pF2 instanceof BitvectorFormula) {
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
    Formula t;
    if (pFormula instanceof BitvectorFormula) {
      t = bitvectorFormulaManager.extract((BitvectorFormula)pFormula, pMsb, pLsb);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return (T) t;
  }

  @SuppressWarnings("unchecked")
  public <T extends Formula> T makeConcat(T pFormula, T pAppendFormula) {
    Formula t;
    if (pFormula instanceof BitvectorFormula) {
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
    Formula t;
    if (pFormula instanceof BitvectorFormula) {
      t = bitvectorFormulaManager.extend((BitvectorFormula)pFormula, pExtensionBits, pSigned);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return (T) t;
  }

  public  <T extends Formula> BooleanFormula makeEqual(T pLhs, T pRhs) {
    BooleanFormula t;
    if (pLhs instanceof BooleanFormula && pRhs instanceof BooleanFormula) {
      t = booleanFormulaManager.equivalence((BooleanFormula)pLhs, (BooleanFormula)pRhs);
    } else if (pLhs instanceof IntegerFormula && pRhs instanceof IntegerFormula) {
      t = integerFormulaManager.equal((IntegerFormula)pLhs, (IntegerFormula)pRhs);
    } else if (pLhs instanceof NumeralFormula && pRhs instanceof NumeralFormula) {
      t = getRationalFormulaManager().equal((NumeralFormula)pLhs, (NumeralFormula)pRhs);
    } else if (pLhs instanceof BitvectorFormula) {
      t = bitvectorFormulaManager.equal((BitvectorFormula)pLhs, (BitvectorFormula)pRhs);
    } else if (pLhs instanceof FloatingPointFormula && pRhs instanceof FloatingPointFormula) {
      t = floatingPointFormulaManager.equalWithFPSemantics((FloatingPointFormula)pLhs, (FloatingPointFormula)pRhs);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return t;
  }

  public  <T extends Formula> BooleanFormula makeLessOrEqual(T pLhs, T pRhs, boolean signed) {
    BooleanFormula t;
    if (pLhs instanceof IntegerFormula && pRhs instanceof IntegerFormula) {
      t = integerFormulaManager.lessOrEquals((IntegerFormula)pLhs, (IntegerFormula)pRhs);
    } else if (pLhs instanceof NumeralFormula && pRhs instanceof NumeralFormula) {
      t = getRationalFormulaManager().lessOrEquals((NumeralFormula)pLhs, (NumeralFormula)pRhs);
    } else if (pLhs instanceof BitvectorFormula && pRhs instanceof BitvectorFormula) {
      t = bitvectorFormulaManager.lessOrEquals((BitvectorFormula)pLhs, (BitvectorFormula)pRhs, signed);
    } else if (pLhs instanceof FloatingPointFormula && pRhs instanceof FloatingPointFormula) {
      t = floatingPointFormulaManager.lessOrEquals((FloatingPointFormula)pLhs, (FloatingPointFormula)pRhs);
    } else {
      throw new IllegalArgumentException("Not supported interface: " + pLhs + " " + pRhs);
    }

    return t;
  }
  public  <T extends Formula> BooleanFormula makeLessThan(T pLhs, T pRhs, boolean signed) {
    BooleanFormula t;
    if (pLhs instanceof IntegerFormula && pRhs instanceof IntegerFormula) {
      t = integerFormulaManager.lessThan((IntegerFormula) pLhs, (IntegerFormula) pRhs);
    } else if (pLhs instanceof NumeralFormula && pRhs instanceof NumeralFormula) {
      t = getRationalFormulaManager().lessThan((NumeralFormula) pLhs, (NumeralFormula) pRhs);
    } else if (pLhs instanceof BitvectorFormula && pRhs instanceof BitvectorFormula) {
      t = bitvectorFormulaManager.lessThan((BitvectorFormula) pLhs, (BitvectorFormula) pRhs, signed);
    } else if (pLhs instanceof FloatingPointFormula && pRhs instanceof FloatingPointFormula) {
      t = floatingPointFormulaManager.lessThan((FloatingPointFormula)pLhs, (FloatingPointFormula)pRhs);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return t;
  }

  public  <T extends Formula> BooleanFormula makeGreaterThan(T pLhs, T pRhs, boolean signed) {
    BooleanFormula t;
    if (pLhs instanceof IntegerFormula && pRhs instanceof IntegerFormula) {
      t = integerFormulaManager.greaterThan((IntegerFormula) pLhs, (IntegerFormula) pRhs);
    } else if (pLhs instanceof NumeralFormula && pRhs instanceof NumeralFormula) {
      t = getRationalFormulaManager().greaterThan((NumeralFormula) pLhs, (NumeralFormula) pRhs);
    } else if (pLhs instanceof BitvectorFormula && pRhs instanceof BitvectorFormula) {
      t = bitvectorFormulaManager.greaterThan((BitvectorFormula) pLhs, (BitvectorFormula) pRhs, signed);
    } else if (pLhs instanceof FloatingPointFormula && pRhs instanceof FloatingPointFormula) {
      t = floatingPointFormulaManager.greaterThan((FloatingPointFormula)pLhs, (FloatingPointFormula)pRhs);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return t;
  }

  public <T extends Formula> BooleanFormula makeGreaterOrEqual(T pLhs, T pRhs, boolean signed) {
    BooleanFormula t;
    if (pLhs instanceof IntegerFormula && pRhs instanceof IntegerFormula) {
      t = integerFormulaManager.greaterOrEquals((IntegerFormula) pLhs, (IntegerFormula) pRhs);
    } else if (pLhs instanceof NumeralFormula && pRhs instanceof NumeralFormula) {
      t = getRationalFormulaManager().greaterOrEquals((NumeralFormula) pLhs, (NumeralFormula) pRhs);
    } else if (pLhs instanceof BitvectorFormula && pRhs instanceof BitvectorFormula) {
      t = bitvectorFormulaManager.greaterOrEquals((BitvectorFormula) pLhs, (BitvectorFormula) pRhs, signed);
    } else if (pLhs instanceof FloatingPointFormula && pRhs instanceof FloatingPointFormula) {
      t = floatingPointFormulaManager.greaterOrEquals((FloatingPointFormula)pLhs, (FloatingPointFormula)pRhs);
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
    // lazy initialisation, because not all SMT-solvers support Rationals and maybe we only want to use Integers.
    if (rationalFormulaManager == null) {
      rationalFormulaManager = new NumeralFormulaManagerView<>(wrappingHandler, manager.getRationalFormulaManager());
    }
    return rationalFormulaManager;
  }

  public BooleanFormulaManagerView getBooleanFormulaManager() {
    return booleanFormulaManager;
  }

  public BitvectorFormulaManagerView getBitvectorFormulaManager() {
    return bitvectorFormulaManager;
  }

  public FloatingPointFormulaManagerView getFloatingPointFormulaManager() {
    return floatingPointFormulaManager;
  }

  public FunctionFormulaManagerView getFunctionFormulaManager() {
    return functionFormulaManager;
  }

  public QuantifiedFormulaManagerView getQuantifiedFormulaManager() {
    return quantifiedFormulaManager;
  }

  public ArrayFormulaManagerView getArrayFormulaManager() {
    return arrayFormulaManager;
  }

  public <T extends Formula> FormulaType<T> getFormulaType(T pFormula) {
    return wrappingHandler.getFormulaType(pFormula);
  }

  private <T extends Formula> FormulaType<T> getRawFormulaType(T pFormula) {
    return manager.getFormulaType(pFormula);
  }


  public <T extends Formula> BooleanFormula assignment(T left, T right) {
    FormulaType<?> lformulaType = getFormulaType(left);
    FormulaType<?> rformulaType = getFormulaType(right);
    if (!lformulaType.equals(rformulaType)) {
      throw new IllegalArgumentException("Can't assign different types! (" + lformulaType + " and " + rformulaType + ")");
    }

    if (lformulaType.isFloatingPointType()) {
      return getFloatingPointFormulaManager().assignment(
          (FloatingPointFormula)left, (FloatingPointFormula)right);
    }
    return makeEqual(left, right);
  }

  public BooleanFormula parse(String pS) throws IllegalArgumentException {
    return manager.parse(pS);
  }

  /**
   * Instantiate a list (!! guarantees to keep the ordering) of formulas.
   *  @see {@link #instantiate(BooleanFormula, SSAMap)}
   */
  public <F extends Formula> List<F> instantiate(List<F> pFormulas, final SSAMap pSsa) {
    return Lists.transform(pFormulas,
       new Function<F, F>() {
         @Override
         public F apply(F pF) {
           // Apply 'instantiate'!
           return instantiate(pF, pSsa);
         }
       });
  }

  public Set<String> instantiate(Iterable<String> pVariableNames, final SSAMap pSsa) {
    return from(pVariableNames).transform(new Function<String, String>() {
      @Override
      public String apply(String pArg0) {
        Pair<String, Integer> parsedVar = parseName(pArg0);
        return makeName(parsedVar.getFirst(), pSsa.getIndex(parsedVar.getFirst()));
      }
    }).toSet();
  }

  // the character for separating name and index of a value
  private static final String INDEX_SEPARATOR = "@";

  static String makeName(String name, int idx) {
    if (idx < 0) {
      return name;
    }
    return name + INDEX_SEPARATOR + idx;
  }


  /**
   * (Re-)instantiate the variables in pF with the SSA indices in pSsa.
   *
   * Existing instantiations are REPLACED by the
   * indices that are provided in the SSA map!
   */
  public <F extends Formula> F instantiate(F pF, final SSAMap pSsa) {
    return wrap(getFormulaType(pF),
        myFreeVariableNodeTransformer(unwrap(pF), new HashMap<Formula, Formula>(),
            new Function<String, String>() {

              @Override
              public String apply(String pFullSymbolName) {

                final Pair<String, Integer> indexedSymbol = parseName(pFullSymbolName);
                final int reInstantiateWithIndex = pSsa.getIndex(indexedSymbol.getFirst());

                if (reInstantiateWithIndex > 0) {
                  // OK, the variable has ALREADY an instance in the SSA, REPLACE it
                  return makeName(indexedSymbol.getFirst(), reInstantiateWithIndex);
                } else {
                  // the variable is not used in the SSA, keep it as is
                  return pFullSymbolName;
                }
              }
            })
        );
  }

  // various caches for speeding up expensive tasks
  //
  // cache for splitting arithmetic equalities in extractAtoms
  private final Map<Formula, Boolean> arithCache = new HashMap<>();

  // cache for uninstantiating terms (see uninstantiate() below)
  private final Map<Formula, Formula> uninstantiateCache = new HashMap<>();

  /**
   * Only use inside this package and for solver-specific classes
   * when creating a {@link Model}.
   * Do not use in client code!
   *
   * @throws IllegalArgumentException thrown if the given name is invalid
   */
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

  /**
   * Uninstantiate a given formula.
   * (remove the SSA indices from its free variables and UFs)
   *
   * @param pF  Input formula
   * @return    Uninstantiated formula
   */
  public <F extends Formula> F uninstantiate(F f) {
    return wrap(getFormulaType(f),
        myFreeVariableNodeTransformer(unwrap(f), uninstantiateCache,
            new Function<String, String>() {
              @Override
              public String apply(String pArg0) {
                // Un-instantiated variable name
                return parseName(pArg0).getFirst();
              }
            })
        );
  }

  public Set<BooleanFormula> uninstantiate(Collection<BooleanFormula> formulas) {
    return from(formulas)
        .transform(new Function<BooleanFormula, BooleanFormula>() {
          @Override
          public BooleanFormula apply(BooleanFormula pInput) {
            return uninstantiate(pInput);
          }
        })
        .toSet();
  }

  /**
   * Apply an arbitrary renaming to all free variables and UFs in a formula.
   * @param pFormula The formula in which the renaming should occur.
   * @param pRenameFunction The renaming function (may not return null).
   * @return A formula of the same type and structure as the input.
   */
  public <F extends Formula> F renameFreeVariablesAndUFs(F pFormula,
      Function<String, String> pRenameFunction) {

    return wrap(getFormulaType(pFormula),
        myFreeVariableNodeTransformer(unwrap(pFormula),
            new HashMap<Formula, Formula>(),
            pRenameFunction));
  }

  private <T extends Formula> T myFreeVariableNodeTransformer(
      final T pFormula,
      final Map<Formula, Formula> pCache,
      final Function<String, String> pRenameFunction) {

    Preconditions.checkNotNull(pCache);
    Preconditions.checkNotNull(pFormula);
    Preconditions.checkNotNull(pRenameFunction);

    Deque<Formula> toProcess = new ArrayDeque<>();

    // Add the formula to the work queue
    toProcess.push(pFormula);

    // Process the work queue
    while (!toProcess.isEmpty()) {
      final Formula tt = toProcess.peek();

      if (pCache.containsKey(tt)) {
        toProcess.pop();
        continue;
      }

      if (unsafeManager.isFreeVariable(tt)) {
        String oldName = unsafeManager.getName(tt);
        String newName = pRenameFunction.apply(oldName);
        Formula renamed = unsafeManager.replaceArgsAndName(
            tt, newName, ImmutableList.<Formula>of());
        pCache.put(tt, renamed);

      } else if (unsafeManager.isBoundVariable(tt)) {

        // There is no need for un-instantiating bound variables.
        pCache.put(tt, tt);

      } else if (unsafeManager.isQuantification(tt)) {

        // Quantifications are no function applications,
        //  i.e., they do not have an arity!

        BooleanFormula ttBody = unsafeManager.getQuantifiedBody(tt);
        BooleanFormula transformedBody = (BooleanFormula) pCache.get(ttBody);

        if (transformedBody != null) {
          // make a new quantified formula
          BooleanFormula newTt = unsafeManager.replaceQuantifiedBody(
              (BooleanFormula) tt, transformedBody);
          pCache.put(tt, newTt);

        } else {
          toProcess.push(ttBody);
        }

      } else {

        boolean allArgumentsTransformed = true;

        // Construct a new argument list for the function application.
        // ATTENTION: also boolean operators, like AND, OR, ...
        //             are function applications!
        int arity = unsafeManager.getArity(tt);
        List<Formula> newargs = Lists.newArrayListWithExpectedSize(arity);

        for (int i = 0; i < arity; ++i) {
          Formula c = unsafeManager.getArg(tt, i);
          Formula newC = pCache.get(c);

          if (newC != null) {
            newargs.add(newC);

          } else {
            toProcess.push(c);
            allArgumentsTransformed = false;
          }
        }

        // The Flag childrenDone indicates whether all arguments
        // of the function were already un-instantiated, i.e., the
        // un-instantiated formula of all arguments is in the cache.

        if (allArgumentsTransformed) {
          // Create an un-instantiated version of the
          // function application.

          toProcess.pop();
          Formula newt;

          if (unsafeManager.isUF(tt)) {
            String oldName = unsafeManager.getName(tt);
            assert oldName != null;

            String newName = pRenameFunction.apply(oldName);
            newt = unsafeManager.replaceArgsAndName(tt, newName, newargs);

          } else {
            newt = unsafeManager.replaceArgs(tt, newargs);
          }

          pCache.put(tt, newt);
        }
      }
    }

    @SuppressWarnings("unchecked")
    T result = (T)pCache.get(pFormula);
    assert result != null;
    assert getRawFormulaType(pFormula).equals(getRawFormulaType(result));
    return result;
  }

  /**
   * Extract all atoms of a given boolean formula.
   */
  public Collection<BooleanFormula> extractAtoms(BooleanFormula f, boolean splitArithEqualities) {
    return myExtractAtoms(f, splitArithEqualities,
        new Predicate<BooleanFormula>() {
          @Override
          public boolean apply(BooleanFormula pInput) {
            return unsafeManager.isAtom(pInput);
          }
        });
  }

  /**
   * Extract all disjuncts of a given boolean formula.
   * It removes the top-level "and" and "not" operators and returns the rest.
   */
  public Collection<BooleanFormula> extractDisjuncts(BooleanFormula f) {
    return myExtractAtoms(f, false /*splitArithEqualities not supported for disjuncts */,
        new Predicate<BooleanFormula>() {
          @Override
          public boolean apply(BooleanFormula pInput) {
            // treat as atomic if formula is neither "not" nor "and"
            return !(booleanFormulaManager.isNot(pInput) || booleanFormulaManager.isAnd(pInput));
          }
        });
  }

  public Collection<BooleanFormula> extractLiterals(BooleanFormula f) {

    return myExtractAtoms(f, false /*splitArithEqualities not supported for literals */,
        new Predicate<BooleanFormula>() {
          @Override
          public boolean apply(BooleanFormula pInput) {
            // TODO: description of UnsafeManager.isLiteral said "atom or negation of atom"
            // The implementation only checked for "atom or negation".
            // This method currently does the latter.
            return unsafeManager.isAtom(pInput)
                || booleanFormulaManager.isNot(pInput);
          }
        });
  }

  /**
   * @see UnsafeFormulaManager#splitNumeralEqualityIfPossible(Formula) for
   * documentation.
   */
  public List<BooleanFormula> splitNumeralEqualityIfPossible(BooleanFormula formula) {
    // only exported here for BooleanFormula because otherwise it is not type-safe
    return unsafeManager.splitNumeralEqualityIfPossible(formula);
  }

  private Collection<BooleanFormula> myExtractAtoms(BooleanFormula pFormula, boolean splitArithEqualities,
      Predicate<BooleanFormula> isLowestLevel) {
    Set<BooleanFormula> seen = new HashSet<>();
    List<BooleanFormula> result = new ArrayList<>();

    Deque<BooleanFormula> toProcess = new ArrayDeque<>();
    toProcess.push(pFormula);
    seen.add(pFormula);

    while (!toProcess.isEmpty()) {
      BooleanFormula f = toProcess.pop();
      assert seen.contains(f);

      if (unsafeManager.isBoundVariable(f)) {
        // Do nothing for variables that are bound by a quantifier!
        continue;
      }

      if (isLowestLevel.apply(f)) {
        if (splitArithEqualities && myIsPurelyArithmetic(f)) {
          List<BooleanFormula> split = unsafeManager.splitNumeralEqualityIfPossible(f);
          // some solvers might produce non-atomic formulas for split,
          // thus push it instead of adding it directly to result
          if (seen.add(split.get(0))) {
            toProcess.push(split.get(0));
          }
        }
        result.add(f);

      } else if (unsafeManager.isQuantification(f)) {
        BooleanFormula body = unsafeManager.getQuantifiedBody(f);
        if (seen.add(body)) {
          toProcess.push(body);
        }

      } else {
        // Go into this formula.
        for (int i = 0; i < unsafeManager.getArity(f); ++i) {
          Formula c = unsafeManager.getArg(f, i);
          assert getRawFormulaType(c).isBooleanType();
          if (seen.add((BooleanFormula)c)) {
            toProcess.push((BooleanFormula)c);
          }
        }
      }
    }

    return result;
  }

  // returns true if the given term is a pure arithmetic term
  private boolean myIsPurelyArithmetic(Formula f) {
    Boolean result = arithCache.get(f);
    if (result != null) { return result; }

    boolean res = true;
    if (unsafeManager.isUF(f)) {
      res = false;

    } else {
      int arity = unsafeManager.getArity(f);
      for (int i = 0; i < arity; ++i) {
        res = myIsPurelyArithmetic(unsafeManager.getArg(f, i));
        if (!res) {
          break;
        }
      }
    }
    arithCache.put(f, res);
    return res;
  }

  private final Predicate<Formula> FILTER_VARIABLES = new Predicate<Formula>() {
    @Override
    public boolean apply(Formula input) {
      return unsafeManager.isVariable(input);
    }
  };

  private final Predicate<Formula> FILTER_UF = new Predicate<Formula>() {
    @Override
    public boolean apply(Formula input) {
      return unsafeManager.isUF(input);
    }
  };

  private final Function<Formula, String> GET_NAME = new Function<Formula, String>() {
    @Override
    public String apply(Formula pInput) {
      return unsafeManager.getName(pInput);
    }
  };

  /**
   * Extract the names of all free variables in a formula.
   *
   * @param f   The input formula
   * @return    Set of variable names (might be instantiated)
   */
  public Set<String> extractVariableNames(Formula f) {
    return Sets.newHashSet(Collections2.transform(
        myExtractSubformulas(unwrap(f), FILTER_VARIABLES, true),
        GET_NAME));
  }

  /**
   * Extract the names of all free variables + UFs in a formula.
   *
   * @param f   The input formula
   * @param recurseIntoFunctions Whether to return arguments which only appear
   * inside functions.
   *
   * @return    Set of variable names (might be instantiated)
   */
  public Set<String> extractFunctionNames(Formula f,
      boolean recurseIntoFunctions) {
    return Sets.newHashSet(Collections2.transform(
        myExtractSubformulas(unwrap(f),
            Predicates.or(FILTER_UF, FILTER_VARIABLES), recurseIntoFunctions),
        GET_NAME));
  }

  /**
   * Extract pairs of <variable name, variable formula>
   *  of all free variables in a formula.
   *
   * @deprecated The type of the returned Formula objects is incorrect.
   * Thus consider using {@link #extractVariableNames(Formula)} instead.
   * @param pF The input formula
   * @return Map from variable names to variable formulas.
   */
  @Deprecated
  public Map<String, Formula> extractFreeVariableMap(Formula pF) {
    Map<String, Formula> result = Maps.newHashMap();

    for (Formula v: myExtractSubformulas(unwrap(pF), FILTER_VARIABLES, true)) {
      result.put(unsafeManager.getName(v), v);
    }

    return result;
  }

  private Collection<Formula> myExtractSubformulas(final Formula pFormula,
      Predicate<Formula> filter, boolean recurseIntoFunctions) {
    // TODO The FormulaType of returned formulas may not be correct,
    // because we cannot determine if for example a Rational formula
    // is really rational, or should be wrapped as a Bitvector formula
    Set<Formula> seen = new HashSet<>();
    List<Formula> result = new ArrayList<>();

    Deque<Formula> toProcess = new ArrayDeque<>();
    toProcess.push(pFormula);
    seen.add(pFormula);

    while (!toProcess.isEmpty()) {
      Formula f = toProcess.pop();
      assert seen.contains(f);

      if (unsafeManager.isBoundVariable(f)) {
        // Do nothing for variables that are bound by a quantifier!
        continue;
      }

      if (filter.apply(f)) {
        result.add(f);
        if (!recurseIntoFunctions) {
          continue;
        }
      }

      if (unsafeManager.isQuantification(f)) {
        Formula body = unsafeManager.getQuantifiedBody(f);
        if (seen.add(body)) {
          toProcess.push(body);
        }

      } else {
        // Go into this formula.
        for (int i = 0; i < unsafeManager.getArity(f); ++i) {
          Formula c = unsafeManager.getArg(f, i);

          if (seen.add(c)) {
            toProcess.push(c);
          }
        }
      }
    }

    return result;
  }

  public Appender dumpFormula(Formula pT) {
    return manager.dumpFormula(unwrap(pT));
  }

  public boolean isPurelyConjunctive(BooleanFormula t) {
    if (unsafeManager.isAtom(t)) {
      // term is atom
      return !containsIfThenElse(t);

    } else if (booleanFormulaManager.isNot(t)) {
      t = (BooleanFormula)unsafeManager.getArg(t, 0);
      return (unsafeManager.isUF(t) || unsafeManager.isAtom(t));

    } else if (booleanFormulaManager.isAnd(t)) {
      for (int i = 0; i < unsafeManager.getArity(t); ++i) {
        if (!isPurelyConjunctive((BooleanFormula)unsafeManager.getArg(t, i))) {
          return false;
        }
      }
      return true;

    } else {
      return false;
    }
  }

  private boolean containsIfThenElse(Formula f) {
    if (booleanFormulaManager.isIfThenElse(f)) {
      return true;
    }
    for (int i = 0; i < unsafeManager.getArity(f); ++i) {
      if (containsIfThenElse(unsafeManager.getArg(f, i))) {
        return true;
      }
    }
    return false;
  }

  static final String BitwiseAndUfName = "_&_";
  static final String BitwiseOrUfName ="_!!_"; // SMTInterpol does not allow "|" to be used
  static final String BitwiseXorUfName ="_^_";
  static final String BitwiseNotUfName ="_~_";

  // returns a formula with some "static learning" about some bitwise
  // operations, so that they are (a bit) "less uninterpreted"
  // Currently it add's the following formulas for each number literal n that
  // appears in the formula: "(n & 0 == 0) and (0 & n == 0)"
  // But only if an bitwise "and" occurs in the formula.
  private BooleanFormula myGetBitwiseAxioms(Formula f) {
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

    BooleanFormula result = booleanFormulaManager.makeBoolean(true);
    if (andFound) {
      // Note: We can assume that we have no real bitvectors here, so size should be not important
      // If it ever should be we can just add an method to the unsafe-manager to read the size.
      BitvectorFormula z = bitvectorFormulaManager.makeBitvector(1, 0);
      FormulaType<BitvectorFormula> type = FormulaType.getBitvectorTypeWithSize(1);
      //Term z = env.numeral("0");
      for (Formula nn : allLiterals) {
        BitvectorFormula n = bitvectorFormulaManager.wrap(type, nn);
        BitvectorFormula u1 = bitvectorFormulaManager.and(z, n);
        BitvectorFormula u2 = bitvectorFormulaManager.and(n, z);
        //Term u1 = env.term(bitwiseAndUfDecl, n, z);
        //Term u2 = env.term(bitwiseAndUfDecl, z, n);
        //Term e1;
        //e1 = env.term("=", u1, z);
        BooleanFormula e1 = bitvectorFormulaManager.equal(u1, z);
        //Term e2 = env.term("=", u2, z);
        BooleanFormula e2 = bitvectorFormulaManager.equal(u2, z);
        BooleanFormula a = booleanFormulaManager.and(e1, e2);
        //Term a = env.term("and", e1, e2);

        result = booleanFormulaManager.and(result, a); //env.term("and", result, a);
      }
    }
    return result;
  }

    // returns a formula with some "static learning" about some bitwise
    public BooleanFormula getBitwiseAxioms(BooleanFormula f) {
      return myGetBitwiseAxioms(f);
    }


  public String getVersion() {
    return manager.getVersion();
  }


  public boolean useBitwiseAxioms() {
    return useBitwiseAxioms;
  }

  public BooleanFormula createPredicateVariable(String pName) {
    return booleanFormulaManager.makeVariable(pName);
  }

  public BooleanFormula simplify(BooleanFormula input) {
    return unsafeManager.simplify(input);
  }

  /**
   * Use a SSA map to conclude what variables of a
   *  (instantiated) formula can be considered 'dead'.
   *
   * A variable is considered 'dead' if its SSA index
   *  is different from the index in the SSA map.
   *
   * @param pFormula
   * @param pSsa
   * @return
   */
  public Set<String> getDeadVariableNames(BooleanFormula pFormula, SSAMap pSsa) {
    Set<String> result = Sets.newHashSet();
    List<Formula> varFormulas = myGetDeadVariables(pFormula, pSsa);
    for (Formula f : varFormulas) {
      result.add(unsafeManager.getName(f));
    }

    return result;
  }

  /**
   * Helper method for {@link #getDeadVariableNames(BooleanFormula, SSAMap)}.
   * Do not make this method public, because the returned formulas have incorrect
   * types (they are not appropriately wrapped).
   */
  private List<Formula> myGetDeadVariables(BooleanFormula pFormula, SSAMap pSsa) {
    List<Formula> result = Lists.newArrayList();

    for (Formula varFormula: myExtractSubformulas(unwrap(pFormula),
        FILTER_VARIABLES, true)) {
      Pair<String, Integer> fullName = parseName(unsafeManager.getName(varFormula));
      String varName = fullName.getFirst();
      Integer varSsaIndex = fullName.getSecond();

      if (varSsaIndex == null) {
        if (pSsa.containsVariable(varName)) {
          result.add(varFormula);
        }

      } else {

        if (varSsaIndex != pSsa.getIndex(varName)) {
          result.add(varFormula);
        }
      }
    }

    return result;
  }

  /**
   * Eliminate all propositions about 'dead' variables
   *  in a given formula.
   *
   * Quantifier elimination is used! This has to be supported by the solver!
   *    (solver-independent approaches would be possible)
   *
   * A variable is considered 'dead' if its SSA index
   *  is different from the index in the SSA map.
   *
   * @param pF
   * @param pSsa
   * @return
   * @throws SolverException
   * @throws InterruptedException
   */
  public BooleanFormula eliminateDeadVariables(
      final BooleanFormula pF,
      final SSAMap pSsa)
    throws SolverException, InterruptedException {

    Preconditions.checkNotNull(pF);
    Preconditions.checkNotNull(pSsa);

    List<Formula> irrelevantVariables = myGetDeadVariables(pF, pSsa);

    BooleanFormula eliminationResult = pF;

    if (!irrelevantVariables.isEmpty()) {
      QuantifiedFormulaManagerView qfmgr = getQuantifiedFormulaManager();
      BooleanFormula quantifiedFormula = qfmgr.exists(irrelevantVariables, pF);
      eliminationResult = qfmgr.eliminateQuantifiers(quantifiedFormula);
    }

    eliminationResult = simplify(eliminationResult); // TODO: Benchmark the effect!
    return eliminationResult;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(new Statistics() {

      @Override
      public void printStatistics(PrintStream pOut, Result pResult, ReachedSet pReached) {
        try {
          symbolEncoding.dump(symbolEncodingFile);
        } catch (IOException e) {
          logger.logUserException(Level.WARNING, e, "Could not write symbol encoding to file");
        }
      }

      @Override
      public String getName() {
        return "";
      }

    });

  }
}
