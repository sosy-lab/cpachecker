// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.math.BigInteger;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqDeclarationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqInitializers.SeqInitializer;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqTypes.SeqSimpleType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector.BinaryBitVectorExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector.BitVectorExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector.HexadecimalBitVectorExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public class BitVectorUtil {

  public static final int MIN_BINARY_LENGTH = SeqBitVectorType.__UINT8_T.size;
  public static final int MAX_BINARY_LENGTH = SeqBitVectorType.__UINT64_T.size;

  // Creation ======================================================================================

  public static BitVectorExpression buildZeroBitVector(
      MPOROptions pOptions, ImmutableList<BitVectorGlobalVariable> pAllGlobalVariables) {

    checkArgument(
        !pOptions.porBitVectorEncoding.equals(BitVectorEncoding.NONE),
        "no bit vector encoding specified");
    return buildBitVectorExpressionByEncoding(
        pOptions.porBitVectorEncoding, pAllGlobalVariables.size(), ImmutableSet.of());
  }

  public static BitVectorExpression buildBitVectorExpression(
      MPOROptions pOptions,
      @NonNull ImmutableList<BitVectorGlobalVariable> pAllVariables,
      @NonNull ImmutableList<CVariableDeclaration> pAccessedVariables) {

    checkArgument(
        !pOptions.porBitVectorEncoding.equals(BitVectorEncoding.NONE),
        "no bit vector encoding specified");
    ImmutableSet<CVariableDeclaration> allVariables =
        pAllVariables.stream()
            .map(BitVectorGlobalVariable::getDeclaration)
            .collect(ImmutableSet.toImmutableSet());
    checkArgument(
        allVariables.containsAll(pAccessedVariables),
        "pAllVariables must contain all pAccessedVariables as keys.");

    // retrieve all variable ids from pAllVariables that are in pAccessedVariables
    final ImmutableSet<BitVectorGlobalVariable> setBits =
        pAllVariables.stream()
            .filter(variable -> pAccessedVariables.contains(variable.getDeclaration()))
            .collect(ImmutableSet.toImmutableSet());
    return buildBitVectorExpressionByEncoding(
        pOptions.porBitVectorEncoding, pAllVariables.size(), setBits);
  }

  private static BitVectorExpression buildBitVectorExpressionByEncoding(
      BitVectorEncoding pEncoding,
      int pNumGlobalVariables,
      ImmutableSet<BitVectorGlobalVariable> pSetBits) {

    int length = getBitVectorLengthByEncoding(pEncoding, pNumGlobalVariables);
    return switch (pEncoding) {
      case NONE -> throw new IllegalArgumentException("no bit vector encoding specified");
      case BINARY -> new BinaryBitVectorExpression(length, pSetBits);
      case HEXADECIMAL -> new HexadecimalBitVectorExpression(length, pSetBits);
      // TODO this is not so nice ...
      case SCALAR ->
          throw new IllegalArgumentException("use constructor directly for scalar bit vectors");
    };
  }

  // Vector Length =================================================================================

  public static SeqBitVectorType getTypeByLength(int pLength) {
    for (SeqBitVectorType type : SeqBitVectorType.values()) {
      if (type.size == pLength) {
        return type;
      }
    }
    throw new IllegalArgumentException("no bit vector type with given length found: " + pLength);
  }

  public static int getBitVectorLengthByEncoding(
      BitVectorEncoding pEncoding, int pNumGlobalVariables) {

    checkArgument(
        pNumGlobalVariables <= MAX_BINARY_LENGTH,
        "cannot have more than %s global variables, please disable bit vectors for this program.",
        MAX_BINARY_LENGTH);

    return switch (pEncoding) {
      case NONE -> throw new IllegalArgumentException("no bit vector encoding specified");
      case BINARY -> getBinaryLength(pNumGlobalVariables);
      case HEXADECIMAL -> convertBinaryLengthToHex(getBinaryLength(pNumGlobalVariables));
      case SCALAR -> pNumGlobalVariables;
    };
  }

  public static int getBinaryLength(int pMinLength) {
    int rLength = MIN_BINARY_LENGTH;
    while (rLength < pMinLength) {
      rLength *= 2;
    }
    assert isValidLength(rLength) : "binary bit vector length is invalid: " + rLength;
    return rLength;
  }

  public static boolean isValidLength(int pLength) {
    for (SeqBitVectorType type : SeqBitVectorType.values()) {
      if (type.size == pLength) {
        return true;
      }
    }
    return false;
  }

  // Scalar ========================================================================================

  public static CIdExpression createScalarAccessVariable(
      MPOROptions pOptions, MPORThread pThread, CVariableDeclaration pVariableDeclaration) {

    checkArgument(pVariableDeclaration.isGlobal(), "pVariableDeclaration must be global");
    // we use the original variable name here, not the substitute -> less code
    String name =
        SeqNameUtil.buildBitVectorScalarAccessVariableName(
            pOptions, pThread.id, pVariableDeclaration);
    // TODO initializer? would be best to adjust to 0/1 directly here, if possible
    CSimpleDeclaration declaration =
        SeqDeclarationBuilder.buildVariableDeclaration(
            true, SeqSimpleType.UNSIGNED_CHAR, name, SeqInitializer.INT_0);
    return SeqExpressionBuilder.buildIdExpression(declaration);
  }

  // Helpers =======================================================================================

  /** Pads the resulting hex string to pLength, e.g. 0x0 -> 0x00 for length 2. */
  public static String padHexString(int pLength, BigInteger pBigInteger) {
    return SeqStringUtil.hexFormat(pLength, pBigInteger);
  }

  public static int convertBinaryLengthToHex(int pBinaryLength) {
    return pBinaryLength / 4;
  }

  public static int convertHexLengthToBinary(int pHexLength) {
    return pHexLength * 4;
  }
}
