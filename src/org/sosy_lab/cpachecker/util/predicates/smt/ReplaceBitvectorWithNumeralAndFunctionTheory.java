// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.smt;

import static org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView.BitwiseAndUfName;
import static org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView.BitwiseNotUfName;
import static org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView.BitwiseOrUfName;
import static org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView.BitwiseXorUfName;
import static org.sosy_lab.java_smt.api.FormulaType.getBitvectorTypeWithSize;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.errorprone.annotations.DoNotCall;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BitvectorFormulaManager;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.FormulaType.BitvectorType;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.NumeralFormula;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.java_smt.api.NumeralFormulaManager;
import org.sosy_lab.java_smt.api.UFManager;

@Options(prefix = "cpa.predicate")
class ReplaceBitvectorWithNumeralAndFunctionTheory<T extends NumeralFormula> extends BaseManagerView
    implements BitvectorFormulaManager {

  private final BooleanFormulaManager booleanFormulaManager;
  private final NumeralFormulaManager<? super T, T> numericFormulaManager;
  private final UFManager functionManager;
  private final FunctionDeclaration<T> bitwiseAndUfDecl;
  private final FunctionDeclaration<T> bitwiseOrUfDecl;
  private final FunctionDeclaration<T> bitwiseXorUfDecl;
  private final FunctionDeclaration<T> bitwiseNotUfDecl;
  private final FunctionDeclaration<T> leftShiftUfDecl;
  private final FunctionDeclaration<T> rightShiftUfDecl;
  private final FunctionDeclaration<T> moduloUfDecl;
  private final FormulaType<T> formulaType;

  @Option(
      secure = true,
      description =
          "Ignore Extract and Extend operations instead of encoding them with a UF when Bitvector"
              + " theory is replaced with Integer or Rational. This is unsound but sometimes more"
              + " practical in order to not make casts return nondeterministic values.")
  private boolean ignoreExtractExtend = true;

  ReplaceBitvectorWithNumeralAndFunctionTheory(
      FormulaWrappingHandler pWrappingHandler,
      BooleanFormulaManager pBooleanFormulaManager,
      NumeralFormulaManager<? super T, T> rawNumericFormulaManager,
      UFManager rawFunctionManager,
      Configuration pConfig)
      throws InvalidConfigurationException {
    super(pWrappingHandler);
    pConfig.inject(this);
    booleanFormulaManager = pBooleanFormulaManager;
    numericFormulaManager = rawNumericFormulaManager;
    functionManager = rawFunctionManager;

    formulaType = numericFormulaManager.getFormulaType();
    bitwiseAndUfDecl = createBinaryFunction(BitwiseAndUfName);
    bitwiseOrUfDecl = createBinaryFunction(BitwiseOrUfName);
    bitwiseXorUfDecl = createBinaryFunction(BitwiseXorUfName);
    bitwiseNotUfDecl = createUnaryFunction(BitwiseNotUfName);

    leftShiftUfDecl = createBinaryFunction("_<<_");
    rightShiftUfDecl = createBinaryFunction("_>>_");
    moduloUfDecl = createBinaryFunction("_%_");
  }

  @SuppressWarnings("unchecked")
  private T unwrap(BitvectorFormula pNumber) {
    return (T) super.unwrap(pNumber);
  }

  private FunctionDeclaration<T> createUnaryFunction(String name) {
    return functionManager.declareUF(name, formulaType, formulaType);
  }

  private FunctionDeclaration<T> createBinaryFunction(String name) {
    return functionManager.declareUF(name, formulaType, formulaType, formulaType);
  }

  private BitvectorFormula makeUf(
      FormulaType<BitvectorFormula> realreturn,
      FunctionDeclaration<T> decl,
      BitvectorFormula... t1) {
    List<Formula> args = unwrap(Arrays.<Formula>asList(t1));

    return wrap(realreturn, functionManager.callUF(decl, args));
  }

  private final Map<Pair<Integer, Integer>, FunctionDeclaration<T>> extractMethods =
      new HashMap<>();

  private FunctionDeclaration<T> getExtractDecl(int pMsb, int pLsb) {
    Pair<Integer, Integer> hasKey = Pair.of(pMsb, pLsb);
    FunctionDeclaration<T> value = extractMethods.get(hasKey);
    if (value == null) {
      value = createUnaryFunction("_extract(" + pMsb + "," + pLsb + ")_");
      extractMethods.put(hasKey, value);
    }
    return value;
  }

  private Map<Pair<Integer, Integer>, FunctionDeclaration<T>> concatMethods = new HashMap<>();

  private FunctionDeclaration<T> getConcatDecl(int firstSize, int secoundSize) {
    Pair<Integer, Integer> hasKey = Pair.of(firstSize, secoundSize);
    FunctionDeclaration<T> value = concatMethods.get(hasKey);
    if (value == null) {
      value = createBinaryFunction("_concat(" + firstSize + "," + secoundSize + ")_");
      concatMethods.put(hasKey, value);
    }
    return value;
  }

  private record UFDeclaration(String name, boolean signed, int id) {}

  private final Map<UFDeclaration, FunctionDeclaration<T>> UFDeclarations = new HashMap<>();

  private FunctionDeclaration<T> getUFDecl(String name, int id, boolean signed) {
    UFDeclaration key = new UFDeclaration(name, signed, id);
    FunctionDeclaration<T> value = UFDeclarations.get(key);
    if (value == null) {
      String UFname = String.format("_%s%s(%d)_", name, (signed ? "Signed" : "Unsigned"), id);
      value = createUnaryFunction(UFname);
      UFDeclarations.put(key, value);
    }
    return value;
  }

  @Override
  public BitvectorFormula makeBitvector(int pLength, long pI) {
    assert BigInteger.valueOf(pI).bitLength() <= pLength
        : String.format("numeral value %s is too big for bitvector of length %d.", pI, pLength);
    T number = numericFormulaManager.makeNumber(pI);
    return wrap(getBitvectorTypeWithSize(pLength), number);
  }

  @Override
  public BitvectorFormula makeBitvector(int pLength, BigInteger pI) {
    assert pI.bitLength() <= pLength
        : String.format("numeral value %s is too big for bitvector of length %d.", pI, pLength);
    T number = numericFormulaManager.makeNumber(pI);
    return wrap(getBitvectorTypeWithSize(pLength), number);
  }

  @Override
  public BitvectorFormula makeVariable(int pLength, String pVar) {
    return wrap(getBitvectorTypeWithSize(pLength), numericFormulaManager.makeVariable(pVar));
  }

  @Override
  public BitvectorFormula makeVariable(BitvectorType type, String pVar) {
    return wrap(type, numericFormulaManager.makeVariable(pVar));
  }

  @Override
  public int getLength(BitvectorFormula pNumber) {
    return ((BitvectorType) getFormulaType(pNumber)).getSize();
  }

  @Override
  public BitvectorFormula negate(BitvectorFormula pNumber) {
    return wrap(getFormulaType(pNumber), numericFormulaManager.negate(unwrap(pNumber)));
  }

  @Override
  public BitvectorFormula add(BitvectorFormula pNumber1, BitvectorFormula pNumber2) {
    assert getLength(pNumber1) == getLength(pNumber2) : "Expect operators to have the same size";
    return wrap(
        getFormulaType(pNumber1), numericFormulaManager.add(unwrap(pNumber1), unwrap(pNumber2)));
  }

  @Override
  public BitvectorFormula subtract(BitvectorFormula pNumber1, BitvectorFormula pNumber2) {
    assert getLength(pNumber1) == getLength(pNumber2) : "Expect operators to have the same size";
    return wrap(
        getFormulaType(pNumber1),
        numericFormulaManager.subtract(unwrap(pNumber1), unwrap(pNumber2)));
  }

  @Override
  public BitvectorFormula divide(
      BitvectorFormula pNumber1, BitvectorFormula pNumber2, boolean pSigned) {
    assert getLength(pNumber1) == getLength(pNumber2) : "Expect operators to have the same size";
    return wrap(
        getFormulaType(pNumber1),
        getC99ReplacementForSMTlib2Division(unwrap(pNumber1), unwrap(pNumber2)));
  }

  @DoNotCall
  @SuppressWarnings({"deprecation", "removal"})
  @Override
  public final BitvectorFormula modulo(
      BitvectorFormula pNumber1, BitvectorFormula pNumber2, boolean pSigned) {
    throw new UnsupportedOperationException(
        "This operation has been deprecated and replaced by smodulo() and remainder().");
  }

  @Override
  public BitvectorFormula smodulo(BitvectorFormula pNumber1, BitvectorFormula pNumber2) {
    // Note: signed bv modulo behaves differently compared to int modulo or bv remainder!
    throw new UnsupportedOperationException("not yet implemented for CPAchecker");
  }

  @Override
  public BitvectorFormula remainder(
      BitvectorFormula numerator, BitvectorFormula denominator, boolean signed) {
    assert getLength(numerator) == getLength(denominator)
        : "Expect operators to have the same size";
    if (numericFormulaManager instanceof IntegerFormulaManagerView imgr) {
      return wrap(
          getFormulaType(numerator),
          imgr.remainder((IntegerFormula) unwrap(numerator), (IntegerFormula) unwrap(denominator)));
    } else {
      return makeUf(getFormulaType(numerator), moduloUfDecl, numerator, denominator);
    }
  }

  /**
   * @see BitvectorFormulaManagerView#divide(BitvectorFormula, BitvectorFormula, boolean)
   */
  private Formula getC99ReplacementForSMTlib2Division(final T f1, final T f2) {

    final T zero = numericFormulaManager.makeNumber(0);
    final T additionalUnit =
        booleanFormulaManager.ifThenElse(
            numericFormulaManager.greaterOrEquals(f2, zero),
            numericFormulaManager.makeNumber(1),
            numericFormulaManager.makeNumber(-1));
    final T div = numericFormulaManager.divide(f1, f2);

    // IF   first operand is positive or is divisible by second operand
    // THEN return plain division --> here C99 is equal to SMTlib2
    // ELSE divide and add an additional unit towards the nearest infinity.

    return booleanFormulaManager.ifThenElse(
        booleanFormulaManager.or(
            numericFormulaManager.greaterOrEquals(f1, zero),
            numericFormulaManager.equal(numericFormulaManager.multiply(div, f2), f1)),
        div,
        numericFormulaManager.add(div, additionalUnit));
  }

  @Override
  public BitvectorFormula multiply(BitvectorFormula pNumber1, BitvectorFormula pNumber2) {
    assert getLength(pNumber1) == getLength(pNumber2) : "Expect operators to have the same size";
    return wrap(
        getFormulaType(pNumber1),
        numericFormulaManager.multiply(unwrap(pNumber1), unwrap(pNumber2)));
  }

  @Override
  public BooleanFormula equal(BitvectorFormula pNumber1, BitvectorFormula pNumber2) {
    assert getLength(pNumber1) == getLength(pNumber2) : "Expect operators to have the same size";
    return numericFormulaManager.equal(unwrap(pNumber1), unwrap(pNumber2));
  }

  @Override
  public BooleanFormula greaterThan(
      BitvectorFormula pNumber1, BitvectorFormula pNumber2, boolean pSigned) {
    assert getLength(pNumber1) == getLength(pNumber2) : "Expect operators to have the same size";
    return numericFormulaManager.greaterThan(unwrap(pNumber1), unwrap(pNumber2));
  }

  @Override
  public BooleanFormula greaterOrEquals(
      BitvectorFormula pNumber1, BitvectorFormula pNumber2, boolean pSigned) {
    assert getLength(pNumber1) == getLength(pNumber2) : "Expect operators to have the same size";
    return numericFormulaManager.greaterOrEquals(unwrap(pNumber1), unwrap(pNumber2));
  }

  @Override
  public BooleanFormula lessThan(
      BitvectorFormula pNumber1, BitvectorFormula pNumber2, boolean pSigned) {
    assert getLength(pNumber1) == getLength(pNumber2) : "Expect operators to have the same size";
    return numericFormulaManager.lessThan(unwrap(pNumber1), unwrap(pNumber2));
  }

  @Override
  public BooleanFormula lessOrEquals(
      BitvectorFormula pNumber1, BitvectorFormula pNumber2, boolean pSigned) {
    assert getLength(pNumber1) == getLength(pNumber2) : "Expect operators to have the same size";
    return numericFormulaManager.lessOrEquals(unwrap(pNumber1), unwrap(pNumber2));
  }

  /**
   * Returns a term representing the (arithmetic if signed is true) right shift of number by
   * toShift.
   */
  @Override
  public BitvectorFormula shiftRight(
      BitvectorFormula pNumber, BitvectorFormula pToShift, boolean signed) {
    assert getLength(pNumber) == getLength(pToShift) : "Expect operators to have the same size";
    return makeUf(getFormulaType(pNumber), rightShiftUfDecl, pNumber, pToShift);
  }

  @Override
  public BitvectorFormula shiftLeft(BitvectorFormula pNumber, BitvectorFormula pToShift) {
    assert getLength(pNumber) == getLength(pToShift) : "Expect operators to have the same size";
    return makeUf(getFormulaType(pNumber), leftShiftUfDecl, pNumber, pToShift);
  }

  @Override
  public BitvectorFormula rotateLeft(BitvectorFormula number, int toRotate) {
    throw new UnsupportedOperationException("not yet implemented for CPAchecker");
  }

  @Override
  public BitvectorFormula rotateLeft(BitvectorFormula number, BitvectorFormula toRotate) {
    throw new UnsupportedOperationException("not yet implemented for CPAchecker");
  }

  @Override
  public BitvectorFormula rotateRight(BitvectorFormula number, int toRotate) {
    throw new UnsupportedOperationException("not yet implemented for CPAchecker");
  }

  @Override
  public BitvectorFormula rotateRight(BitvectorFormula number, BitvectorFormula toRotate) {
    throw new UnsupportedOperationException("not yet implemented for CPAchecker");
  }

  @Override
  public BitvectorFormula concat(BitvectorFormula pFirst, BitvectorFormula pSecound) {
    int firstLength = getLength(pFirst);
    int secoundLength = getLength(pSecound);
    FormulaType<BitvectorFormula> returnType =
        getBitvectorTypeWithSize(firstLength + secoundLength);
    FunctionDeclaration<T> concatUfDecl = getConcatDecl(firstLength, secoundLength);
    return makeUf(returnType, concatUfDecl, pFirst, pSecound);
  }

  @Override
  public BitvectorFormula extract(BitvectorFormula pFirst, int pMsb, int pLsb) {
    FormulaType<BitvectorFormula> returnType = getBitvectorTypeWithSize(pMsb + 1 - pLsb);
    if (ignoreExtractExtend) {
      return wrap(returnType, unwrap(pFirst));
    }
    FunctionDeclaration<T> extractUfDecl = getExtractDecl(pMsb, pLsb);
    return makeUf(returnType, extractUfDecl, pFirst);
  }

  @Override
  public BitvectorFormula extend(BitvectorFormula pNumber, int pExtensionBits, boolean pSigned) {
    FormulaType<BitvectorFormula> returnType =
        getBitvectorTypeWithSize(getLength(pNumber) + pExtensionBits);
    if (ignoreExtractExtend) {
      return wrap(returnType, unwrap(pNumber));
    }
    FunctionDeclaration<T> extendUfDecl = getUFDecl("extend", pExtensionBits, pSigned);
    return makeUf(returnType, extendUfDecl, pNumber);
  }

  @Override
  public BitvectorFormula not(BitvectorFormula pBits) {
    return makeUf(getFormulaType(pBits), bitwiseNotUfDecl, pBits);
  }

  @Override
  public BitvectorFormula and(BitvectorFormula pBits1, BitvectorFormula pBits2) {
    assert getLength(pBits1) == getLength(pBits2) : "Expect operators to have the same size";
    return makeUf(getFormulaType(pBits1), bitwiseAndUfDecl, pBits1, pBits2);
  }

  @Override
  public BitvectorFormula or(BitvectorFormula pBits1, BitvectorFormula pBits2) {
    assert getLength(pBits1) == getLength(pBits2) : "Expect operators to have the same size";
    return makeUf(getFormulaType(pBits1), bitwiseOrUfDecl, pBits1, pBits2);
  }

  @Override
  public BitvectorFormula xor(BitvectorFormula pBits1, BitvectorFormula pBits2) {
    assert getLength(pBits1) == getLength(pBits2) : "Expect operators to have the same size";
    return makeUf(getFormulaType(pBits1), bitwiseXorUfDecl, pBits1, pBits2);
  }

  @Override
  public BitvectorFormula makeBitvector(int pLength, IntegerFormula pI) {
    // INT to BV -> just wrap
    return wrap(getBitvectorTypeWithSize(pLength), unwrap(pI));
  }

  @Override
  public IntegerFormula toIntegerFormula(BitvectorFormula pI, boolean pSigned) {
    final T unwrapped = unwrap(pI);
    if (numericFormulaManager.getFormulaType().equals(FormulaType.IntegerType)) {
      return (IntegerFormula) unwrapped;
    } else {
      return numericFormulaManager.floor(unwrapped);
    }
  }

  @Override
  public BooleanFormula distinct(List<BitvectorFormula> pBits) {
    if (pBits.isEmpty()) {
      return booleanFormulaManager.makeTrue();
    }
    int bitsize = getLength(pBits.get(0));
    pBits.forEach(
        bit ->
            Preconditions.checkArgument(
                bitsize == getLength(bit), "Expect operators to have the same size"));
    return numericFormulaManager.distinct(Lists.transform(pBits, this::unwrap));
  }
}
