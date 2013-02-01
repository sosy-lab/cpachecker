/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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

import java.io.PrintStream;

import org.sosy_lab.cpachecker.core.interfaces.Statistics;


/**
 * Abstract implementation of Statistics that
 * provides some functions for more convenience and cleaner code
 * when dealing with CPAchecker statistics.
 */
public abstract class AbstractStatistics implements Statistics {

  protected int outputNameColWidth = 40;

  protected void put(PrintStream pTarget, String pName, Object pValue) {
    pTarget.println(String.format("%-" + outputNameColWidth + "s %s", pName + ":", pValue));
  }

}
