

void rrc_enb_task_RRC_MAC_CCCH_DATA_IND(uint8_t ue_mod_id, rnti_t rnti,uint32_t frame, instance_t instance){
    ULCCCHRRCMessageDeliver();
    protocol_ctxt_t  ctxt;
    ctxt->module_id = ue_mod_id;
    ctxt->enb_flag  = true;
    ctxt->rnti      = rnti;
    ctxt->frame     = frame;
    ctxt->subframe  = 0;
    ctxt->instance  = instance;
    ENB_rrc_eNB_decode_ccch(&ctxt, NULL, 0);
}

void rrc_enb_task_RRC_DCCH_DATA_IND(uint8_t ue_mod_id, rnti_t rnti, uint32_t frame, instance_t instance){
    ULDCCHRRCMessageDeliver();
    protocol_ctxt_t  ctxt;
    ctxt->module_id = ue_mod_id;
    ctxt->enb_flag  = true;
    ctxt->rnti      = rnti;
    ctxt->frame     = frame;
    ctxt->subframe  = 0;
    ctxt->instance  = instance;
    ENB_rrc_eNB_decode_dcch(&ctxt,0,NULL,0);
}
//no use now
void rrc_enb_task_S1AP_DOWNLINK_NAS(uint16_t ue_initial_id, rnti_t rnti, uint32_t eNB_ue_s1ap_id, instance_t instance, mui_t *rrc_eNB_mui){

      uint8_t srb_id;
      protocol_ctxt_t  ctxt;
      ctxt->instance = instance;
      ctxt->enb_flag  = true;
      ctxt->rnti      = rnti;
      ctxt->frame     = 0;
      ctxt->subframe  = 0;
      ctxt->instance  = 1;
      struct rrc_eNB_ue_context_s *ue_context_p = NULL;
      ue_context_p = ENB_rrc_eNB_get_ue_context_from_s1ap_ids(instance, ue_initial_id, eNB_ue_s1ap_id);
      srb_id = ue_context_p->ue_context.Srb2.Srb_info.Srb_id;
      if (ue_context_p->ue_context.eNB_ue_s1ap_id == 0) {
            ue_context_p->ue_context.eNB_ue_s1ap_id = eNB_ue_s1ap_id;
       }

      uint8_t transactionID;
      transactionID = ENB_rrc_eNB_get_next_transaction_identifier(instance);
      ENB_do_DLInformationTransfer(
                 instance,
                 NULL,
                 transactionID,
                 0,
                 NULL);

      ENB_rrc_data_req(
        &ctxt,
        srb_id,
        *rrc_eNB_mui++,
        false,//SDU_CONFIRM_NO,
        0,
        NULL,
        1);//PDCP_TRANSMISSION_MODE_CONTROL);

}

//no use now
void rrc_enb_task_S1AP_UE_CONTEXT_RELEASE_COMMAND(uint32_t eNB_ue_s1ap_id,instance_t instance, rnti_t rnti){
    struct rrc_eNB_ue_context_s *ue_context_p = NULL;
    ue_context_p = ENB_rrc_eNB_get_ue_context_from_s1ap_ids(instance, 0, eNB_ue_s1ap_id);//UE_INITIAL_ID_INVALID
    ue_context_p->ue_context.ue_release_timer_s1 = 0;
    protocol_ctxt_t  ctxt;
    ctxt->instance = instance;
    ctxt->enb_flag  = true;
    ctxt->rnti      = rnti;
    ctxt->frame     = 0;
    ctxt->subframe  = 0;
    ctxt->instance  = 1;
    ENB_rrc_eNB_generate_RRCConnectionRelease(&ctxt, ue_context_p);
}

