# corda-kyc-app
Corda Know Your Customer Project Repository

Run corda-kyc:: build steps

$ git clone https://github.com/corda/corda.git

change gradle wrapper properties file of "corda" project

import "d:\all_git_repo\corda" and click "Build Model"

comment line number 162 net.corda.node.services.statemachine.StateMachineManagerTests

run "build.gradle" under "corda-project"

# corda dependent jars
Core Corda platform dependent JARs: 
1. client 
2. corda 
3. core 
4. finance 
5. node

Publish Dependent Corda JARs into local .m2 repository 
Goto C:\......\corda>gradlew.bat install


corda::
-------
$ git clone https://github.com/corda/corda.git

$ cd corda

$ git checkout -b biksen-corda-branch tags/release-M7.0


cordapp-template::
------------------
$ git clone https://github.com/corda/cordapp-template.git

$ cd cordapp-template

$ git checkout -b biksen-cordapp-temp-branch tags/release-M7.0


REST API:
--------
PUT::
http://localhost:10005/api/example/NodeB/create-purchase-order

10005 is for NodeA / 10007 is for NodeB

Request:
{
     "kycId": 111,
     "userName": "Belated Happy Birthday Jiya"
 }

GET::
http://localhost:10005/api/example/purchase-orders
