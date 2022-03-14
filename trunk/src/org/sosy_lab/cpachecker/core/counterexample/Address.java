// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.counterexample;

import com.google.common.math.DoubleMath;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.sosy_lab.common.rationals.ExtendedRational;
import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.cpachecker.cfa.ast.ALeftHandSide;

/**
 * The address of a left hand side expression {@link ALeftHandSide}. May be symbolic or concrete.
 * Addresses are preferably used to get values of variables that are stored in the {@link
 * ConcreteState} state.
 */
public abstract class Address {

  private Address() {} // for visibility

  /**
   * Adds a concrete value to the address. Is for example used in pointer arithmetic in C. This
   * method may only be called if the address is concrete.
   *
   * @param pOffset the value that is added on the address. May be negative.
   * @return returns the resulting address when adding this address with the given offset.
   */
  public abstract Address addOffset(BigInteger pOffset);

  /**
   * Adds a concrete value to the address. Is for example used in pointer arithmetic in C. This
   * method may only be called if the address is concrete.
   *
   * @param pOffset the value that is added on the address. May be negative. If the value is no
   *     integer, a unknown address will be returned.
   * @return returns the resulting address when adding this address with the given offset or an
   *     unknown address, when the given offset is not an integer.
   */
  public abstract Address addOffset(BigDecimal pOffset);

  @Override
  public abstract String toString();

  @Override
  public abstract int hashCode();

  @Override
  public abstract boolean equals(Object obj);

  /** Returns returns true if the address is unknown, else false. */
  public abstract boolean isUnknown();

  /** Returns returns true if the address is symbolic, else false. */
  public abstract boolean isSymbolic();

  /** Returns returns true if the address is concrete, else false. */
  public abstract boolean isConcrete();

  /**
   * Returns the concrete value of the address. May only be called on concrete addresses.
   *
   * @return Returns the concrete value of the address.
   */
  public abstract BigInteger getAddressValue();

  /**
   * Returns a string representation of the address to be used as comment for the User. May NOT
   * return a integer value.
   *
   * @return a string representation of the address.
   */
  public abstract String getCommentRepresentation();

  /**
   * Returns an address for the given address representation. If the representation can be exactly
   * converted to an {@link BigInteger} integer, then an concrete address with the given value will
   * be returned.
   *
   * <p>In all other cases an symbolic address will be returned with the given object as symbol.
   *
   * @param pAddress the representation of the address, either a concrete value like an BigInteger
   *     or a symbol like a String.
   * @return Returns an address for the given address representation
   */
  public static Address valueOf(Object pAddress) {

    if (pAddress instanceof Address) {
      return (Address) pAddress;
    } else if (pAddress instanceof BigInteger) {
      return new ConcreteAddress((BigInteger) pAddress);
    } else if (pAddress instanceof Rational) {
      Rational rational = (Rational) pAddress;

      if (rational.isIntegral()) {
        return new ConcreteAddress(rational.getNum());
      }
    } else if (pAddress instanceof ExtendedRational) {
      ExtendedRational eRat = (ExtendedRational) pAddress;

      if (eRat.isRational()) {
        Rational rat = eRat.getRational();
        if (rat.isIntegral()) {
          return new ConcreteAddress(rat.getNum());
        }
      }
    } else if (pAddress instanceof Byte) {
      long value = ((Byte) pAddress).longValue();
      return new ConcreteAddress(BigInteger.valueOf(value));
    } else if (pAddress instanceof Integer) {
      long value = ((Integer) pAddress).longValue();
      return new ConcreteAddress(BigInteger.valueOf(value));
    } else if (pAddress instanceof Long) {
      long value = ((Long) pAddress);
      return new ConcreteAddress(BigInteger.valueOf(value));
    } else if (pAddress instanceof Short) {
      long value = ((Short) pAddress).longValue();
      return new ConcreteAddress(BigInteger.valueOf(value));
    } else if (pAddress instanceof Double) {
      Double value = (Double) pAddress;
      if (DoubleMath.isMathematicalInteger(value)) {
        long dValue = value.longValue();
        return new ConcreteAddress(BigInteger.valueOf(dValue));
      }
    } else if (pAddress instanceof Float) {
      Float value = (Float) pAddress;
      if (DoubleMath.isMathematicalInteger(value)) {
        long dValue = value.longValue();
        return new ConcreteAddress(BigInteger.valueOf(dValue));
      }
    } else if (pAddress instanceof BigDecimal) {
      BigDecimal bdVal = (BigDecimal) pAddress;

      try {
        BigInteger bigIntValue = bdVal.toBigIntegerExact();
        return new ConcreteAddress(bigIntValue);
      } catch (ArithmeticException e) {
        // This double cannot be represented as integer,
        // represent it as symbolic Address instead
        return new SymbolicAddress(pAddress);
      }
    }

    return new SymbolicAddress(pAddress);
  }

  /**
   * Returns a instance that represents an unknown address.
   *
   * @return Returns a instance that represents an unknown address.
   */
  public static Address getUnknownAddress() {
    return UnknownAddress.getInstance();
  }

  private static class ConcreteAddress extends Address {

    private final BigInteger addressValue;

    private ConcreteAddress(BigInteger pAddressValue) {
      addressValue = pAddressValue;
    }

    @Override
    public Address addOffset(BigInteger pOffset) {
      BigInteger newAddressValue = addressValue.add(pOffset);
      return Address.valueOf(newAddressValue);
    }

    @Override
    public Address addOffset(BigDecimal pOffset) {
      BigInteger offset;

      try {
        offset = pOffset.toBigIntegerExact();
      } catch (ArithmeticException e) {
        return getUnknownAddress();
      }

      BigInteger newAddressValue = addressValue.add(offset);
      return Address.valueOf(newAddressValue);
    }

    @Override
    public String toString() {
      return "<Concrete address : value " + addressValue + ">";
    }

    @Override
    public int hashCode() {
      return addressValue.hashCode();
    }

    @Override
    public boolean equals(Object pObj) {

      if (pObj instanceof ConcreteAddress) {
        return addressValue.equals(((ConcreteAddress) pObj).addressValue);
      }

      return false;
    }

    @Override
    public boolean isUnknown() {
      return false;
    }

    @Override
    public boolean isSymbolic() {
      return false;
    }

    @Override
    public boolean isConcrete() {
      return true;
    }

    @Override
    public BigInteger getAddressValue() {
      return addressValue;
    }

    @Override
    public String getCommentRepresentation() {
      return addressValue.toString();
    }
  }

  private static class UnknownAddress extends Address {

    private static final UnknownAddress instance = new UnknownAddress();

    private UnknownAddress() {}

    private static UnknownAddress getInstance() {
      return instance;
    }

    @Override
    public Address addOffset(BigInteger pOffset) {
      throw new UnsupportedOperationException("Can't use pointer arithmetic on unkown address.");
    }

    @Override
    public Address addOffset(BigDecimal pOffset) {
      throw new UnsupportedOperationException("Can't use pointer arithmetic on unkown address.");
    }

    @Override
    public String toString() {
      return "Unknown address";
    }

    @Override
    public int hashCode() {
      throw new UnsupportedOperationException("Can't compare unknown addresses.");
    }

    @Override
    @SuppressFBWarnings("EQ_UNUSUAL")
    public boolean equals(Object pObj) {
      throw new UnsupportedOperationException("Can't compare unknown addresses.");
    }

    @Override
    public boolean isUnknown() {
      return true;
    }

    @Override
    public boolean isSymbolic() {
      return false;
    }

    @Override
    public boolean isConcrete() {
      return false;
    }

    @Override
    public BigInteger getAddressValue() {
      throw new UnsupportedOperationException("Value of this unknown address is not known.");
    }

    @Override
    public String getCommentRepresentation() {
      return "unknown";
    }
  }

  private static class SymbolicAddress extends Address {

    private final Object symbolicAddress;

    private SymbolicAddress(Object pSymbolicAddress) {
      symbolicAddress = pSymbolicAddress;
    }

    @Override
    public Address addOffset(BigInteger pOffset) {
      throw new UnsupportedOperationException("Can't use pointer arithmetic on symbolic address.");
    }

    @Override
    public Address addOffset(BigDecimal pOffset) {
      throw new UnsupportedOperationException("Can't use pointer arithmetic on symbolic address.");
    }

    @Override
    public String toString() {
      return "<Symbolic address :" + symbolicAddress + ">";
    }

    @Override
    public int hashCode() {
      return symbolicAddress.hashCode();
    }

    @Override
    public boolean equals(Object pObj) {

      if (pObj instanceof SymbolicAddress) {
        return symbolicAddress.equals(((SymbolicAddress) pObj).symbolicAddress);
      }

      return false;
    }

    @Override
    public boolean isUnknown() {
      return false;
    }

    @Override
    public boolean isSymbolic() {
      return true;
    }

    @Override
    public boolean isConcrete() {
      return false;
    }

    @Override
    public BigInteger getAddressValue() {
      throw new UnsupportedOperationException("Value of this symbolic address is not known.");
    }

    @Override
    public String getCommentRepresentation() {
      return symbolicAddress.toString();
    }
  }
}
