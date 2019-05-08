ue_channel_message_t *UE_channel_message_cache= NULL;


void ueChannelMessageInit(){
    UE_channel_message_cache=(ue_channel_message_t *)malloc(sizeof(ue_channel_message_t));
}

void uePushNASMSGIDIntoCache(uint16_t msgID){
    UE_channel_message_cache->nas_message.msgID = msgID;
}

void uePushPlainNASEMMMsgIntoCache(EMM_msg msg){
	UE_channel_message_cache->nas_message.nas_message.plain.emm=msg;
}


void uePushPlainNASESMMsgIntoCache(ESM_msg msg){
	UE_channel_message_cache->nas_message.nas_message.plain.esm=msg;
}


void uePushPlainASMsgIntoCache(as_message_t msg){
	UE_channel_message_cache->nas_message.as_message=msg;
}

EMM_msg uePullPlainNASEMMMsgFromCache(){
	EMM_msg msg = UE_channel_message_cache->nas_message.nas_message.plain.emm;
	return msg;
}

ESM_msg uePullPlainNASESMMsgFromCache(){
	ESM_msg msg = UE_channel_message_cache->nas_message.nas_message.plain.esm;
	return msg;
}

as_message_t uePullPlainASMsgFromCache(){
	as_message_t msg = UE_channel_message_cache->nas_message.as_message;
	return msg;
}


void uePushPlainULCCCHMessageIntoCache(LTE_UL_CCCH_Message_t msg){
	UE_channel_message_cache->rrc_message.message.ul_ccch_msg = msg;
}

void uePushPlainDLCCCHMessageIntoCache(LTE_DL_CCCH_Message_t msg){
	UE_channel_message_cache->rrc_message.message.dl_ccch_msg = msg;
}

void uePushPlainULDCCHMessageIntoCache(LTE_UL_DCCH_Message_t msg){
	UE_channel_message_cache->rrc_message.message.ul_dcch_msg = msg;
}

void uePushPlainDLDCCHMessageIntoCache(LTE_DL_DCCH_Message_t msg){
	UE_channel_message_cache->rrc_message.message.dl_dcch_msg = msg;
}

void uePushPlainBCCHBCHMessageIntoCache(LTE_BCCH_BCH_Message_t msg){
	UE_channel_message_cache->rrc_message.message.bcch_bch_msg = msg;
}

void uePushPlainBCCHDLSCHMessageIntoCache(LTE_BCCH_DL_SCH_Message_t msg){
	UE_channel_message_cache->rrc_message.message.bcch_dl_sch_msg = msg;
}

void uePushPlainPCCHMessageIntoCache(LTE_PCCH_Message_t msg){
	UE_channel_message_cache->rrc_message.message.pcch_msg = msg;
}

LTE_UL_CCCH_Message_t uePullPlainULCCCHMessageIntoCache(){
	LTE_UL_CCCH_Message_t msg = UE_channel_message_cache->rrc_message.message.ul_ccch_msg;
	return msg;
}

LTE_DL_CCCH_Message_t uePullPlainDLCCCHMessageIntoCache(){
	LTE_DL_CCCH_Message_t msg = UE_channel_message_cache->rrc_message.message.dl_ccch_msg;
	return msg;
}

LTE_UL_DCCH_Message_t uePullPlainULDCCHMessageIntoCache(){
	return UE_channel_message_cache->rrc_message.message.ul_dcch_msg;
}

LTE_DL_DCCH_Message_t uePullPlainDLDCCHMessageIntoCache(){
	return UE_channel_message_cache->rrc_message.message.dl_dcch_msg;
}

LTE_BCCH_BCH_Message_t uePullPlainBCCHBCHMessageIntoCache(){
	LTE_BCCH_BCH_Message_t msg = UE_channel_message_cache->rrc_message.message.bcch_bch_msg;
	return msg;
}

LTE_BCCH_DL_SCH_Message_t uePullPlainBCCHDLSCHMessageIntoCache(){
	LTE_BCCH_DL_SCH_Message_t msg = UE_channel_message_cache->rrc_message.message.bcch_dl_sch_msg;
	return msg;
}

LTE_PCCH_Message_t uePullPlainPCCHMessageIntoCache(){
	LTE_PCCH_Message_t msg = UE_channel_message_cache->rrc_message.pcch_msg;
	return msg;
}
