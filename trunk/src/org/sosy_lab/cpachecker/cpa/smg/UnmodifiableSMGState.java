// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGAddressValueAndState;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMGPredicateRelation;
import org.sosy_lab.cpachecker.cpa.smg.graphs.UnmodifiableCLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGRegion;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownExpValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownSymbolicValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGSymbolicValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;
import org.sosy_lab.cpachecker.cpa.smg.util.PersistentBiMap;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * A view on a CLangSMG, where no modifications are allowed.
 *
 * <p>All returned Collections are unmodifiable.
 */
public interface UnmodifiableSMGState extends LatticeAbstractState<UnmodifiableSMGState> {

  /**
   * Returns mutable instance of subclass. Changes to the returned instance are independent of this
   * immutable instance and do not change it.
   */
  SMGState copyOf();

  /**
   * Returns mutable instance of subclass, with the given SMG and ExplicitValues. Changes to the
   * returned instance are independent of this immutable instance and do not change it.
   */
  SMGState copyWith(
      CLangSMG pSmg, PersistentBiMap<SMGKnownSymbolicValue, SMGKnownExpValue> pValues);

  /**
   * Returns mutable instance of subclass, with the flag for blockEnd. Changes to the returned
   * instance are independent of this immutable instance and do not change it.
   */
  SMGState copyWithBlockEnd(boolean isBlockEnd);

  int getId();

  int getPredecessorId();

  /** returns an unmodifiable view of the heap. */
  UnmodifiableCLangSMG getHeap();

  String toDot(String pName, String pLocation);

  String getErrorDescription();

  /**
   * Marks that an invalid read operation was performed on this smgState.
   *
   * @return a new state with the corresponding violation property.
   */
  UnmodifiableSMGState withInvalidRead();

  /**
   * Marks that an invalid write operation was performed on this smgState.
   *
   * @return a new state with the corresponding violation property.
   */
  UnmodifiableSMGState withInvalidWrite();

  /**
   * Signals an invalid free call.
   *
   * @return a new state with the corresponding violation property.
   */
  UnmodifiableSMGState withInvalidFree();

  /**
   * Signals a dereference of a pointer or array which could not be resolved.
   *
   * @return a new state with the corresponding violation property.
   */
  UnmodifiableSMGState withUnknownDereference();

  /**
   * Copies the violated properties from one state into another (new) state.
   *
   * @return a new state with the corresponding violation properties.
   */
  SMGState withViolationsOf(SMGState pOther);

  /**
   * Stores a error-message in the state.
   *
   * @return a new state with the corresponding error description.
   */
  UnmodifiableSMGState withErrorDescription(String pErrorDescription);

  List<SMGAddressValueAndState> getPointerFromValue(SMGValue pValue)
      throws SMGInconsistentException;

  boolean isBlockEnded();

  Set<Entry<SMGKnownSymbolicValue, SMGKnownExpValue>> getExplicitValues();

  @Override
  UnmodifiableSMGState join(UnmodifiableSMGState reachedState) throws SMGInconsistentException;

  @Override
  boolean isLessOrEqual(UnmodifiableSMGState reachedState) throws SMGInconsistentException;

  /**
   * Get the symbolic value, that represents the address pointing to the given memory with the given
   * offset, if it exists.
   *
   * @param memory get address belonging to this memory.
   * @param offset get address with this offset relative to the beginning of the memory.
   * @return Address of the given field, or NULL, if such an address does not yet exist in the SMG.
   */
  @Nullable
  default SMGSymbolicValue getAddress(SMGRegion memory, long offset) {
    return getAddress(memory, offset, null);
  }

  /**
   * Get the symbolic value, that represents the address pointing to the given memory with the given
   * offset, if it exists.
   *
   * @param memory get address belonging to this memory.
   * @param offset get address with this offset relative to the beginning of the memory.
   * @return Address of the given field, or NULL, if such an address does not yet exist in the SMG.
   */
  @Nullable SMGSymbolicValue getAddress(SMGObject memory, long offset, SMGTargetSpecifier tg);

  Collection<Object> getInvalidChain();

  Collection<Object> getCurrentChain();

  boolean isTrackPredicatesEnabled();

  SMGPredicateRelation getPathPredicateRelation();

  SMGPredicateRelation getErrorPredicateRelation();

  boolean isExplicit(SMGValue value);

  SMGKnownExpValue getExplicit(SMGValue pKey);

  boolean hasMemoryErrors();

  boolean hasMemoryLeaks();

  boolean areNonEqual(SMGValue pValue1, SMGValue pValue2);

  SMGObject getObjectForFunction(CFunctionDeclaration pDeclaration);

  Map<MemoryLocation, SMGRegion> getStackVariables();
}
