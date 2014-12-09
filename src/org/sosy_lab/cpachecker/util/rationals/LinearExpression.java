package org.sosy_lab.cpachecker.util.rationals;

import java.util.Iterator;
import java.util.Map.Entry;

import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;

/**
 * Simple <i>sparse</i> implementation for <i>homogeneous</i> linear expression
 * of the form $\Sigma a_i  x_i$, where $x_i$ is a set of variables and $a_i$
 * is a set of constants.
 *
 * Every constant stored has to have a non-zero value.
 */
public class LinearExpression implements Iterable<Entry<String, Rational>> {
  private final PersistentMap<String, Rational> data;
  private int hashCache = 0;

  private LinearExpression(PersistentMap<String, Rational> data) {
    this.data = data;
  }

  public static LinearExpression empty() {
    return new LinearExpression(
        PathCopyingPersistentTreeMap.<String, Rational>of());
  }

  public static LinearExpression pair(String var, Rational coeff) {
    if (coeff.equals(Rational.ZERO)) {
      return empty();
    }
    return new LinearExpression(
        PathCopyingPersistentTreeMap.<String, Rational>of().
            putAndCopy(var, coeff)
    );
  }

  public static LinearExpression ofVariable(String var) {
    return LinearExpression.pair(var, Rational.ONE);
  }

  /**
   * Add {@code other} linear expression.
   */
  public LinearExpression add(LinearExpression other) {
    PersistentMap<String, Rational> newData = data;
    for (Entry<String, Rational> e : other.data.entrySet()) {
      String var = e.getKey();
      Rational oldValue = newData.get(var);
      Rational newValue = e.getValue();
      if (oldValue != null) {
        newValue = newValue.plus(oldValue);
      }
      if (newValue.equals(Rational.ZERO)) {
        newData = newData.removeAndCopy(var);
      } else {
        newData = newData.putAndCopy(var, newValue);
      }
    }
    return new LinearExpression(newData);
  }

  /**
   * Subtract {@code other} linear expression.
   */
  public LinearExpression sub(LinearExpression other) {
    return add(other.negate());
  }

  /**
   * Multiply the linear expression by {@code constant}.
   */
  public LinearExpression multByConst(Rational constant) {
    PersistentMap<String, Rational> newData =
        PathCopyingPersistentTreeMap.of();
    if (constant.equals(Rational.ZERO)) {
      return empty();
    }
    for (Entry<String, Rational> e : data.entrySet()) {
      newData = newData.putAndCopy(e.getKey(), e.getValue().times(constant));
    }
    return new LinearExpression(newData);
  }
  /**
   * Negate the linear expression.
   */
  public LinearExpression negate() {
    return multByConst(Rational.NEG_ONE);
  }

  public Rational getCoeff(String variable) {
    Rational out = data.get(variable);
    if (out == null) {
      return Rational.ZERO;
    }
    return out;
  }

  /**
   * @return Number of variables with non-zero coefficients.
   */
  public int size() {
    return data.size();
  }

  /**
   * @return Whether all coefficients are integral.
   */
  public boolean isIntegral() {
    for (Entry<String, Rational> e : this) {
      if (!e.getValue().isIntegral()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public Iterator<Entry<String, Rational>> iterator() {
    return data.entrySet().iterator();
  }

  /**
   * @return Pretty-printing for linear expressions.
   * E. g. <i>-x + 2y + z</i>
   */
  @Override
  public String toString() {
    StringBuilder b = new StringBuilder();
    for (Entry<String, Rational> monomial : this) {
      if (b.length() != 0) {
        b.append(" + ");
      }
      Rational coeff = monomial.getValue();
      String var = monomial.getKey();
      if (coeff == Rational.ONE) {
        b.append(var);
      } else if (coeff == Rational.NEG_ONE) {
        b.append("-").append(var);
      } else {
        b.append(coeff.toString()).append(var);
      }
    }
    return b.toString();
  }

  @Override
  public boolean equals(Object object) {
    if (object == this) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (object.getClass() != this.getClass()) {
      return false;
    }
    LinearExpression other = (LinearExpression) object;
    return data.equals(other.data);
  }

  @Override
  public int hashCode() {
    // Caching the hashing procedure.
    if (hashCache == 0) {
      hashCache = data.hashCode();
    }

    // Safe to do so, since we are immutable.
    return hashCache;
  }
}
