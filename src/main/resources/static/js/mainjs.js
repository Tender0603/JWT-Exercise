$(document).ready(function () {
    // If on profile page, load authenticated user info
    if ($('#profile').length > 0) {
        $.ajax({
            type: 'GET',
            url: '/users/me',
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            beforeSend: function (xhr) {
                if (localStorage.token) {
                    xhr.setRequestHeader('Authorization', 'Bearer ' + localStorage.token);
                }
            },
            success: function (data) {
                $('#profile').html(data.fullName);
                $('#profileDetails').html('<strong>Email:</strong> ' + data.email);

                var imgPath = data.images;
                if (!imgPath || imgPath.trim() === '') {
                    imgPath = 'default.jpg';
                }
                if (!imgPath.startsWith('http') && !imgPath.startsWith('/')) {
                    imgPath = '/images/' + imgPath;
                }
                $('#images').attr('src', imgPath);
            },
            error: function (xhr) {
                var json = xhr.responseText;
                $('#feedback').html('<div class="alert alert-danger">Error: Unauthorized access or expired token.</div>');
                setTimeout(function () {
                    window.location.href = '/login';
                }, 2000);
            }
        });
    }

    // Login Action
    $('#Login').click(function (e) {
        e.preventDefault();
        var email = $('#email').val();
        var password = $('#password').val();

        if (!email || !password) {
            $('#feedback').html('<div class="alert alert-warning">Vui lòng nhập email và mật khẩu.</div>');
            return;
        }

        var basicInfo = JSON.stringify({
            email: email,
            password: password
        });

        $.ajax({
            type: 'POST',
            url: '/auth/login',
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            data: basicInfo,
            success: function (data) {
                localStorage.token = data.token;
                $('#feedback').html('<div class="alert alert-success">Đăng nhập thành công! Đang chuyển hướng...</div>');
                setTimeout(function () {
                    window.location.href = "/user/profile";
                }, 500);
            },
            error: function (xhr) {
                $('#feedback').html('<div class="alert alert-danger">Đăng nhập thất bại! Email hoặc mật khẩu không chính xác.</div>');
            }
        });
    });

    // Logout Action
    $(document).on('click', '#logout', function (e) {
        e.preventDefault();
        localStorage.clear();
        window.location.href = "/login";
    });
});
