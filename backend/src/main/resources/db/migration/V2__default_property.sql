insert into property(id,version,name,zone,cutoff,reporting_scope,contact) values(1,0,'Juniper Casino - Demonstration','America/New_York','06:00','DEMO-US-01','compliance@example.invalid');
select setval(pg_get_serial_sequence('property','id'),1,true);
