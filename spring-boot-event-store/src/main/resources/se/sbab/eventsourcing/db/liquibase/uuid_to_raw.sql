CREATE OR REPLACE FUNCTION uuid_to_raw(in_uuid IN VARCHAR2)
RETURN RAW
    IS out_raw RAW(16);
BEGIN
    SELECT HEXTORAW(REPLACE(LOWER(in_uuid), '-'))
    INTO out_raw
    FROM DUAL;

    RETURN(out_raw);
END uuid_to_raw;