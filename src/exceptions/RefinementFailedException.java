/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package exceptions;

import cpa.common.Path;

/**
 * Exception raised when the refinement procedure fails, or was
 * abandoned. 
 * 
 * @author g.theoduloz
 */
public class RefinementFailedException extends CPAException {

  public static enum Reason {
    InterpolationFailed,
    NoNewPredicates,
    TooMuchUnrolling
  }
  
  private static final long serialVersionUID = 2353178323706458175L;
  
  public String reasonToString(Reason reason) {
    switch (reason)
    {
    case InterpolationFailed:
      return "Interpolation failed";
    case NoNewPredicates:
      return "No new predicates";
    case TooMuchUnrolling:
      return "Too much unrolling";
    default:
      assert false;
      return "";
    }
  }
 
  private final Reason reason;
  private final Path path;
  private final int failurePoint;
  
  public RefinementFailedException(Reason r, Path p, int pFailurePoint)
  {
    reason = r; 
    path = p;
    failurePoint = pFailurePoint;
  }
  
  public RefinementFailedException(Reason r, Path p)
  {
    this(r, p, -1);
  }
  
  /** Return the reason for the failure */
  public Reason getReason()
  {
    return reason;
  }
  
  /** Return the path that caused the failure */
  public Path getErrorPath()
  {
    return path;
  }

  /**
   * Returns the position of the node in the past where
   * the failure occurred (or -1 if the failure cannot
   * be caused by a given node)
   */
  public int getFailurePoint()
  {
    return failurePoint;
  }
  
  @Override
  public String toString() {
    return super.toString() + "[" + reason.toString() + "]";
  }
}
