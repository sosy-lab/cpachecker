// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.cwriter;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Optional;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.cwriter.Statement.CompoundStatement;
import org.sosy_lab.cpachecker.util.cwriter.Statement.EmptyStatement;
import org.sosy_lab.cpachecker.util.cwriter.Statement.FunctionDefinition;
import org.sosy_lab.cpachecker.util.cwriter.Statement.InlinedFunction;
import org.sosy_lab.cpachecker.util.cwriter.Statement.Label;
import org.sosy_lab.cpachecker.util.cwriter.Statement.SimpleStatement;

/**
 * {@link StatementWriter} that also exports metadata about produced code to the given file. This
 * class should be closed after use, and try-with can be used for that. If this class is not closed,
 * the produced metadata JSON may be invalid.
 */
public class StatementWriterWithMetadata extends StatementWriter
    implements StatementVisitor<IOException>, Closeable {

  final AppendableWithLineCounter countingAppendable;
  private final Writer metadataOutput;
  /** Whether the current metadata has already written an entry. True if yes, false otherwise. */
  private boolean entryWritten = false;

  StatementWriterWithMetadata(
      final Appendable pDestination, final Path pMetadataOutput, final TranslatorConfig pConfig)
      throws IOException {
    this(
        new AppendableWithLineCounter(pDestination),
        IO.openOutputFile(pMetadataOutput, Charset.defaultCharset()),
        pConfig);
  }

  private StatementWriterWithMetadata(
      final AppendableWithLineCounter pCountingAppendable,
      final Writer pMetadataOutput,
      final TranslatorConfig pConfig)
      throws IOException {
    super(pCountingAppendable, pConfig);
    countingAppendable = pCountingAppendable;
    metadataOutput = pMetadataOutput;
    metadataOutput.write(getJsonTemplate());
  }

  @Override
  public void write(String pString) throws IOException {
    int before = countingAppendable.getLineCount();
    super.write(pString);
    int after = countingAppendable.getLineCount();
    assert after > before;
  }

  private String getJsonTemplate() {
    return "[";
  }

  private String getJsonForMapping(
      FileLocation origin, int newStartingLineNumber, int newEndingLineNumber) {
    int originalStartingLineNumber = origin.getStartingLineNumber();
    int originalEndingLineNumber = origin.getEndingLineNumber();
    String prefix = "";
    if (entryWritten) {
      prefix = ",";
    } else {
      assert !entryWritten;
      entryWritten = true;
    }
    prefix += "\n";
    return prefix
        + String.format(
            "{%n"
                + "  \"startingLineNumber\": %s,%n"
                + "  \"endingLineNumber\": %s,%n"
                + "  \"metadata\": {%n"
                + "    \"originalStartingLineNumber\": %s,%n"
                + "    \"originalEndingLineNumber\": %s%n"
                + "  }%n"
                + "}",
            newStartingLineNumber,
            newEndingLineNumber,
            originalStartingLineNumber,
            originalEndingLineNumber);
  }

  @Override
  public void visit(SimpleStatement pS) throws IOException {
    int startingLineNumber = countingAppendable.getLineCount();
    super.visit(pS);
    int endlingLineNumber = countingAppendable.getLineCount() - 1;

    assert endlingLineNumber >= startingLineNumber;
    Optional<CFAEdge> edge = pS.getOrigin();
    if (edge.isPresent()) {
      String json =
          getJsonForMapping(
              edge.orElseThrow().getFileLocation(), startingLineNumber, endlingLineNumber);
      metadataOutput.write(json);
    }
  }

  @Override
  public void visit(Label pS) throws IOException {
    super.visit(pS);
  }

  @Override
  public void visit(FunctionDefinition pS) throws IOException {
    super.visit(pS);
  }

  @Override
  public void visit(EmptyStatement pS) throws IOException {
    super.visit(pS);
  }

  @Override
  public void visit(CompoundStatement pS) throws IOException {
    super.visit(pS);
  }

  @Override
  public void visit(InlinedFunction pS) throws IOException {
    super.visit(pS);
  }

  @Override
  public void close() throws IOException {
    metadataOutput.write("]\n");
    metadataOutput.close();
    super.close();
  }

  private static class AppendableWithLineCounter implements Appendable {

    private final Appendable delegate;
    private int lineCount = 1; // we start in line 1

    public AppendableWithLineCounter(final Appendable pDelegate) {
      delegate = pDelegate;
    }

    public int getLineCount() {
      return lineCount;
    }

    @CanIgnoreReturnValue
    @Override
    public Appendable append(CharSequence csq) throws IOException {
      if (csq.toString().contains("\n")) {
        for (int i = 0; i < csq.length(); i++) {
          if (csq.charAt(i) == '\n') {
            lineCount++;
          }
        }
      }
      delegate.append(csq);
      return this;
    }

    @CanIgnoreReturnValue
    @Override
    public Appendable append(CharSequence csq, int start, int end) throws IOException {
      if (csq.toString().contains("\n")) {
        for (int i = 0; i < csq.length(); i++) {
          if (csq.charAt(i) == '\n') {
            lineCount++;
          }
        }
      }
      delegate.append(csq, start, end);
      return this;
    }

    @CanIgnoreReturnValue
    @Override
    public Appendable append(char c) throws IOException {
      if (c == '\n') {
        lineCount++;
      }
      delegate.append(c);
      return this;
    }
  }
}
