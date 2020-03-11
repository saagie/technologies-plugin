# technologies-plugin

[Gradle](https://gradle.org/) plugin for Saagie technologies repository

Provides all necessary tasks to build and package technologies. 


Available tasks : 

 - `:buildDockerImage` (in group : `technologies`)  
 For a job technology context :   
 It will build the docker image, test it, push it in docker repository, and generate a dockerInfo.yml containing the tag of pushed docker image.
 - `:packageAllVersions` (in group `technologies`)
 It will generate all metadata.yml and package them into a zip.
 - `:promote` (in group `technologies`)
 It will fix all versions in metadata.yml (to have real production version), generate the zip file and re-tag all docker image and push them in docker repository.
 
 For the `:buildDockerImage` and `:promote` you need to set these environment variables:  
 
 - DOCKER_USERNAME
 - DOCKER_PASSWORD
 
 An example of usage of this plugin is available here : https://github.com/saagie/technologies/
 
 ## Integrate this plugin to your repository
 
 You just need to add `classpath("com.saagie:technologiesplugin:1.0.29")` to your `build.gradle.kts`.
 Here is an example of a [build.gradle.kts](https://github.com/saagie/technologies/blob/master/build.gradle.kts)
 
 
 ## CI/CD
 
 You can use the CI/CD tool you want to automate your repository workflow. The main tasks are done by the gradle plugin.
 Here is an example of a CI/CD using [Github actions](https://github.com/saagie/technologies/tree/master/.github/workflows)