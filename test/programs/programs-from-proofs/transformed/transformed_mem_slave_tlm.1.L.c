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
int __return_849254;
int __return_849255;
int __return_853016;
int __return_853017;
int __return_856184;
int __return_856185;
int __return_866938;
int __return_866939;
int __return_868327;
int __return_868328;
int __return_856684;
int __return_856685;
int __return_859777;
int __return_859778;
int __return_861810;
int __return_861811;
int __return_865934;
int __return_865935;
int __return_862267;
int __return_862268;
int __return_865438;
int __return_865439;
int __return_866432;
int __return_866433;
int __return_860234;
int __return_860235;
int __return_850579;
int __return_850580;
int __return_851247;
int __return_851248;
int __return_871638;
int __return_871639;
int __return_873671;
int __return_873672;
int __return_877795;
int __return_877796;
int __return_874128;
int __return_874129;
int __return_877299;
int __return_877300;
int __return_872095;
int __return_872096;
int __return_878716;
int __return_878714;
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
goto label_848270;
}
else 
{
m_run_st = 2;
label_848270:; 
if (((int)s_run_i) == 1)
{
s_run_st = 0;
goto label_848277;
}
else 
{
s_run_st = 2;
label_848277:; 
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_848343;
}
else 
{
goto label_848284;
}
}
else 
{
label_848284:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_848343;
}
else 
{
goto label_848291;
}
}
else 
{
label_848291:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_848343;
}
else 
{
goto label_848298;
}
}
else 
{
label_848298:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_848343;
}
else 
{
goto label_848305;
}
}
else 
{
label_848305:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_848343;
}
else 
{
goto label_848312;
}
}
else 
{
label_848312:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_848321;
}
else 
{
label_848321:; 
goto label_848343;
}
}
else 
{
label_848343:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_848365;
}
else 
{
goto label_848350;
}
}
else 
{
label_848350:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_848359;
}
else 
{
label_848359:; 
goto label_848365;
}
}
else 
{
label_848365:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
label_848377:; 
if (((int)m_run_st) == 0)
{
goto label_848391;
}
else 
{
if (((int)s_run_st) == 0)
{
label_848391:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_848873;
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
goto label_848499;
}
else 
{
label_848499:; 
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
goto label_848602;
}
else 
{
goto label_848543;
}
}
else 
{
label_848543:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_848602;
}
else 
{
goto label_848550;
}
}
else 
{
label_848550:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_848602;
}
else 
{
goto label_848557;
}
}
else 
{
label_848557:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_848602;
}
else 
{
goto label_848564;
}
}
else 
{
label_848564:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_848602;
}
else 
{
goto label_848571;
}
}
else 
{
label_848571:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_848580;
}
else 
{
label_848580:; 
goto label_848602;
}
}
else 
{
label_848602:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_848624;
}
else 
{
goto label_848609;
}
}
else 
{
label_848609:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_848618;
}
else 
{
label_848618:; 
goto label_848624;
}
}
else 
{
label_848624:; 
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
goto label_848704;
}
else 
{
goto label_848645;
}
}
else 
{
label_848645:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_848704;
}
else 
{
goto label_848652;
}
}
else 
{
label_848652:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_848704;
}
else 
{
goto label_848659;
}
}
else 
{
label_848659:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_848704;
}
else 
{
goto label_848666;
}
}
else 
{
label_848666:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_848704;
}
else 
{
goto label_848673;
}
}
else 
{
label_848673:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_848682;
}
else 
{
label_848682:; 
goto label_848704;
}
}
else 
{
label_848704:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_848726;
}
else 
{
goto label_848711;
}
}
else 
{
label_848711:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_848720;
}
else 
{
label_848720:; 
goto label_848726;
}
}
else 
{
label_848726:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_848799;
}
else 
{
goto label_848740;
}
}
else 
{
label_848740:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_848799;
}
else 
{
goto label_848747;
}
}
else 
{
label_848747:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_848799;
}
else 
{
goto label_848754;
}
}
else 
{
label_848754:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_848799;
}
else 
{
goto label_848761;
}
}
else 
{
label_848761:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_848799;
}
else 
{
goto label_848768;
}
}
else 
{
label_848768:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_848777;
}
else 
{
label_848777:; 
goto label_848799;
}
}
else 
{
label_848799:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_848806;
}
}
else 
{
label_848806:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_848815;
}
else 
{
label_848815:; 
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
goto label_849695;
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
goto label_849100;
}
else 
{
label_849100:; 
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
goto label_849182;
}
else 
{
goto label_849123;
}
}
else 
{
label_849123:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_849182;
}
else 
{
goto label_849130;
}
}
else 
{
label_849130:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_849182;
}
else 
{
goto label_849137;
}
}
else 
{
label_849137:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_849182;
}
else 
{
goto label_849144;
}
}
else 
{
label_849144:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_849182;
}
else 
{
goto label_849151;
}
}
else 
{
label_849151:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_849160;
}
else 
{
label_849160:; 
goto label_849182;
}
}
else 
{
label_849182:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_849204;
}
else 
{
goto label_849189;
}
}
else 
{
label_849189:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_849198;
}
else 
{
label_849198:; 
goto label_849204;
}
}
else 
{
label_849204:; 
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
 __return_849254 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_849255 = x;
}
rsp_d = __return_849254;
goto label_849257;
rsp_d = __return_849255;
label_849257:; 
rsp_status = 1;
label_849263:; 
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
goto label_849344;
}
else 
{
goto label_849285;
}
}
else 
{
label_849285:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_849344;
}
else 
{
goto label_849292;
}
}
else 
{
label_849292:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_849344;
}
else 
{
goto label_849299;
}
}
else 
{
label_849299:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_849344;
}
else 
{
goto label_849306;
}
}
else 
{
label_849306:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_849344;
}
else 
{
goto label_849313;
}
}
else 
{
label_849313:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_849322;
}
else 
{
label_849322:; 
goto label_849344;
}
}
else 
{
label_849344:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_849366;
}
else 
{
goto label_849351;
}
}
else 
{
label_849351:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_849360;
}
else 
{
label_849360:; 
goto label_849366;
}
}
else 
{
label_849366:; 
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
goto label_849449;
}
else 
{
goto label_849390;
}
}
else 
{
label_849390:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_849449;
}
else 
{
goto label_849397;
}
}
else 
{
label_849397:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_849449;
}
else 
{
goto label_849404;
}
}
else 
{
label_849404:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_849449;
}
else 
{
goto label_849411;
}
}
else 
{
label_849411:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_849449;
}
else 
{
goto label_849418;
}
}
else 
{
label_849418:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_849427;
}
else 
{
label_849427:; 
goto label_849449;
}
}
else 
{
label_849449:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_849456;
}
}
else 
{
label_849456:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_849465;
}
else 
{
label_849465:; 
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
label_849519:; 
label_851724:; 
if (((int)m_run_st) == 0)
{
goto label_851737;
}
else 
{
label_851737:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_852801;
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
goto label_851925;
}
else 
{
goto label_851866;
}
}
else 
{
label_851866:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_851925;
}
else 
{
goto label_851873;
}
}
else 
{
label_851873:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_851925;
}
else 
{
goto label_851880;
}
}
else 
{
label_851880:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_851925;
}
else 
{
goto label_851887;
}
}
else 
{
label_851887:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_851925;
}
else 
{
goto label_851894;
}
}
else 
{
label_851894:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_851903;
}
else 
{
label_851903:; 
goto label_851925;
}
}
else 
{
label_851925:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_851947;
}
else 
{
goto label_851932;
}
}
else 
{
label_851932:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_851941;
}
else 
{
label_851941:; 
goto label_851947;
}
}
else 
{
label_851947:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_852020;
}
else 
{
goto label_851961;
}
}
else 
{
label_851961:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_852020;
}
else 
{
goto label_851968;
}
}
else 
{
label_851968:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_852020;
}
else 
{
goto label_851975;
}
}
else 
{
label_851975:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_852020;
}
else 
{
goto label_851982;
}
}
else 
{
label_851982:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_852020;
}
else 
{
goto label_851989;
}
}
else 
{
label_851989:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_851998;
}
else 
{
label_851998:; 
goto label_852020;
}
}
else 
{
label_852020:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_852042;
}
else 
{
goto label_852027;
}
}
else 
{
label_852027:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_852036;
}
else 
{
label_852036:; 
goto label_852042;
}
}
else 
{
label_852042:; 
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
goto label_852507;
}
else 
{
goto label_852448;
}
}
else 
{
label_852448:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_852507;
}
else 
{
goto label_852455;
}
}
else 
{
label_852455:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_852507;
}
else 
{
goto label_852462;
}
}
else 
{
label_852462:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_852507;
}
else 
{
goto label_852469;
}
}
else 
{
label_852469:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_852507;
}
else 
{
goto label_852476;
}
}
else 
{
label_852476:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_852485;
}
else 
{
label_852485:; 
goto label_852507;
}
}
else 
{
label_852507:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_852529;
}
else 
{
goto label_852514;
}
}
else 
{
label_852514:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_852523;
}
else 
{
label_852523:; 
goto label_852529;
}
}
else 
{
label_852529:; 
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
goto label_852609;
}
else 
{
goto label_852550;
}
}
else 
{
label_852550:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_852609;
}
else 
{
goto label_852557;
}
}
else 
{
label_852557:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_852609;
}
else 
{
goto label_852564;
}
}
else 
{
label_852564:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_852609;
}
else 
{
goto label_852571;
}
}
else 
{
label_852571:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_852609;
}
else 
{
goto label_852578;
}
}
else 
{
label_852578:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_852587;
}
else 
{
label_852587:; 
goto label_852609;
}
}
else 
{
label_852609:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_852631;
}
else 
{
goto label_852616;
}
}
else 
{
label_852616:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_852625;
}
else 
{
label_852625:; 
goto label_852631;
}
}
else 
{
label_852631:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_852704;
}
else 
{
goto label_852645;
}
}
else 
{
label_852645:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_852704;
}
else 
{
goto label_852652;
}
}
else 
{
label_852652:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_852704;
}
else 
{
goto label_852659;
}
}
else 
{
label_852659:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_852704;
}
else 
{
goto label_852666;
}
}
else 
{
label_852666:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_852704;
}
else 
{
goto label_852673;
}
}
else 
{
label_852673:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_852682;
}
else 
{
label_852682:; 
goto label_852704;
}
}
else 
{
label_852704:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_852711;
}
}
else 
{
label_852711:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_852720;
}
else 
{
label_852720:; 
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
goto label_850190;
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
if (a < 1)
{
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
goto label_852153;
}
else 
{
goto label_852094;
}
}
else 
{
label_852094:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_852153;
}
else 
{
goto label_852101;
}
}
else 
{
label_852101:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_852153;
}
else 
{
goto label_852108;
}
}
else 
{
label_852108:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_852153;
}
else 
{
goto label_852115;
}
}
else 
{
label_852115:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_852153;
}
else 
{
goto label_852122;
}
}
else 
{
label_852122:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_852131;
}
else 
{
label_852131:; 
goto label_852153;
}
}
else 
{
label_852153:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_852175;
}
else 
{
goto label_852160;
}
}
else 
{
label_852160:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_852169;
}
else 
{
label_852169:; 
goto label_852175;
}
}
else 
{
label_852175:; 
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
goto label_852255;
}
else 
{
goto label_852196;
}
}
else 
{
label_852196:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_852255;
}
else 
{
goto label_852203;
}
}
else 
{
label_852203:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_852255;
}
else 
{
goto label_852210;
}
}
else 
{
label_852210:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_852255;
}
else 
{
goto label_852217;
}
}
else 
{
label_852217:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_852255;
}
else 
{
goto label_852224;
}
}
else 
{
label_852224:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_852233;
}
else 
{
label_852233:; 
goto label_852255;
}
}
else 
{
label_852255:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_852277;
}
else 
{
goto label_852262;
}
}
else 
{
label_852262:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_852271;
}
else 
{
label_852271:; 
goto label_852277;
}
}
else 
{
label_852277:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_852350;
}
else 
{
goto label_852291;
}
}
else 
{
label_852291:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_852350;
}
else 
{
goto label_852298;
}
}
else 
{
label_852298:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_852350;
}
else 
{
goto label_852305;
}
}
else 
{
label_852305:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_852350;
}
else 
{
goto label_852312;
}
}
else 
{
label_852312:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_852350;
}
else 
{
goto label_852319;
}
}
else 
{
label_852319:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_852328;
}
else 
{
label_852328:; 
goto label_852350;
}
}
else 
{
label_852350:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_852357;
}
}
else 
{
label_852357:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_852366;
}
else 
{
label_852366:; 
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
label_852789:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_853620;
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
goto label_852944;
}
else 
{
goto label_852885;
}
}
else 
{
label_852885:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_852944;
}
else 
{
goto label_852892;
}
}
else 
{
label_852892:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_852944;
}
else 
{
goto label_852899;
}
}
else 
{
label_852899:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_852944;
}
else 
{
goto label_852906;
}
}
else 
{
label_852906:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_852944;
}
else 
{
goto label_852913;
}
}
else 
{
label_852913:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_852922;
}
else 
{
label_852922:; 
goto label_852944;
}
}
else 
{
label_852944:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_852966;
}
else 
{
goto label_852951;
}
}
else 
{
label_852951:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_852960;
}
else 
{
label_852960:; 
goto label_852966;
}
}
else 
{
label_852966:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_2 = req_a;
int i = __tmp_2;
int x;
if (i == 0)
{
x = s_memory0;
 __return_853016 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_853017 = x;
}
rsp_d = __return_853016;
goto label_853019;
rsp_d = __return_853017;
label_853019:; 
rsp_status = 1;
label_853025:; 
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
goto label_853106;
}
else 
{
goto label_853047;
}
}
else 
{
label_853047:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_853106;
}
else 
{
goto label_853054;
}
}
else 
{
label_853054:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_853106;
}
else 
{
goto label_853061;
}
}
else 
{
label_853061:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_853106;
}
else 
{
goto label_853068;
}
}
else 
{
label_853068:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_853106;
}
else 
{
goto label_853075;
}
}
else 
{
label_853075:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_853084;
}
else 
{
label_853084:; 
goto label_853106;
}
}
else 
{
label_853106:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_853128;
}
else 
{
goto label_853113;
}
}
else 
{
label_853113:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_853122;
}
else 
{
label_853122:; 
goto label_853128;
}
}
else 
{
label_853128:; 
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
goto label_853211;
}
else 
{
goto label_853152;
}
}
else 
{
label_853152:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_853211;
}
else 
{
goto label_853159;
}
}
else 
{
label_853159:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_853211;
}
else 
{
goto label_853166;
}
}
else 
{
label_853166:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_853211;
}
else 
{
goto label_853173;
}
}
else 
{
label_853173:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_853211;
}
else 
{
goto label_853180;
}
}
else 
{
label_853180:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_853189;
}
else 
{
label_853189:; 
goto label_853211;
}
}
else 
{
label_853211:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_853218;
}
}
else 
{
label_853218:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_853227;
}
else 
{
label_853227:; 
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
label_853281:; 
label_854055:; 
if (((int)m_run_st) == 0)
{
goto label_854068;
}
else 
{
label_854068:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_854782;
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
goto label_854256;
}
else 
{
goto label_854197;
}
}
else 
{
label_854197:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_854256;
}
else 
{
goto label_854204;
}
}
else 
{
label_854204:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_854256;
}
else 
{
goto label_854211;
}
}
else 
{
label_854211:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_854256;
}
else 
{
goto label_854218;
}
}
else 
{
label_854218:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_854256;
}
else 
{
goto label_854225;
}
}
else 
{
label_854225:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_854234;
}
else 
{
label_854234:; 
goto label_854256;
}
}
else 
{
label_854256:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_854278;
}
else 
{
goto label_854263;
}
}
else 
{
label_854263:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_854272;
}
else 
{
label_854272:; 
goto label_854278;
}
}
else 
{
label_854278:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_854351;
}
else 
{
goto label_854292;
}
}
else 
{
label_854292:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_854351;
}
else 
{
goto label_854299;
}
}
else 
{
label_854299:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_854351;
}
else 
{
goto label_854306;
}
}
else 
{
label_854306:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_854351;
}
else 
{
goto label_854313;
}
}
else 
{
label_854313:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_854351;
}
else 
{
goto label_854320;
}
}
else 
{
label_854320:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_854329;
}
else 
{
label_854329:; 
goto label_854351;
}
}
else 
{
label_854351:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_854373;
}
else 
{
goto label_854358;
}
}
else 
{
label_854358:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_854367;
}
else 
{
label_854367:; 
goto label_854373;
}
}
else 
{
label_854373:; 
c_m_ev = 2;
if ((req_a___0 + 50) == rsp_d___0)
{
a = a + 1;
goto label_854390;
}
else 
{
{
__VERIFIER_error();
}
a = a + 1;
label_854390:; 
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
goto label_854488;
}
else 
{
goto label_854429;
}
}
else 
{
label_854429:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_854488;
}
else 
{
goto label_854436;
}
}
else 
{
label_854436:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_854488;
}
else 
{
goto label_854443;
}
}
else 
{
label_854443:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_854488;
}
else 
{
goto label_854450;
}
}
else 
{
label_854450:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_854488;
}
else 
{
goto label_854457;
}
}
else 
{
label_854457:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_854466;
}
else 
{
label_854466:; 
goto label_854488;
}
}
else 
{
label_854488:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_854510;
}
else 
{
goto label_854495;
}
}
else 
{
label_854495:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_854504;
}
else 
{
label_854504:; 
goto label_854510;
}
}
else 
{
label_854510:; 
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
goto label_854590;
}
else 
{
goto label_854531;
}
}
else 
{
label_854531:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_854590;
}
else 
{
goto label_854538;
}
}
else 
{
label_854538:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_854590;
}
else 
{
goto label_854545;
}
}
else 
{
label_854545:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_854590;
}
else 
{
goto label_854552;
}
}
else 
{
label_854552:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_854590;
}
else 
{
goto label_854559;
}
}
else 
{
label_854559:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_854568;
}
else 
{
label_854568:; 
goto label_854590;
}
}
else 
{
label_854590:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_854612;
}
else 
{
goto label_854597;
}
}
else 
{
label_854597:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_854606;
}
else 
{
label_854606:; 
goto label_854612;
}
}
else 
{
label_854612:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_854685;
}
else 
{
goto label_854626;
}
}
else 
{
label_854626:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_854685;
}
else 
{
goto label_854633;
}
}
else 
{
label_854633:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_854685;
}
else 
{
goto label_854640;
}
}
else 
{
label_854640:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_854685;
}
else 
{
goto label_854647;
}
}
else 
{
label_854647:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_854685;
}
else 
{
goto label_854654;
}
}
else 
{
label_854654:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_854663;
}
else 
{
label_854663:; 
goto label_854685;
}
}
else 
{
label_854685:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_854692;
}
}
else 
{
label_854692:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_854701;
}
else 
{
label_854701:; 
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
goto label_852789;
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
goto label_855310;
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
goto label_854925;
}
else 
{
goto label_854866;
}
}
else 
{
label_854866:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_854925;
}
else 
{
goto label_854873;
}
}
else 
{
label_854873:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_854925;
}
else 
{
goto label_854880;
}
}
else 
{
label_854880:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_854925;
}
else 
{
goto label_854887;
}
}
else 
{
label_854887:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_854925;
}
else 
{
goto label_854894;
}
}
else 
{
label_854894:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_854903;
}
else 
{
label_854903:; 
goto label_854925;
}
}
else 
{
label_854925:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_854932;
}
}
else 
{
label_854932:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_854941;
}
else 
{
label_854941:; 
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
label_854971:; 
label_855745:; 
if (((int)m_run_st) == 0)
{
goto label_855759;
}
else 
{
if (((int)s_run_st) == 0)
{
label_855759:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_855770;
}
else 
{
label_855770:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_855946;
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
goto label_855897;
}
else 
{
goto label_855838;
}
}
else 
{
label_855838:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_855897;
}
else 
{
goto label_855845;
}
}
else 
{
label_855845:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_855897;
}
else 
{
goto label_855852;
}
}
else 
{
label_855852:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_855897;
}
else 
{
goto label_855859;
}
}
else 
{
label_855859:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_855897;
}
else 
{
goto label_855866;
}
}
else 
{
label_855866:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_855875;
}
else 
{
label_855875:; 
goto label_855897;
}
}
else 
{
label_855897:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_855904;
}
}
else 
{
label_855904:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_855913;
}
else 
{
label_855913:; 
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
goto label_854971;
}
}
}
}
else 
{
label_855946:; 
goto label_855745;
}
}
}
else 
{
}
label_855756:; 
kernel_st = 2;
kernel_st = 3;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_857827;
}
else 
{
goto label_857237;
}
}
else 
{
label_857237:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_857827;
}
else 
{
goto label_857313;
}
}
else 
{
label_857313:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_857827;
}
else 
{
goto label_857377;
}
}
else 
{
label_857377:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_857827;
}
else 
{
goto label_857453;
}
}
else 
{
label_857453:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_857827;
}
else 
{
goto label_857517;
}
}
else 
{
label_857517:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_857607;
}
else 
{
label_857607:; 
goto label_857827;
}
}
else 
{
label_857827:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_858053;
}
else 
{
goto label_857903;
}
}
else 
{
label_857903:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_857993;
}
else 
{
label_857993:; 
goto label_858053;
}
}
else 
{
label_858053:; 
if (((int)m_run_st) == 0)
{
goto label_858173;
}
else 
{
if (((int)s_run_st) == 0)
{
label_858173:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
label_869594:; 
if (((int)m_run_st) == 0)
{
goto label_869608;
}
else 
{
if (((int)s_run_st) == 0)
{
label_869608:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_869746;
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
goto label_870099;
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
goto label_869881;
}
else 
{
goto label_869822;
}
}
else 
{
label_869822:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_869881;
}
else 
{
goto label_869829;
}
}
else 
{
label_869829:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_869881;
}
else 
{
goto label_869836;
}
}
else 
{
label_869836:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_869881;
}
else 
{
goto label_869843;
}
}
else 
{
label_869843:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_869881;
}
else 
{
goto label_869850;
}
}
else 
{
label_869850:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_869859;
}
else 
{
label_869859:; 
goto label_869881;
}
}
else 
{
label_869881:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_869888;
}
}
else 
{
label_869888:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_869897;
}
else 
{
label_869897:; 
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
goto label_869594;
}
}
}
}
else 
{
label_870099:; 
goto label_869594;
}
}
}
}
else 
{
label_869746:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_870101;
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
goto label_870047;
}
else 
{
goto label_869988;
}
}
else 
{
label_869988:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_870047;
}
else 
{
goto label_869995;
}
}
else 
{
label_869995:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_870047;
}
else 
{
goto label_870002;
}
}
else 
{
label_870002:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_870047;
}
else 
{
goto label_870009;
}
}
else 
{
label_870009:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_870047;
}
else 
{
goto label_870016;
}
}
else 
{
label_870016:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_870025;
}
else 
{
label_870025:; 
goto label_870047;
}
}
else 
{
label_870047:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_870054;
}
}
else 
{
label_870054:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_870063;
}
else 
{
label_870063:; 
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
goto label_869594;
}
}
}
}
else 
{
label_870101:; 
goto label_869594;
}
}
}
else 
{
}
goto label_855756;
}
}
}
else 
{
}
goto label_858131;
}
}
}
}
}
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
label_855310:; 
goto label_855745;
}
}
else 
{
}
label_854771:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_855312;
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
goto label_855091;
}
else 
{
goto label_855032;
}
}
else 
{
label_855032:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_855091;
}
else 
{
goto label_855039;
}
}
else 
{
label_855039:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_855091;
}
else 
{
goto label_855046;
}
}
else 
{
label_855046:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_855091;
}
else 
{
goto label_855053;
}
}
else 
{
label_855053:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_855091;
}
else 
{
goto label_855060;
}
}
else 
{
label_855060:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_855069;
}
else 
{
label_855069:; 
goto label_855091;
}
}
else 
{
label_855091:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_855098;
}
}
else 
{
label_855098:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_855107;
}
else 
{
label_855107:; 
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
label_855137:; 
label_855318:; 
if (((int)m_run_st) == 0)
{
goto label_855332;
}
else 
{
if (((int)s_run_st) == 0)
{
label_855332:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_855565;
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
goto label_855520;
}
else 
{
goto label_855461;
}
}
else 
{
label_855461:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_855520;
}
else 
{
goto label_855468;
}
}
else 
{
label_855468:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_855520;
}
else 
{
goto label_855475;
}
}
else 
{
label_855475:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_855520;
}
else 
{
goto label_855482;
}
}
else 
{
label_855482:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_855520;
}
else 
{
goto label_855489;
}
}
else 
{
label_855489:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_855498;
}
else 
{
label_855498:; 
goto label_855520;
}
}
else 
{
label_855520:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_855527;
}
}
else 
{
label_855527:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_855536;
}
else 
{
label_855536:; 
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
goto label_854771;
}
}
}
}
else 
{
label_855565:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_855741;
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
goto label_855692;
}
else 
{
goto label_855633;
}
}
else 
{
label_855633:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_855692;
}
else 
{
goto label_855640;
}
}
else 
{
label_855640:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_855692;
}
else 
{
goto label_855647;
}
}
else 
{
label_855647:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_855692;
}
else 
{
goto label_855654;
}
}
else 
{
label_855654:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_855692;
}
else 
{
goto label_855661;
}
}
else 
{
label_855661:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_855670;
}
else 
{
label_855670:; 
goto label_855692;
}
}
else 
{
label_855692:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_855699;
}
}
else 
{
label_855699:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_855708;
}
else 
{
label_855708:; 
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
goto label_855137;
}
}
}
}
else 
{
label_855741:; 
goto label_855318;
}
}
}
else 
{
}
label_855329:; 
kernel_st = 2;
kernel_st = 3;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_857829;
}
else 
{
goto label_857239;
}
}
else 
{
label_857239:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_857829;
}
else 
{
goto label_857311;
}
}
else 
{
label_857311:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_857829;
}
else 
{
goto label_857379;
}
}
else 
{
label_857379:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_857829;
}
else 
{
goto label_857451;
}
}
else 
{
label_857451:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_857829;
}
else 
{
goto label_857519;
}
}
else 
{
label_857519:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_857609;
}
else 
{
label_857609:; 
goto label_857829;
}
}
else 
{
label_857829:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_858051;
}
else 
{
goto label_857901;
}
}
else 
{
label_857901:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_857991;
}
else 
{
label_857991:; 
goto label_858051;
}
}
else 
{
label_858051:; 
if (((int)m_run_st) == 0)
{
goto label_858171;
}
else 
{
if (((int)s_run_st) == 0)
{
label_858171:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
label_870123:; 
if (((int)m_run_st) == 0)
{
goto label_870137;
}
else 
{
if (((int)s_run_st) == 0)
{
label_870137:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_870148;
}
else 
{
label_870148:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_870325;
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
goto label_870275;
}
else 
{
goto label_870216;
}
}
else 
{
label_870216:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_870275;
}
else 
{
goto label_870223;
}
}
else 
{
label_870223:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_870275;
}
else 
{
goto label_870230;
}
}
else 
{
label_870230:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_870275;
}
else 
{
goto label_870237;
}
}
else 
{
label_870237:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_870275;
}
else 
{
goto label_870244;
}
}
else 
{
label_870244:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_870253;
}
else 
{
label_870253:; 
goto label_870275;
}
}
else 
{
label_870275:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_870282;
}
}
else 
{
label_870282:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_870291;
}
else 
{
label_870291:; 
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
goto label_870123;
}
}
}
}
else 
{
label_870325:; 
goto label_870123;
}
}
}
else 
{
}
goto label_855329;
}
}
}
else 
{
}
goto label_858131;
}
}
}
}
}
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
label_855312:; 
goto label_855318;
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_854782:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_855314;
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
goto label_855257;
}
else 
{
goto label_855198;
}
}
else 
{
label_855198:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_855257;
}
else 
{
goto label_855205;
}
}
else 
{
label_855205:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_855257;
}
else 
{
goto label_855212;
}
}
else 
{
label_855212:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_855257;
}
else 
{
goto label_855219;
}
}
else 
{
label_855219:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_855257;
}
else 
{
goto label_855226;
}
}
else 
{
label_855226:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_855235;
}
else 
{
label_855235:; 
goto label_855257;
}
}
else 
{
label_855257:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_855264;
}
}
else 
{
label_855264:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_855273;
}
else 
{
label_855273:; 
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
goto label_853281;
}
}
}
}
else 
{
label_855314:; 
goto label_854055;
}
}
}
}
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
int __tmp_3 = req_a;
int __tmp_4 = req_d;
int i = __tmp_3;
int v = __tmp_4;
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
goto label_852994;
label_852994:; 
rsp_status = 1;
goto label_853025;
}
}
else 
{
rsp_status = 0;
goto label_853025;
}
}
}
}
}
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
label_853620:; 
label_855960:; 
if (((int)m_run_st) == 0)
{
goto label_855974;
}
else 
{
if (((int)s_run_st) == 0)
{
label_855974:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_855985;
}
else 
{
label_855985:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_856452;
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
goto label_856112;
}
else 
{
goto label_856053;
}
}
else 
{
label_856053:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_856112;
}
else 
{
goto label_856060;
}
}
else 
{
label_856060:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_856112;
}
else 
{
goto label_856067;
}
}
else 
{
label_856067:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_856112;
}
else 
{
goto label_856074;
}
}
else 
{
label_856074:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_856112;
}
else 
{
goto label_856081;
}
}
else 
{
label_856081:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_856090;
}
else 
{
label_856090:; 
goto label_856112;
}
}
else 
{
label_856112:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_856134;
}
else 
{
goto label_856119;
}
}
else 
{
label_856119:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_856128;
}
else 
{
label_856128:; 
goto label_856134;
}
}
else 
{
label_856134:; 
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
 __return_856184 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_856185 = x;
}
rsp_d = __return_856184;
goto label_856187;
rsp_d = __return_856185;
label_856187:; 
rsp_status = 1;
label_856193:; 
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
goto label_856274;
}
else 
{
goto label_856215;
}
}
else 
{
label_856215:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_856274;
}
else 
{
goto label_856222;
}
}
else 
{
label_856222:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_856274;
}
else 
{
goto label_856229;
}
}
else 
{
label_856229:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_856274;
}
else 
{
goto label_856236;
}
}
else 
{
label_856236:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_856274;
}
else 
{
goto label_856243;
}
}
else 
{
label_856243:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_856252;
}
else 
{
label_856252:; 
goto label_856274;
}
}
else 
{
label_856274:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_856296;
}
else 
{
goto label_856281;
}
}
else 
{
label_856281:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_856290;
}
else 
{
label_856290:; 
goto label_856296;
}
}
else 
{
label_856296:; 
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
goto label_856379;
}
else 
{
goto label_856320;
}
}
else 
{
label_856320:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_856379;
}
else 
{
goto label_856327;
}
}
else 
{
label_856327:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_856379;
}
else 
{
goto label_856334;
}
}
else 
{
label_856334:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_856379;
}
else 
{
goto label_856341;
}
}
else 
{
label_856341:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_856379;
}
else 
{
goto label_856348;
}
}
else 
{
label_856348:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_856357;
}
else 
{
label_856357:; 
goto label_856379;
}
}
else 
{
label_856379:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_856386;
}
}
else 
{
label_856386:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_856395;
}
else 
{
label_856395:; 
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
goto label_853281;
}
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
goto label_856162;
label_856162:; 
rsp_status = 1;
goto label_856193;
}
}
else 
{
rsp_status = 0;
goto label_856193;
}
}
}
}
}
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
label_856452:; 
goto label_855960;
}
}
}
else 
{
}
label_855971:; 
kernel_st = 2;
kernel_st = 3;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_857825;
}
else 
{
goto label_857235;
}
}
else 
{
label_857235:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_857825;
}
else 
{
goto label_857315;
}
}
else 
{
label_857315:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_857825;
}
else 
{
goto label_857375;
}
}
else 
{
label_857375:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_857825;
}
else 
{
goto label_857455;
}
}
else 
{
label_857455:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_857825;
}
else 
{
goto label_857515;
}
}
else 
{
label_857515:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_857605;
}
else 
{
label_857605:; 
goto label_857825;
}
}
else 
{
label_857825:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_858055;
}
else 
{
goto label_857905;
}
}
else 
{
label_857905:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_857995;
}
else 
{
label_857995:; 
goto label_858055;
}
}
else 
{
label_858055:; 
if (((int)m_run_st) == 0)
{
goto label_858175;
}
else 
{
if (((int)s_run_st) == 0)
{
label_858175:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
label_866714:; 
if (((int)m_run_st) == 0)
{
goto label_866728;
}
else 
{
if (((int)s_run_st) == 0)
{
label_866728:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_866739;
}
else 
{
label_866739:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_867207;
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
goto label_866866;
}
else 
{
goto label_866807;
}
}
else 
{
label_866807:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_866866;
}
else 
{
goto label_866814;
}
}
else 
{
label_866814:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_866866;
}
else 
{
goto label_866821;
}
}
else 
{
label_866821:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_866866;
}
else 
{
goto label_866828;
}
}
else 
{
label_866828:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_866866;
}
else 
{
goto label_866835;
}
}
else 
{
label_866835:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_866844;
}
else 
{
label_866844:; 
goto label_866866;
}
}
else 
{
label_866866:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_866888;
}
else 
{
goto label_866873;
}
}
else 
{
label_866873:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_866882;
}
else 
{
label_866882:; 
goto label_866888;
}
}
else 
{
label_866888:; 
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
 __return_866938 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_866939 = x;
}
rsp_d = __return_866938;
goto label_866941;
rsp_d = __return_866939;
label_866941:; 
rsp_status = 1;
label_866947:; 
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
goto label_867028;
}
else 
{
goto label_866969;
}
}
else 
{
label_866969:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_867028;
}
else 
{
goto label_866976;
}
}
else 
{
label_866976:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_867028;
}
else 
{
goto label_866983;
}
}
else 
{
label_866983:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_867028;
}
else 
{
goto label_866990;
}
}
else 
{
label_866990:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_867028;
}
else 
{
goto label_866997;
}
}
else 
{
label_866997:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_867006;
}
else 
{
label_867006:; 
goto label_867028;
}
}
else 
{
label_867028:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_867050;
}
else 
{
goto label_867035;
}
}
else 
{
label_867035:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_867044;
}
else 
{
label_867044:; 
goto label_867050;
}
}
else 
{
label_867050:; 
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
goto label_867133;
}
else 
{
goto label_867074;
}
}
else 
{
label_867074:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_867133;
}
else 
{
goto label_867081;
}
}
else 
{
label_867081:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_867133;
}
else 
{
goto label_867088;
}
}
else 
{
label_867088:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_867133;
}
else 
{
goto label_867095;
}
}
else 
{
label_867095:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_867133;
}
else 
{
goto label_867102;
}
}
else 
{
label_867102:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_867111;
}
else 
{
label_867111:; 
goto label_867133;
}
}
else 
{
label_867133:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_867140;
}
}
else 
{
label_867140:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_867149;
}
else 
{
label_867149:; 
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
label_867203:; 
label_867210:; 
if (((int)m_run_st) == 0)
{
goto label_867223;
}
else 
{
label_867223:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_867938;
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
goto label_867411;
}
else 
{
goto label_867352;
}
}
else 
{
label_867352:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_867411;
}
else 
{
goto label_867359;
}
}
else 
{
label_867359:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_867411;
}
else 
{
goto label_867366;
}
}
else 
{
label_867366:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_867411;
}
else 
{
goto label_867373;
}
}
else 
{
label_867373:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_867411;
}
else 
{
goto label_867380;
}
}
else 
{
label_867380:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_867389;
}
else 
{
label_867389:; 
goto label_867411;
}
}
else 
{
label_867411:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_867433;
}
else 
{
goto label_867418;
}
}
else 
{
label_867418:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_867427;
}
else 
{
label_867427:; 
goto label_867433;
}
}
else 
{
label_867433:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_867506;
}
else 
{
goto label_867447;
}
}
else 
{
label_867447:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_867506;
}
else 
{
goto label_867454;
}
}
else 
{
label_867454:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_867506;
}
else 
{
goto label_867461;
}
}
else 
{
label_867461:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_867506;
}
else 
{
goto label_867468;
}
}
else 
{
label_867468:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_867506;
}
else 
{
goto label_867475;
}
}
else 
{
label_867475:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_867484;
}
else 
{
label_867484:; 
goto label_867506;
}
}
else 
{
label_867506:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_867528;
}
else 
{
goto label_867513;
}
}
else 
{
label_867513:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_867522;
}
else 
{
label_867522:; 
goto label_867528;
}
}
else 
{
label_867528:; 
c_m_ev = 2;
if ((req_a___0 + 50) == rsp_d___0)
{
a = a + 1;
goto label_867545;
}
else 
{
{
__VERIFIER_error();
}
a = a + 1;
label_867545:; 
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
goto label_867643;
}
else 
{
goto label_867584;
}
}
else 
{
label_867584:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_867643;
}
else 
{
goto label_867591;
}
}
else 
{
label_867591:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_867643;
}
else 
{
goto label_867598;
}
}
else 
{
label_867598:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_867643;
}
else 
{
goto label_867605;
}
}
else 
{
label_867605:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_867643;
}
else 
{
goto label_867612;
}
}
else 
{
label_867612:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_867621;
}
else 
{
label_867621:; 
goto label_867643;
}
}
else 
{
label_867643:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_867665;
}
else 
{
goto label_867650;
}
}
else 
{
label_867650:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_867659;
}
else 
{
label_867659:; 
goto label_867665;
}
}
else 
{
label_867665:; 
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
goto label_867745;
}
else 
{
goto label_867686;
}
}
else 
{
label_867686:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_867745;
}
else 
{
goto label_867693;
}
}
else 
{
label_867693:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_867745;
}
else 
{
goto label_867700;
}
}
else 
{
label_867700:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_867745;
}
else 
{
goto label_867707;
}
}
else 
{
label_867707:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_867745;
}
else 
{
goto label_867714;
}
}
else 
{
label_867714:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_867723;
}
else 
{
label_867723:; 
goto label_867745;
}
}
else 
{
label_867745:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_867767;
}
else 
{
goto label_867752;
}
}
else 
{
label_867752:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_867761;
}
else 
{
label_867761:; 
goto label_867767;
}
}
else 
{
label_867767:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_867840;
}
else 
{
goto label_867781;
}
}
else 
{
label_867781:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_867840;
}
else 
{
goto label_867788;
}
}
else 
{
label_867788:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_867840;
}
else 
{
goto label_867795;
}
}
else 
{
label_867795:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_867840;
}
else 
{
goto label_867802;
}
}
else 
{
label_867802:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_867840;
}
else 
{
goto label_867809;
}
}
else 
{
label_867809:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_867818;
}
else 
{
label_867818:; 
goto label_867840;
}
}
else 
{
label_867840:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_867847;
}
}
else 
{
label_867847:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_867856;
}
else 
{
label_867856:; 
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
goto label_868934;
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
goto label_868255;
}
else 
{
goto label_868196;
}
}
else 
{
label_868196:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_868255;
}
else 
{
goto label_868203;
}
}
else 
{
label_868203:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_868255;
}
else 
{
goto label_868210;
}
}
else 
{
label_868210:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_868255;
}
else 
{
goto label_868217;
}
}
else 
{
label_868217:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_868255;
}
else 
{
goto label_868224;
}
}
else 
{
label_868224:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_868233;
}
else 
{
label_868233:; 
goto label_868255;
}
}
else 
{
label_868255:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_868277;
}
else 
{
goto label_868262;
}
}
else 
{
label_868262:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_868271;
}
else 
{
label_868271:; 
goto label_868277;
}
}
else 
{
label_868277:; 
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
 __return_868327 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_868328 = x;
}
rsp_d = __return_868327;
goto label_868330;
rsp_d = __return_868328;
label_868330:; 
rsp_status = 1;
label_868336:; 
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
goto label_868417;
}
else 
{
goto label_868358;
}
}
else 
{
label_868358:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_868417;
}
else 
{
goto label_868365;
}
}
else 
{
label_868365:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_868417;
}
else 
{
goto label_868372;
}
}
else 
{
label_868372:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_868417;
}
else 
{
goto label_868379;
}
}
else 
{
label_868379:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_868417;
}
else 
{
goto label_868386;
}
}
else 
{
label_868386:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_868395;
}
else 
{
label_868395:; 
goto label_868417;
}
}
else 
{
label_868417:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_868439;
}
else 
{
goto label_868424;
}
}
else 
{
label_868424:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_868433;
}
else 
{
label_868433:; 
goto label_868439;
}
}
else 
{
label_868439:; 
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
goto label_868522;
}
else 
{
goto label_868463;
}
}
else 
{
label_868463:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_868522;
}
else 
{
goto label_868470;
}
}
else 
{
label_868470:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_868522;
}
else 
{
goto label_868477;
}
}
else 
{
label_868477:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_868522;
}
else 
{
goto label_868484;
}
}
else 
{
label_868484:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_868522;
}
else 
{
goto label_868491;
}
}
else 
{
label_868491:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_868500;
}
else 
{
label_868500:; 
goto label_868522;
}
}
else 
{
label_868522:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_868529;
}
}
else 
{
label_868529:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_868538;
}
else 
{
label_868538:; 
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
goto label_867203;
}
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
goto label_868305;
label_868305:; 
rsp_status = 1;
goto label_868336;
}
}
else 
{
rsp_status = 0;
goto label_868336;
}
}
}
}
}
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
label_868934:; 
goto label_866714;
}
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
goto label_868932;
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
goto label_868089;
}
else 
{
goto label_868030;
}
}
else 
{
label_868030:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_868089;
}
else 
{
goto label_868037;
}
}
else 
{
label_868037:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_868089;
}
else 
{
goto label_868044;
}
}
else 
{
label_868044:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_868089;
}
else 
{
goto label_868051;
}
}
else 
{
label_868051:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_868089;
}
else 
{
goto label_868058;
}
}
else 
{
label_868058:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_868067;
}
else 
{
label_868067:; 
goto label_868089;
}
}
else 
{
label_868089:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_868096;
}
}
else 
{
label_868096:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_868105;
}
else 
{
label_868105:; 
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
label_868135:; 
label_869369:; 
if (((int)m_run_st) == 0)
{
goto label_869383;
}
else 
{
if (((int)s_run_st) == 0)
{
label_869383:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_869394;
}
else 
{
label_869394:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_869570;
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
goto label_869521;
}
else 
{
goto label_869462;
}
}
else 
{
label_869462:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_869521;
}
else 
{
goto label_869469;
}
}
else 
{
label_869469:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_869521;
}
else 
{
goto label_869476;
}
}
else 
{
label_869476:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_869521;
}
else 
{
goto label_869483;
}
}
else 
{
label_869483:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_869521;
}
else 
{
goto label_869490;
}
}
else 
{
label_869490:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_869499;
}
else 
{
label_869499:; 
goto label_869521;
}
}
else 
{
label_869521:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_869528;
}
}
else 
{
label_869528:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_869537;
}
else 
{
label_869537:; 
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
goto label_868135;
}
}
}
}
else 
{
label_869570:; 
goto label_869369;
}
}
}
else 
{
}
goto label_855756;
}
}
}
}
}
else 
{
label_868932:; 
goto label_869369;
}
}
else 
{
}
label_867926:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_868936;
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
goto label_868712;
}
else 
{
goto label_868653;
}
}
else 
{
label_868653:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_868712;
}
else 
{
goto label_868660;
}
}
else 
{
label_868660:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_868712;
}
else 
{
goto label_868667;
}
}
else 
{
label_868667:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_868712;
}
else 
{
goto label_868674;
}
}
else 
{
label_868674:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_868712;
}
else 
{
goto label_868681;
}
}
else 
{
label_868681:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_868690;
}
else 
{
label_868690:; 
goto label_868712;
}
}
else 
{
label_868712:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_868719;
}
}
else 
{
label_868719:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_868728;
}
else 
{
label_868728:; 
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
label_868758:; 
label_868942:; 
if (((int)m_run_st) == 0)
{
goto label_868956;
}
else 
{
if (((int)s_run_st) == 0)
{
label_868956:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_869189;
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
goto label_869144;
}
else 
{
goto label_869085;
}
}
else 
{
label_869085:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_869144;
}
else 
{
goto label_869092;
}
}
else 
{
label_869092:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_869144;
}
else 
{
goto label_869099;
}
}
else 
{
label_869099:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_869144;
}
else 
{
goto label_869106;
}
}
else 
{
label_869106:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_869144;
}
else 
{
goto label_869113;
}
}
else 
{
label_869113:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_869122;
}
else 
{
label_869122:; 
goto label_869144;
}
}
else 
{
label_869144:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_869151;
}
}
else 
{
label_869151:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_869160;
}
else 
{
label_869160:; 
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
goto label_867926;
}
}
}
}
else 
{
label_869189:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_869365;
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
goto label_869316;
}
else 
{
goto label_869257;
}
}
else 
{
label_869257:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_869316;
}
else 
{
goto label_869264;
}
}
else 
{
label_869264:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_869316;
}
else 
{
goto label_869271;
}
}
else 
{
label_869271:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_869316;
}
else 
{
goto label_869278;
}
}
else 
{
label_869278:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_869316;
}
else 
{
goto label_869285;
}
}
else 
{
label_869285:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_869294;
}
else 
{
label_869294:; 
goto label_869316;
}
}
else 
{
label_869316:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_869323;
}
}
else 
{
label_869323:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_869332;
}
else 
{
label_869332:; 
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
goto label_868758;
}
}
}
}
else 
{
label_869365:; 
goto label_868942;
}
}
}
else 
{
}
goto label_855329;
}
}
}
}
}
else 
{
label_868936:; 
goto label_868942;
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_867938:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_868938;
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
goto label_868878;
}
else 
{
goto label_868819;
}
}
else 
{
label_868819:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_868878;
}
else 
{
goto label_868826;
}
}
else 
{
label_868826:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_868878;
}
else 
{
goto label_868833;
}
}
else 
{
label_868833:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_868878;
}
else 
{
goto label_868840;
}
}
else 
{
label_868840:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_868878;
}
else 
{
goto label_868847;
}
}
else 
{
label_868847:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_868856;
}
else 
{
label_868856:; 
goto label_868878;
}
}
else 
{
label_868878:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_868885;
}
}
else 
{
label_868885:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_868894;
}
else 
{
label_868894:; 
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
goto label_867203;
}
}
}
}
else 
{
label_868938:; 
goto label_867210;
}
}
}
}
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
int __tmp_12 = req_a;
int __tmp_13 = req_d;
int i = __tmp_12;
int v = __tmp_13;
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
goto label_866916;
label_866916:; 
rsp_status = 1;
goto label_866947;
}
}
else 
{
rsp_status = 0;
goto label_866947;
}
}
}
}
}
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
label_867207:; 
goto label_866714;
}
}
}
else 
{
}
goto label_855971;
}
}
}
else 
{
}
goto label_858135;
}
}
}
}
}
}
}
}
}
}
}
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
}
label_852787:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_853622;
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
goto label_853401;
}
else 
{
goto label_853342;
}
}
else 
{
label_853342:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_853401;
}
else 
{
goto label_853349;
}
}
else 
{
label_853349:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_853401;
}
else 
{
goto label_853356;
}
}
else 
{
label_853356:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_853401;
}
else 
{
goto label_853363;
}
}
else 
{
label_853363:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_853401;
}
else 
{
goto label_853370;
}
}
else 
{
label_853370:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_853379;
}
else 
{
label_853379:; 
goto label_853401;
}
}
else 
{
label_853401:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_853408;
}
}
else 
{
label_853408:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_853417;
}
else 
{
label_853417:; 
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
label_853447:; 
label_853628:; 
if (((int)m_run_st) == 0)
{
goto label_853642;
}
else 
{
if (((int)s_run_st) == 0)
{
label_853642:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_853875;
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
goto label_853830;
}
else 
{
goto label_853771;
}
}
else 
{
label_853771:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_853830;
}
else 
{
goto label_853778;
}
}
else 
{
label_853778:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_853830;
}
else 
{
goto label_853785;
}
}
else 
{
label_853785:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_853830;
}
else 
{
goto label_853792;
}
}
else 
{
label_853792:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_853830;
}
else 
{
goto label_853799;
}
}
else 
{
label_853799:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_853808;
}
else 
{
label_853808:; 
goto label_853830;
}
}
else 
{
label_853830:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_853837;
}
}
else 
{
label_853837:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_853846;
}
else 
{
label_853846:; 
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
goto label_852787;
}
}
}
}
else 
{
label_853875:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_854051;
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
goto label_854002;
}
else 
{
goto label_853943;
}
}
else 
{
label_853943:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_854002;
}
else 
{
goto label_853950;
}
}
else 
{
label_853950:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_854002;
}
else 
{
goto label_853957;
}
}
else 
{
label_853957:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_854002;
}
else 
{
goto label_853964;
}
}
else 
{
label_853964:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_854002;
}
else 
{
goto label_853971;
}
}
else 
{
label_853971:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_853980;
}
else 
{
label_853980:; 
goto label_854002;
}
}
else 
{
label_854002:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_854009;
}
}
else 
{
label_854009:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_854018;
}
else 
{
label_854018:; 
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
goto label_853447;
}
}
}
}
else 
{
label_854051:; 
goto label_853628;
}
}
}
else 
{
}
label_853639:; 
kernel_st = 2;
kernel_st = 3;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_857831;
}
else 
{
goto label_857241;
}
}
else 
{
label_857241:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_857831;
}
else 
{
goto label_857309;
}
}
else 
{
label_857309:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_857831;
}
else 
{
goto label_857381;
}
}
else 
{
label_857381:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_857831;
}
else 
{
goto label_857449;
}
}
else 
{
label_857449:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_857831;
}
else 
{
goto label_857521;
}
}
else 
{
label_857521:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_857611;
}
else 
{
label_857611:; 
goto label_857831;
}
}
else 
{
label_857831:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_858049;
}
else 
{
goto label_857899;
}
}
else 
{
label_857899:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_857989;
}
else 
{
label_857989:; 
goto label_858049;
}
}
else 
{
label_858049:; 
if (((int)m_run_st) == 0)
{
goto label_858169;
}
else 
{
if (((int)s_run_st) == 0)
{
label_858169:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
label_870342:; 
if (((int)m_run_st) == 0)
{
goto label_870356;
}
else 
{
if (((int)s_run_st) == 0)
{
label_870356:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_870367;
}
else 
{
label_870367:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_870544;
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
goto label_870494;
}
else 
{
goto label_870435;
}
}
else 
{
label_870435:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_870494;
}
else 
{
goto label_870442;
}
}
else 
{
label_870442:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_870494;
}
else 
{
goto label_870449;
}
}
else 
{
label_870449:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_870494;
}
else 
{
goto label_870456;
}
}
else 
{
label_870456:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_870494;
}
else 
{
goto label_870463;
}
}
else 
{
label_870463:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_870472;
}
else 
{
label_870472:; 
goto label_870494;
}
}
else 
{
label_870494:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_870501;
}
}
else 
{
label_870501:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_870510;
}
else 
{
label_870510:; 
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
goto label_870342;
}
}
}
}
else 
{
label_870544:; 
goto label_870342;
}
}
}
else 
{
}
goto label_853639;
}
}
}
else 
{
}
goto label_858131;
}
}
}
}
}
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
label_853622:; 
goto label_853628;
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_852801:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_853624;
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
goto label_853567;
}
else 
{
goto label_853508;
}
}
else 
{
label_853508:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_853567;
}
else 
{
goto label_853515;
}
}
else 
{
label_853515:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_853567;
}
else 
{
goto label_853522;
}
}
else 
{
label_853522:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_853567;
}
else 
{
goto label_853529;
}
}
else 
{
label_853529:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_853567;
}
else 
{
goto label_853536;
}
}
else 
{
label_853536:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_853545;
}
else 
{
label_853545:; 
goto label_853567;
}
}
else 
{
label_853567:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_853574;
}
}
else 
{
label_853574:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_853583;
}
else 
{
label_853583:; 
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
goto label_849519;
}
}
}
}
else 
{
label_853624:; 
goto label_851724;
}
}
}
}
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
goto label_849232;
label_849232:; 
rsp_status = 1;
goto label_849263;
}
}
else 
{
rsp_status = 0;
goto label_849263;
}
}
}
}
}
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
label_849695:; 
label_856460:; 
if (((int)m_run_st) == 0)
{
goto label_856474;
}
else 
{
if (((int)s_run_st) == 0)
{
label_856474:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_856485;
}
else 
{
label_856485:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_856952;
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
goto label_856530;
}
else 
{
label_856530:; 
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
goto label_856612;
}
else 
{
goto label_856553;
}
}
else 
{
label_856553:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_856612;
}
else 
{
goto label_856560;
}
}
else 
{
label_856560:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_856612;
}
else 
{
goto label_856567;
}
}
else 
{
label_856567:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_856612;
}
else 
{
goto label_856574;
}
}
else 
{
label_856574:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_856612;
}
else 
{
goto label_856581;
}
}
else 
{
label_856581:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_856590;
}
else 
{
label_856590:; 
goto label_856612;
}
}
else 
{
label_856612:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_856634;
}
else 
{
goto label_856619;
}
}
else 
{
label_856619:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_856628;
}
else 
{
label_856628:; 
goto label_856634;
}
}
else 
{
label_856634:; 
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
 __return_856684 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_856685 = x;
}
rsp_d = __return_856684;
goto label_856687;
rsp_d = __return_856685;
label_856687:; 
rsp_status = 1;
label_856693:; 
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
goto label_856774;
}
else 
{
goto label_856715;
}
}
else 
{
label_856715:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_856774;
}
else 
{
goto label_856722;
}
}
else 
{
label_856722:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_856774;
}
else 
{
goto label_856729;
}
}
else 
{
label_856729:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_856774;
}
else 
{
goto label_856736;
}
}
else 
{
label_856736:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_856774;
}
else 
{
goto label_856743;
}
}
else 
{
label_856743:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_856752;
}
else 
{
label_856752:; 
goto label_856774;
}
}
else 
{
label_856774:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_856796;
}
else 
{
goto label_856781;
}
}
else 
{
label_856781:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_856790;
}
else 
{
label_856790:; 
goto label_856796;
}
}
else 
{
label_856796:; 
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
goto label_856879;
}
else 
{
goto label_856820;
}
}
else 
{
label_856820:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_856879;
}
else 
{
goto label_856827;
}
}
else 
{
label_856827:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_856879;
}
else 
{
goto label_856834;
}
}
else 
{
label_856834:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_856879;
}
else 
{
goto label_856841;
}
}
else 
{
label_856841:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_856879;
}
else 
{
goto label_856848;
}
}
else 
{
label_856848:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_856857;
}
else 
{
label_856857:; 
goto label_856879;
}
}
else 
{
label_856879:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_856886;
}
}
else 
{
label_856886:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_856895;
}
else 
{
label_856895:; 
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
goto label_849519;
}
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
int __tmp_17 = req_a;
int __tmp_18 = req_d;
int i = __tmp_17;
int v = __tmp_18;
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
goto label_856662;
label_856662:; 
rsp_status = 1;
goto label_856693;
}
}
else 
{
rsp_status = 0;
goto label_856693;
}
}
}
}
}
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
label_856952:; 
goto label_856460;
}
}
}
else 
{
}
label_856471:; 
kernel_st = 2;
kernel_st = 3;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_857823;
}
else 
{
goto label_857233;
}
}
else 
{
label_857233:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_857823;
}
else 
{
goto label_857317;
}
}
else 
{
label_857317:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_857823;
}
else 
{
goto label_857373;
}
}
else 
{
label_857373:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_857823;
}
else 
{
goto label_857457;
}
}
else 
{
label_857457:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_857823;
}
else 
{
goto label_857513;
}
}
else 
{
label_857513:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_857603;
}
else 
{
label_857603:; 
goto label_857823;
}
}
else 
{
label_857823:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_858057;
}
else 
{
goto label_857907;
}
}
else 
{
label_857907:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_857997;
}
else 
{
label_857997:; 
goto label_858057;
}
}
else 
{
label_858057:; 
if (((int)m_run_st) == 0)
{
goto label_858177;
}
else 
{
if (((int)s_run_st) == 0)
{
label_858177:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
label_859229:; 
if (((int)m_run_st) == 0)
{
goto label_859243;
}
else 
{
if (((int)s_run_st) == 0)
{
label_859243:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_859570;
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
goto label_859431;
}
else 
{
goto label_859372;
}
}
else 
{
label_859372:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_859431;
}
else 
{
goto label_859379;
}
}
else 
{
label_859379:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_859431;
}
else 
{
goto label_859386;
}
}
else 
{
label_859386:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_859431;
}
else 
{
goto label_859393;
}
}
else 
{
label_859393:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_859431;
}
else 
{
goto label_859400;
}
}
else 
{
label_859400:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_859409;
}
else 
{
label_859409:; 
goto label_859431;
}
}
else 
{
label_859431:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_859453;
}
else 
{
goto label_859438;
}
}
else 
{
label_859438:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_859447;
}
else 
{
label_859447:; 
goto label_859453;
}
}
else 
{
label_859453:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_859526;
}
else 
{
goto label_859467;
}
}
else 
{
label_859467:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_859526;
}
else 
{
goto label_859474;
}
}
else 
{
label_859474:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_859526;
}
else 
{
goto label_859481;
}
}
else 
{
label_859481:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_859526;
}
else 
{
goto label_859488;
}
}
else 
{
label_859488:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_859526;
}
else 
{
goto label_859495;
}
}
else 
{
label_859495:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_859504;
}
else 
{
label_859504:; 
goto label_859526;
}
}
else 
{
label_859526:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_859533;
}
}
else 
{
label_859533:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_859542;
}
else 
{
label_859542:; 
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
goto label_860504;
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
goto label_859623;
}
else 
{
label_859623:; 
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
goto label_859705;
}
else 
{
goto label_859646;
}
}
else 
{
label_859646:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_859705;
}
else 
{
goto label_859653;
}
}
else 
{
label_859653:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_859705;
}
else 
{
goto label_859660;
}
}
else 
{
label_859660:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_859705;
}
else 
{
goto label_859667;
}
}
else 
{
label_859667:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_859705;
}
else 
{
goto label_859674;
}
}
else 
{
label_859674:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_859683;
}
else 
{
label_859683:; 
goto label_859705;
}
}
else 
{
label_859705:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_859727;
}
else 
{
goto label_859712;
}
}
else 
{
label_859712:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_859721;
}
else 
{
label_859721:; 
goto label_859727;
}
}
else 
{
label_859727:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_19 = req_a;
int i = __tmp_19;
int x;
if (i == 0)
{
x = s_memory0;
 __return_859777 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_859778 = x;
}
rsp_d = __return_859777;
goto label_859780;
rsp_d = __return_859778;
label_859780:; 
rsp_status = 1;
label_859786:; 
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
goto label_859867;
}
else 
{
goto label_859808;
}
}
else 
{
label_859808:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_859867;
}
else 
{
goto label_859815;
}
}
else 
{
label_859815:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_859867;
}
else 
{
goto label_859822;
}
}
else 
{
label_859822:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_859867;
}
else 
{
goto label_859829;
}
}
else 
{
label_859829:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_859867;
}
else 
{
goto label_859836;
}
}
else 
{
label_859836:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_859845;
}
else 
{
label_859845:; 
goto label_859867;
}
}
else 
{
label_859867:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_859889;
}
else 
{
goto label_859874;
}
}
else 
{
label_859874:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_859883;
}
else 
{
label_859883:; 
goto label_859889;
}
}
else 
{
label_859889:; 
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
goto label_859972;
}
else 
{
goto label_859913;
}
}
else 
{
label_859913:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_859972;
}
else 
{
goto label_859920;
}
}
else 
{
label_859920:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_859972;
}
else 
{
goto label_859927;
}
}
else 
{
label_859927:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_859972;
}
else 
{
goto label_859934;
}
}
else 
{
label_859934:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_859972;
}
else 
{
goto label_859941;
}
}
else 
{
label_859941:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_859950;
}
else 
{
label_859950:; 
goto label_859972;
}
}
else 
{
label_859972:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_859979;
}
}
else 
{
label_859979:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_859988;
}
else 
{
label_859988:; 
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
label_860042:; 
label_860509:; 
if (((int)m_run_st) == 0)
{
goto label_860522;
}
else 
{
label_860522:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_861587;
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
goto label_860710;
}
else 
{
goto label_860651;
}
}
else 
{
label_860651:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_860710;
}
else 
{
goto label_860658;
}
}
else 
{
label_860658:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_860710;
}
else 
{
goto label_860665;
}
}
else 
{
label_860665:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_860710;
}
else 
{
goto label_860672;
}
}
else 
{
label_860672:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_860710;
}
else 
{
goto label_860679;
}
}
else 
{
label_860679:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_860688;
}
else 
{
label_860688:; 
goto label_860710;
}
}
else 
{
label_860710:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_860732;
}
else 
{
goto label_860717;
}
}
else 
{
label_860717:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_860726;
}
else 
{
label_860726:; 
goto label_860732;
}
}
else 
{
label_860732:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_860805;
}
else 
{
goto label_860746;
}
}
else 
{
label_860746:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_860805;
}
else 
{
goto label_860753;
}
}
else 
{
label_860753:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_860805;
}
else 
{
goto label_860760;
}
}
else 
{
label_860760:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_860805;
}
else 
{
goto label_860767;
}
}
else 
{
label_860767:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_860805;
}
else 
{
goto label_860774;
}
}
else 
{
label_860774:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_860783;
}
else 
{
label_860783:; 
goto label_860805;
}
}
else 
{
label_860805:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_860827;
}
else 
{
goto label_860812;
}
}
else 
{
label_860812:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_860821;
}
else 
{
label_860821:; 
goto label_860827;
}
}
else 
{
label_860827:; 
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
goto label_861292;
}
else 
{
goto label_861233;
}
}
else 
{
label_861233:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_861292;
}
else 
{
goto label_861240;
}
}
else 
{
label_861240:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_861292;
}
else 
{
goto label_861247;
}
}
else 
{
label_861247:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_861292;
}
else 
{
goto label_861254;
}
}
else 
{
label_861254:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_861292;
}
else 
{
goto label_861261;
}
}
else 
{
label_861261:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_861270;
}
else 
{
label_861270:; 
goto label_861292;
}
}
else 
{
label_861292:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_861314;
}
else 
{
goto label_861299;
}
}
else 
{
label_861299:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_861308;
}
else 
{
label_861308:; 
goto label_861314;
}
}
else 
{
label_861314:; 
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
goto label_861394;
}
else 
{
goto label_861335;
}
}
else 
{
label_861335:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_861394;
}
else 
{
goto label_861342;
}
}
else 
{
label_861342:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_861394;
}
else 
{
goto label_861349;
}
}
else 
{
label_861349:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_861394;
}
else 
{
goto label_861356;
}
}
else 
{
label_861356:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_861394;
}
else 
{
goto label_861363;
}
}
else 
{
label_861363:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_861372;
}
else 
{
label_861372:; 
goto label_861394;
}
}
else 
{
label_861394:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_861416;
}
else 
{
goto label_861401;
}
}
else 
{
label_861401:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_861410;
}
else 
{
label_861410:; 
goto label_861416;
}
}
else 
{
label_861416:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_861489;
}
else 
{
goto label_861430;
}
}
else 
{
label_861430:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_861489;
}
else 
{
goto label_861437;
}
}
else 
{
label_861437:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_861489;
}
else 
{
goto label_861444;
}
}
else 
{
label_861444:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_861489;
}
else 
{
goto label_861451;
}
}
else 
{
label_861451:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_861489;
}
else 
{
goto label_861458;
}
}
else 
{
label_861458:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_861467;
}
else 
{
label_861467:; 
goto label_861489;
}
}
else 
{
label_861489:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_861496;
}
}
else 
{
label_861496:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_861505;
}
else 
{
label_861505:; 
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
goto label_862872;
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
goto label_861738;
}
else 
{
goto label_861679;
}
}
else 
{
label_861679:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_861738;
}
else 
{
goto label_861686;
}
}
else 
{
label_861686:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_861738;
}
else 
{
goto label_861693;
}
}
else 
{
label_861693:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_861738;
}
else 
{
goto label_861700;
}
}
else 
{
label_861700:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_861738;
}
else 
{
goto label_861707;
}
}
else 
{
label_861707:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_861716;
}
else 
{
label_861716:; 
goto label_861738;
}
}
else 
{
label_861738:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_861760;
}
else 
{
goto label_861745;
}
}
else 
{
label_861745:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_861754;
}
else 
{
label_861754:; 
goto label_861760;
}
}
else 
{
label_861760:; 
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
 __return_861810 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_861811 = x;
}
rsp_d = __return_861810;
goto label_861813;
rsp_d = __return_861811;
label_861813:; 
rsp_status = 1;
label_861819:; 
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
goto label_861900;
}
else 
{
goto label_861841;
}
}
else 
{
label_861841:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_861900;
}
else 
{
goto label_861848;
}
}
else 
{
label_861848:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_861900;
}
else 
{
goto label_861855;
}
}
else 
{
label_861855:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_861900;
}
else 
{
goto label_861862;
}
}
else 
{
label_861862:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_861900;
}
else 
{
goto label_861869;
}
}
else 
{
label_861869:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_861878;
}
else 
{
label_861878:; 
goto label_861900;
}
}
else 
{
label_861900:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_861922;
}
else 
{
goto label_861907;
}
}
else 
{
label_861907:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_861916;
}
else 
{
label_861916:; 
goto label_861922;
}
}
else 
{
label_861922:; 
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
goto label_862005;
}
else 
{
goto label_861946;
}
}
else 
{
label_861946:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_862005;
}
else 
{
goto label_861953;
}
}
else 
{
label_861953:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_862005;
}
else 
{
goto label_861960;
}
}
else 
{
label_861960:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_862005;
}
else 
{
goto label_861967;
}
}
else 
{
label_861967:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_862005;
}
else 
{
goto label_861974;
}
}
else 
{
label_861974:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_861983;
}
else 
{
label_861983:; 
goto label_862005;
}
}
else 
{
label_862005:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_862012;
}
}
else 
{
label_862012:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_862021;
}
else 
{
label_862021:; 
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
goto label_860042;
}
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
goto label_861788;
label_861788:; 
rsp_status = 1;
goto label_861819;
}
}
else 
{
rsp_status = 0;
goto label_861819;
}
}
}
}
}
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
label_862872:; 
label_865710:; 
if (((int)m_run_st) == 0)
{
goto label_865724;
}
else 
{
if (((int)s_run_st) == 0)
{
label_865724:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_865735;
}
else 
{
label_865735:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_866202;
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
goto label_865862;
}
else 
{
goto label_865803;
}
}
else 
{
label_865803:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_865862;
}
else 
{
goto label_865810;
}
}
else 
{
label_865810:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_865862;
}
else 
{
goto label_865817;
}
}
else 
{
label_865817:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_865862;
}
else 
{
goto label_865824;
}
}
else 
{
label_865824:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_865862;
}
else 
{
goto label_865831;
}
}
else 
{
label_865831:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_865840;
}
else 
{
label_865840:; 
goto label_865862;
}
}
else 
{
label_865862:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_865884;
}
else 
{
goto label_865869;
}
}
else 
{
label_865869:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_865878;
}
else 
{
label_865878:; 
goto label_865884;
}
}
else 
{
label_865884:; 
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
 __return_865934 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_865935 = x;
}
rsp_d = __return_865934;
goto label_865937;
rsp_d = __return_865935;
label_865937:; 
rsp_status = 1;
label_865943:; 
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
goto label_866024;
}
else 
{
goto label_865965;
}
}
else 
{
label_865965:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_866024;
}
else 
{
goto label_865972;
}
}
else 
{
label_865972:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_866024;
}
else 
{
goto label_865979;
}
}
else 
{
label_865979:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_866024;
}
else 
{
goto label_865986;
}
}
else 
{
label_865986:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_866024;
}
else 
{
goto label_865993;
}
}
else 
{
label_865993:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_866002;
}
else 
{
label_866002:; 
goto label_866024;
}
}
else 
{
label_866024:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_866046;
}
else 
{
goto label_866031;
}
}
else 
{
label_866031:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_866040;
}
else 
{
label_866040:; 
goto label_866046;
}
}
else 
{
label_866046:; 
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
goto label_866129;
}
else 
{
goto label_866070;
}
}
else 
{
label_866070:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_866129;
}
else 
{
goto label_866077;
}
}
else 
{
label_866077:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_866129;
}
else 
{
goto label_866084;
}
}
else 
{
label_866084:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_866129;
}
else 
{
goto label_866091;
}
}
else 
{
label_866091:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_866129;
}
else 
{
goto label_866098;
}
}
else 
{
label_866098:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_866107;
}
else 
{
label_866107:; 
goto label_866129;
}
}
else 
{
label_866129:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_866136;
}
}
else 
{
label_866136:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_866145;
}
else 
{
label_866145:; 
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
goto label_860042;
}
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
goto label_865912;
label_865912:; 
rsp_status = 1;
goto label_865943;
}
}
else 
{
rsp_status = 0;
goto label_865943;
}
}
}
}
}
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
label_866202:; 
goto label_865710;
}
}
}
else 
{
}
goto label_851034;
}
}
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
if (a < 1)
{
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
goto label_860938;
}
else 
{
goto label_860879;
}
}
else 
{
label_860879:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_860938;
}
else 
{
goto label_860886;
}
}
else 
{
label_860886:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_860938;
}
else 
{
goto label_860893;
}
}
else 
{
label_860893:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_860938;
}
else 
{
goto label_860900;
}
}
else 
{
label_860900:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_860938;
}
else 
{
goto label_860907;
}
}
else 
{
label_860907:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_860916;
}
else 
{
label_860916:; 
goto label_860938;
}
}
else 
{
label_860938:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_860960;
}
else 
{
goto label_860945;
}
}
else 
{
label_860945:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_860954;
}
else 
{
label_860954:; 
goto label_860960;
}
}
else 
{
label_860960:; 
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
goto label_861040;
}
else 
{
goto label_860981;
}
}
else 
{
label_860981:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_861040;
}
else 
{
goto label_860988;
}
}
else 
{
label_860988:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_861040;
}
else 
{
goto label_860995;
}
}
else 
{
label_860995:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_861040;
}
else 
{
goto label_861002;
}
}
else 
{
label_861002:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_861040;
}
else 
{
goto label_861009;
}
}
else 
{
label_861009:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_861018;
}
else 
{
label_861018:; 
goto label_861040;
}
}
else 
{
label_861040:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_861062;
}
else 
{
goto label_861047;
}
}
else 
{
label_861047:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_861056;
}
else 
{
label_861056:; 
goto label_861062;
}
}
else 
{
label_861062:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_861135;
}
else 
{
goto label_861076;
}
}
else 
{
label_861076:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_861135;
}
else 
{
goto label_861083;
}
}
else 
{
label_861083:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_861135;
}
else 
{
goto label_861090;
}
}
else 
{
label_861090:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_861135;
}
else 
{
goto label_861097;
}
}
else 
{
label_861097:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_861135;
}
else 
{
goto label_861104;
}
}
else 
{
label_861104:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_861113;
}
else 
{
label_861113:; 
goto label_861135;
}
}
else 
{
label_861135:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_861142;
}
}
else 
{
label_861142:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_861151;
}
else 
{
label_861151:; 
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
label_861574:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_862874;
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
goto label_862195;
}
else 
{
goto label_862136;
}
}
else 
{
label_862136:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_862195;
}
else 
{
goto label_862143;
}
}
else 
{
label_862143:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_862195;
}
else 
{
goto label_862150;
}
}
else 
{
label_862150:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_862195;
}
else 
{
goto label_862157;
}
}
else 
{
label_862157:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_862195;
}
else 
{
goto label_862164;
}
}
else 
{
label_862164:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_862173;
}
else 
{
label_862173:; 
goto label_862195;
}
}
else 
{
label_862195:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_862217;
}
else 
{
goto label_862202;
}
}
else 
{
label_862202:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_862211;
}
else 
{
label_862211:; 
goto label_862217;
}
}
else 
{
label_862217:; 
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
 __return_862267 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_862268 = x;
}
rsp_d = __return_862267;
goto label_862270;
rsp_d = __return_862268;
label_862270:; 
rsp_status = 1;
label_862276:; 
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
goto label_862357;
}
else 
{
goto label_862298;
}
}
else 
{
label_862298:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_862357;
}
else 
{
goto label_862305;
}
}
else 
{
label_862305:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_862357;
}
else 
{
goto label_862312;
}
}
else 
{
label_862312:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_862357;
}
else 
{
goto label_862319;
}
}
else 
{
label_862319:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_862357;
}
else 
{
goto label_862326;
}
}
else 
{
label_862326:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_862335;
}
else 
{
label_862335:; 
goto label_862357;
}
}
else 
{
label_862357:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_862379;
}
else 
{
goto label_862364;
}
}
else 
{
label_862364:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_862373;
}
else 
{
label_862373:; 
goto label_862379;
}
}
else 
{
label_862379:; 
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
goto label_862462;
}
else 
{
goto label_862403;
}
}
else 
{
label_862403:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_862462;
}
else 
{
goto label_862410;
}
}
else 
{
label_862410:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_862462;
}
else 
{
goto label_862417;
}
}
else 
{
label_862417:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_862462;
}
else 
{
goto label_862424;
}
}
else 
{
label_862424:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_862462;
}
else 
{
goto label_862431;
}
}
else 
{
label_862431:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_862440;
}
else 
{
label_862440:; 
goto label_862462;
}
}
else 
{
label_862462:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_862469;
}
}
else 
{
label_862469:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_862478;
}
else 
{
label_862478:; 
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
label_862532:; 
label_863309:; 
if (((int)m_run_st) == 0)
{
goto label_863322;
}
else 
{
label_863322:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_864036;
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
goto label_863510;
}
else 
{
goto label_863451;
}
}
else 
{
label_863451:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_863510;
}
else 
{
goto label_863458;
}
}
else 
{
label_863458:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_863510;
}
else 
{
goto label_863465;
}
}
else 
{
label_863465:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_863510;
}
else 
{
goto label_863472;
}
}
else 
{
label_863472:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_863510;
}
else 
{
goto label_863479;
}
}
else 
{
label_863479:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_863488;
}
else 
{
label_863488:; 
goto label_863510;
}
}
else 
{
label_863510:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_863532;
}
else 
{
goto label_863517;
}
}
else 
{
label_863517:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_863526;
}
else 
{
label_863526:; 
goto label_863532;
}
}
else 
{
label_863532:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_863605;
}
else 
{
goto label_863546;
}
}
else 
{
label_863546:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_863605;
}
else 
{
goto label_863553;
}
}
else 
{
label_863553:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_863605;
}
else 
{
goto label_863560;
}
}
else 
{
label_863560:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_863605;
}
else 
{
goto label_863567;
}
}
else 
{
label_863567:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_863605;
}
else 
{
goto label_863574;
}
}
else 
{
label_863574:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_863583;
}
else 
{
label_863583:; 
goto label_863605;
}
}
else 
{
label_863605:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_863627;
}
else 
{
goto label_863612;
}
}
else 
{
label_863612:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_863621;
}
else 
{
label_863621:; 
goto label_863627;
}
}
else 
{
label_863627:; 
c_m_ev = 2;
if ((req_a___0 + 50) == rsp_d___0)
{
a = a + 1;
goto label_863644;
}
else 
{
{
__VERIFIER_error();
}
a = a + 1;
label_863644:; 
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
goto label_863742;
}
else 
{
goto label_863683;
}
}
else 
{
label_863683:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_863742;
}
else 
{
goto label_863690;
}
}
else 
{
label_863690:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_863742;
}
else 
{
goto label_863697;
}
}
else 
{
label_863697:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_863742;
}
else 
{
goto label_863704;
}
}
else 
{
label_863704:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_863742;
}
else 
{
goto label_863711;
}
}
else 
{
label_863711:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_863720;
}
else 
{
label_863720:; 
goto label_863742;
}
}
else 
{
label_863742:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_863764;
}
else 
{
goto label_863749;
}
}
else 
{
label_863749:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_863758;
}
else 
{
label_863758:; 
goto label_863764;
}
}
else 
{
label_863764:; 
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
goto label_863844;
}
else 
{
goto label_863785;
}
}
else 
{
label_863785:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_863844;
}
else 
{
goto label_863792;
}
}
else 
{
label_863792:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_863844;
}
else 
{
goto label_863799;
}
}
else 
{
label_863799:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_863844;
}
else 
{
goto label_863806;
}
}
else 
{
label_863806:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_863844;
}
else 
{
goto label_863813;
}
}
else 
{
label_863813:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_863822;
}
else 
{
label_863822:; 
goto label_863844;
}
}
else 
{
label_863844:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_863866;
}
else 
{
goto label_863851;
}
}
else 
{
label_863851:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_863860;
}
else 
{
label_863860:; 
goto label_863866;
}
}
else 
{
label_863866:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_863939;
}
else 
{
goto label_863880;
}
}
else 
{
label_863880:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_863939;
}
else 
{
goto label_863887;
}
}
else 
{
label_863887:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_863939;
}
else 
{
goto label_863894;
}
}
else 
{
label_863894:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_863939;
}
else 
{
goto label_863901;
}
}
else 
{
label_863901:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_863939;
}
else 
{
goto label_863908;
}
}
else 
{
label_863908:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_863917;
}
else 
{
label_863917:; 
goto label_863939;
}
}
else 
{
label_863939:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_863946;
}
}
else 
{
label_863946:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_863955;
}
else 
{
label_863955:; 
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
goto label_861574;
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
goto label_864564;
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
goto label_864179;
}
else 
{
goto label_864120;
}
}
else 
{
label_864120:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_864179;
}
else 
{
goto label_864127;
}
}
else 
{
label_864127:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_864179;
}
else 
{
goto label_864134;
}
}
else 
{
label_864134:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_864179;
}
else 
{
goto label_864141;
}
}
else 
{
label_864141:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_864179;
}
else 
{
goto label_864148;
}
}
else 
{
label_864148:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_864157;
}
else 
{
label_864157:; 
goto label_864179;
}
}
else 
{
label_864179:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_864186;
}
}
else 
{
label_864186:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_864195;
}
else 
{
label_864195:; 
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
label_864225:; 
label_864999:; 
if (((int)m_run_st) == 0)
{
goto label_865013;
}
else 
{
if (((int)s_run_st) == 0)
{
label_865013:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_865024;
}
else 
{
label_865024:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_865200;
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
goto label_865151;
}
else 
{
goto label_865092;
}
}
else 
{
label_865092:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_865151;
}
else 
{
goto label_865099;
}
}
else 
{
label_865099:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_865151;
}
else 
{
goto label_865106;
}
}
else 
{
label_865106:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_865151;
}
else 
{
goto label_865113;
}
}
else 
{
label_865113:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_865151;
}
else 
{
goto label_865120;
}
}
else 
{
label_865120:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_865129;
}
else 
{
label_865129:; 
goto label_865151;
}
}
else 
{
label_865151:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_865158;
}
}
else 
{
label_865158:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_865167;
}
else 
{
label_865167:; 
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
goto label_864225;
}
}
}
}
else 
{
label_865200:; 
goto label_864999;
}
}
}
else 
{
}
goto label_855756;
}
}
}
}
}
else 
{
label_864564:; 
goto label_864999;
}
}
else 
{
}
label_864025:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_864566;
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
goto label_864345;
}
else 
{
goto label_864286;
}
}
else 
{
label_864286:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_864345;
}
else 
{
goto label_864293;
}
}
else 
{
label_864293:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_864345;
}
else 
{
goto label_864300;
}
}
else 
{
label_864300:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_864345;
}
else 
{
goto label_864307;
}
}
else 
{
label_864307:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_864345;
}
else 
{
goto label_864314;
}
}
else 
{
label_864314:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_864323;
}
else 
{
label_864323:; 
goto label_864345;
}
}
else 
{
label_864345:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_864352;
}
}
else 
{
label_864352:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_864361;
}
else 
{
label_864361:; 
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
label_864391:; 
label_864572:; 
if (((int)m_run_st) == 0)
{
goto label_864586;
}
else 
{
if (((int)s_run_st) == 0)
{
label_864586:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_864819;
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
goto label_864774;
}
else 
{
goto label_864715;
}
}
else 
{
label_864715:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_864774;
}
else 
{
goto label_864722;
}
}
else 
{
label_864722:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_864774;
}
else 
{
goto label_864729;
}
}
else 
{
label_864729:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_864774;
}
else 
{
goto label_864736;
}
}
else 
{
label_864736:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_864774;
}
else 
{
goto label_864743;
}
}
else 
{
label_864743:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_864752;
}
else 
{
label_864752:; 
goto label_864774;
}
}
else 
{
label_864774:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_864781;
}
}
else 
{
label_864781:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_864790;
}
else 
{
label_864790:; 
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
goto label_864025;
}
}
}
}
else 
{
label_864819:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_864995;
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
goto label_864946;
}
else 
{
goto label_864887;
}
}
else 
{
label_864887:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_864946;
}
else 
{
goto label_864894;
}
}
else 
{
label_864894:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_864946;
}
else 
{
goto label_864901;
}
}
else 
{
label_864901:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_864946;
}
else 
{
goto label_864908;
}
}
else 
{
label_864908:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_864946;
}
else 
{
goto label_864915;
}
}
else 
{
label_864915:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_864924;
}
else 
{
label_864924:; 
goto label_864946;
}
}
else 
{
label_864946:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_864953;
}
}
else 
{
label_864953:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_864962;
}
else 
{
label_864962:; 
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
goto label_864391;
}
}
}
}
else 
{
label_864995:; 
goto label_864572;
}
}
}
else 
{
}
goto label_855329;
}
}
}
}
}
else 
{
label_864566:; 
goto label_864572;
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_864036:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_864568;
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
goto label_864511;
}
else 
{
goto label_864452;
}
}
else 
{
label_864452:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_864511;
}
else 
{
goto label_864459;
}
}
else 
{
label_864459:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_864511;
}
else 
{
goto label_864466;
}
}
else 
{
label_864466:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_864511;
}
else 
{
goto label_864473;
}
}
else 
{
label_864473:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_864511;
}
else 
{
goto label_864480;
}
}
else 
{
label_864480:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_864489;
}
else 
{
label_864489:; 
goto label_864511;
}
}
else 
{
label_864511:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_864518;
}
}
else 
{
label_864518:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_864527;
}
else 
{
label_864527:; 
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
goto label_862532;
}
}
}
}
else 
{
label_864568:; 
goto label_863309;
}
}
}
}
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
goto label_862245;
label_862245:; 
rsp_status = 1;
goto label_862276;
}
}
else 
{
rsp_status = 0;
goto label_862276;
}
}
}
}
}
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
label_862874:; 
label_865214:; 
if (((int)m_run_st) == 0)
{
goto label_865228;
}
else 
{
if (((int)s_run_st) == 0)
{
label_865228:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_865239;
}
else 
{
label_865239:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_865706;
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
goto label_865366;
}
else 
{
goto label_865307;
}
}
else 
{
label_865307:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_865366;
}
else 
{
goto label_865314;
}
}
else 
{
label_865314:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_865366;
}
else 
{
goto label_865321;
}
}
else 
{
label_865321:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_865366;
}
else 
{
goto label_865328;
}
}
else 
{
label_865328:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_865366;
}
else 
{
goto label_865335;
}
}
else 
{
label_865335:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_865344;
}
else 
{
label_865344:; 
goto label_865366;
}
}
else 
{
label_865366:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_865388;
}
else 
{
goto label_865373;
}
}
else 
{
label_865373:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_865382;
}
else 
{
label_865382:; 
goto label_865388;
}
}
else 
{
label_865388:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_29 = req_a;
int i = __tmp_29;
int x;
if (i == 0)
{
x = s_memory0;
 __return_865438 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_865439 = x;
}
rsp_d = __return_865438;
goto label_865441;
rsp_d = __return_865439;
label_865441:; 
rsp_status = 1;
label_865447:; 
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
goto label_865528;
}
else 
{
goto label_865469;
}
}
else 
{
label_865469:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_865528;
}
else 
{
goto label_865476;
}
}
else 
{
label_865476:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_865528;
}
else 
{
goto label_865483;
}
}
else 
{
label_865483:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_865528;
}
else 
{
goto label_865490;
}
}
else 
{
label_865490:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_865528;
}
else 
{
goto label_865497;
}
}
else 
{
label_865497:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_865506;
}
else 
{
label_865506:; 
goto label_865528;
}
}
else 
{
label_865528:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_865550;
}
else 
{
goto label_865535;
}
}
else 
{
label_865535:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_865544;
}
else 
{
label_865544:; 
goto label_865550;
}
}
else 
{
label_865550:; 
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
goto label_865633;
}
else 
{
goto label_865574;
}
}
else 
{
label_865574:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_865633;
}
else 
{
goto label_865581;
}
}
else 
{
label_865581:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_865633;
}
else 
{
goto label_865588;
}
}
else 
{
label_865588:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_865633;
}
else 
{
goto label_865595;
}
}
else 
{
label_865595:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_865633;
}
else 
{
goto label_865602;
}
}
else 
{
label_865602:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_865611;
}
else 
{
label_865611:; 
goto label_865633;
}
}
else 
{
label_865633:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_865640;
}
}
else 
{
label_865640:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_865649;
}
else 
{
label_865649:; 
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
goto label_862532;
}
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
int __tmp_30 = req_a;
int __tmp_31 = req_d;
int i = __tmp_30;
int v = __tmp_31;
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
goto label_865416;
label_865416:; 
rsp_status = 1;
goto label_865447;
}
}
else 
{
rsp_status = 0;
goto label_865447;
}
}
}
}
}
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
label_865706:; 
goto label_865214;
}
}
}
else 
{
}
goto label_855971;
}
}
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
}
label_861572:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_862876;
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
goto label_862652;
}
else 
{
goto label_862593;
}
}
else 
{
label_862593:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_862652;
}
else 
{
goto label_862600;
}
}
else 
{
label_862600:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_862652;
}
else 
{
goto label_862607;
}
}
else 
{
label_862607:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_862652;
}
else 
{
goto label_862614;
}
}
else 
{
label_862614:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_862652;
}
else 
{
goto label_862621;
}
}
else 
{
label_862621:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_862630;
}
else 
{
label_862630:; 
goto label_862652;
}
}
else 
{
label_862652:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_862659;
}
}
else 
{
label_862659:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_862668;
}
else 
{
label_862668:; 
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
label_862698:; 
label_862882:; 
if (((int)m_run_st) == 0)
{
goto label_862896;
}
else 
{
if (((int)s_run_st) == 0)
{
label_862896:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_863129;
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
goto label_863084;
}
else 
{
goto label_863025;
}
}
else 
{
label_863025:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_863084;
}
else 
{
goto label_863032;
}
}
else 
{
label_863032:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_863084;
}
else 
{
goto label_863039;
}
}
else 
{
label_863039:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_863084;
}
else 
{
goto label_863046;
}
}
else 
{
label_863046:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_863084;
}
else 
{
goto label_863053;
}
}
else 
{
label_863053:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_863062;
}
else 
{
label_863062:; 
goto label_863084;
}
}
else 
{
label_863084:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_863091;
}
}
else 
{
label_863091:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_863100;
}
else 
{
label_863100:; 
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
goto label_861572;
}
}
}
}
else 
{
label_863129:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_863305;
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
goto label_863256;
}
else 
{
goto label_863197;
}
}
else 
{
label_863197:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_863256;
}
else 
{
goto label_863204;
}
}
else 
{
label_863204:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_863256;
}
else 
{
goto label_863211;
}
}
else 
{
label_863211:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_863256;
}
else 
{
goto label_863218;
}
}
else 
{
label_863218:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_863256;
}
else 
{
goto label_863225;
}
}
else 
{
label_863225:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_863234;
}
else 
{
label_863234:; 
goto label_863256;
}
}
else 
{
label_863256:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_863263;
}
}
else 
{
label_863263:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_863272;
}
else 
{
label_863272:; 
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
goto label_862698;
}
}
}
}
else 
{
label_863305:; 
goto label_862882;
}
}
}
else 
{
}
goto label_853639;
}
}
}
}
}
else 
{
label_862876:; 
goto label_862882;
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_861587:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_862878;
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
goto label_862818;
}
else 
{
goto label_862759;
}
}
else 
{
label_862759:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_862818;
}
else 
{
goto label_862766;
}
}
else 
{
label_862766:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_862818;
}
else 
{
goto label_862773;
}
}
else 
{
label_862773:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_862818;
}
else 
{
goto label_862780;
}
}
else 
{
label_862780:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_862818;
}
else 
{
goto label_862787;
}
}
else 
{
label_862787:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_862796;
}
else 
{
label_862796:; 
goto label_862818;
}
}
else 
{
label_862818:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_862825;
}
}
else 
{
label_862825:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_862834;
}
else 
{
label_862834:; 
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
goto label_860042;
}
}
}
}
else 
{
label_862878:; 
goto label_860509;
}
}
}
}
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
goto label_859755;
label_859755:; 
rsp_status = 1;
goto label_859786;
}
}
else 
{
rsp_status = 0;
goto label_859786;
}
}
}
}
}
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
label_860504:; 
label_866208:; 
if (((int)m_run_st) == 0)
{
goto label_866222;
}
else 
{
if (((int)s_run_st) == 0)
{
label_866222:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_866233;
}
else 
{
label_866233:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_866700;
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
goto label_866278;
}
else 
{
label_866278:; 
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
goto label_866360;
}
else 
{
goto label_866301;
}
}
else 
{
label_866301:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_866360;
}
else 
{
goto label_866308;
}
}
else 
{
label_866308:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_866360;
}
else 
{
goto label_866315;
}
}
else 
{
label_866315:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_866360;
}
else 
{
goto label_866322;
}
}
else 
{
label_866322:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_866360;
}
else 
{
goto label_866329;
}
}
else 
{
label_866329:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_866338;
}
else 
{
label_866338:; 
goto label_866360;
}
}
else 
{
label_866360:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_866382;
}
else 
{
goto label_866367;
}
}
else 
{
label_866367:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_866376;
}
else 
{
label_866376:; 
goto label_866382;
}
}
else 
{
label_866382:; 
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
 __return_866432 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_866433 = x;
}
rsp_d = __return_866432;
goto label_866435;
rsp_d = __return_866433;
label_866435:; 
rsp_status = 1;
label_866441:; 
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
goto label_866522;
}
else 
{
goto label_866463;
}
}
else 
{
label_866463:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_866522;
}
else 
{
goto label_866470;
}
}
else 
{
label_866470:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_866522;
}
else 
{
goto label_866477;
}
}
else 
{
label_866477:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_866522;
}
else 
{
goto label_866484;
}
}
else 
{
label_866484:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_866522;
}
else 
{
goto label_866491;
}
}
else 
{
label_866491:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_866500;
}
else 
{
label_866500:; 
goto label_866522;
}
}
else 
{
label_866522:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_866544;
}
else 
{
goto label_866529;
}
}
else 
{
label_866529:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_866538;
}
else 
{
label_866538:; 
goto label_866544;
}
}
else 
{
label_866544:; 
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
goto label_866627;
}
else 
{
goto label_866568;
}
}
else 
{
label_866568:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_866627;
}
else 
{
goto label_866575;
}
}
else 
{
label_866575:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_866627;
}
else 
{
goto label_866582;
}
}
else 
{
label_866582:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_866627;
}
else 
{
goto label_866589;
}
}
else 
{
label_866589:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_866627;
}
else 
{
goto label_866596;
}
}
else 
{
label_866596:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_866605;
}
else 
{
label_866605:; 
goto label_866627;
}
}
else 
{
label_866627:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_866634;
}
}
else 
{
label_866634:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_866643;
}
else 
{
label_866643:; 
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
goto label_860042;
}
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
goto label_866410;
label_866410:; 
rsp_status = 1;
goto label_866441;
}
}
else 
{
rsp_status = 0;
goto label_866441;
}
}
}
}
}
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
label_866700:; 
goto label_866208;
}
}
}
else 
{
}
goto label_856471;
}
}
}
}
}
}
else 
{
label_859570:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_860506;
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
goto label_860080;
}
else 
{
label_860080:; 
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
goto label_860162;
}
else 
{
goto label_860103;
}
}
else 
{
label_860103:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_860162;
}
else 
{
goto label_860110;
}
}
else 
{
label_860110:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_860162;
}
else 
{
goto label_860117;
}
}
else 
{
label_860117:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_860162;
}
else 
{
goto label_860124;
}
}
else 
{
label_860124:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_860162;
}
else 
{
goto label_860131;
}
}
else 
{
label_860131:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_860140;
}
else 
{
label_860140:; 
goto label_860162;
}
}
else 
{
label_860162:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_860184;
}
else 
{
goto label_860169;
}
}
else 
{
label_860169:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_860178;
}
else 
{
label_860178:; 
goto label_860184;
}
}
else 
{
label_860184:; 
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
 __return_860234 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_860235 = x;
}
rsp_d = __return_860234;
goto label_860237;
rsp_d = __return_860235;
label_860237:; 
rsp_status = 1;
label_860243:; 
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
goto label_860324;
}
else 
{
goto label_860265;
}
}
else 
{
label_860265:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_860324;
}
else 
{
goto label_860272;
}
}
else 
{
label_860272:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_860324;
}
else 
{
goto label_860279;
}
}
else 
{
label_860279:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_860324;
}
else 
{
goto label_860286;
}
}
else 
{
label_860286:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_860324;
}
else 
{
goto label_860293;
}
}
else 
{
label_860293:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_860302;
}
else 
{
label_860302:; 
goto label_860324;
}
}
else 
{
label_860324:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_860346;
}
else 
{
goto label_860331;
}
}
else 
{
label_860331:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_860340;
}
else 
{
label_860340:; 
goto label_860346;
}
}
else 
{
label_860346:; 
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
goto label_860429;
}
else 
{
goto label_860370;
}
}
else 
{
label_860370:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_860429;
}
else 
{
goto label_860377;
}
}
else 
{
label_860377:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_860429;
}
else 
{
goto label_860384;
}
}
else 
{
label_860384:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_860429;
}
else 
{
goto label_860391;
}
}
else 
{
label_860391:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_860429;
}
else 
{
goto label_860398;
}
}
else 
{
label_860398:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_860407;
}
else 
{
label_860407:; 
goto label_860429;
}
}
else 
{
label_860429:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_860436;
}
}
else 
{
label_860436:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_860445;
}
else 
{
label_860445:; 
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
goto label_860042;
}
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
int __tmp_38 = req_a;
int __tmp_39 = req_d;
int i = __tmp_38;
int v = __tmp_39;
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
goto label_860212;
label_860212:; 
rsp_status = 1;
goto label_860243;
}
}
else 
{
rsp_status = 0;
goto label_860243;
}
}
}
}
}
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
label_860506:; 
goto label_859229;
}
}
}
else 
{
}
goto label_856471;
}
}
}
else 
{
}
goto label_858135;
}
}
}
}
}
}
}
}
}
}
}
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
goto label_849693;
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
goto label_848934;
}
else 
{
label_848934:; 
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
goto label_849016;
}
else 
{
goto label_848957;
}
}
else 
{
label_848957:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_849016;
}
else 
{
goto label_848964;
}
}
else 
{
label_848964:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_849016;
}
else 
{
goto label_848971;
}
}
else 
{
label_848971:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_849016;
}
else 
{
goto label_848978;
}
}
else 
{
label_848978:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_849016;
}
else 
{
goto label_848985;
}
}
else 
{
label_848985:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_848994;
}
else 
{
label_848994:; 
goto label_849016;
}
}
else 
{
label_849016:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_849023;
}
}
else 
{
label_849023:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_849032;
}
else 
{
label_849032:; 
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
label_849062:; 
goto label_851519;
}
}
}
}
else 
{
label_849693:; 
label_856956:; 
if (((int)m_run_st) == 0)
{
goto label_856970;
}
else 
{
if (((int)s_run_st) == 0)
{
label_856970:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_856981;
}
else 
{
label_856981:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_857157;
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
goto label_857026;
}
else 
{
label_857026:; 
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
goto label_857108;
}
else 
{
goto label_857049;
}
}
else 
{
label_857049:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_857108;
}
else 
{
goto label_857056;
}
}
else 
{
label_857056:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_857108;
}
else 
{
goto label_857063;
}
}
else 
{
label_857063:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_857108;
}
else 
{
goto label_857070;
}
}
else 
{
label_857070:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_857108;
}
else 
{
goto label_857077;
}
}
else 
{
label_857077:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_857086;
}
else 
{
label_857086:; 
goto label_857108;
}
}
else 
{
label_857108:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_857115;
}
}
else 
{
label_857115:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_857124;
}
else 
{
label_857124:; 
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
goto label_849062;
}
}
}
}
else 
{
label_857157:; 
goto label_856956;
}
}
}
else 
{
}
label_856967:; 
kernel_st = 2;
kernel_st = 3;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_857821;
}
else 
{
goto label_857231;
}
}
else 
{
label_857231:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_857821;
}
else 
{
goto label_857319;
}
}
else 
{
label_857319:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_857821;
}
else 
{
goto label_857371;
}
}
else 
{
label_857371:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_857821;
}
else 
{
goto label_857459;
}
}
else 
{
label_857459:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_857821;
}
else 
{
goto label_857511;
}
}
else 
{
label_857511:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_857601;
}
else 
{
label_857601:; 
goto label_857821;
}
}
else 
{
label_857821:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_858059;
}
else 
{
goto label_857909;
}
}
else 
{
label_857909:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_857999;
}
else 
{
label_857999:; 
goto label_858059;
}
}
else 
{
label_858059:; 
if (((int)m_run_st) == 0)
{
goto label_858179;
}
else 
{
if (((int)s_run_st) == 0)
{
label_858179:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
label_858191:; 
if (((int)m_run_st) == 0)
{
goto label_858205;
}
else 
{
if (((int)s_run_st) == 0)
{
label_858205:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_858343;
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
goto label_858696;
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
goto label_858396;
}
else 
{
label_858396:; 
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
goto label_858478;
}
else 
{
goto label_858419;
}
}
else 
{
label_858419:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_858478;
}
else 
{
goto label_858426;
}
}
else 
{
label_858426:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_858478;
}
else 
{
goto label_858433;
}
}
else 
{
label_858433:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_858478;
}
else 
{
goto label_858440;
}
}
else 
{
label_858440:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_858478;
}
else 
{
goto label_858447;
}
}
else 
{
label_858447:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_858456;
}
else 
{
label_858456:; 
goto label_858478;
}
}
else 
{
label_858478:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_858485;
}
}
else 
{
label_858485:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_858494;
}
else 
{
label_858494:; 
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
label_858524:; 
goto label_858702;
}
}
}
}
else 
{
label_858696:; 
goto label_858191;
}
}
}
}
else 
{
label_858343:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_858698;
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
goto label_858562;
}
else 
{
label_858562:; 
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
goto label_858644;
}
else 
{
goto label_858585;
}
}
else 
{
label_858585:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_858644;
}
else 
{
goto label_858592;
}
}
else 
{
label_858592:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_858644;
}
else 
{
goto label_858599;
}
}
else 
{
label_858599:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_858644;
}
else 
{
goto label_858606;
}
}
else 
{
label_858606:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_858644;
}
else 
{
goto label_858613;
}
}
else 
{
label_858613:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_858622;
}
else 
{
label_858622:; 
goto label_858644;
}
}
else 
{
label_858644:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_858651;
}
}
else 
{
label_858651:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_858660;
}
else 
{
label_858660:; 
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
label_858690:; 
label_858702:; 
if (((int)m_run_st) == 0)
{
goto label_858716;
}
else 
{
if (((int)s_run_st) == 0)
{
label_858716:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_858854;
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
goto label_859205;
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
goto label_858989;
}
else 
{
goto label_858930;
}
}
else 
{
label_858930:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_858989;
}
else 
{
goto label_858937;
}
}
else 
{
label_858937:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_858989;
}
else 
{
goto label_858944;
}
}
else 
{
label_858944:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_858989;
}
else 
{
goto label_858951;
}
}
else 
{
label_858951:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_858989;
}
else 
{
goto label_858958;
}
}
else 
{
label_858958:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_858967;
}
else 
{
label_858967:; 
goto label_858989;
}
}
else 
{
label_858989:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_858996;
}
}
else 
{
label_858996:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_859005;
}
else 
{
label_859005:; 
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
goto label_858524;
}
}
}
}
else 
{
label_859205:; 
goto label_858702;
}
}
}
}
else 
{
label_858854:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_859207;
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
goto label_859155;
}
else 
{
goto label_859096;
}
}
else 
{
label_859096:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_859155;
}
else 
{
goto label_859103;
}
}
else 
{
label_859103:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_859155;
}
else 
{
goto label_859110;
}
}
else 
{
label_859110:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_859155;
}
else 
{
goto label_859117;
}
}
else 
{
label_859117:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_859155;
}
else 
{
goto label_859124;
}
}
else 
{
label_859124:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_859133;
}
else 
{
label_859133:; 
goto label_859155;
}
}
else 
{
label_859155:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_859162;
}
}
else 
{
label_859162:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_859171;
}
else 
{
label_859171:; 
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
goto label_858690;
}
}
}
}
else 
{
label_859207:; 
goto label_858702;
}
}
}
else 
{
}
goto label_851530;
}
}
}
}
}
else 
{
label_858698:; 
goto label_858191;
}
}
}
else 
{
}
goto label_856967;
}
}
}
else 
{
}
goto label_858131;
}
}
}
}
}
}
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
label_848873:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_849697;
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
goto label_849557;
}
else 
{
label_849557:; 
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
goto label_849639;
}
else 
{
goto label_849580;
}
}
else 
{
label_849580:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_849639;
}
else 
{
goto label_849587;
}
}
else 
{
label_849587:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_849639;
}
else 
{
goto label_849594;
}
}
else 
{
label_849594:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_849639;
}
else 
{
goto label_849601;
}
}
else 
{
label_849601:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_849639;
}
else 
{
goto label_849608;
}
}
else 
{
label_849608:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_849617;
}
else 
{
label_849617:; 
goto label_849639;
}
}
else 
{
label_849639:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_849646;
}
}
else 
{
label_849646:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_849655;
}
else 
{
label_849655:; 
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
label_849685:; 
label_849702:; 
if (((int)m_run_st) == 0)
{
goto label_849716;
}
else 
{
if (((int)s_run_st) == 0)
{
label_849716:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_850198;
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
goto label_849824;
}
else 
{
label_849824:; 
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
goto label_849927;
}
else 
{
goto label_849868;
}
}
else 
{
label_849868:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_849927;
}
else 
{
goto label_849875;
}
}
else 
{
label_849875:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_849927;
}
else 
{
goto label_849882;
}
}
else 
{
label_849882:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_849927;
}
else 
{
goto label_849889;
}
}
else 
{
label_849889:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_849927;
}
else 
{
goto label_849896;
}
}
else 
{
label_849896:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_849905;
}
else 
{
label_849905:; 
goto label_849927;
}
}
else 
{
label_849927:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_849949;
}
else 
{
goto label_849934;
}
}
else 
{
label_849934:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_849943;
}
else 
{
label_849943:; 
goto label_849949;
}
}
else 
{
label_849949:; 
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
goto label_850029;
}
else 
{
goto label_849970;
}
}
else 
{
label_849970:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_850029;
}
else 
{
goto label_849977;
}
}
else 
{
label_849977:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_850029;
}
else 
{
goto label_849984;
}
}
else 
{
label_849984:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_850029;
}
else 
{
goto label_849991;
}
}
else 
{
label_849991:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_850029;
}
else 
{
goto label_849998;
}
}
else 
{
label_849998:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_850007;
}
else 
{
label_850007:; 
goto label_850029;
}
}
else 
{
label_850029:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_850051;
}
else 
{
goto label_850036;
}
}
else 
{
label_850036:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_850045;
}
else 
{
label_850045:; 
goto label_850051;
}
}
else 
{
label_850051:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_850124;
}
else 
{
goto label_850065;
}
}
else 
{
label_850065:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_850124;
}
else 
{
goto label_850072;
}
}
else 
{
label_850072:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_850124;
}
else 
{
goto label_850079;
}
}
else 
{
label_850079:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_850124;
}
else 
{
goto label_850086;
}
}
else 
{
label_850086:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_850124;
}
else 
{
goto label_850093;
}
}
else 
{
label_850093:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_850102;
}
else 
{
label_850102:; 
goto label_850124;
}
}
else 
{
label_850124:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_850131;
}
}
else 
{
label_850131:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_850140;
}
else 
{
label_850140:; 
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
label_850190:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_851017;
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
goto label_850507;
}
else 
{
goto label_850448;
}
}
else 
{
label_850448:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_850507;
}
else 
{
goto label_850455;
}
}
else 
{
label_850455:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_850507;
}
else 
{
goto label_850462;
}
}
else 
{
label_850462:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_850507;
}
else 
{
goto label_850469;
}
}
else 
{
label_850469:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_850507;
}
else 
{
goto label_850476;
}
}
else 
{
label_850476:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_850485;
}
else 
{
label_850485:; 
goto label_850507;
}
}
else 
{
label_850507:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_850529;
}
else 
{
goto label_850514;
}
}
else 
{
label_850514:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_850523;
}
else 
{
label_850523:; 
goto label_850529;
}
}
else 
{
label_850529:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_40 = req_a;
int i = __tmp_40;
int x;
if (i == 0)
{
x = s_memory0;
 __return_850579 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_850580 = x;
}
rsp_d = __return_850579;
goto label_850582;
rsp_d = __return_850580;
label_850582:; 
rsp_status = 1;
label_850588:; 
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
goto label_850669;
}
else 
{
goto label_850610;
}
}
else 
{
label_850610:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_850669;
}
else 
{
goto label_850617;
}
}
else 
{
label_850617:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_850669;
}
else 
{
goto label_850624;
}
}
else 
{
label_850624:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_850669;
}
else 
{
goto label_850631;
}
}
else 
{
label_850631:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_850669;
}
else 
{
goto label_850638;
}
}
else 
{
label_850638:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_850647;
}
else 
{
label_850647:; 
goto label_850669;
}
}
else 
{
label_850669:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_850691;
}
else 
{
goto label_850676;
}
}
else 
{
label_850676:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_850685;
}
else 
{
label_850685:; 
goto label_850691;
}
}
else 
{
label_850691:; 
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
goto label_850774;
}
else 
{
goto label_850715;
}
}
else 
{
label_850715:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_850774;
}
else 
{
goto label_850722;
}
}
else 
{
label_850722:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_850774;
}
else 
{
goto label_850729;
}
}
else 
{
label_850729:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_850774;
}
else 
{
goto label_850736;
}
}
else 
{
label_850736:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_850774;
}
else 
{
goto label_850743;
}
}
else 
{
label_850743:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_850752;
}
else 
{
label_850752:; 
goto label_850774;
}
}
else 
{
label_850774:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_850781;
}
}
else 
{
label_850781:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_850790;
}
else 
{
label_850790:; 
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
goto label_849519;
}
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
int __tmp_41 = req_a;
int __tmp_42 = req_d;
int i = __tmp_41;
int v = __tmp_42;
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
goto label_850557;
label_850557:; 
rsp_status = 1;
goto label_850588;
}
}
else 
{
rsp_status = 0;
goto label_850588;
}
}
}
}
}
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
label_851017:; 
label_851023:; 
if (((int)m_run_st) == 0)
{
goto label_851037;
}
else 
{
if (((int)s_run_st) == 0)
{
label_851037:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_851048;
}
else 
{
label_851048:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_851515;
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
goto label_851175;
}
else 
{
goto label_851116;
}
}
else 
{
label_851116:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_851175;
}
else 
{
goto label_851123;
}
}
else 
{
label_851123:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_851175;
}
else 
{
goto label_851130;
}
}
else 
{
label_851130:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_851175;
}
else 
{
goto label_851137;
}
}
else 
{
label_851137:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_851175;
}
else 
{
goto label_851144;
}
}
else 
{
label_851144:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_851153;
}
else 
{
label_851153:; 
goto label_851175;
}
}
else 
{
label_851175:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_851197;
}
else 
{
goto label_851182;
}
}
else 
{
label_851182:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_851191;
}
else 
{
label_851191:; 
goto label_851197;
}
}
else 
{
label_851197:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_43 = req_a;
int i = __tmp_43;
int x;
if (i == 0)
{
x = s_memory0;
 __return_851247 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_851248 = x;
}
rsp_d = __return_851247;
goto label_851250;
rsp_d = __return_851248;
label_851250:; 
rsp_status = 1;
label_851256:; 
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
goto label_851337;
}
else 
{
goto label_851278;
}
}
else 
{
label_851278:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_851337;
}
else 
{
goto label_851285;
}
}
else 
{
label_851285:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_851337;
}
else 
{
goto label_851292;
}
}
else 
{
label_851292:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_851337;
}
else 
{
goto label_851299;
}
}
else 
{
label_851299:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_851337;
}
else 
{
goto label_851306;
}
}
else 
{
label_851306:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_851315;
}
else 
{
label_851315:; 
goto label_851337;
}
}
else 
{
label_851337:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_851359;
}
else 
{
goto label_851344;
}
}
else 
{
label_851344:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_851353;
}
else 
{
label_851353:; 
goto label_851359;
}
}
else 
{
label_851359:; 
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
goto label_851442;
}
else 
{
goto label_851383;
}
}
else 
{
label_851383:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_851442;
}
else 
{
goto label_851390;
}
}
else 
{
label_851390:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_851442;
}
else 
{
goto label_851397;
}
}
else 
{
label_851397:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_851442;
}
else 
{
goto label_851404;
}
}
else 
{
label_851404:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_851442;
}
else 
{
goto label_851411;
}
}
else 
{
label_851411:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_851420;
}
else 
{
label_851420:; 
goto label_851442;
}
}
else 
{
label_851442:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_851449;
}
}
else 
{
label_851449:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_851458;
}
else 
{
label_851458:; 
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
goto label_849519;
}
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
goto label_851225;
label_851225:; 
rsp_status = 1;
goto label_851256;
}
}
else 
{
rsp_status = 0;
goto label_851256;
}
}
}
}
}
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
label_851515:; 
goto label_851023;
}
}
}
else 
{
}
label_851034:; 
kernel_st = 2;
kernel_st = 3;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_857835;
}
else 
{
goto label_857245;
}
}
else 
{
label_857245:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_857835;
}
else 
{
goto label_857305;
}
}
else 
{
label_857305:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_857835;
}
else 
{
goto label_857385;
}
}
else 
{
label_857385:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_857835;
}
else 
{
goto label_857445;
}
}
else 
{
label_857445:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_857835;
}
else 
{
goto label_857525;
}
}
else 
{
label_857525:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_857615;
}
else 
{
label_857615:; 
goto label_857835;
}
}
else 
{
label_857835:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_858045;
}
else 
{
goto label_857895;
}
}
else 
{
label_857895:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_857985;
}
else 
{
label_857985:; 
goto label_858045;
}
}
else 
{
label_858045:; 
if (((int)m_run_st) == 0)
{
goto label_858165;
}
else 
{
if (((int)s_run_st) == 0)
{
label_858165:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
label_871090:; 
if (((int)m_run_st) == 0)
{
goto label_871104;
}
else 
{
if (((int)s_run_st) == 0)
{
label_871104:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_871431;
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
goto label_871292;
}
else 
{
goto label_871233;
}
}
else 
{
label_871233:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_871292;
}
else 
{
goto label_871240;
}
}
else 
{
label_871240:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_871292;
}
else 
{
goto label_871247;
}
}
else 
{
label_871247:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_871292;
}
else 
{
goto label_871254;
}
}
else 
{
label_871254:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_871292;
}
else 
{
goto label_871261;
}
}
else 
{
label_871261:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_871270;
}
else 
{
label_871270:; 
goto label_871292;
}
}
else 
{
label_871292:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_871314;
}
else 
{
goto label_871299;
}
}
else 
{
label_871299:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_871308;
}
else 
{
label_871308:; 
goto label_871314;
}
}
else 
{
label_871314:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_871387;
}
else 
{
goto label_871328;
}
}
else 
{
label_871328:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_871387;
}
else 
{
goto label_871335;
}
}
else 
{
label_871335:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_871387;
}
else 
{
goto label_871342;
}
}
else 
{
label_871342:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_871387;
}
else 
{
goto label_871349;
}
}
else 
{
label_871349:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_871387;
}
else 
{
goto label_871356;
}
}
else 
{
label_871356:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_871365;
}
else 
{
label_871365:; 
goto label_871387;
}
}
else 
{
label_871387:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_871394;
}
}
else 
{
label_871394:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_871403;
}
else 
{
label_871403:; 
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
goto label_872365;
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
goto label_871566;
}
else 
{
goto label_871507;
}
}
else 
{
label_871507:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_871566;
}
else 
{
goto label_871514;
}
}
else 
{
label_871514:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_871566;
}
else 
{
goto label_871521;
}
}
else 
{
label_871521:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_871566;
}
else 
{
goto label_871528;
}
}
else 
{
label_871528:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_871566;
}
else 
{
goto label_871535;
}
}
else 
{
label_871535:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_871544;
}
else 
{
label_871544:; 
goto label_871566;
}
}
else 
{
label_871566:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_871588;
}
else 
{
goto label_871573;
}
}
else 
{
label_871573:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_871582;
}
else 
{
label_871582:; 
goto label_871588;
}
}
else 
{
label_871588:; 
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
 __return_871638 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_871639 = x;
}
rsp_d = __return_871638;
goto label_871641;
rsp_d = __return_871639;
label_871641:; 
rsp_status = 1;
label_871647:; 
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
goto label_871728;
}
else 
{
goto label_871669;
}
}
else 
{
label_871669:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_871728;
}
else 
{
goto label_871676;
}
}
else 
{
label_871676:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_871728;
}
else 
{
goto label_871683;
}
}
else 
{
label_871683:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_871728;
}
else 
{
goto label_871690;
}
}
else 
{
label_871690:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_871728;
}
else 
{
goto label_871697;
}
}
else 
{
label_871697:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_871706;
}
else 
{
label_871706:; 
goto label_871728;
}
}
else 
{
label_871728:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_871750;
}
else 
{
goto label_871735;
}
}
else 
{
label_871735:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_871744;
}
else 
{
label_871744:; 
goto label_871750;
}
}
else 
{
label_871750:; 
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
goto label_871833;
}
else 
{
goto label_871774;
}
}
else 
{
label_871774:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_871833;
}
else 
{
goto label_871781;
}
}
else 
{
label_871781:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_871833;
}
else 
{
goto label_871788;
}
}
else 
{
label_871788:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_871833;
}
else 
{
goto label_871795;
}
}
else 
{
label_871795:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_871833;
}
else 
{
goto label_871802;
}
}
else 
{
label_871802:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_871811;
}
else 
{
label_871811:; 
goto label_871833;
}
}
else 
{
label_871833:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_871840;
}
}
else 
{
label_871840:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_871849;
}
else 
{
label_871849:; 
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
label_871903:; 
label_872370:; 
if (((int)m_run_st) == 0)
{
goto label_872383;
}
else 
{
label_872383:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_873448;
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
goto label_872571;
}
else 
{
goto label_872512;
}
}
else 
{
label_872512:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_872571;
}
else 
{
goto label_872519;
}
}
else 
{
label_872519:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_872571;
}
else 
{
goto label_872526;
}
}
else 
{
label_872526:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_872571;
}
else 
{
goto label_872533;
}
}
else 
{
label_872533:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_872571;
}
else 
{
goto label_872540;
}
}
else 
{
label_872540:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_872549;
}
else 
{
label_872549:; 
goto label_872571;
}
}
else 
{
label_872571:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_872593;
}
else 
{
goto label_872578;
}
}
else 
{
label_872578:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_872587;
}
else 
{
label_872587:; 
goto label_872593;
}
}
else 
{
label_872593:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_872666;
}
else 
{
goto label_872607;
}
}
else 
{
label_872607:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_872666;
}
else 
{
goto label_872614;
}
}
else 
{
label_872614:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_872666;
}
else 
{
goto label_872621;
}
}
else 
{
label_872621:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_872666;
}
else 
{
goto label_872628;
}
}
else 
{
label_872628:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_872666;
}
else 
{
goto label_872635;
}
}
else 
{
label_872635:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_872644;
}
else 
{
label_872644:; 
goto label_872666;
}
}
else 
{
label_872666:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_872688;
}
else 
{
goto label_872673;
}
}
else 
{
label_872673:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_872682;
}
else 
{
label_872682:; 
goto label_872688;
}
}
else 
{
label_872688:; 
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
goto label_873153;
}
else 
{
goto label_873094;
}
}
else 
{
label_873094:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_873153;
}
else 
{
goto label_873101;
}
}
else 
{
label_873101:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_873153;
}
else 
{
goto label_873108;
}
}
else 
{
label_873108:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_873153;
}
else 
{
goto label_873115;
}
}
else 
{
label_873115:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_873153;
}
else 
{
goto label_873122;
}
}
else 
{
label_873122:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_873131;
}
else 
{
label_873131:; 
goto label_873153;
}
}
else 
{
label_873153:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_873175;
}
else 
{
goto label_873160;
}
}
else 
{
label_873160:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_873169;
}
else 
{
label_873169:; 
goto label_873175;
}
}
else 
{
label_873175:; 
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
goto label_873255;
}
else 
{
goto label_873196;
}
}
else 
{
label_873196:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_873255;
}
else 
{
goto label_873203;
}
}
else 
{
label_873203:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_873255;
}
else 
{
goto label_873210;
}
}
else 
{
label_873210:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_873255;
}
else 
{
goto label_873217;
}
}
else 
{
label_873217:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_873255;
}
else 
{
goto label_873224;
}
}
else 
{
label_873224:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_873233;
}
else 
{
label_873233:; 
goto label_873255;
}
}
else 
{
label_873255:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_873277;
}
else 
{
goto label_873262;
}
}
else 
{
label_873262:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_873271;
}
else 
{
label_873271:; 
goto label_873277;
}
}
else 
{
label_873277:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_873350;
}
else 
{
goto label_873291;
}
}
else 
{
label_873291:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_873350;
}
else 
{
goto label_873298;
}
}
else 
{
label_873298:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_873350;
}
else 
{
goto label_873305;
}
}
else 
{
label_873305:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_873350;
}
else 
{
goto label_873312;
}
}
else 
{
label_873312:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_873350;
}
else 
{
goto label_873319;
}
}
else 
{
label_873319:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_873328;
}
else 
{
label_873328:; 
goto label_873350;
}
}
else 
{
label_873350:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_873357;
}
}
else 
{
label_873357:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_873366;
}
else 
{
label_873366:; 
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
goto label_874733;
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
goto label_873599;
}
else 
{
goto label_873540;
}
}
else 
{
label_873540:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_873599;
}
else 
{
goto label_873547;
}
}
else 
{
label_873547:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_873599;
}
else 
{
goto label_873554;
}
}
else 
{
label_873554:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_873599;
}
else 
{
goto label_873561;
}
}
else 
{
label_873561:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_873599;
}
else 
{
goto label_873568;
}
}
else 
{
label_873568:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_873577;
}
else 
{
label_873577:; 
goto label_873599;
}
}
else 
{
label_873599:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_873621;
}
else 
{
goto label_873606;
}
}
else 
{
label_873606:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_873615;
}
else 
{
label_873615:; 
goto label_873621;
}
}
else 
{
label_873621:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_47 = req_a;
int i = __tmp_47;
int x;
if (i == 0)
{
x = s_memory0;
 __return_873671 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_873672 = x;
}
rsp_d = __return_873671;
goto label_873674;
rsp_d = __return_873672;
label_873674:; 
rsp_status = 1;
label_873680:; 
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
goto label_873761;
}
else 
{
goto label_873702;
}
}
else 
{
label_873702:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_873761;
}
else 
{
goto label_873709;
}
}
else 
{
label_873709:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_873761;
}
else 
{
goto label_873716;
}
}
else 
{
label_873716:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_873761;
}
else 
{
goto label_873723;
}
}
else 
{
label_873723:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_873761;
}
else 
{
goto label_873730;
}
}
else 
{
label_873730:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_873739;
}
else 
{
label_873739:; 
goto label_873761;
}
}
else 
{
label_873761:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_873783;
}
else 
{
goto label_873768;
}
}
else 
{
label_873768:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_873777;
}
else 
{
label_873777:; 
goto label_873783;
}
}
else 
{
label_873783:; 
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
goto label_873866;
}
else 
{
goto label_873807;
}
}
else 
{
label_873807:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_873866;
}
else 
{
goto label_873814;
}
}
else 
{
label_873814:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_873866;
}
else 
{
goto label_873821;
}
}
else 
{
label_873821:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_873866;
}
else 
{
goto label_873828;
}
}
else 
{
label_873828:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_873866;
}
else 
{
goto label_873835;
}
}
else 
{
label_873835:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_873844;
}
else 
{
label_873844:; 
goto label_873866;
}
}
else 
{
label_873866:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_873873;
}
}
else 
{
label_873873:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_873882;
}
else 
{
label_873882:; 
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
goto label_871903;
}
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
int __tmp_48 = req_a;
int __tmp_49 = req_d;
int i = __tmp_48;
int v = __tmp_49;
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
goto label_873649;
label_873649:; 
rsp_status = 1;
goto label_873680;
}
}
else 
{
rsp_status = 0;
goto label_873680;
}
}
}
}
}
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
label_874733:; 
label_877571:; 
if (((int)m_run_st) == 0)
{
goto label_877585;
}
else 
{
if (((int)s_run_st) == 0)
{
label_877585:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_877596;
}
else 
{
label_877596:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_878063;
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
goto label_877723;
}
else 
{
goto label_877664;
}
}
else 
{
label_877664:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_877723;
}
else 
{
goto label_877671;
}
}
else 
{
label_877671:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_877723;
}
else 
{
goto label_877678;
}
}
else 
{
label_877678:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_877723;
}
else 
{
goto label_877685;
}
}
else 
{
label_877685:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_877723;
}
else 
{
goto label_877692;
}
}
else 
{
label_877692:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_877701;
}
else 
{
label_877701:; 
goto label_877723;
}
}
else 
{
label_877723:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_877745;
}
else 
{
goto label_877730;
}
}
else 
{
label_877730:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_877739;
}
else 
{
label_877739:; 
goto label_877745;
}
}
else 
{
label_877745:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_50 = req_a;
int i = __tmp_50;
int x;
if (i == 0)
{
x = s_memory0;
 __return_877795 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_877796 = x;
}
rsp_d = __return_877795;
goto label_877798;
rsp_d = __return_877796;
label_877798:; 
rsp_status = 1;
label_877804:; 
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
goto label_877885;
}
else 
{
goto label_877826;
}
}
else 
{
label_877826:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_877885;
}
else 
{
goto label_877833;
}
}
else 
{
label_877833:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_877885;
}
else 
{
goto label_877840;
}
}
else 
{
label_877840:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_877885;
}
else 
{
goto label_877847;
}
}
else 
{
label_877847:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_877885;
}
else 
{
goto label_877854;
}
}
else 
{
label_877854:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_877863;
}
else 
{
label_877863:; 
goto label_877885;
}
}
else 
{
label_877885:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_877907;
}
else 
{
goto label_877892;
}
}
else 
{
label_877892:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_877901;
}
else 
{
label_877901:; 
goto label_877907;
}
}
else 
{
label_877907:; 
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
goto label_877990;
}
else 
{
goto label_877931;
}
}
else 
{
label_877931:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_877990;
}
else 
{
goto label_877938;
}
}
else 
{
label_877938:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_877990;
}
else 
{
goto label_877945;
}
}
else 
{
label_877945:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_877990;
}
else 
{
goto label_877952;
}
}
else 
{
label_877952:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_877990;
}
else 
{
goto label_877959;
}
}
else 
{
label_877959:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_877968;
}
else 
{
label_877968:; 
goto label_877990;
}
}
else 
{
label_877990:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_877997;
}
}
else 
{
label_877997:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_878006;
}
else 
{
label_878006:; 
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
goto label_871903;
}
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
int __tmp_51 = req_a;
int __tmp_52 = req_d;
int i = __tmp_51;
int v = __tmp_52;
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
goto label_877773;
label_877773:; 
rsp_status = 1;
goto label_877804;
}
}
else 
{
rsp_status = 0;
goto label_877804;
}
}
}
}
}
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
label_878063:; 
goto label_877571;
}
}
}
else 
{
}
goto label_851034;
}
}
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
if (a < 1)
{
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
goto label_872799;
}
else 
{
goto label_872740;
}
}
else 
{
label_872740:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_872799;
}
else 
{
goto label_872747;
}
}
else 
{
label_872747:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_872799;
}
else 
{
goto label_872754;
}
}
else 
{
label_872754:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_872799;
}
else 
{
goto label_872761;
}
}
else 
{
label_872761:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_872799;
}
else 
{
goto label_872768;
}
}
else 
{
label_872768:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_872777;
}
else 
{
label_872777:; 
goto label_872799;
}
}
else 
{
label_872799:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_872821;
}
else 
{
goto label_872806;
}
}
else 
{
label_872806:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_872815;
}
else 
{
label_872815:; 
goto label_872821;
}
}
else 
{
label_872821:; 
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
goto label_872901;
}
else 
{
goto label_872842;
}
}
else 
{
label_872842:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_872901;
}
else 
{
goto label_872849;
}
}
else 
{
label_872849:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_872901;
}
else 
{
goto label_872856;
}
}
else 
{
label_872856:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_872901;
}
else 
{
goto label_872863;
}
}
else 
{
label_872863:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_872901;
}
else 
{
goto label_872870;
}
}
else 
{
label_872870:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_872879;
}
else 
{
label_872879:; 
goto label_872901;
}
}
else 
{
label_872901:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_872923;
}
else 
{
goto label_872908;
}
}
else 
{
label_872908:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_872917;
}
else 
{
label_872917:; 
goto label_872923;
}
}
else 
{
label_872923:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_872996;
}
else 
{
goto label_872937;
}
}
else 
{
label_872937:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_872996;
}
else 
{
goto label_872944;
}
}
else 
{
label_872944:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_872996;
}
else 
{
goto label_872951;
}
}
else 
{
label_872951:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_872996;
}
else 
{
goto label_872958;
}
}
else 
{
label_872958:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_872996;
}
else 
{
goto label_872965;
}
}
else 
{
label_872965:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_872974;
}
else 
{
label_872974:; 
goto label_872996;
}
}
else 
{
label_872996:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_873003;
}
}
else 
{
label_873003:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_873012;
}
else 
{
label_873012:; 
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
label_873435:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_874735;
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
goto label_874056;
}
else 
{
goto label_873997;
}
}
else 
{
label_873997:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_874056;
}
else 
{
goto label_874004;
}
}
else 
{
label_874004:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_874056;
}
else 
{
goto label_874011;
}
}
else 
{
label_874011:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_874056;
}
else 
{
goto label_874018;
}
}
else 
{
label_874018:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_874056;
}
else 
{
goto label_874025;
}
}
else 
{
label_874025:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_874034;
}
else 
{
label_874034:; 
goto label_874056;
}
}
else 
{
label_874056:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_874078;
}
else 
{
goto label_874063;
}
}
else 
{
label_874063:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_874072;
}
else 
{
label_874072:; 
goto label_874078;
}
}
else 
{
label_874078:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_53 = req_a;
int i = __tmp_53;
int x;
if (i == 0)
{
x = s_memory0;
 __return_874128 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_874129 = x;
}
rsp_d = __return_874128;
goto label_874131;
rsp_d = __return_874129;
label_874131:; 
rsp_status = 1;
label_874137:; 
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
goto label_874218;
}
else 
{
goto label_874159;
}
}
else 
{
label_874159:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_874218;
}
else 
{
goto label_874166;
}
}
else 
{
label_874166:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_874218;
}
else 
{
goto label_874173;
}
}
else 
{
label_874173:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_874218;
}
else 
{
goto label_874180;
}
}
else 
{
label_874180:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_874218;
}
else 
{
goto label_874187;
}
}
else 
{
label_874187:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_874196;
}
else 
{
label_874196:; 
goto label_874218;
}
}
else 
{
label_874218:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_874240;
}
else 
{
goto label_874225;
}
}
else 
{
label_874225:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_874234;
}
else 
{
label_874234:; 
goto label_874240;
}
}
else 
{
label_874240:; 
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
goto label_874323;
}
else 
{
goto label_874264;
}
}
else 
{
label_874264:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_874323;
}
else 
{
goto label_874271;
}
}
else 
{
label_874271:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_874323;
}
else 
{
goto label_874278;
}
}
else 
{
label_874278:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_874323;
}
else 
{
goto label_874285;
}
}
else 
{
label_874285:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_874323;
}
else 
{
goto label_874292;
}
}
else 
{
label_874292:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_874301;
}
else 
{
label_874301:; 
goto label_874323;
}
}
else 
{
label_874323:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_874330;
}
}
else 
{
label_874330:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_874339;
}
else 
{
label_874339:; 
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
label_874393:; 
label_875170:; 
if (((int)m_run_st) == 0)
{
goto label_875183;
}
else 
{
label_875183:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_875897;
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
goto label_875371;
}
else 
{
goto label_875312;
}
}
else 
{
label_875312:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_875371;
}
else 
{
goto label_875319;
}
}
else 
{
label_875319:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_875371;
}
else 
{
goto label_875326;
}
}
else 
{
label_875326:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_875371;
}
else 
{
goto label_875333;
}
}
else 
{
label_875333:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_875371;
}
else 
{
goto label_875340;
}
}
else 
{
label_875340:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_875349;
}
else 
{
label_875349:; 
goto label_875371;
}
}
else 
{
label_875371:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_875393;
}
else 
{
goto label_875378;
}
}
else 
{
label_875378:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_875387;
}
else 
{
label_875387:; 
goto label_875393;
}
}
else 
{
label_875393:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_875466;
}
else 
{
goto label_875407;
}
}
else 
{
label_875407:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_875466;
}
else 
{
goto label_875414;
}
}
else 
{
label_875414:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_875466;
}
else 
{
goto label_875421;
}
}
else 
{
label_875421:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_875466;
}
else 
{
goto label_875428;
}
}
else 
{
label_875428:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_875466;
}
else 
{
goto label_875435;
}
}
else 
{
label_875435:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_875444;
}
else 
{
label_875444:; 
goto label_875466;
}
}
else 
{
label_875466:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_875488;
}
else 
{
goto label_875473;
}
}
else 
{
label_875473:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_875482;
}
else 
{
label_875482:; 
goto label_875488;
}
}
else 
{
label_875488:; 
c_m_ev = 2;
if ((req_a___0 + 50) == rsp_d___0)
{
a = a + 1;
goto label_875505;
}
else 
{
{
__VERIFIER_error();
}
a = a + 1;
label_875505:; 
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
goto label_875603;
}
else 
{
goto label_875544;
}
}
else 
{
label_875544:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_875603;
}
else 
{
goto label_875551;
}
}
else 
{
label_875551:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_875603;
}
else 
{
goto label_875558;
}
}
else 
{
label_875558:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_875603;
}
else 
{
goto label_875565;
}
}
else 
{
label_875565:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_875603;
}
else 
{
goto label_875572;
}
}
else 
{
label_875572:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_875581;
}
else 
{
label_875581:; 
goto label_875603;
}
}
else 
{
label_875603:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_875625;
}
else 
{
goto label_875610;
}
}
else 
{
label_875610:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_875619;
}
else 
{
label_875619:; 
goto label_875625;
}
}
else 
{
label_875625:; 
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
goto label_875705;
}
else 
{
goto label_875646;
}
}
else 
{
label_875646:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_875705;
}
else 
{
goto label_875653;
}
}
else 
{
label_875653:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_875705;
}
else 
{
goto label_875660;
}
}
else 
{
label_875660:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_875705;
}
else 
{
goto label_875667;
}
}
else 
{
label_875667:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_875705;
}
else 
{
goto label_875674;
}
}
else 
{
label_875674:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_875683;
}
else 
{
label_875683:; 
goto label_875705;
}
}
else 
{
label_875705:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_875727;
}
else 
{
goto label_875712;
}
}
else 
{
label_875712:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_875721;
}
else 
{
label_875721:; 
goto label_875727;
}
}
else 
{
label_875727:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_875800;
}
else 
{
goto label_875741;
}
}
else 
{
label_875741:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_875800;
}
else 
{
goto label_875748;
}
}
else 
{
label_875748:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_875800;
}
else 
{
goto label_875755;
}
}
else 
{
label_875755:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_875800;
}
else 
{
goto label_875762;
}
}
else 
{
label_875762:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_875800;
}
else 
{
goto label_875769;
}
}
else 
{
label_875769:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_875778;
}
else 
{
label_875778:; 
goto label_875800;
}
}
else 
{
label_875800:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_875807;
}
}
else 
{
label_875807:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_875816;
}
else 
{
label_875816:; 
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
goto label_873435;
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
goto label_876425;
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
goto label_876040;
}
else 
{
goto label_875981;
}
}
else 
{
label_875981:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_876040;
}
else 
{
goto label_875988;
}
}
else 
{
label_875988:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_876040;
}
else 
{
goto label_875995;
}
}
else 
{
label_875995:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_876040;
}
else 
{
goto label_876002;
}
}
else 
{
label_876002:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_876040;
}
else 
{
goto label_876009;
}
}
else 
{
label_876009:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_876018;
}
else 
{
label_876018:; 
goto label_876040;
}
}
else 
{
label_876040:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_876047;
}
}
else 
{
label_876047:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_876056;
}
else 
{
label_876056:; 
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
label_876086:; 
label_876860:; 
if (((int)m_run_st) == 0)
{
goto label_876874;
}
else 
{
if (((int)s_run_st) == 0)
{
label_876874:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_876885;
}
else 
{
label_876885:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_877061;
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
goto label_877012;
}
else 
{
goto label_876953;
}
}
else 
{
label_876953:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_877012;
}
else 
{
goto label_876960;
}
}
else 
{
label_876960:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_877012;
}
else 
{
goto label_876967;
}
}
else 
{
label_876967:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_877012;
}
else 
{
goto label_876974;
}
}
else 
{
label_876974:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_877012;
}
else 
{
goto label_876981;
}
}
else 
{
label_876981:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_876990;
}
else 
{
label_876990:; 
goto label_877012;
}
}
else 
{
label_877012:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_877019;
}
}
else 
{
label_877019:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_877028;
}
else 
{
label_877028:; 
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
goto label_876086;
}
}
}
}
else 
{
label_877061:; 
goto label_876860;
}
}
}
else 
{
}
goto label_855756;
}
}
}
}
}
else 
{
label_876425:; 
goto label_876860;
}
}
else 
{
}
label_875886:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_876427;
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
goto label_876206;
}
else 
{
goto label_876147;
}
}
else 
{
label_876147:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_876206;
}
else 
{
goto label_876154;
}
}
else 
{
label_876154:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_876206;
}
else 
{
goto label_876161;
}
}
else 
{
label_876161:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_876206;
}
else 
{
goto label_876168;
}
}
else 
{
label_876168:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_876206;
}
else 
{
goto label_876175;
}
}
else 
{
label_876175:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_876184;
}
else 
{
label_876184:; 
goto label_876206;
}
}
else 
{
label_876206:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_876213;
}
}
else 
{
label_876213:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_876222;
}
else 
{
label_876222:; 
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
label_876252:; 
label_876433:; 
if (((int)m_run_st) == 0)
{
goto label_876447;
}
else 
{
if (((int)s_run_st) == 0)
{
label_876447:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_876680;
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
goto label_876635;
}
else 
{
goto label_876576;
}
}
else 
{
label_876576:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_876635;
}
else 
{
goto label_876583;
}
}
else 
{
label_876583:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_876635;
}
else 
{
goto label_876590;
}
}
else 
{
label_876590:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_876635;
}
else 
{
goto label_876597;
}
}
else 
{
label_876597:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_876635;
}
else 
{
goto label_876604;
}
}
else 
{
label_876604:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_876613;
}
else 
{
label_876613:; 
goto label_876635;
}
}
else 
{
label_876635:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_876642;
}
}
else 
{
label_876642:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_876651;
}
else 
{
label_876651:; 
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
goto label_875886;
}
}
}
}
else 
{
label_876680:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_876856;
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
goto label_876807;
}
else 
{
goto label_876748;
}
}
else 
{
label_876748:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_876807;
}
else 
{
goto label_876755;
}
}
else 
{
label_876755:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_876807;
}
else 
{
goto label_876762;
}
}
else 
{
label_876762:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_876807;
}
else 
{
goto label_876769;
}
}
else 
{
label_876769:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_876807;
}
else 
{
goto label_876776;
}
}
else 
{
label_876776:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_876785;
}
else 
{
label_876785:; 
goto label_876807;
}
}
else 
{
label_876807:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_876814;
}
}
else 
{
label_876814:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_876823;
}
else 
{
label_876823:; 
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
goto label_876252;
}
}
}
}
else 
{
label_876856:; 
goto label_876433;
}
}
}
else 
{
}
goto label_855329;
}
}
}
}
}
else 
{
label_876427:; 
goto label_876433;
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_875897:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_876429;
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
goto label_876372;
}
else 
{
goto label_876313;
}
}
else 
{
label_876313:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_876372;
}
else 
{
goto label_876320;
}
}
else 
{
label_876320:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_876372;
}
else 
{
goto label_876327;
}
}
else 
{
label_876327:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_876372;
}
else 
{
goto label_876334;
}
}
else 
{
label_876334:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_876372;
}
else 
{
goto label_876341;
}
}
else 
{
label_876341:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_876350;
}
else 
{
label_876350:; 
goto label_876372;
}
}
else 
{
label_876372:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_876379;
}
}
else 
{
label_876379:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_876388;
}
else 
{
label_876388:; 
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
goto label_874393;
}
}
}
}
else 
{
label_876429:; 
goto label_875170;
}
}
}
}
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
int __tmp_54 = req_a;
int __tmp_55 = req_d;
int i = __tmp_54;
int v = __tmp_55;
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
goto label_874106;
label_874106:; 
rsp_status = 1;
goto label_874137;
}
}
else 
{
rsp_status = 0;
goto label_874137;
}
}
}
}
}
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
label_874735:; 
label_877075:; 
if (((int)m_run_st) == 0)
{
goto label_877089;
}
else 
{
if (((int)s_run_st) == 0)
{
label_877089:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_877100;
}
else 
{
label_877100:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_877567;
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
goto label_877227;
}
else 
{
goto label_877168;
}
}
else 
{
label_877168:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_877227;
}
else 
{
goto label_877175;
}
}
else 
{
label_877175:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_877227;
}
else 
{
goto label_877182;
}
}
else 
{
label_877182:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_877227;
}
else 
{
goto label_877189;
}
}
else 
{
label_877189:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_877227;
}
else 
{
goto label_877196;
}
}
else 
{
label_877196:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_877205;
}
else 
{
label_877205:; 
goto label_877227;
}
}
else 
{
label_877227:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_877249;
}
else 
{
goto label_877234;
}
}
else 
{
label_877234:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_877243;
}
else 
{
label_877243:; 
goto label_877249;
}
}
else 
{
label_877249:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_56 = req_a;
int i = __tmp_56;
int x;
if (i == 0)
{
x = s_memory0;
 __return_877299 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_877300 = x;
}
rsp_d = __return_877299;
goto label_877302;
rsp_d = __return_877300;
label_877302:; 
rsp_status = 1;
label_877308:; 
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
goto label_877389;
}
else 
{
goto label_877330;
}
}
else 
{
label_877330:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_877389;
}
else 
{
goto label_877337;
}
}
else 
{
label_877337:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_877389;
}
else 
{
goto label_877344;
}
}
else 
{
label_877344:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_877389;
}
else 
{
goto label_877351;
}
}
else 
{
label_877351:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_877389;
}
else 
{
goto label_877358;
}
}
else 
{
label_877358:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_877367;
}
else 
{
label_877367:; 
goto label_877389;
}
}
else 
{
label_877389:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_877411;
}
else 
{
goto label_877396;
}
}
else 
{
label_877396:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_877405;
}
else 
{
label_877405:; 
goto label_877411;
}
}
else 
{
label_877411:; 
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
goto label_877494;
}
else 
{
goto label_877435;
}
}
else 
{
label_877435:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_877494;
}
else 
{
goto label_877442;
}
}
else 
{
label_877442:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_877494;
}
else 
{
goto label_877449;
}
}
else 
{
label_877449:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_877494;
}
else 
{
goto label_877456;
}
}
else 
{
label_877456:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_877494;
}
else 
{
goto label_877463;
}
}
else 
{
label_877463:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_877472;
}
else 
{
label_877472:; 
goto label_877494;
}
}
else 
{
label_877494:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_877501;
}
}
else 
{
label_877501:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_877510;
}
else 
{
label_877510:; 
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
goto label_874393;
}
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
int __tmp_57 = req_a;
int __tmp_58 = req_d;
int i = __tmp_57;
int v = __tmp_58;
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
goto label_877277;
label_877277:; 
rsp_status = 1;
goto label_877308;
}
}
else 
{
rsp_status = 0;
goto label_877308;
}
}
}
}
}
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
label_877567:; 
goto label_877075;
}
}
}
else 
{
}
goto label_855971;
}
}
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
}
label_873433:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_874737;
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
goto label_874513;
}
else 
{
goto label_874454;
}
}
else 
{
label_874454:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_874513;
}
else 
{
goto label_874461;
}
}
else 
{
label_874461:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_874513;
}
else 
{
goto label_874468;
}
}
else 
{
label_874468:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_874513;
}
else 
{
goto label_874475;
}
}
else 
{
label_874475:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_874513;
}
else 
{
goto label_874482;
}
}
else 
{
label_874482:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_874491;
}
else 
{
label_874491:; 
goto label_874513;
}
}
else 
{
label_874513:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_874520;
}
}
else 
{
label_874520:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_874529;
}
else 
{
label_874529:; 
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
label_874559:; 
label_874743:; 
if (((int)m_run_st) == 0)
{
goto label_874757;
}
else 
{
if (((int)s_run_st) == 0)
{
label_874757:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_874990;
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
goto label_874945;
}
else 
{
goto label_874886;
}
}
else 
{
label_874886:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_874945;
}
else 
{
goto label_874893;
}
}
else 
{
label_874893:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_874945;
}
else 
{
goto label_874900;
}
}
else 
{
label_874900:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_874945;
}
else 
{
goto label_874907;
}
}
else 
{
label_874907:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_874945;
}
else 
{
goto label_874914;
}
}
else 
{
label_874914:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_874923;
}
else 
{
label_874923:; 
goto label_874945;
}
}
else 
{
label_874945:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_874952;
}
}
else 
{
label_874952:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_874961;
}
else 
{
label_874961:; 
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
goto label_873433;
}
}
}
}
else 
{
label_874990:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_875166;
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
goto label_875117;
}
else 
{
goto label_875058;
}
}
else 
{
label_875058:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_875117;
}
else 
{
goto label_875065;
}
}
else 
{
label_875065:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_875117;
}
else 
{
goto label_875072;
}
}
else 
{
label_875072:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_875117;
}
else 
{
goto label_875079;
}
}
else 
{
label_875079:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_875117;
}
else 
{
goto label_875086;
}
}
else 
{
label_875086:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_875095;
}
else 
{
label_875095:; 
goto label_875117;
}
}
else 
{
label_875117:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_875124;
}
}
else 
{
label_875124:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_875133;
}
else 
{
label_875133:; 
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
goto label_874559;
}
}
}
}
else 
{
label_875166:; 
goto label_874743;
}
}
}
else 
{
}
goto label_853639;
}
}
}
}
}
else 
{
label_874737:; 
goto label_874743;
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_873448:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_874739;
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
goto label_874679;
}
else 
{
goto label_874620;
}
}
else 
{
label_874620:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_874679;
}
else 
{
goto label_874627;
}
}
else 
{
label_874627:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_874679;
}
else 
{
goto label_874634;
}
}
else 
{
label_874634:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_874679;
}
else 
{
goto label_874641;
}
}
else 
{
label_874641:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_874679;
}
else 
{
goto label_874648;
}
}
else 
{
label_874648:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_874657;
}
else 
{
label_874657:; 
goto label_874679;
}
}
else 
{
label_874679:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_874686;
}
}
else 
{
label_874686:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_874695;
}
else 
{
label_874695:; 
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
goto label_871903;
}
}
}
}
else 
{
label_874739:; 
goto label_872370;
}
}
}
}
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
int __tmp_59 = req_a;
int __tmp_60 = req_d;
int i = __tmp_59;
int v = __tmp_60;
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
goto label_871616;
label_871616:; 
rsp_status = 1;
goto label_871647;
}
}
else 
{
rsp_status = 0;
goto label_871647;
}
}
}
}
}
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
label_872365:; 
goto label_877571;
}
}
}
}
}
else 
{
label_871431:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_872367;
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
goto label_872023;
}
else 
{
goto label_871964;
}
}
else 
{
label_871964:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_872023;
}
else 
{
goto label_871971;
}
}
else 
{
label_871971:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_872023;
}
else 
{
goto label_871978;
}
}
else 
{
label_871978:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_872023;
}
else 
{
goto label_871985;
}
}
else 
{
label_871985:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_872023;
}
else 
{
goto label_871992;
}
}
else 
{
label_871992:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_872001;
}
else 
{
label_872001:; 
goto label_872023;
}
}
else 
{
label_872023:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_872045;
}
else 
{
goto label_872030;
}
}
else 
{
label_872030:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_872039;
}
else 
{
label_872039:; 
goto label_872045;
}
}
else 
{
label_872045:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_61 = req_a;
int i = __tmp_61;
int x;
if (i == 0)
{
x = s_memory0;
 __return_872095 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_872096 = x;
}
rsp_d = __return_872095;
goto label_872098;
rsp_d = __return_872096;
label_872098:; 
rsp_status = 1;
label_872104:; 
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
goto label_872185;
}
else 
{
goto label_872126;
}
}
else 
{
label_872126:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_872185;
}
else 
{
goto label_872133;
}
}
else 
{
label_872133:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_872185;
}
else 
{
goto label_872140;
}
}
else 
{
label_872140:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_872185;
}
else 
{
goto label_872147;
}
}
else 
{
label_872147:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_872185;
}
else 
{
goto label_872154;
}
}
else 
{
label_872154:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_872163;
}
else 
{
label_872163:; 
goto label_872185;
}
}
else 
{
label_872185:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_872207;
}
else 
{
goto label_872192;
}
}
else 
{
label_872192:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_872201;
}
else 
{
label_872201:; 
goto label_872207;
}
}
else 
{
label_872207:; 
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
goto label_872290;
}
else 
{
goto label_872231;
}
}
else 
{
label_872231:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_872290;
}
else 
{
goto label_872238;
}
}
else 
{
label_872238:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_872290;
}
else 
{
goto label_872245;
}
}
else 
{
label_872245:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_872290;
}
else 
{
goto label_872252;
}
}
else 
{
label_872252:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_872290;
}
else 
{
goto label_872259;
}
}
else 
{
label_872259:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_872268;
}
else 
{
label_872268:; 
goto label_872290;
}
}
else 
{
label_872290:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_872297;
}
}
else 
{
label_872297:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_872306;
}
else 
{
label_872306:; 
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
goto label_871903;
}
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
int __tmp_62 = req_a;
int __tmp_63 = req_d;
int i = __tmp_62;
int v = __tmp_63;
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
goto label_872073;
label_872073:; 
rsp_status = 1;
goto label_872104;
}
}
else 
{
rsp_status = 0;
goto label_872104;
}
}
}
}
}
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
label_872367:; 
goto label_871090;
}
}
}
else 
{
}
goto label_851034;
}
}
}
else 
{
}
label_858135:; 
__retres1 = 0;
 __return_878716 = __retres1;
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
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_851015;
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
goto label_850341;
}
else 
{
goto label_850282;
}
}
else 
{
label_850282:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_850341;
}
else 
{
goto label_850289;
}
}
else 
{
label_850289:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_850341;
}
else 
{
goto label_850296;
}
}
else 
{
label_850296:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_850341;
}
else 
{
goto label_850303;
}
}
else 
{
label_850303:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_850341;
}
else 
{
goto label_850310;
}
}
else 
{
label_850310:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_850319;
}
else 
{
label_850319:; 
goto label_850341;
}
}
else 
{
label_850341:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_850348;
}
}
else 
{
label_850348:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_850357;
}
else 
{
label_850357:; 
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
goto label_849062;
}
}
}
}
else 
{
label_851015:; 
label_851519:; 
if (((int)m_run_st) == 0)
{
goto label_851533;
}
else 
{
if (((int)s_run_st) == 0)
{
label_851533:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_851544;
}
else 
{
label_851544:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_851720;
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
goto label_851671;
}
else 
{
goto label_851612;
}
}
else 
{
label_851612:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_851671;
}
else 
{
goto label_851619;
}
}
else 
{
label_851619:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_851671;
}
else 
{
goto label_851626;
}
}
else 
{
label_851626:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_851671;
}
else 
{
goto label_851633;
}
}
else 
{
label_851633:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_851671;
}
else 
{
goto label_851640;
}
}
else 
{
label_851640:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_851649;
}
else 
{
label_851649:; 
goto label_851671;
}
}
else 
{
label_851671:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_851678;
}
}
else 
{
label_851678:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_851687;
}
else 
{
label_851687:; 
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
goto label_849062;
}
}
}
}
else 
{
label_851720:; 
goto label_851519;
}
}
}
else 
{
}
label_851530:; 
kernel_st = 2;
kernel_st = 3;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_857833;
}
else 
{
goto label_857243;
}
}
else 
{
label_857243:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_857833;
}
else 
{
goto label_857307;
}
}
else 
{
label_857307:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_857833;
}
else 
{
goto label_857383;
}
}
else 
{
label_857383:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_857833;
}
else 
{
goto label_857447;
}
}
else 
{
label_857447:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_857833;
}
else 
{
goto label_857523;
}
}
else 
{
label_857523:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_857613;
}
else 
{
label_857613:; 
goto label_857833;
}
}
else 
{
label_857833:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_858047;
}
else 
{
goto label_857897;
}
}
else 
{
label_857897:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_857987;
}
else 
{
label_857987:; 
goto label_858047;
}
}
else 
{
label_858047:; 
if (((int)m_run_st) == 0)
{
goto label_858167;
}
else 
{
if (((int)s_run_st) == 0)
{
label_858167:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
label_870561:; 
if (((int)m_run_st) == 0)
{
goto label_870575;
}
else 
{
if (((int)s_run_st) == 0)
{
label_870575:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_870713;
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
goto label_871066;
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
goto label_870848;
}
else 
{
goto label_870789;
}
}
else 
{
label_870789:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_870848;
}
else 
{
goto label_870796;
}
}
else 
{
label_870796:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_870848;
}
else 
{
goto label_870803;
}
}
else 
{
label_870803:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_870848;
}
else 
{
goto label_870810;
}
}
else 
{
label_870810:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_870848;
}
else 
{
goto label_870817;
}
}
else 
{
label_870817:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_870826;
}
else 
{
label_870826:; 
goto label_870848;
}
}
else 
{
label_870848:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_870855;
}
}
else 
{
label_870855:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_870864;
}
else 
{
label_870864:; 
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
goto label_870561;
}
}
}
}
else 
{
label_871066:; 
goto label_870561;
}
}
}
}
else 
{
label_870713:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_871068;
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
goto label_871014;
}
else 
{
goto label_870955;
}
}
else 
{
label_870955:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_871014;
}
else 
{
goto label_870962;
}
}
else 
{
label_870962:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_871014;
}
else 
{
goto label_870969;
}
}
else 
{
label_870969:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_871014;
}
else 
{
goto label_870976;
}
}
else 
{
label_870976:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_871014;
}
else 
{
goto label_870983;
}
}
else 
{
label_870983:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_870992;
}
else 
{
label_870992:; 
goto label_871014;
}
}
else 
{
label_871014:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_871021;
}
}
else 
{
label_871021:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_871030;
}
else 
{
label_871030:; 
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
goto label_870561;
}
}
}
}
else 
{
label_871068:; 
goto label_870561;
}
}
}
else 
{
}
goto label_851530;
}
}
}
else 
{
}
goto label_858131;
}
}
}
}
}
}
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
label_850198:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_851019;
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
goto label_850964;
}
else 
{
goto label_850905;
}
}
else 
{
label_850905:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_850964;
}
else 
{
goto label_850912;
}
}
else 
{
label_850912:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_850964;
}
else 
{
goto label_850919;
}
}
else 
{
label_850919:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_850964;
}
else 
{
goto label_850926;
}
}
else 
{
label_850926:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_850964;
}
else 
{
goto label_850933;
}
}
else 
{
label_850933:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_850942;
}
else 
{
label_850942:; 
goto label_850964;
}
}
else 
{
label_850964:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_850971;
}
}
else 
{
label_850971:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_850980;
}
else 
{
label_850980:; 
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
goto label_849685;
}
}
}
}
else 
{
label_851019:; 
goto label_849702;
}
}
}
else 
{
}
label_849713:; 
kernel_st = 2;
kernel_st = 3;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_857837;
}
else 
{
goto label_857247;
}
}
else 
{
label_857247:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_857837;
}
else 
{
goto label_857303;
}
}
else 
{
label_857303:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_857837;
}
else 
{
goto label_857387;
}
}
else 
{
label_857387:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_857837;
}
else 
{
goto label_857443;
}
}
else 
{
label_857443:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_857837;
}
else 
{
goto label_857527;
}
}
else 
{
label_857527:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_857617;
}
else 
{
label_857617:; 
goto label_857837;
}
}
else 
{
label_857837:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_858043;
}
else 
{
goto label_857893;
}
}
else 
{
label_857893:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_857983;
}
else 
{
label_857983:; 
goto label_858043;
}
}
else 
{
label_858043:; 
if (((int)m_run_st) == 0)
{
goto label_858163;
}
else 
{
if (((int)s_run_st) == 0)
{
label_858163:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
label_878081:; 
if (((int)m_run_st) == 0)
{
goto label_878095;
}
else 
{
if (((int)s_run_st) == 0)
{
label_878095:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_878106;
}
else 
{
label_878106:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_878283;
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
goto label_878233;
}
else 
{
goto label_878174;
}
}
else 
{
label_878174:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_878233;
}
else 
{
goto label_878181;
}
}
else 
{
label_878181:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_878233;
}
else 
{
goto label_878188;
}
}
else 
{
label_878188:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_878233;
}
else 
{
goto label_878195;
}
}
else 
{
label_878195:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_878233;
}
else 
{
goto label_878202;
}
}
else 
{
label_878202:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_878211;
}
else 
{
label_878211:; 
goto label_878233;
}
}
else 
{
label_878233:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_878240;
}
}
else 
{
label_878240:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_878249;
}
else 
{
label_878249:; 
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
goto label_878081;
}
}
}
}
else 
{
label_878283:; 
goto label_878081;
}
}
}
else 
{
}
goto label_849713;
}
}
}
else 
{
}
goto label_858131;
}
}
}
}
}
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
label_849697:; 
goto label_848377;
}
}
}
else 
{
}
label_848388:; 
kernel_st = 2;
kernel_st = 3;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_857839;
}
else 
{
goto label_857249;
}
}
else 
{
label_857249:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_857839;
}
else 
{
goto label_857301;
}
}
else 
{
label_857301:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_857839;
}
else 
{
goto label_857389;
}
}
else 
{
label_857389:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_857839;
}
else 
{
goto label_857441;
}
}
else 
{
label_857441:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_857839;
}
else 
{
goto label_857529;
}
}
else 
{
label_857529:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_857619;
}
else 
{
label_857619:; 
goto label_857839;
}
}
else 
{
label_857839:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_858041;
}
else 
{
goto label_857891;
}
}
else 
{
label_857891:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_857981;
}
else 
{
label_857981:; 
goto label_858041;
}
}
else 
{
label_858041:; 
if (((int)m_run_st) == 0)
{
goto label_858161;
}
else 
{
if (((int)s_run_st) == 0)
{
label_858161:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
label_878300:; 
if (((int)m_run_st) == 0)
{
goto label_878314;
}
else 
{
if (((int)s_run_st) == 0)
{
label_878314:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_878325;
}
else 
{
label_878325:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_878502;
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
goto label_878370;
}
else 
{
label_878370:; 
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
goto label_878452;
}
else 
{
goto label_878393;
}
}
else 
{
label_878393:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_878452;
}
else 
{
goto label_878400;
}
}
else 
{
label_878400:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_878452;
}
else 
{
goto label_878407;
}
}
else 
{
label_878407:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_878452;
}
else 
{
goto label_878414;
}
}
else 
{
label_878414:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_878452;
}
else 
{
goto label_878421;
}
}
else 
{
label_878421:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_878430;
}
else 
{
label_878430:; 
goto label_878452;
}
}
else 
{
label_878452:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_878459;
}
}
else 
{
label_878459:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_878468;
}
else 
{
label_878468:; 
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
label_878498:; 
label_878505:; 
if (((int)m_run_st) == 0)
{
goto label_878519;
}
else 
{
if (((int)s_run_st) == 0)
{
label_878519:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_878530;
}
else 
{
label_878530:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_878706;
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
goto label_878657;
}
else 
{
goto label_878598;
}
}
else 
{
label_878598:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_878657;
}
else 
{
goto label_878605;
}
}
else 
{
label_878605:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_878657;
}
else 
{
goto label_878612;
}
}
else 
{
label_878612:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_878657;
}
else 
{
goto label_878619;
}
}
else 
{
label_878619:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_878657;
}
else 
{
goto label_878626;
}
}
else 
{
label_878626:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_878635;
}
else 
{
label_878635:; 
goto label_878657;
}
}
else 
{
label_878657:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_878664;
}
}
else 
{
label_878664:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_878673;
}
else 
{
label_878673:; 
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
goto label_878498;
}
}
}
}
else 
{
label_878706:; 
goto label_878505;
}
}
}
else 
{
}
goto label_849713;
}
}
}
}
}
else 
{
label_878502:; 
goto label_878300;
}
}
}
else 
{
}
goto label_848388;
}
}
}
else 
{
}
label_858131:; 
__retres1 = 0;
 __return_878714 = __retres1;
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
