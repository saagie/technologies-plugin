# technologies-plugin
Gradle plugin for Saagie technologies repository

Provides all necessary tasks to build and package technologies. 


Some tasks are available : 

 - `:buildDockerImage` (in group : `technologies`)  
 For a job technology version :   
 It will build the docker image, test it, push it in docker repository, and generate the metadata.yml.
 - `:packageAllVersions` (in group `technologies`)
 It will generate a package containing all metadata.yml previously generated.
 - `:promote` (in group `technologies`)
 It will fix all versions in metadata.yml (to have real production version), generate the package and retag all docker image and push it in docker repository.
 
 For the `:buildDockerImage` and `:promote` you need to set theses environment variables:  
 - DOCKER_USERNAME
 - DOCKER_PASSWORD