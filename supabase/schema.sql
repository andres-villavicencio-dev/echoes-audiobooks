-- Echoes Audiobook App Schema
-- Run this in Supabase SQL Editor

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Audiobooks table
CREATE TABLE audiobooks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title TEXT NOT NULL,
    author TEXT NOT NULL,
    narrator TEXT NOT NULL DEFAULT 'AI Narrator',
    description TEXT,
    cover_url TEXT,
    duration_ms BIGINT NOT NULL DEFAULT 0,
    category TEXT NOT NULL CHECK (category IN ('classic', 'ai_story', 'podcast')),
    is_featured BOOLEAN DEFAULT FALSE,
    release_date DATE DEFAULT CURRENT_DATE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Chapters table
CREATE TABLE chapters (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    audiobook_id UUID NOT NULL REFERENCES audiobooks(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    audio_url TEXT NOT NULL,
    duration_ms BIGINT NOT NULL DEFAULT 0,
    chapter_index INT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(audiobook_id, chapter_index)
);

-- User progress tracking (anonymous users via device_id)
CREATE TABLE playback_progress (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    device_id TEXT NOT NULL,
    audiobook_id UUID NOT NULL REFERENCES audiobooks(id) ON DELETE CASCADE,
    chapter_id UUID NOT NULL REFERENCES chapters(id) ON DELETE CASCADE,
    position_ms BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(device_id, audiobook_id)
);

-- Indexes for performance
CREATE INDEX idx_audiobooks_category ON audiobooks(category);
CREATE INDEX idx_audiobooks_featured ON audiobooks(is_featured) WHERE is_featured = TRUE;
CREATE INDEX idx_chapters_audiobook ON chapters(audiobook_id, chapter_index);
CREATE INDEX idx_progress_device ON playback_progress(device_id);

-- Updated_at trigger function
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply trigger to tables
CREATE TRIGGER audiobooks_updated_at
    BEFORE UPDATE ON audiobooks
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER progress_updated_at
    BEFORE UPDATE ON playback_progress
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- Row Level Security (RLS)
ALTER TABLE audiobooks ENABLE ROW LEVEL SECURITY;
ALTER TABLE chapters ENABLE ROW LEVEL SECURITY;
ALTER TABLE playback_progress ENABLE ROW LEVEL SECURITY;

-- Policies: Anyone can read audiobooks and chapters
CREATE POLICY "Audiobooks are publicly readable"
    ON audiobooks FOR SELECT
    USING (true);

CREATE POLICY "Chapters are publicly readable"
    ON chapters FOR SELECT
    USING (true);

-- Progress: users can only access their own (by device_id header)
CREATE POLICY "Users can read own progress"
    ON playback_progress FOR SELECT
    USING (device_id = current_setting('request.headers')::json->>'x-device-id');

CREATE POLICY "Users can insert own progress"
    ON playback_progress FOR INSERT
    WITH CHECK (device_id = current_setting('request.headers')::json->>'x-device-id');

CREATE POLICY "Users can update own progress"
    ON playback_progress FOR UPDATE
    USING (device_id = current_setting('request.headers')::json->>'x-device-id');

-- View for audiobooks with chapter count
CREATE VIEW audiobooks_with_stats AS
SELECT 
    a.*,
    COUNT(c.id) as chapter_count,
    COALESCE(SUM(c.duration_ms), 0) as total_duration_ms
FROM audiobooks a
LEFT JOIN chapters c ON c.audiobook_id = a.id
GROUP BY a.id;

-- Grant access to the view
GRANT SELECT ON audiobooks_with_stats TO anon, authenticated;

COMMENT ON TABLE audiobooks IS 'Main audiobook catalog';
COMMENT ON TABLE chapters IS 'Individual chapters/segments of audiobooks';
COMMENT ON TABLE playback_progress IS 'User progress tracking by device';
