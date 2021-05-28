package datawave.microservice.query.runner;

import datawave.microservice.common.storage.QueryQueueListener;
import datawave.microservice.common.storage.QueryQueueManager;
import datawave.microservice.common.storage.QueryStatus;
import datawave.microservice.common.storage.QueryStorageCache;
import datawave.microservice.common.storage.Result;
import datawave.microservice.query.config.QueryExpirationProperties;
import datawave.microservice.query.config.QueryProperties;
import datawave.microservice.query.logic.QueryLogic;
import datawave.webservice.query.cache.QueryMetricFactory;
import datawave.webservice.query.cache.ResultsPage;
import datawave.webservice.query.data.ObjectSizeOf;
import datawave.webservice.query.exception.QueryException;
import datawave.webservice.query.metric.BaseQueryMetric;
import datawave.webservice.query.metric.QueryMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class NextCall implements Callable<ResultsPage<Object>> {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    private final QueryProperties queryProperties;
    private final QueryQueueManager queryQueueManager;
    private final QueryStorageCache queryStorageCache;
    private final String queryId;
    private final String identifier;
    
    private volatile boolean canceled = false;
    private volatile Future<ResultsPage<Object>> future = null;
    
    private final int userResultsPerPage;
    private final boolean maxResultsOverridden;
    private final long maxResultsOverride;
    private final long maxResults;
    private final int logicResultsPerPage;
    private final long logicBytesPerPage;
    private final long logicMaxWork;
    private final long maxResultsPerPage;
    
    private final List<Object> results = new LinkedList<>();
    private long pageSizeBytes;
    private long startTimeMillis;
    private ResultsPage.Status status = ResultsPage.Status.COMPLETE;
    
    private final BaseQueryMetric metric;
    
    public NextCall(QueryProperties queryProperties, QueryQueueManager queryQueueManager, QueryStorageCache queryStorageCache,
                    QueryMetricFactory queryMetricFactory, String queryId, QueryLogic<?> queryLogic, String identifier) throws QueryException {
        this.queryProperties = queryProperties;
        this.queryQueueManager = queryQueueManager;
        this.queryStorageCache = queryStorageCache;
        this.queryId = queryId;
        this.identifier = identifier;
        
        QueryStatus status = getQueryStatus();
        this.userResultsPerPage = status.getQuery().getPagesize();
        this.maxResultsOverridden = status.getQuery().isMaxResultsOverridden();
        this.maxResultsOverride = status.getQuery().getMaxResultsOverride();
        
        this.logicResultsPerPage = queryLogic.getMaxPageSize();
        this.logicBytesPerPage = queryLogic.getPageByteTrigger();
        this.logicMaxWork = queryLogic.getMaxWork();
        
        this.maxResultsPerPage = Math.min(userResultsPerPage, logicResultsPerPage);
        
        this.maxResults = queryLogic.getResultLimit(status.getQuery().getDnList());
        if (this.maxResults != queryLogic.getMaxResults()) {
            log.info("Maximum results set to " + this.maxResults + " instead of default " + queryLogic.getMaxResults() + ", user "
                            + status.getQuery().getUserDN() + " has a DN configured with a different limit");
        }
        
        this.metric = queryMetricFactory.createMetric();
    }
    
    @Override
    public ResultsPage<Object> call() throws Exception {
        startTimeMillis = System.currentTimeMillis();
        
        QueryQueueListener resultListener = queryQueueManager.createListener(identifier, queryId);
        
        // keep waiting for results until we're finished
        while (!isFinished(queryId)) {
            Message<Result> message = resultListener.receive(queryProperties.getResultPollRateMillis());
            if (message != null) {
                
                // TODO: In the past, if we got a null result we would mark the next call as finished
                // Should we continue to do that, or something else?
                Object result = message.getPayload().getPayload();
                if (result != null) {
                    results.add(result);
                    
                    if (logicBytesPerPage > 0) {
                        pageSizeBytes += ObjectSizeOf.Sizer.getObjectSize(result);
                    }
                } else {
                    log.debug("Null result encountered, no more results");
                    break;
                }
            }
        }
        
        return new ResultsPage<>(results, status);
    }
    
    private boolean isFinished(String queryId) {
        boolean finished = false;
        
        // 1) was this query canceled?
        if (canceled) {
            log.info("Query [{}]: query cancelled, aborting next call", queryId);
            finished = true;
        }
        
        // 2) have we hit the user's results-per-page limit?
        if (!finished && results.size() >= userResultsPerPage) {
            log.info("Query [{}]: user requested max page size has been reached, aborting next call", queryId);
            finished = true;
        }
        
        // 3) have we hit the query logic's results-per-page limit?
        if (!finished && results.size() >= logicResultsPerPage) {
            log.info("Query [{}]: query logic max page size has been reached, aborting next call", queryId);
            finished = true;
        }
        
        // 4) have we hit the query logic's bytes-per-page limit?
        if (!finished && pageSizeBytes >= logicBytesPerPage) {
            log.info("Query [{}]: query logic max page byte size has been reached, aborting next call", queryId);
            finished = true;
            status = ResultsPage.Status.PARTIAL;
        }
        
        // 5) have we hit the max results (or the max results override)?
        if (!finished) {
            long numResultsReturned = getQueryStatus().getNumResultsReturned();
            long numResults = numResultsReturned + results.size();
            if (this.maxResultsOverridden) {
                if (maxResultsOverride >= 0 && numResults >= maxResultsOverride) {
                    log.info("Query [{}]: max results override has been reached, aborting next call", queryId);
                    // TODO: Figure out query metrics
                    metric.setLifecycle(QueryMetric.Lifecycle.MAXRESULTS);
                    finished = true;
                }
            } else if (maxResults >= 0 && numResults >= maxResults) {
                log.info("Query [{}]: logic max results has been reached, aborting next call", queryId);
                // TODO: Figure out query metrics
                metric.setLifecycle(QueryMetric.Lifecycle.MAXRESULTS);
                finished = true;
            }
        }
        
        // TODO: Do I need to pull query metrics to get the next/seek count?
        // This used to come from the query logic transform iterator
        // 6) have we reached the "max work" limit? (i.e. next count + seek count)
        if (!finished && logicMaxWork >= 0 && (metric.getNextCount() + metric.getSeekCount()) >= logicMaxWork) {
            log.info("Query [{}]:  logic max work has been reached, aborting next call", queryId);
            // TODO: Figure out query metrics
            metric.setLifecycle(BaseQueryMetric.Lifecycle.MAXWORK);
            finished = true;
        }
        
        // 7) are we going to timeout before getting a full page? if so, return partial results
        if (!finished && timeout()) {
            log.info("Query [{}]: logic max expire before page is full, returning existing results: {} of {} results in {}ms", queryId, results.size(),
                            maxResultsPerPage, callTimeMillis());
            finished = true;
            status = ResultsPage.Status.PARTIAL;
        }
        
        return finished;
    }
    
    protected boolean timeout() {
        boolean timeout = false;
        
        // only return prematurely if we have at least 1 result
        if (!results.isEmpty()) {
            long callTimeMillis = callTimeMillis();
            
            QueryExpirationProperties expiration = queryProperties.getExpiration();
            
            // if after the page size short circuit check time
            if (callTimeMillis >= expiration.getShortCircuitCheckTimeMillis()) {
                float percentTimeComplete = (float) callTimeMillis / (float) (expiration.getCallTimeout());
                float percentResultsComplete = (float) results.size() / (float) maxResultsPerPage;
                // if the percent results complete is less than the percent time complete, then break out
                if (percentResultsComplete < percentTimeComplete) {
                    timeout = true;
                }
            }
            
            // if after the page short circuit timeout, then break out
            if (callTimeMillis >= expiration.getShortCircuitTimeoutMillis()) {
                timeout = true;
            }
        }
        
        return timeout;
    }
    
    private long callTimeMillis() {
        return System.currentTimeMillis() - startTimeMillis;
    }
    
    // TODO: We may want to control the rate that we pull query status from the cache
    // Perhaps by adding update interval properties.
    private QueryStatus getQueryStatus() {
        return queryStorageCache.getQueryStatus(UUID.fromString(queryId));
    }
    
    public void cancel() {
        this.canceled = true;
    }
    
    public boolean isCanceled() {
        return canceled;
    }
    
    public Future<ResultsPage<Object>> getFuture() {
        return future;
    }
    
    public void setFuture(Future<ResultsPage<Object>> future) {
        this.future = future;
    }
}