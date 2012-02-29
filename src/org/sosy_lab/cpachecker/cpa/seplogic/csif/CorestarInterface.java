/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.seplogic.csif;

import static org.parboiled.errors.ErrorUtils.printParseErrors;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.parboiled.Parboiled;
import org.parboiled.parserunners.BasicParseRunner;
import org.parboiled.parserunners.TracingParseRunner;
import org.parboiled.support.ParsingResult;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Timer;
import org.sosy_lab.cpachecker.cpa.seplogic.SeplogicParser;
import org.sosy_lab.cpachecker.cpa.seplogic.nodes.Formula;
import org.sosy_lab.cpachecker.cpa.seplogic.nodes.SeplogicNode;



public class CorestarInterface {
  private Process csProcess, absProcess;
  private SeplogicParser parser;
  private LogManager logger;
  private static CorestarInterface singleton = null;
  public static final String RETVAR = "$ret_1";

  private Timer parsingTimer = new Timer();
  private Timer impTimer = new Timer();
  private Timer frameTimer = new Timer();
  private Timer specAssTimer = new Timer();
  private Timer abstractionTimer = new Timer();
  private long maxImplicationDuration = 0;

  private static final byte[] HOLDS = "1\n".getBytes();
  private static final byte[] HOLDSNOT = "0\n".getBytes();
  private byte[] holdsBuffer = new byte[HOLDS.length];

  public Timer getParsingTimer() {
    return parsingTimer;
  }


  public Timer getImpTimer() {
    return impTimer;
  }


  public Timer getFrameTimer() {
    return frameTimer;
  }


  public Timer getSpecAssTimer() {
    return specAssTimer;
  }


  public Timer getAbstractionTimer() {
    return abstractionTimer;
  }

  private CorestarInterface() {
    try {
      csProcess = createProcess(false);
      absProcess = createProcess(true);
      parser = Parboiled.createParser(SeplogicParser.class);
    } catch (IOException e) {
      throw new RuntimeException("Could not start Corestar process", e);
    }
  }


  private Process createProcess(boolean useSMT) throws IOException {
    // ugh, java ...
    Map<String, String> envMap = (new ProcessBuilder("")).environment();
    if (useSMT) {
      //envMap.put("JSTAR_SMT_PATH", "/home/xoraxax/dev/smtsolvers/z3/bin/z3");
      envMap.put("JSTAR_SMT_PATH", "/home/xoraxax/vcs/jstar/corestar-git/myz3.sh");
    }
    String[] envp = new String[envMap.size()];
    int i = 0;
    for (Entry<String, String> item : envMap.entrySet()) {
      envp[i++] = item.getKey() + "=" + item.getValue();
    }

    // XXX parameterize
    return Runtime.getRuntime().exec(new String[] { "/home/xoraxax/vcs/jstar/corestar-git/bin/run_prover",
        "-l", "/home/xoraxax/vcs/jstar/corestar-git/cpachecker.logic",
        "-a", "/home/xoraxax/vcs/jstar/corestar-git/cpachecker.abs"
        }, envp);
  }

  public static CorestarInterface getInstance() {
    if (singleton == null)
      singleton = new CorestarInterface();
    return singleton;
  }

  private void writeToProcess(Process process, byte[] data) throws IOException {
    logger.log(Level.FINER, "Query:\n", new String(data));
    process.getOutputStream().write(data);
    process.getOutputStream().flush();
  }

  private void readFromProcess(Process process, byte[] buffer, int len) throws IOException {
    if (process.getInputStream().read(buffer, 0, len) != len) {
      throw new IOException("Smaller read");
    }
    // System.err.print(new String(buffer));
  }

  private int readFromProcess(Process process) throws IOException {
    int read = process.getInputStream().read();
    if (read == -1) {
      throw new IOException("EOF?");
    }
    // System.err.print((char) read);
    return read;
  }
  public boolean entails(String formula1, String formula2) {
    Timer t = new Timer();

    impTimer.start();
    try {
      String query = "Implication: " + formula1 + " |- " + formula2 + " ENTER\n";
      t.start();
      try {
        writeToProcess(csProcess, query.getBytes());
        readFromProcess(csProcess, holdsBuffer, HOLDS.length);
      } catch (IOException e) {
        throw new RuntimeException("I/O with corestar failed", e);
      }
      t.stop();
      if (t.getSumTime() > maxImplicationDuration) {
        maxImplicationDuration = t.getSumTime();
        System.out.println(t + "::: " + query);
      }

      boolean holds = Arrays.equals(HOLDS, holdsBuffer);
      boolean holdsnot = Arrays.equals(HOLDSNOT, holdsBuffer);
      if (holds == holdsnot)
        throw new RuntimeException("Got error from solver");
      logger.log(Level.FINER, "Reply: " + (holds ? "HOLDS" : "DOESNOTHOLD"));
      return holds;
    } finally {
      impTimer.stop();
    }
  }

  public List<String> frame(String formula1, String formula2) {
    frameTimer.start();

    try {
      String query = "Frame: " + formula1 + " |- " + formula2 + " ENTER\n";
      try {
        writeToProcess(csProcess, query.getBytes());
        return readMultiple(csProcess);
      } catch (IOException e) {
        throw new RuntimeException("I/O with corestar failed", e);
      }
    } finally {
      frameTimer.stop();
    }
  }

  public List<String> biabduct(String formula1, String formula2) {
    String query = "Abduction: " + formula1 + " |- " + formula2 + " ENTER\n";
    try {
      writeToProcess(csProcess, query.getBytes());
      return readMultiple(csProcess);
    } catch (IOException e) {
      throw new RuntimeException("I/O with corestar failed", e);
    }
  }

  private List<String> readMultiple(Process process) throws IOException {
    List<String> retval = new ArrayList<String>();
    String read = "42";
    while (!read.trim().equals("END")) {
      read = readUntilEnter(process);
      retval.add(read);
    }
    retval.remove(retval.size() - 1);
    logger.log(Level.FINER, "Reply:\n", retval);
    return retval;
  }

  private String readUntilEnter(Process process) throws IOException {
    StringBuilder retval = new StringBuilder();
    byte[] ENTER = "\nENTER\n".getBytes();
    int enterpos = -1;
    int lastRead = 0;
    while (true) {
      lastRead = readFromProcess(process);
      if (ENTER[enterpos + 1] == (byte)lastRead) {
        enterpos++;
        if (enterpos == ENTER.length - 1) {
          return retval.substring(0, retval.length() - ENTER.length + 1);
        }
      } else if (enterpos != -1 && ENTER[enterpos] != (byte)lastRead) {
        enterpos = -1;
      }
      retval.append((char)lastRead);
    }
  }

  public Formula parse(String s) {
    return parse(s, false);
  }

  public Formula parse(String s, boolean debug) {
    parsingTimer.start();
    try {
      ParsingResult<SeplogicNode> result;
      if (debug) {
        result = new TracingParseRunner<SeplogicNode>(parser.Formula_npv_input()).run(s);
      } else {
        result = new BasicParseRunner<SeplogicNode>(parser.Formula_npv_input()).run(s);
      }

      if (result.hasErrors()) {
        System.err.println(printParseErrors(result));
        throw new RuntimeException("Parse Errors:\n" + printParseErrors(result));
      }

      Formula value = (Formula) result.parseTreeRoot.getValue();
      // System.out.println("\nParse Tree:\n" + printNodeTree(result) + '\n');

      value.setCachedRepr(s);
      return value;
    } finally {
      parsingTimer.stop();
    }
  }

  public List<String> abstract_(String formula1) {
    abstractionTimer.start();
    try {
      String query = "abstraction: " + formula1 + " ENTER\n";
      try {
        writeToProcess(absProcess, query.getBytes());
        return readMultiple(absProcess);
      } catch (IOException e) {
        throw new RuntimeException("I/O with corestar failed", e);
      }
    } finally {
      abstractionTimer.stop();
    }
  }

  public List<String> specAss(String pPre, String pPost, String pHeap, String pIdent) {
    specAssTimer.start();
    try {
      StringBuilder query = new StringBuilder();
      query.append("SpecAss: ");
      if (pIdent != null) {
        query.append(pIdent);
        query.append(" :=");
      }
      query.append(" : ");
      query.append(pPre);
      query.append(" : ");
      query.append(pPost);
      query.append(" : ");
      query.append(pHeap);
      query.append(" : ");
      if (pIdent != null) {
        query.append(RETVAR);
        query.append(" ");
      }
      query.append("ENTER\n");
      try {
        writeToProcess(csProcess, query.toString().getBytes());
        return readMultiple(csProcess);
      } catch (IOException e) {
        throw new RuntimeException("I/O with corestar failed", e);
      }
    } finally {
      specAssTimer.stop();
    }
  }

  public void setLogger(LogManager pLogger) {
    logger = pLogger;
  }

}
