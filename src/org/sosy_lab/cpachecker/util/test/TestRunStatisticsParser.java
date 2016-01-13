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
package org.sosy_lab.cpachecker.util.test;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.truth.IntegerSubject;
import com.google.common.truth.StringSubject;


public class TestRunStatisticsParser extends StatisticsParser {

  public IntegerSubject assertThatNumber(String pStatisticsKey) {
    final String stringValue = getStatistics().get(pStatisticsKey);
    final int numValue = Integer.parseInt(stringValue.split(" ")[0]);
    return assertThat(numValue);
  }

  public StringSubject assertThatString(String pStatisticsKey) {
    final String stringValue = getStatistics().get(pStatisticsKey);
    return assertThat(stringValue);
  }

}
