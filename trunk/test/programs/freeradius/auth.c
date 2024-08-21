// SPDX-FileCopyrightText: 2000 Jeff Carneal <jeff@apex.net>
// SPDX-FileCopyrightText: 2000 Miquel van Smoorenburg <miquels@cistron.nl>
// SPDX-FileCopyrightText: 2000 The FreeRADIUS server project
//
// SPDX-License-Identifier: GPL-2.0-or-later

/*
 * auth.c	User authentication.
 */

static const char rcsid[] = "$Id$";

#include "autoconf.h"

#include <stdlib.h>
#include <string.h>
#include <ctype.h>

#ifdef HAVE_NETINET_IN_H
#	include <netinet/in.h>
#endif

#include "radiusd.h"
#include "modules.h"
#include "rad_assert.h"

/*
 *	Return a short string showing the terminal server, port
 *	and calling station ID.
 */
char *auth_name(char *buf, size_t buflen, REQUEST *request, int do_cli) {
	VALUE_PAIR	*cli;
	VALUE_PAIR	*pair;
	int		port = 0;

	if ((cli = pairfind(request->packet->vps, PW_CALLING_STATION_ID)) == NULL)
		do_cli = 0;
	if ((pair = pairfind(request->packet->vps, PW_NAS_PORT)) != NULL)
		port = pair->lvalue;

	snprintf(buf, buflen, "from client %.128s port %u%s%.128s",
			client_name_old(&request->packet->src_ipaddr), port,
			(do_cli ? " cli " : ""), (do_cli ? (char *)cli->strvalue : ""));

	return buf;
}



/*
 * Make sure user/pass are clean
 * and then log them
 */
static int rad_authlog(const char *msg, REQUEST *request, int goodpass) {

	char clean_password[1024];
	char clean_username[1024];
	char buf[1024];
	VALUE_PAIR *username = NULL;

	if (!mainconfig.log_auth) {
		return 0;
	}

	/*
	 * Get the correct username based on the configured value
	 */
	if (log_stripped_names == 0) {
		username = pairfind(request->packet->vps, PW_USER_NAME);
	} else {
		username = request->username;
	}

	/*
	 *	Clean up the username
	 */
	if (username == NULL) {
		strcpy(clean_username, "<no User-Name attribute>");
	} else {
		librad_safeprint((char *)username->strvalue,
				username->length,
				clean_username, sizeof(clean_username));
	}

	/*
	 *	Clean up the password
	 */
	if (mainconfig.log_auth_badpass || mainconfig.log_auth_goodpass) {
		if (!request->password) {
			VALUE_PAIR *auth_type;

			auth_type = pairfind(request->config_items,
					     PW_AUTH_TYPE);
			if (auth_type && (auth_type->strvalue[0] != '\0')) {
				snprintf(clean_password, sizeof(clean_password),
					 "<via Auth-Type = %s>",
					 auth_type->strvalue);
			} else {
				strcpy(clean_password, "<no User-Password attribute>");
			}
		} else if (request->password->attribute == PW_CHAP_PASSWORD) {
			strcpy(clean_password, "<CHAP-Password>");
		} else {
			librad_safeprint((char *)request->password->strvalue,
					 request->password->length,
					 clean_password, sizeof(clean_password));
		}
	}

	if (goodpass) {
		radlog(L_AUTH, "%s: [%s%s%s] (%s)",
				msg,
				clean_username,
				mainconfig.log_auth_goodpass ? "/" : "",
				mainconfig.log_auth_goodpass ? clean_password : "",
				auth_name(buf, sizeof(buf), request, 1));
	} else {
		radlog(L_AUTH, "%s: [%s%s%s] (%s)",
				msg,
				clean_username,
				mainconfig.log_auth_badpass ? "/" : "",
				mainconfig.log_auth_badpass ? clean_password : "",
				auth_name(buf, sizeof(buf), request, 1));
	}

	return 0;
}

/*
 *	Check password.
 *
 *	Returns:	0  OK
 *			-1 Password fail
 *			-2 Rejected (Auth-Type = Reject, send Port-Message back)
 *			1  End check & return, don't reply
 *
 *	NOTE: NOT the same as the RLM_ values !
 */
static int rad_check_password(REQUEST *request)
{
	VALUE_PAIR *auth_type_pair;
	VALUE_PAIR *cur_config_item;
	VALUE_PAIR *password_pair;
	VALUE_PAIR *auth_item;
  DICT_VALUE *da;
	char string[MAX_STRING_LEN];
	int auth_type = -1;
	int result;
	int auth_type_count = 0;
	result = 0;

	/*
	 *	Look for matching check items. We skip the whole lot
	 *	if the authentication type is PW_AUTHTYPE_ACCEPT or
	 *	PW_AUTHTYPE_REJECT.
	 */
	cur_config_item = request->config_items;
	while(((auth_type_pair = pairfind(cur_config_item, PW_AUTH_TYPE))) != NULL) {
		auth_type = auth_type_pair->lvalue;
		auth_type_count++;

		DEBUG2("  rad_check_password:  Found Auth-Type %s",
				auth_type_pair->strvalue);
		cur_config_item = auth_type_pair->next;

		if (auth_type == PW_AUTHTYPE_REJECT) {
			DEBUG2("  rad_check_password: Auth-Type = Reject, rejecting user");
			return -2;
		}
	}

	if (( auth_type_count > 1) && (debug_flag)) {
		radlog(L_ERR, "Warning:  Found %d auth-types on request for user '%s'",
			auth_type_count, request->username->strvalue);
	}

	/*
	 *	This means we have a proxy reply or an accept
	 *  and it wasn't rejected in the above loop.  So
	 *  that means it is accepted and we do no further
	 *  authentication
	 */
	if ((auth_type == PW_AUTHTYPE_ACCEPT) || (request->proxy)) {
		DEBUG2("  rad_check_password: Auth-Type = Accept, accepting the user");
		return 0;
	}

	/*
	 *	Find the password from the users file.
	 */
	if ((password_pair = pairfind(request->config_items, PW_CRYPT_PASSWORD)) != NULL) {
		/*
		 *	Re-write Auth-Type, but ONLY if it isn't already
		 *	set.
		 */
		if (auth_type == -1) auth_type = PW_AUTHTYPE_CRYPT;
	} else {
		password_pair = pairfind(request->config_items, PW_PASSWORD);
	}

	if (auth_type < 0) {
		if (password_pair) {
			auth_type = PW_AUTHTYPE_LOCAL;
		} else {
			/*
		 	*	The admin hasn't told us how to
		 	*	authenticate the user, so we reject them!
		 	*
		 	*	This is fail-safe.
		 	*/
			DEBUG2("auth: No authenticate method (Auth-Type) configuration found for the request: Rejecting the user");
			return -2;
		}
	}

	switch(auth_type) {
		case PW_AUTHTYPE_CRYPT:
			/*
			 *	Find the password sent by the user. It
			 *	SHOULD be there, if it's not
			 *	authentication fails.
			 */
			auth_item = request->password;
			if (auth_item == NULL) {
				DEBUG2("auth: No User-Password or CHAP-Password attribute in the request");
				return -1;
			}

			DEBUG2("auth: type Crypt");
			if (password_pair == NULL) {
				DEBUG2("No Crypt-Password configured for the user");
				rad_authlog("Login incorrect "
					"(No Crypt-Password configured for the user)", request, 0);
				return -1;
			}

			switch (lrad_crypt_check((char *)auth_item->strvalue,
									 (char *)password_pair->strvalue)) {
			case -1:
			  rad_authlog("Login incorrect "
						  "(system failed to supply an encrypted password for comparison)", request, 0);
			case 1:
			  return -1;
			}
			break;
		case PW_AUTHTYPE_LOCAL:
			DEBUG2("auth: type Local");

			/*
			 *	Find the password sent by the user. It
			 *	SHOULD be there, if it's not
			 *	authentication fails.
			 */
			auth_item = request->password;
			if (auth_item == NULL) {
				DEBUG2("auth: No User-Password or CHAP-Password attribute in the request");
				return -1;
			}

			/*
			 *	Plain text password.
			 */
			if (password_pair == NULL) {
				DEBUG2("auth: No password configured for the user");
				rad_authlog("Login incorrect "
					"(No password configured for the user)", request, 0);
				return -1;
			}

			/*
			 *	Local password is just plain text.
	 		 */
			if (auth_item->attribute == PW_PASSWORD) {
				if (strcmp((char *)password_pair->strvalue,
					   (char *)auth_item->strvalue) != 0) {
					DEBUG2("auth: user supplied User-Password does NOT match local User-Password");
					return -1;
				}
				DEBUG2("auth: user supplied User-Password matches local User-Password");
				break;

			} else if (auth_item->attribute != PW_CHAP_PASSWORD) {
				DEBUG2("The user did not supply a User-Password or a CHAP-Password attribute");
				rad_authlog("Login incorrect "
					"(no User-Password or CHAP-Password attribute)", request, 0);
				return -1;
			}

			rad_chap_encode(request->packet, string,
					auth_item->strvalue[0], password_pair);

			/*
			 *	Compare them
			 */
			if (memcmp(string + 1, auth_item->strvalue + 1,
				   CHAP_VALUE_LENGTH) != 0) {
				DEBUG2("auth: user supplied CHAP-Password does NOT match local User-Password");
				return -1;
			}
			DEBUG2("auth: user supplied CHAP-Password matches local User-Password");
			break;
		default:
      da = dict_valbyattr(PW_AUTH_TYPE, auth_type);
			DEBUG2("auth: type \"%s\"", da->name);
			/*
			 *	See if there is a module that handles
			 *	this type, and turn the RLM_ return
			 *	status into the values as defined at
			 *	the top of this function.
			 */
			result = module_authenticate(auth_type, request);
			switch (result) {
				/*
				 *	An authentication module FAIL
				 *	return code, or any return code that
				 *	is not expected from authentication,
				 *	is the same as an explicit REJECT!
				 */
				case RLM_MODULE_FAIL:
				case RLM_MODULE_REJECT:
				case RLM_MODULE_USERLOCK:
				case RLM_MODULE_INVALID:
				case RLM_MODULE_NOTFOUND:
				case RLM_MODULE_NOOP:
				case RLM_MODULE_UPDATED:
					result = -1;
					break;
				case RLM_MODULE_OK:
					result = 0;
					break;
				case RLM_MODULE_HANDLED:
					result = 1;
					break;
			}
			break;
	}

	return result;
}

/*
 *	Post-authentication step processes the response before it is
 *	sent to the NAS. It can receive both Access-Accept and Access-Reject
 *	replies.
 */
int rad_postauth(REQUEST *request)
{
	int	result;
	int	postauth_type = 0;
	VALUE_PAIR	*postauth_type_item = NULL;

	/*
	 *	Do post-authentication calls. ignoring the return code.
	 */
	postauth_type_item = pairfind(request->config_items, PW_POST_AUTH_TYPE);
	if (postauth_type_item)
		postauth_type = postauth_type_item->lvalue;
	result = module_post_auth(postauth_type, request);
	switch (result) {
	default:
	  break;

	  /*
	   *	The module failed, or said to reject the user: Do so.
	   */
	case RLM_MODULE_FAIL:
	case RLM_MODULE_REJECT:
	case RLM_MODULE_USERLOCK:
	case RLM_MODULE_INVALID:
	  request->reply->code = PW_AUTHENTICATION_REJECT;
	  result = RLM_MODULE_REJECT;
	  break;

	  /*
	   *	The module had a number of OK return codes.
	   */
	case RLM_MODULE_NOTFOUND:
	case RLM_MODULE_NOOP:
	case RLM_MODULE_UPDATED:
	case RLM_MODULE_OK:
	case RLM_MODULE_HANDLED:
	  result = RLM_MODULE_OK;
	  break;
	}
	return result;
}

/*
 *	Before sending an Access-Reject, call the modules in the
 *	Post-Auth-Type REJECT stanza.
 */
static int rad_postauth_reject(REQUEST *request)
{
	int		result;
	VALUE_PAIR	*tmp;
	DICT_VALUE	*dval;

	dval = dict_valbyname(PW_POST_AUTH_TYPE, "REJECT");
	if (dval) {
		/* Overwrite the Post-Auth-Type with the value REJECT */
		pairdelete(&request->config_items, PW_POST_AUTH_TYPE);
		tmp = paircreate(PW_POST_AUTH_TYPE, PW_TYPE_INTEGER);
		tmp->lvalue = dval->value;
		pairadd(&request->config_items, tmp);
		result = rad_postauth(request);
	} else {
		/* No REJECT stanza */
		result = RLM_MODULE_OK;
	}
	return result;
}

/*
 *	Process and reply to an authentication request
 *
 *	The return value of this function isn't actually used right now, so
 *	it's not entirely clear if it is returning the right things. --Pac.
 */
int rad_authenticate(REQUEST *request)
{
	VALUE_PAIR	*namepair;
	VALUE_PAIR	*check_item;
	VALUE_PAIR	*auth_item;
	VALUE_PAIR	*module_msg;
	VALUE_PAIR	*tmp = NULL;
	int		result, r;
	char		umsg[MAX_STRING_LEN + 1];
	const char	*user_msg = NULL;
	const char	*password;
	char		logstr[1024];
	char		autz_retry = 0;
	int		autz_type = 0;

	password = "";

	/*
	 *	If this request got proxied to another server,
	 *	AND it was an authentication request, then we need
	 *	to add an initial Auth-Type: Auth-Accept for success,
	 *	Auth-Reject for fail. We also need to add the reply
	 *	pairs from the server to the initial reply.
	 *
	 *	Huh?  If the request wasn't an authentication request,
	 *	WTF are we doing here?
	 */
	if ((request->proxy_reply) &&
	    (request->packet->code == PW_AUTHENTICATION_REQUEST)) {
		tmp = paircreate(PW_AUTH_TYPE, PW_TYPE_INTEGER);
		if (tmp == NULL) {
			radlog(L_ERR|L_CONS, "no memory");
			exit(1);
		}

		/*
		 *	Challenges are punted back to the NAS
		 *	without any further processing.
		 */
		if (request->proxy_reply->code == PW_ACCESS_CHALLENGE) {
			request->reply->code = PW_ACCESS_CHALLENGE;
			return RLM_MODULE_HANDLED;
		}

		/*
		 *	Reply of ACCEPT means accept, ALL other
		 *	replies mean reject.  This is fail-safe.
		 */
		if (request->proxy_reply->code == PW_AUTHENTICATION_ACK)
			tmp->lvalue = PW_AUTHTYPE_ACCEPT;
		else
			tmp->lvalue = PW_AUTHTYPE_REJECT;
		pairadd(&request->config_items, tmp);

		/*
		 *	If it's an Access-Reject, then do NOT do any
		 *	authorization or authentication.  They're being
		 *	rejected, so we minimize the amount of work
		 *	done by the server, by rejecting them here.
		 */
		if ((request->proxy_reply->code != PW_AUTHENTICATION_ACK) &&
		    (request->proxy_reply->code != PW_ACCESS_CHALLENGE)) {
			rad_authlog("Login incorrect (Home Server says so)", request, 0);
			request->reply->code = PW_AUTHENTICATION_REJECT;
			rad_postauth_reject(request);
			return RLM_MODULE_REJECT;
		}
	}

	/*
	 *	Get the username from the request.
	 *
	 *	Note that namepair MAY be NULL, in which case there
	 *	is no User-Name attribute in the request.
	 */
	namepair = request->username;

	/*
	 *	Look for, and cache, passwords.
	 */
	if (!request->password) {
		request->password = pairfind(request->packet->vps,
					     PW_PASSWORD);
	}

	/*
	 *	Discover which password we want to use.
	 */
	auth_item = request->password;
	if (auth_item) {
		password = (const char *)auth_item->strvalue;

	} else {
		/*
		 *	Maybe there's a CHAP-Password?
		 */
		if ((auth_item = pairfind(request->packet->vps,
				PW_CHAP_PASSWORD)) != NULL) {
			password = "<CHAP-PASSWORD>";

		} else {
			/*
			 *	No password we recognize.
			 */
			password = "<NO-PASSWORD>";
		}
	}
	request->password = auth_item;

	/*
	 *	Get the user's authorization information from the database
	 */
autz_redo:
	r = module_authorize(autz_type, request);
	if (r != RLM_MODULE_NOTFOUND &&
	    r != RLM_MODULE_NOOP &&
	    r != RLM_MODULE_OK &&
	    r != RLM_MODULE_UPDATED) {
		if (r != RLM_MODULE_FAIL && r != RLM_MODULE_HANDLED) {
			if ((module_msg = pairfind(request->packet->vps,
					PW_MODULE_FAILURE_MESSAGE)) != NULL){
				char msg[MAX_STRING_LEN+16];
				snprintf(msg, sizeof(msg), "Invalid user (%s)",
					 module_msg->strvalue);
				rad_authlog(msg,request,0);
			} else {
				rad_authlog("Invalid user", request, 0);
			}
			request->reply->code = PW_AUTHENTICATION_REJECT;
		}
		return r;
	}
	if (!autz_retry){
		VALUE_PAIR	*autz_type_item = NULL;
		autz_type_item = pairfind(request->config_items, PW_AUTZ_TYPE);
		if (autz_type_item){
			autz_type = autz_type_item->lvalue;
			autz_retry = 1;
			goto autz_redo;
		}
	}

	/*
	 *	If we haven't already proxied the packet, then check
	 *	to see if we should.  Maybe one of the authorize
	 *	modules has decided that a proxy should be used. If
	 *	so, get out of here and send the packet.
	 */
	if ((request->proxy == NULL) &&
	    ((tmp = pairfind(request->config_items, PW_PROXY_TO_REALM)) != NULL)) {
		REALM *realm;

		/*
		 *	Catch users who set Proxy-To-Realm to a LOCAL
		 *	realm (sigh).
		 */
		realm = realm_find(tmp->strvalue, 0);
		rad_assert((realm == NULL) || (realm->ipaddr.af == AF_INET));
		if (realm && (realm->ipaddr.ipaddr.ip4addr.s_addr == htonl(INADDR_NONE))) {
			DEBUG2("  WARNING: You set Proxy-To-Realm = %s, but it is a LOCAL realm!  Cancelling invalid proxy request.", realm->realm);
		} else {
			/*
			 *	Don't authenticate, as the request is
			 *	proxied.
			 */
			return RLM_MODULE_OK;
		}
	}

	/*
	 *	Perhaps there is a Stripped-User-Name now.
	 */
	namepair = request->username;

	/*
	 *	Validate the user
	 */
	do {
		result = rad_check_password(request);
		if (result > 0) {
			/* don't reply! */
			return RLM_MODULE_HANDLED;
		}
	} while(0);

	/*
	 *	Failed to validate the user.
	 *
	 *	We PRESUME that the code which failed will clean up
	 *	request->reply->vps, to be ONLY the reply items it
	 *	wants to send back.
	 */
	if (result < 0) {
		DEBUG2("auth: Failed to validate the user.");
		request->reply->code = PW_AUTHENTICATION_REJECT;

		if ((module_msg = pairfind(request->packet->vps,PW_MODULE_FAILURE_MESSAGE)) != NULL){
			char msg[MAX_STRING_LEN+19];

			snprintf(msg, sizeof(msg), "Login incorrect (%s)",
				 module_msg->strvalue);
			rad_authlog(msg, request, 0);
		} else {
			rad_authlog("Login incorrect", request, 0);
		}

		/* double check: maybe the secret is wrong? */
		if ((debug_flag > 1) && (auth_item != NULL) &&
				(auth_item->attribute == PW_PASSWORD)) {
			u_char *p;

			p = auth_item->strvalue;
			while (*p != '\0') {
				if (!isprint((int) *p)) {
					log_debug("  WARNING: Unprintable characters in the password.\n\t  Double-check the shared secret on the server and the NAS!");
					break;
				}
				p++;
			}
		}
	}

	if (result >= 0 &&
	    (check_item = pairfind(request->config_items, PW_SIMULTANEOUS_USE)) != NULL) {
		VALUE_PAIR	*session_type;
		int		sess_type = 0;

		session_type = pairfind(request->config_items, PW_SESSION_TYPE);
		if (session_type)
			sess_type = session_type->lvalue;

		/*
		 *	User authenticated O.K. Now we have to check
		 *	for the Simultaneous-Use parameter.
		 */
		if (namepair &&
		    (r = module_checksimul(sess_type,request, check_item->lvalue)) != 0) {
			char mpp_ok = 0;

			if (r == 2){
				/* Multilink attempt. Check if port-limit > simultaneous-use */
				VALUE_PAIR *port_limit;

				if ((port_limit = pairfind(request->reply->vps, PW_PORT_LIMIT)) != NULL &&
					port_limit->lvalue > check_item->lvalue){
					DEBUG2("main auth: MPP is OK");
					mpp_ok = 1;
				}
			}
			if (!mpp_ok){
				if (check_item->lvalue > 1) {
		  		snprintf(umsg, sizeof(umsg),
							"\r\nYou are already logged in %d times  - access denied\r\n\n",
							(int)check_item->lvalue);
					user_msg = umsg;
				} else {
					user_msg = "\r\nYou are already logged in - access denied\r\n\n";
				}

				request->reply->code = PW_AUTHENTICATION_REJECT;

				/*
				 *	They're trying to log in too many times.
				 *	Remove ALL reply attributes.
				 */
				pairfree(&request->reply->vps);
				tmp = pairmake("Reply-Message", user_msg, T_OP_SET);
				request->reply->vps = tmp;

				snprintf(logstr, sizeof(logstr), "Multiple logins (max %d) %s",
					check_item->lvalue,
					r == 2 ? "[MPP attempt]" : "");
				rad_authlog(logstr, request, 1);

				result = -1;
			}
		}
	}

	/*
	 *	Result should be >= 0 here - if not, it means the user
	 *	is rejected, so we just process post-auth and return.
	 */
	if (result < 0) {
		rad_postauth_reject(request);
		return RLM_MODULE_REJECT;
	}

	/*
	 *	We might need this later.  The 'password' string
	 *	is NOT used anywhere below here, except for logging,
	 *	so it should be safe...
	 */
	if ((auth_item != NULL) && (auth_item->attribute == PW_CHAP_PASSWORD)) {
		password = "CHAP-Password";
	}

	/*
	 *	Add the port number to the Framed-IP-Address if
	 *	vp->addport is set.
	 */
	if (((tmp = pairfind(request->reply->vps,
			     PW_FRAMED_IP_ADDRESS)) != NULL) &&
	    (tmp->flags.addport != 0)) {
		VALUE_PAIR *vpPortId;

		/*
		 *  Find the NAS port ID.
		 */
		if ((vpPortId = pairfind(request->packet->vps,
					 PW_NAS_PORT)) != NULL) {
		  unsigned long tvalue = ntohl(tmp->lvalue);
		  tmp->lvalue = htonl(tvalue + vpPortId->lvalue);
		  tmp->flags.addport = 0;
		  ip_ntoa(tmp->strvalue, tmp->lvalue);
		} else {
			DEBUG2("WARNING: No NAS-Port attribute in request.  CANNOT return a Framed-IP-Address + NAS-Port.\n");
			pairdelete(&request->reply->vps, PW_FRAMED_IP_ADDRESS);
		}
	}

	/*
	 *	Set the reply to Access-Accept, if it hasn't already
	 *	been set to something.  (i.e. Access-Challenge)
	 */
	if (request->reply->code == 0)
	  request->reply->code = PW_AUTHENTICATION_ACK;

	if ((module_msg = pairfind(request->packet->vps,PW_MODULE_SUCCESS_MESSAGE)) != NULL){
		char msg[MAX_STRING_LEN+12];

		snprintf(msg, sizeof(msg), "Login OK (%s)",
			 module_msg->strvalue);
		rad_authlog(msg, request, 1);
	} else {
		rad_authlog("Login OK", request, 1);
	}

	/*
	 *	Run the modules in the 'post-auth' section.
	 */
	result = rad_postauth(request);

	return result;
}
