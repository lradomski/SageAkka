load("nashorn:mozilla_compat.js");

importPackage(akka.actor);
importPackage(com.tr.analytics.sage.akka);


var d = new com.tr.analytics.sage.akka.ScriptDriver();

var f = function()
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

var timedAsk = function(askTo, message, timeout)
{
    var start = nanoNow();
    var ret = ask(askTo, message, timeout);
    var duration = elapsed(nanoNow);
    return { result: ret, elapsed: duration };
}

var shutdown = function()
{
    return d.shutdown();
}


var UntypedActor = Java.type("akka.actor.UntypedActor");
var PrintActor = Java.extend(UntypedActor, {
    onReceive: function(message) {
        print(message);
    }
});

var LambdaActorType = function(lambda)
{
    return Java.extend(UntypedActor, lambda);
}
