<!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Email Verified</title>
    <style>
        .header,.header h1,.header p,.logo-ring{position:relative}.cta,.footer,.header,.success-box{text-align:center}*{margin:0;padding:0;box-sizing:border-box}body{font-family:'Arial',sans-serif}.wrapper{max-width:560px;margin:0 auto;padding:48px 24px}.card{background:#1d1d1d;border:1px solid #2a2a3a;border-radius:20px;overflow:hidden}.header{background:linear-gradient(135deg,#0d2818 0,#0a3d1f 50%,#065f30 100%);padding:48px 40px 40px}.header::before{content:'';position:absolute;inset:0;background:radial-gradient(ellipse at 50% 0,rgba(34,197,94,.15) 0,transparent 70%)}.logo-ring{width:72px;height:72px;border-radius:50%;background:linear-gradient(135deg,#22c55e,#16a34a);display:flex;align-items:center;justify-content:center;margin:0 auto 24px;box-shadow:0 0 40px rgba(34,197,94,.4)}.logo-ring svg{width:36px;height:36px;color:#fff}.header h1{font-size:28px;font-weight:800;color:#fff;letter-spacing:-.5px}.app-name,.btn,.success-box h2{font-weight:700}.header p{color:#86efac;margin-top:8px;font-size:15px}.body{padding:40px}.greeting{font-size:16px;color:#c4c4d4;margin-bottom:24px;line-height:1.7}.greeting strong{color:#e8e8f0}.success-box{background:rgba(34,197,94,.06);border:1px solid rgba(34,197,94,.2);border-radius:16px;padding:28px;margin:28px 0}.check-badge{font-size:40px;margin-bottom:12px}.success-box h2{font-family:Syne,sans-serif;font-size:20px;color:#22c55e;margin-bottom:8px}.success-box p{font-size:14px;color:#6b6b7e}.divider{height:1px;background:#2a2a3a;margin:28px 0}.cta p{font-size:14px;color:#8888a0;margin-bottom:20px}.btn{display:inline-block;background:#16a34a;color:#fff;text-decoration:none;font-size:15px;padding:14px 36px;border-radius:12px;border:none;outline:none;box-shadow:none;-webkit-appearance:none;appearance:none;}.footer{padding:24px 40px;border-top:1px solid #1e1e2a}.footer p{font-size:12px;color:#6b6b7e;line-height:1.8}.app-name{color:#22c55e;font-size:14px}
    </style>
</head>

<body>
    <div class="wrapper">
        <div class="card">
            <div class="header">
                <h1>You're verified!</h1>
                <p>Your account is ready to go</p>
            </div>
            <div class="body">
                <p class="greeting">Hey <strong>{{ $user->name }}</strong>, your email address has been successfully
                    verified. You now have full access to your account.</p>
                <div class="divider"></div>
                <div class="cta">
                    <p>Ready to get started? Head back to the app.</p>
                    <a href="{{ config('app.url') }}/dashboard" class="btn">Go to Dashboard →</a>
                </div>
            </div>
            <div class="footer">
                <p class="app-name">{{ config('app.name') }}</p>
                <p>This is an automated message, please do not reply.</p>
            </div>
        </div>
    </div>
</body>

</html>