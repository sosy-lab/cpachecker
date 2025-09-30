#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

extern void abort(void);
#include <assert.h>
void reach_error() { assert(0); }
int __VERIFIER_nondet_int(void);
void ldv_assert(int expression) { if (!expression) { ERROR: {reach_error();abort();}}; return; }

pthread_t t1, t2;
pthread_mutex_t mutex;
int pdev;

void ath9k_flush(void) {
   pthread_mutex_lock(&mutex);
   pdev = 6;
   ldv_assert(pdev==6);
   pthread_mutex_unlock(&mutex);
}

//[[thread ath9k]]
void *thread_ath9k(void *arg) {
    while(1) {
      switch(__VERIFIER_nondet_int()) {
	      case 1:
        	//depend on ieee80211_register_hw which enables it
	        ath9k_flush();
	        break;
	      case 2:
                  goto exit_thread_ath9k;
      }
    }
exit_thread_ath9k:
    return 0;
}

int ieee80211_register_hw(void) {
   if(__VERIFIER_nondet_int()) {
      pthread_create(&t2, NULL, thread_ath9k, NULL);
      return 0;
   }
   return -1;
}

void ieee80211_deregister_hw(void) {
   void *status;
   pthread_join(t2, &status);
   return;
}

static int ath_ahb_probe(void)
{
        int error;
        /* Register with mac80211 */
        error = ieee80211_register_hw();
        if (error)
                goto rx_cleanup;
        return 0;
rx_cleanup:
        return -1;
}

void ath_ahb_disconnect() {
        ieee80211_deregister_hw();
}

//thread local variables
int ldv_usb_state;

//[[thread usb]]
void *thread_usb(void *arg) {
    ldv_usb_state = 0;
    int probe_ret;
    while(1) {
      switch(__VERIFIER_nondet_int()) {
		case 0:
                if(ldv_usb_state==0) {
		  probe_ret = ath_ahb_probe();
		  if(probe_ret!=0)
		    goto exit_thread_usb;
                  ldv_usb_state = 1;
		  //race
		  //pdev = 7;
		  //ldv_assert(pdev==7);
                }
		break;
		case 1:
                if(ldv_usb_state==1) {
                   ath_ahb_disconnect();
		   ldv_usb_state=0;
		   //not a race
		   pdev = 8;
		   ldv_assert(pdev==8);
                }
		break;
		case 2:
                if(ldv_usb_state==0) {
                  goto exit_thread_usb;
		}
		break;
      }    
    }
exit_thread_usb: 
    //not a race
    pdev = 9;
    ldv_assert(pdev==9);
    return 0;
}


int module_init() {
   pthread_mutex_init(&mutex, NULL);
   //not a race
   pdev = 1;
   ldv_assert(pdev==1);
   if(__VERIFIER_nondet_int()) {
      pthread_create(&t1, NULL, thread_usb, NULL);
      return 0;
   }
   //not a race
   pdev = 3;
   ldv_assert(pdev==3);
   pthread_mutex_destroy(&mutex);
   return -1;
}

void module_exit() {
   void *status;
   //race
   //pdev = 4;
   //ldv_assert(pdev==4);
   pthread_join(t1, &status);
   pthread_mutex_destroy(&mutex);
   //not a race
   pdev = 5;
   ldv_assert(pdev==5);
}

int main(void) {
    if(module_init()!=0) goto module_exit;
    module_exit();
module_exit:
    return 0;
}
