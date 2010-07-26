drop table $$$TABLENAME$$$;

create table $$$TABLENAME$$$ (
	seq INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1) primary key,
	ts timestamp,
	threadid integer,
	logid varchar(255),
	logbody varchar(1024),
	textentry clob,
	lbs varchar(1024),
	dts varchar(1024)
);
