package datawave.microservice.query.executor.action;

import datawave.microservice.query.executor.QueryExecutor;
import datawave.microservice.query.logic.CheckpointableQueryLogic;
import datawave.microservice.query.logic.QueryLogic;
import datawave.microservice.query.remote.QueryRequest;
import datawave.microservice.query.storage.CachedQueryStatus;
import datawave.microservice.query.storage.QueryTask;
import datawave.microservice.query.storage.TaskKey;
import org.apache.accumulo.core.client.Connector;
import org.apache.log4j.Logger;

public class Next extends ExecutorAction {
    private static final Logger log = Logger.getLogger(Next.class);
    
    public Next(QueryExecutor source, QueryTask task) {
        super(source, task);
    }
    
    @Override
    public boolean executeTask(CachedQueryStatus queryStatus, Connector connector) throws Exception {
        
        assert (QueryRequest.Method.NEXT.equals(task.getAction()));
        
        boolean taskComplete = false;
        TaskKey taskKey = task.getTaskKey();
        String queryId = taskKey.getQueryId();
        
        QueryLogic<?> queryLogic = getQueryLogic(queryStatus.getQuery());
        if (queryLogic instanceof CheckpointableQueryLogic && ((CheckpointableQueryLogic) queryLogic).isCheckpointable()) {
            CheckpointableQueryLogic cpQueryLogic = (CheckpointableQueryLogic) queryLogic;
            
            cpQueryLogic.setupQuery(connector, task.getQueryCheckpoint());
            
            taskComplete = pullResults(taskKey, queryLogic, queryStatus, false);
            if (!taskComplete) {
                checkpoint(taskKey.getQueryKey(), cpQueryLogic);
                taskComplete = true;
            }
        } else {
            Exception e = new IllegalStateException("Attempting to get results for an uninitialized, non-checkpointable query logic");
            cache.updateFailedQueryStatus(queryId, e);
            throw e;
        }
        
        return taskComplete;
    }
    
}
