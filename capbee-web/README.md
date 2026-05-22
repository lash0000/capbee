A laravel setup for DB just ask to ask.

## Project Type: Laravel and React

## Installation

Install dependencies:

```bash
composer install
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