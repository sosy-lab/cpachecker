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
package org.sosy_lab.cpachecker.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.ProcessExecutor;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;

/*
 * This class provides basic access to some functionality provided by the CIL infrastructure.
 */

public class Cilly {

  public static void main(String[] pArguments) throws IOException, InvalidConfigurationException {
    assert(pArguments != null);

    Configuration config = Configuration.defaultConfiguration();
    LogManager logger = new LogManager(config);

    if (pArguments.length != 1) {
      logger.log(Level.SEVERE, "You have to specify a source file!");

    } else {
      String lNiceCILName = Cilly.getNiceCILName(pArguments[0]);

      Cilly lCilly = new Cilly(logger);

      lCilly.cillyfy(pArguments[0], lNiceCILName);

      logger.log(Level.INFO, "Wrote output to", lNiceCILName);
    }
  }

  private static final String mCillyAsmExePath = "cilly.asm.exe";
  private final LogManager logger;

  // TODO: make set flags explicit via method calls?
  private boolean mDoSimplify = true;
  private boolean mPrintCilAsIs = true;
  // TODO: irrelevant for cilly.asm.exe
  // TODO: why is this option requested by HowTo.txt?
  //private boolean mSaveTemps = true;
  private boolean mDoMakeCFG = true;
  private boolean mDoSimpleMem = false;

  public Cilly(LogManager logger) {
    this.logger = logger;
  }

  public void setDoSimplify(boolean pValue) {
    mDoSimplify = pValue;
  }

  public void setPrintCilAsIs(boolean pValue) {
    mPrintCilAsIs = pValue;
  }

  /*public void setSaveTemps(boolean pValue) {
    mSaveTemps = pValue;
  }*/

  public void setDoMakeCFG(boolean pValue) {
    mDoMakeCFG = pValue;
  }

  public void setDoSimpleMem(boolean pValue) {
    mDoSimpleMem = pValue;
  }

  public boolean isCillyInvariant(String pSourceFile) throws IOException {
    if (pSourceFile == null) {
      throw new IllegalArgumentException("Parameter is null!");
    }

    return isCillyInvariant(new File(pSourceFile));
  }

  public boolean isCillyInvariant(File pSourceFile) throws IOException {
    if (pSourceFile == null) {
      throw new IllegalArgumentException();
    }

    if (!pSourceFile.exists()) {
      throw new IllegalArgumentException("File " + pSourceFile.getAbsolutePath() + " does not exist!");
    }

    if (!pSourceFile.canRead()) {
      throw new IllegalArgumentException();
    }

    File lCillyfiedFile = cillyfy(pSourceFile);

    BufferedReader lSourceReader = new BufferedReader(new FileReader(pSourceFile));

    BufferedReader lCillyfiedReader = new BufferedReader(new FileReader(lCillyfiedFile));

    do {
      String lSourceLine = lSourceReader.readLine();
      String lCillyfiedLine = lCillyfiedReader.readLine();

      if (lSourceLine == null) {
        lSourceReader.close();
        lCillyfiedReader.close();

        return (lCillyfiedLine == null);
      }
      else if (!lSourceLine.trim().equals(lCillyfiedLine.trim())) {
        lSourceReader.close();
        lCillyfiedReader.close();

        return false;
      }
    }
    while (true);
  }

  public File cillyfy(String pSourceFile) throws IOException {
    assert(pSourceFile != null);

    return cillyfy(new File(pSourceFile));
  }

  public static String getNiceCILName(String pFileName) {
    assert(pFileName != null);

    int lLastIndex = pFileName.lastIndexOf(".");

    String lPrefix;
    String lPostfix;

    if (lLastIndex < 0) {
      lPrefix = pFileName;
      lPostfix = "c";
    }
    else {
      // TODO what if lLastIndex == 0?

      lPrefix = pFileName.substring(0, lLastIndex);
      lPostfix = pFileName.substring(lLastIndex + 1, pFileName.length());
    }

    return lPrefix + ".cil." + lPostfix;
  }

  public File cillyfy(File pSourceFile) throws IOException {
    if (pSourceFile == null) {
      throw new IllegalArgumentException();
    }

    String lSourceFileString = pSourceFile.getName();

    int lLastIndex = lSourceFileString.lastIndexOf(".");

    String lPrefix;
    String lPostfix;

    if (lLastIndex < 0) {
      lPrefix = lSourceFileString;
      lPostfix = "c";
    }
    else {
      // TODO what if lLastIndex == 0?

      lPrefix = lSourceFileString.substring(0, lLastIndex);
      lPostfix = lSourceFileString.substring(lLastIndex + 1, lSourceFileString.length());
    }

    File lTargetFile = File.createTempFile(lPrefix + ".cil.", "." + lPostfix);

    cillyfy(pSourceFile, lTargetFile);

    return lTargetFile;
  }

  public File cillyfy(String pSourceFile, String pTargetFile) throws IOException {
    assert(pSourceFile != null);
    assert(pTargetFile != null);

    File lTargetFile = new File(pTargetFile);

    cillyfy(new File(pSourceFile), lTargetFile);

    return lTargetFile;
  }

  public void cillyfy(File pSourceFile, File pTargetFile) throws IOException {
    if (pSourceFile == null) {
      throw new IllegalArgumentException();
    }

    if (!pSourceFile.exists()) {
      throw new IllegalArgumentException();
    }

    if (!pSourceFile.canRead()) {
      throw new IllegalArgumentException();
    }

    if (pTargetFile == null) {
      throw new IllegalArgumentException();
    }

    StringBuffer lOptionsString = new StringBuffer();

    if (mDoSimplify) {
      lOptionsString.append(" --dosimplify");
    }

    if (mPrintCilAsIs) {
      lOptionsString.append(" --printCilAsIs");
    }

    // TODO: irrelevant for cilly.asm.exe
    // TODO: why is this option requested by HowTo.txt?
    /*if (mSaveTemps) {
      lOptionsString.append(" --save-temps");
    }*/

    if (mDoMakeCFG) {
      lOptionsString.append(" --domakeCFG");
    }

    if (mDoSimpleMem) {
      lOptionsString.append(" --dosimpleMem");
    }

    lOptionsString.append(" --out ");
    lOptionsString.append(pTargetFile.toString());

    String lExecString = mCillyAsmExePath + lOptionsString.toString() + " " + pSourceFile.getAbsolutePath();

    ProcessExecutor<IOException> lCillyProcess = new ProcessExecutor<IOException>(logger, IOException.class, lExecString.split(" "));
    try {
      int lExitValue = lCillyProcess.join();
      if (lExitValue != 0) {
        throw new RuntimeException("Cilly processing failed!");
      }
    } catch (InterruptedException e) {
      // TODO propagate
      Thread.currentThread().interrupt();
    }
  }
}