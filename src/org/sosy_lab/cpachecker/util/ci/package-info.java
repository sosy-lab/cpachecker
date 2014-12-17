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
/**
 * Package for management of custom instructions.
 *
 * It is assumed that these custom instructions will execute parts of the program's
 * statements as special purpose instructions i.e. implemented on special HW like FPGAs.
 *
 * Used to support the extraction of requirements for the custom instructions from
 * the software analysis result as explained in approach #3 of paper
 *
 * M.-C. Jakobs, M. Platzner, T. Wiersema, H. Wehrheim:
 * Integrating Softwaren and Hardware Verification
 * Integrated Formal Methods, LNCS, Springer, 2014
 */
package org.sosy_lab.cpachecker.util.ci;