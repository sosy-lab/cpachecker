/*
 *  CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.util.test;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

public class StatisticsParser implements StringStreamObserver {

  private static final Pattern keyValuePattern = Pattern.compile("(.*):(.*)");

  private final PrintStream printSrm;
  private final InterceptorStream interceptor;
  private final Map<String, String> keyValues;

  private static class InterceptorStream extends OutputStream {

    private final StringStreamObserver receiver;

    public InterceptorStream(StringStreamObserver pReceiver) {
      receiver = Preconditions.checkNotNull(pReceiver);
    }

    @Override
    public void write(byte[] pB) throws IOException {
      final String s =  new String(pB);
      receiver.onString(s);
    }

    @Override
    public void write(byte[] pB, int pOff, int pLen) throws IOException {
      write(Arrays.copyOfRange(pB, pOff, pOff + pLen));
    }

    @Override
    public void write(int pB) throws IOException {
    }

  }

  public StatisticsParser() {
    interceptor = new InterceptorStream(this);
    printSrm = new PrintStream(interceptor);
    keyValues = Maps.newHashMap();
  }

  @Override
  public void onString(String pStr) {
    Matcher matcher = keyValuePattern.matcher(pStr);

    if (matcher.matches()) {
        String key = matcher.group(1).trim();
        String value = matcher.group(2).trim();

        keyValues.put(key, value);
    }
  }

  public PrintStream getPrintStream() {
    return printSrm;
  }

  public ImmutableMap<String, String> getStatistics() {
    return ImmutableMap.copyOf(keyValues);
  }

  public void dumpStatistics(PrintStream pTarget) {
    for (Entry<String, String> e: getStatistics().entrySet()) {
      pTarget.append(e.getKey());
      pTarget.append("\t");
      pTarget.append(e.getValue());
      pTarget.append("\n");
    }
  }

}
