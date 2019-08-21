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
import java.nio.file.Path;
import java.util.Collection;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.sosy_lab.common.UniqueIdGenerator;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.slab.SLARGState;
import org.sosy_lab.cpachecker.cpa.slab.SLARGToDotWriter;
import org.sosy_lab.cpachecker.util.BiPredicates;

@Options(prefix = "cpa.arg")
public class ARGLogger {
  private final UniqueIdGenerator iterationCount = new UniqueIdGenerator();

  @Option(
      secure = true,
      name = "log.fileTemplate",
      description =
          "write the ARG at various stages during execution "
              + "into dot files whose name is specified by this option. "
              + "Only works if 'cpa.arg.logARGs=true'")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate argLoggerFilenameTemplate =
      PathTemplate.ofFormatString("ARG_log/ARG_%04d.dot");

  @Option(secure = true, description = "Enable logging of ARGs at various positions")
  private boolean logARGs = false;

  private LogManager logger;

  public ARGLogger(Configuration config, LogManager pLogger) throws InvalidConfigurationException {
    config.inject(this);
    logger = pLogger;
  }

  /**
   * Logs a graph of passed reached set with a custom title.
   *
   * @param pTitle string that is shown in the title of the graph
   * @param pReachedSet states that shall be present in the graph
   */
  public void log(String pTitle, UnmodifiableReachedSet pReachedSet) {
    if (!loggingAllowed()
        || pReachedSet == null
        || pReachedSet.isEmpty()
        || !(pReachedSet.iterator().next() instanceof ARGState)) {
      return;
    }
    String label =
        String.format(
            "%s; waitlist=%s; reached=%s",
            pTitle,
            buildStatesList(pReachedSet.getWaitlist()),
            buildStatesList(pReachedSet.asCollection()));
    log(label, pReachedSet.asCollection());
  }

  @SuppressWarnings("unchecked")
  public void log(String pTitle, Collection<AbstractState> pStates) {
    if (!loggingAllowed() || pStates == null || pStates.isEmpty()) {
      return;
    }
    Path file = argLoggerFilenameTemplate.getPath(iterationCount.getFreshId());
    try (Writer w = IO.openOutputFile(file, Charset.defaultCharset())) {
      if (pStates.iterator().next() instanceof SLARGState) {
        SLARGToDotWriter.write(w, (Collection<SLARGState>) (Object) pStates, pTitle);
      } else if (pStates.iterator().next() instanceof ARGState) {
        ARGToDotWriter.write(w, (Collection<ARGState>) (Object) pStates, pTitle);
      }
    } catch (IOException e) {
      logger.logfUserException(Level.WARNING, e, "A problem occurred while writing to %s ", file);
    }
  }

  public void log(AbstractState pRootState) {
    if (!loggingAllowed()) {
      return;
    }
    Path file = argLoggerFilenameTemplate.getPath(iterationCount.getFreshId());
    try (Writer w = IO.openOutputFile(file, Charset.defaultCharset())) {
      ARGToDotWriter.write(
          w,
          (ARGState) pRootState,
          ARGState::getChildren,
          Predicates.alwaysTrue(),
          BiPredicates.alwaysFalse());
    } catch (IOException e) {
      logger.logfUserException(Level.WARNING, e, "A problem occurred while writing to %s ", file);
    }
  }

  private String buildStatesList(Collection<AbstractState> states) {
    return states
        .stream()
        .map(x -> Integer.toString(((ARGState) x).getStateId()))
        .collect(Collectors.joining(", "));
  }

  private boolean loggingAllowed() {
    return logARGs && argLoggerFilenameTemplate != null;
  }
}
