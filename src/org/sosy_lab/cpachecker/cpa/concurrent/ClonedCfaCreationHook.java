// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.concurrent;

import static net.bytebuddy.matcher.ElementMatchers.named;

import java.lang.instrument.Instrumentation;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.ImmutableCFA;

/** Test-only Byte Buddy hook that replaces created CFAs with structural clones. */
public final class ClonedCfaCreationHook {

  private static boolean installed;

  private ClonedCfaCreationHook() {}

  static boolean isAgentInstalled() {
    try {
      ByteBuddyAgent.getInstrumentation();
      return true;
    } catch (IllegalStateException e) {
      return false;
    }
  }

  static synchronized void install() {
    if (installed) {
      return;
    }
    Instrumentation instrumentation = ByteBuddyAgent.getInstrumentation();
    new AgentBuilder.Default()
        .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
        .type(named(CFACreator.class.getName()))
        .transform(
            (builder, type, classLoader, module, protectionDomain) ->
                builder.visit(
                    Advice.to(CloneCfaOnReturn.class)
                        .on(named("parseFileAndCreateCFA").or(named("parseSourceAndCreateCFA")))))
        .installOn(instrumentation);
    installed = true;
  }

  public static ImmutableCFA cloneCfa(ImmutableCFA pCfa) {
    return ConcurrentCfaCloner.cloneForTesting(pCfa);
  }

  public static final class CloneCfaOnReturn {

    private CloneCfaOnReturn() {}

    @Advice.OnMethodExit
    public static void cloneCfa(@Advice.Return(readOnly = false) ImmutableCFA pCfa) {
      pCfa = ClonedCfaCreationHook.cloneCfa(pCfa);
    }
  }
}
