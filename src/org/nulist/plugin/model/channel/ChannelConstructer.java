package org.nulist.plugin.model.channel;

import com.google.common.base.Optional;
import org.nulist.plugin.parser.CFABuilder;
import org.nulist.plugin.parser.CFGFunctionBuilder;
import org.sosy_lab.cpachecker.cfa.CFA;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.nulist.plugin.model.ChannelBuildOperation.*;
import static org.nulist.plugin.parser.CFGParser.*;
import static org.nulist.plugin.parser.CFGParser.MME;
import static org.nulist.plugin.parser.CFGTypeConverter.VoidPointer;

/**
 * @ClassName ChannelConstructer
 * @Description TODO
 * @Author Yinbo Yu
 * @Date 5/2/19 7:06 PM
 * @Version 1.0
 **/
public class ChannelConstructer {
    public final static String uePushNASMSGIDIntoCache = "uePushNASMSGIDIntoCache";
    public final static String uePushPlainNASEMMMsgIntoCache = "uePushPlainNASEMMMsgIntoCache";
    public final static String uePushPlainNASESMMsgIntoCache = "uePushPlainNASESMMsgIntoCache";
    public final static String uePushPlainASMsgIntoCache = "uePushPlainASMsgIntoCache";
    public final static String uepullPlainNASEMMMsgFromCache = "uepullPlainNASEMMMsgFromCache";
    public final static String uepullPlainNASESMMsgFromCache = "uepullPlainNASESMMsgFromCache";
    public final static String uepullPlainASMsgFromCache = "uepullPlainASMsgFromCache";

    public final static String cnPushNASMSGIDIntoCache = "cnPushNASMSGIDIntoCache";
    public final static String cnPullPlainNASEMMMsgFromCache = "cnPullPlainNASEMMMsgFromCache";
    public final static String cnPullPlainNASESMMsgFromCache = "cnPullPlainNASESMMsgFromCache";
    public final static String cnPushPlainNASEMMMsgIntoCache = "cnPushPlainNASEMMMsgIntoCache";
    public final static String cnPushPlainNASESMMsgIntoCache = "cnPushPlainNASESMMsgIntoCache";
    public final static String cnPullPlainASMsgFromCache = "cnPullPlainASMsgFromCache";
    public final static String cnPushPlainASMsgIntoCache = "cnPushPlainASMsgIntoCache";

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

        String union = "rrc_message_t";
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

        String structname = "rrc_channel_message_s";
        String defName = "rrc_channel_message_t";

        CCompositeType cStructType =
                new CCompositeType(false, false, CComplexType.ComplexTypeKind.STRUCT, structname, structname);
        List<CCompositeType.CCompositeTypeMemberDeclaration> structMembers = new ArrayList<>(2);

        CType uint8 = CNumericTypes.UNSIGNED_INT;

        CCompositeType.CCompositeTypeMemberDeclaration msgID =
                new CCompositeType.CCompositeTypeMemberDeclaration(uint8, "msgID");
        structMembers.add(msgID);

        CCompositeType.CCompositeTypeMemberDeclaration message =
                new CCompositeType.CCompositeTypeMemberDeclaration(cElaboratedType, "message");
        structMembers.add(message);

        cStructType.setMembers(structMembers);

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

    public static CType buildNASChannelMessageType(CFABuilder channelBuilder, CFABuilder builder){
        String structname = "nas_channel_message_s";
        String defName = "nas_channel_message_t";
        CCompositeType cStructType =
                new CCompositeType(false, false, CComplexType.ComplexTypeKind.STRUCT, structname, structname);
        List<CCompositeType.CCompositeTypeMemberDeclaration> members = new ArrayList<>(3);

        CType nas_message_t= builder.typeConverter.typeCache.get("nas_message_t".hashCode());
        CType as_message_t= builder.typeConverter.typeCache.get("as_message_t".hashCode());
        CType uint8 = CNumericTypes.UNSIGNED_INT;

        CCompositeType.CCompositeTypeMemberDeclaration msgID =
                new CCompositeType.CCompositeTypeMemberDeclaration(uint8, "msgID");
        members.add(msgID);

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

        String expressionString = ue_channel_msg_cache+"->nas_message.msgID=msg";
        constructMessagePush(channelBuilder,ueBuilder, ue_channel_message_cache,uePushNASMSGIDIntoCache,CNumericTypes.UNSIGNED_INT, expressionString);

        CType EMM_msg = ueBuilder.typeConverter.typeCache.get("EMM_msg".hashCode());
        expressionString = ue_channel_msg_cache+"->nas_message.nas_message.plain.emm=msg";
        constructMessagePush(channelBuilder,ueBuilder, ue_channel_message_cache,uePushPlainNASEMMMsgIntoCache,EMM_msg, expressionString);
        expressionString = ue_channel_msg_cache+"->nas_message.nas_message.plain.emm";
        constructMessagePull(channelBuilder,ueBuilder, ue_channel_message_cache,uepullPlainNASEMMMsgFromCache,EMM_msg, expressionString);

        CType ESM_msg = ueBuilder.typeConverter.typeCache.get("ESM_msg".hashCode());
        expressionString = ue_channel_msg_cache+"->nas_message.nas_message.plain.esm=msg";
        constructMessagePush(channelBuilder,ueBuilder, ue_channel_message_cache,uePushPlainNASESMMsgIntoCache,ESM_msg, expressionString);
        expressionString = ue_channel_msg_cache+"->nas_message.nas_message.plain.esm";
        constructMessagePull(channelBuilder,ueBuilder, ue_channel_message_cache,uepullPlainNASESMMsgFromCache,ESM_msg, expressionString);

        CType as_message_t = ueBuilder.typeConverter.typeCache.get("as_message_t".hashCode());
        expressionString = ue_channel_msg_cache+"->nas_message.as_message=msg";
        constructMessagePush(channelBuilder,ueBuilder, ue_channel_message_cache,uePushPlainASMsgIntoCache,as_message_t, expressionString);
        expressionString = ue_channel_msg_cache+"->nas_message.as_message";
        constructMessagePull(channelBuilder,ueBuilder, ue_channel_message_cache,uepullPlainASMsgFromCache,as_message_t, expressionString);

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

        expressionString = cn_channel_msg_cache+"->nas_message.msgID=msg";
        constructMessagePush(channelBuilder,mmeBuilder, cn_channel_message_cache,cnPushNASMSGIDIntoCache,CNumericTypes.UNSIGNED_INT, expressionString);

        CType EMM_msg1 = mmeBuilder.typeConverter.typeCache.get("EMM_msg".hashCode());
        expressionString = cn_channel_msg_cache+"->nas_message.nas_message.plain.emm=msg";
        constructMessagePush(channelBuilder,mmeBuilder, cn_channel_message_cache,cnPushPlainNASEMMMsgIntoCache,EMM_msg1, expressionString);
        expressionString = cn_channel_msg_cache+"->nas_message.nas_message.plain.emm";
        constructMessagePull(channelBuilder,mmeBuilder, cn_channel_message_cache,cnPullPlainNASEMMMsgFromCache,EMM_msg1, expressionString);
        CType ESM_msg1 = mmeBuilder.typeConverter.typeCache.get("ESM_msg".hashCode());
        expressionString = cn_channel_msg_cache+"->nas_message.nas_message.plain.esm=msg";
        constructMessagePush(channelBuilder,mmeBuilder, cn_channel_message_cache,cnPushPlainNASESMMsgIntoCache,ESM_msg1, expressionString);
        expressionString = cn_channel_msg_cache+"->nas_message.nas_message.plain.esm";
        constructMessagePull(channelBuilder,mmeBuilder, cn_channel_message_cache,cnPullPlainNASESMMsgFromCache,ESM_msg1, expressionString);
        CType as_message_t1 = mmeBuilder.typeConverter.typeCache.get("as_message_t".hashCode());
        expressionString = cn_channel_msg_cache+"->nas_message.as_message=msg";
        constructMessagePush(channelBuilder,mmeBuilder, cn_channel_message_cache,cnPushPlainASMsgIntoCache,as_message_t1, expressionString);
        expressionString = cn_channel_msg_cache+"->nas_message.as_message";
        constructMessagePull(channelBuilder,mmeBuilder, cn_channel_message_cache,cnPullPlainASMsgFromCache,as_message_t1, expressionString);
    }

    public static void constructMessagePush(CFABuilder channelBuilder,
                                            CFABuilder builder,
                                            CVariableDeclaration channelMsgCache,
                                            String functionName,
                                            CType messageType,
                                            String expressionString){

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
        functionBuilder.expressionHandler.variableDeclarations.put(paramDeclaration.getName().hashCode(),paramDeclaration);
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
        channelBuilder.functionDeclarations.put(functionName,functionDeclaration);
        channelBuilder.expressionHandler.globalDeclarations.put(functionName.hashCode(), functionDeclaration);
        channelBuilder.functions.put(functionName,entry);

        CFANode cfaNode = functionBuilder.newCFANode();
        BlankEdge dummyEdge = new BlankEdge("", FileLocation.DUMMY,
                functionBuilder.cfa, cfaNode, "Function start dummy edge");
        functionBuilder.addToCFA(dummyEdge);

        CIdExpression messageIDExpression = new CIdExpression(fileLocation, channelMsgCache);


//
//        CBinaryExpression conditionExp = functionBuilder.expressionHandler.buildBinaryExpression(
//                messageIDExpression,
//                castExpression,
//                CBinaryExpression.BinaryOperator.EQUALS,
//                CNumericTypes.BOOL);
        String exprString = channelMsgCache.getName()+"==NULL";
        CBinaryExpression conditionExp = (CBinaryExpression) expressionParser(functionBuilder,exprString);
        CFANode trueNode = functionBuilder.newCFANode();
        CFANode falseNode = functionBuilder.newCFANode();
        createTrueFalseEdge(functionBuilder,cfaNode,trueNode,falseNode, exprString,conditionExp);

        initMsgCache(functionBuilder, builder, messageIDExpression, trueNode, falseNode, "tempVar1");

        //channelMSGCache->rrc_message.pcch_msg = msg;
        CFANode msgAssignNode = functionBuilder.newCFANode();

        CBinaryExpression assignExpr = (CBinaryExpression) expressionParser(functionBuilder, expressionString);

        CIdExpression paramMSGID = new CIdExpression(fileLocation,paramDeclaration);
        CExpressionAssignmentStatement msgAssign = new CExpressionAssignmentStatement(
                fileLocation,
                (CLeftHandSide) assignExpr.getOperand1(),
                paramMSGID);

        CStatementEdge msgStatementEdge = new CStatementEdge(
                msgAssign.toString(),
                msgAssign,
                fileLocation,
                falseNode,
                msgAssignNode);
        functionBuilder.addToCFA(msgStatementEdge);
        BlankEdge blankEdge = new BlankEdge("",fileLocation,msgAssignNode,functionExit,"");

        functionBuilder.addToCFA(blankEdge);
        functionBuilder.finish();
    }


    public static void constructMessagePull(CFABuilder channelBuilder,
                                            CFABuilder builder,
                                            CVariableDeclaration channelMsgCache,
                                            String functionName,
                                            CType messageType,
                                            String expressionString){
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
        channelBuilder.functionDeclarations.put(functionName,functionDeclaration);
        channelBuilder.expressionHandler.globalDeclarations.put(functionName.hashCode(), functionDeclaration);
        channelBuilder.functions.put(functionName,entry);

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
//        CIdExpression messageIDExpression = new CIdExpression(fileLocation, channelMsgCache);

        CExpression asmessage = expressionParser(functionBuilder, expressionString);

        CExpressionAssignmentStatement msgAssign = new CExpressionAssignmentStatement(
                fileLocation,
                returnVarIdExpr,
                asmessage);
        CReturnStatement returnStatement = new CReturnStatement(
                fileLocation,
                Optional.of(returnVarIdExpr),
                Optional.of(msgAssign));

        CReturnStatementEdge returnStatementEdge = new CReturnStatementEdge(
                "return "+expressionString+";",
                returnStatement,
                fileLocation,
                decNode,
                functionExit);

        functionBuilder.addToCFA(returnStatementEdge);

        functionBuilder.finish();

    }

    /**
     * @Description
     * call from an itti task in one side,
     * transfer the message in its cache to the cache of another side,
     * during the message transfer, we can also inject the adversary model
     * and call corresponding itti task in another side to process the message
     * @Param [channelBuilder, direction]
     * direction: true: ul (UE->CN), false: dl(CN->UE)
     * @return void
     **/
    public static void constructULMessageDeliver(CFABuilder channelBuilder, CFABuilder sourceBuilder){
        String functionName = "ULMessageDeliver";

        FileLocation fileLocation = FileLocation.DUMMY;
        CFGFunctionBuilder functionBuilder = new CFGFunctionBuilder(
                channelBuilder.logger,
                channelBuilder.typeConverter,
                null,
                functionName,
                "",
                channelBuilder);

        List<CType> paramTypes = new ArrayList<>();
        CType messageIds = sourceBuilder.typeConverter.typeCache.get("MessagesIds".hashCode());
        paramTypes.add(messageIds);
        CFunctionType functionType = new CFunctionType(CVoidType.VOID, paramTypes, false);
        List<CParameterDeclaration> paramDecl = new ArrayList<>();
        CParameterDeclaration messageID = new CParameterDeclaration(fileLocation,messageIds,"messageID");
        functionBuilder.expressionHandler.variableDeclarations.put("messageID".hashCode(),messageID);
        paramDecl.add(messageID);
        CFunctionDeclaration functionDeclaration = new CFunctionDeclaration(fileLocation,functionType,functionName,paramDecl);
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

        CFANode cfaNode = functionBuilder.newCFANode();
        BlankEdge dummyEdge = new BlankEdge("", FileLocation.DUMMY,
                functionBuilder.cfa, cfaNode, "Function start dummy edge");
        functionBuilder.addToCFA(dummyEdge);

        CVariableDeclaration sourceCache =(CVariableDeclaration)channelBuilder.expressionHandler.globalDeclarations.get(ue_channel_msg_cache.hashCode());
        CIdExpression sourceCacheIDExpr = new CIdExpression(fileLocation,sourceCache);

        CVariableDeclaration targetCache =(CVariableDeclaration)channelBuilder.expressionHandler.globalDeclarations.get(cn_channel_msg_cache.hashCode());
        CIdExpression targetCacheIDExpr = new CIdExpression(fileLocation,targetCache);

        functionBuilder.expressionHandler.variableDeclarations.put(sourceCache.getName().hashCode(),sourceCache);
        functionBuilder.expressionHandler.variableDeclarations.put(targetCache.getName().hashCode(),targetCache);


        //init targetCache

        CFANode msgInitNode = functionBuilder.newCFANode();
        initMsgCache(functionBuilder,sourceBuilder, targetCacheIDExpr, cfaNode,msgInitNode,"tempVar1");

        //check if source cache message is null
        String expressionString = sourceCache.getName()+"!=NULL";
        CFANode trueNodeS = functionBuilder.newCFANode();
        CFANode falseNodeS = functionBuilder.newCFANode();
        CBinaryExpression cacheMSGNull = (CBinaryExpression) expressionParser(functionBuilder, expressionString);
        createTrueFalseEdge(functionBuilder,msgInitNode, trueNodeS, falseNodeS, expressionString, cacheMSGNull);

        String rawSignature = "switch(messageID)";
        CFANode firstSwitchNode = functionBuilder.newCFANode();
        functionBuilder.addToCFA(new BlankEdge(rawSignature, fileLocation, trueNodeS, firstSwitchNode, rawSignature));

        expressionString = "messageID==34";//"RRC_DCCH_DATA_REQ = 34"
        CBinaryExpression msgID1 = (CBinaryExpression) expressionParser(functionBuilder, expressionString);
        CFANode trueNodeM1 = functionBuilder.newCFANode();
        CFANode falseNodeM1 = functionBuilder.newCFANode();
        createTrueFalseEdge(functionBuilder,trueNodeS, trueNodeM1, falseNodeM1, expressionString, msgID1);

        CFANode rrcTranNode = functionBuilder.newCFANode();
        rrcSubMessageTranslation(functionBuilder,sourceCache, targetCache,"ul_dcch_msg",trueNodeM1,rrcTranNode);
        CFANode nasTranNode = functionBuilder.newCFANode();
        ulNASMessageTranslation(functionBuilder,sourceCache, targetCache,rrcTranNode,nasTranNode);
        CFANode endCase1 = functionBuilder.newCFANode();
        functionBuilder.addToCFA(new BlankEdge("break;",fileLocation,endCase1,falseNodeS,"break;"));

        expressionString = "messageID==18";//RRC_MAC_CCCH_DATA_REQ = 18-no nas message
        CBinaryExpression msgID2 = (CBinaryExpression) expressionParser(functionBuilder, expressionString);
        CFANode trueNodeM2 = functionBuilder.newCFANode();
        CFANode falseNodeM2 = functionBuilder.newCFANode();
        createTrueFalseEdge(functionBuilder,falseNodeM1, trueNodeM2, falseNodeM2, expressionString, msgID2);
        CFANode endCase2 = functionBuilder.newCFANode();
        rrcSubMessageTranslation(functionBuilder,sourceCache, targetCache,"ul_ccch_msg",trueNodeM2,endCase2);
        functionBuilder.addToCFA(new BlankEdge("break;",fileLocation,endCase2,falseNodeS,"break"));

        CFANode defaultNode = functionBuilder.newCFANode();
        functionBuilder.addToCFA(new BlankEdge("default",fileLocation,falseNodeM2,defaultNode,"default"));
        functionBuilder.addToCFA(new BlankEdge("break;",fileLocation,defaultNode,falseNodeS,"break"));

        CFANode returnNode = functionBuilder.newCFANode();
        initMsgCache(functionBuilder, sourceBuilder, sourceCacheIDExpr, falseNodeS,returnNode, "tempVar2");
        functionBuilder.addToCFA(new BlankEdge("return;",fileLocation, returnNode, functionExit,""));
        functionBuilder.finish();
    }

    private static void ulNASMessageTranslation(CFGFunctionBuilder functionBuilder,
                                                 CVariableDeclaration sourceCache,
                                                 CVariableDeclaration targetCache,
                                                 CFANode startNode,
                                                 CFANode endNode){
        //if sourceCache->nas_message!=null
        FileLocation fileLocation = FileLocation.DUMMY;
        String expressionString =sourceCache.getName()+"->nas_message!=NULL";
        CBinaryExpression conditionExp = (CBinaryExpression) expressionParser(functionBuilder, expressionString);
        CFANode trueNode = functionBuilder.newCFANode();
        createTrueFalseEdge(functionBuilder,startNode,trueNode,endNode, expressionString,conditionExp);

        expressionString =sourceCache.getName()+"->nas_message.msgID";
        CExpression asMSG = expressionParser(functionBuilder,expressionString);

        String rawSignature = "switch("+expressionString+")";
        CFANode firstSwitchNode = functionBuilder.newCFANode();
        functionBuilder.addToCFA(new BlankEdge(rawSignature, fileLocation, trueNode, firstSwitchNode, rawSignature));

        String caseString = expressionString+"==47";//NAS_KENB_REFRESH_REQ = 47, no as message
        CBinaryExpression msgID1 = (CBinaryExpression) expressionParser(functionBuilder, caseString);
        CFANode trueNodeM1 = functionBuilder.newCFANode();
        CFANode falseNodeM1 = functionBuilder.newCFANode();
        createTrueFalseEdge(functionBuilder,firstSwitchNode, trueNodeM1, falseNodeM1, caseString, msgID1);
        CFANode endCase1 = functionBuilder.newCFANode();
        nasSubMessageTranslation(functionBuilder,sourceCache,targetCache,47,trueNodeM1,endCase1);
        functionBuilder.addToCFA(new BlankEdge("break;",fileLocation,endCase1,endNode,"break;"));

        caseString = expressionString+"==48";//NAS_CELL_SELECTION_REQ = 48, as msg id = AS_CELL_INFO_REQ
        CBinaryExpression msgID2 = (CBinaryExpression) expressionParser(functionBuilder, caseString);
        CFANode trueNodeM2 = functionBuilder.newCFANode();
        CFANode falseNodeM2 = functionBuilder.newCFANode();
        createTrueFalseEdge(functionBuilder,falseNodeM1, trueNodeM2, falseNodeM2, caseString, msgID2);
        CFANode endCase2 = functionBuilder.newCFANode();
        nasSubMessageTranslation(functionBuilder,sourceCache,targetCache,48,trueNodeM2,endCase2);
        functionBuilder.addToCFA(new BlankEdge("break;",fileLocation,endCase2,endNode,"break;"));

        caseString = expressionString+"==49";//NAS_CONN_ESTABLI_REQ = 49, as msg id = AS_NAS_ESTABLISH_REQ
        CBinaryExpression msgID3 = (CBinaryExpression) expressionParser(functionBuilder, caseString);
        CFANode trueNodeM3 = functionBuilder.newCFANode();
        CFANode falseNodeM3 = functionBuilder.newCFANode();
        createTrueFalseEdge(functionBuilder,falseNodeM2, trueNodeM3, falseNodeM3, caseString, msgID3);
        CFANode endCase3 = functionBuilder.newCFANode();
        nasSubMessageTranslation(functionBuilder,sourceCache,targetCache,49,trueNodeM3,endCase3);
        functionBuilder.addToCFA(new BlankEdge("break;",fileLocation,endCase3,endNode,"break;"));

        caseString = expressionString+"==50";//NAS_UPLINK_DATA_REQ = 50, as msg id = AS_UL_INFO_TRANSFER_REQ
        CBinaryExpression msgID4 = (CBinaryExpression) expressionParser(functionBuilder, caseString);
        CFANode trueNodeM4 = functionBuilder.newCFANode();
        CFANode falseNodeM4 = functionBuilder.newCFANode();
        createTrueFalseEdge(functionBuilder,falseNodeM3, trueNodeM4, falseNodeM4, caseString, msgID4);
        CFANode endCase4 = functionBuilder.newCFANode();
        nasSubMessageTranslation(functionBuilder,sourceCache,targetCache,50,trueNodeM4,endCase4);
        functionBuilder.addToCFA(new BlankEdge("break;",fileLocation,endCase4,endNode,"break;"));

        CFANode defaultNode = functionBuilder.newCFANode();
        functionBuilder.addToCFA(new BlankEdge("default",fileLocation,falseNodeM4,defaultNode,"default"));
        functionBuilder.addToCFA(new BlankEdge("break;",fileLocation,defaultNode,endNode,"break"));

    }

    public static void nasSubMessageTranslation(CFGFunctionBuilder functionBuilder,
                                                CVariableDeclaration sourceCache,
                                                CVariableDeclaration targetCache,
                                                int msgID,
                                                CFANode startNode,
                                                CFANode endNode){




        String expressionString =sourceCache.getName()+"->nas_message.nas_message!=NULL";
        CExpression conditionExp = (CBinaryExpression) expressionParser(functionBuilder, expressionString);
        CFANode trueNode1 = functionBuilder.newCFANode();
        createTrueFalseEdge(functionBuilder,startNode,trueNode1,endNode, expressionString,conditionExp);


        expressionString =sourceCache.getName()+"->nas_message.nas_message.plain!=NULL";
        conditionExp = (CBinaryExpression) expressionParser(functionBuilder, expressionString);
        CFANode trueNode2 = functionBuilder.newCFANode();
        createTrueFalseEdge(functionBuilder,trueNode1,trueNode2,endNode, expressionString,conditionExp);

        expressionString =sourceCache.getName()+"->nas_message.nas_message.plain.emm!=NULL";
        conditionExp = (CBinaryExpression) expressionParser(functionBuilder, expressionString);
        CFANode trueNode20 = functionBuilder.newCFANode();
        CFANode falseNode20 = functionBuilder.newCFANode();
        createTrueFalseEdge(functionBuilder,trueNode2,trueNode20,falseNode20, expressionString,conditionExp);

        expressionString =sourceCache.getName()+"->nas_message.nas_message.plain.esm!=NULL";
        conditionExp = (CBinaryExpression) expressionParser(functionBuilder, expressionString);
        CFANode trueNode21 = functionBuilder.newCFANode();
        createTrueFalseEdge(functionBuilder,falseNode20,trueNode21,endNode, expressionString,conditionExp);
    }

    //need to change msg id and message type
    //req(NAS->AS)--> ind(AS->NAS)
    //rsp(NAS->AS) --> cnf(AS->NAS)
    public static void ulASSubMessageTranslation(CFGFunctionBuilder functionBuilder,
                                                CVariableDeclaration sourceCache,
                                                CVariableDeclaration targetCache,
                                                int msgID,
                                                CFANode startNode,
                                                CFANode endNode){
        FileLocation fileLocation = FileLocation.DUMMY;
        String expressionString =sourceCache.getName()+"->nas_message.as_message!=NULL";
        CExpression conditionExp = (CBinaryExpression) expressionParser(functionBuilder, expressionString);
        CFANode trueNode1 = functionBuilder.newCFANode();
        createTrueFalseEdge(functionBuilder,startNode,trueNode1,endNode, expressionString,conditionExp);

        String sourceASItemName="";
        String targetASItemName="";
        int targetASMsgID = 0;

        if(msgID==48){
            sourceASItemName = "cell_info_req";
            targetASItemName = "cell_info_ind";
            targetASMsgID = 1026;//AS_CELL_INFO_IND=1026
        }else if(msgID==49){
            sourceASItemName = "nas_establish_req";
            targetASItemName = "nas_establish_ind";
            targetASMsgID = 1028;//AS_NAS_ESTABLISH_IND=1028
        }else if(msgID==50){
            sourceASItemName = "ul_info_transfer_req";
            targetASItemName = "ul_info_transfer_ind";
            targetASMsgID = 1030;//AS_UL_INFO_TRANSFER_IND=1030
        }

        expressionString = targetCache.getName()+"->nas_message.as_message.msgID="+targetASMsgID;
        CBinaryExpression binaryExpression = (CBinaryExpression) expressionParser(functionBuilder, expressionString);
        CExpressionAssignmentStatement assignmentStatement = new CExpressionAssignmentStatement(fileLocation,
                (CLeftHandSide) binaryExpression.getOperand1(),
                binaryExpression.getOperand2());
        CFANode msgIDAssignNode = functionBuilder.newCFANode();
        CStatementEdge statementEdge = new CStatementEdge(expressionString,
                assignmentStatement,
                fileLocation,
                trueNode1, msgIDAssignNode);
        functionBuilder.addToCFA(statementEdge);



    }

    public static void rrcSubMessageTranslation(CFGFunctionBuilder functionBuilder,
                                                CVariableDeclaration sourceCache,
                                                CVariableDeclaration targetCache,
                                                String messageName,
                                                CFANode startNode,
                                                CFANode endNode){

        FileLocation fileLocation = FileLocation.DUMMY;
        String expressionString = sourceCache.getName()+"->rrc_message!=NULL";
        CBinaryExpression rrcmsg = (CBinaryExpression) expressionParser(functionBuilder, expressionString);
        CFANode trueNodeR1 = functionBuilder.newCFANode();
        createTrueFalseEdge(functionBuilder,startNode, trueNodeR1, endNode, expressionString, rrcmsg);

        expressionString = sourceCache.getName()+"->rrc_message.message!=NULL";
        CBinaryExpression rrcmsgMSG = (CBinaryExpression) expressionParser(functionBuilder, expressionString);
        CFANode trueNodeR11 = functionBuilder.newCFANode();
        createTrueFalseEdge(functionBuilder,trueNodeR1, trueNodeR11, endNode, expressionString, rrcmsgMSG);

        expressionString = sourceCache.getName()+"->rrc_message.message."+messageName+"!=NULL";
        CBinaryExpression rrcmsgulccchMSG = (CBinaryExpression) expressionParser(functionBuilder, expressionString);
        CFANode trueNodeR111 = functionBuilder.newCFANode();
        createTrueFalseEdge(functionBuilder,trueNodeR11, trueNodeR111, endNode, expressionString, rrcmsgulccchMSG);

        //targetCache->rrc_message.message.ul_ccch_msg = ()sourceCache->rrc_message.message.ul_ccch_msg

        String rawString = expressionString.replace("!=NULL","");
        CExpression ulccchMSG = rrcmsgulccchMSG.getOperand1();
        expressionString = targetCache.getName()+"->rrc_message.message."+messageName;

        CExpression targetULCCCHMSG = expressionParser(functionBuilder,expressionString);
        CCastExpression rrcmsgCast = new CCastExpression(fileLocation,targetULCCCHMSG.getExpressionType(),ulccchMSG);
        CExpressionAssignmentStatement ulccchMSGtranslation = new CExpressionAssignmentStatement(fileLocation,
                (CLeftHandSide) targetULCCCHMSG,
                rrcmsgCast);
        rawString = expressionString+" = ("+targetULCCCHMSG.getExpressionType().toString()+")"+rawString;
        CStatementEdge ulccchMSGtranslationEdge = new CStatementEdge(rawString,
                ulccchMSGtranslation,
                fileLocation,
                trueNodeR111,
                endNode);
        functionBuilder.addToCFA(ulccchMSGtranslationEdge);
    }

    public static void initMsgCache(CFGFunctionBuilder functionBuilder, CFABuilder builder, CIdExpression msgCache, CFANode startNode, CFANode endNode, String tempVarName){
        FileLocation fileLocation = FileLocation.DUMMY;
        CVariableDeclaration tempVar = new CVariableDeclaration(fileLocation,
                false,
                CStorageClass.AUTO,
                VoidPointer,
                tempVarName,
                tempVarName,
                tempVarName,
                null);
        CFANode decNode = functionBuilder.newCFANode();

        CDeclarationEdge declarationEdge = new CDeclarationEdge("Void *"+tempVarName+";",fileLocation,startNode,decNode,tempVar);
        functionBuilder.addToCFA(declarationEdge);

        CFunctionDeclaration malloc = builder.functionDeclarations.get("malloc");
        CIdExpression mallocID = new CIdExpression(fileLocation, malloc);
        List<CExpression> pParameters = new ArrayList<>();
        CType paramType = malloc.getType().getParameters().get(0);
        int size = msgCache.getName().startsWith("UE")?1238:2310;
        pParameters.add(new CIntegerLiteralExpression(fileLocation,paramType, BigInteger.valueOf(size)));//TODO in mme nas_message_t = 1784
        CFunctionCallExpression functionCallExpression = new CFunctionCallExpression(
                fileLocation,
                VoidPointer,
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
                tempVarName+" = malloc("+size+");",
                assignmentStatement,
                fileLocation,
                decNode,
                mallocNode);
        functionBuilder.addToCFA(statementEdge);

        CCastExpression castExpression1 = new CCastExpression(fileLocation,msgCache.getExpressionType(),tempVarId);
        CExpressionAssignmentStatement assignment = new CExpressionAssignmentStatement(
                fileLocation,
                msgCache,
                castExpression1);


        CStatementEdge targetCacheInit = new CStatementEdge(
                msgCache.getName()+"=("+msgCache.getExpressionType().toString()+")"+tempVarName,
                assignment,
                fileLocation,
                mallocNode,
                endNode);

        functionBuilder.addToCFA(targetCacheInit);
    }

    public static void createTrueFalseEdge(CFGFunctionBuilder builder, CFANode rootNode, CFANode trueNode, CFANode falseNode, String conditionExpr, CExpression expression){
        CAssumeEdge trueEdge21 = new CAssumeEdge(
                conditionExpr,
                FileLocation.DUMMY,
                rootNode,
                trueNode,
                expression,
                true,
                false,
                false);
        builder.addToCFA(trueEdge21);


        CAssumeEdge falseEdge21 = new CAssumeEdge(
                "!("+conditionExpr+")",
                FileLocation.DUMMY,
                rootNode,
                falseNode,
                expression,
                false,
                false,
                false);
        builder.addToCFA(falseEdge21);
    }

    public static CExpression expressionParser(CFGFunctionBuilder functionBuilder, String expressionString){
        //==, !=, =
        String left, right;
        CBinaryExpression.BinaryOperator operator = CBinaryExpression.BinaryOperator.EQUALS;
        if(expressionString.contains("==")|| expressionString.contains("!=")){
            if(expressionString.contains("!="))
                operator = CBinaryExpression.BinaryOperator.NOT_EQUALS;
            left = expressionString.split(operator.getOperator())[0].trim();
            right = expressionString.split(operator.getOperator())[1].trim();
        }else if(expressionString.contains("=")){
            left = expressionString.split("=")[0].trim();
            right = expressionString.split("=")[1].trim();
        }else {
            String[] leftelements = splitStringExpression(expressionString);
            return expressionParser(functionBuilder,leftelements);
        }
        String[] leftelements = splitStringExpression(left);
        String[] rightelements = splitStringExpression(right);
        CExpression lefthand = expressionParser(functionBuilder,leftelements);
        CExpression righthand = expressionParser(functionBuilder,rightelements);

        return functionBuilder.expressionHandler.buildBinaryExpression(lefthand,righthand,operator,CNumericTypes.BOOL);
    }

    public static CType getRealCompositeType(CType type){
        if(type instanceof CTypedefType)
            return getRealCompositeType(((CTypedefType) type).getRealType());
        if(type instanceof CElaboratedType)
            return getRealCompositeType(((CElaboratedType) type).getRealType());
        if(type instanceof CCompositeType)
            return type;
        return null;
    }

    private static CType getMemberType(CCompositeType type, String memeber){
        for(CCompositeType.CCompositeTypeMemberDeclaration memberParam: type.getMembers()){
            if(memberParam.getName().equals(memeber))
                return memberParam.getType();
        }
        throw new RuntimeException(memeber+" does not belong to the type: " +type.toString());
    }

    public static CExpression expressionParser(CFGFunctionBuilder functionBuilder, String[] exprElements){
        FileLocation fileLocation = FileLocation.DUMMY;
        if(exprElements.length==1){
            if(exprElements[0].equals("NULL")){
                CType voidpointer = new CPointerType(false,false, CVoidType.VOID);
                return new CCastExpression(fileLocation,  voidpointer,  CIntegerLiteralExpression.ZERO);
            }else {
                try {
                    int caseID = Integer.valueOf(exprElements[0]);
                    return new CIntegerLiteralExpression(fileLocation, CNumericTypes.UNSIGNED_INT, BigInteger.valueOf(caseID));
                }catch (Exception e){
                    throw new RuntimeException(e);
                }
            }
        }else {
            CVariableDeclaration variableDeclaration;
            if(functionBuilder.expressionHandler.globalDeclarations.containsKey(exprElements[0].hashCode()))
                variableDeclaration = (CVariableDeclaration)
                    functionBuilder.expressionHandler.globalDeclarations.get(exprElements[0].hashCode());
            else
                variableDeclaration = (CVariableDeclaration)
                        functionBuilder.expressionHandler.variableDeclarations.get(exprElements[0].hashCode());
            CExpression expression = new CIdExpression(fileLocation,variableDeclaration);
            for(int i=1;i<exprElements.length;i+=2){
                CCompositeType cCompositeType = (CCompositeType) getRealCompositeType(expression.getExpressionType());
                String memberName = exprElements[i+1];
                CType memberType = getMemberType(cCompositeType, memberName);
                boolean ispointerder = exprElements[i].equals("->");
                expression = new CFieldReference(fileLocation,
                        memberType,
                        memberName,
                        expression,
                        ispointerder);
            }
            return expression;
        }
    }

    public static String[] splitStringExpression(String expression){
        List<String> elements = new ArrayList<>();
        char[] chars = expression.toCharArray();
        StringBuilder builder = new StringBuilder();
        for(int i=0;i<chars.length;i++){
            if((chars[i]=='-' && chars[i+1]=='>')){
                elements.add(builder.toString());
                elements.add("->");
                builder = new StringBuilder();
                i++;
            }else if(chars[i]=='.'){
                elements.add(builder.toString());
                elements.add(".");
                builder = new StringBuilder();
            }else
                builder.append(chars[i]);
        }
        elements.add(builder.toString());
        return elements.toArray(new String[elements.size()]);
    }

}
