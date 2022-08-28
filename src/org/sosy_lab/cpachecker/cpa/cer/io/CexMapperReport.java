package org.sosy_lab.cpachecker.cpa.cer.io;

import java.util.ArrayList;
import java.util.List;

import org.sosy_lab.cpachecker.cpa.cer.cex.Cex;

public class CexMapperReport {

    private List<Cex> mappedCex;

    public CexMapperReport() {
        mappedCex = new ArrayList<>();
    }

    public List<Cex> getMappedCex() {
        return mappedCex;
    }

    public void putCex(Cex cex) {
        mappedCex.add(cex);
    }
}
