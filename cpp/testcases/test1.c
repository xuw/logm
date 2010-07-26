#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <signal.h>
static int ssl_sock_init(void)
{
# ifdef SIGINT
      signal(SIGINT,(void (*)(int))sock_cleanup);
#elif defined(OPENSSL_SYS_WINDOWS)
			printf("bbbbbb");
#else
			printf("aaaaa");
# endif


	return(1);
	}
