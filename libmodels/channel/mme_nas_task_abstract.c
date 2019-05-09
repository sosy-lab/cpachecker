
void nas_mme_NAS_INITIAL_UE_MESSAGE(nas_establish_ind_t nas_est_ind_p){
    nas_proc_establish_ind (nas_est_ind_p->ue_id,
                  nas_est_ind_p->tai,
                  nas_est_ind_p->ecgi,
                  nas_est_ind_p->as_cause,
                  NULL);
}

void nas_mme_MME_APP_ACTIVATE_EPS_BEARER_CTX_REQ(){
    nas_proc_activate_dedicated_bearer(NULL);
}
void nas_mme_MME_APP_MODIFY_EPS_BEARER_CTX_REQ(){
    nas_proc_modify_eps_bearer_ctx(NULL);
}
void nas_mme_MME_APP_DEACTIVATE_EPS_BEARER_CTX_REQ(){
    nas_proc_deactivate_dedicated_bearer(NULL);
}
void nas_mme_MME_APP_UPDATE_ESM_BEARER_CTXS_REQ(){
    nas_proc_establish_bearer_update(NULL);
}

void nas_mme_NAS_DOWNLINK_DATA_CNF(int ue_id, int err_code){
    nas_proc_dl_transfer_cnf (ue_id, err_code, NULL);
}

void nas_mme_NAS_UPLINK_DATA_IND(int ue_id, tai_t tai, ecgi_t cgi){
    nas_proc_ul_transfer_ind (ue_id, tai, cgi, NULL);
}

void nas_mme_NAS_UPLINK_DATA_REJ(int ue_id, int err_code){
    nas_proc_dl_transfer_rej(ue_id, err_code, NULL);
}

void nas_mme_NAS_PDN_CONFIG_RSP(){
    nas_proc_pdn_config_res(NULL);
}

void nas_mme_NAS_PDN_CONFIG_FAIL(){
    nas_proc_pdn_connectivity_fail(NULL);
}

void nas_mme_NAS_PDN_CONNECTIVITY_FAIL(){
    nas_proc_pdn_config_res(NULL);
}

void nas_mme_NAS_PDN_CONNECTIVITY_RSP(){
    nas_proc_pdn_connectivity_res(NULL);
}

void nas_mme_NAS_PDN_DISCONNECT_RSP(){
    nas_proc_pdn_disconnect_res(NULL);
}


void nas_mme_NAS_IMPLICIT_DETACH_UE_IND(int ue_id, uint8_t emm_cause, uint8_t detach_type){
    nas_proc_implicit_detach_ue_ind(ue_id, emm_cause, detach_type);
}

void nas_mme_S1AP_DEREGISTER_UE_REQ(int mme_ue_s1ap_id){
    nas_proc_deregister_ue(mme_ue_s1ap_id);
}


void nas_mme_S6A_AUTH_INFO_ANS(){
    nas_proc_authentication_info_answer(NULL);
}

void nas_mme_NAS_CONTEXT_RES(){
    nas_proc_context_res(NULL);
}

void nas_mme_NAS_CONTEXT_FAIL(int ue_id, gtpv2c_cause_value_t cause){
    nas_proc_context_fail(ue_id, cause);
}

void nas_mme_NAS_SIGNALLING_CONNECTION_REL_IND(int ue_id){
    nas_proc_signalling_connection_rel_ind(ue_id);
}