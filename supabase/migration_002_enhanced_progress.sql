-- Migration 002: Enhanced Progress Tracking
-- Run this in Supabase SQL Editor

-- ============================================
-- 1. Enhance playback_progress table
-- ============================================

-- Add new columns to existing table
ALTER TABLE playback_progress 
ADD COLUMN IF NOT EXISTS chapter_index INT NOT NULL DEFAULT 0,
ADD COLUMN IF NOT EXISTS playback_speed REAL NOT NULL DEFAULT 1.0,
ADD COLUMN IF NOT EXISTS is_completed BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS total_listened_ms BIGINT NOT NULL DEFAULT 0;

-- Make chapter_id optional (we'll use chapter_index instead)
ALTER TABLE playback_progress 
ALTER COLUMN chapter_id DROP NOT NULL;

-- ============================================
-- 2. Create listening_sessions table
-- ============================================

CREATE TABLE IF NOT EXISTS listening_sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    device_id TEXT NOT NULL,
    audiobook_id UUID NOT NULL REFERENCES audiobooks(id) ON DELETE CASCADE,
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ended_at TIMESTAMPTZ,
    duration_ms BIGINT NOT NULL DEFAULT 0,
    from_chapter INT NOT NULL DEFAULT 0,
    to_chapter INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_sessions_device ON listening_sessions(device_id);
CREATE INDEX IF NOT EXISTS idx_sessions_audiobook ON listening_sessions(audiobook_id);
CREATE INDEX IF NOT EXISTS idx_sessions_started ON listening_sessions(started_at DESC);

-- RLS
ALTER TABLE listening_sessions ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can read own sessions"
    ON listening_sessions FOR SELECT
    USING (device_id = current_setting('request.headers', true)::json->>'x-device-id');

CREATE POLICY "Users can insert own sessions"
    ON listening_sessions FOR INSERT
    WITH CHECK (device_id = current_setting('request.headers', true)::json->>'x-device-id');

CREATE POLICY "Users can update own sessions"
    ON listening_sessions FOR UPDATE
    USING (device_id = current_setting('request.headers', true)::json->>'x-device-id');

-- ============================================
-- 3. Create bookmarks table
-- ============================================

CREATE TABLE IF NOT EXISTS bookmarks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    device_id TEXT NOT NULL,
    audiobook_id UUID NOT NULL REFERENCES audiobooks(id) ON DELETE CASCADE,
    chapter_index INT NOT NULL,
    position_ms BIGINT NOT NULL,
    note TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_bookmarks_device ON bookmarks(device_id);
CREATE INDEX IF NOT EXISTS idx_bookmarks_audiobook ON bookmarks(audiobook_id);

-- RLS
ALTER TABLE bookmarks ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can read own bookmarks"
    ON bookmarks FOR SELECT
    USING (device_id = current_setting('request.headers', true)::json->>'x-device-id');

CREATE POLICY "Users can insert own bookmarks"
    ON bookmarks FOR INSERT
    WITH CHECK (device_id = current_setting('request.headers', true)::json->>'x-device-id');

CREATE POLICY "Users can delete own bookmarks"
    ON bookmarks FOR DELETE
    USING (device_id = current_setting('request.headers', true)::json->>'x-device-id');

-- ============================================
-- 4. Create helper view for continue listening
-- ============================================

CREATE OR REPLACE VIEW recent_progress AS
SELECT 
    pp.device_id,
    pp.audiobook_id,
    pp.chapter_index,
    pp.position_ms,
    pp.playback_speed,
    pp.is_completed,
    pp.total_listened_ms,
    pp.updated_at,
    a.title,
    a.author,
    a.cover_url,
    a.duration_ms as total_duration_ms
FROM playback_progress pp
JOIN audiobooks a ON a.id = pp.audiobook_id
WHERE pp.is_completed = FALSE
ORDER BY pp.updated_at DESC;

GRANT SELECT ON recent_progress TO anon, authenticated;

-- ============================================
-- 5. Listening stats function
-- ============================================

CREATE OR REPLACE FUNCTION get_listening_stats(p_device_id TEXT)
RETURNS JSON AS $$
DECLARE
    result JSON;
BEGIN
    SELECT json_build_object(
        'total_listened_ms', COALESCE(SUM(duration_ms), 0),
        'total_sessions', COUNT(*),
        'books_started', COUNT(DISTINCT audiobook_id),
        'today_ms', COALESCE(SUM(CASE 
            WHEN started_at >= CURRENT_DATE THEN duration_ms 
            ELSE 0 
        END), 0),
        'this_week_ms', COALESCE(SUM(CASE 
            WHEN started_at >= DATE_TRUNC('week', CURRENT_DATE) THEN duration_ms 
            ELSE 0 
        END), 0)
    ) INTO result
    FROM listening_sessions
    WHERE device_id = p_device_id;
    
    RETURN result;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

COMMENT ON TABLE listening_sessions IS 'Track individual listening sessions for history and stats';
COMMENT ON TABLE bookmarks IS 'User-saved positions in audiobooks';
