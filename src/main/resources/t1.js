//var v = { count: 0 }
//var a1 = actorOf1(CounterActorType, v, "a1");
//asm().tell(reqAll, a1);
//v.count

//asm().tell(makeReq("S","S", false, ['*']), a1)

//replay(1)

//askAll();

var actRes = [];
var count = 100;

for(var i = 0; i < count; i++)
{
    var v = { count: 0 };
    var a = actorOf1(CounterActorType, v, nanoNow());
    actRes.push( {act: a, res:v});
    asm().tell(reqAlls, a);
}

//actRes[0].res.count
//cleanStores();
//replay(1000);

/*
for (var i = 0; i < 1000; i++) if (actRes[i].res.count != 1001) { print(i + "-" + actRes[i].res.count); break; }
actRes[0].res.count
*/

//for(var i = 0; i < count; i++)
//{
//    system().stop(actRes[i].act)
//}

