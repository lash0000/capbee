A laravel setup for DB setup just ask to ask.

## Installation

Install dependencies:

```bash
composer install
npm install
```

Setup environment:

```bash
cp .env.example .env
php artisan key:generate
```

Run migration:
```bash
php artisan migrate
```

Start development server:
```bash
composer run dev
```