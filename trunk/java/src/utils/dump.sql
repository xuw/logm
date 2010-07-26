
--select * from sampleLog;


--select * from logMethod where logid='outfox.omap.OmapMaster-545' order by threadid, seq;

--select * from premethodlog where logid='outfox.omap.OmapMaster-545';

--select distinct logid from logMethod where callerid=1017;

--select le.logid, le.inloop, md.name, md.methodid from logentries le, methoddesc md where md.methodid=le.methodId and logid like '%Client%';

--select * from callgraph where calleeid=2631;

--select * from methoddesc where name='Iter';

--select count(*) from logentries where logid like '%fs%' and level>=800;

--select count(*) from logentries where level>=800;

--select count(*) from logentries where level>=0;
select * from logentries where level>=0;
--select count(*) from logentries where level>=0 and regex!='(@#@)';


--maximumdisplaywidth 10000;
--select logid from normal_10clients where seq=13167;


--select * from tostringdb;

--select regexpr from dark_sample;

--update tostringdb set regEx='(.*)',nameMap='objaddr',typeMap='java.lang.String' where class='java.lang.Object';

--select * from tostringdb where class='java.lang.Object';

--select count(*) from logentries;

--select distinct(threadid) from namenode_ec2;
--select count(*) from jobtracker_ec2;

--select * from smalltest;
--select * from samplelog where logbody LIKE '[WARNING]%';

--create view tmp(logid, cnt) 
--as
--select s.logid as logid,count(*) as cnt
--from samplelog2 s 
--group by s.logid;
--
--select t.logid, t.cnt, l.level, l.regEx
--from tmp t left outer join logentries l on t.logid =l.logid
--order by t.cnt;
--
--drop view tmp;

--select distinct (logid) from logentries where level>800;\


--update tostringdb set regex='(.*):([-]?[0-9]+)' where class='outfox.omap.common.TsDesc';
--select * from tostringdb where  class='outfox.omap.common.TsDesc';

--select * from jobtracker_r10;
--select * from nn_10_251_202_133;

--call SYSCS_UTIL.SYSCS_EXPORT_TABLE(null, 'NN_10_251_202_133', 'c:/users/xuw/logExpData/ttt.del', null, null, null);

