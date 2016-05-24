extern void __VERIFIER_error() __attribute__ ((__noreturn__));
extern int __VERIFIER_nondet_int();
void error(void);
int m_run_st  ;
int m_run_i  ;
int m_run_pc  ;
int s_memory0  ;
int s_run_st  ;
int s_run_i  ;
int s_run_pc  ;
int c_m_lock  ;
int c_m_ev  ;
int c_req_type  ;
int c_req_a  ;
int c_req_d  ;
int c_rsp_type  ;
int c_rsp_status  ;
int c_rsp_d  ;
int c_empty_req  ;
int c_empty_rsp  ;
int c_read_req_ev  ;
int c_write_req_ev  ;
int c_read_rsp_ev  ;
int c_write_rsp_ev  ;
static int d_t  ;
static int a_t  ;
static int req_t_type  ;
static int req_t_a  ;
static int req_t_d  ;
static int rsp_t_type  ;
static int rsp_t_status  ;
static int rsp_t_d  ;
static int req_tt_type  ;
static int req_tt_a  ;
static int req_tt_d  ;
static int rsp_tt_type  ;
static int rsp_tt_status  ;
static int rsp_tt_d  ;
int s_memory_read(int i);
void s_memory_write(int i, int v);
void m_run(void);
static int req_t_type___0  ;
static int req_t_a___0  ;
static int req_t_d___0  ;
static int rsp_t_type___0  ;
static int rsp_t_status___0  ;
static int rsp_t_d___0  ;
void s_run(void);
void eval(void);
void start_simulation(void);
int main(void);
int __return_1689289;
int __return_1689290;
int __return_1705217;
int __return_1705218;
int __return_1800395;
int __return_1800396;
int __return_1814272;
int __return_1814273;
int __return_1845402;
int __return_1845403;
int __return_1706810;
int __return_1706811;
int __return_1846965;
int __return_1846966;
int __return_1861798;
int __return_1861799;
int __return_1950344;
int __return_1847422;
int __return_1847423;
int __return_1861302;
int __return_1861303;
int __return_1798172;
int __return_1798173;
int __return_1798839;
int __return_1798840;
int __return_1815944;
int __return_1815945;
int __return_1933361;
int __return_1933362;
int __return_1941005;
int __return_1941006;
int __return_1832256;
int __return_1832257;
int __return_1950342;
int main()
{
int __retres1 ;
c_m_lock = 0;
c_m_ev = 2;
m_run_i = 1;
m_run_pc = 0;
s_run_i = 1;
s_run_pc = 0;
c_empty_req = 1;
c_empty_rsp = 1;
c_read_req_ev = 2;
c_write_req_ev = 2;
c_read_rsp_ev = 2;
c_write_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 2;
{
int kernel_st ;
kernel_st = 0;
if (((int)m_run_i) == 1)
{
m_run_st = 0;
goto label_1646961;
}
else 
{
m_run_st = 2;
label_1646961:; 
if (((int)s_run_i) == 1)
{
s_run_st = 0;
goto label_1646968;
}
else 
{
s_run_st = 2;
label_1646968:; 
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1647034;
}
else 
{
goto label_1646975;
}
}
else 
{
label_1646975:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1647034;
}
else 
{
goto label_1646982;
}
}
else 
{
label_1646982:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1647034;
}
else 
{
goto label_1646989;
}
}
else 
{
label_1646989:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1647034;
}
else 
{
goto label_1646996;
}
}
else 
{
label_1646996:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1647034;
}
else 
{
goto label_1647003;
}
}
else 
{
label_1647003:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1647012;
}
else 
{
label_1647012:; 
goto label_1647034;
}
}
else 
{
label_1647034:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1647056;
}
else 
{
goto label_1647041;
}
}
else 
{
label_1647041:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1647050;
}
else 
{
label_1647050:; 
goto label_1647056;
}
}
else 
{
label_1647056:; 
label_1647058:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
label_1647068:; 
if (((int)m_run_st) == 0)
{
goto label_1647082;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1647082:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_1647564;
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
if (((int)m_run_pc) == 0)
{
goto label_1647190;
}
else 
{
label_1647190:; 
a = 0;
req_type = 1;
req_a = a;
req_d = a + 50;
if (c_m_lock == 1)
{
m_run_st = 2;
m_run_pc = 1;
req_t_type = req_type;
req_t_a = req_a;
req_t_d = req_d;
rsp_t_type = rsp_type;
rsp_t_status = rsp_status;
rsp_t_d = rsp_d;
d_t = d;
a_t = a;
}
else 
{
c_m_lock = 1;
c_req_type = req_type;
c_req_a = req_a;
c_req_d = req_d;
c_empty_req = 0;
c_write_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1688833;
}
else 
{
goto label_1688774;
}
}
else 
{
label_1688774:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1688833;
}
else 
{
goto label_1688781;
}
}
else 
{
label_1688781:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1688833;
}
else 
{
goto label_1688788;
}
}
else 
{
label_1688788:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1688833;
}
else 
{
goto label_1688795;
}
}
else 
{
label_1688795:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1688833;
}
else 
{
goto label_1688802;
}
}
else 
{
label_1688802:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1688811;
}
else 
{
label_1688811:; 
goto label_1688833;
}
}
else 
{
label_1688833:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1688855;
}
else 
{
goto label_1688840;
}
}
else 
{
label_1688840:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1688849;
}
else 
{
label_1688849:; 
goto label_1688855;
}
}
else 
{
label_1688855:; 
c_write_req_ev = 2;
if (((int)c_empty_rsp) == 1)
{
m_run_st = 2;
m_run_pc = 3;
req_t_type = req_type;
req_t_a = req_a;
req_t_d = req_d;
rsp_t_type = rsp_type;
rsp_t_status = rsp_status;
rsp_t_d = rsp_d;
d_t = d;
a_t = a;
}
else 
{
rsp_type = c_rsp_type;
rsp_status = c_rsp_status;
rsp_d = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1688935;
}
else 
{
goto label_1688876;
}
}
else 
{
label_1688876:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1688935;
}
else 
{
goto label_1688883;
}
}
else 
{
label_1688883:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1688935;
}
else 
{
goto label_1688890;
}
}
else 
{
label_1688890:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1688935;
}
else 
{
goto label_1688897;
}
}
else 
{
label_1688897:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1688935;
}
else 
{
goto label_1688904;
}
}
else 
{
label_1688904:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1688913;
}
else 
{
label_1688913:; 
goto label_1688935;
}
}
else 
{
label_1688935:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1688957;
}
else 
{
goto label_1688942;
}
}
else 
{
label_1688942:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1688951;
}
else 
{
label_1688951:; 
goto label_1688957;
}
}
else 
{
label_1688957:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1689030;
}
else 
{
goto label_1688971;
}
}
else 
{
label_1688971:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1689030;
}
else 
{
goto label_1688978;
}
}
else 
{
label_1688978:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1689030;
}
else 
{
goto label_1688985;
}
}
else 
{
label_1688985:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1689030;
}
else 
{
goto label_1688992;
}
}
else 
{
label_1688992:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1689030;
}
else 
{
goto label_1688999;
}
}
else 
{
label_1688999:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1689008;
}
else 
{
label_1689008:; 
goto label_1689030;
}
}
else 
{
label_1689030:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1689037;
}
}
else 
{
label_1689037:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1689046;
}
else 
{
label_1689046:; 
return 1;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1689558;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
goto label_1689135;
}
else 
{
label_1689135:; 
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1689217;
}
else 
{
goto label_1689158;
}
}
else 
{
label_1689158:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1689217;
}
else 
{
goto label_1689165;
}
}
else 
{
label_1689165:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1689217;
}
else 
{
goto label_1689172;
}
}
else 
{
label_1689172:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1689217;
}
else 
{
goto label_1689179;
}
}
else 
{
label_1689179:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1689217;
}
else 
{
goto label_1689186;
}
}
else 
{
label_1689186:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1689195;
}
else 
{
label_1689195:; 
goto label_1689217;
}
}
else 
{
label_1689217:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1689239;
}
else 
{
goto label_1689224;
}
}
else 
{
label_1689224:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1689233;
}
else 
{
label_1689233:; 
goto label_1689239;
}
}
else 
{
label_1689239:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_1 = req_a;
int i = __tmp_1;
int x;
if (i == 0)
{
x = s_memory0;
 __return_1689289 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_1689290 = x;
}
rsp_d = __return_1689289;
goto label_1689292;
rsp_d = __return_1689290;
label_1689292:; 
rsp_status = 1;
label_1689298:; 
c_rsp_type = rsp_type;
c_rsp_status = rsp_status;
c_rsp_d = rsp_d;
c_empty_rsp = 0;
c_write_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1689379;
}
else 
{
goto label_1689320;
}
}
else 
{
label_1689320:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1689379;
}
else 
{
goto label_1689327;
}
}
else 
{
label_1689327:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1689379;
}
else 
{
goto label_1689334;
}
}
else 
{
label_1689334:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1689379;
}
else 
{
goto label_1689341;
}
}
else 
{
label_1689341:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1689379;
}
else 
{
goto label_1689348;
}
}
else 
{
label_1689348:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1689357;
}
else 
{
label_1689357:; 
goto label_1689379;
}
}
else 
{
label_1689379:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1689401;
}
else 
{
goto label_1689386;
}
}
else 
{
label_1689386:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1689395;
}
else 
{
label_1689395:; 
goto label_1689401;
}
}
else 
{
label_1689401:; 
c_write_rsp_ev = 2;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1689484;
}
else 
{
goto label_1689425;
}
}
else 
{
label_1689425:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1689484;
}
else 
{
goto label_1689432;
}
}
else 
{
label_1689432:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1689484;
}
else 
{
goto label_1689439;
}
}
else 
{
label_1689439:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1689484;
}
else 
{
goto label_1689446;
}
}
else 
{
label_1689446:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1689484;
}
else 
{
goto label_1689453;
}
}
else 
{
label_1689453:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1689462;
}
else 
{
label_1689462:; 
goto label_1689484;
}
}
else 
{
label_1689484:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1689491;
}
}
else 
{
label_1689491:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1689500;
}
else 
{
label_1689500:; 
return 1;
}
}
}
}
}
}
}
}
}
goto label_1797277;
}
}
}
}
}
}
}
}
}
}
else 
{
if (((int)req_type) == 1)
{
{
int __tmp_2 = req_a;
int __tmp_3 = req_d;
int i = __tmp_2;
int v = __tmp_3;
if (i == 0)
{
s_memory0 = v;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_1689267;
label_1689267:; 
rsp_status = 1;
goto label_1689298;
}
}
else 
{
rsp_status = 0;
goto label_1689298;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
else 
{
label_1689558:; 
label_1704993:; 
if (((int)m_run_st) == 0)
{
goto label_1705007;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1705007:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_1705018;
}
else 
{
label_1705018:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1705485;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
goto label_1705063;
}
else 
{
label_1705063:; 
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1705145;
}
else 
{
goto label_1705086;
}
}
else 
{
label_1705086:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1705145;
}
else 
{
goto label_1705093;
}
}
else 
{
label_1705093:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1705145;
}
else 
{
goto label_1705100;
}
}
else 
{
label_1705100:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1705145;
}
else 
{
goto label_1705107;
}
}
else 
{
label_1705107:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1705145;
}
else 
{
goto label_1705114;
}
}
else 
{
label_1705114:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1705123;
}
else 
{
label_1705123:; 
goto label_1705145;
}
}
else 
{
label_1705145:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1705167;
}
else 
{
goto label_1705152;
}
}
else 
{
label_1705152:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1705161;
}
else 
{
label_1705161:; 
goto label_1705167;
}
}
else 
{
label_1705167:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_4 = req_a;
int i = __tmp_4;
int x;
if (i == 0)
{
x = s_memory0;
 __return_1705217 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_1705218 = x;
}
rsp_d = __return_1705217;
goto label_1705220;
rsp_d = __return_1705218;
label_1705220:; 
rsp_status = 1;
label_1705226:; 
c_rsp_type = rsp_type;
c_rsp_status = rsp_status;
c_rsp_d = rsp_d;
c_empty_rsp = 0;
c_write_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1705307;
}
else 
{
goto label_1705248;
}
}
else 
{
label_1705248:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1705307;
}
else 
{
goto label_1705255;
}
}
else 
{
label_1705255:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1705307;
}
else 
{
goto label_1705262;
}
}
else 
{
label_1705262:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1705307;
}
else 
{
goto label_1705269;
}
}
else 
{
label_1705269:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1705307;
}
else 
{
goto label_1705276;
}
}
else 
{
label_1705276:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1705285;
}
else 
{
label_1705285:; 
goto label_1705307;
}
}
else 
{
label_1705307:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1705329;
}
else 
{
goto label_1705314;
}
}
else 
{
label_1705314:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1705323;
}
else 
{
label_1705323:; 
goto label_1705329;
}
}
else 
{
label_1705329:; 
c_write_rsp_ev = 2;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1705412;
}
else 
{
goto label_1705353;
}
}
else 
{
label_1705353:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1705412;
}
else 
{
goto label_1705360;
}
}
else 
{
label_1705360:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1705412;
}
else 
{
goto label_1705367;
}
}
else 
{
label_1705367:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1705412;
}
else 
{
goto label_1705374;
}
}
else 
{
label_1705374:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1705412;
}
else 
{
goto label_1705381;
}
}
else 
{
label_1705381:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1705390;
}
else 
{
label_1705390:; 
goto label_1705412;
}
}
else 
{
label_1705412:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1705419;
}
}
else 
{
label_1705419:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1705428;
}
else 
{
label_1705428:; 
return 1;
}
}
}
}
}
}
}
}
}
label_1797277:; 
label_1799113:; 
if (((int)m_run_st) == 0)
{
goto label_1799126;
}
else 
{
label_1799126:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_1800188;
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
if (((int)m_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type;
req_a = req_t_a;
req_d = req_t_d;
rsp_type = rsp_t_type;
rsp_status = rsp_t_status;
rsp_d = rsp_t_d;
d = d_t;
a = a_t;
rsp_type = c_rsp_type;
rsp_status = c_rsp_status;
rsp_d = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1799314;
}
else 
{
goto label_1799255;
}
}
else 
{
label_1799255:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1799314;
}
else 
{
goto label_1799262;
}
}
else 
{
label_1799262:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1799314;
}
else 
{
goto label_1799269;
}
}
else 
{
label_1799269:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1799314;
}
else 
{
goto label_1799276;
}
}
else 
{
label_1799276:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1799314;
}
else 
{
goto label_1799283;
}
}
else 
{
label_1799283:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1799292;
}
else 
{
label_1799292:; 
goto label_1799314;
}
}
else 
{
label_1799314:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1799336;
}
else 
{
goto label_1799321;
}
}
else 
{
label_1799321:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1799330;
}
else 
{
label_1799330:; 
goto label_1799336;
}
}
else 
{
label_1799336:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1799409;
}
else 
{
goto label_1799350;
}
}
else 
{
label_1799350:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1799409;
}
else 
{
goto label_1799357;
}
}
else 
{
label_1799357:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1799409;
}
else 
{
goto label_1799364;
}
}
else 
{
label_1799364:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1799409;
}
else 
{
goto label_1799371;
}
}
else 
{
label_1799371:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1799409;
}
else 
{
goto label_1799378;
}
}
else 
{
label_1799378:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1799387;
}
else 
{
label_1799387:; 
goto label_1799409;
}
}
else 
{
label_1799409:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1799431;
}
else 
{
goto label_1799416;
}
}
else 
{
label_1799416:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1799425;
}
else 
{
label_1799425:; 
goto label_1799431;
}
}
else 
{
label_1799431:; 
c_m_ev = 2;
a = a + 1;
if (a < 1)
{
req_type = 1;
req_a = a;
req_d = a + 50;
c_m_lock = 1;
c_req_type = req_type;
c_req_a = req_a;
c_req_d = req_d;
c_empty_req = 0;
c_write_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1799896;
}
else 
{
goto label_1799837;
}
}
else 
{
label_1799837:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1799896;
}
else 
{
goto label_1799844;
}
}
else 
{
label_1799844:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1799896;
}
else 
{
goto label_1799851;
}
}
else 
{
label_1799851:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1799896;
}
else 
{
goto label_1799858;
}
}
else 
{
label_1799858:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1799896;
}
else 
{
goto label_1799865;
}
}
else 
{
label_1799865:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1799874;
}
else 
{
label_1799874:; 
goto label_1799896;
}
}
else 
{
label_1799896:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1799918;
}
else 
{
goto label_1799903;
}
}
else 
{
label_1799903:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1799912;
}
else 
{
label_1799912:; 
goto label_1799918;
}
}
else 
{
label_1799918:; 
c_write_req_ev = 2;
if (((int)c_empty_rsp) == 1)
{
m_run_st = 2;
m_run_pc = 3;
req_t_type = req_type;
req_t_a = req_a;
req_t_d = req_d;
rsp_t_type = rsp_type;
rsp_t_status = rsp_status;
rsp_t_d = rsp_d;
d_t = d;
a_t = a;
}
else 
{
rsp_type = c_rsp_type;
rsp_status = c_rsp_status;
rsp_d = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1799998;
}
else 
{
goto label_1799939;
}
}
else 
{
label_1799939:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1799998;
}
else 
{
goto label_1799946;
}
}
else 
{
label_1799946:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1799998;
}
else 
{
goto label_1799953;
}
}
else 
{
label_1799953:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1799998;
}
else 
{
goto label_1799960;
}
}
else 
{
label_1799960:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1799998;
}
else 
{
goto label_1799967;
}
}
else 
{
label_1799967:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1799976;
}
else 
{
label_1799976:; 
goto label_1799998;
}
}
else 
{
label_1799998:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1800020;
}
else 
{
goto label_1800005;
}
}
else 
{
label_1800005:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1800014;
}
else 
{
label_1800014:; 
goto label_1800020;
}
}
else 
{
label_1800020:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1800093;
}
else 
{
goto label_1800034;
}
}
else 
{
label_1800034:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1800093;
}
else 
{
goto label_1800041;
}
}
else 
{
label_1800041:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1800093;
}
else 
{
goto label_1800048;
}
}
else 
{
label_1800048:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1800093;
}
else 
{
goto label_1800055;
}
}
else 
{
label_1800055:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1800093;
}
else 
{
goto label_1800062;
}
}
else 
{
label_1800062:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1800071;
}
else 
{
label_1800071:; 
goto label_1800093;
}
}
else 
{
label_1800093:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1800100;
}
}
else 
{
label_1800100:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1800109;
}
else 
{
label_1800109:; 
return 1;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
goto label_1797783;
}
}
}
}
}
}
}
}
}
else 
{
a = 0;
req_type___0 = 0;
req_a___0 = a;
c_m_lock = 1;
c_req_type = req_type___0;
c_req_a = req_a___0;
c_req_d = req_d___0;
c_empty_req = 0;
c_write_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1799542;
}
else 
{
goto label_1799483;
}
}
else 
{
label_1799483:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1799542;
}
else 
{
goto label_1799490;
}
}
else 
{
label_1799490:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1799542;
}
else 
{
goto label_1799497;
}
}
else 
{
label_1799497:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1799542;
}
else 
{
goto label_1799504;
}
}
else 
{
label_1799504:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1799542;
}
else 
{
goto label_1799511;
}
}
else 
{
label_1799511:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1799520;
}
else 
{
label_1799520:; 
goto label_1799542;
}
}
else 
{
label_1799542:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1799564;
}
else 
{
goto label_1799549;
}
}
else 
{
label_1799549:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1799558;
}
else 
{
label_1799558:; 
goto label_1799564;
}
}
else 
{
label_1799564:; 
c_write_req_ev = 2;
if (((int)c_empty_rsp) == 1)
{
m_run_st = 2;
m_run_pc = 6;
req_tt_type = req_type___0;
req_tt_a = req_a___0;
req_tt_d = req_d___0;
rsp_tt_type = rsp_type___0;
rsp_tt_status = rsp_status___0;
rsp_tt_d = rsp_d___0;
d_t = d;
a_t = a;
}
else 
{
rsp_type___0 = c_rsp_type;
rsp_status___0 = c_rsp_status;
rsp_d___0 = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1799644;
}
else 
{
goto label_1799585;
}
}
else 
{
label_1799585:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1799644;
}
else 
{
goto label_1799592;
}
}
else 
{
label_1799592:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1799644;
}
else 
{
goto label_1799599;
}
}
else 
{
label_1799599:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1799644;
}
else 
{
goto label_1799606;
}
}
else 
{
label_1799606:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1799644;
}
else 
{
goto label_1799613;
}
}
else 
{
label_1799613:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1799622;
}
else 
{
label_1799622:; 
goto label_1799644;
}
}
else 
{
label_1799644:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1799666;
}
else 
{
goto label_1799651;
}
}
else 
{
label_1799651:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1799660;
}
else 
{
label_1799660:; 
goto label_1799666;
}
}
else 
{
label_1799666:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1799739;
}
else 
{
goto label_1799680;
}
}
else 
{
label_1799680:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1799739;
}
else 
{
goto label_1799687;
}
}
else 
{
label_1799687:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1799739;
}
else 
{
goto label_1799694;
}
}
else 
{
label_1799694:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1799739;
}
else 
{
goto label_1799701;
}
}
else 
{
label_1799701:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1799739;
}
else 
{
goto label_1799708;
}
}
else 
{
label_1799708:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1799717;
}
else 
{
label_1799717:; 
goto label_1799739;
}
}
else 
{
label_1799739:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1799746;
}
}
else 
{
label_1799746:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1799755;
}
else 
{
label_1799755:; 
return 1;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
label_1800177:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1800833;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1800323;
}
else 
{
goto label_1800264;
}
}
else 
{
label_1800264:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1800323;
}
else 
{
goto label_1800271;
}
}
else 
{
label_1800271:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1800323;
}
else 
{
goto label_1800278;
}
}
else 
{
label_1800278:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1800323;
}
else 
{
goto label_1800285;
}
}
else 
{
label_1800285:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1800323;
}
else 
{
goto label_1800292;
}
}
else 
{
label_1800292:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1800301;
}
else 
{
label_1800301:; 
goto label_1800323;
}
}
else 
{
label_1800323:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1800345;
}
else 
{
goto label_1800330;
}
}
else 
{
label_1800330:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1800339;
}
else 
{
label_1800339:; 
goto label_1800345;
}
}
else 
{
label_1800345:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_5 = req_a;
int i = __tmp_5;
int x;
if (i == 0)
{
x = s_memory0;
 __return_1800395 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_1800396 = x;
}
rsp_d = __return_1800395;
goto label_1800398;
rsp_d = __return_1800396;
label_1800398:; 
rsp_status = 1;
label_1800404:; 
c_rsp_type = rsp_type;
c_rsp_status = rsp_status;
c_rsp_d = rsp_d;
c_empty_rsp = 0;
c_write_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1800485;
}
else 
{
goto label_1800426;
}
}
else 
{
label_1800426:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1800485;
}
else 
{
goto label_1800433;
}
}
else 
{
label_1800433:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1800485;
}
else 
{
goto label_1800440;
}
}
else 
{
label_1800440:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1800485;
}
else 
{
goto label_1800447;
}
}
else 
{
label_1800447:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1800485;
}
else 
{
goto label_1800454;
}
}
else 
{
label_1800454:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1800463;
}
else 
{
label_1800463:; 
goto label_1800485;
}
}
else 
{
label_1800485:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1800507;
}
else 
{
goto label_1800492;
}
}
else 
{
label_1800492:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1800501;
}
else 
{
label_1800501:; 
goto label_1800507;
}
}
else 
{
label_1800507:; 
c_write_rsp_ev = 2;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1800590;
}
else 
{
goto label_1800531;
}
}
else 
{
label_1800531:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1800590;
}
else 
{
goto label_1800538;
}
}
else 
{
label_1800538:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1800590;
}
else 
{
goto label_1800545;
}
}
else 
{
label_1800545:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1800590;
}
else 
{
goto label_1800552;
}
}
else 
{
label_1800552:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1800590;
}
else 
{
goto label_1800559;
}
}
else 
{
label_1800559:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1800568;
}
else 
{
label_1800568:; 
goto label_1800590;
}
}
else 
{
label_1800590:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1800597;
}
}
else 
{
label_1800597:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1800606;
}
else 
{
label_1800606:; 
return 1;
}
}
}
}
}
}
}
}
}
label_1811937:; 
label_1811941:; 
if (((int)m_run_st) == 0)
{
goto label_1811954;
}
else 
{
label_1811954:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_1812668;
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
if (((int)m_run_pc) == 0)
{
return 1;
}
else 
{
req_type___0 = req_tt_type;
req_a___0 = req_tt_a;
req_d___0 = req_tt_d;
rsp_type___0 = rsp_tt_type;
rsp_status___0 = rsp_tt_status;
rsp_d___0 = rsp_tt_d;
d = d_t;
a = a_t;
rsp_type___0 = c_rsp_type;
rsp_status___0 = c_rsp_status;
rsp_d___0 = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1812142;
}
else 
{
goto label_1812083;
}
}
else 
{
label_1812083:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1812142;
}
else 
{
goto label_1812090;
}
}
else 
{
label_1812090:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1812142;
}
else 
{
goto label_1812097;
}
}
else 
{
label_1812097:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1812142;
}
else 
{
goto label_1812104;
}
}
else 
{
label_1812104:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1812142;
}
else 
{
goto label_1812111;
}
}
else 
{
label_1812111:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1812120;
}
else 
{
label_1812120:; 
goto label_1812142;
}
}
else 
{
label_1812142:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1812164;
}
else 
{
goto label_1812149;
}
}
else 
{
label_1812149:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1812158;
}
else 
{
label_1812158:; 
goto label_1812164;
}
}
else 
{
label_1812164:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1812237;
}
else 
{
goto label_1812178;
}
}
else 
{
label_1812178:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1812237;
}
else 
{
goto label_1812185;
}
}
else 
{
label_1812185:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1812237;
}
else 
{
goto label_1812192;
}
}
else 
{
label_1812192:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1812237;
}
else 
{
goto label_1812199;
}
}
else 
{
label_1812199:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1812237;
}
else 
{
goto label_1812206;
}
}
else 
{
label_1812206:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1812215;
}
else 
{
label_1812215:; 
goto label_1812237;
}
}
else 
{
label_1812237:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1812259;
}
else 
{
goto label_1812244;
}
}
else 
{
label_1812244:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1812253;
}
else 
{
label_1812253:; 
goto label_1812259;
}
}
else 
{
label_1812259:; 
c_m_ev = 2;
if ((req_a___0 + 50) == rsp_d___0)
{
a = a + 1;
goto label_1812276;
}
else 
{
{
__VERIFIER_error();
}
a = a + 1;
label_1812276:; 
if (a < 1)
{
req_type___0 = 0;
req_a___0 = a;
if (c_m_lock == 1)
{
m_run_st = 2;
m_run_pc = 4;
req_tt_type = req_type___0;
req_tt_a = req_a___0;
req_tt_d = req_d___0;
rsp_tt_type = rsp_type___0;
rsp_tt_status = rsp_status___0;
rsp_tt_d = rsp_d___0;
d_t = d;
a_t = a;
}
else 
{
c_m_lock = 1;
c_req_type = req_type___0;
c_req_a = req_a___0;
c_req_d = req_d___0;
c_empty_req = 0;
c_write_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1812374;
}
else 
{
goto label_1812315;
}
}
else 
{
label_1812315:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1812374;
}
else 
{
goto label_1812322;
}
}
else 
{
label_1812322:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1812374;
}
else 
{
goto label_1812329;
}
}
else 
{
label_1812329:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1812374;
}
else 
{
goto label_1812336;
}
}
else 
{
label_1812336:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1812374;
}
else 
{
goto label_1812343;
}
}
else 
{
label_1812343:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1812352;
}
else 
{
label_1812352:; 
goto label_1812374;
}
}
else 
{
label_1812374:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1812396;
}
else 
{
goto label_1812381;
}
}
else 
{
label_1812381:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1812390;
}
else 
{
label_1812390:; 
goto label_1812396;
}
}
else 
{
label_1812396:; 
c_write_req_ev = 2;
if (((int)c_empty_rsp) == 1)
{
m_run_st = 2;
m_run_pc = 6;
req_tt_type = req_type___0;
req_tt_a = req_a___0;
req_tt_d = req_d___0;
rsp_tt_type = rsp_type___0;
rsp_tt_status = rsp_status___0;
rsp_tt_d = rsp_d___0;
d_t = d;
a_t = a;
}
else 
{
rsp_type___0 = c_rsp_type;
rsp_status___0 = c_rsp_status;
rsp_d___0 = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1812476;
}
else 
{
goto label_1812417;
}
}
else 
{
label_1812417:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1812476;
}
else 
{
goto label_1812424;
}
}
else 
{
label_1812424:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1812476;
}
else 
{
goto label_1812431;
}
}
else 
{
label_1812431:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1812476;
}
else 
{
goto label_1812438;
}
}
else 
{
label_1812438:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1812476;
}
else 
{
goto label_1812445;
}
}
else 
{
label_1812445:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1812454;
}
else 
{
label_1812454:; 
goto label_1812476;
}
}
else 
{
label_1812476:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1812498;
}
else 
{
goto label_1812483;
}
}
else 
{
label_1812483:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1812492;
}
else 
{
label_1812492:; 
goto label_1812498;
}
}
else 
{
label_1812498:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1812571;
}
else 
{
goto label_1812512;
}
}
else 
{
label_1812512:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1812571;
}
else 
{
goto label_1812519;
}
}
else 
{
label_1812519:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1812571;
}
else 
{
goto label_1812526;
}
}
else 
{
label_1812526:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1812571;
}
else 
{
goto label_1812533;
}
}
else 
{
label_1812533:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1812571;
}
else 
{
goto label_1812540;
}
}
else 
{
label_1812540:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1812549;
}
else 
{
label_1812549:; 
goto label_1812571;
}
}
else 
{
label_1812571:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1812578;
}
}
else 
{
label_1812578:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1812587;
}
else 
{
label_1812587:; 
return 1;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
goto label_1800177;
}
}
}
}
}
}
}
}
}
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1813196;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1812811;
}
else 
{
goto label_1812752;
}
}
else 
{
label_1812752:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1812811;
}
else 
{
goto label_1812759;
}
}
else 
{
label_1812759:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1812811;
}
else 
{
goto label_1812766;
}
}
else 
{
label_1812766:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1812811;
}
else 
{
goto label_1812773;
}
}
else 
{
label_1812773:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1812811;
}
else 
{
goto label_1812780;
}
}
else 
{
label_1812780:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1812789;
}
else 
{
label_1812789:; 
goto label_1812811;
}
}
else 
{
label_1812811:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1812818;
}
}
else 
{
label_1812818:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1812827;
}
else 
{
label_1812827:; 
return 1;
}
}
}
}
}
}
}
}
}
label_1812857:; 
label_1813631:; 
if (((int)m_run_st) == 0)
{
goto label_1813645;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1813645:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_1813656;
}
else 
{
label_1813656:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1813832;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1813783;
}
else 
{
goto label_1813724;
}
}
else 
{
label_1813724:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1813783;
}
else 
{
goto label_1813731;
}
}
else 
{
label_1813731:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1813783;
}
else 
{
goto label_1813738;
}
}
else 
{
label_1813738:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1813783;
}
else 
{
goto label_1813745;
}
}
else 
{
label_1813745:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1813783;
}
else 
{
goto label_1813752;
}
}
else 
{
label_1813752:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1813761;
}
else 
{
label_1813761:; 
goto label_1813783;
}
}
else 
{
label_1813783:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1813790;
}
}
else 
{
label_1813790:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1813799;
}
else 
{
label_1813799:; 
return 1;
}
}
}
}
}
}
}
}
}
goto label_1812857;
}
}
}
}
else 
{
label_1813832:; 
goto label_1813631;
}
}
}
else 
{
}
goto label_1949316;
}
}
}
}
}
else 
{
label_1813196:; 
goto label_1813631;
}
}
else 
{
}
label_1812657:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1813198;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1812977;
}
else 
{
goto label_1812918;
}
}
else 
{
label_1812918:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1812977;
}
else 
{
goto label_1812925;
}
}
else 
{
label_1812925:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1812977;
}
else 
{
goto label_1812932;
}
}
else 
{
label_1812932:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1812977;
}
else 
{
goto label_1812939;
}
}
else 
{
label_1812939:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1812977;
}
else 
{
goto label_1812946;
}
}
else 
{
label_1812946:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1812955;
}
else 
{
label_1812955:; 
goto label_1812977;
}
}
else 
{
label_1812977:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1812984;
}
}
else 
{
label_1812984:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1812993;
}
else 
{
label_1812993:; 
return 1;
}
}
}
}
}
}
}
}
}
label_1813023:; 
label_1813204:; 
if (((int)m_run_st) == 0)
{
goto label_1813218;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1813218:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_1813451;
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
if (((int)m_run_pc) == 0)
{
return 1;
}
else 
{
req_type___0 = req_tt_type;
req_a___0 = req_tt_a;
req_d___0 = req_tt_d;
rsp_type___0 = rsp_tt_type;
rsp_status___0 = rsp_tt_status;
rsp_d___0 = rsp_tt_d;
d = d_t;
a = a_t;
if (((int)c_empty_rsp) == 1)
{
m_run_st = 2;
m_run_pc = 6;
req_tt_type = req_type___0;
req_tt_a = req_a___0;
req_tt_d = req_d___0;
rsp_tt_type = rsp_type___0;
rsp_tt_status = rsp_status___0;
rsp_tt_d = rsp_d___0;
d_t = d;
a_t = a;
}
else 
{
rsp_type___0 = c_rsp_type;
rsp_status___0 = c_rsp_status;
rsp_d___0 = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1813406;
}
else 
{
goto label_1813347;
}
}
else 
{
label_1813347:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1813406;
}
else 
{
goto label_1813354;
}
}
else 
{
label_1813354:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1813406;
}
else 
{
goto label_1813361;
}
}
else 
{
label_1813361:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1813406;
}
else 
{
goto label_1813368;
}
}
else 
{
label_1813368:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1813406;
}
else 
{
goto label_1813375;
}
}
else 
{
label_1813375:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1813384;
}
else 
{
label_1813384:; 
goto label_1813406;
}
}
else 
{
label_1813406:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1813413;
}
}
else 
{
label_1813413:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1813422;
}
else 
{
label_1813422:; 
return 1;
}
}
}
}
}
}
}
}
}
goto label_1812657;
}
}
}
}
else 
{
label_1813451:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1813627;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1813578;
}
else 
{
goto label_1813519;
}
}
else 
{
label_1813519:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1813578;
}
else 
{
goto label_1813526;
}
}
else 
{
label_1813526:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1813578;
}
else 
{
goto label_1813533;
}
}
else 
{
label_1813533:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1813578;
}
else 
{
goto label_1813540;
}
}
else 
{
label_1813540:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1813578;
}
else 
{
goto label_1813547;
}
}
else 
{
label_1813547:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1813556;
}
else 
{
label_1813556:; 
goto label_1813578;
}
}
else 
{
label_1813578:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1813585;
}
}
else 
{
label_1813585:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1813594;
}
else 
{
label_1813594:; 
return 1;
}
}
}
}
}
}
}
}
}
goto label_1813023;
}
}
}
}
else 
{
label_1813627:; 
goto label_1813204;
}
}
}
else 
{
}
goto label_1933154;
}
}
}
}
}
else 
{
label_1813198:; 
goto label_1813204;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
else 
{
label_1812668:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1813200;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1813143;
}
else 
{
goto label_1813084;
}
}
else 
{
label_1813084:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1813143;
}
else 
{
goto label_1813091;
}
}
else 
{
label_1813091:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1813143;
}
else 
{
goto label_1813098;
}
}
else 
{
label_1813098:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1813143;
}
else 
{
goto label_1813105;
}
}
else 
{
label_1813105:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1813143;
}
else 
{
goto label_1813112;
}
}
else 
{
label_1813112:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1813121;
}
else 
{
label_1813121:; 
goto label_1813143;
}
}
else 
{
label_1813143:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1813150;
}
}
else 
{
label_1813150:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1813159;
}
else 
{
label_1813159:; 
return 1;
}
}
}
}
}
}
}
}
}
goto label_1811937;
}
}
}
}
else 
{
label_1813200:; 
goto label_1811941;
}
}
}
}
}
}
}
}
}
}
}
}
}
else 
{
if (((int)req_type) == 1)
{
{
int __tmp_6 = req_a;
int __tmp_7 = req_d;
int i = __tmp_6;
int v = __tmp_7;
if (i == 0)
{
s_memory0 = v;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_1800373;
label_1800373:; 
rsp_status = 1;
goto label_1800404;
}
}
else 
{
rsp_status = 0;
goto label_1800404;
}
}
}
}
}
}
}
}
}
}
}
label_1800659:; 
label_1813842:; 
if (((int)m_run_st) == 0)
{
goto label_1813856;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1813856:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_1813867;
}
else 
{
label_1813867:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1814043;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1813994;
}
else 
{
goto label_1813935;
}
}
else 
{
label_1813935:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1813994;
}
else 
{
goto label_1813942;
}
}
else 
{
label_1813942:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1813994;
}
else 
{
goto label_1813949;
}
}
else 
{
label_1813949:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1813994;
}
else 
{
goto label_1813956;
}
}
else 
{
label_1813956:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1813994;
}
else 
{
goto label_1813963;
}
}
else 
{
label_1813963:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1813972;
}
else 
{
label_1813972:; 
goto label_1813994;
}
}
else 
{
label_1813994:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1814001;
}
}
else 
{
label_1814001:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1814010;
}
else 
{
label_1814010:; 
return 1;
}
}
}
}
}
}
}
}
}
goto label_1800659;
}
}
}
}
else 
{
label_1814043:; 
goto label_1813842;
}
}
}
else 
{
}
goto label_1933156;
}
}
}
}
}
else 
{
label_1800833:; 
label_1814049:; 
if (((int)m_run_st) == 0)
{
goto label_1814062;
}
else 
{
label_1814062:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_1814073;
}
else 
{
label_1814073:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1814541;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1814200;
}
else 
{
goto label_1814141;
}
}
else 
{
label_1814141:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1814200;
}
else 
{
goto label_1814148;
}
}
else 
{
label_1814148:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1814200;
}
else 
{
goto label_1814155;
}
}
else 
{
label_1814155:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1814200;
}
else 
{
goto label_1814162;
}
}
else 
{
label_1814162:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1814200;
}
else 
{
goto label_1814169;
}
}
else 
{
label_1814169:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1814178;
}
else 
{
label_1814178:; 
goto label_1814200;
}
}
else 
{
label_1814200:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1814222;
}
else 
{
goto label_1814207;
}
}
else 
{
label_1814207:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1814216;
}
else 
{
label_1814216:; 
goto label_1814222;
}
}
else 
{
label_1814222:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_8 = req_a;
int i = __tmp_8;
int x;
if (i == 0)
{
x = s_memory0;
 __return_1814272 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_1814273 = x;
}
rsp_d = __return_1814272;
goto label_1814275;
rsp_d = __return_1814273;
label_1814275:; 
rsp_status = 1;
label_1814281:; 
c_rsp_type = rsp_type;
c_rsp_status = rsp_status;
c_rsp_d = rsp_d;
c_empty_rsp = 0;
c_write_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1814362;
}
else 
{
goto label_1814303;
}
}
else 
{
label_1814303:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1814362;
}
else 
{
goto label_1814310;
}
}
else 
{
label_1814310:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1814362;
}
else 
{
goto label_1814317;
}
}
else 
{
label_1814317:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1814362;
}
else 
{
goto label_1814324;
}
}
else 
{
label_1814324:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1814362;
}
else 
{
goto label_1814331;
}
}
else 
{
label_1814331:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1814340;
}
else 
{
label_1814340:; 
goto label_1814362;
}
}
else 
{
label_1814362:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1814384;
}
else 
{
goto label_1814369;
}
}
else 
{
label_1814369:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1814378;
}
else 
{
label_1814378:; 
goto label_1814384;
}
}
else 
{
label_1814384:; 
c_write_rsp_ev = 2;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1814467;
}
else 
{
goto label_1814408;
}
}
else 
{
label_1814408:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1814467;
}
else 
{
goto label_1814415;
}
}
else 
{
label_1814415:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1814467;
}
else 
{
goto label_1814422;
}
}
else 
{
label_1814422:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1814467;
}
else 
{
goto label_1814429;
}
}
else 
{
label_1814429:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1814467;
}
else 
{
goto label_1814436;
}
}
else 
{
label_1814436:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1814445;
}
else 
{
label_1814445:; 
goto label_1814467;
}
}
else 
{
label_1814467:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1814474;
}
}
else 
{
label_1814474:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1814483;
}
else 
{
label_1814483:; 
return 1;
}
}
}
}
}
}
}
}
}
goto label_1811937;
}
}
}
}
}
}
}
}
}
}
else 
{
if (((int)req_type) == 1)
{
{
int __tmp_9 = req_a;
int __tmp_10 = req_d;
int i = __tmp_9;
int v = __tmp_10;
if (i == 0)
{
s_memory0 = v;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_1814250;
label_1814250:; 
rsp_status = 1;
goto label_1814281;
}
}
else 
{
rsp_status = 0;
goto label_1814281;
}
}
}
}
}
}
}
}
}
}
}
goto label_1800659;
}
}
}
}
else 
{
label_1814541:; 
goto label_1814049;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
else 
{
label_1800188:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1800835;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1800781;
}
else 
{
goto label_1800722;
}
}
else 
{
label_1800722:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1800781;
}
else 
{
goto label_1800729;
}
}
else 
{
label_1800729:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1800781;
}
else 
{
goto label_1800736;
}
}
else 
{
label_1800736:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1800781;
}
else 
{
goto label_1800743;
}
}
else 
{
label_1800743:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1800781;
}
else 
{
goto label_1800750;
}
}
else 
{
label_1800750:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1800759;
}
else 
{
label_1800759:; 
goto label_1800781;
}
}
else 
{
label_1800781:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1800788;
}
}
else 
{
label_1800788:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1800797;
}
else 
{
label_1800797:; 
return 1;
}
}
}
}
}
}
}
}
}
goto label_1797277;
}
}
}
}
else 
{
label_1800835:; 
goto label_1799113;
}
}
}
}
}
}
}
}
}
}
}
}
}
else 
{
if (((int)req_type) == 1)
{
{
int __tmp_11 = req_a;
int __tmp_12 = req_d;
int i = __tmp_11;
int v = __tmp_12;
if (i == 0)
{
s_memory0 = v;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_1705195;
label_1705195:; 
rsp_status = 1;
goto label_1705226;
}
}
else 
{
rsp_status = 0;
goto label_1705226;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
else 
{
label_1705485:; 
goto label_1704993;
}
}
}
else 
{
}
label_1705004:; 
kernel_st = 2;
kernel_st = 3;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1705687;
}
else 
{
goto label_1705510;
}
}
else 
{
label_1705510:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1705687;
}
else 
{
goto label_1705535;
}
}
else 
{
label_1705535:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1705687;
}
else 
{
goto label_1705552;
}
}
else 
{
label_1705552:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1705687;
}
else 
{
goto label_1705577;
}
}
else 
{
label_1705577:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1705687;
}
else 
{
goto label_1705594;
}
}
else 
{
label_1705594:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1705621;
}
else 
{
label_1705621:; 
goto label_1705687;
}
}
else 
{
label_1705687:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1705757;
}
else 
{
goto label_1705712;
}
}
else 
{
label_1705712:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1705739;
}
else 
{
label_1705739:; 
goto label_1705757;
}
}
else 
{
label_1705757:; 
if (((int)m_run_st) == 0)
{
goto label_1705793;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1705793:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
label_1705805:; 
if (((int)m_run_st) == 0)
{
goto label_1705819;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1705819:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_1706146;
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
if (((int)m_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type;
req_a = req_t_a;
req_d = req_t_d;
rsp_type = rsp_t_type;
rsp_status = rsp_t_status;
rsp_d = rsp_t_d;
d = d_t;
a = a_t;
if (((int)c_empty_rsp) == 1)
{
m_run_st = 2;
m_run_pc = 3;
req_t_type = req_type;
req_t_a = req_a;
req_t_d = req_d;
rsp_t_type = rsp_type;
rsp_t_status = rsp_status;
rsp_t_d = rsp_d;
d_t = d;
a_t = a;
}
else 
{
rsp_type = c_rsp_type;
rsp_status = c_rsp_status;
rsp_d = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1706007;
}
else 
{
goto label_1705948;
}
}
else 
{
label_1705948:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1706007;
}
else 
{
goto label_1705955;
}
}
else 
{
label_1705955:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1706007;
}
else 
{
goto label_1705962;
}
}
else 
{
label_1705962:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1706007;
}
else 
{
goto label_1705969;
}
}
else 
{
label_1705969:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1706007;
}
else 
{
goto label_1705976;
}
}
else 
{
label_1705976:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1705985;
}
else 
{
label_1705985:; 
goto label_1706007;
}
}
else 
{
label_1706007:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1706029;
}
else 
{
goto label_1706014;
}
}
else 
{
label_1706014:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1706023;
}
else 
{
label_1706023:; 
goto label_1706029;
}
}
else 
{
label_1706029:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1706102;
}
else 
{
goto label_1706043;
}
}
else 
{
label_1706043:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1706102;
}
else 
{
goto label_1706050;
}
}
else 
{
label_1706050:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1706102;
}
else 
{
goto label_1706057;
}
}
else 
{
label_1706057:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1706102;
}
else 
{
goto label_1706064;
}
}
else 
{
label_1706064:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1706102;
}
else 
{
goto label_1706071;
}
}
else 
{
label_1706071:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1706080;
}
else 
{
label_1706080:; 
goto label_1706102;
}
}
else 
{
label_1706102:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1706109;
}
}
else 
{
label_1706109:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1706118;
}
else 
{
label_1706118:; 
return 1;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1845671;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
goto label_1845248;
}
else 
{
label_1845248:; 
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1845330;
}
else 
{
goto label_1845271;
}
}
else 
{
label_1845271:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1845330;
}
else 
{
goto label_1845278;
}
}
else 
{
label_1845278:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1845330;
}
else 
{
goto label_1845285;
}
}
else 
{
label_1845285:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1845330;
}
else 
{
goto label_1845292;
}
}
else 
{
label_1845292:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1845330;
}
else 
{
goto label_1845299;
}
}
else 
{
label_1845299:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1845308;
}
else 
{
label_1845308:; 
goto label_1845330;
}
}
else 
{
label_1845330:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1845352;
}
else 
{
goto label_1845337;
}
}
else 
{
label_1845337:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1845346;
}
else 
{
label_1845346:; 
goto label_1845352;
}
}
else 
{
label_1845352:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_13 = req_a;
int i = __tmp_13;
int x;
if (i == 0)
{
x = s_memory0;
 __return_1845402 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_1845403 = x;
}
rsp_d = __return_1845402;
goto label_1845405;
rsp_d = __return_1845403;
label_1845405:; 
rsp_status = 1;
label_1845411:; 
c_rsp_type = rsp_type;
c_rsp_status = rsp_status;
c_rsp_d = rsp_d;
c_empty_rsp = 0;
c_write_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1845492;
}
else 
{
goto label_1845433;
}
}
else 
{
label_1845433:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1845492;
}
else 
{
goto label_1845440;
}
}
else 
{
label_1845440:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1845492;
}
else 
{
goto label_1845447;
}
}
else 
{
label_1845447:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1845492;
}
else 
{
goto label_1845454;
}
}
else 
{
label_1845454:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1845492;
}
else 
{
goto label_1845461;
}
}
else 
{
label_1845461:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1845470;
}
else 
{
label_1845470:; 
goto label_1845492;
}
}
else 
{
label_1845492:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1845514;
}
else 
{
goto label_1845499;
}
}
else 
{
label_1845499:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1845508;
}
else 
{
label_1845508:; 
goto label_1845514;
}
}
else 
{
label_1845514:; 
c_write_rsp_ev = 2;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1845597;
}
else 
{
goto label_1845538;
}
}
else 
{
label_1845538:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1845597;
}
else 
{
goto label_1845545;
}
}
else 
{
label_1845545:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1845597;
}
else 
{
goto label_1845552;
}
}
else 
{
label_1845552:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1845597;
}
else 
{
goto label_1845559;
}
}
else 
{
label_1845559:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1845597;
}
else 
{
goto label_1845566;
}
}
else 
{
label_1845566:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1845575;
}
else 
{
label_1845575:; 
goto label_1845597;
}
}
else 
{
label_1845597:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1845604;
}
}
else 
{
label_1845604:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1845613;
}
else 
{
label_1845613:; 
return 1;
}
}
}
}
}
}
}
}
}
goto label_1845185;
}
}
}
}
}
}
}
}
}
}
else 
{
if (((int)req_type) == 1)
{
{
int __tmp_14 = req_a;
int __tmp_15 = req_d;
int i = __tmp_14;
int v = __tmp_15;
if (i == 0)
{
s_memory0 = v;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_1845380;
label_1845380:; 
rsp_status = 1;
goto label_1845411;
}
}
else 
{
rsp_status = 0;
goto label_1845411;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
else 
{
label_1845671:; 
goto label_1705805;
}
}
}
}
}
else 
{
label_1706146:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1707082;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
goto label_1706656;
}
else 
{
label_1706656:; 
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1706738;
}
else 
{
goto label_1706679;
}
}
else 
{
label_1706679:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1706738;
}
else 
{
goto label_1706686;
}
}
else 
{
label_1706686:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1706738;
}
else 
{
goto label_1706693;
}
}
else 
{
label_1706693:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1706738;
}
else 
{
goto label_1706700;
}
}
else 
{
label_1706700:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1706738;
}
else 
{
goto label_1706707;
}
}
else 
{
label_1706707:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1706716;
}
else 
{
label_1706716:; 
goto label_1706738;
}
}
else 
{
label_1706738:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1706760;
}
else 
{
goto label_1706745;
}
}
else 
{
label_1706745:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1706754;
}
else 
{
label_1706754:; 
goto label_1706760;
}
}
else 
{
label_1706760:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_16 = req_a;
int i = __tmp_16;
int x;
if (i == 0)
{
x = s_memory0;
 __return_1706810 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_1706811 = x;
}
rsp_d = __return_1706810;
goto label_1706813;
rsp_d = __return_1706811;
label_1706813:; 
rsp_status = 1;
label_1706819:; 
c_rsp_type = rsp_type;
c_rsp_status = rsp_status;
c_rsp_d = rsp_d;
c_empty_rsp = 0;
c_write_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1706900;
}
else 
{
goto label_1706841;
}
}
else 
{
label_1706841:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1706900;
}
else 
{
goto label_1706848;
}
}
else 
{
label_1706848:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1706900;
}
else 
{
goto label_1706855;
}
}
else 
{
label_1706855:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1706900;
}
else 
{
goto label_1706862;
}
}
else 
{
label_1706862:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1706900;
}
else 
{
goto label_1706869;
}
}
else 
{
label_1706869:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1706878;
}
else 
{
label_1706878:; 
goto label_1706900;
}
}
else 
{
label_1706900:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1706922;
}
else 
{
goto label_1706907;
}
}
else 
{
label_1706907:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1706916;
}
else 
{
label_1706916:; 
goto label_1706922;
}
}
else 
{
label_1706922:; 
c_write_rsp_ev = 2;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1707005;
}
else 
{
goto label_1706946;
}
}
else 
{
label_1706946:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1707005;
}
else 
{
goto label_1706953;
}
}
else 
{
label_1706953:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1707005;
}
else 
{
goto label_1706960;
}
}
else 
{
label_1706960:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1707005;
}
else 
{
goto label_1706967;
}
}
else 
{
label_1706967:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1707005;
}
else 
{
goto label_1706974;
}
}
else 
{
label_1706974:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1706983;
}
else 
{
label_1706983:; 
goto label_1707005;
}
}
else 
{
label_1707005:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1707012;
}
}
else 
{
label_1707012:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1707021;
}
else 
{
label_1707021:; 
return 1;
}
}
}
}
}
}
}
}
}
label_1845185:; 
label_1845674:; 
if (((int)m_run_st) == 0)
{
goto label_1845687;
}
else 
{
label_1845687:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_1846750;
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
if (((int)m_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type;
req_a = req_t_a;
req_d = req_t_d;
rsp_type = rsp_t_type;
rsp_status = rsp_t_status;
rsp_d = rsp_t_d;
d = d_t;
a = a_t;
rsp_type = c_rsp_type;
rsp_status = c_rsp_status;
rsp_d = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1845875;
}
else 
{
goto label_1845816;
}
}
else 
{
label_1845816:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1845875;
}
else 
{
goto label_1845823;
}
}
else 
{
label_1845823:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1845875;
}
else 
{
goto label_1845830;
}
}
else 
{
label_1845830:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1845875;
}
else 
{
goto label_1845837;
}
}
else 
{
label_1845837:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1845875;
}
else 
{
goto label_1845844;
}
}
else 
{
label_1845844:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1845853;
}
else 
{
label_1845853:; 
goto label_1845875;
}
}
else 
{
label_1845875:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1845897;
}
else 
{
goto label_1845882;
}
}
else 
{
label_1845882:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1845891;
}
else 
{
label_1845891:; 
goto label_1845897;
}
}
else 
{
label_1845897:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1845970;
}
else 
{
goto label_1845911;
}
}
else 
{
label_1845911:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1845970;
}
else 
{
goto label_1845918;
}
}
else 
{
label_1845918:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1845970;
}
else 
{
goto label_1845925;
}
}
else 
{
label_1845925:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1845970;
}
else 
{
goto label_1845932;
}
}
else 
{
label_1845932:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1845970;
}
else 
{
goto label_1845939;
}
}
else 
{
label_1845939:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1845948;
}
else 
{
label_1845948:; 
goto label_1845970;
}
}
else 
{
label_1845970:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1845992;
}
else 
{
goto label_1845977;
}
}
else 
{
label_1845977:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1845986;
}
else 
{
label_1845986:; 
goto label_1845992;
}
}
else 
{
label_1845992:; 
c_m_ev = 2;
a = a + 1;
if (a < 1)
{
req_type = 1;
req_a = a;
req_d = a + 50;
c_m_lock = 1;
c_req_type = req_type;
c_req_a = req_a;
c_req_d = req_d;
c_empty_req = 0;
c_write_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1846457;
}
else 
{
goto label_1846398;
}
}
else 
{
label_1846398:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1846457;
}
else 
{
goto label_1846405;
}
}
else 
{
label_1846405:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1846457;
}
else 
{
goto label_1846412;
}
}
else 
{
label_1846412:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1846457;
}
else 
{
goto label_1846419;
}
}
else 
{
label_1846419:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1846457;
}
else 
{
goto label_1846426;
}
}
else 
{
label_1846426:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1846435;
}
else 
{
label_1846435:; 
goto label_1846457;
}
}
else 
{
label_1846457:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1846479;
}
else 
{
goto label_1846464;
}
}
else 
{
label_1846464:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1846473;
}
else 
{
label_1846473:; 
goto label_1846479;
}
}
else 
{
label_1846479:; 
c_write_req_ev = 2;
if (((int)c_empty_rsp) == 1)
{
m_run_st = 2;
m_run_pc = 3;
req_t_type = req_type;
req_t_a = req_a;
req_t_d = req_d;
rsp_t_type = rsp_type;
rsp_t_status = rsp_status;
rsp_t_d = rsp_d;
d_t = d;
a_t = a;
}
else 
{
rsp_type = c_rsp_type;
rsp_status = c_rsp_status;
rsp_d = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1846559;
}
else 
{
goto label_1846500;
}
}
else 
{
label_1846500:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1846559;
}
else 
{
goto label_1846507;
}
}
else 
{
label_1846507:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1846559;
}
else 
{
goto label_1846514;
}
}
else 
{
label_1846514:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1846559;
}
else 
{
goto label_1846521;
}
}
else 
{
label_1846521:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1846559;
}
else 
{
goto label_1846528;
}
}
else 
{
label_1846528:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1846537;
}
else 
{
label_1846537:; 
goto label_1846559;
}
}
else 
{
label_1846559:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1846581;
}
else 
{
goto label_1846566;
}
}
else 
{
label_1846566:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1846575;
}
else 
{
label_1846575:; 
goto label_1846581;
}
}
else 
{
label_1846581:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1846654;
}
else 
{
goto label_1846595;
}
}
else 
{
label_1846595:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1846654;
}
else 
{
goto label_1846602;
}
}
else 
{
label_1846602:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1846654;
}
else 
{
goto label_1846609;
}
}
else 
{
label_1846609:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1846654;
}
else 
{
goto label_1846616;
}
}
else 
{
label_1846616:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1846654;
}
else 
{
goto label_1846623;
}
}
else 
{
label_1846623:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1846632;
}
else 
{
label_1846632:; 
goto label_1846654;
}
}
else 
{
label_1846654:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1846661;
}
}
else 
{
label_1846661:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1846670;
}
else 
{
label_1846670:; 
return 1;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1847861;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1846893;
}
else 
{
goto label_1846834;
}
}
else 
{
label_1846834:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1846893;
}
else 
{
goto label_1846841;
}
}
else 
{
label_1846841:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1846893;
}
else 
{
goto label_1846848;
}
}
else 
{
label_1846848:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1846893;
}
else 
{
goto label_1846855;
}
}
else 
{
label_1846855:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1846893;
}
else 
{
goto label_1846862;
}
}
else 
{
label_1846862:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1846871;
}
else 
{
label_1846871:; 
goto label_1846893;
}
}
else 
{
label_1846893:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1846915;
}
else 
{
goto label_1846900;
}
}
else 
{
label_1846900:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1846909;
}
else 
{
label_1846909:; 
goto label_1846915;
}
}
else 
{
label_1846915:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_17 = req_a;
int i = __tmp_17;
int x;
if (i == 0)
{
x = s_memory0;
 __return_1846965 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_1846966 = x;
}
rsp_d = __return_1846965;
goto label_1846968;
rsp_d = __return_1846966;
label_1846968:; 
rsp_status = 1;
label_1846974:; 
c_rsp_type = rsp_type;
c_rsp_status = rsp_status;
c_rsp_d = rsp_d;
c_empty_rsp = 0;
c_write_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1847055;
}
else 
{
goto label_1846996;
}
}
else 
{
label_1846996:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1847055;
}
else 
{
goto label_1847003;
}
}
else 
{
label_1847003:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1847055;
}
else 
{
goto label_1847010;
}
}
else 
{
label_1847010:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1847055;
}
else 
{
goto label_1847017;
}
}
else 
{
label_1847017:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1847055;
}
else 
{
goto label_1847024;
}
}
else 
{
label_1847024:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1847033;
}
else 
{
label_1847033:; 
goto label_1847055;
}
}
else 
{
label_1847055:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1847077;
}
else 
{
goto label_1847062;
}
}
else 
{
label_1847062:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1847071;
}
else 
{
label_1847071:; 
goto label_1847077;
}
}
else 
{
label_1847077:; 
c_write_rsp_ev = 2;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1847160;
}
else 
{
goto label_1847101;
}
}
else 
{
label_1847101:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1847160;
}
else 
{
goto label_1847108;
}
}
else 
{
label_1847108:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1847160;
}
else 
{
goto label_1847115;
}
}
else 
{
label_1847115:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1847160;
}
else 
{
goto label_1847122;
}
}
else 
{
label_1847122:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1847160;
}
else 
{
goto label_1847129;
}
}
else 
{
label_1847129:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1847138;
}
else 
{
label_1847138:; 
goto label_1847160;
}
}
else 
{
label_1847160:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1847167;
}
}
else 
{
label_1847167:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1847176;
}
else 
{
label_1847176:; 
return 1;
}
}
}
}
}
}
}
}
}
goto label_1845185;
}
}
}
}
}
}
}
}
}
}
else 
{
if (((int)req_type) == 1)
{
{
int __tmp_18 = req_a;
int __tmp_19 = req_d;
int i = __tmp_18;
int v = __tmp_19;
if (i == 0)
{
s_memory0 = v;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_1846943;
label_1846943:; 
rsp_status = 1;
goto label_1846974;
}
}
else 
{
rsp_status = 0;
goto label_1846974;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
else 
{
label_1847861:; 
label_1861575:; 
if (((int)m_run_st) == 0)
{
goto label_1861588;
}
else 
{
label_1861588:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_1861599;
}
else 
{
label_1861599:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1862066;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1861726;
}
else 
{
goto label_1861667;
}
}
else 
{
label_1861667:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1861726;
}
else 
{
goto label_1861674;
}
}
else 
{
label_1861674:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1861726;
}
else 
{
goto label_1861681;
}
}
else 
{
label_1861681:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1861726;
}
else 
{
goto label_1861688;
}
}
else 
{
label_1861688:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1861726;
}
else 
{
goto label_1861695;
}
}
else 
{
label_1861695:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1861704;
}
else 
{
label_1861704:; 
goto label_1861726;
}
}
else 
{
label_1861726:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1861748;
}
else 
{
goto label_1861733;
}
}
else 
{
label_1861733:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1861742;
}
else 
{
label_1861742:; 
goto label_1861748;
}
}
else 
{
label_1861748:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_20 = req_a;
int i = __tmp_20;
int x;
if (i == 0)
{
x = s_memory0;
 __return_1861798 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_1861799 = x;
}
rsp_d = __return_1861798;
goto label_1861801;
rsp_d = __return_1861799;
label_1861801:; 
rsp_status = 1;
label_1861807:; 
c_rsp_type = rsp_type;
c_rsp_status = rsp_status;
c_rsp_d = rsp_d;
c_empty_rsp = 0;
c_write_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1861888;
}
else 
{
goto label_1861829;
}
}
else 
{
label_1861829:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1861888;
}
else 
{
goto label_1861836;
}
}
else 
{
label_1861836:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1861888;
}
else 
{
goto label_1861843;
}
}
else 
{
label_1861843:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1861888;
}
else 
{
goto label_1861850;
}
}
else 
{
label_1861850:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1861888;
}
else 
{
goto label_1861857;
}
}
else 
{
label_1861857:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1861866;
}
else 
{
label_1861866:; 
goto label_1861888;
}
}
else 
{
label_1861888:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1861910;
}
else 
{
goto label_1861895;
}
}
else 
{
label_1861895:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1861904;
}
else 
{
label_1861904:; 
goto label_1861910;
}
}
else 
{
label_1861910:; 
c_write_rsp_ev = 2;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1861993;
}
else 
{
goto label_1861934;
}
}
else 
{
label_1861934:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1861993;
}
else 
{
goto label_1861941;
}
}
else 
{
label_1861941:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1861993;
}
else 
{
goto label_1861948;
}
}
else 
{
label_1861948:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1861993;
}
else 
{
goto label_1861955;
}
}
else 
{
label_1861955:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1861993;
}
else 
{
goto label_1861962;
}
}
else 
{
label_1861962:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1861971;
}
else 
{
label_1861971:; 
goto label_1861993;
}
}
else 
{
label_1861993:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1862000;
}
}
else 
{
label_1862000:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1862009;
}
else 
{
label_1862009:; 
return 1;
}
}
}
}
}
}
}
}
}
goto label_1845185;
}
}
}
}
}
}
}
}
}
}
else 
{
if (((int)req_type) == 1)
{
{
int __tmp_21 = req_a;
int __tmp_22 = req_d;
int i = __tmp_21;
int v = __tmp_22;
if (i == 0)
{
s_memory0 = v;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_1861776;
label_1861776:; 
rsp_status = 1;
goto label_1861807;
}
}
else 
{
rsp_status = 0;
goto label_1861807;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
else 
{
label_1862066:; 
goto label_1861575;
}
}
}
}
}
}
}
}
}
}
}
}
}
else 
{
a = 0;
req_type___0 = 0;
req_a___0 = a;
c_m_lock = 1;
c_req_type = req_type___0;
c_req_a = req_a___0;
c_req_d = req_d___0;
c_empty_req = 0;
c_write_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1846103;
}
else 
{
goto label_1846044;
}
}
else 
{
label_1846044:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1846103;
}
else 
{
goto label_1846051;
}
}
else 
{
label_1846051:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1846103;
}
else 
{
goto label_1846058;
}
}
else 
{
label_1846058:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1846103;
}
else 
{
goto label_1846065;
}
}
else 
{
label_1846065:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1846103;
}
else 
{
goto label_1846072;
}
}
else 
{
label_1846072:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1846081;
}
else 
{
label_1846081:; 
goto label_1846103;
}
}
else 
{
label_1846103:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1846125;
}
else 
{
goto label_1846110;
}
}
else 
{
label_1846110:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1846119;
}
else 
{
label_1846119:; 
goto label_1846125;
}
}
else 
{
label_1846125:; 
c_write_req_ev = 2;
if (((int)c_empty_rsp) == 1)
{
m_run_st = 2;
m_run_pc = 6;
req_tt_type = req_type___0;
req_tt_a = req_a___0;
req_tt_d = req_d___0;
rsp_tt_type = rsp_type___0;
rsp_tt_status = rsp_status___0;
rsp_tt_d = rsp_d___0;
d_t = d;
a_t = a;
}
else 
{
rsp_type___0 = c_rsp_type;
rsp_status___0 = c_rsp_status;
rsp_d___0 = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1846205;
}
else 
{
goto label_1846146;
}
}
else 
{
label_1846146:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1846205;
}
else 
{
goto label_1846153;
}
}
else 
{
label_1846153:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1846205;
}
else 
{
goto label_1846160;
}
}
else 
{
label_1846160:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1846205;
}
else 
{
goto label_1846167;
}
}
else 
{
label_1846167:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1846205;
}
else 
{
goto label_1846174;
}
}
else 
{
label_1846174:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1846183;
}
else 
{
label_1846183:; 
goto label_1846205;
}
}
else 
{
label_1846205:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1846227;
}
else 
{
goto label_1846212;
}
}
else 
{
label_1846212:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1846221;
}
else 
{
label_1846221:; 
goto label_1846227;
}
}
else 
{
label_1846227:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1846300;
}
else 
{
goto label_1846241;
}
}
else 
{
label_1846241:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1846300;
}
else 
{
goto label_1846248;
}
}
else 
{
label_1846248:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1846300;
}
else 
{
goto label_1846255;
}
}
else 
{
label_1846255:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1846300;
}
else 
{
goto label_1846262;
}
}
else 
{
label_1846262:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1846300;
}
else 
{
goto label_1846269;
}
}
else 
{
label_1846269:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1846278;
}
else 
{
label_1846278:; 
goto label_1846300;
}
}
else 
{
label_1846300:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1846307;
}
}
else 
{
label_1846307:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1846316;
}
else 
{
label_1846316:; 
return 1;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
label_1846738:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1847863;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1847350;
}
else 
{
goto label_1847291;
}
}
else 
{
label_1847291:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1847350;
}
else 
{
goto label_1847298;
}
}
else 
{
label_1847298:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1847350;
}
else 
{
goto label_1847305;
}
}
else 
{
label_1847305:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1847350;
}
else 
{
goto label_1847312;
}
}
else 
{
label_1847312:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1847350;
}
else 
{
goto label_1847319;
}
}
else 
{
label_1847319:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1847328;
}
else 
{
label_1847328:; 
goto label_1847350;
}
}
else 
{
label_1847350:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1847372;
}
else 
{
goto label_1847357;
}
}
else 
{
label_1847357:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1847366;
}
else 
{
label_1847366:; 
goto label_1847372;
}
}
else 
{
label_1847372:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_23 = req_a;
int i = __tmp_23;
int x;
if (i == 0)
{
x = s_memory0;
 __return_1847422 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_1847423 = x;
}
rsp_d = __return_1847422;
goto label_1847425;
rsp_d = __return_1847423;
label_1847425:; 
rsp_status = 1;
label_1847431:; 
c_rsp_type = rsp_type;
c_rsp_status = rsp_status;
c_rsp_d = rsp_d;
c_empty_rsp = 0;
c_write_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1847512;
}
else 
{
goto label_1847453;
}
}
else 
{
label_1847453:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1847512;
}
else 
{
goto label_1847460;
}
}
else 
{
label_1847460:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1847512;
}
else 
{
goto label_1847467;
}
}
else 
{
label_1847467:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1847512;
}
else 
{
goto label_1847474;
}
}
else 
{
label_1847474:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1847512;
}
else 
{
goto label_1847481;
}
}
else 
{
label_1847481:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1847490;
}
else 
{
label_1847490:; 
goto label_1847512;
}
}
else 
{
label_1847512:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1847534;
}
else 
{
goto label_1847519;
}
}
else 
{
label_1847519:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1847528;
}
else 
{
label_1847528:; 
goto label_1847534;
}
}
else 
{
label_1847534:; 
c_write_rsp_ev = 2;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1847617;
}
else 
{
goto label_1847558;
}
}
else 
{
label_1847558:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1847617;
}
else 
{
goto label_1847565;
}
}
else 
{
label_1847565:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1847617;
}
else 
{
goto label_1847572;
}
}
else 
{
label_1847572:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1847617;
}
else 
{
goto label_1847579;
}
}
else 
{
label_1847579:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1847617;
}
else 
{
goto label_1847586;
}
}
else 
{
label_1847586:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1847595;
}
else 
{
label_1847595:; 
goto label_1847617;
}
}
else 
{
label_1847617:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1847624;
}
}
else 
{
label_1847624:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1847633;
}
else 
{
label_1847633:; 
return 1;
}
}
}
}
}
}
}
}
}
goto label_1922467;
}
}
}
}
}
}
}
}
}
}
else 
{
if (((int)req_type) == 1)
{
{
int __tmp_24 = req_a;
int __tmp_25 = req_d;
int i = __tmp_24;
int v = __tmp_25;
if (i == 0)
{
s_memory0 = v;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_1847400;
label_1847400:; 
rsp_status = 1;
goto label_1847431;
}
}
else 
{
rsp_status = 0;
goto label_1847431;
}
}
}
}
}
}
}
}
}
}
}
label_1847686:; 
label_1860872:; 
if (((int)m_run_st) == 0)
{
goto label_1860886;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1860886:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_1860897;
}
else 
{
label_1860897:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1861073;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1861024;
}
else 
{
goto label_1860965;
}
}
else 
{
label_1860965:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1861024;
}
else 
{
goto label_1860972;
}
}
else 
{
label_1860972:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1861024;
}
else 
{
goto label_1860979;
}
}
else 
{
label_1860979:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1861024;
}
else 
{
goto label_1860986;
}
}
else 
{
label_1860986:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1861024;
}
else 
{
goto label_1860993;
}
}
else 
{
label_1860993:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1861002;
}
else 
{
label_1861002:; 
goto label_1861024;
}
}
else 
{
label_1861024:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1861031;
}
}
else 
{
label_1861031:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1861040;
}
else 
{
label_1861040:; 
return 1;
}
}
}
}
}
}
}
}
}
goto label_1847686;
}
}
}
}
else 
{
label_1861073:; 
goto label_1860872;
}
}
}
else 
{
}
label_1933156:; 
kernel_st = 2;
kernel_st = 3;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1941410;
}
else 
{
goto label_1941292;
}
}
else 
{
label_1941292:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1941410;
}
else 
{
goto label_1941308;
}
}
else 
{
label_1941308:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1941410;
}
else 
{
goto label_1941320;
}
}
else 
{
label_1941320:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1941410;
}
else 
{
goto label_1941336;
}
}
else 
{
label_1941336:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1941410;
}
else 
{
goto label_1941348;
}
}
else 
{
label_1941348:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1941366;
}
else 
{
label_1941366:; 
goto label_1941410;
}
}
else 
{
label_1941410:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1941456;
}
else 
{
goto label_1941426;
}
}
else 
{
label_1941426:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1941444;
}
else 
{
label_1941444:; 
goto label_1941456;
}
}
else 
{
label_1941456:; 
if (((int)m_run_st) == 0)
{
goto label_1941482;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1941482:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
label_1941494:; 
if (((int)m_run_st) == 0)
{
goto label_1941508;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1941508:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_1941839;
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
if (((int)m_run_pc) == 0)
{
return 1;
}
else 
{
req_type___0 = req_tt_type;
req_a___0 = req_tt_a;
req_d___0 = req_tt_d;
rsp_type___0 = rsp_tt_type;
rsp_status___0 = rsp_tt_status;
rsp_d___0 = rsp_tt_d;
d = d_t;
a = a_t;
if (((int)c_empty_rsp) == 1)
{
m_run_st = 2;
m_run_pc = 6;
req_tt_type = req_type___0;
req_tt_a = req_a___0;
req_tt_d = req_d___0;
rsp_tt_type = rsp_type___0;
rsp_tt_status = rsp_status___0;
rsp_tt_d = rsp_d___0;
d_t = d;
a_t = a;
}
else 
{
rsp_type___0 = c_rsp_type;
rsp_status___0 = c_rsp_status;
rsp_d___0 = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1941696;
}
else 
{
goto label_1941637;
}
}
else 
{
label_1941637:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1941696;
}
else 
{
goto label_1941644;
}
}
else 
{
label_1941644:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1941696;
}
else 
{
goto label_1941651;
}
}
else 
{
label_1941651:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1941696;
}
else 
{
goto label_1941658;
}
}
else 
{
label_1941658:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1941696;
}
else 
{
goto label_1941665;
}
}
else 
{
label_1941665:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1941674;
}
else 
{
label_1941674:; 
goto label_1941696;
}
}
else 
{
label_1941696:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1941718;
}
else 
{
goto label_1941703;
}
}
else 
{
label_1941703:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1941712;
}
else 
{
label_1941712:; 
goto label_1941718;
}
}
else 
{
label_1941718:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1941791;
}
else 
{
goto label_1941732;
}
}
else 
{
label_1941732:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1941791;
}
else 
{
goto label_1941739;
}
}
else 
{
label_1941739:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1941791;
}
else 
{
goto label_1941746;
}
}
else 
{
label_1941746:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1941791;
}
else 
{
goto label_1941753;
}
}
else 
{
label_1941753:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1941791;
}
else 
{
goto label_1941760;
}
}
else 
{
label_1941760:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1941769;
}
else 
{
label_1941769:; 
goto label_1941791;
}
}
else 
{
label_1941791:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1941798;
}
}
else 
{
label_1941798:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1941807;
}
else 
{
label_1941807:; 
return 1;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1942192;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1941974;
}
else 
{
goto label_1941915;
}
}
else 
{
label_1941915:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1941974;
}
else 
{
goto label_1941922;
}
}
else 
{
label_1941922:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1941974;
}
else 
{
goto label_1941929;
}
}
else 
{
label_1941929:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1941974;
}
else 
{
goto label_1941936;
}
}
else 
{
label_1941936:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1941974;
}
else 
{
goto label_1941943;
}
}
else 
{
label_1941943:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1941952;
}
else 
{
label_1941952:; 
goto label_1941974;
}
}
else 
{
label_1941974:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1941981;
}
}
else 
{
label_1941981:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1941990;
}
else 
{
label_1941990:; 
return 1;
}
}
}
}
}
}
}
}
}
goto label_1941494;
}
}
}
}
else 
{
label_1942192:; 
goto label_1941494;
}
}
}
}
}
else 
{
label_1941839:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1942194;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1942140;
}
else 
{
goto label_1942081;
}
}
else 
{
label_1942081:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1942140;
}
else 
{
goto label_1942088;
}
}
else 
{
label_1942088:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1942140;
}
else 
{
goto label_1942095;
}
}
else 
{
label_1942095:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1942140;
}
else 
{
goto label_1942102;
}
}
else 
{
label_1942102:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1942140;
}
else 
{
goto label_1942109;
}
}
else 
{
label_1942109:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1942118;
}
else 
{
label_1942118:; 
goto label_1942140;
}
}
else 
{
label_1942140:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1942147;
}
}
else 
{
label_1942147:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1942156;
}
else 
{
label_1942156:; 
return 1;
}
}
}
}
}
}
}
}
}
goto label_1941494;
}
}
}
}
else 
{
label_1942194:; 
goto label_1941494;
}
}
}
else 
{
}
goto label_1933156;
}
}
}
else 
{
}
label_1941474:; 
__retres1 = 0;
 __return_1950344 = __retres1;
return 1;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
else 
{
label_1847863:; 
label_1861079:; 
if (((int)m_run_st) == 0)
{
goto label_1861092;
}
else 
{
label_1861092:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_1861103;
}
else 
{
label_1861103:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1861571;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1861230;
}
else 
{
goto label_1861171;
}
}
else 
{
label_1861171:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1861230;
}
else 
{
goto label_1861178;
}
}
else 
{
label_1861178:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1861230;
}
else 
{
goto label_1861185;
}
}
else 
{
label_1861185:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1861230;
}
else 
{
goto label_1861192;
}
}
else 
{
label_1861192:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1861230;
}
else 
{
goto label_1861199;
}
}
else 
{
label_1861199:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1861208;
}
else 
{
label_1861208:; 
goto label_1861230;
}
}
else 
{
label_1861230:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1861252;
}
else 
{
goto label_1861237;
}
}
else 
{
label_1861237:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1861246;
}
else 
{
label_1861246:; 
goto label_1861252;
}
}
else 
{
label_1861252:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_26 = req_a;
int i = __tmp_26;
int x;
if (i == 0)
{
x = s_memory0;
 __return_1861302 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_1861303 = x;
}
rsp_d = __return_1861302;
goto label_1861305;
rsp_d = __return_1861303;
label_1861305:; 
rsp_status = 1;
label_1861311:; 
c_rsp_type = rsp_type;
c_rsp_status = rsp_status;
c_rsp_d = rsp_d;
c_empty_rsp = 0;
c_write_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1861392;
}
else 
{
goto label_1861333;
}
}
else 
{
label_1861333:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1861392;
}
else 
{
goto label_1861340;
}
}
else 
{
label_1861340:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1861392;
}
else 
{
goto label_1861347;
}
}
else 
{
label_1861347:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1861392;
}
else 
{
goto label_1861354;
}
}
else 
{
label_1861354:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1861392;
}
else 
{
goto label_1861361;
}
}
else 
{
label_1861361:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1861370;
}
else 
{
label_1861370:; 
goto label_1861392;
}
}
else 
{
label_1861392:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1861414;
}
else 
{
goto label_1861399;
}
}
else 
{
label_1861399:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1861408;
}
else 
{
label_1861408:; 
goto label_1861414;
}
}
else 
{
label_1861414:; 
c_write_rsp_ev = 2;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1861497;
}
else 
{
goto label_1861438;
}
}
else 
{
label_1861438:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1861497;
}
else 
{
goto label_1861445;
}
}
else 
{
label_1861445:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1861497;
}
else 
{
goto label_1861452;
}
}
else 
{
label_1861452:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1861497;
}
else 
{
goto label_1861459;
}
}
else 
{
label_1861459:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1861497;
}
else 
{
goto label_1861466;
}
}
else 
{
label_1861466:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1861475;
}
else 
{
label_1861475:; 
goto label_1861497;
}
}
else 
{
label_1861497:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1861504;
}
}
else 
{
label_1861504:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1861513;
}
else 
{
label_1861513:; 
return 1;
}
}
}
}
}
}
}
}
}
label_1922467:; 
label_1922473:; 
if (((int)m_run_st) == 0)
{
goto label_1922486;
}
else 
{
label_1922486:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_1923200;
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
if (((int)m_run_pc) == 0)
{
return 1;
}
else 
{
req_type___0 = req_tt_type;
req_a___0 = req_tt_a;
req_d___0 = req_tt_d;
rsp_type___0 = rsp_tt_type;
rsp_status___0 = rsp_tt_status;
rsp_d___0 = rsp_tt_d;
d = d_t;
a = a_t;
rsp_type___0 = c_rsp_type;
rsp_status___0 = c_rsp_status;
rsp_d___0 = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1922674;
}
else 
{
goto label_1922615;
}
}
else 
{
label_1922615:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1922674;
}
else 
{
goto label_1922622;
}
}
else 
{
label_1922622:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1922674;
}
else 
{
goto label_1922629;
}
}
else 
{
label_1922629:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1922674;
}
else 
{
goto label_1922636;
}
}
else 
{
label_1922636:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1922674;
}
else 
{
goto label_1922643;
}
}
else 
{
label_1922643:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1922652;
}
else 
{
label_1922652:; 
goto label_1922674;
}
}
else 
{
label_1922674:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1922696;
}
else 
{
goto label_1922681;
}
}
else 
{
label_1922681:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1922690;
}
else 
{
label_1922690:; 
goto label_1922696;
}
}
else 
{
label_1922696:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1922769;
}
else 
{
goto label_1922710;
}
}
else 
{
label_1922710:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1922769;
}
else 
{
goto label_1922717;
}
}
else 
{
label_1922717:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1922769;
}
else 
{
goto label_1922724;
}
}
else 
{
label_1922724:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1922769;
}
else 
{
goto label_1922731;
}
}
else 
{
label_1922731:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1922769;
}
else 
{
goto label_1922738;
}
}
else 
{
label_1922738:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1922747;
}
else 
{
label_1922747:; 
goto label_1922769;
}
}
else 
{
label_1922769:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1922791;
}
else 
{
goto label_1922776;
}
}
else 
{
label_1922776:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1922785;
}
else 
{
label_1922785:; 
goto label_1922791;
}
}
else 
{
label_1922791:; 
c_m_ev = 2;
if ((req_a___0 + 50) == rsp_d___0)
{
a = a + 1;
goto label_1922808;
}
else 
{
{
__VERIFIER_error();
}
a = a + 1;
label_1922808:; 
if (a < 1)
{
req_type___0 = 0;
req_a___0 = a;
if (c_m_lock == 1)
{
m_run_st = 2;
m_run_pc = 4;
req_tt_type = req_type___0;
req_tt_a = req_a___0;
req_tt_d = req_d___0;
rsp_tt_type = rsp_type___0;
rsp_tt_status = rsp_status___0;
rsp_tt_d = rsp_d___0;
d_t = d;
a_t = a;
}
else 
{
c_m_lock = 1;
c_req_type = req_type___0;
c_req_a = req_a___0;
c_req_d = req_d___0;
c_empty_req = 0;
c_write_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1922906;
}
else 
{
goto label_1922847;
}
}
else 
{
label_1922847:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1922906;
}
else 
{
goto label_1922854;
}
}
else 
{
label_1922854:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1922906;
}
else 
{
goto label_1922861;
}
}
else 
{
label_1922861:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1922906;
}
else 
{
goto label_1922868;
}
}
else 
{
label_1922868:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1922906;
}
else 
{
goto label_1922875;
}
}
else 
{
label_1922875:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1922884;
}
else 
{
label_1922884:; 
goto label_1922906;
}
}
else 
{
label_1922906:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1922928;
}
else 
{
goto label_1922913;
}
}
else 
{
label_1922913:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1922922;
}
else 
{
label_1922922:; 
goto label_1922928;
}
}
else 
{
label_1922928:; 
c_write_req_ev = 2;
if (((int)c_empty_rsp) == 1)
{
m_run_st = 2;
m_run_pc = 6;
req_tt_type = req_type___0;
req_tt_a = req_a___0;
req_tt_d = req_d___0;
rsp_tt_type = rsp_type___0;
rsp_tt_status = rsp_status___0;
rsp_tt_d = rsp_d___0;
d_t = d;
a_t = a;
}
else 
{
rsp_type___0 = c_rsp_type;
rsp_status___0 = c_rsp_status;
rsp_d___0 = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1923008;
}
else 
{
goto label_1922949;
}
}
else 
{
label_1922949:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1923008;
}
else 
{
goto label_1922956;
}
}
else 
{
label_1922956:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1923008;
}
else 
{
goto label_1922963;
}
}
else 
{
label_1922963:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1923008;
}
else 
{
goto label_1922970;
}
}
else 
{
label_1922970:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1923008;
}
else 
{
goto label_1922977;
}
}
else 
{
label_1922977:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1922986;
}
else 
{
label_1922986:; 
goto label_1923008;
}
}
else 
{
label_1923008:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1923030;
}
else 
{
goto label_1923015;
}
}
else 
{
label_1923015:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1923024;
}
else 
{
label_1923024:; 
goto label_1923030;
}
}
else 
{
label_1923030:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1923103;
}
else 
{
goto label_1923044;
}
}
else 
{
label_1923044:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1923103;
}
else 
{
goto label_1923051;
}
}
else 
{
label_1923051:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1923103;
}
else 
{
goto label_1923058;
}
}
else 
{
label_1923058:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1923103;
}
else 
{
goto label_1923065;
}
}
else 
{
label_1923065:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1923103;
}
else 
{
goto label_1923072;
}
}
else 
{
label_1923072:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1923081;
}
else 
{
label_1923081:; 
goto label_1923103;
}
}
else 
{
label_1923103:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1923110;
}
}
else 
{
label_1923110:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1923119;
}
else 
{
label_1923119:; 
return 1;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
goto label_1846738;
}
}
}
}
}
}
}
}
}
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1923728;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1923343;
}
else 
{
goto label_1923284;
}
}
else 
{
label_1923284:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1923343;
}
else 
{
goto label_1923291;
}
}
else 
{
label_1923291:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1923343;
}
else 
{
goto label_1923298;
}
}
else 
{
label_1923298:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1923343;
}
else 
{
goto label_1923305;
}
}
else 
{
label_1923305:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1923343;
}
else 
{
goto label_1923312;
}
}
else 
{
label_1923312:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1923321;
}
else 
{
label_1923321:; 
goto label_1923343;
}
}
else 
{
label_1923343:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1923350;
}
}
else 
{
label_1923350:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1923359;
}
else 
{
label_1923359:; 
return 1;
}
}
}
}
}
}
}
}
}
label_1923389:; 
label_1924163:; 
if (((int)m_run_st) == 0)
{
goto label_1924177;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1924177:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_1924188;
}
else 
{
label_1924188:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1924364;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1924315;
}
else 
{
goto label_1924256;
}
}
else 
{
label_1924256:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1924315;
}
else 
{
goto label_1924263;
}
}
else 
{
label_1924263:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1924315;
}
else 
{
goto label_1924270;
}
}
else 
{
label_1924270:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1924315;
}
else 
{
goto label_1924277;
}
}
else 
{
label_1924277:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1924315;
}
else 
{
goto label_1924284;
}
}
else 
{
label_1924284:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1924293;
}
else 
{
label_1924293:; 
goto label_1924315;
}
}
else 
{
label_1924315:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1924322;
}
}
else 
{
label_1924322:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1924331;
}
else 
{
label_1924331:; 
return 1;
}
}
}
}
}
}
}
}
}
goto label_1923389;
}
}
}
}
else 
{
label_1924364:; 
goto label_1924163;
}
}
}
else 
{
}
label_1949316:; 
kernel_st = 2;
kernel_st = 3;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1949773;
}
else 
{
goto label_1949714;
}
}
else 
{
label_1949714:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1949773;
}
else 
{
goto label_1949721;
}
}
else 
{
label_1949721:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1949773;
}
else 
{
goto label_1949728;
}
}
else 
{
label_1949728:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1949773;
}
else 
{
goto label_1949735;
}
}
else 
{
label_1949735:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1949773;
}
else 
{
goto label_1949742;
}
}
else 
{
label_1949742:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1949751;
}
else 
{
label_1949751:; 
goto label_1949773;
}
}
else 
{
label_1949773:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1949795;
}
else 
{
goto label_1949780;
}
}
else 
{
label_1949780:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1949789;
}
else 
{
label_1949789:; 
goto label_1949795;
}
}
else 
{
label_1949795:; 
if (((int)m_run_st) == 0)
{
goto label_1949807;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1949807:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
label_1949819:; 
if (((int)m_run_st) == 0)
{
goto label_1949833;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1949833:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_1949971;
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
if (((int)m_run_pc) == 0)
{
return 1;
}
else 
{
req_type___0 = req_tt_type;
req_a___0 = req_tt_a;
req_d___0 = req_tt_d;
rsp_type___0 = rsp_tt_type;
rsp_status___0 = rsp_tt_status;
rsp_d___0 = rsp_tt_d;
d = d_t;
a = a_t;
m_run_st = 2;
m_run_pc = 4;
req_tt_type = req_type___0;
req_tt_a = req_a___0;
req_tt_d = req_d___0;
rsp_tt_type = rsp_type___0;
rsp_tt_status = rsp_status___0;
rsp_tt_d = rsp_d___0;
d_t = d;
a_t = a;
}
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1950324;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1950106;
}
else 
{
goto label_1950047;
}
}
else 
{
label_1950047:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1950106;
}
else 
{
goto label_1950054;
}
}
else 
{
label_1950054:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1950106;
}
else 
{
goto label_1950061;
}
}
else 
{
label_1950061:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1950106;
}
else 
{
goto label_1950068;
}
}
else 
{
label_1950068:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1950106;
}
else 
{
goto label_1950075;
}
}
else 
{
label_1950075:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1950084;
}
else 
{
label_1950084:; 
goto label_1950106;
}
}
else 
{
label_1950106:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1950113;
}
}
else 
{
label_1950113:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1950122;
}
else 
{
label_1950122:; 
return 1;
}
}
}
}
}
}
}
}
}
goto label_1949819;
}
}
}
}
else 
{
label_1950324:; 
goto label_1949819;
}
}
}
}
else 
{
label_1949971:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1950326;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1950272;
}
else 
{
goto label_1950213;
}
}
else 
{
label_1950213:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1950272;
}
else 
{
goto label_1950220;
}
}
else 
{
label_1950220:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1950272;
}
else 
{
goto label_1950227;
}
}
else 
{
label_1950227:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1950272;
}
else 
{
goto label_1950234;
}
}
else 
{
label_1950234:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1950272;
}
else 
{
goto label_1950241;
}
}
else 
{
label_1950241:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1950250;
}
else 
{
label_1950250:; 
goto label_1950272;
}
}
else 
{
label_1950272:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1950279;
}
}
else 
{
label_1950279:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1950288;
}
else 
{
label_1950288:; 
return 1;
}
}
}
}
}
}
}
}
}
goto label_1949819;
}
}
}
}
else 
{
label_1950326:; 
goto label_1949819;
}
}
}
else 
{
}
goto label_1949316;
}
}
}
else 
{
}
goto label_1667328;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
else 
{
label_1923728:; 
goto label_1924163;
}
}
else 
{
}
label_1923189:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1923730;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1923509;
}
else 
{
goto label_1923450;
}
}
else 
{
label_1923450:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1923509;
}
else 
{
goto label_1923457;
}
}
else 
{
label_1923457:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1923509;
}
else 
{
goto label_1923464;
}
}
else 
{
label_1923464:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1923509;
}
else 
{
goto label_1923471;
}
}
else 
{
label_1923471:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1923509;
}
else 
{
goto label_1923478;
}
}
else 
{
label_1923478:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1923487;
}
else 
{
label_1923487:; 
goto label_1923509;
}
}
else 
{
label_1923509:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1923516;
}
}
else 
{
label_1923516:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1923525;
}
else 
{
label_1923525:; 
return 1;
}
}
}
}
}
}
}
}
}
label_1923555:; 
label_1923736:; 
if (((int)m_run_st) == 0)
{
goto label_1923750;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1923750:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_1923983;
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
if (((int)m_run_pc) == 0)
{
return 1;
}
else 
{
req_type___0 = req_tt_type;
req_a___0 = req_tt_a;
req_d___0 = req_tt_d;
rsp_type___0 = rsp_tt_type;
rsp_status___0 = rsp_tt_status;
rsp_d___0 = rsp_tt_d;
d = d_t;
a = a_t;
if (((int)c_empty_rsp) == 1)
{
m_run_st = 2;
m_run_pc = 6;
req_tt_type = req_type___0;
req_tt_a = req_a___0;
req_tt_d = req_d___0;
rsp_tt_type = rsp_type___0;
rsp_tt_status = rsp_status___0;
rsp_tt_d = rsp_d___0;
d_t = d;
a_t = a;
}
else 
{
rsp_type___0 = c_rsp_type;
rsp_status___0 = c_rsp_status;
rsp_d___0 = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1923938;
}
else 
{
goto label_1923879;
}
}
else 
{
label_1923879:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1923938;
}
else 
{
goto label_1923886;
}
}
else 
{
label_1923886:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1923938;
}
else 
{
goto label_1923893;
}
}
else 
{
label_1923893:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1923938;
}
else 
{
goto label_1923900;
}
}
else 
{
label_1923900:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1923938;
}
else 
{
goto label_1923907;
}
}
else 
{
label_1923907:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1923916;
}
else 
{
label_1923916:; 
goto label_1923938;
}
}
else 
{
label_1923938:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1923945;
}
}
else 
{
label_1923945:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1923954;
}
else 
{
label_1923954:; 
return 1;
}
}
}
}
}
}
}
}
}
goto label_1923189;
}
}
}
}
else 
{
label_1923983:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1924159;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1924110;
}
else 
{
goto label_1924051;
}
}
else 
{
label_1924051:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1924110;
}
else 
{
goto label_1924058;
}
}
else 
{
label_1924058:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1924110;
}
else 
{
goto label_1924065;
}
}
else 
{
label_1924065:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1924110;
}
else 
{
goto label_1924072;
}
}
else 
{
label_1924072:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1924110;
}
else 
{
goto label_1924079;
}
}
else 
{
label_1924079:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1924088;
}
else 
{
label_1924088:; 
goto label_1924110;
}
}
else 
{
label_1924110:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1924117;
}
}
else 
{
label_1924117:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1924126;
}
else 
{
label_1924126:; 
return 1;
}
}
}
}
}
}
}
}
}
goto label_1923555;
}
}
}
}
else 
{
label_1924159:; 
goto label_1923736;
}
}
}
else 
{
}
label_1933154:; 
kernel_st = 2;
kernel_st = 3;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1941412;
}
else 
{
goto label_1941294;
}
}
else 
{
label_1941294:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1941412;
}
else 
{
goto label_1941306;
}
}
else 
{
label_1941306:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1941412;
}
else 
{
goto label_1941322;
}
}
else 
{
label_1941322:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1941412;
}
else 
{
goto label_1941334;
}
}
else 
{
label_1941334:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1941412;
}
else 
{
goto label_1941350;
}
}
else 
{
label_1941350:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1941368;
}
else 
{
label_1941368:; 
goto label_1941412;
}
}
else 
{
label_1941412:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1941454;
}
else 
{
goto label_1941424;
}
}
else 
{
label_1941424:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1941442;
}
else 
{
label_1941442:; 
goto label_1941454;
}
}
else 
{
label_1941454:; 
if (((int)m_run_st) == 0)
{
goto label_1941480;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1941480:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
label_1942216:; 
if (((int)m_run_st) == 0)
{
goto label_1942230;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1942230:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_1942464;
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
if (((int)m_run_pc) == 0)
{
return 1;
}
else 
{
req_type___0 = req_tt_type;
req_a___0 = req_tt_a;
req_d___0 = req_tt_d;
rsp_type___0 = rsp_tt_type;
rsp_status___0 = rsp_tt_status;
rsp_d___0 = rsp_tt_d;
d = d_t;
a = a_t;
if (((int)c_empty_rsp) == 1)
{
m_run_st = 2;
m_run_pc = 6;
req_tt_type = req_type___0;
req_tt_a = req_a___0;
req_tt_d = req_d___0;
rsp_tt_type = rsp_type___0;
rsp_tt_status = rsp_status___0;
rsp_tt_d = rsp_d___0;
d_t = d;
a_t = a;
}
else 
{
rsp_type___0 = c_rsp_type;
rsp_status___0 = c_rsp_status;
rsp_d___0 = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1942418;
}
else 
{
goto label_1942359;
}
}
else 
{
label_1942359:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1942418;
}
else 
{
goto label_1942366;
}
}
else 
{
label_1942366:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1942418;
}
else 
{
goto label_1942373;
}
}
else 
{
label_1942373:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1942418;
}
else 
{
goto label_1942380;
}
}
else 
{
label_1942380:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1942418;
}
else 
{
goto label_1942387;
}
}
else 
{
label_1942387:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1942396;
}
else 
{
label_1942396:; 
goto label_1942418;
}
}
else 
{
label_1942418:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1942425;
}
}
else 
{
label_1942425:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1942434;
}
else 
{
label_1942434:; 
return 1;
}
}
}
}
}
}
}
}
}
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1942817;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1942599;
}
else 
{
goto label_1942540;
}
}
else 
{
label_1942540:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1942599;
}
else 
{
goto label_1942547;
}
}
else 
{
label_1942547:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1942599;
}
else 
{
goto label_1942554;
}
}
else 
{
label_1942554:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1942599;
}
else 
{
goto label_1942561;
}
}
else 
{
label_1942561:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1942599;
}
else 
{
goto label_1942568;
}
}
else 
{
label_1942568:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1942577;
}
else 
{
label_1942577:; 
goto label_1942599;
}
}
else 
{
label_1942599:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1942606;
}
}
else 
{
label_1942606:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1942615;
}
else 
{
label_1942615:; 
return 1;
}
}
}
}
}
}
}
}
}
goto label_1942216;
}
}
}
}
else 
{
label_1942817:; 
goto label_1942216;
}
}
}
}
}
else 
{
label_1942464:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1942819;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1942765;
}
else 
{
goto label_1942706;
}
}
else 
{
label_1942706:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1942765;
}
else 
{
goto label_1942713;
}
}
else 
{
label_1942713:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1942765;
}
else 
{
goto label_1942720;
}
}
else 
{
label_1942720:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1942765;
}
else 
{
goto label_1942727;
}
}
else 
{
label_1942727:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1942765;
}
else 
{
goto label_1942734;
}
}
else 
{
label_1942734:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1942743;
}
else 
{
label_1942743:; 
goto label_1942765;
}
}
else 
{
label_1942765:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1942772;
}
}
else 
{
label_1942772:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1942781;
}
else 
{
label_1942781:; 
return 1;
}
}
}
}
}
}
}
}
}
goto label_1942216;
}
}
}
}
else 
{
label_1942819:; 
goto label_1942216;
}
}
}
else 
{
}
goto label_1933154;
}
}
}
else 
{
}
goto label_1667328;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
else 
{
label_1923730:; 
goto label_1923736;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
else 
{
label_1923200:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1923732;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1923675;
}
else 
{
goto label_1923616;
}
}
else 
{
label_1923616:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1923675;
}
else 
{
goto label_1923623;
}
}
else 
{
label_1923623:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1923675;
}
else 
{
goto label_1923630;
}
}
else 
{
label_1923630:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1923675;
}
else 
{
goto label_1923637;
}
}
else 
{
label_1923637:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1923675;
}
else 
{
goto label_1923644;
}
}
else 
{
label_1923644:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1923653;
}
else 
{
label_1923653:; 
goto label_1923675;
}
}
else 
{
label_1923675:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1923682;
}
}
else 
{
label_1923682:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1923691;
}
else 
{
label_1923691:; 
return 1;
}
}
}
}
}
}
}
}
}
goto label_1922467;
}
}
}
}
else 
{
label_1923732:; 
goto label_1922473;
}
}
}
}
}
}
}
}
}
}
}
}
}
else 
{
if (((int)req_type) == 1)
{
{
int __tmp_27 = req_a;
int __tmp_28 = req_d;
int i = __tmp_27;
int v = __tmp_28;
if (i == 0)
{
s_memory0 = v;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_1861280;
label_1861280:; 
rsp_status = 1;
goto label_1861311;
}
}
else 
{
rsp_status = 0;
goto label_1861311;
}
}
}
}
}
}
}
}
}
}
}
goto label_1847686;
}
}
}
}
else 
{
label_1861571:; 
goto label_1861079;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
else 
{
label_1846750:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1847865;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1847808;
}
else 
{
goto label_1847749;
}
}
else 
{
label_1847749:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1847808;
}
else 
{
goto label_1847756;
}
}
else 
{
label_1847756:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1847808;
}
else 
{
goto label_1847763;
}
}
else 
{
label_1847763:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1847808;
}
else 
{
goto label_1847770;
}
}
else 
{
label_1847770:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1847808;
}
else 
{
goto label_1847777;
}
}
else 
{
label_1847777:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1847786;
}
else 
{
label_1847786:; 
goto label_1847808;
}
}
else 
{
label_1847808:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1847815;
}
}
else 
{
label_1847815:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1847824;
}
else 
{
label_1847824:; 
return 1;
}
}
}
}
}
}
}
}
}
goto label_1845185;
}
}
}
}
else 
{
label_1847865:; 
goto label_1845674;
}
}
}
}
}
}
}
}
}
}
}
}
}
else 
{
if (((int)req_type) == 1)
{
{
int __tmp_29 = req_a;
int __tmp_30 = req_d;
int i = __tmp_29;
int v = __tmp_30;
if (i == 0)
{
s_memory0 = v;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_1706788;
label_1706788:; 
rsp_status = 1;
goto label_1706819;
}
}
else 
{
rsp_status = 0;
goto label_1706819;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
else 
{
label_1707082:; 
goto label_1705805;
}
}
}
else 
{
}
goto label_1705004;
}
}
}
else 
{
}
goto label_1941474;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1674751;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
goto label_1674619;
}
else 
{
label_1674619:; 
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1674701;
}
else 
{
goto label_1674642;
}
}
else 
{
label_1674642:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1674701;
}
else 
{
goto label_1674649;
}
}
else 
{
label_1674649:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1674701;
}
else 
{
goto label_1674656;
}
}
else 
{
label_1674656:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1674701;
}
else 
{
goto label_1674663;
}
}
else 
{
label_1674663:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1674701;
}
else 
{
goto label_1674670;
}
}
else 
{
label_1674670:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1674679;
}
else 
{
label_1674679:; 
goto label_1674701;
}
}
else 
{
label_1674701:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1674708;
}
}
else 
{
label_1674708:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1674717;
}
else 
{
label_1674717:; 
return 1;
}
}
}
}
}
}
}
}
}
goto label_1779487;
}
}
}
}
else 
{
label_1674751:; 
label_1674756:; 
if (((int)m_run_st) == 0)
{
goto label_1674770;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1674770:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_1674781;
}
else 
{
label_1674781:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1674957;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
goto label_1674826;
}
else 
{
label_1674826:; 
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1674908;
}
else 
{
goto label_1674849;
}
}
else 
{
label_1674849:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1674908;
}
else 
{
goto label_1674856;
}
}
else 
{
label_1674856:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1674908;
}
else 
{
goto label_1674863;
}
}
else 
{
label_1674863:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1674908;
}
else 
{
goto label_1674870;
}
}
else 
{
label_1674870:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1674908;
}
else 
{
goto label_1674877;
}
}
else 
{
label_1674877:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1674886;
}
else 
{
label_1674886:; 
goto label_1674908;
}
}
else 
{
label_1674908:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1674915;
}
}
else 
{
label_1674915:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1674924;
}
else 
{
label_1674924:; 
return 1;
}
}
}
}
}
}
}
}
}
label_1779487:; 
label_1779679:; 
if (((int)m_run_st) == 0)
{
goto label_1779693;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1779693:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_1779704;
}
else 
{
label_1779704:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1779880;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1779831;
}
else 
{
goto label_1779772;
}
}
else 
{
label_1779772:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1779831;
}
else 
{
goto label_1779779;
}
}
else 
{
label_1779779:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1779831;
}
else 
{
goto label_1779786;
}
}
else 
{
label_1779786:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1779831;
}
else 
{
goto label_1779793;
}
}
else 
{
label_1779793:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1779831;
}
else 
{
goto label_1779800;
}
}
else 
{
label_1779800:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1779809;
}
else 
{
label_1779809:; 
goto label_1779831;
}
}
else 
{
label_1779831:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1779838;
}
}
else 
{
label_1779838:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1779847;
}
else 
{
label_1779847:; 
return 1;
}
}
}
}
}
}
}
}
}
goto label_1779487;
}
}
}
}
else 
{
label_1779880:; 
goto label_1779679;
}
}
}
else 
{
}
goto label_1779497;
}
}
}
}
}
else 
{
label_1674957:; 
goto label_1674756;
}
}
}
else 
{
}
label_1674767:; 
kernel_st = 2;
kernel_st = 3;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1675027;
}
else 
{
goto label_1674968;
}
}
else 
{
label_1674968:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1675027;
}
else 
{
goto label_1674975;
}
}
else 
{
label_1674975:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1675027;
}
else 
{
goto label_1674982;
}
}
else 
{
label_1674982:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1675027;
}
else 
{
goto label_1674989;
}
}
else 
{
label_1674989:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1675027;
}
else 
{
goto label_1674996;
}
}
else 
{
label_1674996:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1675005;
}
else 
{
label_1675005:; 
goto label_1675027;
}
}
else 
{
label_1675027:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1675049;
}
else 
{
goto label_1675034;
}
}
else 
{
label_1675034:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1675043;
}
else 
{
label_1675043:; 
goto label_1675049;
}
}
else 
{
label_1675049:; 
if (((int)m_run_st) == 0)
{
goto label_1675061;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1675061:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
label_1675073:; 
if (((int)m_run_st) == 0)
{
goto label_1675087;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1675087:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_1675225;
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
if (((int)m_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type;
req_a = req_t_a;
req_d = req_t_d;
rsp_type = rsp_t_type;
rsp_status = rsp_t_status;
rsp_d = rsp_t_d;
d = d_t;
a = a_t;
m_run_st = 2;
m_run_pc = 1;
req_t_type = req_type;
req_t_a = req_a;
req_t_d = req_d;
rsp_t_type = rsp_type;
rsp_t_status = rsp_status;
rsp_t_d = rsp_d;
d_t = d;
a_t = a;
}
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1675578;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
goto label_1675278;
}
else 
{
label_1675278:; 
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1675360;
}
else 
{
goto label_1675301;
}
}
else 
{
label_1675301:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1675360;
}
else 
{
goto label_1675308;
}
}
else 
{
label_1675308:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1675360;
}
else 
{
goto label_1675315;
}
}
else 
{
label_1675315:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1675360;
}
else 
{
goto label_1675322;
}
}
else 
{
label_1675322:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1675360;
}
else 
{
goto label_1675329;
}
}
else 
{
label_1675329:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1675338;
}
else 
{
label_1675338:; 
goto label_1675360;
}
}
else 
{
label_1675360:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1675367;
}
}
else 
{
label_1675367:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1675376;
}
else 
{
label_1675376:; 
return 1;
}
}
}
}
}
}
}
}
}
label_1675406:; 
goto label_1675584;
}
}
}
}
else 
{
label_1675578:; 
goto label_1675073;
}
}
}
}
else 
{
label_1675225:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1675580;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
goto label_1675444;
}
else 
{
label_1675444:; 
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1675526;
}
else 
{
goto label_1675467;
}
}
else 
{
label_1675467:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1675526;
}
else 
{
goto label_1675474;
}
}
else 
{
label_1675474:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1675526;
}
else 
{
goto label_1675481;
}
}
else 
{
label_1675481:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1675526;
}
else 
{
goto label_1675488;
}
}
else 
{
label_1675488:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1675526;
}
else 
{
goto label_1675495;
}
}
else 
{
label_1675495:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1675504;
}
else 
{
label_1675504:; 
goto label_1675526;
}
}
else 
{
label_1675526:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1675533;
}
}
else 
{
label_1675533:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1675542;
}
else 
{
label_1675542:; 
return 1;
}
}
}
}
}
}
}
}
}
label_1675572:; 
label_1675584:; 
if (((int)m_run_st) == 0)
{
goto label_1675598;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1675598:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_1675736;
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
if (((int)m_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type;
req_a = req_t_a;
req_d = req_t_d;
rsp_type = rsp_t_type;
rsp_status = rsp_t_status;
rsp_d = rsp_t_d;
d = d_t;
a = a_t;
m_run_st = 2;
m_run_pc = 1;
req_t_type = req_type;
req_t_a = req_a;
req_t_d = req_d;
rsp_t_type = rsp_type;
rsp_t_status = rsp_status;
rsp_t_d = rsp_d;
d_t = d;
a_t = a;
}
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1676087;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1675871;
}
else 
{
goto label_1675812;
}
}
else 
{
label_1675812:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1675871;
}
else 
{
goto label_1675819;
}
}
else 
{
label_1675819:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1675871;
}
else 
{
goto label_1675826;
}
}
else 
{
label_1675826:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1675871;
}
else 
{
goto label_1675833;
}
}
else 
{
label_1675833:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1675871;
}
else 
{
goto label_1675840;
}
}
else 
{
label_1675840:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1675849;
}
else 
{
label_1675849:; 
goto label_1675871;
}
}
else 
{
label_1675871:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1675878;
}
}
else 
{
label_1675878:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1675887;
}
else 
{
label_1675887:; 
return 1;
}
}
}
}
}
}
}
}
}
goto label_1675406;
}
}
}
}
else 
{
label_1676087:; 
goto label_1675584;
}
}
}
}
else 
{
label_1675736:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1676089;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1676037;
}
else 
{
goto label_1675978;
}
}
else 
{
label_1675978:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1676037;
}
else 
{
goto label_1675985;
}
}
else 
{
label_1675985:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1676037;
}
else 
{
goto label_1675992;
}
}
else 
{
label_1675992:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1676037;
}
else 
{
goto label_1675999;
}
}
else 
{
label_1675999:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1676037;
}
else 
{
goto label_1676006;
}
}
else 
{
label_1676006:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1676015;
}
else 
{
label_1676015:; 
goto label_1676037;
}
}
else 
{
label_1676037:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1676044;
}
}
else 
{
label_1676044:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1676053;
}
else 
{
label_1676053:; 
return 1;
}
}
}
}
}
}
}
}
}
goto label_1675572;
}
}
}
}
else 
{
label_1676089:; 
goto label_1675584;
}
}
}
else 
{
}
label_1779497:; 
kernel_st = 2;
kernel_st = 3;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1780018;
}
else 
{
goto label_1779900;
}
}
else 
{
label_1779900:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1780018;
}
else 
{
goto label_1779916;
}
}
else 
{
label_1779916:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1780018;
}
else 
{
goto label_1779928;
}
}
else 
{
label_1779928:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1780018;
}
else 
{
goto label_1779944;
}
}
else 
{
label_1779944:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1780018;
}
else 
{
goto label_1779956;
}
}
else 
{
label_1779956:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1779974;
}
else 
{
label_1779974:; 
goto label_1780018;
}
}
else 
{
label_1780018:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1780064;
}
else 
{
goto label_1780034;
}
}
else 
{
label_1780034:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1780052;
}
else 
{
label_1780052:; 
goto label_1780064;
}
}
else 
{
label_1780064:; 
if (((int)m_run_st) == 0)
{
goto label_1780088;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1780088:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
label_1780100:; 
if (((int)m_run_st) == 0)
{
goto label_1780114;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1780114:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_1780252;
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
if (((int)m_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type;
req_a = req_t_a;
req_d = req_t_d;
rsp_type = rsp_t_type;
rsp_status = rsp_t_status;
rsp_d = rsp_t_d;
d = d_t;
a = a_t;
m_run_st = 2;
m_run_pc = 1;
req_t_type = req_type;
req_t_a = req_a;
req_t_d = req_d;
rsp_t_type = rsp_type;
rsp_t_status = rsp_status;
rsp_t_d = rsp_d;
d_t = d;
a_t = a;
}
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1780605;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1780387;
}
else 
{
goto label_1780328;
}
}
else 
{
label_1780328:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1780387;
}
else 
{
goto label_1780335;
}
}
else 
{
label_1780335:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1780387;
}
else 
{
goto label_1780342;
}
}
else 
{
label_1780342:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1780387;
}
else 
{
goto label_1780349;
}
}
else 
{
label_1780349:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1780387;
}
else 
{
goto label_1780356;
}
}
else 
{
label_1780356:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1780365;
}
else 
{
label_1780365:; 
goto label_1780387;
}
}
else 
{
label_1780387:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1780394;
}
}
else 
{
label_1780394:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1780403;
}
else 
{
label_1780403:; 
return 1;
}
}
}
}
}
}
}
}
}
goto label_1780100;
}
}
}
}
else 
{
label_1780605:; 
goto label_1780100;
}
}
}
}
else 
{
label_1780252:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1780607;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1780553;
}
else 
{
goto label_1780494;
}
}
else 
{
label_1780494:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1780553;
}
else 
{
goto label_1780501;
}
}
else 
{
label_1780501:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1780553;
}
else 
{
goto label_1780508;
}
}
else 
{
label_1780508:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1780553;
}
else 
{
goto label_1780515;
}
}
else 
{
label_1780515:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1780553;
}
else 
{
goto label_1780522;
}
}
else 
{
label_1780522:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1780531;
}
else 
{
label_1780531:; 
goto label_1780553;
}
}
else 
{
label_1780553:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1780560;
}
}
else 
{
label_1780560:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1780569;
}
else 
{
label_1780569:; 
return 1;
}
}
}
}
}
}
}
}
}
goto label_1780100;
}
}
}
}
else 
{
label_1780607:; 
goto label_1780100;
}
}
}
else 
{
}
goto label_1779497;
}
}
}
else 
{
}
goto label_1667328;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
else 
{
label_1675580:; 
goto label_1675073;
}
}
}
else 
{
}
goto label_1674767;
}
}
}
else 
{
}
goto label_1667328;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
else 
{
label_1647564:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1648388;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
goto label_1648248;
}
else 
{
label_1648248:; 
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1648330;
}
else 
{
goto label_1648271;
}
}
else 
{
label_1648271:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1648330;
}
else 
{
goto label_1648278;
}
}
else 
{
label_1648278:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1648330;
}
else 
{
goto label_1648285;
}
}
else 
{
label_1648285:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1648330;
}
else 
{
goto label_1648292;
}
}
else 
{
label_1648292:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1648330;
}
else 
{
goto label_1648299;
}
}
else 
{
label_1648299:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1648308;
}
else 
{
label_1648308:; 
goto label_1648330;
}
}
else 
{
label_1648330:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1648337;
}
}
else 
{
label_1648337:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1648346;
}
else 
{
label_1648346:; 
return 1;
}
}
}
}
}
}
}
}
}
label_1797281:; 
label_1797295:; 
if (((int)m_run_st) == 0)
{
goto label_1797309;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1797309:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_1797791;
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
if (((int)m_run_pc) == 0)
{
goto label_1797417;
}
else 
{
label_1797417:; 
a = 0;
req_type = 1;
req_a = a;
req_d = a + 50;
if (c_m_lock == 1)
{
m_run_st = 2;
m_run_pc = 1;
req_t_type = req_type;
req_t_a = req_a;
req_t_d = req_d;
rsp_t_type = rsp_type;
rsp_t_status = rsp_status;
rsp_t_d = rsp_d;
d_t = d;
a_t = a;
}
else 
{
c_m_lock = 1;
c_req_type = req_type;
c_req_a = req_a;
c_req_d = req_d;
c_empty_req = 0;
c_write_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1797520;
}
else 
{
goto label_1797461;
}
}
else 
{
label_1797461:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1797520;
}
else 
{
goto label_1797468;
}
}
else 
{
label_1797468:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1797520;
}
else 
{
goto label_1797475;
}
}
else 
{
label_1797475:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1797520;
}
else 
{
goto label_1797482;
}
}
else 
{
label_1797482:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1797520;
}
else 
{
goto label_1797489;
}
}
else 
{
label_1797489:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1797498;
}
else 
{
label_1797498:; 
goto label_1797520;
}
}
else 
{
label_1797520:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1797542;
}
else 
{
goto label_1797527;
}
}
else 
{
label_1797527:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1797536;
}
else 
{
label_1797536:; 
goto label_1797542;
}
}
else 
{
label_1797542:; 
c_write_req_ev = 2;
if (((int)c_empty_rsp) == 1)
{
m_run_st = 2;
m_run_pc = 3;
req_t_type = req_type;
req_t_a = req_a;
req_t_d = req_d;
rsp_t_type = rsp_type;
rsp_t_status = rsp_status;
rsp_t_d = rsp_d;
d_t = d;
a_t = a;
}
else 
{
rsp_type = c_rsp_type;
rsp_status = c_rsp_status;
rsp_d = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1797622;
}
else 
{
goto label_1797563;
}
}
else 
{
label_1797563:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1797622;
}
else 
{
goto label_1797570;
}
}
else 
{
label_1797570:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1797622;
}
else 
{
goto label_1797577;
}
}
else 
{
label_1797577:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1797622;
}
else 
{
goto label_1797584;
}
}
else 
{
label_1797584:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1797622;
}
else 
{
goto label_1797591;
}
}
else 
{
label_1797591:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1797600;
}
else 
{
label_1797600:; 
goto label_1797622;
}
}
else 
{
label_1797622:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1797644;
}
else 
{
goto label_1797629;
}
}
else 
{
label_1797629:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1797638;
}
else 
{
label_1797638:; 
goto label_1797644;
}
}
else 
{
label_1797644:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1797717;
}
else 
{
goto label_1797658;
}
}
else 
{
label_1797658:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1797717;
}
else 
{
goto label_1797665;
}
}
else 
{
label_1797665:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1797717;
}
else 
{
goto label_1797672;
}
}
else 
{
label_1797672:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1797717;
}
else 
{
goto label_1797679;
}
}
else 
{
label_1797679:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1797717;
}
else 
{
goto label_1797686;
}
}
else 
{
label_1797686:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1797695;
}
else 
{
label_1797695:; 
goto label_1797717;
}
}
else 
{
label_1797717:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1797724;
}
}
else 
{
label_1797724:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1797733;
}
else 
{
label_1797733:; 
return 1;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
label_1797783:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1798610;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1798100;
}
else 
{
goto label_1798041;
}
}
else 
{
label_1798041:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1798100;
}
else 
{
goto label_1798048;
}
}
else 
{
label_1798048:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1798100;
}
else 
{
goto label_1798055;
}
}
else 
{
label_1798055:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1798100;
}
else 
{
goto label_1798062;
}
}
else 
{
label_1798062:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1798100;
}
else 
{
goto label_1798069;
}
}
else 
{
label_1798069:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1798078;
}
else 
{
label_1798078:; 
goto label_1798100;
}
}
else 
{
label_1798100:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1798122;
}
else 
{
goto label_1798107;
}
}
else 
{
label_1798107:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1798116;
}
else 
{
label_1798116:; 
goto label_1798122;
}
}
else 
{
label_1798122:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_31 = req_a;
int i = __tmp_31;
int x;
if (i == 0)
{
x = s_memory0;
 __return_1798172 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_1798173 = x;
}
rsp_d = __return_1798172;
goto label_1798175;
rsp_d = __return_1798173;
label_1798175:; 
rsp_status = 1;
label_1798181:; 
c_rsp_type = rsp_type;
c_rsp_status = rsp_status;
c_rsp_d = rsp_d;
c_empty_rsp = 0;
c_write_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1798262;
}
else 
{
goto label_1798203;
}
}
else 
{
label_1798203:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1798262;
}
else 
{
goto label_1798210;
}
}
else 
{
label_1798210:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1798262;
}
else 
{
goto label_1798217;
}
}
else 
{
label_1798217:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1798262;
}
else 
{
goto label_1798224;
}
}
else 
{
label_1798224:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1798262;
}
else 
{
goto label_1798231;
}
}
else 
{
label_1798231:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1798240;
}
else 
{
label_1798240:; 
goto label_1798262;
}
}
else 
{
label_1798262:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1798284;
}
else 
{
goto label_1798269;
}
}
else 
{
label_1798269:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1798278;
}
else 
{
label_1798278:; 
goto label_1798284;
}
}
else 
{
label_1798284:; 
c_write_rsp_ev = 2;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1798367;
}
else 
{
goto label_1798308;
}
}
else 
{
label_1798308:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1798367;
}
else 
{
goto label_1798315;
}
}
else 
{
label_1798315:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1798367;
}
else 
{
goto label_1798322;
}
}
else 
{
label_1798322:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1798367;
}
else 
{
goto label_1798329;
}
}
else 
{
label_1798329:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1798367;
}
else 
{
goto label_1798336;
}
}
else 
{
label_1798336:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1798345;
}
else 
{
label_1798345:; 
goto label_1798367;
}
}
else 
{
label_1798367:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1798374;
}
}
else 
{
label_1798374:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1798383;
}
else 
{
label_1798383:; 
return 1;
}
}
}
}
}
}
}
}
}
goto label_1797277;
}
}
}
}
}
}
}
}
}
}
else 
{
if (((int)req_type) == 1)
{
{
int __tmp_32 = req_a;
int __tmp_33 = req_d;
int i = __tmp_32;
int v = __tmp_33;
if (i == 0)
{
s_memory0 = v;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_1798150;
label_1798150:; 
rsp_status = 1;
goto label_1798181;
}
}
else 
{
rsp_status = 0;
goto label_1798181;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
else 
{
label_1798610:; 
label_1798616:; 
if (((int)m_run_st) == 0)
{
goto label_1798629;
}
else 
{
label_1798629:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_1798640;
}
else 
{
label_1798640:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1799107;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1798767;
}
else 
{
goto label_1798708;
}
}
else 
{
label_1798708:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1798767;
}
else 
{
goto label_1798715;
}
}
else 
{
label_1798715:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1798767;
}
else 
{
goto label_1798722;
}
}
else 
{
label_1798722:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1798767;
}
else 
{
goto label_1798729;
}
}
else 
{
label_1798729:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1798767;
}
else 
{
goto label_1798736;
}
}
else 
{
label_1798736:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1798745;
}
else 
{
label_1798745:; 
goto label_1798767;
}
}
else 
{
label_1798767:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1798789;
}
else 
{
goto label_1798774;
}
}
else 
{
label_1798774:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1798783;
}
else 
{
label_1798783:; 
goto label_1798789;
}
}
else 
{
label_1798789:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_34 = req_a;
int i = __tmp_34;
int x;
if (i == 0)
{
x = s_memory0;
 __return_1798839 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_1798840 = x;
}
rsp_d = __return_1798839;
goto label_1798842;
rsp_d = __return_1798840;
label_1798842:; 
rsp_status = 1;
label_1798848:; 
c_rsp_type = rsp_type;
c_rsp_status = rsp_status;
c_rsp_d = rsp_d;
c_empty_rsp = 0;
c_write_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1798929;
}
else 
{
goto label_1798870;
}
}
else 
{
label_1798870:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1798929;
}
else 
{
goto label_1798877;
}
}
else 
{
label_1798877:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1798929;
}
else 
{
goto label_1798884;
}
}
else 
{
label_1798884:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1798929;
}
else 
{
goto label_1798891;
}
}
else 
{
label_1798891:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1798929;
}
else 
{
goto label_1798898;
}
}
else 
{
label_1798898:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1798907;
}
else 
{
label_1798907:; 
goto label_1798929;
}
}
else 
{
label_1798929:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1798951;
}
else 
{
goto label_1798936;
}
}
else 
{
label_1798936:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1798945;
}
else 
{
label_1798945:; 
goto label_1798951;
}
}
else 
{
label_1798951:; 
c_write_rsp_ev = 2;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1799034;
}
else 
{
goto label_1798975;
}
}
else 
{
label_1798975:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1799034;
}
else 
{
goto label_1798982;
}
}
else 
{
label_1798982:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1799034;
}
else 
{
goto label_1798989;
}
}
else 
{
label_1798989:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1799034;
}
else 
{
goto label_1798996;
}
}
else 
{
label_1798996:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1799034;
}
else 
{
goto label_1799003;
}
}
else 
{
label_1799003:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1799012;
}
else 
{
label_1799012:; 
goto label_1799034;
}
}
else 
{
label_1799034:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1799041;
}
}
else 
{
label_1799041:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1799050;
}
else 
{
label_1799050:; 
return 1;
}
}
}
}
}
}
}
}
}
goto label_1797277;
}
}
}
}
}
}
}
}
}
}
else 
{
if (((int)req_type) == 1)
{
{
int __tmp_35 = req_a;
int __tmp_36 = req_d;
int i = __tmp_35;
int v = __tmp_36;
if (i == 0)
{
s_memory0 = v;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_1798817;
label_1798817:; 
rsp_status = 1;
goto label_1798848;
}
}
else 
{
rsp_status = 0;
goto label_1798848;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
else 
{
label_1799107:; 
goto label_1798616;
}
}
}
}
}
}
}
}
}
}
}
}
}
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1798608;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1797934;
}
else 
{
goto label_1797875;
}
}
else 
{
label_1797875:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1797934;
}
else 
{
goto label_1797882;
}
}
else 
{
label_1797882:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1797934;
}
else 
{
goto label_1797889;
}
}
else 
{
label_1797889:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1797934;
}
else 
{
goto label_1797896;
}
}
else 
{
label_1797896:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1797934;
}
else 
{
goto label_1797903;
}
}
else 
{
label_1797903:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1797912;
}
else 
{
label_1797912:; 
goto label_1797934;
}
}
else 
{
label_1797934:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1797941;
}
}
else 
{
label_1797941:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1797950;
}
else 
{
label_1797950:; 
return 1;
}
}
}
}
}
}
}
}
}
goto label_1779487;
}
}
}
}
else 
{
label_1798608:; 
goto label_1779679;
}
}
}
}
}
else 
{
label_1797791:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1798612;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1798557;
}
else 
{
goto label_1798498;
}
}
else 
{
label_1798498:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1798557;
}
else 
{
goto label_1798505;
}
}
else 
{
label_1798505:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1798557;
}
else 
{
goto label_1798512;
}
}
else 
{
label_1798512:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1798557;
}
else 
{
goto label_1798519;
}
}
else 
{
label_1798519:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1798557;
}
else 
{
goto label_1798526;
}
}
else 
{
label_1798526:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1798535;
}
else 
{
label_1798535:; 
goto label_1798557;
}
}
else 
{
label_1798557:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1798564;
}
}
else 
{
label_1798564:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1798573;
}
else 
{
label_1798573:; 
return 1;
}
}
}
}
}
}
}
}
}
goto label_1797281;
}
}
}
}
else 
{
label_1798612:; 
goto label_1797295;
}
}
}
else 
{
}
label_1797306:; 
kernel_st = 2;
kernel_st = 3;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1814875;
}
else 
{
goto label_1814580;
}
}
else 
{
label_1814580:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1814875;
}
else 
{
goto label_1814623;
}
}
else 
{
label_1814623:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1814875;
}
else 
{
goto label_1814650;
}
}
else 
{
label_1814650:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1814875;
}
else 
{
goto label_1814693;
}
}
else 
{
label_1814693:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1814875;
}
else 
{
goto label_1814720;
}
}
else 
{
label_1814720:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1814765;
}
else 
{
label_1814765:; 
goto label_1814875;
}
}
else 
{
label_1814875:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1814993;
}
else 
{
goto label_1814918;
}
}
else 
{
label_1814918:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1814963;
}
else 
{
label_1814963:; 
goto label_1814993;
}
}
else 
{
label_1814993:; 
if (((int)m_run_st) == 0)
{
goto label_1815055;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1815055:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
label_1815067:; 
if (((int)m_run_st) == 0)
{
goto label_1815081;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1815081:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_1815563;
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
if (((int)m_run_pc) == 0)
{
goto label_1815189;
}
else 
{
label_1815189:; 
a = 0;
req_type = 1;
req_a = a;
req_d = a + 50;
if (c_m_lock == 1)
{
m_run_st = 2;
m_run_pc = 1;
req_t_type = req_type;
req_t_a = req_a;
req_t_d = req_d;
rsp_t_type = rsp_type;
rsp_t_status = rsp_status;
rsp_t_d = rsp_d;
d_t = d;
a_t = a;
}
else 
{
c_m_lock = 1;
c_req_type = req_type;
c_req_a = req_a;
c_req_d = req_d;
c_empty_req = 0;
c_write_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1815292;
}
else 
{
goto label_1815233;
}
}
else 
{
label_1815233:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1815292;
}
else 
{
goto label_1815240;
}
}
else 
{
label_1815240:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1815292;
}
else 
{
goto label_1815247;
}
}
else 
{
label_1815247:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1815292;
}
else 
{
goto label_1815254;
}
}
else 
{
label_1815254:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1815292;
}
else 
{
goto label_1815261;
}
}
else 
{
label_1815261:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1815270;
}
else 
{
label_1815270:; 
goto label_1815292;
}
}
else 
{
label_1815292:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1815314;
}
else 
{
goto label_1815299;
}
}
else 
{
label_1815299:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1815308;
}
else 
{
label_1815308:; 
goto label_1815314;
}
}
else 
{
label_1815314:; 
c_write_req_ev = 2;
if (((int)c_empty_rsp) == 1)
{
m_run_st = 2;
m_run_pc = 3;
req_t_type = req_type;
req_t_a = req_a;
req_t_d = req_d;
rsp_t_type = rsp_type;
rsp_t_status = rsp_status;
rsp_t_d = rsp_d;
d_t = d;
a_t = a;
}
else 
{
rsp_type = c_rsp_type;
rsp_status = c_rsp_status;
rsp_d = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1815394;
}
else 
{
goto label_1815335;
}
}
else 
{
label_1815335:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1815394;
}
else 
{
goto label_1815342;
}
}
else 
{
label_1815342:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1815394;
}
else 
{
goto label_1815349;
}
}
else 
{
label_1815349:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1815394;
}
else 
{
goto label_1815356;
}
}
else 
{
label_1815356:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1815394;
}
else 
{
goto label_1815363;
}
}
else 
{
label_1815363:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1815372;
}
else 
{
label_1815372:; 
goto label_1815394;
}
}
else 
{
label_1815394:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1815416;
}
else 
{
goto label_1815401;
}
}
else 
{
label_1815401:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1815410;
}
else 
{
label_1815410:; 
goto label_1815416;
}
}
else 
{
label_1815416:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1815489;
}
else 
{
goto label_1815430;
}
}
else 
{
label_1815430:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1815489;
}
else 
{
goto label_1815437;
}
}
else 
{
label_1815437:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1815489;
}
else 
{
goto label_1815444;
}
}
else 
{
label_1815444:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1815489;
}
else 
{
goto label_1815451;
}
}
else 
{
label_1815451:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1815489;
}
else 
{
goto label_1815458;
}
}
else 
{
label_1815458:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1815467;
}
else 
{
label_1815467:; 
goto label_1815489;
}
}
else 
{
label_1815489:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1815496;
}
}
else 
{
label_1815496:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1815505;
}
else 
{
label_1815505:; 
return 1;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
label_1815555:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1816385;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1815872;
}
else 
{
goto label_1815813;
}
}
else 
{
label_1815813:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1815872;
}
else 
{
goto label_1815820;
}
}
else 
{
label_1815820:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1815872;
}
else 
{
goto label_1815827;
}
}
else 
{
label_1815827:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1815872;
}
else 
{
goto label_1815834;
}
}
else 
{
label_1815834:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1815872;
}
else 
{
goto label_1815841;
}
}
else 
{
label_1815841:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1815850;
}
else 
{
label_1815850:; 
goto label_1815872;
}
}
else 
{
label_1815872:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1815894;
}
else 
{
goto label_1815879;
}
}
else 
{
label_1815879:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1815888;
}
else 
{
label_1815888:; 
goto label_1815894;
}
}
else 
{
label_1815894:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_37 = req_a;
int i = __tmp_37;
int x;
if (i == 0)
{
x = s_memory0;
 __return_1815944 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_1815945 = x;
}
rsp_d = __return_1815944;
goto label_1815947;
rsp_d = __return_1815945;
label_1815947:; 
rsp_status = 1;
label_1815953:; 
c_rsp_type = rsp_type;
c_rsp_status = rsp_status;
c_rsp_d = rsp_d;
c_empty_rsp = 0;
c_write_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1816034;
}
else 
{
goto label_1815975;
}
}
else 
{
label_1815975:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1816034;
}
else 
{
goto label_1815982;
}
}
else 
{
label_1815982:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1816034;
}
else 
{
goto label_1815989;
}
}
else 
{
label_1815989:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1816034;
}
else 
{
goto label_1815996;
}
}
else 
{
label_1815996:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1816034;
}
else 
{
goto label_1816003;
}
}
else 
{
label_1816003:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1816012;
}
else 
{
label_1816012:; 
goto label_1816034;
}
}
else 
{
label_1816034:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1816056;
}
else 
{
goto label_1816041;
}
}
else 
{
label_1816041:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1816050;
}
else 
{
label_1816050:; 
goto label_1816056;
}
}
else 
{
label_1816056:; 
c_write_rsp_ev = 2;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1816139;
}
else 
{
goto label_1816080;
}
}
else 
{
label_1816080:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1816139;
}
else 
{
goto label_1816087;
}
}
else 
{
label_1816087:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1816139;
}
else 
{
goto label_1816094;
}
}
else 
{
label_1816094:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1816139;
}
else 
{
goto label_1816101;
}
}
else 
{
label_1816101:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1816139;
}
else 
{
goto label_1816108;
}
}
else 
{
label_1816108:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1816117;
}
else 
{
label_1816117:; 
goto label_1816139;
}
}
else 
{
label_1816139:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1816146;
}
}
else 
{
label_1816146:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1816155;
}
else 
{
label_1816155:; 
return 1;
}
}
}
}
}
}
}
}
}
label_1816209:; 
label_1816394:; 
if (((int)m_run_st) == 0)
{
goto label_1816407;
}
else 
{
label_1816407:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_1817469;
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
if (((int)m_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type;
req_a = req_t_a;
req_d = req_t_d;
rsp_type = rsp_t_type;
rsp_status = rsp_t_status;
rsp_d = rsp_t_d;
d = d_t;
a = a_t;
rsp_type = c_rsp_type;
rsp_status = c_rsp_status;
rsp_d = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1816595;
}
else 
{
goto label_1816536;
}
}
else 
{
label_1816536:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1816595;
}
else 
{
goto label_1816543;
}
}
else 
{
label_1816543:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1816595;
}
else 
{
goto label_1816550;
}
}
else 
{
label_1816550:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1816595;
}
else 
{
goto label_1816557;
}
}
else 
{
label_1816557:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1816595;
}
else 
{
goto label_1816564;
}
}
else 
{
label_1816564:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1816573;
}
else 
{
label_1816573:; 
goto label_1816595;
}
}
else 
{
label_1816595:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1816617;
}
else 
{
goto label_1816602;
}
}
else 
{
label_1816602:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1816611;
}
else 
{
label_1816611:; 
goto label_1816617;
}
}
else 
{
label_1816617:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1816690;
}
else 
{
goto label_1816631;
}
}
else 
{
label_1816631:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1816690;
}
else 
{
goto label_1816638;
}
}
else 
{
label_1816638:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1816690;
}
else 
{
goto label_1816645;
}
}
else 
{
label_1816645:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1816690;
}
else 
{
goto label_1816652;
}
}
else 
{
label_1816652:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1816690;
}
else 
{
goto label_1816659;
}
}
else 
{
label_1816659:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1816668;
}
else 
{
label_1816668:; 
goto label_1816690;
}
}
else 
{
label_1816690:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1816712;
}
else 
{
goto label_1816697;
}
}
else 
{
label_1816697:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1816706;
}
else 
{
label_1816706:; 
goto label_1816712;
}
}
else 
{
label_1816712:; 
c_m_ev = 2;
a = a + 1;
if (a < 1)
{
req_type = 1;
req_a = a;
req_d = a + 50;
c_m_lock = 1;
c_req_type = req_type;
c_req_a = req_a;
c_req_d = req_d;
c_empty_req = 0;
c_write_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1817177;
}
else 
{
goto label_1817118;
}
}
else 
{
label_1817118:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1817177;
}
else 
{
goto label_1817125;
}
}
else 
{
label_1817125:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1817177;
}
else 
{
goto label_1817132;
}
}
else 
{
label_1817132:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1817177;
}
else 
{
goto label_1817139;
}
}
else 
{
label_1817139:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1817177;
}
else 
{
goto label_1817146;
}
}
else 
{
label_1817146:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1817155;
}
else 
{
label_1817155:; 
goto label_1817177;
}
}
else 
{
label_1817177:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1817199;
}
else 
{
goto label_1817184;
}
}
else 
{
label_1817184:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1817193;
}
else 
{
label_1817193:; 
goto label_1817199;
}
}
else 
{
label_1817199:; 
c_write_req_ev = 2;
if (((int)c_empty_rsp) == 1)
{
m_run_st = 2;
m_run_pc = 3;
req_t_type = req_type;
req_t_a = req_a;
req_t_d = req_d;
rsp_t_type = rsp_type;
rsp_t_status = rsp_status;
rsp_t_d = rsp_d;
d_t = d;
a_t = a;
}
else 
{
rsp_type = c_rsp_type;
rsp_status = c_rsp_status;
rsp_d = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1817279;
}
else 
{
goto label_1817220;
}
}
else 
{
label_1817220:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1817279;
}
else 
{
goto label_1817227;
}
}
else 
{
label_1817227:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1817279;
}
else 
{
goto label_1817234;
}
}
else 
{
label_1817234:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1817279;
}
else 
{
goto label_1817241;
}
}
else 
{
label_1817241:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1817279;
}
else 
{
goto label_1817248;
}
}
else 
{
label_1817248:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1817257;
}
else 
{
label_1817257:; 
goto label_1817279;
}
}
else 
{
label_1817279:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1817301;
}
else 
{
goto label_1817286;
}
}
else 
{
label_1817286:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1817295;
}
else 
{
label_1817295:; 
goto label_1817301;
}
}
else 
{
label_1817301:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1817374;
}
else 
{
goto label_1817315;
}
}
else 
{
label_1817315:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1817374;
}
else 
{
goto label_1817322;
}
}
else 
{
label_1817322:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1817374;
}
else 
{
goto label_1817329;
}
}
else 
{
label_1817329:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1817374;
}
else 
{
goto label_1817336;
}
}
else 
{
label_1817336:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1817374;
}
else 
{
goto label_1817343;
}
}
else 
{
label_1817343:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1817352;
}
else 
{
label_1817352:; 
goto label_1817374;
}
}
else 
{
label_1817374:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1817381;
}
}
else 
{
label_1817381:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1817390;
}
else 
{
label_1817390:; 
return 1;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
goto label_1815555;
}
}
}
}
}
}
}
}
}
else 
{
a = 0;
req_type___0 = 0;
req_a___0 = a;
c_m_lock = 1;
c_req_type = req_type___0;
c_req_a = req_a___0;
c_req_d = req_d___0;
c_empty_req = 0;
c_write_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1816823;
}
else 
{
goto label_1816764;
}
}
else 
{
label_1816764:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1816823;
}
else 
{
goto label_1816771;
}
}
else 
{
label_1816771:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1816823;
}
else 
{
goto label_1816778;
}
}
else 
{
label_1816778:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1816823;
}
else 
{
goto label_1816785;
}
}
else 
{
label_1816785:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1816823;
}
else 
{
goto label_1816792;
}
}
else 
{
label_1816792:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1816801;
}
else 
{
label_1816801:; 
goto label_1816823;
}
}
else 
{
label_1816823:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1816845;
}
else 
{
goto label_1816830;
}
}
else 
{
label_1816830:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1816839;
}
else 
{
label_1816839:; 
goto label_1816845;
}
}
else 
{
label_1816845:; 
c_write_req_ev = 2;
if (((int)c_empty_rsp) == 1)
{
m_run_st = 2;
m_run_pc = 6;
req_tt_type = req_type___0;
req_tt_a = req_a___0;
req_tt_d = req_d___0;
rsp_tt_type = rsp_type___0;
rsp_tt_status = rsp_status___0;
rsp_tt_d = rsp_d___0;
d_t = d;
a_t = a;
}
else 
{
rsp_type___0 = c_rsp_type;
rsp_status___0 = c_rsp_status;
rsp_d___0 = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1816925;
}
else 
{
goto label_1816866;
}
}
else 
{
label_1816866:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1816925;
}
else 
{
goto label_1816873;
}
}
else 
{
label_1816873:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1816925;
}
else 
{
goto label_1816880;
}
}
else 
{
label_1816880:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1816925;
}
else 
{
goto label_1816887;
}
}
else 
{
label_1816887:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1816925;
}
else 
{
goto label_1816894;
}
}
else 
{
label_1816894:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1816903;
}
else 
{
label_1816903:; 
goto label_1816925;
}
}
else 
{
label_1816925:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1816947;
}
else 
{
goto label_1816932;
}
}
else 
{
label_1816932:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1816941;
}
else 
{
label_1816941:; 
goto label_1816947;
}
}
else 
{
label_1816947:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1817020;
}
else 
{
goto label_1816961;
}
}
else 
{
label_1816961:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1817020;
}
else 
{
goto label_1816968;
}
}
else 
{
label_1816968:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1817020;
}
else 
{
goto label_1816975;
}
}
else 
{
label_1816975:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1817020;
}
else 
{
goto label_1816982;
}
}
else 
{
label_1816982:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1817020;
}
else 
{
goto label_1816989;
}
}
else 
{
label_1816989:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1816998;
}
else 
{
label_1816998:; 
goto label_1817020;
}
}
else 
{
label_1817020:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1817027;
}
}
else 
{
label_1817027:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1817036;
}
else 
{
label_1817036:; 
return 1;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
label_1933152:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1933632;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1933289;
}
else 
{
goto label_1933230;
}
}
else 
{
label_1933230:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1933289;
}
else 
{
goto label_1933237;
}
}
else 
{
label_1933237:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1933289;
}
else 
{
goto label_1933244;
}
}
else 
{
label_1933244:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1933289;
}
else 
{
goto label_1933251;
}
}
else 
{
label_1933251:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1933289;
}
else 
{
goto label_1933258;
}
}
else 
{
label_1933258:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1933267;
}
else 
{
label_1933267:; 
goto label_1933289;
}
}
else 
{
label_1933289:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1933311;
}
else 
{
goto label_1933296;
}
}
else 
{
label_1933296:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1933305;
}
else 
{
label_1933305:; 
goto label_1933311;
}
}
else 
{
label_1933311:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_38 = req_a;
int i = __tmp_38;
int x;
if (i == 0)
{
x = s_memory0;
 __return_1933361 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_1933362 = x;
}
rsp_d = __return_1933361;
goto label_1933364;
rsp_d = __return_1933362;
label_1933364:; 
rsp_status = 1;
label_1933370:; 
c_rsp_type = rsp_type;
c_rsp_status = rsp_status;
c_rsp_d = rsp_d;
c_empty_rsp = 0;
c_write_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1933451;
}
else 
{
goto label_1933392;
}
}
else 
{
label_1933392:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1933451;
}
else 
{
goto label_1933399;
}
}
else 
{
label_1933399:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1933451;
}
else 
{
goto label_1933406;
}
}
else 
{
label_1933406:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1933451;
}
else 
{
goto label_1933413;
}
}
else 
{
label_1933413:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1933451;
}
else 
{
goto label_1933420;
}
}
else 
{
label_1933420:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1933429;
}
else 
{
label_1933429:; 
goto label_1933451;
}
}
else 
{
label_1933451:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1933473;
}
else 
{
goto label_1933458;
}
}
else 
{
label_1933458:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1933467;
}
else 
{
label_1933467:; 
goto label_1933473;
}
}
else 
{
label_1933473:; 
c_write_rsp_ev = 2;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1933556;
}
else 
{
goto label_1933497;
}
}
else 
{
label_1933497:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1933556;
}
else 
{
goto label_1933504;
}
}
else 
{
label_1933504:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1933556;
}
else 
{
goto label_1933511;
}
}
else 
{
label_1933511:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1933556;
}
else 
{
goto label_1933518;
}
}
else 
{
label_1933518:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1933556;
}
else 
{
goto label_1933525;
}
}
else 
{
label_1933525:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1933534;
}
else 
{
label_1933534:; 
goto label_1933556;
}
}
else 
{
label_1933556:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1933563;
}
}
else 
{
label_1933563:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1933572;
}
else 
{
label_1933572:; 
return 1;
}
}
}
}
}
}
}
}
}
label_1938672:; 
label_1938676:; 
if (((int)m_run_st) == 0)
{
goto label_1938689;
}
else 
{
label_1938689:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_1939403;
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
if (((int)m_run_pc) == 0)
{
return 1;
}
else 
{
req_type___0 = req_tt_type;
req_a___0 = req_tt_a;
req_d___0 = req_tt_d;
rsp_type___0 = rsp_tt_type;
rsp_status___0 = rsp_tt_status;
rsp_d___0 = rsp_tt_d;
d = d_t;
a = a_t;
rsp_type___0 = c_rsp_type;
rsp_status___0 = c_rsp_status;
rsp_d___0 = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1938877;
}
else 
{
goto label_1938818;
}
}
else 
{
label_1938818:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1938877;
}
else 
{
goto label_1938825;
}
}
else 
{
label_1938825:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1938877;
}
else 
{
goto label_1938832;
}
}
else 
{
label_1938832:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1938877;
}
else 
{
goto label_1938839;
}
}
else 
{
label_1938839:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1938877;
}
else 
{
goto label_1938846;
}
}
else 
{
label_1938846:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1938855;
}
else 
{
label_1938855:; 
goto label_1938877;
}
}
else 
{
label_1938877:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1938899;
}
else 
{
goto label_1938884;
}
}
else 
{
label_1938884:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1938893;
}
else 
{
label_1938893:; 
goto label_1938899;
}
}
else 
{
label_1938899:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1938972;
}
else 
{
goto label_1938913;
}
}
else 
{
label_1938913:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1938972;
}
else 
{
goto label_1938920;
}
}
else 
{
label_1938920:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1938972;
}
else 
{
goto label_1938927;
}
}
else 
{
label_1938927:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1938972;
}
else 
{
goto label_1938934;
}
}
else 
{
label_1938934:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1938972;
}
else 
{
goto label_1938941;
}
}
else 
{
label_1938941:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1938950;
}
else 
{
label_1938950:; 
goto label_1938972;
}
}
else 
{
label_1938972:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1938994;
}
else 
{
goto label_1938979;
}
}
else 
{
label_1938979:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1938988;
}
else 
{
label_1938988:; 
goto label_1938994;
}
}
else 
{
label_1938994:; 
c_m_ev = 2;
if ((req_a___0 + 50) == rsp_d___0)
{
a = a + 1;
goto label_1939011;
}
else 
{
{
__VERIFIER_error();
}
a = a + 1;
label_1939011:; 
if (a < 1)
{
req_type___0 = 0;
req_a___0 = a;
if (c_m_lock == 1)
{
m_run_st = 2;
m_run_pc = 4;
req_tt_type = req_type___0;
req_tt_a = req_a___0;
req_tt_d = req_d___0;
rsp_tt_type = rsp_type___0;
rsp_tt_status = rsp_status___0;
rsp_tt_d = rsp_d___0;
d_t = d;
a_t = a;
}
else 
{
c_m_lock = 1;
c_req_type = req_type___0;
c_req_a = req_a___0;
c_req_d = req_d___0;
c_empty_req = 0;
c_write_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1939109;
}
else 
{
goto label_1939050;
}
}
else 
{
label_1939050:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1939109;
}
else 
{
goto label_1939057;
}
}
else 
{
label_1939057:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1939109;
}
else 
{
goto label_1939064;
}
}
else 
{
label_1939064:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1939109;
}
else 
{
goto label_1939071;
}
}
else 
{
label_1939071:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1939109;
}
else 
{
goto label_1939078;
}
}
else 
{
label_1939078:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1939087;
}
else 
{
label_1939087:; 
goto label_1939109;
}
}
else 
{
label_1939109:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1939131;
}
else 
{
goto label_1939116;
}
}
else 
{
label_1939116:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1939125;
}
else 
{
label_1939125:; 
goto label_1939131;
}
}
else 
{
label_1939131:; 
c_write_req_ev = 2;
if (((int)c_empty_rsp) == 1)
{
m_run_st = 2;
m_run_pc = 6;
req_tt_type = req_type___0;
req_tt_a = req_a___0;
req_tt_d = req_d___0;
rsp_tt_type = rsp_type___0;
rsp_tt_status = rsp_status___0;
rsp_tt_d = rsp_d___0;
d_t = d;
a_t = a;
}
else 
{
rsp_type___0 = c_rsp_type;
rsp_status___0 = c_rsp_status;
rsp_d___0 = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1939211;
}
else 
{
goto label_1939152;
}
}
else 
{
label_1939152:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1939211;
}
else 
{
goto label_1939159;
}
}
else 
{
label_1939159:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1939211;
}
else 
{
goto label_1939166;
}
}
else 
{
label_1939166:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1939211;
}
else 
{
goto label_1939173;
}
}
else 
{
label_1939173:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1939211;
}
else 
{
goto label_1939180;
}
}
else 
{
label_1939180:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1939189;
}
else 
{
label_1939189:; 
goto label_1939211;
}
}
else 
{
label_1939211:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1939233;
}
else 
{
goto label_1939218;
}
}
else 
{
label_1939218:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1939227;
}
else 
{
label_1939227:; 
goto label_1939233;
}
}
else 
{
label_1939233:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1939306;
}
else 
{
goto label_1939247;
}
}
else 
{
label_1939247:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1939306;
}
else 
{
goto label_1939254;
}
}
else 
{
label_1939254:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1939306;
}
else 
{
goto label_1939261;
}
}
else 
{
label_1939261:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1939306;
}
else 
{
goto label_1939268;
}
}
else 
{
label_1939268:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1939306;
}
else 
{
goto label_1939275;
}
}
else 
{
label_1939275:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1939284;
}
else 
{
label_1939284:; 
goto label_1939306;
}
}
else 
{
label_1939306:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1939313;
}
}
else 
{
label_1939313:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1939322;
}
else 
{
label_1939322:; 
return 1;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
goto label_1933152;
}
}
}
}
}
}
}
}
}
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1949497;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1949447;
}
else 
{
goto label_1949388;
}
}
else 
{
label_1949388:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1949447;
}
else 
{
goto label_1949395;
}
}
else 
{
label_1949395:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1949447;
}
else 
{
goto label_1949402;
}
}
else 
{
label_1949402:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1949447;
}
else 
{
goto label_1949409;
}
}
else 
{
label_1949409:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1949447;
}
else 
{
goto label_1949416;
}
}
else 
{
label_1949416:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1949425;
}
else 
{
label_1949425:; 
goto label_1949447;
}
}
else 
{
label_1949447:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1949454;
}
}
else 
{
label_1949454:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1949463;
}
else 
{
label_1949463:; 
return 1;
}
}
}
}
}
}
}
}
}
label_1949493:; 
label_1949500:; 
if (((int)m_run_st) == 0)
{
goto label_1949514;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1949514:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_1949525;
}
else 
{
label_1949525:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1949701;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1949652;
}
else 
{
goto label_1949593;
}
}
else 
{
label_1949593:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1949652;
}
else 
{
goto label_1949600;
}
}
else 
{
label_1949600:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1949652;
}
else 
{
goto label_1949607;
}
}
else 
{
label_1949607:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1949652;
}
else 
{
goto label_1949614;
}
}
else 
{
label_1949614:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1949652;
}
else 
{
goto label_1949621;
}
}
else 
{
label_1949621:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1949630;
}
else 
{
label_1949630:; 
goto label_1949652;
}
}
else 
{
label_1949652:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1949659;
}
}
else 
{
label_1949659:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1949668;
}
else 
{
label_1949668:; 
return 1;
}
}
}
}
}
}
}
}
}
goto label_1949493;
}
}
}
}
else 
{
label_1949701:; 
goto label_1949500;
}
}
}
else 
{
}
goto label_1949316;
}
}
}
}
}
else 
{
label_1949497:; 
goto label_1949500;
}
}
else 
{
}
label_1939392:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1939933;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1939712;
}
else 
{
goto label_1939653;
}
}
else 
{
label_1939653:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1939712;
}
else 
{
goto label_1939660;
}
}
else 
{
label_1939660:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1939712;
}
else 
{
goto label_1939667;
}
}
else 
{
label_1939667:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1939712;
}
else 
{
goto label_1939674;
}
}
else 
{
label_1939674:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1939712;
}
else 
{
goto label_1939681;
}
}
else 
{
label_1939681:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1939690;
}
else 
{
label_1939690:; 
goto label_1939712;
}
}
else 
{
label_1939712:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1939719;
}
}
else 
{
label_1939719:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1939728;
}
else 
{
label_1939728:; 
return 1;
}
}
}
}
}
}
}
}
}
label_1939758:; 
label_1939939:; 
if (((int)m_run_st) == 0)
{
goto label_1939953;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1939953:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_1940186;
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
if (((int)m_run_pc) == 0)
{
return 1;
}
else 
{
req_type___0 = req_tt_type;
req_a___0 = req_tt_a;
req_d___0 = req_tt_d;
rsp_type___0 = rsp_tt_type;
rsp_status___0 = rsp_tt_status;
rsp_d___0 = rsp_tt_d;
d = d_t;
a = a_t;
if (((int)c_empty_rsp) == 1)
{
m_run_st = 2;
m_run_pc = 6;
req_tt_type = req_type___0;
req_tt_a = req_a___0;
req_tt_d = req_d___0;
rsp_tt_type = rsp_type___0;
rsp_tt_status = rsp_status___0;
rsp_tt_d = rsp_d___0;
d_t = d;
a_t = a;
}
else 
{
rsp_type___0 = c_rsp_type;
rsp_status___0 = c_rsp_status;
rsp_d___0 = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1940141;
}
else 
{
goto label_1940082;
}
}
else 
{
label_1940082:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1940141;
}
else 
{
goto label_1940089;
}
}
else 
{
label_1940089:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1940141;
}
else 
{
goto label_1940096;
}
}
else 
{
label_1940096:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1940141;
}
else 
{
goto label_1940103;
}
}
else 
{
label_1940103:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1940141;
}
else 
{
goto label_1940110;
}
}
else 
{
label_1940110:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1940119;
}
else 
{
label_1940119:; 
goto label_1940141;
}
}
else 
{
label_1940141:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1940148;
}
}
else 
{
label_1940148:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1940157;
}
else 
{
label_1940157:; 
return 1;
}
}
}
}
}
}
}
}
}
goto label_1939392;
}
}
}
}
else 
{
label_1940186:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1940362;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1940313;
}
else 
{
goto label_1940254;
}
}
else 
{
label_1940254:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1940313;
}
else 
{
goto label_1940261;
}
}
else 
{
label_1940261:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1940313;
}
else 
{
goto label_1940268;
}
}
else 
{
label_1940268:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1940313;
}
else 
{
goto label_1940275;
}
}
else 
{
label_1940275:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1940313;
}
else 
{
goto label_1940282;
}
}
else 
{
label_1940282:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1940291;
}
else 
{
label_1940291:; 
goto label_1940313;
}
}
else 
{
label_1940313:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1940320;
}
}
else 
{
label_1940320:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1940329;
}
else 
{
label_1940329:; 
return 1;
}
}
}
}
}
}
}
}
}
goto label_1939758;
}
}
}
}
else 
{
label_1940362:; 
goto label_1939939;
}
}
}
else 
{
}
goto label_1933154;
}
}
}
}
}
else 
{
label_1939933:; 
goto label_1939939;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
else 
{
label_1939403:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1939935;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1939878;
}
else 
{
goto label_1939819;
}
}
else 
{
label_1939819:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1939878;
}
else 
{
goto label_1939826;
}
}
else 
{
label_1939826:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1939878;
}
else 
{
goto label_1939833;
}
}
else 
{
label_1939833:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1939878;
}
else 
{
goto label_1939840;
}
}
else 
{
label_1939840:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1939878;
}
else 
{
goto label_1939847;
}
}
else 
{
label_1939847:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1939856;
}
else 
{
label_1939856:; 
goto label_1939878;
}
}
else 
{
label_1939878:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1939885;
}
}
else 
{
label_1939885:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1939894;
}
else 
{
label_1939894:; 
return 1;
}
}
}
}
}
}
}
}
}
goto label_1938672;
}
}
}
}
else 
{
label_1939935:; 
goto label_1938676;
}
}
}
}
}
}
}
}
}
}
}
}
}
else 
{
if (((int)req_type) == 1)
{
{
int __tmp_39 = req_a;
int __tmp_40 = req_d;
int i = __tmp_39;
int v = __tmp_40;
if (i == 0)
{
s_memory0 = v;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_1933339;
label_1933339:; 
rsp_status = 1;
goto label_1933370;
}
}
else 
{
rsp_status = 0;
goto label_1933370;
}
}
}
}
}
}
}
}
}
}
}
label_1933625:; 
label_1940577:; 
if (((int)m_run_st) == 0)
{
goto label_1940591;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1940591:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_1940602;
}
else 
{
label_1940602:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1940778;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1940729;
}
else 
{
goto label_1940670;
}
}
else 
{
label_1940670:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1940729;
}
else 
{
goto label_1940677;
}
}
else 
{
label_1940677:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1940729;
}
else 
{
goto label_1940684;
}
}
else 
{
label_1940684:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1940729;
}
else 
{
goto label_1940691;
}
}
else 
{
label_1940691:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1940729;
}
else 
{
goto label_1940698;
}
}
else 
{
label_1940698:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1940707;
}
else 
{
label_1940707:; 
goto label_1940729;
}
}
else 
{
label_1940729:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1940736;
}
}
else 
{
label_1940736:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1940745;
}
else 
{
label_1940745:; 
return 1;
}
}
}
}
}
}
}
}
}
goto label_1933625;
}
}
}
}
else 
{
label_1940778:; 
goto label_1940577;
}
}
}
else 
{
}
goto label_1933156;
}
}
}
}
}
else 
{
label_1933632:; 
label_1940782:; 
if (((int)m_run_st) == 0)
{
goto label_1940795;
}
else 
{
label_1940795:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_1940806;
}
else 
{
label_1940806:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1941274;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1940933;
}
else 
{
goto label_1940874;
}
}
else 
{
label_1940874:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1940933;
}
else 
{
goto label_1940881;
}
}
else 
{
label_1940881:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1940933;
}
else 
{
goto label_1940888;
}
}
else 
{
label_1940888:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1940933;
}
else 
{
goto label_1940895;
}
}
else 
{
label_1940895:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1940933;
}
else 
{
goto label_1940902;
}
}
else 
{
label_1940902:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1940911;
}
else 
{
label_1940911:; 
goto label_1940933;
}
}
else 
{
label_1940933:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1940955;
}
else 
{
goto label_1940940;
}
}
else 
{
label_1940940:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1940949;
}
else 
{
label_1940949:; 
goto label_1940955;
}
}
else 
{
label_1940955:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_41 = req_a;
int i = __tmp_41;
int x;
if (i == 0)
{
x = s_memory0;
 __return_1941005 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_1941006 = x;
}
rsp_d = __return_1941005;
goto label_1941008;
rsp_d = __return_1941006;
label_1941008:; 
rsp_status = 1;
label_1941014:; 
c_rsp_type = rsp_type;
c_rsp_status = rsp_status;
c_rsp_d = rsp_d;
c_empty_rsp = 0;
c_write_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1941095;
}
else 
{
goto label_1941036;
}
}
else 
{
label_1941036:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1941095;
}
else 
{
goto label_1941043;
}
}
else 
{
label_1941043:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1941095;
}
else 
{
goto label_1941050;
}
}
else 
{
label_1941050:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1941095;
}
else 
{
goto label_1941057;
}
}
else 
{
label_1941057:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1941095;
}
else 
{
goto label_1941064;
}
}
else 
{
label_1941064:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1941073;
}
else 
{
label_1941073:; 
goto label_1941095;
}
}
else 
{
label_1941095:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1941117;
}
else 
{
goto label_1941102;
}
}
else 
{
label_1941102:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1941111;
}
else 
{
label_1941111:; 
goto label_1941117;
}
}
else 
{
label_1941117:; 
c_write_rsp_ev = 2;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1941200;
}
else 
{
goto label_1941141;
}
}
else 
{
label_1941141:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1941200;
}
else 
{
goto label_1941148;
}
}
else 
{
label_1941148:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1941200;
}
else 
{
goto label_1941155;
}
}
else 
{
label_1941155:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1941200;
}
else 
{
goto label_1941162;
}
}
else 
{
label_1941162:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1941200;
}
else 
{
goto label_1941169;
}
}
else 
{
label_1941169:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1941178;
}
else 
{
label_1941178:; 
goto label_1941200;
}
}
else 
{
label_1941200:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1941207;
}
}
else 
{
label_1941207:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1941216;
}
else 
{
label_1941216:; 
return 1;
}
}
}
}
}
}
}
}
}
goto label_1938672;
}
}
}
}
}
}
}
}
}
}
else 
{
if (((int)req_type) == 1)
{
{
int __tmp_42 = req_a;
int __tmp_43 = req_d;
int i = __tmp_42;
int v = __tmp_43;
if (i == 0)
{
s_memory0 = v;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_1940983;
label_1940983:; 
rsp_status = 1;
goto label_1941014;
}
}
else 
{
rsp_status = 0;
goto label_1941014;
}
}
}
}
}
}
}
}
}
}
}
goto label_1933625;
}
}
}
}
else 
{
label_1941274:; 
goto label_1940782;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
else 
{
label_1817469:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1818116;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1818062;
}
else 
{
goto label_1818003;
}
}
else 
{
label_1818003:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1818062;
}
else 
{
goto label_1818010;
}
}
else 
{
label_1818010:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1818062;
}
else 
{
goto label_1818017;
}
}
else 
{
label_1818017:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1818062;
}
else 
{
goto label_1818024;
}
}
else 
{
label_1818024:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1818062;
}
else 
{
goto label_1818031;
}
}
else 
{
label_1818031:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1818040;
}
else 
{
label_1818040:; 
goto label_1818062;
}
}
else 
{
label_1818062:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1818069;
}
}
else 
{
label_1818069:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1818078;
}
else 
{
label_1818078:; 
return 1;
}
}
}
}
}
}
}
}
}
goto label_1816209;
}
}
}
}
else 
{
label_1818116:; 
goto label_1816394;
}
}
}
}
}
}
}
}
}
}
}
}
}
else 
{
if (((int)req_type) == 1)
{
{
int __tmp_44 = req_a;
int __tmp_45 = req_d;
int i = __tmp_44;
int v = __tmp_45;
if (i == 0)
{
s_memory0 = v;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_1815922;
label_1815922:; 
rsp_status = 1;
goto label_1815953;
}
}
else 
{
rsp_status = 0;
goto label_1815953;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
else 
{
label_1816385:; 
label_1832033:; 
if (((int)m_run_st) == 0)
{
goto label_1832046;
}
else 
{
label_1832046:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_1832057;
}
else 
{
label_1832057:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1832524;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1832184;
}
else 
{
goto label_1832125;
}
}
else 
{
label_1832125:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1832184;
}
else 
{
goto label_1832132;
}
}
else 
{
label_1832132:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1832184;
}
else 
{
goto label_1832139;
}
}
else 
{
label_1832139:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1832184;
}
else 
{
goto label_1832146;
}
}
else 
{
label_1832146:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1832184;
}
else 
{
goto label_1832153;
}
}
else 
{
label_1832153:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1832162;
}
else 
{
label_1832162:; 
goto label_1832184;
}
}
else 
{
label_1832184:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1832206;
}
else 
{
goto label_1832191;
}
}
else 
{
label_1832191:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1832200;
}
else 
{
label_1832200:; 
goto label_1832206;
}
}
else 
{
label_1832206:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_46 = req_a;
int i = __tmp_46;
int x;
if (i == 0)
{
x = s_memory0;
 __return_1832256 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_1832257 = x;
}
rsp_d = __return_1832256;
goto label_1832259;
rsp_d = __return_1832257;
label_1832259:; 
rsp_status = 1;
label_1832265:; 
c_rsp_type = rsp_type;
c_rsp_status = rsp_status;
c_rsp_d = rsp_d;
c_empty_rsp = 0;
c_write_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1832346;
}
else 
{
goto label_1832287;
}
}
else 
{
label_1832287:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1832346;
}
else 
{
goto label_1832294;
}
}
else 
{
label_1832294:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1832346;
}
else 
{
goto label_1832301;
}
}
else 
{
label_1832301:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1832346;
}
else 
{
goto label_1832308;
}
}
else 
{
label_1832308:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1832346;
}
else 
{
goto label_1832315;
}
}
else 
{
label_1832315:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1832324;
}
else 
{
label_1832324:; 
goto label_1832346;
}
}
else 
{
label_1832346:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1832368;
}
else 
{
goto label_1832353;
}
}
else 
{
label_1832353:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1832362;
}
else 
{
label_1832362:; 
goto label_1832368;
}
}
else 
{
label_1832368:; 
c_write_rsp_ev = 2;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1832451;
}
else 
{
goto label_1832392;
}
}
else 
{
label_1832392:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1832451;
}
else 
{
goto label_1832399;
}
}
else 
{
label_1832399:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1832451;
}
else 
{
goto label_1832406;
}
}
else 
{
label_1832406:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1832451;
}
else 
{
goto label_1832413;
}
}
else 
{
label_1832413:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1832451;
}
else 
{
goto label_1832420;
}
}
else 
{
label_1832420:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1832429;
}
else 
{
label_1832429:; 
goto label_1832451;
}
}
else 
{
label_1832451:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1832458;
}
}
else 
{
label_1832458:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1832467;
}
else 
{
label_1832467:; 
return 1;
}
}
}
}
}
}
}
}
}
goto label_1816209;
}
}
}
}
}
}
}
}
}
}
else 
{
if (((int)req_type) == 1)
{
{
int __tmp_47 = req_a;
int __tmp_48 = req_d;
int i = __tmp_47;
int v = __tmp_48;
if (i == 0)
{
s_memory0 = v;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_1832234;
label_1832234:; 
rsp_status = 1;
goto label_1832265;
}
}
else 
{
rsp_status = 0;
goto label_1832265;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
else 
{
label_1832524:; 
goto label_1832033;
}
}
}
}
}
}
}
}
}
}
}
}
}
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1946021;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1945971;
}
else 
{
goto label_1945912;
}
}
else 
{
label_1945912:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1945971;
}
else 
{
goto label_1945919;
}
}
else 
{
label_1945919:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1945971;
}
else 
{
goto label_1945926;
}
}
else 
{
label_1945926:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1945971;
}
else 
{
goto label_1945933;
}
}
else 
{
label_1945933:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1945971;
}
else 
{
goto label_1945940;
}
}
else 
{
label_1945940:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1945949;
}
else 
{
label_1945949:; 
goto label_1945971;
}
}
else 
{
label_1945971:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1945978;
}
}
else 
{
label_1945978:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1945987;
}
else 
{
label_1945987:; 
return 1;
}
}
}
}
}
}
}
}
}
label_1946017:; 
label_1946024:; 
if (((int)m_run_st) == 0)
{
goto label_1946038;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1946038:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_1946049;
}
else 
{
label_1946049:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1946225;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1946176;
}
else 
{
goto label_1946117;
}
}
else 
{
label_1946117:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1946176;
}
else 
{
goto label_1946124;
}
}
else 
{
label_1946124:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1946176;
}
else 
{
goto label_1946131;
}
}
else 
{
label_1946131:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1946176;
}
else 
{
goto label_1946138;
}
}
else 
{
label_1946138:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1946176;
}
else 
{
goto label_1946145;
}
}
else 
{
label_1946145:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1946154;
}
else 
{
label_1946154:; 
goto label_1946176;
}
}
else 
{
label_1946176:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1946183;
}
}
else 
{
label_1946183:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1946192;
}
else 
{
label_1946192:; 
return 1;
}
}
}
}
}
}
}
}
}
goto label_1946017;
}
}
}
}
else 
{
label_1946225:; 
goto label_1946024;
}
}
}
else 
{
}
goto label_1779497;
}
}
}
}
}
else 
{
label_1946021:; 
goto label_1946024;
}
}
}
}
}
else 
{
label_1815563:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1816387;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1816329;
}
else 
{
goto label_1816270;
}
}
else 
{
label_1816270:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1816329;
}
else 
{
goto label_1816277;
}
}
else 
{
label_1816277:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1816329;
}
else 
{
goto label_1816284;
}
}
else 
{
label_1816284:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1816329;
}
else 
{
goto label_1816291;
}
}
else 
{
label_1816291:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1816329;
}
else 
{
goto label_1816298;
}
}
else 
{
label_1816298:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1816307;
}
else 
{
label_1816307:; 
goto label_1816329;
}
}
else 
{
label_1816329:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1816336;
}
}
else 
{
label_1816336:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1816345;
}
else 
{
label_1816345:; 
return 1;
}
}
}
}
}
}
}
}
}
goto label_1815067;
}
}
}
}
else 
{
label_1816387:; 
goto label_1815067;
}
}
}
else 
{
}
goto label_1797306;
}
}
}
else 
{
}
goto label_1667328;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
else 
{
label_1648388:; 
goto label_1647068;
}
}
}
else 
{
}
kernel_st = 2;
kernel_st = 3;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1667094;
}
else 
{
goto label_1666622;
}
}
else 
{
label_1666622:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1667094;
}
else 
{
goto label_1666664;
}
}
else 
{
label_1666664:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1667094;
}
else 
{
goto label_1666734;
}
}
else 
{
label_1666734:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1667094;
}
else 
{
goto label_1666776;
}
}
else 
{
label_1666776:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1667094;
}
else 
{
goto label_1666846;
}
}
else 
{
label_1666846:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1666918;
}
else 
{
label_1666918:; 
goto label_1667094;
}
}
else 
{
label_1667094:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1667256;
}
else 
{
goto label_1667136;
}
}
else 
{
label_1667136:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1667208;
}
else 
{
label_1667208:; 
goto label_1667256;
}
}
else 
{
label_1667256:; 
if (((int)m_run_st) == 0)
{
goto label_1667352;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1667352:; 
goto label_1647058;
}
else 
{
}
label_1667328:; 
__retres1 = 0;
 __return_1950342 = __retres1;
return 1;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
