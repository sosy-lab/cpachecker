package org.nulist.plugin.translation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import org.sosy_lab.cpachecker.cfa.ParseResult;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.util.Pair;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * @ClassName TempParseResult
 * @Description TODO
 * @Author Yinbo Yu
 * @Date 2/27/19 6:02 PM
 * @Version 1.0
 **/
public class TempParseResult {
    public NavigableMap<String, FunctionEntryNode> functions =new TreeMap<>();

    public SortedSetMultimap<String, CFANode> cfaNodes = TreeMultimap.create();

    public List<Pair<ADeclaration, String>> globalDeclarations = new ArrayList<>();

    public List<Path> fileNames = new ArrayList<>();

    public TempParseResult(){

    }

    public TempParseResult(
            NavigableMap<String, FunctionEntryNode> pFunctions,
            SortedSetMultimap<String, CFANode> pCfaNodes,
            List<Pair<ADeclaration, String>> pGlobalDeclarations,
            List<Path> pFileNames
    ){
        functions = pFunctions;
        cfaNodes = pCfaNodes;
        globalDeclarations = pGlobalDeclarations;
        fileNames = ImmutableList.copyOf(pFileNames);
    }

    public TempParseResult combineResult(ParseResult parseResult){

        functions.putAll(parseResult.getFunctions());
        cfaNodes.putAll(parseResult.getCFANodes());
        globalDeclarations.addAll(parseResult.getGlobalDeclarations());
        fileNames.addAll(parseResult.getFileNames());
        return this;
    }
}
