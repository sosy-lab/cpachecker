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
package org.sosy_lab.cpachecker.util.statistics;

import com.google.common.base.Optional;


public abstract class AbstractStatValue {

  private final Optional<String> title;
  private StatKind mainStatisticKind;

  public AbstractStatValue(StatKind pMainStatisticKind, String pTitle) {
    this.title = Optional.of(pTitle);
    this.mainStatisticKind = pMainStatisticKind;
  }

  public AbstractStatValue(StatKind pMainStatisticKind) {
    this.title = Optional.absent();
    this.mainStatisticKind = pMainStatisticKind;
  }

  public String getTitle() {
    return title.get();
  }

  public boolean hasTitle()  {
    return title.isPresent();
  }

  /**
   * How many times was this statistical value updated.
   * @return A nonnegative number.
   */
  public abstract int getUpdateCount();


  public StatKind getMainStatisticKind() {
    return mainStatisticKind;
  }
}
