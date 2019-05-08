//need to change msg id and message type
//req(NAS->AS)--> ind(AS->NAS)
//rsp(NAS->AS) --> cnf(AS->NAS)
//this function is used to translate message between UE and CN and deliver to eNB
void ULMessageDeliver(MessagesIds messageID){
    if(UE_channel_message_cache!=NULL){
        switch(messageID){
            case 34://RRC_DCCH_DATA_REQ
                CN_channel_message_cache->rrc_message.message.msgID = 35;
                CN_channel_message_cache->rrc_message.message.ul_dcch_msg = (LTE_UL_DCCH_Message_t)UE_channel_message_cache->rrc_message.message.ul_dcch_msg;

                uint16_t nasMSGID =  UE_channel_message_cache->nas_message.msgID;
                switch(nasMSGID){
                    case 47://NAS_KENB_REFRESH_REQ = 47, no as message

                    break;
                    case 48://NAS_CELL_SELECTION_REQ = 48, as msg id = AS_CELL_INFO_REQ

                    break;
                    case 49://NAS_CONN_ESTABLI_REQ = 49, as msg id = AS_NAS_ESTABLISH_REQ
                        CN_channel_message_cache->nas_message.msgID = 27; //NAS_INITIAL_UE_MESSAGE
                        //first: translate nas_establish_req_t to nas_establish_ind_t (AS message)
                        CN_channel_message_cache->nas_message.as_message.msgID = 27;
                        CN_channel_message_cache->nas_message.as_message.msg.nas_establish_ind.ue_id =0;
                        CN_channel_message_cache->nas_message.as_message.msg.nas_establish_ind.tai.plmn = (plmn_t)UE_channel_message_cache->nas_message.as_message.msg.nas_establish_req.plmnID;
                        CN_channel_message_cache->nas_message.as_message.msg.nas_establish_ind.tai.tac = (tac_t) 1;//it is assigned by enb, in which tac is configuration by loaded config
                        CN_channel_message_cache->nas_message.as_message.msg.nas_establish_ind.ecgi.plmn = (plmn_t)UE_channel_message_cache->nas_message.as_message.msg.nas_establish_req.plmnID;
                        CN_channel_message_cache->nas_message.as_message.msg.nas_establish_ind.ecgi.cell_identity.enb_id = 0;
                        CN_channel_message_cache->nas_message.as_message.msg.nas_establish_ind.ecgi.cell_identity.cell_id = 0;
                        CN_channel_message_cache->nas_message.as_message.msg.nas_establish_ind.s_tmsi.mme_code = (mme_code_t)UE_channel_message_cache->nas_message.as_message.msg.nas_establish_req.s_tmsi.MMEcode;
                        CN_channel_message_cache->nas_message.as_message.msg.nas_establish_ind.s_tmsi.m_tmsi = (tmsi_t)UE_channel_message_cache->nas_message.as_message.msg.nas_establish_req.s_tmsi.m_tmsi;
                        CN_channel_message_cache->nas_message.as_message.msg.nas_establish_ind.as_cause = (as_cause_t)UE_channel_message_cache->nas_message.as_message.msg.nas_establish_req.as_cause;
                        //translate the emm and then esm message

                    break;
                    case 50://NAS_UPLINK_DATA_REQ = 50, as msg id = AS_UL_INFO_TRANSFER_REQ
                    break;
                    default:break;
                }
                rrc_eNB_decode_dcch(NULL,NULL, 0);//deliver message to eNB through rrc_eNB_decode_dcch
                break;
            case 18://RRC_MAC_CCCH_DATA_REQ, no nas message
                CN_channel_message_cache->rrc_message.message.msgID = 20;
                CN_channel_message_cache->rrc_message.message.ul_ccch_msg = (LTE_UL_CCCH_Message_t)UE_channel_message_cache->rrc_message.message.ul_ccch_msg;
                rrc_eNB_decode_ccch(NULL,NULL, 0);//deliver message to eNB through rrc_eNB_decode_ccch
                break;
            default:break;
        }
    }
}

void DLMessageDeliver(MessagesIds messageID){
    if(CN_channel_message_cache!=NULL){
        switch(messageID){
            case 34://RRC_DCCH_DATA_REQ
                UE_channel_message_cache->rrc_message.message.msgID = 35;//RRC_DCCH_DATA_IND
                UE_channel_message_cache->rrc_message.message.dl_dcch_msg = (LTE_DL_DCCH_Message_t)CN_channel_message_cache->rrc_message.message.dl_dcch_msg;
                uint16_t nasMSGID =  CN_channel_message_cache->nas_message.msgID;
                switch(nasMSGID){
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


                break;
            case 18://RRC_MAC_CCCH_DATA_REQ, no nas message
                UE_channel_message_cache->rrc_message.message.msgID = 20;//RRC_MAC_CCCH_DATA_IND
                UE_channel_message_cache->rrc_message.message.dl_ccch_msg = (LTE_DL_CCCH_Message_t)CN_channel_message_cache->rrc_message.message.dl_ccch_msg;
                break;
            case 21: //RRC_MAC_MCCH_DATA_REQ
//                UE_channel_message_cache->rrc_message.message.msgID = 22;//RRC_MAC_MCCH_DATA_IND
//                UE_channel_message_cache->rrc_message.message.dl_ccch_msg = (LTE_DL_CCCH_Message_t)CN_channel_message_cache->rrc_message.message.dl_ccch_msg;
                break;
            case 16://RRC_MAC_BCCH_DATA_REQ
                UE_channel_message_cache->rrc_message.message.msgID = 17;//RRC_MAC_BCCH_DATA_IND
                UE_channel_message_cache->rrc_message.message.bcch_bch_msg = (LTE_BCCH_BCH_Message_t)CN_channel_message_cache->rrc_message.message.bcch_bch_msg;
                break;
        }
    }
}



void ulNASEMMMessageTranslation(){
    uint8_t msgType = UE_channel_message_cache->nas_message.nas_message.plain.emm.header.message_type;
    switch(msgType){
        case 65://ATTACH_REQUEST

        break;
        case 67://ATTACH_COMPLETE
        case 69://DETACH_REQUEST, ue-sided detach request
        case 70://DETACH_ACCEPT, network-sided detach request
        case 72://TRACKING_AREA_UPDATE_REQUEST
        case 74://TRACKING_AREA_UPDATE_COMPLETE
        case 76://EXTENDED_SERVICE_REQUEST
        case 77://SERVICE_REQUEST
        case 81://GUTI_REALLOCATION_COMPLETE
        case 83://AUTHENTICATION_RESPONSE
        case 92://AUTHENTICATION_FAILURE
        case 86://IDENTITY_RESPONSE
        case 94://SECURITY_MODE_COMPLETE
        case 95://SECURITY_MODE_REJECT
        case 96://EMM_STATUS, both
        case 99://UPLINK_NAS_TRANSPORT


    }
}

void dlNASEMMMessageTranslation(){
    uint8_t msgType = CN_channel_message_cache->nas_message.nas_message.plain.emm.header.message_type;
    switch(msgType){
        case 66://ATTACH_ACCEPT

        case 68://ATTACH_REJECT
        case 69://DETACH_REQUEST, network-sided detach request
        case 70://DETACH_ACCEPT, ue-sided detach request
        case 73://TRACKING_AREA_UPDATE_ACCEPT
        case 75://TRACKING_AREA_UPDATE_REJECT
        case 78://SERVICE_REJECT
        case 88://GUTI_REALLOCATION_COMMAND
        case 82://AUTHENTICATION_REQUEST
        case 84://AUTHENTICATION_REJECT
        case 85://IDENTITY_REQUEST
        case 93://SECURITY_MODE_COMMAND
        case 95://SECURITY_MODE_REJECT
        case 96://EMM_STATUS, both
        case 97://EMM_INFORMATION
        case 98://DOWNLINK_NAS_TRANSPORT
        case 100://CS_SERVICE_NOTIFICATION
    }
}



asn_dec_rval_t uper_decode(const asn_codec_ctx_t *opt_codec_ctx,
            const asn_TYPE_descriptor_t *td, void **sptr, const void *buffer,
            size_t size, int skip_bits, int unused_bits) {


}

asn_enc_rval_t
uper_encode_to_buffer(const asn_TYPE_descriptor_t *td,
                      const asn_per_constraints_t *constraints,
                      const void *sptr, void *buffer, size_t buffer_size) {

}