package org.nulist.plugin.model.action;

import org.nulist.plugin.parser.CFGHandleExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;

import static org.nulist.plugin.parser.CFGParser.*;

/**
 * @ClassName ITTIAbstract
 * @Description TODO
 * @Author Yinbo Yu
 * @Date 4/12/19 3:59 PM
 * @Version 1.0
 **/
public class ITTIAbstract {
    public final static String extendSuffix = "_abstract";

    public final static int TASK_RRC_UE = 20;//rrc_ue_task
    public final static String TASK_RRC_UE_FUNC = "rrc_ue_task";//rrc_ue_task
    public final static int TASK_PDCP_UE = 19;//action out to eNB's action in node
    public final static int TASK_NAS_UE = 21;//nas_ue_task
    public final static String TASK_NAS_UE_FUNC = "nas_ue_task";//nas_ue_task

    public final static int TASK_RRC_ENB = 9;//rrc_enb_task
    public final static String TASK_RRC_ENB_FUNC = "rrc_enb_task";//rrc_enb_task-->rrc_enb_process_itti_msg;
    public final static String TASK_RRC_ENB_FUNC_CALL = "rrc_enb_process_itti_msg";
    public final static int TASK_S1AP_ENB = 11;//s1ap_enb_task, action out to MME's action in node
    public final static int TASK_X2AP = 12;//x2ap_task: action out to other eNB's action in node
    public final static int TASK_PDCP_ENB = 8;//action out to UE's action in node

    public final static int TASK_NAS_MME = 104;//
    public final static String TASK_NAS_MME_FUNC = "nas_intertask_interface";//
    public final static int TASK_S1AP_MME = 107;//slap_mme_thread, action out to eNB's action in node
    public final static int TASK_MME_APP = 103;//
    public final static String TASK_MME_APP_FUNC = "mme_app_thread";//
    public final static int TASK_S10 = 105;//
    public final static int TASK_S11 = 106;//
    public final static int TASK_S6A = 108;//


    public static CFunctionDeclaration itti_send_to_task(int taskID, String Component, CFGHandleExpression handleExpression){
        CFunctionDeclaration functionDeclaration = null;

        if(Component.equals(MME))
            taskID+=100;

        switch (taskID){
            case TASK_RRC_UE:
                functionDeclaration = (CFunctionDeclaration) handleExpression.globalDeclarations.get((TASK_RRC_UE_FUNC+extendSuffix).hashCode());
                break;
            case TASK_NAS_UE:
                functionDeclaration = (CFunctionDeclaration) handleExpression.globalDeclarations.get((TASK_NAS_UE_FUNC+extendSuffix).hashCode());
                break;
            case TASK_PDCP_UE:
                //send out to ENB
                break;
            case TASK_RRC_ENB:
                functionDeclaration = (CFunctionDeclaration) handleExpression.globalDeclarations.get((TASK_RRC_ENB_FUNC+extendSuffix).hashCode());
                break;
            case TASK_PDCP_ENB:
                //send out to UE
                break;
            case TASK_S1AP_ENB:
                //send out to MME
                break;
            case TASK_X2AP:
                break;

            case TASK_S1AP_MME:
                break;

            case TASK_NAS_MME:
                functionDeclaration = (CFunctionDeclaration) handleExpression.globalDeclarations.get((TASK_NAS_MME_FUNC+extendSuffix).hashCode());
                break;

            case TASK_MME_APP:
                functionDeclaration = (CFunctionDeclaration) handleExpression.globalDeclarations.get((TASK_MME_APP_FUNC+extendSuffix).hashCode());
                break;
            default:
                //do nothing
                break;
        }

        return functionDeclaration;
    }

    public static boolean isITTITaskProcessFunction(String name){
        return name.equals(TASK_RRC_ENB_FUNC)||
                name.equals(TASK_RRC_ENB_FUNC_CALL)||
                name.equals(TASK_RRC_UE_FUNC)||
                name.equals(TASK_NAS_MME_FUNC)||
                name.equals(TASK_NAS_UE_FUNC) ||
                name.equals(TASK_MME_APP_FUNC);
    }


    public static boolean isITTIUENASTaskProcessFunction(String name){
        return name.equals(TASK_NAS_UE_FUNC+extendSuffix) ||
                name.equals("nas_ue_user_initialize");
    }


}
