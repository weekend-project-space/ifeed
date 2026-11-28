-- Retired aggregate sources must no longer consume RSS subscription quota.
-- Remove their subscriptions before removing the obsolete source table.
DELETE FROM user_subscriptions WHERE source_type = 'MIX_FEED';

DROP TABLE IF EXISTS mix_feeds;
