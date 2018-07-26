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
RUN npm install && npm install -g @angular/cli && npm update -g @angular/cli

# copy over code
COPY . /usr/local/app

#new build code
#RUN $(npm bin)/ng build --base-href .
RUN ng build --prod --base-href .

FROM tomcat:latest
COPY --from=builder /usr/local/app/dist/omhclient /usr/local/tomcat/webapps/omhonfhir
