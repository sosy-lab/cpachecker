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
 * The LiveVariablesCPA is a backwards program analysis, which is aimed to find out which
 * variable is live (read as used afterwards) at which position.
 * As the information is only complete after this CPA has finished computing live variables was
 * directly added into the preprocessing of CPAchecker and can be toggled with the option
 * <b>cfa.findLiveVariables</b> by default no live variables are generated.
 *
 * Up to now, in case of pointer aliasing this analysis is unsound.
 */
package org.sosy_lab.cpachecker.cpa.livevar;
