import { Head, router, useForm, usePage } from '@inertiajs/react';
import { dashboard } from '@/routes';
import { Button } from '@/components/ui/button';
import type { PageProps as InertiaPageProps } from '@inertiajs/core';

interface User {
    id: number;
    name: string;
    email: string;
    email_verified_at: string | null;
}

interface PageProps extends InertiaPageProps {
    auth: {
        user: User;
    };
}

export default function Dashboard() {
    const { auth } = usePage<PageProps>().props;
    const user = auth.user;

    const verifyForm = useForm({ otp: '' });
    const resendForm = useForm({});

    const handleVerify = () => {
        verifyForm.post('/email/otp/verify', {
            onSuccess: () => router.reload(),
        });
    };

    const handleResend = () => {
        resendForm.post('/email/otp/send');
    };

    return (
        <>
            <Head title="Dashboard" />
            <div className="flex h-full flex-1 flex-col gap-4 overflow-x-auto rounded-xl p-4">
                <div className="lg:min-h-screen flex items-center justify-center p-4">

                    {!user.email_verified_at ? (
                        <div className="bg-muted/40 border rounded-lg shadow-lg p-8 max-w-md w-full text-center">
                            {/* Email Icon */}
                            <div className="mb-6 flex justify-center">
                                <div className="bg-blue-100 rounded-full p-4">
                                    <svg className="w-8 h-8 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                                            d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
                                    </svg>
                                </div>
                            </div>

                            <h1 className="text-2xl font-bold text-foreground mb-2">Verify your email</h1>
                            <p className="text-muted-foreground mb-1">We sent a 6-digit code to</p>
                            <p className="font-medium text-foreground mb-6">{user.email}</p>

                            {/* OTP Input */}
                            <input
                                type="text"
                                maxLength={6}
                                value={verifyForm.data.otp}
                                onChange={(e) => verifyForm.setData('otp', e.target.value.replace(/\D/g, ''))}
                                placeholder="Enter 6-digit code"
                                className="w-full text-center text-2xl tracking-widest border rounded-lg px-4 py-3 mb-4 bg-background focus:outline-none focus:ring-2 focus:ring-blue-500"
                            />

                            {/* Error */}
                            {verifyForm.errors.otp && (
                                <p className="text-red-500 text-sm mb-4">{verifyForm.errors.otp}</p>
                            )}

                            {/* Resend success */}
                            {resendForm.wasSuccessful && (
                                <p className="text-green-500 text-sm mb-4">A new code has been sent to your email.</p>
                            )}

                            {/* Resend error */}
                            {resendForm.hasErrors && (
                                <p className="text-red-500 text-sm mb-4">Failed to resend OTP.</p>
                            )}

                            <Button
                                className="w-full mb-3"
                                onClick={handleVerify}
                                disabled={verifyForm.data.otp.length !== 6 || verifyForm.processing}
                            >
                                {verifyForm.processing ? 'Verifying...' : 'Verify Email'}
                            </Button>

                            <p className="text-sm text-muted-foreground">
                                Didn't receive a code?{' '}
                                <button
                                    onClick={handleResend}
                                    disabled={resendForm.processing}
                                    className="text-blue-500 hover:underline disabled:opacity-50"
                                >
                                    {resendForm.processing ? 'Sending...' : 'Resend'}
                                </button>
                            </p>
                        </div>
                    ) : (
                        <div className="bg-muted/40 border rounded-lg shadow-lg p-8 max-w-md w-full text-center">
                            <div className="mb-6 flex justify-center">
                                <div className="bg-muted rounded-full p-4">
                                    <svg className="w-8 h-8 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                                    </svg>
                                </div>
                            </div>
                            <h1 className="text-3xl font-bold text-foreground mb-2">You're all set!</h1>
                            <p className="text-muted-foreground">Your account is verified and ready to go.</p>
                            <p className="text-gray-500 text-sm mb-4">
                                Start exploring, and feel the way.
                            </p>
                            <Button>Get Started</Button>
                        </div>
                    )}

                </div>
            </div>
        </>
    );
}

Dashboard.layout = {
    breadcrumbs: [
        {
            title: 'Dashboard',
            href: dashboard(),
        },
    ],
};