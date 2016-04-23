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
package org.sosy_lab.cpachecker.exceptions;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.cpa.arg.ARGPath;

import java.util.List;

import javax.annotation.Nullable;

/**
 * Exception raised when the refinement procedure fails, or was
 * abandoned.
 */
public class InfeasibleCounterexampleException extends CPAException {

  private static final long serialVersionUID = 513019120577549278L;

  private final List<ARGPath> paths;

  public InfeasibleCounterexampleException(String msg, List<ARGPath> pInfeasibleErrorPaths) {
    super(msg);
    paths = pInfeasibleErrorPaths;
  }

  public InfeasibleCounterexampleException(String msg, List<ARGPath> p, Throwable t) {
    super(msg, checkNotNull(t));
    paths = p;
  }

  /** Return the path that caused the failure */
  public @Nullable List<ARGPath> getErrorPaths() {
    return paths;
  }
}
