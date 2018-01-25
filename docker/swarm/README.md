# Running Hadoop and Spark in Swarm cluster

Create an overlay network:
```
sudo docker network create -d overlay --attachable core
```
If it is the first time that you are going to execute the `docker-compose-hadoop-swarm.yml` (images are not cached in the file system and will be dowloaded from the Docker-Hub), you may be interested to the download progress, so first execute this pull command:
```
sudo docker-compose -f docker-compose-hadoop-swarm.yml pull
```
To deploy hadoop run:
```
sudo docker stack deploy -c docker-compose-hadoop-swarm.yml hadoop
```

When datanodes are deployed before namenode, they can not reach namenode over the network. In case this happened, restart the datanode service:
```
# First check the logs
sudo docker logs -f hadoop_datanode.mdpzql2nkqyabgikmy2ks9vgg.sl8hoqhrwd10kgrvagm2aopoe
# if there is a line 
# 17/09/25 08:06:19 WARN datanode.DataNode: Problem connecting to server: namenode:8020
# Check the service name
sudo docker service ls
# And restart the datanode service
sudo docker service update --force hadoop_datanode
```

As earlier for spark:
```
sudo docker-compose -f docker-compose-spark.yml pull
```
To deploy spark run:
```
sudo docker stack deploy -c docker-compose-spark.yml spark
```
