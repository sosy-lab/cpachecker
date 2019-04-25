
int N= 100;
double log= -30.8; //-10*log(12*N)
double rssi=-20;
double get_RSRP(){
    return -20+log;
}


long get_RSRQ(){
    return 100*((long)get_RSRP()/rssi);
}