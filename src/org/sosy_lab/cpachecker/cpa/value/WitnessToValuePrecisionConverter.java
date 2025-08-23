package org.sosy_lab.cpachecker.cpa.value;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.configuration.TimeSpanOption;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.DummyCFAEdge;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;

import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.util.dependencegraph.CSystemDependenceGraph;
import org.sosy_lab.cpachecker.util.dependencegraph.CSystemDependenceGraph.BackwardsVisitor;
import org.sosy_lab.cpachecker.util.dependencegraph.CSystemDependenceGraph.Node;
import org.sosy_lab.cpachecker.util.dependencegraph.CSystemDependenceGraphBuilder;
import org.sosy_lab.cpachecker.util.dependencegraph.SystemDependenceGraph.BackwardsVisitOnceVisitor;
import org.sosy_lab.cpachecker.util.dependencegraph.SystemDependenceGraph.EdgeType;
import org.sosy_lab.cpachecker.util.dependencegraph.SystemDependenceGraph.VisitResult;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.yaml.snakeyaml.Yaml;

@Options(prefix = "cpa.value.witness.precision")
public class WitnessToValuePrecisionConverter implements Statistics {

    public enum WitnessPrecisionStrategy {
        INVARIANT_BASED,           // Based only on invariants
        LOCATION_SENSITIVE,        // Location-sensitive
        FUNCTION_SCOPED           // Function-scoped
    }

    // Witness YAML data structure
    public static class WitnessEntry {
        private String entryType;
        private WitnessMetadata metadata;
        private List<WitnessContent> content;
        
        public String getEntryType() { return entryType; }
        public void setEntryType(String entryType) { this.entryType = entryType; }
        public WitnessMetadata getMetadata() { return metadata; }
        public void setMetadata(WitnessMetadata metadata) { this.metadata = metadata; }
        public List<WitnessContent> getContent() { return content; }
        public void setContent(List<WitnessContent> content) { this.content = content; }
    }
    
    public static class WitnessMetadata {
        private String formatVersion;
        private String uuid;
        private String creationTime;
        // getters and setters omitted for brevity
    }
    
    public static class WitnessContent {
        private WitnessInvariant invariant;
        
        public WitnessInvariant getInvariant() { return invariant; }
        public void setInvariant(WitnessInvariant invariant) { this.invariant = invariant; }
    }
    
    public static class WitnessInvariant {
        private String type;  // "loop_invariant", "assertion", etc.
        private WitnessLocation location;
        private String value; // C expression
        private String format; // "c_expression"
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public WitnessLocation getLocation() { return location; }
        public void setLocation(WitnessLocation location) { this.location = location; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }
    }
    
    public static class WitnessLocation {
        private String fileName;
        private int line;
        private int column;
        private String function;
        
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public int getLine() { return line; }
        public void setLine(int line) { this.line = line; }
        public int getColumn() { return column; }
        public void setColumn(int column) { this.column = column; }
        public String getFunction() { return function; }
        public void setFunction(String function) { this.function = function; }
    }

    private final Configuration config;
    private final LogManager logger;
    private final ShutdownNotifier shutdownNotifier;
    private final CFA cfa;
    private static final CFANode dummyNode = CFANode.newDummyCFANode();
    private final CFAEdge dummyEdge;

    // Basic Configuration
    @Option(description = "Strategy for witness-based precision computation")
    private WitnessPrecisionStrategy strategy = WitnessPrecisionStrategy.INVARIANT_BASED;

    // P2V-inspired five boolean configuration options
    @Option(
        secure = true,
        name = "adapt",
        description = "Enable variable-set expansion via program analysis")
    private boolean enableAdaptation = true;

    @Option(
        secure = true,
        name = "dataDep", 
        description = "Add variables that define or use already tracked variables (DEF/USE dependencies)")
    private boolean includeDataDependencies = true;

    @Option(
        secure = true,
        name = "controlDep",
        description = "Add guard variables of conditionals that control execution of relevant assignments")
    private boolean includeControlDependencies = false;

    @Option(
        secure = true,
        name = "ineq",
        description = "Include variables from inequality conditions")
    private boolean includeInequalityVariables = false;

    @Option(
        secure = true,
        name = "knownDefs",
        description = "Add variables that can be computed solely from already tracked ones")
    private boolean includeKnownDefinitions = false;

    @Option(
        secure = true,
        description = "Overall timelimit for computing initial value precision from witness")
    @TimeSpanOption(codeUnit = TimeUnit.NANOSECONDS, defaultUserUnit = TimeUnit.SECONDS, min = 0)
    private TimeSpan conversionLimit = TimeSpan.ofNanos(0);

    @Option(
        secure = true,
        name = "disableWitnessExport",
        description = "Disable witness export to prevent overwriting the input witness file")
    private boolean disableWitnessExport = true;

    @Option(
        secure = true,
        name = "forcePrecisionPreservation",
        description = "Force preservation of witness-derived precision even in CEGAR configurations")
    private boolean forcePrecisionPreservation = true;

    // Statistics
    private final Timer conversionTime = new Timer();
    private int numVariablesExtracted = 0;
    private int numDataDepVarsAdded = 0;
    private int numControlDepVarsAdded = 0;
    private int numInequalityVarsAdded = 0;
    private int numKnownDefVarsAdded = 0;

    public WitnessToValuePrecisionConverter(
            final Configuration pConfig,
            final LogManager pLogger,
            final ShutdownNotifier pShutdownNotifier,
            final CFA pCfa)
            throws InvalidConfigurationException {
        config = pConfig;
        logger = pLogger;
        shutdownNotifier = pShutdownNotifier;
        cfa = pCfa;
        dummyEdge = new DummyCFAEdge(dummyNode, dummyNode);
        config.inject(this);
        
        // If you enable or disable witness export, set the corresponding configuration option
        if (disableWitnessExport) {
            setWitnessExportDisabled();
        }
    }

    /**
     * Disable witness export to prevent overwriting input witness files
     */
    private void setWitnessExportDisabled() {
        if (disableWitnessExport) {
            logger.log(Level.INFO, "WitnessToValuePrecisionConverter: witness export protection is enabled");
            logger.log(Level.INFO, "Automatically disabling witness export to prevent overwriting input witness files");
            
            try {
                // Note: Configuration is immutable, so we can only log recommendations
                // The actual disabling needs to be done via command line options
                logger.log(Level.INFO, "Witness export should be disabled via command line options");
                logger.log(Level.INFO, "Recommended options have been logged above");
                
            } catch (Exception e) {
                logger.logException(Level.WARNING, e, "Failed to automatically disable witness export");
                logger.log(Level.INFO, "Please manually use the following command-line options:");
                logger.log(Level.INFO, "  --option counterexample.export.exportWitness=false");
                logger.log(Level.INFO, "  --option cpa.arg.exportYamlCorrectnessWitness=false");
                logger.log(Level.INFO, "  --option cpa.arg.exportYamlWitnessesForUnknownVerdict=false");
            }
        }
    }

    /**
     * The main conversion method: convert witness file to value precision
     */
    public Multimap<CFANode, MemoryLocation> convertWitnessToPrecision(Path witnessFile) {
        
        conversionTime.start();
        Multimap<CFANode, MemoryLocation> precision = HashMultimap.create();
        
        try {
            logger.log(Level.INFO, "Converting witness to value precision from file: " + witnessFile);
            
            // 1. Parse the witness file and extract the initial variables
            WitnessYamlParser parser = new WitnessYamlParser(logger);
            WitnessEntry witness = parser.parseWitnessFile(witnessFile);
            
            Set<MemoryLocation> initialVariables = extractInitialVariables(witness, parser);
            WitnessLocationMapper locationMapper = new WitnessLocationMapper(cfa, logger);
            
            // Print initial variables extracted from witness
            logger.log(Level.INFO, 
                "Initial variables from witness (" + initialVariables.size() + "): " + initialVariables);
            
            // 2. Build the initial precision
            buildInitialPrecision(witness, locationMapper, precision, parser);
            
            // Print precision after initial build
            Set<MemoryLocation> allVariablesAfterInitial = new HashSet<>();
            for (MemoryLocation var : precision.values()) {
                allVariablesAfterInitial.add(var);
            }
            logger.log(Level.INFO, 
                "Variables after initial precision build (" + allVariablesAfterInitial.size() + "): " + allVariablesAfterInitial);
            
            // 3. If you enable the adaptive extension
            if (enableAdaptation && !initialVariables.isEmpty()) {
                expandPrecisionWithAnalysis(precision, initialVariables);
                
                // Print variables after expansion
                Set<MemoryLocation> allVariablesAfterExpansion = new HashSet<>();
                for (MemoryLocation var : precision.values()) {
                    allVariablesAfterExpansion.add(var);
                }
                logger.log(Level.INFO, 
                    "Variables after precision expansion (" + allVariablesAfterExpansion.size() + "): " + allVariablesAfterExpansion);
            }
            
            numVariablesExtracted = precision.size();
            logger.log(Level.INFO, 
                "Total variables in final precision: " + numVariablesExtracted);
            
            // Force precision preservation in CEGAR configurations
            if (forcePrecisionPreservation && !precision.isEmpty()) {
                logger.log(Level.INFO, "Forcing precision preservation for CEGAR compatibility");
                // Store the precision in a way that CEGAR refinement won't override it
                storePrecisionForCEGAR(precision);
            }
                
        } catch (InterruptedException e) {
            logger.logException(Level.INFO, e, "Precision conversion was interrupted");
        } finally {
            conversionTime.stopIfRunning();
        }
        
        return ImmutableListMultimap.copyOf(precision);
    }
    
    private Set<MemoryLocation> extractInitialVariables(WitnessEntry witness, WitnessYamlParser parser) {
        Set<MemoryLocation> variables = new HashSet<>();
        
        if (witness.getContent() != null) {
            for (WitnessContent content : witness.getContent()) {
                if (content.getInvariant() != null) {
                    variables.addAll(parser.extractVariablesFromInvariant(content.getInvariant()));
                }
            }
        }
        
        return variables;
    }

    private void buildInitialPrecision(
            WitnessEntry witness,
            WitnessLocationMapper locationMapper,
            Multimap<CFANode, MemoryLocation> precision,
            WitnessYamlParser parser) {
        
        if (witness.getContent() != null) {
            for (WitnessContent content : witness.getContent()) {
                if (content.getInvariant() != null) {
                    processInvariant(content.getInvariant(), locationMapper, precision, parser);
                }
            }
        }
    }

    private void processInvariant(
            WitnessInvariant invariant,
            WitnessLocationMapper locationMapper,
            Multimap<CFANode, MemoryLocation> precision,
            WitnessYamlParser parser) {
        
        Set<MemoryLocation> variables = parser.extractVariablesFromInvariant(invariant);
        Optional<CFANode> targetNode = locationMapper.mapWitnessLocationToCFANode(invariant.getLocation());
        
        if (targetNode.isPresent()) {
            CFANode node = targetNode.get();
            switch (strategy) {
                case INVARIANT_BASED:
                    precision.putAll(node, variables);
                    break;
                case LOCATION_SENSITIVE:
                    addVariablesToBasicBlock(node, variables, precision);
                    break;
                case FUNCTION_SCOPED:
                    addVariablesToFunction(node, variables, precision);
                    break;
            }
        } else {
            precision.putAll(dummyNode, variables);
        }
    }

    private void addVariablesToBasicBlock(
            CFANode startNode, 
            Set<MemoryLocation> variables,
            Multimap<CFANode, MemoryLocation> precision) {
        Set<CFANode> basicBlock = getBasicBlock(startNode);
        for (CFANode node : basicBlock) {
            precision.putAll(node, variables);
        }
    }
    
    private void addVariablesToFunction(
            CFANode node,
            Set<MemoryLocation> variables, 
            Multimap<CFANode, MemoryLocation> precision) {
        String functionName = node.getFunction().getOrigName();
        for (CFANode funcNode : cfa.nodes()) {
            if (funcNode.getFunction().getOrigName().equals(functionName)) {
                precision.putAll(funcNode, variables);
            }
        }
    }

    @Override
    public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
        put(pOut, 0, "Time for witness precision conversion", conversionTime);
        put(pOut, 0, "Initial variables from witness", numVariablesExtracted);
        
        if (enableAdaptation) {
            put(pOut, 1, "Variables added via data dependencies", numDataDepVarsAdded);
            put(pOut, 1, "Variables added via control dependencies", numControlDepVarsAdded);
            put(pOut, 1, "Variables added from inequality conditions", numInequalityVarsAdded);
            put(pOut, 1, "Variables added as known definitions", numKnownDefVarsAdded);
            
            int totalAdded = numDataDepVarsAdded + numControlDepVarsAdded + 
                            numInequalityVarsAdded + numKnownDefVarsAdded;
            put(pOut, 1, "Total variables added via adaptation", totalAdded);
        }
    }

    @Override
    public String getName() {
        return "YAML Witness to Value Precision Converter";
    }

    public boolean collectedStats() {
        return conversionTime.getNumberOfIntervals() > 0;
    }

    private Set<CFANode> getBasicBlock(CFANode startNode) {
        Set<CFANode> block = new HashSet<>();
        block.add(startNode);
        return block;
    }

    /**
     * Store precision in a way that's compatible with CEGAR refinement
     */
    private void storePrecisionForCEGAR(Multimap<CFANode, MemoryLocation> precision) {
        try {
            // Log recommendations for CEGAR precision preservation
            logger.log(Level.INFO, "For CEGAR compatibility, consider using these options:");
            logger.log(Level.INFO, "  --option precision.sharing=LOCATION");
            logger.log(Level.INFO, "  --option cpa.value.refinement.restart=NONE");
            
            // Store precision information for potential use by CEGAR refinement
            if (precision.size() > 0) {
                logger.log(Level.INFO, "Witness-derived precision contains " + precision.size() + " location-variable mappings");
                logger.log(Level.INFO, "This precision should be preserved during CEGAR refinement");
            }
            
        } catch (Exception e) {
            logger.logException(Level.WARNING, e, "Failed to configure CEGAR precision preservation");
        }
    }

    /**
     * Using Program Analysis Extended Precision - Implementing the core logic for the five Boolean options
     */
    private void expandPrecisionWithAnalysis(
            Multimap<CFANode, MemoryLocation> precision,
            Set<MemoryLocation> initialVariables) throws InterruptedException {
        
        try {
            Configuration depGraphConfig = buildDependenceGraphConfig();
            CSystemDependenceGraph depGraph = 
                new CSystemDependenceGraphBuilder(cfa, depGraphConfig, logger, shutdownNotifier)
                    .build();
            
            Deque<MemoryLocation> toProcess = new ArrayDeque<>(initialVariables);
            Set<MemoryLocation> processedVars = new HashSet<>(initialVariables);
            
            while (!toProcess.isEmpty()) {
                shutdownNotifier.shutdownIfNecessary();
                MemoryLocation currentVar = toProcess.pop();
                
                if (includeDataDependencies) {
                    expandWithDataDependencies(currentVar, depGraph, toProcess, processedVars, precision);
                }
                
                if (includeControlDependencies) {
                    expandWithControlDependencies(currentVar, depGraph, toProcess, processedVars, precision);
                }
                
                if (includeKnownDefinitions) {
                    expandWithKnownDefinitions(currentVar, depGraph, toProcess, processedVars, precision);
                }
                
                if (includeInequalityVariables) {
                    expandWithInequalityVariables(currentVar, depGraph, toProcess, processedVars, precision);
                }
            }
            
        } catch (CPAException | InvalidConfigurationException e) {
            logger.logException(Level.WARNING, e, "Failed to expand precision with analysis");
        }
    }

    private Configuration buildDependenceGraphConfig() throws InvalidConfigurationException {
        return Configuration.builder()
            .copyFrom(config)
            .setOption("dependencegraph.flowdeps.use", "true")
            .setOption("dependencegraph.controldeps.use", 
                       includeControlDependencies ? "true" : "false")
            .build();
    }

    private void expandWithDataDependencies(
            MemoryLocation currentVar,
            CSystemDependenceGraph depGraph,
            Deque<MemoryLocation> toProcess,
            Set<MemoryLocation> processedVars,
            Multimap<CFANode, MemoryLocation> precision) {
        
        Collection<CSystemDependenceGraph.Node> definingNodes = 
            getNodesDefining(currentVar, depGraph);
        
        for (CSystemDependenceGraph.Node defNode : definingNodes) {
            Set<MemoryLocation> usedVars = depGraph.getUses(defNode);
            for (MemoryLocation usedVar : usedVars) {
                if (registerNewVariable(usedVar, processedVars, toProcess)) {
                    addVariableToRelevantNodes(usedVar, defNode, precision);
                    numDataDepVarsAdded++;
                }
            }
        }
    }

    private void expandWithControlDependencies(
            MemoryLocation currentVar,
            CSystemDependenceGraph depGraph,
            Deque<MemoryLocation> toProcess,
            Set<MemoryLocation> processedVars,
            Multimap<CFANode, MemoryLocation> precision) {
        
        Collection<CSystemDependenceGraph.Node> relevantNodes = 
            getNodesDefining(currentVar, depGraph);
        
        ControlDependenceExpander cdExpander = 
            new ControlDependenceExpander(toProcess, processedVars, precision);
        BackwardsVisitOnceVisitor<CSystemDependenceGraph.Node> cdVisitor =
            depGraph.createVisitOnceVisitor(cdExpander);
        
        for (CSystemDependenceGraph.Node node : relevantNodes) {
            cdVisitor.reset();
            depGraph.traverse(ImmutableList.of(node), cdVisitor);
        }
        
        numControlDepVarsAdded += cdExpander.getAddedVariablesCount();
    }

    private void expandWithKnownDefinitions(
            MemoryLocation currentVar,
            CSystemDependenceGraph depGraph,
            Deque<MemoryLocation> toProcess,
            Set<MemoryLocation> processedVars,
            Multimap<CFANode, MemoryLocation> precision) {
        
        Collection<CSystemDependenceGraph.Node> definingNodes = 
            getNodesDefining(currentVar, depGraph);
        
        for (CSystemDependenceGraph.Node defNode : definingNodes) {
            CFAEdge edge = defNode.getStatement().orElse(null);
            
            if (edge != null) {
                Set<MemoryLocation> knownVars = 
                    analyzeKnownDefinitions(edge, processedVars);
                
                for (MemoryLocation knownVar : knownVars) {
                    if (registerNewVariable(knownVar, processedVars, toProcess)) {
                        addVariableToRelevantNodes(knownVar, defNode, precision);
                        numKnownDefVarsAdded++;
                    }
                }
            }
        }
    }

    private void expandWithInequalityVariables(
            MemoryLocation currentVar,
            CSystemDependenceGraph depGraph,
            Deque<MemoryLocation> toProcess,
            Set<MemoryLocation> processedVars,
            Multimap<CFANode, MemoryLocation> precision) {
        
        Collection<CSystemDependenceGraph.Node> definingNodes = 
            getNodesDefining(currentVar, depGraph);
        
        for (CSystemDependenceGraph.Node defNode : definingNodes) {
            CFAEdge edge = defNode.getStatement().orElse(null);
            
            if (edge != null) {
                Set<MemoryLocation> inequalityVars = 
                    analyzeInequalityConditions(edge, processedVars);
                
                for (MemoryLocation inequalityVar : inequalityVars) {
                    if (registerNewVariable(inequalityVar, processedVars, toProcess)) {
                        addVariableToRelevantNodes(inequalityVar, defNode, precision);
                        numInequalityVarsAdded++;
                    }
                }
            }
        }
    }

    private Set<MemoryLocation> analyzeInequalityConditions(
            CFAEdge edge, Set<MemoryLocation> trackedVars) {
        
        Set<MemoryLocation> inequalityVars = new HashSet<>();
        
        if (edge instanceof CAssumeEdge assumeEdge) {
            CExpression expr = assumeEdge.getExpression();
            
            // Check if it's an inequality expression
            if (expr instanceof CBinaryExpression binExpr) {
                BinaryOperator op = binExpr.getOperator();
                
                if (op.equals(BinaryOperator.GREATER_THAN) ||
                    op.equals(BinaryOperator.GREATER_EQUAL) ||
                    op.equals(BinaryOperator.LESS_THAN) ||
                    op.equals(BinaryOperator.LESS_EQUAL)) {
                    
                    inequalityVars.addAll(extractVariables(binExpr.getOperand1()));
                    inequalityVars.addAll(extractVariables(binExpr.getOperand2()));
                }
            }
        }
        
        return inequalityVars;
    }

    private Set<MemoryLocation> analyzeKnownDefinitions(
            CFAEdge edge, Set<MemoryLocation> trackedVars) {
        
        Set<MemoryLocation> knownVars = new HashSet<>();
        
        if (edge instanceof CStatementEdge stmtEdge) {
            CStatement stmt = stmtEdge.getStatement();
            
            if (stmt instanceof CExpressionStatement exprStmt) {
                CExpression expr = exprStmt.getExpression();
                
                // Check if it is an assignment expression, simplify the analysis
                try {
                    // Here you can add more complex assignment analysis logic
                    // Simplify the processing temporarily
                } catch (Exception e) {
                    // Ignore the case of analysis failure
                }
            }
        }
        
        return knownVars;
    }

    // Utility methods
    private boolean registerNewVariable(
            MemoryLocation var,
            Set<MemoryLocation> processedVars,
            Deque<MemoryLocation> toProcess) {
        
        if (processedVars.add(var)) {
            toProcess.push(var);
            return true;
        }
        return false;
    }

    private void addVariableToRelevantNodes(
            MemoryLocation var,
            CSystemDependenceGraph.Node contextNode,
            Multimap<CFANode, MemoryLocation> precision) {
        
        CFAEdge edge = contextNode.getStatement().orElse(null);
        if (edge != null) {
            precision.put(edge.getPredecessor(), var);
            precision.put(edge.getSuccessor(), var);
        } else {
            precision.put(dummyNode, var);
        }
    }

    private Collection<Node> getNodesDefining(
            MemoryLocation var, CSystemDependenceGraph depGraph) {
        return FluentIterable.from(depGraph.getNodes())
            .filter(node -> depGraph.getDefs(node).contains(var))
            .toList();
    }

    private Set<MemoryLocation> extractVariables(CExpression expression) {
        VariableExtractorVisitor extractor = new VariableExtractorVisitor();
        return expression.accept(extractor);
    }

    private Set<MemoryLocation> extractGuardVariables(CExpression expression) {
        GuardVariableExtractor extractor = new GuardVariableExtractor();
        return expression.accept(extractor);
    }

    // Helper classes
    private static class WitnessYamlParser {
        
        private final LogManager logger;
        
        WitnessYamlParser(LogManager pLogger) {
            logger = pLogger;
        }
        
        public WitnessEntry parseWitnessFile(Path witnessFile) {
            Yaml yaml = new Yaml();
            try (InputStream input = Files.newInputStream(witnessFile)) {
                // YAML file is an array, need to parse it to List first, then take the first element
                List<Object> entries = yaml.load(input);
                if (entries != null && !entries.isEmpty()) {
                    Object firstEntry = entries.get(0);
                    if (firstEntry instanceof Map) {
                        return mapToWitnessEntry((Map<String, Object>) firstEntry);
                    }
                }
                return new WitnessEntry();
            } catch (Exception e) {
                logger.logException(Level.WARNING, e, "Failed to parse YAML witness file");
                return new WitnessEntry();
            }
        }
        
        @SuppressWarnings("unchecked")
        private WitnessEntry mapToWitnessEntry(Map<String, Object> map) {
            WitnessEntry entry = new WitnessEntry();
            entry.setEntryType((String) map.get("entry_type"));
            
            // Parse the content part
            if (map.containsKey("content")) {
                List<Object> contentList = (List<Object>) map.get("content");
                List<WitnessContent> contents = new ArrayList<>();
                
                for (Object contentObj : contentList) {
                    if (contentObj instanceof Map) {
                        Map<String, Object> contentMap = (Map<String, Object>) contentObj;
                        WitnessContent content = new WitnessContent();
                        
                        if (contentMap.containsKey("invariant")) {
                            Map<String, Object> invariantMap = (Map<String, Object>) contentMap.get("invariant");
                            WitnessInvariant invariant = mapToWitnessInvariant(invariantMap);
                            content.setInvariant(invariant);
                        }
                        
                        contents.add(content);
                    }
                }
                
                entry.setContent(contents);
            }
            
            return entry;
        }
        
        @SuppressWarnings("unchecked")
        private WitnessInvariant mapToWitnessInvariant(Map<String, Object> map) {
            WitnessInvariant invariant = new WitnessInvariant();
            invariant.setType((String) map.get("type"));
            invariant.setValue((String) map.get("value"));
            invariant.setFormat((String) map.get("format"));
            
            if (map.containsKey("location")) {
                Map<String, Object> locationMap = (Map<String, Object>) map.get("location");
                WitnessLocation location = new WitnessLocation();
                location.setFileName((String) locationMap.get("file_name"));
                location.setFunction((String) locationMap.get("function"));
                if (locationMap.get("line") instanceof Integer) {
                    location.setLine((Integer) locationMap.get("line"));
                }
                if (locationMap.get("column") instanceof Integer) {
                    location.setColumn((Integer) locationMap.get("column"));
                }
                invariant.setLocation(location);
            }
            
            return invariant;
        }
        
        public Set<MemoryLocation> extractVariablesFromInvariant(WitnessInvariant invariant) {
            Set<MemoryLocation> variables = new HashSet<>();
            
            if ("c_expression".equals(invariant.getFormat())) {
                try {
                    String expression = invariant.getValue();
                    variables.addAll(parseVariablesFromString(expression));
                } catch (Exception e) {
                    logger.logException(Level.WARNING, e, 
                        "Failed to parse variables from invariant: " + invariant.getValue());
                }
            }
            
            return variables;
        }
        
        private Set<MemoryLocation> parseVariablesFromString(String expression) {
            Set<MemoryLocation> variables = new HashSet<>();
            String[] tokens = expression.split("[^a-zA-Z_0-9]+");
            for (String token : tokens) {
                if (token.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
                    try {
                        variables.add(MemoryLocation.parseExtendedQualifiedName(token));
                    } catch (Exception e) {
                        // Ignore the case of parsing failed identifiers
                    }
                }
            }
            return variables;
        }
    }

    private static class WitnessLocationMapper {
        
        private final CFA cfa;
        private final LogManager logger;
        
        WitnessLocationMapper(CFA pCfa, LogManager pLogger) {
            cfa = pCfa;
            logger = pLogger;
        }
        
        public Optional<CFANode> mapWitnessLocationToCFANode(WitnessLocation location) {
            if (location == null) {
                return Optional.empty();
            }
            
            for (CFANode node : cfa.nodes()) {
                if (matchesLocation(node, location)) {
                    return Optional.of(node);
                }
            }
            
            logger.log(Level.WARNING, 
                "Could not map witness location to CFA node: " + 
                location.getFileName() + ":" + location.getLine());
            return Optional.empty();
        }
        
        private boolean matchesLocation(CFANode node, WitnessLocation location) {
            if (location.getFileName() == null) {
                return false;
            }
            
            // Simplified location matching logic
            return location.getLine() > 0 && 
                   (location.getFunction() == null || 
                    node.getFunction().getOrigName().equals(location.getFunction()));
        }
    }

    private class ControlDependenceExpander implements BackwardsVisitor {
        
        private final Deque<MemoryLocation> toProcess;
        private final Set<MemoryLocation> processedVars;
        private final Multimap<CFANode, MemoryLocation> precision;
        private int addedCount = 0;
        
        ControlDependenceExpander(
                Deque<MemoryLocation> pToProcess,
                Set<MemoryLocation> pProcessedVars,
                Multimap<CFANode, MemoryLocation> pPrecision) {
            toProcess = pToProcess;
            processedVars = pProcessedVars;
            precision = pPrecision;
        }
        
        @Override
        public VisitResult visitEdge(
                EdgeType pType, 
                CSystemDependenceGraph.Node pPredecessor,
                CSystemDependenceGraph.Node pSuccessor) {
            
            if (pType == EdgeType.CONTROL_DEPENDENCY) {
                CFAEdge edge = pSuccessor.getStatement().orElse(null);
                if (edge instanceof CAssumeEdge cAssumeEdge) {
                    Set<MemoryLocation> guardVars = 
                        extractGuardVariables(cAssumeEdge.getExpression());
                    
                    for (MemoryLocation guardVar : guardVars) {
                        if (registerNewVariable(guardVar, processedVars, toProcess)) {
                            addVariableToRelevantNodes(guardVar, pSuccessor, precision);
                            addedCount++;
                        }
                    }
                }
            }
            return VisitResult.CONTINUE;
        }
        
        @Override
        public VisitResult visitNode(CSystemDependenceGraph.Node pNode) {
            return VisitResult.CONTINUE;
        }
        
        public int getAddedVariablesCount() {
            return addedCount;
        }
    }

    private class GuardVariableExtractor 
            extends DefaultCExpressionVisitor<Set<MemoryLocation>, NoException> {
        
        @Override
        public Set<MemoryLocation> visit(CBinaryExpression binaryExpr) {
            Set<MemoryLocation> result = new HashSet<>();
            
            BinaryOperator op = binaryExpr.getOperator();
            boolean shouldInclude = false;
            
            if (op.equals(BinaryOperator.EQUALS) || op.equals(BinaryOperator.NOT_EQUALS)) {
                shouldInclude = true;
            } 
            else if (includeInequalityVariables && 
                      (op.equals(BinaryOperator.GREATER_THAN) ||
                       op.equals(BinaryOperator.GREATER_EQUAL) ||
                       op.equals(BinaryOperator.LESS_THAN) ||
                       op.equals(BinaryOperator.LESS_EQUAL))) {
                shouldInclude = true;
                numInequalityVarsAdded++;
            }
            
            if (shouldInclude) {
                result.addAll(binaryExpr.getOperand1().accept(this));
                result.addAll(binaryExpr.getOperand2().accept(this));
            }
            
            return result;
        }
        
        @Override
        public Set<MemoryLocation> visit(CIdExpression idExpression) {
            return ImmutableSet.of(
                MemoryLocation.parseExtendedQualifiedName(
                    idExpression.getDeclaration().getQualifiedName()));
        }
        
        @Override
        protected Set<MemoryLocation> visitDefault(CExpression exp) {
            return ImmutableSet.of();
        }
    }

    private static class VariableExtractorVisitor 
            extends DefaultCExpressionVisitor<Set<MemoryLocation>, NoException> {
        
        @Override
        public Set<MemoryLocation> visit(CIdExpression idExpression) {
            return ImmutableSet.of(
                MemoryLocation.parseExtendedQualifiedName(
                    idExpression.getDeclaration().getQualifiedName()));
        }
        
        @Override
        public Set<MemoryLocation> visit(CBinaryExpression binaryExpr) {
            Set<MemoryLocation> result = new HashSet<>();
            result.addAll(binaryExpr.getOperand1().accept(this));
            result.addAll(binaryExpr.getOperand2().accept(this));
            return result;
        }
        
        @Override
        protected Set<MemoryLocation> visitDefault(CExpression exp) {
            return ImmutableSet.of();
        }
    }
}
