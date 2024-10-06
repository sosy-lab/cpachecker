// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.export.json.deserialization;

import com.fasterxml.jackson.databind.util.StdConverter;
import org.sosy_lab.cpachecker.cfa.export.json.CfaJsonImport;
import org.sosy_lab.cpachecker.cfa.export.json.mixins.CAssumeEdgeMixin;

/**
 * A converter that removes the leading and trailing brackets from a given string.
 *
 * <p>If the input string starts with "[" and ends with "]", the brackets are removed. Otherwise,
 * the input string is returned as is.
 *
 * <p>If the input string is null, null is returned.
 *
 * <p>For example, if this converter is applied to a field "rawStatement" like this:
 *
 * <pre>
 * *AT*JsonDeserialize(converter=BracketRemoverConverter.class)
 * private String rawStatement;
 * </pre>
 *
 * <p>It acts like a filter that removes brackets from the field before it is passed to the
 * constructor of the class that contains the field.
 *
 * <p>Examples:
 *
 * <pre>
 * <table border="1">
 *   <tr><th>JSON</th>                             <th>Converter</th>        <th>Constructor</th></tr>
 *   <tr><td>"rawStatement": "[i == 20]"</td><td>-> "[i == 20]"</td>   <td>-> "i == 20"</td></tr>
 *   <tr><td>"rawStatement": ""</td>         <td>-> ""</td>            <td>-> ""</td></tr>
 *   <tr><td>"rawStatement": null</td>       <td>-> null</td>          <td>-> null</td></tr>
 * </table>
 * </pre>
 *
 * @see CfaJsonImport
 * @see CAssumeEdgeMixin
 */
public final class BracketRemoverConverter extends StdConverter<String, String> {

  @Override
  public String convert(String pInput) {
    if (pInput == null) {
      return null;
    }

    if (pInput.startsWith("[") && pInput.endsWith("]")) {
      return pInput.substring(1, pInput.length() - 1);
    } else {
      return pInput;
    }
  }
}
