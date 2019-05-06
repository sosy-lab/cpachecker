package org.nulist.plugin.model.channel;

import com.google.common.base.Optional;
import com.grammatech.cs.result;
import io.shiftleft.fuzzyc2cpg.ast.AstNode;
import io.shiftleft.fuzzyc2cpg.ast.CodeLocation;
import io.shiftleft.fuzzyc2cpg.parser.TokenSubStream;
import io.shiftleft.fuzzyc2cpg.parser.functions.AntlrCFunctionParserDriver;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Lexer;
import org.nulist.plugin.parser.CFABuilder;
import org.nulist.plugin.parser.CFGFunctionBuilder;
import org.sosy_lab.cpachecker.cfa.ParseResult;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.*;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.*;
import org.sosy_lab.cpachecker.exceptions.ParserException;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

import static org.nulist.plugin.model.channel.ChannelConstructer.createTrueFalseEdge;
import static org.nulist.plugin.model.channel.ChannelConstructer.expressionParser;

/**
 * @ClassName ChannelBuilder
 * @Description TODO
 * @Author Yinbo Yu
 * @Date 5/5/19 5:06 PM
 * @Version 1.0
 **/
public class ChannelBuilder {

    public Map<String, CFABuilder> builderMap;
    public  AntlrCFunctionParserDriver driver;
    public CFABuilder channelBuilder;

    public ChannelBuilder (Map<String, CFABuilder> builderMap, AntlrCFunctionParserDriver driver){
        this.builderMap = builderMap;
        this.driver = driver;
    }

    public CFABuilder parseBuildFile(AntlrCFunctionParserDriver driver,String buildName){

        channelBuilder=new CFABuilder(null, MachineModel.LINUX64,buildName);

        AstNode top = driver.builderStack.peek().getItem();

        Map<Integer, List<AstNode>> globalNodes = globalStatementMap(top);

        globalNodes.forEach(((integer, astNodes) -> {
            AstNode astNode = astNodes.get(astNodes.size()-1);
            switch (astNode.getTypeAsString()){
                case "IdentifierDeclStatement"://global variable
                    CDeclaration declaration = parseVariableDeclaration(true, astNode);
                    channelBuilder.expressionHandler.globalDeclarations.put(declaration.getName().hashCode(),declaration);
                    break;
                case "CompoundStatement"://function
                    functionDeclaration(astNodes);
                    break;
            }
        }));



        return channelBuilder;
    }

    public void functionDeclaration(List<AstNode> astNodes){

        String functionName = astNodes.get(1).getEscapedCodeStr();
        CFGFunctionBuilder functionBuilder = new CFGFunctionBuilder(null,
                channelBuilder.typeConverter, null,
                functionName,
                driver.filename,
                channelBuilder);

        CType returnType = getType(astNodes.get(0).getEscapedCodeStr());
        List<CType> paramTypes = new ArrayList<>();
        List<CParameterDeclaration> parameterDeclarations = new ArrayList<>();
        boolean isTakeArgs = false;

        if(astNodes.size()>5){
            String typename="", param="";
            for(int i=3;i<astNodes.size()-2;i++){
                FileLocation fileLocation = getFileLocation(astNodes.get(i).getLocation());
                if(typename.equals("...")){
                    isTakeArgs = true;
                    continue;
                }
                typename = astNodes.get(i).getEscapedCodeStr();
                i++;
                param = astNodes.get(i).getEscapedCodeStr();
                i++;
                if(!astNodes.get(i).getEscapedCodeStr().equals(",") && !astNodes.get(i).getEscapedCodeStr().equals(")")){
                    typename +=" "+param;
                    param = astNodes.get(i).getEscapedCodeStr();
                    i++;
                }
                CType type = getType(typename);
                paramTypes.add(type);

                CParameterDeclaration parameterDeclaration = new CParameterDeclaration(fileLocation,type,param);
                parameterDeclarations.add(parameterDeclaration);
            }
        }
        CFunctionTypeWithNames functionTypeWithNames = new CFunctionTypeWithNames(returnType,
                parameterDeclarations,
                isTakeArgs);
        FileLocation fileLocation = getFileLocation(astNodes.get(astNodes.size()-1).getLocation());
        CFunctionDeclaration functionDeclaration = new CFunctionDeclaration(fileLocation,
                functionTypeWithNames,
                astNodes.get(1).getEscapedCodeStr(),
                parameterDeclarations);

        functionBuilder.setFunctionDeclaration(functionDeclaration);

        FunctionExitNode functionExit = new FunctionExitNode(functionName);
        CFunctionEntryNode entry = new CFunctionEntryNode(
                fileLocation, functionDeclaration, functionExit, Optional.absent());
        functionExit.setEntryNode(entry);

        functionBuilder.cfa = entry;

        channelBuilder.addNode(functionName, functionExit);
        channelBuilder.addNode(functionName, entry);
        channelBuilder.functionDeclarations.put(functionName,functionDeclaration);
        channelBuilder.expressionHandler.globalDeclarations.put(functionName.hashCode(), functionDeclaration);
        channelBuilder.functions.put(functionName,entry);

        functionVisitor(functionBuilder, astNodes.get(astNodes.size()-1));
    }
    public void functionVisitor(CFGFunctionBuilder functionBuilder, AstNode node){

        CFANode cfaNode = functionBuilder.newCFANode();
        BlankEdge dummyEdge = new BlankEdge("", FileLocation.DUMMY,
                functionBuilder.cfa, cfaNode, "Function start dummy edge");
        functionBuilder.addToCFA(dummyEdge);

        treeVisitor(functionBuilder,node, cfaNode,functionBuilder.cfa.getExitNode());

    }

    public void treeVisitor(CFGFunctionBuilder functionBuilder, AstNode node, CFANode startNode1, CFANode endNode1){
        Iterator<AstNode> iterator = node.getChildIterator();
        CFANode startNode , endNode= startNode1;
        while (iterator.hasNext()){
            AstNode astNode = iterator.next();
            startNode = endNode;
            endNode = functionBuilder.newCFANode();
            switch (astNode.getTypeAsString()){
                case "IdentifierDeclStatement":
                    variableDeclaration(functionBuilder, astNode,startNode,endNode);
                    break;
                case "ExpressionStatement":
                    expressionStatement(functionBuilder,astNode,startNode,endNode);
                    break;
                case "IfStatement":
                    ifStatement(functionBuilder,astNode,startNode,endNode);
                    break;
                case "SwitchStatement":
                    switchStatement(functionBuilder,astNode,startNode,endNode);
                    break;
                case "CaseStatement":
                    break;
                case "ForStatement":
                    forStatement(functionBuilder,astNode,startNode,endNode);
                    break;
                case "WhileStatement":
                    whileStatement(functionBuilder,astNode,startNode,endNode);
                    break;
                case "ReturnStatement":

                    break;

            }
        }
    }



    public void ifStatement(CFGFunctionBuilder functionBuilder, AstNode astNode, CFANode startNode, CFANode endNode){
        AstNode condition = astNode.getChild(0);
        AstNode statement = astNode.getChild(1);

        CBinaryExpression conditionExpr = (CBinaryExpression) expressionParser(functionBuilder, condition.getEscapedCodeStr());
        CFANode ifnode = functionBuilder.newCFANode();
        createTrueFalseEdge(functionBuilder,startNode,ifnode,endNode, condition.getEscapedCodeStr(),conditionExpr);

        treeVisitor(functionBuilder, statement, ifnode,endNode);
    }

    public void switchStatement(CFGFunctionBuilder functionBuilder, AstNode astNode, CFANode startNode, CFANode endNode){
        String switchString = astNode.getEscapedCodeStr();
        AstNode condition = astNode.getChild(0);
        AstNode statement = astNode.getChild(1);

        String variableString = condition.getEscapedCodeStr();
        CFANode switchNode = functionBuilder.newCFANode();
        FileLocation fileLocation = getFileLocation(astNode.getLocation());
        functionBuilder.addToCFA(new BlankEdge(switchString, fileLocation, startNode, switchNode, switchString));

        CExpression variableExpr = expressionParser(functionBuilder,variableString);

        Iterator<AstNode> iterator = statement.getChildIterator();
        while (iterator.hasNext()) {
            AstNode caseNode = iterator.next();
            if(caseNode.getTypeAsString().equals("Label")){

            }

        }

    }

    public void forStatement(CFGFunctionBuilder functionBuilder, AstNode astNode, CFANode startNode, CFANode endNode){
        String switchString = astNode.getEscapedCodeStr();
        AstNode condition1 = astNode.getChild(0);
        AstNode condition2 = astNode.getChild(1);
        AstNode condition3 = astNode.getChild(2);
        AstNode statement = astNode.getChild(3);

        treeVisitor(functionBuilder, statement, ifnode,endNode);
    }

    public void whileStatement(CFGFunctionBuilder functionBuilder, AstNode astNode, CFANode startNode, CFANode endNode){

    }

    public void expressionStatement(CFGFunctionBuilder functionBuilder, AstNode astNode, CFANode startNode, CFANode endNode){
        String expressionString = astNode.getEscapedCodeStr().replace("\\n","").replace(";","");
        CBinaryExpression expression = (CBinaryExpression) expressionParser(functionBuilder, expressionString);
        FileLocation fileLocation = getFileLocation(astNode.getLocation());
        CStatement assignment = new CExpressionAssignmentStatement(fileLocation,(CLeftHandSide)expression.getOperand1(),expression.getOperand2());
        CStatementEdge edge = new CStatementEdge(astNode.getEscapedCodeStr(),
                assignment,
                fileLocation,
                startNode,
                endNode);
        functionBuilder.addToCFA(edge);
    }

    public void variableDeclaration(CFGFunctionBuilder functionBuilder, AstNode astNode, CFANode startNode, CFANode endNode){
        CDeclaration declaration = parseVariableDeclaration(false, astNode);
        functionBuilder.expressionHandler.variableDeclarations.put(declaration.getName().hashCode(),declaration);
        CDeclarationEdge declarationEdge = new CDeclarationEdge(astNode.getEscapedCodeStr(),
                declaration.getFileLocation(),
                startNode,
                endNode,
                declaration);
        functionBuilder.addToCFA(declarationEdge);
    }

    public CType getType(String typename){
        if(channelBuilder.typeConverter.typeCache.containsKey(typename.hashCode()))
            return channelBuilder.typeConverter.typeCache.get(typename.hashCode());
        else {
            for(CFABuilder cfaBuilder: builderMap.values()){
                if(cfaBuilder.typeConverter.typeCache.containsKey(typename.hashCode()))
                    return cfaBuilder.typeConverter.typeCache.get(typename.hashCode());
            }
            throw new RuntimeException("No existing type "+typename);
        }
    }

    public CDeclaration parseVariableDeclaration(boolean isGlobal, AstNode node){
        String expressionString = node.getEscapedCodeStr().replace("\\n","").replace(";","");
        FileLocation fileLocation = getFileLocation(node.getLocation());
        return declarationParser(fileLocation, isGlobal, expressionString);
    }

    public Map<Integer, List<AstNode>> globalStatementMap(AstNode top){
        int i=0;
        Map<Integer, List<AstNode>> astNodeMap = new HashMap<>();
        Iterator<AstNode> iterator = top.getChildIterator();
        List<AstNode> astNodes = new ArrayList<>();
        while (iterator.hasNext()){
            AstNode node = iterator.next();
            astNodes.add(node);
            if(!node.getTypeAsString().equals("Statement")){
                astNodeMap.put(i, astNodes);
                astNodes = new ArrayList<>();
            }
            i++;
        }
        return astNodeMap;
    }

    private FileLocation getFileLocation(CodeLocation location){
        return new FileLocation(driver.filename, 0,1,location.startLine,location.endLine);
    }

    public CDeclaration declarationParser(FileLocation fileLocation, boolean isGlobal, String declarationString){
        String typeString = declarationString.split(" ")[0].trim();
        CType type = getType(typeString);
        declarationString = declarationString.replace(typeString+" ","");
        String varName;
        CInitializer initializer = null;
        if(declarationString.contains("=")){
            varName = declarationString.split("=")[0].trim();
            String initializerString = declarationString.split("=")[1].trim();
            CExpression expression = expressionParser(null, new String[]{initializerString});
            initializer = new CInitializerExpression(fileLocation,expression);
        }
        else
            varName = declarationString.trim();

        CVariableDeclaration variableDeclaration = new CVariableDeclaration(
                fileLocation,
                isGlobal,
                CStorageClass.AUTO,
                type,
                varName,
                varName,
                varName,
                initializer);

        return variableDeclaration;
    }





}
