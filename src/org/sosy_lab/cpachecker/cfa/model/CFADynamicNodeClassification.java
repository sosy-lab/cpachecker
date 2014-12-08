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
package org.sosy_lab.cpachecker.cfa.model;


public enum CFADynamicNodeClassification {
  /**
   * Function entry node of the entry function
   */
  ENTRY,

  /**
   * Set of function entry nodes of all functions.
   */
  FUNCTION_ENTRY,

  /**
   * All locations that are possible targets of the analysis.
   */
  TARGET,

  /**
   * Function exit node of the entry function.
   */
  EXIT,

  /**
   * All function exit nodes of all functions and all loop heads of endless loops.
   */
  FUNCTION_SINK,

  /**
   * All function exit nodes of the entry function, and all loop heads of endless loops.
   */
  PROGRAM_SINK
}
