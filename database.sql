-- =======================================================================================
-- FRIENDZONE GAME SYSTEM v2.5 - FINAL BULLETPROOF EDITION (NO PARTITION ERROR)
-- Đã bỏ partitioning để tương thích 100% mọi MySQL version
-- Date: November 19, 2025
-- =======================================================================================

DROP DATABASE IF EXISTS friendzone_db;
CREATE DATABASE friendzone_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE friendzone_db;

-- 1. CORE TABLES (Đã gộp thông tin profile vào đây)
CREATE TABLE users (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(50) CHARACTER SET utf8mb4 NOT NULL,
    avatar_url VARCHAR(500) DEFAULT 'default.png',
    gender ENUM('MALE', 'FEMALE', 'OTHER') DEFAULT 'OTHER',
    elo_rating INT DEFAULT 1200,
    status ENUM('ONLINE', 'OFFLINE', 'IN_GAME', 'BANNED') DEFAULT 'OFFLINE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP NULL,
    deleted_at TIMESTAMP NULL,
    
    -- Profile fields (gộp từ user_profiles)
    bio TEXT CHARACTER SET utf8mb4,                                    -- Giới thiệu bản thân
    birth_date DATE NULL,
    location VARCHAR(100) CHARACTER SET utf8mb4,
    occupation VARCHAR(100) CHARACTER SET utf8mb4,                     -- Nghề nghiệp
    education VARCHAR(200) CHARACTER SET utf8mb4,                      -- Học vấn
    looking_for ENUM('FRIENDSHIP', 'DATING', 'SERIOUS', 'CASUAL') DEFAULT 'DATING',
    age_min INT DEFAULT 18,
    age_max INT DEFAULT 99,
    preferred_gender ENUM('MALE', 'FEMALE', 'BOTH', 'OTHER') DEFAULT 'BOTH',
    show_online_status BOOLEAN DEFAULT TRUE,
    show_last_active BOOLEAN DEFAULT TRUE,
    profile_complete_percent INT DEFAULT 0,
    is_verified BOOLEAN DEFAULT FALSE,
    
    INDEX idx_username (username),
    INDEX idx_status (status),
    INDEX idx_elo (elo_rating DESC),
    INDEX idx_deleted (deleted_at),
    INDEX idx_gender (gender),
    INDEX idx_looking_for (looking_for)
) ENGINE=InnoDB;

CREATE TABLE friendships (
    user_id1 BIGINT NOT NULL,
    user_id2 BIGINT NOT NULL,
    status ENUM('PENDING', 'ACCEPTED', 'BLOCKED') DEFAULT 'PENDING',
    action_user_id BIGINT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id1, user_id2),
    FOREIGN KEY (user_id1) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id2) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (action_user_id) REFERENCES users(user_id) ON DELETE SET NULL,
    CHECK (user_id1 < user_id2)
) ENGINE=InnoDB;

-- 2. GAME SYSTEM
CREATE TABLE games (
    game_id INT AUTO_INCREMENT PRIMARY KEY,
    game_code VARCHAR(20) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    default_config JSON,
    is_active BOOLEAN DEFAULT TRUE
) ENGINE=InnoDB;

CREATE TABLE game_sessions (
    session_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    game_id INT NOT NULL,
    player1_id BIGINT NOT NULL,
    player2_id BIGINT NOT NULL,
    winner_id BIGINT NULL,
    start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    end_time TIMESTAMP NULL,
    status ENUM('WAITING','ONGOING','FINISHED','ABANDONED','DRAW') DEFAULT 'WAITING',
    match_config JSON,
    total_moves INT DEFAULT 0,
    duration_seconds INT NULL,
    version INT DEFAULT 0,
    FOREIGN KEY (game_id) REFERENCES games(game_id),
    FOREIGN KEY (player1_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (player2_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (winner_id) REFERENCES users(user_id) ON DELETE SET NULL,
    CHECK (player1_id < player2_id),
    INDEX idx_status (status),
    INDEX idx_player1 (player1_id),
    INDEX idx_player2 (player2_id)
) ENGINE=InnoDB;

CREATE TABLE game_moves (
    move_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,
    move_data JSON NOT NULL,
    move_number INT NOT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    FOREIGN KEY (session_id) REFERENCES game_sessions(session_id) ON DELETE CASCADE,
    UNIQUE KEY uq_session_move (session_id, move_number)
) ENGINE=InnoDB;

-- 3. STATS
CREATE TABLE user_game_stats (
    user_id BIGINT NOT NULL,
    game_id INT NOT NULL,
    total_matches INT DEFAULT 0,
    wins INT DEFAULT 0,
    losses INT DEFAULT 0,
    draws INT DEFAULT 0,
    total_playtime_minutes INT DEFAULT 0,
    current_win_streak INT DEFAULT 0,
    longest_win_streak INT DEFAULT 0,
    last_played TIMESTAMP NULL,
    PRIMARY KEY (user_id, game_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (game_id) REFERENCES games(game_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 4. CHAT SYSTEM
CREATE TABLE rooms (
    room_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type ENUM('PRIVATE', 'GROUP', 'GAME') DEFAULT 'PRIVATE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE private_rooms (
    user_id1 BIGINT NOT NULL,
    user_id2 BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    PRIMARY KEY (user_id1, user_id2),
    FOREIGN KEY (room_id) REFERENCES rooms(room_id) ON DELETE CASCADE,
    CHECK (user_id1 < user_id2)
) ENGINE=InnoDB;

CREATE TABLE room_members (
    room_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role ENUM('MEMBER','ADMIN') DEFAULT 'MEMBER',
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_read_at TIMESTAMP NULL,
    PRIMARY KEY (room_id, user_id),
    FOREIGN KEY (room_id) REFERENCES rooms(room_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ←←←← ĐÃ BỎ PARTITION ĐỂ TRÁNH LỖI 1506 →→→→
CREATE TABLE messages (
    message_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    content TEXT, -- Nội dung tin nhắn hoặc URL file hoặc ID Sticker
    message_type ENUM('TEXT','IMAGE','FILE','STICKER','VOICE','SYSTEM') DEFAULT 'TEXT', -- Đã thêm VOICE
    file_meta JSON NULL, -- Lưu tên file, kích thước, loại file, duration (nếu là VOICE)...
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    INDEX idx_room_time (room_id, created_at DESC),
    INDEX idx_sender (sender_id),
    FOREIGN KEY (room_id) REFERENCES rooms(room_id) ON DELETE CASCADE,
    FOREIGN KEY (sender_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 7. CALL SYSTEM
CREATE TABLE calls (
    call_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    caller_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    room_id BIGINT NULL,
    call_type ENUM('AUDIO', 'VIDEO') NOT NULL,
    status ENUM('ONGOING', 'COMPLETED', 'MISSED', 'REJECTED', 'BUSY') DEFAULT 'ONGOING',
    start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    end_time TIMESTAMP NULL,
    duration_seconds INT DEFAULT 0,
    FOREIGN KEY (caller_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (receiver_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (room_id) REFERENCES rooms(room_id) ON DELETE SET NULL,
    INDEX idx_caller (caller_id),
    INDEX idx_receiver (receiver_id)
) ENGINE=InnoDB;

-- 8. NOTIFICATION SYSTEM
CREATE TABLE notifications (
    notification_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type ENUM('FRIEND_REQUEST', 'GAME_INVITE', 'MISSED_CALL', 'SYSTEM') NOT NULL,
    content TEXT,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_user_unread (user_id, is_read)
) ENGINE=InnoDB;

-- 9. CONNECTION LOGS
CREATE TABLE connection_logs (
    log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    ip_address VARCHAR(45),
    action ENUM('LOGIN', 'LOGOUT') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 10. STICKER SYSTEM
CREATE TABLE sticker_packs (
    pack_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    is_premium BOOLEAN DEFAULT FALSE,
    price INT DEFAULT 0, -- 0 = Free
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE stickers (
    sticker_id INT AUTO_INCREMENT PRIMARY KEY,
    pack_id INT NOT NULL,
    file_url VARCHAR(500) NOT NULL, -- Đường dẫn ảnh sticker
    code VARCHAR(50) NOT NULL, -- Mã sticker (vd: :pepe_cry:)
    FOREIGN KEY (pack_id) REFERENCES sticker_packs(pack_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 5. PROCEDURES (giữ nguyên – đã an toàn concurrency)
DELIMITER $$

CREATE PROCEDURE sp_create_game_session(
    IN p_player_a BIGINT,
    IN p_player_b BIGINT,
    IN p_game_code VARCHAR(20),
    OUT p_session_id BIGINT,
    OUT p_error VARCHAR(255)
)
BEGIN
    DECLARE v_game_id INT;
    DECLARE v_config JSON;
    START TRANSACTION;
    SELECT game_id, default_config INTO v_game_id, v_config
    FROM games WHERE game_code = p_game_code AND is_active = TRUE;
    IF v_game_id IS NULL THEN
        SET p_error = 'Game không tồn tại'; ROLLBACK;
    ELSE
        INSERT INTO game_sessions (
            game_id, player1_id, player2_id, match_config, status
        ) VALUES (
            v_game_id,
            LEAST(p_player_a, p_player_b),
            GREATEST(p_player_a, p_player_b),
            v_config,
            'WAITING'
        );
        SET p_session_id = LAST_INSERT_ID();
        UPDATE users SET status = 'IN_GAME' WHERE user_id IN (p_player_a, p_player_b);
        SET p_error = NULL;
        COMMIT;
    END IF;
END$$

CREATE PROCEDURE sp_save_move(
    IN p_session_id BIGINT,
    IN p_player_id BIGINT,
    IN p_move_data JSON,
    OUT p_move_number INT,
    OUT p_error VARCHAR(255)
)
BEGIN
    DECLARE v_status ENUM('WAITING','ONGOING','FINISHED','ABANDONED','DRAW');
    START TRANSACTION;
    SELECT status INTO v_status FROM game_sessions WHERE session_id = p_session_id FOR UPDATE;
    IF v_status NOT IN ('WAITING','ONGOING') THEN
        SET p_error = 'Trận đấu đã kết thúc'; ROLLBACK;
    ELSE
        UPDATE game_sessions SET total_moves = total_moves + 1 WHERE session_id = p_session_id;
        SELECT total_moves INTO p_move_number FROM game_sessions WHERE session_id = p_session_id;
        INSERT INTO game_moves (session_id, player_id, move_data, move_number)
        VALUES (p_session_id, p_player_id, p_move_data, p_move_number);
        UPDATE game_sessions SET status = 'ONGOING'
        WHERE session_id = p_session_id AND status = 'WAITING';
        SET p_error = NULL;
        COMMIT;
    END IF;
END$$

CREATE PROCEDURE sp_finish_game(
    IN p_session_id BIGINT,
    IN p_winner_id BIGINT,
    OUT p_success BOOLEAN,
    OUT p_error VARCHAR(255)
)
BEGIN
    DECLARE v_status ENUM('WAITING','ONGOING','FINISHED','ABANDONED','DRAW');
    DECLARE v_player1 BIGINT;
    DECLARE v_player2 BIGINT;
    DECLARE v_game_id INT;
    
    START TRANSACTION;
    
    -- Check session
    SELECT status, player1_id, player2_id, game_id 
    INTO v_status, v_player1, v_player2, v_game_id
    FROM game_sessions 
    WHERE session_id = p_session_id 
    FOR UPDATE;
    
    IF v_status != 'ONGOING' THEN
        SET p_success = FALSE;
        SET p_error = 'Trận đấu không diễn ra hoặc đã kết thúc';
        ROLLBACK;
    ELSE
        -- Update session
        UPDATE game_sessions 
        SET status = 'FINISHED', 
            winner_id = p_winner_id, 
            end_time = CURRENT_TIMESTAMP,
            duration_seconds = TIMESTAMPDIFF(SECOND, start_time, CURRENT_TIMESTAMP)
        WHERE session_id = p_session_id;
        
        -- Update Stats & ELO
        IF p_winner_id IS NOT NULL THEN
            -- Winner
            INSERT INTO user_game_stats (user_id, game_id, total_matches, wins, current_win_streak, last_played)
            VALUES (p_winner_id, v_game_id, 1, 1, 1, NOW())
            ON DUPLICATE KEY UPDATE 
                total_matches = total_matches + 1,
                wins = wins + 1,
                current_win_streak = current_win_streak + 1,
                longest_win_streak = GREATEST(longest_win_streak, current_win_streak + 1),
                last_played = NOW();
                
            -- Loser
            SET @loser_id = IF(v_player1 = p_winner_id, v_player2, v_player1);
            INSERT INTO user_game_stats (user_id, game_id, total_matches, losses, current_win_streak, last_played)
            VALUES (@loser_id, v_game_id, 1, 1, 0, NOW())
            ON DUPLICATE KEY UPDATE 
                total_matches = total_matches + 1,
                losses = losses + 1,
                current_win_streak = 0,
                last_played = NOW();
                
            -- Simple ELO update (+10 / -10)
            UPDATE users SET elo_rating = elo_rating + 10 WHERE user_id = p_winner_id;
            UPDATE users SET elo_rating = GREATEST(0, elo_rating - 10) WHERE user_id = @loser_id;
        ELSE
            -- DRAW
            INSERT INTO user_game_stats (user_id, game_id, total_matches, draws, last_played)
            VALUES (v_player1, v_game_id, 1, 1, NOW())
            ON DUPLICATE KEY UPDATE total_matches = total_matches + 1, draws = draws + 1, last_played = NOW();
            
            INSERT INTO user_game_stats (user_id, game_id, total_matches, draws, last_played)
            VALUES (v_player2, v_game_id, 1, 1, NOW())
            ON DUPLICATE KEY UPDATE total_matches = total_matches + 1, draws = draws + 1, last_played = NOW();
        END IF;
        
        -- Update User Status back to ONLINE
        UPDATE users SET status = 'ONLINE' WHERE user_id IN (v_player1, v_player2);
        
        SET p_success = TRUE;
        SET p_error = NULL;
        COMMIT;
    END IF;
END$$

DELIMITER ;

-- 6. SEED DATA
INSERT INTO games (game_code, display_name, default_config) VALUES
('CARO', 'Cờ Caro', '{"board_size":20,"win_condition":5}'),
('CATCH_HEART', 'Bắt Trái Tim', '{"duration":60}'),
('WORD_CHAIN', 'Nối Từ', '{"time_per_turn":15}'),
('LOVE_QUIZ', 'Quiz Tình Yêu', '{"questions":10}'),
('DRAW_GUESS', 'Vẽ Hình Đoán Chữ', '{"rounds":3,"time_per_round":60}');

-- 7. LOVE QUIZ QUESTIONS TABLE
CREATE TABLE quiz_questions (
    question_id INT AUTO_INCREMENT PRIMARY KEY,
    question_text VARCHAR(500) CHARACTER SET utf8mb4 NOT NULL,
    answer_a VARCHAR(200) CHARACTER SET utf8mb4 NOT NULL,
    answer_b VARCHAR(200) CHARACTER SET utf8mb4 NOT NULL,
    answer_c VARCHAR(200) CHARACTER SET utf8mb4 NOT NULL,
    answer_d VARCHAR(200) CHARACTER SET utf8mb4 NOT NULL,
    category VARCHAR(50) DEFAULT 'LOVE',
    difficulty ENUM('EASY', 'MEDIUM', 'HARD') DEFAULT 'MEDIUM',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_category (category),
    INDEX idx_active (is_active)
) ENGINE=InnoDB;

-- 8. DRAW WORDS TABLE (cho game vẽ hình đoán chữ)
CREATE TABLE draw_words (
    word_id INT AUTO_INCREMENT PRIMARY KEY,
    word VARCHAR(100) CHARACTER SET utf8mb4 NOT NULL,
    category VARCHAR(50) DEFAULT 'GENERAL',
    difficulty ENUM('EASY', 'MEDIUM', 'HARD') DEFAULT 'MEDIUM',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_category (category),
    INDEX idx_active (is_active)
) ENGINE=InnoDB;

-- SEED QUIZ QUESTIONS (100 câu hỏi)
INSERT INTO quiz_questions (question_text, answer_a, answer_b, answer_c, answer_d, category) VALUES
-- Câu hỏi về cảm xúc và tâm trạng
('Khi buồn, bạn muốn người yêu làm gì?', 'Ở bên cạnh lắng nghe', 'Cho không gian riêng', 'Mua quà an ủi', 'Đưa đi chơi giải trí', 'LOVE'),
('Khi vui, bạn muốn chia sẻ với ai đầu tiên?', 'Người yêu', 'Bạn thân', 'Gia đình', 'Đăng lên mạng xã hội', 'LOVE'),
('Khi stress, bạn thường làm gì?', 'Tâm sự với người yêu', 'Một mình suy nghĩ', 'Đi shopping', 'Nghe nhạc/xem phim', 'LOVE'),
('Điều gì khiến bạn cảm thấy được yêu nhất?', 'Được quan tâm hàng ngày', 'Nhận quà bất ngờ', 'Nghe lời nói ngọt ngào', 'Được ôm ấp', 'LOVE'),
('Khi giận dỗi, bạn muốn được đối xử thế nào?', 'Được xin lỗi ngay', 'Được cho không gian', 'Được mua quà làm hòa', 'Được ôm và dỗ dành', 'LOVE'),

-- Câu hỏi về quà tặng và kỷ niệm
('Món quà sinh nhật lý tưởng?', 'Đồ handmade', 'Tiền mặt', 'Du lịch cùng nhau', 'Đồ công nghệ', 'LOVE'),
('Kỷ niệm yêu thích với người yêu?', 'Ngày đầu gặp', 'Chuyến đi đầu tiên', 'Món quà đặc biệt', 'Khoảnh khắc bất ngờ', 'LOVE'),
('Bạn thích được tặng gì vào Valentine?', 'Hoa và chocolate', 'Trang sức', 'Bữa tối lãng mạn', 'Không cần quà, chỉ cần bên nhau', 'LOVE'),
('Ngày kỷ niệm quan trọng nhất?', 'Ngày quen nhau', 'Ngày tỏ tình', 'Sinh nhật của nhau', 'Ngày đầu tiên hôn', 'LOVE'),
('Cách bạn muốn được cầu hôn?', 'Lãng mạn nơi công cộng', 'Riêng tư chỉ hai người', 'Bất ngờ tại nhà', 'Trong chuyến du lịch', 'LOVE'),

-- Câu hỏi về ngôn ngữ tình yêu
('Cách thể hiện tình yêu bạn thích?', 'Lời nói ngọt ngào', 'Hành động quan tâm', 'Quà tặng bất ngờ', 'Thời gian bên nhau', 'LOVE'),
('Ngôn ngữ tình yêu của bạn?', 'Lời nói yêu thương', 'Thời gian chất lượng', 'Quà tặng', 'Cử chỉ quan tâm', 'LOVE'),
('Bạn muốn nghe câu nào nhất?', 'Anh/Em yêu em/anh', 'Anh/Em nhớ em/anh', 'Anh/Em tự hào về em/anh', 'Anh/Em luôn ở đây vì em/anh', 'LOVE'),
('Hành động nào khiến bạn cảm động nhất?', 'Nấu ăn cho bạn', 'Chăm sóc khi ốm', 'Đợi bạn dù trễ', 'Nhớ những điều nhỏ nhặt', 'LOVE'),
('Bạn thể hiện tình yêu qua?', 'Nói lời yêu thương', 'Làm việc nhà giúp đỡ', 'Mua quà tặng', 'Dành thời gian bên nhau', 'LOVE'),

-- Câu hỏi về hẹn hò
('Cuộc hẹn lý tưởng?', 'Ở nhà xem phim', 'Đi ăn nhà hàng', 'Dạo phố đêm', 'Đi phượt xa', 'LOVE'),
('Bạn thích kiểu hẹn hò nào?', 'Romantic dinner', 'Xem phim rạp', 'Đi công viên', 'Ở nhà nấu ăn cùng', 'LOVE'),
('Địa điểm hẹn hò mơ ước?', 'Bãi biển hoàng hôn', 'Quán cafe yên tĩnh', 'Công viên giải trí', 'Núi rừng thiên nhiên', 'LOVE'),
('Bữa tối lý tưởng?', 'Nến và rượu vang', 'BBQ ngoài trời', 'Lẩu ấm cúng', 'Đồ ăn đường phố', 'LOVE'),
('Hoạt động cuối tuần ưa thích?', 'Ngủ nướng cùng nhau', 'Đi cafe và đọc sách', 'Tập gym hoặc thể thao', 'Shopping và ăn uống', 'LOVE'),

-- Câu hỏi về giao tiếp
('Khi cãi nhau, bạn thường?', 'Im lặng nguội đi', 'Nói thẳng suy nghĩ', 'Nhờ người hòa giải', 'Viết tin nhắn dài', 'LOVE'),
('Cách giải quyết mâu thuẫn?', 'Nói chuyện ngay', 'Đợi nguội rồi nói', 'Viết thư/tin nhắn', 'Cần thời gian một mình', 'LOVE'),
('Bạn có hay nói "Anh/Em yêu em/anh" không?', 'Mỗi ngày', 'Thỉnh thoảng', 'Hiếm khi', 'Chỉ khi đặc biệt', 'LOVE'),
('Khi có chuyện buồn, bạn sẽ?', 'Kể ngay cho người yêu', 'Giữ trong lòng một lúc', 'Kể cho bạn thân trước', 'Không muốn ai biết', 'LOVE'),
('Bạn thích được liên lạc thế nào?', 'Gọi điện thường xuyên', 'Nhắn tin cả ngày', 'Gặp mặt là chính', 'Tùy theo tình huống', 'LOVE'),

-- Câu hỏi về giá trị
('Điều quan trọng nhất trong tình yêu?', 'Tin tưởng', 'Lãng mạn', 'Tự do cá nhân', 'Ổn định tài chính', 'LOVE'),
('Điều không thể chấp nhận?', 'Nói dối', 'Thiếu quan tâm', 'Ghen tuông quá mức', 'Không có tham vọng', 'LOVE'),
('Điều quan trọng khi chọn người yêu?', 'Ngoại hình', 'Tính cách', 'Tài chính', 'Gia đình', 'LOVE'),
('Bạn coi trọng điều gì nhất?', 'Sự chung thủy', 'Sự thấu hiểu', 'Sự hài hước', 'Sự lãng mạn', 'LOVE'),
('Tình yêu lý tưởng là?', 'Đam mê cháy bỏng', 'Bình yên ấm áp', 'Phiêu lưu mạo hiểm', 'Đơn giản và chân thành', 'LOVE'),

-- Câu hỏi về tương lai
('Tương lai mơ ước?', 'Nhà nhỏ hạnh phúc', 'Sự nghiệp thành công', 'Đi khắp thế giới', 'Cuộc sống tự do', 'LOVE'),
('Bạn muốn có mấy con?', 'Không có', 'Một', 'Hai', 'Nhiều hơn hai', 'LOVE'),
('Nơi bạn muốn sống?', 'Thành phố lớn', 'Ngoại ô yên tĩnh', 'Gần biển', 'Ở quê gần gia đình', 'LOVE'),
('Kế hoạch sau khi cưới?', 'Đi du lịch tuần trăng mật', 'Mua nhà riêng', 'Có em bé sớm', 'Tập trung sự nghiệp', 'LOVE'),
('Bạn muốn đám cưới như thế nào?', 'Hoành tráng nhiều khách', 'Nhỏ gọn thân mật', 'Chỉ hai người', 'Đám cưới ở nước ngoài', 'LOVE'),

-- Câu hỏi về ghen tuông và tin tưởng
('Bạn ghen tuông ở mức nào?', 'Không ghen', 'Ghen nhẹ nhàng', 'Ghen vừa phải', 'Rất hay ghen', 'LOVE'),
('Khi người yêu đi chơi với bạn khác giới?', 'Hoàn toàn thoải mái', 'Hơi lo nhưng tin tưởng', 'Muốn biết chi tiết', 'Không thích lắm', 'LOVE'),
('Bạn có kiểm tra điện thoại người yêu không?', 'Không bao giờ', 'Chỉ khi nghi ngờ', 'Thỉnh thoảng', 'Thường xuyên', 'LOVE'),
('Phản ứng khi ai đó tán tỉnh người yêu bạn?', 'Tin tưởng người yêu xử lý', 'Nhẹ nhàng nhắc nhở', 'Đánh dấu chủ quyền', 'Rất khó chịu', 'LOVE'),
('Bạn nghĩ sao về việc giữ liên lạc với người cũ?', 'Hoàn toàn OK', 'Chấp nhận được', 'Không thích lắm', 'Tuyệt đối không', 'LOVE'),

-- Câu hỏi về sở thích
('Bạn thích xem phim thể loại gì cùng nhau?', 'Tình cảm lãng mạn', 'Hài hước', 'Kinh dị', 'Hành động', 'LOVE'),
('Âm nhạc bạn muốn nghe cùng người yêu?', 'Ballad tình cảm', 'Pop sôi động', 'Nhạc cổ điển', 'EDM/Remix', 'LOVE'),
('Hoạt động bạn muốn làm cùng?', 'Nấu ăn', 'Tập thể dục', 'Chơi game', 'Đọc sách/học cùng', 'LOVE'),
('Du lịch bạn thích kiểu nào?', 'Biển và resort', 'Núi và cắm trại', 'Thành phố và mua sắm', 'Khám phá văn hóa', 'LOVE'),
('Bạn thích làm gì buổi tối?', 'Xem phim cùng nhau', 'Đi dạo', 'Nói chuyện tâm sự', 'Mỗi người làm việc riêng', 'LOVE'),

-- Câu hỏi về thói quen
('Buổi sáng thức dậy, bạn muốn?', 'Được ôm ấp thêm', 'Được pha cafe/trà', 'Được hôn chào', 'Được yên tĩnh một mình', 'LOVE'),
('Bạn có thói quen nào khi ngủ?', 'Ôm người yêu ngủ', 'Ngủ quay lưng', 'Cần không gian riêng', 'Thích được ôm từ sau', 'LOVE'),
('Khi ốm, bạn muốn người yêu?', 'Ở bên chăm sóc', 'Nấu cháo cho ăn', 'Cho uống thuốc', 'Để yên cho nghỉ ngơi', 'LOVE'),
('Bạn có hay quên ngày quan trọng không?', 'Không bao giờ quên', 'Thỉnh thoảng quên', 'Hay quên lắm', 'Đặt nhắc nhở hết', 'LOVE'),
('Thói quen xấu bạn có thể chấp nhận?', 'Ngủ ngáy', 'Để đồ bừa bãi', 'Thức khuya', 'Nghiện điện thoại', 'LOVE'),

-- Câu hỏi về tiền bạc
('Quan điểm về tiền bạc trong tình yêu?', 'Chia đều', 'Ai nhiều trả nhiều', 'Nam/Nữ trả hết', 'Luân phiên nhau', 'LOVE'),
('Bạn có tiết kiệm chung không?', 'Có, ngay từ đầu', 'Chỉ khi nghiêm túc', 'Mỗi người tiết kiệm riêng', 'Chưa nghĩ đến', 'LOVE'),
('Khi mua đồ đắt tiền?', 'Bàn bạc cùng nhau', 'Tự quyết định', 'Thông báo sau khi mua', 'Tùy ai trả tiền', 'LOVE'),
('Ai nên quản lý tài chính?', 'Người giỏi hơn', 'Chia đều trách nhiệm', 'Vợ/người phụ nữ', 'Chồng/người đàn ông', 'LOVE'),
('Bạn chi tiêu cho tình yêu thế nào?', 'Rất thoải mái', 'Có kế hoạch', 'Tiết kiệm', 'Tùy tâm trạng', 'LOVE'),

-- Câu hỏi về gia đình
('Bạn muốn sống với gia đình chồng/vợ không?', 'Sẵn sàng', 'Không muốn lắm', 'Chỉ gần thôi', 'Tuyệt đối không', 'LOVE'),
('Khi gia đình không ủng hộ?', 'Cố gắng thuyết phục', 'Vẫn tiếp tục', 'Xem xét lại', 'Gia đình là trên hết', 'LOVE'),
('Bạn gặp gia đình người yêu khi nào?', 'Càng sớm càng tốt', 'Khi nghiêm túc', 'Khi chuẩn bị cưới', 'Để họ chủ động', 'LOVE'),
('Vai trò của gia đình trong tình yêu?', 'Rất quan trọng', 'Quan trọng vừa phải', 'Không ảnh hưởng nhiều', 'Chỉ hai người là đủ', 'LOVE'),
('Tết về nhà ai trước?', 'Nhà trai', 'Nhà gái', 'Luân phiên', 'Ở nhà riêng', 'LOVE'),

-- Câu hỏi về khoảng cách
('Khoảng cách trong yêu xa?', 'Không vấn đề', 'Khó nhưng cố gắng', 'Cần gặp thường xuyên', 'Không chấp nhận được', 'LOVE'),
('Bao lâu cần gặp nhau?', 'Mỗi ngày', 'Vài ngày một lần', 'Mỗi tuần', 'Tùy điều kiện', 'LOVE'),
('Nếu phải xa nhau vì công việc?', 'Ủng hộ hoàn toàn', 'Chấp nhận có thời hạn', 'Không muốn lắm', 'Khó chấp nhận', 'LOVE'),
('Yêu xa, bạn duy trì bằng cách nào?', 'Video call mỗi ngày', 'Nhắn tin thường xuyên', 'Gửi quà bất ngờ', 'Đợi ngày gặp lại', 'LOVE'),
('Bạn có thể yêu xa bao lâu?', 'Không giới hạn', '1-2 năm', 'Vài tháng', 'Không thể yêu xa', 'LOVE'),

-- Câu hỏi về thể hiện tình cảm
('Mức độ PDA bạn thích?', 'Thoải mái', 'Nắm tay thôi', 'Kín đáo', 'Không thích PDA', 'LOVE'),
('Bạn thích được bất ngờ không?', 'Rất thích', 'Thích nhưng vừa phải', 'Không thích lắm', 'Ghét bất ngờ', 'LOVE'),
('Muốn được gọi thế nào?', 'Tên thật', 'Biệt danh dễ thương', 'Anh/Em', 'Baby/Honey', 'LOVE'),
('Bạn hay nhắn tin kiểu gì?', 'Nhiều emoji và sticker', 'Ngắn gọn', 'Dài và chi tiết', 'Voice message', 'LOVE'),
('Khi yêu, bạn là người?', 'Chủ động thể hiện', 'Chờ đợi được quan tâm', 'Cân bằng cho-nhận', 'Tùy theo đối phương', 'LOVE'),

-- Câu hỏi về thử thách
('Khi gặp khó khăn tài chính?', 'Cùng nhau vượt qua', 'Mỗi người tự lo', 'Xem xét lại mối quan hệ', 'Nhờ gia đình giúp', 'LOVE'),
('Nếu người yêu thất bại?', 'Ở bên động viên', 'Giúp đỡ tìm cách', 'Cho không gian riêng', 'Khuyên bỏ cuộc', 'LOVE'),
('Khi có người thứ ba xuất hiện?', 'Tin tưởng tuyệt đối', 'Nói chuyện thẳng thắn', 'Theo dõi kỹ', 'Chia tay ngay', 'LOVE'),
('Nếu phải lựa chọn sự nghiệp hay tình yêu?', 'Tình yêu', 'Sự nghiệp', 'Cố gắng cân bằng', 'Tùy tình huống', 'LOVE'),
('Khi không còn cảm giác đam mê?', 'Tìm cách hâm nóng', 'Chấp nhận thực tế', 'Nói chuyện thật lòng', 'Xem xét chia tay', 'LOVE'),

-- Câu hỏi về tính cách
('Bạn thích người yêu có tính cách?', 'Hài hước vui vẻ', 'Chín chắn trưởng thành', 'Lãng mạn dịu dàng', 'Mạnh mẽ quyết đoán', 'LOVE'),
('Điểm yếu bạn có thể chấp nhận?', 'Nóng tính', 'Ít nói', 'Hay quên', 'Bướng bỉnh', 'LOVE'),
('Bạn muốn người yêu giống bạn không?', 'Hoàn toàn giống', 'Giống một phần', 'Khác biệt hoàn toàn', 'Bổ sung cho nhau', 'LOVE'),
('Tuổi tác quan trọng không?', 'Không quan trọng', 'Chênh lệch vừa phải', 'Nên bằng tuổi', 'Người lớn tuổi hơn', 'LOVE'),
('Chiều cao quan trọng không?', 'Không quan trọng', 'Cao hơn một chút', 'Phải cao hơn nhiều', 'Tùy duyên', 'LOVE'),

-- Câu hỏi về công nghệ
('Bạn có đăng ảnh người yêu lên mạng không?', 'Thường xuyên', 'Thỉnh thoảng', 'Hiếm khi', 'Không bao giờ', 'LOVE'),
('Phản ứng khi người yêu online mà không rep tin?', 'Bình thường', 'Hơi khó chịu', 'Hỏi lý do', 'Rất tức giận', 'LOVE'),
('Bạn có chia sẻ mật khẩu điện thoại không?', 'Sẵn sàng', 'Chỉ khi cần', 'Không thích', 'Tuyệt đối không', 'LOVE'),
('Khi người yêu nghiện điện thoại?', 'Chấp nhận', 'Nhắc nhở nhẹ', 'Khó chịu', 'Yêu cầu thay đổi', 'LOVE'),
('Bạn có stalk người yêu trên mạng không?', 'Thường xuyên', 'Thỉnh thoảng', 'Hiếm khi', 'Không bao giờ', 'LOVE'),

-- Câu hỏi về lãng mạn
('Bạn muốn được tỏ tình thế nào?', 'Lãng mạn với hoa và nến', 'Đơn giản và chân thành', 'Bất ngờ táo bạo', 'Viết thư tay', 'LOVE'),
('Câu tỏ tình bạn thích?', 'Anh/Em yêu em/anh', 'Làm người yêu anh/em nhé', 'Mình yêu nhau đi', 'Không cần nói, hành động thể hiện', 'LOVE'),
('Nụ hôn đầu nên ở đâu?', 'Nơi lãng mạn', 'Bất kỳ đâu tự nhiên', 'Nơi riêng tư', 'Không quan trọng địa điểm', 'LOVE'),
('Bạn có tin vào tình yêu sét đánh không?', 'Tin tuyệt đối', 'Có thể', 'Không tin lắm', 'Hoàn toàn không', 'LOVE'),
('Valentine bạn muốn làm gì?', 'Ăn tối lãng mạn', 'Tặng quà và hoa', 'Đi du lịch', 'Ở nhà bên nhau', 'LOVE'),

-- Câu hỏi về cam kết
('Bạn nghĩ khi nào nên cưới?', 'Khi còn trẻ', 'Khi đủ tài chính', 'Khi đã hiểu nhau sâu', 'Khi cảm thấy sẵn sàng', 'LOVE'),
('Yêu bao lâu thì nên cưới?', 'Dưới 1 năm', '1-2 năm', '3-5 năm', 'Tùy cảm nhận', 'LOVE'),
('Điều kiện để bạn đồng ý kết hôn?', 'Tình yêu là đủ', 'Có nhà có xe', 'Gia đình đồng ý', 'Sự nghiệp ổn định', 'LOVE'),
('Bạn có sẵn sàng hy sinh vì tình yêu?', 'Sẵn sàng tất cả', 'Trong giới hạn', 'Tùy mức độ', 'Không muốn hy sinh', 'LOVE'),
('Nếu phải chọn giữa tình yêu và tự do?', 'Tình yêu', 'Tự do', 'Cân bằng cả hai', 'Tình yêu đúng nghĩa sẽ có tự do', 'LOVE'),

-- Câu hỏi bổ sung
('Điều khiến bạn thấy được yêu?', 'Được nhớ đến', 'Được ưu tiên', 'Được lắng nghe', 'Được tôn trọng', 'LOVE'),
('Bạn thích được chăm sóc thế nào khi mệt?', 'Massage và xoa bóp', 'Nấu đồ ăn ngon', 'Để yên nghỉ ngơi', 'Nói chuyện cho vui', 'LOVE'),
('Hoạt động lãng mạn nhất?', 'Dạo bộ dưới trăng', 'Xem phim ôm nhau', 'Nấu ăn cùng nhau', 'Nhảy cùng nhau', 'LOVE'),
('Bạn thích được tán thế nào?', 'Trực tiếp mạnh dạn', 'Nhẹ nhàng tinh tế', 'Qua tin nhắn trước', 'Để bạn chủ động', 'LOVE'),
('Điều bạn sợ nhất trong tình yêu?', 'Bị phản bội', 'Bị bỏ rơi', 'Không được yêu lại', 'Yêu sai người', 'LOVE'),
('Bạn tin vào định mệnh trong tình yêu?', 'Tin tuyệt đối', 'Tin một phần', 'Không tin lắm', 'Hoàn toàn không', 'LOVE'),
('Điều gì giữ lửa trong tình yêu?', 'Sự quan tâm hàng ngày', 'Những bất ngờ', 'Sự trung thực', 'Thời gian bên nhau', 'LOVE'),
('Bạn xử lý cô đơn thế nào khi yêu xa?', 'Tập trung công việc', 'Nhớ kỷ niệm đẹp', 'Gọi điện thường xuyên', 'Viết nhật ký tình yêu', 'LOVE'),
('Món ăn bạn muốn nấu cho người yêu?', 'Phở hoặc mì', 'Cơm nhà đầy đủ', 'Bánh tự làm', 'Món Tây lãng mạn', 'LOVE'),
('Bài hát tình yêu bạn thích?', 'Ballad buồn', 'Pop vui tươi', 'Nhạc Trịnh', 'Nhạc nước ngoài', 'LOVE');

INSERT INTO users (username, password_hash, nickname) VALUES
('admin', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Admin'),
('hung', '$2y$10$example', 'Hùng'),
('lan', '$2y$10$example', 'Lan');

-- SEED DRAW WORDS (50 từ vựng cho game vẽ hình)
INSERT INTO draw_words (word, category, difficulty) VALUES
-- Động vật (Animals)
('con mèo', 'ANIMAL', 'EASY'),
('con chó', 'ANIMAL', 'EASY'),
('con gà', 'ANIMAL', 'EASY'),
('con vịt', 'ANIMAL', 'EASY'),
('con cá', 'ANIMAL', 'EASY'),
('con bướm', 'ANIMAL', 'MEDIUM'),
('con voi', 'ANIMAL', 'MEDIUM'),
('con rắn', 'ANIMAL', 'MEDIUM'),
('con chuột', 'ANIMAL', 'EASY'),
('con thỏ', 'ANIMAL', 'EASY'),

-- Đồ vật (Objects)
('ngôi nhà', 'OBJECT', 'EASY'),
('chiếc xe', 'OBJECT', 'MEDIUM'),
('cái bàn', 'OBJECT', 'EASY'),
('cái ghế', 'OBJECT', 'EASY'),
('điện thoại', 'OBJECT', 'MEDIUM'),
('máy tính', 'OBJECT', 'HARD'),
('chiếc đèn', 'OBJECT', 'EASY'),
('cái cây', 'OBJECT', 'EASY'),
('bông hoa', 'OBJECT', 'EASY'),
('trái táo', 'OBJECT', 'EASY'),

-- Thiên nhiên (Nature)
('mặt trời', 'NATURE', 'EASY'),
('mặt trăng', 'NATURE', 'EASY'),
('ngôi sao', 'NATURE', 'EASY'),
('đám mây', 'NATURE', 'EASY'),
('cầu vồng', 'NATURE', 'MEDIUM'),
('núi cao', 'NATURE', 'MEDIUM'),
('biển cả', 'NATURE', 'MEDIUM'),
('dòng sông', 'NATURE', 'MEDIUM'),
('cơn mưa', 'NATURE', 'EASY'),
('bãi biển', 'NATURE', 'MEDIUM'),

-- Tình yêu (Love)
('trái tim', 'LOVE', 'EASY'),
('nụ hôn', 'LOVE', 'MEDIUM'),
('đôi uyên ương', 'LOVE', 'HARD'),
('bó hoa hồng', 'LOVE', 'MEDIUM'),
('chiếc nhẫn', 'LOVE', 'MEDIUM'),
('thiệp valentine', 'LOVE', 'HARD'),
('cặp đôi', 'LOVE', 'MEDIUM'),
('buổi hẹn hò', 'LOVE', 'HARD'),
('nến và hoa', 'LOVE', 'MEDIUM'),
('chocolate', 'LOVE', 'MEDIUM'),

-- Hoạt động (Activities)
('đang ngủ', 'ACTIVITY', 'EASY'),
('đang ăn', 'ACTIVITY', 'EASY'),
('đang chạy', 'ACTIVITY', 'EASY'),
('đang bơi', 'ACTIVITY', 'MEDIUM'),
('đang đọc sách', 'ACTIVITY', 'MEDIUM'),
('đang nấu ăn', 'ACTIVITY', 'HARD'),
('đang nhảy múa', 'ACTIVITY', 'MEDIUM'),
('đang hát', 'ACTIVITY', 'MEDIUM'),
('đang chơi game', 'ACTIVITY', 'HARD'),
('đang selfie', 'ACTIVITY', 'HARD');

-- Seed Stickers
INSERT INTO sticker_packs (name, description) VALUES ('Pepe The Frog', 'Bộ sticker ếch xanh huyền thoại');
INSERT INTO stickers (pack_id, file_url, code) VALUES 
(1, 'stickers/pepe/cry.png', ':pepe_cry:'),
(1, 'stickers/pepe/happy.png', ':pepe_happy:'),
(1, 'stickers/pepe/ok.png', ':pepe_ok:');

-- =======================================================================================
-- DATING/MATCHING SYSTEM - Ghép đôi qua game
-- =======================================================================================

-- 9. USER INTERESTS (Sở thích) - Bảng users đã có profile fields rồi
CREATE TABLE interest_categories (
    category_id INT AUTO_INCREMENT PRIMARY KEY,
    category_name VARCHAR(50) CHARACTER SET utf8mb4 NOT NULL,
    icon VARCHAR(10) DEFAULT '🎯'
) ENGINE=InnoDB;

CREATE TABLE interests (
    interest_id INT AUTO_INCREMENT PRIMARY KEY,
    category_id INT NOT NULL,
    interest_name VARCHAR(100) CHARACTER SET utf8mb4 NOT NULL,
    FOREIGN KEY (category_id) REFERENCES interest_categories(category_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE user_interests (
    user_id BIGINT NOT NULL,
    interest_id INT NOT NULL,
    PRIMARY KEY (user_id, interest_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (interest_id) REFERENCES interests(interest_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 11. USER PHOTOS
CREATE TABLE user_photos (
    photo_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    photo_url VARCHAR(500) NOT NULL,
    is_primary BOOLEAN DEFAULT FALSE,
    display_order INT DEFAULT 0,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_user_photos (user_id, display_order)
) ENGINE=InnoDB;

-- 12. LIKES/SWIPES (Thích/Bỏ qua)
CREATE TABLE user_likes (
    liker_id BIGINT NOT NULL,
    liked_id BIGINT NOT NULL,
    like_type ENUM('LIKE', 'SUPER_LIKE', 'PASS') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (liker_id, liked_id),
    FOREIGN KEY (liker_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (liked_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_liked (liked_id)
) ENGINE=InnoDB;

-- 13. MATCHES (Khi cả 2 thích nhau)
CREATE TABLE matches (
    match_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id1 BIGINT NOT NULL,
    user_id2 BIGINT NOT NULL,
    matched_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status ENUM('ACTIVE', 'UNMATCHED', 'BLOCKED') DEFAULT 'ACTIVE',
    last_interaction TIMESTAMP NULL,
    games_played INT DEFAULT 0,
    total_compatibility DECIMAL(5,2) DEFAULT 0,
    FOREIGN KEY (user_id1) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id2) REFERENCES users(user_id) ON DELETE CASCADE,
    UNIQUE KEY uq_match (user_id1, user_id2),
    CHECK (user_id1 < user_id2)
) ENGINE=InnoDB;

-- 14. COMPATIBILITY SCORES (Điểm tương thích đơn giản)
-- Lưu tổng điểm tương thích giữa 2 người dựa trên game đã chơi
CREATE TABLE compatibility_scores (
    user1_id BIGINT NOT NULL,
    user2_id BIGINT NOT NULL,
    game_points INT DEFAULT 0,           -- Điểm từ chơi game cùng nhau (max 50)
    games_played INT DEFAULT 0,          -- Số game đã chơi cùng nhau
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    PRIMARY KEY (user1_id, user2_id),
    FOREIGN KEY (user1_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (user2_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CHECK (user1_id < user2_id)
) ENGINE=InnoDB;

-- 15. GAME INTERACTION STATS (Thống kê tương tác qua game)
CREATE TABLE game_interaction_stats (
    user_id1 BIGINT NOT NULL,
    user_id2 BIGINT NOT NULL,
    total_games INT DEFAULT 0,
    total_time_together_minutes INT DEFAULT 0,
    games_won_user1 INT DEFAULT 0,
    games_won_user2 INT DEFAULT 0,
    games_draw INT DEFAULT 0,
    avg_chemistry_score DECIMAL(5,2) DEFAULT 0,
    avg_fun_score DECIMAL(5,2) DEFAULT 0,
    avg_communication_score DECIMAL(5,2) DEFAULT 0,
    overall_compatibility DECIMAL(5,2) DEFAULT 0,
    last_played TIMESTAMP NULL,
    first_played TIMESTAMP NULL,
    PRIMARY KEY (user_id1, user_id2),
    FOREIGN KEY (user_id1) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id2) REFERENCES users(user_id) ON DELETE CASCADE,
    CHECK (user_id1 < user_id2)
) ENGINE=InnoDB;

-- SEED INTEREST CATEGORIES
INSERT INTO interest_categories (category_name, icon) VALUES
('Âm nhạc', '🎵'),
('Phim ảnh', '🎬'),
('Thể thao', '⚽'),
('Du lịch', '✈️'),
('Ẩm thực', '🍜'),
('Đọc sách', '📚'),
('Gaming', '🎮'),
('Nghệ thuật', '🎨'),
('Công nghệ', '💻'),
('Thú cưng', '🐾');

-- SEED INTERESTS
INSERT INTO interests (category_id, interest_name) VALUES
-- Âm nhạc
(1, 'Nhạc Pop'), (1, 'Nhạc Rock'), (1, 'Nhạc EDM'), (1, 'Nhạc Bolero'), (1, 'K-Pop'),
(1, 'Nhạc Indie'), (1, 'R&B'), (1, 'Hip Hop'), (1, 'Nhạc cổ điển'), (1, 'Chơi nhạc cụ'),
-- Phim ảnh
(2, 'Phim hành động'), (2, 'Phim tình cảm'), (2, 'Phim kinh dị'), (2, 'Phim hoạt hình'),
(2, 'Phim Hàn'), (2, 'Phim Marvel'), (2, 'Phim tài liệu'), (2, 'Anime'),
-- Thể thao
(3, 'Bóng đá'), (3, 'Bóng rổ'), (3, 'Cầu lông'), (3, 'Gym'), (3, 'Yoga'),
(3, 'Chạy bộ'), (3, 'Bơi lội'), (3, 'Leo núi'), (3, 'Võ thuật'),
-- Du lịch
(4, 'Biển đảo'), (4, 'Núi rừng'), (4, 'Phượt'), (4, 'Du lịch nước ngoài'),
(4, 'Cắm trại'), (4, 'Khám phá ẩm thực'), (4, 'Check-in'),
-- Ẩm thực
(5, 'Nấu ăn'), (5, 'Đồ ăn Việt'), (5, 'Đồ ăn Hàn'), (5, 'Đồ ăn Nhật'),
(5, 'Đồ ăn Tây'), (5, 'Trà sữa'), (5, 'Cafe'), (5, 'Ăn chay'),
-- Đọc sách
(6, 'Tiểu thuyết'), (6, 'Sách self-help'), (6, 'Truyện tranh'), (6, 'Light novel'),
(6, 'Sách kinh tế'), (6, 'Sách tâm lý'), (6, 'Thơ văn'),
-- Gaming
(7, 'MOBA'), (7, 'FPS'), (7, 'RPG'), (7, 'Board games'), (7, 'Mobile games'),
(7, 'Console'), (7, 'PC Gaming'), (7, 'Esports'),
-- Nghệ thuật
(8, 'Vẽ tranh'), (8, 'Chụp ảnh'), (8, 'Thiết kế'), (8, 'Thời trang'),
(8, 'Handmade'), (8, 'Âm nhạc'), (8, 'Khiêu vũ'),
-- Công nghệ
(9, 'Lập trình'), (9, 'AI/ML'), (9, 'Crypto'), (9, 'Startup'),
(9, 'Gadgets'), (9, 'Social Media'),
-- Thú cưng
(10, 'Chó'), (10, 'Mèo'), (10, 'Hamster'), (10, 'Cá cảnh'), (10, 'Chim');

-- PROCEDURE: Tính điểm tương thích sau mỗi game
DELIMITER $$

CREATE PROCEDURE sp_calculate_compatibility(
    IN p_session_id BIGINT,
    OUT p_compatibility DECIMAL(5,2)
)
BEGIN
    DECLARE v_user1 BIGINT;
    DECLARE v_user2 BIGINT;
    DECLARE v_winner BIGINT;
    DECLARE v_game_code VARCHAR(20);
    DECLARE v_duration INT;
    DECLARE v_moves INT;
    DECLARE v_chemistry INT DEFAULT 50;
    DECLARE v_fun INT DEFAULT 50;
    DECLARE v_communication INT DEFAULT 50;
    DECLARE v_sportsmanship INT DEFAULT 50;
    DECLARE v_close_match BOOLEAN DEFAULT FALSE;
    
    -- Lấy thông tin session
    SELECT gs.player1_id, gs.player2_id, gs.winner_id, g.game_code, 
           gs.duration_seconds, gs.total_moves
    INTO v_user1, v_user2, v_winner, v_game_code, v_duration, v_moves
    FROM game_sessions gs
    JOIN games g ON gs.game_id = g.game_id
    WHERE gs.session_id = p_session_id;
    
    -- Tính điểm dựa trên loại game và kết quả
    
    -- Chemistry: Dựa trên thời gian chơi và số nước đi
    IF v_duration > 0 THEN
        SET v_chemistry = LEAST(100, 50 + (v_duration / 60) * 5);
    END IF;
    
    -- Fun: Dựa trên việc game có sít sao không
    IF v_moves > 10 THEN
        SET v_fun = LEAST(100, 50 + v_moves);
        SET v_close_match = TRUE;
    END IF;
    
    -- Communication: Bonus cho các game cần giao tiếp
    IF v_game_code IN ('WORD_CHAIN', 'LOVE_QUIZ', 'DRAW_GUESS') THEN
        SET v_communication = 70;
    END IF;
    
    -- Sportsmanship: Bonus nếu game kết thúc bình thường (không quit)
    SET v_sportsmanship = 80;
    
    -- Insert compatibility score
    INSERT INTO compatibility_scores (
        user_id1, user_id2, game_session_id, game_code,
        chemistry_score, fun_score, communication_score, sportsmanship_score,
        winner_id, game_duration_seconds, total_moves, close_match
    ) VALUES (
        v_user1, v_user2, p_session_id, v_game_code,
        v_chemistry, v_fun, v_communication, v_sportsmanship,
        v_winner, v_duration, v_moves, v_close_match
    );
    
    -- Update game interaction stats
    INSERT INTO game_interaction_stats (user_id1, user_id2, total_games, first_played, last_played,
        games_won_user1, games_won_user2, games_draw, avg_chemistry_score, avg_fun_score,
        avg_communication_score, overall_compatibility)
    VALUES (v_user1, v_user2, 1, NOW(), NOW(),
        IF(v_winner = v_user1, 1, 0),
        IF(v_winner = v_user2, 1, 0),
        IF(v_winner IS NULL, 1, 0),
        v_chemistry, v_fun, v_communication,
        (v_chemistry + v_fun + v_communication + v_sportsmanship) / 4)
    ON DUPLICATE KEY UPDATE
        total_games = total_games + 1,
        last_played = NOW(),
        games_won_user1 = games_won_user1 + IF(v_winner = v_user1, 1, 0),
        games_won_user2 = games_won_user2 + IF(v_winner = v_user2, 1, 0),
        games_draw = games_draw + IF(v_winner IS NULL, 1, 0),
        avg_chemistry_score = (avg_chemistry_score * (total_games - 1) + v_chemistry) / total_games,
        avg_fun_score = (avg_fun_score * (total_games - 1) + v_fun) / total_games,
        avg_communication_score = (avg_communication_score * (total_games - 1) + v_communication) / total_games,
        overall_compatibility = (avg_chemistry_score + avg_fun_score + avg_communication_score + v_sportsmanship) / 4;
    
    -- Return overall compatibility
    SET p_compatibility = (v_chemistry + v_fun + v_communication + v_sportsmanship) / 4;
    
    -- Check và tạo match nếu cả 2 đã thích nhau
    IF EXISTS (
        SELECT 1 FROM user_likes 
        WHERE liker_id = v_user1 AND liked_id = v_user2 AND like_type IN ('LIKE', 'SUPER_LIKE')
    ) AND EXISTS (
        SELECT 1 FROM user_likes 
        WHERE liker_id = v_user2 AND liked_id = v_user1 AND like_type IN ('LIKE', 'SUPER_LIKE')
    ) THEN
        INSERT IGNORE INTO matches (user_id1, user_id2, games_played, total_compatibility)
        VALUES (LEAST(v_user1, v_user2), GREATEST(v_user1, v_user2), 1, p_compatibility)
        ON DUPLICATE KEY UPDATE
            games_played = games_played + 1,
            total_compatibility = (total_compatibility * (games_played - 1) + p_compatibility) / games_played,
            last_interaction = NOW();
    END IF;
END$$

DELIMITER ;

-- Test
CALL sp_create_game_session(2, 3, 'CARO', @sid, @err);
SELECT @sid, @err;

SELECT VERSION();