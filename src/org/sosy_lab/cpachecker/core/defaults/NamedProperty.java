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
package org.sosy_lab.cpachecker.core.defaults;

import java.util.Set;

import org.sosy_lab.cpachecker.core.interfaces.Property;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;


public final class NamedProperty implements Property {

  private final String text;

  private NamedProperty(String pText) {
    this.text = Preconditions.checkNotNull(pText);
  }

  @Override
  public int hashCode() {
    return text.hashCode();
  }

  @Override
  public boolean equals(Object pOther) {
    if (!(pOther instanceof NamedProperty)) {
      return false;
    }
    return this.text.equals(pOther.toString());
  }

  @Override
  public String toString() {
    return text;
  }

  public static NamedProperty create(final String pText) {
    return new NamedProperty(pText);
  }

  public static Set<Property> singleton(final String pText) {
    return ImmutableSet.<Property>of(NamedProperty.create(pText));
  }

}
