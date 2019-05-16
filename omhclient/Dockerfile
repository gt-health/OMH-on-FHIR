#Option 1: build project and run with Node
FROM node:8 as builder

ARG buildEnv=prod

# make a working directory
RUN mkdir -p /usr/local/omhclient

# set the working directory
WORKDIR /usr/local/omhclient

# copy dependency definitions
COPY . /usr/local/omhclient

#copy over the js environment file for the environment we are building
COPY app/js/${buildEnv}-env.js /usr/local/omhclient/app/js/env.js

# install bower and then install the project
RUN rm /usr/local/omhclient/app/js/*-env.js && npm install && npm install -g bower && bower install --allow-root omh-web-visualizations

#deploy to NGINX
FROM nginx
COPY --from=builder /usr/local/omhclient/app /etc/nginx/html/omhonfhir
COPY --from=builder ./usr/local/omhclient/nginx.conf /etc/nginx/conf.d/default.conf
