// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.smt;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.FluentIterable.from;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.Appender;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView.BooleanFormulaTransformationVisitor;
import org.sosy_lab.cpachecker.util.predicates.smt.ReplaceIntegerWithBitvectorTheory.ReplaceIntegerEncodingOptions;
import org.sosy_lab.java_smt.api.ArrayFormula;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BitvectorFormulaManager;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.FloatingPointFormula;
import org.sosy_lab.java_smt.api.FloatingPointFormulaManager;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaManager;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.FormulaType.ArrayFormulaType;
import org.sosy_lab.java_smt.api.FormulaType.BitvectorType;
import org.sosy_lab.java_smt.api.FormulaType.FloatingPointType;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.FunctionDeclarationKind;
import org.sosy_lab.java_smt.api.IntegerFormulaManager;
import org.sosy_lab.java_smt.api.Model;
import org.sosy_lab.java_smt.api.NumeralFormula;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.java_smt.api.NumeralFormula.RationalFormula;
import org.sosy_lab.java_smt.api.QuantifiedFormulaManager.Quantifier;
import org.sosy_lab.java_smt.api.SolverException;
import org.sosy_lab.java_smt.api.Tactic;
import org.sosy_lab.java_smt.api.visitors.BooleanFormulaVisitor;
import org.sosy_lab.java_smt.api.visitors.DefaultBooleanFormulaVisitor;
import org.sosy_lab.java_smt.api.visitors.DefaultFormulaVisitor;
import org.sosy_lab.java_smt.api.visitors.FormulaVisitor;
import org.sosy_lab.java_smt.api.visitors.TraversalProcess;

/**
 * This class is the central entry point for all formula creation and manipulation operations for
 * client code. It delegates to the actual solver package and provides additional utilities. The
 * preferred way of instantiating this class is via {@link Solver#create(Configuration, LogManager,
 * ShutdownNotifier)}.
 *
 * <p>This class and some of its related classes have supporting operations for creating and
 * manipulation formulas with SSA indices: - {@link #makeVariable(FormulaType, String, int)} creates
 * a variable with an SSA index - {@link #instantiate(Formula, SSAMap)} adds SSA indices to
 * variables in a formula - {@link #uninstantiate(Formula)} removes all SSA indices from a formula
 *
 * <p>The method {@link #parseName(String)} is also related to this, but should not be used!
 */
@Options(prefix = "cpa.predicate")
public class FormulaManagerView {

  enum Theory {
    UNSUPPORTED,
    INTEGER,
    RATIONAL,
    BITVECTOR,
    FLOAT,
    ;

    String description() {
      if (this == INTEGER) {
        return "unbounded integers";
      } else {
        return name().toLowerCase() + "s";
      }
    }
  }

  private final LogManager logger;
  private final FormulaManager manager;
  private final FormulaWrappingHandler wrappingHandler;
  private final BooleanFormulaManagerView booleanFormulaManager;
  private final FunctionFormulaManagerView functionFormulaManager;
  private final ReplaceIntegerEncodingOptions intOptions;

  private final @Nullable BitvectorFormulaManagerView bitvectorFormulaManager;
  private final @Nullable FloatingPointFormulaManagerView floatingPointFormulaManager;
  private final @Nullable IntegerFormulaManagerView integerFormulaManager;

  // other formula managers use lazy initialization, because some solvers do not support them.
  private @Nullable RationalFormulaManagerView rationalFormulaManager;
  private @Nullable QuantifiedFormulaManagerView quantifiedFormulaManager;
  private @Nullable ArrayFormulaManagerView arrayFormulaManager;
  private @Nullable SLFormulaManagerView slFormulaManager;

  @Option(
      secure = true,
      name = "formulaDumpFilePattern",
      description = "where to dump interpolation and abstraction problems (format string)")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate formulaDumpFile = PathTemplate.ofFormatString("%s%04d-%s%03d.smt2");

  @Option(
      secure = true,
      description =
          "try to add some useful static-learning-like axioms for "
              + "bitwise operations (which are encoded as UFs): essentially, "
              + "we simply collect all the numbers used in bitwise operations, "
              + "and add axioms like (0 & n = 0)")
  private boolean useBitwiseAxioms = false;

  @Option(
      secure = true,
      description =
          "Theory to use as backend for bitvectors. If different from BITVECTOR, the specified"
              + " theory is used to approximate bitvectors. This can be used for solvers that do"
              + " not support bitvectors, or for increased performance. If UNSUPPORTED, solvers can"
              + " be used that support none of the possible alternatives, but CPAchecker will crash"
              + " if bitvectors are required by the analysis.")
  private Theory encodeBitvectorAs = Theory.BITVECTOR;

  @Option(
      secure = true,
      description =
          "Theory to use as backend for floats. If different from FLOAT, the specified theory is"
              + " used to approximate floats. This can be used for solvers that do not support"
              + " floating-point arithmetic, or for increased performance. If UNSUPPORTED, solvers"
              + " can be used that support none of the possible alternatives, but CPAchecker will"
              + " crash if floats are required by the analysis.")
  private Theory encodeFloatAs = Theory.FLOAT;

  @Option(
      secure = true,
      description =
          "Theory to use as backend for integers. If different from INTEGER, the specified theory"
              + " is used to approximate integers. This can be used for solvers that do not support"
              + " integers, or for increased performance. If UNSUPPORTED, solvers can be used that"
              + " support none of the possible alternatives, but CPAchecker will crash if integers"
              + " are required by the analysis.")
  private Theory encodeIntegerAs = Theory.INTEGER;

  @VisibleForTesting
  public FormulaManagerView(
      FormulaManager pFormulaManager, Configuration config, LogManager pLogger)
      throws InvalidConfigurationException {
    config.inject(this, FormulaManagerView.class);
    logger = pLogger;
    manager = checkNotNull(pFormulaManager);

    // Check unsupported configurations first for good error messages instead of assertions
    if (!ImmutableSet.of(Theory.UNSUPPORTED, Theory.BITVECTOR, Theory.INTEGER, Theory.RATIONAL)
        .contains(encodeBitvectorAs)) {
      throw new InvalidConfigurationException(
          "Invalid value "
              + encodeBitvectorAs
              + " for option cpa.predicate.encodeBitvectorAs. "
              + "This kind of theory approximation is not supported.");
    }
    if (!ImmutableSet.of(Theory.UNSUPPORTED, Theory.FLOAT, Theory.INTEGER, Theory.RATIONAL)
        .contains(encodeFloatAs)) {
      throw new InvalidConfigurationException(
          "Invalid value "
              + encodeFloatAs
              + " for option cpa.predicate.encodeFloatAs. "
              + "This kind of theory approximation is not supported.");
    }
    if (!ImmutableSet.of(Theory.UNSUPPORTED, Theory.INTEGER, Theory.BITVECTOR)
        .contains(encodeIntegerAs)) {
      throw new InvalidConfigurationException(
          "Invalid value "
              + encodeIntegerAs
              + " for option cpa.predicate.encodeIntegerAs. "
              + "This kind of theory approximation is not supported.");
    }

    intOptions = new ReplaceIntegerEncodingOptions(config);
    wrappingHandler =
        new FormulaWrappingHandler(
            manager, encodeBitvectorAs, encodeFloatAs, encodeIntegerAs, intOptions);

    booleanFormulaManager =
        new BooleanFormulaManagerView(wrappingHandler, manager.getBooleanFormulaManager());
    functionFormulaManager =
        new FunctionFormulaManagerView(wrappingHandler, manager.getUFManager());

    bitvectorFormulaManager = createBitvectorFormulaManager(config);
    floatingPointFormulaManager = createFloatingPointFormulaManager();
    integerFormulaManager = createIntegerFormulaManager(intOptions);

    logInfo();
  }

  private void logInfo() {
    List<Theory> unsupportedTheories = new ArrayList<>();
    if (encodeBitvectorAs == Theory.UNSUPPORTED) {
      unsupportedTheories.add(Theory.BITVECTOR);
    }
    if (encodeFloatAs == Theory.UNSUPPORTED) {
      unsupportedTheories.add(Theory.FLOAT);
    }
    if (encodeIntegerAs == Theory.UNSUPPORTED) {
      unsupportedTheories.add(Theory.INTEGER);
    }
    if (!unsupportedTheories.isEmpty()) {
      logger.log(
          Level.WARNING,
          "Theory of",
          from(unsupportedTheories).transform(Theory::description).join(Joiner.on(" and ")),
          "unsupported by current configuration.",
          "CPAchecker will crash if any of these are used during the analysis.");
    }

    List<String> approximations = new ArrayList<>();
    if (encodeIntegerAs != Theory.INTEGER && encodeIntegerAs != Theory.UNSUPPORTED) {
      approximations.add(
          "plain ints with "
              + encodeIntegerAs.description()
              + " with bitsize "
              + intOptions.getBitsize());
    }
    if (encodeBitvectorAs != Theory.BITVECTOR && encodeBitvectorAs != Theory.UNSUPPORTED) {
      approximations.add("ints with " + encodeBitvectorAs.description());
    }
    if (encodeFloatAs != Theory.FLOAT && encodeFloatAs != Theory.UNSUPPORTED) {
      approximations.add("floats with " + encodeFloatAs.description());
    }
    if (!approximations.isEmpty()) {
      logger.log(
          Level.WARNING,
          "Using unsound approximation of",
          Joiner.on(" and ").join(approximations),
          "for encoding program semantics.");
    }
  }

  /**
   * Creates the BitvectorFormulaManagerView or a replacement based on the option encodeBitvectorAs.
   */
  private @Nullable BitvectorFormulaManagerView createBitvectorFormulaManager(Configuration config)
      throws InvalidConfigurationException {
    if (encodeBitvectorAs == Theory.UNSUPPORTED) {
      return null;
    }
    BitvectorFormulaManager rawBvmgr;
    try {
      switch (encodeBitvectorAs) {
        case BITVECTOR:
          rawBvmgr = manager.getBitvectorFormulaManager();
          break;
        case INTEGER:
          rawBvmgr =
              new ReplaceBitvectorWithNumeralAndFunctionTheory<>(
                  wrappingHandler,
                  manager.getBooleanFormulaManager(),
                  manager.getIntegerFormulaManager(),
                  manager.getUFManager(),
                  config);
          break;
        case RATIONAL:
          rawBvmgr =
              new ReplaceBitvectorWithNumeralAndFunctionTheory<>(
                  wrappingHandler,
                  manager.getBooleanFormulaManager(),
                  manager.getRationalFormulaManager(),
                  manager.getUFManager(),
                  config);
          break;
        default:
          throw new AssertionError("unexpected encoding for bitvectors: " + encodeBitvectorAs);
      }
    } catch (UnsupportedOperationException e) {
      throw new InvalidConfigurationException(
          "The chosen SMT solver does not support the theory of "
              + encodeBitvectorAs.description()
              + ", please choose another SMT solver "
              + "or use the option cpa.predicate.encodeBitvectorAs "
              + "to approximate bitvectors with another theory. "
              + "The value UNSUPPORTED for this option can be used to override this, "
              + "but CPAchecker will crash if bitvectors are used during the analysis.",
          e);
    }
    return new BitvectorFormulaManagerView(
        wrappingHandler, rawBvmgr, manager.getBooleanFormulaManager());
  }

  /**
   * Creates the FloatingPointFormulaManagerView or a replacement based on the option encodeFloatAs.
   */
  private @Nullable FloatingPointFormulaManagerView createFloatingPointFormulaManager()
      throws InvalidConfigurationException {
    if (encodeFloatAs == Theory.UNSUPPORTED) {
      return null;
    }
    FloatingPointFormulaManager rawFpmgr;
    try {
      switch (encodeFloatAs) {
        case FLOAT:
          rawFpmgr = manager.getFloatingPointFormulaManager();
          break;
        case INTEGER:
          rawFpmgr =
              new ReplaceFloatingPointWithNumeralAndFunctionTheory<>(
                  wrappingHandler,
                  manager.getIntegerFormulaManager(),
                  manager.getUFManager(),
                  manager.getBooleanFormulaManager());
          break;
        case RATIONAL:
          rawFpmgr =
              new ReplaceFloatingPointWithNumeralAndFunctionTheory<>(
                  wrappingHandler,
                  manager.getRationalFormulaManager(),
                  manager.getUFManager(),
                  manager.getBooleanFormulaManager());
          break;
        default:
          throw new AssertionError("unexpected encoding for floating points: " + encodeFloatAs);
      }
    } catch (UnsupportedOperationException e) {
      throw new InvalidConfigurationException(
          "The chosen SMT solver does not support the theory of "
              + encodeFloatAs.description()
              + ", please choose another SMT solver "
              + "or use the option cpa.predicate.encodeFloatAs "
              + "to approximate floats with another theory. "
              + "The value UNSUPPORTED for this option can be used to override this, "
              + "but CPAchecker will crash if floats are used during the analysis.",
          e);
    }
    return new FloatingPointFormulaManagerView(wrappingHandler, rawFpmgr, manager.getUFManager());
  }

  /** Creates the IntegerFormulaManager or a replacement based on the option encodeIntegerAs. */
  private @Nullable IntegerFormulaManagerView createIntegerFormulaManager(
      ReplaceIntegerEncodingOptions pIntegerOptions) throws InvalidConfigurationException {
    if (encodeIntegerAs == Theory.UNSUPPORTED) {
      return null;
    }
    IntegerFormulaManager rawImgr;
    try {
      switch (encodeIntegerAs) {
        case INTEGER:
          rawImgr = manager.getIntegerFormulaManager();
          break;
        case BITVECTOR:
          rawImgr =
              new ReplaceIntegerWithBitvectorTheory(
                  wrappingHandler,
                  manager.getBitvectorFormulaManager(),
                  manager.getBooleanFormulaManager(),
                  pIntegerOptions);
          break;
        default:
          throw new AssertionError("unexpected encoding for plain integers: " + encodeIntegerAs);
      }
    } catch (UnsupportedOperationException e) {
      throw new InvalidConfigurationException(
          "The chosen SMT solver does not support the theory of "
              + encodeIntegerAs.description()
              + ", please choose another SMT solver "
              + "or use the option cpa.predicate.encodeIntegerAs "
              + "to approximate integers with another theory. "
              + "The value UNSUPPORTED for this option can be used to override this, "
              + "but CPAchecker will crash if integers are used during the analysis.",
          e);
    }
    return new IntegerFormulaManagerView(wrappingHandler, rawImgr);
  }

  FormulaWrappingHandler getFormulaWrappingHandler() {
    return wrappingHandler;
  }

  // DO NOT MAKE THIS METHOD PUBLIC!
  FormulaManager getRawFormulaManager() {
    return manager;
  }

  private <T1 extends Formula, T2 extends Formula> T1 wrap(FormulaType<T1> targetType, T2 toWrap) {
    return wrappingHandler.wrap(targetType, toWrap);
  }

  private <T extends Formula> Formula unwrap(T f) {
    return wrappingHandler.unwrap(f);
  }

  public @Nullable Path formatFormulaOutputFile(
      String function, int call, String formula, int index) {
    if (formulaDumpFile == null) {
      return null;
    }

    return formulaDumpFile.getPath(function, call, formula, index);
  }

  public void dumpFormulaToFile(BooleanFormula f, @Nullable Path outputFile) {
    if (outputFile != null) {
      try {
        IO.writeFile(outputFile, Charset.defaultCharset(), dumpFormula(f));
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Failed to save formula to file");
      }
    }
  }

  /**
   * Helper method for creating variables of the given type.
   *
   * @param formulaType the type of the variable.
   * @param name the name of the variable.
   * @return the created variable.
   */
  public <T extends Formula> T makeVariable(FormulaType<T> formulaType, String name) {
    Formula t;
    if (formulaType.isBooleanType()) {
      t = booleanFormulaManager.makeVariable(name);
    } else if (formulaType.isIntegerType()) {
      t = getIntegerFormulaManager().makeVariable(name);
    } else if (formulaType.isRationalType()) {
      t = getRationalFormulaManager().makeVariable(name);
    } else if (formulaType.isBitvectorType()) {
      FormulaType.BitvectorType impl = (FormulaType.BitvectorType) formulaType;
      t = getBitvectorFormulaManager().makeVariable(impl.getSize(), name);
    } else if (formulaType.isFloatingPointType()) {
      t =
          getFloatingPointFormulaManager()
              .makeVariable(name, (FormulaType.FloatingPointType) formulaType);
    } else if (formulaType.isArrayType()) {
      FormulaType.ArrayFormulaType<?, ?> arrayType =
          (FormulaType.ArrayFormulaType<?, ?>) formulaType;
      t =
          getArrayFormulaManager()
              .makeArray(name, arrayType.getIndexType(), arrayType.getElementType());
    } else {
      throw new IllegalArgumentException("Unknown formula type");
    }

    @SuppressWarnings("unchecked")
    T out = (T) t;
    return out;
  }

  /** Make a variable of the given type. */
  @SuppressWarnings("unchecked")
  public <T extends Formula> T makeNumber(FormulaType<T> formulaType, long value) {
    Formula t;
    if (formulaType.isIntegerType()) {
      t = getIntegerFormulaManager().makeNumber(value);
    } else if (formulaType.isRationalType()) {
      t = getRationalFormulaManager().makeNumber(value);
    } else if (formulaType.isBitvectorType()) {
      t =
          getBitvectorFormulaManager()
              .makeBitvector((FormulaType<BitvectorFormula>) formulaType, value);
    } else if (formulaType.isFloatingPointType()) {
      t =
          getFloatingPointFormulaManager()
              .makeNumber(value, (FormulaType.FloatingPointType) formulaType);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return (T) t;
  }

  /**
   * Make a number which type corresponds to the existing formula type. // TODO: refactor all the
   * {@code makeNumber} methods.
   */
  @SuppressWarnings("unchecked")
  public <T extends Formula> T makeNumber(T formula, Rational value) {
    Formula t;
    FormulaType<?> formulaType = getFormulaType(formula);
    if (formulaType.isIntegerType() && value.isIntegral()) {
      t = getIntegerFormulaManager().makeNumber(value.toString());
    } else if (formulaType.isRationalType()) {
      t = getRationalFormulaManager().makeNumber(value.toString());
    } else if (value.isIntegral() && formulaType.isBitvectorType()) {
      t =
          getBitvectorFormulaManager()
              .makeBitvector(
                  (FormulaType<BitvectorFormula>) formulaType, new BigInteger(value.toString()));
    } else if (formulaType.isFloatingPointType()) {
      t =
          getFloatingPointFormulaManager()
              .makeNumber(value, (FormulaType.FloatingPointType) formulaType);
    } else {
      throw new IllegalArgumentException("Not supported interface: " + formula);
    }

    return (T) t;
  }

  /** Make a variable of the given type. */
  @SuppressWarnings("unchecked")
  public <T extends Formula> T makeNumber(FormulaType<T> formulaType, BigInteger value) {
    Formula t;
    if (formulaType.isIntegerType()) {
      t = getIntegerFormulaManager().makeNumber(value);
    } else if (formulaType.isRationalType()) {
      t = getRationalFormulaManager().makeNumber(value);
    } else if (formulaType.isBitvectorType()) {
      t =
          getBitvectorFormulaManager()
              .makeBitvector((FormulaType<BitvectorFormula>) formulaType, value);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return (T) t;
  }

  @SuppressWarnings("unchecked")
  public <T extends Formula> T makeNegate(T pNum) {
    Formula t;
    if (pNum instanceof IntegerFormula) {
      t = getIntegerFormulaManager().negate((IntegerFormula) pNum);
    } else if (pNum instanceof RationalFormula) {
      t = getRationalFormulaManager().negate((RationalFormula) pNum);
    } else if (pNum instanceof BitvectorFormula) {
      t = getBitvectorFormulaManager().negate((BitvectorFormula) pNum);
    } else if (pNum instanceof FloatingPointFormula) {
      t = getFloatingPointFormulaManager().negate((FloatingPointFormula) pNum);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return (T) t;
  }

  @SuppressWarnings("unchecked")
  public <T extends Formula> T makePlus(T pF1, T pF2) {
    Formula t;
    if (pF1 instanceof IntegerFormula && pF2 instanceof IntegerFormula) {
      t = getIntegerFormulaManager().add((IntegerFormula) pF1, (IntegerFormula) pF2);
    } else if (pF1 instanceof NumeralFormula && pF2 instanceof NumeralFormula) {
      t = getRationalFormulaManager().add((NumeralFormula) pF1, (NumeralFormula) pF2);
    } else if (pF1 instanceof BitvectorFormula && pF2 instanceof BitvectorFormula) {
      t = getBitvectorFormulaManager().add((BitvectorFormula) pF1, (BitvectorFormula) pF2);
    } else if (pF1 instanceof FloatingPointFormula && pF2 instanceof FloatingPointFormula) {
      t =
          getFloatingPointFormulaManager()
              .add((FloatingPointFormula) pF1, (FloatingPointFormula) pF2);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return (T) t;
  }

  @SuppressWarnings("unchecked")
  public <T extends Formula> T makeMinus(T pF1, T pF2) {
    Formula t;
    if (pF1 instanceof IntegerFormula && pF2 instanceof IntegerFormula) {
      t = getIntegerFormulaManager().subtract((IntegerFormula) pF1, (IntegerFormula) pF2);
    } else if (pF1 instanceof NumeralFormula && pF2 instanceof NumeralFormula) {
      t = getRationalFormulaManager().subtract((NumeralFormula) pF1, (NumeralFormula) pF2);
    } else if (pF1 instanceof BitvectorFormula && pF2 instanceof BitvectorFormula) {
      t = getBitvectorFormulaManager().subtract((BitvectorFormula) pF1, (BitvectorFormula) pF2);
    } else if (pF1 instanceof FloatingPointFormula && pF2 instanceof FloatingPointFormula) {
      t =
          getFloatingPointFormulaManager()
              .subtract((FloatingPointFormula) pF1, (FloatingPointFormula) pF2);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return (T) t;
  }

  @SuppressWarnings("unchecked")
  public <T extends Formula> T makeMultiply(T pF1, T pF2) {
    Formula t;
    if (pF1 instanceof IntegerFormula && pF2 instanceof IntegerFormula) {
      t = getIntegerFormulaManager().multiply((IntegerFormula) pF1, (IntegerFormula) pF2);
    } else if (pF1 instanceof NumeralFormula && pF2 instanceof NumeralFormula) {
      t = getRationalFormulaManager().multiply((NumeralFormula) pF1, (NumeralFormula) pF2);
    } else if (pF1 instanceof BitvectorFormula && pF2 instanceof BitvectorFormula) {
      t = getBitvectorFormulaManager().multiply((BitvectorFormula) pF1, (BitvectorFormula) pF2);
    } else if (pF1 instanceof FloatingPointFormula && pF2 instanceof FloatingPointFormula) {
      t =
          getFloatingPointFormulaManager()
              .multiply((FloatingPointFormula) pF1, (FloatingPointFormula) pF2);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return (T) t;
  }

  /**
   * This method returns the formula for the DIVIDE-operator. Depending on the used formulaManager,
   * the result can be conform to either C99- or the SMTlib2-standard.
   *
   * <p>Example: SMTlib2: 10%3==1, 10%(-3)==1, (-10)%3==2, (-10)%(-3)==2 C99: 10%3==1, 10%(-3)==1,
   * (-10)%3==(-1), (-10)%(-3)==(-1)
   */
  @SuppressWarnings("unchecked")
  public <T extends Formula> T makeDivide(T pF1, T pF2, boolean pSigned) {
    Formula t;
    if (pF1 instanceof IntegerFormula && pF2 instanceof IntegerFormula) {
      t = getIntegerFormulaManager().divide((IntegerFormula) pF1, (IntegerFormula) pF2);
    } else if (pF1 instanceof NumeralFormula && pF2 instanceof NumeralFormula) {
      t = getRationalFormulaManager().divide((NumeralFormula) pF1, (NumeralFormula) pF2);
    } else if (pF1 instanceof BitvectorFormula && pF2 instanceof BitvectorFormula) {
      t =
          getBitvectorFormulaManager()
              .divide((BitvectorFormula) pF1, (BitvectorFormula) pF2, pSigned);
    } else if (pF1 instanceof FloatingPointFormula && pF2 instanceof FloatingPointFormula) {
      t =
          getFloatingPointFormulaManager()
              .divide((FloatingPointFormula) pF1, (FloatingPointFormula) pF2);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return (T) t;
  }

  /**
   * This method returns the formula for the MODULO-operator. Depending on the used formulaManager,
   * the result can be conform to either C99- or the SMTlib2-standard.
   *
   * <p>Example: SMTlib2: 10%3==1, 10%(-3)==1, (-10)%3==2, (-10)%(-3)==2 C99: 10%3==1, 10%(-3)==1,
   * (-10)%3==(-1), (-10)%(-3)==(-1)
   */
  @SuppressWarnings("unchecked")
  public <T extends Formula> T makeModulo(T pF1, T pF2, boolean pSigned) {
    Formula t;
    if (pF1 instanceof IntegerFormula && pF2 instanceof IntegerFormula) {
      t = getIntegerFormulaManager().modulo((IntegerFormula) pF1, (IntegerFormula) pF2);
    } else if (pF1 instanceof BitvectorFormula && pF2 instanceof BitvectorFormula) {
      t =
          getBitvectorFormulaManager()
              .modulo((BitvectorFormula) pF1, (BitvectorFormula) pF2, pSigned);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return (T) t;
  }

  public <T extends Formula> BooleanFormula makeModularCongruence(
      T pF1, T pF2, long pModulo, boolean pSigned) {
    return makeModularCongruence(pF1, pF2, BigInteger.valueOf(pModulo), pSigned);
  }

  public <T extends Formula> BooleanFormula makeModularCongruence(
      T pF1, T pF2, BigInteger pModulo, boolean pSigned) {
    BooleanFormula t;
    if (pF1 instanceof IntegerFormula && pF2 instanceof IntegerFormula) {
      t =
          getIntegerFormulaManager()
              .modularCongruence((IntegerFormula) pF1, (IntegerFormula) pF2, pModulo);
    } else if (pF1 instanceof NumeralFormula && pF2 instanceof NumeralFormula) {
      t = booleanFormulaManager.makeTrue();
    } else if (pF1 instanceof BitvectorFormula && pF2 instanceof BitvectorFormula) {
      Formula unwrapped1 = unwrap(pF1);
      Formula unwrapped2 = unwrap(pF2);
      if (unwrapped1 instanceof IntegerFormula && unwrapped2 instanceof IntegerFormula) {
        t =
            getIntegerFormulaManager()
                .modularCongruence(
                    (IntegerFormula) unwrapped1, (IntegerFormula) unwrapped2, pModulo);
      } else {
        BitvectorFormulaManagerView bvmgr = getBitvectorFormulaManager();
        BitvectorFormula constant =
            bvmgr.makeBitvector(bvmgr.getLength((BitvectorFormula) pF1), pModulo);
        t =
            bvmgr.equal(
                bvmgr.modulo((BitvectorFormula) pF1, constant, pSigned),
                bvmgr.modulo((BitvectorFormula) pF2, constant, pSigned));
      }
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return t;
  }

  @SuppressWarnings("unchecked")
  public <T extends Formula> T makeNot(T pF1) {
    Formula t;
    if (pF1 instanceof BooleanFormula) {
      t = booleanFormulaManager.not((BooleanFormula) pF1);
    } else if (pF1 instanceof BitvectorFormula) {
      t = getBitvectorFormulaManager().not((BitvectorFormula) pF1);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return (T) t;
  }

  @SuppressWarnings("unchecked")
  public <T extends Formula> T makeAnd(T pF1, T pF2) {
    Formula t;
    if (pF1 instanceof BooleanFormula && pF2 instanceof BooleanFormula) {
      t = booleanFormulaManager.and((BooleanFormula) pF1, (BooleanFormula) pF2);
    } else if (pF1 instanceof BitvectorFormula && pF2 instanceof BitvectorFormula) {
      t = getBitvectorFormulaManager().and((BitvectorFormula) pF1, (BitvectorFormula) pF2);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return (T) t;
  }

  @SuppressWarnings("unchecked")
  public <T extends Formula> T makeOr(T pF1, T pF2) {
    Formula t;
    if (pF1 instanceof BooleanFormula && pF2 instanceof BooleanFormula) {
      t = booleanFormulaManager.or((BooleanFormula) pF1, (BooleanFormula) pF2);
    } else if (pF1 instanceof BitvectorFormula && pF2 instanceof BitvectorFormula) {
      t = getBitvectorFormulaManager().or((BitvectorFormula) pF1, (BitvectorFormula) pF2);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return (T) t;
  }

  @SuppressWarnings("unchecked")
  public <T extends Formula> T makeXor(T pF1, T pF2) {
    Formula t;
    if (pF1 instanceof BooleanFormula && pF2 instanceof BooleanFormula) {
      t = booleanFormulaManager.xor((BooleanFormula) pF1, (BooleanFormula) pF2);
    } else if (pF1 instanceof BitvectorFormula && pF2 instanceof BitvectorFormula) {
      t = getBitvectorFormulaManager().xor((BitvectorFormula) pF1, (BitvectorFormula) pF2);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return (T) t;
  }

  @SuppressWarnings("unchecked")
  public <T extends Formula> T makeShiftLeft(T pF1, T pF2) {
    Formula t;
    if (pF1 instanceof BitvectorFormula && pF2 instanceof BitvectorFormula) {
      t = getBitvectorFormulaManager().shiftLeft((BitvectorFormula) pF1, (BitvectorFormula) pF2);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return (T) t;
  }

  @SuppressWarnings("unchecked")
  public <T extends Formula> T makeShiftRight(T pF1, T pF2, boolean signed) {
    Formula t;
    if (pF1 instanceof BitvectorFormula && pF2 instanceof BitvectorFormula) {
      t =
          getBitvectorFormulaManager()
              .shiftRight((BitvectorFormula) pF1, (BitvectorFormula) pF2, signed);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return (T) t;
  }

  /** Returns a term representing the selection of pFormula[pMsb:pLsb]. */
  @SuppressWarnings("unchecked")
  public <T extends Formula> T makeExtract(T pFormula, int pMsb, int pLsb, boolean signed) {
    checkArgument(pLsb >= 0);
    checkArgument(pMsb >= pLsb);
    checkNotNull(pFormula);
    Formula t;
    if (pFormula instanceof BitvectorFormula) {
      t = getBitvectorFormulaManager().extract((BitvectorFormula) pFormula, pMsb, pLsb, signed);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return (T) t;
  }

  @SuppressWarnings("unchecked")
  public <T extends Formula> T makeConcat(T pFormula, T pAppendFormula) {
    Formula t;
    if (pFormula instanceof BitvectorFormula) {
      t =
          getBitvectorFormulaManager()
              .concat((BitvectorFormula) pFormula, (BitvectorFormula) pAppendFormula);
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
      t = getBitvectorFormulaManager().extend((BitvectorFormula) pFormula, pExtensionBits, pSigned);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return (T) t;
  }

  @SuppressWarnings("unchecked")
  public <T extends Formula> BooleanFormula makeEqual(T pLhs, T pRhs) {
    BooleanFormula t;
    if (pLhs instanceof BooleanFormula && pRhs instanceof BooleanFormula) {
      t = booleanFormulaManager.equivalence((BooleanFormula) pLhs, (BooleanFormula) pRhs);
    } else if (pLhs instanceof IntegerFormula && pRhs instanceof IntegerFormula) {
      t = getIntegerFormulaManager().equal((IntegerFormula) pLhs, (IntegerFormula) pRhs);
    } else if (pLhs instanceof NumeralFormula && pRhs instanceof NumeralFormula) {
      t = getRationalFormulaManager().equal((NumeralFormula) pLhs, (NumeralFormula) pRhs);
    } else if (pLhs instanceof BitvectorFormula) {
      t = getBitvectorFormulaManager().equal((BitvectorFormula) pLhs, (BitvectorFormula) pRhs);
    } else if (pLhs instanceof FloatingPointFormula && pRhs instanceof FloatingPointFormula) {
      t =
          getFloatingPointFormulaManager()
              .equalWithFPSemantics((FloatingPointFormula) pLhs, (FloatingPointFormula) pRhs);
    } else if (pLhs instanceof ArrayFormula<?, ?> && pRhs instanceof ArrayFormula<?, ?>) {
      @SuppressWarnings("rawtypes")
      ArrayFormula rhs = (ArrayFormula) pRhs;
      t = getArrayFormulaManager().equivalence((ArrayFormula<?, ?>) pLhs, rhs);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return t;
  }

  public <T extends Formula> BooleanFormula makeLessOrEqual(T pLhs, T pRhs, boolean signed) {
    BooleanFormula t;
    if (pLhs instanceof IntegerFormula && pRhs instanceof IntegerFormula) {
      t = getIntegerFormulaManager().lessOrEquals((IntegerFormula) pLhs, (IntegerFormula) pRhs);
    } else if (pLhs instanceof NumeralFormula && pRhs instanceof NumeralFormula) {
      t = getRationalFormulaManager().lessOrEquals((NumeralFormula) pLhs, (NumeralFormula) pRhs);
    } else if (pLhs instanceof BitvectorFormula && pRhs instanceof BitvectorFormula) {
      t =
          getBitvectorFormulaManager()
              .lessOrEquals((BitvectorFormula) pLhs, (BitvectorFormula) pRhs, signed);
    } else if (pLhs instanceof FloatingPointFormula && pRhs instanceof FloatingPointFormula) {
      t =
          getFloatingPointFormulaManager()
              .lessOrEquals((FloatingPointFormula) pLhs, (FloatingPointFormula) pRhs);
    } else {
      throw new IllegalArgumentException("Not supported interface: " + pLhs + " " + pRhs);
    }

    return t;
  }

  public <T extends Formula> BooleanFormula makeLessThan(T pLhs, T pRhs, boolean signed) {
    BooleanFormula t;
    if (pLhs instanceof IntegerFormula && pRhs instanceof IntegerFormula) {
      t = getIntegerFormulaManager().lessThan((IntegerFormula) pLhs, (IntegerFormula) pRhs);
    } else if (pLhs instanceof NumeralFormula && pRhs instanceof NumeralFormula) {
      t = getRationalFormulaManager().lessThan((NumeralFormula) pLhs, (NumeralFormula) pRhs);
    } else if (pLhs instanceof BitvectorFormula && pRhs instanceof BitvectorFormula) {
      t =
          getBitvectorFormulaManager()
              .lessThan((BitvectorFormula) pLhs, (BitvectorFormula) pRhs, signed);
    } else if (pLhs instanceof FloatingPointFormula && pRhs instanceof FloatingPointFormula) {
      t =
          getFloatingPointFormulaManager()
              .lessThan((FloatingPointFormula) pLhs, (FloatingPointFormula) pRhs);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return t;
  }

  public <T extends Formula> BooleanFormula makeGreaterThan(T pLhs, T pRhs, boolean signed) {
    BooleanFormula t;
    if (pLhs instanceof IntegerFormula && pRhs instanceof IntegerFormula) {
      t = getIntegerFormulaManager().greaterThan((IntegerFormula) pLhs, (IntegerFormula) pRhs);
    } else if (pLhs instanceof NumeralFormula && pRhs instanceof NumeralFormula) {
      t = getRationalFormulaManager().greaterThan((NumeralFormula) pLhs, (NumeralFormula) pRhs);
    } else if (pLhs instanceof BitvectorFormula && pRhs instanceof BitvectorFormula) {
      t =
          getBitvectorFormulaManager()
              .greaterThan((BitvectorFormula) pLhs, (BitvectorFormula) pRhs, signed);
    } else if (pLhs instanceof FloatingPointFormula && pRhs instanceof FloatingPointFormula) {
      t =
          getFloatingPointFormulaManager()
              .greaterThan((FloatingPointFormula) pLhs, (FloatingPointFormula) pRhs);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return t;
  }

  public <T extends Formula> BooleanFormula makeGreaterOrEqual(T pLhs, T pRhs, boolean signed) {
    BooleanFormula t;
    if (pLhs instanceof IntegerFormula && pRhs instanceof IntegerFormula) {
      t = getIntegerFormulaManager().greaterOrEquals((IntegerFormula) pLhs, (IntegerFormula) pRhs);
    } else if (pLhs instanceof NumeralFormula && pRhs instanceof NumeralFormula) {
      t = getRationalFormulaManager().greaterOrEquals((NumeralFormula) pLhs, (NumeralFormula) pRhs);
    } else if (pLhs instanceof BitvectorFormula && pRhs instanceof BitvectorFormula) {
      t =
          getBitvectorFormulaManager()
              .greaterOrEquals((BitvectorFormula) pLhs, (BitvectorFormula) pRhs, signed);
    } else if (pLhs instanceof FloatingPointFormula && pRhs instanceof FloatingPointFormula) {
      t =
          getFloatingPointFormulaManager()
              .greaterOrEquals((FloatingPointFormula) pLhs, (FloatingPointFormula) pRhs);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }

    return t;
  }

  /**
   * Create a formula for the constraint that a term is a valid index for an array/list/etc. with a
   * given start and a length: {@code start <= term && term < (start + length)}. The start value is
   * included in the range, the end value is not.
   *
   * @param term The term that should be in the range.
   * @param start The inclusive start value of the range.
   * @param length The length of the range (end is exclusive).
   * @param signed Whether the arithmetic should be signed or unsigned.
   * @return A BooleanFormula representing a constraint about term.
   */
  public <T extends Formula> BooleanFormula makeElementIndexConstraint(
      T term, T start, int length, boolean signed) {
    FormulaType<T> type = getFormulaType(start);
    T end = makePlus(start, makeNumber(type, length));
    return booleanFormulaManager.and(
        makeLessOrEqual(start, term, signed), makeLessThan(term, end, signed));
  }

  /**
   * Create a formula for the constraint that a term is in a given inclusive range [start, end].
   *
   * @param term The term that should be in the range.
   * @param start The inclusive start value of the range.
   * @param end The exclusive end value of the range.
   * @param signed Whether the arithmetic should be signed or unsigned.
   * @return A BooleanFormula representing a constraint about term.
   */
  public <T extends Formula> BooleanFormula makeRangeConstraint(
      T term, T start, T end, boolean signed) {
    return booleanFormulaManager.and(
        makeLessOrEqual(start, term, signed), makeLessOrEqual(term, end, signed));
  }

  /** Create a variable with an SSA index. */
  public <T extends Formula> T makeVariable(FormulaType<T> formulaType, String name, int idx) {
    return makeVariable(formulaType, makeName(name, idx));
  }

  /**
   * Create a variable that should never get an SSA index, even when calling {@link
   * #instantiate(Formula, SSAMap)}.
   */
  public <T extends Formula> T makeVariableWithoutSSAIndex(
      FormulaType<T> formulaType, String name) {
    return makeVariable(formulaType, makeNameNoIndex(name));
  }

  public IntegerFormulaManagerView getIntegerFormulaManager() throws UnsupportedOperationException {
    if (integerFormulaManager == null) {
      throw new UnsupportedOperationException(
          "Integers attempted to be used but configured to be unsupported");
    }
    return integerFormulaManager;
  }

  public RationalFormulaManagerView getRationalFormulaManager()
      throws UnsupportedOperationException {
    if (rationalFormulaManager == null) {
      rationalFormulaManager =
          new RationalFormulaManagerView(wrappingHandler, manager.getRationalFormulaManager());
    }
    return rationalFormulaManager;
  }

  public BooleanFormulaManagerView getBooleanFormulaManager() {
    return booleanFormulaManager;
  }

  public BitvectorFormulaManagerView getBitvectorFormulaManager()
      throws UnsupportedOperationException {
    if (bitvectorFormulaManager == null) {
      throw new UnsupportedOperationException(
          "Bitvectors attempted to be used but configured to be unsupported");
    }
    return bitvectorFormulaManager;
  }

  public FloatingPointFormulaManagerView getFloatingPointFormulaManager()
      throws UnsupportedOperationException {
    if (floatingPointFormulaManager == null) {
      throw new UnsupportedOperationException(
          "Floats attempted to be used but configured to be unsupported");
    }
    return floatingPointFormulaManager;
  }

  public FunctionFormulaManagerView getFunctionFormulaManager() {
    return functionFormulaManager;
  }

  public QuantifiedFormulaManagerView getQuantifiedFormulaManager()
      throws UnsupportedOperationException {
    if (quantifiedFormulaManager == null) {
      quantifiedFormulaManager =
          new QuantifiedFormulaManagerView(
              wrappingHandler,
              manager.getQuantifiedFormulaManager(),
              booleanFormulaManager,
              getIntegerFormulaManager());
    }
    return quantifiedFormulaManager;
  }

  public ArrayFormulaManagerView getArrayFormulaManager() throws UnsupportedOperationException {
    if (arrayFormulaManager == null) {
      arrayFormulaManager =
          new ArrayFormulaManagerView(wrappingHandler, manager.getArrayFormulaManager());
    }
    return arrayFormulaManager;
  }

  public SLFormulaManagerView getSLFormulaManager() {
    if (slFormulaManager == null) {
      slFormulaManager = new SLFormulaManagerView(wrappingHandler, manager.getSLFormulaManager());
    }
    return slFormulaManager;
  }

  public <T extends Formula> FormulaType<T> getFormulaType(T pFormula) {
    return wrappingHandler.getFormulaType(pFormula);
  }

  public BitvectorType getFormulaType(BitvectorFormula pFormula) {
    return (BitvectorType) wrappingHandler.getFormulaType(pFormula);
  }

  public FloatingPointType getFormulaType(FloatingPointFormula pFormula) {
    return (FloatingPointType) wrappingHandler.getFormulaType(pFormula);
  }

  @SuppressWarnings("unchecked")
  public <I extends Formula, E extends Formula> ArrayFormulaType<I, E> getFormulaType(
      ArrayFormula<I, E> pFormula) {
    return (ArrayFormulaType<I, E>) wrappingHandler.getFormulaType(pFormula);
  }

  private <T extends Formula> FormulaType<T> getRawFormulaType(T pFormula) {
    return manager.getFormulaType(pFormula);
  }

  public <T extends Formula> BooleanFormula assignment(T left, T right) {
    FormulaType<?> lformulaType = getFormulaType(left);
    FormulaType<?> rformulaType = getFormulaType(right);
    if (!lformulaType.equals(rformulaType)) {
      throw new IllegalArgumentException(
          "Can't assign different types! (" + lformulaType + " and " + rformulaType + ")");
    }

    if (lformulaType.isFloatingPointType()) {
      return getFloatingPointFormulaManager()
          .assignment((FloatingPointFormula) left, (FloatingPointFormula) right);
    }
    return makeEqual(left, right);
  }

  public BooleanFormula parse(String pS) throws IllegalArgumentException {
    return manager.parse(pS);
  }

  // the character for separating name and index of a value
  private static final char INDEX_SEPARATOR = '@';
  private static final Splitter INDEX_SPLITTER = Splitter.on(INDEX_SEPARATOR);

  static String makeName(String name, int idx) {
    checkArgument(
        name.indexOf(INDEX_SEPARATOR) == -1,
        "Instantiating already instantiated variable %s with index %s",
        name,
        idx);
    checkArgument(idx >= 0, "Invalid index %s for variable %s", idx, name);
    return name + INDEX_SEPARATOR + idx;
  }

  static String makeNameNoIndex(String name) {
    checkArgument(
        name.indexOf(INDEX_SEPARATOR) == -1, "Variable with forbidden symbol '@': %s", name);
    return name + INDEX_SEPARATOR;
  }

  /**
   * Instantiate the variables in pF with the SSA indices in pSsa. Already instantiated variables
   * are not allowed in the formula.
   */
  public <F extends Formula> F instantiate(F pF, final SSAMap pSsa) {
    return wrap(
        getFormulaType(pF),
        myFreeVariableNodeTransformer(
            unwrap(pF),
            new HashMap<>(),
            pFullSymbolName -> {
              int sepPos = pFullSymbolName.indexOf(INDEX_SEPARATOR);
              if (sepPos == pFullSymbolName.length() - 1) {
                // variable should never be instantiated
                // TODO check no index in SSAMap
                return pFullSymbolName;
              } else if (sepPos != -1) {
                throw new IllegalArgumentException(
                    "already instantiated variable " + pFullSymbolName + " in formula");
              }
              final int reInstantiateWithIndex = pSsa.getIndex(pFullSymbolName);

              if (reInstantiateWithIndex > 0) {
                return makeName(pFullSymbolName, reInstantiateWithIndex);
              } else {
                // TODO throw exception
                return pFullSymbolName;
              }
            }));
  }

  // various caches for speeding up expensive tasks
  //

  // cache for uninstantiating terms (see uninstantiate() below)
  private final Map<Formula, Formula> uninstantiateCache = new HashMap<>();

  /**
   * Only use inside this package and for solver-specific classes when creating a {@link Model}. Do
   * not use in client code!
   *
   * @throws IllegalArgumentException thrown if the given name is invalid
   */
  public static Pair<String, OptionalInt> parseName(final String name) {
    checkArgument(!name.isEmpty(), "Invalid empty name");
    List<String> parts = INDEX_SPLITTER.splitToList(name);
    if (parts.size() == 2) {
      if (parts.get(1).isEmpty()) {
        // Variable name ending in @ marks variables that should not be instantiated
        return Pair.of(parts.get(0), OptionalInt.empty());
      }
      return Pair.of(parts.get(0), OptionalInt.of(Integer.parseInt(parts.get(1))));
    } else if (parts.size() == 1) {
      // TODO throw exception after forbidding such variable names
      return Pair.of(parts.get(0), OptionalInt.empty());
    } else {
      throw new IllegalArgumentException("Not an instantiated variable nor constant: " + name);
    }
  }

  /**
   * Add SSA indices to a single variable name. Typically it is not necessary and not recommended to
   * use this method, prefer more high-level methods like {@link #instantiate(Formula, SSAMap)}.
   */
  public static String instantiateVariableName(String pVar, SSAMap pSsa) {
    return makeName(pVar, pSsa.getIndex(pVar));
  }

  /**
   * Uninstantiate a given formula. (remove the SSA indices from its free variables and UFs)
   *
   * @param f Input formula
   * @return Uninstantiated formula
   */
  public <F extends Formula> F uninstantiate(F f) {
    return wrap(
        getFormulaType(f),
        myFreeVariableNodeTransformer(
            unwrap(f),
            uninstantiateCache,
            name ->
                name.charAt(name.length() - 1) == INDEX_SEPARATOR
                    ? name
                    : parseName(name).getFirst()));
  }

  /**
   * Apply an arbitrary renaming to all free variables and UFs in a formula.
   *
   * @param pFormula The formula in which the renaming should occur.
   * @param pRenameFunction The renaming function (may not return null).
   * @return A formula of the same type and structure as the input.
   */
  public <F extends Formula> F renameFreeVariablesAndUFs(
      F pFormula, Function<String, String> pRenameFunction) {

    return wrap(
        getFormulaType(pFormula),
        myFreeVariableNodeTransformer(unwrap(pFormula), new HashMap<>(), pRenameFunction));
  }

  private <T extends Formula> T myFreeVariableNodeTransformer(
      final T pFormula,
      final Map<Formula, Formula> pCache,
      final Function<String, String> pRenameFunction) {

    Preconditions.checkNotNull(pCache);
    Preconditions.checkNotNull(pFormula);
    Preconditions.checkNotNull(pRenameFunction);

    final Deque<Formula> toProcess = new ArrayDeque<>();

    // Add the formula to the work queue
    toProcess.push(pFormula);

    FormulaVisitor<Void> process =
        new FormulaVisitor<>() {
          // This visitor works with unwrapped formulas.
          // After calls to other methods that might return wrapped formulas we need to unwrap them.

          @Override
          public Void visitFreeVariable(Formula f, String name) {
            String newName = pRenameFunction.apply(name);
            Formula renamed = unwrap(makeVariable(getFormulaType(f), newName));
            pCache.put(f, renamed);
            return null;
          }

          @Override
          public Void visitBoundVariable(Formula f, int deBruijnIdx) {

            // Bound variables have to stay as-is.
            pCache.put(f, f);
            return null;
          }

          @Override
          public Void visitConstant(Formula f, Object value) {
            pCache.put(f, f);
            return null;
          }

          @Override
          public Void visitFunction(Formula f, List<Formula> args, FunctionDeclaration<?> decl) {

            boolean allArgumentsTransformed = true;

            // Construct a new argument list for the function application.
            List<Formula> newArgs = new ArrayList<>(args.size());

            for (Formula c : args) {
              Formula newC = pCache.get(c);

              if (newC != null) {
                newArgs.add(newC);
              } else {
                toProcess.push(c);
                allArgumentsTransformed = false;
              }
            }

            // The Flag childrenDone indicates whether all arguments
            // of the function were already processed.
            if (allArgumentsTransformed) {

              // Create an processed version of the
              // function application.
              toProcess.pop();
              Formula out;
              if (decl.getKind() == FunctionDeclarationKind.UF) {
                FunctionDeclaration<Formula> uf =
                    getFunctionFormulaManager()
                        .declareUF(
                            pRenameFunction.apply(decl.getName()),
                            getFormulaType(f),
                            decl.getArgumentTypes());
                out = unwrap(getFunctionFormulaManager().callUF(uf, newArgs));

              } else {
                out = manager.makeApplication(decl, newArgs);
              }
              pCache.put(f, out);
            }
            return null;
          }

          @Override
          public Void visitQuantifier(
              BooleanFormula f, Quantifier quantifier, List<Formula> args, BooleanFormula body) {
            BooleanFormula transformedBody = (BooleanFormula) pCache.get(body);

            if (transformedBody != null) {
              BooleanFormula newTt =
                  getQuantifiedFormulaManager().mkQuantifier(quantifier, args, transformedBody);
              pCache.put(f, newTt);

            } else {
              toProcess.push(body);
            }
            return null;
          }
        };

    // Process the work queue
    while (!toProcess.isEmpty()) {
      Formula tt = toProcess.peek();

      if (pCache.containsKey(tt)) {
        toProcess.pop();
        continue;
      }

      //noinspection ResultOfMethodCallIgnored
      visit(tt, process);
    }

    @SuppressWarnings("unchecked")
    T result = (T) pCache.get(pFormula);
    assert result != null;
    assert getRawFormulaType(pFormula).equals(getRawFormulaType(result));
    return result;
  }

  /** Extract all atoms of a given boolean formula. */
  public ImmutableSet<BooleanFormula> extractAtoms(
      BooleanFormula pFormula, final boolean splitArithEqualities) {
    final ImmutableSet.Builder<BooleanFormula> result = ImmutableSet.builder();
    booleanFormulaManager.visitRecursively(
        pFormula,
        new DefaultBooleanFormulaVisitor<TraversalProcess>() {
          @Override
          protected TraversalProcess visitDefault() {
            return TraversalProcess.CONTINUE;
          }

          @Override
          public TraversalProcess visitQuantifier(
              Quantifier quantifier,
              BooleanFormula quantifiedAST,
              List<Formula> boundVars,
              BooleanFormula body) {
            result.add(quantifiedAST);
            return TraversalProcess.SKIP;
          }

          @Override
          public TraversalProcess visitAtom(
              BooleanFormula atom, FunctionDeclaration<BooleanFormula> decl) {
            if (splitArithEqualities && myIsPurelyArithmetic(atom)) {
              result.addAll(extractAtoms(splitNumeralEqualityIfPossible(atom).get(0), false));
            }
            result.add(atom);
            return TraversalProcess.CONTINUE;
          }
        });
    return result.build();
  }

  /**
   * Return the negated part of a formula, if the top-level operator is a negation. I.e., for {@code
   * not f} return {@code f}.
   *
   * <p>For removing the outer-most negation of a formula if it is present or otherwise keeping the
   * original formula, use {@code f = stripNegation(f).or(f);}.
   *
   * @param f The formula, possibly negated.
   * @return An optional formula.
   */
  public Optional<BooleanFormula> stripNegation(BooleanFormula f) {
    return booleanFormulaManager.visit(
        f,
        new DefaultBooleanFormulaVisitor<Optional<BooleanFormula>>() {
          @Override
          protected Optional<BooleanFormula> visitDefault() {
            return Optional.empty();
          }

          @Override
          public Optional<BooleanFormula> visitNot(BooleanFormula negated) {
            return Optional.of(negated);
          }
        });
  }

  /**
   * For an equality {@code x = y} where {@code x} and {@code y} are not boolean, return a list
   * {@code x<=y, x>=y}.
   *
   * <p>Otherwise, return the list consisting of the input formula. Note: 1) Returned list always
   * has one or two elements. 2) Conjunction over the returned list is equivalent to the input
   * formula.
   */
  public List<BooleanFormula> splitNumeralEqualityIfPossible(BooleanFormula formula) {
    return visit(
        formula,
        new DefaultFormulaVisitor<List<BooleanFormula>>() {
          @Override
          protected List<BooleanFormula> visitDefault(Formula f) {
            return ImmutableList.of((BooleanFormula) f);
          }

          @Override
          public List<BooleanFormula> visitFunction(
              Formula f, List<Formula> args, FunctionDeclaration<?> functionDeclaration) {
            if ((functionDeclaration.getKind() == FunctionDeclarationKind.EQ
                    || functionDeclaration.getKind() == FunctionDeclarationKind.EQ_ZERO)
                && !functionDeclaration.getArgumentTypes().get(0).isBooleanType()
                && !functionDeclaration.getArgumentTypes().get(0).isArrayType()) {

              Formula arg1 = args.get(0);
              Formula arg2;

              if (functionDeclaration.getKind() == FunctionDeclarationKind.EQ_ZERO) {
                arg2 = makeNumber(getFormulaType(arg1), 0);
              } else {
                arg2 = args.get(1);
              }
              return ImmutableList.of(
                  makeLessOrEqual(arg1, arg2, true), makeGreaterOrEqual(arg1, arg2, true));
            } else {
              return ImmutableList.of((BooleanFormula) f);
            }
          }
        });
  }

  /** Cache for splitting arithmetic equalities in extractAtoms. */
  private final Map<Formula, Boolean> arithCache = new HashMap<>();

  /** Returns true if the given term is a pure arithmetic term. */
  private boolean myIsPurelyArithmetic(Formula f) {
    Boolean result = arithCache.get(f);
    if (result != null) {
      return result;
    }

    final AtomicBoolean isPurelyAtomic = new AtomicBoolean(true);
    visitRecursively(
        f,
        new DefaultFormulaVisitor<TraversalProcess>() {
          @Override
          protected TraversalProcess visitDefault(Formula pF) {
            return TraversalProcess.CONTINUE;
          }

          @Override
          public TraversalProcess visitFunction(
              Formula pF, List<Formula> args, FunctionDeclaration<?> decl) {
            if (decl.getKind() == FunctionDeclarationKind.UF) {
              isPurelyAtomic.set(false);
              return TraversalProcess.ABORT;
            }
            return TraversalProcess.CONTINUE;
          }
        });
    result = isPurelyAtomic.get();
    arithCache.put(f, result);
    return result;
  }

  /**
   * Extract the Variables in a given Formula
   *
   * <p>Has the advantage compared to extractVariableNames, that the Type information still is
   * intact in the formula.
   *
   * @param pFormula The formula to extract the variables from
   * @return A Map of the variable names to their corresponding formulas.
   */
  public Map<String, Formula> extractVariables(Formula pFormula) {
    return manager.extractVariables(pFormula);
  }

  /**
   * Extract the names of all free variables in a formula.
   *
   * @param f The input formula
   * @return Set of variable names (might be instantiated)
   */
  public Set<String> extractVariableNames(Formula f) {
    return manager.extractVariables(unwrap(f)).keySet();
  }

  /**
   * Extract the names of all free variables + UFs in a formula.
   *
   * @param f The input formula
   * @return Set of variable names (might be instantiated)
   */
  public Set<String> extractFunctionNames(Formula f) {
    return manager.extractVariablesAndUFs(unwrap(f)).keySet();
  }

  public Appender dumpFormula(BooleanFormula pT) {
    return manager.dumpFormula(pT);
  }

  public boolean isPurelyConjunctive(BooleanFormula t) {
    final BooleanFormulaVisitor<Boolean> isAtomicVisitor =
        new DefaultBooleanFormulaVisitor<>() {
          @Override
          protected Boolean visitDefault() {
            return false;
          }

          @Override
          public Boolean visitAtom(BooleanFormula atom, FunctionDeclaration<BooleanFormula> decl) {
            return !containsIfThenElse(atom);
          }
        };

    return booleanFormulaManager.visit(
        t,
        new DefaultBooleanFormulaVisitor<Boolean>() {

          @Override
          public Boolean visitDefault() {
            return false;
          }

          @Override
          public Boolean visitConstant(boolean constantValue) {
            return true;
          }

          @Override
          public Boolean visitAtom(BooleanFormula atom, FunctionDeclaration<BooleanFormula> decl) {
            return !containsIfThenElse(atom);
          }

          @Override
          public Boolean visitNot(BooleanFormula operand) {
            // Return false unless the operand is atomic.
            return booleanFormulaManager.visit(operand, isAtomicVisitor);
          }

          @Override
          public Boolean visitAnd(List<BooleanFormula> operands) {
            for (BooleanFormula operand : operands) {
              if (!booleanFormulaManager.visit(operand, this)) {
                return false;
              }
            }
            return true;
          }
        });
  }

  private boolean containsIfThenElse(Formula f) {
    final AtomicBoolean containsITE = new AtomicBoolean(false);
    visitRecursively(
        f,
        new DefaultFormulaVisitor<TraversalProcess>() {
          @Override
          protected TraversalProcess visitDefault(Formula pF) {
            return TraversalProcess.CONTINUE;
          }

          @Override
          public TraversalProcess visitFunction(
              Formula pF, List<Formula> args, FunctionDeclaration<?> decl) {
            if (decl.getKind() == FunctionDeclarationKind.ITE) {
              containsITE.set(true);
              return TraversalProcess.ABORT;
            }
            return TraversalProcess.CONTINUE;
          }
        });
    return containsITE.get();
  }

  static final String BitwiseAndUfName = "_&_";
  static final String BitwiseOrUfName = "_!!_"; // SMTInterpol does not allow "|" to be used
  static final String BitwiseXorUfName = "_^_";
  static final String BitwiseNotUfName = "_~_";

  // returns a formula with some "static learning" about some bitwise
  // operations, so that they are (a bit) "less uninterpreted"
  // Currently it add's the following formulas for each number literal n that
  // appears in the formula: "(n & 0 == 0) and (0 & n == 0)"
  // But only if an bitwise "and" occurs in the formula.
  private BooleanFormula myGetBitwiseAxioms(BooleanFormula f) {
    final Set<Formula> allLiterals = new HashSet<>();
    final AtomicBoolean andFound = new AtomicBoolean(false);

    visitRecursively(
        f,
        new DefaultFormulaVisitor<TraversalProcess>() {
          @Override
          protected TraversalProcess visitDefault(Formula pF) {
            return TraversalProcess.CONTINUE;
          }

          @Override
          public TraversalProcess visitConstant(Formula pF, Object value) {
            if (value instanceof Number) {
              allLiterals.add(pF);
            }
            return TraversalProcess.CONTINUE;
          }

          @Override
          public TraversalProcess visitFunction(
              Formula pF, List<Formula> args, FunctionDeclaration<?> decl) {
            if (decl.getKind() == FunctionDeclarationKind.UF
                && decl.getName().equals(BitwiseAndUfName)) {
              andFound.set(true);
            }
            return TraversalProcess.CONTINUE;
          }
        });

    List<BooleanFormula> result = new ArrayList<>();
    if (andFound.get()) {
      final BitvectorFormulaManagerView bvmgr = getBitvectorFormulaManager();
      // Note: We can assume that we have no real bitvectors here, so size should be not important
      // If it ever should be we can just add an method to the unsafe-manager to read the size.
      BitvectorFormula z = bvmgr.makeBitvector(1, 0);
      FormulaType<BitvectorFormula> type = FormulaType.getBitvectorTypeWithSize(1);
      // Term z = env.numeral("0");
      for (Formula nn : allLiterals) {
        BitvectorFormula n = bvmgr.wrap(type, nn);
        BitvectorFormula u1 = bvmgr.and(z, n);
        BitvectorFormula u2 = bvmgr.and(n, z);
        // Term u1 = env.term(bitwiseAndUfDecl, n, z);
        // Term u2 = env.term(bitwiseAndUfDecl, z, n);
        // Term e1 = env.term("=", u1, z);
        // Term e2 = env.term("=", u2, z);
        // result = env.term("and", result, e1, e2);
        result.add(bvmgr.equal(u1, z));
        result.add(bvmgr.equal(u2, z));
      }
    }
    return booleanFormulaManager.and(result);
  }

  // returns a formula with some "static learning" about some bitwise
  public BooleanFormula getBitwiseAxioms(BooleanFormula f) {
    return myGetBitwiseAxioms(f);
  }

  public boolean useBitwiseAxioms() {
    return useBitwiseAxioms;
  }

  public BooleanFormula createPredicateVariable(String pName) {
    return booleanFormulaManager.makeVariable(pName);
  }

  public <T extends Formula> T simplify(T input) throws InterruptedException {
    return manager.simplify(input);
  }

  public BooleanFormula substitute(
      BooleanFormula f, Map<? extends Formula, ? extends Formula> replacements) {
    Map<Formula, Formula> m = new HashMap<>();
    for (Entry<? extends Formula, ? extends Formula> e : replacements.entrySet()) {
      m.put(unwrap(e.getKey()), unwrap(e.getValue()));
    }
    return manager.substitute(f, m);
  }

  /** Return true iff the variable name is non-final with respect to the given SSA map. */
  public boolean isIntermediate(String varName, SSAMap ssa) {
    Pair<String, OptionalInt> p = parseName(varName);
    String name = p.getFirst();
    OptionalInt idx = p.getSecond();
    if (!idx.isPresent()) {
      if (ssa.containsVariable(varName)) {
        return true;
      }
    } else {
      if (idx.orElseThrow() != ssa.getIndex(name)) {
        return true;
      }
    }
    return false;
  }

  public Set<String> getDeadFunctionNames(BooleanFormula pFormula, SSAMap pSsa) {
    return getFunctionNames(pFormula, varName -> isIntermediate(varName, pSsa), true);
  }

  private Set<String> getFunctionNames(
      BooleanFormula pFormula, Predicate<String> pIsDesired, boolean extractUFs) {
    return myGetDesiredVariables(pFormula, pIsDesired, extractUFs).keySet();
  }

  /**
   * Do not make this method public, because the returned formulas have incorrect types (they are
   * not appropriately wrapped).
   */
  private Map<String, Formula> myGetDesiredVariables(
      BooleanFormula pFormula, Predicate<String> pIsDesired, boolean extractUF) {
    Map<String, Formula> result = new HashMap<>();

    Map<String, Formula> vars;
    if (extractUF) {
      vars = manager.extractVariablesAndUFs(pFormula);
    } else {
      vars = manager.extractVariables(pFormula);
    }

    for (Entry<String, Formula> entry : vars.entrySet()) {

      String name = entry.getKey();
      Formula varFormula = entry.getValue();
      if (pIsDesired.apply(name)) {
        result.put(name, varFormula);
      }
    }

    return result;
  }

  /**
   * Eliminate all propositions about 'dead' variables in a given formula.
   *
   * <p>Quantifier elimination is used! This has to be supported by the solver! (solver-independent
   * approaches would be possible)
   *
   * <p>A variable is considered 'dead' if its SSA index is different from the index in the SSA map.
   */
  public BooleanFormula eliminateDeadVariables(final BooleanFormula pF, final SSAMap pSsa)
      throws SolverException, InterruptedException {

    Preconditions.checkNotNull(pSsa);
    return eliminateVariables(pF, varName -> isIntermediate(varName, pSsa));
  }

  /**
   * Eliminate all propositions about variables described by a given predicate in a given formula.
   *
   * <p>Quantifier elimination is used! This has to be supported by the solver! (solver-independent
   * approaches would be possible)
   */
  public BooleanFormula eliminateVariables(
      final BooleanFormula pF, final Predicate<String> pToEliminate)
      throws SolverException, InterruptedException {

    Preconditions.checkNotNull(pF);
    Preconditions.checkNotNull(pToEliminate);

    Map<String, Formula> irrelevantVariables = myGetDesiredVariables(pF, pToEliminate, false);

    BooleanFormula eliminationResult = pF;

    if (!irrelevantVariables.isEmpty()) {
      QuantifiedFormulaManagerView qfmgr = getQuantifiedFormulaManager();
      BooleanFormula quantifiedFormula =
          qfmgr.exists(ImmutableList.copyOf(irrelevantVariables.values()), pF);

      eliminationResult = qfmgr.eliminateQuantifiers(quantifiedFormula);
    }

    eliminationResult = simplify(eliminationResult); // TODO: Benchmark the effect!
    return eliminationResult;
  }

  /** Quantify all intermediate variables in the formula. */
  public BooleanFormula quantifyDeadVariables(BooleanFormula pF, SSAMap pSSAMap) {
    Map<String, Formula> irrelevantVariables =
        myGetDesiredVariables(pF, varName -> isIntermediate(varName, pSSAMap), false);
    if (irrelevantVariables.isEmpty()) {
      return pF;
    }
    return getQuantifiedFormulaManager()
        .exists(ImmutableList.copyOf(irrelevantVariables.values()), pF);
  }

  /**
   * Split boolean or non-boolean if-then-else formula into three parts: if, then, else. Return an
   * empty optional for input which does not have if-then-else as an input element.
   */
  public <T extends Formula> Optional<Triple<BooleanFormula, T, T>> splitIfThenElse(final T pF) {
    return visit(
        pF,
        new DefaultFormulaVisitor<Optional<Triple<BooleanFormula, T, T>>>() {

          @Override
          protected Optional<Triple<BooleanFormula, T, T>> visitDefault(Formula f) {
            return Optional.empty();
          }

          @Override
          public Optional<Triple<BooleanFormula, T, T>> visitFunction(
              Formula f, List<Formula> args, FunctionDeclaration<?> functionDeclaration) {
            if (functionDeclaration.getKind() == FunctionDeclarationKind.ITE) {
              assert args.size() == 3;
              BooleanFormula cond = (BooleanFormula) args.get(0);
              Formula thenBranch = args.get(1);
              Formula elseBranch = args.get(2);
              FormulaType<T> targetType = getFormulaType(pF);
              return Optional.of(
                  Triple.of(cond, wrap(targetType, thenBranch), wrap(targetType, elseBranch)));
            }
            return Optional.empty();
          }
        });
  }

  /** See {@link FormulaManager#applyTactic(BooleanFormula, Tactic)} for documentation. */
  public BooleanFormula applyTactic(BooleanFormula input, Tactic tactic)
      throws InterruptedException {
    return manager.applyTactic(input, tactic);
  }

  /** Visit the formula with a given visitor. */
  @CanIgnoreReturnValue
  public <R> R visit(Formula f, FormulaVisitor<R> rFormulaVisitor) {
    return manager.visit(unwrap(f), rFormulaVisitor);
  }

  /**
   * Visit the formula recursively with a given {@link FormulaVisitor}.
   *
   * <p>This method guarantees that the traversal is done iteratively, without using Java recursion,
   * and thus is not prone to StackOverflowErrors.
   *
   * <p>Furthermore, this method also guarantees that every equal part of the formula is visited
   * only once. Thus it can be used to traverse DAG-like formulas efficiently.
   */
  public void visitRecursively(Formula f, FormulaVisitor<TraversalProcess> rFormulaVisitor) {
    manager.visitRecursively(unwrap(f), rFormulaVisitor);
  }

  /**
   * Visit the formula recursively with a given {@link FormulaVisitor}.
   *
   * <p>This method guarantees that the traversal is done iteratively, without using Java recursion,
   * and thus is not prone to StackOverflowErrors.
   *
   * <p>Furthermore, this method also guarantees that every equal part of the formula is visited
   * only once. Thus it can be used to traverse DAG-like formulas efficiently.
   *
   * @param pFormulaVisitor Transformation described by the user.
   */
  public <T extends Formula> T transformRecursively(
      T f, FormulaTransformationVisitor pFormulaVisitor) {
    @SuppressWarnings("unchecked")
    T out =
        (T)
            manager.transformRecursively(
                unwrap(f), new UnwrappingFormulaTransformationVisitor(pFormulaVisitor));
    return out;
  }

  /**
   * Replace all literals in {@code input} which do not satisfy {@code toKeep} with {@code true}.
   */
  public BooleanFormula filterLiterals(BooleanFormula input, final Predicate<BooleanFormula> toKeep)
      throws InterruptedException {
    // No nested NOT's are possible in NNF.
    BooleanFormula nnf = applyTactic(input, Tactic.NNF);

    BooleanFormula nnfNotTransformed =
        booleanFormulaManager.transformRecursively(
            nnf,
            new BooleanFormulaTransformationVisitor(this) {
              @Override
              public BooleanFormula visitNot(BooleanFormula pOperand) {
                if (!toKeep.apply(pOperand)) {
                  return booleanFormulaManager.makeTrue();
                }
                return super.visitNot(pOperand);
              }
            });
    return booleanFormulaManager.transformRecursively(
        nnfNotTransformed,
        new BooleanFormulaTransformationVisitor(this) {
          @Override
          public BooleanFormula visitAtom(
              BooleanFormula pOperand, FunctionDeclaration<BooleanFormula> decl) {
            if (!toKeep.apply(pOperand)) {
              return booleanFormulaManager.makeTrue();
            }
            return super.visitAtom(pOperand, decl);
          }
        });
  }

  public BooleanFormula translateFrom(BooleanFormula other, FormulaManagerView otherManager) {
    return manager.translateFrom(other, otherManager.manager);
  }

  /** View wrapper for {@link #transformRecursively}. */
  public static class FormulaTransformationVisitor
      extends org.sosy_lab.java_smt.api.visitors.FormulaTransformationVisitor {

    protected FormulaTransformationVisitor(FormulaManagerView fmgr) {
      super(fmgr.manager);
    }
  }

  private class UnwrappingFormulaTransformationVisitor
      extends org.sosy_lab.java_smt.api.visitors.FormulaTransformationVisitor {

    private final FormulaTransformationVisitor delegate;

    protected UnwrappingFormulaTransformationVisitor(FormulaTransformationVisitor pDelegate) {
      super(manager);
      delegate = Objects.requireNonNull(pDelegate);
    }

    @Override
    public Formula visitBoundVariable(Formula pF, int pDeBruijnIdx) {
      return unwrap(delegate.visitBoundVariable(pF, pDeBruijnIdx));
    }

    @Override
    public Formula visitFreeVariable(Formula pF, String pName) {
      return unwrap(delegate.visitFreeVariable(pF, pName));
    }

    @Override
    public Formula visitFunction(
        Formula pF, List<Formula> pNewArgs, FunctionDeclaration<?> pFunctionDeclaration) {
      return unwrap(delegate.visitFunction(pF, pNewArgs, pFunctionDeclaration));
    }

    @Override
    public Formula visitConstant(Formula pF, Object pValue) {
      return unwrap(delegate.visitConstant(pF, pValue));
    }

    @Override
    public BooleanFormula visitQuantifier(
        BooleanFormula pF,
        Quantifier pQuantifier,
        List<Formula> pBoundVariables,
        BooleanFormula pTransformedBody) {
      return delegate.visitQuantifier(pF, pQuantifier, pBoundVariables, pTransformedBody);
    }
  }

  private static final String DUMMY_VAR = "__dummy_variable_dumping_formulas__";

  /**
   * Dump an arbitrary formula into a string, in contrast to {@link #dumpFormula(BooleanFormula)}
   * this works with non-boolean formulas. No guarantees are made about the output format, except
   * that it can be parsed by {@link #parseArbitraryFormula(String)}.
   */
  public String dumpArbitraryFormula(Formula f) {
    Formula dummyVar = makeVariable(getFormulaType(f), DUMMY_VAR);
    return dumpFormula(makeEqual(dummyVar, f)).toString();
  }

  /** Parse a string with a formula that was created by {@link #dumpArbitraryFormula(Formula)}. */
  public Formula parseArbitraryFormula(String s) {
    BooleanFormula f = parse(s);
    return visit(
        f,
        new DefaultFormulaVisitor<Formula>() {

          @Override
          protected Formula visitDefault(Formula pF) {
            throw new AssertionError("Unexpected formula " + pF);
          }

          @Override
          public Formula visitFunction(
              Formula pF, List<Formula> pArgs, FunctionDeclaration<?> pDecl) {
            if (pDecl.getKind() != FunctionDeclarationKind.EQ && pArgs.size() != 2) {
              return visitDefault(pF);
            }
            Formula dummyVar = makeVariable(getFormulaType(pArgs.get(0)), DUMMY_VAR);
            if (pArgs.get(0).equals(dummyVar)) {
              return pArgs.get(1);
            } else if (pArgs.get(1).equals(dummyVar)) {
              return pArgs.get(0);
            }
            return visitDefault(pF);
          }
        });
  }

  /**
   * This method simplifies a given boolean formula by putting out of brackets mutual operands of
   * all conjunctions in a single disjunction. The simplified formula should speed up the SAT check
   * of BMC. Example: <code>(or (and A B C) (and A B D E))</code> transformed to <code>
   * ((and A B) and (or (and C) (and D E)))</code>.
   *
   * <p>It depends on the solver whether this speeds up the SMT check, because solvers can
   * internally implement similar routines. Example execution of BMC for concurrent tasks: The
   * performance of SMTInterpol with Integer logic is improved by a factor of up to 5 in several
   * cases. MathSAT with BV logic becomes a little bit slower on the same tasks when formulas are
   * simplified.
   */
  public BooleanFormula simplifyBooleanFormula(final BooleanFormula formula) {
    return simplifyBooleanFormula0(formula, new HashMap<>(), getBooleanFormulaManager());
  }

  private BooleanFormula simplifyBooleanFormula0(
      final BooleanFormula formula,
      final Map<BooleanFormula, BooleanFormula> cache,
      final BooleanFormulaManager bmgr) {
    final Set<BooleanFormula> disjunctionSet = bmgr.toDisjunctionArgs(formula, true);

    if (disjunctionSet.size() <= 1) {
      // a single atom can not be simplified
      return formula;
    }

    final List<Set<BooleanFormula>> listOfOperands = new ArrayList<>();
    for (final BooleanFormula subformula : disjunctionSet) {
      // split each subformula to a set containing all subformulas of conjunction
      Set<BooleanFormula> conjunctionSet = bmgr.toConjunctionArgs(subformula, true);
      // this set contains all operands of current subformula
      Set<BooleanFormula> operandsOfSubformula = new LinkedHashSet<>();
      listOfOperands.add(operandsOfSubformula);
      // iterate through all subformulas of a current conjunction
      for (BooleanFormula formulaInConjunctionSet : conjunctionSet) {
        // formulaInConjunctionSet contains disjunction that should be also transformed
        BooleanFormula transformedFormula = cache.get(formulaInConjunctionSet);
        if (transformedFormula == null) {
          transformedFormula = simplifyBooleanFormula0(formulaInConjunctionSet, cache, bmgr);
          cache.put(formulaInConjunctionSet, transformedFormula);
        }
        operandsOfSubformula.addAll(bmgr.toConjunctionArgs(transformedFormula, true));
      }
    }

    // set of mutual operands that are contained in all subformulas
    final Set<BooleanFormula> mutualOperandsSet = new LinkedHashSet<>(listOfOperands.get(0));
    for (Set<BooleanFormula> operands : listOfOperands) {
      mutualOperandsSet.retainAll(operands);
    }

    // remove all mutual operands from each subformula
    final List<BooleanFormula> transformedSubformulas = new ArrayList<>();
    for (Set<BooleanFormula> operands : listOfOperands) {
      operands.removeAll(mutualOperandsSet);
      transformedSubformulas.add(bmgr.and(operands));
    }

    // simplified BooleanFormula consists of
    // ((conjunction of mutual operands) AND (disjunction of transformed subformulas))
    return bmgr.and(bmgr.and(mutualOperandsSet), bmgr.or(transformedSubformulas));
  }

  /**
   * This method computes the number of boolean operations in the formula, including recursive ones.
   *
   * <p>The result serves as an overview and can be used as a vague measurement for the difficulty
   * of a formula. Please note that for a better measurement, you should also consider the used SMT
   * theory, complexity of arithmetic operations, number of symbols and constants, etc.
   */
  public BigInteger countBooleanOperations(BooleanFormula f) {
    final Map<Formula, BigInteger> cache = new HashMap<>();
    final Deque<Formula> waitlist = new ArrayDeque<>();

    final FormulaVisitor<@Nullable BigInteger> countingVisitor =
        new DefaultFormulaVisitor<>() {
          @Override
          protected BigInteger visitDefault(Formula pF) {
            return BigInteger.ZERO;
          }

          @Override
          public @Nullable BigInteger visitFunction(
              Formula pF, List<Formula> args, FunctionDeclaration<?> decl) {
            assert !args.isEmpty();
            switch (decl.getKind()) {
              case AND:
              case OR:
              case NOT:
                BigInteger count = BigInteger.valueOf(args.size());
                for (Formula arg : args) {
                  final BigInteger subCount = cache.get(arg);
                  if (subCount == null) {
                    // pF will be visited again later, non-recursive implementation of DFS
                    waitlist.push(pF);
                    waitlist.push(arg);
                    return null;
                  } else {
                    count = count.add(subCount);
                  }
                }
                return count;
              default:
                return visitDefault(pF);
            }
          }
        };

    // non-recursive implementation of DFS
    waitlist.push(f);
    while (!waitlist.isEmpty()) {
      Formula formula = waitlist.pop();
      @Nullable BigInteger count = visit(formula, countingVisitor);
      if (count != null) {
        cache.put(formula, count);
      }
    }
    return cache.get(f);
  }
}
