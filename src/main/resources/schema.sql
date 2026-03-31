-- Users
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    avatar_url VARCHAR(500),
    email_notifications_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- Friends
CREATE TABLE IF NOT EXISTS friends (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    requester_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    addressee_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING, ACCEPTED, BLOCKED
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_friendship UNIQUE (requester_id, addressee_id),
    CONSTRAINT no_self_friend CHECK (requester_id != addressee_id)
);

CREATE INDEX IF NOT EXISTS idx_friends_requester ON friends(requester_id);
CREATE INDEX IF NOT EXISTS idx_friends_addressee ON friends(addressee_id);

-- Pieces
CREATE TABLE IF NOT EXISTS pieces (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    svg_key VARCHAR(100) NOT NULL,
    movement_rules JSONB NOT NULL DEFAULT '[]',
    capture_rules JSONB,
    owner_id UUID REFERENCES users(id) ON DELETE SET NULL,
    is_public BOOLEAN NOT NULL DEFAULT FALSE,
    is_standard BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pieces_owner ON pieces(owner_id);
CREATE INDEX IF NOT EXISTS idx_pieces_public ON pieces(is_public) WHERE is_public = TRUE;
CREATE INDEX IF NOT EXISTS idx_pieces_standard ON pieces(is_standard) WHERE is_standard = TRUE;

-- User saved pieces (library)
CREATE TABLE IF NOT EXISTS user_saved_pieces (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    piece_id UUID NOT NULL REFERENCES pieces(id) ON DELETE CASCADE,
    saved_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, piece_id)
);

-- Games
CREATE TABLE IF NOT EXISTS games (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    white_player_id UUID REFERENCES users(id) ON DELETE SET NULL,
    black_player_id UUID REFERENCES users(id) ON DELETE SET NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    result VARCHAR(20),
    result_reason VARCHAR(50),
    game_type VARCHAR(20) NOT NULL DEFAULT 'STANDARD',
    mode VARCHAR(20) NOT NULL DEFAULT 'REALTIME',
    board_state JSONB NOT NULL DEFAULT '{}',
    turn VARCHAR(5) NOT NULL DEFAULT 'white',
    variant_config JSONB NOT NULL DEFAULT '{}',
    move_history JSONB NOT NULL DEFAULT '[]',
    half_move_clock INTEGER NOT NULL DEFAULT 0,
    full_move_number INTEGER NOT NULL DEFAULT 1,
    white_time_remaining_ms BIGINT,
    black_time_remaining_ms BIGINT,
    last_move_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_games_white_player ON games(white_player_id);
CREATE INDEX IF NOT EXISTS idx_games_black_player ON games(black_player_id);
CREATE INDEX IF NOT EXISTS idx_games_status ON games(status);
CREATE INDEX IF NOT EXISTS idx_games_created ON games(created_at DESC);

CREATE TABLE IF NOT EXISTS game_moves (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    game_id UUID NOT NULL REFERENCES games(id) ON DELETE CASCADE,
    move_number INTEGER NOT NULL,
    player_id UUID REFERENCES users(id) ON DELETE SET NULL,
    color VARCHAR(5) NOT NULL,
    from_square VARCHAR(5) NOT NULL,
    to_square VARCHAR(5) NOT NULL,
    piece_id UUID REFERENCES pieces(id) ON DELETE SET NULL,
    promotion_piece_id UUID REFERENCES pieces(id) ON DELETE SET NULL,
    is_capture BOOLEAN NOT NULL DEFAULT FALSE,
    is_check BOOLEAN NOT NULL DEFAULT FALSE,
    is_checkmate BOOLEAN NOT NULL DEFAULT FALSE,
    is_castle_kingside BOOLEAN NOT NULL DEFAULT FALSE,
    is_castle_queenside BOOLEAN NOT NULL DEFAULT FALSE,
    san_notation VARCHAR(20),
    board_state_after JSONB NOT NULL DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_game_moves_game ON game_moves(game_id);
CREATE INDEX IF NOT EXISTS idx_game_moves_order ON game_moves(game_id, move_number);

CREATE TABLE IF NOT EXISTS game_chat_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    game_id UUID NOT NULL REFERENCES games(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    message TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_chat_game ON game_chat_messages(game_id);
CREATE INDEX IF NOT EXISTS idx_chat_created ON game_chat_messages(game_id, created_at);

CREATE TABLE IF NOT EXISTS variant_configs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    owner_id UUID REFERENCES users(id) ON DELETE SET NULL,
    is_public BOOLEAN NOT NULL DEFAULT FALSE,
    pieces JSONB NOT NULL DEFAULT '[]',
    starting_position JSONB NOT NULL DEFAULT '{}',
    barriers JSONB NOT NULL DEFAULT '[]',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_variants_owner ON variant_configs(owner_id);
CREATE INDEX IF NOT EXISTS idx_variants_public ON variant_configs(is_public) WHERE is_public = TRUE;

INSERT INTO pieces (id, name, svg_key, movement_rules, is_standard, is_public) VALUES
(
    '00000000-0000-0000-0000-000000000001',
    'King',
    'standard_king',
    '[{"type":"SLIDE","directions":["N","NE","E","SE","S","SW","W","NW"],"maxDistance":1,"canCapture":true,"mustCapture":false}]',
    TRUE, TRUE
),
(
    '00000000-0000-0000-0000-000000000002',
    'Queen',
    'standard_queen',
    '[{"type":"SLIDE","directions":["N","NE","E","SE","S","SW","W","NW"],"maxDistance":7,"canCapture":true,"mustCapture":false}]',
    TRUE, TRUE
),
(
    '00000000-0000-0000-0000-000000000003',
    'Rook',
    'standard_rook',
    '[{"type":"SLIDE","directions":["N","E","S","W"],"maxDistance":7,"canCapture":true,"mustCapture":false}]',
    TRUE, TRUE
),
(
    '00000000-0000-0000-0000-000000000004',
    'Bishop',
    'standard_bishop',
    '[{"type":"SLIDE","directions":["NE","SE","SW","NW"],"maxDistance":7,"canCapture":true,"mustCapture":false}]',
    TRUE, TRUE
),
(
    '00000000-0000-0000-0000-000000000005',
    'Knight',
    'standard_knight',
    '[{"type":"JUMP","offsets":[[1,2],[2,1],[2,-1],[1,-2],[-1,-2],[-2,-1],[-2,1],[-1,2]],"canCapture":true,"mustCapture":false}]',
    TRUE, TRUE
),
(
    '00000000-0000-0000-0000-000000000006',
    'Pawn',
    'standard_pawn',
    '[{"type":"SLIDE","directions":["N"],"maxDistance":1,"canCapture":false,"mustCapture":false,"firstMoveDouble":true},{"type":"SLIDE","directions":["NE","NW"],"maxDistance":1,"canCapture":true,"mustCapture":true}]',
    TRUE, TRUE
)
ON CONFLICT (id) DO NOTHING;
