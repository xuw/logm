drop view pre;
drop view post;
drop view edgelist;
drop view logMethod;

drop table sampleLog;
drop table callcount;
drop table tostringsubclass;
drop table tostringdb;
drop table premethodlog;
drop table postmethodlog;
drop table logentries;
drop table methodoverride;
drop table callgraph;
drop table methoddesc;


create table methoddesc(
	methodid INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1) primary key,
	class varchar(1024) not null,
	name varchar(256) not null,
	params varchar(2048) not null,
	returntype varchar(1024),
	unique(class, name, params)
);

create table callgraph(
	edgeid INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1) primary key,
	callerid integer not null,
	calleeid integer not null,
	line integer not null,
	inloop integer not null,
	foreign key (callerid) references methodDesc(methodid),
	foreign key (calleeid) references methodDesc(methodid),
	unique(callerid, calleeid, line)
);


create table methodoverride(
	subclass integer not null,
	superclass integer not null,
	foreign key (subclass) references methodDesc(methodid),
	foreign key (superclass) references methodDesc(methodid)
);

create table logentries(
	logid varchar(100) not null,
	regEx varchar(4096) not null,
	nameMap varchar(4096) not null,
	typeMap varchar(4096) not null,
	constStr varchar(4096) not null,
	level integer not null,
	inLoop integer not null,
	methodId integer not null,
	primary key (logid)
);

create table premethodlog(
	logid varchar(100) not null,
	callid integer not null,
	foreign key (callid) references callgraph(edgeid),
	--foreign key (logid) references logentry(logid)
	primary key(logid, callid)
);

create table postmethodlog(
	logid varchar(100) not null,
	callid integer not null,
	foreign key (callid) references callgraph(edgeid),
	--foreign key (logid) references logentry(logid)
	primary key(logid, callid)
);


create table tostringdb(
	class varchar(1000) not null,
	regEx varchar(4096) not null,
	nameMap varchar(4096) not null,
	typeMap varchar(4096) not null,
	parseStatus integer not null default 0,
	primary key(class)
);

create table tostringsubclass(
	superclass varchar(1000) not null,
	subclass varchar(1000) not null,
	foreign key (superclass) references tostringdb(class),
	unique(superclass, subclass)
);


create table callcount(
	edgeid integer not null,
	count integer not null default 0,
	foreign key (edgeid) references callgraph(edgeid),
	primary key(edgeid)
);

create table sampleLog (
	seq INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1) primary key,
	ts timestamp,
	threadid integer,
	logid varchar(50),
	logbody varchar(1024),
	textentry clob,
	lbs varchar(1024),
	dts varchar(1024)
);


create view pre(logid, methodid)
as
select max(p.logid) as prelog, p.callid as methodid from premethodlog p, logentries l
where p.logid = l.logid and l.level>=800
group by p.callid;

create view post(logid, methodid)
as
select min(p.logid) as postlog, p.callid as methodid from postmethodlog p, logentries l
where p.logid = l.logid and l.level>=800
group by p.callid;

create view edgelist(edgeid, caller, callee)
as
select c.edgeid as edgeid, ( m1.class || '.' || m1.name) as caller, ( m2.class || '.' || m2.name) as callee
from callgraph c, methoddesc m1, methoddesc m2
where c.callerid=m1.methodid and c.calleeid=m2.methodid;

