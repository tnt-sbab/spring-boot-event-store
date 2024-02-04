CREATE OR REPLACE FUNCTION raw_to_uuid(in_raw IN RAW)
RETURN VARCHAR2
    IS out_uuid VARCHAR2(36);
BEGIN
    SELECT REGEXP_REPLACE(LOWER(in_raw), '(.{8})(.{4})(.{4})(.{4})(.{12})', '\1-\2-\3-\4-\5')
    INTO out_uuid
    FROM DUAL;

    RETURN(out_uuid);
END raw_to_uuid;