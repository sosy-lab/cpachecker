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
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.SMTMultipleAssignmentHeap.SMTAddressValue;
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
  private final SMTMultipleAssignmentHeap delegate;

  SMTHeapWithByteArray(
      FormulaManagerView pFormulaManager,
      FormulaType<?> pPointerType,
      MachineModel pModel,
      SMTMultipleAssignmentHeap pDelegate) {
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
      I address,
      E value) {
    if (pTargetType.isFloatingPointType()) {
      // convert floating point values to be set to bitvector and call ourselves recursively
      FloatingPointType floatTargetType = (FloatingPointType) pTargetType;
      BitvectorType bvType = FormulaType.getBitvectorTypeWithSize(floatTargetType.getTotalSize());
      FloatingPointFormulaManagerView floatMgr = formulaManager.getFloatingPointFormulaManager();
      BitvectorFormula bvValue = floatMgr.toIeeeBitvector((FloatingPointFormula) value);
      return makePointerAssignment(targetName, bvType, oldIndex, newIndex, address, bvValue);
    } else if (pTargetType.isBitvectorType()) {
      // handle in a tailored function
      BitvectorType targetType = (BitvectorType) formulaManager.getFormulaType(value);
      checkArgument(pTargetType.equals(targetType));

      FormulaType<I> addressType = formulaManager.getFormulaType(address);
      checkArgument(pointerType.equals(addressType));

      return handleBitvectorAssignment(
          targetName, oldIndex, newIndex, addressType, null, address, (BitvectorFormula) value);
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
      BooleanFormula condition,
      I address,
      E value) {
    if (pTargetType.isFloatingPointType()) {
      // convert floating point value to be set to bitvector and call ourselves recursively
      FloatingPointType floatTargetType = (FloatingPointType) pTargetType;
      BitvectorType bvType = FormulaType.getBitvectorTypeWithSize(floatTargetType.getTotalSize());
      FloatingPointFormulaManagerView floatMgr = formulaManager.getFloatingPointFormulaManager();
      BitvectorFormula bvValue = floatMgr.toIeeeBitvector((FloatingPointFormula) value);

      return makeQuantifiedPointerAssignment(
          targetName, bvType, oldIndex, newIndex, condition, address, bvValue);
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
          addressType,
          condition,
          address,
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
    assert (bitLength > 0);

    // compute the number of bytes, even partially filled ones
    final int numPartialBytes = (bitLength / BYTE_SIZE) + ((bitLength % BYTE_SIZE) != 0 ? 1 : 0);

    // the result is a null at first, it will be assigned to by the first partial byte
    BitvectorFormula result = null;

    // iterate through all partial bytes
    for (int partialByte = 0; partialByte < numPartialBytes; partialByte++) {
      // add the byte offset to address
      I addressWithOffset =
          formulaManager.makePlus(address, formulaManager.makeNumber(addressType, partialByte));

      // get the partial byte formula
      BitvectorFormula partialByteFormula;

      // TODO: make one function from the two which depend on the presence of index
      if (ssaIndex != null) {
        partialByteFormula =
            delegate.makePointerDereference(targetName, BYTE_TYPE, ssaIndex, addressWithOffset);
      } else {
        partialByteFormula =
            delegate.makePointerDereference(targetName, BYTE_TYPE, addressWithOffset);
      }

      // discard the part after the target type length if necessary
      final int partialByteBitOffset = partialByte * BYTE_SIZE;
      final int partialByteMaxLength = bitLength - partialByteBitOffset;
      assert (partialByteMaxLength > 0);
      if (partialByteMaxLength < BYTE_SIZE) {
        partialByteFormula = bfmgr.extract(partialByteFormula, partialByteMaxLength - 1, 0);
      }

      // concatenate with result
      if (result != null) {
        result =
            (endianness == ByteOrder.BIG_ENDIAN)
                ? bfmgr.concat(result, partialByteFormula)
                : bfmgr.concat(partialByteFormula, result);
      } else {
        result = partialByteFormula;
      }
    }
    assert (result != null);

    return result;
  }

  private <I extends Formula> BooleanFormula handleBitvectorAssignment(
      String targetName,
      int oldIndex,
      int newIndex,
      FormulaType<I> addressType,
      @Nullable BooleanFormula condition,
      I address,
      BitvectorFormula value) {

    if (condition != null) {
      // TODO: support quantified assignments with byte heap
      throw new UnsupportedOperationException("Byte heap does not support quantified assignments!");
    }

    // make just one delegate assignment so that we do not need to use multiple SSA indices
    ImmutableList.Builder<SMTAddressValue<I, BitvectorFormula>> byteAssignmentsBuilder =
        ImmutableList.builder();

    // split the element into bytes
    final int bitLength = bfmgr.getLength(value);
    assert (bitLength > 0);

    // compute the number of bytes, even partially filled ones
    final int numPartialBytes = (bitLength / BYTE_SIZE) + ((bitLength % BYTE_SIZE) != 0 ? 1 : 0);

    for (int partialByte = 0; partialByte < numPartialBytes; partialByte++) {

      I addressWithOffset =
          formulaManager.makePlus(address, formulaManager.makeNumber(addressType, partialByte));

      // compute partial byte offset and max length
      final int partialByteBitOffset = partialByte * BYTE_SIZE;
      final int partialByteMaxLength = bitLength - partialByteBitOffset;
      assert (partialByteMaxLength > 0);

      final BitvectorFormula partialByteFormula;

      if (partialByteMaxLength >= BYTE_SIZE) {
        // we can extract the whole byte from the supplied value
        partialByteFormula =
            bfmgr.extract(value, partialByteBitOffset + (BYTE_SIZE - 1), partialByteBitOffset);
      } else {
        // we need to fetch the high byts from old value
        BitvectorFormula newBits =
            bfmgr.extract(
                value, partialByteBitOffset + (partialByteMaxLength - 1), partialByteBitOffset);

        BitvectorFormula oldByteFormula =
            delegate.makePointerDereference(targetName, BYTE_TYPE, oldIndex, addressWithOffset);

        BitvectorFormula oldBits =
            bfmgr.extract(oldByteFormula, BYTE_SIZE - 1, partialByteMaxLength);

        // concat is high-then-low
        partialByteFormula = bfmgr.concat(oldBits, newBits);
      }

      // add assignment

      byteAssignmentsBuilder.add(new SMTAddressValue<>(addressWithOffset, partialByteFormula));
    }

    // delegate
    BooleanFormula assignmentFormula =
        delegate.<I, BitvectorFormula>makePointerAssignments(
            targetName, BYTE_TYPE, oldIndex, newIndex, byteAssignmentsBuilder.build());

    return assignmentFormula;
  }
}
