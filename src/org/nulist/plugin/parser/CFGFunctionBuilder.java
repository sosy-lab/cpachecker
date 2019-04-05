/**
 * @ClassName CFGFunctionBuilder
 * @Description TODO
 * @Author Yinbo Yu
 * @Date 3/6/19 5:27 PM
 * @Version 1.0
 **/
package org.nulist.plugin.parser;

import com.google.common.base.Optional;
import com.grammatech.cs.*;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.*;
import org.sosy_lab.cpachecker.cfa.model.*;
import org.sosy_lab.cpachecker.cfa.model.c.*;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.*;
import org.sosy_lab.cpachecker.util.Pair;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.logging.Level;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.nulist.plugin.parser.CFABuilder.pointerOf;
import static org.nulist.plugin.parser.CFGAST.*;
import static org.nulist.plugin.parser.CFGNode.*;
import static org.nulist.plugin.parser.CFGNode.DECLARATION;
import static org.nulist.plugin.util.ClassTool.getUnsignedInt;
import static org.nulist.plugin.util.ClassTool.printWARNING;
import static org.nulist.plugin.util.ClassTool.printf;
import static org.nulist.plugin.util.FileOperations.getLocation;
import static org.nulist.plugin.util.FileOperations.getQualifiedName;
import static org.sosy_lab.cpachecker.cfa.CFACreationUtils.addEdgeUnconditionallyToCFA;

public class CFGFunctionBuilder  {

    // Data structure for maintaining our scope stack in a function
    private Deque<CFANode> locStack = new ArrayDeque<>();

    // Data structures for handling function declarations
    public FunctionEntryNode cfa = null;
    public Set<CFANode> cfaNodes = new HashSet<>();
    public Map<Long, CFANode> cfaNodeMap = new HashMap<>();
    public final Map<Integer, CSimpleDeclaration> variableDeclarations = new HashMap<>();

    public CFGHandleExpression expressionHandler;
    private final LogManager logger;
    public final procedure function;
    public final String functionName;
    public final CFGTypeConverter typeConverter;
    public CFunctionDeclaration functionDeclaration;
    public final String fileName;
    public CFABuilder cfaBuilder;
    private boolean directAddEdge = true;

    public CFGFunctionBuilder(
            LogManager pLogger,
            CFGTypeConverter typeConverter,
            procedure pFunction,
            String functionName,
            String pFileName,
            CFABuilder cfaBuilder
            ) {
        logger = pLogger;
        this.typeConverter = typeConverter;
        function = pFunction;
        this.functionName = functionName;

        fileName = pFileName;
        this.cfaBuilder = cfaBuilder;
        expressionHandler = new CFGHandleExpression(pLogger,functionName,typeConverter);
        expressionHandler.setGlobalVariableDeclarations(cfaBuilder.expressionHandler.globalDeclarations);
    }


    public FunctionEntryNode getCfa() {
        return cfa;
    }

    public FunctionEntryNode getFunctionEntryNode(point functionEntry) throws result{
        if(functionEntry.get_procedure().get_kind().equals(procedure_kind.getUSER_DEFINED()))
            return cfaBuilder.functions.get(functionEntry.get_procedure().name());
        else
            return cfaBuilder.systemFunctions.get(functionEntry.get_procedure().name());
    }

    /**
     *@Description function declaration, need to extract its return type and parameters
     *@Param [function, pFileName]
     *@return void
     **/
    public CFunctionDeclaration handleFunctionDeclaration() throws result{
        String functionName = function.name();

        //for struct example: struct test{int a, int b}
        //function example: test function(int c,int d)
        //routine type: test (c,d)

        ast entryAST = function.entry_point().get_ast(ast_family.getC_UNNORMALIZED());
        CFunctionType cFunctionType = (CFunctionType) typeConverter
                .getCType(entryAST.get(ast_ordinal.getBASE_TYPE()).as_ast(), expressionHandler);
        FileLocation fileLocation = getLocation(function.entry_point(),fileName);

        // Parameters
        point_set formal_ins = function.formal_ins();// each point in formal_ins is an input parameter
        List<CParameterDeclaration> parameters = new ArrayList<>((int)cFunctionType.getParameters().size());

        if(!formal_ins.empty()){
            //left to right
            for(point_set_iterator point_it= formal_ins.cbegin();
                !point_it.at_end();point_it.advance())
            {
                point paramNode = point_it.current();
                ast un_ast = paramNode.get_ast(ast_family.getC_UNNORMALIZED());

                String paramName = un_ast.pretty_print();//param_point.parameter_symbols().get(0).get_ast().pretty_print();

                CType paramType = typeConverter.getCType(un_ast.get(ast_ordinal.getBASE_TYPE()).as_ast(),expressionHandler);

                CParameterDeclaration parameter =
                        new CParameterDeclaration(fileLocation,paramType,paramName);

                parameter.setQualifiedName(paramName);
                expressionHandler.variableDeclarations.put(paramName.hashCode(),parameter);
                if(!paramName.equals("__builtin_va_alist"))
                    parameters.add(parameter);
            }

        }

        CFunctionTypeWithNames cFuncType =
                new CFunctionTypeWithNames(checkNotNull(cFunctionType.getReturnType()),parameters,cFunctionType.takesVarArgs());

        // Function declaration, exit
        functionDeclaration =
                new CFunctionDeclaration(
                        getLocation(function, fileName), cFuncType, functionName, parameters);
        return functionDeclaration;
    }

    /**
     *@Description function entry node
     *@Param [function, pFileName]
     *@return org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode
     **/
    public CFunctionEntryNode handleFunctionDefinition() throws result{

        assert cfa == null;

        String functionName = function.name();
        //logger.log(Level.FINE, "Creating function: " + functionName);

        // Return variable : The return value is written to this
        Optional<CVariableDeclaration> returnVar = Optional.absent();
        FileLocation fileLocation =getLocation(function, fileName);

        if(!functionDeclaration.getType().getReturnType().equals(CVoidType.VOID)){
            point formal_out = function.formal_outs().cbegin().current();
            ast no_ast = formal_out.get_ast(ast_family.getC_NORMALIZED());
            String variabeName = no_ast.get(ast_ordinal.getNC_UNNORMALIZED()).as_ast().pretty_print();
            CVariableDeclaration newVarDecl =
                    new CVariableDeclaration(
                            fileLocation,
                            false,
                            CStorageClass.AUTO,
                            functionDeclaration.getType().getReturnType(),
                            variabeName,
                            variabeName,
                            variabeName,
                            null);
            expressionHandler.variableDeclarations.put(variabeName.hashCode(),newVarDecl);
            returnVar =Optional.of(newVarDecl);
        }

        FunctionExitNode functionExit = new FunctionExitNode(functionName);
        CFunctionEntryNode entry = new CFunctionEntryNode(
                        fileLocation, functionDeclaration, functionExit, returnVar);
        functionExit.setEntryNode(entry);

        cfa = entry;

        cfaNodeMap.put(function.entry_point().id(),cfa);
        cfaBuilder.addNode(functionName, functionExit);
        cfaBuilder.addNode(functionName, entry);

        return entry;
    }

    /**
     *@Description traverse CFG nodes and edges and transform them into CFA
     *@Param [function, pFileName]
     *@return void
     **/
    public void visitFunction() throws result {
        assert function!=null && function.get_kind().equals(procedure_kind.getUSER_DEFINED());

        //expressionHandler.setVariableDeclarations(variableDeclarations);
        //first visit: build nodes before traversing CFGs
        List<point> declSet = new ArrayList<>();

        point_set pointSet = function.points();
        for(point_set_iterator point_it=pointSet.cbegin();!point_it.at_end();point_it.advance()){
            point node = point_it.current();
            if(getKindName(node).equals(CALL_SITE)
                || getKindName(node).equals(CONTROL_POINT)
                || getKindName(node).equals(JUMP)
                || getKindName(node).equals(EXPRESSION)
                || getKindName(node).equals(SWITCH_CASE)
                || getKindName(node).equals(RETURN)
                || getKindName(node).equals(NORMAL_RETURN)
                || getKindName(node).equals(EXCEPTIONAL_RETURN)
                || getKindName(node).equals(INDIRECT_CALL)){
                CFANode newCFAnode = newCFANode();
                cfaNodeMap.put(node.id(),newCFAnode);
            }else if(isGoToLabel(node)){
                String labelName = getLabelName(node);
                CLabelNode labelNode = new CLabelNode(functionName,labelName);
                cfaNodes.add(labelNode);
                cfaNodeMap.put(node.id(),labelNode);
            }else if(getKindName(node).endsWith(LABEL)){
                CFANode newCFAnode = newCFANode();
                cfaNodeMap.put(node.id(),newCFAnode);
            }else if(getKindName(node).equals(DECLARATION)){//static variable has no expression, but actually has initializer
                symbol s = node.declared_symbol();
                ast symbolAST = s.get_ast(ast_family.getC_UNNORMALIZED());
                ast normalizedAST = s.get_ast();
                if(isVLAType(symbolAST.get(ast_ordinal.getBASE_TYPE()).as_ast()) &&
                        normalizedAST.get(ast_ordinal.getBASE_TYPE()).as_ast().pretty_print().contains("$temp")){
//                    symbol temp = normalizedAST.get(ast_ordinal.getBASE_TYPE())
//                            .get(ast_ordinal.getNC_NUM_ELEMENTS_ALOC()).as_symbol();
//                    String tempVarName = temp.get_ast().get(ast_ordinal.getBASE_NAME()).as_str();
//                    noInitiVlaDeclarationPoint.put(tempVarName,node);
                } else if(s.is_local() &&
                        s.get_kind().equals(symbol_kind.getUSER()) &&
                        !s.is_formal()){
                    if(!symbolHasInitialization(symbolAST))
                        declSet.add(node);
                }else if(s.is_local_static() && staticSymbolHasInitialization(symbolAST))
                    declSet.add(node);
                else if(s.get_kind().equals(symbol_kind.getRETURN()))
                    declSet.add(node);
                else if(s.is_global()||s.is_file_static()){//extern variable with no initialization is not built in cfabuilder
                    CType type = typeConverter.getCType(symbolAST.get(ast_ordinal.getBASE_TYPE()).as_ast(), expressionHandler);
                    checkOrInsertNewGlobalVarDeclarations(s, type, null, getLocation(node));
                }
            }
        }

        //second visit: build edges
        point entryNextNode = function.entry_point().cfg_targets().cbegin().current().get_first();

        //handle all variable declarations and get the last declared symbol;
        handleVariableDeclaration(declSet, entryNextNode);

        if(functionName.equals("rrc_eNB_generate_HO_RRCConnectionReconfiguration")){
            directAddEdge = false;
            buildCFAfromBasicBlock();
        } else
            //build edges between cfg nodes
            traverseCFGNode(entryNextNode, null);

        finish();
    }

    /**
     * @Description //TODO
     * @Param []
     * @return void
     **/
    private void buildCFAfromBasicBlock()throws result{

        // basic block is a sequence of consecutive CFG nodes
        // in which intra-procedural flow of control
        // can only enter at the first CFG point and
        // leave at the last CFG point

        List<basic_block> blockList = new ArrayList<>();
        for(basic_block_set_iterator block_it =function.basic_blocks().cbegin();!block_it.at_end();block_it.advance()){
            basic_block block = block_it.current();

            if(isActual_In(block.first_point()) ||
                    isActual_Out(block.first_point()) ||
                    isFunctionEntry(block.first_point()) ||
                    isDeclaration(block.first_point())||
                    isFunctionExit(block.first_point())){

            }else if(isExpression(block.first_point()) &&
                    block.first_point().get_ast(ast_family.getC_UNNORMALIZED()).is_a(ast_class.getUC_INIT()) &&
                    block.first_point().toString().contains("ho_buf = (char*)buffer")){
                traverseCFGNode(block.first_point(),block);
            }else if(isCall_Site(block.first_point())){
                traverseCFGNode(block.first_point(),block);
            }else {
                blockList.add(block);
            }
        }

        for(basic_block block:blockList)
            traverseCFGNode(block.first_point(),block);
    }

    private void finish(){
        for(CFANode node:cfaNodes){
            if(node.getNumEnteringEdges()>0 || node.getNumLeavingEdges()>0)
                cfaBuilder.addNode(functionName, node);
        }
        cfaNodeMap = new HashMap<>();
        locStack = new ArrayDeque<>();
        cfaNodes = new HashSet<>();
        //variableDeclarations.putAll(expressionHandler.variableDeclarations);
        //expressionHandler = null;
        //cfaBuilder = null;
        if(cfa.getExitNode().getNumEnteringEdges()==0)
            printWARNING("Dead exit node in "+ functionName);
    }


    public void checkOrInsertNewGlobalVarDeclarations(symbol varSymbol, CType type,
                                                      CInitializer initializer,
                                                      FileLocation fileLocation) throws result{
        String varName = expressionHandler.getNormalizedVariableName(varSymbol, fileName);
        if(!expressionHandler.globalDeclarations.containsKey(varName.hashCode())){
            expressionHandler.generateVariableDeclaration(varSymbol, type, true, initializer, fileLocation);
        }
    }

    /**
     *@Description traverse local declared symbols in the function
     *@Param [declSet, entryNextNode]
     *@return null
     **/
    private void handleVariableDeclaration(List<point> declSet, point entryNextNode)throws result{

        symbol lastDecSymbol = null;
        boolean isFirstNode = true;

        CFANode prevNode = cfa;
        CVariableDeclaration preVar =null;
        FileLocation fileLocation =null;
        CFANode nextCFANode = cfaNodeMap.get(entryNextNode.id());

        if(declSet.isEmpty()){
            final BlankEdge dummyEdge = new BlankEdge("", FileLocation.DUMMY,
                    prevNode, nextCFANode, "Function start dummy edge");
            addToCFA(dummyEdge);
            //locStack.push(prevNode);
        }else {
            for(point del:declSet){

                symbol variable = del.declared_symbol();//normalized ast
                ast variableAST = variable.get_ast(ast_family.getC_UNNORMALIZED());
                CType type = typeConverter.getCType(variableAST.get(ast_ordinal.getBASE_TYPE()).as_ast(), expressionHandler);
                if(isFirstNode){
                    isFirstNode = false;
                    final CFANode nextNode = newCFANode();

                    final BlankEdge dummyEdge = new BlankEdge("", FileLocation.DUMMY,
                            prevNode, nextNode, "Function start dummy edge");
                    addToCFA(dummyEdge);
                    locStack.push(nextNode);

                }else {
                    prevNode = locStack.peek();
                    CFANode nextNode = newCFANode();
                    CDeclarationEdge edge = new CDeclarationEdge(getRawSignature(del),
                            fileLocation, prevNode, nextNode, preVar);
                    addToCFA(edge);
                    locStack.push(nextNode);
                    cfaNodeMap.put(del.id(),nextNode);
                }

                lastDecSymbol = variable;
                if(variable.get_kind().equals(symbol_kind.getRETURN()))
                    fileLocation = getLocation((int)function.entry_point().file_line().get_second()+1, fileName);
                else{
                    try {
                        fileLocation = getLocation((int)del.file_line().get_second(),fileName);
                    }catch (result r){
                        fileLocation = getLocation((int)function.entry_point().file_line().get_second()+1, fileName);
                    }
                }

                preVar = (CVariableDeclaration) expressionHandler
                        .generateVariableDeclaration(variable, type, false,
                                null, fileLocation);
            }
            prevNode = locStack.peek();
            CDeclarationEdge edge = new CDeclarationEdge(getRawSignature(lastDecSymbol.primary_declaration()),
                    fileLocation,prevNode,nextCFANode, preVar);
            addToCFA(edge);
        }

    }

    /**
     *@Description shall not generate nodes as traversing CFG node,
     *@Param [node, prevNode]
     *@return void
     **/
    private void traverseCFGNode(point cfgNode, basic_block endNode) throws result{

        cfg_edge_set cfgEdgeSet = cfgNode.cfg_targets();
        if(endNode!=null && (!cfgNode.get_basic_block().equals(endNode))){
            return;
        }

        FileLocation fileLocation = getLocation(cfgNode,fileName);//new FileLocation(fileName, 0, 1,getFileLineNumber(cfgNode),0);

        if(isReturn(cfgNode) && fileLocation == null){
            fileLocation = new FileLocation(fileName,0,0,
                    cfa.getFileLocation().getEndingLineNumber(),
                    cfa.getFileLocation().getEndingLineNumber());
        }else if(fileLocation == null)
            printWARNING("A CFG Node has no line: "+ cfgNode.file_line().get_second());

        //printWARNING(cfgNode.toString()+":"+cfgNode.file_line().get_second());

        CFANode cfaNode = cfaNodeMap.get(cfgNode.id());


        if(cfgEdgeSet.empty() && !isFunctionExit(cfgNode)){
            //throw new Exception("");
        } else if(cfaNode==null){
            printf("");
        }else if(cfaNode.getNumLeavingEdges()>0){
            //check if the edge has been built
            return;
        }

        switch (getKindName(cfgNode)){
            case INDIRECT_CALL:
            case CALL_SITE:
                handleFunctionCall(cfgNode, endNode, fileLocation);
                break;
            case CONTROL_POINT:
                if(isIfControlPointNode(cfgNode))
                    handleIFPoint(cfgNode, endNode, fileLocation);
                else if(isWhileControlPointNode(cfgNode))
                    handleWhilePoint(cfgNode, endNode, fileLocation);
                else if(isDoControlPointNode(cfgNode))
                    handleDoWhilePoint(cfgNode, endNode,fileLocation);
                else if(isForControlPointNode(cfgNode))
                    handleForPoint(cfgNode, endNode,fileLocation);
                else if(isSwitchControlPointNode(cfgNode))
                    handleSwitchPoint(cfgNode, endNode,fileLocation);
                else if(isNodeNode(cfgNode)){
                    handleIFPoint(cfgNode, endNode, fileLocation);//ternary operation or if node in Macro
                }else if(isReturnControlPointNode(cfgNode)){
                    handleIFPoint(cfgNode, endNode,fileLocation);
                }else
                    throw new RuntimeException("other control point:"+ cfgNode.toString()+":"+cfgNode.get_syntax_kind().name()+" "+cfgNode.file_line().get_second());
                break;
            case JUMP:
                if(isGotoNode(cfgNode)){
                    handleGotoPoint(cfgNode, endNode, fileLocation);
                }else if(isBreakNode(cfgNode)){
                    handleNormalPoint(cfgNode, endNode, fileLocation,"break");
                }else if(isContinueNode(cfgNode)){
                    handleNormalPoint(cfgNode, endNode, fileLocation,"continue");
                }else
                    throw new RuntimeException("other jump node "+ cfaNode.toString());
                break;
            case EXPRESSION:
                handleExpression(cfgNode, endNode,fileLocation);
                break;
            case LABEL:
                if(isGoToLabel(cfgNode))
                    handleLabelPoint(cfgNode, endNode, fileLocation);
                else if(isElseLabel(cfgNode)){
                    handleNormalPoint(cfgNode, endNode, fileLocation, "else");
                }else if(isDoLabel(cfgNode))
                    handleDoLabelPoint(cfgNode, endNode, fileLocation);
                else if(isNodeNode(cfgNode)){//macro
                    if(cfgNode.get_ast(ast_family.getC_UNNORMALIZED()).is_a(ast_class.getUC_END_TEST_WHILE())){//do label in do while of macro
                        if(cfgNode.is_inside_macro() &&
                                (cfgNode.toString().contains("ASN_DEBUG(")|| cfgNode.toString().contains("ASN_ERROR("))){
                            handleLogMacroPoint(cfgNode, endNode, fileLocation);
                        }else
                            handleDoLabelPoint(cfgNode, endNode, fileLocation);
                    }else {
                        System.out.println("None label node but not an end-test-while node: "+cfgNode.toString()+" "+cfgNode.file_line().get_second());
                    }
                }else
                    throw new RuntimeException("other label node "+ cfaNode.toString());
                break;
            case NORMAL_RETURN:
            case RETURN:
            case EXCEPTIONAL_RETURN:
                handleReturnPoint(cfgNode,fileLocation);
                break;

        }

    }

    private void handleLogMacroPoint(final point macrolog, basic_block endNode, FileLocation fileLocation)throws result{
        CFANode prevNode = cfaNodeMap.get(macrolog.id());
        cfg_edge_vector cfgEdgeVector = macrolog.cfg_targets().to_vector();
        if(cfgEdgeVector.size()==1 && isControl_Point(cfgEdgeVector.get(0).get_first())){//shall only have one target, and is an control point
            point nextMacroPoint = cfgEdgeVector.get(0).get_first().cfg_targets().to_vector().get(1).get_first();
            if(cfgEdgeVector.get(0).get_first().cfg_targets().to_vector().get(1).get_second().name().equals("F")){
                CFANode nextNode = cfaNodeMap.get(nextMacroPoint.id());
                BlankEdge edge = new BlankEdge("LOG", fileLocation, prevNode, nextNode, "LOG");
                addToCFA(edge);
                traverseCFGNode(nextMacroPoint, endNode);
            }else
                throw new RuntimeException("Abnormal macro control point: "+ cfgEdgeVector.get(0).get_first().toString());
        }else {
            throw new RuntimeException("Abnormal macro log point: "+ macrolog.toString());
        }
    }

    /**
     *@Description build the edge for expression node
     *@Param [exprNode, fileLocation]
     *@return void
     **/
    private void handleExpression(final point exprNode, basic_block endNode,
                                  FileLocation fileLocation)throws result{
        assert isExpression(exprNode);

        CFANode prevNode = cfaNodeMap.get(exprNode.id());

        point nextCFGNode = exprNode.cfg_targets().cbegin().current().get_first();

        ast un_ast = exprNode.get_ast(ast_family.getC_UNNORMALIZED());
        ast no_ast = exprNode.get_ast(ast_family.getC_NORMALIZED());
        String rawString = no_ast.pretty_print();

        if(no_ast.is_a(ast_class.getNC_BLOCKASSIGN())){
            // assign a struct variable to another struct variable
            // for struct each member, CFG will have a block assign expr, but actually we only need one express
            // or a struct variable initialization
            CType varType;
            if(un_ast.is_a(ast_class.getUC_INIT())){
                ast init = un_ast.get(ast_ordinal.getUC_DYNAMIC_INIT()).as_ast();
                ast variable = init.get(ast_ordinal.getUC_VARIABLE()).as_ast();
                varType = typeConverter.getCType(
                        variable.get(ast_ordinal.getBASE_TYPE()).as_ast(),
                        expressionHandler);
            }else if(un_ast.is_a(ast_class.getUC_DYNAMIC_INIT_NONCONSTANT_AGGREGATE())){
                ast constant = un_ast.get(ast_ordinal.getUC_CONSTANT()).as_ast();
                varType = typeConverter.getCType(constant.get(ast_ordinal.getBASE_TYPE()).as_ast(), expressionHandler);
            } else{
                varType = typeConverter.getCType(un_ast.get(ast_ordinal.getBASE_TYPE()).as_ast(), expressionHandler);
            }
            rawString = no_ast.get(ast_ordinal.getNC_ORIGINAL()).as_ast().pretty_print();
            nextCFGNode = pointNextToBlockAssignmentExpr(exprNode, varType);
        }

        CFANode nextNode =handleAllSideEffects(nextCFGNode);

        if(un_ast.is_a(ast_class.getUC_VLA_DECL())) {//(void)$temp314 = type array[$temp314];
            throw new RuntimeException("VLA DECL shall have been processed in the previous expression");
        }else if(isTempVariableAssignment(no_ast) && un_ast.is_a(ast_class.getUC_SET_VLA_SIZE())){
            //use temp variable as the function input
            ast tempVar;
            CType type;
            if(no_ast.children().get(0).as_ast().is_a(ast_class.getNC_POINTEREXPR())){
                tempVar = no_ast.children().get(0).as_ast().children().get(0).as_ast();
                type =  typeConverter.getCType(tempVar.get(ast_ordinal.getBASE_TYPE()).as_ast(), expressionHandler);
                rawString = type.toString()+" "+rawString;
                type = new CPointerType(false, false, type);
            }else if(no_ast.children().get(0).as_ast().is_a(ast_class.getNC_VARIABLE())){
                tempVar = no_ast.children().get(0).as_ast();
                type =  typeConverter.getCType(tempVar.get(ast_ordinal.getBASE_TYPE()).as_ast(), expressionHandler);
                rawString = type.toString()+" "+rawString;
            }else{
                dumpAST(no_ast,0, no_ast.get_class().name());
                throw new RuntimeException("Not a pointer expr "+ exprNode.toString());
            }

            CExpression expression = expressionHandler.getExpression(un_ast, type, no_ast.children().get(1).as_ast(), fileLocation);
            CInitializer cInitializer = new CInitializerExpression(fileLocation, expression);
            String varName = tempVar.get(ast_ordinal.getNC_NAME()).as_str();
            CVariableDeclaration newVarDecl =
                    new CVariableDeclaration(
                            fileLocation,
                            false,
                            CStorageClass.AUTO,
                            type,
                            varName,
                            varName,
                            varName,
                            cInitializer);
            expressionHandler.variableDeclarations.put(varName.hashCode(),newVarDecl);

            CDeclarationEdge declarationEdge = new CDeclarationEdge(rawString,fileLocation,prevNode,nextNode,newVarDecl);
            addToCFA(declarationEdge);

            if(nextCFGNode.get_ast(ast_family.getC_UNNORMALIZED()).is_a(ast_class.getUC_VLA_DECL())){
                un_ast = nextCFGNode.get_ast(ast_family.getC_UNNORMALIZED());
                ast variable = un_ast.get(ast_ordinal.getUC_VARIABLE()).as_ast();
                ast vartype = variable.get(ast_ordinal.getBASE_TYPE()).as_ast();
                String arrayName = variable.get(ast_ordinal.getBASE_ABS_LOC()).as_symbol().name().replace("-","_");
                CType elementType = typeConverter.getCType(vartype.get(ast_ordinal.getUC_ELEMENT_TYPE()).as_ast(), expressionHandler);

                CExpression length = new CIdExpression(fileLocation, newVarDecl);
                CType arrayType = new CArrayType(false,false, elementType, length);
                CVariableDeclaration arraryDecl =
                        new CVariableDeclaration(
                                fileLocation,
                                false,
                                CStorageClass.AUTO,
                                arrayType,
                                arrayName,
                                variable.pretty_print(),
                                arrayName,
                                null);

                expressionHandler.variableDeclarations.put(arrayName.hashCode(),arraryDecl);
                nextCFGNode = nextCFGNode.cfg_targets().cbegin().current().get_first();
                rawString = arrayType.toString()+" "+no_ast.pretty_print();
                CFANode nextnextNode = cfaNodeMap.get(nextCFGNode.id());
                CDeclarationEdge edge = new CDeclarationEdge(rawString,
                        fileLocation,
                        nextNode,
                        nextnextNode,
                        arraryDecl);
                addToCFA(edge);
                traverseCFGNode(nextCFGNode, endNode);
            }else
                throw new RuntimeException("Issue in VLA");
        }else if(isTempVariableAssignment(no_ast)){
            ast tempVar;
            CType type;
            CVariableDeclaration variableDeclaration;
            if(no_ast.is_a(ast_class.getNC_BLOCKASSIGN())){
                tempVar= no_ast.get(ast_ordinal.getNC_ORIGINAL()).as_ast().children().get(0).as_ast();
            }else if(no_ast.children().get(0).as_ast().is_a(ast_class.getNC_POINTEREXPR())){
                tempVar = no_ast.children().get(0).as_ast().children().get(0).as_ast();
            }else if(no_ast.children().get(0).as_ast().is_a(ast_class.getNC_VARIABLE())){
                tempVar = no_ast.children().get(0).as_ast();
            }else {
                dumpAST(no_ast,0, no_ast.get_class().name());
                throw new RuntimeException("Not a pointer expr "+ exprNode.toString());
            }

            String variableName = tempVar.get(ast_ordinal.getNC_NAME()).as_str();

            if(un_ast.is_a(ast_class.getUC_ABSTRACT_DYNAMIC_INIT())){
                ast dyam = un_ast.get(ast_ordinal.getUC_CONSTANT()).as_ast();
                type = typeConverter.getCType(dyam.get(ast_ordinal.getBASE_TYPE()).as_ast(), expressionHandler);
                CInitializer initializer = expressionHandler.getInitializerFromUC(un_ast,type, fileLocation);
                variableDeclaration = new CVariableDeclaration(fileLocation,
                        false,
                        CStorageClass.AUTO,
                        type,
                        variableName,
                        variableName,
                        variableName,
                        initializer);
                expressionHandler.variableDeclarations.put(variableName.hashCode(),variableDeclaration);
                String rawstring = type.toString()+" "+rawString;
                CDeclarationEdge edge = new CDeclarationEdge(rawstring, fileLocation, prevNode, nextNode, variableDeclaration);
                addToCFA(edge);
                traverseCFGNode(nextCFGNode, endNode);
            }else {
                if(no_ast.children().get(0).as_ast().is_a(ast_class.getNC_POINTEREXPR())){
                    type =  typeConverter.getCType(tempVar.get(ast_ordinal.getBASE_TYPE()).as_ast(), expressionHandler);
                }else if(no_ast.children().get(0).as_ast().is_a(ast_class.getNC_VARIABLE())){
                    type =  typeConverter.getCType(un_ast.get(ast_ordinal.getBASE_TYPE()).as_ast(), expressionHandler);
                }else {
                    throw new RuntimeException("Not a pointer expr "+ exprNode.toString());
                }

                CExpression rightHandSide;
                if(no_ast.children().get(1).as_ast().pretty_print().contains("$temp")){
                    //TODO get type from noramlized
                    if(un_ast.get(ast_ordinal.getUC_OPERANDS()).as_ast().children().size()==1){
                        rightHandSide = expressionHandler.getExpressionWithTempVar(
                                no_ast.children().get(1).as_ast(),
                                un_ast,
                                fileLocation);
                    }else {
                        rightHandSide = expressionHandler.getExpressionWithTempVar(
                                no_ast.children().get(1).as_ast(),
                                un_ast.get(ast_ordinal.getUC_OPERANDS()).as_ast().children().get(1).as_ast(),
                                fileLocation);
                    }

                }else if(un_ast.is_a(ast_class.getUC_LAND()) || un_ast.is_a(ast_class.getUC_LOR())){
                    rightHandSide = expressionHandler.getExpressionWithTempVar(
                            no_ast.children().get(1).as_ast(),
                            un_ast,
                            fileLocation);
                }else if(un_ast.is_a(ast_class.getUC_ABSTRACT_ASSIGN()))
                    rightHandSide = expressionHandler.getExpressionFromUC(
                            un_ast.get(ast_ordinal.getUC_OPERANDS()).as_ast().children().get(1).as_ast(),
                            type,fileLocation);
                else
                    rightHandSide = expressionHandler.getExpressionFromUC(un_ast,type,fileLocation);

                if(expressionHandler.variableDeclarations.containsKey(variableName.hashCode())){
                    variableDeclaration = (CVariableDeclaration)
                            expressionHandler.variableDeclarations.get(variableName.hashCode());
                    CIdExpression leftHandSide =
                            (CIdExpression) expressionHandler.getAssignedIdExpression(
                                    variableDeclaration, variableDeclaration.getType(), fileLocation);
                    CStatement statement  = new CExpressionAssignmentStatement(fileLocation, leftHandSide, rightHandSide);
                    CStatementEdge edge = new CStatementEdge(rawString,
                            statement, fileLocation, prevNode, nextNode);
                    addToCFA(edge);
                    traverseCFGNode(nextCFGNode, endNode);
                }else {
                    CInitializer initializer = new CInitializerExpression(fileLocation, rightHandSide);
                    variableDeclaration = new CVariableDeclaration(fileLocation,
                            false,
                            CStorageClass.AUTO,
                            type,
                            variableName,
                            variableName,
                            variableName,
                            initializer);
                    expressionHandler.variableDeclarations.put(variableName.hashCode(),variableDeclaration);
                    String rawstring = type.toString()+" "+rawString;
                    CDeclarationEdge edge = new CDeclarationEdge(rawstring, fileLocation, prevNode, nextNode, variableDeclaration);
                    addToCFA(edge);
                    traverseCFGNode(nextCFGNode, endNode);
                }
            }
        }else if(un_ast.is_a(ast_class.getUC_INIT())){
            CVariableDeclaration variableDeclaration;

            if(useTempVariable(no_ast)){//initilizer uses the temp var
                variableDeclaration = (CVariableDeclaration)
                        expressionHandler.generateInitVarDecl(un_ast,no_ast,fileLocation);
            }else{
                variableDeclaration = (CVariableDeclaration)
                        expressionHandler.generateInitVarDeclFromUC(un_ast,fileLocation);
            }

            rawString = variableDeclaration.getType().toString()+" "+rawString;
            CDeclarationEdge edge = new CDeclarationEdge(rawString,
                    fileLocation,
                    prevNode,
                    nextNode,
                    variableDeclaration);
            addToCFA(edge);
            traverseCFGNode(nextCFGNode, endNode);
        }else if(isReturn(nextCFGNode)&& hasReturnVariable()){
            CType type = functionDeclaration.getType().getReturnType();
            CLeftHandSide leftHandSide = (CLeftHandSide) expressionHandler.
                        getAssignedIdExpression((CVariableDeclaration)cfa.getReturnVariable().get(),
                                type, fileLocation);

            CExpression rightHandSide = expressionHandler.getExpression(un_ast,type,no_ast.children().get(1).as_ast(),fileLocation);

            CStatement statement = new CExpressionAssignmentStatement(fileLocation, leftHandSide, rightHandSide);

            CReturnStatement returnStatement = new CReturnStatement(fileLocation,
                                                                    Optional.of(rightHandSide),
                                                                    Optional.of((CAssignment) statement));

            CReturnStatementEdge edge = new CReturnStatementEdge(rawString,
                                            returnStatement, fileLocation,
                                            prevNode, cfa.getExitNode());
            addToCFA(edge);
        }else if(isReturn(nextCFGNode)){
            CType type = typeConverter.getCType(un_ast.get(ast_ordinal.getBASE_TYPE()).as_ast(), expressionHandler);
            CExpression expression = expressionHandler.getExpressionFromUC(un_ast,type,fileLocation);
            CReturnStatement returnStatement = new CReturnStatement(fileLocation, Optional.of(expression),
                    Optional.absent());
            CReturnStatementEdge edge = new CReturnStatementEdge(no_ast.toString(),
                    returnStatement, fileLocation,
                    prevNode, cfa.getExitNode());
            addToCFA(edge);
        }else if(useTempVariable(no_ast) && (!isReturn(nextCFGNode) || !hasReturnVariable())){
            //these expression points that uses a temp var
            // we need to noramlize the expression with the temp var, for example, a[i++]=10==> int temp=i; i=i+1; a[temp]=10;
            CStatement statement = expressionHandler.getAssignStatement(exprNode, fileLocation);
            CStatementEdge edge = new CStatementEdge(no_ast.toString(), statement,
                    fileLocation, prevNode, nextNode);
            addToCFA(edge);
            traverseCFGNode(nextCFGNode, endNode);
        }else if(un_ast.is_a(ast_class.getUC_ABSTRACT_OPERATION())){
            CStatement statement = expressionHandler.getAssignStatementFromUC(un_ast, fileLocation);
            CStatementEdge edge = new CStatementEdge(no_ast.toString(), statement,
                    fileLocation, prevNode, nextNode);
            addToCFA(edge);
            traverseCFGNode(nextCFGNode, endNode);
        }else if(un_ast.is_a(ast_class.getUC_ASM())){
            ast operands = un_ast.get(ast_ordinal.getUC_ASM_ENTRY()).get(ast_ordinal.getUC_GNU_OPERANDS()).as_ast();
            ast leftOper = operands.children().get(0).get(ast_ordinal.getUC_EXPR()).as_ast();
            CType leftType = typeConverter.getCType(leftOper.get(ast_ordinal.getBASE_TYPE()).as_ast(),expressionHandler);
            ast rightOper = operands.children().get(1).get(ast_ordinal.getUC_EXPR()).as_ast();
            CType rightType = typeConverter.getCType(leftOper.get(ast_ordinal.getBASE_TYPE()).as_ast(),expressionHandler);
            CLeftHandSide leftHandSide = (CLeftHandSide)expressionHandler.getExpressionFromUC(leftOper,leftType,fileLocation);
            CExpression rightHandSide = expressionHandler.getExpressionFromUC(rightOper,rightType,fileLocation);
            CStatement statement = new CExpressionAssignmentStatement(fileLocation, leftHandSide, rightHandSide);

            CStatementEdge edge = new CStatementEdge(statement.toString(), statement,
                    fileLocation, prevNode, nextNode);
            addToCFA(edge);
            traverseCFGNode(nextCFGNode, endNode);
        }else if(un_ast.is_a(ast_class.getUC_EXPR_VARIABLE())){
            CType type = typeConverter.getCType(un_ast.get(ast_ordinal.getBASE_TYPE()).as_ast(), expressionHandler);
            CExpression expression = expressionHandler.getExpressionFromUC(un_ast,type, fileLocation);
            CStatement statement = new CExpressionStatement(fileLocation, expression);
            CStatementEdge edge = new CStatementEdge(no_ast.pretty_print(), statement,
                    fileLocation, prevNode, nextNode);
            addToCFA(edge);
            traverseCFGNode(nextCFGNode, endNode);
        }else if(un_ast.is_a(ast_class.getUC_RUNTIME_SIZEOF())){
            //actually, the runtime_sizeof has been processed before
            BlankEdge edge = new BlankEdge(no_ast.pretty_print(),fileLocation, prevNode, nextNode, no_ast.pretty_print());
            addToCFA(edge);
            traverseCFGNode(nextCFGNode, endNode);
        }else  {
            throw new RuntimeException("Not support expression: "+ exprNode.toString()+ " "+ exprNode.file_line().get_second()+ " "+ un_ast.get_class().name() +" "+ nextCFGNode.get_kind().name() +" "+ hasReturnVariable());
        }
    }


    /**
     *@Description TODO need to add inter-edge of function call
     *@Param [cfgNode, prevNode, fileLocation]
     *@return void
     **/
    private void handleFunctionCall(final point cfgNode, basic_block endNode,
                                    FileLocation fileLocation)throws result{
        assert isCall_Site(cfgNode) || isIndirect_Call(cfgNode);

        CFANode prevNode = cfaNodeMap.get(cfgNode.id());
        CFANode nextNode;

        ast callAST = cfgNode.get_ast(ast_family.getC_UNNORMALIZED());
        ast operands = callAST.get(ast_ordinal.getUC_OPERANDS()).as_ast();
        CType type = typeConverter.getCType(callAST.get(ast_ordinal.getBASE_TYPE()).as_ast(), expressionHandler);
        //the 1st child is the function, others are inputs,
        CExpression funcNameExpr = expressionHandler
                .getExpressionFromUC(operands.children().get(0).as_ast(),type,fileLocation);
        String rawCharacters="";
        point actualoutCFGNode = null, nextCFGNode = null;
        point_set actuals_in = cfgNode.actuals_in();
        point_set actuals_out = cfgNode.actuals_out();

        List<CExpression> params = new ArrayList<>();
        if(operands.children().size()>1){
            StringBuilder sb = new StringBuilder(cfgNode.characters().replace(")",""));
            point_vector actualIns = sortActualInVectorByID(actuals_in.to_vector());

            if(actualIns.size()!=operands.children().size()-1)
                throw new RuntimeException("Not consistent param in "+cfgNode.toString());
            for(int i=1;i<operands.children().size();i++){
                ast actual_in = actualIns.get(i-1).get_ast(ast_family.getC_NORMALIZED());
                ast oper = operands.children().get(i).as_ast();
                CType paramType = typeConverter.getCType(oper.get(ast_ordinal.getBASE_TYPE()).as_ast(), expressionHandler);
                CExpression param =expressionHandler.
                        getExpression(oper, paramType, actual_in.children().get(1).as_ast(), fileLocation);

                if(i==operands.children().size()-1)
                sb.append(param.toString()).append(")");
                else
                    sb.append(param.toString()).append(", ");
                params.add(param);
            }
            rawCharacters=sb.toString();
            if(actuals_out.empty()){
                for(int i=0;i<actuals_in.to_vector().size();i++){
                    String param = actuals_in.to_vector().get(i).get_ast(ast_family.getC_NORMALIZED())
                            .children().get(0).as_ast().pretty_print();
                    if(param.equals("$param_1")){
                        nextCFGNode = actuals_in.to_vector().get(i).cfg_targets()
                                .cbegin().current().get_first();
                        break;
                    }
                }
                assert nextCFGNode!=null;
            }
            else {
                actualoutCFGNode = actuals_out.cbegin().current();
                nextCFGNode = actualoutCFGNode.cfg_targets().cbegin().current().get_first();
            }
        }else {
            rawCharacters = getRawSignature(cfgNode);
            if(actuals_out.empty())
                nextCFGNode = cfgNode.cfg_targets().cbegin()
                    .current().get_first();
            else {
                actualoutCFGNode = actuals_out.cbegin().current();
                nextCFGNode = actualoutCFGNode.cfg_targets().cbegin().current().get_first();
            }
        }

        CFunctionCallExpression functionCallExpression;

        if(funcNameExpr instanceof CPointerExpression){
            functionCallExpression = new CFunctionCallExpression(fileLocation,
                    type, funcNameExpr, params, null);
        }else if(typeConverter.isFunctionPointerType(funcNameExpr.getExpressionType())){
            //TODO it may be incorrect
            CType funcType  = typeConverter.getFuntionTypeFromFunctionPointer(funcNameExpr.getExpressionType());
            CPointerExpression pointerExpression = new CPointerExpression(fileLocation, funcType, funcNameExpr);
            functionCallExpression = new CFunctionCallExpression(fileLocation,
                    type, pointerExpression, params, null);
        }else if(funcNameExpr instanceof CFieldReference){
            CFieldReference fieldReference = (CFieldReference)funcNameExpr;
            CType refType = fieldReference.getExpressionType();

            if(refType instanceof CTypedefType){
                refType = ((CTypedefType) refType).getRealType();
            }
            CFunctionType functionType = (CFunctionType) ((CPointerType)refType).getType();

            CPointerExpression pointerExpression = new CPointerExpression(fileLocation,
                    functionType, funcNameExpr);
            functionCallExpression = new CFunctionCallExpression(fileLocation,
                    type, pointerExpression, params, null);
        }else if(((CIdExpression)funcNameExpr).getDeclaration() instanceof CParameterDeclaration){
            functionCallExpression = new CFunctionCallExpression(fileLocation,
                    type, funcNameExpr, params, null);
        }else {
            functionCallExpression = new CFunctionCallExpression(fileLocation,
                    type, funcNameExpr, params,
                    (CFunctionDeclaration) ((CIdExpression)funcNameExpr).getDeclaration());
        }

        CFunctionCall functionCallStatement;

        nextNode = handleAllSideEffects(nextCFGNode);
        CStatementEdge statementEdge;

        if(actualoutCFGNode==null){
            functionCallStatement = new CFunctionCallStatement(fileLocation,functionCallExpression);
            if(nextNode == null)
                throw new RuntimeException("Null nextnode:"+ nextCFGNode.toString() + " with the previous node: "+ cfgNode.toString()+" "+cfgNode.file_line().get_second()+" "+ functionName+" "+fileName);
            else
                statementEdge= new CStatementEdge(rawCharacters,functionCallStatement,
                    fileLocation, prevNode, nextNode);
        }else {

            //shall have an assignment expression
            // condition 1: variable = function();
            // condition 2: no real variable, for example, if(function(p))
            // in this condition, CodeSurfer built a temporary variable
            // and use the variable for following instruction;
            // thus, CFG always has a variable that is assigned with the return value of the called function
            //need a temporary variable to convert the operation as a binary operation,
            // e.g., if(function(p))--> temporary_var = function(p), if(temporary_var)
            //insert a decl node here

            //functionName$result+uid==>tempory result name


            ast un_ast = actualoutCFGNode.get_ast(ast_family.getC_UNNORMALIZED());
            CType actualOutType = typeConverter.getCType(un_ast.get(ast_ordinal.getBASE_TYPE()).as_ast(),expressionHandler);

//            CType returnType;
//            if(cfgNode.get_kind().equals(point_kind.getINDIRECT_CALL()))
//                returnType = type;
//            else
//                returnType = functionCallExpression.getDeclaration().getType().getReturnType();
            String name = funcNameExpr.toString()+"$result__"+ un_ast.get(ast_ordinal.getUC_UID()).as_uint32();

            CVariableDeclaration declaration =
                    new CVariableDeclaration(
                            fileLocation,
                            false,
                            CStorageClass.AUTO,
                            actualOutType,
                            name,
                            name,
                            name,
                            null);

            expressionHandler.variableDeclarations.put(name.hashCode(),declaration);
            String rawString = declaration.toString();
            CFANode declNode = newCFANode();

            CDeclarationEdge declarationEdge = new CDeclarationEdge(rawString,
                    fileLocation,
                    prevNode,
                    declNode,
                    declaration);
            addToCFA(declarationEdge);

            //function call assign
            CLeftHandSide assignedVarExp = new CIdExpression(fileLocation, declaration);

            functionCallStatement = new CFunctionCallAssignmentStatement(
                    fileLocation, assignedVarExp,
                    functionCallExpression);
            statementEdge= new CStatementEdge(rawCharacters,functionCallStatement,
                    fileLocation, declNode, nextNode);
        }

        addToCFA(statementEdge);
        traverseCFGNode(nextCFGNode, endNode);
    }
    /**
     *@Description build edge for label node
     *@Param [labelNode, fileloc]
     *@return void
     **/
    private void handleLabelPoint(final point labelNode, basic_block endNode,  FileLocation fileloc) throws result{

        assert isLabel(labelNode);

        String labelName = getLabelName(labelNode);
        CFANode prevNode = cfaNodeMap.get(labelNode.id());

        point nextCFGNode = labelNode.cfg_targets().cbegin().current().get_first();
        CFANode nextNode = handleAllSideEffects(nextCFGNode);// cfaNodeMap.get(nextCFGNode.id());
        BlankEdge blankEdge =
                new BlankEdge(
                        getRawSignature(labelNode),
                        fileloc,
                        prevNode,
                        nextNode,
                        "Label: " + labelName);
        nextNode.setLoopStart();
        addToCFA(blankEdge);

        traverseCFGNode(nextCFGNode, endNode);
    }

    /**
     *@Description build edge for goto jump node
     *@Param [gotoStatement, fileloc]
     *@return void
     **/
    private void handleGotoPoint(final point gotoNode, basic_block endNode, FileLocation fileloc) throws result{
        assert isJump(gotoNode);
        CFANode prevNode = cfaNodeMap.get(gotoNode.id());

        String gotoLabelName = getGoToLabelName(gotoNode);

        point nextCFGNode = gotoNode.cfg_targets().cbegin().current().get_first();
        assert getLabelName(nextCFGNode).equals(gotoLabelName);
        //goto the node next to the label node
        nextCFGNode = nextCFGNode.cfg_targets().cbegin().current().get_first();

        CFANode nextCFANode = cfaNodeMap.get(nextCFGNode.id());

        BlankEdge gotoEdge = new BlankEdge(getRawSignature(gotoNode),
                fileloc, prevNode, nextCFANode, "Goto: " + gotoLabelName);
        addToCFA(gotoEdge);

        traverseCFGNode(nextCFGNode, endNode);
    }


    //node that has only one processor
    private void handleNormalPoint(final point node, basic_block endNode,  FileLocation fileLocation,
                                   String description) throws result{
        CFANode prevNode = cfaNodeMap.get(node.id());

        point nextCFGNode = node.cfg_targets().cbegin().current().get_first();

        CFANode nextCFANode = handleAllSideEffects(nextCFGNode);//cfaNodeMap.get(nextCFGNode.id());

        BlankEdge gotoEdge = new BlankEdge(getRawSignature(node),
                    fileLocation, prevNode, nextCFANode, description);
        addToCFA(gotoEdge);

        traverseCFGNode(nextCFGNode, endNode);
    }

    private void handleReturnPoint(point returnNode, FileLocation fileloc)throws result {
        //only have unnormalized ast
        CFANode prevNode = cfaNodeMap.get(returnNode.id());
        //if there is the return variable, it should be processed in its processor
        //However, sometime, there is no return semantic, e.g., int a(){};
        if(cfa.getReturnVariable().isPresent() && !isExpression(returnNode.cfg_sources().cbegin().current().get_first())){

            if(isNormal_Return(returnNode)){
                BlankEdge edge = new BlankEdge("",fileloc, prevNode, cfa.getExitNode(), "normal return");
                addToCFA(edge);
                return;
            }else if(isExceptional_Return(returnNode)){
                BlankEdge edge = new BlankEdge("",fileloc, prevNode, cfa.getExitNode(), "exceptional return");
                addToCFA(edge);
                return;
            }
            try {
                ast un_ast = returnNode.get_ast(ast_family.getC_UNNORMALIZED());

                if(un_ast.has_field(ast_ordinal.getUC_RETURN_VALUE())){
                    ast returnValue = un_ast.get(ast_ordinal.getUC_RETURN_VALUE()).as_ast();

                    CType expectedType = typeConverter.getCType(returnValue.get(ast_ordinal.getBASE_TYPE()).as_ast(), expressionHandler);
                    CExpression returnExp =expressionHandler.getExpression(returnValue,
                            expectedType,
                            returnNode.get_ast(ast_family.getC_NORMALIZED()),
                            fileloc);

                    Optional<CExpression> returnExpression = Optional.of(returnExp);

                    CSimpleDeclaration returnVarDecl = (CSimpleDeclaration) cfa.getReturnVariable();

                    CIdExpression returnVar = new CIdExpression(fileloc, returnVarDecl);

                    CAssignment returnVarAssignment =
                            new CExpressionAssignmentStatement(
                                    fileloc, returnVar, returnExp);
                    Optional<CAssignment> returnAssignment = Optional.of(returnVarAssignment);
                    CReturnStatement returnStatement = new CReturnStatement(fileloc, returnExpression, returnAssignment);

                    CReturnStatementEdge returnStatementEdge = new CReturnStatementEdge(getRawSignature(returnNode),
                            returnStatement, fileloc, prevNode, cfa.getExitNode());
                    addToCFA(returnStatementEdge);
                }else {
                    BlankEdge edge;
                    if(isReturn(returnNode))
                        edge = new BlankEdge("",fileloc, prevNode, cfa.getExitNode(), "default return");
                    else if(isNormal_Return(returnNode)){
                        edge = new BlankEdge("",fileloc, prevNode, cfa.getExitNode(), "normal return");
                    }else {
                        edge = new BlankEdge("",fileloc, prevNode, cfa.getExitNode(), "exceptional return");
                    }
                    addToCFA(edge);
                }
            }catch (result r){
                BlankEdge edge;
                if(isReturn(returnNode))
                    edge = new BlankEdge("",fileloc, prevNode, cfa.getExitNode(), "default return");
                else if(isNormal_Return(returnNode)){
                    edge = new BlankEdge("",fileloc, prevNode, cfa.getExitNode(), "normal return");
                }else {
                    edge = new BlankEdge("",fileloc, prevNode, cfa.getExitNode(), "exceptional return");
                }
                addToCFA(edge);
            }
        }else {
            BlankEdge edge;
            if(isReturn(returnNode))
                edge = new BlankEdge("",fileloc, prevNode, cfa.getExitNode(), "default return");
            else if(isNormal_Return(returnNode)){
                edge = new BlankEdge("",fileloc, prevNode, cfa.getExitNode(), "normal return");
            }else {
                edge = new BlankEdge("",fileloc, prevNode, cfa.getExitNode(), "exceptional return");
            }
            addToCFA(edge);
        }
    }

    private void handleWhilePoint(point whileNode, basic_block endNode,  FileLocation fileLocation)throws result{
        assert isWhileControlPointNode(whileNode);
        CFANode prevNode = cfaNodeMap.get(whileNode.id());

        cfg_edge_set cfgEdgeSet = whileNode.cfg_targets();

        point trueCFGNode = cfgEdgeSet.to_vector().get(0).get_first();
        CFANode trueCFANode = cfaNodeMap.get(trueCFGNode.id());

        point falseCFGNode = cfgEdgeSet.to_vector().get(1).get_first();
        CFANode falseCFANode = cfaNodeMap.get(falseCFGNode.id());

        CFANode whileCFANode = newCFANode();
        BlankEdge blankEdge = new BlankEdge(getRawSignature(whileNode), fileLocation,
                prevNode, whileCFANode, "while");
        addToCFA(blankEdge);
        whileCFANode.setLoopStart();
        cfaNodeMap.replace(whileNode.id(), whileCFANode);

        ast uc_condition = whileNode.get_ast(ast_family.getC_UNNORMALIZED());
        CType type = typeConverter.getCType(uc_condition.get(ast_ordinal.getBASE_TYPE()).as_ast(), expressionHandler);
        CExpression conditionExpr=expressionHandler.getExpression(uc_condition,
                type,
                whileNode.get_ast(ast_family.getC_NORMALIZED()),
                fileLocation);

        createConditionEdges(whileCFANode, trueCFANode, trueCFGNode, falseCFANode, falseCFGNode, conditionExpr, endNode, fileLocation);
    }

    private void handleDoWhilePoint(point whileNode, basic_block endNode, FileLocation fileLocation)throws result{
        assert isDoControlPointNode(whileNode);
        CFANode prevNode = cfaNodeMap.get(whileNode.id());
        prevNode.setLoopStart();

        cfg_edge_set cfgEdgeSet = whileNode.cfg_targets();

        point trueCFGNode = cfgEdgeSet.to_vector().get(0).get_first();
        CFANode trueCFANode;
        if(isDoLabel(trueCFGNode))
            trueCFANode = cfaNodeMap.get(trueCFGNode.id()).getLeavingEdge(0).getSuccessor();
        else
            trueCFANode = handleSwitchCasePoint(trueCFGNode);

        point falseCFGNode = cfgEdgeSet.to_vector().get(1).get_first();
        CFANode falseCFANode = handleSwitchCasePoint(falseCFGNode);

        ast uc_condition = whileNode.get_ast(ast_family.getC_UNNORMALIZED());
        CType type = typeConverter.getCType(uc_condition.get(ast_ordinal.getBASE_TYPE()).as_ast(), expressionHandler);
        CExpression conditionExpr= expressionHandler.getExpression(uc_condition,
                type,
                whileNode.get_ast(ast_family.getC_NORMALIZED()),
                fileLocation);

        createConditionEdges(prevNode, trueCFANode, trueCFGNode, falseCFANode, falseCFGNode, conditionExpr, endNode, fileLocation);
    }

    private void handleForPoint(point forNode, basic_block endNode,  FileLocation fileLocation)throws result{
        assert isForControlPointNode(forNode);
        CFANode prevNode = cfaNodeMap.get(forNode.id());
        prevNode.setLoopStart();
        cfg_edge_set cfgEdgeSet = forNode.cfg_targets();

        point trueCFGNode = cfgEdgeSet.to_vector().get(0).get_first();
        point falseCFGNode = cfgEdgeSet.to_vector().get(1).get_first();

        CFANode trueNode = cfaNodeMap.get(trueCFGNode.id());

        CFANode falseNode = cfaNodeMap.get(falseCFGNode.id());

        ast uc_condition = forNode.get_ast(ast_family.getC_UNNORMALIZED());
        if(uc_condition.is_a(ast_class.getUC_FOR())){
            BlankEdge edge = new BlankEdge("for", fileLocation, prevNode, trueNode, "for");
            addToCFA(edge);
            traverseCFGNode(trueCFGNode, endNode);
        }else {
            CType type = typeConverter.getCType(uc_condition.get(ast_ordinal.getBASE_TYPE()).as_ast(), expressionHandler);

            CExpression conditionExpr= expressionHandler.getExpression(uc_condition,
                    type,
                    forNode.get_ast(ast_family.getC_NORMALIZED()),
                    fileLocation);

            createConditionEdges(prevNode,trueNode,trueCFGNode,falseNode,falseCFGNode, conditionExpr, endNode,fileLocation);
        }
    }

    private void handleSwitchPoint(point switchNode, basic_block endNode, FileLocation fileLocation)throws result{
        assert  isSwitchControlPointNode(switchNode);

        CFANode prevNode = cfaNodeMap.get(switchNode.id());

        cfg_edge_vector cfgEdgeVector = sortVectorByLineNo(switchNode.cfg_targets().to_vector());

        ast variableAST = switchNode.get_ast(ast_family.getC_UNNORMALIZED());
        CType variableType = typeConverter.getCType(variableAST
                .get(ast_ordinal.getBASE_TYPE()).as_ast(),expressionHandler);

        CExpression switchExpr = expressionHandler.getExpression(variableAST, variableType,
                switchNode.get_ast(ast_family.getC_NORMALIZED()), fileLocation);

        String rawSignature = "switch (" + getRawSignature(switchNode) + ")";
        String description = "switch (" + getRawSignature(switchNode) + ")";

        cfgEdgeVector = moveSwitchDefault2Last(cfgEdgeVector);
        // firstSwitchNode is first Node of switch-Statement.
        point firstSwitchCFGNode = cfgEdgeVector.get(0).get_first();
        point defaultCFGNode = cfgEdgeVector.get((int)(cfgEdgeVector.size()-1)).get_first();
        CFANode firstSwitchNode = cfaNodeMap.get(firstSwitchCFGNode.id());
        addToCFA(new BlankEdge(rawSignature, fileLocation, prevNode, firstSwitchNode, description));

        //TODO: there is a bug that the default case is not in the end of cfaedges
        if(cfgEdgeVector.size()>1){
            for(int i=0;i<cfgEdgeVector.size()-1;i++){
                CExpression conditionExpr = handleSwitchCase(cfgEdgeVector.get(i).get_first(), endNode, switchExpr);
                String conditionString = conditionExpr.toASTString();
                CFANode case1 = cfaNodeMap.get(cfgEdgeVector.get(i).get_first().id());
                CFANode case2 = cfaNodeMap.get(cfgEdgeVector.get(i+1).get_first().id());
                FileLocation fileLoc = getLocation(cfgEdgeVector.get(i).get_first(),fileName);
                if(cfgEdgeVector.get(i+1).get_second().name().equals("implicit default")){
                    CFANode emptyNode = newCFANode();
                    FileLocation fileLoc2 = getLocation(cfgEdgeVector.get(i+1).get_first(),fileName);
                    CAssumeEdge falseEdge =
                            new CAssumeEdge(
                                    "!(" + conditionString + ")",
                                    fileLoc,
                                    case1,
                                    emptyNode,
                                    conditionExpr,
                                    false,
                                    false,
                                    false);
                    addToCFA(falseEdge);
                    BlankEdge blankEdge = new BlankEdge("",fileLoc2, emptyNode,case2,"");
                    addToCFA(blankEdge);
                    traverseCFGNode(cfgEdgeVector.get(i+1).get_first(), endNode);
                }else if(cfgEdgeVector.get(i+1).get_second().name().equals("default")){
                    CAssumeEdge falseEdge =
                            new CAssumeEdge(
                                    "!(" + conditionString + ")",
                                    fileLoc,
                                    case1,
                                    case2,
                                    conditionExpr,
                                    false,
                                    false,
                                    false);
                    addToCFA(falseEdge);

                    point nextCFGNode = cfgEdgeVector.get(i+1).get_first().cfg_targets().cbegin().current().get_first();
                    FileLocation fileLocation1 = getLocation(defaultCFGNode,fileName);
                    CFANode nextNode = cfaNodeMap.get(nextCFGNode.id());
                    BlankEdge blankEdge = new BlankEdge("default", fileLocation1,
                            case2, nextNode,"default");
                    addToCFA(blankEdge);
                    traverseCFGNode(nextCFGNode, endNode);
                }else {
                    //not include default branch
                    CAssumeEdge falseEdge =
                            new CAssumeEdge(
                                    "!(" + conditionString + ")",
                                    fileLoc,
                                    case1,
                                    case2,
                                    conditionExpr,
                                    false,
                                    false,
                                    false);
                    addToCFA(falseEdge);
                }
            }
        }else {
            if(cfgEdgeVector.get(0).get_second().name().equals("default")){

                point nextCFGNode = firstSwitchCFGNode.cfg_targets().cbegin().current().get_first();
                FileLocation fileLocation1 = getLocation(firstSwitchCFGNode,fileName);
                CFANode nextNode = cfaNodeMap.get(nextCFGNode.id());
                BlankEdge blankEdge = new BlankEdge("default", fileLocation1,
                        firstSwitchNode, nextNode,"default");
                addToCFA(blankEdge);
                traverseCFGNode(nextCFGNode, endNode);
            }else if(cfgEdgeVector.get(0).get_second().name().equals("implicit default")){
                point nextCFGNode = firstSwitchCFGNode.cfg_targets().cbegin().current().get_first();
                FileLocation fileLocation1 = getLocation(firstSwitchCFGNode,fileName);
                CFANode nextNode = cfaNodeMap.get(nextCFGNode.id());
                BlankEdge blankEdge = new BlankEdge("", fileLocation1,
                        firstSwitchNode, nextNode,"");
                addToCFA(blankEdge);
                traverseCFGNode(nextCFGNode, endNode);
            }
        }
    }

    private CExpression handleSwitchCase(point caseNode, basic_block endNode, CExpression switchExpr)throws result{
        CFANode caseCFANode = cfaNodeMap.get(caseNode.id());

        point nextCFGNode = caseNode.cfg_targets().cbegin().current().get_first();

        FileLocation fileLocation = getLocation(caseNode,fileName);
        //case node: no normalized ast
        ast condition = caseNode.get_ast(ast_family.getC_UNNORMALIZED());
        ast valueAST = condition.get(ast_ordinal.getBASE_VALUE()).as_ast()
                    .get(ast_ordinal.getUC_CONSTANT()).as_ast();

        //in c, the case type can only be Integer or Char
        CType valueType = typeConverter.getCType(valueAST.get(ast_ordinal.getBASE_TYPE()).as_ast(), expressionHandler);

        CExpression caseExpr = expressionHandler.getExpressionFromUC(valueAST, valueType, fileLocation);

        CBinaryExpression conditionExpr = expressionHandler.buildBinaryExpression(
                switchExpr, caseExpr, CBinaryExpression.BinaryOperator.EQUALS);

        CFANode nextCFANode = handleAllSideEffects(nextCFGNode);

        String conditionString = conditionExpr.toASTString();
        final CAssumeEdge trueEdge =
                new CAssumeEdge(
                        conditionString,
                        fileLocation,
                        caseCFANode,
                        nextCFANode,
                        conditionExpr,
                        true,
                        false,
                        false);
        addToCFA(trueEdge);
        if(!isSwitchCase(nextCFGNode))
            traverseCFGNode(nextCFGNode, endNode);

        return conditionExpr;
    }

    private CFANode handleAllSideEffects(point nextNode)throws result{

//        //eliminate asm point
//        if(isExpression(nextNode)){
//            if(nextNode.get_ast(ast_family.getC_UNNORMALIZED()).is_a(ast_class.getUC_ASM())){
//                point nextPoint = nextNode.cfg_targets().cbegin().current().get_first();
//                return cfaNodeMap.get(nextPoint.id());
//            }
//        }

        return handleSwitchCasePoint(nextNode);
    }

    private CFANode handleSwitchCasePoint(point caseNode)throws result{
        //if a non-switch control point has a inter edge to a switch case point, this means
        //in the case of the non-switch control point, there is no break point, thus, shall have
        //a fall through edge to the new switch case
        //fall through

        if(!isSwitchCase(caseNode)){
           return cfaNodeMap.get(caseNode.id());
        }
        FileLocation fileLocation = getLocation(caseNode,fileName);

        CFANode caseCFANode = cfaNodeMap.get(caseNode.id());
        CFANode fallNode = newCFANode();


        final BlankEdge blankEdge =
                new BlankEdge("", fileLocation, fallNode, caseCFANode, "fall through");
        fallNode.addLeavingEdge(blankEdge);
        caseCFANode.addEnteringEdge(blankEdge);
        return fallNode;
    }


    private void handleDoLabelPoint(point doWhileNode, basic_block endNode, FileLocation fileLocation)throws result{
        //assert isDoControlPointNode(doWhileNode);
        CFANode prevNode = cfaNodeMap.get(doWhileNode.id());

        point nextCFGNode = doWhileNode.cfg_targets().cbegin().current().get_first();

        CFANode nextCFANode = cfaNodeMap.get(nextCFGNode.id());
        nextCFANode.setLoopStart();

        BlankEdge gotoEdge = new BlankEdge("",
                    fileLocation,
                    prevNode,
                    nextCFANode,
                 "do");
        addToCFA(gotoEdge);

        traverseCFGNode(nextCFGNode, endNode);
    }

    private void handleIFPoint(point ifNode, basic_block endNode, FileLocation fileLocation)throws result{
        //assert isIfControlPointNode(ifNode) ;

        CFANode prevNode = cfaNodeMap.get(ifNode.id());

        cfg_edge_set cfgEdgeSet = ifNode.cfg_targets();

        point thenCFGNode = cfgEdgeSet.to_vector().get(0).get_first();
        point elseCFGNode = cfgEdgeSet.to_vector().get(1).get_first();

        CFANode thenNode = cfaNodeMap.get(thenCFGNode.id());

        // elseNode is the start of the else branch,
        // or the node after the loop if there is no else branch
        CFANode elseNode = cfaNodeMap.get(elseCFGNode.id());

        ast uc_condition = ifNode.get_ast(ast_family.getC_UNNORMALIZED());
        CType type = typeConverter.getCType(uc_condition.get(ast_ordinal.getBASE_TYPE()).as_ast(), expressionHandler);

        CExpression conditionExpr= expressionHandler.getExpression(uc_condition, type,
                ifNode.get_ast(ast_family.getC_NORMALIZED()),
                fileLocation);

        createConditionEdges(prevNode, thenNode, thenCFGNode,
                elseNode, elseCFGNode, conditionExpr, endNode, fileLocation);

    }

    private void createConditionEdges(CFANode rootNode, CFANode thenNode, point thenCFGNode, CFANode elseNode,
                                      point elseCFGNode, CExpression conditionExp, basic_block endNode, FileLocation fileLocation) throws result {

        String conditionString = conditionExp.toString();

        // edge connecting condition with thenNode
        final CAssumeEdge trueEdge =
                new CAssumeEdge(
                        conditionString,
                        fileLocation,
                        rootNode,
                        thenNode,
                        conditionExp,
                        true,
                        false,
                        false);
        addToCFA(trueEdge);
        traverseCFGNode(thenCFGNode, endNode);

        // edge connecting condition with elseNode
        final CAssumeEdge falseEdge =
                new CAssumeEdge(
                        "!(" + conditionString + ")",
                        fileLocation,
                        rootNode,
                        elseNode,
                        conditionExp,
                        false,
                        false,
                        false);
        addToCFA(falseEdge);
        traverseCFGNode(elseCFGNode, endNode);
    }

    private void addToCFA(CFAEdge edge) {
        if(directAddEdge)
            CFACreationUtils.addEdgeToCFA(edge, logger);
        else
            addEdgeUnconditionallyToCFA(edge);
    }

    /**
     * @category helper
     */
    private CFANode newCFANode() {
        assert cfa != null;
        CFANode nextNode = new CFANode(cfa.getFunctionName());
        cfaNodes.add(nextNode);
        return nextNode;
    }

    private boolean hasReturnVariable(){
        assert cfa != null;
        return cfa.getReturnVariable().isPresent();
    }
}
