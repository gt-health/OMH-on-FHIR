#!/usr/bin/env groovy
pipeline{
    agent any

    environment {
        GTRI_IMAGE_REGISTRY = credentials('gtri-image-registry-url')
        GTRI_RANCHER_API_ENDPOINT = credentials('gtri-rancher-api-endpoint')
        GTRI_HDAP_ENV_ID = credentials('gtri-hdap-env-id')
    }

    //Define stages for the build process
    stages{
        //Define the deploy stage
        stage('Deploy'){
            steps{
                //The Jenkins Declarative Pipeline does not provide functionality to deploy to a private
                //Docker registry. In order to deploy to the HDAP Docker registry we must write a custom Groovy
                //script using the Jenkins Scripting Pipeline. This is done by placing Groovy code with in a "script"
                //element. The script below registers the HDAP Docker registry with the Docker instance used by
                //the Jenkins Pipeline, builds a Docker image of the project, and pushes it to the registry.
                script{
                    docker.withRegistry("${GTRI_IMAGE_REGISTRY}"){
                        //Build and push the database image
                        def webApiImage = docker.build("omhonfhirapp:1.0", "--no-cache -f ./omhserver/Dockerfile ./omhserver")
                        webApiImage.push('latest')

                        //Build and push the database image
                        def uiImage = docker.build("omhonfhirui:1.0", "--no-cache -f ./omhclient/Dockerfile ./omhclient")
                        uiImage.push('latest')
                    }
                }
            }
        }

        //Define stage to notify rancher
        stage('Notify'){
            steps{
                script{
                    rancher confirm: true, credentialId: 'gt-rancher-server', endpoint: "${GTRI_RANCHER_API_ENDPOINT}", environmentId: "${GTRI_HDAP_ENV_ID}", environments: '', image: 'openmhealth/shimmer-resource-server', ports: '', service: 'OMHonFHIR/resource-server', timeout: 50
                    rancher confirm: true, credentialId: 'gt-rancher-server', endpoint: "${GTRI_RANCHER_API_ENDPOINT}", environmentId: "${GTRI_HDAP_ENV_ID}", environments: '', image: 'mongo', ports: '', service: 'OMHonFHIR/mongo', timeout: 50
                    rancher confirm: true, credentialId: 'gt-rancher-server', endpoint: "${GTRI_RANCHER_API_ENDPOINT}", environmentId: "${GTRI_HDAP_ENV_ID}", environments: '', image: 'openmhealth/shimmer-console', ports: '', service: 'OMHonFHIR/console', timeout: 50
                    rancher confirm: true, credentialId: 'gt-rancher-server', endpoint: "${GTRI_RANCHER_API_ENDPOINT}", environmentId: "${GTRI_HDAP_ENV_ID}", environments: '', image: 'postgres:latest', ports: '', service: 'OMHonFHIR/mdata-db', timeout: 50
                    rancher confirm: true, credentialId: 'gt-rancher-server', endpoint: "${GTRI_RANCHER_API_ENDPOINT}", environmentId: "${GTRI_HDAP_ENV_ID}", environments: '', image: 'gt-build.hdap.gatech.edu/omhonfhirapp:latest', ports: '', service: 'OMHonFHIR/mdata-app', timeout: 50
                    rancher confirm: true, credentialId: 'gt-rancher-server', endpoint: "${GTRI_RANCHER_API_ENDPOINT}", environmentId: "${GTRI_HDAP_ENV_ID}", environments: '', image: 'gt-build.hdap.gatech.edu/omhonfhirui:latest', ports: '', service: 'OMHonFHIR/omh-on-fhir-client', timeout: 50
                }
            }
        }
    }
}