/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.util;

import static com.google.common.base.Preconditions.checkState;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility class for generating unique.
 * This class is fully thread-safe.
 *
 * It gives out at most MAX_INT ids, afterwards it throws an exception.
 */
public final class UniqueIdGenerator {

  private final AtomicInteger nextId = new AtomicInteger();

  public int getFreshId() {
    int id = nextId.getAndIncrement();
    checkState(id >= 0, "Overflow for unique ID");
    return id;
  }
}
