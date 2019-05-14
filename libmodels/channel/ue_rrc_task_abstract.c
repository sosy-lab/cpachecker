
void rrc_ue_task_RRC_MAC_IN_SYNC_IND(uint8_t ue_mod_id, uint8_t enb_index){
    UE_rrc_inst[ue_mod_id].Info[enb_index].N310_cnt = 0;

    if (UE_rrc_inst[ue_mod_id].Info[enb_index].T310_active == 1) {
      UE_rrc_inst[ue_mod_id].Info[enb_index].N311_cnt++;
    }
}

void rrc_ue_task_RRC_MAC_IN_SYNC_IND(uint8_t ue_mod_id, uint8_t enb_index){
    UE_rrc_inst[ue_mod_id].Info[enb_index].N310_cnt ++;
}

void rrc_ue_task_RRC_MAC_BCCH_DATA_IND(uint8_t ue_mod_id, instance_t instance, uint8_t enb_index,uint32_t frame,  long rsrq, double rsrp){
    //uint8_t ue_mod_id = 0;
    DLBCCHRRCMessageDeliver();
    protocol_ctxt_t  ctxt;
    ctxt->module_id = ue_mod_id;
    ctxt->enb_flag  = false;
    ctxt->rnti      = (rnti_t)0;//NOT_A_RNTI;
    ctxt->frame     = frame;
    ctxt->subframe  = 0;
    ctxt->eNB_index  = enb_index;
    ctxt->instance  = instance;//ue_mod_id+1;
    UE_decode_BCCH_DLSCH_Message (&ctxt,
                               enb_index,
                               NULL,
                               0,
                               rsrq,
                               rsrp);
}
void rrc_ue_task_RRC_MAC_CCCH_DATA_CNF(uint8_t ue_mod_id, uint8_t enb_index){
    UE_rrc_inst[ue_mod_id].Srb0[enb_index].Tx_buffer.payload_size = 0;
}

void rrc_ue_task_RRC_MAC_CCCH_DATA_CNF(uint8_t ue_mod_id, uint8_t enb_index){
    UE_rrc_inst[ue_mod_id].Srb0[enb_index].Tx_buffer.payload_size = 0;
}

void rrc_ue_task_RRC_MAC_CCCH_DATA_IND(uint8_t ue_mod_id, instance_t instance, uint8_t enb_index,uint32_t frame, rnti_t rnti){
    DLCCCHRRCMessageDeliver();
    SRB_INFO *srb_info_p;
    srb_info_p = &UE_rrc_inst[ue_mod_id].Srb0[enb_index];
    protocol_ctxt_t  ctxt;
    ctxt->module_id = ue_mod_id;
    ctxt->enb_flag  = false;
    ctxt->rnti      = rnti;
    ctxt->frame     = frame;
    ctxt->subframe  = 0;
    ctxt->eNB_index  = enb_index;
    ctxt->instance  = instance;

    UE_rrc_ue_decode_ccch (&ctxt,
                        srb_info_p,
                        enb_index);
}


void rrc_ue_task_RRC_MAC_MCCH_DATA_IND(uint8_t ue_mod_id, instance_t instance, uint8_t enb_index, uint32_t frame, uint8_t mbsfn_sync_area){
    DLMCCHRRCMessageDeliver();
    protocol_ctxt_t  ctxt;
    ctxt->module_id = ue_mod_id;
    ctxt->enb_flag  = false;
    ctxt->rnti      = (rnti_t)65533;//M_RNTI;
    ctxt->frame     = frame;
    ctxt->subframe  = 0;
    ctxt->eNB_index  = enb_index;
    ctxt->instance  = instance;

    UE_decode_MCCH_Message (
      &ctxt,
      enb_index,
      NULL,
      0,
      mbsfn_sync_area);
}


void rrc_ue_task_RRC_DCCH_DATA_IND(uint8_t ue_mod_id, instance_t instance, uint8_t enb_index, rnti_t rnti, uint32_t frame){
    DLDCCHRRCMessageDeliver();
    protocol_ctxt_t  ctxt;
    ctxt->module_id = ue_mod_id;
    ctxt->enb_flag  = false;
    ctxt->rnti      = rnti;
    ctxt->frame     = frame;
    ctxt->subframe  = 0;
    ctxt->eNB_index  = enb_index;
    ctxt->instance  = instance;
    UE_rrc_ue_decode_dcch (
      &ctxt,
      0,
      NULL,
      enb_index);
}

//void rrc_ue_task_NAS_KENB_REFRESH_REQ(uint8_t ue_mod_id, Byte_t kenb[]){
//    UE_memcpy((void *)UE_rrc_inst[ue_mod_id].kenb, (void *)kenb, sizeof(UE_rrc_inst[ue_mod_id].kenb));
//}


void rrc_ue_task_NAS_CELL_SELECTION_REQ(uint8_t ue_mod_id, instance_t instance, plmn_t plmnID, Byte_t rat){
    Rrc_State_t rrc_state = UE_rrc_inst[ue_mod_id].RrcState;

    if (rrc_state == 0) {//RRC_STATE_INACTIVE
      UE_openair_rrc_ue_init(ue_mod_id,0);
    }

    /* Save cell selection criterion */
    UE_rrc_inst[ue_mod_id].plmnID = plmnID;
    UE_rrc_inst[ue_mod_id].rat = rat;


    switch (rrc_state) {
      case 0: {//RRC_STATE_INACTIVE
        UE_rrc_set_state (ue_mod_id, (Rrc_State_t)1);//RRC_STATE_IDLE);
        /* Fall through to next case */
      }

      case 1: {//RRC_STATE_IDLE
        /* Ask to layer 1 to find a cell matching the criterion */
//        MessageDef *message_p;
//        message_p = UE_itti_alloc_new_message(TASK_RRC_UE, PHY_FIND_CELL_REQ);
//        message_p->ittiMsg.phy_find_cell_req.earfcn_start = 1;
//        message_p->ittiMsg.phy_find_cell_req.earfcn_end = 1;
//        UE_itti_send_msg_to_task(TASK_PHY_UE, UE_MODULE_ID_TO_INSTANCE(ue_mod_id), message_p);
//        UE_rrc_set_sub_state (ue_mod_id, (Rrc_Sub_State_t)1);//RRC_SUB_STATE_IDLE_SEARCHING
        break;
      }

      default:
        break;
    }
}


void rrc_ue_task_NAS_CONN_ESTABLI_REQ(uint8_t ue_mod_id,instance_t instance){
    protocol_ctxt_t  ctxt;
    ctxt->module_id = ue_mod_id;
    ctxt->enb_flag  = false;
    ctxt->rnti      = (rnti_t)0;//NOT_A_RNTI;
    ctxt->frame     = 0;
    ctxt->subframe  = 0;
    ctxt->eNB_index  = 0;
    ctxt->instance  = instance;

    Rrc_State_t rrc_state = UE_rrc_inst[ue_mod_id].RrcState;

    switch (rrc_state) {
      case 1: {//RRC_STATE_IDLE
        Rrc_Sub_State_t subrrcState = UE_rrc_inst[ue_mod_id].RrcSubState;
        if (subrrcState == 3) {//RRC_SUB_STATE_IDLE_SIB_COMPLETE
          UE_rrc_ue_generate_RRCConnectionRequest(&ctxt, 0);
          UE_rrc_set_sub_state(ue_mod_id, (Rrc_Sub_State_t)4);//RRC_SUB_STATE_IDLE_CONNECTING
        }
        break;
      }

      default:
        break;
    }
}

void rrc_ue_task_NAS_UPLINK_DATA_REQ(uint8_t ue_mod_id, instance_t instance){
    uint32_t length;
    uint8_t *buffer;
    length = UE_do_ULInformationTransfer(&buffer, 0, NULL);
    /* Transfer data to PDCP */
    protocol_ctxt_t  ctxt;
    ctxt->module_id = ue_mod_id;
    ctxt->enb_flag  = false;
    ctxt->rnti      = UE_rrc_inst[ue_mod_id].Info[0].rnti;
    ctxt->frame     = 0;
    ctxt->subframe  = 0;
    ctxt->eNB_index  = 0;
    ctxt->instance  = instance;

    // check if SRB2 is created, if yes request data_req on DCCH1 (SRB2)
    //RRC_DCCH_DATA_REQ
    DLDCCHRRCMessageDeliver();
}

