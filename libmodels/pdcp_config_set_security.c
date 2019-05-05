
//activate security
//set ciphering algorithm
//set integrity algorithm
//set user-plane encryption key
//set control-plane RRC encryption key
//set control-plane RRC integrity key
void pdcp_config_set_security(){}

void pushPlainNASESMIntoCache(ESM_msg msg){
	if(channelMSGCache==NULL)
		channelMSGCache=(channel_message_t *)malloc(sizeof(channel_message_t));
	channelMSGCache->esm_msg = msg;
}