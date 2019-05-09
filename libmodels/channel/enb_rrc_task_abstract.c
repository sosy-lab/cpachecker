SRB_INFO     *srb_info_p;
protocol_ctxt_t  ctxt;
instance_t instance =1;
int CC_id;
void rrc_enb_task_RRC_MAC_CCCH_DATA_IND(rnti_t rnti, int CC_id){
    ctxt->module_id = ue_mod_id;
    ctxt->enb_flag  = true;
    ctxt->rnti      = rnti;
    ctxt->frame     = 0;
    ctxt->subframe  = 0;
    ctxt->instance  = 1;
    rrc_eNB_decode_ccch(&ctxt, NULL, CC_id);
}

void rrc_enb_task_RRC_MAC_CCCH_DATA_IND(rnti_t rnti){
    ctxt->module_id = ue_mod_id;
    ctxt->enb_flag  = true;
    ctxt->rnti      = rnti;
    ctxt->frame     = 0;
    ctxt->subframe  = 0;
    ctxt->instance  = 1;
    rrc_eNB_decode_dcch(&ctxt,0,NULL,0);
}

void rrc_enb_task_S1AP_DOWNLINK_NAS(){
    char * msg_name_p = "S1AP_DOWNLINK_NAS";
    rrc_eNB_process_S1AP_DOWNLINK_NAS(NULL, msg_name_p, instance, &rrc_eNB_mui);
}
