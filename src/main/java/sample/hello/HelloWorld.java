package sample.hello;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.contrib.throttle.Throttler;
import akka.contrib.throttle.TimerBasedThrottler;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;


public class HelloWorld extends UntypedActor {

  @Override
  public void preStart() {


    // create the greeter actor
    final ActorRef greeter = getContext().actorOf(Props.create(Greeter.class), "greeter");

    // The throttler for this example, setting the rate
      int rateMs = 100;
      int intervalMs = 50;
    ActorRef throttler = getContext().actorOf(Props.create(TimerBasedThrottler.class,
    new Throttler.Rate(intervalMs*rateMs, Duration.create(intervalMs, TimeUnit.MILLISECONDS))));
    // Set the target
    throttler.tell(new Throttler.SetTarget(greeter), null);
      //throttler = greeter;

    // tell it to perform the greeting
    //greeter.tell(Greeter.Msg.GREET, getSelf());
      for(int i = 0; i < 10*1000*rateMs; i++) {
          throttler.tell(Greeter.Msg.GREET, getSelf());
      }
      throttler.tell(Greeter.Msg.DONE, getSelf());
  }

  @Override
  public void onReceive(Object msg) {
    if (msg == Greeter.Msg.DONE) {
      // when the greeter is done, stop this actor and with it the application
      getContext().stop(getSelf());
    } else
      unhandled(msg);
  }
}
