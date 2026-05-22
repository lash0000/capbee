import { Head, Link, usePage } from '@inertiajs/react';
import { dashboard, login } from '@/routes';
import { register } from '@/routes';
import { Button } from '@/components/ui/button';
import { Headset } from 'lucide-react';

export default function Welcome() {
    const { auth } = usePage().props;

    return (
        <>
            <Head title="Welcome" />
            <div className="bg-background lg:container lg:mx-auto">
                <header className="py-4 px-6 text-sm not-has-[nav]:hidden flex w-full justify-between items-center">
                    <Button size="sm">
                        <Headset />
                    </Button>
                    <nav className="flex gap-1">
                        {auth.user ? (
                            <Link
                                href={dashboard()}
                            >
                                <Button variant="outline">
                                    Dashboard
                                </Button>
                            </Link>
                        ) : (
                            <>
                                <Link href={login()}>
                                    <Button variant="ghost">
                                        Log in
                                    </Button>
                                </Link>
                                <Link href={register()}>
                                    <Button variant="ghost">
                                        Register
                                    </Button>
                                </Link>
                            </>
                        )}
                    </nav>
                </header>

                <section className="flex flex-1 flex-col items-center justify-center px-6 py-12 text-center md:px-12 lg:px-16">
                    <div className="max-w-2xl space-y-6">
                        <h1 className="text-balance text-4xl font-bold tracking-tight text-foreground md:text-5xl lg:text-6xl">
                            Trust and Patience
                        </h1>
                        <p className="text-pretty text-lg leading-relaxed text-muted-foreground md:text-xl">
                            Create stunning web experiences with modern design and cutting-edge technology. Your vision deserves a platform that delivers.
                        </p>
                        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-center">
                            <Button>Try now</Button>
                            <Button variant="ghost">Explore</Button>
                        </div>
                    </div>

                    {/* Hero Image/Banner */}
                    <div className="mt-12 w-full max-w-2xl">
                        <div className="aspect-video rounded-xl bg-gradient-to-br from-muted to-muted/50 flex items-center justify-center border border-border">
                            <div className="text-center">
                                <p className="text-muted-foreground text-sm">Hero Banner</p>
                                <p className="text-muted-foreground text-xs mt-1">Your image or content here</p>
                            </div>
                        </div>
                    </div>
                </section>
                
            </div>
        </>
    );
}
