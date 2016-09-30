/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import org.sosy_lab.cpachecker.cfa.types.c.CType;

/**
 * A memory region for pointer analysis
 */
public interface MemoryRegion {
  /**
   * Returns the type of memory region.
   * The type is used for historical reasons
   * and may be deprecated in the future.
   *
   * @return the type of memory region
   */
  CType getType();
  /**
   * The function returns identifier of the region used
   * for the name of uninterpreted functions. Usually,
   * the name contains a string representation of the type as substring.
   *
   * @return identifier of the region
   */
  String getName();
}
