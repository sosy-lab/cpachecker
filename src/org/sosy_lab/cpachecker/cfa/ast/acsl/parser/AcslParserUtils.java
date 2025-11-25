// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AcslParserUtils {

  public static String stripCommentMarker(String pCommentString) {
    String commentString = pCommentString;
    if (pCommentString.startsWith("//@")) {
      Pattern pattern = Pattern.compile("(//@(\\s)*)(?<content>.*)");
      Matcher matcher = pattern.matcher(pCommentString);
      if (matcher.matches()) {
        commentString = matcher.group("content");
      }
    } else if (pCommentString.startsWith("/*@")) {
      Pattern pattern = Pattern.compile("/\\*@\\s*(?<content>(.*\\s*)*)\\*/");
      Matcher matcher = pattern.matcher(commentString);
      if (matcher.matches()) {
        commentString = matcher.group("content");
      }
    }

    return commentString;
  }
}
