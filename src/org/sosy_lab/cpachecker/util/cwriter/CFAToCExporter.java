package org.sosy_lab.cpachecker.util.cwriter;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.Language;

public class CFAToCExporter {

  /**
   * Exports the given {@link CFA} to a C program.
   *
   * @param pCfa the CFA to export
   * @return C representation of the given CFA
   * @throws InvalidConfigurationException if the given CFA is not the CFA of a C program
   */
  public String exportCfa(CFA pCfa) throws InvalidConfigurationException {
    if (pCfa.getLanguage() != Language.C) {
      throw new InvalidConfigurationException(
          "CFA can only be exported to C for C input programs, at the moment");
    }

    return ""; // TODO
  }
}
