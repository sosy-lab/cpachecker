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
package org.sosy_lab.cpachecker.cpa.loopinvariants;

import com.google.common.base.Splitter;
import com.google.common.io.Closeables;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class SympyProcess implements AutoCloseable {

  private static final String[] COMMAND = new String[] {"python", "-i", "-c", ""};

  private static final Splitter PROMPT_SPLITTER = Splitter.on(">>>").omitEmptyStrings();

  private final BufferedWriter stdin;

  private final BufferedReader stdout;

  private final BufferedReader stderr;

  private final Process process;

  private SympyProcess() throws IOException {
    ProcessBuilder processBuilder = new ProcessBuilder(COMMAND);
    process = processBuilder.start();
    stdin =
        new BufferedWriter(
            new OutputStreamWriter(process.getOutputStream(), Charset.defaultCharset()));
    stdout =
        new BufferedReader(
            new InputStreamReader(process.getInputStream(), Charset.defaultCharset()));
    stderr =
        new BufferedReader(
            new InputStreamReader(process.getErrorStream(), Charset.defaultCharset()));
    sendLine("from __future__ import division");
    sendLine("from sympy import *");
    sendLine("x, y, z, t = symbols('x y z t')");
    sendLine("k, m, n = symbols('k m n', integer=True)");
    sendLine("f, g, h = symbols('f g h', cls=Function)");
    sendLine("init_printing()");
  }

  public void sendLine(String pLine) throws IOException {
    stdin.write(pLine);
    stdin.newLine();
    stdin.flush();
  }

  public String readLine() throws IOException {
    return stdout.readLine();
  }

  public void commit() throws IOException {
    stdin.close();
  }

  public Stream<String> readLines() {
    return stdout.lines();
  }

  public Stream<String> readErrorLines() {
    return stderr
        .lines()
        .flatMap(l -> StreamSupport.stream(PROMPT_SPLITTER.split(l).spliterator(), false));
  }

  @Override
  public void close() throws IOException {
    IOException e = null;
    for (Closeable closeable : Arrays.asList(stdout, stderr, stdin, () -> process.destroy())) {
      try {
        Closeables.close(closeable, e != null);
      } catch (IOException e1) {
        assert e == null;
        e = e1;
      }
    }
    if (e != null) {
      throw e;
    }
  }

  public static SympyProcess newProcess() throws IOException {
    return new SympyProcess();
  }
}
