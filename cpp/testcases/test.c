#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <signal.h>
static int ssl_sock_init(void)
	{
#ifdef WATT32
	extern int _watt_do_exit;
	_watt_do_exit = 0;
	if (sock_init())
		return (0);
#elif defined(OPENSSL_SYS_WINDOWS)
	if (!wsa_init_done)
		{
		int err;
 	 
#ifdef SIGINT
		signal(SIGINT,(void (*)(int))ssl_sock_cleanup);
#endif
		wsa_init_done=1;
		memset(&wsa_state,0,sizeof(wsa_state));
		if (WSAStartup(0x0101,&wsa_state)!=0)
			{
			err=WSAGetLastError();
			BIO_printf(bio_err,"unable to start WINSOCK, error code=%d\n",err);
			return(0);
			}

#ifdef OPENSSL_SYS_WIN16
		EnumTaskWindows(GetCurrentTask(),enumproc,0L);
		lpTopWndProc=(FARPROC)GetWindowLong(topWnd,GWL_WNDPROC);
		lpTopHookProc=MakeProcInstance((FARPROC)topHookProc,_hInstance);

		SetWindowLong(topWnd,GWL_WNDPROC,(LONG)lpTopHookProc);
#endif /* OPENSSL_SYS_WIN16 */
		}
#elif defined(OPENSSL_SYS_NETWARE) && !defined(NETWARE_BSDSOCK)
   WORD wVerReq;
   WSADATA wsaData;
   int err;

   if (!wsa_init_done)
      {
   
# ifdef SIGINT
      signal(SIGINT,(void (*)(int))sock_cleanup);
# endif

      wsa_init_done=1;
      wVerReq = MAKEWORD( 2, 0 );
      err = WSAStartup(wVerReq,&wsaData);
      if (err != 0)
         {
         BIO_printf(bio_err,"unable to start WINSOCK2, error code=%d\n",err);
         return(0);
         }
      }
#endif /* OPENSSL_SYS_WINDOWS */
	return(1);
	}
