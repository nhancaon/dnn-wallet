pipeline {
    agent {
        label 'ubuntu-server'
    }
    environment {
        appUser = "wallet"
        appName = "OnlineBankingApp"
        appVersion = "0.0.1-SNAPSHOT"
        appType = "war"
        processName = "${appName}-${appVersion}.${appType}"
        folderDeploy = "/datas/${appUser}"
        buildScript = "mvn clean install -DskipTests=true"
        copyScript = "sudo cp target/${processName} ${folderDeploy}"
        permsScript = "sudo chown -R ${appUser}. ${folderDeploy}"
        killScript = "sudo kill -9 \$(ps -ef| grep ${processName}| grep -v grep| awk '{print \$2}')"
        runScript = 'sudo su ${appUser} -c "cd ${folderDeploy}; nohup java -jar ${processName} > nohup.out 2>&1 &"'
    }
    stages {
        stage('build') {
            steps {
                sh (script: """ ${buildScript} """, label: "build with maven")
            }
        }
        stage('deploy') {
            steps {
                script {
                    try {
                        timeout(time: 5, unit: 'MINUTES'){
                            env.useChoice = input message: "Can it be deployed?",
                                parameters: [choice(name: "deploy", choices:'no\nyes', description: 'Choose "yes" if you want to deploy')]
                        }
                        if (env.useChoice == 'yes') {
                            sh (script: """ ${copyScript} """, label: "copy the .jar file into deploy folder")
                            sh (script: """ ${permsScript} """, label: "set permission folder")
                            sh (script: """ ${killScript} """, label: "terminate the running process")
                            sh (script: """ ${runScript} """, label: "run the projects")
                        }
                        else {
                            echo "Do not confirm the deployment"
                        }
                    } catch (Exception err) {
                        
                    }
                }
            }
        }
    }
}
