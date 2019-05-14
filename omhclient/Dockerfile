#Option 1: build project and run with Node
FROM node:8 as builder

# make a working directory
RUN mkdir -p /usr/local/app

# set the working directory
WORKDIR /usr/local/app

# copy dependency definitions
#COPY package.json /usr/local/app
COPY . /usr/local/app

# install dependencies
#RUN npm install
# install bower and then install the project
#RUN ls -la /usr/local/app
#RUN npm install && npm update && npm install -g bower && bower install --allow-root omh-web-visualizations
RUN npm install && npm install -g bower && bower install --allow-root omh-web-visualizations

# copy over project
#COPY . /usr/local/app
#RUN ls -la /usr/local/app/app
#build code
#RUN ng build --prod --base-href=/omhonfhir/

#deploy to NGINX
FROM nginx
#COPY --from=builder /usr/local/app/dist/omhclient /etc/nginx/html/omhonfhir
COPY --from=builder /usr/local/app/app /etc/nginx/html/omhonfhir
COPY --from=builder ./usr/local/app/nginx.conf /etc/nginx/conf.d/default.conf
