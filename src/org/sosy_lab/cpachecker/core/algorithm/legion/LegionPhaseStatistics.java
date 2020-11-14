/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.core.algorithm.legion;

import java.time.Duration;
import java.time.Instant;

public class LegionPhaseStatistics {
    private String name;

    private Duration execution_time;
    private Instant now;

    private int iterations;

    public LegionPhaseStatistics(String pName) {
        this.name = pName;
        this.execution_time = Duration.ZERO;
        this.iterations = 0;
    }

    public void start() {
        this.now = Instant.now();
    }

    public void finish() {
        this.execution_time = this.execution_time.plus(Duration.between(now, Instant.now()));
        this.iterations += 1;
    }

    public String collect() {
        StringBuilder buff = new StringBuilder();

        buff.append("  " + this.name + ":");
        buff.append("\n    exec_time: " + String.format("%.3fs", (float) this.execution_time.toMillis() / 1000));
        buff.append("\n    iterations: " + this.iterations);

        return buff.toString();
    }

}
