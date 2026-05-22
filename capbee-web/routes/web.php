<?php

use Illuminate\Support\Facades\Route;
use App\Http\Controllers\OtpVerificationController;

Route::inertia('/', 'welcome')->name('home');

Route::middleware('auth')->group(function () {
    Route::post('/email/otp/send', [OtpVerificationController::class, 'send']);
    Route::post('/email/otp/verify', [OtpVerificationController::class, 'verify']);
});

Route::middleware(['auth', 'verified'])->group(function () {
    Route::inertia('dashboard', 'dashboard')->name('dashboard');
});

require __DIR__ . '/settings.php';
