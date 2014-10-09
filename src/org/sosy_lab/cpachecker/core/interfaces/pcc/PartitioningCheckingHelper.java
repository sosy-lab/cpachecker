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
package org.sosy_lab.cpachecker.core.interfaces.pcc;


public interface PartitioningCheckingHelper {

  /**
   * Does the necessary actions to abort the complete certificate checking process.
   * Informs all certificate checking components that certificate check failed.
   * Possibly does more actions like stops checking of other partitions,
   * prohibits start of a partition check.
   */
  public void abortCheckingPreparation();

  /**
   * Returns intermediate size of the certificate. The returned size
   * contains all elements which belong to an already checked partition
   * as well as those elements recomputed in an already checked partition.
   * The size may or may not include elements already explored in a
   * partition check which is not completed.
   *
   * @return current size of certificate
   */
  public int getCurrentCertificateSize();

}
