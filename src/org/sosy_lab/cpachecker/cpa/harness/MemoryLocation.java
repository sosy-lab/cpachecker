/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.harness;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.sosy_lab.common.UniqueIdGenerator;

public class MemoryLocation implements Comparable<MemoryLocation> {

  @Nullable
  private String identifier;
  @Nullable
  private String functionScope;
  @Nullable
  private boolean isPrecise;
  @Nonnull
  private int uniqueId;

  private static final UniqueIdGenerator idGenerator = new UniqueIdGenerator();

  public MemoryLocation() {
    isPrecise = true;
    uniqueId = idGenerator.getFreshId();
  }

  public MemoryLocation(boolean pIsPrecise) {
    isPrecise = pIsPrecise;
    uniqueId = idGenerator.getFreshId();
  }

  public MemoryLocation(String pIdentifier) {
    identifier = pIdentifier;
    isPrecise = true;
    uniqueId = idGenerator.getFreshId();
  }

  public MemoryLocation(String pIdentifier, boolean pIsPrecise) {
    identifier = pIdentifier;
    isPrecise = pIsPrecise;
    uniqueId = idGenerator.getFreshId();
  }

  // Does this instance represent a precisely known location or could it be one of several
  public boolean isPrecise() {
    return isPrecise;
  }

  /**
   * @return the identifier of this location in the target code
   */
  public String getIdentifier() {
    return identifier;
  }

  /**
   * @return Unique Id of this location
   */
  public int getUniqueId() {
    return uniqueId;
  }

  @Override
  public int compareTo(MemoryLocation pO) {
    return Integer.compare(uniqueId, pO.getUniqueId());
  }

}
