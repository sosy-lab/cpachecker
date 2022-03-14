// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.interfaces.pcc;

public interface PartitioningCheckingHelper {

  /**
   * Does the necessary actions to abort the complete certificate checking process. Informs all
   * certificate checking components that certificate check failed. Possibly does more actions like
   * stops checking of other partitions, prohibits start of a partition check.
   */
  void abortCheckingPreparation();

  /**
   * Returns intermediate size of the certificate. The returned size contains all elements which
   * belong to an already checked partition as well as those elements recomputed in an already
   * checked partition. The size may or may not include elements already explored in a partition
   * check which is not completed.
   *
   * @return current size of certificate
   */
  int getCurrentCertificateSize();
}
