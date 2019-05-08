package org.nulist.plugin.model;

import org.eclipse.cdt.internal.core.dom.parser.c.CTypedef;
import org.nulist.plugin.parser.CFABuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.c.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.nulist.plugin.model.ChannelBuildOperation.cn_channel_msg_cache;
import static org.nulist.plugin.model.ChannelBuildOperation.ue_channel_msg_cache;
import static org.nulist.plugin.model.channel.ChannelConstructer.getRealCompositeType;
import static org.nulist.plugin.util.ClassTool.printWARNING;

/**
 * @ClassName MsgTranslationGenerator
 * @Description TODO
 * @Author Yinbo Yu
 * @Date 5/7/19 7:02 PM
 * @Version 1.0
 **/
public class MsgTranslationGenerator {

    private static String message = "ATTACH_REQUEST_msg,ATTACH_ACCEPT_msg,ATTACH_COMPLETE_msg,ATTACH_REJECT_msg,DETACH_REQUEST_msg,DETACH_ACCEPT_msg,TRACKING_AREA_UPDATE_REQUEST_msg,TRACKING_AREA_UPDATE_ACCEPT_msg,TRACKING_AREA_UPDATE_COMPLETE_msg,TRACKING_AREA_UPDATE_REJECT_msg,EXTENDED_SERVICE_REQUEST_msg,SERVICE_REQUEST_msg,SERVICE_REJECT_msg,GUTI_REALLOCATION_COMMAND_msg,GUTI_REALLOCATION_COMPLETE_msg,AUTHENTICATION_REQUEST_msg,AUTHENTICATION_RESPONSE_msg,AUTHENTICATION_REJECT_msg,AUTHENTICATION_FAILURE_msg,IDENTITY_REQUEST_msg,IDENTITY_RESPONSE_msg,SECURITY_MODE_COMMAND_msg,SECURITY_MODE_COMPLETE_msg,SECURITY_MODE_REJECT_msg,EMM_STATUS_msg,EMM_INFORMATION_msg,DOWNLINK_NAS_TRANSPORT_msg,UPLINK_NAS_TRANSPORT_msg,CS_SERVICE_NOTIFICATION_msg";

    public static void generateNASmessageTranslation(CFABuilder ueBuilder, CFABuilder mmeBuilder){
        String[] nasmessages = message.replace(" ","").split(",");
        try {
            FileWriter fileWriter = new FileWriter(new File("messageTranslation.c"));
            for(String nasmsg:nasmessages){
                CType ueType = ueBuilder.typeConverter.typeCache.get(nasmsg.toLowerCase().hashCode());
                CType mmeType = mmeBuilder.typeConverter.typeCache.get(nasmsg.toLowerCase().hashCode());
                if(mmeType==null){
                    printWARNING("no type of "+ nasmsg+ " in mme CFABuilder");
                }else if(ueType == null)
                    printWARNING("no type of "+ nasmsg+ " in ue CFABuilder");
                else{
                    String messageName=nasmsg.replace("_msg","");
                    generateTranslationFunction(ueType,mmeType,messageName,true,fileWriter);
                    generateTranslationFunction(ueType,mmeType,messageName,false,fileWriter);
                }
            }
            CType ueType = ueBuilder.typeConverter.typeCache.get("emm_msg_header_t".hashCode());
            CType mmeType = mmeBuilder.typeConverter.typeCache.get("emm_msg_header_t".hashCode());
            generateTranslationFunction(ueType,mmeType,"Header",true,fileWriter);
            fileWriter.flush();
            generateTranslationFunction(ueType,mmeType,"Header",false,fileWriter);
            fileWriter.flush();

            fileWriter.close();
        }catch (Exception e){
            e.printStackTrace();
        }


    }

    public static void generateTranslationFunction(CType ueType, CType mmeType, String messagename, boolean direction, FileWriter writer)throws IOException {
        String functionName = "translate_"+(direction?"UL":"DL")+"_"+messagename;
        writer.write("void "+functionName+"(){\n");

        String targetMSG= "\t\t"+(direction?cn_channel_msg_cache:ue_channel_msg_cache)+"->nas_message.nas_message.plain.emm";
        String sourceMSG= (direction?ue_channel_msg_cache:cn_channel_msg_cache)+"->nas_message.nas_message.plain.emm";

        CCompositeType ueCompositeType = (CCompositeType) getRealCompositeType(ueType);
        CCompositeType mmeCompositeType = (CCompositeType)getRealCompositeType(mmeType);
        CCompositeType sourceType = direction?ueCompositeType:mmeCompositeType;
        CCompositeType targetType = direction?mmeCompositeType:ueCompositeType;

        translationEachItem(targetType,targetMSG, sourceType,sourceMSG,"."+messagename.toLowerCase(),writer);

        writer.write("}\n");
        writer.flush();
    }


    public static void translationEachItem(CType leftType, String leftPrefix, CType rightType,  String rightPrefix, String membername, FileWriter fileWriter)throws IOException{

        String left=leftPrefix+membername;
        String right=rightPrefix+membername;
        if(leftType.getCanonicalType() instanceof CSimpleType || leftType.getCanonicalType() instanceof CEnumType){
            fileWriter.write(left+"=("+leftType.toString().trim()+")"+right+";\n");
        }else if(leftType instanceof CArrayType){
            CType lType = ((CArrayType) leftType).getType();
            CType rType = ((CArrayType) rightType).getType();
            int length = ((CIntegerLiteralExpression)((CArrayType) leftType).getLength()).getValue().intValue();
            for(int j=0;j<length;j++){
                translationEachItem(lType,left,rType,right,"["+j+"]",fileWriter);
            }
        }else if(leftType instanceof CPointerType){
            left=rightPrefix+"->"+membername.replace(".","");
            right=leftPrefix+"->"+membername.replace(".","");
            fileWriter.write(left+"=("+leftType.toString().trim()+")"+right+";//Pointer Type check it\n");
        }else {
            CCompositeType tType = (CCompositeType)getRealCompositeType(leftType);
            CCompositeType sType = (CCompositeType)getRealCompositeType(rightType);
            if(tType==null ||sType==null){
                printWARNING("This is not a composite type: "+leftType.toString());
                fileWriter.write(left+"=("+leftType.toString().trim()+")("+rightType.toString().trim()+")"+right+";//Not composite Type\n");
                return;
            }

            for(int i=0;i<tType.getMembers().size();i++){
                CCompositeType.CCompositeTypeMemberDeclaration memberDecTType = tType.getMembers().get(i);
                CCompositeType.CCompositeTypeMemberDeclaration memberDecSType = null;
                if(sType.getMembers().size()>i){
                    //memberDecSType= sType.getMembers().get(i);
                    if(sType.getMembers().get(i).getName().equals(memberDecTType.getName())){
                        memberDecSType= sType.getMembers().get(i);
                    }else {
                        for(int j=0;j<sType.getMembers().size();j++){
                            if(sType.getMembers().get(j).getName().equals(memberDecTType.getName())){
                                memberDecSType= sType.getMembers().get(j);
                                break;
                            }
                        }
                    }
                }
                if(memberDecSType==null){
                    if(sType.getMembers().size()>i){

                        String lleft =left +"."+memberDecTType.getName();

//                        if(memberDecTType.getType()instanceof CArrayType){
//                            int length = ((CIntegerLiteralExpression)((CArrayType) memberDecTType.getType()).getLength()).getValue().intValue();
//                            CType lType = ((CArrayType)  memberDecTType.getType()).getType();
//                            for(int j=0;j<length;j++){
//                                memberDecSType= sType.getMembers().get(i+j);
//                                String rright =right +"."+memberDecSType.getName();
//                                fileWriter.write(lleft+"["+j+"]"+"=("+lType.toString().trim()+")"+rright+";//CArray Type\n");
//                            }
//                        }else {
                            memberDecSType= sType.getMembers().get(i);
                            String rright =right +"."+memberDecSType.getName();
                            fileWriter.write(lleft+"=("+memberDecTType.getType().toString().trim()+")"+rright+";//No corresponding Type\n");
//                        }
                    }else
                        fileWriter.write(left+"."+memberDecTType.getName()+"=("+memberDecTType.getType().toString().trim()+");//No corresponding Type\n");
                }else {
                    String member = "."+memberDecTType.getName();
                    translationEachItem(memberDecTType.getType(),left, memberDecSType.getType(),right,member,fileWriter);
                }
            }
        }
    }
}
