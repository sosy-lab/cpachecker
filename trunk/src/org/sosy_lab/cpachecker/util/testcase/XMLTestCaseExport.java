// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.testcase;

import com.google.common.base.Preconditions;
import com.google.common.xml.XmlEscapers;
import java.io.IOException;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.specification.Property;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon;

public class XMLTestCaseExport {

  private XMLTestCaseExport() {}

  public static void writeXMLMetadata(
      final Appendable pWriter,
      final CFA pCfa,
      final @Nullable Property pProp,
      final String producerString)
      throws IOException {

    Preconditions.checkArgument(pCfa.getFileNames().size() == 1);
    Path programFile = pCfa.getFileNames().get(0);

    pWriter.append(
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
            + "<!DOCTYPE test-metadata SYSTEM"
            + " \"https://gitlab.com/sosy-lab/software/test-format/blob/master/test-metadata.dtd\">\n");
    pWriter.append("<test-metadata>\n");

    pWriter.append("\t<sourcecodelang>");
    pWriter.append(pCfa.getLanguage().toString());
    pWriter.append("</sourcecodelang>\n");

    pWriter.append("\t<producer>");
    pWriter.append(XmlEscapers.xmlContentEscaper().escape(producerString));
    pWriter.append("</producer>\n");

    if (pProp != null) {
      pWriter.append("\t<specification>");
      pWriter.append(pProp.toFullString(pCfa));
      pWriter.append("</specification>\n");
    } else {
      pWriter.append("\t<specification/>\n");
    }

    pWriter.append("\t<programfile>");
    pWriter.append(programFile.toString());
    pWriter.append("</programfile>\n");

    pWriter.append("\t<programhash>");
    pWriter.append(AutomatonGraphmlCommon.computeHash(programFile));
    pWriter.append("</programhash>\n");

    pWriter.append("\t<entryfunction>");
    pWriter.append(pCfa.getMainFunction().getFunctionName());
    pWriter.append("</entryfunction>\n");

    pWriter.append("\t<architecture>");
    pWriter.append(AutomatonGraphmlCommon.getArchitecture(pCfa.getMachineModel()));
    pWriter.append("</architecture>\n");

    pWriter.append("\t<creationtime>");
    pWriter.append(
        ZonedDateTime.now(ZoneId.systemDefault())
            .withNano(0)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    pWriter.append("</creationtime>\n");

    pWriter.append("</test-metadata>");
  }

  public static final TestValuesToFormat XML_TEST_CASE =
      valueList -> {
        StringBuilder strB = new StringBuilder();
        strB.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n");
        strB.append(
            "<!DOCTYPE testcase SYSTEM"
                + " \"https://gitlab.com/sosy-lab/software/test-format/blob/master/testcase.dtd\">\n");
        strB.append("<testcase>\n");
        for (String value : valueList) {
          strB.append("\t<input>");
          strB.append(value);
          strB.append("</input>\n");
        }
        strB.append("</testcase>\n");
        return strB.toString();
      };
}
