-- GIFT TYPE ENUM
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'gift_type') THEN
    CREATE TYPE gift_type AS ENUM ('Point', 'Voucher', 'Delivery', 'Etc');
  END IF;
END;
$$;

-- GOODS TYPE ENUM
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'goods_type') THEN
    CREATE TYPE goods_type AS ENUM ('Point', 'Voucher', 'Delivery', 'Etc');
  END IF;
END;
$$;

-- GOODS STATUS ENUM (hidden/visible)
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'goods_status') THEN
    CREATE TYPE goods_status AS ENUM ('hidden', 'visible');
  END IF;
END;
$$;

-- PAYMENT TYPE ENUM
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'payment_type') THEN
    CREATE TYPE payment_type AS ENUM ('Gift', 'Point');
  END IF;
END;
$$;

-- PAYMENT STATUS ENUM
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'payment_status') THEN
    CREATE TYPE payment_status AS ENUM (
      'paid',
      'pending',
      'failed',
      'canceled',
      'partial',
      'refunded',
      'purchased'
    );
  END IF;
END;
$$;
