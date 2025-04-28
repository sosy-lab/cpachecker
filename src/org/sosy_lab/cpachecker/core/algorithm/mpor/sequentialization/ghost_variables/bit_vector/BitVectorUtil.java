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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.math.BigInteger;
import java.util.Objects;
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

  public static final int MIN_BINARY_LENGTH = BitVectorDataType.__UINT8_T.size;
  public static final int MAX_BINARY_LENGTH = BitVectorDataType.__UINT64_T.size;

  // Creation ======================================================================================

  public static BitVectorExpression buildZeroBitVector(
      MPOROptions pOptions, int pNumGlobalVariables) {

    checkArgument(
        !pOptions.porBitVectorEncoding.equals(BitVectorEncoding.NONE),
        "no bit vector encoding specified");
    return buildBitVectorExpressionByEncoding(
        pOptions.porBitVectorEncoding, pNumGlobalVariables, ImmutableSet.of());
  }

  public static BitVectorExpression buildBitVectorExpression(
      MPOROptions pOptions,
      @NonNull ImmutableMap<CVariableDeclaration, Integer> pAllVariables,
      @NonNull ImmutableList<CVariableDeclaration> pAccessedVariables) {

    checkArgument(
        !pOptions.porBitVectorEncoding.equals(BitVectorEncoding.NONE),
        "no bit vector encoding specified");
    checkArgument(
        pAllVariables.keySet().containsAll(pAccessedVariables),
        "pAllVariables must contain all pAccessedVariables as keys.");

    // retrieve all variable ids from pAllVariables that are in pAccessedVariables
    ImmutableSet<Integer> setBits =
        pAccessedVariables.stream()
            .map(pAllVariables::get)
            .filter(Objects::nonNull)
            .collect(ImmutableSet.toImmutableSet());
    return buildBitVectorExpressionByEncoding(
        pOptions.porBitVectorEncoding, pAllVariables.size(), setBits);
  }

  private static BitVectorExpression buildBitVectorExpressionByEncoding(
      BitVectorEncoding pEncoding, int pNumGlobalVariables, ImmutableSet<Integer> pSetBits) {

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

  public static BitVectorDataType getDataTypeByLength(int pLength) {
    for (BitVectorDataType type : BitVectorDataType.values()) {
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
    for (BitVectorDataType type : BitVectorDataType.values()) {
      if (type.size == pLength) {
        return true;
      }
    }
    return false;
  }

  // Scalar ========================================================================================

  public static CIdExpression createScalarAccessVariable(
      MPOROptions pOptions,
      MPORThread pThread,
      CVariableDeclaration pVariableDeclaration,
      BitVectorAccessType pAccessType) {

    checkArgument(pVariableDeclaration.isGlobal(), "pVariableDeclaration must be global");
    // we use the original variable name here, not the substitute -> less code
    String name =
        getBitVectorScalarVariableNameByAccessType(
            pOptions, pThread.id, pVariableDeclaration, pAccessType);
    // TODO initializer? would be best to adjust to 0/1 directly here, if possible
    CSimpleDeclaration declaration =
        SeqDeclarationBuilder.buildVariableDeclaration(
            true, SeqSimpleType.UNSIGNED_CHAR, name, SeqInitializer.INT_0);
    return SeqExpressionBuilder.buildIdExpression(declaration);
  }

  private static String getBitVectorScalarVariableNameByAccessType(
      MPOROptions pOptions,
      int pThreadId,
      CVariableDeclaration pDeclaration,
      BitVectorAccessType pAccessType) {

    return switch (pAccessType) {
      case NONE ->
          throw new IllegalArgumentException(
              "cannot create bit vector variable name for access type none");
      case ACCESS ->
          SeqNameUtil.buildBitVectorScalarAccessVariableName(pOptions, pThreadId, pDeclaration);
      case READ ->
          SeqNameUtil.buildBitVectorScalarReadVariableName(pOptions, pThreadId, pDeclaration);
      case WRITE ->
          SeqNameUtil.buildBitVectorScalarWriteVariableName(pOptions, pThreadId, pDeclaration);
    };
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
