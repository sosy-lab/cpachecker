// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package org.sosy_lab.cpachecker.core.algorithm.legion;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.util.statistics.StatInt;

public class LegionComponentStatistics {
  private final String name;

  private final Timer timer = new Timer();

  private long iterations = 0;
  private final Map<String, StatInt> others = new HashMap<>();

  public LegionComponentStatistics(String pName) {
    this.name = pName;
  }

  public void start() {
    this.timer.start();
  }

  public void finish() {
    this.timer.stopIfRunning();
    this.iterations += 1;
  }

  public String collect() {
    StringBuilder buff = new StringBuilder();

    buff.append("  " + this.name + ":");
    buff.append(
        "\n    exec_time: "
            + String.format("%.3fs", (float) this.timer.getSumTime().asMillis() / 1000));
    buff.append("\n    iterations: " + this.iterations);

    StringBuilder otherBuff = new StringBuilder();
    for (Entry<String, StatInt> entry : this.others.entrySet()) {
      otherBuff.append("\n      " + entry.getKey() + ": " + entry.getValue().getValueSum());
    }

    if (otherBuff.length() > 0) {
      buff.append("\n    others:");
      buff.append(otherBuff);
    }

    return buff.toString();
  }

  public void setOther(StatInt value) {
    this.others.put(value.getTitle(), value);
  }
}
