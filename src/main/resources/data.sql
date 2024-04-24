--Insert config: IngesterJob - for input file's paths
INSERT INTO JOB_CONFIG (CLIENT_ID, JOB_NAME, CONFIG_NAME, VALUE)
VALUES ('client1', 'ingesterJob', 'inputPath', 'src/main/resources/data/ingestion/');

--Inserts for Sales Summary Job
INSERT INTO SALES (SELLER_ID, SALES_DATE, PRODUCT, QUANTITY)
VALUES ('Joe', PARSEDATETIME('20201111000000','yyyyMMddHHmmss'), 'Beer', 18);
INSERT INTO SALES (SELLER_ID, SALES_DATE, PRODUCT, QUANTITY)
VALUES ('Joe', PARSEDATETIME('20201111000000','yyyyMMddHHmmss'), 'Food', 28);
INSERT INTO SALES (SELLER_ID, SALES_DATE, PRODUCT, QUANTITY)
VALUES ('Joe', PARSEDATETIME('20201111000000','yyyyMMddHHmmss'), 'Food', 10);
INSERT INTO SALES (SELLER_ID, SALES_DATE, PRODUCT, QUANTITY)
VALUES ('Joe', PARSEDATETIME('20201111000000','yyyyMMddHHmmss'), 'Car', 18);
INSERT INTO SALES (SELLER_ID, SALES_DATE, PRODUCT, QUANTITY)
VALUES ('Joe', PARSEDATETIME('20201112000000','yyyyMMddHHmmss'), 'Beer', 19);
INSERT INTO SALES (SELLER_ID, SALES_DATE, PRODUCT, QUANTITY)
VALUES ('Beth', PARSEDATETIME('20201111000000','yyyyMMddHHmmss'), 'Beer', 18);
INSERT INTO SALES (SELLER_ID, SALES_DATE, PRODUCT, QUANTITY)
VALUES ('Beth', PARSEDATETIME('20201111000000','yyyyMMddHHmmss'), 'Food', 18);
INSERT INTO SALES (SELLER_ID, SALES_DATE, PRODUCT, QUANTITY)
VALUES ('Beth', PARSEDATETIME('20201111000000','yyyyMMddHHmmss'), 'Food', 18);
INSERT INTO SALES (SELLER_ID, SALES_DATE, PRODUCT, QUANTITY)
VALUES ('Beth', PARSEDATETIME('20201111000000','yyyyMMddHHmmss'), 'Car', 18);
INSERT INTO SALES (SELLER_ID, SALES_DATE, PRODUCT, QUANTITY)
VALUES ('Beth', PARSEDATETIME('20201113000000','yyyyMMddHHmmss'), 'Beer', 19);