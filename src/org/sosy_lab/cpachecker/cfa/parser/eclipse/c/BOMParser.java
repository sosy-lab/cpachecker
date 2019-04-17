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

import com.google.common.base.Ascii;
import com.google.common.collect.ImmutableList;
import com.google.common.io.MoreFiles;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.cpachecker.exceptions.CParserException;

/** Detects Byte Order Mark (BOM) in a C file and decodes the file accordingly */
public class BOMParser {

  private enum ByteOrderMark {
    NO_BOM("Without BOM", ImmutableList.of()),
    UTF8_BOM("UTF8", ImmutableList.of(0xEF, 0xBB, 0xBF)),
    UTF16_BE_BOM("UTF16 BE", ImmutableList.of(0xFE, 0xFF, 0xFE, 0xFF)),
    UTF16_LE_BOM("UTF16 LE", ImmutableList.of(0xFF, 0xFE, 0xFF, 0xFE)),
    UTF32_BE_BOM("UTF32 BE", ImmutableList.of(0x00, 0x00, 0xFE, 0xFF, 0x00, 0x00, 0xFE, 0xFF)),
    UTF32_LE_BOM("UTF32 LE", ImmutableList.of(0xFF, 0xFE, 0x00, 0x00, 0xFF, 0xFE, 0x00, 0x00)),
    UNKNOWN_BOM("Unknown BOM", ImmutableList.of());

    private final String encoding;
    private final ImmutableList<Integer> sequence;

    ByteOrderMark(String encoding, ImmutableList<Integer> sequence) {
      this.encoding = encoding;
      this.sequence = sequence;
    }
  }

  private static final int MAX_BOM_LENGTH = 8;

  /**
   * Filters the BOM in the {@code pFilename} if present. If a BOM is detected the corresponding
   * charset is applied and the file is read starting at the first character after the BOM. If no
   * BOM is detected the default charset is applied and the file is read starting from the beginning
   * of the file.
   *
   * @param pFilename - the file name as string
   * @return String - the code as string
   * @throws CParserException - if we have a unknown BOM or a BOM file with non ascii characters in
   *     the code
   */
  public static String filterAndDecode(String pFilename) throws IOException, CParserException {
    BufferedReader bufferedReader = null;
    try (InputStream in = MoreFiles.asByteSource(Paths.get(pFilename)).openStream()) {
      List<Integer> codeBeginning = new ArrayList<>();
      int c = 0;
      int counter = 0;
      ByteOrderMark bom = ByteOrderMark.NO_BOM;
      while ((c = in.read()) > -1 && counter < MAX_BOM_LENGTH) {
        codeBeginning.add(c);
        counter++;
        bom = getBOM(codeBeginning);
        if (bom != ByteOrderMark.NO_BOM && bom != ByteOrderMark.UNKNOWN_BOM) {
          break;
        }
      }
      switch (bom) {
        case NO_BOM:
          // Read the file from the beginning again
          return MoreFiles.asCharSource(Paths.get(pFilename), Charset.defaultCharset()).read();
        case UTF8_BOM:
          bufferedReader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
          break;
        case UTF16_BE_BOM:
          bufferedReader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_16BE));
          break;
        case UTF16_LE_BOM:
          bufferedReader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_16LE));
          break;
        case UTF32_BE_BOM:
          bufferedReader =
              new BufferedReader(new InputStreamReader(in, Charset.forName("UTF-32BE")));
          break;
        case UTF32_LE_BOM:
          bufferedReader =
              new BufferedReader(new InputStreamReader(in, Charset.forName("UTF-32LE")));
          break;
        default:
          throw new CParserException("Byte Order Mark Unknown");
      }
      StringBuilder sb = new StringBuilder();
      int data;
      while ((data = bufferedReader.read()) != -1) {
        // when we continue to read the data in a BOM file we do not want any no ascii characters
        if (Ascii.MAX < data) {
          throw new CParserException(noAsciiValuesErrorMessage(bom));
        }
        sb.append((char) data);
      }
      return sb.toString();
    } finally {
      if (bufferedReader != null) {
        bufferedReader.close();
      }
    }
  }

  private static ByteOrderMark getBOM(List<Integer> codeBeginning) {
    if (isPureAscii(codeBeginning)) {
      return ByteOrderMark.NO_BOM;
    } else if (containsBOM(codeBeginning, ByteOrderMark.UTF8_BOM.sequence)) {
      return ByteOrderMark.UTF8_BOM;
    } else if (containsBOM(codeBeginning, ByteOrderMark.UTF16_LE_BOM.sequence)) {
      return ByteOrderMark.UTF16_LE_BOM;
    } else if (containsBOM(codeBeginning, ByteOrderMark.UTF16_BE_BOM.sequence)) {
      return ByteOrderMark.UTF16_BE_BOM;
    } else if (containsBOM(codeBeginning, ByteOrderMark.UTF32_LE_BOM.sequence)) {
      return ByteOrderMark.UTF32_LE_BOM;
    } else if (containsBOM(codeBeginning, ByteOrderMark.UTF32_BE_BOM.sequence)) {
      return ByteOrderMark.UTF32_BE_BOM;
    } else {
      return ByteOrderMark.UNKNOWN_BOM;
    }
  }

  private static boolean containsBOM(List<Integer> codeBeginning, List<Integer> bomBytes) {
    if (codeBeginning.size() < bomBytes.size()) {
      return false;
    }
    for (int i = 0; i < bomBytes.size(); i++) {
      if (!codeBeginning.get(i).equals(bomBytes.get(i))) {
        return false;
      }
    }
    return true;
  }

  private static boolean isPureAscii(List<Integer> code) {
    for (int b : code) {
      if (Ascii.MAX < b) {
        return false;
      }
    }
    return true;
  }

  private static String noAsciiValuesErrorMessage(ByteOrderMark bom) {
    return bom.encoding + " encoded file has non-ascii values";
  }
}
