// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.taint;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

public class TaintState implements AbstractState {

  private static final TaintState NO_INPUT = new TaintState(ImmutableSet.of());

  private final Set<String> inputs;

  private TaintState(Set<String> pInputs) {
    inputs = ImmutableSet.copyOf(pInputs);
  }

  public Set<String> getInputs() {
    return inputs;
  }

  @Override
  public String toString() {
    return inputs.toString();
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    if (pOther instanceof TaintState) {
      TaintState other = (TaintState) pOther;
      return inputs.equals(other.inputs);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return inputs.hashCode();
  }

  public static TaintState empty() {
    return NO_INPUT;
  }

  public static TaintState forInputs(Set<String> pInputs) {
    if (pInputs.isEmpty()) {
      return empty();
    }
    return new TaintState(pInputs);
  }

}
