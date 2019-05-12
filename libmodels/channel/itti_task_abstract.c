
int itti_send_msg_to_task_ue(task_id_t destination_task_id, instance_t instance, MessageDef *message) {
    message->ittiMsgHeader.destinationTaskId = destination_task_id;
    message->ittiMsgHeader.instance = instance;
    message->ittiMsgHeader.lte_time.frame = 0;
    message->ittiMsgHeader.lte_time.slot = 0;

    switch (destination_task_id){

        case TASK_TIMER:

        break;
        case TASK_L2L1:

        break;

        case TASK_PHY_UE:
        if(message->ittiMsgHeader.originTaskId==TASK_RRC_UE &&
          (message->ittiMsgHeader.messageId==PHY_FIND_CELL_REQ||
           message->ittiMsgHeader.messageId==PHY_FIND_NEXT_CELL_REQ)){

        }
        break;
        case TASK_MAC_UE:
        if(message->ittiMsgHeader.originTaskId==TASK_RRC_UE &&
          message->ittiMsgHeader.messageId==RRC_MAC_CCCH_DATA_REQ){
          uint32_t frame = (uint32_t)message->ittiMsg.rrc_mac_ccch_data_req.frame;
          uint8_t eNB_index = (uint8_t)message->ittiMsg.rrc_mac_ccch_data_req.enb_index;
          uint8_t ue_mod_id = (uint8_t)instance;
          rnti_t rnti = (rnti_t)UE_rrc_inst[ue_mod_id].Info[eNB_index].rnti;
          rrc_enb_task_RRC_MAC_CCCH_DATA_IND(rnti,frame,instance);
        }
        break;
        case TASK_RLC_UE:

        break;
        case TASK_PDCP_UE:
        if(message->ittiMsgHeader.originTaskId==TASK_RRC_UE &&
          message->ittiMsgHeader.messageId==RRC_DCCH_DATA_REQ){
          uint32_t frame = (uint32_t)message_p->ittiMsg.rrc_dcch_data_req.frame;
          rnti_t rnti = (rnti_t)message_p->ittiMsg.rrc_dcch_data_req.rnti;
          uint8_t eNB_index = (uint8_t)message_p->ittiMsg.rrc_dcch_data_req.eNB_index;
          uint8_t module_id = (uint8_t)message_p->ittiMsg.rrc_dcch_data_req.module_id;
          rrc_enb_task_RRC_DCCH_DATA_IND(module_id, rnti, frame, instance);
        }
        break;
        case TASK_RRC_UE:
            rrc_ue_task_abstract(message);
        break;
        case TASK_NAS_UE:
            nas_ue_task_abstract(message);
            break;
//            if(if(message->ittiMsgHeader.originTaskId==TASK_RRC_UE){
//                nas_user_t *user = &users->item[instance-1];
//                switch(message->ittiMsgHeader.messageId){
//                    case INITIALIZE_MESSAGE:
//                         nas_ue_INITIALIZE_MESSAGE(user);
//                    break;
//                    case NAS_CELL_SELECTION_CNF:
//                        nas_ue_NAS_CELL_SELECTION_CNF(user,
//                            message->ittiMsg.nas_cell_selection_cnf.errCode,
//                            message->ittiMsg.nas_cell_selection_cnf.tac,
//                            message->ittiMsg.nas_cell_selection_cnf.cellID,
//                            message->ittiMsg.nas_cell_selection_cnf.rat,
//                            message->ittiMsg.nas_cell_selection_cnf.rsrq,
//                            message->ittiMsg.nas_cell_selection_cnf.rsrp
//                            );
//                        break;
//                      case NAS_CONN_ESTABLI_CNF:
//                        nas_ue_NAS_CONN_ESTABLI_CNF(user, message->ittiMsg.nas_conn_establi_cnf.errCode);
//                        break;
//
//                      case NAS_CONN_RELEASE_IND:
//                        nas_proc_release_ind (user, message->ittiMsg.nas_conn_release_ind.cause);
//                        break;
//
//                      case NAS_UPLINK_DATA_CNF:
//                        if (message->ittiMsg.nas_ul_data_cnf.errCode == AS_SUCCESS) {
//                          nas_proc_ul_transfer_cnf (user);
//                        } else {
//                          nas_proc_ul_transfer_rej (user);
//                        }
//
//                        break;
//
//                      case NAS_DOWNLINK_DATA_IND:
//                        nas_proc_dl_transfer_ind (user, NULL,0);
//                        break;
//
//                      default:
//                        break;
//                }
//            }else


        default:
        break;
    }
    return 0;
}

int itti_send_msg_to_task_eNB(task_id_t destination_task_id, instance_t instance, MessageDef *message) {
    message->ittiMsgHeader.destinationTaskId = destination_task_id;
    message->ittiMsgHeader.instance = instance;
    message->ittiMsgHeader.lte_time.frame = 0;
    message->ittiMsgHeader.lte_time.slot = 0;

    switch (destination_task_id){

        case TASK_TIMER:

        break;
        case TASK_L2L1:

        break;
        case TASK_BM:

        break;
        case TASK_PHY_ENB:

        break;
        case TASK_MAC_ENB:
        if(message->ittiMsgHeader.originTaskId==TASK_RRC_ENB){

        }
        break;
        case TASK_RLC_ENB:

        break;
        case TASK_PDCP_ENB:
            if(message->ittiMsgHeader.originTaskId==TASK_RRC_ENB){
              if(message->ittiMsgHeader.messageId==RRC_DCCH_DATA_REQ){
                uint32_t frame = (uint32_t)RRC_DCCH_DATA_REQ (message).frame;
                uint8_t mod_id = (uint8_t)RRC_DCCH_DATA_REQ (message).module_id;
                rnti_t rnti = (rnti_t)RRC_DCCH_DATA_REQ (message).rnti;
                uint8_t eNB_index = (uint8_t)RRC_DCCH_DATA_REQ (message).eNB_index;
                rrc_ue_task_RRC_MAC_CCCH_DATA_IND(enb_index, frame, rnti);
              }
            }
            break;
        case TASK_RRC_ENB:
            rrc_enb_task_abstract(message);
            break;
        case TASK_S1AP:
            s1ap_eNB_process_itti_msg_abstract(message);
            //build channel between eNB through s1ap and mme through nas
            break;
        case TASK_X2AP:

        break;
        default:
        break;
    }
    return 0;
}

int itti_send_msg_to_task_mme(task_id_t destination_task_id, instance_t instance, MessageDef *message){
//  task_id_t                               origin_task_id;
//  uint32_t                                priority;
//  message_number_t                        message_number;
//  uint32_t                                message_id;

  message->ittiMsgHeader.destinationTaskId = destination_task_id;
  message->ittiMsgHeader.instance = instance;
  message->ittiMsgHeader.lte_time.time.tv_sec = itti_desc.lte_time.time.tv_sec;
  message->ittiMsgHeader.lte_time.time.tv_usec = itti_desc.lte_time.time.tv_usec;
//  message_id = message->ittiMsgHeader.messageId;
//  origin_task_id = message->ittiMsgHeader.originTaskId;
//
//  message_number = itti_increment_message_number ();
  switch(destination_task_id){
    case TASK_NAS_MME:
        nas_intertask_interface_abstract(message);
    break;

    case TASK_MME_APP:
        mme_app_thread_abstract(message);
    break;

    case TASK_S1AP:
        s1ap_mme_thread_abstract(message);
    break;

    case TASK_S6A:
    break;

    default:
      break;
    }
  return 0;
}
