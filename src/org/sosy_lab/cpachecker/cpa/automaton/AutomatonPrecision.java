/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.automaton;

import java.util.Set;

import org.sosy_lab.cpachecker.core.interfaces.Precision;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

public class AutomatonPrecision implements Precision {

  private ImmutableSet<SafetyProperty> blacklist = ImmutableSet.of();

  private AutomatonPrecision(Set<SafetyProperty> pBlacklist) {
    this.blacklist = ImmutableSet.copyOf(pBlacklist);
  }

  public static AutomatonPrecision emptyBlacklist() {
    return new AutomatonPrecision(ImmutableSet.<SafetyProperty>of());
  }

  public static AutomatonPrecision initBlacklist(Set<SafetyProperty> pBlacklist) {
    return new AutomatonPrecision(pBlacklist);
  }

  public AutomatonPrecision cloneAndAddBlacklisted(Set<SafetyProperty> pProperty) {
    return new AutomatonPrecision(
        ImmutableSet.<SafetyProperty>builder().addAll(this.blacklist).addAll(pProperty).build());
  }

  public ImmutableSet<SafetyProperty> getBlacklist() {
    return blacklist;
  }

  @Override
  public String toString() {
    return blacklist.toString();
  }

  /**
   * The join of two automata precisions with property
   *  blacklists is special.
   *
   * The resulting precision blacklists the intersection of the input blacklists.
   *  (which is the union of the whitelists).
   */
  @Override
  public Precision join(Precision pOther) {
    Preconditions.checkArgument(pOther instanceof AutomatonPrecision);
    AutomatonPrecision other = (AutomatonPrecision) pOther;

    return new AutomatonPrecision(ImmutableSet.<SafetyProperty>copyOf(
        Sets.intersection(this.blacklist, other.blacklist)));
  }


}
