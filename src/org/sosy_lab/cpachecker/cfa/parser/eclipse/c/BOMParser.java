/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.cfa.parser.eclipse.c;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import org.sosy_lab.cpachecker.exceptions.CParserException;

public class BOMParser {

  private static final byte[] UTF8_BYTE_ORDER_MARK = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

  private static final byte[] UTF16_BE_BYTE_ORDER_MARK = {
    (byte) 0xFE, (byte) 0xFF, (byte) 0xFE, (byte) 0xFF
  };

  private static final byte[] UTF16_LE_BYTE_ORDER_MARK = {
    (byte) 0xFF, (byte) 0xFE, (byte) 0xFF, (byte) 0xFE,
  };

  private static final byte[] UTF32_BE_BYTE_ORDER_MARK = {
    (byte) 0x00,
    (byte) 0x00,
    (byte) 0xFE,
    (byte) 0xFF,
    (byte) 0x00,
    (byte) 0x00,
    (byte) 0xFE,
    (byte) 0xFF
  };

  private static final byte[] UTF32_LE_BYTE_ORDER_MARK = {
    (byte) 0xFF,
    (byte) 0xFE,
    (byte) 0x00,
    (byte) 0x00,
    (byte) 0xFF,
    (byte) 0xFE,
    (byte) 0x00,
    (byte) 0x00
  };

  // ASCII Range
  private static final byte NULL_CHAR = (byte) 0x00;
  private static final byte DEL_CHAR = (byte) 0x7F;

  /**
   * Filters the BOM in the {@code input} if available. If BOM detected and filtered it is also
   * checked if {@code input} has further non ascii values
   *
   * @param input - the input code as byte array
   * @return String - the code as string
   * @throws CParserException - if we have a unknown BOM or a BOM file with non ascii characters in
   *     the code
   */
  public static String filterAndDecode(byte[] input) throws CParserException {
    if (hasNoBOM(input)) {
      return new String(input);
    }
    if (hasBOM(input, UTF8_BYTE_ORDER_MARK)) {
      byte[] filtered = filterBOM(input, UTF8_BYTE_ORDER_MARK);
      if (isPureAscii(filtered)) {
        return new String(filtered);
      }
      throw new CParserException(noAsciiValuesErrorMessage("UTF-8"));
    }
    if (hasBOM(input, UTF16_BE_BYTE_ORDER_MARK)) {
      byte[] filtered = filterBOM(input, UTF16_BE_BYTE_ORDER_MARK);
      if (isPureAscii(filtered)) {
        return Charset.forName("UTF-16BE").decode(ByteBuffer.wrap(filtered)).toString();
      }
      throw new CParserException(noAsciiValuesErrorMessage("UTF-16BE"));
    }
    if (hasBOM(input, UTF16_LE_BYTE_ORDER_MARK)) {
      byte[] filtered = filterBOM(input, UTF16_LE_BYTE_ORDER_MARK);
      if (isPureAscii(filtered)) {
        return Charset.forName("UTF-16LE").decode(ByteBuffer.wrap(filtered)).toString();
      }
      throw new CParserException(noAsciiValuesErrorMessage("UTF-16LE"));
    }
    if (hasBOM(input, UTF32_BE_BYTE_ORDER_MARK)) {
      byte[] filtered = filterBOM(input, UTF32_BE_BYTE_ORDER_MARK);
      if (isPureAscii(filtered)) {
        return Charset.forName("UTF-32BE").decode(ByteBuffer.wrap(filtered)).toString();
      }
      throw new CParserException(noAsciiValuesErrorMessage("UTF-32BE"));
    }
    if (hasBOM(input, UTF32_LE_BYTE_ORDER_MARK)) {
      byte[] filtered = filterBOM(input, UTF32_LE_BYTE_ORDER_MARK);
      if (isPureAscii(filtered)) {
        return Charset.forName("UTF-32LE").decode(ByteBuffer.wrap(filtered)).toString();
      }
      throw new CParserException(noAsciiValuesErrorMessage("UTF-32LE"));
    }
    throw new CParserException("Byte Order Mark Unknown");
  }

  private static boolean hasBOM(byte[] source, byte[] bomBytes) {
    if (source.length < bomBytes.length) {
      return false;
    }
    return Arrays.equals(Arrays.copyOfRange(source, 0, bomBytes.length), bomBytes);
  }

  private static byte[] filterBOM(byte[] source, byte[] bomBytes) {
    return Arrays.copyOfRange(source, bomBytes.length, source.length);
  }

  private static boolean isPureAscii(byte[] source) {
    for (int i = 0; i < source.length; i++) {
      if (source[i] < NULL_CHAR || source[i] > DEL_CHAR) {
        return false;
      }
    }
    return true;
  }

  private static boolean hasNoBOM(byte[] source) {
    // if we have values not in the ASCII Range for the first 4 values we have a BOM or other
    // non-ascii values
    for (int i = 0; i < 4; i++) {
      if (source[i] < NULL_CHAR || source[i] > DEL_CHAR) {
        return false;
      }
    }
    return true;
  }

  private static String noAsciiValuesErrorMessage(String usedEncoding) {
    return usedEncoding + " encoded file has non-ascii values";
  }
}