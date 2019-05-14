
typedef struct nas_channel_message_s{
    uint16_t msgID;
    nas_message_t nas_message;
    as_message_t as_message;
}nas_channel_message_t;

typedef struct rrc_channel_message_s{
    uint16_t msgID;
    union rrc_message_t{
        LTE_UL_CCCH_Message_t ul_ccch_msg;
        LTE_UL_DCCH_Message_t ul_dcch_msg;
        LTE_DL_CCCH_Message_t dl_ccch_msg;
        LTE_DL_DCCH_Message_t dl_dcch_msg;
        LTE_BCCH_BCH_Message_t bcch_bch_msg;//MIB
        LTE_BCCH_DL_SCH_Message_t bcch_dl_sch_msg;//SIB
        LTE_PCCH_Message_t pcch_msg;
    }message;
}rrc_channel_message_t;


typedef struct {
  nas_channel_message_t nas_message;//from UE
  rrc_channel_message_t rrc_message;//from UE
} ue_channel_message_t;


typedef struct {
  nas_channel_message_t nas_message;//from MME
  rrc_channel_message_t rrc_message;//from eNB
} cn_channel_message_t;
