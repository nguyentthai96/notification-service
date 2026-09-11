-- Seed notification templates

-- New Device Login (Vietnamese)
INSERT INTO notification_template (id, code, name, channel, subject_template, body_template, tracking_mode, language, active, created_at, updated_at)
VALUES (
    1,
    'NEW_DEVICE_LOGIN',
    'Thông báo đăng nhập từ thiết bị mới',
    'EMAIL',
    'Cảnh báo bảo mật: Đăng nhập từ thiết bị mới',
    E'Xin chào {{userName}},\n\nChúng tôi phát hiện tài khoản của bạn vừa được đăng nhập từ một thiết bị mới:\n\n- Thiết bị: {{deviceName}}\n- Loại: {{deviceType}}\n- Trình duyệt: {{browserName}}\n- Hệ điều hành: {{osName}}\n- Địa chỉ IP: {{ipAddress}}\n- Thời gian: {{loginTime}}\n\nNếu đây không phải bạn, vui lòng đổi mật khẩu ngay lập tức và kiểm tra danh sách thiết bị đang hoạt động.\n\nTrân trọng,\nĐội ngũ Bảo mật',
    'NONE',
    'vi',
    TRUE,
    NOW(),
    NOW()
);

-- Password Reset (Vietnamese)
INSERT INTO notification_template (id, code, name, channel, subject_template, body_template, tracking_mode, language, active, created_at, updated_at)
VALUES (
    2,
    'PASSWORD_RESET',
    'Yêu cầu đặt lại mật khẩu',
    'EMAIL',
    'Yêu cầu đặt lại mật khẩu',
    E'Xin chào {{userName}},\n\nChúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn.\n\nNhấn vào liên kết sau để đặt lại mật khẩu:\n{{resetLink}}\n\nLiên kết này sẽ hết hạn sau {{expiryMinutes}} phút.\n\nNếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.\n\nTrân trọng,\nĐội ngũ Bảo mật',
    'NONE',
    'vi',
    TRUE,
    NOW(),
    NOW()
);

-- OTP Verification (Vietnamese)
INSERT INTO notification_template (id, code, name, channel, subject_template, body_template, tracking_mode, language, active, created_at, updated_at)
VALUES (
    3,
    'OTP_VERIFICATION',
    'Mã xác thực OTP',
    'EMAIL',
    'Mã xác thực: {{otpCode}}',
    E'Xin chào {{userName}},\n\nMã xác thực OTP của bạn là: {{otpCode}}\n\nMã này sẽ hết hạn sau {{expiryMinutes}} phút.\n\nNếu bạn không yêu cầu mã này, vui lòng bỏ qua email này.\n\nTrân trọng,\nĐội ngũ Bảo mật',
    'NONE',
    'vi',
    TRUE,
    NOW(),
    NOW()
);
