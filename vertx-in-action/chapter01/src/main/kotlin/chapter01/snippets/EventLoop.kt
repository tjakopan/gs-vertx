package chapter01.snippets

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import kotlin.concurrent.thread

class EventLoop {
  class Event(val key: String, val data: Any?)

  private val events: ConcurrentLinkedDeque<Event> = ConcurrentLinkedDeque()
  private val handlers: ConcurrentHashMap<String, (Any?) -> Unit> = ConcurrentHashMap()

  fun on(key: String, handler: (Any?) -> Unit): EventLoop {
    handlers[key] = handler
    return this
  }

  fun dispatch(event: Event) {
    events.add(event)
  }

  fun run() {
    while (!(events.isEmpty() && Thread.interrupted())) {
      if (!events.isEmpty()) {
        val event = events.pop()
        val handler = handlers[event.key]
        if (handler != null) handler(event.data)
        else System.err.println("No handler for key ${event.key}")
      }
    }
  }

  fun stop() {
    Thread.currentThread().interrupt()
  }

  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      val eventLoop = EventLoop()

      thread {
        for (i in 0..5) {
          delay(1000)
          eventLoop.dispatch(Event("tick", i))
        }
        eventLoop.dispatch(Event("stop", null))
      }

      thread {
        delay(2500)
        eventLoop.dispatch(Event("hello", "beautiful world"))
        delay(800)
        eventLoop.dispatch(Event("hello", "beautiful universe"))
      }

      eventLoop.dispatch(Event("hello", "world!"))
      eventLoop.dispatch(Event("foo", "bar"))

      eventLoop
        .on("hello") { s -> println("hello $s") }
        .on("tick") { n -> println("tick #$n") }
        .on("stop") { eventLoop.stop() }
        .run()

      println("Bye!")
    }

    private fun delay(millis: Long) =
      try {
        Thread.sleep(millis)
      } catch (e: InterruptedException) {
        throw RuntimeException(e)
      }
  }
}
