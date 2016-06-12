package common;


import scala.concurrent.ExecutionContext;

import java.util.LinkedList;
import java.util.Queue;

// Useful for actor testing - inject this to control execution of actions submitted to dispatchers by making them run
// at specific points of time on test thread.
public class TestManualDispatcher implements ExecutionContext
{
    final Queue<Runnable> runnables = new LinkedList<>();
    //final ExecutorService es = Executors.newSingleThreadExecutor();

    @Override
    public void execute(Runnable runnable)
    {
        runnables.add(runnable);
    }

    public boolean allowOne()
    {
        if (0 < runnables.size()) {
            //es.submit(runnables.remove());
            runnables.remove().run();
            return true;
        }
        else return false;
    }


    public void allowAll()
    {
        while (allowOne())
        {
        }
    }

    public void clear()
    {
        runnables.clear();
    }

    public Queue<Runnable> getQueue()
    {
        return runnables;
    }

    @Override
    public void reportFailure(Throwable cause) {

    }

    @Override
    public ExecutionContext prepare() {
        return this;
    }
}