// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.export;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Producer {
  private final String name;
  private final String version;
  private final String configuration;

  public Producer(String pName, String pVersion, String pConfiguration) {
    name = pName;
    version = pVersion;
    configuration = pConfiguration;
  }

  public String getName() {
    return name;
  }

  public String getVersion() {
    return version;
  }

  public String getConfiguration() {
    return configuration;
  }
}
