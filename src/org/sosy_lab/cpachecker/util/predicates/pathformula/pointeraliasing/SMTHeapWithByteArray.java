// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.util.predicates.smt.FloatingPointFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BitvectorFormulaManager;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.FloatingPointFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.FormulaType.BitvectorType;
import org.sosy_lab.java_smt.api.FormulaType.FloatingPointType;

/**
 * SMT heap representation which enables proper interaction between accesses to the same byte
 * location done using different types.
 *
 * <p>Each element is represented by the number of bytes equal to {@code
 * ceil(number_of_bits(element) / number_of_bits(byte))}.
 */
class SMTHeapWithByteArray implements SMTHeap {

  private static final int BYTE_SIZE = 8;
  private static final BitvectorType BYTE_TYPE = FormulaType.getBitvectorTypeWithSize(BYTE_SIZE);

  private final FormulaManagerView formulaManager;
  private final BitvectorFormulaManager bfmgr;
  private final FormulaType<?> pointerType;
  private final ByteOrder endianness;
  private final SMTHeap delegate;

  SMTHeapWithByteArray(
      FormulaManagerView pFormulaManager,
      FormulaType<?> pPointerType,
      MachineModel pModel,
      SMTHeap pDelegate) {
    formulaManager = pFormulaManager;
    pointerType = pPointerType;
    endianness = pModel.getEndianness();
    bfmgr = formulaManager.getBitvectorFormulaManager();
    delegate = pDelegate;
  }

  @Override
  public <I extends Formula, E extends Formula> BooleanFormula makePointerAssignment(
      String targetName,
      FormulaType<?> pTargetType,
      int oldIndex,
      int newIndex,
      final List<SMTAddressValue<I, E>> assignments) {
    if (pTargetType.isFloatingPointType()) {
      // convert floating point values to be set to bitvector and call ourselves recursively
      FloatingPointType floatTargetType = (FloatingPointType) pTargetType;
      BitvectorType bvType = FormulaType.getBitvectorTypeWithSize(floatTargetType.getTotalSize());
      FloatingPointFormulaManagerView floatMgr = formulaManager.getFloatingPointFormulaManager();
      List<SMTAddressValue<I, BitvectorFormula>> bvAssignments = new ArrayList<>();
      for (SMTAddressValue<I, E> assignment : assignments) {
        BitvectorFormula bvValue =
            floatMgr.toIeeeBitvector((FloatingPointFormula) assignment.value());
        bvAssignments.add(new SMTAddressValue<>(assignment.address(), bvValue));
      }
      return makePointerAssignment(targetName, bvType, oldIndex, newIndex, bvAssignments);

    } else if (pTargetType.isBitvectorType()) {
      // support only one assignment for simplicity, multiple assignments are only used
      // when this heap calls underlying heaps
      if (assignments.size() != 1) {
        throw new UnsupportedOperationException(
            String.format(
                "Expected exactly one pointer assignment, but %s supplied!", assignments.size()));
      }
      I address = assignments.get(0).address();
      E value = assignments.get(0).value();

      // handle in a tailored function
      BitvectorType targetType = (BitvectorType) formulaManager.getFormulaType(value);
      checkArgument(pTargetType.equals(targetType));

      FormulaType<I> addressType = formulaManager.getFormulaType(address);
      checkArgument(pointerType.equals(addressType));

      return handleBitvectorAssignment(
          targetName, oldIndex, newIndex, address, addressType, null, (BitvectorFormula) value);
    } else {
      throw new UnsupportedOperationException(
          "ByteArray Heap encoding does not support " + pTargetType);
    }
  }

  @Override
  public <I extends Formula, E extends Formula> BooleanFormula makeQuantifiedPointerAssignment(
      String targetName,
      FormulaType<?> pTargetType,
      int oldIndex,
      int newIndex,
      I address,
      BooleanFormula condition,
      E value) {
    if (pTargetType.isFloatingPointType()) {
      // convert floating point value to be set to bitvector and call ourselves recursively
      FloatingPointType floatTargetType = (FloatingPointType) pTargetType;
      BitvectorType bvType = FormulaType.getBitvectorTypeWithSize(floatTargetType.getTotalSize());
      FloatingPointFormulaManagerView floatMgr = formulaManager.getFloatingPointFormulaManager();
      BitvectorFormula bvValue = floatMgr.toIeeeBitvector((FloatingPointFormula) value);
      return makeQuantifiedPointerAssignment(
          targetName, bvType, oldIndex, newIndex, address, condition, bvValue);

    } else if (pTargetType.isBitvectorType()) {
      // handle in a tailored function
      BitvectorType targetType = (BitvectorType) formulaManager.getFormulaType(value);
      checkArgument(pTargetType.equals(targetType));

      FormulaType<I> addressType = formulaManager.getFormulaType(address);
      checkArgument(pointerType.equals(addressType));

      return handleBitvectorAssignment(
          targetName,
          oldIndex,
          newIndex,
          address,
          addressType,
          condition,
          (BitvectorFormula) value);
    } else {
      throw new UnsupportedOperationException(
          "ByteArray Heap encoding does not support " + pTargetType);
    }
  }

  @Override
  public <E extends Formula> BooleanFormula makeIdentityPointerAssignment(
      String targetName, FormulaType<E> pTargetType, int oldIndex, int newIndex) {
    // the identity assignment can be immediately handed over to the delegate
    // just make sure the array is considered to be a byte array
    return delegate.makeIdentityPointerAssignment(targetName, BYTE_TYPE, oldIndex, newIndex);
  }

  @Override
  public <I extends Formula, E extends Formula> E makePointerDereference(
      String targetName, FormulaType<E> targetType, I address) {
    if (targetType.isFloatingPointType()) {
      // call ourselves recursively with bitvector type
      FloatingPointType floatType = (FloatingPointType) targetType;
      BitvectorType bvType = FormulaType.getBitvectorTypeWithSize(floatType.getTotalSize());
      BitvectorFormula bvFormula = makePointerDereference(targetName, bvType, address);
      // convert the return value back to float formula
      FloatingPointFormulaManagerView floatMgr = formulaManager.getFloatingPointFormulaManager();
      @SuppressWarnings("unchecked")
      E floatFormula = (E) floatMgr.fromIeeeBitvector(bvFormula, floatType);
      return floatFormula;

    } else if (targetType.isBitvectorType()) {
      // handle in a tailored function
      final FormulaType<I> addressType = formulaManager.getFormulaType(address);
      checkArgument(pointerType.equals(addressType));
      BitvectorType bvTargetType = (BitvectorType) targetType;

      @SuppressWarnings("unchecked")
      E returnVal = (E) handleBitvectorDeref(targetName, null, address, addressType, bvTargetType);
      return returnVal;
    } else {
      throw new UnsupportedOperationException(
          "ByteArray Heap encoding does not support " + targetType);
    }
  }

  @Override
  public <I extends Formula, V extends Formula> V makePointerDereference(
      String targetName, FormulaType<V> targetType, int ssaIndex, I address) {
    if (targetType.isFloatingPointType()) {
      // call ourselves recursively
      FloatingPointType floatType = (FloatingPointType) targetType;
      BitvectorType bvType = FormulaType.getBitvectorTypeWithSize(floatType.getTotalSize());
      BitvectorFormula bvFormula = makePointerDereference(targetName, bvType, ssaIndex, address);
      FloatingPointFormulaManagerView floatMgr = formulaManager.getFloatingPointFormulaManager();
      @SuppressWarnings("unchecked")
      V floatFormula = (V) floatMgr.fromIeeeBitvector(bvFormula, floatType);
      return floatFormula;

    } else if (targetType.isBitvectorType()) {
      // handle in a tailored function
      final FormulaType<I> addressType = formulaManager.getFormulaType(address);
      checkArgument(pointerType.equals(addressType));
      BitvectorType bvTargetType = (BitvectorType) targetType;

      @SuppressWarnings("unchecked")
      V returnVal =
          (V) handleBitvectorDeref(targetName, ssaIndex, address, addressType, bvTargetType);
      return returnVal;
    } else {
      throw new UnsupportedOperationException(
          "ByteArray Heap encoding does not support " + targetType);
    }
  }

  private <I extends Formula> BitvectorFormula handleBitvectorDeref(
      String targetName,
      @Nullable Integer ssaIndex,
      I address,
      FormulaType<I> addressType,
      BitvectorType targetType) {
    final int bitLength = targetType.getSize();
    BitvectorFormula result;
    // TODO: make one function from the two which depend on the presence of index
    if (ssaIndex != null) {
      result = delegate.makePointerDereference(targetName, BYTE_TYPE, ssaIndex, address);
    } else {
      result = delegate.makePointerDereference(targetName, BYTE_TYPE, address);
    }

    if (bitLength < BYTE_SIZE) {
      // if the actual bit size of target type is smaller than the number of bits in byte,
      // we need to discard the high bits
      return bfmgr.extract(result, bitLength - 1, 0);
    } else if (bitLength == BYTE_SIZE) {
      // if the actual bit size of target type matches the number of bits in byte,
      // we can just return the result as-is
      return result;
    } else {
      // if the actual bit size of target type is larger than the number of bits in byte,
      // we have to read from more addresses and concatenate the parts

      // TODO: this will fail with bit-fields which contain more bits than byte
      checkArgument(
          bitLength % BYTE_SIZE == 0,
          "Bitvector size %s is not a multiple of %s!",
          bitLength,
          BYTE_SIZE);

      // result starts with first byte, loop appends the other bytes
      for (int byteOffset = 1; byteOffset < bitLength / BYTE_SIZE; byteOffset++) {
        I addressWithOffset =
            formulaManager.makePlus(address, formulaManager.makeNumber(addressType, byteOffset));
        final BitvectorFormula nextBVPart;

        // TODO: make one function from the two which depend on the presence of index
        if (ssaIndex != null) {
          nextBVPart =
              delegate.makePointerDereference(targetName, BYTE_TYPE, ssaIndex, addressWithOffset);
        } else {
          nextBVPart = delegate.makePointerDereference(targetName, BYTE_TYPE, addressWithOffset);
        }

        result =
            (endianness == ByteOrder.BIG_ENDIAN)
                ? bfmgr.concat(result, nextBVPart)
                : bfmgr.concat(nextBVPart, result);
      }
      return result;
    }
  }

  private <I extends Formula> BooleanFormula handleBitvectorAssignment(
      String targetName,
      int oldIndex,
      int newIndex,
      I address,
      FormulaType<I> addressType,
      @Nullable BooleanFormula condition,
      BitvectorFormula value) {

    // since each element is represented by a certain amount of bytes and there is no overlap, there
    // is no need to obtain the old value and merge them
    final int bitLength = bfmgr.getLength(value);

    ImmutableList<BitvectorFormula> bytes;
    if (bitLength < BYTE_SIZE) {
      BitvectorFormula oldValue =
          delegate.makePointerDereference(targetName, BYTE_TYPE, oldIndex, address);
      BitvectorFormula remainingBits = bfmgr.extract(oldValue, (BYTE_SIZE - 1), bitLength);
      bytes = ImmutableList.of(bfmgr.concat(remainingBits, value));
    } else if (bitLength == BYTE_SIZE) {
      bytes = ImmutableList.of(value);
    } else {
      bytes = splitBitvectorToBytes(value);
    }

    // store the value in delegate byte-by-byte
    // make just one delegate assignment so that we do not need to use multiple SSA indices
    ImmutableList.Builder<SMTAddressValue<I, BitvectorFormula>> builder = ImmutableList.builder();
    int byteOffset = 0;
    for (BitvectorFormula byteValue : bytes) {
      I addressWithOffset =
          formulaManager.makePlus(address, formulaManager.makeNumber(addressType, byteOffset++));
      builder.add(new SMTAddressValue<>(addressWithOffset, byteValue));
    }

    if (condition != null) {
      // TODO: support quantified assignments with byte heap
      throw new UnsupportedOperationException(
          String.format("Byte heap does not support quantified assignments!"));
    }
    BooleanFormula assignmentFormula =
        delegate.makePointerAssignment(targetName, BYTE_TYPE, oldIndex, newIndex, builder.build());

    return assignmentFormula;
  }

  private <I extends BitvectorFormula> ImmutableList<BitvectorFormula> splitBitvectorToBytes(
      I bitvector) {
    final int bitLength = bfmgr.getLength(bitvector);
    // TODO: this will fail with bit-fields which contain more bits than byte
    checkArgument(
        bitLength % BYTE_SIZE == 0,
        "Bitvector size %s is not a multiple of %s!",
        bitLength,
        BYTE_SIZE);
    ImmutableList.Builder<BitvectorFormula> builder = ImmutableList.builder();
    for (int bitOffset = 0; bitOffset < bitLength; bitOffset += BYTE_SIZE) {
      builder.add(bfmgr.extract(bitvector, bitOffset + (BYTE_SIZE - 1), bitOffset));
    }
    return (endianness == ByteOrder.BIG_ENDIAN) ? builder.build().reverse() : builder.build();
  }
}
