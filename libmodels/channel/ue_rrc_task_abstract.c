SRB_INFO     *srb_info_p;
protocol_ctxt_t  ctxt;
instance_t instance =1;
uint8_t ue_mod_id = 0;

void rrc_ue_task_RRC_MAC_IN_SYNC_IND(uint8_t enb_index){
    UE_rrc_inst[ue_mod_id].Info[enb_index].N310_cnt = 0;

    if (UE_rrc_inst[ue_mod_id].Info[enb_index].T310_active == 1) {
      UE_rrc_inst[ue_mod_id].Info[enb_index].N311_cnt++;
    }
}

void rrc_ue_task_RRC_MAC_IN_SYNC_IND(uint8_t enb_index){
    UE_rrc_inst[ue_mod_id].Info[enb_index].N310_cnt ++;
}

void rrc_ue_task_RRC_MAC_BCCH_DATA_IND(uint8_t enb_index, long rsrq, double rsrp){
    ctxt->module_id = ue_mod_id;
    ctxt->enb_flag  = false;
    ctxt->rnti      = NOT_A_RNTI;
    ctxt->frame     = 0;
    ctxt->subframe  = 0;
    ctxt->eNB_index  = enb_index;
    ctxt->instance  = ue_mod_id+1;
    decode_BCCH_DLSCH_Message (&ctxt,
                               enb_index,
                               NULL,
                               0,
                               rsrq,
                               rsrp);
}
void rrc_ue_task_RRC_MAC_CCCH_DATA_CNF(uint8_t enb_index){
    UE_rrc_inst[ue_mod_id].Srb0[enb_index].Tx_buffer.payload_size = 0;
}

void rrc_ue_task_RRC_MAC_CCCH_DATA_CNF(uint8_t enb_index){
    UE_rrc_inst[ue_mod_id].Srb0[enb_index].Tx_buffer.payload_size = 0;
}

void rrc_ue_task_RRC_MAC_CCCH_DATA_IND(uint8_t enb_index,rnti_t rnti){
    srb_info_p = &UE_rrc_inst[ue_mod_id].Srb0[enb_index];

    //      PROTOCOL_CTXT_SET_BY_INSTANCE(&ctxt, instance, false, RRC_MAC_CCCH_DATA_IND (msg_p).rnti, RRC_MAC_CCCH_DATA_IND (msg_p).frame, 0);
    ctxt->module_id = ue_mod_id;
    ctxt->enb_flag  = false;
    ctxt->rnti      = rnti;
    ctxt->frame     = 0;
    ctxt->subframe  = 0;
    ctxt->eNB_index  = enb_index;
    ctxt->instance  = instance;

    rrc_ue_decode_ccch (&ctxt,
                        srb_info_p,
                        enb_index);
}


void rrc_ue_task_RRC_MAC_MCCH_DATA_IND(uint8_t enb_index,rnti_t rnti){
    ctxt->module_id = ue_mod_id;
    ctxt->enb_flag  = false;
    ctxt->rnti      = M_RNTI;
    ctxt->frame     = 0;
    ctxt->subframe  = 0;
    ctxt->eNB_index  = enb_index;
    ctxt->instance  = instance;

    decode_MCCH_Message (
      &ctxt,
      enb_index,
      NULL,
      0,
      NULL);
}


void rrc_ue_task_RRC_DCCH_DATA_IND(uint8_t enb_index,rnti_t rnti){
    ctxt->module_id = ue_mod_id;
    ctxt->enb_flag  = false;
    ctxt->rnti      = rnti;
    ctxt->frame     = 0;
    ctxt->subframe  = 0;
    ctxt->eNB_index  = enb_index;
    ctxt->instance  = instance;
    rrc_ue_decode_dcch (
      &ctxt,
      0,
      NULL,
      eNB_index);
}

void rrc_ue_task_NAS_KENB_REFRESH_REQ(Byte_t kenb[]){
    memcpy((void *)UE_rrc_inst[ue_mod_id].kenb, (void *)kenb, sizeof(UE_rrc_inst[ue_mod_id].kenb));
}


void rrc_ue_task_NAS_CELL_SELECTION_REQ(plmn_t plmnID, Byte_t rat){
    if (rrc_get_state(ue_mod_id) == RRC_STATE_INACTIVE) {
      openair_rrc_ue_init(ue_mod_id,0);
    }

    /* Save cell selection criterion */
    {
      UE_rrc_inst[ue_mod_id].plmnID = plmnID;
      UE_rrc_inst[ue_mod_id].rat = rat;
    }

    switch (rrc_get_state(ue_mod_id)) {
      case RRC_STATE_INACTIVE: {
        rrc_set_state (ue_mod_id, RRC_STATE_IDLE);
        /* Fall through to next case */
      }

      case RRC_STATE_IDLE: {
        /* Ask to layer 1 to find a cell matching the criterion */
        MessageDef *message_p;
        message_p = itti_alloc_new_message(TASK_RRC_UE, PHY_FIND_CELL_REQ);
        message_p->ittiMsg.phy_find_cell_req.earfcn_start = 1;
        message_p->ittiMsg.phy_find_cell_req.earfcn_end = 1;
        itti_send_msg_to_task(TASK_PHY_UE, UE_MODULE_ID_TO_INSTANCE(ue_mod_id), message_p);
        rrc_set_sub_state (ue_mod_id, RRC_SUB_STATE_IDLE_SEARCHING);
        break;
      }

      case RRC_STATE_CONNECTED:
        /* should not happen */
        break;

      default:
        break;
    }
}


void rrc_ue_task_NAS_CONN_ESTABLI_REQ(){
    ctxt->module_id = ue_mod_id;
    ctxt->enb_flag  = false;
    ctxt->rnti      = NOT_A_RNTI;
    ctxt->frame     = 0;
    ctxt->subframe  = 0;
    ctxt->eNB_index  = 0;
    ctxt->instance  = instance;

    switch (rrc_get_state(ue_mod_id)) {
      case RRC_STATE_IDLE: {
        if (rrc_get_sub_state(ue_mod_id) == RRC_SUB_STATE_IDLE_SIB_COMPLETE) {
          rrc_ue_generate_RRCConnectionRequest(&ctxt, 0);
          rrc_set_sub_state (ue_mod_id, RRC_SUB_STATE_IDLE_CONNECTING);
        }
        break;
      }

      default:
        break;
    }
}

void rrc_ue_task_NAS_UPLINK_DATA_REQ(){
    uint32_t length;
    uint8_t *buffer;
    length = do_ULInformationTransfer(&buffer, 0, NULL);
    /* Transfer data to PDCP */
    ctxt->module_id = ue_mod_id;
    ctxt->enb_flag  = false;
    ctxt->rnti      = UE_rrc_inst[ue_mod_id].Info[0].rnti;
    ctxt->frame     = 0;
    ctxt->subframe  = 0;
    ctxt->eNB_index  = 0;
    ctxt->instance  = instance;

    // check if SRB2 is created, if yes request data_req on DCCH1 (SRB2)
    //RRC_DCCH_DATA_REQ
    MessagesIds messageID = 34;
    ULMessageDeliver(messageID);
}

