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
package org.sosy_lab.cpachecker.appengine.common;

import org.sosy_lab.cpachecker.appengine.entity.Job;

/**
 * Interface for classes that are able to run a verification job.
 * Running the job does not need to be immediate. A job runner implementation may
 * also delay the run.
 * Examples are:
 * - Asynchronous runner
 * - Synchronous runner
 * - Task Queue runner
 * - Remote runner
 * - ...
 */
public interface JobRunner {

  /**
   * Runs the given job and return the given job instance.
   * So it is possible to look at the job instance properties after the call
   * to see e.g. if the status has changed.
   *
   * @param job The job to run.
   * @return The job instance that was to be run.
   */
  public Job run(Job job);

}
