// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer.transfer;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cpa.pointer.StructHandlingStrategy;

@Options(prefix = "cpa.pointer")
public class PointerTransferOptions {

  @Option(
      secure = true,
      description =
          "Strategy for mapping heap allocations to symbolic heap locations: SINGLE, PER_CALL,"
              + " PER_LINE")
  private HeapAllocationStrategy heapAllocationStrategy = HeapAllocationStrategy.PER_CALL;

  @Option(secure = true, description = "Strategy for handling structs in pointer analysis")
  private StructHandlingStrategy structHandlingStrategy = StructHandlingStrategy.ALL_FIELDS;

  @Option(
      secure = true,
      description =
          "Enable or disable offset-sensitive pointer analysis. "
              + "When false, offsets in pointer arithmetic are ignored.")
  private boolean isOffsetSensitive = true;

  public HeapAllocationStrategy getHeapAllocationStrategy() {
    return heapAllocationStrategy;
  }

  public boolean isOffsetSensitive() {
    return isOffsetSensitive;
  }

  public StructHandlingStrategy getStructHandlingStrategy() {
    return structHandlingStrategy;
  }

  public PointerTransferOptions(Configuration config) throws InvalidConfigurationException {
    config.inject(this);
  }
}
