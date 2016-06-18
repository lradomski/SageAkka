load("nashorn:mozilla_compat.js");

importPackage("akka.actor");
importPackage("com.tr.analytics.sage.akka");


var d = new com.tr.analytics.sage.akka.ScriptDriver();

var f = function()
{
    return d.system();
}

var system = function()
{
    return d.system();
}

var asm = function()
{
    return d.asm();
}

var makeReq = function(name, instance, isSnapshot,  rics)
{
    return d.makeReq(name, instance, isSnapshot, rics);
}

var reqAll = makeReq("TT", "ALL", true, ["*"]);
var reqAlls = makeReq("TT", "ALL", false, ["*"]);

var makeVerb = function(verb, data)
{
    return d.makeVerb(verb, data);
}

var nanoNow = function()
{
    return d.nanoTime();
}

var elapsed = function(nanoTime)
{
    return d.elapsed(nanoTime);
}

var ask = function(askTo, message, timeout)
{
    return d.ask(askTo, message, timeout);
}

var askAll = function()
{
    return ask(asm(), reqAll, "5 seconds");
}

var askAlls = function()
{
    return ask(asm(), reqAlls, "5 seconds");
}

var timedAsk = function(askTo, message, timeout)
{
    var start = nanoNow();
    var ret = ask(askTo, message, timeout);
    var duration = elapsed(start);
    return { result: ret, elapsed: duration };
}

var timedAskAll = function()
{
    return timedAsk(asm(), reqAll, "5 seconds");
}

var timeAll = function(count)
{
    if (!count) count = 1;
    var total = 0;

    for (var i = 0; i < count; i++)
    {
        var ms = timedAskAll().elapsed.toMillis();
        print(ms);
        total += ms;
    }

    print("Avg: " + (total/count).toFixed(2));
}

var ReplayParams = Java.type("com.tr.analytics.sage.akka.TradeSource.ReplayParams");
var defaultReplayRatePerMs = 30;

var replay = function(count)
{
    replayAt(count, defaultReplayRatePerMs)
}

var lastReplayParams = new ReplayParams();

var replayAt = function(count, ratePerMs)
{
    lastReplayParams = new ReplayParams(ratePerMs, count);
    d.sources().tell(makeVerb("start", lastReplayParams), null);
}

var rl = function()
{
    d.sources().tell(makeVerb("start", lastReplayParams), null);
}

var stopReplay = function()
{
    d.sources().tell(makeVerb("stop", null), null);
}

var cleanStores = function()
{
    d.tradeRouters().tell(makeVerb("clean", null), null);
}


var shutdown = function()
{
    d.shutdown();
    exit();
}


var actorOf = function(type, name)
{
    var props = Props.create(type.class);
    return system().actorOf(props, name);
}

var actorOf1 = function(type, ctorArg, name)
{
    var props = Props.create(type.class, ctorArg);
    return system().actorOf(props, name);
}

var stopActor = function(actorRef)
{
    system().stop(actorRef);
}

var Thread = Java.type("java.lang.Thread")

var sleep = function(millis)
{
    Thread.sleep(millis);
}

var UntypedActor = Java.type("akka.actor.UntypedActor");
var ScriptUntypedActor = Java.type("com.tr.analytics.sage.akka.ScriptUntypedActor");

var PrintActor = Java.extend(UntypedActor, {
    onReceive: function(message) {
        print(message);
    }
});

var CounterActorType = Java.extend(ScriptUntypedActor, {
    onReceiveCore: function(message, data) {
        ++data.count
//        print(message);
//        print(data);
    }
});


