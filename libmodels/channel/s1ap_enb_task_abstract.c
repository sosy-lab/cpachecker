
//TODO: replace s1ap_eNB_itti_send_sctp_data_req with s1ap_mme_itti_s1ap_initial_ue_message in s1ap_eNB_handle_nas_first_req of s1ap_eNB_nas_procedure.c
//TODO: replace s1ap_eNB_itti_send_sctp_data_req with s1ap_mme_itti_nas_uplink_ind in s1ap_eNB_nas_uplink of s1ap_eNB_nas_procedure.c
//TODO: replace s1ap_eNB_itti_send_sctp_data_req with s1ap_mme_itti_nas_non_delivery_ind in s1ap_eNB_nas_non_delivery_ind of s1ap_eNB_nas_procedure.c

//void s1ap_mme_itti_s1ap_initial_ue_message(
//  const sctp_assoc_id_t   assoc_id, ---mme_desc_p->assoc_id
//  const uint32_t          enb_id,  --instance_p->eNB_id
//  const enb_ue_s1ap_id_t  enb_ue_s1ap_id, -- ue_desc_p->eNB_ue_s1ap_id
//  const mme_ue_s1ap_id_t  mme_ue_s1ap_id, -- 0
//  const uint8_t * const   nas_msg,  --- NULL,
//  const size_t            nas_msg_length, ---100
//  const tai_t      *const tai, ----{instance_p->tac,instance_p->mcc[ue_desc_p->selected_plmn_identity], instance_p->mnc[ue_desc_p->selected_plmn_identity],instance_p->mnc_digit_length[ue_desc_p->selected_plmn_identity]}
//  const ecgi_t     *const ecgi, -- {instance_p->eNB_id, 0}
//  const long              rrc_cause,  --s1ap_nas_first_req_p->establishment_cause
//  const s_tmsi_t   *const opt_s_tmsi, NULL
//  const csg_id_t   *const opt_csg_id, NULL
//  const gummei_t   *const opt_gummei, NULL
//  const void       *const opt_cell_access_mode,  // unused
//  const void       *const opt_cell_gw_transport_address,  // unused
//  const void       *const opt_relay_node_indicator)  // unused

void s1ap_eNB_handle_nas_first_req(){
    uint16_t mcc = (uint16_t)instance_p->mcc[ue_desc_p->selected_plmn_identity];
    uint16_t mnc = (uint16_t)instance_p->mnc[ue_desc_p->selected_plmn_identity];
    uint8_t length = (uint16_t)instance_p->mnc_digit_length[ue_desc_p->selected_plmn_identity];
    uint8_t mnc3 = mnc/100;
    if(length==2){
        mnc3 = 15;
    }
    plmn_t plmn = {mcc/10%10,mcc/100, mnc3, mcc%10, mnc%10, mnc/10%10};
    tai_t tai = {plmn, (uint16_t)instance_p->tac};
    eci_t cell_identity={(uint32_t)instance_p->eNB_id, 0, 0};
    ecgi_t ecgi =  {plmn, cell_identity};

    MME_s1ap_mme_itti_s1ap_initial_ue_message(
    (sctp_assoc_id_t)mme_desc_p->assoc_id,
    (uint32_t)instance_p->eNB_id,
    (enb_ue_s1ap_id_t)ue_desc_p->eNB_ue_s1ap_id,
    (mme_ue_s1ap_id_t)ue_desc_p->mme_ue_s1ap_id,
    NULL,
    0,
    &tai,
    &ecgi,
    (long)s1ap_nas_first_req_p->establishment_cause,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL);
}

//int
//s1ap_mme_itti_nas_uplink_ind (
//  const mme_ue_s1ap_id_t  ue_id,  --ue_context_p->ue_initial_id
//  STOLEN_REF bstring *payload,  --- NULL
//  const tai_t      *const tai,  ----{s1ap_eNB_instance_p->tac
//                                        s1ap_eNB_instance_p->mcc[ue_context_p->selected_plmn_identity],
//                                                s1ap_eNB_instance_p->mnc[ue_context_p->selected_plmn_identity],
//                                                s1ap_eNB_instance_p->mnc_digit_length[ue_context_p->selected_plmn_identity]}
//  const ecgi_t     *const cgi)   --- {s1ap_eNB_instance_p->mcc[ue_context_p->selected_plmn_identity],
//                                                                                  s1ap_eNB_instance_p->mnc[ue_context_p->selected_plmn_identity],
//                                                                                  s1ap_eNB_instance_p->mnc_digit_length[ue_context_p->selected_plmn_identity]}
//



void s1ap_eNB_nas_uplink(){
    uint16_t mcc = (uint16_t)s1ap_eNB_instance_p->mcc[ue_context_p->selected_plmn_identity];
    uint16_t mnc = (uint16_t)s1ap_eNB_instance_p->mnc[ue_context_p->selected_plmn_identity];
    uint8_t length = (uint16_t)s1ap_eNB_instance_p->mnc_digit_length[ue_context_p->selected_plmn_identity];
    uint8_t mnc3 = mnc/100;
    if(length==2){
        mnc3 = 15;
    }
    plmn_t plmn = {mcc/10%10,mcc/100, mnc3, mcc%10, mnc%10,mnc/10%10};
    tai_t tai = {plmn, (uint16_t)s1ap_eNB_instance_p->tac};
    eci_t cell_identity={(uint32_t)s1ap_eNB_instance_p->eNB_id, 0, 0};
    ecgi_t ecgi =  {plmn, cell_identity};
    MME_s1ap_mme_itti_nas_uplink_ind(
        (mme_ue_s1ap_id_t)ue_context_p->ue_initial_id,
        NULL,
        &tai,
        &ecgi);
}


//  void s1ap_mme_itti_nas_non_delivery_ind(
//      const mme_ue_s1ap_id_t ue_id,  --ue_context_p->ue_initial_id
//      uint8_t * const nas_msg, NULL
//      const size_t nas_msg_length, 0
//      const S1ap_Cause_t * const cause) NULL//dummy cause

//void s1ap_eNB_S1AP_NAS_FIRST_REQ(tac_t tac, uint16_t enb_id, uint8_t cell_id){//instance_p->tac,instance_p->eNB_id, 0, // Cell ID
//
////    CN_channel_message_cache->nas_message.msgID = 27; //NAS_INITIAL_UE_MESSAGE
////    //first: translate nas_establish_req_t to nas_establish_ind_t (AS message)
////    CN_channel_message_cache->nas_message.as_message.msgID = 27;
////    CN_channel_message_cache->nas_message.as_message.msg.nas_establish_ind.ue_id =message->ittiMsg.s1ap_nas_first_req.ue_initial_id;
////    CN_channel_message_cache->nas_message.as_message.msg.nas_establish_ind.tai.plmn = (plmn_t)UE_channel_message_cache->nas_message.as_message.msg.nas_establish_req.plmnID;
////    CN_channel_message_cache->nas_message.as_message.msg.nas_establish_ind.tai.tac = tac;
////    CN_channel_message_cache->nas_message.as_message.msg.nas_establish_ind.ecgi.plmn = (plmn_t)UE_channel_message_cache->nas_message.as_message.msg.nas_establish_req.plmnID;
////    CN_channel_message_cache->nas_message.as_message.msg.nas_establish_ind.ecgi.cell_identity.enb_id = enb_id;//
////    CN_channel_message_cache->nas_message.as_message.msg.nas_establish_ind.ecgi.cell_identity.cell_id = cell_id;//
////    CN_channel_message_cache->nas_message.as_message.msg.nas_establish_ind.s_tmsi.mme_code = (mme_code_t)UE_channel_message_cache->nas_message.as_message.msg.nas_establish_req.s_tmsi.MMEcode;
////    CN_channel_message_cache->nas_message.as_message.msg.nas_establish_ind.s_tmsi.m_tmsi = (tmsi_t)UE_channel_message_cache->nas_message.as_message.msg.nas_establish_req.s_tmsi.m_tmsi;
////    CN_channel_message_cache->nas_message.as_message.msg.nas_establish_ind.as_cause = (as_cause_t)UE_channel_message_cache->nas_message.as_message.msg.nas_establish_req.as_cause;
//
//    s1ap_mme_itti_s1ap_initial_ue_message();
//    //nas_mme_NAS_INITIAL_UE_MESSAGE(CN_channel_message_cache->nas_message.as_message.msg.nas_establish_ind);
//}



void s1ap_eNB_nas_non_delivery_ind(){
    MME_s1ap_mme_itti_nas_non_delivery_ind(
        (mme_ue_s1ap_id_t)ue_context_p->ue_initial_id,
        NULL,
        0,
        NULL)
}

