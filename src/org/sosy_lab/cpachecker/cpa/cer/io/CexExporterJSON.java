package org.sosy_lab.cpachecker.cpa.cer.io;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;

import org.sosy_lab.common.io.IO;
import org.sosy_lab.cpachecker.cpa.cer.CERCPAStatistics;
import org.sosy_lab.cpachecker.cpa.cer.CERUtils;
import org.sosy_lab.cpachecker.cpa.cer.cex.Cex;
import org.sosy_lab.cpachecker.cpa.cer.cex.CexFunctionHeadTransition;
import org.sosy_lab.cpachecker.cpa.cer.cex.CexFunctionReturnTransition;
import org.sosy_lab.cpachecker.cpa.cer.cex.CexState;
import org.sosy_lab.cpachecker.cpa.cer.cex.CexStatementTransition;
import org.sosy_lab.cpachecker.cpa.cer.cex.CexTransition;
import org.sosy_lab.cpachecker.cpa.cer.cexInfos.CounterexampleInformation;
import org.sosy_lab.cpachecker.cpa.cer.cexInfos.PrecisionInformation;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.ThreadSafeTimerContainer.TimerWrapper;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class CexExporterJSON {

    public static void exportStore(
            Collection<Cex> cexs,
            Path pPath,
            boolean pGzip,
            CERCPAStatistics statistics)
            throws IOException {
        TimerWrapper exportTimer = statistics.getFileExportTimer().getNewTimer();
        exportTimer.start();
        try {
            JsonMapper mapper = CERUtils.getJSONMapperInstance();
            ObjectNode fileNode = mapper.createObjectNode();
            exportCexs(fileNode, cexs, statistics);
            write(pPath, pGzip, fileNode.toPrettyString());
            exportTimer.stop();
        } catch (IOException e) {
            exportTimer.stop();
            throw e;
        }
    }

    private static void write(Path pPath, boolean pGzip, String pOutString) throws IOException {
        if (pGzip) {
            IO.writeGZIPFile(pPath, StandardCharsets.UTF_8, pOutString);
        } else {
            IO.writeFile(pPath, StandardCharsets.UTF_8, pOutString);
        }
    }

    private static void exportCexs(
            ObjectNode parentJsonNode,
            Collection<Cex> cexs,
            CERCPAStatistics statistics) {
        StatCounter exportedCexCounter = statistics.getExportedCexCounter();
        ArrayNode cexsJsonNode = parentJsonNode.putArray("Counterexamples");
        for (Cex cex : cexs) {
            ObjectNode cexJsonNode = cexsJsonNode.addObject();
            ArrayNode nodesJsonNode = cexJsonNode.putArray("Nodes");
            ArrayNode transitionsJsonNode = cexJsonNode.putArray("Transitions");
            CexState currentCexNode = cex.getRootState();

            while (true) {
                ObjectNode nodeJsonNode = nodesJsonNode.addObject();
                exportCexNode(nodeJsonNode, currentCexNode);
                Optional<CexTransition> currentCexTransition =
                        currentCexNode.getLeavingTransition();
                if (!currentCexTransition.isPresent()) {
                    break;
                }
                ObjectNode transitionJsonNode = transitionsJsonNode.addObject();
                exportCexTransition(transitionJsonNode, currentCexTransition.get());
                currentCexNode = currentCexTransition.get().getEndState();
            }

            exportedCexCounter.inc();
        }
    }

    private static void exportCexNode(ObjectNode cexNodeJsonNode, CexState currentNode) {
        cexNodeJsonNode.put("ID", currentNode.getId());
        for (CounterexampleInformation cexInfo : currentNode.getCexInfos()) {
            if (cexInfo instanceof PrecisionInformation) {
                PrecisionInformation precisionInfo = (PrecisionInformation) cexInfo;
                ArrayNode valuePrecionJsonNode = cexNodeJsonNode.putArray("ValuePrecision");
                for (MemoryLocation var : precisionInfo.getValuePrecison()) {
                    valuePrecionJsonNode.add(var.getExtendedQualifiedName());
                }
            }
        }
    }

    private static void
            exportCexTransition(ObjectNode cexTransitionJsonNode, CexTransition pTransition) {
        if (pTransition instanceof CexStatementTransition) {
            CexStatementTransition transition = (CexStatementTransition) pTransition;
            cexTransitionJsonNode.put("Type", CexStatementTransition.class.getSimpleName());
            cexTransitionJsonNode.put("Statement", transition.getStatement());
        } else if (pTransition instanceof CexFunctionHeadTransition) {
            CexFunctionHeadTransition transition = (CexFunctionHeadTransition) pTransition;
            cexTransitionJsonNode.put("Type", CexFunctionHeadTransition.class.getSimpleName());
            cexTransitionJsonNode.put("Function", transition.getFunctionName());
        } else if (pTransition instanceof CexFunctionReturnTransition) {
            CexFunctionReturnTransition transition = (CexFunctionReturnTransition) pTransition;
            cexTransitionJsonNode.put("Type", CexFunctionReturnTransition.class.getSimpleName());
            cexTransitionJsonNode.put("Statement", transition.getStatement());
            cexTransitionJsonNode.put("Function", transition.getFunctionName());
        }

        cexTransitionJsonNode.put("Start", pTransition.getStartState().getId());
        cexTransitionJsonNode.put("End", pTransition.getEndState().getId());
    }
}
