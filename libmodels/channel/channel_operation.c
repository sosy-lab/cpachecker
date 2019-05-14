//need to change msg id and message type
//req(NAS->AS)--> ind(AS->NAS)
//rsp(NAS->AS) --> cnf(AS->NAS)
//this function is used to translate message between UE and CN and deliver to eNB


void ULNASMessageDeliver(MessagesIds messageID){
    switch(messageID){
        case 47://NAS_KENB_REFRESH_REQ = 47, no as message
        break;
        case 48://NAS_CELL_SELECTION_REQ = 48, as msg id = AS_CELL_INFO_REQ
        break;
        case 49://NAS_CONN_ESTABLI_REQ = 49, as msg id = AS_NAS_ESTABLISH_REQ
            CN_channel_message_cache->nas_message.msgID = 27; //NAS_INITIAL_UE_MESSAGE
            //first: translate nas_establish_req_t to nas_establish_ind_t (AS message)
            CN_channel_message_cache->nas_message.as_message.msg_id = 27;
            CN_channel_message_cache->nas_message.as_message.msg.nas_establish_ind.ue_id =0;
            CN_channel_message_cache->nas_message.as_message.msg.nas_establish_ind.tai.plmn = (plmn_t)UE_channel_message_cache->nas_message.as_message.msg.nas_establish_req.plmnID;
            CN_channel_message_cache->nas_message.as_message.msg.nas_establish_ind.tai.tac = (tac_t) 1;//it is assigned by enb, in which tac is configuration by loaded config
            CN_channel_message_cache->nas_message.as_message.msg.nas_establish_ind.ecgi.plmn = (plmn_t)UE_channel_message_cache->nas_message.as_message.msg.nas_establish_req.plmnID;
            CN_channel_message_cache->nas_message.as_message.msg.nas_establish_ind.ecgi.cell_identity.enb_id = 0;
            CN_channel_message_cache->nas_message.as_message.msg.nas_establish_ind.ecgi.cell_identity.cell_id = 0;
            CN_channel_message_cache->nas_message.as_message.msg.nas_establish_ind.s_tmsi.mme_code = (mme_code_t)UE_channel_message_cache->nas_message.as_message.msg.nas_establish_req.s_tmsi.MMEcode;
            CN_channel_message_cache->nas_message.as_message.msg.nas_establish_ind.s_tmsi.m_tmsi = (tmsi_t)UE_channel_message_cache->nas_message.as_message.msg.nas_establish_req.s_tmsi.m_tmsi;
            CN_channel_message_cache->nas_message.as_message.msg.nas_establish_ind.as_cause = (as_cause_t)UE_channel_message_cache->nas_message.as_message.msg.nas_establish_req.cause;
            //nas_proc_establish_ind
        break;
        case 50://NAS_UPLINK_DATA_REQ = 50, as msg id = AS_UL_INFO_TRANSFER_REQ
            CN_channel_message_cache->nas_message.msgID = 30;//NAS_UPLINK_DATA_IND = 30
            CN_channel_message_cache->nas_message.as_message.msg.ul_info_transfer_ind.ue_id =UE_channel_message_cache->nas_message.as_message.msg.ul_info_transfer_req.UEid;
            //nas_proc_ul_transfer_ind
        break;
        default:break;
    }
}

void DLNASMessageDeliver(MessagesIds messageID){
    switch(messageID){
        case 31://NAS_DOWNLINK_DATA_REQ = 31, as msg id =
        break;
        case 39://NAS_PDN_CONFIG_REQ = 39, as msg id =
        break;
        case 26://NAS_PDN_CONNECTIVITY_REQ = 26, as msg id =
        break;
        case 45://NAS_PDN_DISCONNECT_REQ = 45, as msg id =
        break;
        case 37://NAS_AUTHENTICATION_PARAM_REQ = 37, as msg id =
        break;
        case 28://NAS_CONNECTION_ESTABLISHMENT_CNF = 28
        break;
        case 38://NAS_DETACH_REQ = 38
        break;
        default:break;
    }
}

void ULNASEMMMessageTranslation(){
    uint8_t msgType = UE_channel_message_cache->nas_message.nas_message.plain.emm.header.message_type;
    translate_UL_Header();
    switch(msgType){
        case 65://ATTACH_REQUEST
            translate_UL_ATTACH_REQUEST();break;
        case 67://ATTACH_COMPLETE
            translate_UL_ATTACH_COMPLETE();break;
        case 69://DETACH_REQUEST, ue-sided detach request
            translate_UL_DETACH_REQUEST();break;
        case 70://DETACH_ACCEPT, network-sided detach request
            translate_UL_DETACH_ACCEPT();break;
        case 72://TRACKING_AREA_UPDATE_REQUEST
            translate_UL_TRACKING_AREA_UPDATE_REQUEST();break;
        case 74://TRACKING_AREA_UPDATE_COMPLETE
            translate_UL_TRACKING_AREA_UPDATE_COMPLETE();break;
        case 76://EXTENDED_SERVICE_REQUEST
            translate_UL_EXTENDED_SERVICE_REQUEST();break;
        case 77://SERVICE_REQUEST
            translate_UL_SERVICE_REQUEST();break;
        case 81://GUTI_REALLOCATION_COMPLETE
            translate_UL_GUTI_REALLOCATION_COMPLETE();break;
        case 83://AUTHENTICATION_RESPONSE
            translate_UL_AUTHENTICATION_RESPONSE();break;
        case 92://AUTHENTICATION_FAILURE
            translate_UL_AUTHENTICATION_FAILURE();break;
        case 86://IDENTITY_RESPONSE
            translate_UL_IDENTITY_RESPONSE();break;
        case 94://SECURITY_MODE_COMPLETE
            translate_UL_SECURITY_MODE_COMPLETE();break;
        case 95://SECURITY_MODE_REJECT
            translate_UL_SECURITY_MODE_REJECT();break;
        case 96://EMM_STATUS, both
            translate_UL_EMM_STATUS();break;
        case 99://UPLINK_NAS_TRANSPORT
            translate_UL_UPLINK_NAS_TRANSPORT();break;
        default:break;
    }
}

void DLNASEMMMessageTranslation(){
    uint8_t msgType = CN_channel_message_cache->nas_message.nas_message.plain.emm.header.message_type;
    translate_DL_Header();
    switch(msgType){
        case 66://ATTACH_ACCEPT
            translate_DL_ATTACH_ACCEPT();
            break;
        case 68://ATTACH_REJECT
            translate_DL_ATTACH_REJECT();
            break;
        case 69://DETACH_REQUEST, network-sided detach request
            translate_DL_DETACH_REQUEST();
            break;
        case 70://DETACH_ACCEPT, ue-sided detach request
            translate_DL_DETACH_ACCEPT();
            break;
        case 73://TRACKING_AREA_UPDATE_ACCEPT
            translate_DL_TRACKING_AREA_UPDATE_ACCEPT();break;
        case 75://TRACKING_AREA_UPDATE_REJECT
            translate_DL_TRACKING_AREA_UPDATE_REJECT();break;
        case 78://SERVICE_REJECT
            translate_DL_SERVICE_REJECT();break;
        case 88://GUTI_REALLOCATION_COMMAND
            translate_DL_GUTI_REALLOCATION_COMMAND();break;
        case 82://AUTHENTICATION_REQUEST
            translate_DL_AUTHENTICATION_REQUEST();break;
        case 84://AUTHENTICATION_REJECT
            translate_DL_AUTHENTICATION_REJECT();break;
        case 85://IDENTITY_REQUEST
            translate_DL_IDENTITY_REQUEST();break;
        case 93://SECURITY_MODE_COMMAND
            translate_DL_SECURITY_MODE_COMMAND();break;
        case 96://EMM_STATUS, both
            translate_DL_EMM_STATUS();break;
        case 97://EMM_INFORMATION
            translate_DL_EMM_INFORMATION();break;
        case 98://DOWNLINK_NAS_TRANSPORT
            translate_DL_DOWNLINK_NAS_TRANSPORT();break;
        case 100://CS_SERVICE_NOTIFICATION
            translate_DL_CS_SERVICE_NOTIFICATION();break;
        default:break;
    }
}

void ULDCCHRRCMessageDeliver(){
    if(UE_channel_message_cache!=NULL){
        CN_channel_message_cache->rrc_message.msgID = 35;//RRC_DCCH_DATA_IND
        CN_channel_message_cache->rrc_message.message.ul_dcch_msg = (LTE_UL_DCCH_Message_t)UE_channel_message_cache->rrc_message.message.ul_dcch_msg;
        ULNASEMMMessageTranslation();
    }
}

void ULCCCHRRCMessageDeliver(){
    if(UE_channel_message_cache!=NULL){
        CN_channel_message_cache->rrc_message.msgID = 20;//RRC_MAC_CCCH_DATA_IND
        CN_channel_message_cache->rrc_message.message.ul_ccch_msg = (LTE_UL_CCCH_Message_t)UE_channel_message_cache->rrc_message.message.ul_ccch_msg;
    }
}

void DLDCCHRRCMessageDeliver(){
    if(CN_channel_message_cache!=NULL){
        UE_channel_message_cache->rrc_message.msgID = 35;//RRC_DCCH_DATA_IND
        UE_channel_message_cache->rrc_message.message.dl_dcch_msg = (LTE_DL_DCCH_Message_t)CN_channel_message_cache->rrc_message.message.dl_dcch_msg;
        DLNASEMMMessageTranslation();
    }
}

void DLCCCHRRCMessageDeliver(){
    if(CN_channel_message_cache!=NULL){
        UE_channel_message_cache->rrc_message.msgID = 20;//RRC_MAC_CCCH_DATA_IND
        UE_channel_message_cache->rrc_message.message.dl_ccch_msg = (LTE_DL_CCCH_Message_t)CN_channel_message_cache->rrc_message.message.dl_ccch_msg;
    }
}

void DLMCCHRRCMessageDeliver(){
    if(CN_channel_message_cache!=NULL){
        UE_channel_message_cache->rrc_message.msgID = 22;//RRC_MAC_MCCH_DATA_IND
        UE_channel_message_cache->rrc_message.message.dl_ccch_msg = (LTE_DL_CCCH_Message_t)CN_channel_message_cache->rrc_message.message.dl_ccch_msg;
    }
}

void DLBCCHRRCMessageDeliver(){
    if(CN_channel_message_cache!=NULL){
        UE_channel_message_cache->rrc_message.msgID = 17;//RRC_MAC_BCCH_DATA_IND
        UE_channel_message_cache->rrc_message.message.bcch_bch_msg = (LTE_BCCH_BCH_Message_t)CN_channel_message_cache->rrc_message.message.bcch_bch_msg;
    }
}