//TODO: replace s1ap_mme_itti_send_sctp_request with s1ap_eNB_itti_send_nas_downlink_ind in s1ap_generate_downlink_nas_transport of s1ap_mme_nas_procedure.c

//void s1ap_eNB_itti_send_nas_downlink_ind(instance_t instance,
//    uint16_t ue_initial_id,   --ue_id
//    uint32_t eNB_ue_s1ap_id,   -enb_ue_s1ap_id
//    uint8_t *nas_pdu,  -- NULL
//    uint32_t nas_pdu_length)  --0

void s1ap_generate_downlink_nas_transport(){
    ENB_s1ap_eNB_itti_send_nas_downlink_ind(
        (uint16_t)ue_id,
        (uint32_t)enb_ue_s1ap_id,
        NULL,
        0);
}