/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.arg;

import com.google.common.base.Predicates;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.stream.Collectors;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;

@Options(prefix = "cpa.arg")
public class ARGLogger {
  private static int iterationCount = 0;
  private static final String FILENAMEPATTERN = "output/arglog%04d.dot";

  @Option(secure = true, description = "Enable logging of ARGs at various positions")
  private boolean logARGs = false;

  public ARGLogger(Configuration config) throws InvalidConfigurationException {
    config.inject(this);
  }

  public static void increaseIterationCount() {
    iterationCount++;
  }

  public void log(String pMessage, UnmodifiableReachedSet pReachedSet) {
    if (!logARGs
        || pReachedSet.asCollection().isEmpty()
        || !(pReachedSet.asCollection().iterator().next() instanceof ARGState)) {
      return;
    }
    String label =
        pMessage
            + ";"
            + "waitlist="
            + pReachedSet
                .getWaitlist()
                .stream()
                .map(x -> ((ARGState) x).getStateId())
                .collect(Collectors.toList())
                .toString()
            + "; reached="
            + pReachedSet
                .asCollection()
                .stream()
                .map(x -> ((ARGState) x).getStateId())
                .collect(Collectors.toList())
                .toString();
    log(label, pReachedSet.asCollection());
  }

  @SuppressWarnings("unchecked")
  public void log(String pMessage, Collection<AbstractState> pStates) {
    if (!logARGs || pStates.isEmpty()) {
      return;
    }
    String filename = String.format(FILENAMEPATTERN, iterationCount);
    try (Writer w = IO.openOutputFile(Paths.get(filename), Charset.defaultCharset())) {
      if (pStates.iterator().next() instanceof SLARGState) {
        SLARGToDotWriter.write(w, (Collection<SLARGState>) (Object) pStates, pMessage);
      } else if (pStates.iterator().next() instanceof ARGState) {
        ARGToDotWriter.write(w, (Collection<ARGState>) (Object) pStates, pMessage);
      }
      increaseIterationCount();
    } catch (IOException e) {
    }
  }

  public void log(@SuppressWarnings("unused") String pMessage, AbstractState pRootState) {
    if (!logARGs) {
      return;
    }
    String filename = String.format(FILENAMEPATTERN, iterationCount);
    try (Writer w = IO.openOutputFile(Paths.get(filename), Charset.defaultCharset())) {
      ARGToDotWriter.write(
          w,
          (ARGState) pRootState,
          ARGState::getChildren,
          Predicates.alwaysTrue(),
          Predicates.alwaysFalse());
      increaseIterationCount();
    } catch (IOException e) {
    }
  }
}
