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
package org.sosy_lab.cpachecker.cpa.context;

public class Thread {

  private final String threadName;
  private final boolean isActive;
  private final boolean isFinished;
  private final int lastProgramCounter;
  private final int maxProgramCounter;
  
  public Thread(String threadName, boolean isActive, boolean isFinished, int lastProgramCounter, int maxProgramCounter) {
    super();
    this.threadName = threadName;
    this.isActive = isActive;
    this.isFinished = isFinished;
    this.lastProgramCounter = lastProgramCounter;
    this.maxProgramCounter = maxProgramCounter;
  }

  public String getThreadName() {
    return threadName;
  }

  public boolean isActive() {
    return isActive;
  }

  public boolean isFinished() {
    return isFinished;
  }

  public int getLastProgramCounter() {
    return lastProgramCounter;
  }
  
  public int getMaxProgramCounter() {
    return maxProgramCounter;
  }
  
  

  @Override
  public String toString() {
    return threadName + " [threadName=" + threadName + ", isActive=" + isActive
        + ", isFinished=" + isFinished + ", lastProgramCounter="
        + lastProgramCounter + ", maxProgramCounter=" + maxProgramCounter + "]";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (isActive ? 1231 : 1237);
    result = prime * result + (isFinished ? 1231 : 1237);
    result = prime * result + lastProgramCounter;
    result = prime * result + maxProgramCounter;
    result = prime * result
        + ((threadName == null) ? 0 : threadName.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Thread other = (Thread) obj;
    if (isActive != other.isActive)
      return false;
    if (isFinished != other.isFinished)
      return false;
    if (lastProgramCounter != other.lastProgramCounter)
      return false;
    if (maxProgramCounter != other.maxProgramCounter)
      return false;
    if (threadName == null) {
      if (other.threadName != null)
        return false;
    } else if (!threadName.equals(other.threadName))
      return false;
    return true;
  }
  
  
}
