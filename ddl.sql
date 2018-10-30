file -inlinebatch END_DROP_BATCH

drop procedure GetUser IF EXISTS;
drop procedure UpsertUser IF EXISTS;
drop procedure RequestQuota IF EXISTS;
drop procedure InitUsage IF EXISTS;
drop table PRODUCT  if exists  cascade;
drop table USER if exists  cascade;
drop table USAGE if exists  cascade;
drop table BALANCE if exists  cascade;

END_DROP_BATCH

file -inlinebatch CREATE_BATCH

CREATE table PRODUCT (
	productid bigint not null primary key
	,unit_cost bigint not null
);

CREATE table USER (
	userid bigint not null primary key
	,user_blob varchar(8000)
);

CREATE table USAGE (
	userid bigint not null
	,productid bigint not null
	,allocated_units bigint not null
	,primary key (userid, productid)
);

CREATE table BALANCE (
	userid bigint not null
	,balance bigint not null
	,primary key (userid)
);

echo Partition above tables

PARTITION TABLE USER ON COLUMN userid;
PARTITION TABLE USAGE ON COLUMN userid;
PARTITION TABLE BALANCE ON COLUMN userid;

echo create procedures

CREATE_BATCH

load classes build/libs/procs-1.0.jar;

file -inlinebatch PROCS_BATCH

CREATE PROCEDURE 
   PARTITION ON TABLE USER COLUMN userid
   FROM CLASS com.example.GetUser;  

CREATE PROCEDURE 
   PARTITION ON TABLE USER COLUMN userid
   FROM CLASS com.example.UpsertUser;
   
CREATE PROCEDURE 
   PARTITION ON TABLE USER COLUMN userid
   FROM CLASS com.example.RequestQuota;  

CREATE PROCEDURE
   PARTITION ON TABLE USER COLUMN userid
   FROM CLASS com.example.InitUsage;
PROCS_BATCH