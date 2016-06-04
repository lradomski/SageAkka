package sample.hello;

import akka.actor.Cancellable;
import akka.actor.UntypedActor;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

public class Greeter extends UntypedActor {

  int i = 0;

  public enum Msg {
    TICK, GREET, DONE
  }

  @Override
  public void preStart() throws Exception {
      Cancellable cancellable = getContext().system().scheduler().schedule(Duration.Zero(),
              Duration.create(100, TimeUnit.MILLISECONDS), getSelf(), Msg.TICK,
              getContext().system().dispatcher(), null);}

  @Override
  public void onReceive(Object msg) {
      if (Msg.TICK == msg) {
          System.out.println(i);
          i = 0;
      } else if (msg == Msg.GREET) {
          ++i;
          //System.out.println("Hello World!");
      } else if (Msg.DONE == msg) {
          getSender().tell(Msg.DONE, getSelf());
      } else {
          unhandled(msg);
      }
  }

}
