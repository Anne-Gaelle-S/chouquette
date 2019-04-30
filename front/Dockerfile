FROM node:10.15.3

EXPOSE 8080

ENV BASEDIR=/chouquette

RUN mkdir $BASEDIR
WORKDIR $BASEDIR
COPY . .
RUN npm i --production
RUN npm run clean
RUN npm run build

ENTRYPOINT npm run serve
