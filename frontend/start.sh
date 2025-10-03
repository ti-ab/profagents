#!/bin/sh

if [ "$NODE_ENV" = "production" ]; then
  echo "🚀 Starting in production mode..."
  exec npm run start
else
  echo "🛠 Starting in development mode..."
  exec npm run dev
fi
