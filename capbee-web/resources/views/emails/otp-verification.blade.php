<!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Verify Your Email</title>
    <style>
        .header,.header h1,.header p,.logo-ring,.otp-container{position:relative}.footer,.header,.otp-container{text-align:center}*{margin:0;padding:0;box-sizing:border-box}body{font-family:'Arial',sans-serif}.wrapper{max-width:560px;margin:0 auto;padding:48px 24px}.card{background:#1d1d1d;border:1px solid #2a2a3a;border-radius:20px;overflow:hidden}.header{background:linear-gradient(135deg,#1a1a2e 0,#16213e 50%,#0f3460 100%);padding:48px 40px 40px}.header::before{content:'';position:absolute;inset:0;background:radial-gradient(ellipse at 50% 0,rgba(99,102,241,.15) 0,transparent 70%)}.logo-ring{width:72px;height:72px;border-radius:50%;background:linear-gradient(135deg,#6366f1,#8b5cf6);display:flex;align-items:center;justify-content:center;margin:0 auto 24px;box-shadow:0 0 40px rgba(99,102,241,.4)}.logo-ring svg{width:32px;height:32px;color:#fff}.header h1{font-size:28px;font-weight:800;color:#fff;letter-spacing:-.5px}.header p{color:#a5b4fc;margin-top:8px;font-size:15px}.body{padding:40px}.greeting{font-size:16px;color:#c4c4d4;margin-bottom:24px;line-height:1.6}.greeting strong{color:#e8e8f0}.otp-container{background:#0a0a0f;border:1px solid #2a2a3a;border-radius:16px;padding:32px;margin:28px 0;overflow:hidden}.otp-code{font-size:28px;font-weight:800;color:#fff;text-shadow:0 0 30px rgba(99,102,241,.5)}.expiry{font-size:13px;color:#6b6b7e;margin-top:16px}.expiry span{color:#f59e0b;font-weight:500}.divider{height:1px;background:#2a2a3a;margin:28px 0}.warning{background:rgba(245,158,11,.08);border:1px solid rgba(245,158,11,.2);border-radius:12px;padding:16px 20px;font-size:13px;color:#d4a843;line-height:1.6}.warning strong{display:block;margin-bottom:4px}.footer{padding:24px 40px;border-top:1px solid #1e1e2a}.footer p{font-size:12px;color:#6b6b7e;line-height:1.8}.app-name{font-weight:700;color:#6366f1;font-size:14px}
    </style>
</head>

<body>
    <div class="wrapper">
        <div class="card">
            <div class="header">
                <h1>Verify your email</h1>
                <p>One step away from getting started</p>
            </div>
            <div class="body">
                <p class="greeting">Hey <strong>{{ $user->name }}</strong>, Use the code below to verify
                    your email address.</p>
                <div class="otp-container">
                    <div class="otp-code">{{ $otp }}</div>
                    <div class="expiry">Expires in <span>10 minutes</span></div>
                </div>
                <div class="divider"></div>
                <div class="warning">
                    <strong>⚠ Keep this code private</strong>
                    If you didn't create an account, you can safely ignore this email. Never share this code with
                    anyone.
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