drop view logMethod;

create view logMethod(threadid, seq, logid, callerid)
as 
select distinct sl.threadid,sl.seq, sl.logid, cg.callerid
from sampleLog sl, premethodlog pre, callgraph cg, methoddesc m
where sl.logid= pre.logid and pre.callid =cg.edgeid and cg.callerid = m.methodid;