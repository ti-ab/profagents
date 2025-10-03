FROM node:20-alpine
WORKDIR /app

ARG NODE_ENV=development
ENV NODE_ENV=$NODE_ENV

COPY . .

RUN yarn install

# Build uniquement en production
RUN if [ "$NODE_ENV" = "production" ]; then npm run build; fi

# Entrée via un shell script (voir plus bas)
COPY start.sh .
RUN chmod +x start.sh

CMD ["./start.sh"]
