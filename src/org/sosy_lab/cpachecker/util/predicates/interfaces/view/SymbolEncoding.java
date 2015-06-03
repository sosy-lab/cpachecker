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
package org.sosy_lab.cpachecker.util.predicates.interfaces.view;

import static org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType.getBitvectorTypeWithSize;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sosy_lab.common.Appender;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;

import com.google.common.base.Joiner;


public class SymbolEncoding {

  public SymbolEncoding() {}

  private final Map<String, Type<FormulaType<?>>> encodedSymbols = new HashMap<>();

  public void put(String symbol, int length) {
    put(symbol, new Type<FormulaType<?>>(getBitvectorTypeWithSize(length)));
  }

  public void put(String symbol, FormulaType<?> pReturnType, List<FormulaType<?>> pArgs) {
    put(symbol, new Type<>(pReturnType, pArgs));
  }

  public void put(String symbol, Type<FormulaType<?>> t) {
    // TODO currently we store all variables (even SSA-indexed ones),
    // but the basic form (without indices) maybe would be enough.
    if (encodedSymbols.containsKey(symbol)) {
      assert encodedSymbols.get(symbol).equals(t) :
        String.format("Symbol '%s' of type '%s' is already declared with the type '%s'.",
            symbol, t, encodedSymbols.get(symbol));
    } else {
      encodedSymbols.put(symbol, t);
    }
  }

  public boolean containsSymbol(String symbol) {
    return encodedSymbols.containsKey(symbol);
  }

  public Type<FormulaType<?>> getType(String symbol) {
    return encodedSymbols.get(symbol);
  }

  public void dump(Path symbolEncodingFile) throws IOException {
    if (symbolEncodingFile != null) {
      Files.writeFile(symbolEncodingFile, new Appender() {
        @Override
        public void appendTo(Appendable app) throws IOException {
          for (String symbol : encodedSymbols.keySet()) {
            final Type<FormulaType<?>> type = encodedSymbols.get(symbol);
            app.append(symbol + "\t" + type.getReturnType());
            if (!type.getParameterTypes().isEmpty()) {
              app.append("\t" + Joiner.on("\t").join(type.getParameterTypes()));
            }
            app.append("\n");
          }}});
    }
  }

  /** parse the file into a map */
  public static SymbolEncoding readSymbolEncoding(Path symbolEncodingFile) throws IOException {
    final SymbolEncoding encoding = new SymbolEncoding();
    Files.checkReadableFile(symbolEncodingFile);
    try (BufferedReader reader = symbolEncodingFile.asCharSource(StandardCharsets.UTF_8).openBufferedStream()) {
      String line;
      while ((line = reader.readLine()) != null) {
        final String[] splitted = line.split("\t");
        final List<FormulaType<?>> lst = new ArrayList<>();
        for (int i = 2; i < splitted.length; i++) {
          lst.add(FormulaType.fromString(splitted[i]));
        }
        encoding.put(splitted[0],
            new Type<>(FormulaType.fromString(splitted[1]), lst));
      }
    }
    return encoding;
  }


  /** just a nice replacement for Pair<T,List<T>> */
  public static class Type<T> {

    private final T returnType;
    private final List<T> parameterTypes;

    public Type(T pReturnType, List<T> pParameterTypes) {
      this.returnType = pReturnType;
      this.parameterTypes = pParameterTypes;
    }

    public Type(T pReturnType) {
      this.returnType = pReturnType;
      this.parameterTypes = Collections.<T>emptyList();
    }

    public T getReturnType() { return returnType; }

    public List<T> getParameterTypes() { return parameterTypes; }

    @Override
    public String toString() {
      return returnType + " " + parameterTypes;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object other) {
      if (other != null && other instanceof Type) {
        Type<T> t = (Type<T>)other;
        return returnType.equals(t.returnType)
            && parameterTypes.equals(t.parameterTypes);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return returnType.hashCode() + 17 * parameterTypes.hashCode();
    }

    public final static Type<Integer> BOOL = new Type<>(-1);
  }
}
