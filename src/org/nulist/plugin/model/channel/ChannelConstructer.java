package org.nulist.plugin.model.channel;

import com.google.common.base.Optional;
import org.nulist.plugin.parser.CFABuilder;
import org.nulist.plugin.parser.CFGFunctionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.*;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.*;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.*;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.nulist.plugin.model.ChannelBuildOperation.*;
import static org.nulist.plugin.parser.CFGParser.*;
import static org.nulist.plugin.parser.CFGParser.MME;

/**
 * @ClassName ChannelConstructer
 * @Description TODO
 * @Author Yinbo Yu
 * @Date 5/2/19 7:06 PM
 * @Version 1.0
 **/
public class ChannelConstructer {
    public final static String uepullPlainNASEMMMsgFromCache = "uepullPlainNASEMMMsgFromCache";
    public final static String uepullPlainNASESMMsgFromCache = "uepullPlainNASESMMsgFromCache";
    public final static String uepushPlainNASEMMMsgIntoCache = "uepushPlainNASEMMMsgIntoCache";
    public final static String uepushPlainNASESMMsgIntoCache = "uepushPlainNASESMMsgIntoCache";
    public final static String uepullPlainASMsgFromCache = "uepullPlainASMsgFromCache";
    public final static String uepushPlainASMsgIntoCache = "uepushPlainASMsgIntoCache";

    public final static String mmepullPlainNASEMMMsgFromCache = "mmepullPlainNASEMMMsgFromCache";
    public final static String mmepullPlainNASESMMsgFromCache = "mmepullPlainNASESMMsgFromCache";
    public final static String mmepushPlainNASEMMMsgIntoCache = "mmepushPlainNASEMMMsgIntoCache";
    public final static String mmepushPlainNASESMMsgIntoCache = "mmepushPlainNASESMMsgIntoCache";
    public final static String mmepullPlainASMsgFromCache = "mmepullPlainASMsgFromCache";
    public final static String mmepushPlainASMsgIntoCache = "mmepushPlainASMsgIntoCache";

    public static CFABuilder constructionMessageChannel(Map<String, CFABuilder> builderMap){


        CFABuilder channelBuilder = new CFABuilder(null, MachineModel.LINUX64,"Channel");

        constructChannelCache(channelBuilder,builderMap);
        CFABuilder ueBuilder = builderMap.get(UE);


        return channelBuilder;
    }



    public static void buildChannelMessageType(CFABuilder channelBuilder, Map<String, CFABuilder> builderMap){
        CFABuilder ueBuilder = builderMap.get(UE);
        CType nas = buildNASChannelMessageType(channelBuilder, ueBuilder);
        CType rrc = buildRRCChannelMessageType(channelBuilder, ueBuilder);
        buildChannelMessageType(channelBuilder,nas,rrc,"ue");


        CFABuilder enbBuilder = builderMap.get(ENB);
        CFABuilder mmeBuilder = builderMap.get(MME);
        CType mmenas = buildNASChannelMessageType(channelBuilder, mmeBuilder);
        CType enbrrc = buildRRCChannelMessageType(channelBuilder, enbBuilder);
        buildChannelMessageType(channelBuilder,mmenas,enbrrc,"cn");
    }

    public static void buildChannelMessageType(CFABuilder channelBuilder, CType nas,CType rrc, String typeside){
        String typename = typeside+"_channel_message_s";
        String defname = typeside+"_channel_message_t";
        CCompositeType cStructType =
                new CCompositeType(false, false, CComplexType.ComplexTypeKind.STRUCT, typename, typename);
        List<CCompositeType.CCompositeTypeMemberDeclaration> members = new ArrayList<>(2);


        CCompositeType.CCompositeTypeMemberDeclaration nasMessage =
                new CCompositeType.CCompositeTypeMemberDeclaration(nas, "nas_message");
        members.add(nasMessage);

        CCompositeType.CCompositeTypeMemberDeclaration rrcMessage =
                new CCompositeType.CCompositeTypeMemberDeclaration(rrc, "rrc_message");
        members.add(rrcMessage);

        cStructType.setMembers(members);

        CElaboratedType cEStructType = new CElaboratedType(false,
                false,
                CComplexType.ComplexTypeKind.STRUCT,
                typename,
                typename,
                cStructType);
        CTypedefType cTypedefType = new CTypedefType(false,false, defname, cEStructType);
        channelBuilder.typeConverter.typeCache.put(defname.hashCode(),cTypedefType);
    }

    public static CType buildRRCChannelMessageType(CFABuilder channelBuilder, CFABuilder builder){
        String union = "rrc_channel_message_s";
        List<CCompositeType.CCompositeTypeMemberDeclaration> members = new ArrayList<>();

        CType LTE_UL_CCCH_Message_t= builder.typeConverter.typeCache.get("LTE_UL_CCCH_Message_t".hashCode());
        CCompositeType.CCompositeTypeMemberDeclaration memberDeclaration1 =
                new CCompositeType.CCompositeTypeMemberDeclaration(LTE_UL_CCCH_Message_t, "ul_ccch_msg");
        members.add(memberDeclaration1);

        CType LTE_UL_DCCH_Message_t= builder.typeConverter.typeCache.get("LTE_UL_DCCH_Message_t".hashCode());
        CCompositeType.CCompositeTypeMemberDeclaration memberDeclaration2 =
                new CCompositeType.CCompositeTypeMemberDeclaration(LTE_UL_DCCH_Message_t, "ul_dcch_msg");
        members.add(memberDeclaration2);

        CType LTE_DL_CCCH_Message_t= builder.typeConverter.typeCache.get("LTE_DL_CCCH_Message_t".hashCode());
        CCompositeType.CCompositeTypeMemberDeclaration memberDeclaration3 =
                new CCompositeType.CCompositeTypeMemberDeclaration(LTE_DL_CCCH_Message_t, "dl_ccch_msg");
        members.add(memberDeclaration3);

        CType LTE_DL_DCCH_Message_t= builder.typeConverter.typeCache.get("LTE_DL_DCCH_Message_t".hashCode());
        CCompositeType.CCompositeTypeMemberDeclaration memberDeclaration4 =
                new CCompositeType.CCompositeTypeMemberDeclaration(LTE_DL_DCCH_Message_t, "dl_dcch_msg");
        members.add(memberDeclaration4);

        CType LTE_BCCH_BCH_Message_t= builder.typeConverter.typeCache.get("LTE_BCCH_BCH_Message_t".hashCode());
        CCompositeType.CCompositeTypeMemberDeclaration memberDeclaration5 =
                new CCompositeType.CCompositeTypeMemberDeclaration(LTE_BCCH_BCH_Message_t, "bcch_bch_msg");
        members.add(memberDeclaration5);

        CType LTE_BCCH_DL_SCH_Message_t= builder.typeConverter.typeCache.get("LTE_BCCH_DL_SCH_Message_t".hashCode());
        CCompositeType.CCompositeTypeMemberDeclaration memberDeclaration6 =
                new CCompositeType.CCompositeTypeMemberDeclaration(LTE_BCCH_DL_SCH_Message_t, "bcch_dl_sch_msg");
        members.add(memberDeclaration6);

        CType LTE_PCCH_Message_t= builder.typeConverter.typeCache.get("LTE_PCCH_Message_t".hashCode());
        CCompositeType.CCompositeTypeMemberDeclaration memberDeclaration7 =
                new CCompositeType.CCompositeTypeMemberDeclaration(LTE_PCCH_Message_t, "pcch_msg");
        members.add(memberDeclaration7);



        CCompositeType cCompositeType = new CCompositeType(false, false,
                CComplexType.ComplexTypeKind.UNION,
                members, union, union);

        CElaboratedType cElaboratedType= new CElaboratedType(false, false,
                CComplexType.ComplexTypeKind.UNION,
                union, union, cCompositeType);

        channelBuilder.typeConverter.typeCache.put(union.hashCode(),cElaboratedType);

        return cElaboratedType;
    }

    public static CType buildNASChannelMessageType(CFABuilder channelBuilder, CFABuilder builder){
        String structname = "nas_channel_message_s";
        String defName = "nas_channel_message_t";
        CCompositeType cStructType =
                new CCompositeType(false, false, CComplexType.ComplexTypeKind.STRUCT, structname, structname);
        List<CCompositeType.CCompositeTypeMemberDeclaration> members = new ArrayList<>(2);

        CType nas_message_t= builder.typeConverter.typeCache.get("nas_message_t".hashCode());
        CType as_message_t= builder.typeConverter.typeCache.get("as_message_t".hashCode());

        CCompositeType.CCompositeTypeMemberDeclaration nasMessage =
                new CCompositeType.CCompositeTypeMemberDeclaration(nas_message_t, "nas_message");
        members.add(nasMessage);

        CCompositeType.CCompositeTypeMemberDeclaration asMessage =
                new CCompositeType.CCompositeTypeMemberDeclaration(as_message_t, "as_message");
        members.add(asMessage);

        cStructType.setMembers(members);

        CElaboratedType cEStructType = new CElaboratedType(false,
                false,
                CComplexType.ComplexTypeKind.STRUCT,
                structname,
                structname,
                cStructType);
        CTypedefType cTypedefType = new CTypedefType(false,false,defName, cEStructType);
        channelBuilder.typeConverter.typeCache.put(defName.hashCode(),cTypedefType);
        return cTypedefType;
    }

    public static void constructChannelCache(CFABuilder channelBuilder, Map<String, CFABuilder> builderMap){
        buildChannelMessageType(channelBuilder,builderMap);

        CFABuilder ueBuilder = builderMap.get(UE);
        CType ue_channel_message_t = channelBuilder.typeConverter.typeCache.get("ue_channel_message_t".hashCode());

        FileLocation fileLocation = FileLocation.DUMMY;

        //build global channel message caches
        CType nullpointer = new CPointerType(false,false, CVoidType.VOID);
        CCastExpression castExpression = new CCastExpression(fileLocation,
                nullpointer,
                CIntegerLiteralExpression.ZERO);

        CInitializer nullInitializer = new CInitializerExpression(fileLocation,castExpression);

        //build a global variable of ue-side channel message cache
        CVariableDeclaration ue_channel_message_cache = new CVariableDeclaration(fileLocation,
                true,
                CStorageClass.AUTO,
                new CPointerType(false,false, ue_channel_message_t),
                ue_channel_msg_cache,ue_channel_msg_cache,ue_channel_msg_cache,
                nullInitializer);

        channelBuilder.expressionHandler.globalDeclarations.put(ue_channel_msg_cache.hashCode(),ue_channel_message_cache);

        CType EMM_msg = ueBuilder.typeConverter.typeCache.get("EMM_msg".hashCode());
        constructMessagePush(channelBuilder,ueBuilder, ue_channel_message_cache,uepushPlainNASEMMMsgIntoCache,EMM_msg);
        constructMessagePull(channelBuilder,ueBuilder, ue_channel_message_cache,uepullPlainNASEMMMsgFromCache,EMM_msg);
        CType ESM_msg = ueBuilder.typeConverter.typeCache.get("ESM_msg".hashCode());
        constructMessagePush(channelBuilder,ueBuilder, ue_channel_message_cache,uepushPlainNASESMMsgIntoCache,ESM_msg);
        constructMessagePull(channelBuilder,ueBuilder, ue_channel_message_cache,uepullPlainNASESMMsgFromCache,ESM_msg);
        CType as_message_t = ueBuilder.typeConverter.typeCache.get("as_message_t".hashCode());
        constructMessagePush(channelBuilder,ueBuilder, ue_channel_message_cache,uepushPlainASMsgIntoCache,as_message_t);
        constructMessagePull(channelBuilder,ueBuilder, ue_channel_message_cache,uepullPlainASMsgFromCache,as_message_t);

        CFABuilder enbBuilder = builderMap.get(ENB);
        CFABuilder mmeBuilder = builderMap.get(MME);

        CType cn_channel_message_t = channelBuilder.typeConverter.typeCache.get("cn_channel_message_t".hashCode());

        //build a global variable of enb-side channel message cache
        CVariableDeclaration cn_channel_message_cache = new CVariableDeclaration(fileLocation,
                true,
                CStorageClass.AUTO,
                new CPointerType(false,false, cn_channel_message_t),
                cn_channel_msg_cache,cn_channel_msg_cache,cn_channel_msg_cache,
                nullInitializer);

        channelBuilder.expressionHandler.globalDeclarations.put(cn_channel_msg_cache.hashCode(),cn_channel_message_cache);


        CType EMM_msg1 = mmeBuilder.typeConverter.typeCache.get("EMM_msg".hashCode());
        constructMessagePush(channelBuilder,mmeBuilder, cn_channel_message_cache,mmepushPlainNASEMMMsgIntoCache,EMM_msg1);
        constructMessagePull(channelBuilder,mmeBuilder, cn_channel_message_cache,mmepullPlainNASEMMMsgFromCache,EMM_msg1);
        CType ESM_msg1 = mmeBuilder.typeConverter.typeCache.get("ESM_msg".hashCode());
        constructMessagePush(channelBuilder,mmeBuilder, cn_channel_message_cache,mmepushPlainNASESMMsgIntoCache,ESM_msg1);
        constructMessagePull(channelBuilder,mmeBuilder, cn_channel_message_cache,mmepullPlainNASESMMsgFromCache,ESM_msg1);
        CType as_message_t1 = mmeBuilder.typeConverter.typeCache.get("as_message_t".hashCode());
        constructMessagePush(channelBuilder,mmeBuilder, cn_channel_message_cache,mmepushPlainASMsgIntoCache,as_message_t1);
        constructMessagePull(channelBuilder,mmeBuilder, cn_channel_message_cache,mmepullPlainASMsgFromCache,as_message_t1);
    }


    public static void constructMessagePush(CFABuilder channelBuilder, CFABuilder builder, CVariableDeclaration channelMsgCache, String functionName, CType messageType){

        FileLocation fileLocation = FileLocation.DUMMY;
        CFGFunctionBuilder functionBuilder = new CFGFunctionBuilder(
                channelBuilder.logger,
                channelBuilder.typeConverter,
                null,
                functionName,
                "",
                channelBuilder);

        List<CType> paramTypes = new ArrayList<>();
        paramTypes.add(messageType);
        CFunctionType functionType = new CFunctionType(CVoidType.VOID, paramTypes, false);
        CParameterDeclaration paramDeclaration = new CParameterDeclaration(fileLocation,messageType,"msg");
        List<CParameterDeclaration> params = new ArrayList<>();
        params.add(paramDeclaration);
        CFunctionDeclaration functionDeclaration = new CFunctionDeclaration(fileLocation,functionType,functionName,params);
        functionBuilder.setFunctionDeclaration(functionDeclaration);


        FunctionExitNode functionExit = new FunctionExitNode(functionName);
        CFunctionEntryNode entry = new CFunctionEntryNode(
                fileLocation, functionDeclaration, functionExit, Optional.absent());
        functionExit.setEntryNode(entry);

        functionBuilder.cfa = entry;

        channelBuilder.addNode(functionName, functionExit);
        channelBuilder.addNode(functionName, entry);

        CFANode cfaNode = functionBuilder.newCFANode();
        BlankEdge dummyEdge = new BlankEdge("", FileLocation.DUMMY,
                functionBuilder.cfa, cfaNode, "Function start dummy edge");
        functionBuilder.addToCFA(dummyEdge);

        CIdExpression messageIDExpression = new CIdExpression(fileLocation, channelMsgCache);
        CType voidpointer = new CPointerType(false,false, CVoidType.VOID);
        CCastExpression castExpression = new CCastExpression(fileLocation,
                voidpointer,
                CIntegerLiteralExpression.ZERO);

        CBinaryExpression conditionExp = functionBuilder.expressionHandler.buildBinaryExpression(
                messageIDExpression,
                castExpression,
                CBinaryExpression.BinaryOperator.EQUALS,
                CNumericTypes.BOOL);

        CFANode trueNode = functionBuilder.newCFANode();
        CFANode falseNode = functionBuilder.newCFANode();
        CAssumeEdge trueEdge = new CAssumeEdge(
                        channelMsgCache.getName()+"==NULL",
                        fileLocation,
                        cfaNode,
                        trueNode,
                        conditionExp,
                        true,
                        false,
                        false);
        functionBuilder.addToCFA(trueEdge);

        CVariableDeclaration tempVar = new CVariableDeclaration(fileLocation,
                false,
                CStorageClass.AUTO,
                voidpointer,
                "temp1",
                "temp1",
                "temp1",
                null);
        CFANode decNode = functionBuilder.newCFANode();

        CDeclarationEdge declarationEdge = new CDeclarationEdge("Void *temp1;",fileLocation,trueNode,decNode,tempVar);
        functionBuilder.addToCFA(declarationEdge);

        CFunctionDeclaration malloc = builder.functionDeclarations.get("malloc");
        CIdExpression mallocID = new CIdExpression(fileLocation, malloc);
        List<CExpression> pParameters = new ArrayList<>();
        CType paramType = malloc.getType().getParameters().get(0);
        pParameters.add(new CIntegerLiteralExpression(fileLocation,paramType, BigInteger.valueOf(100)));//TODO
        CFunctionCallExpression functionCallExpression = new CFunctionCallExpression(
                fileLocation,
                voidpointer,
                mallocID,
                pParameters,
                malloc);
        CIdExpression tempVarId = new CIdExpression(fileLocation, tempVar);

        CFunctionCallAssignmentStatement assignmentStatement = new CFunctionCallAssignmentStatement(
                fileLocation,
                tempVarId,
                functionCallExpression);
        CFANode mallocNode = functionBuilder.newCFANode();
        CStatementEdge statementEdge = new CStatementEdge(
                "temp1=malloc(100);",
                assignmentStatement,
                fileLocation,
                decNode,
                mallocNode);
        functionBuilder.addToCFA(statementEdge);

        CCastExpression castExpression1 = new CCastExpression(fileLocation,channelMsgCache.getType(),tempVarId);
        CExpressionAssignmentStatement assignment = new CExpressionAssignmentStatement(
                fileLocation,
                messageIDExpression,
                castExpression1);

        CStatementEdge statementEdge1 = new CStatementEdge(
                channelMsgCache.getName()+"=("+channelMsgCache.getType().toString()+")temp1",
                assignment,
                fileLocation,
                mallocNode,
                falseNode);

        functionBuilder.addToCFA(statementEdge1);

        CAssumeEdge falseEdge = new CAssumeEdge(
                channelMsgCache.getName()+"==NULL",
                fileLocation,
                cfaNode,
                falseNode,
                conditionExp,
                false,
                false,
                false);
        functionBuilder.addToCFA(falseEdge);

        //channelMSGCache->rrc_message.pcch_msg = msg;
        CFANode msgAssignNode = functionBuilder.newCFANode();

        if(functionName.contains("PlainNASEMM")){
            //channelMSGCache->nas_message.nas_message.plain.emm = msg;
            CType nas_channel_message_t = builder.typeConverter.typeCache.get("nas_channel_message_t".hashCode());

            CFieldReference fieldReference = new CFieldReference(fileLocation,
                    nas_channel_message_t,
                    "nas_message",
                    messageIDExpression,
                    true);

            CType nas_message_t = builder.typeConverter.typeCache.get("nas_message_t".hashCode());
            CFieldReference fieldReference1 = new CFieldReference(fileLocation,
                    nas_message_t,
                    "nas_message",
                    fieldReference,
                    false);

            CType nas_message_plain_t = builder.typeConverter.typeCache.get("nas_message_plain_t".hashCode());
            CFieldReference plain = new CFieldReference(fileLocation,
                    nas_message_plain_t,
                    "plain",
                    fieldReference1,
                    false);
            CType EMM_msg = builder.typeConverter.typeCache.get("EMM_msg".hashCode());
            CFieldReference emm = new CFieldReference(fileLocation,
                    EMM_msg,
                    "emm",
                    plain,
                    false);

            CIdExpression paramMSGID = new CIdExpression(fileLocation,paramDeclaration);
            CExpressionAssignmentStatement msgAssign = new CExpressionAssignmentStatement(
                    fileLocation,
                    emm,
                    paramMSGID);

            CStatementEdge msgStatementEdge = new CStatementEdge(
                    msgAssign.toString(),
                    msgAssign,
                    fileLocation,
                    falseNode,
                    msgAssignNode);
            functionBuilder.addToCFA(msgStatementEdge);
        }else if(functionName.contains("PlainNASESM")){
            //channelMSGCache->nas_message.nas_message.plain.esm = msg;
            CType nas_channel_message_t = builder.typeConverter.typeCache.get("nas_channel_message_t".hashCode());

            CFieldReference fieldReference = new CFieldReference(fileLocation,
                    nas_channel_message_t,
                    "nas_message",
                    messageIDExpression,
                    true);

            CType nas_message_t = builder.typeConverter.typeCache.get("nas_message_t".hashCode());
            CFieldReference fieldReference1 = new CFieldReference(fileLocation,
                    nas_message_t,
                    "nas_message",
                    fieldReference,
                    false);

            CType nas_message_plain_t = builder.typeConverter.typeCache.get("nas_message_plain_t".hashCode());
            CFieldReference plain = new CFieldReference(fileLocation,
                    nas_message_plain_t,
                    "plain",
                    fieldReference1,
                    false);
            CType ESM_msg = builder.typeConverter.typeCache.get("ESM_msg".hashCode());
            CFieldReference esm = new CFieldReference(fileLocation,
                    ESM_msg,
                    "esm",
                    plain,
                    false);

            CIdExpression paramMSGID = new CIdExpression(fileLocation,paramDeclaration);
            CExpressionAssignmentStatement msgAssign = new CExpressionAssignmentStatement(
                    fileLocation,
                    esm,
                    paramMSGID);

            CStatementEdge msgStatementEdge = new CStatementEdge(
                    msgAssign.toString(),
                    msgAssign,
                    fileLocation,
                    falseNode,
                    msgAssignNode);
            functionBuilder.addToCFA(msgStatementEdge);
        }else if(functionName.contains("PlainAS")){
            //channelMSGCache->nas_message.as_message = msg;
            CType nas_channel_message_t = builder.typeConverter.typeCache.get("nas_channel_message_t".hashCode());

            CFieldReference fieldReference = new CFieldReference(fileLocation,
                    nas_channel_message_t,
                    "nas_message",
                    messageIDExpression,
                    true);

            CFieldReference fieldReference1 = new CFieldReference(fileLocation,
                    messageType,
                    "as_message",
                    fieldReference,
                    false);
            CIdExpression paramMSGID = new CIdExpression(fileLocation,paramDeclaration);
            CExpressionAssignmentStatement msgAssign = new CExpressionAssignmentStatement(
                    fileLocation,
                    fieldReference1,
                    paramMSGID);

            CStatementEdge msgStatementEdge = new CStatementEdge(
                    msgAssign.toString(),
                    msgAssign,
                    fileLocation,
                    falseNode,
                    msgAssignNode);
            functionBuilder.addToCFA(msgStatementEdge);
        }

        BlankEdge blankEdge = new BlankEdge("",fileLocation,msgAssignNode,functionExit,"");

        functionBuilder.addToCFA(blankEdge);
        functionBuilder.finish();
    }


    public static void constructMessagePull(CFABuilder channelBuilder, CFABuilder builder, CVariableDeclaration channelMsgCache, String functionName, CType messageType){
        FileLocation fileLocation = FileLocation.DUMMY;
        CFGFunctionBuilder functionBuilder = new CFGFunctionBuilder(
                channelBuilder.logger,
                channelBuilder.typeConverter,
                null,
                functionName,
                "",
                channelBuilder);

        CFunctionType functionType = new CFunctionType(messageType, new ArrayList<>(), false);
        CFunctionDeclaration functionDeclaration = new CFunctionDeclaration(fileLocation,functionType,functionName,new ArrayList<>());
        functionBuilder.setFunctionDeclaration(functionDeclaration);

        Optional<CVariableDeclaration> returnVar = Optional.absent();
        String returnVarName = "returnVar";
        CVariableDeclaration returnVararDecl =
                new CVariableDeclaration(
                        fileLocation,
                        false,
                        CStorageClass.AUTO,
                        messageType,
                        returnVarName,
                        returnVarName,
                        returnVarName,
                        null);
        functionBuilder.expressionHandler.variableDeclarations.put(returnVarName.hashCode(),returnVararDecl);
        returnVar =Optional.of(returnVararDecl);

        FunctionExitNode functionExit = new FunctionExitNode(functionName);
        CFunctionEntryNode entry = new CFunctionEntryNode(
                fileLocation, functionDeclaration, functionExit, returnVar);
        functionExit.setEntryNode(entry);

        functionBuilder.cfa = entry;

        channelBuilder.addNode(functionName, functionExit);
        channelBuilder.addNode(functionName, entry);

        CFANode cfaNode = functionBuilder.newCFANode();
        BlankEdge dummyEdge = new BlankEdge("", FileLocation.DUMMY,
                functionBuilder.cfa, cfaNode, "Function start dummy edge");
        functionBuilder.addToCFA(dummyEdge);

        CFANode decNode = functionBuilder.newCFANode();

        CDeclarationEdge declarationEdge = new CDeclarationEdge(
                messageType.toString()+" "+returnVarName+";",
                fileLocation,cfaNode,decNode,returnVararDecl);
        functionBuilder.addToCFA(declarationEdge);

        CIdExpression returnVarIdExpr = new CIdExpression(fileLocation,returnVararDecl);
        CIdExpression messageIDExpression = new CIdExpression(fileLocation, channelMsgCache);

        if(functionName.contains("PlainNASEMM")){
            //channelMSGCache->nas_message.nas_message.plain.emm = msg;
            CType nas_channel_message_t = builder.typeConverter.typeCache.get("nas_channel_message_t".hashCode());

            CFieldReference fieldReference = new CFieldReference(fileLocation,
                    nas_channel_message_t,
                    "nas_message",
                    messageIDExpression,
                    true);

            CType nas_message_t = builder.typeConverter.typeCache.get("nas_message_t".hashCode());
            CFieldReference fieldReference1 = new CFieldReference(fileLocation,
                    nas_message_t,
                    "nas_message",
                    fieldReference,
                    false);

            CType nas_message_plain_t = builder.typeConverter.typeCache.get("nas_message_plain_t".hashCode());
            CFieldReference plain = new CFieldReference(fileLocation,
                    nas_message_plain_t,
                    "plain",
                    fieldReference1,
                    false);
            CType EMM_msg = builder.typeConverter.typeCache.get("EMM_msg".hashCode());
            CFieldReference emm = new CFieldReference(fileLocation,
                    EMM_msg,
                    "emm",
                    plain,
                    false);


            CExpressionAssignmentStatement msgAssign = new CExpressionAssignmentStatement(
                    fileLocation,
                    returnVarIdExpr,
                    emm);
            CReturnStatement returnStatement = new CReturnStatement(
                    fileLocation,
                    Optional.of(returnVarIdExpr),
                    Optional.of(msgAssign));


            CReturnStatementEdge returnStatementEdge = new CReturnStatementEdge(
                    "return "+emm.toString()+";",
                    returnStatement,
                    fileLocation,
                    decNode,
                    functionExit);

            functionBuilder.addToCFA(returnStatementEdge);
        }else if(functionName.contains("PlainNASESM")){
            //channelMSGCache->nas_message.nas_message.plain.esm = msg;
            CType nas_channel_message_t = builder.typeConverter.typeCache.get("nas_channel_message_t".hashCode());

            CFieldReference fieldReference = new CFieldReference(fileLocation,
                    nas_channel_message_t,
                    "nas_message",
                    messageIDExpression,
                    true);

            CType nas_message_t = builder.typeConverter.typeCache.get("nas_message_t".hashCode());
            CFieldReference fieldReference1 = new CFieldReference(fileLocation,
                    nas_message_t,
                    "nas_message",
                    fieldReference,
                    false);

            CType nas_message_plain_t = builder.typeConverter.typeCache.get("nas_message_plain_t".hashCode());
            CFieldReference plain = new CFieldReference(fileLocation,
                    nas_message_plain_t,
                    "plain",
                    fieldReference1,
                    false);
            CType ESM_msg = builder.typeConverter.typeCache.get("ESM_msg".hashCode());
            CFieldReference esm = new CFieldReference(fileLocation,
                    ESM_msg,
                    "esm",
                    plain,
                    false);

            CExpressionAssignmentStatement msgAssign = new CExpressionAssignmentStatement(
                    fileLocation,
                    returnVarIdExpr,
                    esm);
            CReturnStatement returnStatement = new CReturnStatement(
                    fileLocation,
                    Optional.of(returnVarIdExpr),
                    Optional.of(msgAssign));


            CReturnStatementEdge returnStatementEdge = new CReturnStatementEdge(
                    "return "+esm.toString()+";",
                    returnStatement,
                    fileLocation,
                    decNode,
                    functionExit);

            functionBuilder.addToCFA(returnStatementEdge);
        }else if(functionName.contains("PlainAS")){
            //channelMSGCache->nas_message.as_message = msg;
            CType nas_channel_message_t = builder.typeConverter.typeCache.get("nas_channel_message_t".hashCode());

            CFieldReference fieldReference = new CFieldReference(fileLocation,
                    nas_channel_message_t,
                    "nas_message",
                    messageIDExpression,
                    true);

            CFieldReference fieldReference1 = new CFieldReference(fileLocation,
                    messageType,
                    "as_message",
                    fieldReference,
                    false);
            CExpressionAssignmentStatement msgAssign = new CExpressionAssignmentStatement(
                    fileLocation,
                    returnVarIdExpr,
                    fieldReference1);
            CReturnStatement returnStatement = new CReturnStatement(
                    fileLocation,
                    Optional.of(returnVarIdExpr),
                    Optional.of(msgAssign));


            CReturnStatementEdge returnStatementEdge = new CReturnStatementEdge(
                    "return "+fieldReference1.toString()+";",
                    returnStatement,
                    fileLocation,
                    decNode,
                    functionExit);

            functionBuilder.addToCFA(returnStatementEdge);
        }

    }
}
