struct ixgbe_stats {
  char stat_string[32U];
  struct {
    int sizeof_stat;
    int stat_offset;
    int base_stat_offset;
    int saved_reset_offset;
  };
};
static struct ixgbe_stats const ixgbe_gstrings_stats[15U] = { [ 0U ] = { . stat_string = "rx_packets" , { . sizeof_stat = 8 , . stat_offset = 1376 , . base_stat_offset = 1296 , . saved_reset_offset = 1416 } } , [ 1U ] = { . stat_string = "tx_packets" , { . sizeof_stat = 8 , . stat_offset = 1384 , . base_stat_offset = 1304 , . saved_reset_offset = 1424 } } , [ 2U ] = { . stat_string = "rx_bytes" , { . sizeof_stat = 8 , . stat_offset = 1392 , . base_stat_offset = 1312 , . saved_reset_offset = 1432 } } , [ 3U ] = { . stat_string = "tx_bytes" , { . sizeof_stat = 8 , . stat_offset = 1400 , . base_stat_offset = 1320 , . saved_reset_offset = 1440 } } , [ 4U ] = { . stat_string = "tx_busy" , { . sizeof_stat = 8 , . stat_offset = 1464 , . base_stat_offset = -1 , . saved_reset_offset = -1 } } , [ 5U ] = { . stat_string = "tx_restart_queue" , { . sizeof_stat = 8 , . stat_offset = 816 , . base_stat_offset = -1 , . saved_reset_offset = -1 } } , [ 6U ] = { . stat_string = "tx_timeout_count" , { . sizeof_stat = 4 , . stat_offset = 824 , . base_stat_offset = -1 , . saved_reset_offset = -1 } } , [ 7U ] = { . stat_string = "multicast" , { . sizeof_stat = 8 , . stat_offset = 1408 , . base_stat_offset = 1328 , . saved_reset_offset = 1448 } } , [ 8U ] = { . stat_string = "rx_csum_offload_errors" , { . sizeof_stat = 8 , . stat_offset = 896 , . base_stat_offset = -1 , . saved_reset_offset = -1 } } , [ 9U ] = { . stat_string = "rx_bp_poll_yield" , { . sizeof_stat = 8 , . stat_offset = 1480 , . base_stat_offset = -1 , . saved_reset_offset = -1 } } , [ 10U ] = { . stat_string = "rx_bp_cleaned" , { . sizeof_stat = 8 , . stat_offset = 1488 , . base_stat_offset = -1 , . saved_reset_offset = -1 } } , [ 11U ] = { . stat_string = "rx_bp_misses" , { . sizeof_stat = 8 , . stat_offset = 1496 , . base_stat_offset = -1 , . saved_reset_offset = -1 } } , [ 12U ] = { . stat_string = "tx_bp_napi_yield" , { . sizeof_stat = 8 , . stat_offset = 1504 , . base_stat_offset = -1 , . saved_reset_offset = -1 } } , [ 13U ] = { . stat_string = "tx_bp_cleaned" , { . sizeof_stat = 8 , . stat_offset = 1512 , . base_stat_offset = -1 , . saved_reset_offset = -1 } } , [ 14U ] = { . stat_string = "tx_bp_misses" , { . sizeof_stat = 8 , . stat_offset = 1520 , . base_stat_offset = -1 , . saved_reset_offset = -1 } } };

int main() {
	if (ixgbe_gstrings_stats[0].stat_string[0] != 'r') {
ERROR:
		return 1;
	}
	return 0;
}
