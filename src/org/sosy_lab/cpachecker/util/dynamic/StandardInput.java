/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.dynamic;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.logging.Level;

/**
 * Provides information given to a program as standard input at runtime.
 *
 * The standard input is specified in a file. This file is provided using
 * the configuration of CPAchecker.
 */
@Options(prefix="dynamic")
public class StandardInput {

  @Option(secure=true,
      description="File that contains the standard input."
          + " It is interpreted like real standard input,"
          + " e.g. a newline represents a press of 'Enter'")
  private String inputFile = null;

  private final LogManager logger;

  private List<String> lines;
  private Iterator<String> nextLine;
  private Deque<Character> currentlyRemainingLine = new LinkedList<>();

  /**
   * Constructor for testing.
   * @param pInputString the String representing standard input
   */
  StandardInput(final String pInputString) {
    Scanner s = new Scanner(pInputString);
    lines = getContentsAsList(s);
    nextLine = lines.iterator();

    logger = LogManager.createTestLogManager();
  }

  public StandardInput(final Configuration pConfig, final LogManager pLogger)
      throws InvalidConfigurationException {
    pConfig.inject(this);

    logger = pLogger;

    try {
      lines = readFile(inputFile);
      nextLine = lines.iterator();
    } catch (IOException e) {
      throw new InvalidConfigurationException("Error reading file " + inputFile + " .", e);
    }
  }

  private List<String> readFile(final String pFileName) throws IOException {
    File input = new File(pFileName);

    if (!input.exists() || !input.isFile() || !input.canRead()) {
      throw new IOException("File " + pFileName + " can't be read.");
    }

    try (Scanner fileRead = new Scanner(input)) {
      return getContentsAsList(fileRead);
    }
  }

  private List<String> getContentsAsList(final Scanner pScanner) {
    List<String> fileContents = new ArrayList<>();
    while (pScanner.hasNextLine()) {
      fileContents.add(pScanner.nextLine());
    }

    return fileContents;
  }


  public boolean hasNext() {
    return nextLine.hasNext() || !currentlyRemainingLine.isEmpty();
  }

  /**
   * Return next line of standard input.
   * Only works if a next line exists.
   *
   * @return next line of standard input, no format applied
   * @throws IllegalStateException if no next line exists
   * @see #hasNext()
   */
  public String getNext() {
    if (!currentlyRemainingLine.isEmpty()) {
      throw new IllegalStateException("Current line started with some format but not"
          + " completely consumed");

    } else if (!nextLine.hasNext()) {
      throw new IllegalStateException("No next line.");

    } else {
      return nextLine.next();
    }
  }

  /**
   * Returns next line of standard input, formatted as specified by the
   * <code>scanf</code> output format string given.
   *
   * @param pFormat an output format string in the style of C's <code>scanf</code>
   *                that specifies how to interpret the next line's values
   * @return a List containing the separate elements of the next line after applying the format
   *                string
   * formatting
   */
  public List<InputValue> getNext(final String pFormat) {
    if (currentlyRemainingLine.isEmpty()) {
      final String rawLine = getNext();
      currentlyRemainingLine = buildStack(rawLine);
    }

    List<InputValue> formattedStrings = new ArrayList<>();
    FormatElement currentFormatElement = new RootElement();

    // FormatElement objects are supposed to handle the case that the remaining line is empty,
    // e.g. doing nothing or returning an empty String.
    for (int p = 0; p < pFormat.length(); p++) {
      currentFormatElement = currentFormatElement.feed(pFormat.charAt(p));
      if (currentFormatElement.isComplete()) {
        Optional<InputValue> producedValue =
            currentFormatElement.applyToString(currentlyRemainingLine);
        if (producedValue.isPresent()) {
          formattedStrings.add(producedValue.get());
        }
        currentFormatElement = new RootElement();
      }
    }
    if (!(currentFormatElement instanceof RootElement)) {
      throw new IllegalStateException("Format not complete: " + pFormat);
    } else {
      return formattedStrings;
    }
  }

  private Deque<Character> buildStack(final String pString) {
    final Deque<Character> stack = new LinkedList<>();
    for (int p = pString.length() - 1; p >= 0; p--) {
      stack.push(pString.charAt(p));
    }
    return stack;
  }

  private interface FormatElement {
    FormatElement feed(char pNextChar);
    boolean isComplete();
    Optional<InputValue> applyToString(Deque<Character> pCharStack);
  }

  private static abstract class IncompleteElement implements FormatElement {

    @Override
    public boolean isComplete() {
      return false;
    }

    @Override
    public Optional<InputValue> applyToString(final Deque<Character> pCharStack) {
      throw new IllegalStateException("Element not complete!");
    }
  }

  private static abstract class CompleteElement implements FormatElement {

    private final int lineWidth;

    public CompleteElement() {
      lineWidth = Integer.MAX_VALUE;
    }

    public CompleteElement(final int pLineWidth) {
      lineWidth = pLineWidth;
    }

    public int getLineWidth() {
      return lineWidth;
    }

    @Override
    public FormatElement feed(char pNextChar) {
      throw new IllegalStateException("Element is complete!");
    }

    @Override
    public boolean isComplete() {
      return true;
    }
  }

  private class RootElement extends IncompleteElement {

    @Override
    public FormatElement feed(final char pNextChar) {
      switch (pNextChar) {
        case '%':
          return new PlaceHolder();
        case ' ':
          return new WhiteSpace();
        default:
          return new ConstantElement(pNextChar);
      }
    }

    public boolean isComplete() {
      return false;
    }

    public Optional<InputValue> applyToString(final Deque<Character> pCharStack) {
      throw new IllegalStateException("FormatElement not complete!");
    }
  }

  private class ConstantElement extends CompleteElement {

    private char character;

    public ConstantElement(final char pChar) {
      character = pChar;
    }

    @Override
    public Optional<InputValue> applyToString(Deque<Character> pCharStack) {
      Character nextChar = pCharStack.peek();

      if (nextChar == character) {
        pCharStack.pop();
      }

      return Optional.empty(); // Constants are not assigned to any variable in scanf
    }
  }

  private class PlaceHolder extends IncompleteElement {

    private boolean lineWidthSpecified = false;
    private int lineWidth = 0;

    @Override
    public FormatElement feed(final char pNextChar) {
      if (Character.isDigit(pNextChar)) {
        lineWidthSpecified = true;
        int digit = Integer.parseInt(Character.toString(pNextChar));
        // Shift existing number one to the left to get full number
        // Example:
        //   '532' is parsed, position is before '2'.
        //   Current lineWidth = 53; New lineWidth = 530 + 2 = 532
        lineWidth = lineWidth * 10 + digit;
        return this;

      } else {
        switch(pNextChar) {
          case '%':
            return new ConstantElement('%');
          case 'd':
            return lineWidthSpecified ? new IntegerDec(lineWidth) : new IntegerDec();
          default:
            throw new AssertionError("Placeholder component " + pNextChar + " not supported");
        }
      }
    }
  }

  private class IntegerDec extends CompleteElement {

    public IntegerDec() {
      super();
    }

    public IntegerDec(int pLineWidth) {
      super(pLineWidth);
    }

    @Override
    public Optional<InputValue> applyToString(final Deque<Character> pCharStack) {
      StringBuilder number = new StringBuilder();
      for (int p = 0; p < getLineWidth(); p++) {
        if (pCharStack.isEmpty()) {
          break;
        }

        // Only pop if the value is consumed, i.e. is part of the integer
        char currentChar = pCharStack.peek();

        if ((!('-' == currentChar) && !Character.isDigit(currentChar))
            || ('-' == currentChar && number.length() > 0)) {
          break;
        } else {
          number.append(currentChar);
          pCharStack.pop();
        }
      }

      Value value;
      try {
        value = new NumericValue(Integer.parseInt(number.toString()));
      } catch (NumberFormatException e) {
        logger.log(Level.WARNING, "Format '%d' for scanf did not match any input."
            + " Behavior is not defined");
        value = UnknownValue.getInstance();
      }

      return Optional.of(new InputValue(value, CNumericTypes.INT));
    }
  }

  private static class WhiteSpace extends CompleteElement {

    @Override
    public Optional<InputValue> applyToString(final Deque<Character> pCharStack) {
      if (!pCharStack.isEmpty()) {
        char currentChar = pCharStack.peek();
        while (!pCharStack.isEmpty() && Character.isSpaceChar(currentChar)) {
          pCharStack.pop();
          currentChar = pCharStack.peek();
        }
      }

      return Optional.empty();
    }
  }
}
