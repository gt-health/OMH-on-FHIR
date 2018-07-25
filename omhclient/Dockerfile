#Option 1: build project and run with Node
FROM node:10 as builder

# make a working directory
RUN mkdir -p /usr/local/app

# set the working directory
WORKDIR /usr/local/app

# copy dependency definitions
COPY package.json /usr/local/app

# install dependencies
#RUN npm install
RUN npm install -g @angular/cli

# copy over code
COPY . /usr/local/app
#old code
# expose port 4200
#EXPOSE 4200

#Start the server
#CMD ["npm", "start", "--host 0.0.0.0", "--disable-host-check true"]
# set environment variables
ENV NODE_ENV production

#new build code
#RUN $(npm bin)/ng build --base-href .
RUN ng build --base-href .

FROM tomcat:latest
COPY --from=builder /usr/local/app/dist/omhclient /usr/local/tomcat/webapps/omhonfhir
