// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.counterexample;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.CFA;

/**
 * This class represent the memory of a concrete State. It contains a map of values, and the
 * addresses, which contain the location the values are stored at in the memory. It also contains
 * the name of the memory. Each concrete State can hold a finite amount of memories with different
 * names.
 *
 * <p>The addresses the values are stored at have to match the types of the assignments stored in
 * the {@link CFA} bit precisely.
 *
 * <p>e.g: int a[5] = {1,2,3}; may be represented as {0 = 1, 4 = 2, 8 = 3}.
 */
public class Memory {

  private final String name;
  private final Map<Address, Object> values;

  public Memory(String pName, Map<Address, Object> pValues) {
    name = pName;
    values = ImmutableMap.copyOf(pValues);
  }

  public String getName() {
    return name;
  }

  /**
   * Checks, if the value at the given address is defined.
   *
   * @param address Check if value is defined at this address.
   * @return returns true, if the value is defined, false otherwise.
   */
  public boolean hasValue(Address address) {
    return values.containsKey(address);
  }

  /**
   * Return the value stored at the given address.
   *
   * @param address the address
   * @return Returns the value stored at the given address.
   */
  public Object getValue(Address address) {
    Preconditions.checkArgument(hasValue(address));
    return values.get(address);
  }

  @Override
  public String toString() {
    return "Memory name=" + name + " , " + " values=" + values;
  }
}
