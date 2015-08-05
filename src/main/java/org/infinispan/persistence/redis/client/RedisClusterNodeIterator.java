package org.infinispan.persistence.redis.client;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;

import java.util.Iterator;
import java.util.Map;

public class RedisClusterNodeIterator implements Iterator<Object>
{
    private RedisMarshaller<String> marshaller;
    private Map<String, JedisPool> clusterNodes;
    private Iterator<String> clusterNodeIt;
    private Iterator<Object> keyIterator = null;

    public RedisClusterNodeIterator(JedisCluster cluster, RedisMarshaller<String> marshaller)
    {
        this.marshaller = marshaller;
        this.clusterNodes = cluster.getClusterNodes();
        this.clusterNodeIt = clusterNodes.keySet().iterator();
    }

    @Override
    public boolean hasNext()
    {
        if (null != this.keyIterator && this.keyIterator.hasNext()) {
            // Further keys on the current cluster node to process
            return true;
        }
        else if (clusterNodeIt.hasNext()) {
            // Discover next cluster node
            Jedis client = null;

            try {
                JedisPool pool = this.clusterNodes.get(this.clusterNodeIt.next());
                client = pool.getResource();
                this.keyIterator = new RedisServerKeyIterator(client, this.marshaller);
                return this.keyIterator.hasNext();
            }
            catch(Exception ex) {
                if (null != client) {
                    client.close();
                }

                throw ex;
            }
        }
        else {
            return false;
        }
    }

    @Override
    public Object next()
    {
        return this.keyIterator.next();
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException();
    }
}