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
package org.sosy_lab.cpachecker.core;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sosy_lab.cpachecker.core.ShutdownNotifier.ShutdownRequestListener;


public class ShutdownNotifierTest {

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  private final String reason = "Shutdown Request Reason";

  private ShutdownNotifier instance = null;

  @Before
  public void setUp() {
    instance = ShutdownNotifier.create();
  }

  @After
  public void tearDown() {
    instance = null;
  }

  @Test
  public void testNotRequested() throws InterruptedException {
    assertFalse(instance.shouldShutdown());
    instance.shutdownIfNecessary();
  }

  @Test
  public void testNotRequestedReason() throws InterruptedException {
    thrown.expect(IllegalStateException.class);
    instance.getReason();
  }

  @Test
  public void testRequested() throws InterruptedException {
    instance.requestShutdown(reason);
    assertTrue(instance.shouldShutdown());
    assertEquals(reason, instance.getReason());
  }

  @Test
  public void testRequestedException() throws InterruptedException {
    instance.requestShutdown(reason);

    thrown.expect(InterruptedException.class);
    thrown.expectMessage(reason);
    instance.shutdownIfNecessary();
  }

  @Test
  public void testRegisterListenerTwice() {
    ShutdownRequestListener l = new ShutdownRequestListener() {
        @Override
        public void shutdownRequested(String reason) { }
      };

    instance.register(l);
    thrown.expect(IllegalArgumentException.class);
    instance.register(l);
  }

  @Test
  public void testListenerNotification() {
    final AtomicBoolean flag = new AtomicBoolean(false);

    instance.register(new ShutdownRequestListener() {
        @Override
        public void shutdownRequested(String reason) {
          flag.set(true);
        }
      });

    instance.requestShutdown(reason);
    assertTrue(flag.get());
  }

  @Test
  public void testListenerNotificationReason() {
    final AtomicReference<String> reasonReference = new AtomicReference<>();

    instance.register(new ShutdownRequestListener() {
        @Override
        public void shutdownRequested(String pReason) {
          reasonReference.set(pReason);
        }
      });

    instance.requestShutdown(reason);
    assertEquals(reason, reasonReference.get());
  }

  @Test
  public void testListenerNotification10() {
    final int count = 10;
    final AtomicInteger i = new AtomicInteger(0);

    for (int j = 0; j < count; j++) {
      instance.register(new ShutdownRequestListener() {
          @Override
          public void shutdownRequested(String reason) {
            i.incrementAndGet();
          }
        });
    }

    instance.requestShutdown(reason);
    assertEquals(count, i.get());
  }

  @Test
  public void testUnregisterListener() {
    final AtomicBoolean flag = new AtomicBoolean(false);

    ShutdownRequestListener l = new ShutdownRequestListener() {
        @Override
        public void shutdownRequested(String reason) {
          flag.set(true);
        }
      };
    instance.register(l);
    instance.unregister(l);

    instance.requestShutdown(reason);
    assertFalse(flag.get());
  }

  @Test
  public void testListenerRegisterAndCheck() {
    final AtomicBoolean flag = new AtomicBoolean(false);

    instance.registerAndCheckImmediately(new ShutdownRequestListener() {
        @Override
        public void shutdownRequested(String reason) {
          flag.set(true);
        }
      });

    assertFalse(flag.get());
    instance.requestShutdown(reason);
    assertTrue(flag.get());
  }

  @Test
  public void testListenerNotificationOnRegister() {
    final AtomicBoolean flag = new AtomicBoolean(false);

    instance.requestShutdown(reason);
    instance.registerAndCheckImmediately(new ShutdownRequestListener() {
        @Override
        public void shutdownRequested(String reason) {
          flag.set(true);
        }
      });

    assertTrue(flag.get());
  }

  @Test
  public void testListenerNotificationReasonOnRegister() {
    final AtomicReference<String> reasonReference = new AtomicReference<>();

    instance.requestShutdown(reason);
    instance.registerAndCheckImmediately(new ShutdownRequestListener() {
        @Override
        public void shutdownRequested(String pReason) {
          reasonReference.set(pReason);
        }
      });

    assertEquals(reason, reasonReference.get());
  }

  @Test
  public void testParentChild() {
    ShutdownNotifier child = ShutdownNotifier.createWithParent(instance);

    assertFalse(instance.shouldShutdown());
    assertFalse(child.shouldShutdown());

    instance.requestShutdown(reason);

    assertTrue(child.shouldShutdown());
    assertEquals(reason, child.getReason());
  }

  @Test
  public void testChildParent() {
    ShutdownNotifier child = ShutdownNotifier.createWithParent(instance);

    assertFalse(instance.shouldShutdown());
    assertFalse(child.shouldShutdown());

    child.requestShutdown(reason);

    assertFalse(instance.shouldShutdown());
    assertTrue(child.shouldShutdown());
    assertEquals(reason, child.getReason());
  }

  @Test
  public void testParentChildListenerNotification() {
    final AtomicBoolean flag = new AtomicBoolean(false);

    ShutdownNotifier child = ShutdownNotifier.createWithParent(instance);

    child.register(new ShutdownRequestListener() {
        @Override
        public void shutdownRequested(String reason) {
          flag.set(true);
        }
      });

    instance.requestShutdown(reason);
    assertTrue(flag.get());
  }

  @Test
  public void testParentChildListenerNotificationReason() {
    final AtomicReference<String> reasonReference = new AtomicReference<>();

    ShutdownNotifier child = ShutdownNotifier.createWithParent(instance);

    child.register(new ShutdownRequestListener() {
        @Override
        public void shutdownRequested(String pReason) {
          reasonReference.set(pReason);
        }
      });

    instance.requestShutdown(reason);
    assertEquals(reason, reasonReference.get());
  }
}
