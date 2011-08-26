/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.core;

import static com.google.common.base.Preconditions.checkState;

import org.sosy_lab.cpachecker.cpa.art.Path;

public class CounterexampleInfo {

  private final boolean spurious;

  private final Path targetPath;
  private final Object assignment;

  private static final CounterexampleInfo SPURIOUS = new CounterexampleInfo(true, null, null);

  private CounterexampleInfo(boolean pSpurious, Path pTargetPath, Object pAssignment) {
    spurious = pSpurious;
    targetPath = pTargetPath;
    assignment = pAssignment;
  }

  public static CounterexampleInfo spurious() {
    return SPURIOUS;
  }

  public static CounterexampleInfo feasible(Path pTargetPath, Object pAssignment) {
    return new CounterexampleInfo(false, pTargetPath, pAssignment);
  }

  public boolean isSpurious() {
    return spurious;
  }

  public Path getTargetPath() {
    checkState(!spurious);

    return targetPath;
  }

  public Object getTargetPathAssignment() {
    checkState(!spurious);

    return assignment;
  }
}
