-- V51: Link residents to users after iam-service migration V22 creates users
-- This migration should run after iam-service migration V22__insert_resident_users.sql
-- It links primary residents (chủ hộ) to their user accounts

-- Link Resident 1: Nguyễn Văn A → user nguyenvana
UPDATE data.residents
SET user_id = '550e8400-e29b-41d4-a716-446655440110'::uuid,
    updated_at = now()
WHERE id = '550e8400-e29b-41d4-a716-446655440100'::uuid
  AND user_id IS NULL
  AND EXISTS (
    SELECT 1 FROM iam.users 
    WHERE id = '550e8400-e29b-41d4-a716-446655440110'::uuid
  );

-- Link Resident 4: Trần Thị D → user tranthid
UPDATE data.residents
SET user_id = '550e8400-e29b-41d4-a716-446655440111'::uuid,
    updated_at = now()
WHERE id = '550e8400-e29b-41d4-a716-446655440103'::uuid
  AND user_id IS NULL
  AND EXISTS (
    SELECT 1 FROM iam.users 
    WHERE id = '550e8400-e29b-41d4-a716-446655440111'::uuid
  );

-- Link Resident 6: Lê Văn F → user levanf
UPDATE data.residents
SET user_id = '550e8400-e29b-41d4-a716-446655440112'::uuid,
    updated_at = now()
WHERE id = '550e8400-e29b-41d4-a716-446655440105'::uuid
  AND user_id IS NULL
  AND EXISTS (
    SELECT 1 FROM iam.users 
    WHERE id = '550e8400-e29b-41d4-a716-446655440112'::uuid
  );

-- Link Resident 8: Phạm Văn H → user phamvanh
UPDATE data.residents
SET user_id = '550e8400-e29b-41d4-a716-446655440113'::uuid,
    updated_at = now()
WHERE id = '550e8400-e29b-41d4-a716-446655440107'::uuid
  AND user_id IS NULL
  AND EXISTS (
    SELECT 1 FROM iam.users 
    WHERE id = '550e8400-e29b-41d4-a716-446655440113'::uuid
  );



