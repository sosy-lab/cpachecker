/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.statistics;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.List;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.cpachecker.util.statistics.interfaces.Aggregateable;
import org.sosy_lab.cpachecker.util.statistics.interfaces.Context;
import org.sosy_lab.cpachecker.util.statistics.interfaces.NoStatisticsException;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;

public class ContextCsv {

  private List<Object> columStatKeys = Lists.newArrayList(); // needed to have an ordering of stat keys
  private LinkedHashMultimap<Object, String> columnAttributeMap = LinkedHashMultimap.create();

  private final PrintWriter csv;
  private boolean headerWritten = false;

  public ContextCsv(final Path pTargetFile) throws InvalidConfigurationException {
    try {
      Writer w = Files.openOutputFile(pTargetFile);
      this.csv = new PrintWriter(w);
    } catch (IOException e) {
      throw new InvalidConfigurationException("Target file invalid!", e);
    }
  }

  public void close() {
    csv.close();
  }

  public void registerColumn(final Object pKey, final String[] pAttributes) {
    Preconditions.checkState(!headerWritten);

    columStatKeys.add(pKey);
    columnAttributeMap.putAll(pKey, Lists.newArrayList(pAttributes));
  }

  private String getNaValue() {
    return "";
  }

  public void writeCsvHeader() {

    boolean needDelim = false;

    for (Object key: columStatKeys) {
      for (String att: columnAttributeMap.get(key)) {

        final String colName = (key.toString().replace(" ", "_") + "_" + att).toLowerCase();

        if (needDelim) {
          csv.print('\t');
        }
        csv.print(colName);
        needDelim = true;
      }
    }

    csv.println();
    csv.flush();

    headerWritten = true;
  }


  public void appendContextRow(final Context pContext) {

    if (!headerWritten) {
      writeCsvHeader();
    }

    boolean needDelim = false;

    for (Object key: columStatKeys) {
      for (String att: columnAttributeMap.get(key)) {

        String colValue;

        try {
          Aggregateable a = pContext.getStatistic(key, Aggregateable.class);
          ImmutableMap<String, ? extends Object> attVals = a.getAttributeValueMap();

          Object val = attVals.get(att);
          if (val == null) {
            colValue = getNaValue();
          } else {
            colValue = val.toString();
          }

        } catch (NoStatisticsException e) {
          colValue = getNaValue();
        }

        if (needDelim) {
          csv.print('\t');
        }
        csv.print(colValue);
        needDelim = true;
      }
    }

    csv.println("");

    csv.flush();
  }

}
