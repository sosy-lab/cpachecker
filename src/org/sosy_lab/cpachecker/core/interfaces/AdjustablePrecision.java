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
package org.sosy_lab.cpachecker.core.interfaces;

/**
 * Class represents Precision as a set, in which it is possible to add elements and subtract them.
 */
public interface AdjustablePrecision extends Precision {

  /** Add other Precision to current and return a new Precision. */
  public AdjustablePrecision add(AdjustablePrecision otherPrecision);

  /** Subtract other Precision from current and a return new Precision. */
  public AdjustablePrecision subtract(AdjustablePrecision otherPrecision);
}
