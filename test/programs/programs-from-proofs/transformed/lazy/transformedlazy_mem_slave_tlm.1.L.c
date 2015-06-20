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
int __return_1132677;
int __return_1132678;
int __return_1135973;
int __return_1135974;
int __return_1136965;
int __return_1136966;
int __return_1138101;
int __return_1138102;
int __return_1144350;
int __return_1144351;
int __return_1152295;
int __return_1140555;
int __return_1140556;
int __return_1143854;
int __return_1143855;
int __return_1138558;
int __return_1138559;
int __return_1136469;
int __return_1136470;
int __return_1121420;
int __return_1121421;
int __return_1124716;
int __return_1124717;
int __return_1125419;
int __return_1125420;
int __return_1152293;
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
goto label_897584;
}
else 
{
m_run_st = 2;
label_897584:; 
if (((int)s_run_i) == 1)
{
s_run_st = 0;
goto label_897591;
}
else 
{
s_run_st = 2;
label_897591:; 
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_897657;
}
else 
{
goto label_897598;
}
}
else 
{
label_897598:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_897657;
}
else 
{
goto label_897605;
}
}
else 
{
label_897605:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_897657;
}
else 
{
goto label_897612;
}
}
else 
{
label_897612:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_897657;
}
else 
{
goto label_897619;
}
}
else 
{
label_897619:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_897657;
}
else 
{
goto label_897626;
}
}
else 
{
label_897626:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_897635;
}
else 
{
label_897635:; 
goto label_897657;
}
}
else 
{
label_897657:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_897679;
}
else 
{
goto label_897664;
}
}
else 
{
label_897664:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_897673;
}
else 
{
label_897673:; 
goto label_897679;
}
}
else 
{
label_897679:; 
label_897681:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
label_897691:; 
if (((int)m_run_st) == 0)
{
goto label_897705;
}
else 
{
if (((int)s_run_st) == 0)
{
label_897705:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_898187;
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
goto label_897813;
}
else 
{
label_897813:; 
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
goto label_924923;
}
else 
{
goto label_924864;
}
}
else 
{
label_924864:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_924923;
}
else 
{
goto label_924871;
}
}
else 
{
label_924871:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_924923;
}
else 
{
goto label_924878;
}
}
else 
{
label_924878:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_924923;
}
else 
{
goto label_924885;
}
}
else 
{
label_924885:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_924923;
}
else 
{
goto label_924892;
}
}
else 
{
label_924892:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_924901;
}
else 
{
label_924901:; 
goto label_924923;
}
}
else 
{
label_924923:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_924945;
}
else 
{
goto label_924930;
}
}
else 
{
label_924930:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_924939;
}
else 
{
label_924939:; 
goto label_924945;
}
}
else 
{
label_924945:; 
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
goto label_925025;
}
else 
{
goto label_924966;
}
}
else 
{
label_924966:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_925025;
}
else 
{
goto label_924973;
}
}
else 
{
label_924973:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_925025;
}
else 
{
goto label_924980;
}
}
else 
{
label_924980:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_925025;
}
else 
{
goto label_924987;
}
}
else 
{
label_924987:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_925025;
}
else 
{
goto label_924994;
}
}
else 
{
label_924994:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_925003;
}
else 
{
label_925003:; 
goto label_925025;
}
}
else 
{
label_925025:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_925047;
}
else 
{
goto label_925032;
}
}
else 
{
label_925032:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_925041;
}
else 
{
label_925041:; 
goto label_925047;
}
}
else 
{
label_925047:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_925120;
}
else 
{
goto label_925061;
}
}
else 
{
label_925061:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_925120;
}
else 
{
goto label_925068;
}
}
else 
{
label_925068:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_925120;
}
else 
{
goto label_925075;
}
}
else 
{
label_925075:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_925120;
}
else 
{
goto label_925082;
}
}
else 
{
label_925082:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_925120;
}
else 
{
goto label_925089;
}
}
else 
{
label_925089:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_925098;
}
else 
{
label_925098:; 
goto label_925120;
}
}
else 
{
label_925120:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_925127;
}
}
else 
{
label_925127:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_925136;
}
else 
{
label_925136:; 
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
goto label_1131390;
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
goto label_1130561;
}
else 
{
label_1130561:; 
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
goto label_1130643;
}
else 
{
goto label_1130584;
}
}
else 
{
label_1130584:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1130643;
}
else 
{
goto label_1130591;
}
}
else 
{
label_1130591:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1130643;
}
else 
{
goto label_1130598;
}
}
else 
{
label_1130598:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1130643;
}
else 
{
goto label_1130605;
}
}
else 
{
label_1130605:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1130643;
}
else 
{
goto label_1130612;
}
}
else 
{
label_1130612:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1130621;
}
else 
{
label_1130621:; 
goto label_1130643;
}
}
else 
{
label_1130643:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1130665;
}
else 
{
goto label_1130650;
}
}
else 
{
label_1130650:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1130659;
}
else 
{
label_1130659:; 
goto label_1130665;
}
}
else 
{
label_1130665:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 1)
{
{
int __tmp_1 = req_a;
int __tmp_2 = req_d;
int i = __tmp_1;
int v = __tmp_2;
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
goto label_1130693;
label_1130693:; 
rsp_status = 1;
label_1130700:; 
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
goto label_1130779;
}
else 
{
goto label_1130720;
}
}
else 
{
label_1130720:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1130779;
}
else 
{
goto label_1130727;
}
}
else 
{
label_1130727:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1130779;
}
else 
{
goto label_1130734;
}
}
else 
{
label_1130734:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1130779;
}
else 
{
goto label_1130741;
}
}
else 
{
label_1130741:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1130779;
}
else 
{
goto label_1130748;
}
}
else 
{
label_1130748:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1130757;
}
else 
{
label_1130757:; 
goto label_1130779;
}
}
else 
{
label_1130779:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1130801;
}
else 
{
goto label_1130786;
}
}
else 
{
label_1130786:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1130795;
}
else 
{
label_1130795:; 
goto label_1130801;
}
}
else 
{
label_1130801:; 
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
goto label_1130884;
}
else 
{
goto label_1130825;
}
}
else 
{
label_1130825:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1130884;
}
else 
{
goto label_1130832;
}
}
else 
{
label_1130832:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1130884;
}
else 
{
goto label_1130839;
}
}
else 
{
label_1130839:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1130884;
}
else 
{
goto label_1130846;
}
}
else 
{
label_1130846:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1130884;
}
else 
{
goto label_1130853;
}
}
else 
{
label_1130853:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1130862;
}
else 
{
label_1130862:; 
goto label_1130884;
}
}
else 
{
label_1130884:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1130891;
}
}
else 
{
label_1130891:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1130900;
}
else 
{
label_1130900:; 
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
label_1130954:; 
label_1131395:; 
if (((int)m_run_st) == 0)
{
goto label_1131408;
}
else 
{
label_1131408:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_1132470;
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
goto label_1131596;
}
else 
{
goto label_1131537;
}
}
else 
{
label_1131537:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1131596;
}
else 
{
goto label_1131544;
}
}
else 
{
label_1131544:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1131596;
}
else 
{
goto label_1131551;
}
}
else 
{
label_1131551:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1131596;
}
else 
{
goto label_1131558;
}
}
else 
{
label_1131558:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1131596;
}
else 
{
goto label_1131565;
}
}
else 
{
label_1131565:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1131574;
}
else 
{
label_1131574:; 
goto label_1131596;
}
}
else 
{
label_1131596:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1131618;
}
else 
{
goto label_1131603;
}
}
else 
{
label_1131603:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1131612;
}
else 
{
label_1131612:; 
goto label_1131618;
}
}
else 
{
label_1131618:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1131691;
}
else 
{
goto label_1131632;
}
}
else 
{
label_1131632:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1131691;
}
else 
{
goto label_1131639;
}
}
else 
{
label_1131639:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1131691;
}
else 
{
goto label_1131646;
}
}
else 
{
label_1131646:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1131691;
}
else 
{
goto label_1131653;
}
}
else 
{
label_1131653:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1131691;
}
else 
{
goto label_1131660;
}
}
else 
{
label_1131660:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1131669;
}
else 
{
label_1131669:; 
goto label_1131691;
}
}
else 
{
label_1131691:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1131713;
}
else 
{
goto label_1131698;
}
}
else 
{
label_1131698:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1131707;
}
else 
{
label_1131707:; 
goto label_1131713;
}
}
else 
{
label_1131713:; 
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
goto label_1132178;
}
else 
{
goto label_1132119;
}
}
else 
{
label_1132119:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1132178;
}
else 
{
goto label_1132126;
}
}
else 
{
label_1132126:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1132178;
}
else 
{
goto label_1132133;
}
}
else 
{
label_1132133:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1132178;
}
else 
{
goto label_1132140;
}
}
else 
{
label_1132140:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1132178;
}
else 
{
goto label_1132147;
}
}
else 
{
label_1132147:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1132156;
}
else 
{
label_1132156:; 
goto label_1132178;
}
}
else 
{
label_1132178:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1132200;
}
else 
{
goto label_1132185;
}
}
else 
{
label_1132185:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1132194;
}
else 
{
label_1132194:; 
goto label_1132200;
}
}
else 
{
label_1132200:; 
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
goto label_1132280;
}
else 
{
goto label_1132221;
}
}
else 
{
label_1132221:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1132280;
}
else 
{
goto label_1132228;
}
}
else 
{
label_1132228:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1132280;
}
else 
{
goto label_1132235;
}
}
else 
{
label_1132235:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1132280;
}
else 
{
goto label_1132242;
}
}
else 
{
label_1132242:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1132280;
}
else 
{
goto label_1132249;
}
}
else 
{
label_1132249:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1132258;
}
else 
{
label_1132258:; 
goto label_1132280;
}
}
else 
{
label_1132280:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1132302;
}
else 
{
goto label_1132287;
}
}
else 
{
label_1132287:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1132296;
}
else 
{
label_1132296:; 
goto label_1132302;
}
}
else 
{
label_1132302:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1132375;
}
else 
{
goto label_1132316;
}
}
else 
{
label_1132316:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1132375;
}
else 
{
goto label_1132323;
}
}
else 
{
label_1132323:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1132375;
}
else 
{
goto label_1132330;
}
}
else 
{
label_1132330:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1132375;
}
else 
{
goto label_1132337;
}
}
else 
{
label_1132337:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1132375;
}
else 
{
goto label_1132344;
}
}
else 
{
label_1132344:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1132353;
}
else 
{
label_1132353:; 
goto label_1132375;
}
}
else 
{
label_1132375:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1132382;
}
}
else 
{
label_1132382:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1132391;
}
else 
{
label_1132391:; 
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
goto label_1130499;
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
goto label_1131824;
}
else 
{
goto label_1131765;
}
}
else 
{
label_1131765:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1131824;
}
else 
{
goto label_1131772;
}
}
else 
{
label_1131772:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1131824;
}
else 
{
goto label_1131779;
}
}
else 
{
label_1131779:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1131824;
}
else 
{
goto label_1131786;
}
}
else 
{
label_1131786:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1131824;
}
else 
{
goto label_1131793;
}
}
else 
{
label_1131793:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1131802;
}
else 
{
label_1131802:; 
goto label_1131824;
}
}
else 
{
label_1131824:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1131846;
}
else 
{
goto label_1131831;
}
}
else 
{
label_1131831:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1131840;
}
else 
{
label_1131840:; 
goto label_1131846;
}
}
else 
{
label_1131846:; 
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
goto label_1131926;
}
else 
{
goto label_1131867;
}
}
else 
{
label_1131867:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1131926;
}
else 
{
goto label_1131874;
}
}
else 
{
label_1131874:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1131926;
}
else 
{
goto label_1131881;
}
}
else 
{
label_1131881:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1131926;
}
else 
{
goto label_1131888;
}
}
else 
{
label_1131888:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1131926;
}
else 
{
goto label_1131895;
}
}
else 
{
label_1131895:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1131904;
}
else 
{
label_1131904:; 
goto label_1131926;
}
}
else 
{
label_1131926:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1131948;
}
else 
{
goto label_1131933;
}
}
else 
{
label_1131933:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1131942;
}
else 
{
label_1131942:; 
goto label_1131948;
}
}
else 
{
label_1131948:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1132021;
}
else 
{
goto label_1131962;
}
}
else 
{
label_1131962:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1132021;
}
else 
{
goto label_1131969;
}
}
else 
{
label_1131969:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1132021;
}
else 
{
goto label_1131976;
}
}
else 
{
label_1131976:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1132021;
}
else 
{
goto label_1131983;
}
}
else 
{
label_1131983:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1132021;
}
else 
{
goto label_1131990;
}
}
else 
{
label_1131990:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1131999;
}
else 
{
label_1131999:; 
goto label_1132021;
}
}
else 
{
label_1132021:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1132028;
}
}
else 
{
label_1132028:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1132037;
}
else 
{
label_1132037:; 
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
goto label_1133115;
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
goto label_1132605;
}
else 
{
goto label_1132546;
}
}
else 
{
label_1132546:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1132605;
}
else 
{
goto label_1132553;
}
}
else 
{
label_1132553:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1132605;
}
else 
{
goto label_1132560;
}
}
else 
{
label_1132560:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1132605;
}
else 
{
goto label_1132567;
}
}
else 
{
label_1132567:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1132605;
}
else 
{
goto label_1132574;
}
}
else 
{
label_1132574:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1132583;
}
else 
{
label_1132583:; 
goto label_1132605;
}
}
else 
{
label_1132605:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1132627;
}
else 
{
goto label_1132612;
}
}
else 
{
label_1132612:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1132621;
}
else 
{
label_1132621:; 
goto label_1132627;
}
}
else 
{
label_1132627:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_3 = req_a;
int i = __tmp_3;
int x;
if (i == 0)
{
x = s_memory0;
 __return_1132677 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_1132678 = x;
}
rsp_d = __return_1132677;
goto label_1132680;
rsp_d = __return_1132678;
label_1132680:; 
rsp_status = 1;
label_1132686:; 
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
goto label_1132767;
}
else 
{
goto label_1132708;
}
}
else 
{
label_1132708:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1132767;
}
else 
{
goto label_1132715;
}
}
else 
{
label_1132715:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1132767;
}
else 
{
goto label_1132722;
}
}
else 
{
label_1132722:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1132767;
}
else 
{
goto label_1132729;
}
}
else 
{
label_1132729:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1132767;
}
else 
{
goto label_1132736;
}
}
else 
{
label_1132736:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1132745;
}
else 
{
label_1132745:; 
goto label_1132767;
}
}
else 
{
label_1132767:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1132789;
}
else 
{
goto label_1132774;
}
}
else 
{
label_1132774:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1132783;
}
else 
{
label_1132783:; 
goto label_1132789;
}
}
else 
{
label_1132789:; 
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
goto label_1132872;
}
else 
{
goto label_1132813;
}
}
else 
{
label_1132813:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1132872;
}
else 
{
goto label_1132820;
}
}
else 
{
label_1132820:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1132872;
}
else 
{
goto label_1132827;
}
}
else 
{
label_1132827:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1132872;
}
else 
{
goto label_1132834;
}
}
else 
{
label_1132834:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1132872;
}
else 
{
goto label_1132841;
}
}
else 
{
label_1132841:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1132850;
}
else 
{
label_1132850:; 
goto label_1132872;
}
}
else 
{
label_1132872:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1132879;
}
}
else 
{
label_1132879:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1132888;
}
else 
{
label_1132888:; 
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
label_1134382:; 
label_1134386:; 
if (((int)m_run_st) == 0)
{
goto label_1134399;
}
else 
{
label_1134399:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_1134755;
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
goto label_1134587;
}
else 
{
goto label_1134528;
}
}
else 
{
label_1134528:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1134587;
}
else 
{
goto label_1134535;
}
}
else 
{
label_1134535:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1134587;
}
else 
{
goto label_1134542;
}
}
else 
{
label_1134542:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1134587;
}
else 
{
goto label_1134549;
}
}
else 
{
label_1134549:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1134587;
}
else 
{
goto label_1134556;
}
}
else 
{
label_1134556:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1134565;
}
else 
{
label_1134565:; 
goto label_1134587;
}
}
else 
{
label_1134587:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1134609;
}
else 
{
goto label_1134594;
}
}
else 
{
label_1134594:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1134603;
}
else 
{
label_1134603:; 
goto label_1134609;
}
}
else 
{
label_1134609:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1134682;
}
else 
{
goto label_1134623;
}
}
else 
{
label_1134623:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1134682;
}
else 
{
goto label_1134630;
}
}
else 
{
label_1134630:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1134682;
}
else 
{
goto label_1134637;
}
}
else 
{
label_1134637:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1134682;
}
else 
{
goto label_1134644;
}
}
else 
{
label_1134644:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1134682;
}
else 
{
goto label_1134651;
}
}
else 
{
label_1134651:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1134660;
}
else 
{
label_1134660:; 
goto label_1134682;
}
}
else 
{
label_1134682:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1134704;
}
else 
{
goto label_1134689;
}
}
else 
{
label_1134689:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1134698;
}
else 
{
label_1134698:; 
goto label_1134704;
}
}
else 
{
label_1134704:; 
c_m_ev = 2;
if ((req_a___0 + 50) == rsp_d___0)
{
a = a + 1;
goto label_1134721;
}
else 
{
{
__VERIFIER_error();
}
a = a + 1;
label_1134721:; 
}
label_1134750:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1135107;
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
goto label_1134890;
}
else 
{
goto label_1134831;
}
}
else 
{
label_1134831:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1134890;
}
else 
{
goto label_1134838;
}
}
else 
{
label_1134838:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1134890;
}
else 
{
goto label_1134845;
}
}
else 
{
label_1134845:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1134890;
}
else 
{
goto label_1134852;
}
}
else 
{
label_1134852:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1134890;
}
else 
{
goto label_1134859;
}
}
else 
{
label_1134859:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1134868;
}
else 
{
label_1134868:; 
goto label_1134890;
}
}
else 
{
label_1134890:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1134897;
}
}
else 
{
label_1134897:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1134906;
}
else 
{
label_1134906:; 
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
label_1134936:; 
label_1135112:; 
if (((int)m_run_st) == 0)
{
goto label_1135126;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1135126:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_1135359;
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
goto label_1135314;
}
else 
{
goto label_1135255;
}
}
else 
{
label_1135255:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1135314;
}
else 
{
goto label_1135262;
}
}
else 
{
label_1135262:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1135314;
}
else 
{
goto label_1135269;
}
}
else 
{
label_1135269:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1135314;
}
else 
{
goto label_1135276;
}
}
else 
{
label_1135276:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1135314;
}
else 
{
goto label_1135283;
}
}
else 
{
label_1135283:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1135292;
}
else 
{
label_1135292:; 
goto label_1135314;
}
}
else 
{
label_1135314:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1135321;
}
}
else 
{
label_1135321:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1135330;
}
else 
{
label_1135330:; 
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
goto label_1134750;
}
}
}
}
else 
{
label_1135359:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1135535;
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
goto label_1135486;
}
else 
{
goto label_1135427;
}
}
else 
{
label_1135427:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1135486;
}
else 
{
goto label_1135434;
}
}
else 
{
label_1135434:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1135486;
}
else 
{
goto label_1135441;
}
}
else 
{
label_1135441:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1135486;
}
else 
{
goto label_1135448;
}
}
else 
{
label_1135448:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1135486;
}
else 
{
goto label_1135455;
}
}
else 
{
label_1135455:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1135464;
}
else 
{
label_1135464:; 
goto label_1135486;
}
}
else 
{
label_1135486:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1135493;
}
}
else 
{
label_1135493:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1135502;
}
else 
{
label_1135502:; 
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
goto label_1134936;
}
}
}
}
else 
{
label_1135535:; 
goto label_1135112;
}
}
}
else 
{
}
goto label_1145784;
}
}
}
}
}
else 
{
label_1135107:; 
goto label_1135112;
}
}
}
}
}
}
}
}
}
}
}
}
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
label_1134755:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1135109;
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
goto label_1135056;
}
else 
{
goto label_1134997;
}
}
else 
{
label_1134997:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1135056;
}
else 
{
goto label_1135004;
}
}
else 
{
label_1135004:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1135056;
}
else 
{
goto label_1135011;
}
}
else 
{
label_1135011:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1135056;
}
else 
{
goto label_1135018;
}
}
else 
{
label_1135018:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1135056;
}
else 
{
goto label_1135025;
}
}
else 
{
label_1135025:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1135034;
}
else 
{
label_1135034:; 
goto label_1135056;
}
}
else 
{
label_1135056:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1135063;
}
}
else 
{
label_1135063:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1135072;
}
else 
{
label_1135072:; 
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
goto label_1134382;
}
}
}
}
else 
{
label_1135109:; 
goto label_1134386;
}
}
}
}
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
int __tmp_4 = req_a;
int __tmp_5 = req_d;
int i = __tmp_4;
int v = __tmp_5;
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
goto label_1132655;
label_1132655:; 
rsp_status = 1;
goto label_1132686;
}
}
else 
{
rsp_status = 0;
goto label_1132686;
}
}
}
}
}
}
}
}
}
}
}
label_1132941:; 
label_1135543:; 
if (((int)m_run_st) == 0)
{
goto label_1135557;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1135557:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_1135568;
}
else 
{
label_1135568:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1135744;
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
goto label_1135695;
}
else 
{
goto label_1135636;
}
}
else 
{
label_1135636:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1135695;
}
else 
{
goto label_1135643;
}
}
else 
{
label_1135643:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1135695;
}
else 
{
goto label_1135650;
}
}
else 
{
label_1135650:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1135695;
}
else 
{
goto label_1135657;
}
}
else 
{
label_1135657:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1135695;
}
else 
{
goto label_1135664;
}
}
else 
{
label_1135664:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1135673;
}
else 
{
label_1135673:; 
goto label_1135695;
}
}
else 
{
label_1135695:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1135702;
}
}
else 
{
label_1135702:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1135711;
}
else 
{
label_1135711:; 
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
goto label_1132941;
}
}
}
}
else 
{
label_1135744:; 
goto label_1135543;
}
}
}
else 
{
}
goto label_1151252;
}
}
}
}
}
else 
{
label_1133115:; 
label_1135750:; 
if (((int)m_run_st) == 0)
{
goto label_1135763;
}
else 
{
label_1135763:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_1135774;
}
else 
{
label_1135774:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1136242;
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
goto label_1135901;
}
else 
{
goto label_1135842;
}
}
else 
{
label_1135842:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1135901;
}
else 
{
goto label_1135849;
}
}
else 
{
label_1135849:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1135901;
}
else 
{
goto label_1135856;
}
}
else 
{
label_1135856:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1135901;
}
else 
{
goto label_1135863;
}
}
else 
{
label_1135863:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1135901;
}
else 
{
goto label_1135870;
}
}
else 
{
label_1135870:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1135879;
}
else 
{
label_1135879:; 
goto label_1135901;
}
}
else 
{
label_1135901:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1135923;
}
else 
{
goto label_1135908;
}
}
else 
{
label_1135908:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1135917;
}
else 
{
label_1135917:; 
goto label_1135923;
}
}
else 
{
label_1135923:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_6 = req_a;
int i = __tmp_6;
int x;
if (i == 0)
{
x = s_memory0;
 __return_1135973 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_1135974 = x;
}
rsp_d = __return_1135973;
goto label_1135976;
rsp_d = __return_1135974;
label_1135976:; 
rsp_status = 1;
label_1135982:; 
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
goto label_1136063;
}
else 
{
goto label_1136004;
}
}
else 
{
label_1136004:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1136063;
}
else 
{
goto label_1136011;
}
}
else 
{
label_1136011:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1136063;
}
else 
{
goto label_1136018;
}
}
else 
{
label_1136018:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1136063;
}
else 
{
goto label_1136025;
}
}
else 
{
label_1136025:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1136063;
}
else 
{
goto label_1136032;
}
}
else 
{
label_1136032:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1136041;
}
else 
{
label_1136041:; 
goto label_1136063;
}
}
else 
{
label_1136063:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1136085;
}
else 
{
goto label_1136070;
}
}
else 
{
label_1136070:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1136079;
}
else 
{
label_1136079:; 
goto label_1136085;
}
}
else 
{
label_1136085:; 
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
goto label_1136168;
}
else 
{
goto label_1136109;
}
}
else 
{
label_1136109:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1136168;
}
else 
{
goto label_1136116;
}
}
else 
{
label_1136116:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1136168;
}
else 
{
goto label_1136123;
}
}
else 
{
label_1136123:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1136168;
}
else 
{
goto label_1136130;
}
}
else 
{
label_1136130:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1136168;
}
else 
{
goto label_1136137;
}
}
else 
{
label_1136137:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1136146;
}
else 
{
label_1136146:; 
goto label_1136168;
}
}
else 
{
label_1136168:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1136175;
}
}
else 
{
label_1136175:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1136184;
}
else 
{
label_1136184:; 
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
goto label_1134382;
}
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
int __tmp_7 = req_a;
int __tmp_8 = req_d;
int i = __tmp_7;
int v = __tmp_8;
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
goto label_1135951;
label_1135951:; 
rsp_status = 1;
goto label_1135982;
}
}
else 
{
rsp_status = 0;
goto label_1135982;
}
}
}
}
}
}
}
}
}
}
}
goto label_1132941;
}
}
}
}
else 
{
label_1136242:; 
goto label_1135750;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_1132470:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1133117;
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
goto label_1133063;
}
else 
{
goto label_1133004;
}
}
else 
{
label_1133004:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1133063;
}
else 
{
goto label_1133011;
}
}
else 
{
label_1133011:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1133063;
}
else 
{
goto label_1133018;
}
}
else 
{
label_1133018:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1133063;
}
else 
{
goto label_1133025;
}
}
else 
{
label_1133025:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1133063;
}
else 
{
goto label_1133032;
}
}
else 
{
label_1133032:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1133041;
}
else 
{
label_1133041:; 
goto label_1133063;
}
}
else 
{
label_1133063:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1133070;
}
}
else 
{
label_1133070:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1133079;
}
else 
{
label_1133079:; 
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
goto label_1130954;
}
}
}
}
else 
{
label_1133117:; 
goto label_1131395;
}
}
}
}
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
rsp_status = 0;
goto label_1130700;
}
}
}
}
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
label_1131390:; 
label_1136741:; 
if (((int)m_run_st) == 0)
{
goto label_1136755;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1136755:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_1136766;
}
else 
{
label_1136766:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1137233;
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
goto label_1136811;
}
else 
{
label_1136811:; 
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
goto label_1136893;
}
else 
{
goto label_1136834;
}
}
else 
{
label_1136834:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1136893;
}
else 
{
goto label_1136841;
}
}
else 
{
label_1136841:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1136893;
}
else 
{
goto label_1136848;
}
}
else 
{
label_1136848:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1136893;
}
else 
{
goto label_1136855;
}
}
else 
{
label_1136855:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1136893;
}
else 
{
goto label_1136862;
}
}
else 
{
label_1136862:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1136871;
}
else 
{
label_1136871:; 
goto label_1136893;
}
}
else 
{
label_1136893:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1136915;
}
else 
{
goto label_1136900;
}
}
else 
{
label_1136900:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1136909;
}
else 
{
label_1136909:; 
goto label_1136915;
}
}
else 
{
label_1136915:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_9 = req_a;
int i = __tmp_9;
int x;
if (i == 0)
{
x = s_memory0;
 __return_1136965 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_1136966 = x;
}
rsp_d = __return_1136965;
goto label_1136968;
rsp_d = __return_1136966;
label_1136968:; 
rsp_status = 1;
label_1136974:; 
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
goto label_1137055;
}
else 
{
goto label_1136996;
}
}
else 
{
label_1136996:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1137055;
}
else 
{
goto label_1137003;
}
}
else 
{
label_1137003:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1137055;
}
else 
{
goto label_1137010;
}
}
else 
{
label_1137010:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1137055;
}
else 
{
goto label_1137017;
}
}
else 
{
label_1137017:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1137055;
}
else 
{
goto label_1137024;
}
}
else 
{
label_1137024:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1137033;
}
else 
{
label_1137033:; 
goto label_1137055;
}
}
else 
{
label_1137055:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1137077;
}
else 
{
goto label_1137062;
}
}
else 
{
label_1137062:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1137071;
}
else 
{
label_1137071:; 
goto label_1137077;
}
}
else 
{
label_1137077:; 
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
goto label_1137160;
}
else 
{
goto label_1137101;
}
}
else 
{
label_1137101:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1137160;
}
else 
{
goto label_1137108;
}
}
else 
{
label_1137108:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1137160;
}
else 
{
goto label_1137115;
}
}
else 
{
label_1137115:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1137160;
}
else 
{
goto label_1137122;
}
}
else 
{
label_1137122:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1137160;
}
else 
{
goto label_1137129;
}
}
else 
{
label_1137129:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1137138;
}
else 
{
label_1137138:; 
goto label_1137160;
}
}
else 
{
label_1137160:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1137167;
}
}
else 
{
label_1137167:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1137176;
}
else 
{
label_1137176:; 
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
goto label_1130954;
}
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
int __tmp_10 = req_a;
int __tmp_11 = req_d;
int i = __tmp_10;
int v = __tmp_11;
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
goto label_1136943;
label_1136943:; 
rsp_status = 1;
goto label_1136974;
}
}
else 
{
rsp_status = 0;
goto label_1136974;
}
}
}
}
}
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
label_1137233:; 
goto label_1136741;
}
}
}
else 
{
}
label_1136752:; 
kernel_st = 2;
kernel_st = 3;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1137435;
}
else 
{
goto label_1137258;
}
}
else 
{
label_1137258:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1137435;
}
else 
{
goto label_1137283;
}
}
else 
{
label_1137283:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1137435;
}
else 
{
goto label_1137300;
}
}
else 
{
label_1137300:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1137435;
}
else 
{
goto label_1137325;
}
}
else 
{
label_1137325:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1137435;
}
else 
{
goto label_1137342;
}
}
else 
{
label_1137342:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1137369;
}
else 
{
label_1137369:; 
goto label_1137435;
}
}
else 
{
label_1137435:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1137505;
}
else 
{
goto label_1137460;
}
}
else 
{
label_1137460:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1137487;
}
else 
{
label_1137487:; 
goto label_1137505;
}
}
else 
{
label_1137505:; 
if (((int)m_run_st) == 0)
{
goto label_1137541;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1137541:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
label_1137553:; 
if (((int)m_run_st) == 0)
{
goto label_1137567;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1137567:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_1137894;
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
goto label_1137755;
}
else 
{
goto label_1137696;
}
}
else 
{
label_1137696:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1137755;
}
else 
{
goto label_1137703;
}
}
else 
{
label_1137703:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1137755;
}
else 
{
goto label_1137710;
}
}
else 
{
label_1137710:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1137755;
}
else 
{
goto label_1137717;
}
}
else 
{
label_1137717:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1137755;
}
else 
{
goto label_1137724;
}
}
else 
{
label_1137724:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1137733;
}
else 
{
label_1137733:; 
goto label_1137755;
}
}
else 
{
label_1137755:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1137777;
}
else 
{
goto label_1137762;
}
}
else 
{
label_1137762:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1137771;
}
else 
{
label_1137771:; 
goto label_1137777;
}
}
else 
{
label_1137777:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1137850;
}
else 
{
goto label_1137791;
}
}
else 
{
label_1137791:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1137850;
}
else 
{
goto label_1137798;
}
}
else 
{
label_1137798:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1137850;
}
else 
{
goto label_1137805;
}
}
else 
{
label_1137805:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1137850;
}
else 
{
goto label_1137812;
}
}
else 
{
label_1137812:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1137850;
}
else 
{
goto label_1137819;
}
}
else 
{
label_1137819:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1137828;
}
else 
{
label_1137828:; 
goto label_1137850;
}
}
else 
{
label_1137850:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1137857;
}
}
else 
{
label_1137857:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1137866;
}
else 
{
label_1137866:; 
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
goto label_1138828;
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
goto label_1137947;
}
else 
{
label_1137947:; 
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
goto label_1138029;
}
else 
{
goto label_1137970;
}
}
else 
{
label_1137970:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1138029;
}
else 
{
goto label_1137977;
}
}
else 
{
label_1137977:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1138029;
}
else 
{
goto label_1137984;
}
}
else 
{
label_1137984:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1138029;
}
else 
{
goto label_1137991;
}
}
else 
{
label_1137991:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1138029;
}
else 
{
goto label_1137998;
}
}
else 
{
label_1137998:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1138007;
}
else 
{
label_1138007:; 
goto label_1138029;
}
}
else 
{
label_1138029:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1138051;
}
else 
{
goto label_1138036;
}
}
else 
{
label_1138036:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1138045;
}
else 
{
label_1138045:; 
goto label_1138051;
}
}
else 
{
label_1138051:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_12 = req_a;
int i = __tmp_12;
int x;
if (i == 0)
{
x = s_memory0;
 __return_1138101 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_1138102 = x;
}
rsp_d = __return_1138101;
goto label_1138104;
rsp_d = __return_1138102;
label_1138104:; 
rsp_status = 1;
label_1138110:; 
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
goto label_1138191;
}
else 
{
goto label_1138132;
}
}
else 
{
label_1138132:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1138191;
}
else 
{
goto label_1138139;
}
}
else 
{
label_1138139:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1138191;
}
else 
{
goto label_1138146;
}
}
else 
{
label_1138146:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1138191;
}
else 
{
goto label_1138153;
}
}
else 
{
label_1138153:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1138191;
}
else 
{
goto label_1138160;
}
}
else 
{
label_1138160:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1138169;
}
else 
{
label_1138169:; 
goto label_1138191;
}
}
else 
{
label_1138191:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1138213;
}
else 
{
goto label_1138198;
}
}
else 
{
label_1138198:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1138207;
}
else 
{
label_1138207:; 
goto label_1138213;
}
}
else 
{
label_1138213:; 
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
goto label_1138296;
}
else 
{
goto label_1138237;
}
}
else 
{
label_1138237:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1138296;
}
else 
{
goto label_1138244;
}
}
else 
{
label_1138244:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1138296;
}
else 
{
goto label_1138251;
}
}
else 
{
label_1138251:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1138296;
}
else 
{
goto label_1138258;
}
}
else 
{
label_1138258:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1138296;
}
else 
{
goto label_1138265;
}
}
else 
{
label_1138265:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1138274;
}
else 
{
label_1138274:; 
goto label_1138296;
}
}
else 
{
label_1138296:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1138303;
}
}
else 
{
label_1138303:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1138312;
}
else 
{
label_1138312:; 
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
label_1138366:; 
label_1138833:; 
if (((int)m_run_st) == 0)
{
goto label_1138846;
}
else 
{
label_1138846:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_1139909;
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
goto label_1139034;
}
else 
{
goto label_1138975;
}
}
else 
{
label_1138975:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1139034;
}
else 
{
goto label_1138982;
}
}
else 
{
label_1138982:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1139034;
}
else 
{
goto label_1138989;
}
}
else 
{
label_1138989:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1139034;
}
else 
{
goto label_1138996;
}
}
else 
{
label_1138996:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1139034;
}
else 
{
goto label_1139003;
}
}
else 
{
label_1139003:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1139012;
}
else 
{
label_1139012:; 
goto label_1139034;
}
}
else 
{
label_1139034:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1139056;
}
else 
{
goto label_1139041;
}
}
else 
{
label_1139041:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1139050;
}
else 
{
label_1139050:; 
goto label_1139056;
}
}
else 
{
label_1139056:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1139129;
}
else 
{
goto label_1139070;
}
}
else 
{
label_1139070:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1139129;
}
else 
{
goto label_1139077;
}
}
else 
{
label_1139077:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1139129;
}
else 
{
goto label_1139084;
}
}
else 
{
label_1139084:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1139129;
}
else 
{
goto label_1139091;
}
}
else 
{
label_1139091:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1139129;
}
else 
{
goto label_1139098;
}
}
else 
{
label_1139098:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1139107;
}
else 
{
label_1139107:; 
goto label_1139129;
}
}
else 
{
label_1139129:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1139151;
}
else 
{
goto label_1139136;
}
}
else 
{
label_1139136:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1139145;
}
else 
{
label_1139145:; 
goto label_1139151;
}
}
else 
{
label_1139151:; 
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
goto label_1139616;
}
else 
{
goto label_1139557;
}
}
else 
{
label_1139557:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1139616;
}
else 
{
goto label_1139564;
}
}
else 
{
label_1139564:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1139616;
}
else 
{
goto label_1139571;
}
}
else 
{
label_1139571:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1139616;
}
else 
{
goto label_1139578;
}
}
else 
{
label_1139578:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1139616;
}
else 
{
goto label_1139585;
}
}
else 
{
label_1139585:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1139594;
}
else 
{
label_1139594:; 
goto label_1139616;
}
}
else 
{
label_1139616:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1139638;
}
else 
{
goto label_1139623;
}
}
else 
{
label_1139623:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1139632;
}
else 
{
label_1139632:; 
goto label_1139638;
}
}
else 
{
label_1139638:; 
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
goto label_1139718;
}
else 
{
goto label_1139659;
}
}
else 
{
label_1139659:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1139718;
}
else 
{
goto label_1139666;
}
}
else 
{
label_1139666:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1139718;
}
else 
{
goto label_1139673;
}
}
else 
{
label_1139673:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1139718;
}
else 
{
goto label_1139680;
}
}
else 
{
label_1139680:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1139718;
}
else 
{
goto label_1139687;
}
}
else 
{
label_1139687:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1139696;
}
else 
{
label_1139696:; 
goto label_1139718;
}
}
else 
{
label_1139718:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1139740;
}
else 
{
goto label_1139725;
}
}
else 
{
label_1139725:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1139734;
}
else 
{
label_1139734:; 
goto label_1139740;
}
}
else 
{
label_1139740:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1139813;
}
else 
{
goto label_1139754;
}
}
else 
{
label_1139754:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1139813;
}
else 
{
goto label_1139761;
}
}
else 
{
label_1139761:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1139813;
}
else 
{
goto label_1139768;
}
}
else 
{
label_1139768:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1139813;
}
else 
{
goto label_1139775;
}
}
else 
{
label_1139775:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1139813;
}
else 
{
goto label_1139782;
}
}
else 
{
label_1139782:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1139791;
}
else 
{
label_1139791:; 
goto label_1139813;
}
}
else 
{
label_1139813:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1139820;
}
}
else 
{
label_1139820:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1139829;
}
else 
{
label_1139829:; 
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
goto label_1140994;
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
goto label_1140052;
}
else 
{
goto label_1139993;
}
}
else 
{
label_1139993:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1140052;
}
else 
{
goto label_1140000;
}
}
else 
{
label_1140000:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1140052;
}
else 
{
goto label_1140007;
}
}
else 
{
label_1140007:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1140052;
}
else 
{
goto label_1140014;
}
}
else 
{
label_1140014:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1140052;
}
else 
{
goto label_1140021;
}
}
else 
{
label_1140021:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1140030;
}
else 
{
label_1140030:; 
goto label_1140052;
}
}
else 
{
label_1140052:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1140074;
}
else 
{
goto label_1140059;
}
}
else 
{
label_1140059:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1140068;
}
else 
{
label_1140068:; 
goto label_1140074;
}
}
else 
{
label_1140074:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 1)
{
{
int __tmp_13 = req_a;
int __tmp_14 = req_d;
int i = __tmp_13;
int v = __tmp_14;
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
goto label_1140102;
label_1140102:; 
rsp_status = 1;
label_1140109:; 
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
goto label_1140188;
}
else 
{
goto label_1140129;
}
}
else 
{
label_1140129:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1140188;
}
else 
{
goto label_1140136;
}
}
else 
{
label_1140136:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1140188;
}
else 
{
goto label_1140143;
}
}
else 
{
label_1140143:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1140188;
}
else 
{
goto label_1140150;
}
}
else 
{
label_1140150:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1140188;
}
else 
{
goto label_1140157;
}
}
else 
{
label_1140157:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1140166;
}
else 
{
label_1140166:; 
goto label_1140188;
}
}
else 
{
label_1140188:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1140210;
}
else 
{
goto label_1140195;
}
}
else 
{
label_1140195:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1140204;
}
else 
{
label_1140204:; 
goto label_1140210;
}
}
else 
{
label_1140210:; 
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
goto label_1140293;
}
else 
{
goto label_1140234;
}
}
else 
{
label_1140234:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1140293;
}
else 
{
goto label_1140241;
}
}
else 
{
label_1140241:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1140293;
}
else 
{
goto label_1140248;
}
}
else 
{
label_1140248:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1140293;
}
else 
{
goto label_1140255;
}
}
else 
{
label_1140255:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1140293;
}
else 
{
goto label_1140262;
}
}
else 
{
label_1140262:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1140271;
}
else 
{
label_1140271:; 
goto label_1140293;
}
}
else 
{
label_1140293:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1140300;
}
}
else 
{
label_1140300:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1140309;
}
else 
{
label_1140309:; 
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
goto label_1138366;
}
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
rsp_status = 0;
goto label_1140109;
}
}
}
}
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
label_1140994:; 
label_1144127:; 
if (((int)m_run_st) == 0)
{
goto label_1144140;
}
else 
{
label_1144140:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_1144151;
}
else 
{
label_1144151:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1144618;
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
goto label_1144278;
}
else 
{
goto label_1144219;
}
}
else 
{
label_1144219:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1144278;
}
else 
{
goto label_1144226;
}
}
else 
{
label_1144226:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1144278;
}
else 
{
goto label_1144233;
}
}
else 
{
label_1144233:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1144278;
}
else 
{
goto label_1144240;
}
}
else 
{
label_1144240:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1144278;
}
else 
{
goto label_1144247;
}
}
else 
{
label_1144247:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1144256;
}
else 
{
label_1144256:; 
goto label_1144278;
}
}
else 
{
label_1144278:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1144300;
}
else 
{
goto label_1144285;
}
}
else 
{
label_1144285:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1144294;
}
else 
{
label_1144294:; 
goto label_1144300;
}
}
else 
{
label_1144300:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_15 = req_a;
int i = __tmp_15;
int x;
if (i == 0)
{
x = s_memory0;
 __return_1144350 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_1144351 = x;
}
rsp_d = __return_1144350;
goto label_1144353;
rsp_d = __return_1144351;
label_1144353:; 
rsp_status = 1;
label_1144359:; 
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
goto label_1144440;
}
else 
{
goto label_1144381;
}
}
else 
{
label_1144381:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1144440;
}
else 
{
goto label_1144388;
}
}
else 
{
label_1144388:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1144440;
}
else 
{
goto label_1144395;
}
}
else 
{
label_1144395:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1144440;
}
else 
{
goto label_1144402;
}
}
else 
{
label_1144402:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1144440;
}
else 
{
goto label_1144409;
}
}
else 
{
label_1144409:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1144418;
}
else 
{
label_1144418:; 
goto label_1144440;
}
}
else 
{
label_1144440:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1144462;
}
else 
{
goto label_1144447;
}
}
else 
{
label_1144447:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1144456;
}
else 
{
label_1144456:; 
goto label_1144462;
}
}
else 
{
label_1144462:; 
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
goto label_1144545;
}
else 
{
goto label_1144486;
}
}
else 
{
label_1144486:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1144545;
}
else 
{
goto label_1144493;
}
}
else 
{
label_1144493:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1144545;
}
else 
{
goto label_1144500;
}
}
else 
{
label_1144500:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1144545;
}
else 
{
goto label_1144507;
}
}
else 
{
label_1144507:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1144545;
}
else 
{
goto label_1144514;
}
}
else 
{
label_1144514:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1144523;
}
else 
{
label_1144523:; 
goto label_1144545;
}
}
else 
{
label_1144545:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1144552;
}
}
else 
{
label_1144552:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1144561;
}
else 
{
label_1144561:; 
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
goto label_1138366;
}
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
int __tmp_16 = req_a;
int __tmp_17 = req_d;
int i = __tmp_16;
int v = __tmp_17;
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
goto label_1144328;
label_1144328:; 
rsp_status = 1;
goto label_1144359;
}
}
else 
{
rsp_status = 0;
goto label_1144359;
}
}
}
}
}
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
label_1144618:; 
goto label_1144127;
}
}
}
}
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
goto label_1139262;
}
else 
{
goto label_1139203;
}
}
else 
{
label_1139203:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1139262;
}
else 
{
goto label_1139210;
}
}
else 
{
label_1139210:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1139262;
}
else 
{
goto label_1139217;
}
}
else 
{
label_1139217:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1139262;
}
else 
{
goto label_1139224;
}
}
else 
{
label_1139224:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1139262;
}
else 
{
goto label_1139231;
}
}
else 
{
label_1139231:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1139240;
}
else 
{
label_1139240:; 
goto label_1139262;
}
}
else 
{
label_1139262:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1139284;
}
else 
{
goto label_1139269;
}
}
else 
{
label_1139269:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1139278;
}
else 
{
label_1139278:; 
goto label_1139284;
}
}
else 
{
label_1139284:; 
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
goto label_1139364;
}
else 
{
goto label_1139305;
}
}
else 
{
label_1139305:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1139364;
}
else 
{
goto label_1139312;
}
}
else 
{
label_1139312:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1139364;
}
else 
{
goto label_1139319;
}
}
else 
{
label_1139319:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1139364;
}
else 
{
goto label_1139326;
}
}
else 
{
label_1139326:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1139364;
}
else 
{
goto label_1139333;
}
}
else 
{
label_1139333:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1139342;
}
else 
{
label_1139342:; 
goto label_1139364;
}
}
else 
{
label_1139364:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1139386;
}
else 
{
goto label_1139371;
}
}
else 
{
label_1139371:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1139380;
}
else 
{
label_1139380:; 
goto label_1139386;
}
}
else 
{
label_1139386:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1139459;
}
else 
{
goto label_1139400;
}
}
else 
{
label_1139400:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1139459;
}
else 
{
goto label_1139407;
}
}
else 
{
label_1139407:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1139459;
}
else 
{
goto label_1139414;
}
}
else 
{
label_1139414:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1139459;
}
else 
{
goto label_1139421;
}
}
else 
{
label_1139421:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1139459;
}
else 
{
goto label_1139428;
}
}
else 
{
label_1139428:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1139437;
}
else 
{
label_1139437:; 
goto label_1139459;
}
}
else 
{
label_1139459:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1139466;
}
}
else 
{
label_1139466:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1139475;
}
else 
{
label_1139475:; 
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
goto label_1140996;
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
goto label_1140483;
}
else 
{
goto label_1140424;
}
}
else 
{
label_1140424:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1140483;
}
else 
{
goto label_1140431;
}
}
else 
{
label_1140431:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1140483;
}
else 
{
goto label_1140438;
}
}
else 
{
label_1140438:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1140483;
}
else 
{
goto label_1140445;
}
}
else 
{
label_1140445:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1140483;
}
else 
{
goto label_1140452;
}
}
else 
{
label_1140452:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1140461;
}
else 
{
label_1140461:; 
goto label_1140483;
}
}
else 
{
label_1140483:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1140505;
}
else 
{
goto label_1140490;
}
}
else 
{
label_1140490:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1140499;
}
else 
{
label_1140499:; 
goto label_1140505;
}
}
else 
{
label_1140505:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_18 = req_a;
int i = __tmp_18;
int x;
if (i == 0)
{
x = s_memory0;
 __return_1140555 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_1140556 = x;
}
rsp_d = __return_1140555;
goto label_1140558;
rsp_d = __return_1140556;
label_1140558:; 
rsp_status = 1;
label_1140564:; 
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
goto label_1140645;
}
else 
{
goto label_1140586;
}
}
else 
{
label_1140586:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1140645;
}
else 
{
goto label_1140593;
}
}
else 
{
label_1140593:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1140645;
}
else 
{
goto label_1140600;
}
}
else 
{
label_1140600:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1140645;
}
else 
{
goto label_1140607;
}
}
else 
{
label_1140607:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1140645;
}
else 
{
goto label_1140614;
}
}
else 
{
label_1140614:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1140623;
}
else 
{
label_1140623:; 
goto label_1140645;
}
}
else 
{
label_1140645:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1140667;
}
else 
{
goto label_1140652;
}
}
else 
{
label_1140652:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1140661;
}
else 
{
label_1140661:; 
goto label_1140667;
}
}
else 
{
label_1140667:; 
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
goto label_1140750;
}
else 
{
goto label_1140691;
}
}
else 
{
label_1140691:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1140750;
}
else 
{
goto label_1140698;
}
}
else 
{
label_1140698:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1140750;
}
else 
{
goto label_1140705;
}
}
else 
{
label_1140705:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1140750;
}
else 
{
goto label_1140712;
}
}
else 
{
label_1140712:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1140750;
}
else 
{
goto label_1140719;
}
}
else 
{
label_1140719:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1140728;
}
else 
{
label_1140728:; 
goto label_1140750;
}
}
else 
{
label_1140750:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1140757;
}
}
else 
{
label_1140757:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1140766;
}
else 
{
label_1140766:; 
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
label_1142263:; 
label_1142267:; 
if (((int)m_run_st) == 0)
{
goto label_1142280;
}
else 
{
label_1142280:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_1142636;
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
goto label_1142468;
}
else 
{
goto label_1142409;
}
}
else 
{
label_1142409:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1142468;
}
else 
{
goto label_1142416;
}
}
else 
{
label_1142416:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1142468;
}
else 
{
goto label_1142423;
}
}
else 
{
label_1142423:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1142468;
}
else 
{
goto label_1142430;
}
}
else 
{
label_1142430:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1142468;
}
else 
{
goto label_1142437;
}
}
else 
{
label_1142437:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1142446;
}
else 
{
label_1142446:; 
goto label_1142468;
}
}
else 
{
label_1142468:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1142490;
}
else 
{
goto label_1142475;
}
}
else 
{
label_1142475:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1142484;
}
else 
{
label_1142484:; 
goto label_1142490;
}
}
else 
{
label_1142490:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1142563;
}
else 
{
goto label_1142504;
}
}
else 
{
label_1142504:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1142563;
}
else 
{
goto label_1142511;
}
}
else 
{
label_1142511:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1142563;
}
else 
{
goto label_1142518;
}
}
else 
{
label_1142518:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1142563;
}
else 
{
goto label_1142525;
}
}
else 
{
label_1142525:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1142563;
}
else 
{
goto label_1142532;
}
}
else 
{
label_1142532:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1142541;
}
else 
{
label_1142541:; 
goto label_1142563;
}
}
else 
{
label_1142563:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1142585;
}
else 
{
goto label_1142570;
}
}
else 
{
label_1142570:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1142579;
}
else 
{
label_1142579:; 
goto label_1142585;
}
}
else 
{
label_1142585:; 
c_m_ev = 2;
if ((req_a___0 + 50) == rsp_d___0)
{
a = a + 1;
goto label_1142602;
}
else 
{
{
__VERIFIER_error();
}
a = a + 1;
label_1142602:; 
}
label_1142631:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1142988;
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
goto label_1142771;
}
else 
{
goto label_1142712;
}
}
else 
{
label_1142712:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1142771;
}
else 
{
goto label_1142719;
}
}
else 
{
label_1142719:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1142771;
}
else 
{
goto label_1142726;
}
}
else 
{
label_1142726:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1142771;
}
else 
{
goto label_1142733;
}
}
else 
{
label_1142733:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1142771;
}
else 
{
goto label_1142740;
}
}
else 
{
label_1142740:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1142749;
}
else 
{
label_1142749:; 
goto label_1142771;
}
}
else 
{
label_1142771:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1142778;
}
}
else 
{
label_1142778:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1142787;
}
else 
{
label_1142787:; 
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
label_1142817:; 
label_1142993:; 
if (((int)m_run_st) == 0)
{
goto label_1143007;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1143007:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_1143240;
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
goto label_1143195;
}
else 
{
goto label_1143136;
}
}
else 
{
label_1143136:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1143195;
}
else 
{
goto label_1143143;
}
}
else 
{
label_1143143:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1143195;
}
else 
{
goto label_1143150;
}
}
else 
{
label_1143150:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1143195;
}
else 
{
goto label_1143157;
}
}
else 
{
label_1143157:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1143195;
}
else 
{
goto label_1143164;
}
}
else 
{
label_1143164:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1143173;
}
else 
{
label_1143173:; 
goto label_1143195;
}
}
else 
{
label_1143195:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1143202;
}
}
else 
{
label_1143202:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1143211;
}
else 
{
label_1143211:; 
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
goto label_1142631;
}
}
}
}
else 
{
label_1143240:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1143416;
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
goto label_1143367;
}
else 
{
goto label_1143308;
}
}
else 
{
label_1143308:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1143367;
}
else 
{
goto label_1143315;
}
}
else 
{
label_1143315:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1143367;
}
else 
{
goto label_1143322;
}
}
else 
{
label_1143322:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1143367;
}
else 
{
goto label_1143329;
}
}
else 
{
label_1143329:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1143367;
}
else 
{
goto label_1143336;
}
}
else 
{
label_1143336:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1143345;
}
else 
{
label_1143345:; 
goto label_1143367;
}
}
else 
{
label_1143367:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1143374;
}
}
else 
{
label_1143374:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1143383;
}
else 
{
label_1143383:; 
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
goto label_1142817;
}
}
}
}
else 
{
label_1143416:; 
goto label_1142993;
}
}
}
else 
{
}
label_1145784:; 
kernel_st = 2;
kernel_st = 3;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1147013;
}
else 
{
goto label_1146954;
}
}
else 
{
label_1146954:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1147013;
}
else 
{
goto label_1146961;
}
}
else 
{
label_1146961:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1147013;
}
else 
{
goto label_1146968;
}
}
else 
{
label_1146968:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1147013;
}
else 
{
goto label_1146975;
}
}
else 
{
label_1146975:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1147013;
}
else 
{
goto label_1146982;
}
}
else 
{
label_1146982:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1146991;
}
else 
{
label_1146991:; 
goto label_1147013;
}
}
else 
{
label_1147013:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1147035;
}
else 
{
goto label_1147020;
}
}
else 
{
label_1147020:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1147029;
}
else 
{
label_1147029:; 
goto label_1147035;
}
}
else 
{
label_1147035:; 
if (((int)m_run_st) == 0)
{
goto label_1147047;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1147047:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
label_1147059:; 
if (((int)m_run_st) == 0)
{
goto label_1147073;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1147073:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_1147307;
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
goto label_1147261;
}
else 
{
goto label_1147202;
}
}
else 
{
label_1147202:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1147261;
}
else 
{
goto label_1147209;
}
}
else 
{
label_1147209:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1147261;
}
else 
{
goto label_1147216;
}
}
else 
{
label_1147216:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1147261;
}
else 
{
goto label_1147223;
}
}
else 
{
label_1147223:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1147261;
}
else 
{
goto label_1147230;
}
}
else 
{
label_1147230:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1147239;
}
else 
{
label_1147239:; 
goto label_1147261;
}
}
else 
{
label_1147261:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1147268;
}
}
else 
{
label_1147268:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1147277;
}
else 
{
label_1147277:; 
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
goto label_1147660;
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
goto label_1147442;
}
else 
{
goto label_1147383;
}
}
else 
{
label_1147383:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1147442;
}
else 
{
goto label_1147390;
}
}
else 
{
label_1147390:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1147442;
}
else 
{
goto label_1147397;
}
}
else 
{
label_1147397:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1147442;
}
else 
{
goto label_1147404;
}
}
else 
{
label_1147404:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1147442;
}
else 
{
goto label_1147411;
}
}
else 
{
label_1147411:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1147420;
}
else 
{
label_1147420:; 
goto label_1147442;
}
}
else 
{
label_1147442:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1147449;
}
}
else 
{
label_1147449:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1147458;
}
else 
{
label_1147458:; 
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
goto label_1147059;
}
}
}
}
else 
{
label_1147660:; 
goto label_1147059;
}
}
}
}
}
else 
{
label_1147307:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1147662;
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
goto label_1147608;
}
else 
{
goto label_1147549;
}
}
else 
{
label_1147549:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1147608;
}
else 
{
goto label_1147556;
}
}
else 
{
label_1147556:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1147608;
}
else 
{
goto label_1147563;
}
}
else 
{
label_1147563:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1147608;
}
else 
{
goto label_1147570;
}
}
else 
{
label_1147570:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1147608;
}
else 
{
goto label_1147577;
}
}
else 
{
label_1147577:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1147586;
}
else 
{
label_1147586:; 
goto label_1147608;
}
}
else 
{
label_1147608:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1147615;
}
}
else 
{
label_1147615:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1147624;
}
else 
{
label_1147624:; 
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
goto label_1147059;
}
}
}
}
else 
{
label_1147662:; 
goto label_1147059;
}
}
}
else 
{
}
goto label_1145784;
}
}
}
else 
{
}
goto label_907416;
}
}
}
}
}
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
label_1142988:; 
goto label_1142993;
}
}
}
}
}
}
}
}
}
}
}
}
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
label_1142636:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1142990;
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
goto label_1142937;
}
else 
{
goto label_1142878;
}
}
else 
{
label_1142878:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1142937;
}
else 
{
goto label_1142885;
}
}
else 
{
label_1142885:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1142937;
}
else 
{
goto label_1142892;
}
}
else 
{
label_1142892:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1142937;
}
else 
{
goto label_1142899;
}
}
else 
{
label_1142899:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1142937;
}
else 
{
goto label_1142906;
}
}
else 
{
label_1142906:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1142915;
}
else 
{
label_1142915:; 
goto label_1142937;
}
}
else 
{
label_1142937:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1142944;
}
}
else 
{
label_1142944:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1142953;
}
else 
{
label_1142953:; 
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
goto label_1142263;
}
}
}
}
else 
{
label_1142990:; 
goto label_1142267;
}
}
}
}
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
int __tmp_19 = req_a;
int __tmp_20 = req_d;
int i = __tmp_19;
int v = __tmp_20;
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
goto label_1140533;
label_1140533:; 
rsp_status = 1;
goto label_1140564;
}
}
else 
{
rsp_status = 0;
goto label_1140564;
}
}
}
}
}
}
}
}
}
}
}
label_1140819:; 
label_1143424:; 
if (((int)m_run_st) == 0)
{
goto label_1143438;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1143438:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_1143449;
}
else 
{
label_1143449:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1143625;
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
goto label_1143576;
}
else 
{
goto label_1143517;
}
}
else 
{
label_1143517:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1143576;
}
else 
{
goto label_1143524;
}
}
else 
{
label_1143524:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1143576;
}
else 
{
goto label_1143531;
}
}
else 
{
label_1143531:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1143576;
}
else 
{
goto label_1143538;
}
}
else 
{
label_1143538:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1143576;
}
else 
{
goto label_1143545;
}
}
else 
{
label_1143545:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1143554;
}
else 
{
label_1143554:; 
goto label_1143576;
}
}
else 
{
label_1143576:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1143583;
}
}
else 
{
label_1143583:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1143592;
}
else 
{
label_1143592:; 
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
goto label_1140819;
}
}
}
}
else 
{
label_1143625:; 
goto label_1143424;
}
}
}
else 
{
}
label_1151252:; 
kernel_st = 2;
kernel_st = 3;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1151529;
}
else 
{
goto label_1151470;
}
}
else 
{
label_1151470:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1151529;
}
else 
{
goto label_1151477;
}
}
else 
{
label_1151477:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1151529;
}
else 
{
goto label_1151484;
}
}
else 
{
label_1151484:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1151529;
}
else 
{
goto label_1151491;
}
}
else 
{
label_1151491:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1151529;
}
else 
{
goto label_1151498;
}
}
else 
{
label_1151498:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1151507;
}
else 
{
label_1151507:; 
goto label_1151529;
}
}
else 
{
label_1151529:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1151551;
}
else 
{
goto label_1151536;
}
}
else 
{
label_1151536:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1151545;
}
else 
{
label_1151545:; 
goto label_1151551;
}
}
else 
{
label_1151551:; 
if (((int)m_run_st) == 0)
{
goto label_1151565;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1151565:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
label_1151577:; 
if (((int)m_run_st) == 0)
{
goto label_1151591;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1151591:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_1151922;
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
goto label_1151779;
}
else 
{
goto label_1151720;
}
}
else 
{
label_1151720:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1151779;
}
else 
{
goto label_1151727;
}
}
else 
{
label_1151727:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1151779;
}
else 
{
goto label_1151734;
}
}
else 
{
label_1151734:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1151779;
}
else 
{
goto label_1151741;
}
}
else 
{
label_1151741:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1151779;
}
else 
{
goto label_1151748;
}
}
else 
{
label_1151748:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1151757;
}
else 
{
label_1151757:; 
goto label_1151779;
}
}
else 
{
label_1151779:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1151801;
}
else 
{
goto label_1151786;
}
}
else 
{
label_1151786:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1151795;
}
else 
{
label_1151795:; 
goto label_1151801;
}
}
else 
{
label_1151801:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1151874;
}
else 
{
goto label_1151815;
}
}
else 
{
label_1151815:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1151874;
}
else 
{
goto label_1151822;
}
}
else 
{
label_1151822:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1151874;
}
else 
{
goto label_1151829;
}
}
else 
{
label_1151829:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1151874;
}
else 
{
goto label_1151836;
}
}
else 
{
label_1151836:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1151874;
}
else 
{
goto label_1151843;
}
}
else 
{
label_1151843:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1151852;
}
else 
{
label_1151852:; 
goto label_1151874;
}
}
else 
{
label_1151874:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1151881;
}
}
else 
{
label_1151881:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1151890;
}
else 
{
label_1151890:; 
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
goto label_1152275;
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
goto label_1152057;
}
else 
{
goto label_1151998;
}
}
else 
{
label_1151998:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1152057;
}
else 
{
goto label_1152005;
}
}
else 
{
label_1152005:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1152057;
}
else 
{
goto label_1152012;
}
}
else 
{
label_1152012:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1152057;
}
else 
{
goto label_1152019;
}
}
else 
{
label_1152019:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1152057;
}
else 
{
goto label_1152026;
}
}
else 
{
label_1152026:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1152035;
}
else 
{
label_1152035:; 
goto label_1152057;
}
}
else 
{
label_1152057:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1152064;
}
}
else 
{
label_1152064:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1152073;
}
else 
{
label_1152073:; 
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
goto label_1151577;
}
}
}
}
else 
{
label_1152275:; 
goto label_1151577;
}
}
}
}
}
else 
{
label_1151922:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1152277;
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
goto label_1152223;
}
else 
{
goto label_1152164;
}
}
else 
{
label_1152164:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1152223;
}
else 
{
goto label_1152171;
}
}
else 
{
label_1152171:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1152223;
}
else 
{
goto label_1152178;
}
}
else 
{
label_1152178:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1152223;
}
else 
{
goto label_1152185;
}
}
else 
{
label_1152185:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1152223;
}
else 
{
goto label_1152192;
}
}
else 
{
label_1152192:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1152201;
}
else 
{
label_1152201:; 
goto label_1152223;
}
}
else 
{
label_1152223:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1152230;
}
}
else 
{
label_1152230:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1152239;
}
else 
{
label_1152239:; 
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
goto label_1151577;
}
}
}
}
else 
{
label_1152277:; 
goto label_1151577;
}
}
}
else 
{
}
goto label_1151252;
}
}
}
else 
{
}
label_1151560:; 
__retres1 = 0;
 __return_1152295 = __retres1;
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
label_1140996:; 
label_1143631:; 
if (((int)m_run_st) == 0)
{
goto label_1143644;
}
else 
{
label_1143644:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_1143655;
}
else 
{
label_1143655:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1144123;
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
goto label_1143782;
}
else 
{
goto label_1143723;
}
}
else 
{
label_1143723:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1143782;
}
else 
{
goto label_1143730;
}
}
else 
{
label_1143730:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1143782;
}
else 
{
goto label_1143737;
}
}
else 
{
label_1143737:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1143782;
}
else 
{
goto label_1143744;
}
}
else 
{
label_1143744:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1143782;
}
else 
{
goto label_1143751;
}
}
else 
{
label_1143751:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1143760;
}
else 
{
label_1143760:; 
goto label_1143782;
}
}
else 
{
label_1143782:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1143804;
}
else 
{
goto label_1143789;
}
}
else 
{
label_1143789:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1143798;
}
else 
{
label_1143798:; 
goto label_1143804;
}
}
else 
{
label_1143804:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_21 = req_a;
int i = __tmp_21;
int x;
if (i == 0)
{
x = s_memory0;
 __return_1143854 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_1143855 = x;
}
rsp_d = __return_1143854;
goto label_1143857;
rsp_d = __return_1143855;
label_1143857:; 
rsp_status = 1;
label_1143863:; 
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
goto label_1143944;
}
else 
{
goto label_1143885;
}
}
else 
{
label_1143885:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1143944;
}
else 
{
goto label_1143892;
}
}
else 
{
label_1143892:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1143944;
}
else 
{
goto label_1143899;
}
}
else 
{
label_1143899:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1143944;
}
else 
{
goto label_1143906;
}
}
else 
{
label_1143906:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1143944;
}
else 
{
goto label_1143913;
}
}
else 
{
label_1143913:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1143922;
}
else 
{
label_1143922:; 
goto label_1143944;
}
}
else 
{
label_1143944:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1143966;
}
else 
{
goto label_1143951;
}
}
else 
{
label_1143951:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1143960;
}
else 
{
label_1143960:; 
goto label_1143966;
}
}
else 
{
label_1143966:; 
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
goto label_1144049;
}
else 
{
goto label_1143990;
}
}
else 
{
label_1143990:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1144049;
}
else 
{
goto label_1143997;
}
}
else 
{
label_1143997:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1144049;
}
else 
{
goto label_1144004;
}
}
else 
{
label_1144004:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1144049;
}
else 
{
goto label_1144011;
}
}
else 
{
label_1144011:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1144049;
}
else 
{
goto label_1144018;
}
}
else 
{
label_1144018:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1144027;
}
else 
{
label_1144027:; 
goto label_1144049;
}
}
else 
{
label_1144049:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1144056;
}
}
else 
{
label_1144056:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1144065;
}
else 
{
label_1144065:; 
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
goto label_1142263;
}
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
int __tmp_22 = req_a;
int __tmp_23 = req_d;
int i = __tmp_22;
int v = __tmp_23;
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
goto label_1143832;
label_1143832:; 
rsp_status = 1;
goto label_1143863;
}
}
else 
{
rsp_status = 0;
goto label_1143863;
}
}
}
}
}
}
}
}
}
}
}
goto label_1140819;
}
}
}
}
else 
{
label_1144123:; 
goto label_1143631;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_1139909:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1140998;
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
goto label_1140941;
}
else 
{
goto label_1140882;
}
}
else 
{
label_1140882:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1140941;
}
else 
{
goto label_1140889;
}
}
else 
{
label_1140889:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1140941;
}
else 
{
goto label_1140896;
}
}
else 
{
label_1140896:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1140941;
}
else 
{
goto label_1140903;
}
}
else 
{
label_1140903:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1140941;
}
else 
{
goto label_1140910;
}
}
else 
{
label_1140910:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1140919;
}
else 
{
label_1140919:; 
goto label_1140941;
}
}
else 
{
label_1140941:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1140948;
}
}
else 
{
label_1140948:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1140957;
}
else 
{
label_1140957:; 
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
goto label_1138366;
}
}
}
}
else 
{
label_1140998:; 
goto label_1138833;
}
}
}
}
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
goto label_1138079;
label_1138079:; 
rsp_status = 1;
goto label_1138110;
}
}
else 
{
rsp_status = 0;
goto label_1138110;
}
}
}
}
}
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
label_1138828:; 
goto label_1137553;
}
}
}
}
}
else 
{
label_1137894:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1138830;
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
goto label_1138404;
}
else 
{
label_1138404:; 
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
goto label_1138486;
}
else 
{
goto label_1138427;
}
}
else 
{
label_1138427:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1138486;
}
else 
{
goto label_1138434;
}
}
else 
{
label_1138434:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1138486;
}
else 
{
goto label_1138441;
}
}
else 
{
label_1138441:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1138486;
}
else 
{
goto label_1138448;
}
}
else 
{
label_1138448:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1138486;
}
else 
{
goto label_1138455;
}
}
else 
{
label_1138455:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1138464;
}
else 
{
label_1138464:; 
goto label_1138486;
}
}
else 
{
label_1138486:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1138508;
}
else 
{
goto label_1138493;
}
}
else 
{
label_1138493:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1138502;
}
else 
{
label_1138502:; 
goto label_1138508;
}
}
else 
{
label_1138508:; 
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
 __return_1138558 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_1138559 = x;
}
rsp_d = __return_1138558;
goto label_1138561;
rsp_d = __return_1138559;
label_1138561:; 
rsp_status = 1;
label_1138567:; 
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
goto label_1138648;
}
else 
{
goto label_1138589;
}
}
else 
{
label_1138589:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1138648;
}
else 
{
goto label_1138596;
}
}
else 
{
label_1138596:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1138648;
}
else 
{
goto label_1138603;
}
}
else 
{
label_1138603:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1138648;
}
else 
{
goto label_1138610;
}
}
else 
{
label_1138610:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1138648;
}
else 
{
goto label_1138617;
}
}
else 
{
label_1138617:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1138626;
}
else 
{
label_1138626:; 
goto label_1138648;
}
}
else 
{
label_1138648:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1138670;
}
else 
{
goto label_1138655;
}
}
else 
{
label_1138655:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1138664;
}
else 
{
label_1138664:; 
goto label_1138670;
}
}
else 
{
label_1138670:; 
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
goto label_1138753;
}
else 
{
goto label_1138694;
}
}
else 
{
label_1138694:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1138753;
}
else 
{
goto label_1138701;
}
}
else 
{
label_1138701:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1138753;
}
else 
{
goto label_1138708;
}
}
else 
{
label_1138708:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1138753;
}
else 
{
goto label_1138715;
}
}
else 
{
label_1138715:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1138753;
}
else 
{
goto label_1138722;
}
}
else 
{
label_1138722:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1138731;
}
else 
{
label_1138731:; 
goto label_1138753;
}
}
else 
{
label_1138753:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1138760;
}
}
else 
{
label_1138760:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1138769;
}
else 
{
label_1138769:; 
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
goto label_1138366;
}
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
goto label_1138536;
label_1138536:; 
rsp_status = 1;
goto label_1138567;
}
}
else 
{
rsp_status = 0;
goto label_1138567;
}
}
}
}
}
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
label_1138830:; 
goto label_1137553;
}
}
}
else 
{
}
goto label_1136752;
}
}
}
else 
{
}
goto label_1151560;
}
}
}
}
}
}
}
}
}
}
}
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
goto label_913757;
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
goto label_913625;
}
else 
{
label_913625:; 
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
goto label_913707;
}
else 
{
goto label_913648;
}
}
else 
{
label_913648:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_913707;
}
else 
{
goto label_913655;
}
}
else 
{
label_913655:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_913707;
}
else 
{
goto label_913662;
}
}
else 
{
label_913662:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_913707;
}
else 
{
goto label_913669;
}
}
else 
{
label_913669:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_913707;
}
else 
{
goto label_913676;
}
}
else 
{
label_913676:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_913685;
}
else 
{
label_913685:; 
goto label_913707;
}
}
else 
{
label_913707:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_913714;
}
}
else 
{
label_913714:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_913723;
}
else 
{
label_913723:; 
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
goto label_1116509;
}
}
}
}
else 
{
label_913757:; 
label_913762:; 
if (((int)m_run_st) == 0)
{
goto label_913776;
}
else 
{
if (((int)s_run_st) == 0)
{
label_913776:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_913787;
}
else 
{
label_913787:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_913963;
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
goto label_913832;
}
else 
{
label_913832:; 
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
goto label_913914;
}
else 
{
goto label_913855;
}
}
else 
{
label_913855:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_913914;
}
else 
{
goto label_913862;
}
}
else 
{
label_913862:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_913914;
}
else 
{
goto label_913869;
}
}
else 
{
label_913869:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_913914;
}
else 
{
goto label_913876;
}
}
else 
{
label_913876:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_913914;
}
else 
{
goto label_913883;
}
}
else 
{
label_913883:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_913892;
}
else 
{
label_913892:; 
goto label_913914;
}
}
else 
{
label_913914:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_913921;
}
}
else 
{
label_913921:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_913930;
}
else 
{
label_913930:; 
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
label_1116509:; 
goto label_1117818;
}
}
}
}
else 
{
label_913963:; 
goto label_913762;
}
}
}
else 
{
}
label_913773:; 
kernel_st = 2;
kernel_st = 3;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_914033;
}
else 
{
goto label_913974;
}
}
else 
{
label_913974:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_914033;
}
else 
{
goto label_913981;
}
}
else 
{
label_913981:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_914033;
}
else 
{
goto label_913988;
}
}
else 
{
label_913988:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_914033;
}
else 
{
goto label_913995;
}
}
else 
{
label_913995:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_914033;
}
else 
{
goto label_914002;
}
}
else 
{
label_914002:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_914011;
}
else 
{
label_914011:; 
goto label_914033;
}
}
else 
{
label_914033:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_914055;
}
else 
{
goto label_914040;
}
}
else 
{
label_914040:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_914049;
}
else 
{
label_914049:; 
goto label_914055;
}
}
else 
{
label_914055:; 
if (((int)m_run_st) == 0)
{
goto label_914067;
}
else 
{
if (((int)s_run_st) == 0)
{
label_914067:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
label_914079:; 
if (((int)m_run_st) == 0)
{
goto label_914093;
}
else 
{
if (((int)s_run_st) == 0)
{
label_914093:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_914231;
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
goto label_914584;
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
goto label_914284;
}
else 
{
label_914284:; 
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
goto label_914366;
}
else 
{
goto label_914307;
}
}
else 
{
label_914307:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_914366;
}
else 
{
goto label_914314;
}
}
else 
{
label_914314:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_914366;
}
else 
{
goto label_914321;
}
}
else 
{
label_914321:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_914366;
}
else 
{
goto label_914328;
}
}
else 
{
label_914328:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_914366;
}
else 
{
goto label_914335;
}
}
else 
{
label_914335:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_914344;
}
else 
{
label_914344:; 
goto label_914366;
}
}
else 
{
label_914366:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_914373;
}
}
else 
{
label_914373:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_914382;
}
else 
{
label_914382:; 
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
label_914412:; 
goto label_914590;
}
}
}
}
else 
{
label_914584:; 
goto label_914079;
}
}
}
}
else 
{
label_914231:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_914586;
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
goto label_914450;
}
else 
{
label_914450:; 
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
goto label_914532;
}
else 
{
goto label_914473;
}
}
else 
{
label_914473:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_914532;
}
else 
{
goto label_914480;
}
}
else 
{
label_914480:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_914532;
}
else 
{
goto label_914487;
}
}
else 
{
label_914487:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_914532;
}
else 
{
goto label_914494;
}
}
else 
{
label_914494:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_914532;
}
else 
{
goto label_914501;
}
}
else 
{
label_914501:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_914510;
}
else 
{
label_914510:; 
goto label_914532;
}
}
else 
{
label_914532:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_914539;
}
}
else 
{
label_914539:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_914548;
}
else 
{
label_914548:; 
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
label_914578:; 
label_914590:; 
if (((int)m_run_st) == 0)
{
goto label_914604;
}
else 
{
if (((int)s_run_st) == 0)
{
label_914604:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_914742;
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
goto label_915093;
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
goto label_914877;
}
else 
{
goto label_914818;
}
}
else 
{
label_914818:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_914877;
}
else 
{
goto label_914825;
}
}
else 
{
label_914825:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_914877;
}
else 
{
goto label_914832;
}
}
else 
{
label_914832:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_914877;
}
else 
{
goto label_914839;
}
}
else 
{
label_914839:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_914877;
}
else 
{
goto label_914846;
}
}
else 
{
label_914846:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_914855;
}
else 
{
label_914855:; 
goto label_914877;
}
}
else 
{
label_914877:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_914884;
}
}
else 
{
label_914884:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_914893;
}
else 
{
label_914893:; 
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
goto label_914412;
}
}
}
}
else 
{
label_915093:; 
goto label_914590;
}
}
}
}
else 
{
label_914742:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_915095;
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
goto label_915043;
}
else 
{
goto label_914984;
}
}
else 
{
label_914984:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_915043;
}
else 
{
goto label_914991;
}
}
else 
{
label_914991:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_915043;
}
else 
{
goto label_914998;
}
}
else 
{
label_914998:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_915043;
}
else 
{
goto label_915005;
}
}
else 
{
label_915005:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_915043;
}
else 
{
goto label_915012;
}
}
else 
{
label_915012:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_915021;
}
else 
{
label_915021:; 
goto label_915043;
}
}
else 
{
label_915043:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_915050;
}
}
else 
{
label_915050:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_915059;
}
else 
{
label_915059:; 
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
goto label_914578;
}
}
}
}
else 
{
label_915095:; 
goto label_914590;
}
}
}
else 
{
}
label_1116517:; 
kernel_st = 2;
kernel_st = 3;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1118721;
}
else 
{
goto label_1118544;
}
}
else 
{
label_1118544:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1118721;
}
else 
{
goto label_1118565;
}
}
else 
{
label_1118565:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1118721;
}
else 
{
goto label_1118586;
}
}
else 
{
label_1118586:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1118721;
}
else 
{
goto label_1118607;
}
}
else 
{
label_1118607:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1118721;
}
else 
{
goto label_1118628;
}
}
else 
{
label_1118628:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1118655;
}
else 
{
label_1118655:; 
goto label_1118721;
}
}
else 
{
label_1118721:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1118787;
}
else 
{
goto label_1118742;
}
}
else 
{
label_1118742:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1118769;
}
else 
{
label_1118769:; 
goto label_1118787;
}
}
else 
{
label_1118787:; 
if (((int)m_run_st) == 0)
{
goto label_1118823;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1118823:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
label_1125703:; 
if (((int)m_run_st) == 0)
{
goto label_1125717;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1125717:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_1125855;
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
goto label_1126208;
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
goto label_1125990;
}
else 
{
goto label_1125931;
}
}
else 
{
label_1125931:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1125990;
}
else 
{
goto label_1125938;
}
}
else 
{
label_1125938:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1125990;
}
else 
{
goto label_1125945;
}
}
else 
{
label_1125945:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1125990;
}
else 
{
goto label_1125952;
}
}
else 
{
label_1125952:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1125990;
}
else 
{
goto label_1125959;
}
}
else 
{
label_1125959:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1125968;
}
else 
{
label_1125968:; 
goto label_1125990;
}
}
else 
{
label_1125990:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1125997;
}
}
else 
{
label_1125997:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1126006;
}
else 
{
label_1126006:; 
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
goto label_1125703;
}
}
}
}
else 
{
label_1126208:; 
goto label_1125703;
}
}
}
}
else 
{
label_1125855:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1126210;
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
goto label_1126156;
}
else 
{
goto label_1126097;
}
}
else 
{
label_1126097:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1126156;
}
else 
{
goto label_1126104;
}
}
else 
{
label_1126104:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1126156;
}
else 
{
goto label_1126111;
}
}
else 
{
label_1126111:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1126156;
}
else 
{
goto label_1126118;
}
}
else 
{
label_1126118:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1126156;
}
else 
{
goto label_1126125;
}
}
else 
{
label_1126125:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1126134;
}
else 
{
label_1126134:; 
goto label_1126156;
}
}
else 
{
label_1126156:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1126163;
}
}
else 
{
label_1126163:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1126172;
}
else 
{
label_1126172:; 
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
goto label_1125703;
}
}
}
}
else 
{
label_1126210:; 
goto label_1125703;
}
}
}
else 
{
}
goto label_1116517;
}
}
}
else 
{
}
goto label_907416;
}
}
}
}
}
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
label_914586:; 
goto label_914079;
}
}
}
else 
{
}
goto label_913773;
}
}
}
else 
{
}
goto label_907416;
}
}
}
}
}
}
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
label_898187:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_898985;
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
goto label_898845;
}
else 
{
label_898845:; 
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
goto label_898927;
}
else 
{
goto label_898868;
}
}
else 
{
label_898868:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_898927;
}
else 
{
goto label_898875;
}
}
else 
{
label_898875:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_898927;
}
else 
{
goto label_898882;
}
}
else 
{
label_898882:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_898927;
}
else 
{
goto label_898889;
}
}
else 
{
label_898889:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_898927;
}
else 
{
goto label_898896;
}
}
else 
{
label_898896:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_898905;
}
else 
{
label_898905:; 
goto label_898927;
}
}
else 
{
label_898927:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_898934;
}
}
else 
{
label_898934:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_898943;
}
else 
{
label_898943:; 
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
label_1116513:; 
label_1116966:; 
if (((int)m_run_st) == 0)
{
goto label_1116980;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1116980:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_1117461;
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
goto label_1117088;
}
else 
{
label_1117088:; 
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
goto label_1117191;
}
else 
{
goto label_1117132;
}
}
else 
{
label_1117132:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1117191;
}
else 
{
goto label_1117139;
}
}
else 
{
label_1117139:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1117191;
}
else 
{
goto label_1117146;
}
}
else 
{
label_1117146:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1117191;
}
else 
{
goto label_1117153;
}
}
else 
{
label_1117153:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1117191;
}
else 
{
goto label_1117160;
}
}
else 
{
label_1117160:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1117169;
}
else 
{
label_1117169:; 
goto label_1117191;
}
}
else 
{
label_1117191:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1117213;
}
else 
{
goto label_1117198;
}
}
else 
{
label_1117198:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1117207;
}
else 
{
label_1117207:; 
goto label_1117213;
}
}
else 
{
label_1117213:; 
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
goto label_1117293;
}
else 
{
goto label_1117234;
}
}
else 
{
label_1117234:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1117293;
}
else 
{
goto label_1117241;
}
}
else 
{
label_1117241:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1117293;
}
else 
{
goto label_1117248;
}
}
else 
{
label_1117248:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1117293;
}
else 
{
goto label_1117255;
}
}
else 
{
label_1117255:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1117293;
}
else 
{
goto label_1117262;
}
}
else 
{
label_1117262:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1117271;
}
else 
{
label_1117271:; 
goto label_1117293;
}
}
else 
{
label_1117293:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1117315;
}
else 
{
goto label_1117300;
}
}
else 
{
label_1117300:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1117309;
}
else 
{
label_1117309:; 
goto label_1117315;
}
}
else 
{
label_1117315:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1117388;
}
else 
{
goto label_1117329;
}
}
else 
{
label_1117329:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1117388;
}
else 
{
goto label_1117336;
}
}
else 
{
label_1117336:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1117388;
}
else 
{
goto label_1117343;
}
}
else 
{
label_1117343:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1117388;
}
else 
{
goto label_1117350;
}
}
else 
{
label_1117350:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1117388;
}
else 
{
goto label_1117357;
}
}
else 
{
label_1117357:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1117366;
}
else 
{
label_1117366:; 
goto label_1117388;
}
}
else 
{
label_1117388:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1117395;
}
}
else 
{
label_1117395:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1117404;
}
else 
{
label_1117404:; 
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
label_1130499:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1131392;
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
goto label_1131074;
}
else 
{
goto label_1131015;
}
}
else 
{
label_1131015:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1131074;
}
else 
{
goto label_1131022;
}
}
else 
{
label_1131022:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1131074;
}
else 
{
goto label_1131029;
}
}
else 
{
label_1131029:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1131074;
}
else 
{
goto label_1131036;
}
}
else 
{
label_1131036:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1131074;
}
else 
{
goto label_1131043;
}
}
else 
{
label_1131043:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1131052;
}
else 
{
label_1131052:; 
goto label_1131074;
}
}
else 
{
label_1131074:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1131096;
}
else 
{
goto label_1131081;
}
}
else 
{
label_1131081:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1131090;
}
else 
{
label_1131090:; 
goto label_1131096;
}
}
else 
{
label_1131096:; 
c_read_req_ev = 2;
rsp_type = req_type;
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
goto label_1131124;
label_1131124:; 
rsp_status = 1;
label_1131131:; 
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
goto label_1131210;
}
else 
{
goto label_1131151;
}
}
else 
{
label_1131151:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1131210;
}
else 
{
goto label_1131158;
}
}
else 
{
label_1131158:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1131210;
}
else 
{
goto label_1131165;
}
}
else 
{
label_1131165:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1131210;
}
else 
{
goto label_1131172;
}
}
else 
{
label_1131172:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1131210;
}
else 
{
goto label_1131179;
}
}
else 
{
label_1131179:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1131188;
}
else 
{
label_1131188:; 
goto label_1131210;
}
}
else 
{
label_1131210:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1131232;
}
else 
{
goto label_1131217;
}
}
else 
{
label_1131217:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1131226;
}
else 
{
label_1131226:; 
goto label_1131232;
}
}
else 
{
label_1131232:; 
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
goto label_1131315;
}
else 
{
goto label_1131256;
}
}
else 
{
label_1131256:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1131315;
}
else 
{
goto label_1131263;
}
}
else 
{
label_1131263:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1131315;
}
else 
{
goto label_1131270;
}
}
else 
{
label_1131270:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1131315;
}
else 
{
goto label_1131277;
}
}
else 
{
label_1131277:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1131315;
}
else 
{
goto label_1131284;
}
}
else 
{
label_1131284:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1131293;
}
else 
{
label_1131293:; 
goto label_1131315;
}
}
else 
{
label_1131315:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1131322;
}
}
else 
{
label_1131322:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1131331;
}
else 
{
label_1131331:; 
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
goto label_1130954;
}
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
rsp_status = 0;
goto label_1131131;
}
}
}
}
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
label_1131392:; 
label_1136246:; 
if (((int)m_run_st) == 0)
{
goto label_1136259;
}
else 
{
label_1136259:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_1136270;
}
else 
{
label_1136270:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1136737;
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
goto label_1136397;
}
else 
{
goto label_1136338;
}
}
else 
{
label_1136338:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1136397;
}
else 
{
goto label_1136345;
}
}
else 
{
label_1136345:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1136397;
}
else 
{
goto label_1136352;
}
}
else 
{
label_1136352:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1136397;
}
else 
{
goto label_1136359;
}
}
else 
{
label_1136359:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1136397;
}
else 
{
goto label_1136366;
}
}
else 
{
label_1136366:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1136375;
}
else 
{
label_1136375:; 
goto label_1136397;
}
}
else 
{
label_1136397:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1136419;
}
else 
{
goto label_1136404;
}
}
else 
{
label_1136404:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1136413;
}
else 
{
label_1136413:; 
goto label_1136419;
}
}
else 
{
label_1136419:; 
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
 __return_1136469 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_1136470 = x;
}
rsp_d = __return_1136469;
goto label_1136472;
rsp_d = __return_1136470;
label_1136472:; 
rsp_status = 1;
label_1136478:; 
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
goto label_1136559;
}
else 
{
goto label_1136500;
}
}
else 
{
label_1136500:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1136559;
}
else 
{
goto label_1136507;
}
}
else 
{
label_1136507:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1136559;
}
else 
{
goto label_1136514;
}
}
else 
{
label_1136514:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1136559;
}
else 
{
goto label_1136521;
}
}
else 
{
label_1136521:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1136559;
}
else 
{
goto label_1136528;
}
}
else 
{
label_1136528:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1136537;
}
else 
{
label_1136537:; 
goto label_1136559;
}
}
else 
{
label_1136559:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1136581;
}
else 
{
goto label_1136566;
}
}
else 
{
label_1136566:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1136575;
}
else 
{
label_1136575:; 
goto label_1136581;
}
}
else 
{
label_1136581:; 
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
goto label_1136664;
}
else 
{
goto label_1136605;
}
}
else 
{
label_1136605:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1136664;
}
else 
{
goto label_1136612;
}
}
else 
{
label_1136612:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1136664;
}
else 
{
goto label_1136619;
}
}
else 
{
label_1136619:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1136664;
}
else 
{
goto label_1136626;
}
}
else 
{
label_1136626:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1136664;
}
else 
{
goto label_1136633;
}
}
else 
{
label_1136633:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1136642;
}
else 
{
label_1136642:; 
goto label_1136664;
}
}
else 
{
label_1136664:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1136671;
}
}
else 
{
label_1136671:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1136680;
}
else 
{
label_1136680:; 
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
goto label_1130954;
}
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
goto label_1136447;
label_1136447:; 
rsp_status = 1;
goto label_1136478;
}
}
else 
{
rsp_status = 0;
goto label_1136478;
}
}
}
}
}
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
label_1136737:; 
goto label_1136246;
}
}
}
}
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
goto label_1117812;
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
goto label_1117596;
}
else 
{
goto label_1117537;
}
}
else 
{
label_1117537:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1117596;
}
else 
{
goto label_1117544;
}
}
else 
{
label_1117544:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1117596;
}
else 
{
goto label_1117551;
}
}
else 
{
label_1117551:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1117596;
}
else 
{
goto label_1117558;
}
}
else 
{
label_1117558:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1117596;
}
else 
{
goto label_1117565;
}
}
else 
{
label_1117565:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1117574;
}
else 
{
label_1117574:; 
goto label_1117596;
}
}
else 
{
label_1117596:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1117603;
}
}
else 
{
label_1117603:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1117612;
}
else 
{
label_1117612:; 
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
goto label_1116509;
}
}
}
}
else 
{
label_1117812:; 
label_1117818:; 
if (((int)m_run_st) == 0)
{
goto label_1117832;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1117832:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_1117843;
}
else 
{
label_1117843:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1118019;
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
goto label_1117970;
}
else 
{
goto label_1117911;
}
}
else 
{
label_1117911:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1117970;
}
else 
{
goto label_1117918;
}
}
else 
{
label_1117918:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1117970;
}
else 
{
goto label_1117925;
}
}
else 
{
label_1117925:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1117970;
}
else 
{
goto label_1117932;
}
}
else 
{
label_1117932:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1117970;
}
else 
{
goto label_1117939;
}
}
else 
{
label_1117939:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1117948;
}
else 
{
label_1117948:; 
goto label_1117970;
}
}
else 
{
label_1117970:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1117977;
}
}
else 
{
label_1117977:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1117986;
}
else 
{
label_1117986:; 
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
goto label_1116509;
}
}
}
}
else 
{
label_1118019:; 
goto label_1117818;
}
}
}
else 
{
}
goto label_1116517;
}
}
}
}
}
}
else 
{
label_1117461:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1117814;
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
goto label_1117762;
}
else 
{
goto label_1117703;
}
}
else 
{
label_1117703:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1117762;
}
else 
{
goto label_1117710;
}
}
else 
{
label_1117710:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1117762;
}
else 
{
goto label_1117717;
}
}
else 
{
label_1117717:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1117762;
}
else 
{
goto label_1117724;
}
}
else 
{
label_1117724:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1117762;
}
else 
{
goto label_1117731;
}
}
else 
{
label_1117731:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1117740;
}
else 
{
label_1117740:; 
goto label_1117762;
}
}
else 
{
label_1117762:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1117769;
}
}
else 
{
label_1117769:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1117778;
}
else 
{
label_1117778:; 
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
goto label_1116513;
}
}
}
}
else 
{
label_1117814:; 
goto label_1116966;
}
}
}
else 
{
}
label_1116977:; 
kernel_st = 2;
kernel_st = 3;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1118719;
}
else 
{
goto label_1118542;
}
}
else 
{
label_1118542:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1118719;
}
else 
{
goto label_1118567;
}
}
else 
{
label_1118567:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1118719;
}
else 
{
goto label_1118584;
}
}
else 
{
label_1118584:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1118719;
}
else 
{
goto label_1118609;
}
}
else 
{
label_1118609:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1118719;
}
else 
{
goto label_1118626;
}
}
else 
{
label_1118626:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1118653;
}
else 
{
label_1118653:; 
goto label_1118719;
}
}
else 
{
label_1118719:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1118789;
}
else 
{
goto label_1118744;
}
}
else 
{
label_1118744:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1118771;
}
else 
{
label_1118771:; 
goto label_1118789;
}
}
else 
{
label_1118789:; 
if (((int)m_run_st) == 0)
{
goto label_1118825;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1118825:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
label_1118837:; 
if (((int)m_run_st) == 0)
{
goto label_1118851;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1118851:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_1119333;
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
goto label_1118959;
}
else 
{
label_1118959:; 
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
goto label_1119062;
}
else 
{
goto label_1119003;
}
}
else 
{
label_1119003:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1119062;
}
else 
{
goto label_1119010;
}
}
else 
{
label_1119010:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1119062;
}
else 
{
goto label_1119017;
}
}
else 
{
label_1119017:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1119062;
}
else 
{
goto label_1119024;
}
}
else 
{
label_1119024:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1119062;
}
else 
{
goto label_1119031;
}
}
else 
{
label_1119031:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1119040;
}
else 
{
label_1119040:; 
goto label_1119062;
}
}
else 
{
label_1119062:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1119084;
}
else 
{
goto label_1119069;
}
}
else 
{
label_1119069:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1119078;
}
else 
{
label_1119078:; 
goto label_1119084;
}
}
else 
{
label_1119084:; 
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
goto label_1119164;
}
else 
{
goto label_1119105;
}
}
else 
{
label_1119105:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1119164;
}
else 
{
goto label_1119112;
}
}
else 
{
label_1119112:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1119164;
}
else 
{
goto label_1119119;
}
}
else 
{
label_1119119:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1119164;
}
else 
{
goto label_1119126;
}
}
else 
{
label_1119126:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1119164;
}
else 
{
goto label_1119133;
}
}
else 
{
label_1119133:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1119142;
}
else 
{
label_1119142:; 
goto label_1119164;
}
}
else 
{
label_1119164:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1119186;
}
else 
{
goto label_1119171;
}
}
else 
{
label_1119171:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1119180;
}
else 
{
label_1119180:; 
goto label_1119186;
}
}
else 
{
label_1119186:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1119259;
}
else 
{
goto label_1119200;
}
}
else 
{
label_1119200:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1119259;
}
else 
{
goto label_1119207;
}
}
else 
{
label_1119207:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1119259;
}
else 
{
goto label_1119214;
}
}
else 
{
label_1119214:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1119259;
}
else 
{
goto label_1119221;
}
}
else 
{
label_1119221:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1119259;
}
else 
{
goto label_1119228;
}
}
else 
{
label_1119228:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1119237;
}
else 
{
label_1119237:; 
goto label_1119259;
}
}
else 
{
label_1119259:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1119266;
}
}
else 
{
label_1119266:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1119275;
}
else 
{
label_1119275:; 
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
label_1119325:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1120129;
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
goto label_1119642;
}
else 
{
goto label_1119583;
}
}
else 
{
label_1119583:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1119642;
}
else 
{
goto label_1119590;
}
}
else 
{
label_1119590:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1119642;
}
else 
{
goto label_1119597;
}
}
else 
{
label_1119597:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1119642;
}
else 
{
goto label_1119604;
}
}
else 
{
label_1119604:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1119642;
}
else 
{
goto label_1119611;
}
}
else 
{
label_1119611:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1119620;
}
else 
{
label_1119620:; 
goto label_1119642;
}
}
else 
{
label_1119642:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1119664;
}
else 
{
goto label_1119649;
}
}
else 
{
label_1119649:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1119658;
}
else 
{
label_1119658:; 
goto label_1119664;
}
}
else 
{
label_1119664:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 1)
{
{
int __tmp_34 = req_a;
int __tmp_35 = req_d;
int i = __tmp_34;
int v = __tmp_35;
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
goto label_1119692;
label_1119692:; 
rsp_status = 1;
label_1119699:; 
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
goto label_1119778;
}
else 
{
goto label_1119719;
}
}
else 
{
label_1119719:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1119778;
}
else 
{
goto label_1119726;
}
}
else 
{
label_1119726:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1119778;
}
else 
{
goto label_1119733;
}
}
else 
{
label_1119733:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1119778;
}
else 
{
goto label_1119740;
}
}
else 
{
label_1119740:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1119778;
}
else 
{
goto label_1119747;
}
}
else 
{
label_1119747:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1119756;
}
else 
{
label_1119756:; 
goto label_1119778;
}
}
else 
{
label_1119778:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1119800;
}
else 
{
goto label_1119785;
}
}
else 
{
label_1119785:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1119794;
}
else 
{
label_1119794:; 
goto label_1119800;
}
}
else 
{
label_1119800:; 
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
goto label_1119883;
}
else 
{
goto label_1119824;
}
}
else 
{
label_1119824:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1119883;
}
else 
{
goto label_1119831;
}
}
else 
{
label_1119831:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1119883;
}
else 
{
goto label_1119838;
}
}
else 
{
label_1119838:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1119883;
}
else 
{
goto label_1119845;
}
}
else 
{
label_1119845:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1119883;
}
else 
{
goto label_1119852;
}
}
else 
{
label_1119852:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1119861;
}
else 
{
label_1119861:; 
goto label_1119883;
}
}
else 
{
label_1119883:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1119890;
}
}
else 
{
label_1119890:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1119899;
}
else 
{
label_1119899:; 
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
label_1119953:; 
label_1120138:; 
if (((int)m_run_st) == 0)
{
goto label_1120151;
}
else 
{
label_1120151:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_1121213;
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
goto label_1120339;
}
else 
{
goto label_1120280;
}
}
else 
{
label_1120280:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1120339;
}
else 
{
goto label_1120287;
}
}
else 
{
label_1120287:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1120339;
}
else 
{
goto label_1120294;
}
}
else 
{
label_1120294:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1120339;
}
else 
{
goto label_1120301;
}
}
else 
{
label_1120301:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1120339;
}
else 
{
goto label_1120308;
}
}
else 
{
label_1120308:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1120317;
}
else 
{
label_1120317:; 
goto label_1120339;
}
}
else 
{
label_1120339:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1120361;
}
else 
{
goto label_1120346;
}
}
else 
{
label_1120346:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1120355;
}
else 
{
label_1120355:; 
goto label_1120361;
}
}
else 
{
label_1120361:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1120434;
}
else 
{
goto label_1120375;
}
}
else 
{
label_1120375:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1120434;
}
else 
{
goto label_1120382;
}
}
else 
{
label_1120382:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1120434;
}
else 
{
goto label_1120389;
}
}
else 
{
label_1120389:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1120434;
}
else 
{
goto label_1120396;
}
}
else 
{
label_1120396:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1120434;
}
else 
{
goto label_1120403;
}
}
else 
{
label_1120403:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1120412;
}
else 
{
label_1120412:; 
goto label_1120434;
}
}
else 
{
label_1120434:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1120456;
}
else 
{
goto label_1120441;
}
}
else 
{
label_1120441:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1120450;
}
else 
{
label_1120450:; 
goto label_1120456;
}
}
else 
{
label_1120456:; 
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
goto label_1120921;
}
else 
{
goto label_1120862;
}
}
else 
{
label_1120862:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1120921;
}
else 
{
goto label_1120869;
}
}
else 
{
label_1120869:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1120921;
}
else 
{
goto label_1120876;
}
}
else 
{
label_1120876:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1120921;
}
else 
{
goto label_1120883;
}
}
else 
{
label_1120883:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1120921;
}
else 
{
goto label_1120890;
}
}
else 
{
label_1120890:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1120899;
}
else 
{
label_1120899:; 
goto label_1120921;
}
}
else 
{
label_1120921:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1120943;
}
else 
{
goto label_1120928;
}
}
else 
{
label_1120928:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1120937;
}
else 
{
label_1120937:; 
goto label_1120943;
}
}
else 
{
label_1120943:; 
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
goto label_1121023;
}
else 
{
goto label_1120964;
}
}
else 
{
label_1120964:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1121023;
}
else 
{
goto label_1120971;
}
}
else 
{
label_1120971:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1121023;
}
else 
{
goto label_1120978;
}
}
else 
{
label_1120978:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1121023;
}
else 
{
goto label_1120985;
}
}
else 
{
label_1120985:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1121023;
}
else 
{
goto label_1120992;
}
}
else 
{
label_1120992:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1121001;
}
else 
{
label_1121001:; 
goto label_1121023;
}
}
else 
{
label_1121023:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1121045;
}
else 
{
goto label_1121030;
}
}
else 
{
label_1121030:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1121039;
}
else 
{
label_1121039:; 
goto label_1121045;
}
}
else 
{
label_1121045:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1121118;
}
else 
{
goto label_1121059;
}
}
else 
{
label_1121059:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1121118;
}
else 
{
goto label_1121066;
}
}
else 
{
label_1121066:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1121118;
}
else 
{
goto label_1121073;
}
}
else 
{
label_1121073:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1121118;
}
else 
{
goto label_1121080;
}
}
else 
{
label_1121080:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1121118;
}
else 
{
goto label_1121087;
}
}
else 
{
label_1121087:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1121096;
}
else 
{
label_1121096:; 
goto label_1121118;
}
}
else 
{
label_1121118:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1121125;
}
}
else 
{
label_1121125:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1121134;
}
else 
{
label_1121134:; 
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
goto label_1119325;
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
goto label_1120567;
}
else 
{
goto label_1120508;
}
}
else 
{
label_1120508:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1120567;
}
else 
{
goto label_1120515;
}
}
else 
{
label_1120515:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1120567;
}
else 
{
goto label_1120522;
}
}
else 
{
label_1120522:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1120567;
}
else 
{
goto label_1120529;
}
}
else 
{
label_1120529:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1120567;
}
else 
{
goto label_1120536;
}
}
else 
{
label_1120536:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1120545;
}
else 
{
label_1120545:; 
goto label_1120567;
}
}
else 
{
label_1120567:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1120589;
}
else 
{
goto label_1120574;
}
}
else 
{
label_1120574:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1120583;
}
else 
{
label_1120583:; 
goto label_1120589;
}
}
else 
{
label_1120589:; 
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
goto label_1120669;
}
else 
{
goto label_1120610;
}
}
else 
{
label_1120610:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1120669;
}
else 
{
goto label_1120617;
}
}
else 
{
label_1120617:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1120669;
}
else 
{
goto label_1120624;
}
}
else 
{
label_1120624:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1120669;
}
else 
{
goto label_1120631;
}
}
else 
{
label_1120631:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1120669;
}
else 
{
goto label_1120638;
}
}
else 
{
label_1120638:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1120647;
}
else 
{
label_1120647:; 
goto label_1120669;
}
}
else 
{
label_1120669:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1120691;
}
else 
{
goto label_1120676;
}
}
else 
{
label_1120676:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1120685;
}
else 
{
label_1120685:; 
goto label_1120691;
}
}
else 
{
label_1120691:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1120764;
}
else 
{
goto label_1120705;
}
}
else 
{
label_1120705:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1120764;
}
else 
{
goto label_1120712;
}
}
else 
{
label_1120712:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1120764;
}
else 
{
goto label_1120719;
}
}
else 
{
label_1120719:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1120764;
}
else 
{
goto label_1120726;
}
}
else 
{
label_1120726:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1120764;
}
else 
{
goto label_1120733;
}
}
else 
{
label_1120733:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1120742;
}
else 
{
label_1120742:; 
goto label_1120764;
}
}
else 
{
label_1120764:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1120771;
}
}
else 
{
label_1120771:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1120780;
}
else 
{
label_1120780:; 
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
goto label_1121858;
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
goto label_1121348;
}
else 
{
goto label_1121289;
}
}
else 
{
label_1121289:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1121348;
}
else 
{
goto label_1121296;
}
}
else 
{
label_1121296:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1121348;
}
else 
{
goto label_1121303;
}
}
else 
{
label_1121303:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1121348;
}
else 
{
goto label_1121310;
}
}
else 
{
label_1121310:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1121348;
}
else 
{
goto label_1121317;
}
}
else 
{
label_1121317:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1121326;
}
else 
{
label_1121326:; 
goto label_1121348;
}
}
else 
{
label_1121348:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1121370;
}
else 
{
goto label_1121355;
}
}
else 
{
label_1121355:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1121364;
}
else 
{
label_1121364:; 
goto label_1121370;
}
}
else 
{
label_1121370:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_36 = req_a;
int i = __tmp_36;
int x;
if (i == 0)
{
x = s_memory0;
 __return_1121420 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_1121421 = x;
}
rsp_d = __return_1121420;
goto label_1121423;
rsp_d = __return_1121421;
label_1121423:; 
rsp_status = 1;
label_1121429:; 
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
goto label_1121510;
}
else 
{
goto label_1121451;
}
}
else 
{
label_1121451:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1121510;
}
else 
{
goto label_1121458;
}
}
else 
{
label_1121458:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1121510;
}
else 
{
goto label_1121465;
}
}
else 
{
label_1121465:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1121510;
}
else 
{
goto label_1121472;
}
}
else 
{
label_1121472:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1121510;
}
else 
{
goto label_1121479;
}
}
else 
{
label_1121479:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1121488;
}
else 
{
label_1121488:; 
goto label_1121510;
}
}
else 
{
label_1121510:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1121532;
}
else 
{
goto label_1121517;
}
}
else 
{
label_1121517:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1121526;
}
else 
{
label_1121526:; 
goto label_1121532;
}
}
else 
{
label_1121532:; 
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
goto label_1121615;
}
else 
{
goto label_1121556;
}
}
else 
{
label_1121556:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1121615;
}
else 
{
goto label_1121563;
}
}
else 
{
label_1121563:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1121615;
}
else 
{
goto label_1121570;
}
}
else 
{
label_1121570:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1121615;
}
else 
{
goto label_1121577;
}
}
else 
{
label_1121577:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1121615;
}
else 
{
goto label_1121584;
}
}
else 
{
label_1121584:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1121593;
}
else 
{
label_1121593:; 
goto label_1121615;
}
}
else 
{
label_1121615:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1121622;
}
}
else 
{
label_1121622:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1121631;
}
else 
{
label_1121631:; 
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
goto label_1145780;
}
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
int __tmp_37 = req_a;
int __tmp_38 = req_d;
int i = __tmp_37;
int v = __tmp_38;
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
goto label_1121398;
label_1121398:; 
rsp_status = 1;
goto label_1121429;
}
}
else 
{
rsp_status = 0;
goto label_1121429;
}
}
}
}
}
}
}
}
}
}
}
goto label_1151248;
}
}
}
}
else 
{
label_1121858:; 
label_1124493:; 
if (((int)m_run_st) == 0)
{
goto label_1124506;
}
else 
{
label_1124506:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_1124517;
}
else 
{
label_1124517:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1124985;
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
goto label_1124644;
}
else 
{
goto label_1124585;
}
}
else 
{
label_1124585:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1124644;
}
else 
{
goto label_1124592;
}
}
else 
{
label_1124592:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1124644;
}
else 
{
goto label_1124599;
}
}
else 
{
label_1124599:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1124644;
}
else 
{
goto label_1124606;
}
}
else 
{
label_1124606:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1124644;
}
else 
{
goto label_1124613;
}
}
else 
{
label_1124613:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1124622;
}
else 
{
label_1124622:; 
goto label_1124644;
}
}
else 
{
label_1124644:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1124666;
}
else 
{
goto label_1124651;
}
}
else 
{
label_1124651:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1124660;
}
else 
{
label_1124660:; 
goto label_1124666;
}
}
else 
{
label_1124666:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_39 = req_a;
int i = __tmp_39;
int x;
if (i == 0)
{
x = s_memory0;
 __return_1124716 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_1124717 = x;
}
rsp_d = __return_1124716;
goto label_1124719;
rsp_d = __return_1124717;
label_1124719:; 
rsp_status = 1;
label_1124725:; 
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
goto label_1124806;
}
else 
{
goto label_1124747;
}
}
else 
{
label_1124747:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1124806;
}
else 
{
goto label_1124754;
}
}
else 
{
label_1124754:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1124806;
}
else 
{
goto label_1124761;
}
}
else 
{
label_1124761:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1124806;
}
else 
{
goto label_1124768;
}
}
else 
{
label_1124768:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1124806;
}
else 
{
goto label_1124775;
}
}
else 
{
label_1124775:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1124784;
}
else 
{
label_1124784:; 
goto label_1124806;
}
}
else 
{
label_1124806:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1124828;
}
else 
{
goto label_1124813;
}
}
else 
{
label_1124813:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1124822;
}
else 
{
label_1124822:; 
goto label_1124828;
}
}
else 
{
label_1124828:; 
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
goto label_1124911;
}
else 
{
goto label_1124852;
}
}
else 
{
label_1124852:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1124911;
}
else 
{
goto label_1124859;
}
}
else 
{
label_1124859:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1124911;
}
else 
{
goto label_1124866;
}
}
else 
{
label_1124866:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1124911;
}
else 
{
goto label_1124873;
}
}
else 
{
label_1124873:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1124911;
}
else 
{
goto label_1124880;
}
}
else 
{
label_1124880:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1124889;
}
else 
{
label_1124889:; 
goto label_1124911;
}
}
else 
{
label_1124911:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1124918;
}
}
else 
{
label_1124918:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1124927;
}
else 
{
label_1124927:; 
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
label_1145780:; 
label_1145790:; 
if (((int)m_run_st) == 0)
{
goto label_1145803;
}
else 
{
label_1145803:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_1146159;
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
goto label_1145991;
}
else 
{
goto label_1145932;
}
}
else 
{
label_1145932:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1145991;
}
else 
{
goto label_1145939;
}
}
else 
{
label_1145939:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1145991;
}
else 
{
goto label_1145946;
}
}
else 
{
label_1145946:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1145991;
}
else 
{
goto label_1145953;
}
}
else 
{
label_1145953:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1145991;
}
else 
{
goto label_1145960;
}
}
else 
{
label_1145960:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1145969;
}
else 
{
label_1145969:; 
goto label_1145991;
}
}
else 
{
label_1145991:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1146013;
}
else 
{
goto label_1145998;
}
}
else 
{
label_1145998:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1146007;
}
else 
{
label_1146007:; 
goto label_1146013;
}
}
else 
{
label_1146013:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1146086;
}
else 
{
goto label_1146027;
}
}
else 
{
label_1146027:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1146086;
}
else 
{
goto label_1146034;
}
}
else 
{
label_1146034:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1146086;
}
else 
{
goto label_1146041;
}
}
else 
{
label_1146041:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1146086;
}
else 
{
goto label_1146048;
}
}
else 
{
label_1146048:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1146086;
}
else 
{
goto label_1146055;
}
}
else 
{
label_1146055:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1146064;
}
else 
{
label_1146064:; 
goto label_1146086;
}
}
else 
{
label_1146086:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1146108;
}
else 
{
goto label_1146093;
}
}
else 
{
label_1146093:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1146102;
}
else 
{
label_1146102:; 
goto label_1146108;
}
}
else 
{
label_1146108:; 
c_m_ev = 2;
if ((req_a___0 + 50) == rsp_d___0)
{
a = a + 1;
goto label_1146125;
}
else 
{
{
__VERIFIER_error();
}
a = a + 1;
label_1146125:; 
}
label_1146154:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1146511;
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
goto label_1146294;
}
else 
{
goto label_1146235;
}
}
else 
{
label_1146235:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1146294;
}
else 
{
goto label_1146242;
}
}
else 
{
label_1146242:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1146294;
}
else 
{
goto label_1146249;
}
}
else 
{
label_1146249:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1146294;
}
else 
{
goto label_1146256;
}
}
else 
{
label_1146256:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1146294;
}
else 
{
goto label_1146263;
}
}
else 
{
label_1146263:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1146272;
}
else 
{
label_1146272:; 
goto label_1146294;
}
}
else 
{
label_1146294:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1146301;
}
}
else 
{
label_1146301:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1146310;
}
else 
{
label_1146310:; 
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
label_1146340:; 
label_1146516:; 
if (((int)m_run_st) == 0)
{
goto label_1146530;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1146530:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_1146763;
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
goto label_1146718;
}
else 
{
goto label_1146659;
}
}
else 
{
label_1146659:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1146718;
}
else 
{
goto label_1146666;
}
}
else 
{
label_1146666:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1146718;
}
else 
{
goto label_1146673;
}
}
else 
{
label_1146673:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1146718;
}
else 
{
goto label_1146680;
}
}
else 
{
label_1146680:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1146718;
}
else 
{
goto label_1146687;
}
}
else 
{
label_1146687:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1146696;
}
else 
{
label_1146696:; 
goto label_1146718;
}
}
else 
{
label_1146718:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1146725;
}
}
else 
{
label_1146725:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1146734;
}
else 
{
label_1146734:; 
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
goto label_1146154;
}
}
}
}
else 
{
label_1146763:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1146939;
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
goto label_1146890;
}
else 
{
goto label_1146831;
}
}
else 
{
label_1146831:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1146890;
}
else 
{
goto label_1146838;
}
}
else 
{
label_1146838:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1146890;
}
else 
{
goto label_1146845;
}
}
else 
{
label_1146845:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1146890;
}
else 
{
goto label_1146852;
}
}
else 
{
label_1146852:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1146890;
}
else 
{
goto label_1146859;
}
}
else 
{
label_1146859:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1146868;
}
else 
{
label_1146868:; 
goto label_1146890;
}
}
else 
{
label_1146890:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1146897;
}
}
else 
{
label_1146897:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1146906;
}
else 
{
label_1146906:; 
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
goto label_1146340;
}
}
}
}
else 
{
label_1146939:; 
goto label_1146516;
}
}
}
else 
{
}
goto label_1145784;
}
}
}
}
}
else 
{
label_1146511:; 
goto label_1146516;
}
}
}
}
}
}
}
}
}
}
}
}
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
label_1146159:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1146513;
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
goto label_1146460;
}
else 
{
goto label_1146401;
}
}
else 
{
label_1146401:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1146460;
}
else 
{
goto label_1146408;
}
}
else 
{
label_1146408:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1146460;
}
else 
{
goto label_1146415;
}
}
else 
{
label_1146415:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1146460;
}
else 
{
goto label_1146422;
}
}
else 
{
label_1146422:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1146460;
}
else 
{
goto label_1146429;
}
}
else 
{
label_1146429:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1146438;
}
else 
{
label_1146438:; 
goto label_1146460;
}
}
else 
{
label_1146460:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1146467;
}
}
else 
{
label_1146467:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1146476;
}
else 
{
label_1146476:; 
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
goto label_1145780;
}
}
}
}
else 
{
label_1146513:; 
goto label_1145790;
}
}
}
}
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
int __tmp_40 = req_a;
int __tmp_41 = req_d;
int i = __tmp_40;
int v = __tmp_41;
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
goto label_1124694;
label_1124694:; 
rsp_status = 1;
goto label_1124725;
}
}
else 
{
rsp_status = 0;
goto label_1124725;
}
}
}
}
}
}
}
}
}
}
}
label_1151248:; 
if (((int)m_run_st) == 0)
{
goto label_1151272;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1151272:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_1151283;
}
else 
{
label_1151283:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1151459;
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
goto label_1151410;
}
else 
{
goto label_1151351;
}
}
else 
{
label_1151351:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1151410;
}
else 
{
goto label_1151358;
}
}
else 
{
label_1151358:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1151410;
}
else 
{
goto label_1151365;
}
}
else 
{
label_1151365:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1151410;
}
else 
{
goto label_1151372;
}
}
else 
{
label_1151372:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1151410;
}
else 
{
goto label_1151379;
}
}
else 
{
label_1151379:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1151388;
}
else 
{
label_1151388:; 
goto label_1151410;
}
}
else 
{
label_1151410:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1151417;
}
}
else 
{
label_1151417:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1151426;
}
else 
{
label_1151426:; 
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
goto label_1151248;
}
}
}
}
else 
{
label_1151459:; 
goto label_1124493;
}
}
}
else 
{
}
goto label_1151252;
}
}
}
}
}
else 
{
label_1124985:; 
goto label_1124493;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_1121213:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1121860;
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
goto label_1121806;
}
else 
{
goto label_1121747;
}
}
else 
{
label_1121747:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1121806;
}
else 
{
goto label_1121754;
}
}
else 
{
label_1121754:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1121806;
}
else 
{
goto label_1121761;
}
}
else 
{
label_1121761:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1121806;
}
else 
{
goto label_1121768;
}
}
else 
{
label_1121768:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1121806;
}
else 
{
goto label_1121775;
}
}
else 
{
label_1121775:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1121784;
}
else 
{
label_1121784:; 
goto label_1121806;
}
}
else 
{
label_1121806:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1121813;
}
}
else 
{
label_1121813:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1121822;
}
else 
{
label_1121822:; 
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
goto label_1119953;
}
}
}
}
else 
{
label_1121860:; 
goto label_1120138;
}
}
}
}
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
rsp_status = 0;
goto label_1119699;
}
}
}
}
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
label_1120129:; 
label_1125196:; 
if (((int)m_run_st) == 0)
{
goto label_1125209;
}
else 
{
label_1125209:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_1125220;
}
else 
{
label_1125220:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1125687;
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
goto label_1125347;
}
else 
{
goto label_1125288;
}
}
else 
{
label_1125288:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1125347;
}
else 
{
goto label_1125295;
}
}
else 
{
label_1125295:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1125347;
}
else 
{
goto label_1125302;
}
}
else 
{
label_1125302:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1125347;
}
else 
{
goto label_1125309;
}
}
else 
{
label_1125309:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1125347;
}
else 
{
goto label_1125316;
}
}
else 
{
label_1125316:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1125325;
}
else 
{
label_1125325:; 
goto label_1125347;
}
}
else 
{
label_1125347:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1125369;
}
else 
{
goto label_1125354;
}
}
else 
{
label_1125354:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1125363;
}
else 
{
label_1125363:; 
goto label_1125369;
}
}
else 
{
label_1125369:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_42 = req_a;
int i = __tmp_42;
int x;
if (i == 0)
{
x = s_memory0;
 __return_1125419 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_1125420 = x;
}
rsp_d = __return_1125419;
goto label_1125422;
rsp_d = __return_1125420;
label_1125422:; 
rsp_status = 1;
label_1125428:; 
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
goto label_1125509;
}
else 
{
goto label_1125450;
}
}
else 
{
label_1125450:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1125509;
}
else 
{
goto label_1125457;
}
}
else 
{
label_1125457:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1125509;
}
else 
{
goto label_1125464;
}
}
else 
{
label_1125464:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1125509;
}
else 
{
goto label_1125471;
}
}
else 
{
label_1125471:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1125509;
}
else 
{
goto label_1125478;
}
}
else 
{
label_1125478:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1125487;
}
else 
{
label_1125487:; 
goto label_1125509;
}
}
else 
{
label_1125509:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1125531;
}
else 
{
goto label_1125516;
}
}
else 
{
label_1125516:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1125525;
}
else 
{
label_1125525:; 
goto label_1125531;
}
}
else 
{
label_1125531:; 
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
goto label_1125614;
}
else 
{
goto label_1125555;
}
}
else 
{
label_1125555:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1125614;
}
else 
{
goto label_1125562;
}
}
else 
{
label_1125562:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1125614;
}
else 
{
goto label_1125569;
}
}
else 
{
label_1125569:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1125614;
}
else 
{
goto label_1125576;
}
}
else 
{
label_1125576:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1125614;
}
else 
{
goto label_1125583;
}
}
else 
{
label_1125583:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1125592;
}
else 
{
label_1125592:; 
goto label_1125614;
}
}
else 
{
label_1125614:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1125621;
}
}
else 
{
label_1125621:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1125630;
}
else 
{
label_1125630:; 
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
goto label_1119953;
}
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
int __tmp_43 = req_a;
int __tmp_44 = req_d;
int i = __tmp_43;
int v = __tmp_44;
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
goto label_1125397;
label_1125397:; 
rsp_status = 1;
goto label_1125428;
}
}
else 
{
rsp_status = 0;
goto label_1125428;
}
}
}
}
}
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
label_1125687:; 
goto label_1125196;
}
}
}
}
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
goto label_1120127;
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
goto label_1119476;
}
else 
{
goto label_1119417;
}
}
else 
{
label_1119417:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1119476;
}
else 
{
goto label_1119424;
}
}
else 
{
label_1119424:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1119476;
}
else 
{
goto label_1119431;
}
}
else 
{
label_1119431:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1119476;
}
else 
{
goto label_1119438;
}
}
else 
{
label_1119438:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1119476;
}
else 
{
goto label_1119445;
}
}
else 
{
label_1119445:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1119454;
}
else 
{
label_1119454:; 
goto label_1119476;
}
}
else 
{
label_1119476:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1119483;
}
}
else 
{
label_1119483:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1119492;
}
else 
{
label_1119492:; 
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
label_1119522:; 
label_1124989:; 
if (((int)m_run_st) == 0)
{
goto label_1125003;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1125003:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_1125014;
}
else 
{
label_1125014:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1125190;
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
goto label_1125141;
}
else 
{
goto label_1125082;
}
}
else 
{
label_1125082:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1125141;
}
else 
{
goto label_1125089;
}
}
else 
{
label_1125089:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1125141;
}
else 
{
goto label_1125096;
}
}
else 
{
label_1125096:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1125141;
}
else 
{
goto label_1125103;
}
}
else 
{
label_1125103:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1125141;
}
else 
{
goto label_1125110;
}
}
else 
{
label_1125110:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1125119;
}
else 
{
label_1125119:; 
goto label_1125141;
}
}
else 
{
label_1125141:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1125148;
}
}
else 
{
label_1125148:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1125157;
}
else 
{
label_1125157:; 
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
goto label_1119522;
}
}
}
}
else 
{
label_1125190:; 
goto label_1124989;
}
}
}
else 
{
}
goto label_1116517;
}
}
}
}
}
else 
{
label_1120127:; 
goto label_1124989;
}
}
}
}
}
else 
{
label_1119333:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1120131;
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
goto label_1120073;
}
else 
{
goto label_1120014;
}
}
else 
{
label_1120014:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1120073;
}
else 
{
goto label_1120021;
}
}
else 
{
label_1120021:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1120073;
}
else 
{
goto label_1120028;
}
}
else 
{
label_1120028:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1120073;
}
else 
{
goto label_1120035;
}
}
else 
{
label_1120035:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1120073;
}
else 
{
goto label_1120042;
}
}
else 
{
label_1120042:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1120051;
}
else 
{
label_1120051:; 
goto label_1120073;
}
}
else 
{
label_1120073:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1120080;
}
}
else 
{
label_1120080:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1120089;
}
else 
{
label_1120089:; 
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
goto label_1118837;
}
}
}
}
else 
{
label_1120131:; 
goto label_1118837;
}
}
}
else 
{
}
goto label_1116977;
}
}
}
else 
{
}
goto label_907416;
}
}
}
}
}
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
label_898985:; 
goto label_897691;
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
goto label_907153;
}
else 
{
goto label_906622;
}
}
else 
{
label_906622:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_907153;
}
else 
{
goto label_906669;
}
}
else 
{
label_906669:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_907153;
}
else 
{
goto label_906748;
}
}
else 
{
label_906748:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_907153;
}
else 
{
goto label_906795;
}
}
else 
{
label_906795:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_907153;
}
else 
{
goto label_906874;
}
}
else 
{
label_906874:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_906955;
}
else 
{
label_906955:; 
goto label_907153;
}
}
else 
{
label_907153:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_907335;
}
else 
{
goto label_907200;
}
}
else 
{
label_907200:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_907281;
}
else 
{
label_907281:; 
goto label_907335;
}
}
else 
{
label_907335:; 
if (((int)m_run_st) == 0)
{
goto label_907443;
}
else 
{
if (((int)s_run_st) == 0)
{
label_907443:; 
goto label_897681;
}
else 
{
}
label_907416:; 
__retres1 = 0;
 __return_1152293 = __retres1;
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
