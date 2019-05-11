

void rrc_enb_task_RRC_MAC_CCCH_DATA_IND(rnti_t rnti,uint32_t frame, instance_t instance){
    ULCCCHRRCMessageDeliver();
    protocol_ctxt_t  ctxt;
    ctxt->module_id = ue_mod_id;
    ctxt->enb_flag  = true;
    ctxt->rnti      = rnti;
    ctxt->frame     = frame;
    ctxt->subframe  = 0;
    ctxt->instance  = instance;
    rrc_eNB_decode_ccch(&ctxt, NULL, 0);
}

void rrc_enb_task_RRC_DCCH_DATA_IND(uint8_t ue_mod_id, rnti_t rnti, uint32_t frame, instance){
    ULDCCHRRCMessageDeliver();
    protocol_ctxt_t  ctxt;
    ctxt->module_id = ue_mod_id;
    ctxt->enb_flag  = true;
    ctxt->rnti      = rnti;
    ctxt->frame     = frame;
    ctxt->subframe  = 0;
    ctxt->instance  = instance;
    rrc_eNB_decode_dcch(&ctxt,0,NULL,0);
}

void rrc_enb_task_S1AP_DOWNLINK_NAS(uint16_t ue_initial_id, uint32_t eNB_ue_s1ap_id,instance_t instance, mui_t *rrc_eNB_mui){

      uint8_t srb_id;

      ctxt->instance = instance;
      ctxt->enb_flag  = true;
      ctxt->rnti      = rnti;
      ctxt->frame     = 0;
      ctxt->subframe  = 0;
      ctxt->instance  = 1;
      struct rrc_eNB_ue_context_s *ue_context_p = NULL;
      ue_context_p = rrc_eNB_get_ue_context_from_s1ap_ids(instance, ue_initial_id, eNB_ue_s1ap_id);
      srb_id = ue_context_p->ue_context.Srb2.Srb_info.Srb_id;
      if (ue_context_p->ue_context.eNB_ue_s1ap_id == 0) {
            ue_context_p->ue_context.eNB_ue_s1ap_id = eNB_ue_s1ap_id;
       }

      length = do_DLInformationTransfer (
                 instance,
                 NULL,
                 rrc_eNB_get_next_transaction_identifier (instance),
                 0,
                 NULL);

      rrc_data_req (
        &ctxt,
        srb_id,
        *rrc_eNB_mui++,
        false,//SDU_CONFIRM_NO,
        0,
        NULL,
        1);//PDCP_TRANSMISSION_MODE_CONTROL);

}


void rrc_enb_task_S1AP_UE_CONTEXT_RELEASE_COMMAND(uint32_t eNB_ue_s1ap_id,instance_t instance){
    struct rrc_eNB_ue_context_s *ue_context_p = NULL;
    ue_context_p = rrc_eNB_get_ue_context_from_s1ap_ids(instance, UE_INITIAL_ID_INVALID, eNB_ue_s1ap_id);
    ue_context_p->ue_context.ue_release_timer_s1 = 0;
    ctxt->instance = instance;
    ctxt->enb_flag  = true;
    ctxt->rnti      = rnti;
    ctxt->frame     = 0;
    ctxt->subframe  = 0;
    ctxt->instance  = 1;
    rrc_eNB_generate_RRCConnectionRelease(&ctxt, ue_context_p);
}

