instance_t            instance =1;
unsigned int          Mod_id = 0;

void nas_ue_INITIALIZE_MESSAGE(nas_user_t *user){
    char *user_data = "at+cfun=1\r";
    nas_user_receive_and_process (user, user_data);
}



void nas_ue_NAS_CELL_SELECTION_CNF(nas_user_t *user, nas_error_code_t errCode, unint16_t tac, unint32_t cellID, Byte_t rat, long rsrq, double rsrp){
    int cell_found = errCode==1;//1:AS_SUCCESS
    nas_proc_cell_info (user, cell_found, tac,
                                  cellID, rat,
                                  rsrq, rsrp);
}

void nas_ue_NAS_CONN_ESTABLI_CNF(nas_user_t *user,nas_error_code_t errCode){
    if ((errCode == 1)|| (errCode == 2)) {//1:AS_SUCCESS, 2:AS_TERMINATED_NAS
         nas_proc_establish_cnf(user, NULL, 0);
    }
}


void nas_ue_NAS_CONN_RELEASE_IND(nas_user_t *user, int cause){
    nas_proc_release_ind(user, cause);
}

void nas_ue_NAS_UPLINK_DATA_CNF(nas_user_t *user,nas_error_code_t errCode){
    if(errCode==1)
        nas_proc_ul_transfer_cnf(user);
    else
        nas_proc_ul_transfer_rej (user);
}

void nas_ue_NAS_DOWNLINK_DATA_IND(nas_user_t *user, int cause){
    nas_proc_dl_transfer_ind(user, NULL, 0);
}