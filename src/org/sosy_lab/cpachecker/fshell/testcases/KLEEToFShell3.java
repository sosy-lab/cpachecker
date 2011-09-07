/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.fshell.testcases;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

public class KLEEToFShell3 {

  enum State {
    STARTED,
    READ_FILE_NAME,
    READ_ARGS,
    READ_NUM_OBJECTS,
    READ_NAME,
    READ_SIZE,
    READ_DATA
  }

  public static TestCase translate(String pFileName) throws IOException {
    return translate(new FileInputStream(pFileName));
  }

  public static TestCase translate(InputStream pInputStream) throws IOException {
    BufferedReader lReader = new BufferedReader(new InputStreamReader(pInputStream));

    List<Integer> lInputValues = new LinkedList<Integer>();

    State lState = State.STARTED;

    int lNumObjects = -1;

    String lLine = null;

    int lCurrentSize = -1;

    while ((lLine = lReader.readLine()) != null) {
      switch (lState) {
      case STARTED:
        lState = State.READ_FILE_NAME;
        break;
      case READ_FILE_NAME:
        lState = State.READ_ARGS;
        break;
      case READ_ARGS:
        String lPrefix = "num objects: ";
        String lStringValue = lLine.substring(lPrefix.length()).trim();

        lNumObjects = Integer.valueOf(lStringValue);

        lState = State.READ_NUM_OBJECTS;
        break;
      case READ_NUM_OBJECTS:
      case READ_DATA:
        String lNamePattern = "name: ";

        int lNameIndex = lLine.indexOf(lNamePattern);

        String lNameString = lLine.substring(lNameIndex + lNamePattern.length()).trim();
        lNameString = lNameString.substring(1, lNameString.length() - 1);

        if (!lNameString.equals("input")) {
          throw new RuntimeException();
        }

        lState = State.READ_NAME;
        break;
      case READ_NAME:
        String lSizePattern = "size: ";

        int lSizeIndex = lLine.indexOf(lSizePattern);

        String lSizeStringValue = lLine.substring(lSizeIndex + lSizePattern.length()).trim();

        lCurrentSize = Integer.valueOf(lSizeStringValue);

        if (lCurrentSize != 4) {
          throw new RuntimeException("Number of bytes: " + lCurrentSize);
        }

        lState = State.READ_SIZE;
        break;
      case READ_SIZE:
        String lDataPattern = "data: ";

        int lDataIndex = lLine.indexOf(lDataPattern);

        String lDataString = lLine.substring(lDataIndex + lDataPattern.length()).trim();

        lDataString = lDataString.substring(1, lDataString.length() - 1);

        if (!lDataString.matches("(\\\\x(\\d|[a-fA-F])(\\d|[a-fA-F]))*")) {
          throw new RuntimeException("Invalid data string: " + lDataString);
        }

        String[] lByteStrings = lDataString.split("\\\\x");

        if (lByteStrings.length != lCurrentSize + 1) {
          throw new RuntimeException("Non-matching size: " + (lByteStrings.length - 1) + " vs. " + lCurrentSize);
        }

        int lResultValue = 0;

        for (int i = 1; i < lByteStrings.length; i++) {
          String lByteString = lByteStrings[i];
          String lFirstByte = lByteString.substring(0, 1);
          String lSecondByte = lByteString.substring(1, 2);

          int lFirstByteValue = getValue(lFirstByte);
          int lSecondByteValue = getValue(lSecondByte);

          int lTmpValue = 0;
          lTmpValue |= (lFirstByteValue << 4);
          lTmpValue |= lSecondByteValue;

          lResultValue |= (lTmpValue << (8 * (i - 1)));
        }

        lInputValues.add(lResultValue);

        lState = State.READ_DATA;
        break;
      }
    }

    if (lNumObjects != lInputValues.size()) {
      throw new RuntimeException(lNumObjects + " vs. " + lInputValues.size());
    }

    TestCase lTestCase = new PreciseInputsTestCase(lInputValues);

    return lTestCase;
  }

  private static int getValue(String pHexValue) {
    if (pHexValue.equals("0")) {
      return 0;
    }
    else if (pHexValue.equals("1")) {
      return 1;
    }
    else if (pHexValue.equals("2")) {
      return 2;
    }
    else if (pHexValue.equals("3")) {
      return 3;
    }
    else if (pHexValue.equals("4")) {
      return 4;
    }
    else if (pHexValue.equals("5")) {
      return 5;
    }
    else if (pHexValue.equals("6")) {
      return 6;
    }
    else if (pHexValue.equals("7")) {
      return 7;
    }
    else if (pHexValue.equals("8")) {
      return 8;
    }
    else if (pHexValue.equals("9")) {
      return 9;
    }
    else if (pHexValue.toLowerCase().equals("a")) {
      return 10;
    }
    else if (pHexValue.toLowerCase().equals("b")) {
      return 11;
    }
    else if (pHexValue.toLowerCase().equals("c")) {
      return 12;
    }
    else if (pHexValue.toLowerCase().equals("d")) {
      return 13;
    }
    else if (pHexValue.toLowerCase().equals("e")) {
      return 14;
    }
    else if (pHexValue.toLowerCase().equals("f")) {
      return 15;
    }
    else {
      throw new IllegalArgumentException(pHexValue);
    }
  }

  /**
   * @param args
   * @throws IOException
   */
  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      System.err.println("Usage: java " + KLEEToFShell3.class.getCanonicalName() + " <ktest-output-file>");

      return;
    }

    String lTestCaseFile = args[0];

    TestCase lFShell3TestCase = translate(lTestCaseFile);
    System.out.println(lFShell3TestCase);
  }

}
