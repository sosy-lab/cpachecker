/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.cpachecker.util.statistics.Aggregateables.AggregationInt;
import org.sosy_lab.cpachecker.util.statistics.StatCpuTime.StatCpuTimer;
import org.sosy_lab.cpachecker.util.statistics.Stats.Contexts;
import org.sosy_lab.cpachecker.util.statistics.interfaces.NoStatisticsException;
import org.sosy_lab.cpachecker.util.statistics.interfaces.RetrospectiveContext;


public class StatsTest {

  @Before
  public void setUp() throws Exception {
    Stats.reset();
  }

  public static void main(String[] args) throws InterruptedException {
    new StatsTest().test();
  }

  @Test
  public void test() throws InterruptedException {
    int i = 100000;
    while (i-- > 0) {
      try (Contexts ctx1 = Stats.beginSubContext("Multi Property Verification")) {
        try (Contexts ctx2 = Stats.beginSubContext("CEGAR")) {
          try (Contexts ctx3 = Stats.beginSubContext("CPA")) {
            try (Contexts ctx4 = Stats.beginSubContext("Transfer Relation")) {
              try (Contexts ctxO = Stats.beginRootContext("Property 1", "Property 2")) {
                try (StatCpuTimer c = Stats.startTimer("Target SAT check")) {
                }
//                Stats.putItems("Predicates", Sets.<Object>newHashSet("a==1", "b>5"));
              }

              try (Contexts ctxO = Stats.beginRootContext("Property 1")) {
                Stats.incCounter("Number of Predicates", 5);
              }

              try (RetrospectiveContext ctxR = Stats.retrospectiveRootContext()) {
                Stats.incCounter("Number of Predicates", 5);
                ctxR.putRootContexts("Property 3");
              }
            }
            try (Contexts ctx4 = Stats.beginSubContext("Precision Adjustment")) {

            }
          }
        }
      }
    }

    Stats.printStatitics(System.out);

  }

  @Test
  public void test4() throws InterruptedException {
    int i = 2000;
    while (i-- > 0) {
      try (Contexts ctx1 = Stats.beginSubContext("Multi Property Verification")) {
        try (RetrospectiveContext ctxR = Stats.retrospectiveRootContext()) {
          Stats.incCounter("Number of Predicates", 5);
          ctxR.putRootContexts("Property 3");
        }
      }
    }

    Stats.printStatitics(System.out);

  }

  @Test
  public void test3() throws InterruptedException {
    int i = 2;
    while (i-- > 0) {
      try (Contexts ctx1 = Stats.beginSubContext("Multi Property Verification")) {
        Thread.sleep(111);
      }
    }

    Stats.printStatitics(System.out);

  }


  @Test
  public void test2() throws NoStatisticsException {
    try (Contexts ctx1 = Stats.beginSubContext("Multi Property Verification")) {
      try (Contexts ctxO = Stats.beginRootContext("Property 1", "Property 2")) {
        Stats.incCounter("Number of Predicates", 10);
      }

      try (Contexts ctxO = Stats.beginRootContext("Property 1")) {
        Stats.incCounter("Number of Predicates", 777);
      }
    }

    Stats.printStatitics(System.out);

    AggregationInt p1stats = Stats.query(Thread.currentThread(),
        "Number of Predicates",
        "Property 1",
        Aggregateables.AggregationInt.class);

    AggregationInt p2stats = Stats.query(Thread.currentThread(),
        "Number of Predicates",
        "Property 2",
        Aggregateables.AggregationInt.class);

    assertEquals(787, (int) p1stats.getSum());
    assertEquals(10, (int) p2stats.getSum());
    assertEquals(2, p1stats.getValuations());
    assertEquals(1, p2stats.getValuations());
  }


}
