// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.eclipse.c;

import com.google.common.base.Ascii;
import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableList;
import com.google.common.io.CharStreams;
import com.google.common.io.MoreFiles;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.sosy_lab.cpachecker.exceptions.CParserException;

/** Detects Byte Order Mark (BOM) in a C file and decodes the file accordingly */
public class BOMParser {

  private enum ByteOrderMark {
    NO_BOM(Charset.defaultCharset(), ImmutableList.of()),
    UTF8_BOM(StandardCharsets.UTF_8, ImmutableList.of(0xEF, 0xBB, 0xBF)),
    UTF16_BE_BOM(StandardCharsets.UTF_16BE, ImmutableList.of(0xFE, 0xFF, 0xFE, 0xFF)),
    UTF16_LE_BOM(StandardCharsets.UTF_16LE, ImmutableList.of(0xFF, 0xFE, 0xFF, 0xFE)),
    UTF32_BE_BOM(
        Charset.forName("UTF-32BE"),
        ImmutableList.of(0x00, 0x00, 0xFE, 0xFF, 0x00, 0x00, 0xFE, 0xFF)),
    UTF32_LE_BOM(
        Charset.forName("UTF-32LE"),
        ImmutableList.of(0xFF, 0xFE, 0x00, 0x00, 0xFF, 0xFE, 0x00, 0x00)),
    UNKNOWN_BOM(Charset.defaultCharset(), ImmutableList.of());

    private final ImmutableList<Integer> sequence;
    private final Charset charset;

    ByteOrderMark(Charset charset, ImmutableList<Integer> sequence) {
      this.charset = charset;
      this.sequence = sequence;
    }
  }

  private static final int MAX_BOM_LENGTH =
      Arrays.stream(ByteOrderMark.values())
          .max(Comparator.comparingInt(bom -> bom.sequence.size()))
          .orElseThrow()
          .sequence
          .size();

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
  public static String filterAndDecode(Path pFilename) throws IOException, CParserException {
    try (BufferedInputStream in =
        new BufferedInputStream(MoreFiles.asByteSource(pFilename).openStream())) {
      List<Integer> codeBeginning = new ArrayList<>();
      int c = 0;
      int counter = 0;
      ByteOrderMark bom = ByteOrderMark.NO_BOM;
      in.mark(MAX_BOM_LENGTH);
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
          // Reset the stream to read the file from the beginning again
          in.reset();
          break;
        case UNKNOWN_BOM:
          throw new CParserException("Byte Order Mark is unknown");
        default:
          break;
      }
      String code;
      try (InputStreamReader reader = new InputStreamReader(in, bom.charset)) {
        code = CharStreams.toString(reader);
      }
      // If we have a BOM we need to check whether it contains only ascii values
      if (bom != ByteOrderMark.NO_BOM && !CharMatcher.ascii().matchesAllOf(code)) {
        throw new CParserException(bom.charset + " encoded file has non-ascii values");
      }
      return code;
    }
  }

  private static ByteOrderMark getBOM(List<Integer> codeBeginning) {
    if (isPureAscii(codeBeginning)) {
      return ByteOrderMark.NO_BOM;
    }
    for (ByteOrderMark bom : ByteOrderMark.values()) {
      if (codeBeginning.equals(bom.sequence)) {
        return bom;
      }
    }
    return ByteOrderMark.UNKNOWN_BOM;
  }

  private static boolean isPureAscii(List<Integer> code) {
    for (int b : code) {
      if (Ascii.MAX < b) {
        return false;
      }
    }
    return true;
  }
}
