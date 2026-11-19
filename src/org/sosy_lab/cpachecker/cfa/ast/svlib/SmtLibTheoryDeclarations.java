// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib;

import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import java.util.Collections;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibFunctionType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibArrayType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibBitVectorType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibFloatingPointType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibPredefinedType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibType;

public class SmtLibTheoryDeclarations {

  /* Integer stuff */

  public static SvLibFunctionDeclaration INT_EQUALITY =
      SvLibFunctionDeclaration.logicBuiltinFunction(
          FileLocation.DUMMY,
          new SvLibFunctionType(
              ImmutableList.of(SvLibSmtLibPredefinedType.INT, SvLibSmtLibPredefinedType.INT),
              SvLibSmtLibPredefinedType.BOOL),
          "=",
          "=",
          // Empty parameter list, because it has no body
          ImmutableList.of());

  public static SvLibFunctionDeclaration INT_LESS_THAN =
      SvLibFunctionDeclaration.logicBuiltinFunction(
          FileLocation.DUMMY,
          new SvLibFunctionType(
              ImmutableList.of(SvLibSmtLibPredefinedType.INT, SvLibSmtLibPredefinedType.INT),
              SvLibSmtLibPredefinedType.BOOL),
          "<",
          "<",
          ImmutableList.of());

  public static SvLibFunctionDeclaration INT_LESS_EQUAL_THAN =
      SvLibFunctionDeclaration.logicBuiltinFunction(
          FileLocation.DUMMY,
          new SvLibFunctionType(
              ImmutableList.of(SvLibSmtLibPredefinedType.INT, SvLibSmtLibPredefinedType.INT),
              SvLibSmtLibPredefinedType.BOOL),
          "<=",
          "<=",
          ImmutableList.of());

  public static SvLibFunctionDeclaration INT_GREATER_THAN =
      SvLibFunctionDeclaration.logicBuiltinFunction(
          FileLocation.DUMMY,
          new SvLibFunctionType(
              ImmutableList.of(SvLibSmtLibPredefinedType.INT, SvLibSmtLibPredefinedType.INT),
              SvLibSmtLibPredefinedType.BOOL),
          ">",
          ">",
          ImmutableList.of());

  public static SvLibFunctionDeclaration INT_GREATER_EQUAL_THAN =
      SvLibFunctionDeclaration.logicBuiltinFunction(
          FileLocation.DUMMY,
          new SvLibFunctionType(
              ImmutableList.of(SvLibSmtLibPredefinedType.INT, SvLibSmtLibPredefinedType.INT),
              SvLibSmtLibPredefinedType.BOOL),
          ">=",
          ">=",
          ImmutableList.of());

  public static SvLibFunctionDeclaration intSubtraction(int amountArguments) {
    Verify.verify(amountArguments == 1 || amountArguments == 2);
    return SvLibFunctionDeclaration.logicBuiltinFunction(
        FileLocation.DUMMY,
        new SvLibFunctionType(
            ImmutableList.copyOf(
                Collections.nCopies(amountArguments, SvLibSmtLibPredefinedType.INT)),
            SvLibSmtLibPredefinedType.INT),
        "-",
        "-",
        ImmutableList.of());
  }

  public static SvLibFunctionDeclaration INT_MULTIPLICATION =
      SvLibFunctionDeclaration.logicBuiltinFunction(
          FileLocation.DUMMY,
          new SvLibFunctionType(
              ImmutableList.of(SvLibSmtLibPredefinedType.INT, SvLibSmtLibPredefinedType.INT),
              SvLibSmtLibPredefinedType.INT),
          "*",
          "*",
          ImmutableList.of());

  public static SvLibFunctionDeclaration intAddition(int amountArguments) {
    return SvLibFunctionDeclaration.logicBuiltinFunction(
        FileLocation.DUMMY,
        new SvLibFunctionType(
            ImmutableList.copyOf(
                Collections.nCopies(amountArguments, SvLibSmtLibPredefinedType.INT)),
            SvLibSmtLibPredefinedType.INT),
        "+",
        "+",
        ImmutableList.of());
  }

  public static SvLibFunctionDeclaration INT_DIV =
      SvLibFunctionDeclaration.logicBuiltinFunction(
          FileLocation.DUMMY,
          new SvLibFunctionType(
              ImmutableList.of(SvLibSmtLibPredefinedType.INT, SvLibSmtLibPredefinedType.INT),
              SvLibSmtLibPredefinedType.INT),
          "div",
          "div",
          ImmutableList.of());

  /* Non-Linear Integer stuff */

  public static SvLibFunctionDeclaration INT_MOD =
      SvLibFunctionDeclaration.logicBuiltinFunction(
          FileLocation.DUMMY,
          new SvLibFunctionType(
              ImmutableList.of(SvLibSmtLibPredefinedType.INT, SvLibSmtLibPredefinedType.INT),
              SvLibSmtLibPredefinedType.INT),
          "mod",
          "mod",
          ImmutableList.of());

  /* Boolean stuff */

  public static SvLibFunctionDeclaration BOOL_EQUALITY =
      SvLibFunctionDeclaration.logicBuiltinFunction(
          FileLocation.DUMMY,
          new SvLibFunctionType(
              ImmutableList.of(SvLibSmtLibPredefinedType.BOOL, SvLibSmtLibPredefinedType.BOOL),
              SvLibSmtLibPredefinedType.BOOL),
          "=",
          "=",
          ImmutableList.of());

  /** The SMT-LIB {@code =} for two arguments of the given type. */
  public static SvLibFunctionDeclaration equality(SvLibType pArgumentType) {
    return SvLibFunctionDeclaration.logicBuiltinFunction(
        FileLocation.DUMMY,
        new SvLibFunctionType(
            ImmutableList.of(pArgumentType, pArgumentType), SvLibSmtLibPredefinedType.BOOL),
        "=",
        "=",
        ImmutableList.of());
  }

  /**
   * The SMT-LIB {@code distinct} predicate, which holds iff all of its arguments are pairwise
   * different. It is generic over the argument type (which must be the same for all arguments) and
   * always returns a boolean.
   */
  public static SvLibFunctionDeclaration distinct(SvLibType pArgumentType, int amountArguments) {
    Verify.verify(amountArguments >= 2);
    return SvLibFunctionDeclaration.logicBuiltinFunction(
        FileLocation.DUMMY,
        new SvLibFunctionType(
            ImmutableList.copyOf(Collections.nCopies(amountArguments, pArgumentType)),
            SvLibSmtLibPredefinedType.BOOL),
        "distinct",
        "distinct",
        ImmutableList.of());
  }

  public static SvLibFunctionDeclaration BOOL_NEGATION =
      SvLibFunctionDeclaration.logicBuiltinFunction(
          FileLocation.DUMMY,
          new SvLibFunctionType(
              ImmutableList.of(SvLibSmtLibPredefinedType.BOOL), SvLibSmtLibPredefinedType.BOOL),
          "not",
          "not",
          ImmutableList.of());

  public static SvLibFunctionDeclaration boolDisjunction(int amountArguments) {
    return SvLibFunctionDeclaration.logicBuiltinFunction(
        FileLocation.DUMMY,
        new SvLibFunctionType(
            ImmutableList.copyOf(
                Collections.nCopies(amountArguments, SvLibSmtLibPredefinedType.BOOL)),
            SvLibSmtLibPredefinedType.BOOL),
        "or",
        "or",
        ImmutableList.of());
  }

  public static SvLibFunctionDeclaration boolConjunction(int amountArguments) {
    return SvLibFunctionDeclaration.logicBuiltinFunction(
        FileLocation.DUMMY,
        new SvLibFunctionType(
            ImmutableList.copyOf(
                Collections.nCopies(amountArguments, SvLibSmtLibPredefinedType.BOOL)),
            SvLibSmtLibPredefinedType.BOOL),
        "and",
        "and",
        ImmutableList.of());
  }

  public static SvLibFunctionDeclaration boolImplication(int amountArguments) {
    return SvLibFunctionDeclaration.logicBuiltinFunction(
        FileLocation.DUMMY,
        new SvLibFunctionType(
            ImmutableList.copyOf(
                Collections.nCopies(amountArguments, SvLibSmtLibPredefinedType.BOOL)),
            SvLibSmtLibPredefinedType.BOOL),
        "=>",
        "=>",
        ImmutableList.of());
  }

  public static SvLibFunctionDeclaration ite(SvLibType pReturnType) {
    return SvLibFunctionDeclaration.logicBuiltinFunction(
        FileLocation.DUMMY,
        new SvLibFunctionType(
            ImmutableList.of(SvLibSmtLibPredefinedType.BOOL, pReturnType, pReturnType),
            pReturnType),
        "ite",
        "ite",
        ImmutableList.of());
  }

  /* Real stuff */

  /** The floor of a real, which SMT-LIB calls {@code to_int}. */
  public static SvLibFunctionDeclaration REAL_TO_INT =
      SvLibFunctionDeclaration.logicBuiltinFunction(
          FileLocation.DUMMY,
          new SvLibFunctionType(
              ImmutableList.of(SvLibSmtLibPredefinedType.REAL), SvLibSmtLibPredefinedType.INT),
          "to_int",
          "to_int",
          ImmutableList.of());

  public static SvLibFunctionDeclaration INT_TO_REAL =
      SvLibFunctionDeclaration.logicBuiltinFunction(
          FileLocation.DUMMY,
          new SvLibFunctionType(
              ImmutableList.of(SvLibSmtLibPredefinedType.INT), SvLibSmtLibPredefinedType.REAL),
          "to_real",
          "to_real",
          ImmutableList.of());

  public static SvLibFunctionDeclaration REAL_MINUS =
      SvLibFunctionDeclaration.logicBuiltinFunction(
          FileLocation.DUMMY,
          new SvLibFunctionType(
              ImmutableList.of(SvLibSmtLibPredefinedType.REAL, SvLibSmtLibPredefinedType.REAL),
              SvLibSmtLibPredefinedType.REAL),
          "-",
          "-",
          ImmutableList.of());

  public static SvLibFunctionDeclaration REAL_MULTIPLICATION =
      SvLibFunctionDeclaration.logicBuiltinFunction(
          FileLocation.DUMMY,
          new SvLibFunctionType(
              ImmutableList.of(SvLibSmtLibPredefinedType.REAL, SvLibSmtLibPredefinedType.REAL),
              SvLibSmtLibPredefinedType.REAL),
          "*",
          "*",
          ImmutableList.of());

  public static SvLibFunctionDeclaration realAddition(int amountArguments) {
    return SvLibFunctionDeclaration.logicBuiltinFunction(
        FileLocation.DUMMY,
        new SvLibFunctionType(
            ImmutableList.copyOf(
                Collections.nCopies(amountArguments, SvLibSmtLibPredefinedType.REAL)),
            SvLibSmtLibPredefinedType.REAL),
        "+",
        "+",
        ImmutableList.of());
  }

  /* Array stuff */

  public static SvLibFunctionDeclaration arraySelect(
      SvLibSmtLibType indexType, SvLibSmtLibType elementType) {
    return SvLibFunctionDeclaration.logicBuiltinFunction(
        FileLocation.DUMMY,
        new SvLibFunctionType(
            ImmutableList.of(new SvLibSmtLibArrayType(indexType, elementType), indexType),
            elementType),
        "select",
        "select",
        ImmutableList.of());
  }

  public static SvLibFunctionDeclaration arrayStore(
      SvLibSmtLibType indexType, SvLibSmtLibType elementType) {
    return SvLibFunctionDeclaration.logicBuiltinFunction(
        FileLocation.DUMMY,
        new SvLibFunctionType(
            ImmutableList.of(
                new SvLibSmtLibArrayType(indexType, elementType), indexType, elementType),
            new SvLibSmtLibArrayType(indexType, elementType)),
        "store",
        "store",
        ImmutableList.of());
  }

  /**
   * The qualified constant array identifier {@code (as const (Array K V))}, which maps every index
   * to the given default element.
   */
  public static SvLibFunctionDeclaration arrayConst(
      SvLibSmtLibType indexType, SvLibSmtLibType elementType) {
    return SvLibFunctionDeclaration.logicBuiltinFunction(
        FileLocation.DUMMY,
        new SvLibFunctionType(
            ImmutableList.of(elementType), new SvLibSmtLibArrayType(indexType, elementType)),
        "const",
        "const",
        ImmutableList.of());
  }

  public static SvLibFunctionDeclaration arrayEquality(
      SvLibSmtLibType indexType, SvLibSmtLibType elementType) {
    return SvLibFunctionDeclaration.logicBuiltinFunction(
        FileLocation.DUMMY,
        new SvLibFunctionType(
            ImmutableList.of(
                new SvLibSmtLibArrayType(indexType, elementType),
                new SvLibSmtLibArrayType(indexType, elementType)),
            SvLibSmtLibPredefinedType.BOOL),
        "=",
        "=",
        ImmutableList.of());
  }

  /* Bit Vector stuff */

  public static SvLibFunctionDeclaration bitVectorConcat(int pLeftSize, int pRightSize) {
    return SvLibFunctionDeclaration.logicBuiltinFunction(
        FileLocation.DUMMY,
        new SvLibFunctionType(
            ImmutableList.of(
                new SvLibSmtLibBitVectorType(pLeftSize), new SvLibSmtLibBitVectorType(pRightSize)),
            new SvLibSmtLibBitVectorType(pLeftSize + pRightSize)),
        "concat",
        "concat",
        ImmutableList.of());
  }

  /** The extraction {@code (_ extract pMsb pLsb)} of the bits from pMsb down to pLsb. */
  public static SvLibFunctionDeclaration bitVectorExtract(int pSourceSize, int pMsb, int pLsb) {
    Verify.verify(pSourceSize > pMsb && pMsb >= pLsb && pLsb >= 0);
    return SvLibFunctionDeclaration.logicBuiltinFunction(
        FileLocation.DUMMY,
        new SvLibFunctionType(
            ImmutableList.of(new SvLibSmtLibBitVectorType(pSourceSize)),
            new SvLibSmtLibBitVectorType(pMsb - pLsb + 1)),
        SmtLibIndexedSymbol.of("extract", pMsb, pLsb));
  }

  public static SvLibFunctionDeclaration bitVectorBitwiseNegation(int pSize) {
    return SvLibFunctionDeclaration.logicBuiltinFunction(
        FileLocation.DUMMY,
        new SvLibFunctionType(
            ImmutableList.of(new SvLibSmtLibBitVectorType(pSize)),
            new SvLibSmtLibBitVectorType(pSize)),
        "bvnot",
        "bvnot",
        ImmutableList.of());
  }

  public static SvLibFunctionDeclaration bitVectorComplementNegation(int pSize) {
    return SvLibFunctionDeclaration.logicBuiltinFunction(
        FileLocation.DUMMY,
        new SvLibFunctionType(
            ImmutableList.of(new SvLibSmtLibBitVectorType(pSize)),
            new SvLibSmtLibBitVectorType(pSize)),
        "bvneg",
        "bvneg",
        ImmutableList.of());
  }

  public static SvLibFunctionDeclaration bitVectorEquality(int pSize) {
    return SvLibFunctionDeclaration.logicBuiltinFunction(
        FileLocation.DUMMY,
        new SvLibFunctionType(
            ImmutableList.of(
                new SvLibSmtLibBitVectorType(pSize), new SvLibSmtLibBitVectorType(pSize)),
            SvLibSmtLibPredefinedType.BOOL),
        "=",
        "=",
        ImmutableList.of());
  }

  public static SvLibFunctionDeclaration bitVectorAnd(int pSize) {
    return SvLibFunctionDeclaration.logicBuiltinFunction(
        FileLocation.DUMMY,
        new SvLibFunctionType(
            ImmutableList.of(
                new SvLibSmtLibBitVectorType(pSize), new SvLibSmtLibBitVectorType(pSize)),
            new SvLibSmtLibBitVectorType(pSize)),
        "bvand",
        "bvand",
        ImmutableList.of());
  }

  public static SvLibFunctionDeclaration bitVectorNand(int pSize) {
    return SvLibFunctionDeclaration.logicBuiltinFunction(
        FileLocation.DUMMY,
        new SvLibFunctionType(
            ImmutableList.of(
                new SvLibSmtLibBitVectorType(pSize), new SvLibSmtLibBitVectorType(pSize)),
            new SvLibSmtLibBitVectorType(pSize)),
        "bvnand",
        "bvnand",
        ImmutableList.of());
  }

  public static SvLibFunctionDeclaration bitVectorOr(int pSize) {
    return SvLibFunctionDeclaration.logicBuiltinFunction(
        FileLocation.DUMMY,
        new SvLibFunctionType(
            ImmutableList.of(
                new SvLibSmtLibBitVectorType(pSize), new SvLibSmtLibBitVectorType(pSize)),
            new SvLibSmtLibBitVectorType(pSize)),
        "bvor",
        "bvor",
        ImmutableList.of());
  }

  public static SvLibFunctionDeclaration bitVectorAddition(int pSize) {
    return SvLibFunctionDeclaration.logicBuiltinFunction(
        FileLocation.DUMMY,
        new SvLibFunctionType(
            ImmutableList.of(
                new SvLibSmtLibBitVectorType(pSize), new SvLibSmtLibBitVectorType(pSize)),
            new SvLibSmtLibBitVectorType(pSize)),
        "bvadd",
        "bvadd",
        ImmutableList.of());
  }

  public static SvLibFunctionDeclaration bitVectorMul(int pSize) {
    return SvLibFunctionDeclaration.logicBuiltinFunction(
        FileLocation.DUMMY,
        new SvLibFunctionType(
            ImmutableList.of(
                new SvLibSmtLibBitVectorType(pSize), new SvLibSmtLibBitVectorType(pSize)),
            new SvLibSmtLibBitVectorType(pSize)),
        "bvmul",
        "bvmul",
        ImmutableList.of());
  }

  public static SvLibFunctionDeclaration bitVectorUnsignedDivision(int pSize) {
    return SvLibFunctionDeclaration.logicBuiltinFunction(
        FileLocation.DUMMY,
        new SvLibFunctionType(
            ImmutableList.of(
                new SvLibSmtLibBitVectorType(pSize), new SvLibSmtLibBitVectorType(pSize)),
            new SvLibSmtLibBitVectorType(pSize)),
        "bvudiv",
        "bvudiv",
        ImmutableList.of());
  }

  public static SvLibFunctionDeclaration bitVectorUnsignedRemainder(int pSize) {
    return SvLibFunctionDeclaration.logicBuiltinFunction(
        FileLocation.DUMMY,
        new SvLibFunctionType(
            ImmutableList.of(
                new SvLibSmtLibBitVectorType(pSize), new SvLibSmtLibBitVectorType(pSize)),
            new SvLibSmtLibBitVectorType(pSize)),
        "bvurem",
        "bvurem",
        ImmutableList.of());
  }

  public static SvLibFunctionDeclaration bitVectorShiftLeft(int pSize) {
    return SvLibFunctionDeclaration.logicBuiltinFunction(
        FileLocation.DUMMY,
        new SvLibFunctionType(
            ImmutableList.of(
                new SvLibSmtLibBitVectorType(pSize), new SvLibSmtLibBitVectorType(pSize)),
            new SvLibSmtLibBitVectorType(pSize)),
        "bvshl",
        "bvshl",
        ImmutableList.of());
  }

  public static SvLibFunctionDeclaration bitVectorLogicalShiftRight(int pSize) {
    return SvLibFunctionDeclaration.logicBuiltinFunction(
        FileLocation.DUMMY,
        new SvLibFunctionType(
            ImmutableList.of(
                new SvLibSmtLibBitVectorType(pSize), new SvLibSmtLibBitVectorType(pSize)),
            new SvLibSmtLibBitVectorType(pSize)),
        "bvlshr",
        "bvlshr",
        ImmutableList.of());
  }

  /* Bit Vector stuff not in the SMT-LIB FixedSizeBitVector theory but used by Z3 */

  public static SvLibFunctionDeclaration bitVectorArithmeticShiftRight(int pSize) {
    return SvLibFunctionDeclaration.logicBuiltinFunction(
        FileLocation.DUMMY,
        new SvLibFunctionType(
            ImmutableList.of(
                new SvLibSmtLibBitVectorType(pSize), new SvLibSmtLibBitVectorType(pSize)),
            new SvLibSmtLibBitVectorType(pSize)),
        "bvashr",
        "bvashr",
        ImmutableList.of());
  }

  /** Reduction xor as used by btor2 and Z3: xor of all bits of the argument. */
  public static SvLibFunctionDeclaration bitVectorReductionXor(int pSize) {
    return SvLibFunctionDeclaration.logicBuiltinFunction(
        FileLocation.DUMMY,
        new SvLibFunctionType(
            ImmutableList.of(new SvLibSmtLibBitVectorType(pSize)), new SvLibSmtLibBitVectorType(1)),
        "redxor",
        "redxor",
        ImmutableList.of());
  }

  /** Reduction or as used by btor2 and Z3: or of all bits of the argument. */
  public static SvLibFunctionDeclaration bitVectorReductionOr(int pSize) {
    return SvLibFunctionDeclaration.logicBuiltinFunction(
        FileLocation.DUMMY,
        new SvLibFunctionType(
            ImmutableList.of(new SvLibSmtLibBitVectorType(pSize)), new SvLibSmtLibBitVectorType(1)),
        "redor",
        "redor",
        ImmutableList.of());
  }

  /** Reduction and as used by btor2 and Z3: and of all bits of the argument. */
  public static SvLibFunctionDeclaration bitVectorReductionAnd(int pSize) {
    return SvLibFunctionDeclaration.logicBuiltinFunction(
        FileLocation.DUMMY,
        new SvLibFunctionType(
            ImmutableList.of(new SvLibSmtLibBitVectorType(pSize)), new SvLibSmtLibBitVectorType(1)),
        "redand",
        "redand",
        ImmutableList.of());
  }

  /** Equality returning a bitvector of size 1 instead of a boolean. */
  public static SvLibFunctionDeclaration bitVectorComparison(int pSize) {
    return SvLibFunctionDeclaration.logicBuiltinFunction(
        FileLocation.DUMMY,
        new SvLibFunctionType(
            ImmutableList.of(
                new SvLibSmtLibBitVectorType(pSize), new SvLibSmtLibBitVectorType(pSize)),
            new SvLibSmtLibBitVectorType(1)),
        "bvcomp",
        "bvcomp",
        ImmutableList.of());
  }

  public static SvLibFunctionDeclaration bitVectorXnor(int pSize) {
    return SvLibFunctionDeclaration.logicBuiltinFunction(
        FileLocation.DUMMY,
        new SvLibFunctionType(
            ImmutableList.of(
                new SvLibSmtLibBitVectorType(pSize), new SvLibSmtLibBitVectorType(pSize)),
            new SvLibSmtLibBitVectorType(pSize)),
        "bvxnor",
        "bvxnor",
        ImmutableList.of());
  }

  /** The operation {@code (_ repeat pCount)}, which concatenates pCount copies of its argument. */
  public static SvLibFunctionDeclaration bitVectorRepeat(int pSourceSize, int pCount) {
    Verify.verify(pSourceSize > 0 && pCount > 0);
    return SvLibFunctionDeclaration.logicBuiltinFunction(
        FileLocation.DUMMY,
        new SvLibFunctionType(
            ImmutableList.of(new SvLibSmtLibBitVectorType(pSourceSize)),
            new SvLibSmtLibBitVectorType(pSourceSize * pCount)),
        SmtLibIndexedSymbol.of("repeat", pCount));
  }

  public static SvLibFunctionDeclaration bitVectorXor(int pSize) {
    return SvLibFunctionDeclaration.logicBuiltinFunction(
        FileLocation.DUMMY,
        new SvLibFunctionType(
            ImmutableList.of(
                new SvLibSmtLibBitVectorType(pSize), new SvLibSmtLibBitVectorType(pSize)),
            new SvLibSmtLibBitVectorType(pSize)),
        "bvxor",
        "bvxor",
        ImmutableList.of());
  }

  public static SvLibFunctionDeclaration bitVectorUnsignedLessEqual(int pSize) {
    return SvLibFunctionDeclaration.logicBuiltinFunction(
        FileLocation.DUMMY,
        new SvLibFunctionType(
            ImmutableList.of(
                new SvLibSmtLibBitVectorType(pSize), new SvLibSmtLibBitVectorType(pSize)),
            SvLibSmtLibPredefinedType.BOOL),
        "bvule",
        "bvule",
        ImmutableList.of());
  }

  public static SvLibFunctionDeclaration bitVectorUnsignedLessThan(int pSize) {
    return SvLibFunctionDeclaration.logicBuiltinFunction(
        FileLocation.DUMMY,
        new SvLibFunctionType(
            ImmutableList.of(
                new SvLibSmtLibBitVectorType(pSize), new SvLibSmtLibBitVectorType(pSize)),
            SvLibSmtLibPredefinedType.BOOL),
        "bvult",
        "bvult",
        ImmutableList.of());
  }

  public static SvLibFunctionDeclaration bitVectorUnsignedGreaterEqual(int pSize) {
    return SvLibFunctionDeclaration.logicBuiltinFunction(
        FileLocation.DUMMY,
        new SvLibFunctionType(
            ImmutableList.of(
                new SvLibSmtLibBitVectorType(pSize), new SvLibSmtLibBitVectorType(pSize)),
            SvLibSmtLibPredefinedType.BOOL),
        "bvuge",
        "bvuge",
        ImmutableList.of());
  }

  public static SvLibFunctionDeclaration bitVectorUnsignedGreaterThan(int pSize) {
    return SvLibFunctionDeclaration.logicBuiltinFunction(
        FileLocation.DUMMY,
        new SvLibFunctionType(
            ImmutableList.of(
                new SvLibSmtLibBitVectorType(pSize), new SvLibSmtLibBitVectorType(pSize)),
            SvLibSmtLibPredefinedType.BOOL),
        "bvugt",
        "bvugt",
        ImmutableList.of());
  }

  public static SvLibFunctionDeclaration bitVectorSignedGreaterEqual(int pSize) {
    return SvLibFunctionDeclaration.logicBuiltinFunction(
        FileLocation.DUMMY,
        new SvLibFunctionType(
            ImmutableList.of(
                new SvLibSmtLibBitVectorType(pSize), new SvLibSmtLibBitVectorType(pSize)),
            SvLibSmtLibPredefinedType.BOOL),
        "bvsge",
        "bvsge",
        ImmutableList.of());
  }

  public static SvLibFunctionDeclaration bitVectorSignedGreaterThan(int pSize) {
    return SvLibFunctionDeclaration.logicBuiltinFunction(
        FileLocation.DUMMY,
        new SvLibFunctionType(
            ImmutableList.of(
                new SvLibSmtLibBitVectorType(pSize), new SvLibSmtLibBitVectorType(pSize)),
            SvLibSmtLibPredefinedType.BOOL),
        "bvsgt",
        "bvsgt",
        ImmutableList.of());
  }

  /** Used by Z3 */
  public static SvLibFunctionDeclaration bitVectorSignedLessEqual(int pSize) {
    return SvLibFunctionDeclaration.logicBuiltinFunction(
        FileLocation.DUMMY,
        new SvLibFunctionType(
            ImmutableList.of(
                new SvLibSmtLibBitVectorType(pSize), new SvLibSmtLibBitVectorType(pSize)),
            SvLibSmtLibPredefinedType.BOOL),
        "bvsle",
        "bvsle",
        ImmutableList.of());
  }

  /** Used by Z3 */
  public static SvLibFunctionDeclaration bitVectorSignedLessThan(int pSize) {
    return SvLibFunctionDeclaration.logicBuiltinFunction(
        FileLocation.DUMMY,
        new SvLibFunctionType(
            ImmutableList.of(
                new SvLibSmtLibBitVectorType(pSize), new SvLibSmtLibBitVectorType(pSize)),
            SvLibSmtLibPredefinedType.BOOL),
        "bvslt",
        "bvslt",
        ImmutableList.of());
  }

  /** Used by Z3 */
  public static SvLibFunctionDeclaration bitVectorSignedDivision(int pSize) {
    return SvLibFunctionDeclaration.logicBuiltinFunction(
        FileLocation.DUMMY,
        new SvLibFunctionType(
            ImmutableList.of(
                new SvLibSmtLibBitVectorType(pSize), new SvLibSmtLibBitVectorType(pSize)),
            new SvLibSmtLibBitVectorType(pSize)),
        "bvsdiv",
        "bvsdiv",
        ImmutableList.of());
  }

  /** Used by Z3 */
  public static SvLibFunctionDeclaration bitVectorSignedRemainder(int pSize) {
    return SvLibFunctionDeclaration.logicBuiltinFunction(
        FileLocation.DUMMY,
        new SvLibFunctionType(
            ImmutableList.of(
                new SvLibSmtLibBitVectorType(pSize), new SvLibSmtLibBitVectorType(pSize)),
            new SvLibSmtLibBitVectorType(pSize)),
        "bvsrem",
        "bvsrem",
        ImmutableList.of());
  }

  /** Used by Z3 */
  public static SvLibFunctionDeclaration bitVectorSubstraction(int pSize) {
    return SvLibFunctionDeclaration.logicBuiltinFunction(
        FileLocation.DUMMY,
        new SvLibFunctionType(
            ImmutableList.of(
                new SvLibSmtLibBitVectorType(pSize), new SvLibSmtLibBitVectorType(pSize)),
            new SvLibSmtLibBitVectorType(pSize)),
        "bvsub",
        "bvsub",
        ImmutableList.of());
  }

  public static SvLibFunctionDeclaration bitVectorZeroExtend(int pSourceSize, int pTargetSize) {
    return bitVectorExtend("zero_extend", pSourceSize, pTargetSize);
  }

  public static SvLibFunctionDeclaration bitVectorSignExtend(int pSourceSize, int pTargetSize) {
    return bitVectorExtend("sign_extend", pSourceSize, pTargetSize);
  }

  /** The extension {@code (_ pOperator i)} of a bitvector by i bits. */
  private static SvLibFunctionDeclaration bitVectorExtend(
      String pOperator, int pSourceSize, int pTargetSize) {
    Verify.verify(pTargetSize >= pSourceSize);
    return SvLibFunctionDeclaration.logicBuiltinFunction(
        FileLocation.DUMMY,
        new SvLibFunctionType(
            ImmutableList.of(new SvLibSmtLibBitVectorType(pSourceSize)),
            new SvLibSmtLibBitVectorType(pTargetSize)),
        SmtLibIndexedSymbol.of(pOperator, pTargetSize - pSourceSize));
  }

  // ***** Floating point numbers *****

  /** The addition of two floating point numbers, which rounds. */
  public static SvLibFunctionDeclaration floatingPointAddition(SvLibSmtLibFloatingPointType pType) {
    return floatingPointArithmetic("fp.add", 2, pType);
  }

  /** The subtraction of two floating point numbers, which rounds. */
  public static SvLibFunctionDeclaration floatingPointSubtraction(
      SvLibSmtLibFloatingPointType pType) {
    return floatingPointArithmetic("fp.sub", 2, pType);
  }

  /** The multiplication of two floating point numbers, which rounds. */
  public static SvLibFunctionDeclaration floatingPointMultiplication(
      SvLibSmtLibFloatingPointType pType) {
    return floatingPointArithmetic("fp.mul", 2, pType);
  }

  /** The division of two floating point numbers, which rounds. */
  public static SvLibFunctionDeclaration floatingPointDivision(SvLibSmtLibFloatingPointType pType) {
    return floatingPointArithmetic("fp.div", 2, pType);
  }

  /** The square root of a floating point number, which rounds. */
  public static SvLibFunctionDeclaration floatingPointSquareRoot(
      SvLibSmtLibFloatingPointType pType) {
    return floatingPointArithmetic("fp.sqrt", 1, pType);
  }

  /** The integral floating point number that a floating point number rounds to. */
  public static SvLibFunctionDeclaration floatingPointRoundToIntegral(
      SvLibSmtLibFloatingPointType pType) {
    return floatingPointArithmetic("fp.roundToIntegral", 1, pType);
  }

  /** The remainder of the division of two floating point numbers. */
  public static SvLibFunctionDeclaration floatingPointRemainder(
      SvLibSmtLibFloatingPointType pType) {
    return floatingPointOperation("fp.rem", 2, pType, pType);
  }

  /** The negation of a floating point number. */
  public static SvLibFunctionDeclaration floatingPointNegation(SvLibSmtLibFloatingPointType pType) {
    return floatingPointOperation("fp.neg", 1, pType, pType);
  }

  /** The absolute value of a floating point number. */
  public static SvLibFunctionDeclaration floatingPointAbsolute(SvLibSmtLibFloatingPointType pType) {
    return floatingPointOperation("fp.abs", 1, pType, pType);
  }

  /** The maximum of two floating point numbers. */
  public static SvLibFunctionDeclaration floatingPointMaximum(SvLibSmtLibFloatingPointType pType) {
    return floatingPointOperation("fp.max", 2, pType, pType);
  }

  /** The minimum of two floating point numbers. */
  public static SvLibFunctionDeclaration floatingPointMinimum(SvLibSmtLibFloatingPointType pType) {
    return floatingPointOperation("fp.min", 2, pType, pType);
  }

  /** Whether the first floating point number is less than the second one. */
  public static SvLibFunctionDeclaration floatingPointLessThan(SvLibSmtLibFloatingPointType pType) {
    return floatingPointOperation("fp.lt", 2, pType, SvLibSmtLibPredefinedType.BOOL);
  }

  /** Whether the first floating point number is less than or equal to the second one. */
  public static SvLibFunctionDeclaration floatingPointLessEqual(
      SvLibSmtLibFloatingPointType pType) {
    return floatingPointOperation("fp.leq", 2, pType, SvLibSmtLibPredefinedType.BOOL);
  }

  /** Whether the first floating point number is greater than the second one. */
  public static SvLibFunctionDeclaration floatingPointGreaterThan(
      SvLibSmtLibFloatingPointType pType) {
    return floatingPointOperation("fp.gt", 2, pType, SvLibSmtLibPredefinedType.BOOL);
  }

  /** Whether the first floating point number is greater than or equal to the second one. */
  public static SvLibFunctionDeclaration floatingPointGreaterEqual(
      SvLibSmtLibFloatingPointType pType) {
    return floatingPointOperation("fp.geq", 2, pType, SvLibSmtLibPredefinedType.BOOL);
  }

  /** The equality of IEEE 754, which is false for NaN and true for the two zeros. */
  public static SvLibFunctionDeclaration floatingPointEquality(SvLibSmtLibFloatingPointType pType) {
    return floatingPointOperation("fp.eq", 2, pType, SvLibSmtLibPredefinedType.BOOL);
  }

  /** The equality of the core theory, which is the identity of two floating point numbers. */
  public static SvLibFunctionDeclaration floatingPointIdentity(SvLibSmtLibFloatingPointType pType) {
    return floatingPointOperation("=", 2, pType, SvLibSmtLibPredefinedType.BOOL);
  }

  /** Whether a floating point number is NaN. */
  public static SvLibFunctionDeclaration floatingPointIsNaN(SvLibSmtLibFloatingPointType pType) {
    return floatingPointOperation("fp.isNaN", 1, pType, SvLibSmtLibPredefinedType.BOOL);
  }

  /** Whether a floating point number is infinite. */
  public static SvLibFunctionDeclaration floatingPointIsInfinite(
      SvLibSmtLibFloatingPointType pType) {
    return floatingPointOperation("fp.isInfinite", 1, pType, SvLibSmtLibPredefinedType.BOOL);
  }

  /** Whether a floating point number is a zero. */
  public static SvLibFunctionDeclaration floatingPointIsZero(SvLibSmtLibFloatingPointType pType) {
    return floatingPointOperation("fp.isZero", 1, pType, SvLibSmtLibPredefinedType.BOOL);
  }

  /** Whether a floating point number is negative. */
  public static SvLibFunctionDeclaration floatingPointIsNegative(
      SvLibSmtLibFloatingPointType pType) {
    return floatingPointOperation("fp.isNegative", 1, pType, SvLibSmtLibPredefinedType.BOOL);
  }

  /** Whether a floating point number is subnormal. */
  public static SvLibFunctionDeclaration floatingPointIsSubnormal(
      SvLibSmtLibFloatingPointType pType) {
    return floatingPointOperation("fp.isSubnormal", 1, pType, SvLibSmtLibPredefinedType.BOOL);
  }

  /** Whether a floating point number is normal. */
  public static SvLibFunctionDeclaration floatingPointIsNormal(SvLibSmtLibFloatingPointType pType) {
    return floatingPointOperation("fp.isNormal", 1, pType, SvLibSmtLibPredefinedType.BOOL);
  }

  /**
   * The declaration of an operation of the theory of floating point numbers that rounds, i.e. whose
   * first argument is a rounding mode.
   */
  private static SvLibFunctionDeclaration floatingPointArithmetic(
      String pOperator, int pArity, SvLibSmtLibFloatingPointType pType) {
    ImmutableList.Builder<SvLibType> argumentTypes = ImmutableList.builder();
    argumentTypes.add(SvLibSmtLibPredefinedType.ROUNDING_MODE);
    for (int argument = 0; argument < pArity; argument++) {
      argumentTypes.add(pType);
    }
    return SvLibFunctionDeclaration.logicBuiltinFunction(
        FileLocation.DUMMY,
        new SvLibFunctionType(argumentTypes.build(), pType),
        pOperator,
        pOperator,
        ImmutableList.of());
  }

  /**
   * The declaration of an operation of the theory of floating point numbers that does not round,
   * such as the negation or a comparison.
   */
  private static SvLibFunctionDeclaration floatingPointOperation(
      String pOperator, int pArity, SvLibSmtLibFloatingPointType pType, SvLibType pReturnType) {
    ImmutableList.Builder<SvLibType> argumentTypes = ImmutableList.builder();
    for (int argument = 0; argument < pArity; argument++) {
      argumentTypes.add(pType);
    }
    return SvLibFunctionDeclaration.logicBuiltinFunction(
        FileLocation.DUMMY,
        new SvLibFunctionType(argumentTypes.build(), pReturnType),
        pOperator,
        pOperator,
        ImmutableList.of());
  }

  /**
   * The declaration of a conversion into a floating point number, which is an indexed identifier
   * whose indices are the sizes of the target type and which rounds.
   */
  public static SvLibFunctionDeclaration toFloatingPoint(
      boolean pFromUnsigned, SvLibType pSourceType, SvLibSmtLibFloatingPointType pTargetType) {
    return SvLibFunctionDeclaration.logicBuiltinFunction(
        FileLocation.DUMMY,
        new SvLibFunctionType(
            ImmutableList.of(SvLibSmtLibPredefinedType.ROUNDING_MODE, pSourceType), pTargetType),
        SmtLibIndexedSymbol.of(
            pFromUnsigned ? "to_fp_unsigned" : "to_fp",
            pTargetType.getExponentSize(),
            pTargetType.getSignificandSize()));
  }

  /**
   * The declaration of a conversion of a bitvector into an integer, which interprets the bits as a
   * number with or without a sign.
   */
  public static SvLibFunctionDeclaration bitVectorToInt(boolean pSigned, int pSize) {
    String name = pSigned ? "sbv_to_int" : "ubv_to_int";
    return SvLibFunctionDeclaration.logicBuiltinFunction(
        FileLocation.DUMMY,
        new SvLibFunctionType(
            ImmutableList.of(new SvLibSmtLibBitVectorType(pSize)), SvLibSmtLibPredefinedType.INT),
        name,
        name,
        ImmutableList.of());
  }

  /**
   * The declaration of a conversion of an integer into a bitvector, which is an indexed identifier
   * whose index is the size of the bitvector.
   */
  public static SvLibFunctionDeclaration intToBitVector(int pSize) {
    return SvLibFunctionDeclaration.logicBuiltinFunction(
        FileLocation.DUMMY,
        new SvLibFunctionType(
            ImmutableList.of(SvLibSmtLibPredefinedType.INT), new SvLibSmtLibBitVectorType(pSize)),
        SmtLibIndexedSymbol.of("int_to_bv", pSize));
  }

  /**
   * The declaration of the conversion of a floating point number into the bitvector of its bits in
   * the representation of IEEE 754, which is the reverse of the one-argument {@code to_fp}.
   */
  public static SvLibFunctionDeclaration floatingPointAsBitVector(
      SvLibSmtLibFloatingPointType pSourceType) {
    String name = "fp.to_ieee_bv";
    return SvLibFunctionDeclaration.logicBuiltinFunction(
        FileLocation.DUMMY,
        new SvLibFunctionType(
            ImmutableList.of(pSourceType),
            new SvLibSmtLibBitVectorType(
                pSourceType.getExponentSize() + pSourceType.getSignificandSize())),
        name,
        name,
        ImmutableList.of());
  }

  /**
   * The declaration of the conversion of the bits of the representation of IEEE 754 into the
   * floating point number they denote, which does not round because it loses no information.
   */
  public static SvLibFunctionDeclaration floatingPointFromBitVector(
      SvLibSmtLibBitVectorType pSourceType, SvLibSmtLibFloatingPointType pTargetType) {
    return SvLibFunctionDeclaration.logicBuiltinFunction(
        FileLocation.DUMMY,
        new SvLibFunctionType(ImmutableList.of(pSourceType), pTargetType),
        SmtLibIndexedSymbol.of(
            "to_fp", pTargetType.getExponentSize(), pTargetType.getSignificandSize()));
  }

  /**
   * The declaration of a conversion of a floating point number into a bitvector, which is an
   * indexed identifier whose index is the size of the bitvector and which rounds.
   */
  public static SvLibFunctionDeclaration floatingPointToBitVector(
      boolean pSigned,
      SvLibSmtLibFloatingPointType pSourceType,
      SvLibSmtLibBitVectorType pTargetType) {
    return SvLibFunctionDeclaration.logicBuiltinFunction(
        FileLocation.DUMMY,
        new SvLibFunctionType(
            ImmutableList.of(SvLibSmtLibPredefinedType.ROUNDING_MODE, pSourceType), pTargetType),
        SmtLibIndexedSymbol.of(pSigned ? "fp.to_sbv" : "fp.to_ubv", pTargetType.getSize()));
  }
}
