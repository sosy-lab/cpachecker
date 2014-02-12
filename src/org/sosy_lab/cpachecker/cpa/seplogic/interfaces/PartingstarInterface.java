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
package org.sosy_lab.cpachecker.cpa.seplogic.interfaces;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cpa.seplogic.SeplogicState.SeplogicQueryUnsuccessful;


@Options(prefix="cpa.seplogic.partingstar")
public class PartingstarInterface {
  private Process psProcess;
  private LogManager logger;
  private static PartingstarInterface singleton = null;
  private Object syncSentinel = new Object();
  public static final String RETVAR = "$ret_1";

  private static final String LOAD_RULES = "LOAD_RULES";
  private static final String ENTAILS = "ENTAILS";
  private static final String FRAME = "FRAME";
  private static final String ABSTRACT = "ABSTRACT";
  private static final String STRING = "STRING";
  private static final String DEL = "DEL";
  private static final String AND = "AND";
  private static final String OR = "OR";
  private static final String STAR = "STAR";
  private static final String VAR = "VAR";
  private static final String INEQ = "INEQ";
  private static final String EQ = "EQ";
  private static final String SPRED = "SPRED";
  private static final String FALSE = "FALSE";
  private static final String EMP = "EMP";
  private static final String RENAMEIDENT = "RENAMEIDENT";
  private static final String EXTRACTVALUE = "EXTRACTVALUE";
  private static final String REPR = "REPR";
  private static final String INT = "INT";
  private static final String PLUS = "PLUS";
  private static final String MINUS = "MINUS";
  private static final String EXTRACTEQS = "EXTRACTEQS";
  private static final String NIL = "NIL";
  private static final String SPECASS = "SPECASS";

  private Timer creationTimer = new Timer();
  private Timer impTimer = new Timer();
  private Timer frameTimer = new Timer();
  private Timer specAssTimer = new Timer();
  private Timer abstractionTimer = new Timer();

  @Option(name="pspath", required=true,
      description="path to partingstar command")
  @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
  private Path psPath = null;

  @Option(name="logicsfile", required=true,
      description="path to a file with logic rules")
  @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
  private Path logicsFile = null;

  @Option(name="abstractionfile", required=true,
      description="path to a file with abstraction rules")
  @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
  private Path abstractionFile = null;


  public Timer getCreationTimer() {
    return creationTimer;
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

  private RuntimeException generateExc(String msg, Throwable e) {
    byte[] buffer = new byte[4096 * 64];
    try {
      psProcess.getErrorStream().read(buffer);
    } catch (IOException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }
    return new RuntimeException(msg + "\n\nDetail:\n" + new String(buffer), e);
  }

  private class PartingstarHandle implements Handle {
    int handle;
    private PartingstarHandle(int handle) {
      this.handle = handle;
    }

    @Override
    public String toString() {
      return handle + "";
    }

    @Override
    protected void finalize() throws Throwable {
      System.err.println("DEL for " + handle);
      StringBuilder sb = new StringBuilder();
      sb.append(DEL);
      sb.append(' ');
      sb.append(handle);
      sb.append('\n');
      String ret;
      synchronized (syncSentinel) {
        writeToProcess(sb.toString().getBytes());
        ret = readUntilEnter();
      }
      if (!"".equals(ret)) {
        throw generateExc("Finalizer run into error: " + ret, null);
      }
    }

    @Override
    public boolean isNonZero() {
      return handle != 0;
    }
  }

  private PartingstarInterface(Configuration config, LogManager pLogger) throws InvalidConfigurationException {
    logger = pLogger;
    config.inject(this, PartingstarInterface.class);
    try {
      psProcess = createProcess();
      loadRules(logicsFile.toAbsolutePath().getPath(), false);
      loadRules(abstractionFile.toAbsolutePath().getPath(), true);
    } catch (IOException e) {
      throw generateExc("Could not start Partingstar process", e);
    }
  }


  private Process createProcess() throws IOException {
    return Runtime.getRuntime().exec(new String[] { psPath.toAbsolutePath().getPath(),
        });
  }

  public void loadRules(String filename, boolean isAbs) throws IOException {
    singleCommand(LOAD_RULES, loadString(filename), loadString("" + (isAbs ? 1 : 0)));
  }

  public Handle makeAnd(Handle in1, Handle in2) {
    return safeSingleCommand(AND, in1, in2);
  }

  public Handle makeOr(Handle in1, Handle in2) {
    return safeSingleCommand(OR, in1, in2);
  }

  public Handle makeStar(Handle in1, Handle in2) {
    return safeSingleCommand(STAR, in1, in2);
  }

  public Handle makeVar(Handle in1) {
    return safeSingleCommand(VAR, in1);
  }

  public Handle makeInt(Handle in1) {
    return safeSingleCommand(INT, in1);
  }

  public Handle makeIneq(Handle in1, Handle in2) {
    return safeSingleCommand(INEQ, in1, in2);
  }

  public Handle makeEq(Handle in1, Handle in2) {
    return safeSingleCommand(EQ, in1, in2);
  }

  public Handle makePlus(Handle in1, Handle in2) {
    return safeSingleCommand(PLUS, in1, in2);
  }

  public Handle makeMinus(Handle in1, Handle in2) {
    return safeSingleCommand(MINUS, in1, in2);
  }


  public Handle makeNil() {
    return safeSingleCommand(NIL);
  }

  public Handle makeSpatialPredicate(Handle predName, Handle... arguments) {
    Handle[] myArguments = new Handle[1 + arguments.length];
    myArguments[0] = predName;
    System.arraycopy(arguments, 0, myArguments, 1, arguments.length);
    return safeSingleCommand(SPRED, myArguments);
  }

  public Handle renameIdent(Handle h, String from, String to) {
    try {
      return singleCommand(RENAMEIDENT, h, loadString(from), loadString(to));
    } catch (IOException e) {
      throw generateExc("Error when renaming ident", e);
    }
  }

  public Long extractExplicitValue(Handle h, String pVarName) {
    try {
      return Long.parseLong(repr(singleCommand(EXTRACTVALUE, h, loadString(pVarName))));
    } catch (IOException e) {
      throw generateExc("Error when extracting explicit value", e);
    }
  }

  public String repr(Handle pHeap) {
    try {
      StringBuilder sb = new StringBuilder();
      sb.append(REPR);
      sb.append(' ');
      sb.append(pHeap.toString());
      sb.append('\n');
      synchronized (syncSentinel) {
        writeToProcess(sb.toString().getBytes());
        return readUntilEnter();
      }
    } catch (IOException e) {
      throw generateExc("Error when fetching repr", e);
    }
  }

  public Handle makeEmp() {
    return safeSingleCommand(EMP);
  }

  public Handle makeFalse() {
    return safeSingleCommand(FALSE);
  }

  public Handle loadString(String str) {
    if (str == null) {
      return new PartingstarHandle(0);
    }
    StringBuilder sb = new StringBuilder();
    sb.append(STRING);
    sb.append(' ');
    sb.append(str);
    sb.append('\n');
    String ret;
    try {
      synchronized (syncSentinel) {
        writeToProcess(sb.toString().getBytes());
        ret = readUntilEnter();
      }
    } catch (IOException e) {
      throw generateExc("Error when loading string", e);
    }
    return new PartingstarHandle(Integer.parseInt(ret));
  }

  public Handle extractEqs(Handle pHeap) {
    return safeSingleCommand(EXTRACTEQS, pHeap);
  }

  private Handle safeSingleCommand(String cmd, Handle... arguments) {
    try {
      return singleCommand(cmd, arguments);
    } catch (IOException e) {
      throw generateExc("Error when performing command " + cmd, e);
    }
  }

  private Handle singleCommand(String cmd, Handle... arguments) throws IOException {
    StringBuilder sb = new StringBuilder();
    sb.append(cmd);
    sb.append(' ');
    for (Handle arg : arguments) {
      sb.append(arg.toString());
      sb.append(' ');
    }
    sb.append('\n');
    String ret;
    synchronized (syncSentinel) {
      writeToProcess(sb.toString().getBytes());
      ret = readUntilEnter();
    }
    return new PartingstarHandle(Integer.parseInt(ret));
  }

  private List<Handle> listCommand(String cmd, Handle... arguments) throws IOException {
    StringBuilder sb = new StringBuilder();
    sb.append(cmd);
    sb.append(' ');
    for (Handle arg : arguments) {
      sb.append(arg.toString());
      sb.append(' ');
    }
    sb.append('\n');
    String ret;
    synchronized (syncSentinel) {
      writeToProcess(sb.toString().getBytes());
      ret = readUntilEnter();
    }
    List<Handle> retvals = new ArrayList<>();
    if ("".equals(ret)) {
      return retvals;
    }
    String[] strings = ret.split(" ");
    for (String string : strings) {
      retvals.add(new PartingstarHandle(Integer.parseInt(string)));
    }
    return retvals;
  }

  public static PartingstarInterface getInstance() {
    if (singleton == null) {
      throw new java.lang.IllegalStateException("Partingstar Interface was not initialized correctly.");
    }
    return singleton;
  }

  public static void prepare(Configuration config, LogManager pLogger) throws InvalidConfigurationException {
    singleton = new PartingstarInterface(config, pLogger);
  }

  private void writeToProcess(byte[] data) throws IOException {
    logger.log(Level.FINER, "Query:\n", new String(data));
    psProcess.getOutputStream().write(data);
    psProcess.getOutputStream().flush();
  }

  private int readFromProcess() throws IOException {
    int read = psProcess.getInputStream().read();
    if (read == -1) {
      throw new IOException("EOF?");
    }
    return read;
  }

  public boolean entails(Handle formula1, Handle formula2) {
    Timer t = new Timer();

    impTimer.start();
    try {
      t.start();
      Handle holds;
      try {
        holds = singleCommand(ENTAILS, formula1, formula2);
      } catch (IOException e) {
        throw generateExc("I/O with partingstar failed", e);
      }
      t.stop();
/*
      if (t.getSumTime() > maxImplicationDuration) {
        maxImplicationDuration = t.getSumTime();
        System.out.println(t + "::: " + query);
      }
      */

      logger.log(Level.FINER, "Reply: " + (holds.isNonZero() ? "HOLDS" : "DOESNOTHOLD"));
      return holds.isNonZero();
    } finally {
      impTimer.stop();
    }
  }

  public List<Handle> frame(Handle formula1, Handle formula2) {
    frameTimer.start();
    try {
      try {
        return listCommand(FRAME, formula1, formula2);
      } catch (IOException e) {
        throw generateExc("I/O with partingstar failed", e);
      }
    } finally {
      frameTimer.stop();
    }
  }

  @SuppressWarnings("unused")
  private List<String> readMultiple() throws IOException {
    List<String> retval = new ArrayList<>();
    String read = "42";
    while (!read.trim().equals("END")) {
      read = readUntilEnter();
      retval.add(read);
    }
    retval.remove(retval.size() - 1);
    logger.log(Level.FINER, "Reply:\n", retval);
    return retval;
  }

  private String readUntilEnter() throws IOException {
    StringBuilder retval = new StringBuilder();
    int lastRead = 0;
    while (true) {
      lastRead = readFromProcess();
      if (lastRead == '\n') {
        return retval.toString();
      }
      retval.append((char)lastRead);
    }
  }

  public List<Handle> abstract_(Handle formula1) {
    abstractionTimer.start();
    try {
      try {
        return listCommand(ABSTRACT, formula1);
      } catch (IOException e) {
        throw new RuntimeException("I/O with corestar failed", e);
      }
    } finally {
      abstractionTimer.stop();
    }
  }

  public Handle specAss(Handle pHeap, Handle pPre, Handle pPost, String pIdent) {
    specAssTimer.start();
    try {
      try {
        Handle retval = singleCommand(SPECASS, pHeap, pPre, pPost, loadString(pIdent));
        if (!retval.isNonZero()) {
          throw new SeplogicQueryUnsuccessful("Could not frame in spec ass");
        }
        return retval;
      } catch (IOException e) {
        throw generateExc("I/O with partingstar failed", e);
      }
    } finally {
      specAssTimer.stop();
    }
  }
}
