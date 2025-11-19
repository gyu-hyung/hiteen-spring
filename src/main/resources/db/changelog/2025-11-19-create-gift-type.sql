DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_type WHERE typname = 'gift_type'
  ) THEN
    CREATE TYPE gift_type AS ENUM ('Point','Voucher','Delivery','Etc');
  END IF;
END;
$$;
