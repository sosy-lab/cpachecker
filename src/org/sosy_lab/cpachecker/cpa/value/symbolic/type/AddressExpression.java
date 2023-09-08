// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.symbolic.type;

import com.google.common.base.Preconditions;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.smg2.SMGState;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGException;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * Represents the inner part of a pointer i.e. ptr + 3. The idea is that this class models the
 * address always as + offset. This does not make use of the MemoryLocation (so it is null!). The
 * idea is that the addressValue maps somehow to a memory location (representing the address
 * essentially). The type helps evaluating/using the address.
 */
public final class AddressExpression extends SymbolicExpression {

  private static final long serialVersionUID = -1498889385306613159L;

  // The address Value should map to memory
  private final Value addressValue;
  private final Type addressType;

  // The offset may be any Value, but numeric values are prefered
  private final Value offset;

  private AddressExpression(Value pAddress, Type pAddressType, Value pOffsetValue) {
    Preconditions.checkNotNull(pAddress);
    Preconditions.checkNotNull(pAddressType);
    Preconditions.checkNotNull(pOffsetValue);
    addressValue = pAddress;
    addressType = pAddressType;
    offset = pOffsetValue;
  }

  // TODO: add of/withZeroOffset etc. with state
  private AddressExpression(
      Value pAddress, Type pAddressType, Value pOffsetValue, AbstractState pAbstractState) {
    super(pAbstractState);
    Preconditions.checkNotNull(pAddress);
    Preconditions.checkNotNull(pAddressType);
    Preconditions.checkNotNull(pOffsetValue);
    addressValue = pAddress;
    addressType = pAddressType;
    offset = pOffsetValue;
  }

  public static AddressExpression of(Value pAddress, Type pAddressType, Value pOffsetValue) {
    return new AddressExpression(pAddress, pAddressType, pOffsetValue);
  }

  public static AddressExpression withZeroOffset(Value pAddress, Type pType) {
    return new AddressExpression(pAddress, pType, new NumericValue(0));
  }

  public AddressExpression copyWithNewOffset(Value pOffsetValue) {
    return new AddressExpression(addressValue, addressType, pOffsetValue);
  }

  @Override
  public String getRepresentation() {
    return toString();
  }

  public Value getMemoryAddress() {
    return addressValue;
  }

  public Value getOffset() {
    return offset;
  }

  @Override
  public Type getType() {
    return addressType;
  }

  @Override
  public SymbolicExpression copyForLocation(MemoryLocation pRepresentedLocation) {
    return null;
  }

  @Override
  public SymbolicExpression copyForState(AbstractState pCurrentState) {
    return new AddressExpression(addressValue, addressType, offset, pCurrentState);
  }

  @Override
  public <VisitorReturnT> VisitorReturnT accept(SymbolicValueVisitor<VisitorReturnT> pVisitor) {
    return null;
  }

  @Override
  public boolean isTrivial() {
    return offset instanceof SymbolicExpression;
  }

  @Override
  public String toString() {
    if (addressType != null) {
      return "Address " + addressValue + " at offset: " + offset + " | type: " + addressType;
    }
    return "Address " + addressValue + " at offset " + offset;
  }

  @Override
  public boolean equals(Object o) {
    if (this.hasAbstractState()
        && o instanceof AddressExpression
        && ((AddressExpression) o).hasAbstractState()) {
      if (this.getAbstractState() instanceof SMGState
          && ((AddressExpression) o).getAbstractState() instanceof SMGState) {
        try {
          // Precondition as this should never fail in SMGs
          Preconditions.checkArgument(this.getOffset().equals(((AddressExpression) o).getOffset()));
          // SMG values have the offset baked into them. Only the SMG truly knows equality for them
          return SMGState.areValuesEqual(
              (SMGState) this.getAbstractState(),
              addressValue,
              (SMGState) ((AddressExpression) o).getAbstractState(),
              ((AddressExpression) o).addressValue);
        } catch (SMGException pE) {
          throw new RuntimeException(pE);
        }
      }
    }
    return super.equals(o);
  }
}
