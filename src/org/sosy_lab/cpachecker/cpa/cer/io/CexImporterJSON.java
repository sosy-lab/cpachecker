package org.sosy_lab.cpachecker.cpa.cer.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.sosy_lab.cpachecker.cpa.cer.CERCPAStatistics;
import org.sosy_lab.cpachecker.cpa.cer.CERUtils;
import org.sosy_lab.cpachecker.cpa.cer.cex.Cex;
import org.sosy_lab.cpachecker.cpa.cer.cex.CexFunctionHeadTransition;
import org.sosy_lab.cpachecker.cpa.cer.cex.CexFunctionReturnTransition;
import org.sosy_lab.cpachecker.cpa.cer.cex.CexNode;
import org.sosy_lab.cpachecker.cpa.cer.cex.CexStatementTransition;
import org.sosy_lab.cpachecker.cpa.cer.cex.CexTransition;
import org.sosy_lab.cpachecker.cpa.cer.cexInfos.CounterexampleInformation;
import org.sosy_lab.cpachecker.cpa.cer.cexInfos.PrecisionInformation;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.ThreadSafeTimerContainer.TimerWrapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class CexImporterJSON {

    public static Collection<Cex> importCexs(Path pPath, boolean pGzip, CERCPAStatistics statistics)
            throws IOException, NoSuchFileException {
        TimerWrapper importTimer = statistics.getFileImportTimer().getNewTimer();
        StatCounter importedCexCounter = statistics.getImportedCexCounter();

        importTimer.start();
        try (Reader reader = getFileReader(pPath, pGzip)) {
            JsonMapper mapper = CERUtils.getJSONMapperInstance();
            ObjectNode fileNode = (ObjectNode) mapper.readTree(reader);
            ArrayNode cexsNode = (ArrayNode) fileNode.get("Counterexamples");
            List<Cex> result = new ArrayList<>(cexsNode.size());
            for (int i = 0; i < cexsNode.size(); ++i) {
                Cex cex = importCex((ObjectNode) cexsNode.get(i));
                if (cex != null) {
                    result.add(cex);
                }
                importedCexCounter.inc();
            }
            importTimer.stop();
            return result;
        } catch (NoSuchFileException e) {
            importTimer.stop();
            throw e;
        } catch (Exception e) {
            importTimer.stop();
            throw new IOException(e);
        }
    }

    private static Cex importCex(ObjectNode cexJsonNode) throws InvalidInputException {
        ArrayNode nodesJsonNode = (ArrayNode) cexJsonNode.get("Nodes");
        ArrayNode transitionsJsonNode = (ArrayNode) cexJsonNode.get("Transitions");

        // call importCexNode before importTransitions to fill the cexNodeIdentifier
        Map<Integer, CexNode> cexNodeIdentifier = new HashMap<>();
        for (int i = 0; i < nodesJsonNode.size(); ++i) {
            importCexNode((ObjectNode) nodesJsonNode.get(i), cexNodeIdentifier);
        }
        List<CexTransition> transitions = new ArrayList<>(transitionsJsonNode.size());
        for (int i = 0; i < transitionsJsonNode.size(); ++i) {
            transitions.add(
                    importCexTransition(
                            (ObjectNode) transitionsJsonNode.get(i),
                            cexNodeIdentifier));
        }

        if (transitions.size() <= 0) {
            return null;
        }

        CexNode currentNode = transitions.get(0).getStartNode();
        Cex cex = new Cex(currentNode);
        for (CexTransition transition : transitions) {
            currentNode.setLeavingTransition(transition);
            currentNode = transition.getEndNode();
        }
        return cex;
    }

    private static CexNode
            importCexNode(ObjectNode cexNodeJsonNode, Map<Integer, CexNode> cexNodeIdentifier) {
        int id = cexNodeJsonNode.get("ID").asInt();
        CexNode resultNode = new CexNode();
        cexNodeIdentifier.put(id, resultNode);

        Set<CounterexampleInformation> cexInfos = new HashSet<>();

        JsonNode node = cexNodeJsonNode.get("ValuePrecision");
        if (node != null) {
            ArrayNode valuePrecionJsonNode = (ArrayNode) cexNodeJsonNode.get("ValuePrecision");
            Set<MemoryLocation> valuePrecision = new HashSet<>(valuePrecionJsonNode.size());
            for (int i = 0; i < valuePrecionJsonNode.size(); ++i) {
                String varName = valuePrecionJsonNode.get(i).asText();
                valuePrecision.add(MemoryLocation.fromQualifiedName(varName));
                PrecisionInformation precInfo = new PrecisionInformation(valuePrecision);
                cexInfos.add(precInfo);
            }
        }

        if (cexInfos.size() > 0) {
            resultNode.setCexInfos(cexInfos);
        }

        return resultNode;
    }

    private static CexTransition importCexTransition(
            ObjectNode cexTransitionJsonNode,
            Map<Integer, CexNode> cexNodeIdentifier)
            throws InvalidInputException {
        String type = cexTransitionJsonNode.get("Type").asText();
        int startNodeId = cexTransitionJsonNode.get("Start").asInt();
        CexNode startNode = cexNodeIdentifier.get(startNodeId);
        int endNodeId = cexTransitionJsonNode.get("End").asInt();
        CexNode endNode = cexNodeIdentifier.get(endNodeId);
        if (startNode == null || endNode == null) {
            throw new InvalidInputException("The nodes of this transition are unknown.");
        }

        if (type.equals(CexStatementTransition.class.getSimpleName())) {
            String statement = cexTransitionJsonNode.get("Statement").asText();
            return CexStatementTransition.create(startNode, statement, endNode);
        } else if (type.equals(CexFunctionHeadTransition.class.getSimpleName())) {
            String functionName = cexTransitionJsonNode.get("Function").asText();
            return CexFunctionHeadTransition.create(startNode, functionName, endNode);
        } else if (type.equals(CexFunctionReturnTransition.class.getSimpleName())) {
            String statement = cexTransitionJsonNode.get("Statement").asText();
            String functionName = cexTransitionJsonNode.get("Function").asText();
            return CexFunctionReturnTransition.create(startNode, statement, functionName, endNode);
        }

        throw new InvalidInputException("Unknown transition specification.");
    }

    private static Reader getFileReader(Path pPath, boolean pGzip) throws IOException {
        if (pGzip) {
            try {
                InputStream inputStream = Files.newInputStream(pPath);
                InputStream gzipInputStream = new GZIPInputStream(inputStream);
                Reader reader = new InputStreamReader(gzipInputStream, StandardCharsets.UTF_8);
                return reader;
            } catch (IOException e) {
                Reader reader = Files.newBufferedReader(pPath, StandardCharsets.UTF_8);
                return reader;
            }
        }
        return Files.newBufferedReader(pPath, StandardCharsets.UTF_8);
    }
}
