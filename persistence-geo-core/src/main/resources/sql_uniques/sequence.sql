--select setval('persistence_geo.hibernate_sequence', 100);

DROP SEQUENCE "GISSICECATUSER"."HIBERNATE_SEQUENCE" ;

CREATE SEQUENCE "GISSICECATUSER"."HIBERNATE_SEQUENCE" 
MINVALUE 1 MAXVALUE 999999999999999999999999999 INCREMENT BY 1 
START WITH 100 CACHE 20 NOORDER NOCYCLE ;