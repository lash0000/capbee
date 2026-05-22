<?php

namespace App\Http\Controllers;

use App\Mail\EmailVerifiedMail;
use App\Mail\OtpVerificationMail;
use App\Models\EmailVerificationOtp;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Mail;

class OtpVerificationController extends Controller
{
    public function send(Request $request)
    {
        $user = $request->user();

        if ($user->hasVerifiedEmail()) {
            return back()->with('message', 'Email already verified.');
        }

        EmailVerificationOtp::where('user_id', $user->id)->delete();

        $otp = str_pad(random_int(0, 999999), 6, '0', STR_PAD_LEFT);

        EmailVerificationOtp::create([
            'user_id'    => $user->id,
            'otp'        => $otp,
            'expires_at' => now()->addMinutes(10),
        ]);

        Mail::to($user->email)->send(new OtpVerificationMail($user, $otp));

        return back()->with('message', 'OTP sent to your email.');
    }

    public function verify(Request $request)
    {
        $request->validate([
            'otp' => 'required|string|size:6',
        ]);

        $user = $request->user();

        $record = EmailVerificationOtp::where('user_id', $user->id)
            ->where('otp', $request->otp)
            ->latest()
            ->first();

        if (!$record) {
            return back()->withErrors(['otp' => 'Invalid OTP.']);
        }

        if ($record->isExpired()) {
            $record->delete();
            return back()->withErrors(['otp' => 'OTP has expired. Please request a new one.']);
        }

        $user->markEmailAsVerified();
        $record->delete();

        Mail::to($user->email)->send(new EmailVerifiedMail($user));

        return back();
    }
}